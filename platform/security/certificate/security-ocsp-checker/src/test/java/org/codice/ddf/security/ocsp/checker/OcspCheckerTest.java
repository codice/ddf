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
package org.codice.ddf.security.ocsp.checker;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.OCSPException;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.SingleResp;
import org.bouncycastle.cert.ocsp.UnknownStatus;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.junit.Test;
import org.osgi.service.event.EventAdmin;

public class OcspCheckerTest {

  private final ClientFactoryFactory factory = mock(ClientFactoryFactory.class);
  private final EventAdmin eventAdmin = mock(EventAdmin.class);

  @Test
  public void testConvertingX509CertificatesToBCCertificates() throws Exception {
    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    X509Certificate x509Certificate = getX509Certificate();

    Certificate certificate = ocspChecker.convertToBouncyCastleCert(x509Certificate);
    assertThat(certificate, is(notNullValue()));
    assertThat(
        x509Certificate.getSerialNumber(), equalTo(certificate.getSerialNumber().getValue()));
    assertThat(x509Certificate.getNotAfter(), equalTo(certificate.getEndDate().getDate()));
    assertThat(x509Certificate.getNotBefore(), equalTo(certificate.getStartDate().getDate()));

    X500Principal subjectX500Principal = x509Certificate.getSubjectX500Principal();
    X500Name x500name = new X500Name(subjectX500Principal.getName(X500Principal.RFC1779));
    assertThat(x500name, equalTo(certificate.getIssuer()));
  }

  @Test
  public void testGeneratingOcspRequest() throws Exception {
    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    Certificate certificate = getBouncyCastleCertificate();

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(certificate);
    assertThat(ocspReq, is(notNullValue()));

    assertThat(
        ocspReq.getRequestList()[0].getCertID().getSerialNumber(),
        equalTo(certificate.getSerialNumber().getValue()));
  }

  @Test
  public void testSendingOcspRequest() throws Exception {
    ClientFactoryFactory clientFactoryFactory = mockClientFactoryResponse();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspServerUrl("https://testurl:8993");

    Certificate certificate = getBouncyCastleCertificate();
    OCSPReq ocspReq = ocspChecker.generateOcspRequest(certificate);

    Response response = ocspChecker.sendOcspRequest(ocspReq);

    assertThat(response, is(notNullValue()));
  }

  @Test
  public void testValidatingOCSPResponseWithUnknownStatus() throws Exception {
    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);

    OCSPResp resp = mockOcspResponse(new UnknownStatus());
    boolean response = ocspChecker.isOCSPResponseValid(resp);
    assertThat(response, is(false));
  }

  @Test
  public void testValidatingOCSPResponseWithRevokedStatus() throws Exception {
    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);

    OCSPResp resp = mockOcspResponse(new RevokedStatus(new Date(), 0));
    boolean response = ocspChecker.isOCSPResponseValid(resp);
    assertThat(response, is(false));
  }

  @Test
  public void testValidatingOCSPResponseWithNullBasicResponse() throws Exception {
    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);

    OCSPResp resp = mock(OCSPResp.class);
    when(resp.getResponseObject()).thenReturn(null);

    boolean response = ocspChecker.isOCSPResponseValid(resp);
    assertThat(response, is(false));
  }

  @Test
  public void testValidatingOCSPResponseWithNullResponse() throws Exception {
    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);

    OCSPResp resp = mock(OCSPResp.class);
    BasicOCSPResp basicOCSPResp = mock(BasicOCSPResp.class);
    when(basicOCSPResp.getResponses()).thenReturn(null);
    when(resp.getResponseObject()).thenReturn(basicOCSPResp);

    boolean response = ocspChecker.isOCSPResponseValid(resp);
    assertThat(response, is(false));
  }

  @Test
  public void testValidatingOCSPResponseWithNullStatus() throws Exception {
    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);

    OCSPResp resp = mockOcspResponse(null);
    boolean response = ocspChecker.isOCSPResponseValid(resp);
    assertThat(response, is(true));
  }

  @Test(expected = OcspChecker.OcspCheckerException.class)
  public void testPostEvent() throws Exception {
    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    Response response = mock(Response.class);

    ocspChecker.createOcspResponse(response);
  }

  @Test
  public void testIfErrorOccursCertificateIsNotRevoked() throws Exception {
    ClientFactoryFactory clientFactoryFactory = mockClientFactoryResponse();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrl("https://testurl:8993");

    X509Certificate x509Certificate = getX509Certificate();
    X509Certificate[] x509CertificateArray = {x509Certificate};

    boolean ocspCheckPasses = ocspChecker.passesOcspCheck(x509CertificateArray);
    verify(eventAdmin, times(1)).postEvent(any());
    assertThat(ocspCheckPasses, is(true));
  }

  private X509Certificate getX509Certificate() throws Exception {
    String certificateString =
        "MIIC5DCCAk2gAwIBAgIJAKj7ROPHjo1yMA0GCSqGSIb3DQEBCwUAMIGKMQswCQYDVQQGEwJVUzEQ"
            + "MA4GA1UECAwHQXJpem9uYTERMA8GA1UEBwwIR29vZHllYXIxGDAWBgNVBAoMD0xvY2toZWVkIE1h"
            + "cnRpbjENMAsGA1UECwwESTRDRTEPMA0GA1UEAwwGY2xpZW50MRwwGgYJKoZIhvcNAQkBFg1pNGNl"
            + "QGxtY28uY29tMB4XDTEyMDYyMDE5NDMwOVoXDTIyMDYxODE5NDMwOVowgYoxCzAJBgNVBAYTAlVT"
            + "MRAwDgYDVQQIDAdBcml6b25hMREwDwYDVQQHDAhHb29keWVhcjEYMBYGA1UECgwPTG9ja2hlZWQg"
            + "TWFydGluMQ0wCwYDVQQLDARJNENFMQ8wDQYDVQQDDAZjbGllbnQxHDAaBgkqhkiG9w0BCQEWDWk0"
            + "Y2VAbG1jby5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAIpHxCBLYE7xfDLcITS9SsPG"
            + "4Q04Z6S32/+TriGsRgpGTj/7GuMG7oJ98m6Ws5cTYl7nyunyHTkZuP7rBzy4esDIHheyx18EgdSJ"
            + "vvACgGVCnEmHndkf9bWUlAOfNaxW+vZwljUkRUVdkhPbPdPwOcMdKg/SsLSNjZfsQIjoWd4rAgMB"
            + "AAGjUDBOMB0GA1UdDgQWBBQx11VLtYXLvFGpFdHnhlNW9+lxBDAfBgNVHSMEGDAWgBQx11VLtYXL"
            + "vFGpFdHnhlNW9+lxBDAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4GBAHYs2OI0K6yVXzyS"
            + "sKcv2fmfw6XCICGTnyA7BOdAjYoqq6wD+33dHJUCFDqye7AWdcivuc7RWJt9jnlfJZKIm2BHcDTR"
            + "Hhk6CvjJ14Gf40WQdeMHoX8U8b0diq7Iy5Ravx+zRg7SdiyJUqFYjRh/O5tywXRT1+freI3bwAN0"
            + "L6tQ";

    InputStream inputStream =
        new ByteArrayInputStream(Base64.getMimeDecoder().decode(certificateString.getBytes()));
    return (X509Certificate)
        CertificateFactory.getInstance("X.509").generateCertificate(inputStream);
  }

  private Certificate getBouncyCastleCertificate() throws Exception {
    X500Name dnName = new X500Name("dc=name");

    long now = System.currentTimeMillis();
    BigInteger certSerialNumber = new BigInteger(Long.toString(now));

    Date startDate = new Date(now);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(startDate);
    calendar.add(Calendar.YEAR, 1);
    Date endDate = calendar.getTime();

    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
    keyPairGenerator.initialize(4096, new SecureRandom());
    KeyPair keyPair = keyPairGenerator.generateKeyPair();

    X509v3CertificateBuilder v3CertGen =
        new X509v3CertificateBuilder(
            dnName,
            certSerialNumber,
            startDate,
            endDate,
            dnName,
            SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));

    ContentSigner contentSigner =
        new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
    X509CertificateHolder x509CertificateHolder = v3CertGen.build(contentSigner);
    return x509CertificateHolder.toASN1Structure();
  }

  private OCSPResp mockOcspResponse(Object status) throws OCSPException {
    CertificateStatus certificateStatus;
    if (status instanceof UnknownStatus) {
      certificateStatus = (UnknownStatus) status;
    } else if (status instanceof RevokedStatus) {
      certificateStatus = (RevokedStatus) status;
    } else {
      certificateStatus = null;
    }

    BasicOCSPResp basicOCSPResp = mock(BasicOCSPResp.class);
    SingleResp singleResp = mock(SingleResp.class);
    when(singleResp.getCertStatus()).thenReturn(certificateStatus);
    SingleResp[] singleResps = {singleResp};
    when(basicOCSPResp.getResponses()).thenReturn(singleResps);

    OCSPResp ocspResp = mock(OCSPResp.class);
    when(ocspResp.getResponseObject()).thenReturn(basicOCSPResp);
    return ocspResp;
  }

  private ClientFactoryFactory mockClientFactoryResponse() {
    Response response = mock(Response.class);

    WebClient webClient = mock(WebClient.class);
    when(webClient.type(anyString())).thenReturn(webClient);
    when(webClient.accept(anyString())).thenReturn(webClient);
    when(webClient.post(any())).thenReturn(response);

    SecureCxfClientFactory secureCxfClientFactory = mock(SecureCxfClientFactory.class);
    when(secureCxfClientFactory.getWebClient()).thenReturn(webClient);

    ClientFactoryFactory clientFactoryFactory = mock(ClientFactoryFactory.class);
    when(clientFactoryFactory.getSecureCxfClientFactory("https://testurl:8993", WebClient.class))
        .thenReturn(secureCxfClientFactory);
    return clientFactoryFactory;
  }
}
