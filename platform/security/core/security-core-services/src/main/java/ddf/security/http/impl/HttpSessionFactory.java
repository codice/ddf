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
package ddf.security.http.impl;

import com.google.common.hash.Hashing;
import ddf.security.SecurityConstants;
import ddf.security.audit.SecurityLogger;
import ddf.security.common.PrincipalHolder;
import ddf.security.http.SessionFactory;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class HttpSessionFactory implements SessionFactory {

  private int expirationTime;

  private SecurityLogger securityLogger;

  /**
   * Synchronized method because of jettys getSession method is not thread safe. Additionally,
   * assures a SAML {@link PrincipalHolder} has been set on the {@link
   * SecurityConstants#SECURITY_TOKEN_KEY} attribute
   *
   * @param httpRequest
   * @return
   */
  @Override
  public synchronized HttpSession getOrCreateSession(HttpServletRequest httpRequest) {
    HttpSession session = httpRequest.getSession(true);
    if (session.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY) == null) {
      session.setMaxInactiveInterval(Math.toIntExact(TimeUnit.MINUTES.toSeconds(expirationTime)));
      session.setAttribute(SecurityConstants.SECURITY_TOKEN_KEY, new PrincipalHolder());
      securityLogger.audit(
          "Creating a new session with id {} for client {}.",
          Hashing.sha256().hashString(session.getId(), StandardCharsets.UTF_8).toString(),
          httpRequest.getRemoteAddr());
    }
    return session;
  }

  public void setExpirationTime(int expirationTime) {
    this.expirationTime = expirationTime;
  }

  public void setSecurityLogger(SecurityLogger securityLogger) {
    this.securityLogger = securityLogger;
  }
}
