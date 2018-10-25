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
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.shiro.util.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet filter extracts client-specific information and places it in the shiro {@link
 * ThreadContext} so it can be forwarded to useful areas of interest. Also contains the constants
 * for working with the client info map.
 *
 * <p>The information currently all comes from the servlet API; specifically a select few getters
 * within {@link ServletRequest}. The format of the keys follows the format of java beans. The keys
 * are camel-cased names without the preceeding 'get' found in the method name.
 *
 * <p>For example, the key associated with {@link ServletRequest#getRemoteAddr()} would be the
 * string {@code remoteAddr}.
 *
 * <p>The only exception to this rule, {@link ClientInfoFilter#CLIENT_INFO_KEY}, which holds a value
 * string of {@code client-info}, is the key used to access the entire client information map. It
 * may contain different kinds of data that does not necessarily correlate to the servlet API.
 */
public class ClientInfoFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientInfoFilter.class);

  public static final String CLIENT_INFO_KEY = "client-info";

  public static final String SERVLET_REMOTE_ADDR = "remoteAddr";

  public static final String SERVLET_REMOTE_HOST = "remoteHost";

  public static final String SERVLET_SCHEME = "scheme";

  public static final String SERVLET_CONTEXT_PATH = "contextPath";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void destroy() {}

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    ThreadContext.put(CLIENT_INFO_KEY, createClientInfoMap(servletRequest));
    try {
      filterChain.doFilter(servletRequest, servletResponse);
    } finally {
      ThreadContext.remove(CLIENT_INFO_KEY);
    }
  }

  private Map<String, String> createClientInfoMap(ServletRequest request) {
    Map<String, String> clientInfoMap = new HashMap<>();
    clientInfoMap.put(SERVLET_REMOTE_ADDR, request.getRemoteAddr());
    clientInfoMap.put(SERVLET_REMOTE_HOST, request.getRemoteHost());
    clientInfoMap.put(SERVLET_SCHEME, request.getScheme());
    ServletContext servletContext = request.getServletContext();
    if (servletContext != null) {
      clientInfoMap.put(SERVLET_CONTEXT_PATH, servletContext.getContextPath());
    }
    LOGGER.debug("Creating client info map with the following pairs, {}", clientInfoMap);
    return clientInfoMap;
  }
}
