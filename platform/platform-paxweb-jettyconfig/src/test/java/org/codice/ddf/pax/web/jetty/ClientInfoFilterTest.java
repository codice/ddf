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

import static org.codice.ddf.security.util.ThreadContextProperties.CLIENT_INFO_KEY;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Ensure our client info properties get set during the lifetime of the filter, and are cleaned up
 * after the fact.
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientInfoFilterTest {
  @Mock private HttpServletRequest mockRequest;

  @Mock private HttpServletResponse mockResponse;

  @Mock private ProxyHttpFilterChain mockFilterChain;

  private ClientInfoFilter clientInfoFilter;

  @Before
  public void setup() throws Exception {
    clientInfoFilter = new ClientInfoFilter();
  }

  @Test
  public void testClientInfoPresentInMap() throws Exception {
    doAnswer(invocationOnMock -> assertThatMapIsNotNull())
        .when(mockFilterChain)
        .doFilter(mockRequest, mockResponse);
    clientInfoFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
    assertThatMapIsNull();
  }

  @Test(expected = RuntimeException.class)
  public void testClientInfoCleansUpOnException() throws Exception {
    doThrow(RuntimeException.class).when(mockFilterChain).doFilter(mockRequest, mockResponse);
    try {
      clientInfoFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
    } finally {
      assertThatMapIsNull();
    }
  }

  private Object assertThatMapIsNotNull() throws Exception {
    assertThat(ThreadContext.get(CLIENT_INFO_KEY), notNullValue());
    return null;
  }

  private void assertThatMapIsNull() throws Exception {
    assertThat(ThreadContext.get(CLIENT_INFO_KEY), nullValue());
  }
}
