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

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import org.apache.felix.cm.PersistenceManager;
import org.keyczar.Crypter;
import org.keyczar.KeyczarTool;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special kind of {@link WrappedPersistenceManager} that performs encryption on all string values
 * for all property dictionaries that get stored. These values are decrypted when the dictionaries
 * are loaded.
 *
 * <p>This allows the configuration data in the bundle cache to be fully encrypted.
 */
public class EncryptingPersistenceManager extends WrappedPersistenceManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(EncryptingPersistenceManager.class);

  private static final Logger AUDIT_LOG = LoggerFactory.getLogger("securityLogger");

  private static final String AUDIT_MESSAGE =
      "An encryption operation failed. Passwords might be exposed in the bundle cache.";

  private static final String AUDIT_MESSAGE_INIT =
      "The system did not initialize encryption keys correctly and is insecure.";

  private static final Set<String> EXCLUDED_PROPERTIES = new HashSet<>();

  static {
    EXCLUDED_PROPERTIES.add(SERVICE_PID);
    EXCLUDED_PROPERTIES.add(SERVICE_FACTORYPID);
    EXCLUDED_PROPERTIES.add(FELIX_FILENAME);
  }

  private class InitOperation implements PrivilegedAction<EncryptionAgent> {
    private final String passwordDirectory;

    InitOperation(String passwordDirectory) {
      this.passwordDirectory = passwordDirectory;
    }

    @Override
    public EncryptionAgent run() {
      return new EncryptionAgent(passwordDirectory);
    }
  }

  private final EncryptionAgent agent;

  public EncryptingPersistenceManager(PersistenceManager persistenceManager) {
    this(
        persistenceManager,
        FrameworkUtil.getBundle(EncryptingPersistenceManager.class)
            .getDataFile("")
            .getAbsolutePath());
  }

  public EncryptingPersistenceManager(
      PersistenceManager persistenceManager, String passwordDirectory) {
    super(persistenceManager);
    this.agent = AccessController.doPrivileged(new InitOperation(passwordDirectory));
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

  /**
   * PAX Logging might not be fully configured and ready if there is a failure in the agent's
   * constructor. To remedy this, we will cache failure data to later throw back.
   */
  private class EncryptionAgent {
    private String initFailureMessage = null;

    private Exception initException = null;

    private Crypter crypter;

    EncryptionAgent(String pwDirString) {
      File pwDir = new File(pwDirString);
      if (!pwDir.mkdirs() && !pwDir.exists()) {
        initFailureMessage =
            "Cannot create required directory for encryption ["
                + pwDir.getAbsolutePath()
                + "]. This might be a permissions issue. Check "
                + "OS settings, file permissions, and the active user account. Then unzip a "
                + "new instance and try again";
        throw new IllegalStateException(initFailureMessage);
      }

      String[] children = pwDir.list();
      if (children == null) {
        initFailureMessage =
            "Expected directory ["
                + pwDir.getAbsolutePath()
                + "] to "
                + "exist and be accessible. Check OS settings, file permissions, and the "
                + "active user account. Then unzip a new instance and try again";
        throw new IllegalStateException(initFailureMessage);
      }

      synchronized (EncryptionAgent.class) {
        if (!Arrays.asList(children).contains("meta")) {
          KeyczarTool.main(
              new String[] {
                "create", "--location=" + pwDirString, "--purpose=crypt", "--name=Password"
              });
          KeyczarTool.main(
              new String[] {"addkey", "--location=" + pwDirString, "--status=primary"});
        }
        try {
          this.crypter = new Crypter(pwDirString);
        } catch (Exception e) {
          initFailureMessage = "Could not setup encryption. ";
          initException = e;
          throw new IllegalStateException(initFailureMessage, initException);
        }
      }
    }

    synchronized String encrypt(String plainTextValue) {
      if (initFailureMessage != null) {
        alertSystem(initFailureMessage, initException);
      }
      try {
        return crypter.encrypt(plainTextValue);
      } catch (Exception e) {
        LOGGER.error("Failed to encrypt to bundle cache. {}", e);
        AUDIT_LOG.warn(AUDIT_MESSAGE);
        return plainTextValue;
      }
    }

    synchronized String decrypt(String encryptedValue) {
      if (initFailureMessage != null) {
        alertSystem(initFailureMessage, initException);
      }
      try {
        return crypter.decrypt(encryptedValue);
      } catch (Exception e) {
        LOGGER.error("Failed to decrypt from bundle cache. {}", e);
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
