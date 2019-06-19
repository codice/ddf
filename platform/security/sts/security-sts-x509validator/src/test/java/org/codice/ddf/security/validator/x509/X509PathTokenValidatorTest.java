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
package org.codice.ddf.security.validator.x509;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.SubjectUtils;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.sts.token.validator.X509TokenValidator;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;
import org.junit.Before;
import org.junit.Test;

public class X509PathTokenValidatorTest {

  private X509PathTokenValidator x509PathTokenValidator;

  private Validator validator;

  @Before
  public void setUp() {
    x509PathTokenValidator = new X509PathTokenValidator();
    x509PathTokenValidator.merlin = mock(Merlin.class);
    try {
      X509Certificate mockCert = mock(X509Certificate.class);
      X509Certificate[] x509Certificates = new X509Certificate[] {mockCert};
      when(x509PathTokenValidator.merlin.getCertificatesFromBytes(any(byte[].class)))
          .thenReturn(x509Certificates);

      when(x509PathTokenValidator.merlin.loadCertificate(any(InputStream.class)))
          .thenReturn(mockCert);
    } catch (WSSecurityException e) {
      // ignore
    }
    validator = mock(Validator.class);

    System.setProperty(
        SecurityConstants.TRUSTSTORE_PATH, Paths.get("/serverTruststore.jks").toString());
    System.setProperty(SecurityConstants.TRUSTSTORE_PASSWORD, "changeit");
  }

  @Test
  public void testValidateGoodPath() {
    goodToken(X509PathTokenValidator.X509_PKI_PATH);
  }

  @Test
  public void testValidateGoodToken() {
    goodToken(X509TokenValidator.X509_V3_TYPE);
  }

  private void goodToken(String type) {

    try {
      Credential credential = mock(Credential.class);
      X509Certificate x509Certificate = mock(X509Certificate.class);
      X500Principal x500Principal = new X500Principal("cn=myxman,ou=someunit,o=someorg");
      when(x509Certificate.getSubjectX500Principal()).thenReturn(x500Principal);
      X509Certificate[] x509Certificates = new X509Certificate[] {x509Certificate};
      when(credential.getCertificates()).thenReturn(x509Certificates);
      when(validator.validate(any(Credential.class), any(RequestData.class)))
          .thenReturn(credential);
    } catch (WSSecurityException e) {
      // ignore
    }
    x509PathTokenValidator.setValidator(validator);

    TokenValidatorParameters tokenParameters = mock(TokenValidatorParameters.class);
    STSPropertiesMBean stsPropertiesMBean = mock(STSPropertiesMBean.class);
    when(tokenParameters.getStsProperties()).thenReturn(stsPropertiesMBean);
    Crypto crypto = mock(Crypto.class);
    when(stsPropertiesMBean.getSignatureCrypto()).thenReturn(crypto);
    ReceivedToken receivedToken = mock(ReceivedToken.class);
    doCallRealMethod().when(receivedToken).setState(any(ReceivedToken.STATE.class));
    doCallRealMethod().when(receivedToken).getState();
    when(tokenParameters.getToken()).thenReturn(receivedToken);
    when(receivedToken.isBinarySecurityToken()).thenReturn(true);

    BinarySecurityTokenType binarySecurityTokenType = mock(BinarySecurityTokenType.class);
    when(binarySecurityTokenType.getValueType()).thenReturn(type);

    when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);
    when(binarySecurityTokenType.getEncodingType())
        .thenReturn(X509PathTokenValidator.BASE64_ENCODING);
    when(binarySecurityTokenType.getValue()).thenReturn("data");

    TokenValidatorResponse tokenValidatorResponse =
        x509PathTokenValidator.validateToken(tokenParameters);

    assertEquals(ReceivedToken.STATE.VALID, tokenValidatorResponse.getToken().getState());
  }

  @Test
  public void testValidateBadToken() {
    X509PathTokenValidator x509PathTokenValidator = new X509PathTokenValidator();

    try {
      Credential credential = mock(Credential.class);
      X509Certificate x509Certificate = mock(X509Certificate.class);
      X500Principal x500Principal = new X500Principal("cn=myxman,ou=someunit,o=someorg");
      when(x509Certificate.getSubjectX500Principal()).thenReturn(x500Principal);
      X509Certificate[] x509Certificates = new X509Certificate[] {x509Certificate};
      when(credential.getCertificates()).thenReturn(x509Certificates);
      when(validator.validate(any(Credential.class), any(RequestData.class)))
          .thenThrow(new WSSecurityException(WSSecurityException.ErrorCode.SECURITY_ERROR));
    } catch (WSSecurityException e) {
      // ignore
    }
    x509PathTokenValidator.setValidator(validator);

    TokenValidatorParameters tokenParameters = mock(TokenValidatorParameters.class);
    STSPropertiesMBean stsPropertiesMBean = mock(STSPropertiesMBean.class);
    when(tokenParameters.getStsProperties()).thenReturn(stsPropertiesMBean);
    Crypto crypto = mock(Crypto.class);
    when(stsPropertiesMBean.getSignatureCrypto()).thenReturn(crypto);
    ReceivedToken receivedToken = mock(ReceivedToken.class);
    doCallRealMethod().when(receivedToken).setState(any(ReceivedToken.STATE.class));
    doCallRealMethod().when(receivedToken).getState();
    when(tokenParameters.getToken()).thenReturn(receivedToken);
    when(receivedToken.isBinarySecurityToken()).thenReturn(true);
    BinarySecurityTokenType binarySecurityTokenType = mock(BinarySecurityTokenType.class);
    when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);
    when(binarySecurityTokenType.getEncodingType())
        .thenReturn(X509PathTokenValidator.BASE64_ENCODING);
    when(binarySecurityTokenType.getValueType()).thenReturn("valuetype");
    when(binarySecurityTokenType.getValue()).thenReturn("data");

    TokenValidatorResponse tokenValidatorResponse =
        x509PathTokenValidator.validateToken(tokenParameters);

    assertEquals(ReceivedToken.STATE.INVALID, tokenValidatorResponse.getToken().getState());
  }

  @Test
  public void testAdditionalPropertyEmail() {
    try {
      Credential credential = mock(Credential.class);
      X509Certificate x509Certificate = mock(X509Certificate.class);
      X500Principal x500Principal =
          new X500Principal("cn=myxman,ou=someunit,o=someorg,EMAILADDRESS=name@example.com");
      when(x509Certificate.getSubjectX500Principal()).thenReturn(x500Principal);
      X509Certificate[] x509Certificates = new X509Certificate[] {x509Certificate};
      when(credential.getCertificates()).thenReturn(x509Certificates);
      when(validator.validate(any(Credential.class), any(RequestData.class)))
          .thenReturn(credential);
    } catch (WSSecurityException e) {
      // ignore
    }
    x509PathTokenValidator.setValidator(validator);

    TokenValidatorParameters tokenParameters = mock(TokenValidatorParameters.class);
    STSPropertiesMBean stsPropertiesMBean = mock(STSPropertiesMBean.class);
    when(tokenParameters.getStsProperties()).thenReturn(stsPropertiesMBean);
    Crypto crypto = mock(Crypto.class);
    when(stsPropertiesMBean.getSignatureCrypto()).thenReturn(crypto);
    ReceivedToken receivedToken = mock(ReceivedToken.class);
    doCallRealMethod().when(receivedToken).setState(any(ReceivedToken.STATE.class));
    doCallRealMethod().when(receivedToken).getState();
    when(tokenParameters.getToken()).thenReturn(receivedToken);
    when(receivedToken.isBinarySecurityToken()).thenReturn(true);

    BinarySecurityTokenType binarySecurityTokenType = mock(BinarySecurityTokenType.class);
    when(binarySecurityTokenType.getValueType()).thenReturn(X509TokenValidator.X509_V3_TYPE);

    when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);
    when(binarySecurityTokenType.getEncodingType())
        .thenReturn(X509PathTokenValidator.BASE64_ENCODING);
    when(binarySecurityTokenType.getValue()).thenReturn("data");

    TokenValidatorResponse tokenValidatorResponse =
        x509PathTokenValidator.validateToken(tokenParameters);

    assertEquals(ReceivedToken.STATE.VALID, tokenValidatorResponse.getToken().getState());
    assertEquals(
        "name@example.com",
        tokenValidatorResponse.getAdditionalProperties().get(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI));
  }

  @Test
  public void testAdditionalPropertyCountry() {
    try {
      Credential credential = mock(Credential.class);
      X509Certificate x509Certificate = mock(X509Certificate.class);
      X500Principal x500Principal = new X500Principal("cn=myxman,ou=someunit,o=someorg,C=US");
      when(x509Certificate.getSubjectX500Principal()).thenReturn(x500Principal);
      X509Certificate[] x509Certificates = new X509Certificate[] {x509Certificate};
      when(credential.getCertificates()).thenReturn(x509Certificates);
      when(validator.validate(any(Credential.class), any(RequestData.class)))
          .thenReturn(credential);
    } catch (WSSecurityException e) {
      // ignore
    }
    x509PathTokenValidator.setValidator(validator);

    TokenValidatorParameters tokenParameters = mock(TokenValidatorParameters.class);
    STSPropertiesMBean stsPropertiesMBean = mock(STSPropertiesMBean.class);
    when(tokenParameters.getStsProperties()).thenReturn(stsPropertiesMBean);
    Crypto crypto = mock(Crypto.class);
    when(stsPropertiesMBean.getSignatureCrypto()).thenReturn(crypto);
    ReceivedToken receivedToken = mock(ReceivedToken.class);
    doCallRealMethod().when(receivedToken).setState(any(ReceivedToken.STATE.class));
    doCallRealMethod().when(receivedToken).getState();
    when(tokenParameters.getToken()).thenReturn(receivedToken);
    when(receivedToken.isBinarySecurityToken()).thenReturn(true);

    BinarySecurityTokenType binarySecurityTokenType = mock(BinarySecurityTokenType.class);
    when(binarySecurityTokenType.getValueType()).thenReturn(X509TokenValidator.X509_V3_TYPE);

    when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);
    when(binarySecurityTokenType.getEncodingType())
        .thenReturn(X509PathTokenValidator.BASE64_ENCODING);
    when(binarySecurityTokenType.getValue()).thenReturn("data");

    TokenValidatorResponse tokenValidatorResponse =
        x509PathTokenValidator.validateToken(tokenParameters);

    assertEquals(ReceivedToken.STATE.VALID, tokenValidatorResponse.getToken().getState());
    assertEquals(
        "US", tokenValidatorResponse.getAdditionalProperties().get(SubjectUtils.COUNTRY_CLAIM_URI));
  }

  @Test
  public void testAdditionalPropertyBoth() {
    try {
      Credential credential = mock(Credential.class);
      X509Certificate x509Certificate = mock(X509Certificate.class);
      X500Principal x500Principal =
          new X500Principal("cn=myxman,ou=someunit,o=someorg,C=US,EMAILADDRESS=name@example.com");
      when(x509Certificate.getSubjectX500Principal()).thenReturn(x500Principal);
      X509Certificate[] x509Certificates = new X509Certificate[] {x509Certificate};
      when(credential.getCertificates()).thenReturn(x509Certificates);
      when(validator.validate(any(Credential.class), any(RequestData.class)))
          .thenReturn(credential);
    } catch (WSSecurityException e) {
      // ignore
    }
    x509PathTokenValidator.setValidator(validator);

    TokenValidatorParameters tokenParameters = mock(TokenValidatorParameters.class);
    STSPropertiesMBean stsPropertiesMBean = mock(STSPropertiesMBean.class);
    when(tokenParameters.getStsProperties()).thenReturn(stsPropertiesMBean);
    Crypto crypto = mock(Crypto.class);
    when(stsPropertiesMBean.getSignatureCrypto()).thenReturn(crypto);
    ReceivedToken receivedToken = mock(ReceivedToken.class);
    doCallRealMethod().when(receivedToken).setState(any(ReceivedToken.STATE.class));
    doCallRealMethod().when(receivedToken).getState();
    when(tokenParameters.getToken()).thenReturn(receivedToken);
    when(receivedToken.isBinarySecurityToken()).thenReturn(true);

    BinarySecurityTokenType binarySecurityTokenType = mock(BinarySecurityTokenType.class);
    when(binarySecurityTokenType.getValueType()).thenReturn(X509TokenValidator.X509_V3_TYPE);

    when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);
    when(binarySecurityTokenType.getEncodingType())
        .thenReturn(X509PathTokenValidator.BASE64_ENCODING);
    when(binarySecurityTokenType.getValue()).thenReturn("data");

    TokenValidatorResponse tokenValidatorResponse =
        x509PathTokenValidator.validateToken(tokenParameters);

    assertEquals(ReceivedToken.STATE.VALID, tokenValidatorResponse.getToken().getState());
    assertEquals(
        "US", tokenValidatorResponse.getAdditionalProperties().get(SubjectUtils.COUNTRY_CLAIM_URI));
    assertEquals(
        "name@example.com",
        tokenValidatorResponse.getAdditionalProperties().get(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI));
  }
}
