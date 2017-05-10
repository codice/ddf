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
package ddf.camel.component.catalog.content;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileMessage;
import org.apache.commons.collections.map.HashedMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import ddf.catalog.CatalogFramework;
import ddf.catalog.Constants;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.mime.MimeTypeMapper;

public class ContentProducerDataAccessObjectTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    ContentProducerDataAccessObject contentProducerDataAccessObject =
            new ContentProducerDataAccessObject();

    private FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

    @Test
    public void testGetFileUsingRefKey() throws Exception {
        File testFile = temporaryFolder.newFile("testRefKey");
        GenericFileMessage<File> mockMessage = getMockMessage(testFile);

        //test with storeRefKey == false
        assertThat(contentProducerDataAccessObject.getFileUsingRefKey(false, mockMessage)
                .equals(testFile), is(true));

        //test with storeRefKey == true
        assertThat(contentProducerDataAccessObject.getFileUsingRefKey(true, mockMessage)
                .equals(testFile), is(true));
    }

    @Test
    public void testGetEventType() throws Exception {
        File testFile = temporaryFolder.newFile("testEventType");
        GenericFileMessage<File> mockMessage = getMockMessage(testFile);

        //test that a new standard kind is created if storeRefKey == false
        assertThat(contentProducerDataAccessObject.getEventType(false, mockMessage)
                .equals(StandardWatchEventKinds.ENTRY_CREATE), is(true));

        //test that the sample kind is returned when storeRefKey == true
        assertThat(contentProducerDataAccessObject.getEventType(true, mockMessage)
                .name()
                .equals("example"), is(true));

    }

    private GenericFileMessage<File> getMockMessage(File testFile) {
        //set the mockGenericFile to return the test file when requested
        GenericFile<File> mockGenericFileFromBody = mock(GenericFile.class);
        doReturn(testFile).when(mockGenericFileFromBody)
                .getFile();

        WatchEvent<Path> mockWatchEvent = mock(WatchEvent.class);

        //mock out the context that links to the file
        Path mockContext = mock(Path.class);
        doReturn(testFile).when(mockContext)
                .toFile();

        //mock out with a sample kind
        WatchEvent.Kind mockKind = mock(WatchEvent.Kind.class);
        doReturn("example").when(mockKind)
                .name();

        //return the kind or context for the mockWatchEvent
        doReturn(mockKind).when(mockWatchEvent)
                .kind();
        doReturn(mockContext).when(mockWatchEvent)
                .context();

        //return the mockWatchEvent when the file is called for
        GenericFile<File> mockGenericFile = mock(GenericFile.class);
        doReturn(mockWatchEvent).when(mockGenericFile)
                .getFile();

        GenericFileMessage mockMessage = mock(GenericFileMessage.class);
        doReturn(mockGenericFileFromBody).when(mockMessage)
                .getBody();
        doReturn(mockGenericFile).when(mockMessage)
                .getGenericFile();

        return mockMessage;
    }

    @Test
    public void testGetMimeType() throws Exception {
        File testFile1 = temporaryFolder.newFile("testGetMimeType1.xml");
        File testFile2 = temporaryFolder.newFile("testGetMimeType2.txt");

        //mock out two different ways for getting mimeType
        MimeTypeMapper mockMimeTypeMapper = mock(MimeTypeMapper.class);
        doReturn("guess").when(mockMimeTypeMapper)
                .guessMimeType(any(), any());
        doReturn("extension").when(mockMimeTypeMapper)
                .getMimeTypeForFileExtension(any());

        //mock out Component that returns mockMimeTypeMapper when the mimeTypeMapper is requested
        ContentComponent mockComponent = mock(ContentComponent.class);
        doReturn(mockMimeTypeMapper).when(mockComponent)
                .getMimeTypeMapper();

        //mock out ContentEndpoint that returns the mockComponent
        ContentEndpoint mockEndpoint = mock(ContentEndpoint.class);
        doReturn(mockComponent).when(mockEndpoint)
                .getComponent();

        //assert the mock mimeTypeMappers are reached if/not xml
        assertThat(contentProducerDataAccessObject.getMimeType(mockEndpoint, testFile1)
                .equals("guess"), is(true));
        assertThat(contentProducerDataAccessObject.getMimeType(mockEndpoint, testFile2)
                .equals("extension"), is(true));
    }

    @Test
    public void testCreateContentItem() throws Exception {
        File testFile = temporaryFolder.newFile("testCreateContentItem.txt");

        //make sample list of metacard and set of keys
        List<MetacardImpl> metacardList = ImmutableList.of(new MetacardImpl());
        Set<String> keys = ImmutableSet.of(String.valueOf(testFile.getAbsolutePath()
                .hashCode()));

        //mock out responses for create, delete, update
        CreateResponse mockCreateResponse = mock(CreateResponse.class);
        doReturn(metacardList).when(mockCreateResponse)
                .getCreatedMetacards();

        DeleteResponse mockDeleteResponse = mock(DeleteResponse.class);
        doReturn(metacardList).when(mockDeleteResponse)
                .getDeletedMetacards();

        UpdateResponse mockUpdateResponse = mock(UpdateResponse.class);
        doReturn(metacardList).when(mockUpdateResponse)
                .getUpdatedMetacards();

        Result mockResult = mock(Result.class);
        doReturn(new MetacardImpl()).when(mockResult)
                .getMetacard();
        List<Result> results = ImmutableList.of(mockResult);

        QueryResponse mockQueryResponse = mock(QueryResponse.class);
        doReturn(results).when(mockQueryResponse)
                .getResults();

        //setup mockFileSystemPersistenceProvider
        FileSystemPersistenceProvider mockFileSystemPersistenceProvider = mock(
                FileSystemPersistenceProvider.class);
        doReturn(keys).when(mockFileSystemPersistenceProvider)
                .loadAllKeys();
        doReturn("sample").when(mockFileSystemPersistenceProvider)
                .loadFromPersistence(any(String.class));

        //setup mockCatalogFramework
        CatalogFramework mockCatalogFramework = mock(CatalogFramework.class);
        doReturn(mockCreateResponse).when(mockCatalogFramework)
                .create(any(CreateStorageRequest.class));
        doReturn(mockDeleteResponse).when(mockCatalogFramework)
                .delete(any(DeleteRequest.class));
        doReturn(mockQueryResponse).when(mockCatalogFramework)
                .query(any(QueryRequest.class));

        //setup mockComponent
        ContentComponent mockComponent = mock(ContentComponent.class);
        doReturn(mockCatalogFramework).when(mockComponent)
                .getCatalogFramework();
        doReturn(filterBuilder).when(mockComponent)
                .getFilterBuilder();

        //setup mockEndpoint
        ContentEndpoint mockEndpoint = mock(ContentEndpoint.class);
        doReturn(mockComponent).when(mockEndpoint)
                .getComponent();

        WatchEvent.Kind<Path> kind;

        String mimeType = "txt";

        Map<String, Object> headers = new HashedMap();
        Map<String, Serializable> attributeOverrides = new HashMap<>();
        attributeOverrides.put("example", ImmutableList.of("something", "something1"));
        attributeOverrides.put("example2", ImmutableList.of("something2"));
        headers.put(Constants.ATTRIBUTE_OVERRIDES_KEY, attributeOverrides);

        kind = StandardWatchEventKinds.ENTRY_CREATE;
        contentProducerDataAccessObject.createContentItem(mockFileSystemPersistenceProvider,
                mockEndpoint,
                testFile,
                kind,
                mimeType,
                headers);

        kind = StandardWatchEventKinds.ENTRY_DELETE;
        contentProducerDataAccessObject.createContentItem(mockFileSystemPersistenceProvider,
                mockEndpoint,
                testFile,
                kind,
                mimeType,
                headers);

        kind = StandardWatchEventKinds.ENTRY_MODIFY;
        contentProducerDataAccessObject.createContentItem(mockFileSystemPersistenceProvider,
                mockEndpoint,
                testFile,
                kind,
                mimeType,
                headers);

    }

    @Test
    public void testUpdateContentItem() throws Exception {
        File testFile = temporaryFolder.newFile("testUpdateContentItem.txt");

        //make sample list of metacard and set of keys
        List<MetacardImpl> metacardList = ImmutableList.of(new MetacardImpl());
        Set<String> keys = ImmutableSet.of(String.valueOf(testFile.getAbsolutePath()
                .hashCode()));

        //mock metacard
        Metacard mockMetacard = mock(Metacard.class);
        doReturn("someId").when(mockMetacard).getId();

        //mock out create response
        CreateResponse mockCreateResponse = mock(CreateResponse.class);
        doReturn(metacardList).when(mockCreateResponse)
                .getCreatedMetacards();

        //mock out query response
        Result mockResult = mock(Result.class);
        doReturn(mockMetacard).when(mockResult)
                .getMetacard();
        List<Result> results = ImmutableList.of(mockResult);

        QueryResponse mockQueryResponse = mock(QueryResponse.class);
        doReturn(results).when(mockQueryResponse)
                .getResults();

        //mock out update response
        Update mockUpdate = mock(Update.class);
        doReturn(mockMetacard).when(mockUpdate).getNewMetacard();

        UpdateResponse mockUpdateResponse = mock(UpdateResponse.class);
        doReturn(Collections.singletonList(mockUpdate)).when(mockUpdateResponse)
                .getUpdatedMetacards();

        //setup mockFileSystemPersistenceProvider
        FileSystemPersistenceProvider mockFileSystemPersistenceProvider = mock(
                FileSystemPersistenceProvider.class);
        doReturn(keys).when(mockFileSystemPersistenceProvider)
                .loadAllKeys();
        doReturn("sample").when(mockFileSystemPersistenceProvider)
                .loadFromPersistence(any(String.class));

        //setup mockCatalogFramework
        CatalogFramework mockCatalogFramework = mock(CatalogFramework.class);
        doReturn(mockCreateResponse).when(mockCatalogFramework)
                .create(any(CreateStorageRequest.class));
        doReturn(mockQueryResponse).when(mockCatalogFramework)
                .query(any(QueryRequest.class));
        doReturn(mockUpdateResponse).when(mockCatalogFramework)
                .update(any(UpdateStorageRequest.class));

        //setup mockComponent
        ContentComponent mockComponent = mock(ContentComponent.class);
        doReturn(mockCatalogFramework).when(mockComponent)
                .getCatalogFramework();
        doReturn(filterBuilder).when(mockComponent)
                .getFilterBuilder();

        //setup mockEndpoint
        ContentEndpoint mockEndpoint = mock(ContentEndpoint.class);
        doReturn(mockComponent).when(mockEndpoint)
                .getComponent();

        WatchEvent.Kind<Path> kind;

        String mimeType = "txt";

        Map<String, Object> headers = new HashedMap();
        Map<String, Serializable> attributeOverrides = new HashMap<>();
        attributeOverrides.put("example", ImmutableList.of("something", "something1"));
        attributeOverrides.put("example2", ImmutableList.of("something2"));
        headers.put(Constants.ATTRIBUTE_OVERRIDES_KEY, attributeOverrides);

        kind = StandardWatchEventKinds.ENTRY_CREATE;
        contentProducerDataAccessObject.createContentItem(mockFileSystemPersistenceProvider,
                mockEndpoint,
                testFile,
                kind,
                mimeType,
                headers);

        kind = StandardWatchEventKinds.ENTRY_MODIFY;
        contentProducerDataAccessObject.createContentItem(mockFileSystemPersistenceProvider,
                mockEndpoint,
                testFile,
                kind,
                mimeType,
                headers);
    }

    @Test(expected = ContentComponentException.class)
    public void testUpdateContentItemWithNoFilterBuilder() throws Exception {
        File testFile = temporaryFolder.newFile("testUpdateContentItemWithNoId.txt");

        //make sample set of keys
        Set<String> keys = ImmutableSet.of(String.valueOf(testFile.getAbsolutePath()
                .hashCode()));

        //setup mockFileSystemPersistenceProvider
        FileSystemPersistenceProvider mockFileSystemPersistenceProvider = mock(
                FileSystemPersistenceProvider.class);
        doReturn(keys).when(mockFileSystemPersistenceProvider)
                .loadAllKeys();
        doReturn("fakeId").when(mockFileSystemPersistenceProvider)
                .loadFromPersistence(any(String.class));

        //setup mockCatalogFramework
        CatalogFramework mockCatalogFramework = mock(CatalogFramework.class);

        //setup mockComponent
        ContentComponent mockComponent = mock(ContentComponent.class);
        doReturn(mockCatalogFramework).when(mockComponent)
                .getCatalogFramework();

        //setup mockEndpoint
        ContentEndpoint mockEndpoint = mock(ContentEndpoint.class);
        doReturn(mockComponent).when(mockEndpoint)
                .getComponent();

        WatchEvent.Kind<Path> kind;
        String mimeType = "txt";

        kind = StandardWatchEventKinds.ENTRY_MODIFY;
        contentProducerDataAccessObject.createContentItem(mockFileSystemPersistenceProvider,
                mockEndpoint,
                testFile,
                kind,
                mimeType,
                new HashedMap());
    }

    @Test(expected = ContentComponentException.class)
    public void testUpdateContentItemWithNoId() throws Exception {
        File testFile = temporaryFolder.newFile("testUpdateContentItemWithNoId.txt");

        //make sample set of keys
        Set<String> keys = ImmutableSet.of(String.valueOf(testFile.getAbsolutePath()
                .hashCode()));

        //setup mockFileSystemPersistenceProvider
        FileSystemPersistenceProvider mockFileSystemPersistenceProvider = mock(
                FileSystemPersistenceProvider.class);
        doReturn(keys).when(mockFileSystemPersistenceProvider)
                .loadAllKeys();
        doReturn("").when(mockFileSystemPersistenceProvider)
                .loadFromPersistence(any(String.class));

        WatchEvent.Kind<Path> kind;
        String mimeType = "txt";

        kind = StandardWatchEventKinds.ENTRY_MODIFY;
        contentProducerDataAccessObject.createContentItem(mockFileSystemPersistenceProvider,
                null,
                testFile,
                kind,
                mimeType,
                new HashedMap());
    }

    @Test
    public void testProcessHeaders() throws IOException {
        Map<String, Serializable> requestProperties = new HashMap<>();
        StorageRequest storageRequest = mock(StorageRequest.class);
        doReturn(requestProperties).when(storageRequest)
                .getProperties();

        Map<String, Object> camelHeaders = new HashMap<>();
        Map<String, Serializable> attributeOverrides = new HashMap<>();
        attributeOverrides.put("example", ImmutableList.of("something", "something1"));
        attributeOverrides.put("example2", ImmutableList.of("something2"));
        camelHeaders.put(Constants.ATTRIBUTE_OVERRIDES_KEY, attributeOverrides);

        contentProducerDataAccessObject.processHeaders(camelHeaders,
                storageRequest,
                Files.createTempFile("foobar", "txt")
                        .toFile());
        assertThat(requestProperties.containsKey(Constants.STORE_REFERENCE_KEY), is(false));
        assertThat(((Map<String, List<String>>) requestProperties.get(Constants.ATTRIBUTE_OVERRIDES_KEY)).get(
                "example"), is(Arrays.asList("something", "something1")));
        requestProperties.clear();
        camelHeaders.put(Constants.ATTRIBUTE_OVERRIDES_KEY, ImmutableMap.of());
        camelHeaders.put(Constants.STORE_REFERENCE_KEY, "true");
        contentProducerDataAccessObject.processHeaders(camelHeaders,
                storageRequest,
                Files.createTempFile("foobar", "txt")
                        .toFile());
        assertThat(requestProperties.containsKey(Constants.STORE_REFERENCE_KEY), is(true));
        assertThat(requestProperties.containsKey(Constants.ATTRIBUTE_OVERRIDES_KEY), is(false));
    }
}
