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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import org.apache.shiro.authc.AuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseAuthenticationToken implements AuthenticationToken {

  private X509Certificate[] x509Certs;

  private String requestURI;

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseAuthenticationToken.class);

  /**
   * Represents the account identity submitted during the authentication process.
   *
   * <p>
   *
   * <p>Most application authentications are username/password based and have this object represent
   * a username. However, this can also represent the DN from an X509 certificate, or any other
   * unique identifier.
   *
   * <p>
   *
   * <p>Ultimately, the object is application specific and can represent any account identity (user
   * id, X.509 certificate, etc).
   */
  protected Object principal;

  /**
   * Represents the credentials submitted by the user during the authentication process that
   * verifies the submitted Principal account identity.
   *
   * <p>
   *
   * <p>Most application authentications are username/password based and have this object represent
   * a submitted password.
   *
   * <p>
   *
   * <p>Ultimately, the credentials Object is application specific and can represent any credential
   * mechanism.
   */
  protected Object credentials;

  protected String ip;

  protected boolean allowGuest;

  public BaseAuthenticationToken(Object principal, Object credentials, String ip) {
    this.principal = principal;
    this.credentials = credentials;
    this.ip = formatIpAddress(ip);
  }

  public boolean getAllowGuest() {
    return allowGuest;
  }

  public void setAllowGuest(boolean allowGuest) {
    this.allowGuest = allowGuest;
  }

  @Override
  public Object getPrincipal() {
    return principal;
  }

  @Override
  public Object getCredentials() {
    return credentials;
  }

  protected void setCredentials(Object o) {
    this.credentials = o;
  }

  public void setX509Certs(X509Certificate[] x509Certs) {
    this.x509Certs = x509Certs;
  }

  public X509Certificate[] getX509Certs() {
    return x509Certs;
  }

  public String getRequestURI() {
    return requestURI;
  }

  public void setRequestURI(String requestURI) {
    this.requestURI = requestURI;
  }

  public String getCredentialsAsString() {
    return credentials.toString();
  }

  // IPv6 addresses should be contained within brackets to conform
  // to the spec IETF RFC 2732
  private static String formatIpAddress(String ipAddress) {
    try {
      if (InetAddress.getByName(ipAddress) instanceof Inet6Address && !ipAddress.contains("[")) {
        ipAddress = "[" + ipAddress + "]";
      }
    } catch (UnknownHostException e) {
      LOGGER.debug("Error formatting the ip address, using the unformatted ipaddress", e);
    }

    return ipAddress;
  }

  public String getIpAddress() {
    return ip;
  }
}
