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
package org.codice.ddf.security.session.management.impl;

import ddf.security.SecurityConstants;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.http.SessionFactory;
import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.session.management.service.SessionManagementService;

public class SessionManagementServiceImpl implements SessionManagementService {

  private SessionFactory sessionFactory;

  @Override
  public String getExpiry(HttpServletRequest request) {
    long timeLeft = 0;
    HttpSession session = sessionFactory.getOrCreateSession(request);
    if (session == null) {
      return Long.toString(timeLeft);
    }

    Object securityToken = session.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY);
    if (!(securityToken instanceof SecurityTokenHolder)) {
      return Long.toString(timeLeft);
    }

    timeLeft = session.getMaxInactiveInterval() * 1000L;
    return Long.toString(timeLeft);
  }

  @Override
  public URI getInvalidate(HttpServletRequest request) {
    String requestQueryString = request.getQueryString();
    return URI.create(
        SystemBaseUrl.EXTERNAL
            .constructUrl("/logout")
            .concat(requestQueryString != null ? "?" + requestQueryString : ""));
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }
}
