/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.content.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;

import ddf.catalog.content.StorageException;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.DeleteStorageRequest;
import ddf.catalog.content.operation.DeleteStorageResponse;
import ddf.catalog.content.operation.ReadStorageRequest;
import ddf.catalog.content.operation.ReadStorageResponse;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.content.operation.impl.DeleteStorageRequestImpl;
import ddf.catalog.content.operation.impl.ReadStorageRequestImpl;
import ddf.catalog.content.operation.impl.UpdateStorageRequestImpl;
import ddf.catalog.data.Metacard;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolver;
import ddf.mime.mapper.MimeTypeMapperImpl;

public class FileSystemStorageProviderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    public String baseDir;

    public String baseTmpDir;

    private static final String NITF_MIME_TYPE = "image/nitf";

    private static final String TEST_INPUT_CONTENTS = "Hello World";

    private static final String TEST_INPUT_FILENAME = "myfile.nitf";

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemStorageProviderTest.class);

    private FileSystemStorageProvider provider;

    @Before
    public void setUp() throws IOException {
        tempFolder.create();
        baseDir = tempFolder.getRoot()
                .getAbsolutePath();

        baseTmpDir = baseDir + File.separator + FileSystemStorageProvider.DEFAULT_TMP;
        MimeTypeResolver resolver = new MockMimeTypeResolver();
        MimeTypeMapper mapper = new MimeTypeMapperImpl(Collections.singletonList(resolver));

        this.provider = new FileSystemStorageProvider();
        try {
            provider.setBaseContentDirectory(baseDir);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        provider.setMimeTypeMapper(mapper);
    }

    @After
    public void clean() {
        tempFolder.delete();
    }

    @Test
    public void testCreate() throws Exception {
        assertContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE, ".nitf");
    }

    @Test
    public void testCreateMimeTypeWithNonIdParameter() throws Exception {
        String mimeType = "image/nitf; charset=UTF-8";
        assertContentItem(TEST_INPUT_CONTENTS, mimeType, ".nitf");
    }

    @Test
    public void testCreateMimeTypeWithIdParameter() throws Exception {
        String mimeType = "text/xml; id=xml";
        assertContentItem(TEST_INPUT_CONTENTS, mimeType, ".xml");
    }

    @Test
    public void testCreateMimeTypeWithIdAndOtherParameter() throws Exception {
        String mimeType = "text/xml; charset=UTF-8; id=xml";
        assertContentItem(TEST_INPUT_CONTENTS, mimeType, ".xml");
    }

    @Test
    public void testRead() throws Exception {
        CreateStorageResponse createResponse = storeContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE,
                TEST_INPUT_FILENAME);
        String uri = createResponse.getCreatedContentItems()
                .get(0)
                .getUri();
        ReadStorageRequest readRequest = new ReadStorageRequestImpl(new URI(uri), null);

        ReadStorageResponse readResponse = provider.read(readRequest);
        ContentItem item = readResponse.getContentItem();

        LOGGER.debug("Item retrieved: {}", item);
        assertEquals(new URI(uri).getSchemeSpecificPart(), item.getId());
        assertEquals(NITF_MIME_TYPE, item.getMimeTypeRawData());

        String[] parts = provider.getContentFilePathParts(new URI(uri).getSchemeSpecificPart());
        String expectedFilePath =
                baseDir + File.separator + FileSystemStorageProvider.DEFAULT_CONTENT_REPOSITORY
                        + File.separator + FileSystemStorageProvider.DEFAULT_CONTENT_STORE
                        + File.separator + parts[0] + File.separator + parts[1] + File.separator
                        + parts[2] + File.separator + item.getFile()
                        .getName();
        assertThat(item.getFile()
                .getAbsolutePath(), is(expectedFilePath));
        assertTrue(item.getSize() > 0);
        assertTrue(item.getFile()
                .exists());
    }

    @Test
    public void testUpdate() throws Exception {
        CreateStorageResponse createResponse = storeContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE,
                TEST_INPUT_FILENAME);
        String id = createResponse.getCreatedContentItems()
                .get(0)
                .getId();
        ByteSource byteSource = new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                return IOUtils.toInputStream("Updated NITF");
            }
        };
        ContentItem updateItem = new ContentItemImpl(id, byteSource, NITF_MIME_TYPE,
                mock(Metacard.class));
        UpdateStorageRequest updateRequest = new UpdateStorageRequestImpl(
                Collections.singletonList(updateItem), null);

        UpdateStorageResponse updateResponse = provider.update(updateRequest);
        List<ContentItem> items = updateResponse.getUpdatedContentItems();
        ContentItem item = items.get(0);

        LOGGER.debug("Item retrieved: {}", item);
        assertEquals(id, item.getId());
        assertEquals(NITF_MIME_TYPE, item.getMimeTypeRawData());

        String expectedFilePath =
                baseDir + File.separator + FileSystemStorageProvider.DEFAULT_CONTENT_REPOSITORY
                        + File.separator + FileSystemStorageProvider.DEFAULT_CONTENT_STORE
                        + File.separator + FileSystemStorageProvider.DEFAULT_TMP + File.separator
                        + updateRequest.getId() + File.separator + id + File.separator
                        + item.getFile()
                        .getName();

        assertThat(item.getFile()
                .getAbsolutePath(), endsWith(expectedFilePath));
        assertTrue(item.getSize() > 0);
        assertTrue(item.getFile()
                .exists());

        String updatedFileContents = getFileContentsAsString(item.getFile()
                .getAbsolutePath());
        assertEquals("Updated NITF", updatedFileContents);
    }

    @Test
    public void testDelete() throws Exception {
        CreateStorageResponse createResponse = storeContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE,
                TEST_INPUT_FILENAME);
        String id = createResponse.getCreatedContentItems()
                .get(0)
                .getId();
        DeleteStorageRequest deleteRequest = new DeleteStorageRequestImpl(
                createResponse.getCreatedContentItems()
                        .stream()
                        .map(ContentItem::getMetacard)
                        .collect(Collectors.toList()), null);

        DeleteStorageResponse deleteResponse = provider.delete(deleteRequest);
        provider.commit(deleteRequest);
        List<ContentItem> items = deleteResponse.getDeletedContentItems();
        ContentItem item = items.get(0);

        LOGGER.debug("Item retrieved: {}", item);
        assertEquals(id, item.getId());
        assertThat(item.getFilename(), is(""));
    }

    @Test(expected = StorageException.class)
    public void testRollback() throws Exception {
        String id = UUID.randomUUID()
                .toString()
                .replaceAll("-", "");
        ByteSource byteSource = new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                return IOUtils.toInputStream(TEST_INPUT_CONTENTS);
            }
        };
        Metacard metacard = mock(Metacard.class);
        when(metacard.getId()).thenReturn(id);
        ContentItem contentItem = new ContentItemImpl(id, byteSource, NITF_MIME_TYPE, TEST_INPUT_FILENAME,
                TEST_INPUT_CONTENTS.getBytes().length, metacard);
        CreateStorageRequest createRequest = new CreateStorageRequestImpl(
                Collections.singletonList(contentItem), null);

        CreateStorageResponse createStorageResponse = provider.create(createRequest);
        provider.rollback(createRequest);

        ReadStorageRequest readStorageRequest = new ReadStorageRequestImpl(new URI("content:" + id), null);
        ReadStorageResponse read = provider.read(readStorageRequest);
    }

    /**
     * *******************************************************************************
     */

    private void assertContentItem(String data, String mimeTypeRawData, String expectedFileSuffix)
            throws Exception {
        // Simulates what ContentFrameworkImpl would do
        String uuid = UUID.randomUUID()
                .toString()
                .replaceAll("-", "");
        ByteSource byteSource = new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                return IOUtils.toInputStream(data);
            }
        };
        ContentItem contentItem = new ContentItemImpl(uuid, byteSource, mimeTypeRawData,
                mock(Metacard.class));
        CreateStorageRequest createRequest = new CreateStorageRequestImpl(
                Collections.singletonList(contentItem), null);
        CreateStorageResponse createResponse = provider.create(createRequest);
        List<ContentItem> createdContentItems = createResponse.getCreatedContentItems();
        ContentItem createdContentItem = createdContentItems.get(0);

        assertNotNull(createdContentItem);
        String id = createdContentItem.getId();
        assertNotNull(id);
        assertThat(id, equalTo(uuid));

        String contentUri = createdContentItem.getUri();
        LOGGER.debug("contentUri = {}", contentUri);
        assertNotNull(contentUri);
        String expectedContentUri = FileSystemStorageProvider.CONTENT_URI_PREFIX + uuid;
        assertThat(contentUri, equalTo(expectedContentUri));

        File file = createdContentItem.getFile();
        assertNotNull(file);
        assertTrue(file.exists());
        assertTrue(createdContentItem.getSize() > 0);
        String createdMimeType = createdContentItem.getMimeTypeRawData()
                .replace(";", "");
        List<String> createdMimeTypeArr = new ArrayList<>(
                Arrays.asList(createdMimeType.split(" ")));
        List<String> givenMimeTypeArr = new ArrayList<>(Arrays.asList(
                mimeTypeRawData.replace(";", "")
                        .split(" ")));
        assertEquals(createdMimeTypeArr.size(), givenMimeTypeArr.size());
        givenMimeTypeArr.removeAll(createdMimeTypeArr);
        assertThat(givenMimeTypeArr.size(), is(0));

    }

    private CreateStorageResponse storeContentItem(String data, String mimeType, String filename)
            throws Exception {
        String id = UUID.randomUUID()
                .toString()
                .replaceAll("-", "");
        ByteSource byteSource = new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                return IOUtils.toInputStream(data);
            }
        };
        Metacard metacard = mock(Metacard.class);
        when(metacard.getId()).thenReturn(id);
        ContentItem contentItem = new ContentItemImpl(id, byteSource, mimeType, filename,
                data.getBytes().length, metacard);
        CreateStorageRequest createRequest = new CreateStorageRequestImpl(
                Collections.singletonList(contentItem), null);

        CreateStorageResponse createStorageResponse = provider.create(createRequest);
        provider.commit(createRequest);
        return createStorageResponse;
    }

    private String getFileContentsAsString(String filename) throws Exception {
        FileInputStream file = new FileInputStream(filename);
        byte[] b = new byte[file.available()];
        file.read(b);
        file.close();

        return new String(b);
    }

}
