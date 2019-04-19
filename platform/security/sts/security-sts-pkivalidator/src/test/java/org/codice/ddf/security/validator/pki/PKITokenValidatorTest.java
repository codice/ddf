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
package org.codice.ddf.security.validator.pki;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.PropertiesLoader;
import ddf.security.SecurityConstants;
import ddf.security.SubjectUtils;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.dom.WSConstants;
import org.codice.ddf.security.handler.api.PKIAuthenticationToken;
import org.codice.ddf.security.handler.api.PKIAuthenticationTokenFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PKITokenValidatorTest {

  private static Properties properties;

  private PKITokenValidator pkiTokenValidator;

  private X509Certificate[] certificates;

  private X509Certificate[] badCertificates;

  private X509Certificate[] userCertificates;

  private Merlin merlin;

  @BeforeClass
  public static void setUpBeforeClass() {
    properties = System.getProperties();
    System.setProperty(SecurityConstants.KEYSTORE_TYPE, "jks");
  }

  @AfterClass
  public static void tearDownAfterClass() {
    System.setProperties(properties);
  }

  @Before
  public void setup() {
    pkiTokenValidator = new PKITokenValidator();
    pkiTokenValidator.setSignaturePropertiesPath(
        PKITokenValidatorTest.class.getResource("/signature.properties").getPath());
    pkiTokenValidator.init();

    try {
      KeyStore trustStore = KeyStore.getInstance(System.getProperty("javax.net.ssl.keyStoreType"));
      InputStream trustFIS = PKITokenValidatorTest.class.getResourceAsStream("/serverKeystore.jks");
      try {
        trustStore.load(trustFIS, "changeit".toCharArray());
      } catch (CertificateException e) {
        fail(e.getMessage());
      } finally {
        IOUtils.closeQuietly(trustFIS);
      }
      Certificate[] certs = trustStore.getCertificateChain("localhost");
      certificates = new X509Certificate[certs.length];
      for (int i = 0; i < certs.length; i++) {
        certificates[i] = (X509Certificate) certs[i];
      }

      trustStore = KeyStore.getInstance(System.getProperty(SecurityConstants.KEYSTORE_TYPE));
      trustFIS = PKITokenValidatorTest.class.getResourceAsStream("/badKeystore.jks");
      try {
        trustStore.load(trustFIS, "changeit".toCharArray());
      } catch (CertificateException e) {
        fail(e.getMessage());
      } finally {
        IOUtils.closeQuietly(trustFIS);
      }
      certs = trustStore.getCertificateChain("badhost");
      badCertificates = new X509Certificate[certs.length];
      for (int i = 0; i < certs.length; i++) {
        badCertificates[i] = (X509Certificate) certs[i];
      }
      merlin =
          new Merlin(
              PropertiesLoader.loadProperties(
                  PKITokenValidatorTest.class.getResource("/signature.properties").getPath()),
              PKITokenValidator.class.getClassLoader(),
              null);

      KeyStore keystore = KeyStore.getInstance(System.getProperty(SecurityConstants.KEYSTORE_TYPE));
      try (InputStream keystoreIS =
          PKITokenValidatorTest.class.getResourceAsStream("/test-user.jks")) {
        keystore.load(keystoreIS, "changeit".toCharArray());
      }
      Certificate cert = keystore.getCertificate("test");
      userCertificates = new X509Certificate[] {(X509Certificate) cert};

    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testCanHandleToken() {
    BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
    binarySecurityTokenType.setEncodingType(WSConstants.SOAPMESSAGE_NS + "#Base64Binary");
    binarySecurityTokenType.setValueType(PKIAuthenticationToken.PKI_TOKEN_VALUE_TYPE);
    PKIAuthenticationTokenFactory pkiAuthenticationTokenFactory =
        new PKIAuthenticationTokenFactory();
    pkiAuthenticationTokenFactory.setSignaturePropertiesPath(
        PKITokenValidatorTest.class.getResource("/signature.properties").getPath());
    pkiAuthenticationTokenFactory.init();
    PKIAuthenticationToken pkiAuthenticationToken =
        pkiAuthenticationTokenFactory.getTokenFromCerts(certificates);
    binarySecurityTokenType.setValue(pkiAuthenticationToken.getEncodedCredentials());
    ReceivedToken receivedToken = mock(ReceivedToken.class);
    when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);

    boolean result = pkiTokenValidator.canHandleToken(receivedToken);
    assertEquals(true, result);
  }

  @Test
  public void testCanNotHandleToken() {
    BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
    binarySecurityTokenType.setEncodingType(WSConstants.SOAPMESSAGE_NS + "#Base64Binary");
    binarySecurityTokenType.setValueType("randomvaluetype");
    PKIAuthenticationTokenFactory pkiAuthenticationTokenFactory =
        new PKIAuthenticationTokenFactory();
    pkiAuthenticationTokenFactory.setSignaturePropertiesPath(
        PKITokenValidatorTest.class.getResource("/signature.properties").getPath());
    pkiAuthenticationTokenFactory.init();
    PKIAuthenticationToken pkiAuthenticationToken =
        pkiAuthenticationTokenFactory.getTokenFromCerts(certificates);
    binarySecurityTokenType.setValue(pkiAuthenticationToken.getEncodedCredentials());
    ReceivedToken receivedToken = mock(ReceivedToken.class);
    when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);

    boolean result = pkiTokenValidator.canHandleToken(receivedToken);
    assertEquals(false, result);
  }

  @Test
  public void testValidateToken() {
    BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
    binarySecurityTokenType.setEncodingType(WSConstants.SOAPMESSAGE_NS + "#Base64Binary");
    binarySecurityTokenType.setValueType(PKIAuthenticationToken.PKI_TOKEN_VALUE_TYPE);
    PKIAuthenticationTokenFactory pkiAuthenticationTokenFactory =
        new PKIAuthenticationTokenFactory();
    pkiAuthenticationTokenFactory.setSignaturePropertiesPath(
        PKITokenValidatorTest.class.getResource("/signature.properties").getPath());
    pkiAuthenticationTokenFactory.init();
    PKIAuthenticationToken pkiAuthenticationToken =
        pkiAuthenticationTokenFactory.getTokenFromCerts(certificates);
    binarySecurityTokenType.setValue(pkiAuthenticationToken.getEncodedCredentials());
    ReceivedToken receivedToken = mock(ReceivedToken.class);
    when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);
    TokenValidatorParameters tokenValidatorParameters = mock(TokenValidatorParameters.class);
    STSPropertiesMBean stsPropertiesMBean = mock(STSPropertiesMBean.class);
    when(stsPropertiesMBean.getSignatureCrypto()).thenReturn(merlin);
    when(tokenValidatorParameters.getStsProperties()).thenReturn(stsPropertiesMBean);
    when(tokenValidatorParameters.getToken()).thenReturn(receivedToken);
    doCallRealMethod().when(receivedToken).setState(any(ReceivedToken.STATE.class));
    doCallRealMethod().when(receivedToken).getState();

    TokenValidatorResponse tokenValidatorResponse =
        pkiTokenValidator.validateToken(tokenValidatorParameters);
    assertEquals(ReceivedToken.STATE.VALID, tokenValidatorResponse.getToken().getState());

    assertEquals(
        "US", tokenValidatorResponse.getAdditionalProperties().get(SubjectUtils.COUNTRY_CLAIM_URI));
    verifyEmail(tokenValidatorResponse, "localhost@example.org");
  }

  @Test
  public void testValidateSubjectAlternativeNameEmailToken() throws Exception {
    BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
    binarySecurityTokenType.setEncodingType(WSConstants.SOAPMESSAGE_NS + "#Base64Binary");
    binarySecurityTokenType.setValueType(PKIAuthenticationToken.PKI_TOKEN_VALUE_TYPE);
    PKIAuthenticationTokenFactory pkiAuthenticationTokenFactory =
        new PKIAuthenticationTokenFactory();
    pkiAuthenticationTokenFactory.init();
    PKIAuthenticationToken pkiAuthenticationToken =
        pkiAuthenticationTokenFactory.getTokenFromCerts(userCertificates);
    binarySecurityTokenType.setValue(pkiAuthenticationToken.getEncodedCredentials());
    ReceivedToken receivedToken = mock(ReceivedToken.class);
    when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);
    TokenValidatorParameters tokenValidatorParameters = mock(TokenValidatorParameters.class);
    STSPropertiesMBean stsPropertiesMBean = mock(STSPropertiesMBean.class);
    when(stsPropertiesMBean.getSignatureCrypto()).thenReturn(merlin);
    when(tokenValidatorParameters.getStsProperties()).thenReturn(stsPropertiesMBean);
    when(tokenValidatorParameters.getToken()).thenReturn(receivedToken);
    doCallRealMethod().when(receivedToken).setState(any(ReceivedToken.STATE.class));
    doCallRealMethod().when(receivedToken).getState();

    TokenValidatorResponse tokenValidatorResponse =
        pkiTokenValidator.validateToken(tokenValidatorParameters);
    assertEquals(ReceivedToken.STATE.VALID, tokenValidatorResponse.getToken().getState());

    assertEquals(
        "US", tokenValidatorResponse.getAdditionalProperties().get(SubjectUtils.COUNTRY_CLAIM_URI));

    verifyEmail(tokenValidatorResponse, "test2@test");
  }

  @Test
  public void testNoValidateToken() {
    BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
    binarySecurityTokenType.setEncodingType(WSConstants.SOAPMESSAGE_NS + "#Base64Binary");
    binarySecurityTokenType.setValueType(PKIAuthenticationToken.PKI_TOKEN_VALUE_TYPE);
    PKIAuthenticationTokenFactory pkiAuthenticationTokenFactory =
        new PKIAuthenticationTokenFactory();
    pkiAuthenticationTokenFactory.setSignaturePropertiesPath(
        PKITokenValidatorTest.class.getResource("/badSignature.properties").getPath());
    pkiAuthenticationTokenFactory.init();
    PKIAuthenticationToken pkiAuthenticationToken =
        pkiAuthenticationTokenFactory.getTokenFromCerts(badCertificates);
    binarySecurityTokenType.setValue(pkiAuthenticationToken.getEncodedCredentials());
    ReceivedToken receivedToken = mock(ReceivedToken.class);
    when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);
    TokenValidatorParameters tokenValidatorParameters = mock(TokenValidatorParameters.class);
    STSPropertiesMBean stsPropertiesMBean = mock(STSPropertiesMBean.class);
    when(stsPropertiesMBean.getSignatureCrypto()).thenReturn(merlin);
    when(tokenValidatorParameters.getStsProperties()).thenReturn(stsPropertiesMBean);
    when(tokenValidatorParameters.getToken()).thenReturn(receivedToken);
    doCallRealMethod().when(receivedToken).setState(any(ReceivedToken.STATE.class));
    doCallRealMethod().when(receivedToken).getState();

    TokenValidatorResponse tokenValidatorResponse =
        pkiTokenValidator.validateToken(tokenValidatorParameters);
    assertEquals(ReceivedToken.STATE.INVALID, tokenValidatorResponse.getToken().getState());
  }

  private void verifyEmail(TokenValidatorResponse tokenValidatorResponse, String emailAddress) {
    List<String> emailAddresses =
        (List<String>)
            tokenValidatorResponse
                .getAdditionalProperties()
                .get(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI);
    assertThat(emailAddresses, hasItem(emailAddress));
  }
}
