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

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.SessionCookieConfig;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Injects the {@link SecurityJavaSubjectFilter} into the {@link ServletContext} with properties
 * filterName = {@link FilterInjector#SECURITY_JAVA_SUBJECT_FILTER}, urlPatterns = {@link
 * FilterInjector#ALL_URLS}, and no servlet name as the {@link ServletContext} is being created.
 * Sets up all {@link javax.servlet.Servlet}s with the {@link SecurityJavaSubjectFilter} in the
 * {@link javax.servlet.FilterChain}.
 */
public class FilterInjector implements EventListenerHook {

  private static final Logger LOGGER = LoggerFactory.getLogger(FilterInjector.class);

  private static final String ALL_URLS = "/*";

  private static final String SECURITY_JAVA_SUBJECT_FILTER = "security-java-subject-filter";

  private final SecurityJavaSubjectFilter securityJavaSubjectFilter;

  private final ScheduledExecutorService executorService;

  /**
   * Creates a new filter injector with the specified {@link SecurityJavaSubjectFilter}.
   *
   * @param securityJavaSubjectFilter filter that should be injected.
   * @param executorService used to check for missed servlet contexts
   */
  public FilterInjector(
      SecurityJavaSubjectFilter securityJavaSubjectFilter,
      ScheduledExecutorService executorService) {
    this.securityJavaSubjectFilter = securityJavaSubjectFilter;
    this.executorService = executorService;
  }

  @Nullable
  protected BundleContext getContext() {
    final Bundle cxfBundle = FrameworkUtil.getBundle(SecurityJavaSubjectFilter.class);
    if (cxfBundle != null) {
      return cxfBundle.getBundleContext();
    }
    return null;
  }

  @Override
  public void event(ServiceEvent event, Map<BundleContext, Collection<ListenerInfo>> listeners) {
    if (event.getType() == ServiceEvent.REGISTERED) {
      Bundle refBundle = event.getServiceReference().getBundle();
      BundleContext bundlectx = refBundle.getBundleContext();
      Object service = bundlectx.getService(event.getServiceReference());
      if (service instanceof ServletContext) {
        injectFilter((ServletContext) service, refBundle);
      }
    }
  }

  public void init() {
    executorService.schedule(this::checkForMissedServletContexts, 1, TimeUnit.SECONDS);
  }

  public void destroy() {
    executorService.shutdownNow();
  }

  private void checkForMissedServletContexts() {
    try {
      BundleContext context = getContext();
      if (context == null) {
        return; // bundle is probably refreshing
      }
      Collection<ServiceReference<ServletContext>> references =
          context.getServiceReferences(ServletContext.class, null);
      for (ServiceReference<ServletContext> reference : references) {
        Bundle refBundle = reference.getBundle();
        BundleContext bundlectx = refBundle.getBundleContext();
        ServletContext service = bundlectx.getService(reference);

        if (service.getFilterRegistration(SECURITY_JAVA_SUBJECT_FILTER) == null) {
          LOGGER.error(
              "Security java subject filter failed to start in time to inject itself into {} {}. This means the {} servlet will not properly attach the user subject to requests. A system restart is recommended.",
              refBundle.getSymbolicName(),
              refBundle.getBundleId(),
              refBundle.getSymbolicName());
        }
      }

    } catch (InvalidSyntaxException e) {
      LOGGER.error(
          "Problem checking ServletContexts for SecurityJavaSubjectFilter injections. One of the servlets running might not have all of the needed filters injected. A system restart is recommended. See debug logs for additional details.");
      LOGGER.debug("Additional Details:", e);
    }
  }

  /**
   * Injects the filter into the passed-in servlet context.
   *
   * @param context The servlet context that the filter should be injected into.
   * @param refBundle The bundle of the ServletContext
   */
  private void injectFilter(ServletContext context, Bundle refBundle) {

    LOGGER.info(
        "Injecting SecurityJavaSubjectFilter into {} ID: {}",
        refBundle.getSymbolicName(),
        refBundle.getBundleId());
    try {
      SessionCookieConfig sessionCookieConfig = context.getSessionCookieConfig();
      sessionCookieConfig.setPath("/");
      sessionCookieConfig.setSecure(true);
      sessionCookieConfig.setHttpOnly(true);
    } catch (Exception e) {
      LOGGER.trace(
          "Failed trying to set the cookie config path to /. This can usually be ignored", e);
    }

    try {

      FilterRegistration filterReg =
          context.addFilter(SECURITY_JAVA_SUBJECT_FILTER, securityJavaSubjectFilter);

      if (filterReg == null) {
        filterReg = context.getFilterRegistration(SECURITY_JAVA_SUBJECT_FILTER);
      } else {
        ((FilterRegistration.Dynamic) filterReg).setAsyncSupported(true);
      }

      filterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, ALL_URLS);
    } catch (IllegalStateException ise) {
      LOGGER.error(
          "Could not inject SecurityJavaSubjectFilter into {} because the servlet was already initialized. This means that SecurityJavaSubjectFilter will not be included in {}.",
          refBundle.getSymbolicName(),
          refBundle.getSymbolicName(),
          ise);
    }
  }
}
