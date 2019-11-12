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

import java.security.cert.X509Certificate;
import java.util.Base64;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.shiro.authc.AuthenticationToken;

public class AuthenticationTokenFactory {

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
}
