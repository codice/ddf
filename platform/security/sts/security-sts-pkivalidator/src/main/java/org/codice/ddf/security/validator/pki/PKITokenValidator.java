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
package org.codice.ddf.security.validator.pki;

import ddf.security.SecurityConstants;
import ddf.security.SubjectUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.collections.CollectionUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SignatureTrustValidator;
import org.apache.wss4j.dom.validate.Validator;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.PKIAuthenticationToken;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/** PKIAuthenticationToken validator for the STS. */
public class PKITokenValidator implements TokenValidator {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PKITokenValidator.class);

  private static final int SAN_RFC822NAME = 1;

  private Validator validator = new SignatureTrustValidator();

  private Merlin merlin;

  private List<String> realms = new ArrayList<>();

  private boolean doPathValidation = true;

  /** Initialize Merlin crypto object. */
  public void init() {
    // NOTE: THE TRUSTSTORE SHOULD BE USED FOR CERTIFICATE VALIDATION!!!!
    Path trustStorePath = Paths.get(SecurityConstants.getTruststorePath());

    if (!trustStorePath.isAbsolute()) {
      Path ddfHomePath = Paths.get(System.getProperty("ddf.home"));
      trustStorePath = Paths.get(ddfHomePath.toString(), trustStorePath.toString());
    }

    try (InputStream inputStream = Files.newInputStream(trustStorePath)) {
      KeyStore trustStore = SecurityConstants.newTruststore();
      trustStore.load(inputStream, SecurityConstants.getTruststorePassword().toCharArray());

      merlin = new Merlin();
      merlin.setKeyStore(trustStore);
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      LOGGER.warn("Unable to read merlin properties file. Unable to validate certificates.", e);
    }
  }

  /**
   * Set the WSS4J Validator instance to use to validate the token.
   *
   * @param validator the WSS4J Validator instance to use to validate the token
   */
  public void setValidator(Validator validator) {
    this.validator = validator;
  }

  public void setRealms(List<String> realms) {
    this.realms = realms;
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
   * @param realm
   * @return true if the token can be handled
   */
  public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
    PKIAuthenticationToken pkiToken = getPKITokenFromTarget(validateTarget);
    if (pkiToken != null) {
      if (realms != null && realms.contains(pkiToken.getRealm())
          || "*".equals(pkiToken.getRealm())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Validate a Token using the given TokenValidatorParameters.
   *
   * @param tokenParameters
   * @return TokenValidatorResponse
   */
  public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
    LOGGER.trace("Validating PKI Token");
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

    PKIAuthenticationToken pkiToken = getPKITokenFromTarget(validateTarget);

    if (pkiToken == null) {
      return response;
    }

    BinarySecurityTokenType binarySecurityType =
        pkiToken.createBinarySecurityTokenType(pkiToken.getCredentials());

    // Test the encoding type
    String encodingType = binarySecurityType.getEncodingType();
    if (!PKIAuthenticationToken.BASE64_ENCODING.equals(encodingType)) {
      LOGGER.trace("Bad encoding type attribute specified: {}", encodingType);
      return response;
    }

    //
    // Turn the received JAXB object into a DOM element
    //
    Document doc = DOMUtils.createDocument();
    BinarySecurity binarySecurity = new X509Security(doc);
    binarySecurity.setEncodingType(encodingType);
    binarySecurity.setValueType(binarySecurityType.getValueType());
    String data = binarySecurityType.getValue();
    Node textNode = doc.createTextNode(data);
    binarySecurity.getElement().appendChild(textNode);

    //
    // Validate the token
    //
    try {
      Credential credential = new Credential();
      credential.setBinarySecurityToken(binarySecurity);
      if (merlin != null) {
        byte[] token = binarySecurity.getToken();
        if (token != null) {
          X509Certificate[] certificates = merlin.getCertificatesFromBytes(token);
          if (certificates != null) {
            if (doPathValidation) {
              credential.setCertificates(certificates);
            } else {
              credential.setCertificates(new X509Certificate[] {certificates[0]});
            }
          }
        } else {
          LOGGER.debug("Binary Security Token bytes were null.");
        }
      }

      Credential returnedCredential = validator.validate(credential, requestData);
      X500Principal subjectX500Principal =
          returnedCredential.getCertificates()[0].getSubjectX500Principal();

      response.setPrincipal(subjectX500Principal);
      if (response.getAdditionalProperties() == null) {
        response.setAdditionalProperties(new HashMap<>());
      }

      List<String> emailAddresses = new ArrayList<>();

      addRfc822Name(returnedCredential, emailAddresses);

      try {
        String emailAddress = SubjectUtils.getEmailAddress(subjectX500Principal);
        if (emailAddress != null) {
          emailAddresses.add(emailAddress);
        }

        if (CollectionUtils.isNotEmpty(emailAddresses)) {
          response
              .getAdditionalProperties()
              .put(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI, emailAddresses);
        }

        String country = SubjectUtils.getCountry(subjectX500Principal);
        if (country != null) {
          response.getAdditionalProperties().put(SubjectUtils.COUNTRY_CLAIM_URI, country);
        }
      } catch (Exception e) {
        LOGGER.debug("Unable to set email address or country from certificate.", e);
      }
      validateTarget.setPrincipal(subjectX500Principal);
      validateTarget.setState(STATE.VALID);
    } catch (WSSecurityException ex) {
      LOGGER.info("Unable to validate credentials.", ex);
    }
    return response;
  }

  private void addRfc822Name(Credential credential, List<String> emailAddresses) {
    try {
      Collection<List<?>> subjectAlternativeNames =
          credential.getCertificates()[0].getSubjectAlternativeNames();
      if (CollectionUtils.isNotEmpty(subjectAlternativeNames)) {
        subjectAlternativeNames
            .stream()
            .filter(sanData -> sanData.size() > 1)
            .map(this::getRfc822Name)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(emailAddresses::add);
      }
    } catch (CertificateParsingException e) {
      LOGGER.debug("Unable to get Subject Alternative Names from certificate", e);
    }
  }

  private Optional<String> getRfc822Name(List<?> alternativeNameData) {
    if (alternativeNameData.size() > 1) {
      Object typeData = alternativeNameData.get(0);
      if (typeData instanceof Integer) {
        Integer type = (Integer) typeData;
        if (type == SAN_RFC822NAME) {
          Object emailAddress = alternativeNameData.get(1);
          if (emailAddress instanceof String) {
            return Optional.of((String) emailAddress);
          }
        }
      }
    }
    return Optional.empty();
  }

  public void setPathValidation(boolean value) {
    doPathValidation = value;
  }

  private PKIAuthenticationToken getPKITokenFromTarget(ReceivedToken validateTarget) {
    Object token = validateTarget.getToken();
    if ((token instanceof BinarySecurityTokenType)
        && PKIAuthenticationToken.PKI_TOKEN_VALUE_TYPE.equals(
            ((BinarySecurityTokenType) token).getValueType())) {
      String encodedCredential = ((BinarySecurityTokenType) token).getValue();
      LOGGER.debug("Encoded username/password credential: {}", encodedCredential);
      BaseAuthenticationToken base = null;
      try {
        base = PKIAuthenticationToken.parse(encodedCredential, true);
        return new PKIAuthenticationToken(
            base.getPrincipal(), base.getCredentials().toString(), base.getRealm());
      } catch (WSSecurityException e) {
        LOGGER.info(
            "Unable to parse {} from encodedToken.",
            PKIAuthenticationToken.class.getSimpleName(),
            e);
        return null;
      }
    }
    return null;
  }
}
