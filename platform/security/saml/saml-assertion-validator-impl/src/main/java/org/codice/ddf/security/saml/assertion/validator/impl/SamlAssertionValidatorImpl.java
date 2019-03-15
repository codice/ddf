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
package org.codice.ddf.security.saml.assertion.validator.impl;

import ddf.security.PropertiesLoader;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.saml.impl.SecurityAssertionSaml;
import ddf.security.service.SecurityServiceException;
import java.io.ByteArrayInputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.cxf.rs.security.saml.sso.SAMLProtocolResponseValidator;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.saml.WSSSAMLKeyInfoProcessor;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.codice.ddf.platform.filter.AuthenticationFailureException;
import org.codice.ddf.platform.util.XMLUtils;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.codice.ddf.security.saml.assertion.validator.SamlAssertionValidator;
import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SamlAssertionValidatorImpl implements SamlAssertionValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SamlAssertionValidatorImpl.class);

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  private static final ThreadLocal<DocumentBuilder> BUILDER =
      ThreadLocal.withInitial(
          () -> {
            try {
              return XML_UTILS.getSecureDocumentBuilder(true);
            } catch (ParserConfigurationException ex) {
              // This exception should not happen
              throw new IllegalArgumentException("Unable to create new DocumentBuilder", ex);
            }
          });

  private static SAMLObjectBuilder<Response> responseBuilder;

  private static SAMLObjectBuilder<Status> statusBuilder;

  private static SAMLObjectBuilder<StatusCode> statusCodeBuilder;

  private static SAMLObjectBuilder<StatusMessage> statusMessageBuilder;

  private static SAMLObjectBuilder<Issuer> issuerBuilder;

  private static XMLObjectBuilderFactory builderFactory =
      XMLObjectProviderRegistrySupport.getBuilderFactory();

  private Crypto signatureCrypto;

  private String signatureProperties;

  private Validator assertionValidator = new org.apache.wss4j.dom.validate.SamlAssertionValidator();

  /**
   * Validates a SAMLAuthenticationToken by checking it's signature against the configured system
   * certs.
   *
   * @param token token to validate
   * @throws AuthenticationFailureException thrown when the cert fails to validate
   */
  @Override
  public void validate(SAMLAuthenticationToken token) throws AuthenticationFailureException {
    try {
      LOGGER.debug("Validation received SAML Assertion");

      PrincipalCollection principalCollection = (PrincipalCollection) token.getCredentials();

      Collection<SecurityAssertion> securityAssertions =
          principalCollection.byType(SecurityAssertion.class);
      SecurityAssertion securityAssertion = null;
      for (SecurityAssertion assertion : securityAssertions) {
        if (SecurityAssertionSaml.SAML2_TOKEN_TYPE.equals(assertion.getTokenType())) {
          securityAssertion = assertion;
          break;
        }
      }
      if (securityAssertion == null) {
        throw new AuthenticationFailureException(
            "Unable to validate SAML token. Token is not SAML.");
      }
      SamlAssertionWrapper assertion =
          new SamlAssertionWrapper(((SecurityToken) securityAssertion.getToken()).getToken());

      // get the crypto junk
      Crypto crypto = getSignatureCrypto();
      Response samlResponse =
          createSamlResponse(
              token.getRequestURI(),
              assertion.getIssuerString(),
              createStatus(SAMLProtocolResponseValidator.SAML2_STATUSCODE_SUCCESS, null));

      BUILDER.get().reset();
      Document doc = BUILDER.get().newDocument();
      Element policyElement = OpenSAMLUtil.toDom(samlResponse, doc);
      doc.appendChild(policyElement);

      Credential credential = new Credential();
      credential.setSamlAssertion(assertion);

      RequestData requestData = new RequestData();
      requestData.setWsDocInfo(new WSDocInfo(samlResponse.getDOM().getOwnerDocument()));
      requestData.setSigVerCrypto(crypto);
      WSSConfig wssConfig = WSSConfig.getNewInstance();
      requestData.setWssConfig(wssConfig);

      X509Certificate[] x509Certs = token.getX509Certs();
      requestData.setTlsCerts(x509Certs);

      validateHolderOfKeyConfirmation(assertion, x509Certs);

      if (assertion.isSigned()) {
        // Verify the signature
        WSSSAMLKeyInfoProcessor wsssamlKeyInfoProcessor = new WSSSAMLKeyInfoProcessor(requestData);
        assertion.verifySignature(wsssamlKeyInfoProcessor, crypto);

        assertion.parseSubject(
            new WSSSAMLKeyInfoProcessor(requestData),
            requestData.getSigVerCrypto(),
            requestData.getCallbackHandler());
      }

      assertionValidator.validate(credential, requestData);

    } catch (SecurityServiceException e) {
      LOGGER.debug("Unable to get subject from SAML request.", e);
      throw new AuthenticationFailureException(e);
    } catch (WSSecurityException e) {
      LOGGER.debug("Unable to read/validate security token from request.", e);
      throw new AuthenticationFailureException(e);
    }
  }

  /**
   * Creates the SAML response that we use for validation against the CXF code.
   *
   * @param inResponseTo
   * @param issuer
   * @param status
   * @return Response
   */
  private static Response createSamlResponse(String inResponseTo, String issuer, Status status) {
    if (responseBuilder == null) {
      responseBuilder =
          (SAMLObjectBuilder<Response>) builderFactory.getBuilder(Response.DEFAULT_ELEMENT_NAME);
    }
    Response response = responseBuilder.buildObject();

    response.setID(UUID.randomUUID().toString());
    response.setIssueInstant(new DateTime());
    response.setInResponseTo(inResponseTo);
    response.setIssuer(createIssuer(issuer));
    response.setStatus(status);
    response.setVersion(SAMLVersion.VERSION_20);

    return response;
  }

  /**
   * Creates the status object for the response.
   *
   * @param statusCodeValue
   * @param statusMessage
   * @return Status
   */
  private static Status createStatus(String statusCodeValue, String statusMessage) {
    if (statusBuilder == null) {
      statusBuilder =
          (SAMLObjectBuilder<Status>) builderFactory.getBuilder(Status.DEFAULT_ELEMENT_NAME);
    }
    if (statusCodeBuilder == null) {
      statusCodeBuilder =
          (SAMLObjectBuilder<StatusCode>)
              builderFactory.getBuilder(StatusCode.DEFAULT_ELEMENT_NAME);
    }
    if (statusMessageBuilder == null) {
      statusMessageBuilder =
          (SAMLObjectBuilder<StatusMessage>)
              builderFactory.getBuilder(StatusMessage.DEFAULT_ELEMENT_NAME);
    }

    Status status = statusBuilder.buildObject();

    StatusCode statusCode = statusCodeBuilder.buildObject();
    statusCode.setValue(statusCodeValue);
    status.setStatusCode(statusCode);

    if (statusMessage != null) {
      StatusMessage statusMessageObject = statusMessageBuilder.buildObject();
      statusMessageObject.setMessage(statusMessage);
      status.setStatusMessage(statusMessageObject);
    }

    return status;
  }

  /**
   * Creates the issuer object for the response.
   *
   * @param issuerValue
   * @return Issuer
   */
  private static Issuer createIssuer(String issuerValue) {
    if (issuerBuilder == null) {
      issuerBuilder =
          (SAMLObjectBuilder<Issuer>) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
    }
    Issuer issuer = issuerBuilder.buildObject();
    issuer.setValue(issuerValue);

    return issuer;
  }

  /**
   * Returns a Crypto object initialized against the system signature properties.
   *
   * @return Crypto
   */
  private Crypto getSignatureCrypto() {
    if (signatureCrypto == null && signatureProperties != null) {
      Properties sigProperties = PropertiesLoader.loadProperties(signatureProperties);
      if (sigProperties == null) {
        LOGGER.trace("Cannot load signature properties using: {}", signatureProperties);
        return null;
      }
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      Thread.currentThread()
          .setContextClassLoader(SamlAssertionValidatorImpl.class.getClassLoader());
      try {
        signatureCrypto = CryptoFactory.getInstance(sigProperties);
      } catch (WSSecurityException ex) {
        LOGGER.trace("Error in loading the signature Crypto object.", ex);
        return null;
      } finally {
        Thread.currentThread().setContextClassLoader(contextClassLoader);
      }
    }
    return signatureCrypto;
  }

  private void validateHolderOfKeyConfirmation(
      SamlAssertionWrapper assertion, X509Certificate[] x509Certs) throws SecurityServiceException {
    List<String> confirmationMethods = assertion.getConfirmationMethods();
    boolean hasHokMethod = false;
    for (String method : confirmationMethods) {
      if (OpenSAMLUtil.isMethodHolderOfKey(method)) {
        hasHokMethod = true;
      }
    }

    if (hasHokMethod) {
      if (x509Certs != null && x509Certs.length > 0) {
        List<SubjectConfirmation> subjectConfirmations =
            assertion.getSaml2().getSubject().getSubjectConfirmations();
        for (SubjectConfirmation subjectConfirmation : subjectConfirmations) {
          if (OpenSAMLUtil.isMethodHolderOfKey(subjectConfirmation.getMethod())) {
            Element dom = subjectConfirmation.getSubjectConfirmationData().getDOM();
            Node keyInfo = dom.getFirstChild();
            Node x509Data = keyInfo.getFirstChild();
            Node dataNode = x509Data.getFirstChild();
            Node dataText = dataNode.getFirstChild();

            X509Certificate tlsCertificate = x509Certs[0];
            if (dataNode.getLocalName().equals("X509Certificate")) {
              String textContent = dataText.getTextContent();
              byte[] byteValue = Base64.getMimeDecoder().decode(textContent);
              try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert =
                    (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(byteValue));
                // check that the certificate is still valid
                cert.checkValidity();

                // HoK spec section 2.5:
                // relying party MUST ensure that the certificate bound to the assertion matches the
                // X.509 certificate in its possession.
                // Matching is done by comparing the base64-decoded certificates, or the hash values
                // of the base64-decoded certificates, byte-for-byte.
                // if the certs aren't the same, verify
                if (!tlsCertificate.equals(cert)) {
                  // verify that the cert was signed by the same private key as the TLS cert
                  cert.verify(tlsCertificate.getPublicKey());
                }
              } catch (CertificateException
                  | NoSuchAlgorithmException
                  | InvalidKeyException
                  | SignatureException
                  | NoSuchProviderException e) {
                throw new SecurityServiceException(
                    "Unable to validate Holder of Key assertion with certificate.");
              }

            } else if (dataNode.getLocalName().equals("X509SubjectName")) {
              String textContent = dataText.getTextContent();
              // HoK spec section 2.5:
              // relying party MUST ensure that the subject distinguished name (DN) bound to the
              // assertion matches the DN bound to the X.509 certificate.
              // If, however, the relying party does not trust the certificate issuer to issue such
              // a DN, the attesting entity is not confirmed and the relying party SHOULD disregard
              // the assertion.
              if (!tlsCertificate.getSubjectDN().getName().equals(textContent)) {
                throw new SecurityServiceException(
                    "Unable to validate Holder of Key assertion with subject DN.");
              }

            } else if (dataNode.getLocalName().equals("X509IssuerSerial")) {
              // we have no way to support this confirmation type so we have to throw an error
              throw new SecurityServiceException(
                  "Unable to validate Holder of Key assertion with issuer serial. NOT SUPPORTED");
            } else if (dataNode.getLocalName().equals("X509SKI")) {
              String textContent = dataText.getTextContent();
              byte[] tlsSKI = tlsCertificate.getExtensionValue("2.5.29.14");
              byte[] assertionSKI = Base64.getMimeDecoder().decode(textContent);
              if (tlsSKI != null && tlsSKI.length > 0) {
                ASN1OctetString tlsOs = ASN1OctetString.getInstance(tlsSKI);
                ASN1OctetString assertionOs = ASN1OctetString.getInstance(assertionSKI);
                SubjectKeyIdentifier tlsSubjectKeyIdentifier =
                    SubjectKeyIdentifier.getInstance(tlsOs.getOctets());
                SubjectKeyIdentifier assertSubjectKeyIdentifier =
                    SubjectKeyIdentifier.getInstance(assertionOs.getOctets());
                // HoK spec section 2.5:
                // relying party MUST ensure that the value bound to the assertion matches the
                // Subject Key Identifier (SKI) extension bound to the X.509 certificate.
                // Matching is done by comparing the base64-decoded SKI values byte-for-byte. If the
                // X.509 certificate does not contain an SKI extension,
                // the attesting entity is not confirmed and the relying party SHOULD disregard the
                // assertion.
                if (!Arrays.equals(
                    tlsSubjectKeyIdentifier.getKeyIdentifier(),
                    assertSubjectKeyIdentifier.getKeyIdentifier())) {
                  throw new SecurityServiceException(
                      "Unable to validate Holder of Key assertion with subject key identifier.");
                }
              } else {
                throw new SecurityServiceException(
                    "Unable to validate Holder of Key assertion with subject key identifier.");
              }
            }
          }
        }
      } else {
        throw new SecurityServiceException("Holder of Key assertion, must be used with 2-way TLS.");
      }
    }
  }

  @Override
  public void setSignatureProperties(String signatureProperties) {
    this.signatureProperties = signatureProperties;
  }

  @Override
  public String getSignatureProperties(String signatureProperties) {
    return signatureProperties;
  }
}
