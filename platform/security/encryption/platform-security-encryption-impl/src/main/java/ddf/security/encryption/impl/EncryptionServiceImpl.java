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
package ddf.security.encryption.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import ddf.security.encryption.EncryptionService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionServiceImpl implements EncryptionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionServiceImpl.class);

  private static final Pattern ENC_PATTERN = Pattern.compile("^ENC\\((.*)\\)$");
  private static final String ENC_TEMPLATE = "ENC(%s)";
  private static final String KEYSET_FILE_NAME = "keyset.json";
  private static final byte[] ASSOCIATED_DATA = "associated.data".getBytes();

  @VisibleForTesting KeysetHandle keysetHandle;
  private Aead aead;

  public EncryptionServiceImpl() {
    String passwordDirectory = System.getProperty("ddf.etc").concat("/certs");
    String keysetLocation = Paths.get(passwordDirectory, KEYSET_FILE_NAME).toString();

    synchronized (EncryptionServiceImpl.class) {
      File keysetFile = new File(keysetLocation);
      InputStream keysetFileInputStream = null;
      OutputStream keysetFileOutputStream = null;
      try {
        AeadConfig.register();
        if (!keysetFile.exists()) {
          keysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES128_GCM);
          keysetFileOutputStream = Files.newOutputStream(Paths.get(keysetLocation));
          CleartextKeysetHandle.write(
              keysetHandle, JsonKeysetWriter.withOutputStream(keysetFileOutputStream));
        } else {
          keysetFileInputStream = Files.newInputStream(Paths.get(keysetLocation));
          keysetHandle =
              CleartextKeysetHandle.read(JsonKeysetReader.withInputStream(keysetFileInputStream));
        }
        aead = AeadFactory.getPrimitive(keysetHandle);
      } catch (GeneralSecurityException | IOException e) {
        LOGGER.warn("Problem initializing Tink. Enable debug logging for more information.");
        LOGGER.debug("", e);
      } finally {
        // close streams
        try {
          keysetFileInputStream.close();
        } catch (IOException | NullPointerException ignore) {
        }
        try {
          keysetFileOutputStream.close();
        } catch (IOException | NullPointerException ignore) {
        }
      }
    }
  }

  /**
   * Encrypts a plain text value using Tink.
   *
   * @param plainTextValue The value to encrypt.
   */
  public synchronized String encrypt(String plainTextValue) {
    try {
      byte[] encryptedBytes = aead.encrypt(plainTextValue.getBytes(), ASSOCIATED_DATA);
      return Base64.getEncoder().encodeToString(encryptedBytes);
    } catch (GeneralSecurityException | NullPointerException e) {
      LOGGER.debug("Key and encryption service failed to set up. Failed to encrypt.", e);
      return plainTextValue;
    }
  }

  /**
   * Decrypts a plain text value using Tink
   *
   * @param encryptedValue The value to decrypt.
   */
  public synchronized String decrypt(String encryptedValue) {
    try {
      byte[] encryptedBytes = Base64.getDecoder().decode(encryptedValue);
      return new String(aead.decrypt(encryptedBytes, ASSOCIATED_DATA));
    } catch (GeneralSecurityException | NullPointerException e) {
      LOGGER.debug("Key and encryption service failed to set up. Failed to decrypt.", e);
      return encryptedValue;
    }
  }

  /**
   * {@inheritDoc}
   *
   * <pre>{@code
   * One can encrypt passwords using the security:encrypt console command.
   *
   * user@local>security:encrypt secret
   * c+GitDfYAMTDRESXSDDsMw==
   *
   * A wrapped encrypted password is wrapped in ENC() as follows: ENC(HsOcGt8seSKc34sRUYpakQ==)
   *
   * }</pre>
   */
  @Override
  public String decryptValue(String wrappedEncryptedValue) {
    if (StringUtils.isEmpty(wrappedEncryptedValue)) {
      return wrappedEncryptedValue;
    }

    String encryptedValue = unwrapEncryptedValue(wrappedEncryptedValue);

    // If the password is not in the form ENC(my-encrypted-password),
    // we assume the password is not encrypted.
    if (wrappedEncryptedValue.equals(encryptedValue)) {
      return wrappedEncryptedValue;
    }
    LOGGER.debug("Unwrapped encrypted password is now being decrypted");
    return decrypt(encryptedValue);
  }

  @Override
  public String encryptValue(String unwrappedPlaintext) {
    if (StringUtils.isEmpty(unwrappedPlaintext)) {
      return unwrappedPlaintext;
    }

    return String.format(ENC_TEMPLATE, encrypt(unwrappedPlaintext));
  }

  /**
   * {@inheritDoc}
   *
   * <p>Given a string that starts with 'ENC(' and ends with ')', returns the in-between substring.
   * This method is meant to remove the wrapping notation for encrypted values, typically passwords.
   *
   * <p>If the input is a password and is not in the form ENC(my-encrypted-password), we assume the
   * password is not encrypted.
   *
   * @param wrappedEncryptedValue The wrapped encrypted value, in the form
   *     'ENC(my-encrypted-value)'.
   * @return The value within the parenthesis.
   */
  @Override
  public String unwrapEncryptedValue(String wrappedEncryptedValue) {
    if (wrappedEncryptedValue == null) {
      LOGGER.debug("You have provided a null password in your configuration.");
      return null;
    }

    // Get the value in parenthesis. In this example, ENC(my-encrypted-password),
    // m.group(1) would return my-encrypted-password.
    Matcher m = ENC_PATTERN.matcher(wrappedEncryptedValue);
    if (m.find()) {
      LOGGER.debug("Wrapped encrypted password value found.");
      return m.group(1);
    }
    return wrappedEncryptedValue;
  }
}
