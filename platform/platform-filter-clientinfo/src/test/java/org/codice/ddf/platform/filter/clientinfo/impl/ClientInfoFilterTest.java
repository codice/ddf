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
package org.codice.ddf.platform.filter.clientinfo.impl;

import static org.codice.ddf.platform.filter.clientinfo.ClientInfoFilter.CLIENT_INFO_KEY;
import static org.codice.ddf.platform.filter.clientinfo.ClientInfoFilter.SERVLET_CONTEXT_PATH;
import static org.codice.ddf.platform.filter.clientinfo.ClientInfoFilter.SERVLET_REMOTE_ADDR;
import static org.codice.ddf.platform.filter.clientinfo.ClientInfoFilter.SERVLET_REMOTE_HOST;
import static org.codice.ddf.platform.filter.clientinfo.ClientInfoFilter.SERVLET_SCHEME;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.platform.filter.clientinfo.ClientInfoFilter;
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

  @Mock private ServletContext mockServletContext;

  @Mock private ServletRequest mockServletRequest;

  @Mock private ServletResponse mockServletResponse;

  @Mock private FilterChain mockFilterChain;

  private ClientInfoFilter clientInfoFilter;

  @Before
  public void setup() throws Exception {
    when(mockServletRequest.getRemoteAddr()).thenReturn(MOCK_REMOTE_ADDRESS);
    when(mockServletRequest.getRemoteHost()).thenReturn(MOCK_REMOTE_HOST);
    when(mockServletRequest.getScheme()).thenReturn(MOCK_SCHEME);
    when(mockServletRequest.getServletContext()).thenReturn(mockServletContext);
    when(mockServletContext.getContextPath()).thenReturn(MOCK_CONTEXT_PATH);

    clientInfoFilter = new ClientInfoFilter();
  }

  @Test
  public void testClientInfoPresentInMap() throws Exception {
    doAnswer(invocationOnMock -> assertThatMapIsAccurate())
        .when(mockFilterChain)
        .doFilter(mockServletRequest, mockServletResponse);
    clientInfoFilter.doFilter(mockServletRequest, mockServletResponse, mockFilterChain);
    assertThatMapIsNull();
  }

  @Test(expected = RuntimeException.class)
  public void testClientInfoCleansUpOnException() throws Exception {
    doThrow(RuntimeException.class)
        .when(mockFilterChain)
        .doFilter(mockServletRequest, mockServletResponse);
    try {
      clientInfoFilter.doFilter(mockServletRequest, mockServletResponse, mockFilterChain);
    } finally {
      assertThatMapIsNull();
    }
  }

  private Object assertThatMapIsAccurate() throws Exception {
    Map<String, String> clientInfoMap = (Map<String, String>) ThreadContext.get(CLIENT_INFO_KEY);
    assertThat(clientInfoMap, notNullValue());
    assertThat(clientInfoMap.get(SERVLET_REMOTE_ADDR), is(MOCK_REMOTE_ADDRESS));
    assertThat(clientInfoMap.get(SERVLET_REMOTE_HOST), is(MOCK_REMOTE_HOST));
    assertThat(clientInfoMap.get(SERVLET_SCHEME), is(MOCK_SCHEME));
    assertThat(clientInfoMap.get(SERVLET_CONTEXT_PATH), is(MOCK_CONTEXT_PATH));
    return null;
  }

  private void assertThatMapIsNull() throws Exception {
    Map<String, String> clientInfoMap = (Map<String, String>) ThreadContext.get(CLIENT_INFO_KEY);
    assertThat(clientInfoMap, nullValue());
  }
}
