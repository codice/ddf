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

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.Objects;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.HttpFilter;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
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
public class DelegatingHttpFilterHandler extends HandlerWrapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(DelegatingHttpFilterHandler.class);

  private final ServiceTracker<HttpFilter, HttpFilter> filterTracker;

  @VisibleForTesting
  DelegatingHttpFilterHandler(ServiceTracker<HttpFilter, HttpFilter> filterTracker) {
    this.filterTracker = Objects.requireNonNull(filterTracker);
  }

  public DelegatingHttpFilterHandler() {
    Bundle bundle = FrameworkUtil.getBundle(DelegatingHttpFilterHandler.class);
    Objects.requireNonNull(bundle, "Bundle must not be null");
    Objects.requireNonNull(bundle.getBundleContext(), "Bundle has no valid BundleContext");

    this.filterTracker =
        new ServiceTracker(bundle.getBundleContext(), HttpFilter.class.getName(), null);
  }

  @Override
  public void handle(
      String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    HttpFilter[] filters = getFilters();
    LOGGER.debug("Delegating to {} HttpFilters.", filters.length);

    ProxyHttpFilterChain filterChain =
        new ProxyHttpFilterChain(filters, getHandler(), target, baseRequest);
    filterChain.doFilter(request, response);
  }

  private HttpFilter[] getFilters() {
    HttpFilter[] filters = new HttpFilter[filterTracker.size()];
    return filterTracker.getServices(filters);
  }

  protected void doStart() throws Exception {
    super.doStart();
    this.filterTracker.open();
  }

  protected void doStop() throws Exception {
    this.filterTracker.close();
    super.doStop();
  }
}
