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
package ddf.metrics.interceptor;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;
import org.junit.Before;
import org.junit.Test;

/** @author willisod */
public class MetricsOutInterceptorTest {

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#MetricsOutInterceptor(MeterRegistryService)} .
   */
  private Exchange exchange;

  private LatencyTimeRecorder latencyTimeRecorder;

  private Message message;

  private MetricsOutInterceptor metricsOutInterceptor;

  @Before
  public void init() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    MeterRegistryService meterRegistryService = mock(MeterRegistryService.class);
    when(meterRegistryService.getMeterRegistry()).thenReturn(meterRegistry);
    metricsOutInterceptor = new MetricsOutInterceptor(meterRegistryService);

    message = mock(Message.class);
    exchange = spy(ExchangeImpl.class);
    latencyTimeRecorder = mock(LatencyTimeRecorder.class);
    when(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn(false);
    when(message.getExchange()).thenReturn(exchange);
  }

  @Test
  public void testMetricsOutInterceptor() {
    assertEquals(Phase.SEND, metricsOutInterceptor.getPhase());
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithoutExchange() {
    Message messageWithoutExchange = mock(Message.class);
    when(messageWithoutExchange.getExchange()).thenReturn(null);
    metricsOutInterceptor.handleMessage(messageWithoutExchange);
    verify(messageWithoutExchange, never()).get(Message.PARTIAL_RESPONSE_MESSAGE);
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithPartialResponseMessage() {
    Message partialMessage = mock(Message.class);
    when(partialMessage.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn(true);
    metricsOutInterceptor.handleMessage(partialMessage);
    verify(partialMessage, never()).get(Message.REQUESTOR_ROLE);
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithTwoWayClientMessageWithLatencyTimeRecorder() {
    when(message.get(Message.REQUESTOR_ROLE)).thenReturn(true);
    when(exchange.get(LatencyTimeRecorder.class)).thenReturn(latencyTimeRecorder);
    metricsOutInterceptor.handleMessage(message);
    verify(latencyTimeRecorder, times(1)).beginHandling();
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithTwoWayClientMessageWithoutLatencyTimeRecorder() {
    when(message.get(Message.REQUESTOR_ROLE)).thenReturn(true);
    metricsOutInterceptor.handleMessage(message);
    assertThat(exchange.get(LatencyTimeRecorder.class), instanceOf(LatencyTimeRecorder.class));
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithOneWayClientMessage() {
    when(message.get(Message.REQUESTOR_ROLE)).thenReturn(true);
    exchange.setOneWay(true);
    InterceptorChain interceptorChain = mock(InterceptorChain.class);
    when(message.getInterceptorChain()).thenReturn(interceptorChain);
    metricsOutInterceptor.handleMessage(message);
    verify(message, times(1)).getInterceptorChain();
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithNonClientMessageWithLatencyTimeRecorder() {
    when(message.get(Message.REQUESTOR_ROLE)).thenReturn(false);
    when(exchange.get(LatencyTimeRecorder.class)).thenReturn(latencyTimeRecorder);
    metricsOutInterceptor.handleMessage(message);
    verify(latencyTimeRecorder, times(1)).endHandling();
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithNonClientMessageWithoutLatencyTimeRecorder() {
    when(message.get(Message.REQUESTOR_ROLE)).thenReturn(false);
    metricsOutInterceptor.handleMessage(message);
    assertNull(exchange.get(LatencyTimeRecorder.class));
  }
}
