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
    // is already wrapped. Instead, just copy the wrapped request and systemBaseUrl references
    if (request instanceof FilteredRequest) {
      FilteredRequest filteredRequest = (FilteredRequest) request;
      contextPath = filteredRequest.contextPath;
      requestUri = filteredRequest.requestUri;
      requestUrl = filteredRequest.requestUrl;
    } else {
      URL baseUrl = toBaseUrl(serverName);
      contextPath =
          isExternalRequest(baseUrl)
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

  private URL toBaseUrl(String serverName) {
    try {
      URL url = new URL(serverName);
      return new URI(url.getProtocol(), url.getAuthority(), null, null, null).toURL();
    } catch (MalformedURLException | URISyntaxException e) {
      LOGGER.warn("Error parsing CAS client server name config. Is it a valid URL?");
      return null;
    }
  }

  private boolean isExternalRequest(URL url) {
    if (url == null) {
      return false;
    }

    String protocol = SystemBaseUrl.EXTERNAL.getProtocol().replaceFirst("://$", "");
    return SystemBaseUrl.EXTERNAL.getHost().equals(url.getHost())
        && SystemBaseUrl.EXTERNAL.getPort().equals(Integer.toString(url.getPort()))
        && protocol.equals(url.getProtocol());
  }
}
