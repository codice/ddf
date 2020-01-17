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
package org.codice.ddf.platform.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

  private HttpUtils() {}

  /**
   * Strip a query string from a url. If the url is not valid, it will be returned as is.
   *
   * <p>NOTE: a url with multiple query strings is still considered valid.
   *
   * @param url valid url
   * @return url query parameters removed
   */
  public static String stripQueryString(String url) {
    try {
      return validateAndStripQueryString(url);
    } catch (MalformedURLException ex) {
      LOGGER.debug("Tried to strip query string from invalid url, {}", url);
      return url;
    }
  }

  /**
   * Strip a query string from a url. If the url is not valid, an exception will be thrown.
   *
   * @param url the url to strip
   * @return url with query parameters removed
   * @throws MalformedURLException if the input url is invalid
   */
  public static String validateAndStripQueryString(String url) throws MalformedURLException {
    URL u = new URL(url);
    if (u.getPort() == -1) {
      return String.format("%s://%s%s", u.getProtocol(), u.getHost(), u.getPath());
    } else {
      return String.format("%s://%s:%s%s", u.getProtocol(), u.getHost(), u.getPort(), u.getPath());
    }
  }

  /**
   * Sends the given response code back to the caller.
   *
   * @param code HTTP response code for this request
   * @param response the servlet response object
   */
  public static void returnSimpleResponse(int code, HttpServletResponse response) {
    try {
      LOGGER.debug("Sending response code {}", code);
      response.setStatus(code);
      response.setContentLength(0);
      response.flushBuffer();
    } catch (IOException ioe) {
      LOGGER.debug("Failed to send auth response", ioe);
    }
  }

  /**
   * Returns a mapping of cookies from the incoming request. Key is the cookie name, while the value
   * is the Cookie object itself.
   *
   * @param req Servlet request for this call
   * @return map of Cookie objects present in the current request - always returns a map
   */
  public static Map<String, Cookie> getCookieMap(HttpServletRequest req) {
    HashMap<String, Cookie> map = new HashMap<String, Cookie>();

    Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie != null) {
          map.put(cookie.getName(), cookie);
        }
      }
    }

    return map;
  }
}
