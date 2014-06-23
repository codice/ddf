/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.security.handler.pki;

import org.jasypt.contrib.org.apache.commons.codec_1_3.binary.Base64;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.junit.Test;

import java.security.cert.X509Certificate;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestPKIHandler {

    /**
     * This test ensures the proper functionality of PKIHandler's method,
     * getNormalizedToken(), when given a valid HTTPServletRequest.
     */
    @Test
    public void testGetNormalizedTokenSuccess() throws java.security.cert.CertificateException {
        PKIHandler handler = new PKIHandler();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

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

        InputStream stream = new ByteArrayInputStream(
                Base64.decodeBase64(certificateString.getBytes()));
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate)factory.generateCertificate(stream);
        X509Certificate [] certs = new X509Certificate[1];
        certs[0] = cert;
        when(request.getAttribute(("java.servlet.request.X509Certificate"))).thenReturn(certs);

        /**
         * Note that the getNormalizedToken() method for PKI handlers do not
         * use the resolve tag.
         */
        handler.init();
        HandlerResult result = null;
        try {
            result = handler.getNormalizedToken(request, response, chain, true);
        } catch (ServletException e) {
            e.printStackTrace();
        }

        assertNotNull(result);
        assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
    }

    /**
     * This test ensures the proper functionality of PKIHandler's method,
     * getNormalizedToken(), when given a valid HTTPServletRequest with
     * invalid data.
     */
    @Test
    public void testGetNormalizedTokenFailureNoCertBytes() throws java.security.cert.CertificateException {
        PKIHandler handler = new PKIHandler();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

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

        InputStream stream = new ByteArrayInputStream(
                Base64.decodeBase64(certificateString.getBytes()));
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate)factory.generateCertificate(stream);
        X509Certificate [] certs = new X509Certificate[1];
        certs[0] = cert;
        when(request.getAttribute(("java.servlet.request.X509Certificate"))).thenReturn(certs);

        /**
         * Note that the getNormalizedToken() method for PKI handlers do not
         * use the resolve tag.
         */
        HandlerResult result = null;
        try {
            result = handler.getNormalizedToken(request, response, chain, true);
        } catch (ServletException e) {
            e.printStackTrace();
        }

        assertNotNull(result);
        assertEquals(HandlerResult.Status.NO_ACTION, result.getStatus());
    }

    /**
     * This test ensures the proper functionality of PKIHandler's method,
     * getNormalizedToken(), when given an invalid HTTPServletRequest.
     */
    @Test
    public void testGetNormalizedTokenFailureNoCerts() {
        PKIHandler handler = new PKIHandler();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getAttribute("java.servlet.request.X509Certificate")).thenReturn(null);

        /**
         * Note that the getNormalizedToken() method for PKI handlers do not
         * use the resolve tag.
         */
        HandlerResult result = null;
        try {
            result = handler.getNormalizedToken(request, response, chain, true);
        } catch (ServletException e) {
            e.printStackTrace();
        }

        assertNotNull(result);
        assertEquals(HandlerResult.Status.NO_ACTION, result.getStatus());
    }
}