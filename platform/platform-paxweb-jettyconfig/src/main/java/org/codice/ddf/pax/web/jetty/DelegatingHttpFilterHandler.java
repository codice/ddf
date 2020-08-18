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
import java.util.Collection;
import java.util.Objects;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.http.HttpFilter;
import org.codice.ddf.platform.util.SortedServiceList;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code DelegatingHttpFilterHandler} provides a way to create global filters which will apply
 * to all requests. It finds any registered {@link HttpFilter} services and passes incoming
 * requests to them in order of service ranking.
 *
 * As of OSGi R6, there is a proper way to define global servlets/filters/listeners/etc., defined
 * by the HTTP Whiteboard spec. However, pax-web does not yet implement that feature, so we're left
 * using this workaround.
 *
 * <p>When https://ops4j1.jira.com/browse/PAXWEB-1123 is resolved, this workaround should be
 * revisited.
 */
public class DelegatingHttpFilterHandler extends HandlerWrapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(DelegatingHttpFilterHandler.class);

  private static final String FILTER = "(objectclass=" + HttpFilter.class.getName() + ")";

  private final HttpFilterServiceListener listener = new HttpFilterServiceListener();

  private final SortedServiceList<HttpFilter> httpFilters;

  private final BundleContext context;

  private static BundleContext getContext() {
    Bundle bundle = FrameworkUtil.getBundle(DelegatingHttpFilterHandler.class);
    Objects.requireNonNull(bundle, "Bundle cannot be null");
    return bundle.getBundleContext();
  }

  public DelegatingHttpFilterHandler() throws InvalidSyntaxException {
    this(getContext());
  }

  public DelegatingHttpFilterHandler(BundleContext context) throws InvalidSyntaxException {
    Objects.requireNonNull(context, "Bundle context cannot be null");
    this.context = context;
    this.context.addServiceListener(listener, FILTER);
    this.httpFilters =
        new SortedServiceList<HttpFilter>() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };

    /*
     * The service listener won't pick up services that are already registered. Must manually
     * add them to the service list.
     */
    Collection<ServiceReference<HttpFilter>> serviceReferences =
        this.context.getServiceReferences(HttpFilter.class, FILTER);
    for (ServiceReference<HttpFilter> reference : serviceReferences) {
      this.listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference));
    }
  }

  @Override
  public void handle(
      String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    LOGGER.trace("Delegating to {} HttpFilters.", httpFilters.size());

    ProxyHttpFilterChain filterChain =
        new ProxyHttpFilterChain(httpFilters, getHandler(), target, baseRequest);
    filterChain.doFilter(request, response);
  }

  protected void doStart() throws Exception {
    super.doStart();
    LOGGER.debug("Started {}", DelegatingHttpFilterHandler.class.getSimpleName());
  }

  protected void doStop() throws Exception {
    super.doStop();
    LOGGER.debug("Stopped {}", DelegatingHttpFilterHandler.class.getSimpleName());
  }

  private class HttpFilterServiceListener implements ServiceListener {

    @Override
    public void serviceChanged(ServiceEvent event) {
      ServiceReference<?> reference = event.getServiceReference();
      switch (event.getType()) {
        case ServiceEvent.REGISTERED:
          httpFilters.bindPlugin(reference);
          break;
        case ServiceEvent.UNREGISTERING:
          httpFilters.unbindPlugin(reference);
          break;
        default:
          /* only care when services are added or removed */
          break;
      }
    }
  }
}
