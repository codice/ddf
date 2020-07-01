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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.audit.SecurityLogger;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.security.auth.x500.X500Principal;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.UnknownStatus;
import org.codice.ddf.cxf.client.ClientBuilder;
import org.codice.ddf.cxf.client.ClientBuilderFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.cxf.client.impl.ClientBuilderImpl;
import org.codice.ddf.cxf.oauth.OAuthSecurity;
import org.codice.ddf.security.jaxrs.SamlSecurity;
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

  private final ClientBuilderFactory factory = mock(ClientBuilderFactory.class);
  private final EventAdmin eventAdmin = mock(EventAdmin.class);

  // these should be populated per method; used when mocking the client factory
  private final List<URI> goodEndpoints = new ArrayList<>();
  private final List<URI> revokedEndpoints = new ArrayList<>();
  private final List<URI> unknownEndpoints = new ArrayList<>();
  private final List<URI> brokenEndpoints = new ArrayList<>();

  // mocks
  @Mock private Response goodResponse;
  @Mock private Response revokedResponse;
  @Mock private Response unknownResponse;
  @Mock private Response brokenResponse;
  @Mock private WebClient goodWebClient;
  @Mock private WebClient revokedWebClient;
  @Mock private WebClient unknownWebClient;
  @Mock private WebClient brokenWebClient;
  @Mock private SecureCxfClientFactory<WebClient> goodSecureCxfClientFactory;
  @Mock private SecureCxfClientFactory<WebClient> revokedSecureCxfClientFactory;
  @Mock private SecureCxfClientFactory<WebClient> unknownSecureCxfClientFactory;
  @Mock private SecureCxfClientFactory<WebClient> brokenSecureCxfClientFactory;

  // mockito argument matchers for list matching
  private final ArgumentMatcher<URI> inGoodList = goodEndpoints::contains;
  private final ArgumentMatcher<URI> inRevokedList = revokedEndpoints::contains;
  private final ArgumentMatcher<URI> inUnknownList = unknownEndpoints::contains;
  private final ArgumentMatcher<URI> inBrokenList = brokenEndpoints::contains;

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

    ClientBuilder<WebClient> clientBuilder =
        new ClientBuilderImpl<WebClient>(
            mock(OAuthSecurity.class), mock(SamlSecurity.class), mock(SecurityLogger.class)) {
          @Override
          public SecureCxfClientFactory<WebClient> build() {
            if (inGoodList.matches(endpointUrl)) {
              return goodSecureCxfClientFactory;
            }
            if (inRevokedList.matches(endpointUrl)) {
              return revokedSecureCxfClientFactory;
            }
            if (inUnknownList.matches(endpointUrl)) {
              return unknownSecureCxfClientFactory;
            }
            if (inBrokenList.matches(endpointUrl)) {
              return brokenSecureCxfClientFactory;
            }
            return null;
          }
        };

    when(factory.<WebClient>getClientBuilder()).thenReturn(clientBuilder);
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
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));

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
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
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
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));

    ocspChecker.generateOcspRequest(notTrustedCertBc);
  }

  @Test
  public void testSendOcspRequestsGoodStatus() throws Exception {
    goodEndpoints.add(new URI("https://goodurl:8993"));
    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));
    List<URI> ocspServerUrls = new ArrayList<>(goodEndpoints);

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<URI, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestsRevokedStatus() throws Exception {
    revokedEndpoints.add(new URI("https://revokedurl:8993"));
    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));
    List<URI> ocspServerUrls = new ArrayList<>(revokedEndpoints);

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<URI, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestsUnknownStatus() throws Exception {
    unknownEndpoints.add(new URI("https://unknownurl:8993"));
    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));
    List<URI> ocspServerUrls = new ArrayList<>(unknownEndpoints);

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<URI, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestsAllStatuses() throws Exception {
    unknownEndpoints.add(new URI("https://unknownurl:8993"));
    unknownEndpoints.add(new URI("https://unknownurl2:8993"));
    goodEndpoints.add(new URI("https://goodurl:8993"));
    goodEndpoints.add(new URI("https://goodurl2:8993"));
    revokedEndpoints.add(new URI("https://revokedurl:8993"));
    revokedEndpoints.add(new URI("https://revokedurl2:8993"));
    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    List<URI> ocspServerUrls = new ArrayList<>();
    ocspServerUrls.addAll(unknownEndpoints);
    ocspServerUrls.addAll(revokedEndpoints);
    ocspServerUrls.addAll(goodEndpoints);

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<URI, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestsGoodEmbeddedUrl() throws Exception {
    unknownEndpoints.add(new URI("https://unknownurl:8993"));
    unknownEndpoints.add(new URI("https://unknownurl2:8993"));
    goodEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    List<URI> ocspServerUrls = new ArrayList<>(unknownEndpoints);

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<URI, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestsRevokedEmbeddedUrl() throws Exception {
    unknownEndpoints.add(new URI("https://unknownurl:8993"));
    unknownEndpoints.add(new URI("https://unknownurl2:8993"));
    revokedEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    List<URI> ocspServerUrls = new ArrayList<>(unknownEndpoints);

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<URI, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestsUnknownEmbeddedUrl() throws Exception {
    unknownEndpoints.add(new URI("https://unknownurl:8993"));
    unknownEndpoints.add(new URI("https://unknownurl2:8993"));

    List<URI> ocspServerUrls = new ArrayList<>(unknownEndpoints);

    unknownEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<URI, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestNoServerUrls() throws Exception {
    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<URI, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testSendOcspRequestBrokenServerUrls() throws Exception {
    brokenEndpoints.add(new URI("https://brokenurl:8993"));
    brokenEndpoints.add(new URI("https://brokenurl2:8993"));
    brokenEndpoints.add(new URI("https://brokenurl3:8993"));

    List<URI> ocspServerUrls = new ArrayList<>(brokenEndpoints);

    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    OCSPReq ocspReq = ocspChecker.generateOcspRequest(trustedCertBc);
    Map<URI, CertificateStatus> ocspStatuses =
        ocspChecker.sendOcspRequests(trustedCertX509, ocspReq);

    assertStatuses(ocspStatuses);
  }

  @Test
  public void testOcspCheckGoodStatus() throws URISyntaxException {
    goodEndpoints.add(new URI("https://goodurl:8993"));
    goodEndpoints.add(new URI("https://goodurl2:8993"));
    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    List<URI> ocspServerUrls = new ArrayList<>(goodEndpoints);

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(true));
  }

  @Test
  public void testOcspCheckRevokedStatus() throws URISyntaxException {
    revokedEndpoints.add(new URI("https://revokedurl:8993"));
    revokedEndpoints.add(new URI("https://revokedurl2:8993"));
    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    List<URI> ocspServerUrls = new ArrayList<>(revokedEndpoints);

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(false));
  }

  @Test
  public void testOcspCheckUnknownStatus() throws URISyntaxException {
    unknownEndpoints.add(new URI("https://unknownurl:8993"));
    unknownEndpoints.add(new URI("https://unknownurl2:8993"));
    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    List<URI> ocspServerUrls = new ArrayList<>(unknownEndpoints);

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(true));
  }

  @Test
  public void testOcspCheckAllStatuses() throws URISyntaxException {
    unknownEndpoints.add(new URI("https://unknownurl:8993"));
    unknownEndpoints.add(new URI("https://unknownurl2:8993"));
    goodEndpoints.add(new URI("https://goodurl:8993"));
    goodEndpoints.add(new URI("https://goodurl2:8993"));
    revokedEndpoints.add(new URI("https://revokedurl:8993"));
    revokedEndpoints.add(new URI("https://revokedurl2:8993"));
    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    List<URI> ocspServerUrls = new ArrayList<>();
    ocspServerUrls.addAll(unknownEndpoints);
    ocspServerUrls.addAll(revokedEndpoints);
    ocspServerUrls.addAll(goodEndpoints);

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(false));
  }

  @Test
  public void testOcspRevokedEmbeddedUrl() throws URISyntaxException {
    unknownEndpoints.add(new URI("https://unknownurl:8993"));
    unknownEndpoints.add(new URI("https://unknownurl2:8993"));
    goodEndpoints.add(new URI("https://goodurl:8993"));
    goodEndpoints.add(new URI("https://goodurl2:8993"));

    List<URI> ocspServerUrls = new ArrayList<>();
    ocspServerUrls.addAll(unknownEndpoints);
    ocspServerUrls.addAll(goodEndpoints);

    revokedEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(false));
  }

  @Test
  public void testOcspCheckUnknownCert() throws URISyntaxException {
    unknownEndpoints.add(new URI("https://unknownurl:8993"));
    unknownEndpoints.add(new URI("https://unknownurl2:8993"));
    goodEndpoints.add(new URI("https://goodurl:8993"));
    goodEndpoints.add(new URI("https://goodurl2:8993"));
    revokedEndpoints.add(new URI("https://revokedurl:8993"));
    revokedEndpoints.add(new URI("https://revokedurl2:8993"));
    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    List<URI> ocspServerUrls = new ArrayList<>();
    ocspServerUrls.addAll(unknownEndpoints);
    ocspServerUrls.addAll(revokedEndpoints);
    ocspServerUrls.addAll(goodEndpoints);

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    X509Certificate[] certs = new X509Certificate[] {notTrustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(true));
  }

  @Test
  public void testOcspCheckMultiCertGoodStatus() throws URISyntaxException {
    unknownEndpoints.add(new URI("https://unknownurl:8993"));
    unknownEndpoints.add(new URI("https://unknownurl2:8993"));
    goodEndpoints.add(new URI("https://goodurl:8993"));
    goodEndpoints.add(new URI("https://goodurl2:8993"));
    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    List<URI> ocspServerUrls = new ArrayList<>();
    ocspServerUrls.addAll(unknownEndpoints);
    ocspServerUrls.addAll(goodEndpoints);

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    X509Certificate[] certs = new X509Certificate[] {notTrustedCertX509, trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(true));
  }

  @Test
  public void testOcspCheckMultiCertAllStatuses() throws URISyntaxException {
    unknownEndpoints.add(new URI("https://unknownurl:8993"));
    unknownEndpoints.add(new URI("https://unknownurl2:8993"));
    goodEndpoints.add(new URI("https://goodurl:8993"));
    goodEndpoints.add(new URI("https://goodurl2:8993"));
    revokedEndpoints.add(new URI("https://revokedurl:8993"));
    revokedEndpoints.add(new URI("https://revokedurl2:8993"));
    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    List<URI> ocspServerUrls = new ArrayList<>();
    ocspServerUrls.addAll(unknownEndpoints);
    ocspServerUrls.addAll(goodEndpoints);
    ocspServerUrls.addAll(revokedEndpoints);

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    X509Certificate[] certs = new X509Certificate[] {notTrustedCertX509, trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(false));
  }

  @Test
  public void testOcspCheckNoServerUrls() throws URISyntaxException {
    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(true));
  }

  @Test
  public void testOcspCheckBrokenServerUrls() throws URISyntaxException {
    brokenEndpoints.add(new URI("https://brokenurl:8993"));
    brokenEndpoints.add(new URI("https://brokenurl2:8993"));
    brokenEndpoints.add(new URI("https://brokenurl3:8993"));

    List<URI> ocspServerUrls = new ArrayList<>(brokenEndpoints);

    brokenEndpoints.add(new URI(EMBEDDED_OCSP_SERVER_URL));

    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
    ocspChecker.setOcspEnabled(true);
    ocspChecker.setOcspServerUrls(
        ocspServerUrls.stream().map(URI::toString).collect(Collectors.toList()));

    X509Certificate[] certs = new X509Certificate[] {trustedCertX509};

    assertThat(ocspChecker.passesOcspCheck(certs), is(true));
  }

  @Test
  public void testOcspCheckDisabled() throws Exception {
    OcspChecker ocspChecker = new OcspChecker(factory, eventAdmin);
    ocspChecker.setSecurityLogger(mock(SecurityLogger.class));
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

  private Answer<InputStream> getResourceStreamAsAnswer(String filename) {
    return new Answer<InputStream>() {
      @Override
      public InputStream answer(InvocationOnMock invocationOnMock) {
        return this.getClass().getClassLoader().getResourceAsStream(filename);
      }
    };
  }

  private void assertStatuses(Map<URI, CertificateStatus> ocspStatuses) {
    goodEndpoints.forEach(endpoint -> assertNull(ocspStatuses.get(endpoint)));
    revokedEndpoints.forEach(
        endpoint -> assertThat(ocspStatuses.get(endpoint), instanceOf(RevokedStatus.class)));
    unknownEndpoints.forEach(
        endpoint -> assertThat(ocspStatuses.get(endpoint), instanceOf(UnknownStatus.class)));
    brokenEndpoints.forEach(
        endpoint -> assertThat(ocspStatuses.get(endpoint), instanceOf(UnknownStatus.class)));
  }
}
