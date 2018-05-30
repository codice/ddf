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
package org.codice.ddf.attachment.impl;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.codice.ddf.attachment.AttachmentInfo;
import org.codice.ddf.attachment.AttachmentParser;
import org.junit.Before;
import org.junit.Test;

public class AttachmentParserImplTest {

  private static final String FULL_FILENAME = "myFile.txt";
  private static final String TEXT_PLAIN = "text/plain";
  private static final String TEXT_EXT = "txt";

  private AttachmentParser attachmentParser;

  @Before
  public void setup() throws MimeTypeResolutionException {
    MimeTypeMapper mimeTypeMapper = mock(MimeTypeMapper.class);
    when(mimeTypeMapper.getMimeTypeForFileExtension(TEXT_EXT)).thenReturn(TEXT_PLAIN);
    when(mimeTypeMapper.getFileExtensionForMimeType(TEXT_PLAIN)).thenReturn(TEXT_EXT);
    attachmentParser = new AttachmentParserImpl(mimeTypeMapper);
  }

  @Test
  public void testSimpleCase() throws IOException {

    try (InputStream inputStream = createTestInputStream()) {

      AttachmentInfo attachmentInfo =
          attachmentParser.generateAttachmentInfo(inputStream, TEXT_PLAIN, FULL_FILENAME);

      assertThat(attachmentInfo.getFilename(), is(FULL_FILENAME));
      assertThat(attachmentInfo.getContentType(), is(TEXT_PLAIN));
      assertThat(attachmentInfo.getStream(), is(inputStream));
    }
  }

  @Test
  public void testWithoutFilename() throws IOException {

    try (InputStream inputStream = createTestInputStream()) {

      AttachmentInfo attachmentInfo =
          attachmentParser.generateAttachmentInfo(inputStream, TEXT_PLAIN, null);

      assertThat(attachmentInfo.getFilename(), is("file." + TEXT_EXT));
      assertThat(attachmentInfo.getContentType(), is(TEXT_PLAIN));
      assertThat(attachmentInfo.getStream(), is(inputStream));
    }
  }

  @Test
  public void testWithoutFilenameAndUnrecognizedContentType() throws IOException {

    String unrecognizedContentType = "foo/bar";

    try (InputStream inputStream = createTestInputStream()) {

      AttachmentInfo attachmentInfo =
          attachmentParser.generateAttachmentInfo(inputStream, unrecognizedContentType, null);

      assertThat(attachmentInfo.getFilename(), is("file.bin"));
      assertThat(attachmentInfo.getContentType(), is(unrecognizedContentType));
      assertThat(attachmentInfo.getStream(), is(inputStream));
    }
  }

  private InputStream createTestInputStream() {
    return new ByteArrayInputStream("xyz".getBytes());
  }
}
