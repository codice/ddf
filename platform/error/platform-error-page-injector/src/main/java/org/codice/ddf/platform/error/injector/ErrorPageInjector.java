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
package org.codice.ddf.platform.error.injector;

import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import javax.servlet.ServletContext;
import org.codice.ddf.platform.error.servlet.ErrorServlet;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorPageInjector implements EventListenerHook {

  private final String errorPagePath = "/ErrorServlet";
  private ImmutableMap<String, String> errorCodesMap =
      new ImmutableMap.Builder<String, String>()
          .put("400", errorPagePath)
          .put("401", errorPagePath)
          .put("403", errorPagePath)
          .put("404", errorPagePath)
          .put("405", errorPagePath)
          .put("406", errorPagePath)
          .put("500", errorPagePath)
          .put("501", errorPagePath)
          .put("502", errorPagePath)
          .put("503", errorPagePath)
          .put("504", errorPagePath)
          .build();

  private static final Logger LOGGER = LoggerFactory.getLogger(ErrorPageInjector.class);

  Optional<BundleContext> getContext() {
    final Bundle cxfBundle = FrameworkUtil.getBundle(ErrorPageInjector.class);
    return (cxfBundle != null) ? Optional.of(cxfBundle.getBundleContext()) : Optional.empty();
  }

  @Override
  public void event(ServiceEvent event, Map<BundleContext, Collection<ListenerInfo>> listeners) {
    if (event.getType() == ServiceEvent.REGISTERED) {
      Bundle refBundle = event.getServiceReference().getBundle();
      BundleContext bundlectx = refBundle.getBundleContext();
      Object service = bundlectx.getService(event.getServiceReference());
      if (service instanceof ServletContext) {
        injectErrorPage((ServletContext) service, refBundle);
      }
    }
  }

  public void init() {
    checkForMissedServletContexts();
  }

  @SuppressWarnings("unchecked")
  private void checkForMissedServletContexts() {

    Optional<BundleContext> optionalBundleContext = getContext();
    if (!optionalBundleContext.isPresent()) {
      LOGGER.error(
          "Problem retrieving the bundle `Platform :: Error :: Page Injector` after it's just been initialized");
      return; // This should never happen
    }

    //  Get all registered servletContexts in OSGI
    final BundleContext context = optionalBundleContext.get();
    ServiceReference<ServletContext>[] references = null;

    try {
      references =
          (ServiceReference<ServletContext>[])
              context.getAllServiceReferences(
                  null, "(" + Constants.OBJECTCLASS + "=javax.servlet.ServletContext)");

    } catch (InvalidSyntaxException e) {
      LOGGER.error(
          "Problem getting ServletContexts from OSGI, One of the servlets running might print stack traces to the browser. A system restart is recommended. See debug logs for additional details.");
      LOGGER.debug("Additional Details:", e);
    }

    if (references == null) return;

    //  Now since we have the list of references loop through them and check to see if they have
    // error pages added
    for (ServiceReference<ServletContext> reference : references) {

      final Bundle refBundle = reference.getBundle();
      final BundleContext bundlectx = refBundle.getBundleContext();
      ServletContext service = bundlectx.getService(reference);

      //  Need to use the service to get the errorPageErrorHandler through the
      // ServletContextHandler
      Optional<ServletContextHandler> optionalHttpContext = getHTTPContext(service, refBundle);

      if (!optionalHttpContext.isPresent()) return;

      ServletContextHandler httpContext = optionalHttpContext.get();
      ErrorPageErrorHandler errorPageErrorHandler =
          (ErrorPageErrorHandler) httpContext.getErrorHandler();

      final Map<String, String> errorPages = errorPageErrorHandler.getErrorPages();

      if (errorPages.isEmpty()) {

        createAndAddErrorPageHandler(httpContext, refBundle);
      }
    }
  }

  private void injectErrorPage(ServletContext context, Bundle refBundle) {

    Optional<ServletContextHandler> optionalHttpServiceContext = getHTTPContext(context, refBundle);
    optionalHttpServiceContext.ifPresent(p -> createAndAddErrorPageHandler(p, refBundle));
  }

  private Optional<ServletContextHandler> getHTTPContext(ServletContext context, Bundle refBundle) {

    Field field;
    try {
      // this grabs the enclosing instance class, which is actually a private class
      // this is the only way to do this in Java
      field = context.getClass().getDeclaredField("this$0");
      field.setAccessible(true);
    } catch (NoSuchFieldException e) {
      LOGGER.warn(
          "Unable to find enclosing class of ServletContext for delegating the error page. The default jetty errors will display in the browser",
          e);

      return Optional.empty();
    }

    try {
      // need to grab the servlet context handler so we can get down to the handler, which is what
      // we really need
      return Optional.of((ServletContextHandler) field.get(context));

    } catch (IllegalAccessException e) {
      LOGGER.warn(
          "Unable to get the ServletContextHandler for {}. Jetty's default error page will be used for this context",
          refBundle.getSymbolicName(),
          e);

      return Optional.empty();
    }
  }

  private void createAndAddErrorPageHandler(
      ServletContextHandler httpServiceContext, Bundle refBundle) {

    // now that we have the handler, we can add in our own ErrorServlet
    ServletHandler handler = httpServiceContext.getServletHandler();

    ServletHolder errorServletHolder = new ServletHolder(new ErrorServlet());
    errorServletHolder.setServletHandler(handler);

    try {
      errorServletHolder.start();
      errorServletHolder.initialize();
    } catch (Exception e) {
      LOGGER.warn(
          "Unable to initialize an errorServletHolder for {}. Jetty's default error page will be used for this context",
          refBundle.getSymbolicName(),
          e);

      return;
    }

    LOGGER.info(
        "Injecting an error page into into {} ID: {}",
        refBundle.getSymbolicName(),
        refBundle.getBundleId());

    handler.addServletWithMapping(errorServletHolder, errorPagePath);

    ErrorPageErrorHandler errorPageErrorHandler = new ErrorPageErrorHandler();
    errorPageErrorHandler.setErrorPages(errorCodesMap);
    httpServiceContext.setErrorHandler(errorPageErrorHandler);
  }
}
