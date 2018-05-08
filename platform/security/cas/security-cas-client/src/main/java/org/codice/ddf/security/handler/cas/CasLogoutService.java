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
package org.codice.ddf.security.handler.cas;

import ddf.security.SecurityConstants;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.common.audit.SecurityLogger;
import java.io.IOException;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class CasLogoutService {
  private static final Logger LOGGER = LoggerFactory.getLogger(CasLogoutService.class);

  private String casServerLogoutUrl;

  @GET
  @Path("/logout")
  public void sendLogoutRequest(
      @Context HttpServletRequest request, @Context HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-cache, no-store");
    response.setHeader("Pragma", "no-cache");
    response.setContentType("text/html");

    UriBuilder redirectPage = UriBuilder.fromUri(casServerLogoutUrl);
    HttpSession session = request.getSession(false);
    if (session != null) {
      SecurityTokenHolder savedToken =
          (SecurityTokenHolder) session.getAttribute(SecurityConstants.SAML_ASSERTION);
      if (savedToken != null) {
        Subject subject = ThreadContext.getSubject();
        if (subject != null) {
          boolean hasSecurityAuditRole =
              Arrays.stream(System.getProperty("security.audit.roles", "").split(","))
                  .filter(subject::hasRole)
                  .findFirst()
                  .isPresent();
          if (hasSecurityAuditRole) {
            SecurityLogger.audit("Subject with admin privileges has logged out", subject);
          }
        }
        savedToken.removeAll();
      }
      session.invalidate();
    }

    try {
      response.sendRedirect(redirectPage.build().toString());
    } catch (IOException e) {
      LOGGER.warn("Failed to send redirect: ", e);
    }
  }

  public void setCasServerLogoutUrl(String url) {
    this.casServerLogoutUrl = url;
  }

  public String getCasServerLogoutUrl() {
    return this.casServerLogoutUrl;
  }
}
