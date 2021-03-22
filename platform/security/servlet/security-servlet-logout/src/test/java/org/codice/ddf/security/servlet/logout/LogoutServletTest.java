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
package org.codice.ddf.security.servlet.logout;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.common.SecurityTokenHolder;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Test;

public class LogoutServletTest {
  private LocalLogoutServlet localLogoutServlet;

  private HttpServletRequest request;

  private HttpServletResponse response;

  private HttpSession httpSession;

  private PrintWriter printWriter;

  @Before
  public void testsetup() throws Exception {
    localLogoutServlet = new MockLocalLogoutServlet();
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    printWriter = mock(PrintWriter.class);

    httpSession = mock(HttpSession.class);
    when(request.getSession()).thenReturn(httpSession);
    when(request.getSession().getId()).thenReturn("id");
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://foo.bar"));
    when(response.getWriter()).thenReturn(printWriter);

    Subject subject = mock(Subject.class);
    when(subject.hasRole(anyString())).thenReturn(false);
    ThreadContext.bind(subject);

    System.setProperty("security.audit.roles", "none");
  }

  @Test
  public void testLocalLogoutBasicAuth() {
    // Used for detecting basic auth
    when(request.getHeaders(anyString())).thenReturn(new LogoutServletEnumeration());

    // used for detecting pki
    when(request.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(null);

    SecurityTokenHolder securityTokenHolder = mock(SecurityTokenHolder.class);
    when(httpSession.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY))
        .thenReturn(securityTokenHolder);
    try {
      localLogoutServlet.doGet(request, response);
    } catch (ServletException e) {
      fail(e.getMessage());
    }
    verify(httpSession).invalidate();
    verify(printWriter).write("{ \"mustCloseBrowser\": true }");
  }

  @Test
  public void testLocalLogoutPkiAuth() {
    // Used for detecting basic auth
    when(request.getHeaders(anyString()))
        .thenReturn(Collections.enumeration(Collections.emptyList()));

    // used for detecting pki
    when(request.getAttribute("javax.servlet.request.X509Certificate"))
        .thenReturn(new X509Certificate[] {mock(X509Certificate.class)});

    SecurityTokenHolder securityTokenHolder = mock(SecurityTokenHolder.class);
    when(httpSession.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY))
        .thenReturn(securityTokenHolder);
    try {
      localLogoutServlet.doGet(request, response);
    } catch (ServletException e) {
      fail(e.getMessage());
    }
    verify(httpSession).invalidate();
    verify(printWriter).write("{ \"mustCloseBrowser\": true }");
  }

  @Test
  public void testLocalLogoutNotBasicOrPki() {
    // Used for detecting basic auth
    when(request.getHeaders(anyString()))
        .thenReturn(Collections.enumeration(Collections.emptyList()));

    // used for detecting pki
    when(request.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(null);

    SecurityTokenHolder securityTokenHolder = mock(SecurityTokenHolder.class);
    when(httpSession.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY))
        .thenReturn(securityTokenHolder);
    try {
      localLogoutServlet.doGet(request, response);
    } catch (ServletException e) {
      fail(e.getMessage());
    }
    verify(httpSession).invalidate();
    verify(printWriter).write("{ \"mustCloseBrowser\": false }");
  }

  @Test()
  public void testNullSubject() throws Exception {
    ThreadContext.bind((Subject) null);

    // Used for detecting basic auth
    when(request.getHeaders(anyString())).thenReturn(new LogoutServletEnumeration());

    // used for detecting pki
    when(request.getAttribute("javax.servlet.request.X509Certificate"))
        .thenReturn(new X509Certificate[] {mock(X509Certificate.class)});

    SecurityTokenHolder securityTokenHolder = mock(SecurityTokenHolder.class);
    when(httpSession.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY))
        .thenReturn(securityTokenHolder);
    try {
      localLogoutServlet.doGet(request, response);
    } catch (ServletException e) {
      fail(e.getMessage());
    }
    verify(httpSession).invalidate();
  }

  @Test
  public void testNullSystemProperty() throws Exception {
    System.clearProperty("security.audit.roles");

    // Used for detecting basic auth
    when(request.getHeaders(anyString())).thenReturn(new LogoutServletEnumeration());

    // used for detecting pki
    when(request.getAttribute("javax.servlet.request.X509Certificate"))
        .thenReturn(new X509Certificate[] {mock(X509Certificate.class)});

    SecurityTokenHolder securityTokenHolder = mock(SecurityTokenHolder.class);
    when(httpSession.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY))
        .thenReturn(securityTokenHolder);
    try {
      localLogoutServlet.doGet(request, response);
    } catch (ServletException e) {
      fail(e.getMessage());
    }
    verify(httpSession).invalidate();
  }

  // Since the servlet context is only set properly during startup, this mocks out the servlet
  // context to avoid an exception during redirect
  private class MockLocalLogoutServlet extends LocalLogoutServlet {
    @Override
    public ServletContext getServletContext() {
      ServletContext servletContext = mock(ServletContext.class);
      when(servletContext.getContext(any(String.class))).thenReturn(servletContext);
      when(servletContext.getRequestDispatcher(any(String.class)))
          .thenReturn(mock(RequestDispatcher.class));
      return servletContext;
    }
  }

  private class LogoutServletEnumeration implements Enumeration<String> {
    @Override
    public boolean hasMoreElements() {
      return true;
    }

    @Override
    public String nextElement() {
      return "Basic";
    }
  }
}
