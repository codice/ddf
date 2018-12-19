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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.camel.component.catalog.CatalogEndpoint;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeToTransformerMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import javax.activation.MimeType;
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

  private InputTransformerProducer inputTransformerProducer;

  private MimeTypeToTransformerMapper mimeTypeToTransformerMapper;

  private MimeTypeMapper mimeTypeMapper;

  @Before
  public void setup() throws Exception {
    message = mock(Message.class);

    CatalogEndpoint catalogEndpoint = mock(CatalogEndpoint.class);
    mimeTypeMapper = mock(MimeTypeMapper.class);
    when(catalogEndpoint.getMimeTypeMapper()).thenReturn(mimeTypeMapper);

    inputTransformerProducer = new InputTransformerProducer(catalogEndpoint);

    mimeTypeToTransformerMapper = mock(MimeTypeToTransformerMapper.class);
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
}
