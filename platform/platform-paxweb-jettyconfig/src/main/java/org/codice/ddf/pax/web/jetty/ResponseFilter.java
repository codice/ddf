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
package org.codice.ddf.pax.web.jetty;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.http.HttpFilter;
import org.codice.ddf.platform.filter.http.HttpFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Servlet filter that adds security information to the http response header. */
public class ResponseFilter implements HttpFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResponseFilter.class);

  public static final String X_XSS_PROTECTION = "X-XSS-Protection";

  public static final String X_FRAME_OPTIONS = "X-Frame-Options";

  public static final String X_CONTENT_SECURITY_POLICY = "X-Content-Security-Policy";

  public static final String CACHE_CONTROL = "Cache-Control";

  public static final String DEFAULT_XSS_PROTECTION_VALUE = "1; mode=block";

  public static final String DEFAULT_X_FRAME_OPTIONS_VALUE = "SAMEORIGIN";

  public static final String DEFAULT_CONTENT_SECURITY_POLICY =
      "default-src 'none'; connect-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'";

  // Arbitrarily chose one week as a reasonable default. 604800 is one week in seconds.
  public static final String DEFAULT_CACHE_CONTROL_VALUE = "private, max-age=604800, immutable";

  public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
  public static final String STRICT_TRANSPORT_SECURITY_VALUE =
      "max-age=31536000 ; includeSubDomains";

  private List<String> headers =
      Arrays.asList(
          X_XSS_PROTECTION + "=" + DEFAULT_XSS_PROTECTION_VALUE,
          X_FRAME_OPTIONS + "=" + DEFAULT_X_FRAME_OPTIONS_VALUE,
          X_CONTENT_SECURITY_POLICY + "=" + DEFAULT_CONTENT_SECURITY_POLICY,
          STRICT_TRANSPORT_SECURITY + "=" + STRICT_TRANSPORT_SECURITY_VALUE,
          CACHE_CONTROL + "=" + DEFAULT_CACHE_CONTROL_VALUE);

  // Index paths (such as /search/catalog/ or /admin/) should never be cached
  private void disableCachingForHtml(HttpServletRequest request, HttpServletResponse response) {
    String requestURI = request.getRequestURI();
    if (requestURI.endsWith("/") || requestURI.endsWith(".html")) {
      response.setHeader(CACHE_CONTROL, "no-cache, no-store, must-revalidate"); // HTTP 1.1.
      response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
      response.setHeader("Expires", "0"); // Proxies.
    }
  }

  private void addCommonHeaders(HttpServletResponse response) {
    for (String header : headers) {
      String[] keyVal = header.split("=", 2);
      if (keyVal.length != 2) {
        LOGGER.debug("Skipping bad header {}", header);
        continue;
      }
      response.setHeader(keyVal[0], keyVal[1]);
    }
  }

  @Override
  public void doFilter(
      HttpServletRequest request, HttpServletResponse response, HttpFilterChain filterChain)
      throws IOException, ServletException {
    addCommonHeaders(response);
    disableCachingForHtml(request, response);
    filterChain.doFilter(request, response);
  }

  public void setHeaders(List<String> headers) {
    this.headers = headers;
  }
}
