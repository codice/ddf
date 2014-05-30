/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.security.filter.delegate;

import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that the DelegateServletFilter is functionality properly.
 *
 */
public class DelegateServletFilterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelegateServletFilterTest.class);

    private Filter filter1;

    private Filter filter2;

    private Filter filter3;

    FilterChain initialChain;

    @Before
    public void resetGlobals() {
        initialChain = mock(FilterChain.class);
    }

    /**
     * Tests the main logic of performing the filter with adding filters.
     *
     * @throws ServletException
     * @throws IOException
     * @throws InvalidSyntaxException
     */
    @Test
    public void testDoFilterWithFiltersNoPolicy() throws IOException, ServletException,
            InvalidSyntaxException {
        ServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(HttpServletResponse.class);

        DelegateServletFilter filter = new DelegateServletFilter(mockContext(true));

        filter.doFilter(request, response, initialChain);

        verifyFiltersCalled(request, response, initialChain);

    }

    /**
     * Tests the main logic of performing the filter with no incoming filters.
     *
     * @throws ServletException
     * @throws IOException
     * @throws InvalidSyntaxException
     */
    @Test
    public void testDoFilterWithNoFiltersNoPolicy() throws IOException, ServletException,
            InvalidSyntaxException {
        ServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(HttpServletResponse.class);

        DelegateServletFilter filter = new DelegateServletFilter(mockContext(false));

        filter.doFilter(request, response, initialChain);

        verifyFiltersNotCalled(request, response, initialChain);
    }

    /**
     * Tests that all of the local filters are skipped if the current context
     * matches a policy with no authentication methods.
     *
     * @throws InvalidSyntaxException
     * @throws IOException
     * @throws ServletException
     */
    @Test
    public void testDoFilterWithFiltersWithEmptyPolicy() throws InvalidSyntaxException,
            IOException, ServletException {
        String contextPath = "/test/path";
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn(contextPath);
        ServletResponse response = mock(HttpServletResponse.class);

        // Create policy manager and policy without authentication method
        ContextPolicyManager policyManager = mock(ContextPolicyManager.class);
        when(policyManager.isWhiteListed(contextPath)).thenReturn(true);

        DelegateServletFilter filter = new DelegateServletFilter(mockContext(true));
        filter.setContextPolicyManager(policyManager);

        filter.doFilter(request, response, initialChain);

        verifyFiltersNotCalled(request, response, initialChain);
    }

    /**
     * Tests that all of the local filters are called if the current context
     * matches a policy that contains authentication methods.
     *
     * @throws InvalidSyntaxException
     * @throws IOException
     * @throws ServletException
     */
    @Test
    public void testDoFilterWithFiltersWithAuthPolicy() throws InvalidSyntaxException, IOException,
            ServletException {
        String contextPath = "/test/path";
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn(contextPath);
        ServletResponse response = mock(HttpServletResponse.class);

        // Create policy manager and policy with authentication method
        ContextPolicyManager policyManager = mock(ContextPolicyManager.class);
        ContextPolicy policy = mock(ContextPolicy.class);
        when(policy.getAuthenticationMethods()).thenReturn(Arrays.asList(new String[] {"SAML"}));
        when(policyManager.getContextPolicy(contextPath)).thenReturn(policy);

        BundleContext context = mockContext(true);
        DelegateServletFilter filter = new DelegateServletFilter(context);
        filter.setContextPolicyManager(policyManager);

        filter.doFilter(request, response, initialChain);

        verifyFiltersCalled(request, response, initialChain);

    }

    private void verifyFiltersCalled(ServletRequest request, ServletResponse response,
            FilterChain initialChain) throws IOException, ServletException {

        // verify that all of the filters were called once
        verify(filter1).doFilter(eq(request), eq(response), any(FilterChain.class));
        verify(filter2).doFilter(eq(request), eq(response), any(FilterChain.class));
        verify(filter3).doFilter(eq(request), eq(response), any(FilterChain.class));

        // verify initial chain was called once
        verify(initialChain).doFilter(request, response);
    }

    private void verifyFiltersNotCalled(ServletRequest request, ServletResponse response,
            FilterChain initialChain) throws IOException, ServletException {

        // verify that none of the filters were called
        verify(filter1, never()).doFilter(eq(request), eq(response), any(FilterChain.class));
        verify(filter2, never()).doFilter(eq(request), eq(response), any(FilterChain.class));
        verify(filter3, never()).doFilter(eq(request), eq(response), any(FilterChain.class));

        // verify initial chain was called once
        verify(initialChain).doFilter(request, response);
    }

    private BundleContext mockContext(boolean includeFilters) throws InvalidSyntaxException,
            IOException, ServletException {
        BundleContext context = mock(BundleContext.class);
        filter1 = createMockFilter("filter1");
        filter2 = createMockFilter("filter2");
        filter3 = createMockFilter("filter3");
        ServiceReference<Filter> ref1 = createMockReference();
        ServiceReference<Filter> ref2 = createMockReference();
        ServiceReference<Filter> ref3 = createMockReference();
        List<ServiceReference<Filter>> refList = new ArrayList<ServiceReference<Filter>>(3);
        if (includeFilters) {
            refList.add(ref1);
            refList.add(ref2);
            refList.add(ref3);
        }
        when(context.getServiceReferences(Filter.class, null)).thenReturn(refList);
        when(context.getService(ref1)).thenReturn(filter1);
        when(context.getService(ref2)).thenReturn(filter2);
        when(context.getService(ref3)).thenReturn(filter3);
        return context;
    }

    private Filter createMockFilter(final String name) throws IOException, ServletException {
        Filter mockFilter = mock(Filter.class);
        Mockito.when(mockFilter.toString()).thenReturn(name);
        Mockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                LOGGER.debug("{} was called.", name);
                ((FilterChain) args[2]).doFilter(((ServletRequest) args[0]),
                        ((ServletResponse) args[1]));
                return null;
            }

        })
                .when(mockFilter)
                .doFilter(any(ServletRequest.class), any(ServletResponse.class),
                        any(FilterChain.class));

        return mockFilter;
    }

    @SuppressWarnings("unchecked")
    private ServiceReference<Filter> createMockReference() {
        ServiceReference<Filter> reference = mock(ServiceReference.class);
        return reference;
    }

}
