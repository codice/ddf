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
package ddf.camel.component.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.camel.component.catalog.ingest.PostIngestConsumer;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.UpdateResponse;
import java.util.Dictionary;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class PostIngestConsumerTest {
  private CatalogEndpoint mockEndpoint = mock(CatalogEndpoint.class);

  private Processor mockProcessor = mock(Processor.class);

  private ServiceRegistration mockRegistration = mock(ServiceRegistration.class);

  private Exchange mockExchange = mock(Exchange.class);

  private Message mockMessage = mock(Message.class);

  private CreateResponse mockCreateResponse = mock(CreateResponse.class);

  private UpdateResponse mockUpdateResponse = mock(UpdateResponse.class);

  private DeleteResponse mockDeleteResponse = mock(DeleteResponse.class);

  private CatalogComponent mockCatalogComponent = mock(CatalogComponent.class);

  private BundleContext mockBundleContext = mock(BundleContext.class);

  private PostIngestConsumer postIngestConsumer;

  @Before
  public void setUp() {
    when(mockEndpoint.getComponent()).thenReturn(mockCatalogComponent);
    when(mockCatalogComponent.getBundleContext()).thenReturn(mockBundleContext);
    when(mockEndpoint
            .getComponent()
            .getBundleContext()
            .registerService(any(String.class), any(Object.class), any(Dictionary.class)))
        .thenReturn(mockRegistration);
    when(mockEndpoint.createExchange()).thenReturn(mockExchange);
    when(mockExchange.getIn()).thenReturn(mockMessage);
    postIngestConsumer = new PostIngestConsumer(mockEndpoint, mockProcessor);
  }

  @Test
  public void testCreate() throws Exception {
    postIngestConsumer.process(mockCreateResponse);
    verify(mockProcessor, timeout(5000).atLeastOnce()).process(any());
  }

  @Test
  public void testUpdate() throws Exception {
    postIngestConsumer.process(mockUpdateResponse);
    verify(mockProcessor, timeout(5000).atLeastOnce()).process(any());
  }

  @Test
  public void testDelete() throws Exception {
    postIngestConsumer.process(mockDeleteResponse);
    verify(mockProcessor, timeout(5000).atLeastOnce()).process(any());
  }
}
