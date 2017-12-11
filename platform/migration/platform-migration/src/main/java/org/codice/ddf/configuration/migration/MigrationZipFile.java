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
import static org.codice.ddf.configuration.migration.MigrationZipConstants.getDefaultChecksumPathFor;
import static org.codice.ddf.configuration.migration.MigrationZipConstants.getDefaultKeyPathFor;

import com.google.common.base.Charsets;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationZipFile implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(CipherUtils.class);

  private final Path zipPath;
  private final Path keyPath;
  private final Path checksumPath;
  private final ZipFile zipFile;
  private final Cipher cipher;
  private boolean checksumValid;
  private boolean closed = false;

  /**
   * Opens a MigrationZipFile for reading encrypted entries using default locations for the key and
   * checksum files
   *
   * @param zipPath path to zip file
   * @throws IOException when there is a problem reading the zip file
   */
  public MigrationZipFile(Path zipPath) throws IOException {
    this(zipPath, getDefaultKeyPathFor(zipPath), getDefaultChecksumPathFor(zipPath));
  }

  /**
   * Opens a MigrationZipFile for reading encrypted entries using custom locations for the key and
   * checksum files
   *
   * @param zipPath path to zip file
   * @param keyPath path to key file
   * @param checksumPath path to checksum file
   * @throws IOException
   */
  public MigrationZipFile(Path zipPath, Path keyPath, Path checksumPath) throws IOException {
    Validate.notNull(zipPath, "Null zip path");
    Validate.notNull(keyPath, "Null key path");
    Validate.notNull(checksumPath, "Null checksum path");
    if ((!zipPath.toFile().exists())
        || (!keyPath.toFile().exists())
        || (!checksumPath.toFile().exists())) {
      throw new FileNotFoundException(
          String.format(
              "Could not find all required files [%s], [%s], [%s]",
              zipPath, keyPath, checksumPath));
    }
    this.zipPath = zipPath;
    this.keyPath = keyPath;
    this.checksumPath = checksumPath;
    checksumValid = validateChecksum(zipPath, checksumPath);
    zipFile = new ZipFile(zipPath.toFile());
    cipher = initCipher();
  }

  /**
   * Wraps the {@link InputStream} from ZipFile.getInputStream with a {@link CipherInputStream}
   *
   * @param entry
   * @return
   * @throws IOException
   */
  public InputStream getInputStream(ZipEntry entry) throws IOException {
    return getCipherInputStreamFor(new BufferedInputStream(zipFile.getInputStream(entry)));
  }

  /**
   * Proxy for ZipFile.stream();
   *
   * @return
   */
  @SuppressWarnings(
      "squid:S1452" /* Return type needs to use generic wildcard as this method proxies an existing one from ZipFile */)
  public Stream<? extends ZipEntry> stream() {
    return zipFile.stream();
  }

  /**
   * Determines if the MigrationZipFile checksum is valid.
   *
   * @return the boolean corresponding the checksum validity
   */
  public boolean isValidChecksum() {
    return checksumValid;
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      closed = true;
      zipFile.close();
    }
  }

  public Path getZipPath() {
    return zipPath;
  }

  public Path getChecksumPath() {
    return checksumPath;
  }

  public Path getKeyPath() {
    return keyPath;
  }

  public boolean isClosed() {
    return closed;
  }

  /** Deletes the file, key, and checksum */
  public void deleteQuitetly() {
    FileUtils.deleteQuietly(zipPath.toFile());
    FileUtils.deleteQuietly(keyPath.toFile());
    FileUtils.deleteQuietly(checksumPath.toFile());
  }

  private Cipher initCipher() throws ZipException {
    try {
      Cipher iCipher = Cipher.getInstance(MigrationZipConstants.CIPHER_ALGORITHM);
      IvParameterSpec ivParameterSpec = new IvParameterSpec(MigrationZipConstants.CIPHER_IV);
      SecretKey key = initKey(keyPath);
      iCipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
      return iCipher;
    } catch (InvalidKeyException
        | NoSuchPaddingException
        | NoSuchAlgorithmException
        | InvalidAlgorithmParameterException e) {
      String message = String.format(MigrationZipConstants.KEY_INVALID_ERROR, keyPath, e);
      LOGGER.error(message);
      throw new ZipException(message);
    }
  }

  @SuppressWarnings({
    "squid:S2093", /* try-with-resource will throw IOException with InputStream and we do not care to get that exception */
    "squid:S2095" /* stream is closed in the finally clause */
  })
  private SecretKey initKey(Path keyPath) throws ZipException {
    BufferedInputStream bif = null;
    try {
      bif = new BufferedInputStream(new FileInputStream(keyPath.toFile()));
      String keyData = IOUtils.toString(bif, StandardCharsets.UTF_8);
      byte[] keyBytes = decodeHex(keyData.toCharArray());
      return new SecretKeySpec(keyBytes, MigrationZipConstants.KEY_ALGORITHM);
    } catch (IOException e) {
      String message = String.format(MigrationZipConstants.FILE_IO_ERROR, keyPath, e);
      LOGGER.warn(message);
      throw new ZipException(message);
    } catch (DecoderException e) {
      String message = String.format(MigrationZipConstants.KEY_DECODE_ERROR, keyPath, e);
      LOGGER.warn(message);
      throw new ZipException(message);
    } finally {
      IOUtils.closeQuietly(bif);
    }
  }

  private CipherInputStream getCipherInputStreamFor(InputStream inputStream) {
    return new CipherInputStream(inputStream, cipher);
  }

  private boolean validateChecksum(Path zipPath, Path checksumPath) throws ZipException {
    String actualHash = getChecksumFor(zipPath);
    String storedHash = null;
    try {
      storedHash = readFileToString(checksumPath.toFile(), Charsets.UTF_8);
    } catch (IOException e) {
      String message =
          String.format(MigrationZipConstants.FILE_IO_ERROR, checksumPath.toString(), e);
      LOGGER.warn(message);
      throw new ZipException(message);
    }
    return actualHash.equals(storedHash);
  }

  private static String getChecksumFor(Path path) throws ZipException {
    try (FileInputStream fis = new FileInputStream(path.toFile())) {
      MessageDigest digest =
          MessageDigest.getInstance(MigrationZipConstants.CHECKSUM_DIGEST_ALGORITHM);
      byte[] fileBytes = IOUtils.toByteArray(fis);
      digest.update(fileBytes);
      return new String(encodeHex(digest.digest()));
    } catch (FileNotFoundException e) {
      String message = String.format(MigrationZipConstants.FILE_NOT_EXIST, path.toString(), e);
      LOGGER.warn(message);
      throw new ZipException(message);
    } catch (IOException e) {
      String message = String.format(MigrationZipConstants.FILE_IO_ERROR, path.toString(), e);
      LOGGER.warn(message);
      throw new ZipException(message);
    } catch (NoSuchAlgorithmException e) {
      String message =
          String.format(
              MigrationZipConstants.CHECKSUM_INVALID_ALGORITHM_ERROR,
              MigrationZipConstants.CHECKSUM_DIGEST_ALGORITHM,
              e);
      LOGGER.warn(message);
      throw new ZipException(message);
    }
  }
}
