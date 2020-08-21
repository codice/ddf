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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import java.io.IOException;
import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SecurityJavaSubjectFilterTest {

  @Mock private HttpServletRequest mockRequest;

  @Mock private HttpServletResponse mockResponse;

  @Mock private ProxyHttpFilterChain mockFilterChain;

  /** Tests that the filter is registered when the injectFilter method is called. */
  @Test
  public void testSecurityJavaSubjectNotAvailable() throws IOException, ServletException {
    // given
    SecurityJavaSubjectFilter securityJavaSubjectFilter = new SecurityJavaSubjectFilter();
    // when
    when(mockRequest.getAttribute(SecurityConstants.SECURITY_JAVA_SUBJECT)).thenReturn(null);
    securityJavaSubjectFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
    // verify
    verify(mockFilterChain, times(1)).doFilter(mockRequest, mockResponse);
  }

  @Test
  public void testSecurityJavaSubjectISAvailable() throws IOException, ServletException {
    // given
    Subject subject = new Subject();
    SecurityJavaSubjectFilter securityJavaSubjectFilter = new SecurityJavaSubjectFilter();
    // when
    when(mockRequest.getAttribute(SecurityConstants.SECURITY_JAVA_SUBJECT)).thenReturn(subject);
    securityJavaSubjectFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
    // verify
    verify(mockFilterChain, times(1)).doFilter(mockRequest, mockResponse);
    // cannot really verify static Subject.doAs() was called with subject...
  }
}
