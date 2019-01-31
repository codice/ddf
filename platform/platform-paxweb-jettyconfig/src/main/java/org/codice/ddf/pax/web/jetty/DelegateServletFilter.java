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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.annotation.Nullable;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DelegateServletFilter} is meant to detect any {@link Filter}s and doFilter through those
 * before continuing on its {@link FilterChain}. If no {@link Filter}s are found, it becomes a
 * pass-through filter.
 */
public class DelegateServletFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DelegateServletFilter.class);

  private final Set<String> keysOfInitializedFilters;

  @Nullable
  private FilterConfig filterConfig;

  public DelegateServletFilter() {
    keysOfInitializedFilters = new CopyOnWriteArraySet<>();
  }

  BundleContext getContext() {
    final Bundle bundle = FrameworkUtil.getBundle(DelegateServletFilter.class);
    if (bundle != null) {
      return bundle.getBundleContext();
    }
    return null;
  }

  @Override
  public void init(@Nullable FilterConfig filterConfig) {
    this.filterConfig = filterConfig;
    // need to (re)initialize every Filter after the DelegateServletFilter is initialized
    keysOfInitializedFilters.clear();
  }

  @Override
  public void destroy() {
    // do nothing
  }

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    TreeSet<ServiceReference<Filter>> sortedFilterServiceReferences = null;
    final BundleContext bundleContext = getContext();

    if (bundleContext == null) {
      throw new ServletException(
          "Unable to get BundleContext. No servlet Filters can be applied. Blocking the request processing.");
    }

    try {
      sortedFilterServiceReferences =
          new TreeSet<>(bundleContext.getServiceReferences(Filter.class, null));
    } catch (InvalidSyntaxException ise) {
      LOGGER.debug("Should never get this exception as there is no filter being passed.", ise);
    }

    if (!CollectionUtils.isEmpty(sortedFilterServiceReferences)) {
      LOGGER.debug("Found {} filter(s), now filtering...", sortedFilterServiceReferences.size());
      final ProxyFilterChain chain = new ProxyFilterChain(filterChain);

      // Insert the Filters into the chain one at a time (from lowest service ranking
      // to highest service ranking). The Filter with the highest service-ranking will
      // end up at index 0 in the FilterChain, which means that the Filters will be
      // run in order of highest to lowest service ranking.
      for (ServiceReference<Filter> filterServiceReference : sortedFilterServiceReferences) {
        final Filter filter = bundleContext.getService(filterServiceReference);

        if (!hasBeenInitialized(filterServiceReference, bundleContext)) {
          initializeFilter(bundleContext, filterServiceReference, filter);
        }

        chain.addFilter(filter);
      }

      chain.doFilter(servletRequest, servletResponse);
    } else {
      LOGGER.debug("Did not find any Filters. Acting as a pass-through filter...");
      filterChain.doFilter(servletRequest, servletResponse);
    }
  }

  private boolean hasBeenInitialized(
      final ServiceReference<Filter> filterServiceReference, final BundleContext bundleContext) {
    return keysOfInitializedFilters.contains(getFilterKey(filterServiceReference, bundleContext));
  }

  private void initializeFilter(
      BundleContext bundleContext, ServiceReference<Filter> filterServiceReference, Filter filter)
      throws ServletException {
    final ServletContext servletContext;
    final String filterName = getFilterName(filterServiceReference, bundleContext);

    if (filterConfig == null) {
      LOGGER.warn(
          "DelegateServletFilter was initialized with a null filterConfig. Initializing Filter {} with null ServletContext",
          filterName);
      servletContext = null;
    } else {
      servletContext = filterConfig.getServletContext();
    }

    filter.init(new InitFilterConfig(filterServiceReference, servletContext, bundleContext));
    keysOfInitializedFilters.add(getFilterKey(filterServiceReference, bundleContext));
    LOGGER.debug("Initialized Filter {}", filterName);
  }

  public void removeFilter(@Nullable final ServiceReference<Filter> filterServiceReference) {
    if (filterServiceReference != null) {
      final BundleContext bundleContext = getContext();

      if (bundleContext != null) {
        // unmark the filter as initialized so that it can be re-initialized if the
        // filter is registered again
        keysOfInitializedFilters.remove(getFilterKey(filterServiceReference, bundleContext));
        bundleContext.getService(filterServiceReference).destroy();
      } else {
        LOGGER.warn(
            "Unable to remove Filter. Try restarting the system or turning up logging to monitor current Filters.");
      }
    }
  }

  private static String getFilterKey(
      final ServiceReference<Filter> filterServiceReference, final BundleContext bundleContext) {
    return getFilterName(filterServiceReference, bundleContext);
  }

  /**
   * This logic to get the filter name from a {@link ServiceReference<Filter>} is copied from {@link
   * org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletTracker#createWebElement(ServiceReference,
   * javax.servlet.Servlet)}. See the pax-web Whiteboard documentation and {@link
   * org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_FILTER_NAME} for how
   * to configure {@link Filter} services with a filter name.
   */
  private static String getFilterName(
      ServiceReference<Filter> filterServiceReference, BundleContext bundleContext) {
    final String HTTP_WHITEBOARD_FILTER_NAME = "osgi.http.whiteboard.filter.name";
    final String filterNameFromTheServiceProperty =
        getStringProperty(filterServiceReference, HTTP_WHITEBOARD_FILTER_NAME);
    // If this service property is not specified, the fully qualified name of the service object's
    // class is used as the servlet filter name.
    if (StringUtils.isBlank(filterNameFromTheServiceProperty)) {
      return bundleContext.getService(filterServiceReference).getClass().getCanonicalName();
    } else {
      return filterNameFromTheServiceProperty;
    }
  }

  @Nullable
  private static String getStringProperty(ServiceReference<?> serviceReference, String key) {
    Object value = serviceReference.getProperty(key);
    if (value != null && !(value instanceof String)) {
      LOGGER.warn("Service property [key={}] value must be a String", key);
      return null;
    } else {
      return (String) value;
    }
  }

  /**
   * This inner class is used to instantiate a {@link FilterConfig} from a {@link Filter}'s service
   * properties containing init params and the same {@link ServletContext} as the {@link
   * DelegateServletFilter}. The {@link FilterConfig} is used to initialize the {@link Filter}.
   *
   * <p>The logic of this inner class is copied from the {@link
   * org.ops4j.pax.web.extender.whiteboard} feature.
   */
  private static class InitFilterConfig implements FilterConfig {

    private final String DEFAULT_INIT_PREFIX_PROP = "init.";

    private final String HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX = "servlet.init.";

    private final String PROPERTY_INIT_PREFIX = "init-prefix";


    private final ServiceReference<Filter> filterServiceReference;

    private final ServletContext servletContext;

    private final Map<String, String> initParams;

    private final BundleContext bundleContext;

    InitFilterConfig(
        ServiceReference<Filter> filterServiceReference,
        ServletContext servletContext,
        BundleContext bundleContext) {
      this.filterServiceReference = filterServiceReference;
      this.servletContext = servletContext;
      this.bundleContext = bundleContext;
      initParams = createInitParams();
    }

    @Override
    public String getFilterName() {
      return DelegateServletFilter.getFilterName(filterServiceReference, bundleContext);
    }

    @Override
    public ServletContext getServletContext() {
      return servletContext;
    }

    @Override
    public String getInitParameter(String name) {
      return initParams.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
      return Collections.enumeration(initParams.keySet());
    }

    /**
     * This code to create the init params from a {@link ServiceReference<Filter>} is copied from
     * {@link
     * org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletTracker#createWebElement(ServiceReference,
     * javax.servlet.Servlet)}. See the pax-web Whiteboard documentation and {@link
     * org.ops4j.pax.web.extender.whiteboard.ExtenderConstants#PROPERTY_INIT_PREFIX} for how to
     * configure {@link Filter} services with init params.
     */
    private Map<String, String> createInitParams() {
      String[] initParamKeys = filterServiceReference.getPropertyKeys();
      String initPrefixProp = getStringProperty(filterServiceReference, PROPERTY_INIT_PREFIX);
      if (initPrefixProp == null) {
        initPrefixProp = DEFAULT_INIT_PREFIX_PROP;
      }

      // make all the service parameters available as initParams
      Map<String, String> initParameters = new HashMap<>();
      for (String key : initParamKeys) {
        Object valueObject = filterServiceReference.getProperty(key);
        String value = valueObject == null ? "" : valueObject.toString();

        if (key.startsWith(initPrefixProp)) {
          initParameters.put(key.replaceFirst(initPrefixProp, ""), value);
        } else if (key.startsWith(HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX)) {
          initParameters.put(
              key.replaceFirst(HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX, ""), value);
        }
      }

      return initParameters;
    }
  }
}
