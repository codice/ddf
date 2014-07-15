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
package org.codice.admin.modules.installer.filter;

import org.mockito.exceptions.verification.NeverWantedButInvoked;
import org.mockito.exceptions.verification.WantedButNotInvoked;
import org.codice.admin.modules.installer.filter.RedirectFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeast;

public class RedirectFilterTest {

    @Test
    public void testDoFilterRedirect() throws Exception {
        RedirectFilter testFilter = new RedirectFilter();
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        Map tempMap = new HashMap<String, Object>();
        String[] tempArray = new String[]{RedirectFilter.ADMIN, RedirectFilter.INSTALLER, RedirectFilter.JOLOKIA};
        tempMap.put(RedirectFilter.LOGIN_IGNORE_LIST, tempArray);
        testFilter.setLoginIgnoreList(tempMap);
        when(servletRequest.getRequestURI()).thenReturn("http://localhost:8181/system/console");
        FilterChain filterChain = mock(FilterChain.class);
        try {
            testFilter.doFilter(servletRequest, servletResponse, filterChain);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        try {
            verify(servletResponse).sendRedirect("/admin");
        } catch (WantedButNotInvoked e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testDoFilterRedirectChangeConfig() throws Exception {
        RedirectFilter testFilter = new RedirectFilter();
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        when(servletRequest.getRequestURI()).thenReturn("http://localhost:8181/system/console");
        FilterChain filterChain = mock(FilterChain.class);
        Map tempMap = new HashMap<String, Object>();
        String[] tempArray = new String[]{RedirectFilter.ADMIN, RedirectFilter.INSTALLER, RedirectFilter.JOLOKIA, RedirectFilter.SYSTEM_CONSOLE};
        tempMap.put(RedirectFilter.LOGIN_IGNORE_LIST, tempArray);
        testFilter.setLoginIgnoreList(tempMap);
        try {
            testFilter.doFilter(servletRequest, servletResponse, filterChain);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        try {
            verify(servletResponse, never()).sendRedirect("/admin");
        } catch (NeverWantedButInvoked e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testDoFilterNoRedirect() throws Exception {
        RedirectFilter testFilter = new RedirectFilter();
        HttpServletRequest serviceRequest1 = mock(HttpServletRequest.class);
        HttpServletRequest serviceRequest2 = mock(HttpServletRequest.class);
        HttpServletRequest serviceRequest3 = mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        when(serviceRequest1.getRequestURI()).thenReturn("http://localhost:8181/admin");
        when(serviceRequest2.getRequestURI()).thenReturn("http://localhost:8181/installer");
        when(serviceRequest3.getRequestURI()).thenReturn("http://localhost:8181/jolokia");
        FilterChain filterChain = mock(FilterChain.class);

        testDoFilterNoRedirectCore(serviceRequest1, servletResponse, filterChain, testFilter, null);
        testDoFilterNoRedirectCore(serviceRequest2, servletResponse, filterChain, testFilter, null);
        testDoFilterNoRedirectCore(serviceRequest3, servletResponse, filterChain, testFilter, null);
    }

    private void testDoFilterNoRedirectCore(ServletRequest serviceRequest,
            ServletResponse servletResponse, FilterChain filterChain, RedirectFilter testFilter,
            String[] noBlockArgs) throws Exception {
        Map tempMap = new HashMap<String, Object>();
        String[] tempArray = noBlockArgs;
        tempMap.put(RedirectFilter.LOGIN_IGNORE_LIST, tempArray);
        testFilter.setLoginIgnoreList(tempMap);
        try {
            testFilter.doFilter(serviceRequest, servletResponse, filterChain);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        try {
            verify((HttpServletResponse) servletResponse, never()).sendRedirect("/admin");
        } catch (NeverWantedButInvoked e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testDoFilterNoRedirectChangeConfig() throws Exception {
        RedirectFilter testFilter = new RedirectFilter();
        HttpServletRequest serviceRequest1 = mock(HttpServletRequest.class);
        HttpServletRequest serviceRequest2 = mock(HttpServletRequest.class);
        HttpServletRequest serviceRequest3 = mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        when(serviceRequest1.getRequestURI()).thenReturn("http://localhost:8181/admin");
        when(serviceRequest2.getRequestURI()).thenReturn("http://localhost:8181/installer");
        when(serviceRequest3.getRequestURI()).thenReturn("http://localhost:8181/jolokia");
        FilterChain filterChain = mock(FilterChain.class);

        testDoFilterNoRedirectChangeConfigCore(serviceRequest1, servletResponse, filterChain,
                testFilter, new String[]{"/foo"});
        testDoFilterNoRedirectChangeConfigCore(serviceRequest2, servletResponse, filterChain,
                testFilter, new String[]{"/foo"});
        testDoFilterNoRedirectChangeConfigCore(serviceRequest3, servletResponse, filterChain,
                testFilter, new String[]{"/foo"});
    }

    private void testDoFilterNoRedirectChangeConfigCore(ServletRequest serviceRequest,
            ServletResponse servletResponse, FilterChain filterChain, RedirectFilter testFilter,
            String[] noBlockArgs) throws Exception {
        Map tempMap = new HashMap<String, Object>();
        String[] tempArray = noBlockArgs;
        tempMap.put(RedirectFilter.LOGIN_IGNORE_LIST, tempArray);
        testFilter.setLoginIgnoreList(tempMap);
        try {
            testFilter.doFilter(serviceRequest, servletResponse, filterChain);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        try {
            verify((HttpServletResponse) servletResponse, atLeast(1)).sendRedirect("/admin");
        } catch (WantedButNotInvoked e) {
            fail(e.getMessage());
        }
    }

}
