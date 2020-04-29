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
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelegateServletFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DelegateServletFilter.class);

  private final List<Filter> filters;

  public DelegateServletFilter(List<Filter> filters) {
    this.filters = filters;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    LOGGER.debug("Initialized DelegateServletFilter");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    LOGGER.debug("Delegating to {} filters.", filters.size());
    FilterChain proxyChain = new ProxyFilterChain(filters, chain);
    proxyChain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    LOGGER.debug("Destroyed DelegateServletFilter");
  }
}
