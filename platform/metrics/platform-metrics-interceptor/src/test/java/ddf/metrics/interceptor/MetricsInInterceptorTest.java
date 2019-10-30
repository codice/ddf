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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** @author willisod */
public class MetricsInInterceptorTest {

  private Exchange exchange;

  private LatencyTimeRecorder latencyTimeRecorder;

  private Message message;

  private MetricsInInterceptor metricsInInterceptor;

  @Before
  public void init() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    MeterRegistryService meterRegistryService = mock(MeterRegistryService.class);
    when(meterRegistryService.getMeterRegistry()).thenReturn(meterRegistry);
    metricsInInterceptor = new MetricsInInterceptor(meterRegistryService);

    message = mock(Message.class);
    exchange = spy(ExchangeImpl.class);
    latencyTimeRecorder = mock(LatencyTimeRecorder.class);
    when(message.getExchange()).thenReturn(exchange);
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsInInterceptor#MetricsInInterceptor(MeterRegistryService)} .
   */
  @Test
  public void testMetricsInInterceptor() {
    assertEquals(Phase.RECEIVE, metricsInInterceptor.getPhase());
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsInInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithoutExchange() {
    Message messageWithoutExchange = mock(Message.class);
    when(messageWithoutExchange.getExchange()).thenReturn(null);
    metricsInInterceptor.handleMessage(messageWithoutExchange);
    verify(messageWithoutExchange, never()).get(Message.REQUESTOR_ROLE);
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsInInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithTwoWayClientMessageWithLatencyTimeRecorder() {
    when(message.get(Message.REQUESTOR_ROLE)).thenReturn(true);
    when(exchange.get(LatencyTimeRecorder.class)).thenReturn(latencyTimeRecorder);
    metricsInInterceptor.handleMessage(message);
    verify(latencyTimeRecorder, times(1)).endHandling();
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsInInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithTwoWayClientMessageWithoutLatencyTimeRecorder() {
    when(message.get(Message.REQUESTOR_ROLE)).thenReturn(true);
    metricsInInterceptor.handleMessage(message);
    assertNull(exchange.get(LatencyTimeRecorder.class));
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsInInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithOneWayClientMessage() {
    when(message.get(Message.REQUESTOR_ROLE)).thenReturn(true);
    exchange.setOneWay(true);
    metricsInInterceptor.handleMessage(message);
    assertNull(exchange.get(LatencyTimeRecorder.class));
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsInInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithNonClientMessageWithLatencyTimeRecorder() {
    when(message.get(Message.REQUESTOR_ROLE)).thenReturn(false);
    when(exchange.get(LatencyTimeRecorder.class)).thenReturn(latencyTimeRecorder);
    metricsInInterceptor.handleMessage(message);
    verify(latencyTimeRecorder, times(1)).beginHandling();
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsInInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithNonClientMessageWithoutLatencyTimeRecorder() {
    when(message.get(Message.REQUESTOR_ROLE)).thenReturn(false);
    metricsInInterceptor.handleMessage(message);
    assertThat(exchange.get(LatencyTimeRecorder.class), instanceOf(LatencyTimeRecorder.class));
  }
}
