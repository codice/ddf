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

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.util.ThreadContext;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Ensure the trace context gets set during the lifetime of the filter, and is cleaned up after the
 * fact.
 */
@RunWith(MockitoJUnitRunner.class)
public class TraceContextFilterTest {

  @Mock private HttpServletRequest mockRequest;

  @Mock private HttpServletResponse mockResponse;

  @Mock private ProxyHttpFilterChain mockFilterChain;

  private TraceContextFilter traceContextFilter;

  public static final String TRACE_CONTEXT_KEY = "trace-context";

  private static final String TRACE_ID = "trace-id";

  @Before
  public void setup() throws Exception {
    traceContextFilter = new TraceContextFilter();
  }

  @Test
  public void testTraceContextIsPresent() throws Exception {
    doAnswer(invocationOnMock -> assertThatMapIsAccurate())
        .when(mockFilterChain)
        .doFilter(mockRequest, mockResponse);
    traceContextFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
    assertThatMapIsNull();
  }

  @Test(expected = RuntimeException.class)
  public void testTraceContextCleansUpOnException() throws Exception {
    doThrow(RuntimeException.class).when(mockFilterChain).doFilter(mockRequest, mockResponse);
    try {
      traceContextFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
    } finally {
      assertThatMapIsNull();
    }
  }

  private Object assertThatMapIsAccurate() throws Exception {
    Map<String, String> traceContext = (Map<String, String>) ThreadContext.get(TRACE_CONTEXT_KEY);
    MatcherAssert.assertThat(traceContext, Matchers.notNullValue());
    String traceId = traceContext.get(TRACE_ID);
    MatcherAssert.assertThat(traceId, Matchers.notNullValue());
    return null;
  }

  private void assertThatMapIsNull() throws Exception {
    Map<String, String> traceContext = (Map<String, String>) ThreadContext.get(TRACE_CONTEXT_KEY);
    assertThat(traceContext, nullValue());
  }
}
