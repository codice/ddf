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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.concurrent.ScheduledExecutorService;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.SessionCookieConfig;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Tests the functionality of the filter injector to verify that all of the methods function
 * properly with adding and removing filters.
 */
public class FilterInjectorTest {

  private ServiceReference curReference;

  private ServiceEvent curEvent;

  private ServletContext curContext;

  private ScheduledExecutorService executorService;

  /** Tests that the filter is registered when the injectFilter method is called. */
  @Test
  public void testInjectFilter() {
    SecurityJavaSubjectFilter filter = mock(SecurityJavaSubjectFilter.class);
    executorService = mock(ScheduledExecutorService.class);
    FilterInjector injector = new FilterInjector(filter, executorService);
    updateMockReference();

    injector.event(curEvent, null);

    verify(curContext).addFilter("security-java-subject-filter", filter);
  }

  @Test
  public void testInjectFilterHandlesOnlyServletContext() {
    SecurityJavaSubjectFilter filter = mock(SecurityJavaSubjectFilter.class);
    FilterInjector injector = new FilterInjector(filter, executorService);
    curEvent = mock(ServiceEvent.class);
    curReference = mock(ServiceReference.class);
    curContext = mock(ServletContext.class);
    Servlet service = mock(Servlet.class);
    Bundle bundle = mock(Bundle.class);
    BundleContext context = mock(BundleContext.class);

    when(curEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
    when(curEvent.getServiceReference()).thenReturn(curReference);
    when(curReference.getBundle()).thenReturn(bundle);
    when(bundle.getBundleContext()).thenReturn(context);
    when(context.getService(curReference)).thenReturn(service);

    injector.event(curEvent, null);

    verify(curContext, never()).addFilter("delegating-filter", filter);
  }

  @Test
  public void testInjectFilterIgnoresUnregisteringEvents() {
    SecurityJavaSubjectFilter filter = mock(SecurityJavaSubjectFilter.class);
    FilterInjector injector = new FilterInjector(filter, executorService);
    curEvent = mock(ServiceEvent.class);
    when(curEvent.getType()).thenReturn(ServiceEvent.UNREGISTERING);

    injector.event(curEvent, null);

    verify(curEvent, never()).getServiceReference();
  }

  @Test
  public void testInjectFilterIgnoresModifiedEvents() {
    SecurityJavaSubjectFilter filter = mock(SecurityJavaSubjectFilter.class);
    FilterInjector injector = new FilterInjector(filter, executorService);
    curEvent = mock(ServiceEvent.class);
    when(curEvent.getType()).thenReturn(ServiceEvent.MODIFIED);

    injector.event(curEvent, null);

    verify(curEvent, never()).getServiceReference();
  }

  @Test
  public void testInjectFilterIgnoresModifiedEndMatchEvents() {
    SecurityJavaSubjectFilter filter = mock(SecurityJavaSubjectFilter.class);
    FilterInjector injector = new FilterInjector(filter, executorService);
    curEvent = mock(ServiceEvent.class);
    when(curEvent.getType()).thenReturn(ServiceEvent.MODIFIED_ENDMATCH);

    injector.event(curEvent, null);

    verify(curEvent, never()).getServiceReference();
  }

  @SuppressWarnings("unchecked")
  private void updateMockReference() {
    curEvent = mock(ServiceEvent.class);
    curReference = mock(ServiceReference.class);
    Bundle bundle = mock(Bundle.class);
    BundleContext context = mock(BundleContext.class);
    curContext = mock(ServletContext.class);
    FilterRegistration.Dynamic filterReg = mock(FilterRegistration.Dynamic.class);
    when(curContext.addFilter(anyString(), any(Filter.class))).thenReturn(filterReg);
    when(curContext.getSessionCookieConfig()).thenReturn(mock(SessionCookieConfig.class));
    ServiceRegistration<Filter> curRegistration = mock(ServiceRegistration.class);
    when(context.registerService(
            eq(Filter.class),
            Mockito.any(Filter.class),
            Matchers.<Dictionary<String, Object>>any()))
        .thenReturn(curRegistration);
    when(context.getService(curReference)).thenReturn(curContext);
    when(bundle.getBundleContext()).thenReturn(context);
    when(bundle.getSymbolicName()).thenReturn("Mock Bundle.");
    when(curReference.getBundle()).thenReturn(bundle);
    when(curEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
    when(curEvent.getServiceReference()).thenReturn(curReference);
  }
}
