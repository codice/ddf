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
package org.codice.ddf.security.rest.authentication.impl;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.http.SessionFactory;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;
import org.codice.ddf.security.rest.authentication.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

  private SecurityManager securityManager;

  private SessionFactory sessionFactory;

  public AuthenticationServiceImpl(SecurityManager securityManager, SessionFactory sessionFactory) {
    this.securityManager = securityManager;
    this.sessionFactory = sessionFactory;
  }

  @Override
  public void login(HttpServletRequest request, String username, String password, String prevurl)
      throws SecurityServiceException {

    // Make sure we're using HTTPS
    if (!request.isSecure()) {
      throw new IllegalArgumentException("Authentication request must use TLS.");
    }

    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }

    // Create an authentication token
    UPAuthenticationToken authenticationToken = new UPAuthenticationToken(username, password);

    // Authenticate
    Subject subject = securityManager.getSubject(authenticationToken);
    if (subject == null) {
      throw new SecurityServiceException("Authentication failed");
    }

    for (Object principal : subject.getPrincipals()) {
      if (principal instanceof SecurityAssertion) {
        SecurityToken securityToken = ((SecurityAssertion) principal).getSecurityToken();

        if (securityToken == null) {
          LOGGER.debug("Cannot add null security token to session");
          continue;
        }

        // Create a session and add the security token
        session = sessionFactory.getOrCreateSession(request);
        SecurityTokenHolder holder =
            (SecurityTokenHolder) session.getAttribute(SecurityConstants.SAML_ASSERTION);
        holder.setSecurityToken(securityToken);
      }
    }
  }
}
