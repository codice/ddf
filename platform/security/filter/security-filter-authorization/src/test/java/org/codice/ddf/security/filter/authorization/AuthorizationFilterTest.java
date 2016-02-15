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
package org.codice.ddf.security.filter.authorization;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.codice.ddf.security.policy.context.attributes.ContextAttributeMapping;
import org.junit.Before;
import org.junit.Test;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;

public class AuthorizationFilterTest {
    private boolean sucess = false;

    @Before
    public void setup() {
        sucess = false;
    }

    @Test
    public void testAuthorizedSubject() {
        FilterConfig filterConfig = mock(FilterConfig.class);
        ContextPolicyManager contextPolicyManager = new TestPolicyManager();
        contextPolicyManager.setContextPolicy("/path", new TestContextPolicy());
        AuthorizationFilter loginFilter = new AuthorizationFilter(contextPolicyManager);
        try {
            loginFilter.init(filterConfig);
        } catch (ServletException e) {
            fail(e.getMessage());
        }

        Subject subject = mock(Subject.class);
        when(subject.isPermitted(any(CollectionPermission.class))).thenReturn(true);
        ThreadContext.bind(subject);

        HttpServletRequest servletRequest = new TestHttpServletRequest();
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);

        FilterChain filterChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                sucess = true;
            }
        };

        try {
            loginFilter.doFilter(servletRequest, servletResponse, filterChain);
            if (!sucess) {
                fail("Should have called doFilter with a valid Subject");
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (ServletException e) {
            fail(e.getMessage());
        }
        ThreadContext.unbindSubject();
    }

    @Test
    public void testUnAuthorizedSubject() {
        FilterConfig filterConfig = mock(FilterConfig.class);
        ContextPolicyManager contextPolicyManager = new TestPolicyManager();
        contextPolicyManager.setContextPolicy("/path", new TestContextPolicy());
        AuthorizationFilter loginFilter = new AuthorizationFilter(contextPolicyManager);
        try {
            loginFilter.init(filterConfig);
        } catch (ServletException e) {
            fail(e.getMessage());
        }

        Subject subject = mock(Subject.class);
        when(subject.isPermitted(any(CollectionPermission.class))).thenReturn(false);
        ThreadContext.bind(subject);

        HttpServletRequest servletRequest = new TestHttpServletRequest();
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                fail("Should not have called doFilter without a valid Subject");
            }
        };

        try {
            loginFilter.doFilter(servletRequest, servletResponse, filterChain);
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (ServletException e) {
            fail(e.getMessage());
        }
        ThreadContext.unbindSubject();
    }

    @Test
    public void testNoSubject() {
        FilterConfig filterConfig = mock(FilterConfig.class);
        ContextPolicyManager contextPolicyManager = new TestPolicyManager();
        contextPolicyManager.setContextPolicy("/path", new TestContextPolicy());
        AuthorizationFilter loginFilter = new AuthorizationFilter(contextPolicyManager);
        try {
            loginFilter.init(filterConfig);
        } catch (ServletException e) {
            fail(e.getMessage());
        }

        HttpServletRequest servletRequest = new TestHttpServletRequest();
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                fail("Should not have called doFilter without a valid Subject");
            }
        };

        try {
            loginFilter.doFilter(servletRequest, servletResponse, filterChain);
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (ServletException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testBadSubject() {
        FilterConfig filterConfig = mock(FilterConfig.class);
        ContextPolicyManager contextPolicyManager = new TestPolicyManager();
        contextPolicyManager.setContextPolicy("/path", new TestContextPolicy());
        AuthorizationFilter loginFilter = new AuthorizationFilter(contextPolicyManager);
        try {
            loginFilter.init(filterConfig);
        } catch (ServletException e) {
            fail(e.getMessage());
        }

        HttpServletRequest servletRequest = new TestHttpServletRequest();
        servletRequest.setAttribute(SecurityConstants.SECURITY_SUBJECT, mock(Subject.class));
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                fail("Should not have called doFilter without a valid Subject");
            }
        };

        try {
            loginFilter.doFilter(servletRequest, servletResponse, filterChain);
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (ServletException e) {
            fail(e.getMessage());
        }
    }

    private class TestContextPolicy implements ContextPolicy {

        @Override
        public String getContextPath() {
            return "/path";
        }

        @Override
        public Collection<String> getAuthenticationMethods() {
            return Arrays.asList("BASIC");
        }

        @Override
        public CollectionPermission getAllowedAttributePermissions() {

            return new KeyValueCollectionPermission(getContextPath(),
                    new KeyValuePermission(getContextPath(), Arrays.asList("permission")));
        }

        @Override
        public Collection<String> getAllowedAttributeNames() {
            return null;
        }

        @Override
        public Collection<ContextAttributeMapping> getAllowedAttributes() {
            return null;
        }

        @Override
        public String getRealm() {
            return "DDF";
        }
    }

    private class TestPolicyManager implements ContextPolicyManager {
        Map<String, ContextPolicy> stringContextPolicyMap = new HashMap<String, ContextPolicy>();

        @Override
        public ContextPolicy getContextPolicy(String path) {
            return stringContextPolicyMap.get(path);
        }

        @Override
        public Collection<ContextPolicy> getAllContextPolicies() {
            return stringContextPolicyMap.values();
        }

        @Override
        public void setContextPolicy(String path, ContextPolicy contextPolicy) {
            stringContextPolicyMap.put(path, contextPolicy);
        }

        @Override
        public boolean isWhiteListed(String path) {
            return false;
        }
    }

    private class TestHttpServletRequest implements HttpServletRequest {

        private Map<Object, Object> attrMap = new HashMap<Object, Object>();

        @Override
        public String getAuthType() {
            return null;
        }

        @Override
        public Cookie[] getCookies() {
            return new Cookie[0];
        }

        @Override
        public long getDateHeader(String name) {
            return 0;
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public Enumeration getHeaders(String name) {
            return null;
        }

        @Override
        public Enumeration getHeaderNames() {
            return null;
        }

        @Override
        public int getIntHeader(String name) {
            return 0;
        }

        @Override
        public String getMethod() {
            return null;
        }

        @Override
        public String getPathInfo() {
            return null;
        }

        @Override
        public String getPathTranslated() {
            return null;
        }

        @Override
        public String getContextPath() {
            return "/path";
        }

        @Override
        public String getQueryString() {
            return null;
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @Override
        public boolean isUserInRole(String role) {
            return false;
        }

        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public String getRequestedSessionId() {
            return null;
        }

        @Override
        public String getRequestURI() {
            return null;
        }

        @Override
        public StringBuffer getRequestURL() {
            return null;
        }

        @Override
        public String getServletPath() {
            return null;
        }

        @Override
        public HttpSession getSession(boolean create) {
            return null;
        }

        @Override
        public HttpSession getSession() {
            return null;
        }

        @Override
        public String changeSessionId() {
            return null;
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {
            return false;
        }

        @Override
        public boolean authenticate(HttpServletResponse httpServletResponse)
                throws IOException, ServletException {
            return false;
        }

        @Override
        public void login(String s, String s1) throws ServletException {

        }

        @Override
        public void logout() throws ServletException {

        }

        @Override
        public Collection<Part> getParts() throws IOException, ServletException {
            return null;
        }

        @Override
        public Part getPart(String s) throws IOException, ServletException {
            return null;
        }

        @Override
        public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass)
                throws IOException, ServletException {
            return null;
        }

        @Override
        public Object getAttribute(String name) {
            return attrMap.get(name);
        }

        @Override
        public Enumeration getAttributeNames() {
            return null;
        }

        @Override
        public String getCharacterEncoding() {
            return null;
        }

        @Override
        public void setCharacterEncoding(String env) throws UnsupportedEncodingException {

        }

        @Override
        public int getContentLength() {
            return 0;
        }

        @Override
        public long getContentLengthLong() {
            return 0;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return null;
        }

        @Override
        public String getParameter(String name) {
            return null;
        }

        @Override
        public Enumeration getParameterNames() {
            return null;
        }

        @Override
        public String[] getParameterValues(String name) {
            return new String[0];
        }

        @Override
        public Map getParameterMap() {
            return null;
        }

        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public String getScheme() {
            return null;
        }

        @Override
        public String getServerName() {
            return null;
        }

        @Override
        public int getServerPort() {
            return 0;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return null;
        }

        @Override
        public String getRemoteAddr() {
            return null;
        }

        @Override
        public String getRemoteHost() {
            return null;
        }

        @Override
        public void setAttribute(String name, Object o) {
            attrMap.put(name, o);
        }

        @Override
        public void removeAttribute(String name) {

        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public Enumeration getLocales() {
            return null;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path) {
            return null;
        }

        @Override
        public String getRealPath(String path) {
            return null;
        }

        @Override
        public int getRemotePort() {
            return 0;
        }

        @Override
        public String getLocalName() {
            return null;
        }

        @Override
        public String getLocalAddr() {
            return null;
        }

        @Override
        public int getLocalPort() {
            return 0;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public AsyncContext startAsync() throws IllegalStateException {
            return null;
        }

        @Override
        public AsyncContext startAsync(ServletRequest servletRequest,
                ServletResponse servletResponse) throws IllegalStateException {
            return null;
        }

        @Override
        public boolean isAsyncStarted() {
            return false;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public AsyncContext getAsyncContext() {
            return null;
        }

        @Override
        public DispatcherType getDispatcherType() {
            return null;
        }
    }
}
