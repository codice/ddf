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
package org.codice.ddf.platform.filter.delegate;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.codice.ddf.platform.filter.delegate.ProxyFilterChain;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the proxy filter chain class.
 * 
 */
public class ProxyFilterChainTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyFilterChainTest.class);

    /**
     * Tests that all of the filters are properly called.
     * 
     * @throws ServletException
     * @throws IOException
     */
    @Test
    public void testDoFilter() throws IOException, ServletException {
        FilterChain initialChain = mock(FilterChain.class);
        ProxyFilterChain proxyChain = new ProxyFilterChain(
                initialChain);
        Filter filter1 = createMockFilter("filter1");
        Filter filter2 = createMockFilter("filter2");
        Filter filter3 = createMockFilter("filter3");

        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);

        proxyChain.addFilter(filter1);
        proxyChain.addFilters(Arrays.asList(filter2, filter3));

        proxyChain.doFilter(request, response);

        // Verify that all of the filters were called once.
        verify(filter1).doFilter(request, response, proxyChain);
        verify(filter2).doFilter(request, response, proxyChain);
        verify(filter3).doFilter(request, response, proxyChain);
        
        // the initial chain should have also been called once (at the end).
        verify(initialChain).doFilter(request, response);

    }

    /**
     * Tests that an exception is thrown if a new filter is attempted to be
     * added after the filter has been run.
     * 
     * @throws IOException
     * @throws ServletException
     */
    @Test(expected = IllegalStateException.class)
    public void testAddFilterAfterDo() throws IOException, ServletException {
        FilterChain initialChain = mock(FilterChain.class);
        ProxyFilterChain proxyChain = new ProxyFilterChain(
                initialChain);
        Filter filter1 = mock(Filter.class);
        proxyChain.doFilter(mock(ServletRequest.class), mock(ServletResponse.class));
        proxyChain.addFilter(filter1);
    }

    /**
     * Tests that an exception is thrown if more filters are attempted to be
     * added after the filter has been run.
     * 
     * @throws IOException
     * @throws ServletException
     */
    @Test(expected = IllegalStateException.class)
    public void testAddFiltersAfterDo() throws IOException, ServletException {
        FilterChain initialChain = mock(FilterChain.class);
        ProxyFilterChain proxyChain = new ProxyFilterChain(
                initialChain);
        Filter filter2 = mock(Filter.class);
        Filter filter3 = mock(Filter.class);
        proxyChain.doFilter(mock(ServletRequest.class), mock(ServletResponse.class));
        proxyChain.addFilters(Arrays.asList(filter2, filter3));
    }
    
    private Filter createMockFilter(final String name) throws IOException, ServletException {
        Filter mockFilter = mock(Filter.class);
        Mockito.when(mockFilter.toString()).thenReturn(name);
        Mockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                LOGGER.debug("{} was called.", name);
                ((FilterChain)args[2]).doFilter(((ServletRequest)args[0]), ((ServletResponse)args[1]));
                return null;
            }
            
        }).when(mockFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
        
        return mockFilter;
    }

}
