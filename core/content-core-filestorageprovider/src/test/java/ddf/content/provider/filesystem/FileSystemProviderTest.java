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
package ddf.content.provider.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.content.data.ContentItem;
import ddf.content.data.impl.IncomingContentItem;
import ddf.content.operation.CreateRequest;
import ddf.content.operation.CreateResponse;
import ddf.content.operation.DeleteRequest;
import ddf.content.operation.DeleteResponse;
import ddf.content.operation.ReadRequest;
import ddf.content.operation.ReadResponse;
import ddf.content.operation.UpdateRequest;
import ddf.content.operation.UpdateResponse;
import ddf.content.operation.impl.CreateRequestImpl;
import ddf.content.operation.impl.DeleteRequestImpl;
import ddf.content.operation.impl.ReadRequestImpl;
import ddf.content.operation.impl.UpdateRequestImpl;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolver;
import ddf.mime.mapper.MimeTypeMapperImpl;

public class FileSystemProviderTest {
    public static final String BASE_DIR = "target" + File.separator + "test-content";

    private static final String NITF_MIME_TYPE = "image/nitf";

    private static final String TEST_INPUT_CONTENTS = "Hello World";

    private static final String TEST_INPUT_FILENAME = "myfile.nitf";

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemProviderTest.class);

    private FileSystemProvider provider;

    @Before
    public void setUp() {
        MimeTypeResolver resolver = new MockMimeTypeResolver();
        MimeTypeMapper mapper = new MimeTypeMapperImpl(Collections.singletonList(resolver));

        this.provider = new FileSystemProvider();
        provider.setBaseContentDirectory(BASE_DIR);
        provider.setMimeTypeMapper(mapper);
    }

    @Test
    public void testCreate() throws Exception {
        assertContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE, ".nitf");
    }

    @Test
    public void testCreate_MimeTypeWithNonIdParameter() throws Exception {
        String mimeType = "image/nitf;charset=UTF-8";
        assertContentItem(TEST_INPUT_CONTENTS, mimeType, ".nitf");
    }

    @Test
    public void testCreate_MimeTypeWithIdParameter() throws Exception {
        String mimeType = "text/xml;id=ddms20";
        assertContentItem(TEST_INPUT_CONTENTS, mimeType, ".xml");
    }

    @Test
    public void testCreate_MimeTypeWithIdAndOtherParameter() throws Exception {
        String mimeType = "text/xml;charset=UTF-8;id=ddms20";
        assertContentItem(TEST_INPUT_CONTENTS, mimeType, ".xml");
    }

    @Test
    public void testRead() throws Exception {
        CreateResponse createResponse = storeContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE,
                TEST_INPUT_FILENAME);
        String id = createResponse.getCreatedContentItem().getId();
        ReadRequest readRequest = new ReadRequestImpl(id, null);

        ReadResponse readResponse = provider.read(readRequest);
        ContentItem item = readResponse.getContentItem();

        LOGGER.debug("Item retrieved: {}", item);
        assertEquals(id, item.getId());
        assertEquals(NITF_MIME_TYPE, item.getMimeTypeRawData());

        String expectedFilePath = BASE_DIR + File.separator + id + File.separator
                + item.getFilename();
        assertThat(item.getFile().getAbsolutePath(), endsWith(expectedFilePath));
        assertTrue(item.getSize() > 0);
        assertTrue(item.getFile().exists());
    }

    @Test
    public void testUpdate() throws Exception {
        CreateResponse createResponse = storeContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE,
                TEST_INPUT_FILENAME);
        String id = createResponse.getCreatedContentItem().getId();
        ContentItem updateItem = new IncomingContentItem(id, IOUtils.toInputStream("Updated NITF"),
                NITF_MIME_TYPE);
        UpdateRequest updateRequest = new UpdateRequestImpl(updateItem);

        UpdateResponse updateResponse = provider.update(updateRequest);
        ContentItem item = updateResponse.getUpdatedContentItem();

        LOGGER.debug("Item retrieved: {}", item);
        assertEquals(id, item.getId());
        assertEquals(NITF_MIME_TYPE, item.getMimeTypeRawData());

        String expectedFilePath = BASE_DIR + File.separator + id + File.separator
                + item.getFilename();
        assertThat(item.getFile().getAbsolutePath(), endsWith(expectedFilePath));
        assertTrue(item.getSize() > 0);
        assertTrue(item.getFile().exists());

        String updatedFileContents = getFileContentsAsString(item.getFile().getAbsolutePath());
        assertEquals("Updated NITF", updatedFileContents);
    }

    @Test
    public void testDelete() throws Exception {
        CreateResponse createResponse = storeContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE,
                TEST_INPUT_FILENAME);
        String id = createResponse.getCreatedContentItem().getId();
        DeleteRequest deleteRequest = new DeleteRequestImpl(createResponse.getCreatedContentItem());

        DeleteResponse deleteResponse = provider.delete(deleteRequest);
        ContentItem item = deleteResponse.getContentItem();

        LOGGER.debug("Item retrieved: {}", item);
        assertEquals(id, item.getId());
        assertEquals(NITF_MIME_TYPE, item.getMimeTypeRawData());
        assertNull(item.getFile());
    }

    /***********************************************************************************/

    private void assertContentItem(String data, String mimeTypeRawData, String expectedFileSuffix)
        throws Exception {
        // Simulates what ContentFrameworkImpl would do
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        ContentItem contentItem = new IncomingContentItem(uuid, IOUtils.toInputStream(data),
                mimeTypeRawData);
        CreateRequest createRequest = new CreateRequestImpl(contentItem, null);
        CreateResponse createResponse = provider.create(createRequest);
        ContentItem createdContentItem = createResponse.getCreatedContentItem();

        assertNotNull(createdContentItem);
        String id = createdContentItem.getId();
        assertNotNull(id);
        assertThat(id, equalTo(uuid));

        String contentUri = createdContentItem.getUri();
        LOGGER.debug("contentUri = {}", contentUri);
        assertNotNull(contentUri);
        String expectedContentUri = FileSystemProvider.CONTENT_URI_PREFIX + uuid;
        assertThat(contentUri, equalTo(expectedContentUri));

        File file = createdContentItem.getFile();
        assertNotNull(file);
        assertTrue(file.exists());
        assertTrue(createdContentItem.getSize() > 0);
        assertEquals(mimeTypeRawData, createdContentItem.getMimeTypeRawData());
        assertEquals(data, IOUtils.toString(createdContentItem.getInputStream()));
    }

    private CreateResponse storeContentItem(String data, String mimeType, String filename)
        throws Exception {
        String id = UUID.randomUUID().toString().replaceAll("-", "");
        ContentItem contentItem = new IncomingContentItem(id, IOUtils.toInputStream(data),
                mimeType, filename);
        CreateRequest createRequest = new CreateRequestImpl(contentItem, null);
        CreateResponse createResponse = provider.create(createRequest);

        return createResponse;
    }

    private String getFileContentsAsString(String filename) throws Exception {
        FileInputStream file = new FileInputStream(filename);
        byte[] b = new byte[file.available()];
        file.read(b);
        file.close();

        return new String(b);
    }

}
