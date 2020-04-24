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
package org.codice.ddf.security.crl.generator;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import ddf.security.audit.SecurityLogger;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.platform.util.properties.PropertiesLoader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.osgi.service.event.EventAdmin;

public class CrlGeneratorTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private static final String KEYSTORE_PASS_PROPERTY = "${javax.net.ssl.keyStorePassword}";
  private byte[] demEncodedCrl;
  private EventAdmin eventAdmin = mock(EventAdmin.class);

  @Before
  public void setup() throws Exception {
    CrlGenerator.issuerEncryptionPropertiesLocation =
        getClass().getResource("/issuer/encryption.properties").getPath();
    CrlGenerator.issuerSignaturePropertiesLocation =
        getClass().getResource("/issuer/signature.properties").getPath();
    CrlGenerator.serverEncryptionPropertiesLocation =
        getClass().getResource("/server/encryption.properties").getPath();
    CrlGenerator.serverSignaturePropertiesLocation =
        getClass().getResource("/server/signature.properties").getPath();
    CrlGenerator.crlFileLocation = temporaryFolder.getRoot().toString();
    try (InputStream inputStream =
        new FileInputStream(new File(getClass().getResource("/root.crl").getPath()))) {
      demEncodedCrl = new byte[inputStream.available()];
      inputStream.read(demEncodedCrl);
    }
  }

  @Test
  public void testAddingRemovingCrlProperties() {
    String localCrlPath = "/local/crl/path";
    CrlGenerator crlGenerator = new CrlGenerator(mock(ClientFactoryFactory.class), eventAdmin);
    crlGenerator.setSecurityLogger(mock(SecurityLogger.class));
    crlGenerator.setCrlFileLocationInPropertiesFile(localCrlPath);
    assertTrue(
        PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.issuerEncryptionPropertiesLocation)
            .containsValue(localCrlPath));
    assertTrue(
        PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.issuerSignaturePropertiesLocation)
            .containsValue(localCrlPath));
    assertTrue(
        PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.serverEncryptionPropertiesLocation)
            .containsValue(localCrlPath));
    assertTrue(
        PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.serverSignaturePropertiesLocation)
            .containsValue(localCrlPath));
    // Verify that the system properties aren't converted to their actual values
    assertTrue(
        PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.issuerEncryptionPropertiesLocation)
            .containsValue(KEYSTORE_PASS_PROPERTY));
    assertTrue(
        PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.issuerSignaturePropertiesLocation)
            .containsValue(KEYSTORE_PASS_PROPERTY));
    assertTrue(
        PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.serverEncryptionPropertiesLocation)
            .containsValue(KEYSTORE_PASS_PROPERTY));
    assertTrue(
        PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.serverSignaturePropertiesLocation)
            .containsValue(KEYSTORE_PASS_PROPERTY));
    crlGenerator.removeCrlFileLocationInPropertiesFile();
    assertTrue(
        !PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.issuerEncryptionPropertiesLocation)
            .containsKey(CrlGenerator.CRL_PROPERTY_KEY));
    assertTrue(
        !PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.issuerSignaturePropertiesLocation)
            .containsKey(CrlGenerator.CRL_PROPERTY_KEY));
    assertTrue(
        !PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.serverEncryptionPropertiesLocation)
            .containsKey(CrlGenerator.CRL_PROPERTY_KEY));
    assertTrue(
        !PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.serverSignaturePropertiesLocation)
            .containsKey(CrlGenerator.CRL_PROPERTY_KEY));
    // Verify that the system properties aren't converted to their actual values
    assertTrue(
        PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.issuerEncryptionPropertiesLocation)
            .containsValue(KEYSTORE_PASS_PROPERTY));
    assertTrue(
        PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.issuerSignaturePropertiesLocation)
            .containsValue(KEYSTORE_PASS_PROPERTY));
    assertTrue(
        PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.serverEncryptionPropertiesLocation)
            .containsValue(KEYSTORE_PASS_PROPERTY));
    assertTrue(
        PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.serverSignaturePropertiesLocation)
            .containsValue(KEYSTORE_PASS_PROPERTY));
  }

  @Test
  public void testGettingPemCrlFromUrl() throws Exception {
    ClientFactoryFactory clientFactoryFactory = getCxfClient(getCRL());
    CrlGenerator crlGenerator = new CrlGenerator(clientFactoryFactory, eventAdmin);
    crlGenerator.setSecurityLogger(mock(SecurityLogger.class));
    crlGenerator.setCrlLocationUrl("https://testurl:8993");
    crlGenerator.setCrlByUrlEnabled(true);
    crlGenerator.run();
    byte[] encoded =
        Files.readAllBytes(Paths.get(CrlGenerator.crlFileLocation + CrlGenerator.PEM_CRL));
    assertTrue(Arrays.equals(getCRL(), encoded));
  }

  @Test
  public void testGettingDemCrlFromUrl() throws Exception {
    ClientFactoryFactory clientFactoryFactory = getCxfClient(demEncodedCrl);
    CrlGenerator crlGenerator = new CrlGenerator(clientFactoryFactory, eventAdmin);
    crlGenerator.setSecurityLogger(mock(SecurityLogger.class));
    crlGenerator.setCrlLocationUrl("https://testurl:8993");
    crlGenerator.setCrlByUrlEnabled(true);
    crlGenerator.run();
    InputStream inputStream =
        new FileInputStream(
            new File(Paths.get(CrlGenerator.crlFileLocation + CrlGenerator.DEM_CRL).toString()));
    byte[] encoded = new byte[inputStream.available()];
    inputStream.read(encoded);
    assertTrue(Arrays.equals(demEncodedCrl, encoded));
  }

  @Test
  public void testHttpUrl() {
    ClientFactoryFactory clientFactoryFactory = getCxfClient(demEncodedCrl);
    CrlGenerator crlGenerator = new CrlGenerator(clientFactoryFactory, eventAdmin);
    crlGenerator.setSecurityLogger(mock(SecurityLogger.class));
    crlGenerator.setCrlLocationUrl("http://testurl:8993");
    crlGenerator.setCrlByUrlEnabled(true);
    crlGenerator.run();
    assertTrue(
        !PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.issuerEncryptionPropertiesLocation)
            .containsKey(CrlGenerator.CRL_PROPERTY_KEY));
    assertTrue(
        !PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.issuerSignaturePropertiesLocation)
            .containsKey(CrlGenerator.CRL_PROPERTY_KEY));
    assertTrue(
        !PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.serverEncryptionPropertiesLocation)
            .containsKey(CrlGenerator.CRL_PROPERTY_KEY));
    assertTrue(
        !PropertiesLoader.getInstance()
            .loadProperties(CrlGenerator.serverSignaturePropertiesLocation)
            .containsKey(CrlGenerator.CRL_PROPERTY_KEY));
  }

  private ClientFactoryFactory getCxfClient(byte[] message) {
    Response response = mock(Response.class);
    when(response.getEntity()).thenReturn(new ByteArrayInputStream(message));
    WebClient webClient = mock(WebClient.class);
    when(webClient.get()).thenReturn(response);
    SecureCxfClientFactory secureCxfClientFactory = mock(SecureCxfClientFactory.class);
    when(secureCxfClientFactory.getWebClient()).thenReturn(webClient);
    ClientFactoryFactory clientFactoryFactory = mock(ClientFactoryFactory.class);
    when(clientFactoryFactory.getSecureCxfClientFactory("https://testurl:8993", WebClient.class))
        .thenReturn(secureCxfClientFactory);
    return clientFactoryFactory;
  }

  @Test
  public void testSetCrlLocationUrl() {
    ClientFactoryFactory clientFactoryFactory = getCxfClient(demEncodedCrl);
    ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    CrlGenerator crlGenerator = new CrlGenerator(clientFactoryFactory, eventAdmin, scheduler);
    crlGenerator.setSecurityLogger(mock(SecurityLogger.class));
    crlGenerator.setCrlByUrlEnabled(true);

    when(scheduler.scheduleAtFixedRate(crlGenerator, 0, 30, TimeUnit.MINUTES)).thenReturn(null);
    crlGenerator.setCrlLocationUrl("https://testurl:8993");
    Mockito.verify(scheduler, times(1)).scheduleAtFixedRate(crlGenerator, 0, 30, TimeUnit.MINUTES);

    // Will set handler value (not null)
    ScheduledFuture handler = mock(ScheduledFuture.class);
    when(scheduler.scheduleAtFixedRate(crlGenerator, 0, 30, TimeUnit.MINUTES)).thenReturn(handler);
    crlGenerator.setCrlLocationUrl("https://testurl:8993");

    // Next time we call setCrlLocationUrl, the first handler should be chanced
    crlGenerator.setCrlLocationUrl("https://testurl:8993");
    Mockito.verify(handler, times(1)).cancel(false);
  }

  @Test
  public void testSetCrlByUrlEnabled() {
    ClientFactoryFactory clientFactoryFactory = getCxfClient(demEncodedCrl);
    ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    CrlGenerator crlGenerator = new CrlGenerator(clientFactoryFactory, eventAdmin, scheduler);
    crlGenerator.setSecurityLogger(mock(SecurityLogger.class));

    crlGenerator.setCrlByUrlEnabled(false);
    Mockito.verify(scheduler, times(0)).submit(any(Callable.class));

    crlGenerator.setCrlByUrlEnabled(true);
    Mockito.verify(scheduler, times(0)).submit(any(Callable.class));
  }

  private static byte[] getCRL() {
    return ("-----BEGIN X509 CRL-----\n"
            + "MIICLzCCAZgCAQEwDQYJKoZIhvcNAQEFBQAwdzELMAkGA1UEBhMCVVMxCzAJBgNV\n"
            + "BAgMAkFaMQwwCgYDVQQKDANEREYxDDAKBgNVBAsMA0RldjEZMBcGA1UEAwwQRERG\n"
            + "IERlbW8gUm9vdCBDQTEkMCIGCSqGSIb3DQEJARYVZGRmcm9vdGNhQGV4YW1wbGUu\n"
            + "b3JnFw0xNzA4MjExODU1MjVaGA8yMTE3MDgwMTE4NTUyNVowHDAaAgkAjNzgVisi\n"
            + "n2YXDTE3MDgwMTE4NTUyNVqggcwwgckwgbkGA1UdIwSBsTCBroAU4VTHl98Kwr+p\n"
            + "X3heOwsr5EgXvcahgYqkgYcwgYQxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJBWjEM\n"
            + "MAoGA1UEChMDRERGMQwwCgYDVQQLEwNEZXYxGTAXBgNVBAMTEERERiBEZW1vIFJv\n"
            + "b3QgQ0ExMTAvBgkqhkiG9w0BCQEWImVtYWlsQWRkcmVzcz1kZGZyb290Y2FAZXhh\n"
            + "bXBsZS5vcmeCCQC9D0C4n4h0YDALBgNVHRQEBAICEAEwDQYJKoZIhvcNAQEFBQAD\n"
            + "gYEAbVZ+u1563OiYSjHctPYNWIlYConwuGXNVcSAb1ykSyBOukBobKQvEb3YWS+B\n"
            + "lFLCLzwwWNSzXOsK8gqr5+wxL7sU/F9kBP5QS/fIHaPXGNY05wslialkrJ03BUNd\n"
            + "jlL6NAAwZkyl9y6XoDAZs7NI7kXCfYHLO0VUyiC1Lka55Rs=\n"
            + "-----END X509 CRL-----")
        .getBytes();
  }
}
