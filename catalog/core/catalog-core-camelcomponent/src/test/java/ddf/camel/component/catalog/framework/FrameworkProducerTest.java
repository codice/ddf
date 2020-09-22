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
package ddf.camel.component.catalog.framework;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.camel.component.catalog.CatalogComponent;
import ddf.camel.component.catalog.CatalogEndpoint;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.security.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.junit.Before;
import org.junit.Test;

public class FrameworkProducerTest {

  private static final String CREATE_OPERATION = "CREATE";

  private static final String UPDATE_OPERATION = "UPDATE";

  private static final String DELETE_OPERATION = "DELETE";

  private static final String OPERATION_HEADER_KEY = "operation";

  private static final String TIMEOUT_HEADER_KEY = "timeoutMilliseconds";

  CatalogEndpoint catalogEndpoint;

  List<Future<Object>> futures;

  FrameworkProducer frameworkProducer;

  @Before
  public void setup() {
    SecurityUtils.setSecurityManager(mock(SecurityManager.class));
    when(SecurityUtils.getSubject()).thenReturn(mock(Subject.class));
  }

  private void setupFrameworkProducer(boolean timeout) throws Exception {
    CatalogFramework catalogFramework = mock(CatalogFramework.class);
    catalogEndpoint = mock(CatalogEndpoint.class);

    CatalogComponent catalogComponent = mock(CatalogComponent.class);
    when(catalogEndpoint.getComponent()).thenReturn(catalogComponent);

    futures = new ArrayList<>();
    Future<Object> future = mock(Future.class);
    when(future.isDone()).thenReturn(!timeout);
    futures.add(future);

    ExecutorService executorService = mock(ExecutorService.class);
    when(executorService.invokeAll(any(), anyLong(), any(TimeUnit.class))).thenReturn(futures);
    when(catalogEndpoint.getExecutor()).thenReturn(executorService);

    frameworkProducer = new FrameworkProducer(catalogEndpoint, catalogFramework);
  }

  @Test
  public void testFrameworkProducerCreate() throws Exception {
    setupFrameworkProducer(false);

    Exchange mockExchange = mock(Exchange.class);
    Message message = mock(Message.class);

    when(mockExchange.getIn()).thenReturn(message);
    when(mockExchange.getOut()).thenReturn(message);
    when(mockExchange.getIn().getHeader(OPERATION_HEADER_KEY)).thenReturn(CREATE_OPERATION);
    when(mockExchange.getIn().getHeader(TIMEOUT_HEADER_KEY)).thenReturn(1000L);

    when(mockExchange.getIn().getBody()).thenReturn(new MetacardImpl());
    when(mockExchange.getIn().getBody(any())).thenReturn(new MetacardImpl());

    frameworkProducer.process(mockExchange);
  }

  @Test(expected = IngestTimeoutException.class)
  public void testFrameworkProducerCreateTimeout() throws Exception {
    setupFrameworkProducer(true);

    Exchange mockExchange = mock(Exchange.class);
    Message message = mock(Message.class);

    when(mockExchange.getIn()).thenReturn(message);
    when(mockExchange.getOut()).thenReturn(message);
    when(mockExchange.getIn().getHeader(OPERATION_HEADER_KEY)).thenReturn(CREATE_OPERATION);
    when(mockExchange.getIn().getHeader(TIMEOUT_HEADER_KEY)).thenReturn(1000L);

    when(mockExchange.getIn().getBody()).thenReturn(new MetacardImpl());
    when(mockExchange.getIn().getBody(any())).thenReturn(new MetacardImpl());

    frameworkProducer.process(mockExchange);
  }

  @Test
  public void testFrameworkProducerUpdate() throws Exception {
    setupFrameworkProducer(false);

    Exchange mockExchange = mock(Exchange.class);
    Message message = mock(Message.class);

    when(mockExchange.getIn()).thenReturn(message);
    when(mockExchange.getOut()).thenReturn(message);
    when(mockExchange.getIn().getHeader(OPERATION_HEADER_KEY)).thenReturn(UPDATE_OPERATION);
    when(mockExchange.getIn().getHeader(TIMEOUT_HEADER_KEY)).thenReturn(1000L);

    when(mockExchange.getIn().getBody()).thenReturn(new MetacardImpl());
    when(mockExchange.getIn().getBody(any())).thenReturn(new MetacardImpl());

    frameworkProducer.process(mockExchange);
  }

  @Test(expected = IngestTimeoutException.class)
  public void testFrameworkProducerUpdateTimeout() throws Exception {
    setupFrameworkProducer(true);

    Exchange mockExchange = mock(Exchange.class);
    Message message = mock(Message.class);

    when(mockExchange.getIn()).thenReturn(message);
    when(mockExchange.getOut()).thenReturn(message);
    when(mockExchange.getIn().getHeader(OPERATION_HEADER_KEY)).thenReturn(UPDATE_OPERATION);
    when(mockExchange.getIn().getHeader(TIMEOUT_HEADER_KEY)).thenReturn(1000L);

    when(mockExchange.getIn().getBody()).thenReturn(new MetacardImpl());
    when(mockExchange.getIn().getBody(any())).thenReturn(new MetacardImpl());

    frameworkProducer.process(mockExchange);
  }

  @Test
  public void testFrameworkProducerDelete() throws Exception {
    setupFrameworkProducer(false);

    Exchange mockExchangeCreate = mock(Exchange.class);
    Exchange mockExchangeDelete = mock(Exchange.class);
    Message message = mock(Message.class);
    Metacard metacard = mock(MetacardImpl.class);
    when(metacard.getId()).thenReturn("metacard1");

    when(mockExchangeCreate.getIn()).thenReturn(message);
    when(mockExchangeCreate.getOut()).thenReturn(message);
    when(mockExchangeCreate.getIn().getHeader(OPERATION_HEADER_KEY)).thenReturn(CREATE_OPERATION);
    when(mockExchangeCreate.getIn().getHeader(TIMEOUT_HEADER_KEY)).thenReturn(1000L);

    when(mockExchangeCreate.getIn().getBody()).thenReturn(metacard);
    when(mockExchangeCreate.getIn().getBody(any())).thenReturn(metacard);
    frameworkProducer.process(mockExchangeCreate);

    when(mockExchangeDelete.getIn()).thenReturn(message);
    when(mockExchangeDelete.getOut()).thenReturn(message);
    when(mockExchangeDelete.getIn().getHeader(OPERATION_HEADER_KEY)).thenReturn(DELETE_OPERATION);
    when(mockExchangeDelete.getIn().getHeader(TIMEOUT_HEADER_KEY)).thenReturn(1000L);
  }
}
