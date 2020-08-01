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
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of filter chain that allows the ability to dynamically add new {@link Filter}s to
 * a chain. The {@link ProxyFilterChain} may not be reused. That is, once the {@link
 * ProxyFilterChain#doFilter} method is called, no more {@link Filter}s may be added.
 */
public class ProxyFilterChain implements FilterChain {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyFilterChain.class);

  private final Iterator<Filter> iterator;

  public ProxyFilterChain(List<Filter> filters) {
    iterator = filters.iterator();
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
      throws IOException, ServletException {
    if (iterator.hasNext()) {
      Filter filter = iterator.next();
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
