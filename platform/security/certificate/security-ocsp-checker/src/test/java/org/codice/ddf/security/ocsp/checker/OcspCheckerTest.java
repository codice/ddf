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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.security.auth.x500.X500Principal;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.UnknownStatus;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.security.ocsp.checker.OcspChecker.OcspCheckerException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.event.EventAdmin;

public class OcspCheckerTest {

  // all certs in test resources have this embedded ocsp server url
  private static final String EMBEDDED_OCSP_SERVER_URL = "http://127.0.0.1:8080";

  private static X509Certificate trustedCertX509;
  private static Certificate trustedCertBc;
  private static X509Certificate notTrustedCertX509;
  private static Certificate notTrustedCertBc;

  private final ClientFactoryFactory factory = mock(ClientFactoryFactory.class);
  private final EventAdmin eventAdmin = mock(EventAdmin.class);

  // these should be populated per method; used when mocking the client factory
  private List<String> goodEndpoints = new ArrayList<>();
  private List<String> revokedEndpoints = new ArrayList<>();
  private List<String> unknownEndpoints = new ArrayList<>();
  private List<String> brokenEndpoints = new ArrayList<>();

  // mocks
  @Mock private Response goodResponse;
  @Mock private Response revokedResponse;
  @Mock private Response unknownResponse;
  @Mock private Response brokenResponse;
  @Mock private WebClient goodWebClient;
  @Mock private WebClient revokedWebClient;
  @Mock private WebClient unknownWebClient;
  @Mock private WebClient brokenWebClient;
  @Mock private SecureCxfClientFactory goodSecureCxfClientFactory;
  @Mock private SecureCxfClientFactory revokedSecureCxfClientFactory;
  @Mock private SecureCxfClientFactory unknownSecureCxfClientFactory;
  @Mock private SecureCxfClientFactory brokenSecureCxfClientFactory;

  // mockito argument matchers for list matching
  private ArgumentMatcher<String> inGoodList =
      new ArgumentMatcher<String>() {
        @Override
        public boolean matches(Object string) {
          return goodEndpoints.contains(string);
        }
      };

  private ArgumentMatcher<String> inRevokedList =
      new ArgumentMatcher<String>() {
        @Override
        public boolean matches(Object string) {
          return revokedEndpoints.contains(string);
        }
      };

  private ArgumentMatcher<String> inUnknownList =
      new ArgumentMatcher<String>() {
        @Override
        public boolean matches(Object string) {
          return unknownEndpoints.contains(string);
        }
      };

  private ArgumentMatcher<String> inBrokenList =
      new ArgumentMatcher<String>() {
        @Override
        public boolean matches(Object string) {
          return brokenEndpoints.contains(string);
        }
      };

  @BeforeClass
  public static void setupClass() throws Exception {
    // truststore.jks contains CA_CERT
    URL truststoreUrl = OcspCheckerTest.class.getClassLoader().getResource("truststore.jks");
    System.setProperty(SecurityConstants.TRUSTSTORE_PATH, truststoreUrl.getPath());
    System.setProperty(SecurityConstants.TRUSTSTORE_PASSWORD, "changeit");
    System.setProperty(SecurityConstants.TRUSTSTORE_TYPE, "JKS");

    trustedCertX509 = getX509Certificate("trusted.crt");
    trustedCertBc = getBouncyCastleCertificate(trustedCertX509);
    notTrustedCertX509 = getX509Certificate("not-trusted.crt");
    notTrustedCertBc = getBouncyCastleCertificate(notTrustedCertX509);
  }

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(goodWebClient.type(anyString())).thenReturn(goodWebClient);
    when(goodWebClient.accept(anyString())).thenReturn(goodWebClient);
    when(goodWebClient.post(any())).thenReturn(goodResponse);

    when(revokedWebClient.type(anyString())).thenReturn(revokedWebClient);
    when(revokedWebClient.accept(anyString())).thenReturn(revokedWebClient);
    when(revokedWebClient.post(any())).thenReturn(revokedResponse);

    when(unknownWebClient.type(anyString())).thenReturn(unknownWebClient);
    when(unknownWebClient.accept(anyString())).thenReturn(unknownWebClient);
    when(unknownWebClient.post(any())).thenReturn(unknownResponse);

    when(brokenWebClient.type(anyString())).thenReturn(brokenWebClient);
    when(brokenWebClient.accept(anyString())).thenReturn(brokenWebClient);
    when(brokenWebClient.post(any())).thenReturn(brokenResponse);

    when(goodSecureCxfClientFactory.getWebClient()).thenReturn(goodWebClient);
    when(revokedSecureCxfClientFactory.getWebClient()).thenReturn(revokedWebClient);
    when(unknownSecureCxfClientFactory.getWebClient()).thenReturn(unknownWebClient);
    when(brokenSecureCxfClientFactory.getWebClient()).thenReturn(brokenWebClient);

    when(goodResponse.getEntity()).then(getResourceStreamAsAnswer("goodOcspResponse.streamData"));
    when(revokedResponse.getEntity())
        .then(getResourceStreamAsAnswer("revokedOcspResponse.streamData"));
    when(unknownResponse.getEntity())
        .then(getResourceStreamAsAnswer("unknownOcspResponse.streamData"));
    when(brokenResponse.getEntity()).thenReturn(null);
  }

  @AfterClass
  public static void cleanupClass() {
    System.clearProperty(SecurityConstants.TRUSTSTORE_PATH);
    System.clearProperty(SecurityConstants.TRUSTSTORE_PASSWORD);
    System.clearProperty(SecurityConstants.TRUSTSTORE_TYPE);
  }

  @Test
  public void testConvertingX509CertificatesToBcCertificates() throws Exception {
    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);

    Certificate certificate = ocspChecker.convertToBouncyCastleCert(trustedCertX509);
    assertThat(certificate, is(notNullValue()));
    assertThat(
        trustedCertX509.getSerialNumber(), equalTo(certificate.getSerialNumber().getValue()));
    assertThat(trustedCertX509.getNotAfter(), equalTo(certificate.getEndDate().getDate()));
    assertThat(trustedCertX509.getNotBefore(), equalTo(certificate.getStartDate().getDate()));

    X500Principal subjectX500Principal = trustedCertX509.getSubjectX500Principal();
    X500Name x500name = new X500Name(subjectX500Principal.getName(X500Principal.RFC1779));
    assertThat(x500name, equalTo(certificate.getSubject()));
  }

  @Test
  public void testGeneratingOcspRequest() throws Exception {
    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    Certificate certificate = trustedCertBc;

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(certificate);
    assertThat(ocspReq, is(notNullValue()));

    assertThat(
        ocspReq.getRequestList()[0].getCertID().getSerialNumber(),
        equalTo(certificate.getSerialNumber().getValue()));
  }

  @Test(expected = OcspCheckerException.class)
  public void testGeneratingOcspRequestNonResolvableIssuer() throws Exception {
    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);

    ocspChecker.generateOcspRequest(notTrustedCertBc);
  }

  @Test
  public void testSendOcspRequestsGoodStatus() throws Exception {
    goodEndpoints.add("https://goodurl:8993");
    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);
    List<String> ocspServerUrls = new ArrayList<>(goodEndpoints);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<String, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestsRevokedStatus() throws Exception {
    revokedEndpoints.add("https://revokedurl:8993");
    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);
    List<String> ocspServerUrls = new ArrayList<>(revokedEndpoints);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<String, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestsUnknownStatus() throws Exception {
    unknownEndpoints.add("https://unknownurl:8993");
    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);
    List<String> ocspServerUrls = new ArrayList<>(unknownEndpoints);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<String, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestsAllStatuses() throws Exception {
    unknownEndpoints.add("https://unknownurl:8993");
    unknownEndpoints.add("https://unknownurl2:8993");
    goodEndpoints.add("https://goodurl:8993");
    goodEndpoints.add("https://goodurl2:8993");
    revokedEndpoints.add("https://revokedurl:8993");
    revokedEndpoints.add("https://revokedurl2:8993");
    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    List<String> ocspServerUrls = new ArrayList<>();
    ocspServerUrls.addAll(unknownEndpoints);
    ocspServerUrls.addAll(revokedEndpoints);
    ocspServerUrls.addAll(goodEndpoints);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<String, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestsGoodEmbeddedUrl() throws Exception {
    unknownEndpoints.add("https://unknownurl:8993");
    unknownEndpoints.add("https://unknownurl2:8993");
    goodEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    List<String> ocspServerUrls = new ArrayList<>(unknownEndpoints);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<String, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestsRevokedEmbeddedUrl() throws Exception {
    unknownEndpoints.add("https://unknownurl:8993");
    unknownEndpoints.add("https://unknownurl2:8993");
    revokedEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    List<String> ocspServerUrls = new ArrayList<>(unknownEndpoints);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<String, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestsUnknownEmbeddedUrl() throws Exception {
    unknownEndpoints.add("https://unknownurl:8993");
    unknownEndpoints.add("https://unknownurl2:8993");

    List<String> ocspServerUrls = new ArrayList<>(unknownEndpoints);

    unknownEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<String, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestNoServerUrls() throws Exception {
    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<String, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestBrokenServerUrls() throws Exception {
    brokenEndpoints.add("https://brokenurl:8993");
    brokenEndpoints.add("https://brokenurl2:8993");
    brokenEndpoints.add("https://brokenurl3:8993");

    List<String> ocspServerUrls = new ArrayList<>(brokenEndpoints);

    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<String, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testOcspCheckGoodStatus() {
    goodEndpoints.add("https://goodurl:8993");
    goodEndpoints.add("https://goodurl2:8993");
    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    List<String> ocspServerUrls = new ArrayList<>(goodEndpoints);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(true));
  }

  @Test
  public void testOcspCheckRevokedStatus() {
    revokedEndpoints.add("https://revokedurl:8993");
    revokedEndpoints.add("https://revokedurl2:8993");
    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    List<String> ocspServerUrls = new ArrayList<>(revokedEndpoints);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(false));
  }

  @Test
  public void testOcspCheckUnknownStatus() {
    unknownEndpoints.add("https://unknownurl:8993");
    unknownEndpoints.add("https://unknownurl2:8993");
    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    List<String> ocspServerUrls = new ArrayList<>(unknownEndpoints);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(true));
  }

  @Test
  public void testOcspCheckAllStatuses() {
    unknownEndpoints.add("https://unknownurl:8993");
    unknownEndpoints.add("https://unknownurl2:8993");
    goodEndpoints.add("https://goodurl:8993");
    goodEndpoints.add("https://goodurl2:8993");
    revokedEndpoints.add("https://revokedurl:8993");
    revokedEndpoints.add("https://revokedurl2:8993");
    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    List<String> ocspServerUrls = new ArrayList<>();
    ocspServerUrls.addAll(unknownEndpoints);
    ocspServerUrls.addAll(revokedEndpoints);
    ocspServerUrls.addAll(goodEndpoints);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(false));
  }

  @Test
  public void testOcspRevokedEmbeddedUrl() {
    unknownEndpoints.add("https://unknownurl:8993");
    unknownEndpoints.add("https://unknownurl2:8993");
    goodEndpoints.add("https://goodurl:8993");
    goodEndpoints.add("https://goodurl2:8993");

    List<String> ocspServerUrls = new ArrayList<>();
    ocspServerUrls.addAll(unknownEndpoints);
    ocspServerUrls.addAll(goodEndpoints);

    revokedEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(false));
  }

  @Test
  public void testOcspCheckUnknownCert() {
    unknownEndpoints.add("https://unknownurl:8993");
    unknownEndpoints.add("https://unknownurl2:8993");
    goodEndpoints.add("https://goodurl:8993");
    goodEndpoints.add("https://goodurl2:8993");
    revokedEndpoints.add("https://revokedurl:8993");
    revokedEndpoints.add("https://revokedurl2:8993");
    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    List<String> ocspServerUrls = new ArrayList<>();
    ocspServerUrls.addAll(unknownEndpoints);
    ocspServerUrls.addAll(revokedEndpoints);
    ocspServerUrls.addAll(goodEndpoints);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    X509Certificate[] certs = new X509Certificate[] {notTrustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(true));
  }

  @Test
  public void testOcspCheckMultiCertGoodStatus() {
    unknownEndpoints.add("https://unknownurl:8993");
    unknownEndpoints.add("https://unknownurl2:8993");
    goodEndpoints.add("https://goodurl:8993");
    goodEndpoints.add("https://goodurl2:8993");
    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    List<String> ocspServerUrls = new ArrayList<>();
    ocspServerUrls.addAll(unknownEndpoints);
    ocspServerUrls.addAll(goodEndpoints);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    X509Certificate[] certs = new X509Certificate[] {notTrustedCertX509, trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(true));
  }

  @Test
  public void testOcspCheckMultiCertAllStatuses() {
    unknownEndpoints.add("https://unknownurl:8993");
    unknownEndpoints.add("https://unknownurl2:8993");
    goodEndpoints.add("https://goodurl:8993");
    goodEndpoints.add("https://goodurl2:8993");
    revokedEndpoints.add("https://revokedurl:8993");
    revokedEndpoints.add("https://revokedurl2:8993");
    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    List<String> ocspServerUrls = new ArrayList<>();
    ocspServerUrls.addAll(unknownEndpoints);
    ocspServerUrls.addAll(goodEndpoints);
    ocspServerUrls.addAll(revokedEndpoints);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    X509Certificate[] certs = new X509Certificate[] {notTrustedCertX509, trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(false));
  }

  @Test
  public void testOcspCheckNoServerUrls() {
    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(true));
  }

  @Test
  public void testOcspCheckBrokenServerUrls() {
    brokenEndpoints.add("https://brokenurl:8993");
    brokenEndpoints.add("https://brokenurl2:8993");
    brokenEndpoints.add("https://brokenurl3:8993");

    List<String> ocspServerUrls = new ArrayList<>(brokenEndpoints);

    brokenEndpoints.add(EMBEDDED_OCSP_SERVER_URL);

    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(ocspServerUrls);

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(true));
  }

  @Test
  public void testOcspCheckDisabled() throws Exception {
    ClientFactoryFactory clientFactoryFactory = mockClientFactory();
    OcspChecker ocspChecker = new OcspChecker(clientFactoryFactory, eventAdmin);
    ocspChecker.setOcspEnabled(false);

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(true));
  }

  private static X509Certificate getX509Certificate(String filename) throws Exception {
    try (InputStream certInputStream =
        OcspCheckerTest.class.getClassLoader().getResourceAsStream(filename)) {
      return (X509Certificate)
          CertificateFactory.getInstance("X.509").generateCertificate(certInputStream);
    }
  }

  private static Certificate getBouncyCastleCertificate(X509Certificate cert) throws Exception {
    return Certificate.getInstance(cert.getEncoded());
  }

  private ClientFactoryFactory mockClientFactory() {
    ClientFactoryFactory clientFactoryFactory = mock(ClientFactoryFactory.class);

    when(clientFactoryFactory.getSecureCxfClientFactory(argThat(inGoodList), eq(WebClient.class)))
        .thenReturn(goodSecureCxfClientFactory);
    when(clientFactoryFactory.getSecureCxfClientFactory(
            argThat(inRevokedList), eq(WebClient.class)))
        .thenReturn(revokedSecureCxfClientFactory);
    when(clientFactoryFactory.getSecureCxfClientFactory(
            argThat(inUnknownList), eq(WebClient.class)))
        .thenReturn(unknownSecureCxfClientFactory);
    when(clientFactoryFactory.getSecureCxfClientFactory(argThat(inBrokenList), eq(WebClient.class)))
        .thenReturn(brokenSecureCxfClientFactory);

    return clientFactoryFactory;
  }

  private Answer<InputStream> getResourceStreamAsAnswer(String filename) {
    return new Answer<InputStream>() {
      @Override
      public InputStream answer(InvocationOnMock invocationOnMock) {
        return this.getClass().getClassLoader().getResourceAsStream(filename);
      }
    };
  }

  private void assertStatuses(Map<String, CertificateStatus> ocspStatuses) {
    goodEndpoints.forEach(endpoint -> assertNull(ocspStatuses.get(endpoint)));
    revokedEndpoints.forEach(
        endpoint -> assertThat(ocspStatuses.get(endpoint), instanceOf(RevokedStatus.class)));
    unknownEndpoints.forEach(
        endpoint -> assertThat(ocspStatuses.get(endpoint), instanceOf(UnknownStatus.class)));
    brokenEndpoints.forEach(
        endpoint -> assertThat(ocspStatuses.get(endpoint), instanceOf(UnknownStatus.class)));
  }
}
