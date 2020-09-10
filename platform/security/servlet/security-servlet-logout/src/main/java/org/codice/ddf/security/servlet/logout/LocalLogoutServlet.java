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
package org.codice.ddf.security.servlet.logout;

import ddf.security.SecurityConstants;
import ddf.security.audit.SecurityLogger;
import ddf.security.common.PrincipalHolder;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.token.storage.api.TokenStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalLogoutServlet extends HttpServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(LocalLogoutServlet.class);

  private final TokenStorage tokenStorage;

  private final String redirectUri;

  private final SecurityLogger securityLogger;

  public LocalLogoutServlet(
      final TokenStorage tokenStorage,
      final String redirectUri,
      final SecurityLogger securityLogger) {
    this.tokenStorage = tokenStorage;
    this.redirectUri = redirectUri;
    this.securityLogger = securityLogger;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-cache, no-store");
    response.setHeader("Pragma", "no-cache");
    response.setContentType("text/html");

    invalidateSession(request, response);

    try {
      URIBuilder redirectUrlBuilder =
          new URIBuilder(SystemBaseUrl.EXTERNAL.constructUrl(redirectUri));
      redirectUrlBuilder.addParameter("mustCloseBrowser", "true");

      response.sendRedirect(redirectUrlBuilder.build().toString());
    } catch (URISyntaxException e) {
      LOGGER.debug("Invalid URI: ", e);
      response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    } catch (IOException e) {
      LOGGER.warn("Unable to redirect to logout page.", e);
      response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }
  }

  private void invalidateSession(HttpServletRequest request, HttpServletResponse response) {
    HttpSession session = request.getSession();
    if (session != null) {
      PrincipalHolder principalHolder =
          (PrincipalHolder) session.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY);
      if (principalHolder != null) {
        Subject subject = ThreadContext.getSubject();

        if (subject != null) {
          boolean hasSecurityAuditRole =
              Arrays.stream(System.getProperty("security.audit.roles", "").split(","))
                  .anyMatch(subject::hasRole);
          if (hasSecurityAuditRole) {
            securityLogger.audit("Subject with admin privileges has logged out", subject);
          }
        }
        principalHolder.remove();
      }
      removeTokens(session.getId());
      session.invalidate();
      deleteJSessionId(response);
    }
  }

  private void deleteJSessionId(HttpServletResponse response) {
    Cookie cookie = new Cookie("JSESSIONID", "");
    cookie.setSecure(true);
    cookie.setMaxAge(0);
    cookie.setPath("/");
    cookie.setComment("EXPIRING COOKIE at " + System.currentTimeMillis());
    response.addCookie(cookie);
  }

  /** Removes OAuth tokens stored for the given session */
  private void removeTokens(String sessionId) {
    tokenStorage.delete(sessionId);
  }
}
