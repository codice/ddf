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
package org.codice.ddf.security.validator.x509;

import ddf.security.SecurityConstants;
import ddf.security.SubjectUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.x500.X500Principal;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.token.realm.CertConstraintsParser;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.sts.token.validator.X509TokenValidator;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SignatureTrustValidator;
import org.apache.wss4j.dom.validate.Validator;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.content.X509Data;
import org.apache.xml.security.keys.content.x509.XMLX509Certificate;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * X509PKIPathv1 validator for the STS. This validator is responsible for validating X509 tokens
 * that are directly presented to the STS without going through any SSO Filters. It will validate
 * the certificate chain instead of the specific certificate.
 */
public class X509PathTokenValidator implements TokenValidator {

  public static final String X509_PKI_PATH = WSConstants.X509TOKEN_NS + "#X509PKIPathv1";

  public static final String BASE64_ENCODING = WSConstants.SOAPMESSAGE_NS + "#Base64Binary";

  private static final org.slf4j.Logger LOGGER =
      LoggerFactory.getLogger(X509PathTokenValidator.class);

  protected Merlin merlin;

  private Validator validator = new SignatureTrustValidator();

  private CertConstraintsParser certConstraints = new CertConstraintsParser();

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
   * Set a list of Strings corresponding to regular expression constraints on the subject DN of a
   * certificate
   */
  public void setSubjectConstraints(List<String> subjectConstraints) {
    certConstraints.setSubjectConstraints(subjectConstraints);
  }

  /**
   * Set the WSS4J Validator instance to use to validate the token.
   *
   * @param validator the WSS4J Validator instance to use to validate the token
   */
  public void setValidator(Validator validator) {
    this.validator = validator;
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
    Object token = validateTarget.getToken();
    if ((token instanceof BinarySecurityTokenType)
        && (X509_PKI_PATH.equals(((BinarySecurityTokenType) token).getValueType())
            || X509TokenValidator.X509_V3_TYPE.equals(
                ((BinarySecurityTokenType) token).getValueType()))) {
      return true;
    }
    return (token instanceof Element
        && WSConstants.SIG_NS.equals(((Element) token).getNamespaceURI())
        && WSConstants.X509_DATA_LN.equals(((Element) token).getLocalName()));
  }

  /**
   * Validate a Token using the given TokenValidatorParameters.
   *
   * @param tokenParameters
   * @return TokenValidatorResponse
   */
  public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
    LOGGER.trace("Validating X.509 Token");
    STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
    Crypto sigCrypto = stsProperties.getSignatureCrypto();
    CallbackHandler callbackHandler = stsProperties.getCallbackHandler();

    RequestData requestData = new RequestData();
    requestData.setSigVerCrypto(sigCrypto);
    requestData.setWssConfig(WSSConfig.getNewInstance());
    requestData.setCallbackHandler(callbackHandler);
    requestData.setMsgContext(tokenParameters.getMessageContext());
    requestData.setSubjectCertConstraints(certConstraints.getCompiledSubjectContraints());

    TokenValidatorResponse response = new TokenValidatorResponse();
    ReceivedToken validateTarget = tokenParameters.getToken();
    validateTarget.setState(STATE.INVALID);
    response.setToken(validateTarget);

    BinarySecurity binarySecurity = null;
    BinarySecurityTokenType binarySecurityType = null;
    if (validateTarget.isBinarySecurityToken()) {
      binarySecurityType = (BinarySecurityTokenType) validateTarget.getToken();

      // Test the encoding type
      String encodingType = binarySecurityType.getEncodingType();
      if (!BASE64_ENCODING.equals(encodingType)) {
        LOGGER.trace("Bad encoding type attribute specified: {}", encodingType);
        return response;
      }

      //
      // Turn the received JAXB object into a DOM element
      //
      Document doc = DOMUtils.createDocument();
      binarySecurity = new X509Security(doc);
      binarySecurity.setEncodingType(encodingType);
      binarySecurity.setValueType(binarySecurityType.getValueType());
      String data = binarySecurityType.getValue();
      Node textNode = doc.createTextNode(data);
      binarySecurity.getElement().appendChild(textNode);
    } else if (validateTarget.isDOMElement()) {
      try {
        Document doc = DOMUtils.createDocument();
        binarySecurity = new X509Security(doc);
        binarySecurity.setEncodingType(BASE64_ENCODING);
        X509Data x509Data = new X509Data((Element) validateTarget.getToken(), "");
        if (x509Data.containsCertificate()) {
          XMLX509Certificate xmlx509Certificate = x509Data.itemCertificate(0);
          if (xmlx509Certificate == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN);
          }
          X509Certificate cert = xmlx509Certificate.getX509Certificate();
          ((X509Security) binarySecurity).setX509Certificate(cert);
        }
      } catch (WSSecurityException ex) {
        LOGGER.debug("Unable to set certificate", ex);
        return response;
      } catch (XMLSecurityException ex) {
        LOGGER.debug("Unable to get certificates", ex);
        return response;
      }
    } else {
      return response;
    }

    //
    // Validate the token
    //
    try {
      Credential credential = new Credential();
      credential.setBinarySecurityToken(binarySecurity);
      if (merlin != null) {
        byte[] token = binarySecurity.getToken();
        if (token != null) {
          if (binarySecurityType != null) {
            if (binarySecurityType.getValueType().equals(X509_PKI_PATH)) {
              X509Certificate[] certificates = merlin.getCertificatesFromBytes(token);
              if (certificates != null) {
                credential.setCertificates(certificates);
              }
            } else {
              X509Certificate singleCert = merlin.loadCertificate(new ByteArrayInputStream(token));
              credential.setCertificates(new X509Certificate[] {singleCert});
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
      try {
        String emailAddress = SubjectUtils.getEmailAddress(subjectX500Principal);
        if (emailAddress != null) {
          response
              .getAdditionalProperties()
              .put(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI, emailAddress);
        }
        String country = SubjectUtils.getCountry(subjectX500Principal);
        if (country != null) {
          response.getAdditionalProperties().put(SubjectUtils.COUNTRY_CLAIM_URI, country);
        }
      } catch (Exception e) {
        LOGGER.debug("Unable to set email address or country from certificate.", e);
      }
      validateTarget.setState(STATE.VALID);
      validateTarget.setPrincipal(subjectX500Principal);
    } catch (WSSecurityException ex) {
      LOGGER.debug("Unable to validate credentials.", ex);
    }
    return response;
  }
}
