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

import com.google.common.annotations.VisibleForTesting;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.StreamingAead;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.google.crypto.tink.proto.KeyTemplate;
import com.google.crypto.tink.streamingaead.StreamingAeadConfig;
import com.google.crypto.tink.streamingaead.StreamingAeadFactory;
import com.google.crypto.tink.streamingaead.StreamingAeadKeyTemplates;
import ddf.security.SecurityConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
  private static final String STREAMING_KEYSET_FILE_SPECIFIER = "-streaming";
  private static final String KEYSET_FILE_EXTENSION = ".json";
  private static final int ASSOCIATED_DATA_BYTE_SIZE = 10;
  @VisibleForTesting static final int CHUNK_SIZE = 256;

  private final String keysetDir =
      AccessController.doPrivileged(
          (PrivilegedAction<String>) () -> System.getProperty(SecurityConstants.KEYSET_DIR));
  private final String associatedDataPath =
      AccessController.doPrivileged(
          (PrivilegedAction<String>)
              () -> System.getProperty(SecurityConstants.ASSOCIATED_DATA_PATH));

  @VisibleForTesting KeysetHandle keysetHandle;
  @VisibleForTesting KeysetHandle streamingKeysetHandle;
  private byte[] associatedData;
  private Aead aead;
  private StreamingAead streamingAead;

  /**
   * Read/create a specified keyset under the keyset directory.
   *
   * @param keysetFileName name of the keyset to read/create.
   * @throws CrypterException
   */
  public Crypter(String keysetFileName) throws CrypterException {
    String keysetLocation = Paths.get(keysetDir, keysetFileName + KEYSET_FILE_EXTENSION).toString();
    String streamingKeysetLocation =
        Paths.get(
                keysetDir, keysetFileName + STREAMING_KEYSET_FILE_SPECIFIER + KEYSET_FILE_EXTENSION)
            .toString();

    synchronized (Crypter.class) {
      File keysetFile = new File(keysetLocation);
      File streamingKeysetFile = new File(streamingKeysetLocation);

      boolean keysetFileExists =
          AccessController.doPrivileged((PrivilegedAction<Boolean>) keysetFile::exists);
      boolean streamingKeysetFileExists =
          AccessController.doPrivileged((PrivilegedAction<Boolean>) streamingKeysetFile::exists);

      try {
        AeadConfig.register();
        StreamingAeadConfig.register();

        // aead keyset
        if (!keysetFileExists) {
          keysetHandle = initKeysetHandle(keysetFile, AeadKeyTemplates.AES128_GCM);
        } else {
          keysetHandle = readKeysetHandle(keysetFile);
        }

        // streaming aead keyset
        if (!streamingKeysetFileExists) {
          streamingKeysetHandle =
              initKeysetHandle(streamingKeysetFile, StreamingAeadKeyTemplates.AES128_GCM_HKDF_4KB);
        } else {
          streamingKeysetHandle = readKeysetHandle(streamingKeysetFile);
        }

        aead = AeadFactory.getPrimitive(keysetHandle);
        streamingAead = StreamingAeadFactory.getPrimitive(streamingKeysetHandle);
        associatedData = getAssociatedData();
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
   * @param plainBytes The value to encrypt.
   */
  public byte[] encrypt(byte[] plainBytes) throws CrypterException {
    if (plainBytes == null || plainBytes.length < 1) {
      throw new CrypterException("Bytes to encrypt cannot be null or empty.");
    }
    if (associatedData == null) {
      throw new CrypterException("Associated data cannot be null.");
    }

    try {
      return aead.encrypt(plainBytes, associatedData);
    } catch (GeneralSecurityException e) {
      throw new CrypterException("Problem encrypting.", e);
    }
  }

  /**
   * Decrypts a plain text value using Tink.
   *
   * @param encryptedBytes The value to decrypt.
   */
  public byte[] decrypt(byte[] encryptedBytes) throws CrypterException {
    if (encryptedBytes == null || encryptedBytes.length < 1) {
      throw new CrypterException("Bytes to decrypt cannot be null or empty.");
    }
    if (associatedData == null) {
      throw new CrypterException("Associated data cannot be null.");
    }

    try {
      return aead.decrypt(encryptedBytes, associatedData);
    } catch (GeneralSecurityException | NullPointerException e) {
      throw new CrypterException("Problem decrypting.", e);
    }
  }

  /**
   * Encrypts a plain text value using Tink.
   *
   * @param plainTextValue The value to encrypt.
   */
  public String encrypt(String plainTextValue) throws CrypterException {
    if (isBlank(plainTextValue)) {
      throw new CrypterException("Value to encrypt cannot be null or blank.");
    }
    if (associatedData == null) {
      throw new CrypterException("Associated data cannot be null.");
    }

    try {
      byte[] encryptedBytes = aead.encrypt(plainTextValue.getBytes(), associatedData);
      return Base64.getEncoder().encodeToString(encryptedBytes);
    } catch (GeneralSecurityException e) {
      throw new CrypterException("Problem encrypting.", e);
    }
  }

  /**
   * Decrypts a plain text value using Tink.
   *
   * @param encryptedValue The value to decrypt.
   */
  public String decrypt(String encryptedValue) throws CrypterException {
    if (isBlank(encryptedValue)) {
      throw new CrypterException("Value to decrypt cannot be null or blank.");
    }
    if (associatedData == null) {
      throw new CrypterException("Associated data cannot be null.");
    }

    try {
      byte[] encryptedBytes = Base64.getDecoder().decode(encryptedValue);
      return new String(aead.decrypt(encryptedBytes, associatedData));
    } catch (GeneralSecurityException | NullPointerException e) {
      throw new CrypterException("Problem decrypting.", e);
    }
  }

  /**
   * Encrypts a plain InputStream using Tink.
   *
   * @param plainInputStream The InputStream to encrypt.
   */
  public InputStream encrypt(InputStream plainInputStream) throws CrypterException {
    if (associatedData == null) {
      throw new CrypterException("Associated data cannot be null.");
    }
    try {
      if (plainInputStream == null || plainInputStream.available() < 1) {
        throw new CrypterException("Plain InputStream cannot be null or empty.");
      }
    } catch (IOException e) {
      throw new CrypterException("Problem reading data from plain InputStream.", e);
    }

    File tmpFile;
    try {
      tmpFile = File.createTempFile("encryption", ".tmp");
    } catch (IOException e) {
      throw new CrypterException("Unable to create temporary file for encryption.");
    }

    try (FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
        OutputStream encryptedOutputStream =
            streamingAead.newEncryptingStream(fileOutputStream, associatedData)) {

      byte[] byteBuffer = new byte[CHUNK_SIZE];
      int availableBytes = getAvailableBytesLessThanChunkSize(plainInputStream);

      while (availableBytes > 0) {
        int bytesRead = plainInputStream.read(byteBuffer);
        if (bytesRead < 1) {
          throw new IOException(
              String.format("Incorrect # of bytes read from plain InputStream: %d.", bytesRead));
        }
        encryptedOutputStream.write(byteBuffer, 0, availableBytes);
        availableBytes = getAvailableBytesLessThanChunkSize(plainInputStream);
      }
      encryptedOutputStream.close(); // need to close it here in order for it to flush
      return new FileInputStream(tmpFile);
    } catch (GeneralSecurityException | IOException e) {
      throw new CrypterException("Problem encrypting.", e);
    } finally {
      tmpFile.deleteOnExit();
    }
  }

  /**
   * Decrypts an encrypted InputStream using Tink.
   *
   * @param encryptedInputStream The InputStream to decrypt.
   */
  public InputStream decrypt(InputStream encryptedInputStream) throws CrypterException {
    if (associatedData == null) {
      throw new CrypterException("Associated data cannot be null.");
    }
    try {
      if (encryptedInputStream == null || encryptedInputStream.available() < 1) {
        throw new CrypterException("Encrypted InputStream cannot be null or empty.");
      }
    } catch (IOException e) {
      throw new CrypterException("Problem reading data from encrypted InputStream.", e);
    }

    try {
      return streamingAead.newDecryptingStream(encryptedInputStream, associatedData);
    } catch (GeneralSecurityException | IOException e) {
      throw new CrypterException("Problem decrypting.", e);
    }
  }

  private int getAvailableBytesLessThanChunkSize(InputStream inputStream) throws IOException {
    int available = inputStream.available();
    return available > CHUNK_SIZE ? CHUNK_SIZE : available;
  }

  private KeysetHandle initKeysetHandle(File keysetFile, KeyTemplate keyType)
      throws IOException, GeneralSecurityException {

    if (!keysetFile.getParentFile().exists()) {
      AccessController.doPrivileged(
          (PrivilegedAction<Boolean>) () -> keysetFile.getParentFile().mkdirs());
    }

    KeysetHandle keysetHandle = KeysetHandle.generateNew(keyType);

    try (OutputStream keysetFileOutputStream = getKeysetOutputStream(keysetFile)) {
      if (keysetFileOutputStream == null) {
        throw new IOException("Problem opening keyset file output stream.");
      }
      CleartextKeysetHandle.write(
          keysetHandle, JsonKeysetWriter.withOutputStream(keysetFileOutputStream));
    }

    try (OutputStream keysetFileOutputStream = getKeysetOutputStream(keysetFile)) {
      if (keysetFileOutputStream == null) {
        throw new IOException("Problem opening keyset file output stream.");
      }
      CleartextKeysetHandle.write(
          keysetHandle, JsonKeysetWriter.withOutputStream(keysetFileOutputStream));
    }
    return keysetHandle;
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

  private byte[] getAssociatedData() throws CrypterException {
    String primaryKeyId = Integer.toString(keysetHandle.getKeysetInfo().getPrimaryKeyId());

    Properties properties = getAssociatedDataProperties();
    if (properties.containsKey(primaryKeyId)) {
      LOGGER.debug("Found MAC (%s) in property file (%s).", primaryKeyId, associatedDataPath);
      return Base64.getDecoder().decode(properties.getProperty(primaryKeyId));
    }
    LOGGER.debug(
        "Could not find key (%s) in properties file (%s).", primaryKeyId, associatedDataPath);

    try {
      byte[] generatedAssociatedData = generateAssociatedData();
      storeAssociatedDataToProperties(primaryKeyId, generatedAssociatedData, properties);
      return generatedAssociatedData;
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

  private byte[] generateAssociatedData() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] generatedAssociatedData = new byte[ASSOCIATED_DATA_BYTE_SIZE];
    secureRandom.nextBytes(generatedAssociatedData);
    return generatedAssociatedData;
  }

  private void storeAssociatedDataToProperties(
      String primaryKeyId, byte[] associatedData, Properties properties) throws IOException {
    try (OutputStream outputStream = getAssociatedDataPropertiesOutputStream()) {
      if (outputStream == null) {
        throw new IOException("Problem initializing properties output stream.");
      }

      properties.setProperty(primaryKeyId, Base64.getEncoder().encodeToString(associatedData));
      properties.store(outputStream, null);
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

  private boolean isBlank(String string) {
    return string == null || string.trim().isEmpty();
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
