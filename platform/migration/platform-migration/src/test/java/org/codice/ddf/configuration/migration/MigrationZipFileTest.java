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

import static org.apache.commons.codec.binary.Hex.encodeHex;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.google.common.base.Charsets;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

public class MigrationZipFileTest extends AbstractMigrationSupport {

  private Path zipPath;
  private Path keyPath;
  private Path checksumPath;

  @Before
  public void setup() throws Exception {
    zipPath = Paths.get(ddfHome.toString(), "exported", "test.dar");
    keyPath = Paths.get(ddfHome.toString(), "exported", "valid.key");
    checksumPath = Paths.get(ddfHome.toString(), "exported", "valid.checksum");
    createZipWithEncryptedContents(zipPath, keyPath, checksumPath);
  }

  @Test
  public void testMigrationZipFileWithDefaultKeyAndChecksumPaths() throws Exception {
    Path defaultsZipPath = Paths.get(ddfHome.toString(), "test", "defaults.dar");
    Path defaultsKeyPath =
        Paths.get(defaultsZipPath.toString() + MigrationZipConstants.KEY_EXTENSION);
    Path defaultsChecksumPath =
        Paths.get(defaultsZipPath.toString() + MigrationZipConstants.CHECKSUM_EXTENSION);
    createZipWithEncryptedContents(defaultsZipPath, defaultsKeyPath, defaultsChecksumPath);
    MigrationZipFile migrationZipFile = new MigrationZipFile(defaultsZipPath);
    assertThat(migrationZipFile.getZipPath(), equalTo(defaultsZipPath));
    assertThat(migrationZipFile.getKeyPath(), equalTo(defaultsKeyPath));
    assertThat(migrationZipFile.getChecksumPath(), equalTo(defaultsChecksumPath));
  }

  @Test(expected = FileNotFoundException.class)
  public void testConstructorWhenZipFileNotFound() throws IOException {
    new MigrationZipFile(Paths.get(ddfHome.toString(), "non-existent.dar"), keyPath, checksumPath);
  }

  @Test(expected = FileNotFoundException.class)
  public void testConstructorWhenKeyFileNotFound() throws IOException {
    new MigrationZipFile(zipPath, Paths.get(ddfHome.toString(), "non-existent.key"), checksumPath);
  }

  @Test(expected = FileNotFoundException.class)
  public void testConstructorWhenChecksumFileNotFound() throws IOException {
    new MigrationZipFile(zipPath, keyPath, Paths.get(ddfHome.toString(), "non-existent.sum"));
  }

  @Test
  public void testClose() throws IOException {
    MigrationZipFile migrationZipFile = new MigrationZipFile(zipPath, keyPath, checksumPath);
    migrationZipFile.close();
    assertThat(migrationZipFile.isClosed(), equalTo(true));
  }

  @Test
  public void testChecksumValid() throws IOException, NoSuchAlgorithmException {
    MigrationZipFile migrationZipFile = new MigrationZipFile(zipPath, keyPath, checksumPath);
    assertThat(migrationZipFile.isValidChecksum(), equalTo(true));
    assertThat(validateChecksum(zipPath, checksumPath), equalTo(true));
  }

  @Test
  public void testChecksumInvalid() throws Exception {
    writeStringToFile(checksumPath.toFile(), "test", StandardCharsets.UTF_8);
    MigrationZipFile migrationZipFile = new MigrationZipFile(zipPath, keyPath, checksumPath);
    assertThat(migrationZipFile.isValidChecksum(), equalTo(false));
  }

  @Test
  public void testGetZipEntries() throws IOException {
    MigrationZipFile migrationZipFile = new MigrationZipFile(zipPath, keyPath, checksumPath);
    assertThat(migrationZipFile.stream().count(), equalTo(2L));
  }

  @Test
  public void testReadingEncryptedEntry() throws IOException {
    MigrationZipFile migrationZipFile = new MigrationZipFile(zipPath, keyPath, checksumPath);
    migrationZipFile.stream()
        .forEach(
            entry -> {
              try {
                InputStream is = migrationZipFile.getInputStream(entry);
                String contents = IOUtils.toString(is, StandardCharsets.UTF_8);
                assertThat(contents, notNullValue());
              } catch (IOException e) {
                // Ignore
              }
            });
  }

  @Test(expected = IOException.class)
  public void testReadingUnencryptedEntryFails() throws Exception {
    Path altZipPath = Paths.get(ddfHome.toString(), "alt.zip");
    Path altKeyPath = Paths.get(ddfHome.toString(), "alt.key");
    Path altChecksumPath = Paths.get(ddfHome.toString(), "alt.sum");
    createZipWithUnencryptedContents(altZipPath, altKeyPath, altChecksumPath);
    MigrationZipFile migrationZipFile =
        new MigrationZipFile(altZipPath, altKeyPath, altChecksumPath);
    ZipEntry entry = migrationZipFile.stream().findAny().get();
    InputStream is = migrationZipFile.getInputStream(entry);
    IOUtils.toString(is, StandardCharsets.UTF_8);
  }

  @Test(expected = ZipException.class)
  public void testKeyWithIncorrectAlgorithm() throws IOException, NoSuchAlgorithmException {
    deleteQuietly(keyPath.toFile());
    generateKeyWithWrongAlgorithm(keyPath);
    new MigrationZipFile(zipPath, keyPath, checksumPath);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorWithNullZipPath() throws IOException {
    new MigrationZipFile(null, keyPath, checksumPath);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorWithNullKeyPath() throws IOException {
    new MigrationZipFile(zipPath, null, checksumPath);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorWithNullChecksumPath() throws IOException {
    new MigrationZipFile(zipPath, keyPath, null);
  }

  @Test(expected = ZipException.class)
  public void testZipWithInvalidEncryptionKey() throws Exception {
    Path badKeyPath = Paths.get(ddfHome.toString(), "bad.key");
    writeStringToFile(badKeyPath.toFile(), "bad-key", StandardCharsets.UTF_8);
    new MigrationZipFile(zipPath, badKeyPath, checksumPath);
  }

  private void createZipWithUnencryptedContents(Path zipPath, Path keyPath, Path checksumPath)
      throws Exception {
    createEncryptionCipher(keyPath);
    ZipEntry zipEntry = new ZipEntry("foo");
    ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipPath.toFile()));
    zipOutputStream.putNextEntry(zipEntry);
    IOUtils.write("bar", zipOutputStream, StandardCharsets.UTF_8);
    zipOutputStream.closeEntry();
    zipOutputStream.close();
    createChecksum(zipPath, checksumPath);
  }

  private void createZipWithEncryptedContents(Path zipPath, Path keyPath, Path checksumPath)
      throws Exception {
    Cipher cipher = createEncryptionCipher(keyPath);
    ZipEntry zipEntry = new ZipEntry("foo");
    ZipEntry zipEntry1 = new ZipEntry("baz");
    ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipPath.toFile()));
    zipOutputStream.putNextEntry(zipEntry);
    IOUtils.write("bar", new CipherOutputStream(zipOutputStream, cipher), StandardCharsets.UTF_8);
    zipOutputStream.closeEntry();
    zipOutputStream.putNextEntry(zipEntry1);
    IOUtils.write("qux", new CipherOutputStream(zipOutputStream, cipher), StandardCharsets.UTF_8);
    zipOutputStream.closeEntry();
    zipOutputStream.close();
    createChecksum(zipPath, checksumPath);
  }

  private Cipher createEncryptionCipher(Path keyPath) throws Exception {
    Cipher cipher = Cipher.getInstance(MigrationZipConstants.CIPHER_ALGORITHM);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(MigrationZipConstants.CIPHER_IV);
    cipher.init(Cipher.ENCRYPT_MODE, createSecretKey(keyPath), ivParameterSpec);
    return cipher;
  }

  private SecretKey createSecretKey(Path keyPath) throws Exception {
    KeyGenerator keyGenerator = null;
    keyGenerator = KeyGenerator.getInstance(MigrationZipConstants.KEY_ALGORITHM);
    SecretKey secretKey = keyGenerator.generateKey();
    char[] hexKey = encodeHex(secretKey.getEncoded());
    writeStringToFile(keyPath.toFile(), String.valueOf(hexKey), Charsets.UTF_8);
    return secretKey;
  }

  private void generateKeyWithWrongAlgorithm(Path keyPath)
      throws NoSuchAlgorithmException, IOException {
    KeyGenerator keyGenerator = KeyGenerator.getInstance("DES");
    SecretKey secretKey = keyGenerator.generateKey();
    char[] hexKey = encodeHex(secretKey.getEncoded());
    writeStringToFile(keyPath.toFile(), String.valueOf(hexKey), Charsets.UTF_8);
  }

  private void createChecksum(Path zipPath, Path checksumPath) throws Exception {
    FileInputStream fis = new FileInputStream(zipPath.toFile());
    MessageDigest digest =
        MessageDigest.getInstance(MigrationZipConstants.CHECKSUM_DIGEST_ALGORITHM);
    byte[] fileBytes = IOUtils.toByteArray(fis);
    digest.update(fileBytes);
    String hash = new String(encodeHex(digest.digest()));
    writeStringToFile(checksumPath.toFile(), hash, Charsets.UTF_8);
  }

  private boolean validateChecksum(Path zipPath, Path checksumPath)
      throws IOException, NoSuchAlgorithmException {
    FileInputStream fis = new FileInputStream(zipPath.toFile());
    MessageDigest digest =
        MessageDigest.getInstance(MigrationZipConstants.CHECKSUM_DIGEST_ALGORITHM);
    byte[] fileBytes = IOUtils.toByteArray(fis);
    digest.update(fileBytes);
    String hash = new String(encodeHex(digest.digest()));
    String expectedHash = readFileToString(checksumPath.toFile(), Charsets.UTF_8);
    return hash.equals(expectedHash);
  }
}
