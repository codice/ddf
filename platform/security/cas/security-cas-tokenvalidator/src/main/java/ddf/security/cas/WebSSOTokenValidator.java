/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.cas;

import ddf.security.encryption.EncryptionService;
import java.nio.charset.Charset;
import java.util.Base64;
import javax.security.auth.callback.CallbackHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.codice.ddf.configuration.PropertyResolver;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates Web Single Sign-On Tokens.
 *
 * @author kcwire
 */
public class WebSSOTokenValidator implements TokenValidator {

  // The Supported SSO Token Types
  public static final String CAS_TYPE = "#CAS";

  public static final String CAS_BST_SEP = "|";

  private static final Logger LOGGER = LoggerFactory.getLogger(WebSSOTokenValidator.class);

  private PropertyResolver casServerUrl;

  private EncryptionService encryptionService;

  public String getCasServerUrl() {
    return casServerUrl.getResolvedString();
  }

  public void setCasServerUrl(String casServerUrl) {
    this.casServerUrl = new PropertyResolver(casServerUrl);
  }

  public void setEncryptionService(EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
  }

  /*
   * Return true if this TokenValidator implementation is capable of validating the ReceivedToken
   * argument.
   */
  @Override
  public boolean canHandleToken(ReceivedToken validateTarget) {
    return canHandleToken(validateTarget, null);
  }

  /*
   * Return true if this TokenValidator implementation is capable of validating the ReceivedToken
   * argument. The realm is ignored in this token Validator.
   */
  @Override
  public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
    final Object token = validateTarget.getToken();
    // Check the ValueType to see if this is a supported SSO Token.
    if ((token instanceof BinarySecurityTokenType)) {
      if (CAS_TYPE.equalsIgnoreCase(((BinarySecurityTokenType) token).getValueType())) {
        LOGGER.debug(
            "Can handle token type of: " + ((BinarySecurityTokenType) token).getValueType());
        return true;
      }
      LOGGER.debug(
          "Cannot handle token type of: " + ((BinarySecurityTokenType) token).getValueType());
    }
    return false;
  }

  /** Validate a Token using the given TokenValidatorParameters. */
  @Override
  public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
    LOGGER.debug("Validating SSO Token");
    STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
    Crypto sigCrypto = stsProperties.getSignatureCrypto();
    CallbackHandler callbackHandler = stsProperties.getCallbackHandler();

    RequestData requestData = new RequestData();
    requestData.setSigVerCrypto(sigCrypto);
    WSSConfig wssConfig = WSSConfig.getNewInstance();
    requestData.setWssConfig(wssConfig);
    requestData.setCallbackHandler(callbackHandler);

    LOGGER.debug("Setting validate state to invalid before check.");
    TokenValidatorResponse response = new TokenValidatorResponse();
    ReceivedToken validateTarget = tokenParameters.getToken();
    validateTarget.setState(STATE.INVALID);
    response.setToken(validateTarget);

    if (!validateTarget.isBinarySecurityToken()) {
      LOGGER.debug("Validate target is not a binary security token, returning invalid response.");
      return response;
    }
    LOGGER.debug("Getting binary security token from validate target");
    BinarySecurityTokenType binarySecurityToken =
        (BinarySecurityTokenType) validateTarget.getToken();

    //
    // Decode the token
    //
    LOGGER.debug("Decoding binary security token.");
    String base64Token = binarySecurityToken.getValue();
    String ticket = null;
    String service = null;
    try {
      byte[] token = Base64.getDecoder().decode(base64Token);
      if (token == null || token.length == 0) {
        throw new WSSecurityException(
            WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN,
            "Binary security token NOT successfully decoded, is empty or null.");
      }
      String decodedToken = new String(token, Charset.forName("UTF-8"));
      if (StringUtils.isNotBlank(decodedToken)) {
        LOGGER.debug("Binary security token successfully decoded: {}", decodedToken);
        // Token is in the format ticket|service
        String[] parts = StringUtils.split(decodedToken, CAS_BST_SEP);
        if (parts.length == 2) {
          ticket = parts[0];
          service = parts[1];
        } else {
          throw new WSSecurityException(
              WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN,
              "Was not able to parse out BST propertly. Should be in ticket|service format.");
        }
      } else {
        throw new WSSecurityException(
            WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN,
            "Binary security token NOT successfully decoded, is empty or null.");
      }
    } catch (WSSecurityException wsse) {
      String msg = "Unable to decode BST into ticket and service for validation to CAS.";
      LOGGER.info(msg, wsse);
      return response;
    }

    //
    // Do some validation of the token here
    //
    try {
      LOGGER.debug("Validating ticket [{}] for service [{}].", ticket, service);

      // validate either returns an assertion or throws an exception
      Assertion assertion = validate(ticket, service);

      AttributePrincipal principal = assertion.getPrincipal();
      LOGGER.debug("User name retrieved from CAS: {}", principal.getName());

      response.setPrincipal(principal);
      LOGGER.debug("CAS ticket successfully validated, setting state to valid.");
      validateTarget.setState(STATE.VALID);

    } catch (TicketValidationException e) {
      LOGGER.debug("Unable to validate CAS token.", e);
    }

    return response;
  }

  /**
   * Validate the CAS ticket and service
   *
   * @param ticket
   * @param service
   * @return
   * @throws TicketValidationException
   */
  public Assertion validate(String ticket, String service) throws TicketValidationException {
    LOGGER.trace("CAS Server URL = {}", casServerUrl);

    Cas20ProxyTicketValidator casValidator =
        new Cas20ProxyTicketValidator(casServerUrl.getResolvedString());
    casValidator.setAcceptAnyProxy(true);

    return casValidator.validate(ticket, service);
  }
}
