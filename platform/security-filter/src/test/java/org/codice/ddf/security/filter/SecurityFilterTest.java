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
package org.codice.ddf.security.filter;

import static org.codice.ddf.security.filter.SecurityFilter.AUTHORIZATION_HEADER;
import static org.codice.ddf.security.filter.SecurityFilter.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE;
import static org.codice.ddf.security.filter.SecurityFilter.JAVA_SUBJECT;
import static org.codice.ddf.security.filter.SecurityFilter.KARAF_SUBJECT_RUN_AS;
import static org.codice.ddf.security.filter.SecurityFilter.WWW_AUTHENTICATE_HEADER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.x500.X500Principal;
import javax.servlet.FilterChain;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SecurityFilterTest {

  private static final String ADMIN_BASIC_AUTH_HEADER_VALUE = "Basic YWRtaW46YWRtaW4=";

  SecurityFilter filter;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  @Mock private HttpSession session;

  @Mock private LoginContextFactory loginContextFactory;

  @Mock private LoginContext loginContext;

  @Captor ArgumentCaptor<Subject> subjectCaptor;

  private AutoCloseable closeable;

  @Before
  public void setUp() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);

    filter = new SecurityFilter();
    filter.setLoginContextFactory(loginContextFactory);
    when(loginContextFactory.create(subjectCaptor.capture(), any(), any()))
        .thenReturn(loginContext);
    when(request.getSession(false)).thenReturn(null);
    when(request.getSession(true)).thenReturn(session);
    when(request.getRequestURI()).thenReturn("/services/catalog/query");
  }

  @After
  public void tearDown() throws Exception {
    closeable.close();
  }

  @Test
  public void missingAuth() throws Exception {
    filter.doFilter(request, response, filterChain);

    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  public void disabledAuth() throws Exception {
    try {
      System.setProperty("org.codice.ddf.http.auth.basic", "false");
      System.setProperty("org.codice.ddf.http.auth.cert", "false");
      filter = new SecurityFilter();

      filter.doFilter(request, response, filterChain);

      verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      verify(filterChain).doFilter(request, response);
    } finally {
      System.clearProperty("org.codice.ddf.http.auth.basic");
      System.clearProperty("org.codice.ddf.http.auth.cert");
    }
  }

  @Test
  public void disabledBasic() throws Exception {
    try {
      ServletOutputStream outputStream = mock(ServletOutputStream.class);
      when(response.getOutputStream()).thenReturn(outputStream);

      System.setProperty("org.codice.ddf.http.auth.basic", "false");
      filter = new SecurityFilter();

      filter.doFilter(request, response, filterChain);

      verify(response, never()).setHeader(eq(WWW_AUTHENTICATE_HEADER), anyString());
      verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      verify(filterChain, never()).doFilter(request, response);
      verify(outputStream).println(anyString());
    } finally {
      System.clearProperty("org.codice.ddf.http.auth.basic");
    }
  }

  @Test
  public void activeSessionWithSubject() throws Exception {
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(eq(KARAF_SUBJECT_RUN_AS))).thenReturn(new Subject());

    filter.doFilter(request, response, filterChain);

    verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void verifyHeaders() throws Exception {
    filter.doFilter(request, response, filterChain);

    verify(response).setHeader(eq("Cache-Control"), anyString());
    verify(response).setHeader(eq("Strict-Transport-Security"), anyString());
    verify(response).setHeader(eq("Content-Security-Policy"), anyString());
    verify(response).setHeader(eq("X-FRAME-OPTIONS"), anyString());
    verify(response).setHeader(eq("X-XSS-Protection"), anyString());
    verify(response).setHeader(eq("Referrer-Policy"), anyString());
    verify(response).setHeader(eq("X-Content-Type-Options"), anyString());
  }

  @Test
  public void basicAuth() throws Exception {
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(ADMIN_BASIC_AUTH_HEADER_VALUE);

    filter.doFilter(request, response, filterChain);

    verify(loginContextFactory).create(subjectCaptor.getValue(), "admin", "admin");
    verify(request).setAttribute(JAVA_SUBJECT, subjectCaptor.getValue());
    verify(session).setAttribute(KARAF_SUBJECT_RUN_AS, subjectCaptor.getValue());
  }

  @Test
  public void certAuth() throws Exception {
    X509Certificate cert = mock(X509Certificate.class);
    PublicKey key = mock(PublicKey.class);
    X500Principal principal = new X500Principal("CN=localhost");

    when(cert.getSubjectX500Principal()).thenReturn(principal);
    when(cert.getPublicKey()).thenReturn(key);

    when(request.getAttribute(JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE))
        .thenReturn(new X509Certificate[] {cert});

    filter.doFilter(request, response, filterChain);
    verify(loginContextFactory).create(subjectCaptor.getValue(), "CN=localhost", key);
    verify(request).setAttribute(JAVA_SUBJECT, subjectCaptor.getValue());
    verify(session).setAttribute(KARAF_SUBJECT_RUN_AS, subjectCaptor.getValue());
  }
}
