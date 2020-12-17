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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.audit.SecurityLogger;
import ddf.security.common.PrincipalHolder;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.security.token.storage.api.TokenStorage;
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
    localLogoutServlet =
        new MockLocalLogoutServlet(mock(TokenStorage.class), "/logout", mock(SecurityLogger.class));
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
  }

  @Test
  public void testLocalLogout() throws Exception {
    PrincipalHolder principalHolderMock = mock(PrincipalHolder.class);
    when(principalHolderMock.getPrincipals()).thenReturn(mock(PrincipalCollection.class));
    when(httpSession.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY))
        .thenReturn(principalHolderMock);

    localLogoutServlet.doGet(request, response);

    verify(httpSession).invalidate();
    verify(response).sendRedirect("https://localhost:8993/logout?mustCloseBrowser=true");
    verify(principalHolderMock).remove();
  }

  @Test()
  public void testNullSubject() throws Exception {
    ThreadContext.bind((Subject) null);

    // Used for detecting basic auth
    when(request.getHeaders(anyString())).thenReturn(new LogoutServletEnumeration());

    // used for detecting pki
    when(request.getAttribute("javax.servlet.request.X509Certificate"))
        .thenReturn(new X509Certificate[] {mock(X509Certificate.class)});

    PrincipalHolder principalHolderMock = mock(PrincipalHolder.class);
    when(httpSession.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY))
        .thenReturn(principalHolderMock);

    localLogoutServlet.doGet(request, response);

    verify(httpSession).invalidate();
  }

  @Test
  public void testNullSystemProperty() throws Exception {
    // Used for detecting basic auth
    when(request.getHeaders(anyString())).thenReturn(new LogoutServletEnumeration());

    // used for detecting pki
    when(request.getAttribute("javax.servlet.request.X509Certificate"))
        .thenReturn(new X509Certificate[] {mock(X509Certificate.class)});

    PrincipalHolder principalHolderMock = mock(PrincipalHolder.class);
    when(httpSession.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY))
        .thenReturn(principalHolderMock);

    localLogoutServlet.doGet(request, response);

    verify(httpSession).invalidate();
  }

  // Since the servlet context is only set properly during startup, this mocks out the servlet
  // context to avoid an exception during redirect
  private class MockLocalLogoutServlet extends LocalLogoutServlet {

    public MockLocalLogoutServlet(
        final TokenStorage tokenStorage,
        final String redirectUri,
        final SecurityLogger securityLogger) {
      super(tokenStorage, redirectUri, securityLogger);
    }

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
