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
import java.util.Iterator;
import java.util.LinkedList;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of filter chain that allows the ability to add new {@link SecurityFilter}s to a
 * chain. The {@link ProxyFilterChain} may not be reused. That is, once the {@link
 * ProxyFilterChain#doFilter} method is called, no more {@link SecurityFilter}s may be added.
 */
public class ProxyFilterChain implements FilterChain {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyFilterChain.class);

  private final LinkedList<SecurityFilter> filters;

  private Iterator<SecurityFilter> iterator;

  /** Creates a new ProxyFilterChain */
  public ProxyFilterChain() {
    filters = new LinkedList<>();
  }

  /**
   * Adds a single {@link SecurityFilter} to the start of the local filter chain.
   *
   * @param filter The servlet filter to add.
   * @throws IllegalArgumentException when the {@param filer} is null
   * @throws IllegalStateException when a trying to add a {@link Filter} to this when the {@link
   *     ProxyFilterChain#doFilter} has been called at least once. This ensures that the {@link
   *     ProxyFilterChain} may not be reused.
   */
  public void addSecurityFilter(SecurityFilter filter) {
    if (filter == null) {
      throw new IllegalArgumentException("Cannot add null filter to chain.");
    }

    // a null iterator indicates that the ProxyFilterChain is not yet running
    if (iterator != null) {
      throw new IllegalStateException("Cannot add filter to current running chain.");
    }

    filters.add(0, filter);
    LOGGER.debug("Added filter {} to filter chain.", filter.getClass().getName());
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
      throws IOException, AuthenticationException {
    if (iterator == null) {
      iterator = filters.iterator();
    }

    if (iterator.hasNext()) {
      SecurityFilter filter = iterator.next();
      LOGGER.debug(
          "Calling filter {}.doFilter({}, {}, {})",
          filter.getClass().getName(),
          servletRequest,
          servletResponse,
          this);
      filter.doFilter(servletRequest, servletResponse, this);
    }
  }
}
