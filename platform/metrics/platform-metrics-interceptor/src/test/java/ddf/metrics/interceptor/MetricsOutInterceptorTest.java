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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;
import org.junit.Test;

/** @author willisod */
public class MetricsOutInterceptorTest {

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#MetricsOutInterceptor(MeterRegistryService)} .
   */
  @Test
  public void testMetricsOutInterceptor() {

    // Perform test
    MetricsOutInterceptor outInterceptor =
        new MetricsOutInterceptor(mock(MeterRegistryService.class));

    // Validate
    assertEquals(Phase.SEND, outInterceptor.getPhase());
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithPartialResponseMessage() {

    // Setup
    MetricsOutInterceptor outInterceptor =
        new MetricsOutInterceptor(mock(MeterRegistryService.class));

    Message mockMessage = mock(Message.class);
    Exchange ex = new ExchangeImpl();

    when(mockMessage.getExchange()).thenReturn(ex);
    when(mockMessage.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn("true");

    // Perform test
    outInterceptor.handleMessage(mockMessage);

    // validate that there is not an instance of LatencyTimeRecorder on the
    // exchange
    assertNull(ex.get(LatencyTimeRecorder.class));
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithTwoWayClientMessageWithLatencyTimeRecorder() {

    // Setup
    MetricsOutInterceptor outInterceptor =
        new MetricsOutInterceptor(mock(MeterRegistryService.class));

    Message mockMessage = mock(Message.class);
    Exchange ex = new ExchangeImpl();
    Bus mockBus = mock(Bus.class);
    LatencyTimeRecorder mockLtr = mock(LatencyTimeRecorder.class);

    ex.put(Bus.class, mockBus);
    ex.put(LatencyTimeRecorder.class, mockLtr);

    when(mockBus.getId()).thenReturn("bus_id");
    when(mockMessage.getExchange()).thenReturn(ex);
    when(mockMessage.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn("false");
    when(mockMessage.get(Message.REQUESTOR_ROLE)).thenReturn(true);

    // Perform test
    outInterceptor.handleMessage(mockMessage);

    // validate that LatencyTimeRecorder.beginHandling was called once
    verify(mockLtr, times(1)).beginHandling();
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithTwoWayClientMessageWithoutLatencyTimeRecorder() {

    // Setup
    MetricsOutInterceptor outInterceptor =
        new MetricsOutInterceptor(mock(MeterRegistryService.class));

    Message mockMessage = mock(Message.class);
    Exchange ex = new ExchangeImpl();
    Bus mockBus = mock(Bus.class);

    ex.put(Bus.class, mockBus);

    when(mockBus.getId()).thenReturn("bus_id");
    when(mockMessage.getExchange()).thenReturn(ex);
    when(mockMessage.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn("false");
    when(mockMessage.get(Message.REQUESTOR_ROLE)).thenReturn(true);

    // Perform test
    outInterceptor.handleMessage(mockMessage);

    // validate that an instance of LatencyTimeRecorder was put onto the
    // exchange
    assertThat(ex.get(LatencyTimeRecorder.class), instanceOf(LatencyTimeRecorder.class));
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithOneWayClientMessage() {

    // Setup
    MetricsOutInterceptor outInterceptor =
        new MetricsOutInterceptor(mock(MeterRegistryService.class));

    Message mockMessage = mock(Message.class);
    Exchange ex = new ExchangeImpl();
    Bus mockBus = mock(Bus.class);
    InterceptorChain mockIc = mock(InterceptorChain.class);

    ex.put(Bus.class, mockBus);
    ex.setOneWay(true);

    when(mockBus.getId()).thenReturn("bus_id");
    when(mockMessage.getExchange()).thenReturn(ex);
    when(mockMessage.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn("false");
    when(mockMessage.get(Message.REQUESTOR_ROLE)).thenReturn(true);
    when(mockMessage.getInterceptorChain()).thenReturn(mockIc);

    // Perform test
    outInterceptor.handleMessage(mockMessage);

    // validate that LatencyTimeRecorder.beginHandling was called once
    verify(mockMessage, times(1)).getInterceptorChain();
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithNonClientMessageWithNullExchange() {

    // Setup
    MetricsOutInterceptor outInterceptor =
        new MetricsOutInterceptor(mock(MeterRegistryService.class));

    Message mockMessage = mock(Message.class);

    when(mockMessage.getExchange()).thenReturn(null);
    when(mockMessage.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn("false");
    when(mockMessage.get(Message.REQUESTOR_ROLE)).thenReturn("false");

    // Perform test
    outInterceptor.handleMessage(mockMessage);

    // validate that there is not an instance of LatencyTimeRecorder on the
    // exchange
    verify(mockMessage, times(1)).getExchange();
  }

  /**
   * Test method for {@link
   * ddf.metrics.interceptor.MetricsOutInterceptor#handleMessage(org.apache.cxf.message.Message)} .
   *
   * @throws InterruptedException
   */
  @Test
  public void testHandleMessageWithNonClientMessageWithoutLatencyTimeRecorder() {

    // Setup
    MetricsOutInterceptor outInterceptor =
        new MetricsOutInterceptor(mock(MeterRegistryService.class));

    Message mockMessage = mock(Message.class);
    Exchange ex = new ExchangeImpl();
    Bus mockBus = mock(Bus.class);

    ex.put(Bus.class, mockBus);

    when(mockBus.getId()).thenReturn("bus_id");
    when(mockMessage.getExchange()).thenReturn(ex);
    when(mockMessage.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn("false");
    when(mockMessage.get(Message.REQUESTOR_ROLE)).thenReturn("false");

    // Perform test
    outInterceptor.handleMessage(mockMessage);

    // validate that there is not an instance of LatencyTimeRecorder on the
    // exchange
    assertNull(ex.get(LatencyTimeRecorder.class));
  }
}
