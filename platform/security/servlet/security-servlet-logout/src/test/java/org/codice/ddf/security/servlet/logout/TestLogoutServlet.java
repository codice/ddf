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
package org.codice.ddf.security.servlet.logout;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Test;

import ddf.security.SecurityConstants;
import ddf.security.common.SecurityTokenHolder;

public class TestLogoutServlet {
    @Test
    public void testLocalLogout() {

        LocalLogoutServlet localLogoutServlet = new MockLocalLogoutServlet();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Subject subject = mock(Subject.class);
        when(subject.hasRole(anyString())).thenReturn(false);
        ThreadContext.bind(subject);

        System.setProperty("security.audit.roles", "none");

        HttpSession httpSession = mock(HttpSession.class);
        when(request.getSession(anyBoolean())).thenReturn(httpSession);
        when(request.getSession(anyBoolean())
                .getId()).thenReturn("id");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://foo.bar"));

        //Used for detecting basic auth
        when(request.getHeaders(anyString())).thenReturn(new Enumeration() {
            @Override
            public boolean hasMoreElements() {
                return true;
            }

            @Override
            public Object nextElement() {
                return "Basic";
            }
        });

        //used for detecting pki
        when(request.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(new X509Certificate[] {
                mock(X509Certificate.class)});

        SecurityTokenHolder securityTokenHolder = mock(SecurityTokenHolder.class);
        when(httpSession.getAttribute(SecurityConstants.SAML_ASSERTION)).thenReturn(
                securityTokenHolder);
        try {
            localLogoutServlet.doGet(request, response);
        } catch (ServletException | IOException e) {
            fail(e.getMessage());
        }
        verify(httpSession).invalidate();
    }

    //Since the servlet context is only set properly during startup, this mocks out the servlet context to avoid an exception during redirect
    private class MockLocalLogoutServlet extends LocalLogoutServlet {
        @Override
        public ServletContext getServletContext() {
            ServletContext servletContext = mock(ServletContext.class);
            when(servletContext.getContext(any(String.class))).thenReturn(servletContext);
            when(servletContext.getRequestDispatcher(any(String.class))).thenReturn(mock(
                    RequestDispatcher.class));
            return servletContext;
        }

    }

}