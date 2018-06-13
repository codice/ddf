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
package org.codice.ddf.security.filter.csrf;

import ddf.security.common.audit.SecurityLogger;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Contains multiple checks to prevent cross-site requests for protected context paths. */
public class CsrfFilter implements SecurityFilter {

  public static final String CSRF_HEADER = "X-Requested-With";
  public static final String ORIGIN_HEADER = "Origin";
  public static final String REFERER_HEADER = "Referer";

  public static final String JOLOKIA_CONTEXT = "/admin/jolokia";
  public static final String INTRIGUE_CONTEXT = "/search/catalog/internal";
  public static final String WEBSOCKET_CONTEXT = "/search/catalog/ws";

  private static final Logger LOGGER = LoggerFactory.getLogger(CsrfFilter.class);

  // List of context paths that require cross-site protections
  private List<String> protectedContexts;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    LOGGER.debug("Starting CSRF filter.");

    protectedContexts = new ArrayList<>();
    protectedContexts.add(JOLOKIA_CONTEXT);
    protectedContexts.add(INTRIGUE_CONTEXT);
    protectedContexts.add(WEBSOCKET_CONTEXT);
  }

  /**
   * Checks that the origin or referer header of the request matches the target origin when
   * attempting to access certain contexts. Also checks for the existence of anti-CSRF header
   * "X-Requested-With". A 403 is returned and the request is stopped if one or more of these
   * conditions are met: - No Origin or Referer header is present on the request. - Neither the
   * Origin or Referer header match the target origin. - An X-Requested-With header is not present
   * on the request.
   *
   * @param request incoming http request
   * @param response response stream for returning the response
   * @param chain chain of filters to be invoked following this filter
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    LOGGER.debug("Performing doFilter() on CsrfFilter");
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    String targetContextPath = httpRequest.getRequestURI();

    // Begin CSRF checks if request is accessing a Cross-Site protected context
    if (protectedContexts.stream().anyMatch(targetContextPath::startsWith)) {

      String targetOrigin = httpRequest.getRequestURL().toString();
      String sourceOrigin = httpRequest.getHeader(ORIGIN_HEADER);
      String sourceReferer = httpRequest.getHeader(REFERER_HEADER);
      String csrfHeader = httpRequest.getHeader(CSRF_HEADER);

      // Reject if no origin or referer header is present on the request
      if (sourceOrigin == null && sourceReferer == null) {
        respondForbidden(
            httpResponse, "Incoming request did not have an Origin or Referer header.");
        return;
      }

      // Reject if neither the Origin or Referer header match the target origin
      if (!isSameOrigin(sourceOrigin, targetOrigin) && !isSameOrigin(sourceReferer, targetOrigin)) {
        respondForbidden(
            httpResponse, "Origin or Referer header of request did not match target origin.");
        return;
      }

      // Check for presence of anti-CSRF header
      // WebSockets API does not allow for custom headers, origin check is sufficient
      if (!targetContextPath.startsWith(WEBSOCKET_CONTEXT) && csrfHeader == null) {
        respondForbidden(httpResponse, "Request did not have required X-Requested-With header.");
        return;
      }
    }

    // All checks passed
    chain.doFilter(request, response);
  }

  /**
   * Returns true if both URLs have the same host and port. If either are null or empty, false is
   * returned
   *
   * @param source source URL
   * @param target destination URL
   * @return true if matching, false if different or a parsing error occurs
   */
  private Boolean isSameOrigin(String source, String target) {
    if (StringUtils.isBlank(source) || StringUtils.isBlank(target)) {
      return false;
    } else {
      try {
        String sourceOrigin = new URL(source).getAuthority();
        String targetOrigin = new URL(target).getAuthority();
        return (sourceOrigin.equals(targetOrigin));
      } catch (MalformedURLException e) {
        LOGGER.debug("Could not extract origin from URLs", e);
        return false;
      }
    }
  }

  /**
   * Security audits, logs, and then responds with a 403.
   *
   * @param httpResponse
   * @param msg
   */
  private void respondForbidden(HttpServletResponse httpResponse, String msg) {
    SecurityLogger.audit(msg);
    LOGGER.debug(msg);
    try {
      httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
      httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
      httpResponse.flushBuffer();
    } catch (IOException ioe) {
      LOGGER.debug("Failed to send auth response: {}", ioe);
    }
  }

  @Override
  public void destroy() {
    LOGGER.debug("Destroying CSRF filter.");
  }
}
