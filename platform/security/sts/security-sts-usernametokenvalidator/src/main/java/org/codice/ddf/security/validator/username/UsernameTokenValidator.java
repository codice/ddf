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
package org.codice.ddf.security.validator.username;

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
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** This class validates a wsse UsernameToken. */
public class UsernameTokenValidator implements TokenValidator {

  private final Parser parser;

  private final FailedLoginDelayer failedLoginDelayer;

  private static final Logger LOGGER = LoggerFactory.getLogger(UsernameTokenValidator.class);

  protected Map<String, Validator> validators = new ConcurrentHashMap<>();

  public UsernameTokenValidator(Parser parser, FailedLoginDelayer failedLoginDelayer) {
    this.parser = parser;
    this.failedLoginDelayer = failedLoginDelayer;
  }

  public void addRealm(ServiceReference<JaasRealm> serviceReference) {
    Bundle bundle = FrameworkUtil.getBundle(UsernameTokenValidator.class);
    if (null != bundle) {
      JaasRealm realm = bundle.getBundleContext().getService(serviceReference);
      LOGGER.trace("Adding validator for JaasRealm {}", realm.getName());
      JAASUsernameTokenValidator validator = new JAASUsernameTokenValidator();
      validator.setContextName(realm.getName());
      validators.put(realm.getName(), validator);
    }
  }

  public void removeRealm(ServiceReference<JaasRealm> serviceReference) {
    Bundle bundle = FrameworkUtil.getBundle(UsernameTokenValidator.class);
    if (null != bundle) {
      JaasRealm realm = bundle.getBundleContext().getService(serviceReference);
      LOGGER.trace("Removing validator for JaasRealm {}", realm.getName());
      validators.remove(realm.getName());
    }
  }

  /**
   * Return true if this TokenValidator implementation is capable of validating the ReceivedToken
   * argument.
   */
  public boolean canHandleToken(ReceivedToken validateTarget) {
    return canHandleToken(validateTarget, null);
  }

  /**
   * Return true if this TokenValidator implementation is capable of validating the ReceivedToken
   * argument. The realm is ignored in this token Validator.
   */
  public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
    if (validateTarget.getToken() instanceof UsernameTokenType) {
      return true;
    }
    return false;
  }

  /** Validate a Token using the given TokenValidatorParameters. */
  public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
    LOGGER.debug("Validating UsernameToken");

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
    WSSConfig wssConfig = WSSConfig.getNewInstance();
    requestData.setWssConfig(wssConfig);
    requestData.setCallbackHandler(callbackHandler);

    TokenValidatorResponse response = new TokenValidatorResponse();
    ReceivedToken validateTarget = tokenParameters.getToken();
    validateTarget.setState(ReceivedToken.STATE.INVALID);
    response.setToken(validateTarget);

    if (!validateTarget.isUsernameToken()) {
      return response;
    }

    //
    // Turn the JAXB UsernameTokenType into a DOM Element for validation
    //
    UsernameTokenType usernameTokenType = (UsernameTokenType) validateTarget.getToken();
    JAXBElement<UsernameTokenType> tokenType =
        new JAXBElement<>(
            QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameTokenType);
    Document doc = DOMUtils.createDocument();
    Element rootElement = doc.createElement("root-element");

    List<String> ctxPath = new ArrayList<>(1);
    ctxPath.add(UsernameTokenType.class.getPackage().getName());

    Element usernameTokenElement = null;

    ParserConfigurator configurator =
        parser.configureParser(ctxPath, UsernameTokenValidator.class.getClassLoader());
    try {
      parser.marshal(configurator, tokenType, rootElement);
      usernameTokenElement = (Element) rootElement.getFirstChild();
    } catch (ParserException ex) {
      LOGGER.info("Unable to parse username token", ex);
      return response;
    }

    //
    // Validate the token
    //
    try {
      boolean allowNamespaceQualifiedPasswordTypes =
          requestData.isAllowNamespaceQualifiedPasswordTypes();
      UsernameToken ut =
          new UsernameToken(
              usernameTokenElement, allowNamespaceQualifiedPasswordTypes, new BSPEnforcer());
      // The parsed principal is set independent whether validation is successful or not
      response.setPrincipal(new CustomTokenPrincipal(ut.getName()));
      if (ut.getPassword() == null) {
        failedLoginDelayer.delay(ut.getName());

        return response;
      }

      Credential credential = new Credential();
      credential.setUsernametoken(ut);
      // Only this section is new, the rest is copied from the apache class
      Set<Map.Entry<String, Validator>> entries = validators.entrySet();
      for (Map.Entry<String, Validator> entry : entries) {
        try {
          entry.getValue().validate(credential, requestData);
          validateTarget.setState(ReceivedToken.STATE.VALID);
          break;
        } catch (WSSecurityException ex) {
          LOGGER.debug("Unable to validate user against {}" + entry.getKey(), ex);
        }
      }
      if (ReceivedToken.STATE.INVALID.equals(validateTarget.getState())) {
        failedLoginDelayer.delay(ut.getName());

        return response;
      }
      // end new section

      Principal principal =
          createPrincipal(
              ut.getName(), ut.getPassword(), ut.getPasswordType(), ut.getNonce(), ut.getCreated());

      response.setPrincipal(principal);
      response.setTokenRealm(null);
      validateTarget.setState(ReceivedToken.STATE.VALID);
      validateTarget.setPrincipal(principal);
    } catch (WSSecurityException ex) {
      LOGGER.debug("Unable to validate token.", ex);
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
}
