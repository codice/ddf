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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hands the request off to a set chain of servlet {@link Filter}s. Since SecurityFilters are run on
 * each request, this provides a mechanism to add global servlet filters. As of OSGi R6, there is a
 * proper way to define global servlets/filters/listeners/etc., defined by the HTTP Whiteboard spec.
 * However, pax-web does not yet implement that feature, so we're left using this workaround.
 *
 * <p>When https://ops4j1.jira.com/browse/PAXWEB-1123 is resolved, this workaround should be
 * revisited.
 */
public class DelegatingSecurityFilter implements SecurityFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DelegatingSecurityFilter.class);

  private final List<Filter> filters;

  public DelegatingSecurityFilter(List<Filter> filters) {
    this.filters = filters;
  }

  @Override
  public void init() {
    LOGGER.debug("Initialized " + DelegatingSecurityFilter.class.getSimpleName());
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, AuthenticationException {
    LOGGER.debug("Delegating to {} global ServletFilters.", filters.size());
    try {
      new ProxyFilterChain(filters).doFilter(request, response);
    } catch (ServletException e) {
      throw new AuthenticationException("Error in global ServletFilter chain", e);
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    LOGGER.debug("Destroyed " + DelegatingSecurityFilter.class.getSimpleName());
  }
}
