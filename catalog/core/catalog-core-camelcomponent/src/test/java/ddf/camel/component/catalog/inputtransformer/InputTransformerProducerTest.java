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
package ddf.camel.component.catalog.inputtransformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.camel.component.catalog.CatalogComponent;
import ddf.camel.component.catalog.CatalogEndpoint;
import ddf.camel.component.catalog.transformer.TransformerTimeoutException;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeToTransformerMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.activation.MimeType;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class InputTransformerProducerTest {

  private static final String FILE_EXT_HEADER = "org.codice.ddf.camel.FileExtension";

  private static final String TEXT_FILE_EXT = "txt";

  private static final String XML = "xml";

  private static final String TEXT_MIME_TYPE = "text/plain";

  private static final String XML_MIME_TYPE = "text/xml";

  private Message message;

  CatalogEndpoint catalogEndpoint;

  List<Future<Object>> futures;

  private InputTransformerProducer inputTransformerProducer;

  private MimeTypeToTransformerMapper mimeTypeToTransformerMapper;

  private MimeTypeMapper mimeTypeMapper;

  private void setupCatalogEndpoint(boolean timeout) throws Exception {
    catalogEndpoint = mock(CatalogEndpoint.class);
    mimeTypeMapper = mock(MimeTypeMapper.class);
    when(catalogEndpoint.getMimeTypeMapper()).thenReturn(mimeTypeMapper);

    CatalogComponent catalogComponent = mock(CatalogComponent.class);
    when(catalogEndpoint.getComponent()).thenReturn(catalogComponent);
    when(catalogComponent.getMimeTypeToTransformerMapper()).thenReturn(mimeTypeToTransformerMapper);

    futures = new ArrayList<>();
    Future<Object> future = mock(Future.class);
    when(future.isDone()).thenReturn(!timeout);
    futures.add(future);

    ExecutorService executorService = mock(ExecutorService.class);
    when(executorService.invokeAll(any(), anyLong(), any(TimeUnit.class)))
        .thenAnswer(
            invocationOnMock -> {
              ((Callable) ((Set) invocationOnMock.getArguments()[0]).iterator().next()).call();
              return futures;
            });
    when(catalogEndpoint.getExecutor()).thenReturn(executorService);

    inputTransformerProducer = new InputTransformerProducer(catalogEndpoint);
  }

  @Before
  public void setup() throws Exception {
    message = mock(Message.class);
    mimeTypeToTransformerMapper = mock(MimeTypeToTransformerMapper.class);

    setupCatalogEndpoint(false);

    InputTransformer inputTransformer = mock(InputTransformer.class);
    when(inputTransformer.transform(any(InputStream.class))).thenReturn(mock(Metacard.class));
    when(mimeTypeToTransformerMapper.findMatches(any(Class.class), any(MimeType.class)))
        .thenReturn(Collections.singletonList(inputTransformer));

    when(message.getBody(InputStream.class))
        .thenReturn(this.getClass().getClassLoader().getResourceAsStream("file.txt"));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testNullMessageBodyThrowsException() throws Exception {
    when(message.getBody(InputStream.class)).thenReturn(null);
    inputTransformerProducer.transform(
        message, "doesnt matter", "doesnt matter", mimeTypeToTransformerMapper);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testErrorReadingInputStreamThrowsException() throws Exception {
    InputStream is = mock(InputStream.class);
    when(is.read(any())).thenThrow(IOException.class);
    when(message.getBody(InputStream.class)).thenReturn(is);
    inputTransformerProducer.transform(
        message, "doesnt matter", "doesnt matter", mimeTypeToTransformerMapper);
  }

  @Test
  public void testFileExtensionHeaderMimeType() throws Exception {
    when(message.getHeader(FILE_EXT_HEADER, String.class)).thenReturn(TEXT_FILE_EXT);
    when(mimeTypeMapper.guessMimeType(any(InputStream.class), any(String.class)))
        .thenReturn(TEXT_MIME_TYPE);

    ArgumentCaptor<MimeType> mimeTypeCaptor = ArgumentCaptor.forClass(MimeType.class);
    inputTransformerProducer.transform(message, XML_MIME_TYPE, XML, mimeTypeToTransformerMapper);
    verify(mimeTypeToTransformerMapper).findMatches(any(Class.class), mimeTypeCaptor.capture());
    assertThat(mimeTypeCaptor.getValue().toString(), is(equalTo(TEXT_MIME_TYPE)));
  }

  @Test
  public void testEndpointMimeTypeAndTransformerIdMimeType() throws Exception {
    ArgumentCaptor<MimeType> mimeTypeCaptor = ArgumentCaptor.forClass(MimeType.class);
    inputTransformerProducer.transform(message, XML_MIME_TYPE, XML, mimeTypeToTransformerMapper);
    verify(mimeTypeToTransformerMapper).findMatches(any(Class.class), mimeTypeCaptor.capture());
    assertThat(mimeTypeCaptor.getValue().toString(), is(equalTo("text/xml; id=xml")));
  }

  @Test
  public void testEndpointMimeType() throws Exception {
    ArgumentCaptor<MimeType> mimeTypeCaptor = ArgumentCaptor.forClass(MimeType.class);
    inputTransformerProducer.transform(message, XML_MIME_TYPE, "", mimeTypeToTransformerMapper);
    verify(mimeTypeToTransformerMapper).findMatches(any(Class.class), mimeTypeCaptor.capture());
    assertThat(mimeTypeCaptor.getValue().toString(), is(equalTo(XML_MIME_TYPE)));
  }

  @Test
  public void testDefaultMimeType() throws Exception {
    ArgumentCaptor<MimeType> mimeTypeCaptor = ArgumentCaptor.forClass(MimeType.class);
    inputTransformerProducer.transform(message, "", "", mimeTypeToTransformerMapper);
    verify(mimeTypeToTransformerMapper).findMatches(any(Class.class), mimeTypeCaptor.capture());
    assertThat(mimeTypeCaptor.getValue().toString(), is(equalTo("application/octet-stream")));
  }

  @Test
  public void testMessageInputStreamIsClosed() throws Exception {
    InputStream is = mock(InputStream.class);
    when(is.read(any())).thenReturn(1).thenReturn(-1);
    when(message.getBody(InputStream.class)).thenReturn(is);
    inputTransformerProducer.transform(message, "", "", mimeTypeToTransformerMapper);
    verify(is).close();
  }

  @Test(expected = TransformerTimeoutException.class)
  public void testTransformTimeout() throws Exception {
    setupCatalogEndpoint(true);

    Exchange mockExchange = mock(Exchange.class);

    when(mockExchange.getIn()).thenReturn(message);
    when(mockExchange.getOut()).thenReturn(message);
    when(mockExchange.getOut()).thenReturn(message);
    when(mockExchange.getIn().getHeader("timeoutMilliseconds")).thenReturn(1000L);

    inputTransformerProducer.process(mockExchange);
  }

  @Test
  public void testTransformNoTimeout() throws Exception {
    Exchange mockExchange = mock(Exchange.class);

    when(mockExchange.getIn()).thenReturn(message);
    when(mockExchange.getOut()).thenReturn(message);
    when(mockExchange.getOut()).thenReturn(message);
    when(mockExchange.getIn().getHeader("timeoutMilliseconds")).thenReturn(1000L);

    inputTransformerProducer.process(mockExchange);
    verify(message).setBody(any(Metacard.class));
  }
}
