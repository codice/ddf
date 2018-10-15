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
package org.codice.ddf.pax.web.jetty;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import java.io.IOException;
import javax.security.auth.Subject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.Test;

public class SecurityJavaSubjectFilterTest {

  /** Tests that the filter is registered when the injectFilter method is called. */
  @Test
  public void testSecurityJavaSubjectNotAvailable() throws IOException, ServletException {
    // given
    ServletRequest servletRequest = mock(ServletRequest.class);
    ServletResponse servletResponse = mock(ServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    SecurityJavaSubjectFilter securityJavaSubjectFilter = new SecurityJavaSubjectFilter();
    // when
    when(servletRequest.getAttribute(SecurityConstants.SECURITY_JAVA_SUBJECT)).thenReturn(null);
    securityJavaSubjectFilter.doFilter(servletRequest, servletResponse, filterChain);
    // verify
    verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
  }

  @Test
  public void testSecurityJavaSubjectISAvailable() throws IOException, ServletException {
    // given
    ServletRequest servletRequest = mock(ServletRequest.class);
    ServletResponse servletResponse = mock(ServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    Subject subject = new Subject();
    SecurityJavaSubjectFilter securityJavaSubjectFilter = new SecurityJavaSubjectFilter();
    // when
    when(servletRequest.getAttribute(SecurityConstants.SECURITY_JAVA_SUBJECT)).thenReturn(subject);
    securityJavaSubjectFilter.doFilter(servletRequest, servletResponse, filterChain);
    // verify
    verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    // cannot really verify static Subject.doAs() was called with subject...
  }
}
