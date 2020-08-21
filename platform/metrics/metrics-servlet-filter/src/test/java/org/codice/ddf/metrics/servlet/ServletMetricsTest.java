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
package org.codice.ddf.metrics.servlet;

import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.http.HttpFilterChain;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ServletMetricsTest {

  public static final String DEFAULT_METHOD = "GET";

  public static final int DEFAULT_STATUS = 200;

  public static final String LATENCY = "ddf.platform.http.latency";

  public static final Iterable<Tag> DEFAULT_TAGS = getTags(DEFAULT_METHOD, DEFAULT_STATUS);

  @Mock private HttpServletRequest mockRequest;

  @Mock private HttpServletResponse mockResponse;

  @Mock private HttpFilterChain mockFilterChain;

  private final ServletMetrics underTest = new ServletMetrics();

  private SimpleMeterRegistry meterRegistry;

  private Iterable<Tag> tags = DEFAULT_TAGS;

  @Before
  public void before() throws Exception {
    MockitoAnnotations.initMocks(this);

    meterRegistry = new SimpleMeterRegistry();
    Metrics.addRegistry(meterRegistry);

    doAnswer(
            invocationOnMock -> {
              sleep(10);
              return null;
            })
        .when(mockFilterChain)
        .doFilter(mockRequest, mockResponse);
    when(mockRequest.isAsyncStarted()).thenReturn(false);
    when(mockRequest.getMethod()).thenReturn(DEFAULT_METHOD);
    when(mockResponse.getStatus()).thenReturn(DEFAULT_STATUS);
  }

  @Test
  public void syncDoFilter() throws Exception {
    underTest.doFilter(mockRequest, mockResponse, mockFilterChain);

    assertThat(meterRegistry.summary(LATENCY, tags).count(), is(1L));
    assertThat(meterRegistry.summary(LATENCY, tags).max(), greaterThanOrEqualTo(10.0));
  }

  @Test
  public void asyncDoFilter() throws Exception {
    when(mockRequest.isAsyncStarted()).thenReturn(true);
    AsyncContext asyncContext = mock(AsyncContext.class);
    when(mockRequest.getAsyncContext()).thenReturn(asyncContext);
    AsyncEvent event = mock(AsyncEvent.class);
    when(event.getSuppliedRequest()).thenReturn(mockRequest);
    when(event.getSuppliedResponse()).thenReturn(mockResponse);

    underTest.doFilter(mockRequest, mockResponse, mockFilterChain);

    ArgumentCaptor<AsyncListener> arg = ArgumentCaptor.forClass(AsyncListener.class);
    verify(asyncContext).addListener(arg.capture());
    AsyncListener listener = arg.getValue();

    assertThat(meterRegistry.summary(LATENCY, tags).count(), is(0L));

    listener.onStartAsync(event);
    listener.onComplete(event);

    assertThat(meterRegistry.summary(LATENCY, tags).count(), is(1L));
  }

  @Test
  public void asyncDoFilterTimeout() throws Exception {
    when(mockRequest.isAsyncStarted()).thenReturn(true);
    AsyncContext asyncContext = mock(AsyncContext.class);
    when(mockRequest.getAsyncContext()).thenReturn(asyncContext);
    AsyncEvent event = mock(AsyncEvent.class);
    when(event.getSuppliedRequest()).thenReturn(mockRequest);
    when(event.getSuppliedResponse()).thenReturn(mockResponse);

    underTest.doFilter(mockRequest, mockResponse, mockFilterChain);

    ArgumentCaptor<AsyncListener> arg = ArgumentCaptor.forClass(AsyncListener.class);
    verify(asyncContext).addListener(arg.capture());
    AsyncListener listener = arg.getValue();

    listener.onStartAsync(event);
    listener.onTimeout(event);
    listener.onComplete(event);

    assertThat(meterRegistry.summary(LATENCY, getTags(DEFAULT_METHOD, 408)).count(), is(1L));
  }

  @Test
  public void asyncDoFilterError() throws Exception {
    when(mockRequest.isAsyncStarted()).thenReturn(true);
    AsyncContext asyncContext = mock(AsyncContext.class);
    when(mockRequest.getAsyncContext()).thenReturn(asyncContext);
    AsyncEvent event = mock(AsyncEvent.class);
    when(event.getSuppliedRequest()).thenReturn(mockRequest);
    when(event.getSuppliedResponse()).thenReturn(mockResponse);

    underTest.doFilter(mockRequest, mockResponse, mockFilterChain);

    ArgumentCaptor<AsyncListener> arg = ArgumentCaptor.forClass(AsyncListener.class);
    verify(asyncContext).addListener(arg.capture());
    AsyncListener listener = arg.getValue();

    listener.onStartAsync(event);
    listener.onError(event);
    listener.onComplete(event);

    assertThat(meterRegistry.summary(LATENCY, getTags(DEFAULT_METHOD, 500)).count(), is(1L));
  }

  @Test(expected = Exception.class)
  public void syncException() throws Exception {
    doThrow(Exception.class).when(mockFilterChain).doFilter(mockRequest, mockResponse);
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockResponse.getStatus()).thenReturn(500);

    try {
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain);
    } finally {
      tags = getTags("POST", 500);
      assertThat(meterRegistry.summary(LATENCY, tags).count(), is(1L));
    }
  }

  @Test(expected = Exception.class)
  public void syncExceptionWithConflictingStatusCode() throws Exception {
    doThrow(Exception.class).when(mockFilterChain).doFilter(mockRequest, mockResponse);
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockResponse.getStatus()).thenReturn(200);

    try {
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain);
    } finally {
      tags = getTags("POST", 500);
      assertThat(meterRegistry.summary(LATENCY, tags).count(), is(1L));

      tags = getTags("POST", 200);
      assertThat(meterRegistry.summary(LATENCY, tags).count(), is(0L));
    }
  }

  private static Iterable<Tag> getTags(String method, int status) {
    return Tags.of("method", method, "status", Integer.toString(status));
  }
}
