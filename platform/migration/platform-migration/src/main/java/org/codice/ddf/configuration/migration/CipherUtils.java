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
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.codice.ddf.configuration.migration.MigrationZipConstants.getDefaultChecksumPathFor;
import static org.codice.ddf.configuration.migration.MigrationZipConstants.getDefaultKeyPathFor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.MigrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CipherUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(CipherUtils.class);

  private final Path zipPath;
  private final Path keyPath;
  private final Path checksumPath;
  private final Cipher cipher;
  private SecretKey secretKey;

  public CipherUtils(Path zipPath) {
    this(zipPath, getDefaultKeyPathFor(zipPath), getDefaultChecksumPathFor(zipPath));
  }

  public CipherUtils(Path zipPath, Path keyPath, Path checksumPath) {
    Validate.notNull(zipPath, "Null zip path");
    Validate.notNull(keyPath, "Null key path");
    Validate.notNull(checksumPath, "Null checksum path");
    this.zipPath = zipPath;
    this.keyPath = keyPath;
    this.checksumPath = checksumPath;
    cipher = createEncryptionCipher();
  }

  /**
   * Wraps a given {@link OutputStream} in a {@link CipherOutputStream}
   *
   * @param outputStream
   * @return a {@link CipherOutputStream} for writing encrypted {@link java.util.zip.ZipEntry}
   *     contents
   */
  public CipherOutputStream getCipherOutputStream(OutputStream outputStream) {
    return new CipherOutputStream(outputStream, cipher);
  }

  /**
   * Creates a {@link Cipher} for use during file encryption and stores the secret key in a file
   *
   * @throws MigrationException when an invalid key is provided
   * @throws MigrationException when an invalid key padding is used
   * @throws MigrationException when and invalid cipher algorithm is used
   * @return a {@link Cipher} in encrypt mode
   */
  private Cipher createEncryptionCipher() {
    try {
      Cipher iCipher = Cipher.getInstance(MigrationZipConstants.CIPHER_ALGORITHM);
      IvParameterSpec ivParameterSpec = new IvParameterSpec(MigrationZipConstants.CIPHER_IV);
      iCipher.init(Cipher.ENCRYPT_MODE, initKey(keyPath), ivParameterSpec);
      return iCipher;
    } catch (InvalidKeyException
        | NoSuchPaddingException
        | NoSuchAlgorithmException
        | InvalidAlgorithmParameterException e) {
      throw new MigrationException(MigrationZipConstants.KEY_INVALID_ERROR, keyPath, e);
    }
  }

  /**
   * Creates a file containing the checksum for the zip file
   *
   * @throws IOException
   * @throws NoSuchAlgorithmException
   */
  public void createZipChecksumFile() throws IOException, NoSuchAlgorithmException {
    String hash = getChecksum(zipPath);
    writeStringToFile(checksumPath.toFile(), hash, Charsets.UTF_8);
  }

  public Path getZipPath() {
    return zipPath;
  }

  public Path getKeyPath() {
    return keyPath;
  }

  public Path getChecksumPath() {
    return checksumPath;
  }

  @VisibleForTesting
  SecretKey getSecretKey() {
    return secretKey;
  }

  @VisibleForTesting
  Cipher getCipher() {
    return cipher;
  }

  private SecretKey initKey(Path keyPath) throws NoSuchAlgorithmException {
    if (keyPath.toFile().exists()) {
      SecretKey key;
      try {
        byte[] keyBytes = decodeHex(loadKeyDataFrom(keyPath).toCharArray());
        key = new SecretKeySpec(keyBytes, MigrationZipConstants.KEY_ALGORITHM);
      } catch (DecoderException e) {
        String message = String.format(MigrationZipConstants.KEY_DECODE_ERROR, keyPath, e);
        LOGGER.warn(message);
        throw new MigrationException(message);
      }
      this.secretKey = key;
      return key;
    } else {
      return createSecretKey(keyPath);
    }
  }

  private String loadKeyDataFrom(Path keyPath) {
    try (FileInputStream fis = new FileInputStream(keyPath.toFile())) {
      return IOUtils.toString(fis, StandardCharsets.UTF_8);
    } catch (IOException e) {
      String message = String.format(MigrationZipConstants.FILE_IO_ERROR, keyPath, e);
      LOGGER.warn(message);
      throw new MigrationException(message);
    }
  }

  /**
   * Creates a secret key and stores it in a file
   *
   * @param keyPath the path for storing the secret key
   * @throws MigrationException when an invalid key algorithm is used
   * @throws MigrationException when the key can not be written to a file
   * @throws NoSuchAlgorithmException when an invalid algorithm is used for the {@link KeyGenerator}
   * @return a {@link SecretKey}
   */
  private SecretKey createSecretKey(Path keyPath) throws NoSuchAlgorithmException {
    KeyGenerator keyGenerator;
    try {
      keyGenerator = KeyGenerator.getInstance(MigrationZipConstants.KEY_ALGORITHM);
      keyGenerator.init(128);
      secretKey = keyGenerator.generateKey();
      char[] hexKey = encodeHex(secretKey.getEncoded());
      writeStringToFile(keyPath.toFile(), String.valueOf(hexKey), Charsets.UTF_8);
      return secretKey;
    } catch (IOException e) {
      throw new MigrationException(String.format(MigrationZipConstants.FILE_IO_ERROR, keyPath, e));
    }
  }

  private String getChecksum(Path path) throws IOException, NoSuchAlgorithmException {
    try (BufferedInputStream bif = new BufferedInputStream(new FileInputStream(path.toFile()))) {
      MessageDigest digest =
          MessageDigest.getInstance(MigrationZipConstants.CHECKSUM_DIGEST_ALGORITHM);
      byte[] fileBytes = IOUtils.toByteArray(bif);
      digest.update(fileBytes);
      return new String(encodeHex(digest.digest()));
    } catch (FileNotFoundException e) {
      LOGGER.error("File {} does not exist", path.toString(), e);
      throw e;
    }
  }
}
