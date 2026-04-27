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

import static org.codice.ddf.pax.web.jetty.ResponseFilter.CACHE_CONTROL;
import static org.codice.ddf.pax.web.jetty.ResponseFilter.DEFAULT_CACHE_CONTROL_VALUE;
import static org.codice.ddf.pax.web.jetty.ResponseFilter.DEFAULT_CONTENT_SECURITY_POLICY;
import static org.codice.ddf.pax.web.jetty.ResponseFilter.DEFAULT_XSS_PROTECTION_VALUE;
import static org.codice.ddf.pax.web.jetty.ResponseFilter.DEFAULT_X_FRAME_OPTIONS_VALUE;
import static org.codice.ddf.pax.web.jetty.ResponseFilter.STRICT_TRANSPORT_SECURITY;
import static org.codice.ddf.pax.web.jetty.ResponseFilter.STRICT_TRANSPORT_SECURITY_VALUE;
import static org.codice.ddf.pax.web.jetty.ResponseFilter.X_CONTENT_SECURITY_POLICY;
import static org.codice.ddf.pax.web.jetty.ResponseFilter.X_FRAME_OPTIONS;
import static org.codice.ddf.pax.web.jetty.ResponseFilter.X_XSS_PROTECTION;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResponseFilterTest {

  @Mock private HttpServletRequest mockRequest;

  @Mock private HttpServletResponse mockResponse;

  @Mock private ProxyHttpFilterChain mockFilterChain;

  private ResponseFilter filter;

  @Before
  public void setUp() {
    filter = new ResponseFilter();
  }

  @Test
  public void defaultSecurityHeadersAreSet() throws Exception {
    when(mockRequest.getRequestURI()).thenReturn("/services/foo");

    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    verify(mockResponse).setHeader(X_XSS_PROTECTION, DEFAULT_XSS_PROTECTION_VALUE);
    verify(mockResponse).setHeader(X_FRAME_OPTIONS, DEFAULT_X_FRAME_OPTIONS_VALUE);
    verify(mockResponse).setHeader(X_CONTENT_SECURITY_POLICY, DEFAULT_CONTENT_SECURITY_POLICY);
    verify(mockResponse).setHeader(STRICT_TRANSPORT_SECURITY, STRICT_TRANSPORT_SECURITY_VALUE);
    verify(mockResponse).setHeader(CACHE_CONTROL, DEFAULT_CACHE_CONTROL_VALUE);
    verify(mockFilterChain).doFilter(mockRequest, mockResponse);
  }

  @Test
  public void cachingDisabledForIndexPaths() throws Exception {
    when(mockRequest.getRequestURI()).thenReturn("/admin/");

    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // Cache-Control gets set twice: once with the default, then overridden to no-cache.
    verify(mockResponse).setHeader(CACHE_CONTROL, DEFAULT_CACHE_CONTROL_VALUE);
    verify(mockResponse).setHeader(CACHE_CONTROL, "no-cache, no-store, must-revalidate");
    verify(mockResponse).setHeader("Pragma", "no-cache");
    verify(mockResponse).setHeader("Expires", "0");
  }

  @Test
  public void cachingDisabledForHtmlPaths() throws Exception {
    when(mockRequest.getRequestURI()).thenReturn("/index.html");

    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    verify(mockResponse).setHeader(CACHE_CONTROL, "no-cache, no-store, must-revalidate");
    verify(mockResponse).setHeader("Pragma", "no-cache");
    verify(mockResponse).setHeader("Expires", "0");
  }

  @Test
  public void cachingNotDisabledForResourcePaths() throws Exception {
    when(mockRequest.getRequestURI()).thenReturn("/static/app.js");

    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    verify(mockResponse, never()).setHeader("Pragma", "no-cache");
    verify(mockResponse, never()).setHeader("Expires", "0");
    // Default Cache-Control is still set, but the no-cache override is not.
    verify(mockResponse, times(1)).setHeader(CACHE_CONTROL, DEFAULT_CACHE_CONTROL_VALUE);
  }

  @Test
  public void customHeadersOverrideDefaults() throws Exception {
    filter.setHeaders(Arrays.asList("X-Custom=value", "X-Other=42"));
    when(mockRequest.getRequestURI()).thenReturn("/services/foo");

    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    verify(mockResponse).setHeader("X-Custom", "value");
    verify(mockResponse).setHeader("X-Other", "42");
    verify(mockResponse, never()).setHeader(X_XSS_PROTECTION, DEFAULT_XSS_PROTECTION_VALUE);
  }

  @Test
  public void malformedHeadersAreSkipped() throws Exception {
    filter.setHeaders(Arrays.asList("X-Good=value", "no-equals-sign"));
    when(mockRequest.getRequestURI()).thenReturn("/services/foo");

    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    verify(mockResponse).setHeader("X-Good", "value");
    verify(mockResponse, never()).setHeader("no-equals-sign", "");
  }
}
