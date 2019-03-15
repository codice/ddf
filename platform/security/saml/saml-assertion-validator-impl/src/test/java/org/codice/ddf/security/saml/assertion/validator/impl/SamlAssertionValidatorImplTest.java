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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.assertion.saml.impl.SecurityAssertionSaml;
import ddf.security.encryption.EncryptionService;
import ddf.security.samlp.SystemCrypto;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.bouncycastle.util.encoders.Base64;
import org.codice.ddf.platform.filter.AuthenticationFailureException;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.codice.ddf.security.util.SAMLUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.KeyInfoConfirmationDataType;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.impl.AssertionBuilder;
import org.opensaml.saml.saml2.core.impl.AttributeBuilder;
import org.opensaml.saml.saml2.core.impl.AttributeStatementBuilder;
import org.opensaml.saml.saml2.core.impl.AuthnContextBuilder;
import org.opensaml.saml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml.saml2.core.impl.AuthnStatementBuilder;
import org.opensaml.saml.saml2.core.impl.ConditionsBuilder;
import org.opensaml.saml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml.saml2.core.impl.KeyInfoConfirmationDataTypeBuilder;
import org.opensaml.saml.saml2.core.impl.NameIDBuilder;
import org.opensaml.saml.saml2.core.impl.SubjectBuilder;
import org.opensaml.saml.saml2.core.impl.SubjectConfirmationBuilder;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xacml.ctx.AttributeValueType;
import org.opensaml.xacml.ctx.impl.AttributeValueTypeImplBuilder;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.X509SubjectName;
import org.opensaml.xmlsec.signature.impl.KeyInfoBuilder;
import org.opensaml.xmlsec.signature.impl.X509CertificateBuilder;
import org.opensaml.xmlsec.signature.impl.X509DataBuilder;
import org.opensaml.xmlsec.signature.impl.X509SubjectNameBuilder;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SamlAssertionValidatorImplTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final String ISSUER = "localhost";

  private X509Certificate certificate;

  private PrivateKey privateKey;

  private SamlAssertionValidatorImpl samlAssertionValidator;

  static {
    OpenSAMLUtil.initSamlEngine();
  }

  @Before
  public void setUp() throws Exception {
    File signatureFile = temporaryFolder.newFile("signature.properties");
    File encryptionFile = temporaryFolder.newFile("encryption.properties");
    File jksFile = temporaryFolder.newFile("serverKeystore.jks");

    try (FileOutputStream outputStream = new FileOutputStream(signatureFile);
        InputStream inputStream = getClass().getResourceAsStream("/signature.properties")) {
      IOUtils.copy(inputStream, outputStream);
    }

    try (FileOutputStream outputStream = new FileOutputStream(encryptionFile);
        InputStream inputStream = getClass().getResourceAsStream("/encryption.properties")) {
      IOUtils.copy(inputStream, outputStream);
    }

    try (FileOutputStream outputStream = new FileOutputStream(jksFile);
        InputStream inputStream = getClass().getResourceAsStream("/serverKeystore.jks")) {
      IOUtils.copy(inputStream, outputStream);
    }

    System.setProperty("javax.net.ssl.keyStore", jksFile.getAbsolutePath());
    System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
    System.setProperty("org.codice.ddf.system.hostname", "localhost");

    EncryptionService encryptionService = mock(EncryptionService.class);
    when(encryptionService.decrypt(anyString())).thenReturn("changeit");
    when(encryptionService.encrypt(anyString())).thenReturn("changeit");

    SystemCrypto crypto =
        new SystemCrypto(
            signatureFile.getAbsolutePath(), encryptionFile.getAbsolutePath(), encryptionService);

    CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
    cryptoType.setAlias(crypto.getSignatureAlias());
    certificate = crypto.getSignatureCrypto().getX509Certificates(cryptoType)[0];

    privateKey =
        crypto
            .getSignatureCrypto()
            .getPrivateKey(crypto.getSignatureAlias(), crypto.getSignaturePassword());

    samlAssertionValidator = new SamlAssertionValidatorImpl();
    samlAssertionValidator.setSignatureProperties(signatureFile.getAbsolutePath());
  }

  @Test
  public void testValidateBearerAssertion() throws Exception {
    Assertion assertion = createAssertion(true, true, ISSUER, new DateTime().plusDays(3));

    SecurityToken securityToken =
        SAMLUtils.getInstance().getSecurityTokenFromSAMLAssertion(samlObjectToString(assertion));
    SimplePrincipalCollection simplePrincipalCollection = new SimplePrincipalCollection();
    simplePrincipalCollection.add(new SecurityAssertionSaml(securityToken), "default");
    SAMLAuthenticationToken samlAuthenticationToken =
        new SAMLAuthenticationToken(
            simplePrincipalCollection, simplePrincipalCollection, "127.0.0.1");

    X509Certificate[] certs = {certificate};
    samlAuthenticationToken.setX509Certs(certs);

    samlAssertionValidator.validate(samlAuthenticationToken);
  }

  @Test
  public void testValidateWithHolderOfKeyAssertion() throws Exception {
    Assertion assertion = createHolderOfKeyAssertion();

    SecurityToken securityToken =
        SAMLUtils.getInstance().getSecurityTokenFromSAMLAssertion(samlObjectToString(assertion));
    SimplePrincipalCollection simplePrincipalCollection = new SimplePrincipalCollection();
    simplePrincipalCollection.add(new SecurityAssertionSaml(securityToken), "default");
    SAMLAuthenticationToken samlAuthenticationToken =
        new SAMLAuthenticationToken(
            simplePrincipalCollection, simplePrincipalCollection, "127.0.0.1");

    X509Certificate[] certs = {certificate};
    samlAuthenticationToken.setX509Certs(certs);

    samlAssertionValidator.validate(samlAuthenticationToken);
  }

  @Test(expected = AuthenticationFailureException.class)
  public void testValidateUnsignedAssertion() throws Exception {
    Assertion assertion = createAssertion(false, true, ISSUER, new DateTime().plusDays(3));

    SecurityToken securityToken =
        SAMLUtils.getInstance().getSecurityTokenFromSAMLAssertion(samlObjectToString(assertion));
    SimplePrincipalCollection simplePrincipalCollection = new SimplePrincipalCollection();
    simplePrincipalCollection.add(new SecurityAssertionSaml(securityToken), "default");
    SAMLAuthenticationToken samlAuthenticationToken =
        new SAMLAuthenticationToken(
            simplePrincipalCollection, simplePrincipalCollection, "127.0.0.1");

    samlAssertionValidator.validate(samlAuthenticationToken);
  }

  @Test(expected = AuthenticationFailureException.class)
  public void testValidateIncorrectSamlVersion() throws Exception {
    org.opensaml.saml.saml1.core.Assertion assertion =
        new org.opensaml.saml.saml1.core.impl.AssertionBuilder().buildObject();

    SecurityToken securityToken =
        SAMLUtils.getInstance().getSecurityTokenFromSAMLAssertion(samlObjectToString(assertion));
    SimplePrincipalCollection simplePrincipalCollection = new SimplePrincipalCollection();
    simplePrincipalCollection.add(new SecurityAssertionSaml(securityToken), "default");
    SAMLAuthenticationToken samlAuthenticationToken =
        new SAMLAuthenticationToken(
            simplePrincipalCollection, simplePrincipalCollection, "127.0.0.1");

    samlAssertionValidator.validate(samlAuthenticationToken);
  }

  @Test(expected = AuthenticationFailureException.class)
  public void testValidateExpiredAssertion() throws Exception {
    Assertion assertion = createAssertion(false, true, ISSUER, new DateTime().minusSeconds(10));

    SecurityToken securityToken =
        SAMLUtils.getInstance().getSecurityTokenFromSAMLAssertion(samlObjectToString(assertion));
    SimplePrincipalCollection simplePrincipalCollection = new SimplePrincipalCollection();
    simplePrincipalCollection.add(new SecurityAssertionSaml(securityToken), "default");
    SAMLAuthenticationToken samlAuthenticationToken =
        new SAMLAuthenticationToken(
            simplePrincipalCollection, simplePrincipalCollection, "127.0.0.1");

    samlAssertionValidator.validate(samlAuthenticationToken);
  }

  @Test(expected = AuthenticationFailureException.class)
  public void testValidateInvalidIssuer() throws Exception {
    Assertion assertion = createAssertion(false, true, "WRONG", new DateTime().minusSeconds(10));

    SecurityToken securityToken =
        SAMLUtils.getInstance().getSecurityTokenFromSAMLAssertion(samlObjectToString(assertion));
    SimplePrincipalCollection simplePrincipalCollection = new SimplePrincipalCollection();
    simplePrincipalCollection.add(new SecurityAssertionSaml(securityToken), "default");
    SAMLAuthenticationToken samlAuthenticationToken =
        new SAMLAuthenticationToken(
            simplePrincipalCollection, simplePrincipalCollection, "127.0.0.1");

    samlAssertionValidator.validate(samlAuthenticationToken);
  }

  @Test(expected = AuthenticationFailureException.class)
  public void testValidateInvalidSignature() throws Exception {
    Assertion assertion = createAssertion(false, false, "WRONG", new DateTime().minusSeconds(10));

    SecurityToken securityToken =
        SAMLUtils.getInstance().getSecurityTokenFromSAMLAssertion(samlObjectToString(assertion));
    SimplePrincipalCollection simplePrincipalCollection = new SimplePrincipalCollection();
    simplePrincipalCollection.add(new SecurityAssertionSaml(securityToken), "default");
    SAMLAuthenticationToken samlAuthenticationToken =
        new SAMLAuthenticationToken(
            simplePrincipalCollection, simplePrincipalCollection, "127.0.0.1");

    samlAssertionValidator.validate(samlAuthenticationToken);
  }

  private Assertion createAssertion(
      boolean sign, boolean validSignature, String issuerString, DateTime notOnOrAfter)
      throws Exception {
    Assertion assertion = new AssertionBuilder().buildObject();
    assertion.setID(UUID.randomUUID().toString());
    assertion.setIssueInstant(new DateTime());

    Issuer issuer = new IssuerBuilder().buildObject();
    issuer.setValue(issuerString);
    assertion.setIssuer(issuer);

    NameID nameID = new NameIDBuilder().buildObject();
    nameID.setFormat("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
    nameID.setNameQualifier("http://cxf.apache.org/sts");
    nameID.setValue("admin");

    SubjectConfirmation subjectConfirmation = new SubjectConfirmationBuilder().buildObject();
    subjectConfirmation.setMethod("urn:oasis:names:tc:SAML:2.0:cm:bearer");

    Subject subject = new SubjectBuilder().buildObject();
    subject.setNameID(nameID);
    subject.getSubjectConfirmations().add(subjectConfirmation);
    assertion.setSubject(subject);

    Conditions conditions = new ConditionsBuilder().buildObject();
    conditions.setNotBefore(new DateTime().minusDays(3));
    conditions.setNotOnOrAfter(notOnOrAfter);
    assertion.setConditions(conditions);

    AuthnStatement authnStatement = new AuthnStatementBuilder().buildObject();
    authnStatement.setAuthnInstant(new DateTime());
    AuthnContext authnContext = new AuthnContextBuilder().buildObject();
    AuthnContextClassRef authnContextClassRef = new AuthnContextClassRefBuilder().buildObject();
    authnContextClassRef.setAuthnContextClassRef(
        "urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified");
    authnContext.setAuthnContextClassRef(authnContextClassRef);
    authnStatement.setAuthnContext(authnContext);
    assertion.getAuthnStatements().add(authnStatement);

    AttributeStatement attributeStatement = new AttributeStatementBuilder().buildObject();
    Attribute attribute = new AttributeBuilder().buildObject();
    AttributeValueType attributeValue = new AttributeValueTypeImplBuilder().buildObject();
    attributeValue.setValue("admin");
    attribute.setName("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role");
    attribute.setNameFormat("urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified");
    attribute.getAttributeValues().add(attributeValue);
    attributeStatement.getAttributes().add(attribute);
    assertion.getAttributeStatements().add(attributeStatement);

    if (sign) {
      Signature signature = OpenSAMLUtil.buildSignature();
      signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
      signature.setSignatureAlgorithm(WSS4JConstants.RSA);

      BasicX509Credential signingCredential;
      if (validSignature) {
        signingCredential = new BasicX509Credential(certificate);
        signingCredential.setPrivateKey(privateKey);
        signature.setSigningCredential(signingCredential);
      } else {
        try (InputStream inputStream = getClass().getResourceAsStream("/localhost.crt")) {
          CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
          X509Certificate cert =
              (X509Certificate) certificateFactory.generateCertificate(inputStream);
          signingCredential = new BasicX509Credential(cert);
          signature.setSigningCredential(signingCredential);
        }
      }

      X509KeyInfoGeneratorFactory x509KeyInfoGeneratorFactory = new X509KeyInfoGeneratorFactory();
      x509KeyInfoGeneratorFactory.setEmitEntityCertificate(true);

      KeyInfo keyInfo = x509KeyInfoGeneratorFactory.newInstance().generate(signingCredential);
      signature.setKeyInfo(keyInfo);

      assertion.setSignature(signature);
    }

    return assertion;
  }

  private Assertion createHolderOfKeyAssertion() throws Exception {
    Assertion assertion = new AssertionBuilder().buildObject();
    assertion.setID(UUID.randomUUID().toString());
    assertion.setIssueInstant(new DateTime());

    Issuer issuer = new IssuerBuilder().buildObject();
    issuer.setValue(ISSUER);
    assertion.setIssuer(issuer);

    NameID nameID = new NameIDBuilder().buildObject();
    nameID.setFormat("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
    nameID.setNameQualifier("http://cxf.apache.org/sts");
    nameID.setValue("admin");

    X509SubjectName x509SubjectName = new X509SubjectNameBuilder().buildObject();
    x509SubjectName.setValue(
        "EMAILADDRESS=localhost@example.org, CN=localhost, OU=Dev, O=DDF, ST=AZ, C=US");

    org.opensaml.xmlsec.signature.X509Certificate x509Certificate =
        new X509CertificateBuilder().buildObject();
    byte[] certBytes = certificate.getEncoded();
    String certString = new String(Base64.encode(certBytes));
    x509Certificate.setValue(certString);

    X509Data x509Data = new X509DataBuilder().buildObject();
    x509Data.getX509SubjectNames().add(x509SubjectName);
    x509Data.getX509Certificates().add(x509Certificate);

    KeyInfo keyInfo = new KeyInfoBuilder().buildObject();
    keyInfo.getX509Datas().add(x509Data);

    KeyInfoConfirmationDataType keyInfoConfirmationDataType =
        new KeyInfoConfirmationDataTypeBuilder().buildObject();
    keyInfoConfirmationDataType.getKeyInfos().add(keyInfo);

    SubjectConfirmation subjectConfirmation = new SubjectConfirmationBuilder().buildObject();
    subjectConfirmation.setMethod("urn:oasis:names:tc:SAML:2.0:cm:holder-of-key");
    subjectConfirmation.setSubjectConfirmationData(keyInfoConfirmationDataType);

    Subject subject = new SubjectBuilder().buildObject();
    subject.setNameID(nameID);
    subject.getSubjectConfirmations().add(subjectConfirmation);
    assertion.setSubject(subject);

    Conditions conditions = new ConditionsBuilder().buildObject();
    conditions.setNotBefore(new DateTime().minusDays(3));
    conditions.setNotOnOrAfter(new DateTime().plusDays(3));
    assertion.setConditions(conditions);

    AuthnStatement authnStatement = new AuthnStatementBuilder().buildObject();
    authnStatement.setAuthnInstant(new DateTime());
    AuthnContext authnContext = new AuthnContextBuilder().buildObject();
    AuthnContextClassRef authnContextClassRef = new AuthnContextClassRefBuilder().buildObject();
    authnContextClassRef.setAuthnContextClassRef(
        "urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified");
    authnContext.setAuthnContextClassRef(authnContextClassRef);
    authnStatement.setAuthnContext(authnContext);
    assertion.getAuthnStatements().add(authnStatement);

    AttributeStatement attributeStatement = new AttributeStatementBuilder().buildObject();
    Attribute attribute = new AttributeBuilder().buildObject();
    AttributeValueType attributeValue = new AttributeValueTypeImplBuilder().buildObject();
    attributeValue.setValue("admin");
    attribute.setName("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role");
    attribute.setNameFormat("urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified");
    attribute.getAttributeValues().add(attributeValue);
    attributeStatement.getAttributes().add(attribute);
    assertion.getAttributeStatements().add(attributeStatement);

    Signature signature = OpenSAMLUtil.buildSignature();
    signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
    signature.setSignatureAlgorithm(WSS4JConstants.RSA);

    BasicX509Credential signingCredential;

    signingCredential = new BasicX509Credential(certificate);
    signingCredential.setPrivateKey(privateKey);
    signature.setSigningCredential(signingCredential);

    X509KeyInfoGeneratorFactory x509KeyInfoGeneratorFactory = new X509KeyInfoGeneratorFactory();
    x509KeyInfoGeneratorFactory.setEmitEntityCertificate(true);

    KeyInfo signatureKeyInfo =
        x509KeyInfoGeneratorFactory.newInstance().generate(signingCredential);
    signature.setKeyInfo(signatureKeyInfo);

    assertion.setSignature(signature);

    return assertion;
  }

  private String samlObjectToString(XMLObject samlObject) throws Exception {
    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));

    Element samlElement = OpenSAMLUtil.toDom(samlObject, doc);
    return DOM2Writer.nodeToString(samlElement);
  }
}
