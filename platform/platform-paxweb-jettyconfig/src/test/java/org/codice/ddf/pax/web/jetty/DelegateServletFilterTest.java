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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.mock.MockServiceReference;

@RunWith(MockitoJUnitRunner.class)
public class DelegateServletFilterTest {

  private DelegateServletFilter delegateServletFilter;

  @Mock private BundleContext bundleContext;

  private Set<ServiceReference<Filter>> registeredFilterServiceReferences;

  @Before
  public void setup() throws InvalidSyntaxException {
    registeredFilterServiceReferences = new HashSet<>();
    bundleContext = mock(BundleContext.class);
    when(bundleContext.getServiceReferences(Filter.class, null))
        .thenReturn(registeredFilterServiceReferences);

    delegateServletFilter =
        new DelegateServletFilter() {
          @Override
          protected BundleContext getContext() {
            return bundleContext;
          }
        };
  }

  @Test
  public void testInit() throws ServletException {
    delegateServletFilter.init(mock(FilterConfig.class));
  }

  @Test
  public void testDestroy() {
    delegateServletFilter.destroy();
  }

  @Test
  public void testDoFilterWithFilter() throws IOException, ServletException {
    // given
    final FilterConfig filterConfig = mock(FilterConfig.class);
    final ServletContext servletContext = mock(ServletContext.class);
    when(filterConfig.getServletContext()).thenReturn(servletContext);
    delegateServletFilter.init(filterConfig);

    final Filter filter = registerFilter(new Hashtable());

    final ServletRequest servletRequest = mock(ServletRequest.class);
    final ServletResponse servletResponse = mock(ServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    // when
    delegateServletFilter.doFilter(servletRequest, servletResponse, filterChain);

    // then
    final InOrder inOrder = Mockito.inOrder(filter, filterChain);

    final ArgumentCaptor<FilterConfig> argument = ArgumentCaptor.forClass(FilterConfig.class);
    inOrder.verify(filter).init(argument.capture());
    final FilterConfig filterConfigInitArg = argument.getValue();
    assertThat(filterConfigInitArg.getServletContext(), is(servletContext));

    inOrder
        .verify(filter)
        .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
    inOrder.verify(filterChain).doFilter(servletRequest, servletResponse);
  }

  @Test
  public void testDoFilterWithNoFilters() throws IOException, ServletException {
    // given
    delegateServletFilter.init(mock(FilterConfig.class));

    final ServletRequest servletRequest = mock(ServletRequest.class);
    final ServletResponse servletResponse = mock(ServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    // when
    delegateServletFilter.doFilter(servletRequest, servletResponse, filterChain);

    // then
    verify(filterChain).doFilter(servletRequest, servletResponse);
  }

  /**
   * The {@link javax.servlet.Filter#init(FilterConfig)} javadoc does not specify that the {@link
   * FilterConfig} argument may not be null. This test confirms that there are no errors when
   * initializing the {@link Filter}s and filtering if this situation occurs.
   */
  @Test
  public void testDoFilterAfterInitDelegateServletFilterWithNullFilterConfig()
      throws ServletException, IOException {
    // given
    delegateServletFilter.init(null);

    final Filter filter = registerFilter(new Hashtable());

    final ServletRequest servletRequest = mock(ServletRequest.class);
    final ServletResponse servletResponse = mock(ServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    // when
    delegateServletFilter.doFilter(servletRequest, servletResponse, filterChain);

    // then
    final InOrder inOrder = Mockito.inOrder(filter, filterChain);

    final ArgumentCaptor<FilterConfig> argument = ArgumentCaptor.forClass(FilterConfig.class);
    inOrder.verify(filter).init(argument.capture());
    final FilterConfig filterConfigInitArg = argument.getValue();
    assertThat(filterConfigInitArg.getServletContext(), is(nullValue(ServletContext.class)));

    inOrder
        .verify(filter)
        .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
    inOrder.verify(filterChain).doFilter(servletRequest, servletResponse);
  }

  @Test
  public void testFiltersOnlyInitializedOnce() throws IOException, ServletException {
    // given
    delegateServletFilter.init(mock(FilterConfig.class));

    final Dictionary dictionary1 = new Hashtable();
    dictionary1.put("osgi.http.whiteboard.filter.name", "filter1");
    final Filter filter1 = registerFilter(dictionary1);

    delegateServletFilter.doFilter(
        mock(ServletRequest.class), mock(ServletResponse.class), mock(FilterChain.class));

    final Dictionary dictionary2 = new Hashtable();
    dictionary2.put("osgi.http.whiteboard.filter.name", "filter2");
    final Filter filter2 = registerFilter(dictionary2);

    delegateServletFilter.doFilter(
        mock(ServletRequest.class), mock(ServletResponse.class), mock(FilterChain.class));
    delegateServletFilter.doFilter(
        mock(ServletRequest.class), mock(ServletResponse.class), mock(FilterChain.class));
    delegateServletFilter.doFilter(
        mock(ServletRequest.class), mock(ServletResponse.class), mock(FilterChain.class));

    final Dictionary dictionary3 = new Hashtable();
    dictionary3.put("osgi.http.whiteboard.filter.name", "filter3");
    final Filter filter3 = registerFilter(dictionary3);

    delegateServletFilter.doFilter(
        mock(ServletRequest.class), mock(ServletResponse.class), mock(FilterChain.class));

    // when
    verify(filter1).init(any(FilterConfig.class));
    verify(filter2).init(any(FilterConfig.class));
    verify(filter3).init(any(FilterConfig.class));
  }

  @Test
  public void testInitializeFilter() throws ServletException, IOException {
    // given
    final FilterConfig filterConfig = mock(FilterConfig.class);
    final ServletContext servletContext = mock(ServletContext.class);
    when(filterConfig.getServletContext()).thenReturn(servletContext);
    delegateServletFilter.init(filterConfig);

    final Dictionary dictionary = new Hashtable();
    final String filterNameValue = "my-test-filter";
    dictionary.put("osgi.http.whiteboard.filter.name", filterNameValue);
    final String param1Key = "param1Key";
    final String param1Value = "param1Value";
    dictionary.put("init." + param1Key, param1Value);
    final String param2Key = "param2Key";
    final String param2Value = "param2Value";
    dictionary.put("init." + param2Key, param2Value);
    final Filter filter = registerFilter(dictionary);

    // when
    delegateServletFilter.doFilter(
        mock(ServletRequest.class), mock(ServletResponse.class), mock(FilterChain.class));

    // then
    final ArgumentCaptor<FilterConfig> argument = ArgumentCaptor.forClass(FilterConfig.class);
    verify(filter).init(argument.capture());
    final FilterConfig filterConfigInitArg = argument.getValue();
    assertThat(filterConfigInitArg.getFilterName(), is(filterNameValue));
    assertThat(filterConfigInitArg.getServletContext(), is(servletContext));
    assertThat(filterConfigInitArg.getInitParameter(param1Key), is(param1Value));
    assertThat(filterConfigInitArg.getInitParameter(param2Key), is(param2Value));
    assertThat(filterConfigInitArg.getInitParameter("notAnInitPropertyKey"), nullValue());
    final List<String> initParamNames =
        Collections.list(filterConfigInitArg.getInitParameterNames());
    assertThat(initParamNames, hasSize(2));
    assertThat(initParamNames, containsInAnyOrder(param1Key, param2Key));
  }

  /**
   * This method tests some edge cases of adding {@link Filter}s through {@link ServiceReference}s.
   * Filter names must have service-property key="{@link
   * org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_FILTER_NAME}", so the
   * service-property with key="filter-name" will not be used to init the {@link
   * javax.servlet.Filter}. The init param service-property prefix may be defined using the {@link
   * org.ops4j.pax.web.extender.whiteboard.ExtenderConstants#PROPERTY_INIT_PREFIX} service-property
   * key.
   */
  @Test
  public void testInitializeFilterWithComplicatedInitParams() throws ServletException, IOException {
    // given
    final FilterConfig filterConfig = mock(FilterConfig.class);
    final ServletContext servletContext = mock(ServletContext.class);
    when(filterConfig.getServletContext()).thenReturn(servletContext);
    delegateServletFilter.init(filterConfig);

    final Dictionary dictionary = new Hashtable();
    dictionary.put("filter-name", "my-test-filter");
    final String incorrectlyFormattedInitParamKey = "incorrectlyFormattedInitParamKey";
    dictionary.put(
        "init." + incorrectlyFormattedInitParamKey, "incorrectlyFormattedInitParamValue");
    final String customInitPrefix = "myInitPrefix.";
    dictionary.put("init-prefix", customInitPrefix);
    final String initParamKey = "initParamKey";
    final String initParamValue = "initParamValue";
    dictionary.put(customInitPrefix + initParamKey, initParamValue);
    final String servletInitParamKey = "servletInitParamKey";
    final String servletInitParamValue = "servletInitParamValue";
    dictionary.put("servlet.init." + servletInitParamKey, servletInitParamValue);
    final String anotherServiceProperty = "anotherServiceProperty";
    dictionary.put(anotherServiceProperty, "anotherServicePropertyValue");
    final Filter filter = registerFilter(dictionary);

    // when
    delegateServletFilter.doFilter(
        mock(ServletRequest.class), mock(ServletResponse.class), mock(FilterChain.class));

    // then
    final ArgumentCaptor<FilterConfig> argument = ArgumentCaptor.forClass(FilterConfig.class);
    verify(filter).init(argument.capture());
    final FilterConfig filterConfigInitArg = argument.getValue();
    assertThat(filterConfigInitArg.getFilterName(), is(filter.getClass().getCanonicalName()));
    assertThat(filterConfigInitArg.getServletContext(), is(servletContext));
    assertThat(filterConfigInitArg.getInitParameter(incorrectlyFormattedInitParamKey), nullValue());
    assertThat(filterConfigInitArg.getInitParameter(initParamKey), is(initParamValue));
    assertThat(
        filterConfigInitArg.getInitParameter(servletInitParamKey), is(servletInitParamValue));
    assertThat(filterConfigInitArg.getInitParameter(anotherServiceProperty), nullValue());
    final List<String> initParamNames =
        Collections.list(filterConfigInitArg.getInitParameterNames());
    assertThat(initParamNames, hasSize(2));
    assertThat(initParamNames, containsInAnyOrder(initParamKey, servletInitParamKey));
  }

  @Test
  public void testRemoveFilter() {
    // given
    final Filter filter = mock(Filter.class);
    final MockServiceReference filterServiceReference = new MockServiceReference();
    filterServiceReference.setProperties(new Hashtable());
    when(bundleContext.getService(filterServiceReference)).thenReturn(filter);
    registeredFilterServiceReferences.add(filterServiceReference);

    // when
    delegateServletFilter.removeFilter(filterServiceReference);

    // then
    verify(filter).destroy();
  }

  @Test
  public void testRemoveNullFilter() {
    delegateServletFilter.removeFilter(null);
  }

  @Test
  public void testDoFilterWithFiltersInCorrectOrder() throws IOException, ServletException {
    // given
    delegateServletFilter.init(mock(FilterConfig.class));

    final Dictionary dictionary0 = new Hashtable();
    dictionary0.put(Constants.SERVICE_RANKING, 0);
    final Filter filter0 = registerFilter(dictionary0);

    final Dictionary dictionary100 = new Hashtable();
    dictionary100.put(Constants.SERVICE_RANKING, 100);
    final Filter filter100 = registerFilter(dictionary100);

    final Dictionary dictionary1 = new Hashtable();
    dictionary1.put(Constants.SERVICE_RANKING, 1);
    final Filter filter1 = registerFilter(dictionary1);

    final Filter filter = registerFilter(new Hashtable());

    final Dictionary dictionary99 = new Hashtable();
    dictionary99.put(Constants.SERVICE_RANKING, 99);
    final Filter filter99 = registerFilter(dictionary99);

    final ServletRequest servletRequest = mock(ServletRequest.class);
    final ServletResponse servletResponse = mock(ServletResponse.class);

    // when
    delegateServletFilter.doFilter(servletRequest, servletResponse, mock(FilterChain.class));

    // then
    final InOrder inOrder = Mockito.inOrder(filter0, filter100, filter1, filter, filter99);
    inOrder
        .verify(filter100)
        .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
    inOrder
        .verify(filter99)
        .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
    inOrder
        .verify(filter1)
        .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
    inOrder
        .verify(filter0)
        .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
    inOrder
        .verify(filter)
        .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
  }

  private Filter registerFilter(Dictionary serviceProperties) throws IOException, ServletException {
    final Filter filter = mock(Filter.class);
    Mockito.doAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              ((FilterChain) args[2])
                  .doFilter(((ServletRequest) args[0]), ((ServletResponse) args[1]));
              return null;
            })
        .when(filter)
        .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

    final MockServiceReference filterServiceReference = new MockServiceReference();
    filterServiceReference.setProperties(serviceProperties);
    when(bundleContext.getService(filterServiceReference)).thenReturn(filter);
    registeredFilterServiceReferences.add(filterServiceReference);
    return filter;
  }
}
