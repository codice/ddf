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
package org.codice.ddf.security.sts.crl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.jasypt.contrib.org.apache.commons.codec_1_3.binary.Base64;
import org.junit.Test;

/**
 * Tests the CRL Interceptor by using use cases where a certificate gets revoked
 * or passes.
 *
 *
 */
public class CRLInterceptorTest {

    /**
     * Tests that the interceptor will NOT throw an error if CRL is disabled
     */
    @Test
    public void testRevokedCertWithCrlDisabledPasses() throws CertificateException {
        Message message = createMockMessageWithCert(getRevokedCert());
        CRLInterceptor crlInterceptor = new CRLInterceptor();
        crlInterceptor.crlChecker.setCrlLocation(null);

        crlInterceptor.handleMessage(message);
    }

    /**
     * Tests that the interceptor will NOT throw an error if the cert is not in the CRL and it is enabled
     */
    @Test
    public void testUnrevokedCertWithCrlEnabledPasses() throws CertificateException {
        Message message = createMockMessageWithCert(getUnrevokedCert());
        CRLInterceptor crlInterceptor = getCrlInterceptorWithConfiguredCrl(
                "encryption-crl-revoked.properties");

        crlInterceptor.handleMessage(message);
    }

    /**
     * Tests that the interceptor will throw an error if the cert is in the CRL and it is enabled
     */
    @Test(expected = AccessDeniedException.class)
    public void testRevokedCertWithCrlEnabledFails() throws CertificateException {
        Message message = createMockMessageWithCert(getRevokedCert());
        CRLInterceptor crlInterceptor = getCrlInterceptorWithConfiguredCrl(
                "encryption-crl-revoked.properties");

        crlInterceptor.handleMessage(message);
    }

    /**
     * Creates a mock message with a cert attached
     * @param certificateString The string of the certificate to attach
     * @return A message object to be passed to the CRLInterceptor for testing
     * @throws CertificateException
     */
    private Message createMockMessageWithCert(String certificateString)
            throws CertificateException {
        // create mock objects
        Message message = mock(Message.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(message.get(AbstractHTTPDestination.HTTP_REQUEST)).thenReturn(request);

        // add in certificate
        InputStream stream = new ByteArrayInputStream(
                Base64.decodeBase64(certificateString.getBytes()));
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(stream);
        X509Certificate[] certs = new X509Certificate[] {cert};
        when(request.getAttribute(("javax.servlet.request.X509Certificate"))).thenReturn(certs);

        return message;
    }

    /**
     * Creates a CRLInterceptor with the CrlChecker inside of it pointing to the specified file
     * @param encryptionProperties the name of the encryption.properties file that points to the CRL file
     * @return a configured CRLInterceptor
     */
    private CRLInterceptor getCrlInterceptorWithConfiguredCrl(String encryptionProperties) {
        CRLInterceptor crlInterceptor = new CRLInterceptor();
        Properties prop = crlInterceptor.crlChecker.loadProperties(encryptionProperties);
        String crlRelativePath = "/" + prop.getProperty(crlInterceptor.crlChecker.CRL_PROPERTY_KEY);
        String crlAbsolutePath = CRLInterceptorTest.class.getResource(crlRelativePath).getPath();
        crlInterceptor.crlChecker.setCrlLocation(crlAbsolutePath);

        return crlInterceptor;
    }

    /**
     * Returns a string of a cert that is listed in the crl-revoked.pem CRL
     * @return Cert String
     */
    private String getRevokedCert() {
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

    /**
     * Returns a string of a cert that is NOT listed in the crl-revoked.pem CRL
     * @return Cert String
     */
    private String getUnrevokedCert() {
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
        return certificateString;
    }
}
