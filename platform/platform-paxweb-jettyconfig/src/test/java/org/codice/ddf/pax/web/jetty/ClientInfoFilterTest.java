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

import static org.codice.ddf.pax.web.jetty.ClientInfoFilter.CLIENT_INFO_KEY;
import static org.codice.ddf.pax.web.jetty.ClientInfoFilter.SERVLET_CONTEXT_PATH;
import static org.codice.ddf.pax.web.jetty.ClientInfoFilter.SERVLET_REMOTE_ADDR;
import static org.codice.ddf.pax.web.jetty.ClientInfoFilter.SERVLET_REMOTE_HOST;
import static org.codice.ddf.pax.web.jetty.ClientInfoFilter.SERVLET_SCHEME;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Ensure our client info properties get set during the life time of the filter, and are cleaned up
 * after the fact.
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientInfoFilterTest {
  private static final String MOCK_REMOTE_ADDRESS = "0.0.0.0";

  private static final String MOCK_REMOTE_HOST = "localhost";

  private static final String MOCK_SCHEME = "http";

  private static final String MOCK_CONTEXT_PATH = "/example/path";

  private static final String MOCK_FORWARDED_HEADER = "for=192.0.2.60;proto=http;by=203.0.113.43";

  private static final String MOCK_X_FORWARDED_FOR = "127.0.0.1";

  private static final String MOCK_X_FORWARDED_HOST_HEADER = "www.example.com";

  private static final String MOCK_X_FORWARDED_PORT_HEADER = "1234";

  private static final String MOCK_X_FORWARDED_PROTO_HEADER = "https";

  private static final String MOCK_X_FORWARDED_PREFIX_HEADER = "/api";

  private static final String MOCK_X_FORWARDED_SSL_HEADER = "on";

  @Mock private ServletContext mockServletContext;

  @Mock private ServletResponse mockServletResponse;

  @Mock private FilterChain mockFilterChain;

  private ClientInfoFilter clientInfoFilter;

  @Before
  public void setup() throws Exception {
    when(mockServletContext.getContextPath()).thenReturn(MOCK_CONTEXT_PATH);
    clientInfoFilter = new ClientInfoFilter();
  }

  @Test
  public void testClientInfoPresentInMap() throws Exception {
    HttpServletRequest httpServletRequest = mockFullClientInfo();
    doAnswer(invocationOnMock -> assertThatMapIsAccurate())
        .when(mockFilterChain)
        .doFilter(httpServletRequest, mockServletResponse);
    clientInfoFilter.doFilter(httpServletRequest, mockServletResponse, mockFilterChain);
    assertThatMapIsNull();
  }

  @Test(expected = RuntimeException.class)
  public void testClientInfoCleansUpOnException() throws Exception {
    HttpServletRequest httpServletRequest = mockFullClientInfo();
    doThrow(RuntimeException.class)
        .when(mockFilterChain)
        .doFilter(httpServletRequest, mockServletResponse);
    try {
      clientInfoFilter.doFilter(httpServletRequest, mockServletResponse, mockFilterChain);
    } finally {
      assertThatMapIsNull();
    }
  }

  @Test
  public void testForwardedHeadersAreOptional() throws Exception {
    HttpServletRequest httpServletRequest = mockClientInfoMissingForwardedHeaders();
    doAnswer(invocationOnMock -> assertMapWithoutForwardedHeaders())
        .when(mockFilterChain)
        .doFilter(httpServletRequest, mockServletResponse);
    clientInfoFilter.doFilter(httpServletRequest, mockServletResponse, mockFilterChain);
    assertThatMapIsNull();
  }

  private Object assertThatMapIsAccurate() throws Exception {
    Map<String, String> clientInfoMap = (Map<String, String>) ThreadContext.get(CLIENT_INFO_KEY);
    assertThat(clientInfoMap, notNullValue());
    assertThat(clientInfoMap.get(SERVLET_REMOTE_ADDR), is(MOCK_REMOTE_ADDRESS));
    assertThat(clientInfoMap.get(SERVLET_REMOTE_HOST), is(MOCK_REMOTE_HOST));
    assertThat(clientInfoMap.get(SERVLET_SCHEME), is(MOCK_SCHEME));
    assertThat(clientInfoMap.get(SERVLET_CONTEXT_PATH), is(MOCK_CONTEXT_PATH));
    assertThat(clientInfoMap.get("Forwarded"), is(MOCK_FORWARDED_HEADER));
    assertThat(clientInfoMap.get("X-Forwarded-For"), is(MOCK_X_FORWARDED_FOR));
    assertThat(clientInfoMap.get("X-Forwarded-Host"), is(MOCK_X_FORWARDED_HOST_HEADER));
    assertThat(clientInfoMap.get("X-Forwarded-Port"), is(MOCK_X_FORWARDED_PORT_HEADER));
    assertThat(clientInfoMap.get("X-Forwarded-Proto"), is(MOCK_X_FORWARDED_PROTO_HEADER));
    assertThat(clientInfoMap.get("X-Forwarded-Prefix"), is(MOCK_X_FORWARDED_PREFIX_HEADER));
    assertThat(clientInfoMap.get("X-Forwarded-Ssl"), is(MOCK_X_FORWARDED_SSL_HEADER));
    assertThat(clientInfoMap.size(), is(11));
    return null;
  }

  private Object assertMapWithoutForwardedHeaders() throws Exception {
    Map<String, String> clientInfoMap = (Map<String, String>) ThreadContext.get(CLIENT_INFO_KEY);
    assertThat(clientInfoMap, notNullValue());
    assertThat(clientInfoMap.get(SERVLET_REMOTE_ADDR), is(MOCK_REMOTE_ADDRESS));
    assertThat(clientInfoMap.get(SERVLET_REMOTE_HOST), is(MOCK_REMOTE_HOST));
    assertThat(clientInfoMap.get(SERVLET_SCHEME), is(MOCK_SCHEME));
    assertThat(clientInfoMap.get(SERVLET_CONTEXT_PATH), is(MOCK_CONTEXT_PATH));
    assertThat(clientInfoMap.size(), is(4));
    return null;
  }

  private void assertThatMapIsNull() throws Exception {
    Map<String, String> clientInfoMap = (Map<String, String>) ThreadContext.get(CLIENT_INFO_KEY);
    assertThat(clientInfoMap, nullValue());
  }

  private HttpServletRequest mockFullClientInfo() {
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getRemoteAddr()).thenReturn(MOCK_REMOTE_ADDRESS);
    when(httpServletRequest.getRemoteHost()).thenReturn(MOCK_REMOTE_HOST);
    when(httpServletRequest.getScheme()).thenReturn(MOCK_SCHEME);
    when(httpServletRequest.getServletContext()).thenReturn(mockServletContext);
    when(httpServletRequest.getHeader("Forwarded")).thenReturn(MOCK_FORWARDED_HEADER);
    when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(MOCK_X_FORWARDED_FOR);
    when(httpServletRequest.getHeader("X-Forwarded-Host")).thenReturn(MOCK_X_FORWARDED_HOST_HEADER);
    when(httpServletRequest.getHeader("X-Forwarded-Port")).thenReturn(MOCK_X_FORWARDED_PORT_HEADER);
    when(httpServletRequest.getHeader("X-Forwarded-Proto"))
        .thenReturn(MOCK_X_FORWARDED_PROTO_HEADER);
    when(httpServletRequest.getHeader("X-Forwarded-Prefix"))
        .thenReturn(MOCK_X_FORWARDED_PREFIX_HEADER);
    when(httpServletRequest.getHeader("X-Forwarded-Ssl")).thenReturn(MOCK_X_FORWARDED_SSL_HEADER);
    return httpServletRequest;
  }

  private HttpServletRequest mockClientInfoMissingForwardedHeaders() {
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getRemoteAddr()).thenReturn(MOCK_REMOTE_ADDRESS);
    when(httpServletRequest.getRemoteHost()).thenReturn(MOCK_REMOTE_HOST);
    when(httpServletRequest.getScheme()).thenReturn(MOCK_SCHEME);
    when(httpServletRequest.getServletContext()).thenReturn(mockServletContext);
    return httpServletRequest;
  }
}
