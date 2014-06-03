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
package org.codice.ddf.security.filter.websso;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.HandlerResult.Status;
import org.junit.Test;

public class WebSSOFilterTest {

    private static final String DDF_SECURITY_TOKEN = "ddf.security.securityToken";

    private static final String DDF_AUTHENTICATION_TOKEN = "ddf.security.token";
    
    private static final String MOCK_CONTEXT = "/test";

    @Test
    public void testDoFilterNoSecurityToken() throws IOException, ServletException {
        WebSSOFilter filter = new WebSSOFilter();

        // set handlers
        AuthenticationHandler handler1 = mock(AuthenticationHandler.class);
        HandlerResult resolveResult = mock(HandlerResult.class);
        when(resolveResult.getStatus()).thenReturn(Status.NO_ACTION);
        HandlerResult authResult = mock(HandlerResult.class);
        when(authResult.getStatus()).thenReturn(Status.COMPLETED);
        when(authResult.hasSecurityToken()).thenReturn(false);
        when(
                handler1.getNormalizedToken(any(ServletRequest.class), any(ServletResponse.class),
                        any(FilterChain.class), eq(true))).thenReturn(resolveResult);
        when(
                handler1.getNormalizedToken(any(ServletRequest.class), any(ServletResponse.class),
                        any(FilterChain.class), eq(false))).thenReturn(authResult);

        filter.setHandlerList(Arrays.asList(handler1));

        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn(MOCK_CONTEXT);
        HttpServletResponse response = mock(HttpServletResponse.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        // make sure the authentication result was set in the request
        verify(request).setAttribute(DDF_AUTHENTICATION_TOKEN, authResult);
    }

    @Test
    public void testDoFilterWithSecurityToken() throws ServletException, IOException {
        WebSSOFilter filter = new WebSSOFilter();

        // set handlers
        AuthenticationHandler handler1 = mock(AuthenticationHandler.class);
        HandlerResult resolveResult = mock(HandlerResult.class);
        when(resolveResult.getStatus()).thenReturn(Status.COMPLETED);
        when(resolveResult.hasSecurityToken()).thenReturn(true);
        when(
                handler1.getNormalizedToken(any(ServletRequest.class), any(ServletResponse.class),
                        any(FilterChain.class), eq(false))).thenReturn(resolveResult);

        filter.setHandlerList(Arrays.asList(handler1));

        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn(MOCK_CONTEXT);
        HttpServletResponse response = mock(HttpServletResponse.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        // make sure the security token result was set in the request
        verify(request).setAttribute(DDF_SECURITY_TOKEN, resolveResult.getCredentials());
    }
    
    @Test
    public void testDoFilterWithRedirected() throws ServletException, IOException {
        WebSSOFilter filter = new WebSSOFilter();

        // set handlers
        AuthenticationHandler handler1 = mock(AuthenticationHandler.class);
        HandlerResult resolveResult = mock(HandlerResult.class);
        when(resolveResult.getStatus()).thenReturn(Status.REDIRECTED);
        when(resolveResult.hasSecurityToken()).thenReturn(false);
        when(
                handler1.getNormalizedToken(any(ServletRequest.class), any(ServletResponse.class),
                        any(FilterChain.class), eq(false))).thenReturn(resolveResult);

        filter.setHandlerList(Arrays.asList(handler1));

        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn(MOCK_CONTEXT);
        HttpServletResponse response = mock(HttpServletResponse.class);

        filter.doFilter(request, response, filterChain);

        // the next filter should NOT be called
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testDoFilterForbidden() throws IOException, ServletException {
        WebSSOFilter filter = new WebSSOFilter();
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).flushBuffer();
        verify(filterChain, never()).doFilter(request, response);
    }

}
