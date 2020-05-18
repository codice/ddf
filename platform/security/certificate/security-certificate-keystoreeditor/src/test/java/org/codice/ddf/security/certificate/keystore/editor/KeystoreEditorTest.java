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
package org.codice.ddf.security.certificate.keystore.editor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.audit.SecurityLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class KeystoreEditorTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  File keyStoreFile = null;

  File trustStoreFile = null;

  File pkcs12StoreFile = null;

  File crtFile = null;

  File localhostCrtFile = null;

  File chainFile = null;

  File keyFile = null;

  File pemFile = null;

  File derFile = null;

  File localhostKeyFile = null;

  File jksFile = null;

  File p7bFile = null;

  File badFile = null;

  File systemKeystoreFile = null;

  File systemTruststoreFile = null;

  String password = "changeit";

  @Before
  public void setup() throws IOException {
    keyStoreFile = temporaryFolder.newFile("keystore.jks");
    trustStoreFile = temporaryFolder.newFile("truststore.jks");

    pkcs12StoreFile = temporaryFolder.newFile("asdf.p12");
    FileOutputStream p12OutStream = new FileOutputStream(pkcs12StoreFile);
    InputStream p12Stream = KeystoreEditor.class.getResourceAsStream("/asdf.p12");
    IOUtils.copy(p12Stream, p12OutStream);

    crtFile = temporaryFolder.newFile("asdf.crt");
    FileOutputStream crtOutStream = new FileOutputStream(crtFile);
    InputStream crtStream = KeystoreEditor.class.getResourceAsStream("/asdf.crt");
    IOUtils.copy(crtStream, crtOutStream);

    localhostCrtFile = temporaryFolder.newFile("localhost-cert.pem");
    FileOutputStream lCrtOutStream = new FileOutputStream(localhostCrtFile);
    InputStream lCrtStream = KeystoreEditor.class.getResourceAsStream("/localhost-cert.pem");
    IOUtils.copy(lCrtStream, lCrtOutStream);

    chainFile = temporaryFolder.newFile("chain.txt");
    FileOutputStream chainOutStream = new FileOutputStream(chainFile);
    InputStream chainStream = KeystoreEditor.class.getResourceAsStream("/chain.txt");
    IOUtils.copy(chainStream, chainOutStream);

    keyFile = temporaryFolder.newFile("asdf.key");
    FileOutputStream keyOutStream = new FileOutputStream(keyFile);
    InputStream keyStream = KeystoreEditor.class.getResourceAsStream("/asdf.key");
    IOUtils.copy(keyStream, keyOutStream);

    localhostKeyFile = temporaryFolder.newFile("localhost-key.pem");
    FileOutputStream lKeyOutStream = new FileOutputStream(localhostKeyFile);
    InputStream lKeyStream = KeystoreEditor.class.getResourceAsStream("/localhost-key.pem");
    IOUtils.copy(lKeyStream, lKeyOutStream);

    jksFile = temporaryFolder.newFile("asdf.jks");
    FileOutputStream jksOutStream = new FileOutputStream(jksFile);
    InputStream jksStream = KeystoreEditor.class.getResourceAsStream("/asdf.jks");
    IOUtils.copy(jksStream, jksOutStream);

    p7bFile = temporaryFolder.newFile("asdf.p7b");
    FileOutputStream p7bOutStream = new FileOutputStream(p7bFile);
    InputStream p7bStream = KeystoreEditor.class.getResourceAsStream("/asdf.p7b");
    IOUtils.copy(p7bStream, p7bOutStream);

    pemFile = temporaryFolder.newFile("asdf.pem");
    FileOutputStream pemOutStream = new FileOutputStream(pemFile);
    InputStream pemStream = KeystoreEditor.class.getResourceAsStream("/asdf.pem");
    IOUtils.copy(pemStream, pemOutStream);

    derFile = temporaryFolder.newFile("asdf.der");
    FileOutputStream derOutStream = new FileOutputStream(derFile);
    InputStream derStream = KeystoreEditor.class.getResourceAsStream("/asdf.der");
    IOUtils.copy(derStream, derOutStream);

    badFile = temporaryFolder.newFile("badfile.pem");
    FileOutputStream badOutStream = new FileOutputStream(badFile);
    InputStream badStream = KeystoreEditor.class.getResourceAsStream("/badfile.pem");
    IOUtils.copy(badStream, badOutStream);

    systemKeystoreFile = temporaryFolder.newFile("serverKeystore.jks");
    FileOutputStream systemKeyOutStream = new FileOutputStream(systemKeystoreFile);
    InputStream systemKeyStream = KeystoreEditor.class.getResourceAsStream("/serverKeystore.jks");
    IOUtils.copy(systemKeyStream, systemKeyOutStream);

    systemTruststoreFile = temporaryFolder.newFile("serverTruststore.jks");
    FileOutputStream systemTrustOutStream = new FileOutputStream(systemTruststoreFile);
    InputStream systemTrustStream =
        KeystoreEditor.class.getResourceAsStream("/serverTruststore.jks");
    IOUtils.copy(systemTrustStream, systemTrustOutStream);

    IOUtils.closeQuietly(p12OutStream);
    IOUtils.closeQuietly(p12Stream);
    IOUtils.closeQuietly(crtOutStream);
    IOUtils.closeQuietly(crtStream);
    IOUtils.closeQuietly(chainOutStream);
    IOUtils.closeQuietly(chainStream);
    IOUtils.closeQuietly(keyOutStream);
    IOUtils.closeQuietly(keyStream);
    IOUtils.closeQuietly(jksOutStream);
    IOUtils.closeQuietly(jksStream);
    IOUtils.closeQuietly(lKeyStream);
    IOUtils.closeQuietly(lKeyOutStream);
    IOUtils.closeQuietly(lCrtStream);
    IOUtils.closeQuietly(lCrtOutStream);
    IOUtils.closeQuietly(p7bStream);
    IOUtils.closeQuietly(p7bOutStream);
    IOUtils.closeQuietly(pemStream);
    IOUtils.closeQuietly(pemOutStream);
    IOUtils.closeQuietly(derStream);
    IOUtils.closeQuietly(derOutStream);
    IOUtils.closeQuietly(badStream);
    IOUtils.closeQuietly(badOutStream);
    IOUtils.closeQuietly(systemKeyStream);
    IOUtils.closeQuietly(systemKeyOutStream);
    IOUtils.closeQuietly(systemTrustStream);
    IOUtils.closeQuietly(systemTrustOutStream);

    System.setProperty(SecurityConstants.KEYSTORE_TYPE, "jks");
    System.setProperty(SecurityConstants.TRUSTSTORE_TYPE, "jks");
    System.setProperty("ddf.home", "");
    System.setProperty(SecurityConstants.KEYSTORE_PATH, keyStoreFile.getAbsolutePath());
    System.setProperty(SecurityConstants.TRUSTSTORE_PATH, trustStoreFile.getAbsolutePath());
    System.setProperty(SecurityConstants.KEYSTORE_PASSWORD, password);
    System.setProperty(SecurityConstants.TRUSTSTORE_PASSWORD, password);
  }

  @Test
  public void testShowCertificateFromUrl()
      throws IOException, KeystoreEditor.KeystoreEditorException {
    KeystoreEditor keystoreEditor = getNonVerifyingKeystoreEditor();
    addCannedCertificatesToTruststore(keystoreEditor);
    byte[] encoded = Base64.getEncoder().encode("https://doesnotexist.codice.org".getBytes());
    List<Map<String, Object>> mapList =
        keystoreEditor.certificateDetails(new String(encoded, "UTF-8"));
    Assert.assertThat(mapList.size(), Is.is(2));
    Assert.assertThat(
        ((Principal) mapList.get(0).get("subjectDn")).getName(),
        Is.is("EMAILADDRESS=asdf@example.com, CN=asdf, O=Example, L=Phoenix, ST=Arizona, C=US"));
    Assert.assertThat(
        ((Principal) mapList.get(1).get("subjectDn")).getName(),
        Is.is("EMAILADDRESS=localhost@example.org, CN=localhost, OU=Dev, O=DDF, ST=AZ, C=US"));
  }

  @Test
  public void testAddCertificateFromUrl()
      throws IOException, KeystoreEditor.KeystoreEditorException {
    KeystoreEditor keystoreEditor = getNonVerifyingKeystoreEditor();
    addCannedCertificatesToTruststore(keystoreEditor);
    byte[] encoded = Base64.getEncoder().encode("https://notarealurlatall.com".getBytes());
    List<Map<String, Object>> mapList =
        keystoreEditor.addTrustedCertificateFromUrl(new String(encoded, "UTF-8"));
    Assert.assertThat(mapList.size(), Is.is(2));
    Assert.assertThat(mapList.get(0).get("success"), Is.is(true));
    Assert.assertThat(mapList.get(1).get("success"), Is.is(true));
  }

  private void addCannedCertificatesToTruststore(KeystoreEditor keystoreEditor)
      throws IOException, KeystoreEditor.KeystoreEditorException {
    FileInputStream fileInputStream = new FileInputStream(crtFile);
    byte[] crtBytes = IOUtils.toByteArray(fileInputStream);
    IOUtils.closeQuietly(fileInputStream);
    keystoreEditor.addTrustedCertificate(
        "asdf",
        password,
        "",
        new String(Base64.getEncoder().encode(crtBytes)),
        KeystoreEditor.PEM_TYPE,
        crtFile.toString());
    fileInputStream = new FileInputStream(localhostCrtFile);
    crtBytes = IOUtils.toByteArray(fileInputStream);
    IOUtils.closeQuietly(fileInputStream);
    keystoreEditor.addTrustedCertificate(
        "localhost",
        password,
        "",
        new String(Base64.getEncoder().encode(crtBytes)),
        KeystoreEditor.PEM_TYPE,
        crtFile.toString());
  }

  private KeystoreEditor getNonVerifyingKeystoreEditor() {
    KeystoreEditor keystoreEditor =
        new KeystoreEditor() {
          SSLSocket createNonVerifyingSslSocket(String decodedUrl)
              throws IOException, KeyStoreException {
            SSLSession sslSession = mock(SSLSession.class);
            SSLSocket sslSocket = mock(SSLSocket.class);
            when(sslSocket.getSession()).thenReturn(sslSession);
            KeyStore trustStore = SecurityConstants.newTruststore();
            String trustStorePassword = SecurityConstants.getTruststorePassword();
            try (InputStream tfis =
                Files.newInputStream(Paths.get(trustStoreFile.getAbsolutePath()))) {
              trustStore.load(tfis, trustStorePassword.toCharArray());
            } catch (CertificateException | NoSuchAlgorithmException e) {
              // ignore
            }
            X509Certificate[] certificates = new X509Certificate[2];
            certificates[0] = (X509Certificate) trustStore.getCertificate("asdf");
            certificates[1] = (X509Certificate) trustStore.getCertificate("localhost");
            when(sslSession.getPeerCertificates()).thenReturn(certificates);
            return sslSocket;
          }
        };

    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));

    return keystoreEditor;
  }

  @Test
  public void testGetKeystoreInfo() {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(0));
  }

  @Test
  public void testGetTruststoreInfo() {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
    Assert.assertThat(truststore.size(), Is.is(0));
  }

  @Test
  public void testAddKey() throws KeystoreEditor.KeystoreEditorException, IOException {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    addCertChain(keystoreEditor);
    List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(2));
    String alias1 = (String) keystore.get(0).get("alias");
    String alias2 = (String) keystore.get(1).get("alias");
    Assert.assertThat(alias1, AnyOf.anyOf(Is.is("asdf"), Is.is("ddf demo root ca")));
    Assert.assertThat(alias2, AnyOf.anyOf(Is.is("asdf"), Is.is("ddf demo root ca")));
    List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
    Assert.assertThat(truststore.size(), Is.is(0));

    addPrivateKey(keystoreEditor, keyFile, "");
    keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(2));
    alias1 = (String) keystore.get(0).get("alias");
    alias2 = (String) keystore.get(1).get("alias");
    Assert.assertThat(alias1, AnyOf.anyOf(Is.is("asdf"), Is.is("ddf demo root ca")));
    Assert.assertThat(alias2, AnyOf.anyOf(Is.is("asdf"), Is.is("ddf demo root ca")));
    truststore = keystoreEditor.getTruststore();
    Assert.assertThat(truststore.size(), Is.is(0));
  }

  @Test
  public void testAddPem() throws KeystoreEditor.KeystoreEditorException, IOException {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    addPrivateKey(keystoreEditor, pemFile, KeystoreEditor.PEM_TYPE);
    List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(1));
    Assert.assertThat(keystore.get(0).get("alias"), Is.is("asdf"));
    List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
    Assert.assertThat(truststore.size(), Is.is(0));
  }

  @Test
  public void testAddDer() throws KeystoreEditor.KeystoreEditorException, IOException {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    addPrivateKey(keystoreEditor, derFile, KeystoreEditor.DER_TYPE);
    List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(1));
    Assert.assertThat(keystore.get(0).get("alias"), Is.is("asdf"));
    List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
    Assert.assertThat(truststore.size(), Is.is(0));
  }

  private void addPrivateKey(KeystoreEditor keystoreEditor, File keyFile, String type)
      throws KeystoreEditor.KeystoreEditorException, IOException {
    FileInputStream fileInputStream = new FileInputStream(keyFile);
    byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
    IOUtils.closeQuietly(fileInputStream);
    keystoreEditor.addPrivateKey(
        "asdf",
        password,
        "",
        Base64.getEncoder().encodeToString(keyBytes),
        type,
        keyFile.toString());
  }

  private void addCertChain(KeystoreEditor keystoreEditor)
      throws KeystoreEditor.KeystoreEditorException, IOException {
    FileInputStream fileInputStream = new FileInputStream(chainFile);
    byte[] crtBytes = IOUtils.toByteArray(fileInputStream);
    IOUtils.closeQuietly(fileInputStream);
    keystoreEditor.addPrivateKey(
        "asdf",
        password,
        "",
        Base64.getEncoder().encodeToString(crtBytes),
        KeystoreEditor.PEM_TYPE,
        chainFile.toString());
  }

  @Test
  public void testAddKeyJks() throws KeystoreEditor.KeystoreEditorException, IOException {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    FileInputStream fileInputStream = new FileInputStream(jksFile);
    byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
    IOUtils.closeQuietly(fileInputStream);
    keystoreEditor.addPrivateKey(
        "asdf",
        password,
        password,
        Base64.getEncoder().encodeToString(keyBytes),
        "",
        jksFile.toString());
    List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(1));
    Assert.assertThat((String) keystore.get(0).get("alias"), Is.is("asdf"));

    List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
    Assert.assertThat(truststore.size(), Is.is(0));
  }

  @Test
  public void testAddKeyP12() throws KeystoreEditor.KeystoreEditorException, IOException {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    FileInputStream fileInputStream = new FileInputStream(pkcs12StoreFile);
    byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
    IOUtils.closeQuietly(fileInputStream);
    keystoreEditor.addPrivateKey(
        "asdf",
        password,
        password,
        Base64.getEncoder().encodeToString(keyBytes),
        KeystoreEditor.PKCS12_TYPE,
        pkcs12StoreFile.toString());
    List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(1));
    Assert.assertThat((String) keystore.get(0).get("alias"), Is.is("asdf"));

    List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
    Assert.assertThat(truststore.size(), Is.is(0));
  }

  @Test
  public void testAddKeyLocal() throws KeystoreEditor.KeystoreEditorException, IOException {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    FileInputStream fileInputStream = new FileInputStream(localhostCrtFile);
    byte[] crtBytes = IOUtils.toByteArray(fileInputStream);
    IOUtils.closeQuietly(fileInputStream);
    keystoreEditor.addPrivateKey(
        "localhost",
        password,
        "",
        Base64.getEncoder().encodeToString(crtBytes),
        KeystoreEditor.PEM_TYPE,
        localhostCrtFile.toString());
    List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(1));

    List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
    Assert.assertThat(truststore.size(), Is.is(0));

    keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    fileInputStream = new FileInputStream(localhostKeyFile);
    byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
    IOUtils.closeQuietly(fileInputStream);
    keystoreEditor.addPrivateKey(
        "localhost",
        password,
        "",
        Base64.getEncoder().encodeToString(keyBytes),
        "",
        localhostKeyFile.toString());
    keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(1));
    Assert.assertThat((String) keystore.get(0).get("alias"), Is.is("localhost"));

    truststore = keystoreEditor.getTruststore();
    Assert.assertThat(truststore.size(), Is.is(0));
  }

  @Test
  public void testReplaceSystemStores() throws Exception {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    Assert.assertThat(keystoreEditor.getKeystore().size(), Is.is(0));
    Assert.assertThat(keystoreEditor.getTruststore().size(), Is.is(0));

    try (FileInputStream keystoreInputStream = new FileInputStream(systemKeystoreFile);
        FileInputStream truststoreInputStream = new FileInputStream(systemTruststoreFile); ) {
      byte[] keystoreCrtBytes = IOUtils.toByteArray(keystoreInputStream);
      byte[] keystoreEncodedBytes = Base64.getEncoder().encode(keystoreCrtBytes);
      byte[] truststoreCrtBytes = IOUtils.toByteArray(truststoreInputStream);
      byte[] truststoreEncodedBytes = Base64.getEncoder().encode(truststoreCrtBytes);
      List<String> errors =
          keystoreEditor.replaceSystemStores(
              "localhost",
              password,
              password,
              new String(keystoreEncodedBytes),
              systemKeystoreFile.getName(),
              password,
              new String(truststoreEncodedBytes),
              systemTruststoreFile.getName());
      Assert.assertThat(errors.size(), Is.is(0));
      List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
      List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
      Assert.assertThat(keystore.size(), Is.is(2));
      String alias1 = (String) keystore.get(0).get("alias");
      String alias2 = (String) keystore.get(1).get("alias");
      Assert.assertThat(alias1, AnyOf.anyOf(Is.is("localhost"), Is.is("ddf demo root ca")));
      Assert.assertThat(alias2, AnyOf.anyOf(Is.is("localhost"), Is.is("ddf demo root ca")));
      Assert.assertThat(truststore.get(0).get("alias"), Is.is("ddf demo root ca"));
    }
  }

  @Test
  public void testReplaceSystemStoresBadFqdn() throws Exception {
    replaceSystemStores("asdf");
  }

  @Test
  public void testReplaceSystemStoresEmptyFqdn() throws Exception {
    replaceSystemStores("");
  }

  private void replaceSystemStores(String fqdn) throws Exception {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    Assert.assertThat(keystoreEditor.getKeystore().size(), Is.is(0));
    Assert.assertThat(keystoreEditor.getTruststore().size(), Is.is(0));

    try (FileInputStream keystoreInputStream = new FileInputStream(systemKeystoreFile);
        FileInputStream truststoreInputStream = new FileInputStream(systemTruststoreFile)) {
      byte[] keystoreCrtBytes = IOUtils.toByteArray(keystoreInputStream);
      byte[] keystoreEncodedBytes = Base64.getEncoder().encode(keystoreCrtBytes);
      byte[] truststoreCrtBytes = IOUtils.toByteArray(truststoreInputStream);
      byte[] truststoreEncodedBytes = Base64.getEncoder().encode(truststoreCrtBytes);
      List<String> errors =
          keystoreEditor.replaceSystemStores(
              fqdn,
              password,
              password,
              new String(keystoreEncodedBytes),
              systemKeystoreFile.getName(),
              password,
              new String(truststoreEncodedBytes),
              systemTruststoreFile.getName());
      Assert.assertThat(errors.size(), Is.is(1));
    }
  }

  @Test
  public void testReplaceSystemStoresBadInput() throws Exception {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    Assert.assertThat(keystoreEditor.getKeystore().size(), Is.is(0));
    Assert.assertThat(keystoreEditor.getTruststore().size(), Is.is(0));

    List<String> errors =
        keystoreEditor.replaceSystemStores(
            "localhost",
            password,
            password,
            null,
            systemKeystoreFile.getName(),
            password,
            null,
            systemTruststoreFile.getName());
    Assert.assertThat(errors.size(), Is.is(2));
  }

  @Test
  public void testReplaceSystemStoresBadEncoding() throws Exception {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    Assert.assertThat(keystoreEditor.getKeystore().size(), Is.is(0));
    Assert.assertThat(keystoreEditor.getTruststore().size(), Is.is(0));

    try (FileInputStream keystoreInputStream = new FileInputStream(systemKeystoreFile);
        FileInputStream truststoreInputStream = new FileInputStream(systemTruststoreFile); ) {
      byte[] keystoreCrtBytes = IOUtils.toByteArray(keystoreInputStream);

      byte[] truststoreCrtBytes = IOUtils.toByteArray(truststoreInputStream);

      List<String> errors =
          keystoreEditor.replaceSystemStores(
              "localhost",
              password,
              password,
              new String(keystoreCrtBytes),
              systemKeystoreFile.getName(),
              password,
              new String(truststoreCrtBytes),
              systemTruststoreFile.getName());
      Assert.assertThat(errors.size(), Is.is(1));
    }
  }

  @Test
  public void testAddCert() throws KeystoreEditor.KeystoreEditorException, IOException {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    FileInputStream fileInputStream = new FileInputStream(crtFile);
    byte[] crtBytes = IOUtils.toByteArray(fileInputStream);
    IOUtils.closeQuietly(fileInputStream);
    keystoreEditor.addTrustedCertificate(
        "asdf",
        password,
        "",
        new String(Base64.getEncoder().encode(crtBytes)),
        KeystoreEditor.PEM_TYPE,
        crtFile.toString());
    List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
    Assert.assertThat(truststore.size(), Is.is(1));
    Assert.assertThat(truststore.get(0).get("alias"), Is.is("asdf"));

    List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(0));
  }

  @Test
  public void testDeleteCert() throws KeystoreEditor.KeystoreEditorException, IOException {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    addCertChain(keystoreEditor);
    List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(2));

    keystoreEditor.deletePrivateKey("asdf");
    keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(1));
  }

  @Test
  public void testDeleteTrustedCert() throws KeystoreEditor.KeystoreEditorException, IOException {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    FileInputStream fileInputStream = new FileInputStream(crtFile);
    byte[] crtBytes = IOUtils.toByteArray(fileInputStream);
    IOUtils.closeQuietly(fileInputStream);
    keystoreEditor.addTrustedCertificate(
        "asdf",
        password,
        "",
        new String(Base64.getEncoder().encode(crtBytes)),
        KeystoreEditor.PEM_TYPE,
        crtFile.toString());
    List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
    Assert.assertThat(truststore.size(), Is.is(1));

    List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(0));

    keystoreEditor.deleteTrustedCertificate("asdf");
    truststore = keystoreEditor.getTruststore();
    Assert.assertThat(truststore.size(), Is.is(0));

    keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(0));
  }

  @Test
  public void testDeleteKey() throws KeystoreEditor.KeystoreEditorException, IOException {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    addCertChain(keystoreEditor);
    List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(2));

    addPrivateKey(keystoreEditor, keyFile, "");
    Assert.assertThat(keystore.size(), Is.is(2));

    keystoreEditor.deletePrivateKey("asdf");
    keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(1));
  }

  @Test
  public void testEncryptedData() throws KeystoreEditor.KeystoreEditorException, IOException {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    FileInputStream fileInputStream = new FileInputStream(p7bFile);
    byte[] crtBytes = IOUtils.toByteArray(fileInputStream);
    IOUtils.closeQuietly(fileInputStream);
    keystoreEditor.addTrustedCertificate(
        "asdf",
        password,
        "",
        new String(Base64.getEncoder().encode(crtBytes)),
        KeystoreEditor.PEM_TYPE,
        p7bFile.toString());
    List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
    Assert.assertThat(truststore.size(), Is.is(1));
    Assert.assertThat(truststore.get(0).get("alias"), Is.is("asdf"));

    List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
    Assert.assertThat(keystore.size(), Is.is(0));
  }

  @Test(expected = KeystoreEditor.KeystoreEditorException.class)
  public void testBadData() throws KeystoreEditor.KeystoreEditorException {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    keystoreEditor.addPrivateKey("asdf", password, password, "*$%^*", "", "file.pem");
  }

  @Test(expected = KeystoreEditor.KeystoreEditorException.class)
  public void testBadKeyPassword() throws KeystoreEditor.KeystoreEditorException, IOException {
    addPrivateKey("asdf", jksFile);
  }

  @Test(expected = KeystoreEditor.KeystoreEditorException.class)
  public void testBadKeyPasswordP12() throws KeystoreEditor.KeystoreEditorException, IOException {
    addPrivateKey("asdf", pkcs12StoreFile);
  }

  @Test(expected = KeystoreEditor.KeystoreEditorException.class)
  public void testBadStorePassword() throws KeystoreEditor.KeystoreEditorException, IOException {
    addPrivateKey("asdf", jksFile);
  }

  @Test(expected = KeystoreEditor.KeystoreEditorException.class)
  public void testBadStorePasswordP12() throws KeystoreEditor.KeystoreEditorException, IOException {
    addPrivateKey("asdf", pkcs12StoreFile);
  }

  @Test(expected = KeystoreEditor.KeystoreEditorException.class)
  public void testNullAlias() throws KeystoreEditor.KeystoreEditorException, IOException {
    addPrivateKey(null, pkcs12StoreFile);
  }

  @Test(expected = KeystoreEditor.KeystoreEditorException.class)
  public void testBlankAlias() throws KeystoreEditor.KeystoreEditorException, IOException {
    addPrivateKey("", pkcs12StoreFile);
  }

  @Test(expected = KeystoreEditor.KeystoreEditorException.class)
  public void testBadFile() throws KeystoreEditor.KeystoreEditorException, IOException {
    addPrivateKey("", badFile);
  }

  private void addPrivateKey(String alias, File file)
      throws KeystoreEditor.KeystoreEditorException, IOException {
    KeystoreEditor keystoreEditor = new KeystoreEditor();
    keystoreEditor.setSecurityLogger(mock(SecurityLogger.class));
    FileInputStream fileInputStream = new FileInputStream(file);
    byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
    IOUtils.closeQuietly(fileInputStream);
    keystoreEditor.addPrivateKey(
        alias,
        password,
        "blah",
        new String(Base64.getEncoder().encode(keyBytes)),
        "",
        file.toString());
  }
}
