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
 * This servlet filter inserts a trace-id into the shiro {@link ThreadContext} so it can be
 * forwarded to useful areas of interest.
 *
 * <p>This filter supports but doesn't full implement the W3C Trace Context specification. Refer to
 * <a href="https://www.w3.org/TR/trace-context/">W3C Trace Context Specification</a> and
 * specifically the <a href="https://www.w3.org/TR/trace-context/#trace-id">trace-id</a> property.
 */
public class TraceContextFilter implements HttpFilter, SecurityFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(TraceContextFilter.class);

  @Override
  public void doFilter(
      HttpServletRequest request, HttpServletResponse response, HttpFilterChain filterChain)
      throws IOException, ServletException {

    // TODO: use trace-id from traceparent HTTP header if it exists, otherwise generate our own
    String traceId = ThreadContextProperties.addTraceId();
    LOGGER.trace("Adding traceId {} to context", traceId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      LOGGER.trace("Removing trace ID {} from context", traceId);
      ThreadContextProperties.removeTraceId();
    }
  }

  @Override
  public void init() {
    LOGGER.debug("Starting TraceContextFilter.");
  }

  @Override
  public void doFilter(
      ServletRequest request, ServletResponse response, SecurityFilterChain filterChain)
      throws IOException, AuthenticationException {
    String traceId = ThreadContextProperties.addTraceId();
    LOGGER.trace("Adding traceId {} to context", traceId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      LOGGER.trace("Removing trace ID {} from context", traceId);
      ThreadContextProperties.removeTraceId();
    }
  }

  @Override
  public void destroy() {
    LOGGER.debug("Destroying TraceContextFilter.");
  }
}
