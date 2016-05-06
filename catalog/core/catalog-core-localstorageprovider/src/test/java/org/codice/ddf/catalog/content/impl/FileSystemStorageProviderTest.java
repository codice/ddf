/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.content.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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

    private static final Logger LOGGER =
            LoggerFactory.getLogger(FileSystemStorageProviderTest.class);

    private static final String QUALIFIER = "example";

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
        assertContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE, "test.nitf");
    }

    @Test
    public void testCreateMimeTypeWithNonIdParameter() throws Exception {
        String mimeType = "image/nitf; charset=UTF-8";
        assertContentItem(TEST_INPUT_CONTENTS, mimeType, "test.nitf");
    }

    @Test
    public void testCreateMimeTypeWithIdParameter() throws Exception {
        String mimeType = "text/xml; id=xml";
        assertContentItem(TEST_INPUT_CONTENTS, mimeType, "test.xml");
    }

    @Test
    public void testCreateMimeTypeWithIdAndOtherParameter() throws Exception {
        String mimeType = "text/xml; charset=UTF-8; id=xml";
        assertContentItem(TEST_INPUT_CONTENTS, mimeType, "test.xml");
    }

    @Test
    public void testRead() throws Exception {
        CreateStorageResponse createResponse = assertContentItem(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME);
        URI uri = new URI(createResponse.getCreatedContentItems()
                .get(0)
                .getUri());

        assertReadRequest(uri, NITF_MIME_TYPE);
    }

    @Test
    public void testUpdate() throws Exception {
        CreateStorageResponse createResponse = assertContentItem(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
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
        ContentItem updateItem = new ContentItemImpl(id,
                byteSource,
                NITF_MIME_TYPE,
                mock(Metacard.class));
        UpdateStorageRequest updateRequest = new UpdateStorageRequestImpl(Collections.singletonList(
                updateItem), null);

        assertUpdateRequest(updateRequest);
    }

    @Test
    public void testDelete() throws Exception {
        CreateStorageResponse createResponse = assertContentItem(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME);
        String id = createResponse.getCreatedContentItems()
                .get(0)
                .getId();
        DeleteStorageRequest deleteRequest =
                new DeleteStorageRequestImpl(createResponse.getCreatedContentItems()
                        .stream()
                        .map(ContentItem::getMetacard)
                        .collect(Collectors.toList()), null);
        when(deleteRequest.getMetacards()
                .get(0)
                .getId()).thenReturn(id);

        DeleteStorageResponse deleteResponse = provider.delete(deleteRequest);
        List<ContentItem> items = deleteResponse.getDeletedContentItems();
        ContentItem item = items.get(0);

        LOGGER.debug("Item retrieved: {}", item);
        assertEquals(id, item.getId());
        assertThat(item.getFilename(), isEmptyString());
    }

    @Test
    public void testCreateWithQualifier() throws Exception {
        assertContentItemWithQualifier(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME,
                "",
                "example");
    }

    @Test
    public void testReadWithQualifier() throws Exception {
        CreateStorageResponse createResponse = assertContentItemWithQualifier(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME,
                "",
                QUALIFIER);
        URI uri = new URI(createResponse.getCreatedContentItems()
                .get(0)
                .getUri());

        assertReadRequest(uri, NITF_MIME_TYPE);
    }

    @Test
    public void testUpdateWithQualifier() throws Exception {
        CreateStorageResponse createResponse = assertContentItemWithQualifier(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME,
                "",
                QUALIFIER);
        String id = createResponse.getCreatedContentItems()
                .get(0)
                .getId();
        ByteSource byteSource = new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                return IOUtils.toInputStream("Updated NITF");
            }
        };
        ContentItem updateItem = new ContentItemImpl(id,
                QUALIFIER,
                byteSource,
                NITF_MIME_TYPE,
                mock(Metacard.class));
        UpdateStorageRequest updateRequest = new UpdateStorageRequestImpl(Collections.singletonList(
                updateItem), null);

        assertUpdateRequest(updateRequest);
    }

    @Test
    public void testDeleteWithQualifier() throws Exception {
        CreateStorageResponse createResponse = assertContentItemWithQualifier(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME,
                "",
                QUALIFIER);
        String id = createResponse.getCreatedContentItems()
                .get(0)
                .getId();
        DeleteStorageRequest deleteRequest =
                new DeleteStorageRequestImpl(createResponse.getCreatedContentItems()
                        .stream()
                        .map(ContentItem::getMetacard)
                        .collect(Collectors.toList()), null);

        when(deleteRequest.getMetacards()
                .get(0)
                .getId()).thenReturn(id);

        DeleteStorageResponse deleteResponse = provider.delete(deleteRequest);
        provider.commit(deleteRequest);
        List<ContentItem> items = deleteResponse.getDeletedContentItems();
        ContentItem item = items.get(0);

        LOGGER.debug("Item retrieved: {}", item);
        assertEquals(id, item.getId());
        assertThat(item.getFilename(), is(""));
    }

    @Test
    public void testCreateWithMultipleItems() throws Exception {
        assertContentItemWithQualifier(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME,
                "",
                "example");
        assertContentItemWithQualifier(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME,
                "",
                "example");
    }

    @Test
    public void testReadWithMultipleItems() throws Exception {
        CreateStorageResponse createResponse = assertContentItem(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME);
        URI unqualifiedUri = new URI(createResponse.getCreatedContentItems()
                .get(0)
                .getUri());
        createResponse = assertContentItemWithQualifier(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME,
                createResponse.getCreatedContentItems()
                        .get(0)
                        .getId(),
                QUALIFIER);
        URI qualifiedUri = new URI(createResponse.getCreatedContentItems()
                .get(0)
                .getUri());

        assertReadRequest(unqualifiedUri, NITF_MIME_TYPE);

        assertReadRequest(qualifiedUri, NITF_MIME_TYPE);
    }

    @Test
    public void testUpdateWithMultipleItems() throws Exception {
        CreateStorageResponse createResponse = assertContentItem(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME);
        URI unqualifiedUri = new URI(createResponse.getCreatedContentItems()
                .get(0)
                .getUri());

        createResponse = assertContentItemWithQualifier(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME,
                createResponse.getCreatedContentItems()
                        .get(0)
                        .getId(),
                QUALIFIER);

        URI qualifiedUri = new URI(createResponse.getCreatedContentItems()
                .get(0)
                .getUri());

        String id = createResponse.getCreatedContentItems()
                .get(0)
                .getId();
        ByteSource byteSource = new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                return IOUtils.toInputStream("Updated NITF");
            }
        };
        ContentItem updateItem = new ContentItemImpl(id,
                byteSource,
                NITF_MIME_TYPE,
                mock(Metacard.class));
        UpdateStorageRequest updateRequest = new UpdateStorageRequestImpl(Collections.singletonList(
                updateItem), null);

        assertUpdateRequest(updateRequest);

        updateItem = new ContentItemImpl(id,
                qualifiedUri.getFragment(),
                byteSource,
                NITF_MIME_TYPE,
                mock(Metacard.class));

        updateRequest = new UpdateStorageRequestImpl(Collections.singletonList(updateItem), null);

        assertUpdateRequest(updateRequest);
    }

    @Test
    public void testDeleteIdWithMultpleContent() throws Exception {
        CreateStorageResponse createResponse = assertContentItem(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME);
        createResponse = assertContentItemWithQualifier(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME,
                createResponse.getCreatedContentItems()
                        .get(0)
                        .getId(),
                QUALIFIER);
        String id = createResponse.getCreatedContentItems()
                .get(0)
                .getId();
        DeleteStorageRequest deleteRequest =
                new DeleteStorageRequestImpl(createResponse.getCreatedContentItems()
                        .stream()
                        .map(ContentItem::getMetacard)
                        .collect(Collectors.toList()), null);
        when(deleteRequest.getMetacards()
                .get(0)
                .getId()).thenReturn(id);

        DeleteStorageResponse deleteResponse = provider.delete(deleteRequest);
        provider.commit(deleteRequest);
        List<ContentItem> items = deleteResponse.getDeletedContentItems();
        assertThat(items, hasSize(2));
        for (ContentItem item : items) {
            LOGGER.debug("Item retrieved: {}", item);
            assertThat(item.getId(), is(id));
            assertThat(item.getFilename(), isEmptyString());
        }
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
        ContentItem contentItem = new ContentItemImpl(id,
                byteSource,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME,
                TEST_INPUT_CONTENTS.getBytes().length,
                metacard);
        CreateStorageRequest createRequest = new CreateStorageRequestImpl(Collections.singletonList(
                contentItem), null);

        CreateStorageResponse createStorageResponse = provider.create(createRequest);
        provider.rollback(createRequest);

        ReadStorageRequest readStorageRequest = new ReadStorageRequestImpl(new URI("content:" + id),
                null);
        ReadStorageResponse read = provider.read(readStorageRequest);
    }

    @Test
    public void testDeleteWithSimilarIds() throws Exception {
        CreateStorageResponse createResponse = assertContentItem(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME);
        String id = createResponse.getCreatedContentItems()
                .get(0)
                .getId();
        String uuid = UUID.randomUUID()
                .toString()
                .replaceAll("-", "");
        String newId = id.substring(0, 6) + uuid.substring(6, uuid.length() - 1);
        createResponse = assertContentItemWithQualifier(TEST_INPUT_CONTENTS,
                NITF_MIME_TYPE,
                TEST_INPUT_FILENAME,
                newId,
                "");

        DeleteStorageRequest deleteRequest =
                new DeleteStorageRequestImpl(createResponse.getCreatedContentItems()
                        .stream()
                        .map(ContentItem::getMetacard)
                        .collect(Collectors.toList()), null);
        when(deleteRequest.getMetacards()
                .get(0)
                .getId()).thenReturn(id);

        DeleteStorageResponse deleteResponse = provider.delete(deleteRequest);
        List<ContentItem> items = deleteResponse.getDeletedContentItems();
        ContentItem item = items.get(0);

        LOGGER.debug("Item retrieved: {}", item);
        assertEquals(id, item.getId());
        assertThat(item.getFilename(), is(""));
        provider.commit(deleteRequest);

        assertReadRequest(new URI(createResponse.getCreatedContentItems()
                .get(0)
                .getUri()), NITF_MIME_TYPE);
    }

    /**
     * *******************************************************************************
     */

    private CreateStorageResponse assertContentItem(String data, String mimeTypeRawData,
            String filename) throws Exception {
        return assertContentItemWithQualifier(data, mimeTypeRawData, filename, "", "");
    }

    public CreateStorageResponse assertContentItemWithQualifier(String data, String mimeTypeRawData,
            String filename, String id, String qualifier) throws Exception {
        // Simulates what ContentFrameworkImpl would do
        String uuid = StringUtils.defaultIfBlank(id,
                UUID.randomUUID()
                        .toString()
                        .replaceAll("-", ""));
        ByteSource byteSource = new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                return IOUtils.toInputStream(data);
            }
        };
        ContentItem contentItem = new ContentItemImpl(uuid,
                qualifier,
                byteSource,
                mimeTypeRawData,
                filename,
                byteSource.size(),
                mock(Metacard.class));
        CreateStorageRequest createRequest = new CreateStorageRequestImpl(Collections.singletonList(
                contentItem), null);
        CreateStorageResponse createResponse = provider.create(createRequest);
        List<ContentItem> createdContentItems = createResponse.getCreatedContentItems();
        ContentItem createdContentItem = createdContentItems.get(0);

        assertNotNull(createdContentItem);
        String createdId = createdContentItem.getId();
        assertNotNull(createdId);
        assertThat(createdId, equalTo(uuid));

        String contentUri = createdContentItem.getUri();
        LOGGER.debug("contentUri = {}", contentUri);
        assertNotNull(contentUri);
        String expectedContentUri =
                ContentItem.CONTENT_SCHEME + ":" + uuid + ((StringUtils.isNotBlank(qualifier)) ?
                        "#" + qualifier :
                        "");
        assertThat(contentUri, equalTo(expectedContentUri));

        assertTrue(createdContentItem.getSize() > 0);
        String createdMimeType = createdContentItem.getMimeTypeRawData()
                .replace(";", "");
        List<String> createdMimeTypeArr =
                new ArrayList<>(Arrays.asList(createdMimeType.split(" ")));
        List<String> givenMimeTypeArr = new ArrayList<>(Arrays.asList(mimeTypeRawData.replace(";",
                "")
                .split(" ")));
        assertEquals(createdMimeTypeArr.size(), givenMimeTypeArr.size());
        givenMimeTypeArr.removeAll(createdMimeTypeArr);
        assertThat(givenMimeTypeArr.size(), is(0));
        provider.commit(createRequest);
        return createResponse;
    }

    private void assertReadRequest(URI uri, String mimeType) throws StorageException, IOException {
        ReadStorageRequest readRequest = new ReadStorageRequestImpl(uri, null);

        ReadStorageResponse readResponse = provider.read(readRequest);
        ContentItem item = readResponse.getContentItem();

        LOGGER.debug("Item retrieved: {}", item);
        assertThat(item.getId(), is(uri.getSchemeSpecificPart()));
        if (uri.getFragment() != null) {
            assertThat(item.getQualifier(), is(uri.getFragment()));
        }
        assertThat(item.getMimeTypeRawData(), is(mimeType));

        List<String> parts = provider.getContentFilePathParts(uri.getSchemeSpecificPart(),
                uri.getFragment());
        String expectedFilePath =
                baseDir + File.separator + FileSystemStorageProvider.DEFAULT_CONTENT_REPOSITORY
                        + File.separator + FileSystemStorageProvider.DEFAULT_CONTENT_STORE
                        + File.separator + parts.get(0) + File.separator + parts.get(1)
                        + File.separator + parts.get(2)
                        + (StringUtils.isNotBlank(item.getQualifier()) ?
                        File.separator + item.getQualifier() :
                        "") + File.separator + item.getFilename();
        assertThat(Files.exists(Paths.get(expectedFilePath)), is(true));
        assertTrue(item.getSize() > 0);
    }

    private void assertUpdateRequest(UpdateStorageRequest updateRequest) throws Exception {
        UpdateStorageResponse updateResponse = provider.update(updateRequest);

        List<ContentItem> items = updateResponse.getUpdatedContentItems();
        ContentItem item = items.get(0);
        String id = item.getId();

        LOGGER.debug("Item retrieved: {}", item);
        assertEquals(id, item.getId());
        assertEquals(NITF_MIME_TYPE, item.getMimeTypeRawData());
        URI uri = new URI(item.getUri());

        List<String> parts = provider.getContentFilePathParts(uri.getSchemeSpecificPart(),
                uri.getFragment());

        String expectedFilePath =
                baseDir + File.separator + FileSystemStorageProvider.DEFAULT_CONTENT_REPOSITORY
                        + File.separator + FileSystemStorageProvider.DEFAULT_CONTENT_STORE
                        + File.separator + FileSystemStorageProvider.DEFAULT_TMP + File.separator
                        + updateRequest.getId() + File.separator + parts.get(0) + File.separator
                        + parts.get(1) + File.separator + parts.get(2) + (StringUtils.isNotBlank(
                        item.getQualifier()) ? File.separator + item.getQualifier() : "")
                        + File.separator + item.getFilename();

        assertThat(Files.exists(Paths.get(expectedFilePath)), is(true));
        assertTrue(item.getSize() > 0);

        String updatedFileContents = null;
        try (InputStream is = item.getInputStream()) {
            updatedFileContents = IOUtils.toString(is);
        }

        assertEquals("Updated NITF", updatedFileContents);
        provider.commit(updateRequest);
        assertReadRequest(new URI(updateRequest.getContentItems()
                .get(0)
                .getUri()), NITF_MIME_TYPE);
    }
}
