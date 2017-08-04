/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.platform.filter.delegate;

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

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.codice.ddf.platform.filter.SecurityFilter;
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

    @Mock
    private BundleContext bundleContext;

    private Set<ServiceReference<SecurityFilter>> registeredSecurityFilterServiceReferences;

    @Before
    public void setup() throws InvalidSyntaxException {
        registeredSecurityFilterServiceReferences = new HashSet<>();
        bundleContext = mock(BundleContext.class);
        when(bundleContext.getServiceReferences(SecurityFilter.class, null)).thenReturn(
                registeredSecurityFilterServiceReferences);

        delegateServletFilter = new DelegateServletFilter() {
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
    public void testDestroy() throws ServletException {
        delegateServletFilter.destroy();
    }

    @Test
    public void testDoFilterWithSecurityFilter() throws IOException, ServletException {
        // given
        final FilterConfig filterConfig = mock(FilterConfig.class);
        final ServletContext servletContext = mock(ServletContext.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        delegateServletFilter.init(filterConfig);

        final SecurityFilter securityFilter = registerSecurityFilter(new Hashtable());

        final ServletRequest servletRequest = mock(ServletRequest.class);
        final ServletResponse servletResponse = mock(ServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        // when
        delegateServletFilter.doFilter(servletRequest, servletResponse, filterChain);

        // then
        final InOrder inOrder = Mockito.inOrder(securityFilter, filterChain);

        final ArgumentCaptor<FilterConfig> argument = ArgumentCaptor.forClass(FilterConfig.class);
        inOrder.verify(securityFilter).init(argument.capture());
        final FilterConfig filterConfigInitArg = argument.getValue();
        assertThat(filterConfigInitArg.getServletContext(), is(servletContext));

        inOrder.verify(securityFilter)
                .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
        inOrder.verify(filterChain)
                .doFilter(servletRequest, servletResponse);
    }

    @Test
    public void testDoFilterWithNoSecurityFilters() throws IOException, ServletException {
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
     * The {@link javax.servlet.Filter#init(FilterConfig)} javadoc does not specify that the
     * {@link FilterConfig} argument may not be null. This test confirms that there are no errors
     * when initializing the {@link SecurityFilter}s and filtering if this situation occurs.
     */
    @Test
    public void testDoFilterAfterInitDelegateServletFilterWithNullFilterConfig()
            throws ServletException, IOException {
        // given
        delegateServletFilter.init(null);

        final SecurityFilter securityFilter = registerSecurityFilter(new Hashtable());

        final ServletRequest servletRequest = mock(ServletRequest.class);
        final ServletResponse servletResponse = mock(ServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        // when
        delegateServletFilter.doFilter(servletRequest, servletResponse, filterChain);

        // then
        final InOrder inOrder = Mockito.inOrder(securityFilter, filterChain);

        final ArgumentCaptor<FilterConfig> argument = ArgumentCaptor.forClass(FilterConfig.class);
        inOrder.verify(securityFilter).init(argument.capture());
        final FilterConfig filterConfigInitArg = argument.getValue();
        assertThat(filterConfigInitArg.getServletContext(), is(nullValue(ServletContext.class)));

        inOrder.verify(securityFilter)
                .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
        inOrder.verify(filterChain)
                .doFilter(servletRequest, servletResponse);
    }

    @Test
    public void testSecurityFiltersOnlyInitializedOnce() throws IOException, ServletException {
        // given
        delegateServletFilter.init(mock(FilterConfig.class));

        final Dictionary dictionary1 = new Hashtable();
        dictionary1.put("osgi.http.whiteboard.filter.name", "filter1");
        final SecurityFilter securityFilter1 = registerSecurityFilter(dictionary1);

        delegateServletFilter.doFilter(mock(ServletRequest.class),
                mock(ServletResponse.class),
                mock(FilterChain.class));

        final Dictionary dictionary2 = new Hashtable();
        dictionary2.put("osgi.http.whiteboard.filter.name", "filter2");
        final SecurityFilter securityFilter2 = registerSecurityFilter(dictionary2);

        delegateServletFilter.doFilter(mock(ServletRequest.class),
                mock(ServletResponse.class),
                mock(FilterChain.class));
        delegateServletFilter.doFilter(mock(ServletRequest.class),
                mock(ServletResponse.class),
                mock(FilterChain.class));
        delegateServletFilter.doFilter(mock(ServletRequest.class),
                mock(ServletResponse.class),
                mock(FilterChain.class));

        final Dictionary dictionary3 = new Hashtable();
        dictionary3.put("osgi.http.whiteboard.filter.name", "filter3");
        final SecurityFilter securityFilter3 = registerSecurityFilter(dictionary3);

        delegateServletFilter.doFilter(mock(ServletRequest.class),
                mock(ServletResponse.class),
                mock(FilterChain.class));

        // when
        verify(securityFilter1).init(any(FilterConfig.class));
        verify(securityFilter2).init(any(FilterConfig.class));
        verify(securityFilter3).init(any(FilterConfig.class));
    }

    @Test
    public void testInitializeSecurityFilter() throws ServletException, IOException {
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
        final SecurityFilter securityFilter = registerSecurityFilter(dictionary);

        // when
        delegateServletFilter.doFilter(mock(ServletRequest.class),
                mock(ServletResponse.class),
                mock(FilterChain.class));

        // then
        final ArgumentCaptor<FilterConfig> argument = ArgumentCaptor.forClass(FilterConfig.class);
        verify(securityFilter).init(argument.capture());
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
     * This method tests some edge cases of adding {@link SecurityFilter}s through
     * {@link ServiceReference}s. Filter names must have service-property key="{@link
     * org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_FILTER_NAME}", so
     * the service-property with key="filter-name" will not be used to init the
     * {@link javax.servlet.Filter}. The init param service-property prefix may be defined using the
     * {@link org.ops4j.pax.web.extender.whiteboard.ExtenderConstants#PROPERTY_INIT_PREFIX}
     * service-property key.
     */
    @Test
    public void testInitializeSecurityFilterWithComplicatedInitParams()
            throws ServletException, IOException {
        // given
        final FilterConfig filterConfig = mock(FilterConfig.class);
        final ServletContext servletContext = mock(ServletContext.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        delegateServletFilter.init(filterConfig);

        final Dictionary dictionary = new Hashtable();
        dictionary.put("filter-name", "my-test-filter");
        final String incorrectlyFormattedInitParamKey = "incorrectlyFormattedInitParamKey";
        dictionary.put("init." + incorrectlyFormattedInitParamKey,
                "incorrectlyFormattedInitParamValue");
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
        final SecurityFilter securityFilter = registerSecurityFilter(dictionary);

        // when
        delegateServletFilter.doFilter(mock(ServletRequest.class),
                mock(ServletResponse.class),
                mock(FilterChain.class));

        // then
        final ArgumentCaptor<FilterConfig> argument = ArgumentCaptor.forClass(FilterConfig.class);
        verify(securityFilter).init(argument.capture());
        final FilterConfig filterConfigInitArg = argument.getValue();
        assertThat(filterConfigInitArg.getFilterName(),
                is(securityFilter.getClass()
                        .getCanonicalName()));
        assertThat(filterConfigInitArg.getServletContext(), is(servletContext));
        assertThat(filterConfigInitArg.getInitParameter(incorrectlyFormattedInitParamKey),
                nullValue());
        assertThat(filterConfigInitArg.getInitParameter(initParamKey), is(initParamValue));
        assertThat(filterConfigInitArg.getInitParameter(servletInitParamKey),
                is(servletInitParamValue));
        assertThat(filterConfigInitArg.getInitParameter(anotherServiceProperty), nullValue());
        final List<String> initParamNames =
                Collections.list(filterConfigInitArg.getInitParameterNames());
        assertThat(initParamNames, hasSize(2));
        assertThat(initParamNames, containsInAnyOrder(initParamKey, servletInitParamKey));
    }

    @Test
    public void testRemoveSecurityFilter() throws IOException, ServletException {
        // given
        final SecurityFilter securityFilter = mock(SecurityFilter.class);
        final MockServiceReference securityFilterServiceReference = new MockServiceReference();
        securityFilterServiceReference.setProperties(new Hashtable());
        when(bundleContext.getService(securityFilterServiceReference)).thenReturn(securityFilter);
        registeredSecurityFilterServiceReferences.add(securityFilterServiceReference);

        // when
        delegateServletFilter.removeSecurityFilter(securityFilterServiceReference);

        // then
        verify(securityFilter).destroy();
    }

    @Test
    public void testRemoveNullSecurityFilter() throws IOException, ServletException {
        delegateServletFilter.removeSecurityFilter(null);
    }

    @Test
    public void testDoFilterWithSecurityFiltersInCorrectOrder()
            throws IOException, ServletException, InvalidSyntaxException {
        // given
        delegateServletFilter.init(mock(FilterConfig.class));

        final Dictionary dictionary0 = new Hashtable();
        dictionary0.put(Constants.SERVICE_RANKING, 0);
        final SecurityFilter securityFilter0 = registerSecurityFilter(dictionary0);

        final Dictionary dictionary100 = new Hashtable();
        dictionary100.put(Constants.SERVICE_RANKING, 100);
        final SecurityFilter securityFilter100 = registerSecurityFilter(dictionary100);

        final Dictionary dictionary1 = new Hashtable();
        dictionary1.put(Constants.SERVICE_RANKING, 1);
        final SecurityFilter securityFilter1 = registerSecurityFilter(dictionary1);

        final SecurityFilter securityFilter = registerSecurityFilter(new Hashtable());

        final Dictionary dictionary99 = new Hashtable();
        dictionary99.put(Constants.SERVICE_RANKING, 99);
        final SecurityFilter securityFilter99 = registerSecurityFilter(dictionary99);

        final ServletRequest servletRequest = mock(ServletRequest.class);
        final ServletResponse servletResponse = mock(ServletResponse.class);

        // when
        delegateServletFilter.doFilter(servletRequest, servletResponse, mock(FilterChain.class));

        // then
        final InOrder inOrder = Mockito.inOrder(securityFilter0,
                securityFilter100,
                securityFilter1,
                securityFilter,
                securityFilter99);
        inOrder.verify(securityFilter100)
                .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
        inOrder.verify(securityFilter99)
                .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
        inOrder.verify(securityFilter1)
                .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
        inOrder.verify(securityFilter0)
                .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
        inOrder.verify(securityFilter)
                .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
    }

    private SecurityFilter registerSecurityFilter(Dictionary serviceProperties)
            throws IOException, ServletException {
        final SecurityFilter securityFilter = mock(SecurityFilter.class);
        Mockito.doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((FilterChain) args[2]).doFilter(((ServletRequest) args[0]),
                    ((ServletResponse) args[1]));
            return null;
        })
                .when(securityFilter)
                .doFilter(any(ServletRequest.class),
                        any(ServletResponse.class),
                        any(FilterChain.class));

        final MockServiceReference securityFilterServiceReference = new MockServiceReference();
        securityFilterServiceReference.setProperties(serviceProperties);
        when(bundleContext.getService(securityFilterServiceReference)).thenReturn(securityFilter);
        registeredSecurityFilterServiceReferences.add(securityFilterServiceReference);
        return securityFilter;
    }
}
