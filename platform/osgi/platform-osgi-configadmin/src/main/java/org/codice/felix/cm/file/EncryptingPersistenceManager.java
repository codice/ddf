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
package org.codice.felix.cm.file;

import static java.util.Collections.enumeration;
import static org.codice.felix.cm.file.ConfigurationContextImpl.FELIX_FILENAME;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID;

import com.google.common.annotations.VisibleForTesting;
import ddf.security.encryption.crypter.Crypter;
import ddf.security.encryption.crypter.Crypter.CrypterException;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import org.apache.felix.cm.PersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special kind of {@link WrappedPersistenceManager} that performs encryption on all string values
 * for all property dictionaries that get stored. These values are decrypted when the dictionaries
 * are loaded.
 *
 * <p>This allows the configuration data in the bundle cache to be fully encrypted.
 *
 * <p><b>See FELIX-4005 & FELIX-4556. This class cannot utilize Java 8 language constructs due to
 * maven bundle plugin 2.3.7</b>
 */
public class EncryptingPersistenceManager extends WrappedPersistenceManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(EncryptingPersistenceManager.class);

  private static final Logger AUDIT_LOG = LoggerFactory.getLogger("securityLogger");

  private static final String AUDIT_MESSAGE =
      "An encryption operation failed. Passwords might be exposed in the bundle cache.";

  private static final String AUDIT_MESSAGE_INIT =
      "The system did not initialize encryption keys correctly and is insecure.";

  private static final String CRYPTER_NAME = "encryption-persistence-manager";

  private static final Set<String> EXCLUDED_PROPERTIES = new HashSet<>();

  static {
    EXCLUDED_PROPERTIES.add(SERVICE_PID);
    EXCLUDED_PROPERTIES.add(SERVICE_FACTORYPID);
    EXCLUDED_PROPERTIES.add(FELIX_FILENAME);
  }

  private final EncryptionAgent agent;

  public EncryptingPersistenceManager(PersistenceManager persistenceManager) {
    super(persistenceManager);
    this.agent =
        AccessController.doPrivileged(
            (PrivilegedAction<EncryptionAgent>) () -> new EncryptionAgent(CRYPTER_NAME));
  }

  @Override
  public Enumeration getDictionaries() throws IOException {
    List<Dictionary> plaintextDictionaries = new ArrayList<>();
    Enumeration<Dictionary<String, Object>> encryptedDictionaries = super.getDictionaries();
    while (encryptedDictionaries.hasMoreElements()) {
      plaintextDictionaries.add(decryptAndReturnDictonary(encryptedDictionaries.nextElement()));
    }
    return enumeration(plaintextDictionaries);
  }

  @Override
  public Dictionary load(String pid) throws IOException {
    return decryptAndReturnDictonary(super.load(pid));
  }

  @Override
  public void store(String pid, Dictionary properties) throws IOException {
    super.store(pid, encryptAndReturnDictonary(properties));
  }

  /* The 2.3.7 version of the maven-bundle-plugin fails on lambdas so we need to use Java 7 constructs */
  /* squid:S1149 Synchronized classes Vector, Hashtable, Stack and StringBuffer should not be used
  but we are API bound on Dictionary and need to return one */
  @SuppressWarnings("squid:S1149")
  private Dictionary<String, Object> encryptAndReturnDictonary(Dictionary<String, Object> dict) {
    Dictionary<String, Object> encryptedDictionaryToReturn = new Hashtable<>(); // S1149
    Enumeration<String> keys = dict.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      encryptedDictionaryToReturn.put(key, encryptIfNecessary(key, dict.get(key)));
    }
    return encryptedDictionaryToReturn;
  }

  /* The 2.3.7 version of the maven-bundle-plugin fails on lambdas so we need to use Java 7 constructs */
  /* squid:S1149 Synchronized classes Vector, Hashtable, Stack and StringBuffer should not be used
  but we are API bound on Dictionary and need to return one */
  @SuppressWarnings("squid:S1149")
  private Dictionary<String, Object> decryptAndReturnDictonary(Dictionary<String, Object> dict) {
    Dictionary<String, Object> decryptedDictionaryToReturn = new Hashtable<>(); // S1149
    Enumeration<String> keys = dict.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      decryptedDictionaryToReturn.put(key, decryptIfNecessary(key, dict.get(key)));
    }
    return decryptedDictionaryToReturn;
  }

  private Object encryptIfNecessary(String key, Object value) {
    if (value instanceof String && !EXCLUDED_PROPERTIES.contains(key)) {
      return agent.encrypt((String) value);
    }
    return value;
  }

  private Object decryptIfNecessary(String key, Object value) {
    if (value instanceof String && !EXCLUDED_PROPERTIES.contains(key)) {
      return agent.decrypt((String) value);
    }
    return value;
  }

  // cannot use apache-commons libraries in this bundle
  private boolean isBlank(String string) {
    return string == null || string.trim().length() == 0;
  }

  /**
   * PAX Logging might not be fully configured and ready if there is a failure in the agent's
   * constructor. To remedy this, we will cache failure data to later throw back.
   */
  @VisibleForTesting
  class EncryptionAgent {
    private String initFailureMessage = null;

    private Exception initException = null;

    Crypter crypter;

    EncryptionAgent(String crypterName) {
      try {
        crypter = new Crypter(crypterName);
      } catch (CrypterException e) {
        initFailureMessage = "Problem initializing encryption agent.";
        initException = e;
      }
    }

    synchronized String encrypt(String plainTextValue) {
      if (initFailureMessage != null) {
        alertSystem(initFailureMessage, initException);
      }
      if (isBlank(plainTextValue)) {
        LOGGER.debug(
            "Failed to encrypt value of {}, because it was null or blank.", plainTextValue);
        return plainTextValue;
      }

      try {
        return AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> crypter.encrypt(plainTextValue));
      } catch (CrypterException e) {
        LOGGER.warn("Failed to encrypt to bundle cache. {}", e.getMessage());
        AUDIT_LOG.warn(AUDIT_MESSAGE);
        return plainTextValue;
      }
    }

    synchronized String decrypt(String encryptedValue) {
      if (initFailureMessage != null) {
        alertSystem(initFailureMessage, initException);
      }
      if (isBlank(encryptedValue)) {
        LOGGER.debug(
            "Failed to decrypt value of {}, because it was null or blank.", encryptedValue);
        return encryptedValue;
      }

      try {
        return AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> crypter.decrypt(encryptedValue));
      } catch (CrypterException e) {
        LOGGER.warn("Failed to decrypt from bundle cache. {}", e.getMessage());
        return encryptedValue;
      }
    }

    private void alertSystem(String msg, Exception e) {
      AUDIT_LOG.warn(AUDIT_MESSAGE_INIT);
      if (e != null) {
        LOGGER.error(msg, e);
        throw new IllegalStateException(msg, e);
      }
      LOGGER.error(msg);
      throw new IllegalStateException(msg);
    }
  }
}
