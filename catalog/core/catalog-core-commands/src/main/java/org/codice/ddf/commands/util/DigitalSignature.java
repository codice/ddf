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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.security.common.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DigitalSignature {

  private static final Logger LOGGER = LoggerFactory.getLogger(DigitalSignature.class);

  private static final int BUFFER_SIZE = 1024;

  private static final int OFFSET = 0;

  private KeyStore keyStore;

  public DigitalSignature() {
    this.keyStore = Security.getInstance().getSystemKeyStore();
  }

  public DigitalSignature(KeyStore keyStore) {
    this.keyStore = keyStore;
  }

  public byte[] createDigitalSignature(InputStream data, String alias, String password)
      throws IOException {
    PrivateKey privateKey = getPrivateKey(alias, password);

    if (privateKey == null) {
      throw new CatalogCommandRuntimeException("");
    }

    try (BufferedInputStream bufferedInputStream = new BufferedInputStream(data)) {
      Signature rsa = Signature.getInstance("SHA256withRSA");

      rsa.initSign(getPrivateKey(alias, password));

      byte[] buffer = new byte[BUFFER_SIZE];
      int len;

      while ((len = bufferedInputStream.read(buffer)) >= 0) {
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
      InputStream data, InputStream signature, String certificateAlias)
      throws IOException, CatalogCommandRuntimeException {
    byte[] sigToVerify = IOUtils.toByteArray(signature);

    Certificate certificate = getCertificate(certificateAlias);

    if (certificate == null) {
      throw new CatalogCommandRuntimeException("");
    }

    try (BufferedInputStream bufferedInputStream = new BufferedInputStream(data)) {
      Signature rsa = Signature.getInstance("SHA256withRSA");
      rsa.initVerify(certificate);

      byte[] buffer = new byte[BUFFER_SIZE];
      int len;

      while (bufferedInputStream.available() != 0) {
        len = bufferedInputStream.read(buffer);
        rsa.update(buffer, OFFSET, len);
      }

      return rsa.verify(sigToVerify);
    } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
      String message = "An error occurred while signing file";
      LOGGER.debug(message, e);
      throw new CatalogCommandRuntimeException(message, e);
    }
  }

  private PrivateKey getPrivateKey(String alias, String password) {
    try {
      return (PrivateKey) keyStore.getKey(alias, password.toCharArray());
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
