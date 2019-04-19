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

import ddf.security.PropertiesLoader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

  private String signaturePropertiesPath;

  /** Initializes Merlin crypto object. */
  public void init() {
    try {
      merlin =
          new Merlin(
              PropertiesLoader.loadProperties(signaturePropertiesPath),
              PKIAuthenticationTokenFactory.class.getClassLoader(),
              null);
    } catch (WSSecurityException | IOException e) {
      LOGGER.warn("Unable to read merlin properties file. Unable to validate certificates.", e);
    }
    Init.init();
  }

  public PKIAuthenticationToken getTokenFromString(String certString, boolean isEncoded) {
    PKIAuthenticationToken token;
    byte[] certBytes =
        isEncoded
            ? Base64.getDecoder().decode(certString)
            : certString.getBytes(StandardCharsets.UTF_8);
    token = getTokenFromBytes(certBytes);
    return token;
  }

  public PKIAuthenticationToken getTokenFromBytes(byte[] certBytes) {
    PKIAuthenticationToken token = null;
    try {
      if (certBytes == null || certBytes.length == 0) {
        throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN);
      }
      X509Certificate[] certs = merlin.getCertificatesFromBytes(certBytes);
      token = new PKIAuthenticationToken(certs[0].getSubjectDN(), certBytes);
    } catch (WSSecurityException e) {
      LOGGER.debug("Unable to extract certificates from bytes: {}", e.getMessage(), e);
    }
    return token;
  }

  public PKIAuthenticationToken getTokenFromCerts(X509Certificate[] certs) {
    PKIAuthenticationToken token = null;
    if (certs != null && certs.length > 0) {
      byte[] certBytes = null;
      try {
        certBytes = getCertBytes(certs);
      } catch (WSSecurityException e) {
        LOGGER.debug("Unable to convert PKI certs to byte array.", e);
      }
      if (certBytes != null) {
        token = new PKIAuthenticationToken(certs[0].getSubjectDN(), certBytes);
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

  public String getSignaturePropertiesPath() {
    return signaturePropertiesPath;
  }

  public void setSignaturePropertiesPath(String path) {
    this.signaturePropertiesPath = path;
  }
}
