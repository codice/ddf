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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.x500.X500Principal;
import javax.servlet.FilterChain;
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
