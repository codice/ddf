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
import javax.annotation.Nullable;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DelegateServletFilter} is meant to pick up any Filters that are exposed as services and
 * hide their init and destroy methods. This allows us to configure {@link Filter}s in blueprint
 * without that configuration being overwritten by future calls on the {@link Filter}s init method.
 * Pax Web should handle the lifecycle of any {@link Filter} services so we don't need to worry
 * about that here.
 */
public class DelegateServletFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DelegateServletFilter.class);

  private List<Filter> filterList;

  public DelegateServletFilter(List<Filter> filterList) {
    this.filterList = filterList;
  }

  @Override
  public void init(@Nullable FilterConfig filterConfig) {
    // do nothing
  }

  @Override
  public void destroy() {
    // do nothing
  }

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    if (!filterList.isEmpty()) {
      ProxyFilterChain chain = new ProxyFilterChain(filterChain);

      for (Filter filter : filterList) {
        chain.addFilter(filter);
      }

      chain.doFilter(servletRequest, servletResponse);
    } else {
      LOGGER.debug("Did not find any Filters. Acting as a pass-through filter...");
      filterChain.doFilter(servletRequest, servletResponse);
    }
  }
}
