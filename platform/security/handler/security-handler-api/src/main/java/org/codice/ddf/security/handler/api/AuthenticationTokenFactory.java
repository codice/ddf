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

import static org.apache.wss4j.common.WSS4JConstants.X509TOKEN_NS;

import ddf.security.PropertiesLoader;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.xml.security.Init;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.xml.XmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationTokenFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationTokenFactory.class);

  public static final String BASE64_ENCODING = WSConstants.SOAPMESSAGE_NS + "#Base64Binary";

  public static final String TOKEN_VALUE_SEPARATOR = "#";

  public static final String PKI_TOKEN_ID = "X509PKIPathv1";

  private static final String PKI_TOKEN_VALUE = X509TOKEN_NS + TOKEN_VALUE_SEPARATOR + PKI_TOKEN_ID;

  private Parser parser = new XmlParser();

  private Merlin merlin;

  private String signaturePropertiesPath;

  /** Initializes Merlin crypto object. */
  public void init() {
    try {
      merlin =
          new Merlin(
              PropertiesLoader.loadProperties(signaturePropertiesPath),
              AuthenticationTokenFactory.class.getClassLoader(),
              null);
    } catch (WSSecurityException | IOException e) {
      LOGGER.warn("Unable to read merlin properties file. Unable to validate certificates.", e);
    }
    Init.init();
  }

  /**
   * Creates a {@link AuthenticationToken} from a given username and password. Uses a {@link
   * UsernameTokenType} internally to store the username and password.
   *
   * @param username - user's username
   * @param password - user's password
   * @return a BaseAuthenticationToken containing the given username and password
   */
  public AuthenticationToken fromUsernamePassword(String username, String password, String ip) {
    BaseAuthenticationToken token =
        new BaseAuthenticationToken(
            username,
            Base64.getEncoder().encodeToString(username.getBytes())
                + ":"
                + Base64.getEncoder().encodeToString(password.getBytes()),
            ip);
    token.setType(AuthenticationTokenType.USERNAME);
    return token;
  }

  /**
   * Creates a {@link AuthenticationToken} from a given list of certificates. Uses a {@link
   * BinarySecurityTokenType} internally to store the certificates.
   *
   * @param certs - the user's certificates
   * @return a BaseAuthenticationToken containing the given certificates
   */
  public AuthenticationToken fromCertificates(X509Certificate[] certs, String ip) {
    if (certs == null || certs.length == 0) {
      return null;
    }

    BaseAuthenticationToken token =
        new BaseAuthenticationToken(certs[0].getSubjectX500Principal(), certs, ip);
    token.setType(AuthenticationTokenType.PKI);
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

  public void setSignaturePropertiesPath(String path) {
    this.signaturePropertiesPath = path;
  }
}
