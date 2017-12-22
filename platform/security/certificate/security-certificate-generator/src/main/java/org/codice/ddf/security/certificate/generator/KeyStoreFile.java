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
package org.codice.ddf.security.certificate.generator;

import ddf.security.SecurityConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade class for a Java Keystore (JKS) file. Exposes a few high-level behaviors to abstract away
 * the complexity of JCA/JCE, as well as file I/O operations.
 */
public class KeyStoreFile {

  private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreFile.class);

  protected KeyStore keyStore;

  protected File file;

  private char[] password;

  static KeyStoreFile openFile(String filePath, char[] password) {

    KeyStoreFile facade = new KeyStoreFile();
    File file;
    char[] pw = PkiTools.formatPassword(password);
    KeyStore keyStore = null;
    try {
      file = PkiTools.createFileObject(filePath);
      try (FileInputStream resource = new FileInputStream(file)) {
        keyStore = SecurityConstants.newKeystore();
        keyStore.load(resource, pw);
      }
    } catch (GeneralSecurityException | IOException e) {
      String msg = "Could not create new instance of KeyStoreFile";
      throw new CertificateGeneratorException(msg, e);
    }

    facade.file = file;
    facade.keyStore = keyStore;
    facade.password = pw;
    return facade;
  }

  /**
   * Return the aliases of the items in the key store. Return null if there is an error.
   *
   * @return List of aliases or throw an exception
   */
  public List<String> aliases() {

    try {
      return Collections.list(keyStore.aliases());
    } catch (KeyStoreException e) {
      throw new CertificateGeneratorException("Could not retrieve keys from keystore", e);
    }
  }

  public boolean isKey(String alias) {
    try {
      return keyStore.isKeyEntry(alias);
    } catch (KeyStoreException e) {
      throw new CertificateGeneratorException("Could not determine if alias is a key", e);
    }
  }

  /**
   * Retrieve keystore entry, given the entry's alias. If the entry is encrypted, this method tries
   * to decrypt it using the keystore's password.
   *
   * @param alias of the entry to retrieve
   * @return concrete subclass of Keystore.Entry
   */
  public KeyStore.Entry getEntry(String alias) {
    KeyStore.Entry entry = null;
    try {
      entry = getProtectedEntry(alias);
    } catch (UnsupportedOperationException e) {
      // If keystore entry not password protected, using a password generates exception
      entry = getUnprotectedEntry(alias);
    }
    return entry;
  }

  @SuppressWarnings("squid:S00112") /* Unrecoverable state that we do not want caught later */
  KeyStore.Entry getUnprotectedEntry(String alias) {
    try {
      return keyStore.getEntry(alias, null);
    } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException e) {
      throw new CertificateGeneratorException(
          String.format("Could not get keystore entry %s", alias), e);
    }
  }

  @SuppressWarnings("squid:S00112") /* Unrecoverable state that we do not want caught later */
  KeyStore.Entry getProtectedEntry(String alias) {
    try {
      return keyStore.getEntry(alias, getPasswordObject());
    } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException e) {
      throw new CertificateGeneratorException(
          String.format("Could not get keystore entry %s", alias), e);
    }
  }

  KeyStore.PasswordProtection getPasswordObject() {
    return new KeyStore.PasswordProtection(password);
  }

  /**
   * Add a new entry to the keystore. Use the given alias.
   *
   * @param alias of keystore entry
   * @param entry instance
   */
  @SuppressWarnings("squid:S00112") /* Unrecoverable state that we do not want caught later */
  public void setEntry(String alias, KeyStore.Entry entry) {
    try {
      keyStore.setEntry(alias, entry, getPasswordObject());
    } catch (KeyStoreException e) {
      throw new CertificateGeneratorException(
          String.format("Could not add %s to keystore", alias), e);
    }
  }

  /**
   * Remove the key store entry at the given alias. If the alias does not exist, log that it does
   * not exist.
   *
   * @param alias the name of the entry in the keystore
   * @return true if entry is not in the keystore false otherwise
   */
  public boolean deleteEntry(String alias) {
    try {
      keyStore.deleteEntry(alias);
    } catch (KeyStoreException e) {
      LOGGER.info("Attempted to remove key named {} from keyStore. No such key", alias);
    }
    return isKey(alias);
  }

  /** Save the keyStore to the original file and encrypt it with the original password. */
  @SuppressWarnings("squid:S00112") /* Unrecoverable state that we do not want caught later */
  public void save() {
    // Use the try-with-resources statement. If an exception is raised, rethrow the exception.
    try (FileOutputStream fd = new FileOutputStream(file)) {
      keyStore.store(fd, password);
    } catch (Exception e) {
      throw new CertificateGeneratorException(
          String.format("Could not save the keystore %s", file.getAbsolutePath()), e);
    }
  }
}
