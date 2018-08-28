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
package org.codice.ddf.security.handler.cas.filter;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapped HttpServletRequest that is aware of the system root context, and returns the correct
 * paths for both internal and external requests.
 */
public class FilteredRequest extends HttpServletRequestWrapper {
  private static final Logger LOGGER = LoggerFactory.getLogger(FilteredRequest.class);

  private final String contextPath;
  private final String requestUri;
  private final String requestUrl;

  public FilteredRequest(HttpServletRequest request, String serverName) {
    super(request);

    // Wrapping FilteredRequests will result in repeated root contexts since the original request
    // is already wrapped. So just copy FilteredRequests instead of wrapping.
    if (request instanceof FilteredRequest) {
      FilteredRequest filteredRequest = (FilteredRequest) request;
      contextPath = filteredRequest.contextPath;
      requestUri = filteredRequest.requestUri;
      requestUrl = filteredRequest.requestUrl;
    } else {
      URL baseUrl = toBaseUrl(serverName);
      contextPath =
          isExternalUrl(baseUrl)
              ? SystemBaseUrl.EXTERNAL.getRootContext().replaceFirst("/$", "")
              : "";
      requestUri = contextPath + request.getRequestURI();
      requestUrl =
          (baseUrl == null)
              ? serverName.replaceFirst("/$", "") + requestUri
              : baseUrl.toString() + requestUri;
    }
  }

  @Override
  public String getContextPath() {
    return contextPath;
  }

  @Override
  public String getRequestURI() {
    return requestUri;
  }

  @Override
  public StringBuffer getRequestURL() {
    return new StringBuffer(requestUrl);
  }

  /**
   * Convert the input string into a URL containing only a protocol, hostname, and port. Returns
   * null if the input is not a valid URL.
   */
  private URL toBaseUrl(String serverName) {
    try {
      URL url = new URL(serverName);
      return new URI(url.getProtocol(), url.getAuthority(), null, null, null).toURL();
    } catch (MalformedURLException | URISyntaxException e) {
      LOGGER.warn("Invalid CAS client configuration. Server name must be a valid URL");
      return null;
    }
  }

  /**
   * Determines whether the input url matches the external system base url. If the input does not
   * specify a port, it will be inferred from the protocol.
   *
   * @return true if the protocol, host, and port of the given url match the external system url;
   *     false otherwise
   */
  private boolean isExternalUrl(URL url) {
    if (url == null) {
      return false;
    }

    int inferredPort = (url.getPort() == -1) ? url.getDefaultPort() : url.getPort();
    String externalProtocol = SystemBaseUrl.EXTERNAL.getProtocol().replaceFirst("://$", "");
    return SystemBaseUrl.EXTERNAL.getHost().equals(url.getHost())
        && SystemBaseUrl.EXTERNAL.getPort().equals(Integer.toString(inferredPort))
        && externalProtocol.equals(url.getProtocol());
  }
}
