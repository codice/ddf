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
package org.codice.ddf.configuration;

import java.util.function.Consumer;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jasypt.exceptions.EncryptionInitializationException;

/**
 * PropertyEncryptor holds a string potentially containing values of the format ENC(prop) and
 * handles decrypting those values.
 */
public class PropertyEncryptor {

  private final String prefix;

  private final String suffix;

  private final StringEncryptor encryptor;

  public PropertyEncryptor() {
    prefix = getConfig("ENCRYPTION_PREFIX", "ENC(");
    suffix = getConfig("ENCRYPTION_SUFFIX", ")");
    encryptor = createEncryptor();
  }

  private static StringEncryptor createEncryptor() {
    SimpleStringPBEConfig config = new SimpleStringPBEConfig();

    updateConfig(config::setPassword, "ENCRYPTION_PASSWORD", "changeit");
    updateConfig(config::setAlgorithm, "ENCRYPTION_ALGORITHM", "PBEWithHmacSHA512AndAES_256");
    updateConfig(
        config::setIvGeneratorClassName,
        "ENCRYPTION_IV_CLASSNAME",
        "org.jasypt.iv.RandomIvGenerator");
    updateConfig(config::setStringOutputType, "ENCRYPTION_OUTPUT_TYPE", "base64");
    updateConfig(config::setKeyObtentionIterations, "ENCRYPTION_ITERATIONS", null);
    updateConfig(config::setSaltGeneratorClassName, "ENCRYPTION_SALT_CLASSNAME", null);
    updateConfig(config::setProviderName, "ENCRYPTION_PROVIDER_NAME", null);
    updateConfig(config::setProviderClassName, "ENCRYPTION_PROVIDER_CLASSNAME", null);
    updateConfig(config::setPoolSize, "ENCRYPTION_POOL_SIZE", null);

    StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
    encryptor.setConfig(config);

    try {
      encryptor.encrypt("test");
    } catch (EncryptionInitializationException e) {
      throw new IllegalArgumentException(e);
    }

    return encryptor;
  }

  private static String getConfig(String environmentVar, String defaultValue) {
    String result = System.getenv(environmentVar);

    if (result == null || result.isBlank()) {
      result = System.getProperty(environmentVar.toLowerCase().replace("_", "."));
    }

    if (result == null || result.isBlank()) {
      result = defaultValue;
    }

    return result;
  }

  private static void updateConfig(
      Consumer<String> configConsumer, String environmentVar, String defaultValue) {
    String config = getConfig(environmentVar, defaultValue);

    if (config != null && !config.isBlank()) {
      configConsumer.accept(config);
    }
  }

  public String encrypt(String value) {
    if (value != null && !value.isBlank() && !value.startsWith(prefix) && !value.endsWith(suffix)) {
      return prefix + encryptor.encrypt(value) + suffix;
    } else {
      return value;
    }
  }

  public String decrypt(String value) {
    if (value != null && !value.isBlank() && value.startsWith(prefix) && value.endsWith(suffix)) {
      return encryptor.decrypt(value.substring(prefix.length(), value.length() - suffix.length()));
    } else {
      return value;
    }
  }
}
