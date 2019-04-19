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
import ddf.security.common.SecurityTokenHolder;
import ddf.security.common.audit.SecurityLogger;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.util.Strings;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalLogoutServlet extends HttpServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(LocalLogoutServlet.class);

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException {
    response.setHeader("Cache-Control", "no-cache, no-store");
    response.setHeader("Pragma", "no-cache");
    response.setContentType("text/html");

    URIBuilder redirectUrlBuilder = null;
    List<NameValuePair> params = new ArrayList<>();

    try {
      redirectUrlBuilder = new URIBuilder("/logout/logout-response.html");

      if (Strings.isNotBlank(SystemBaseUrl.EXTERNAL.getRootContext())) {
        redirectUrlBuilder =
            new URIBuilder(
                SystemBaseUrl.EXTERNAL.getRootContext() + "/logout/logout-response.html");
      }

      invalidateSession(request, response);

      checkForPki(request, params);

      checkForBasic(request, redirectUrlBuilder, params);

      response.sendRedirect(redirectUrlBuilder.build().toString());
    } catch (URISyntaxException e) {
      LOGGER.debug("Invalid URI: ", e);
    } catch (IOException e) {
      LOGGER.warn("Send Redirect failed due to: ", e);
    }
  }

  private void checkForBasic(
      HttpServletRequest request, URIBuilder redirectUrlBuilder, List<NameValuePair> params) {
    Enumeration authHeaders = request.getHeaders(javax.ws.rs.core.HttpHeaders.AUTHORIZATION);
    while (authHeaders.hasMoreElements()) {
      if (((String) authHeaders.nextElement()).contains("Basic")) {
        params.add(
            new BasicNameValuePair("msg", "Please close your browser to finish logging out"));
        break;
      }
    }
    redirectUrlBuilder.addParameters(params);
  }

  private void checkForPki(HttpServletRequest request, List<NameValuePair> params) {
    if (request.getAttribute("javax.servlet.request.X509Certificate") != null
        && ((X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate"))
                .length
            > 0) {
      params.add(new BasicNameValuePair("msg", "Please close your browser to finish logging out"));
    }
  }

  private void invalidateSession(HttpServletRequest request, HttpServletResponse response) {
    HttpSession session = request.getSession();
    if (session != null) {
      SecurityTokenHolder savedToken =
          (SecurityTokenHolder) session.getAttribute(SecurityConstants.SAML_ASSERTION);
      if (savedToken != null) {
        Subject subject = ThreadContext.getSubject();

        if (subject != null) {
          boolean hasSecurityAuditRole =
              Arrays.stream(System.getProperty("security.audit.roles", "").split(","))
                  .anyMatch(subject::hasRole);
          if (hasSecurityAuditRole) {
            SecurityLogger.audit("Subject with admin privileges has logged out", subject);
          }
        }
        savedToken.remove();
      }
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
}
