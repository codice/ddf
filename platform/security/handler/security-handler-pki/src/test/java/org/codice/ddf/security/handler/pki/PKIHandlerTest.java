/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.handler.pki;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.PKIAuthenticationTokenFactory;
import org.jasypt.contrib.org.apache.commons.codec_1_3.binary.Base64;
import org.junit.Test;

public class PKIHandlerTest {

    /**
     * This test ensures the proper functionality of PKIHandler's method,
     * getNormalizedToken(), when given a valid HTTPServletRequest.
     */
    @Test
    public void testGetNormalizedTokenSuccessNoCRL()
            throws java.security.cert.CertificateException, ServletException {
        for (AbstractPKIHandler handler : new AbstractPKIHandler[] {new PKIHandler(),
                new WssPKIHandler()}) {
            PKIAuthenticationTokenFactory tokenFactory = new PKIAuthenticationTokenFactory();
            tokenFactory.setSignaturePropertiesPath("signatures.properties");
            tokenFactory.init();
            handler.setTokenFactory(tokenFactory);

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain chain = mock(FilterChain.class);

            String certificateString = getLocalhostCert();

            InputStream stream = new ByteArrayInputStream(
                    Base64.decodeBase64(certificateString.getBytes()));
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) factory.generateCertificate(stream);
            X509Certificate[] certs = new X509Certificate[1];
            certs[0] = cert;
            when(request.getAttribute(("javax.servlet.request.X509Certificate"))).thenReturn(certs);

            /**
             * Note that the getNormalizedToken() method for PKI handlers do not
             * use the resolve tag.
             */
            HandlerResult result = null;
            result = handler.getNormalizedToken(request, response, chain, true);

            assertNotNull(result);
            assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
        }
    }

    /**
     * This test ensures the proper functionality of PKIHandler's method,
     * getNormalizedToken(), when given a valid HTTPServletRequest with
     * invalid data.
     */
    @Test
    public void testGetNormalizedTokenFailureNoCertBytes()
            throws java.security.cert.CertificateException, ServletException {
        PKIHandler handler = new PKIHandler();
        PKIAuthenticationTokenFactory tokenFactory = new PKIAuthenticationTokenFactory();
        tokenFactory.setSignaturePropertiesPath("signatures.properties");
        tokenFactory.init();
        handler.setTokenFactory(tokenFactory);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        /**
         * Note that the getNormalizedToken() method for PKI handlers do not
         * use the resolve tag.
         */
        HandlerResult result = null;
        result = handler.getNormalizedToken(request, response, chain, true);

        assertNotNull(result);
        assertEquals(HandlerResult.Status.NO_ACTION, result.getStatus());
    }

    /**
     * This test ensures the proper functionality of PKIHandler's method,
     * getNormalizedToken(), when given an invalid HTTPServletRequest.
     */
    @Test
    public void testGetNormalizedTokenFailureNoCerts() throws ServletException {
        PKIHandler handler = new PKIHandler();
        PKIAuthenticationTokenFactory tokenFactory = new PKIAuthenticationTokenFactory();
        tokenFactory.setSignaturePropertiesPath("signatures.properties");
        tokenFactory.init();
        handler.setTokenFactory(tokenFactory);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(null);

        /**
         * Note that the getNormalizedToken() method for PKI handlers do not
         * use the resolve tag.
         */
        HandlerResult result = null;
        result = handler.getNormalizedToken(request, response, chain, true);

        assertNotNull(result);
        assertEquals(HandlerResult.Status.NO_ACTION, result.getStatus());
    }

    /**
     * Tests that CRL checking becomes enabled when given a valid location
     */
    @Test
    public void testCRLEnabledWhenPropertyIsSpecified() {
        PKIHandler handler = new PKIHandler();
        assertThat(handler.getIsEnabled(), is(false));
        Properties prop = handler.loadProperties("encryption-crl-valid.properties");
        String crlRelativePath = "/" + prop.getProperty(handler.CRL_PROPERTY_KEY);
        String crlAbsolutePath = PKIHandlerTest.class.getResource(crlRelativePath).getPath();
        handler.setCrlLocation(crlAbsolutePath);
        assertThat(handler.getIsEnabled(), is(true));
    }

    /**
     * Test that CRL checking is not enabled when the CRL property is not specified
     */
    @Test
    public void testCRLDisabledWhenPropertyIsNotSpecified() {
        PKIHandler handler = new PKIHandler();
        assertThat(handler.getIsEnabled(), is(false));
        Properties prop = handler.loadProperties("encryption-crl-none.properties");
        String crlRelativePath = prop.getProperty(handler.CRL_PROPERTY_KEY);
        assertNull(crlRelativePath);
        handler.setCrlLocation(crlRelativePath);
        assertThat(handler.getIsEnabled(), is(false));
    }

    /**
     * Tests that the certificate gets through when CRL checking is enabled but
     * the cert is not listed in the CRL
     *
     * @throws java.security.cert.CertificateException
     * @throws ServletException
     */
    @Test
    public void testCertPassesCRLCheckWhenNotListedInCRL()
            throws java.security.cert.CertificateException, ServletException {

        PKIHandler handler = configurePKIHandlerWithCRL("signature.properties",
                "encryption-crl-valid.properties");

        String certificateString = getLocalhostCert();

        InputStream stream = new ByteArrayInputStream(
                Base64.decodeBase64(certificateString.getBytes()));
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(stream);
        X509Certificate[] certs = new X509Certificate[1];
        certs[0] = cert;

        assertThat(handler.passesCRL(certs), is(true));
    }

    /**
     * Tests that the certificate is not let through when it is listed in the CRL
     *
     * @throws java.security.cert.CertificateException
     * @throws ServletException
     */
    @Test
    public void testCertFailsCRLCheckWhenListedInCRL()
            throws java.security.cert.CertificateException, ServletException {

        PKIHandler handler = configurePKIHandlerWithCRL("signature.properties",
                "encryption-crl-revoked.properties");

        String certificateString = getLocalhostCert();

        InputStream stream = new ByteArrayInputStream(
                Base64.decodeBase64(certificateString.getBytes()));
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(stream);
        X509Certificate[] certs = new X509Certificate[1];
        certs[0] = cert;

        assertThat(handler.passesCRL(certs), is(false));
    }

    @Test
    public void testHandlerCompletedWhenPassingCRL()
            throws java.security.cert.CertificateException, ServletException {

        PKIHandler handler = configurePKIHandlerWithCRL("signature.properties",
                "encryption-crl-valid.properties");

        String certificateString = getLocalhostCert();

        InputStream stream = new ByteArrayInputStream(
                Base64.decodeBase64(certificateString.getBytes()));
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(stream);
        X509Certificate[] certs = new X509Certificate[1];
        certs[0] = cert;

        HandlerResult handlerResult = new HandlerResult();
        BaseAuthenticationToken token = new BaseAuthenticationToken(new Object(), "test",
                new Object());

        HttpServletResponse httpResponse = mock(HttpServletResponse.class);

        handlerResult = handler.checkAgainstCRL(httpResponse, token, certs, handlerResult);
        assertThat(handlerResult.getStatus(), equalTo(HandlerResult.Status.COMPLETED));
    }

    @Test
    public void testHandlerRedirectedWhenFailingCRL()
            throws java.security.cert.CertificateException, ServletException {

        PKIHandler handler = configurePKIHandlerWithCRL("signature.properties",
                "encryption-crl-revoked.properties");

        String certificateString = getLocalhostCert();

        InputStream stream = new ByteArrayInputStream(
                Base64.decodeBase64(certificateString.getBytes()));
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(stream);
        X509Certificate[] certs = new X509Certificate[1];
        certs[0] = cert;

        HandlerResult handlerResult = new HandlerResult();
        BaseAuthenticationToken token = new BaseAuthenticationToken(new Object(), "test",
                new Object());

        HttpServletResponse httpResponse = mock(HttpServletResponse.class);

        handlerResult = handler.checkAgainstCRL(httpResponse, token, certs, handlerResult);
        assertThat(handlerResult.getStatus(), equalTo(HandlerResult.Status.REDIRECTED));
    }

    @Test
    public void testPassesCRLCheckWhenNoCertsArePresent()
            throws java.security.cert.CertificateException, ServletException {

        PKIHandler handler = configurePKIHandlerWithCRL("signature.properties",
                "encryption-crl-revoked.properties");

        X509Certificate[] certs = null;

        assertThat(handler.passesCRL(certs), is(true));
    }

    @Test
    public void testPassesCRLCheckWhenNoCRLIsDefined()
            throws java.security.cert.CertificateException, ServletException {

        PKIHandler handler = new PKIHandler();
        PKIAuthenticationTokenFactory tokenFactory = new PKIAuthenticationTokenFactory();
        tokenFactory.setSignaturePropertiesPath("signature.properties");
        tokenFactory.init();
        handler.setTokenFactory(tokenFactory);

        // handler CRL is not set -- it will default to null and isEnabled to false
        assertThat(handler.getIsEnabled(), is(false));

        String certificateString = getLocalhostCert();

        InputStream stream = new ByteArrayInputStream(
                Base64.decodeBase64(certificateString.getBytes()));
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(stream);
        X509Certificate[] certs = new X509Certificate[1];
        certs[0] = cert;

        assertThat(handler.passesCRL(certs), is(true));
    }

    private PKIHandler configurePKIHandlerWithCRL(String signatureProperties,
            String encryptionProperties) {
        PKIHandler handler = new PKIHandler();
        PKIAuthenticationTokenFactory tokenFactory = new PKIAuthenticationTokenFactory();
        tokenFactory.setSignaturePropertiesPath(signatureProperties);
        tokenFactory.init();
        handler.setTokenFactory(tokenFactory);

        Properties prop = handler.loadProperties(encryptionProperties);
        String crlRelativePath = "/" + prop.getProperty(handler.CRL_PROPERTY_KEY);
        String crlAbsolutePath = PKIHandlerTest.class.getResource(crlRelativePath).getPath();
        handler.setCrlLocation(crlAbsolutePath);
        assertThat(handler.getIsEnabled(), is(true));

        return handler;
    }

    private String getLocalhostCert() {
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
