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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import ddf.security.audit.SecurityLogger;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Properties;
import org.junit.Test;

public class CrlCheckerTest {

  @Test
  public void testDisabledCrlBothCertsPass() throws CertificateException {

    CrlChecker crlChecker = getConfiguredCrlChecker("encryption-crl-none.properties");

    // First cert
    String certificateString = getRevokedCert();
    X509Certificate[] certs = extractX509CertsFromString(certificateString);
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(true));

    // Second cert
    certificateString = getUnrevokedCert();
    certs = extractX509CertsFromString(certificateString);
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(true));
  }

  @Test
  public void testEnabledEmptyCrlFileBothCertsPass() throws CertificateException {

    CrlChecker crlChecker = getConfiguredCrlChecker("encryption-crl-valid.properties");

    // First cert
    String certificateString = getRevokedCert();
    X509Certificate[] certs = extractX509CertsFromString(certificateString);
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(true));

    // Second cert
    certificateString = getUnrevokedCert();
    certs = extractX509CertsFromString(certificateString);
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(true));
  }

  @Test
  public void testEnabledCrlFileRevokedCertFails() throws CertificateException {

    CrlChecker crlChecker = getConfiguredCrlChecker("encryption-crl-revoked.properties");

    // Revoked cert
    String certificateString = getRevokedCert();
    X509Certificate[] certs = extractX509CertsFromString(certificateString);
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(false));
  }

  @Test
  public void testEnabledCrlFileUnRevokedCertPasses() throws CertificateException {

    CrlChecker crlChecker = getConfiguredCrlChecker("encryption-crl-revoked.properties");

    // Unrevoked cert
    String certificateString = getUnrevokedCert();
    X509Certificate[] certs = extractX509CertsFromString(certificateString);
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(true));
  }

  @Test
  public void testEnabledCrlFileNullCertsPass() throws CertificateException {

    CrlChecker crlChecker = getConfiguredCrlChecker("encryption-crl-revoked.properties");

    // Null cert
    X509Certificate[] certs = null;
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(true));
  }

  @Test
  public void testEnabledCrlFileEmptyCertsPass() throws CertificateException {

    CrlChecker crlChecker = getConfiguredCrlChecker("encryption-crl-revoked.properties");

    // Empty cert
    X509Certificate[] certs = new X509Certificate[0];
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(true));
  }

  @Test
  public void testGetPropertiesFailsBothCertsPass() throws CertificateException {

    // should be unable to read default location during unit testing
    CrlChecker crlChecker = new CrlChecker(mock(SecurityLogger.class));
    crlChecker.setCrlLocation(null);

    // First cert
    String certificateString = getRevokedCert();
    X509Certificate[] certs = extractX509CertsFromString(certificateString);
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(true));

    // Second cert
    certificateString = getUnrevokedCert();
    certs = extractX509CertsFromString(certificateString);
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(true));
  }

  @Test
  public void testEnabledCrlFromURLRevokedCertFails() throws CertificateException {

    CrlChecker crlChecker = getConfiguredCrlChecker("encryption-crl-none.properties");

    // Get a URL path for the file, this will generate a URL
    String urlPath = ClassLoader.getSystemResource("crl-revoked.pem").toString();
    crlChecker.setCrlLocation(urlPath);

    // Cert should fail
    String certificateString = getRevokedCert();
    X509Certificate[] certs = extractX509CertsFromString(certificateString);
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(false));
  }

  @Test
  public void testEnabledCrlFromURLUnRevokedCertPasses() throws CertificateException {

    CrlChecker crlChecker = getConfiguredCrlChecker("encryption-crl-none.properties");

    // Get a URL path for the file, this will generate a URL
    String urlPath = ClassLoader.getSystemResource("crl-revoked.pem").toString();
    crlChecker.setCrlLocation(urlPath);

    // Cert should pass
    String certificateString = getUnrevokedCert();
    X509Certificate[] certs = extractX509CertsFromString(certificateString);
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(true));
  }

  @Test
  public void testEnabledEmptyCrlFromURLRevokedCertPasses() throws CertificateException {

    CrlChecker crlChecker = getConfiguredCrlChecker("encryption-crl-none.properties");

    // Get a URL path for the file, this will generate a URL
    String urlPath = ClassLoader.getSystemResource("crl-valid.pem").toString();
    crlChecker.setCrlLocation(urlPath);

    // Cert should pass
    String certificateString = getRevokedCert();
    X509Certificate[] certs = extractX509CertsFromString(certificateString);
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(true));
  }

  @Test
  public void testEnabledEmptyCrlFromURLUnRevokedCertPasses() throws CertificateException {

    CrlChecker crlChecker = getConfiguredCrlChecker("encryption-crl-none.properties");

    // Get a URL path for the file, this will generate a URL
    String urlPath = ClassLoader.getSystemResource("crl-valid.pem").toString();
    crlChecker.setCrlLocation(urlPath);

    // Cert should pass
    String certificateString = getUnrevokedCert();
    X509Certificate[] certs = extractX509CertsFromString(certificateString);
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(true));
  }

  @Test
  public void testCrlFromUrlDoesNotExist() throws CertificateException {

    // Start with valid CRL with revoked certs
    CrlChecker crlChecker = getConfiguredCrlChecker("encryption-crl-revoked.properties");

    // URL does not contain a CRL
    String urlPath = "http://example.com/";
    crlChecker.setCrlLocation(urlPath);

    // Cert should fail because existing crl was maintained
    String certificateString = getRevokedCert();
    X509Certificate[] certs = extractX509CertsFromString(certificateString);
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(false));
  }

  @Test
  public void testCrlFromUrlDoesNotResolve() throws CertificateException {

    // Start with valid CRL with revoked certs
    CrlChecker crlChecker = getConfiguredCrlChecker("encryption-crl-revoked.properties");

    // URL does not resolve
    String urlPath = "http://example.com/nocrl.pem";
    crlChecker.setCrlLocation(urlPath);

    // Cert should fail because the existing crl was maintained
    String certificateString = getRevokedCert();
    X509Certificate[] certs = extractX509CertsFromString(certificateString);
    assertThat(crlChecker.passesCrlCheck(certs), equalTo(false));
  }

  private CrlChecker getConfiguredCrlChecker(String encryptionProperties) {
    CrlChecker crlChecker = new CrlChecker(mock(SecurityLogger.class));
    Properties prop = crlChecker.loadProperties(encryptionProperties);
    String crlPropertyValue = prop.getProperty(CrlChecker.CRL_PROPERTY_KEY);

    // Prevents a null pointer in the unit tests when appending the unit test's getResource path
    if (crlPropertyValue == null) {
      crlChecker.setCrlLocation(null);
    } else {
      URL url = crlChecker.urlFromPath(crlPropertyValue);
      String crlPath;

      // If this isn't a URL get the absolute path
      if (url != null) {
        crlPath = crlPropertyValue;
      } else {
        String crlRelativePath = "/" + prop.getProperty(CrlChecker.CRL_PROPERTY_KEY);
        crlPath = PKIHandlerTest.class.getResource(crlRelativePath).getPath();
      }
      crlChecker.setCrlLocation(crlPath);
    }

    return crlChecker;
  }

  /**
   * Exctracts list of X509 certs from a given cert string
   *
   * @param certString Certificate string
   * @return List of X509 certs in the string
   */
  private X509Certificate[] extractX509CertsFromString(String certString)
      throws CertificateException {
    InputStream stream =
        new ByteArrayInputStream(Base64.getMimeDecoder().decode(certString.getBytes()));
    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    X509Certificate cert = (X509Certificate) factory.generateCertificate(stream);
    X509Certificate[] certs = new X509Certificate[1];
    certs[0] = cert;

    return certs;
  }

  /**
   * Returns a string of a cert that is listed in the crl-revoked.pem CRL
   *
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
   *
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
