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
package org.codice.ddf.security.certificate.generator;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Properties;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(value = MockitoJUnitRunner.class)
public class KeyStoreFileTest {

  public static final String BOGUS_FILENAME = "not_keystore.jks";

  public static final String KEYSTORE_TEMPLATE = "keystore-password_changeit.jks";

  public static final String KEYSTORE_COPY = "workingCopy.jks";

  public static final String ALIAS_DEMO_CA = "demorootca";

  public static final char[] PASSWORD = "changeit".toCharArray();

  public static final char[] BOGUS_PASSWORD = "ThisIsNotThePassword".toCharArray();

  public static final String ALIAS_SAMPLE_PRIVATE_KEY_ENTRY = "sampleprivatekeyentry";

  private static Properties properties;

  @BeforeClass
  public static void init() {
    properties = System.getProperties();
    System.setProperty("javax.net.ssl.keyStoreType", "JKS");
  }

  @AfterClass
  public static void tearDown() {
    System.setProperties(properties);
  }

  String getPathTo(String path) {
    URL resourcePath = getClass().getClassLoader().getResource(path);
    if (resourcePath == null) {
      fail("Could not retrieve resource. Check the resources folder.");
    }
    return new File(resourcePath.getPath()).getPath();
  }

  @Before
  public void setUp() throws IOException {
    refreshKeyStoreFile();
  }

  // Test constructor. Null path to keyStore file.
  @Test(expected = IllegalArgumentException.class)
  public void constructorNullPath() throws Exception {
    KeyStoreFile.openFile(null, null);
  }

  // Test constructor. Invalid path to keyStore file.
  @Test(expected = CertificateGeneratorException.class)
  public void constructorInvalidPath() throws Exception {
    KeyStoreFile.openFile("", null);
  }

  // Test Constructor. Path is a directory, not a file.
  @Test(expected = CertificateGeneratorException.class)
  public void constructorPathIsDirectory() throws Exception {
    String anyDirectory = getPathTo("");
    KeyStoreFile.openFile(anyDirectory, null);
  }

  // Test Constructor. File is not keyStore.
  @Test(expected = CertificateGeneratorException.class)
  public void constructorFileNotKeyStore() throws Exception {
    KeyStoreFile.openFile(getPathTo(BOGUS_FILENAME), null);
  }

  // Test Constructor. Password is null.
  @Test(expected = CertificateGeneratorException.class)
  public void constructorNullPassword() throws Exception {
    KeyStoreFile.openFile(getPathTo(KEYSTORE_COPY), null);
  }

  // Test Constructor. Password is wrong.
  @Test(expected = CertificateGeneratorException.class)
  public void constructorWrongPassword() throws Exception {
    KeyStoreFile.openFile(getPathTo(KEYSTORE_COPY), BOGUS_PASSWORD);
  }

  // Test Constructor. Valid file, valid password.
  @Test
  public void testConstructor() {
    KeyStoreFile keyStore = KeyStoreFile.openFile(getPathTo(KEYSTORE_COPY), PASSWORD);
    assertNotNull(keyStore.aliases());
    assertThat(
        "Missing key in keystore test file resource", keyStore.aliases(), hasItem(ALIAS_DEMO_CA));
  }

  Path refreshKeyStoreFile() throws IOException {
    String stringToKeystoreTemplate = getPathTo(KEYSTORE_TEMPLATE);
    String stringToKeystoreCopy = getPathTo(KEYSTORE_COPY);
    Path pathToKeystoreTemplate = Paths.get(stringToKeystoreTemplate);
    Path pathToKeystoreCopy = Paths.get(stringToKeystoreCopy);
    return Files.copy(pathToKeystoreTemplate, pathToKeystoreCopy, REPLACE_EXISTING);
  }

  @Test
  public void addAndRemoveEntry() throws IOException, GeneralSecurityException {

    KeyStoreFile ksFile = KeyStoreFile.openFile(getPathTo(KEYSTORE_COPY), PASSWORD);

    // Get a cert from the keystore
    KeyStore.TrustedCertificateEntry demoCa =
        (KeyStore.TrustedCertificateEntry) ksFile.getEntry(ALIAS_DEMO_CA);
    assertThat(
        "Could not retrieve Demo CA from keystore",
        demoCa,
        instanceOf(KeyStore.TrustedCertificateEntry.class));
    assertThat(ksFile.isKey(ALIAS_DEMO_CA), is(false));

    // Delete a cert from the file
    ksFile.deleteEntry(ALIAS_DEMO_CA);
    assertThat("Could not delete key from keystore", ksFile.aliases(), not(hasItem(ALIAS_DEMO_CA)));

    // Add a new entry to the file
    KeyStore.Entry pkEntry = ksFile.getEntry(ALIAS_SAMPLE_PRIVATE_KEY_ENTRY);
    assertThat(
        "Could not find sample private key in keystore",
        ksFile.aliases(),
        hasItem(ALIAS_SAMPLE_PRIVATE_KEY_ENTRY));
    String alias = "temp";
    ksFile.setEntry(alias, pkEntry);
    assertThat("Did not add key to file as expected", ksFile.aliases(), hasItem(alias));
    assertThat(ksFile.isKey(alias), is(true));

    // Save and reload file
    ksFile.save();
    ksFile = KeyStoreFile.openFile(getPathTo(KEYSTORE_COPY), PASSWORD);
    assertThat("Keystore file did not save the new key", ksFile.aliases(), hasItem(alias));
  }
}
