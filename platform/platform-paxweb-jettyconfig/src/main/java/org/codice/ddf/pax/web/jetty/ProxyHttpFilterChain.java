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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.http.HttpFilter;
import org.codice.ddf.platform.filter.http.HttpFilterChain;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of filter chain that allows the ability to dynamically add new {@link Filter}s to
 * a chain. The {@link ProxyHttpFilterChain} may not be reused. That is, once the {@link
 * ProxyHttpFilterChain#doFilter} method is called, no more {@link HttpFilter}s may be added.
 */
public class ProxyHttpFilterChain implements HttpFilterChain {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyHttpFilterChain.class);

  private final Iterator<HttpFilter> iterator;
  private final Handler handler;
  private final String target;
  private final Request baseRequest;

  public ProxyHttpFilterChain(
      List<HttpFilter> filters, Handler handler, String target, Request baseRequest) {
    this.iterator = filters.iterator();
    this.handler = handler;
    this.target = target;
    this.baseRequest = baseRequest;
  }

  @Override
  public void doFilter(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    if (iterator.hasNext()) {
      HttpFilter filter = iterator.next();
      LOGGER.debug(
          "Calling filter {}.doFilter({}, {}, {}, {}, {})",
          filter.getClass().getName(),
          target,
          baseRequest,
          request,
          response,
          this);
      filter.doFilter(request, response, this);
    } else {
      handler.handle(target, baseRequest, request, response);
    }
  }
}
