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
package org.codice.ddf.security.handler.api;

import ddf.security.SecurityConstants;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.xml.security.Init;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PKIAuthenticationTokenFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(PKIAuthenticationTokenFactory.class);

  private Merlin merlin;

  /** Initializes Merlin crypto object. */
  public void init() {
    // NOTE: THE TRUSTSTORE SHOULD BE USED FOR CERTIFICATE VALIDATION!!!!
    Path trustStorePath = Paths.get(SecurityConstants.getTruststorePath());

    if (!trustStorePath.isAbsolute()) {
      Path ddfHomePath = Paths.get(System.getProperty("ddf.home"));
      trustStorePath = Paths.get(ddfHomePath.toString(), trustStorePath.toString());
    }

    try (InputStream inputStream = Files.newInputStream(trustStorePath)) {
      KeyStore trustStore = SecurityConstants.newTruststore();
      trustStore.load(inputStream, SecurityConstants.getTruststorePassword().toCharArray());

      merlin = new Merlin();
      merlin.setKeyStore(trustStore);
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      LOGGER.warn("Unable to read merlin properties file. Unable to validate certificates.", e);
    }
    Init.init();
  }

  public PKIAuthenticationToken getTokenFromString(
      String certString, boolean isEncoded, String realm) {
    PKIAuthenticationToken token;
    byte[] certBytes =
        isEncoded
            ? Base64.getDecoder().decode(certString)
            : certString.getBytes(StandardCharsets.UTF_8);
    token = getTokenFromBytes(certBytes, realm);
    return token;
  }

  public PKIAuthenticationToken getTokenFromBytes(byte[] certBytes, String realm) {
    PKIAuthenticationToken token = null;
    try {
      if (certBytes == null || certBytes.length == 0) {
        throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN);
      }
      X509Certificate[] certs = merlin.getCertificatesFromBytes(certBytes);
      token = new PKIAuthenticationToken(certs[0].getSubjectDN(), certBytes, realm);
    } catch (WSSecurityException e) {
      LOGGER.debug("Unable to extract certificates from bytes: {}", e.getMessage(), e);
    }
    return token;
  }

  public PKIAuthenticationToken getTokenFromCerts(X509Certificate[] certs, String realm) {
    PKIAuthenticationToken token = null;
    if (certs != null && certs.length > 0) {
      byte[] certBytes = null;
      try {
        certBytes = getCertBytes(certs);
      } catch (WSSecurityException e) {
        LOGGER.debug("Unable to convert PKI certs to byte array.", e);
      }
      if (certBytes != null) {
        token = new PKIAuthenticationToken(certs[0].getSubjectDN(), certBytes, realm);
      }
    }
    return token;
  }

  /**
   * Returns a byte array representing a certificate chain.
   *
   * @param certs
   * @return byte[]
   * @throws WSSecurityException
   */
  private byte[] getCertBytes(X509Certificate[] certs) throws WSSecurityException {
    byte[] certBytes = null;

    if (merlin != null) {
      certBytes = merlin.getBytesFromCertificates(certs);
    }
    return certBytes;
  }
}
