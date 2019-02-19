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
import ddf.security.encryption.EncryptionService;
import ddf.security.encryption.crypter.Crypter;
import ddf.security.encryption.crypter.Crypter.CrypterException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionServiceImpl implements EncryptionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionServiceImpl.class);

  private static final Pattern ENC_PATTERN = Pattern.compile("^ENC\\((.*)\\)$");
  private static final String ENC_TEMPLATE = "ENC(%s)";
  @VisibleForTesting static final String CRYPTER_NAME = "encryption-service";

  @VisibleForTesting Crypter crypter;

  public EncryptionServiceImpl() {
    crypter =
        AccessController.doPrivileged((PrivilegedAction<Crypter>) () -> new Crypter(CRYPTER_NAME));
  }

  /**
   * Encrypts a plain text value. Returns no-op in the case of a problem.
   *
   * @param plainTextValue The value to encrypt.
   */
  public synchronized String encrypt(String plainTextValue) {
    try {
      return crypter.encrypt(plainTextValue);
    } catch (CrypterException e) {
      LOGGER.debug("Failed to encrypt string.", e);
      return plainTextValue;
    }
  }

  /**
   * Decrypts a plain text value. Returns no-op in the case of a problem.
   *
   * @param encryptedValue The value to decrypt.
   */
  public synchronized String decrypt(String encryptedValue) {
    try {
      return crypter.decrypt(encryptedValue);
    } catch (CrypterException e) {
      LOGGER.debug("Failed to decrypt string of value %s.", encryptedValue, e);
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
  public String encryptValue(String unwrappedPlaintext) {
    if (StringUtils.isEmpty(unwrappedPlaintext)) {
      return unwrappedPlaintext;
    }

    return String.format(ENC_TEMPLATE, encrypt(unwrappedPlaintext));
  }

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
    return decrypt(encryptedValue);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Given a string that starts with 'ENC(' and ends with ')', returns the in-between substring.
   * This method is meant to remove the wrapping notation for encrypted values, typically passwords.
   *
   * <p>If the input is a password and is not in the form ENC(my-encrypted-password), we assume the
   * password is not encrypted. Returns a no-op in the case of a problem.
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
