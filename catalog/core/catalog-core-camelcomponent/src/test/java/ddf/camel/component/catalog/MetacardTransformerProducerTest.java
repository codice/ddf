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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import ddf.camel.component.catalog.metacardtransformer.MetacardTransformerProducer;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.codice.ddf.catalog.transform.Transform;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({FrameworkUtil.class})
public class MetacardTransformerProducerTest {

  @Rule public PowerMockRule rule = new PowerMockRule();

  private static final String TEST_TRANSFORMER_ID = "metacard";

  private Exchange mockExchange = mock(Exchange.class);

  private Endpoint mockEndpoint = mock(Endpoint.class);

  private Message mockMessage = mock(Message.class);

  private Metacard mockMetacard = mock(Metacard.class);

  private MetacardTransformerProducer metacardTransformerProducer;

  private BinaryContent mockBinaryContent = mock(BinaryContent.class);

  private Transform transform;

  @Before
  public void setUp() throws InvalidSyntaxException {
    initMocks(this);

    when(mockExchange.getIn()).thenReturn(mockMessage);
    when(mockExchange.getOut()).thenReturn(mockMessage);
    when(mockMessage.getBody()).thenReturn(mockMetacard);
    when(mockMessage.getHeader(any(String.class), any(Class.class)))
        .thenReturn(TEST_TRANSFORMER_ID);

    transform = mock(Transform.class);

    metacardTransformerProducer = new MetacardTransformerProducer(mockEndpoint, transform);
  }

  @Test
  public void testProcess() throws Exception {

    when(transform.transform(any(List.class), any(String.class), any(Map.class)))
        .thenReturn(Collections.emptyList());

    metacardTransformerProducer.process(mockExchange);
    verify(mockMessage, times(1)).setBody(null);
    verify(mockMessage, times(0)).setBody(notNull());
  }

  @Test
  public void testNullTransform() throws Exception {

    when(mockBinaryContent.getByteArray()).thenReturn("TEST".getBytes());

    when(transform.transform(any(List.class), any(String.class), any(Map.class)))
        .thenReturn(Collections.singletonList(mockBinaryContent));

    metacardTransformerProducer.process(mockExchange);

    verify(mockMessage, times(1)).setBody(notNull());
    verify(mockMessage, times(0)).setBody(null);
  }
}
