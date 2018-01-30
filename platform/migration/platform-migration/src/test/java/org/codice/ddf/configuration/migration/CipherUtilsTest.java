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
package org.codice.ddf.configuration.migration;

import static org.apache.commons.codec.binary.Hex.decodeHex;
import static org.apache.commons.codec.binary.Hex.encodeHex;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.Assert.assertThat;

import com.google.common.base.Charsets;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.migration.MigrationException;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class CipherUtilsTest extends AbstractMigrationSupport {

  private Path zipPath;
  private Path keyPath;
  private Path checksumPath;

  @Before
  public void setup() throws IOException, NoSuchAlgorithmException {
    zipPath = Paths.get(ddfHome.toString(), "exported", "test.dar");
    keyPath = Paths.get(ddfHome.toString(), "exported", "valid.key");
    checksumPath = Paths.get(ddfHome.toString(), "exported", "valid.checksum");
  }

  @Test
  public void testConstructorDefaultKeyAndChecksumLocations() {
    CipherUtils cipherUtils = new CipherUtils(zipPath);
    assertThat(cipherUtils.getZipPath(), Matchers.equalTo(zipPath));
    assertThat(
        cipherUtils.getKeyPath(),
        Matchers.equalTo(Paths.get(zipPath + MigrationZipConstants.KEY_EXTENSION)));
    assertThat(
        cipherUtils.getChecksumPath(),
        Matchers.equalTo(Paths.get(zipPath + MigrationZipConstants.CHECKSUM_EXTENSION)));
    assertThat(cipherUtils.getKeyPath().toFile().exists(), Matchers.equalTo(true));
  }

  @Test
  public void testConstructorAlternateKeyAndChecksumLocations() {
    CipherUtils cipherUtils = new CipherUtils(zipPath, keyPath, checksumPath);
    assertThat(cipherUtils.getZipPath(), Matchers.equalTo(zipPath));
    assertThat(cipherUtils.getKeyPath(), Matchers.equalTo(keyPath));
    assertThat(cipherUtils.getChecksumPath(), Matchers.equalTo(checksumPath));
    assertThat(keyPath.toFile().exists(), Matchers.equalTo(true));
  }

  @Test
  public void testCreatingKey() throws Exception {
    Path keyPath = Paths.get(ddfHome.toString(), "secret.key");

    CipherUtils cipherUtils = new CipherUtils(zipPath, keyPath, checksumPath);
    SecretKey secretKey = cipherUtils.getSecretKey();
    SecretKey loadedKey = loadKeyFrom(cipherUtils.getKeyPath());
    assertThat(keyPath.toFile().exists(), Matchers.equalTo(true));
    assertThat(secretKey, Matchers.equalTo(loadedKey));
  }

  @Test
  public void testKeyAlreadyExists() throws IOException {
    CipherUtils cipherUtils = new CipherUtils(zipPath, keyPath, checksumPath);
    CipherUtils testCipherUtils = new CipherUtils(zipPath, keyPath, checksumPath);
    assertThat(testCipherUtils.getSecretKey(), Matchers.equalTo(cipherUtils.getSecretKey()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorWithNullChecksumPath() {
    new CipherUtils(zipPath, keyPath, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorWithNullzipPath() {
    new CipherUtils(null, keyPath, checksumPath);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorWithNullKeyPath() {
    new CipherUtils(zipPath, null, checksumPath);
  }

  @Test
  public void testCreateEncryptionCipher() {
    Path keyPath = Paths.get(ddfHome.toString(), "secret.key");
    CipherUtils cipherUtils = new CipherUtils(zipPath, keyPath, checksumPath);
    Cipher cipher = cipherUtils.getCipher();
    assertThat(keyPath.toFile().exists(), Matchers.equalTo(true));
    assertThat(cipher, Matchers.notNullValue());
  }

  @Test
  public void testCreateChecksum() throws Exception {
    CipherUtils cipherUtils = new CipherUtils(zipPath, keyPath, checksumPath);
    writeStringToFile(zipPath.toFile(), "test", StandardCharsets.UTF_8);
    cipherUtils.createZipChecksumFile();
    String expectedHash = getChecksum(zipPath);
    String hashFileContent = readFileToString(checksumPath.toFile(), Charsets.UTF_8);
    assertThat(hashFileContent, Matchers.equalTo(expectedHash));
  }

  @Test
  public void createEncryptedFile() throws IOException {
    CipherUtils cipherUtils = new CipherUtils(zipPath);
    Path encryptedFile = Paths.get(ddfHome.toString(), "encrypted.test.file");
    FileOutputStream fos = new FileOutputStream(encryptedFile.toFile());
    CipherOutputStream cipherOutputStream = cipherUtils.getCipherOutputStream(fos);
    IOUtils.write("foo", cipherOutputStream, StandardCharsets.UTF_8);
    cipherOutputStream.close();
    assertThat(
        readFileToString(encryptedFile.toFile(), StandardCharsets.UTF_8), Matchers.not("foo"));
  }

  @Test(expected = FileNotFoundException.class)
  public void testCreateChecksumFileNotExist() throws Exception {
    CipherUtils cipherUtils = new CipherUtils(zipPath);
    cipherUtils.createZipChecksumFile();
  }

  @Test(expected = MigrationException.class)
  public void testBadlyEncodedKeyFile() throws IOException {
    new CipherUtils(zipPath, keyPath, checksumPath);
    writeStringToFile(keyPath.toFile(), "1", StandardCharsets.UTF_8, true);
    new CipherUtils(zipPath, keyPath, checksumPath);
  }

  private SecretKey loadKeyFrom(Path keyPath) throws Exception {
    BufferedInputStream bif = new BufferedInputStream(new FileInputStream(keyPath.toFile()));
    String keyData = IOUtils.toString(bif, Charsets.UTF_8);
    IOUtils.closeQuietly(bif);
    byte[] keyBytes = decodeHex(keyData.toCharArray());
    return new SecretKeySpec(keyBytes, MigrationZipConstants.KEY_ALGORITHM);
  }

  private String getChecksum(Path path) throws Exception {
    FileInputStream fis = new FileInputStream(path.toFile());
    MessageDigest digest =
        MessageDigest.getInstance(MigrationZipConstants.CHECKSUM_DIGEST_ALGORITHM);
    byte[] fileBytes = IOUtils.toByteArray(fis);
    digest.update(fileBytes);
    return new String(encodeHex(digest.digest()));
  }
}
