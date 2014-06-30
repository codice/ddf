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
package org.codice.ddf.security.sts.crl;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the CRL Interceptor by using use cases where a certificate gets revoked
 * or passes.
 * 
 * 
 */
public class CRLInterceptorTest {

    private static final String REVOKED_CERT = "/certs/ia-bad.crt";

    private static final String VALID_CERT = "/certs/ia.crt";

    private static final String CRL_LOCATION = CRLInterceptorTest.class.getResource(
            "/certs/root.crl").getFile();

    private static final Logger LOGGER = LoggerFactory.getLogger(CRLInterceptorTest.class);

    // START Error cases (calling interceptor should throw AccessDeniedException)
    
    /**
     * Verifies that a certificate designated as revoked in the CRL is properly
     * caught in the interceptor.
     * 
     */
    @Test(expected = AccessDeniedException.class)
    public void testDenyCertificate() {
        Message message = null;
        try {
            message = createMockMessageWithCert(REVOKED_CERT);
        } catch (CertificateException ce) {
            LOGGER.error(
                    "Could not create certificate objects from files located in test, failing test.",
                    ce);
            fail();
        }

        callNewInterceptor(CRL_LOCATION, true, message);

    }
    
    /**
     * Tests that the interceptor will deny access if it is enabled and there is
     * no CRL file set.
     */
    @Test(expected = AccessDeniedException.class)
    public void testDenyNoCRL() {
        Message message = null;
        try {
            message = createMockMessageWithCert(VALID_CERT);
        } catch (CertificateException ce) {
            LOGGER.error(
                    "Could not create certificate objects from files located in test, failing test.",
                    ce);
            fail();
        }

        // create and call interceptor manually (without setting the CRL)
        CRLInterceptor interceptor = new CRLInterceptor();
        interceptor.setIsEnabled(true);
        interceptor.handleMessage(message);
    }

    /**
     * Tests that the interceptor will deny access if it is enabled and there is
     * a bad CRL file set.
     */
    @Test(expected = AccessDeniedException.class)
    public void testDenyBadCRL() {
        Message message = null;
        try {
            message = createMockMessageWithCert(VALID_CERT);
        } catch (CertificateException ce) {
            LOGGER.error(
                    "Could not create certificate objects from files located in test, failing test.",
                    ce);
            fail();
        }

        callNewInterceptor(VALID_CERT, true, message);
    }
    
    // END Error Cases
    
    // START Success Cases

    /**
     * Tests that a certificate not designated as revoked in the CRL is properly
     * let through the interceptor.
     * 
     */
    @Test
    public void testPassCertificate() {
        Message message = null;
        try {
            message = createMockMessageWithCert(VALID_CERT);
        } catch (CertificateException ce) {
            LOGGER.error(
                    "Could not create certificate objects from files located in test, failing test.",
                    ce);
            fail();
        }

        callNewInterceptor(CRL_LOCATION, true, message);
    }

    /**
     * Tests that the interceptor does not error out if no certificate is in the
     * incoming message.
     * 
     */
    @Test
    public void testPassNoCertificate() {

        Message message = mock(Message.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(message.get(AbstractHTTPDestination.HTTP_REQUEST)).thenReturn(request);

        callNewInterceptor(CRL_LOCATION, true, message);

    }

    /**
     * Tests that the interceptor will allow access if it is disabled, even if
     * the certificate is bad or the crl file location is incorrect.
     */
    @Test
    public void testPassBadFilesDisabledCheck() {
        Message message = null;
        try {
            message = createMockMessageWithCert(REVOKED_CERT);
        } catch (CertificateException ce) {
            LOGGER.error(
                    "Could not create certificate objects from files located in test, failing test.",
                    ce);
            fail();
        }

        // bad certificate
        callNewInterceptor(CRL_LOCATION, false, message);
        
        // bad crl file (points to cert instead of crl)
        callNewInterceptor(VALID_CERT, false, message);
    }
    
    // END Success Cases

    private Message createMockMessageWithCert(String certificateFile) throws CertificateException {
        // create mock objects
        Message message = mock(Message.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(message.get(AbstractHTTPDestination.HTTP_REQUEST)).thenReturn(request);

        // add in certificate
        InputStream stream = getClass().getResourceAsStream(certificateFile);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(stream);
        X509Certificate[] certs = new X509Certificate[] {cert};
        when(request.getAttribute(("javax.servlet.request.X509Certificate"))).thenReturn(certs);

        return message;
    }

    private void callNewInterceptor(String crlLocation, boolean isEnabled, Message message) {
        // create and call interceptor
        CRLInterceptor interceptor = new CRLInterceptor();
        interceptor.setCrlLocation(crlLocation);
        interceptor.setIsEnabled(isEnabled);
        interceptor.handleMessage(message);
    }

}
