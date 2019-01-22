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
package org.codice.ddf.platform.filter.delegate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Injects the {@link DelegateServletFilter} into the {@link ServletContext} with properties
 * filterName = {@link FilterInjector#DELEGATING_FILTER}, urlPatterns = {@link
 * FilterInjector#ALL_URLS}, and no servlet name as the {@link ServletContext} is being created.
 * Sets up all {@link javax.servlet.Servlet}s with the {@link DelegateServletFilter} as the first in
 * the {@link javax.servlet.FilterChain}.
 */
public class FilterInjector implements EventListenerHook {

  private static final Logger LOGGER = LoggerFactory.getLogger(FilterInjector.class);

  private static final String ALL_URLS = "/*";

  private static final String DELEGATING_FILTER = "delegating-filter";

  private final Filter delegateServletFilter;

  private final List<HttpSessionListener> sessionListeners = new ArrayList<>();

  /**
   * Creates a new filter injector with the specified filter.
   *
   * @param filter filter that should be injected.
   */
  public FilterInjector(Filter filter) {
    this.delegateServletFilter = filter;
  }

  @Override
  public void event(ServiceEvent event, Map<BundleContext, Collection<ListenerInfo>> listeners) {
    if (event.getType() == ServiceEvent.REGISTERED) {
      Bundle refBundle = event.getServiceReference().getBundle();
      BundleContext bundlectx = refBundle.getBundleContext();
      Object service = bundlectx.getService(event.getServiceReference());
      if (service instanceof ServletContext
          && !refBundle.getSymbolicName().contains("platform-solr-server-standalone")) {
        injectFilter((ServletContext) service, refBundle);
      }
    }
  }

  /**
   * Injects the filter into the passed-in servlet context.
   *
   * @param context The servlet context that the filter should be injected into.
   * @param refBundle The bundle of the ServletContext
   */
  private void injectFilter(ServletContext context, Bundle refBundle) {
    try {
      SessionCookieConfig sessionCookieConfig = context.getSessionCookieConfig();
      sessionCookieConfig.setPath("/");
      sessionCookieConfig.setSecure(true);
      sessionCookieConfig.setHttpOnly(true);
    } catch (Exception e) {
      LOGGER.trace(
          "Failed trying to set the cookie config path to /. This can usually be ignored", e);
    }

    // Jetty will place non-programmatically added filters (filters added via web.xml) in front of
    // programmatically
    // added filters. This is probably OK in most instances, however, this security filter must
    // ALWAYS be first.
    // This reflection hack basically tricks Jetty into believing that this filter is a web.xml
    // filter so that it always ends up first.
    // In order for this to work correctly, the delegating filter must always be added before any
    // other programmatically added filters.
    Field field = null;
    try {
      // this grabs the enclosing instance class, which is actually a private class
      // this is the only way to do this in Java
      field = context.getClass().getDeclaredField("this$0");
      field.setAccessible(true);
    } catch (NoSuchFieldException e) {
      LOGGER.warn(
          "Unable to find enclosing class of ServletContext for delegating filter. Security may not work correctly",
          e);
    }
    Field matchAfterField = null;
    Object matchAfterValue = null;
    ServletHandler handler = null;
    if (field != null) {
      // need to grab the servlet context handler so we can get down to the handler, which is what
      // we really need
      ServletContextHandler httpServiceContext = null;
      try {
        httpServiceContext = (ServletContextHandler) field.get(context);
      } catch (IllegalAccessException e) {
        LOGGER.warn(
            "Unable to get the ServletContextHandler for {}. The delegating filter may not work properly.",
            refBundle.getSymbolicName(),
            e);
      }

      if (httpServiceContext != null) {
        // now that we have the handler, we can muck with the filters and state variables
        handler = httpServiceContext.getServletHandler();

        SessionHandler sessionHandler = httpServiceContext.getSessionHandler();
        if (sessionHandler != null) {
          sessionHandler.addEventListener(new WrapperListener());
        }

        if (handler != null) {
          try {
            matchAfterField =
                handler.getClass().getSuperclass().getDeclaredField("_matchAfterIndex");
            matchAfterField.setAccessible(true);
          } catch (NoSuchFieldException e) {
            LOGGER.warn(
                "Unable to find the matchAfterIndex value for the ServletHandler. The delegating filter may not work properly.",
                e);
          }

          if (matchAfterField != null) {
            try {
              // this value is initialized to -1 and only changes after a programmatic filter has
              // been added
              // so basically we are grabbing this value (should be -1) and then setting the field
              // back to that value
              // after we add our delegating filter to the mix
              matchAfterValue = matchAfterField.get(handler);
            } catch (IllegalAccessException e) {
              LOGGER.warn(
                  "Unable to get the value of the match after field. The delegating filter may not work properly.",
                  e);
            }
          }
        }
      }
    }

    try {
      // This causes the value of "_matchAfterIndex" to jump to 0 which means all web.xml filters
      // will be added in front of it
      // this isn't what we want, so we need to reset it back to what it was before
      FilterRegistration filterReg = context.addFilter(DELEGATING_FILTER, delegateServletFilter);

      if (filterReg == null) {
        filterReg = context.getFilterRegistration(DELEGATING_FILTER);
      } else {
        ((FilterRegistration.Dynamic) filterReg).setAsyncSupported(true);
      }

      filterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, ALL_URLS);
    } catch (IllegalStateException ise) {
      LOGGER.error(
          "Could not inject DelegateServletFilter into {} because the servlet was already initialized. This means that SecurityFilters will not be included in {}.",
          refBundle.getSymbolicName(),
          refBundle.getSymbolicName(),
          ise);
    }

    if (matchAfterField != null && matchAfterValue != null) {
      try {
        // Reset the value back to what it was before we added our delegating filter, this should
        // cause Jetty to behave as if
        // this was a filter added via web.xml
        matchAfterField.set(handler, matchAfterValue);
      } catch (IllegalAccessException e) {
        LOGGER.warn(
            "Unable to set the match after field back to the original value. The delegating filter might be out of order",
            e);
      }
    } else {
      LOGGER.warn(
          "Unable to set the match after field back to the original value. The delegating filter might be out of order.");
    }
  }

  public void addListener(ServiceReference<HttpSessionListener> sessionListener) {
    if (sessionListener != null) {
      Bundle refBundle = sessionListener.getBundle();
      if (refBundle != null) {
        BundleContext bundlectx = refBundle.getBundleContext();
        if (bundlectx != null) {
          HttpSessionListener service = bundlectx.getService(sessionListener);
          if (service != null) {
            sessionListeners.add(service);
          }
        }
      }
    }
  }

  public void removeListener(ServiceReference<HttpSessionListener> sessionListener) {
    if (sessionListener != null) {
      Bundle refBundle = sessionListener.getBundle();
      if (refBundle != null) {
        BundleContext bundlectx = refBundle.getBundleContext();
        if (bundlectx != null) {
          HttpSessionListener service = bundlectx.getService(sessionListener);
          if (service != null) {
            sessionListeners.remove(service);
          }
        }
      }
    }
  }

  private class WrapperListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent se) {
      for (HttpSessionListener httpSessionListener : sessionListeners) {
        httpSessionListener.sessionCreated(se);
      }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
      for (HttpSessionListener httpSessionListener : sessionListeners) {
        httpSessionListener.sessionDestroyed(se);
      }
    }
  }
}
