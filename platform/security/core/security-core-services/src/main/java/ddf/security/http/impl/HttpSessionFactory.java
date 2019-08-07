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
import ddf.security.common.SecurityTokenHolder;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.http.SessionFactory;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class HttpSessionFactory implements SessionFactory {

  private static final int DEFAULT_EXPIRATION_TIME = 30;

  private int expirationTime = DEFAULT_EXPIRATION_TIME;

  /**
   * Synchronized method because of jettys getSession method is not thread safe. Additionally,
   * assures a SAML {@link SecurityTokenHolder} has been set on the {@link
   * SecurityConstants#SAML_ASSERTION} attribute
   *
   * @param httpRequest
   * @return
   */
  @Override
  public synchronized HttpSession getOrCreateSession(HttpServletRequest httpRequest) {
    HttpSession session = httpRequest.getSession(true);
    if (session.getAttribute(SecurityConstants.SAML_ASSERTION) == null) {
      session.setMaxInactiveInterval(Math.toIntExact(TimeUnit.MINUTES.toSeconds(expirationTime)));
      session.setAttribute(SecurityConstants.SAML_ASSERTION, new SecurityTokenHolder());
      SecurityLogger.audit(
          "Creating a new session with id {} for client {}.",
          Hashing.sha256().hashString(session.getId(), StandardCharsets.UTF_8).toString(),
          httpRequest.getRemoteAddr());
    }
    return session;
  }

  public void setExpirationTime(int expirationTime) {
    // Sets expirationTime to the default if the provided value is less than 2
    if (expirationTime >= 2) {
      this.expirationTime = expirationTime;
    } else {
      this.expirationTime = DEFAULT_EXPIRATION_TIME;
    }
  }
}
