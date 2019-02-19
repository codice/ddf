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
package ddf.security.encryption.crypter;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.google.common.annotations.VisibleForTesting;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import ddf.security.SecurityConstants;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A "crypter" combines an "encryptor" and a "decryptor". This class is responsible for abstracting
 * underlying cryptography implementations for use throughout the system.
 */
public class Crypter {
  private static final Logger LOGGER = LoggerFactory.getLogger(Crypter.class);
  private static final String DEFAULT_KEYSET_FILE_NAME = "default";
  private static final String KEYSET_FILE_EXTENSION = ".json";
  private static final int ASSOCIATED_DATA_BYTE_SIZE = 10;

  private final String keysetDir =
      AccessController.doPrivileged(
          (PrivilegedAction<String>) () -> System.getProperty(SecurityConstants.KEYSET_DIR));
  private final String associatedDataPath =
      AccessController.doPrivileged(
          (PrivilegedAction<String>)
              () -> System.getProperty(SecurityConstants.ASSOCIATED_DATA_PATH));

  @VisibleForTesting KeysetHandle keysetHandle;
  private byte[] associatedData;
  private Aead aead;

  /**
   * Read/create a specified keyset under the keyset directory.
   *
   * @param keysetFileName name of the keyset to read/create.
   * @throws CrypterException
   */
  public Crypter(String keysetFileName) throws CrypterException {
    String keysetLocation = Paths.get(keysetDir, keysetFileName + KEYSET_FILE_EXTENSION).toString();

    synchronized (Crypter.class) {
      File keysetFile = new File(keysetLocation);

      try {
        AeadConfig.register();
        if (!keysetFile.exists()) {
          keysetHandle = initKeysetHandle(keysetFile);
        } else {
          keysetHandle = readKeysetHandle(keysetFile);
        }
        aead = AeadFactory.getPrimitive(keysetHandle);
        associatedData = getAssociatedData(keysetHandle);
      } catch (GeneralSecurityException | IOException e) {
        LOGGER.warn("Problem initializing keyset.");
        throw new CrypterException("Problem initializing keyset", e);
      }
    }
  }

  public Crypter() throws CrypterException {
    this(DEFAULT_KEYSET_FILE_NAME);
  }

  /**
   * Encrypts a plain text value using Tink.
   *
   * @param plainTextValue The value to encrypt.
   */
  public String encrypt(String plainTextValue) throws CrypterException {
    if (isEmpty(plainTextValue)) {
      throw new CrypterException("Value to encrypt cannot be null or empty.");
    }
    if (associatedData == null) {
      throw new CrypterException("Associated data cannot be null.");
    }
    try {
      byte[] encryptedBytes = aead.encrypt(plainTextValue.getBytes(), associatedData);
      return Base64.getEncoder().encodeToString(encryptedBytes);
    } catch (GeneralSecurityException e) {
      LOGGER.debug("Problem encrypting.", e);
      throw new CrypterException("Problem encrypting.", e);
    }
  }

  /**
   * Decrypts a plain text value using Tink
   *
   * @param encryptedValue The value to decrypt.
   */
  public String decrypt(String encryptedValue) throws CrypterException {
    if (isEmpty(encryptedValue)) {
      throw new CrypterException("Value to decrypt cannot be null or empty.");
    }
    if (associatedData == null) {
      throw new CrypterException("Associated data cannot be null.");
    }
    try {
      byte[] encryptedBytes = Base64.getDecoder().decode(encryptedValue);
      return new String(aead.decrypt(encryptedBytes, associatedData));
    } catch (GeneralSecurityException | NullPointerException e) {
      LOGGER.debug("Problem decrypting.", e);
      throw new CrypterException("Problem decrypting.", e);
    }
  }

  private KeysetHandle initKeysetHandle(File keysetFile)
      throws IOException, GeneralSecurityException {

    if (!keysetFile.getParentFile().exists()) {
      AccessController.doPrivileged(
          (PrivilegedAction<Boolean>) () -> keysetFile.getParentFile().mkdirs());
    }

    try (OutputStream keysetFileOutputStream = getKeysetOutputStream(keysetFile)) {
      if (keysetFileOutputStream == null) {
        throw new IOException("Problem opening keyset file output stream.");
      }
      KeysetHandle keysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES128_GCM);
      CleartextKeysetHandle.write(
          keysetHandle, JsonKeysetWriter.withOutputStream(keysetFileOutputStream));
      return keysetHandle;
    }
  }

  private OutputStream getKeysetOutputStream(File keysetFile) {
    return AccessController.doPrivileged(
        (PrivilegedAction<OutputStream>)
            () -> {
              try {
                return Files.newOutputStream(keysetFile.toPath());
              } catch (IOException e) {
                return null;
              }
            });
  }

  private KeysetHandle readKeysetHandle(File keysetFile)
      throws IOException, GeneralSecurityException {

    try (InputStream keysetFileInputStream = getKeysetInputStream(keysetFile)) {
      if (keysetFileInputStream == null) {
        throw new IOException("Problem opening keyset file input stream.");
      }
      return CleartextKeysetHandle.read(JsonKeysetReader.withInputStream(keysetFileInputStream));
    }
  }

  private InputStream getKeysetInputStream(File keysetFile) {
    return AccessController.doPrivileged(
        (PrivilegedAction<InputStream>)
            () -> {
              try {
                return Files.newInputStream(keysetFile.toPath());
              } catch (IOException e) {
                return null;
              }
            });
  }

  private byte[] getAssociatedData(KeysetHandle keysetHandle) throws CrypterException {
    String primaryKeyId = Integer.toString(keysetHandle.getKeysetInfo().getPrimaryKeyId());

    Properties properties = getAssociatedDataProperties();
    if (properties.containsKey(primaryKeyId)) {
      LOGGER.debug("Found MAC (%s) in property file (%s).", primaryKeyId, associatedDataPath);
      return Base64.getDecoder().decode(properties.getProperty(primaryKeyId));
    }
    LOGGER.debug(
        "Could not find key (%s) in properties file (%s).", primaryKeyId, associatedDataPath);

    try {
      return generateAssociatedDataAndStoreToProperties(primaryKeyId, properties);
    } catch (IOException e) {
      throw new CrypterException("Problem getting associated data.", e);
    }
  }

  private Properties getAssociatedDataProperties() {
    try (InputStream inputStream = getAssociatedDataPropertiesInputStream()) {
      Properties properties = new Properties();
      if (inputStream != null) {
        properties.load(inputStream);
      }
      return properties;
    } catch (IOException e) {
      return new Properties();
    }
  }

  private byte[] generateAssociatedDataAndStoreToProperties(
      String primaryKeyId, Properties properties) throws IOException {
    try (OutputStream outputStream = getAssociatedDataPropertiesOutputStream()) {
      if (outputStream == null) {
        throw new IOException("Problem initializing properties output stream.");
      }
      SecureRandom secureRandom = new SecureRandom();
      byte[] generatedAssociatedData = new byte[ASSOCIATED_DATA_BYTE_SIZE];
      secureRandom.nextBytes(generatedAssociatedData);

      properties.setProperty(
          primaryKeyId, Base64.getEncoder().encodeToString(generatedAssociatedData));
      properties.store(outputStream, null);

      return generatedAssociatedData;
    }
  }

  private InputStream getAssociatedDataPropertiesInputStream() {
    return AccessController.doPrivileged(
        (PrivilegedAction<InputStream>)
            () -> {
              try {
                return Files.newInputStream(Paths.get(associatedDataPath));
              } catch (IOException e) {
                return null;
              }
            });
  }

  private OutputStream getAssociatedDataPropertiesOutputStream() {
    return AccessController.doPrivileged(
        (PrivilegedAction<OutputStream>)
            () -> {
              try {
                return Files.newOutputStream(Paths.get(associatedDataPath));
              } catch (IOException e) {
                return null;
              }
            });
  }

  public static class CrypterException extends RuntimeException {
    public CrypterException(String msg) {
      super(msg);
    }

    public CrypterException(Exception e) {
      super(e);
    }

    public CrypterException(String msg, Exception e) {
      super(msg, e);
    }
  }
}
