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
package ddf.camel.component.catalog.metacardtransformer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.MetacardTransformer;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class MetacardTransformerProducerTest {

  private static final String TEST_TRANSFORMER_ID = "metacard";

  private Exchange mockExchange = mock(Exchange.class);

  private Endpoint mockEndpoint = mock(Endpoint.class);

  private Message mockMessage = mock(Message.class);

  private Metacard mockMetacard = mock(Metacard.class);

  private Bundle mockBundle = mock(Bundle.class);

  private BundleContext mockBundleContext = mock(BundleContext.class);

  private MetacardTransformerProducer metacardTransformerProducer;

  private MetacardTransformer mockTransformer = mock(MetacardTransformer.class);

  private ServiceReference mockServiceReference = mock(ServiceReference.class);

  private BinaryContent mockBinaryContent = mock(BinaryContent.class);

  private Collection transformerReferences = new ArrayList<>();

  @Before
  public void setUp() throws InvalidSyntaxException {
    transformerReferences.add(mockServiceReference);

    when(mockBundle.getBundleContext()).thenReturn(mockBundleContext);
    when(mockBundleContext.getServiceReferences(any(Class.class), any(String.class)))
        .thenReturn(transformerReferences);
    when(mockBundleContext.getService(any())).thenReturn(mockTransformer);
    when(mockExchange.getIn()).thenReturn(mockMessage);
    when(mockExchange.getOut()).thenReturn(mockMessage);
    when(mockMessage.getBody()).thenReturn(mockMetacard);
    when(mockMessage.getHeader(any(String.class), any(Class.class)))
        .thenReturn(TEST_TRANSFORMER_ID);
    metacardTransformerProducer =
        new MetacardTransformerProducer(mockEndpoint, clazz -> mockBundle);
  }

  @Test
  public void testProcess() throws Exception {
    metacardTransformerProducer.process(mockExchange);
    verify(mockTransformer, atLeastOnce()).transform(any(Metacard.class), any());
    verify(mockMessage, times(1)).setBody(null);
    verify(mockMessage, times(0)).setBody(notNull());
  }

  @Test
  public void testNullTransform() throws Exception {
    when(mockTransformer.transform(any(), any())).thenReturn(mockBinaryContent);
    when(mockBinaryContent.getByteArray()).thenReturn("TEST".getBytes());
    metacardTransformerProducer.process(mockExchange);
    verify(mockTransformer, atLeastOnce()).transform(any(Metacard.class), any());
    verify(mockMessage, times(1)).setBody(notNull());
    verify(mockMessage, times(0)).setBody(null);
  }
}
