/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.content.endpoint.rest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ddf.content.ContentFramework;
import ddf.content.ContentFrameworkException;
import ddf.content.data.ContentItem;
import ddf.content.operation.ReadRequest;
import ddf.content.operation.ReadResponse;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;

public class ContentEndpointCreateTest {
    private static final String CONTENT_DISPOSITION = "Content-Disposition";

    // used to print logging while running as a JUnit test
    static {
        org.apache.log4j.BasicConfigurator.configure();
    }

    @Test
    public void read_Valid() throws ContentFrameworkException, IOException, MimeTypeParseException,
        MimeTypeResolutionException {
        // String requestId = "testRequestId";
        String content = "This is a test";
        String fileName = "TestFilename.txt";
        Long size = 1000L;
        String mimeType = "text/plain";

        executeReadTest(content, fileName, size, mimeType);
    }

    @Test
    public void read_Valid_Null_FileName_And_MimeType() throws ContentFrameworkException,
        IOException, MimeTypeParseException, MimeTypeResolutionException {
        String content = "This is a test";

        executeReadTest(content, null, null, null);
    }

    @Test(expected = ContentEndpointException.class)
    public void read_ContentEndpointExceptionException_Expected() throws ContentEndpointException,
        ContentFrameworkException, MimeTypeResolutionException {
        ContentFramework framework = mock(ContentFramework.class);
        when(framework.read(isA(ReadRequest.class))).thenThrow(
                new ContentEndpointException("Content Item not found.", Response.Status.NOT_FOUND));

        ContentEndpoint endpoint = new ContentEndpoint(framework, getMockMimeTypeMapper());
        endpoint.read(anyString());
    }

    protected void executeReadTest(String content, String fileName, Long size, String mimeType)
        throws IOException, MimeTypeParseException, ContentFrameworkException,
        MimeTypeResolutionException {

        ContentFramework framework = mock(ContentFramework.class);
        ReadResponse readResponse = mock(ReadResponse.class);

        ContentItem contentItem = getMockGoodContentItem(content, fileName, "contentIdValue", size,
                mimeType);
        when(readResponse.getContentItem()).thenReturn(contentItem);

        when(framework.read(isA(ReadRequest.class))).thenReturn(readResponse);

        ContentEndpoint endpoint = new ContentEndpoint(framework, getMockMimeTypeMapper());
        Response response = endpoint.read(anyString());

        // Assertions for all valid headers returned
        assertThat(response.getStatus(), equalTo(200));
        assertThat(IOUtils.toString((InputStream) response.getEntity()), equalTo(content));

        if (fileName != null) {
            assertThat((String) response.getMetadata().getFirst(CONTENT_DISPOSITION),
                    equalToIgnoringWhiteSpace("inline; filename=" + fileName));
        } else {
            assertThat(response.getMetadata().getFirst(CONTENT_DISPOSITION), is(nullValue()));
        }

        if (mimeType != null) {
            assertThat((String) response.getMetadata().getFirst(HttpHeaders.CONTENT_TYPE),
                    equalTo(mimeType));
        } else {
            assertThat((String) response.getMetadata().getFirst(HttpHeaders.CONTENT_TYPE),
                    equalTo(MediaType.APPLICATION_OCTET_STREAM));
        }

        if (size != null) {
            assertThat((String) response.getMetadata().getFirst(HttpHeaders.CONTENT_LENGTH),
                    equalTo(String.valueOf(size)));
        } else {
            assertThat(response.getMetadata().getFirst(HttpHeaders.CONTENT_LENGTH), is(nullValue()));
        }

    }

    protected ContentItem getMockGoodContentItem(String content, String fileName, String contentId,
            Long size, String mimeType) throws IOException, MimeTypeParseException {
        ContentItem item = mock(ContentItem.class);
        when(item.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
        when(item.getFilename()).thenReturn(fileName);
        when(item.getId()).thenReturn(contentId);
        if (size != null) {
            when(item.getSize()).thenReturn(size);
        } else {
            when(item.getSize()).thenThrow(new IOException("IOException"));
        }
        when(item.getMimeTypeRawData()).thenReturn(mimeType);
        if (mimeType != null) {
            when(item.getMimeType()).thenReturn(new MimeType(mimeType));
        }

        return item;
    }

    protected MimeTypeMapper getMockMimeTypeMapper() throws MimeTypeResolutionException {
        MimeTypeMapper mapper = mock(MimeTypeMapper.class);
        when(mapper.getFileExtensionForMimeType(isA(String.class))).thenReturn("txt");
        when(mapper.getMimeTypeForFileExtension(isA(String.class))).thenReturn("text/plain");
        return mapper;
    }

}
