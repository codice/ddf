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
 *
 * <p>
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codice.ddf.security.validator.uname;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.bind.JAXBElement;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.AttributedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.secext.PasswordString;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.JAASUsernameTokenValidator;
import org.apache.wss4j.dom.validate.Validator;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.security.common.FailedLoginDelayer;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** DDFUsername BST validator for the STS. */
public class UPBSTValidator implements TokenValidator {
  private static final Logger LOGGER = LoggerFactory.getLogger(UPBSTValidator.class);

  private final Parser parser;

  private final FailedLoginDelayer failedLoginDelayer;

  protected Map<String, Validator> validators = new ConcurrentHashMap<>();

  public UPBSTValidator(Parser parser, FailedLoginDelayer failedLoginDelayer) {
    this.parser = parser;
    this.failedLoginDelayer = failedLoginDelayer;
  }

  public void addRealm(ServiceReference<JaasRealm> serviceReference) {
    Bundle bundle = FrameworkUtil.getBundle(UPBSTValidator.class);
    if (null != bundle) {
      JaasRealm realm = bundle.getBundleContext().getService(serviceReference);
      LOGGER.trace("Adding validator for JaasRealm {}", realm.getName());
      JAASUsernameTokenValidator validator = new JAASUsernameTokenValidator();
      validator.setContextName(realm.getName());
      validators.put(realm.getName(), validator);
    }
  }

  public void removeRealm(ServiceReference<JaasRealm> serviceReference) {
    Bundle bundle = FrameworkUtil.getBundle(UPBSTValidator.class);
    if (null != bundle) {
      JaasRealm realm = bundle.getBundleContext().getService(serviceReference);
      LOGGER.trace("Removing validator for JaasRealm {}", realm.getName());
      validators.remove(realm.getName());
    }
  }

  /**
   * Return true if this TokenValidator implementation is capable of validating the ReceivedToken
   * argument.
   *
   * @param validateTarget
   * @return true if the token can be handled
   */
  public boolean canHandleToken(ReceivedToken validateTarget) {
    return canHandleToken(validateTarget, null);
  }

  /**
   * Return true if this TokenValidator implementation is capable of validating the ReceivedToken
   * argument. The realm is ignored in this token Validator.
   *
   * @param validateTarget
   * @param cxfRealm
   * @return true if the token can be handled
   */
  public boolean canHandleToken(ReceivedToken validateTarget, String cxfRealm) {
    boolean canHandle = false;
    UPAuthenticationToken usernameToken = getUsernameTokenFromTarget(validateTarget);
    if (usernameToken != null) {
      // currently realm is not being passed through (no RealmParser that determines the realm
      // based on the web context. So this just looks at the realm passed in the credentials.
      // This generic instance just looks for the default realms (DDF and Karaf)
      if (usernameToken.getRealm() == null) {
        LOGGER.trace("No realm specified in request");
        canHandle = (validators != null);
      } else if (validators != null && validators.containsKey(usernameToken.getRealm())) {
        LOGGER.trace("Realm '{}' recognized - canHandleToken = true", usernameToken.getRealm());
        canHandle = true;
      } else if (validators != null && "*".equals(usernameToken.getRealm())) {
        LOGGER.trace("Realm '*' recognized - canHandleToken = true");
        canHandle = true;
      }
      if (!canHandle) {
        LOGGER.trace("Realm '{}' unrecognized - canHandleToken = false", usernameToken.getRealm());
      }
    }
    LOGGER.debug("Returning canHandle: {}", canHandle);
    return canHandle;
  }

  /**
   * Validate a Token using the given TokenValidatorParameters.
   *
   * @param tokenParameters
   * @return TokenValidatorResponse
   */
  public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
    LOGGER.trace("Validating UPBST Token");

    if (parser == null) {
      throw new IllegalStateException("XMLParser must be configured.");
    }

    if (failedLoginDelayer == null) {
      throw new IllegalStateException("Failed Login Delayer must be configured");
    }

    STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
    Crypto sigCrypto = stsProperties.getSignatureCrypto();
    CallbackHandler callbackHandler = stsProperties.getCallbackHandler();

    RequestData requestData = new RequestData();
    requestData.setSigVerCrypto(sigCrypto);
    requestData.setWssConfig(WSSConfig.getNewInstance());
    requestData.setCallbackHandler(callbackHandler);

    TokenValidatorResponse response = new TokenValidatorResponse();
    ReceivedToken validateTarget = tokenParameters.getToken();
    validateTarget.setState(STATE.INVALID);
    response.setToken(validateTarget);

    if (!validateTarget.isBinarySecurityToken()) {
      return response;
    }

    BinarySecurityTokenType binarySecurityType =
        (BinarySecurityTokenType) validateTarget.getToken();

    // Test the encoding type
    String encodingType = binarySecurityType.getEncodingType();
    if (!UPAuthenticationToken.BASE64_ENCODING.equals(encodingType)) {
      LOGGER.trace("Bad encoding type attribute specified: {}", encodingType);
      return response;
    }

    UPAuthenticationToken usernameToken = getUsernameTokenFromTarget(validateTarget);
    if (usernameToken == null) {
      return response;
    }
    UsernameTokenType usernameTokenType = getUsernameTokenType(usernameToken);
    // Marshall the received JAXB object into a DOM Element
    Element usernameTokenElement = null;

    JAXBElement<UsernameTokenType> tokenType =
        new JAXBElement<>(
            QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameTokenType);

    Document doc = DOMUtils.createDocument();
    Element rootElement = doc.createElement("root-element");

    List<String> ctxPath = new ArrayList<>(1);
    ctxPath.add(UsernameTokenType.class.getPackage().getName());

    ParserConfigurator configurator =
        parser.configureParser(ctxPath, UPBSTValidator.class.getClassLoader());
    try {
      parser.marshal(configurator, tokenType, rootElement);
    } catch (ParserException ex) {
      LOGGER.info("Unable to parse username token", ex);
      return response;
    }
    usernameTokenElement = (Element) rootElement.getFirstChild();

    //
    // Validate the token
    //
    WSSConfig wssConfig = WSSConfig.getNewInstance();
    try {
      boolean allowNamespaceQualifiedPasswordTypes =
          requestData.isAllowNamespaceQualifiedPasswordTypes();
      UsernameToken ut =
          new UsernameToken(
              usernameTokenElement, allowNamespaceQualifiedPasswordTypes, new BSPEnforcer());

      // The parsed principal is set independent whether validation is successful or not
      response.setPrincipal(new CustomTokenPrincipal(ut.getName()));
      if (ut.getPassword() == null) {
        return response;
      }
      String tokenId =
          String.format(
              "%s:%s:%s",
              usernameToken.getUsername(), usernameToken.getPassword(), usernameToken.getRealm());

      // See if the UsernameToken is stored in the cache
      int hash = tokenId.hashCode();
      SecurityToken secToken = null;
      if (tokenParameters.getTokenStore() != null) {
        secToken = tokenParameters.getTokenStore().getToken(Integer.toString(hash));
        if (secToken != null && secToken.getTokenHash() != hash) {
          secToken = null;
        } else if (secToken != null) {
          validateTarget.setState(STATE.VALID);
        }
      }

      if (secToken == null) {
        Credential credential = new Credential();
        credential.setUsernametoken(ut);

        if (usernameToken.getRealm() != null && !"*".equals(usernameToken.getRealm())) {
          Validator validator = validators.get(usernameToken.getRealm());
          if (validator != null) {
            try {
              validator.validate(credential, requestData);
              validateTarget.setState(STATE.VALID);
              LOGGER.debug("Validated user against realm {}", usernameToken.getRealm());
            } catch (WSSecurityException ex) {
              LOGGER.debug("Not able to validate user against realm {}", usernameToken.getRealm());
            }
          }
        } else {
          Set<Map.Entry<String, Validator>> entries = validators.entrySet();
          for (Map.Entry<String, Validator> entry : entries) {
            try {
              entry.getValue().validate(credential, requestData);
              validateTarget.setState(STATE.VALID);
              LOGGER.debug("Validated user against realm {}", entry.getKey());
              break;
            } catch (WSSecurityException ex) {
              LOGGER.debug("Not able to validate user against realm {}", entry.getKey());
            }
          }
        }
      }

      Principal principal =
          createPrincipal(
              ut.getName(), ut.getPassword(), ut.getPasswordType(), ut.getNonce(), ut.getCreated());

      // Store the successfully validated token in the cache
      if (tokenParameters.getTokenStore() != null
          && secToken == null
          && STATE.VALID.equals(validateTarget.getState())) {
        secToken = new SecurityToken(ut.getID());
        secToken.setToken(ut.getElement());
        int hashCode = tokenId.hashCode();
        String identifier = Integer.toString(hashCode);
        secToken.setTokenHash(hashCode);
        tokenParameters.getTokenStore().add(identifier, secToken);
      }

      response.setPrincipal(principal);
      response.setTokenRealm(null);
      validateTarget.setPrincipal(principal);
    } catch (WSSecurityException ex) {
      LOGGER.debug("Unable to validate token.", ex);
    }

    if (response.getToken().getState() != STATE.VALID) {
      failedLoginDelayer.delay(response.getToken().getPrincipal().getName());
    }

    return response;
  }

  /** Create a principal based on the authenticated UsernameToken. */
  private Principal createPrincipal(
      String username,
      String passwordValue,
      String passwordType,
      String nonce,
      String createdTime) {
    boolean hashed = false;
    if (WSConstants.PASSWORD_DIGEST.equals(passwordType)) {
      hashed = true;
    }
    WSUsernameTokenPrincipalImpl principal = new WSUsernameTokenPrincipalImpl(username, hashed);
    if (nonce != null) {
      principal.setNonce(nonce.getBytes(StandardCharsets.UTF_8));
    }
    principal.setPassword(passwordValue);
    principal.setCreatedTime(createdTime);
    principal.setPasswordType(passwordType);
    return principal;
  }

  public UsernameTokenType getUsernameTokenType(UPAuthenticationToken token) {
    UsernameTokenType usernameTokenType = new UsernameTokenType();
    AttributedString user = new AttributedString();
    user.setValue(token.getUsername());
    usernameTokenType.setUsername(user);

    // Add a password
    PasswordString password = new PasswordString();
    password.setValue(token.getPassword());
    password.setType(WSConstants.PASSWORD_TEXT);
    JAXBElement<PasswordString> passwordType =
        new JAXBElement<PasswordString>(QNameConstants.PASSWORD, PasswordString.class, password);
    usernameTokenType.getAny().add(passwordType);

    return usernameTokenType;
  }

  private UPAuthenticationToken getUsernameTokenFromTarget(ReceivedToken validateTarget) {
    Object token = validateTarget.getToken();
    if ((token instanceof BinarySecurityTokenType)
        && UPAuthenticationToken.UP_TOKEN_VALUE_TYPE.equals(
            ((BinarySecurityTokenType) token).getValueType())) {
      String encodedCredential = ((BinarySecurityTokenType) token).getValue();
      LOGGER.debug("Encoded username/password credential: {}", encodedCredential);
      BaseAuthenticationToken base = null;
      try {
        base = UPAuthenticationToken.parse(encodedCredential, true);
        return new UPAuthenticationToken(
            base.getPrincipal().toString(), base.getCredentials().toString(), base.getRealm());
      } catch (WSSecurityException e) {
        LOGGER.info(
            "Unable to parse {} from encodedToken.",
            UPAuthenticationToken.class.getSimpleName(),
            e);
        return null;
      }
    }
    return null;
  }
}
