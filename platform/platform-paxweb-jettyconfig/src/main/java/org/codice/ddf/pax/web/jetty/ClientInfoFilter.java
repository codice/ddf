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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.codice.ddf.platform.filter.SecurityFilterChain;
import org.codice.ddf.platform.filter.http.HttpFilter;
import org.codice.ddf.platform.filter.http.HttpFilterChain;
import org.codice.ddf.security.util.ThreadContextProperties;
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
 * <p>The only exception to this rule, {@link ThreadContextProperties#CLIENT_INFO_KEY}, which holds
 * a value string of {@code client-info}, is the key used to access the entire client information
 * map. It may contain different kinds of data that does not necessarily correlate to the servlet
 * API.
 */
public class ClientInfoFilter implements HttpFilter, SecurityFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientInfoFilter.class);

  @Override
  public void doFilter(
      HttpServletRequest request, HttpServletResponse response, HttpFilterChain filterChain)
      throws IOException, ServletException {
    LOGGER.debug("ClientInfoFilter HttpFilter doFilter.");
    ThreadContextProperties.addClientInfo(request);
    try {
      filterChain.doFilter(request, response);
    } finally {
      ThreadContextProperties.removeClientInfo();
    }
  }

  @Override
  public void init() {
    LOGGER.debug("Starting ClientInfoFilter.");
  }

  @Override
  public void doFilter(
      ServletRequest request, ServletResponse response, SecurityFilterChain filterChain)
      throws IOException, AuthenticationException {
    LOGGER.debug("ClientInfoFilter SecureFilter doFilter.");
    ThreadContextProperties.addClientInfo(request);
    try {
      filterChain.doFilter(request, response);
    } finally {
      ThreadContextProperties.removeClientInfo();
    }
  }

  @Override
  public void destroy() {
    LOGGER.debug("Destroying ClientInfoFilter.");
  }
}
