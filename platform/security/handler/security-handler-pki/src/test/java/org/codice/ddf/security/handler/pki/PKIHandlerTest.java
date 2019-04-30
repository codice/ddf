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
package org.codice.ddf.security.handler.pki;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.OcspService;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.PKIAuthenticationTokenFactory;
import org.junit.Test;

public class PKIHandlerTest {

  /**
   * This test ensures the proper functionality of PKIHandler's method, getNormalizedToken(), when
   * given a valid HTTPServletRequest.
   */
  @Test
  public void testGetNormalizedTokenSuccessNoCrlPki() throws CertificateException {
    PKIHandler handler = getPKIHandlerWithMockedCrl("signature.properties", true);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getAttribute(("javax.servlet.request.X509Certificate")))
        .thenReturn(getTestCerts());

    /** Note that the getNormalizedToken() method for PKI handlers do not use the resolve tag. */
    HandlerResult result = null;
    result = handler.getNormalizedToken(request, response, chain, true);

    assertThat(result, is(notNullValue()));
    assertThat(result.getStatus(), equalTo(HandlerResult.Status.COMPLETED));

    verify(handler.crlChecker).passesCrlCheck(getTestCerts());
  }

  /**
   * This test ensures the proper functionality of PKIHandler's method, getNormalizedToken(), when
   * given a valid HTTPServletRequest and resolve is set to false.
   */
  @Test
  public void testGetNormalizedTokenSuccessNoCrlPkiNoResolveNoResponse()
      throws CertificateException {
    PKIHandler handler = getPKIHandlerWithMockedCrl("signature.properties", true);

    HttpServletRequest request = mock(HttpServletRequest.class);

    when(request.getAttribute(("javax.servlet.request.X509Certificate")))
        .thenReturn(getTestCerts());

    /** Note that the getNormalizedToken() method for PKI handlers do not use the resolve tag. */
    HandlerResult result = null;
    result = handler.getNormalizedToken(request, null, null, false);

    assertThat(result, is(notNullValue()));
    assertThat(result.getStatus(), equalTo(HandlerResult.Status.COMPLETED));

    verify(handler.crlChecker).passesCrlCheck(getTestCerts());
  }

  /**
   * This test ensures the proper functionality of PKIHandler's method, getNormalizedToken(), when
   * given an invalid HTTPServletRequest.
   */
  @Test
  public void testGetNormalizedTokenFailureNoCerts() throws CertificateException {
    PKIHandler handler = getPKIHandlerWithMockedCrl("signature.properties", false);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getAttribute(("javax.servlet.request.X509Certificate"))).thenReturn(null);

    /** Note that the getNormalizedToken() method for PKI handlers do not use the resolve tag. */
    HandlerResult result = null;
    result = handler.getNormalizedToken(request, response, chain, true);

    assertThat(result, is(notNullValue()));
    assertThat(result.getStatus(), equalTo(HandlerResult.Status.NO_ACTION));

    verify(handler.crlChecker, never()).passesCrlCheck(getTestCerts());
  }

  /** Tests that the PKIHandler returns REDIRECTED when the cert fails to pass the CRL check */
  @Test
  public void testGetNormalizedTokenFailsWhenCrlFails() throws CertificateException {
    PKIHandler handler = getPKIHandlerWithMockedCrl("signature.properties", false);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getAttribute(("javax.servlet.request.X509Certificate")))
        .thenReturn(getTestCerts());

    // should return REDIRECTED
    HandlerResult handlerResult = handler.getNormalizedToken(request, response, chain, true);
    assertThat(handlerResult.getStatus(), equalTo(HandlerResult.Status.REDIRECTED));
  }

  /**
   * Tests that the certificate gets through when CRL checking is enabled but the cert is not listed
   * in the CRL
   *
   * @throws java.security.cert.CertificateException
   */
  @Test
  public void testNoActionWhenHttpResponseIsNull() throws CertificateException {

    PKIHandler handler = getPKIHandlerWithMockedCrl("signature.properties", true);

    HttpServletResponse httpResponse = null;
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    FilterChain chain = mock(FilterChain.class);

    HandlerResult result = handler.getNormalizedToken(httpRequest, httpResponse, chain, true);
    assertThat(result.getStatus(), equalTo(HandlerResult.Status.NO_ACTION));

    verify(handler.crlChecker, never()).passesCrlCheck(getTestCerts());
  }

  /**
   * Tests Error Handling
   *
   * @throws java.security.cert.CertificateException
   */
  @Test
  public void testErrorHandling() throws CertificateException {

    PKIHandler handler = getPKIHandlerWithMockedCrl("signature.properties", true);

    HttpServletResponse httpResponse = mock(HttpServletResponse.class);
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    FilterChain filterChain = mock(FilterChain.class);

    HandlerResult result = handler.handleError(httpRequest, httpResponse, filterChain);

    assertThat(result.getStatus(), equalTo(HandlerResult.Status.NO_ACTION));
  }

  /**
   * Creates a PKIHandler with a mocked CrlChecker that always returns true or false
   *
   * @param returnedValue Boolean value that the mocked CrlChecker will always return
   * @return A PKIHandler with a mocked CrlChecker
   */
  private PKIHandler getPKIHandlerWithMockedCrl(String signatureProperties, boolean returnedValue) {
    PKIHandler handler = new PKIHandler();
    PKIAuthenticationTokenFactory tokenFactory = new PKIAuthenticationTokenFactory();
    tokenFactory.setSignaturePropertiesPath(signatureProperties);
    tokenFactory.init();
    handler.setTokenFactory(tokenFactory);

    OcspService ocspService = mock(OcspService.class);
    when(ocspService.passesOcspCheck(any())).thenReturn(returnedValue);
    handler.setOcspService(ocspService);

    CrlChecker crlChecker = mock(CrlChecker.class);
    when(crlChecker.passesCrlCheck(any())).thenReturn(returnedValue);
    handler.crlChecker = crlChecker;

    return handler;
  }

  private X509Certificate[] getTestCerts() throws CertificateException {
    String certificateString = getTestCertString();

    InputStream stream =
        new ByteArrayInputStream(Base64.getMimeDecoder().decode(certificateString.getBytes()));
    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    X509Certificate cert = (X509Certificate) factory.generateCertificate(stream);
    X509Certificate[] certs = new X509Certificate[1];
    certs[0] = cert;

    return certs;
  }

  private String getTestCertString() {
    String certificateString =
        "MIIDEzCCAnygAwIBAgIJAIzc4FYrIp9mMA0GCSqGSIb3DQEBBQUAMHcxCzAJBgNV\n"
            + "BAYTAlVTMQswCQYDVQQIDAJBWjEMMAoGA1UECgwDRERGMQwwCgYDVQQLDANEZXYx\n"
            + "GTAXBgNVBAMMEERERiBEZW1vIFJvb3QgQ0ExJDAiBgkqhkiG9w0BCQEWFWRkZnJv\n"
            + "b3RjYUBleGFtcGxlLm9yZzAeFw0xNDEyMTAyMTU4MThaFw0xNTEyMTAyMTU4MTha\n"
            + "MIGDMQswCQYDVQQGEwJVUzELMAkGA1UECAwCQVoxETAPBgNVBAcMCEdvb2R5ZWFy\n"
            + "MQwwCgYDVQQKDANEREYxDDAKBgNVBAsMA0RldjESMBAGA1UEAwwJbG9jYWxob3N0\n"
            + "MSQwIgYJKoZIhvcNAQkBFhVsb2NhbGhvc3RAZXhhbXBsZS5vcmcwgZ8wDQYJKoZI\n"
            + "hvcNAQEBBQADgY0AMIGJAoGBAMeCyNZbCTZphHQfB5g8FrgBq1RYzV7ikVw/pVGk\n"
            + "z8gx3l3A99s8WtA4mRAeb6n0vTR9yNBOekW4nYOiEOq//YTi/frI1kz0QbEH1s2c\n"
            + "I5nFButabD3PYGxUSuapbc+AS7+Pklr0TDI4MRzPPkkTp4wlORQ/a6CfVsNr/mVg\n"
            + "L2CfAgMBAAGjgZkwgZYwCQYDVR0TBAIwADAnBglghkgBhvhCAQ0EGhYYRk9SIFRF\n"
            + "U1RJTkcgUFVSUE9TRSBPTkxZMB0GA1UdDgQWBBSA95QIMyBAHRsd0R4s7C3BreFr\n"
            + "sDAfBgNVHSMEGDAWgBThVMeX3wrCv6lfeF47CyvkSBe9xjAgBgNVHREEGTAXgRVs\n"
            + "b2NhbGhvc3RAZXhhbXBsZS5vcmcwDQYJKoZIhvcNAQEFBQADgYEAtRUp7fAxU/E6\n"
            + "JD2Kj/+CTWqu8Elx13S0TxoIqv3gMoBW0ehyzEKjJi0bb1gUxO7n1SmOESp5sE3j\n"
            + "GTnh0GtYV0D219z/09n90cd/imAEhknJlayyd0SjpnaL9JUd8uYxJexy8TJ2sMhs\n"
            + "GAZ6EMTZCfT9m07XduxjsmDz0hlSGV0";
    return certificateString;
  }
}
