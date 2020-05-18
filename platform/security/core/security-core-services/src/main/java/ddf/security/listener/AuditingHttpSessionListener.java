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
package ddf.security.listener;

import com.google.common.hash.Hashing;
import ddf.security.audit.SecurityLogger;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class AuditingHttpSessionListener implements HttpSessionListener {

  private SecurityLogger securityLogger;

  @Override
  public void sessionCreated(HttpSessionEvent se) {
    HttpSession session = se.getSession();

    securityLogger.audit(
        "Session {} created.",
        Hashing.sha256().hashString(session.getId(), StandardCharsets.UTF_8).toString());
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent se) {
    HttpSession session = se.getSession();

    securityLogger.audit(
        "Session {} destroyed.",
        Hashing.sha256().hashString(session.getId(), StandardCharsets.UTF_8).toString());
  }

  public void setSecurityLogger(SecurityLogger securityLogger) {
    this.securityLogger = securityLogger;
  }
}
