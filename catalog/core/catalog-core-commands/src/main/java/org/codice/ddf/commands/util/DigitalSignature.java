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
package org.codice.ddf.commands.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DigitalSignature {

  private static final Logger LOGGER = LoggerFactory.getLogger(DigitalSignature.class);

  private static final int BUFFER_SIZE = 1024;

  private static final int OFFSET = 0;

  private KeyStore keyStore;

  public DigitalSignature() {
    this.keyStore = AccessController.doPrivileged((PrivilegedAction<KeyStore>) getSystemKeyStore());
  }

  public KeyStore getSystemKeyStore() {
    KeyStore keyStore;

    try {
      keyStore = KeyStore.getInstance(System.getProperty("javax.net.ssl.keyStoreType"));

    } catch (KeyStoreException e) {
      LOGGER.warn(
          "Unable to create keystore instance of type {}",
          System.getProperty("javax.net.ssl.keyStoreType"),
          e);
      return null;
    }

    Path keyStoreFile = new File(System.getProperty("javax.net.ssl.keyStore")).toPath();
    Path ddfHomePath = Paths.get(System.getProperty("ddf.home"));

    if (!keyStoreFile.isAbsolute()) {
      keyStoreFile = Paths.get(ddfHomePath.toString(), keyStoreFile.toString());
    }

    String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");

    if (!Files.isReadable(keyStoreFile)) {
      LOGGER.warn("Unable to read system key/trust store files: [ {} ] ", keyStoreFile);
      return null;
    }

    try (InputStream kfis = Files.newInputStream(keyStoreFile)) {
      keyStore.load(kfis, keyStorePassword.toCharArray());
    } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
      LOGGER.warn("Unable to load system key file.", e);
    }

    return keyStore;
  }

  public DigitalSignature(KeyStore keyStore) {
    this.keyStore = keyStore;
  }

  public byte[] createDigitalSignature(InputStream data, String alias, String password)
      throws IOException {
    PrivateKey privateKey = getPrivateKey(alias, password);

    if (privateKey == null) {
      throw new CatalogCommandRuntimeException("Unable to retrieve private key");
    }

    try {
      Signature rsa = Signature.getInstance("SHA256withRSA");

      rsa.initSign(privateKey);

      byte[] buffer = new byte[BUFFER_SIZE];
      int len;

      while ((len = data.read(buffer)) >= 0) {
        rsa.update(buffer, OFFSET, len);
      }

      return rsa.sign();
    } catch (SignatureException | InvalidKeyException | NoSuchAlgorithmException e) {
      String message = "An error occurred while signing file";
      LOGGER.debug(message, e);
      throw new CatalogCommandRuntimeException(message, e);
    }
  }

  public boolean verifyDigitalSignature(
      InputStream data, InputStream signature, String certificateAlias) throws IOException {
    byte[] sigToVerify = IOUtils.toByteArray(signature);

    Certificate certificate = getCertificate(certificateAlias);

    if (certificate == null) {
      throw new CatalogCommandRuntimeException("Unable to retrieve certificate");
    }

    try {
      Signature rsa = Signature.getInstance("SHA256withRSA");
      rsa.initVerify(certificate);

      byte[] buffer = new byte[BUFFER_SIZE];
      int len;

      while ((len = data.read(buffer)) >= 0) {
        rsa.update(buffer, OFFSET, len);
      }

      return rsa.verify(sigToVerify);
    } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
      String message = "An error occurred while verifying file";
      LOGGER.debug(message, e);
      throw new CatalogCommandRuntimeException(message, e);
    }
  }

  private PrivateKey getPrivateKey(String alias, String password) {
    try {
      Key key = keyStore.getKey(alias, password.toCharArray());

      if (key instanceof PrivateKey) {
        return (PrivateKey) key;
      }

    } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
      LOGGER.debug("Unable to retrieve private key from key store", e);
    }

    return null;
  }

  private Certificate getCertificate(String alias) {
    try {
      return keyStore.getCertificate(alias);
    } catch (KeyStoreException e) {
      LOGGER.debug("Unable to retrieve certificate from key store", e);
    }

    return null;
  }
}
