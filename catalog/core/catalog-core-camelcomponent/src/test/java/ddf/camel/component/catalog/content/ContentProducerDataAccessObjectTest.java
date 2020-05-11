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
package ddf.camel.component.catalog.content;

import static ddf.camel.component.catalog.content.ContentProducerDataAccessObject.ENTRY_CREATE;
import static ddf.camel.component.catalog.content.ContentProducerDataAccessObject.ENTRY_DELETE;
import static ddf.camel.component.catalog.content.ContentProducerDataAccessObject.ENTRY_MODIFY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import ddf.catalog.CatalogFramework;
import ddf.catalog.Constants;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.source.SourceDescriptor;
import ddf.mime.MimeTypeMapper;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileMessage;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.map.HashedMap;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ContentProducerDataAccessObjectTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private ContentProducerDataAccessObject contentProducerDataAccessObject;

  @Before
  public void setUp() {
    UuidGenerator uuidGenerator = mock(UuidGenerator.class);
    when((uuidGenerator.generateUuid())).thenReturn(UUID.randomUUID().toString());
    contentProducerDataAccessObject = new ContentProducerDataAccessObject(uuidGenerator);
  }

  @Test
  public void testGetFileUsingRefKey() throws Exception {
    File testFile = temporaryFolder.newFile("testRefKey");
    GenericFileMessage<File> mockMessage = getMockMessage(testFile);

    // test with storeRefKey == false
    assertThat(
        contentProducerDataAccessObject.getFileUsingRefKey(false, mockMessage).equals(testFile),
        is(true));

    // test with storeRefKey == true
    assertThat(
        contentProducerDataAccessObject.getFileUsingRefKey(true, mockMessage).equals(testFile),
        is(true));
  }

  @Test
  public void testGetFileUsingRefKeyWithNullFile() throws Exception {
    GenericFileMessage<File> mockMessage = getMockMessage(null);
    assertThat(
        contentProducerDataAccessObject.getFileUsingRefKey(true, mockMessage), is(nullValue()));
  }

  @Test
  public void testGetFileWithoutRefKeyWithNullFile() throws Exception {
    GenericFileMessage<File> mockMessage = getMockMessage(null);
    assertThat(
        contentProducerDataAccessObject.getFileUsingRefKey(false, mockMessage), is(nullValue()));
  }

  @Test
  public void testProcessNullFileOnEntryCreate() throws Exception {
    Map<String, Object> headers = mock(Map.class);
    CatalogFramework mockCatalogFramework = mock(CatalogFramework.class);
    FileSystemPersistenceProvider mockFileSystemPersistenceProvider =
        mock(FileSystemPersistenceProvider.class);

    ContentComponent mockComponent = mock(ContentComponent.class);
    doReturn(mockCatalogFramework).when(mockComponent).getCatalogFramework();

    ContentEndpoint mockEndpoint = mock(ContentEndpoint.class);
    doReturn(mockComponent).when(mockEndpoint).getComponent();

    contentProducerDataAccessObject.createContentItem(
        mockFileSystemPersistenceProvider, mockEndpoint, null, ENTRY_CREATE, "", headers);

    verifyZeroInteractions(headers, mockCatalogFramework, mockComponent);
  }

  @Test
  public void testProcessNullFileOnEntryUpdate() throws Exception {
    Map<String, Object> headers = mock(Map.class);
    CatalogFramework mockCatalogFramework = mock(CatalogFramework.class);
    FileSystemPersistenceProvider mockFileSystemPersistenceProvider =
        mock(FileSystemPersistenceProvider.class);

    ContentComponent mockComponent = mock(ContentComponent.class);
    doReturn(mockCatalogFramework).when(mockComponent).getCatalogFramework();

    ContentEndpoint mockEndpoint = mock(ContentEndpoint.class);
    doReturn(mockComponent).when(mockEndpoint).getComponent();

    contentProducerDataAccessObject.createContentItem(
        mockFileSystemPersistenceProvider, mockEndpoint, null, ENTRY_MODIFY, "", headers);

    verifyZeroInteractions(headers, mockCatalogFramework, mockComponent);
  }

  @Test
  public void testProcessNullFileOnEntryDelete() throws Exception {
    Map<String, Object> headers = mock(Map.class);
    CatalogFramework mockCatalogFramework = mock(CatalogFramework.class);
    FileSystemPersistenceProvider mockFileSystemPersistenceProvider =
        mock(FileSystemPersistenceProvider.class);

    ContentComponent mockComponent = mock(ContentComponent.class);
    doReturn(mockCatalogFramework).when(mockComponent).getCatalogFramework();

    ContentEndpoint mockEndpoint = mock(ContentEndpoint.class);
    doReturn(mockComponent).when(mockEndpoint).getComponent();

    contentProducerDataAccessObject.createContentItem(
        mockFileSystemPersistenceProvider, mockEndpoint, null, ENTRY_DELETE, "", headers);

    verify(mockCatalogFramework, times(1)).delete(any(DeleteRequest.class));
  }

  @Test
  public void testGetEventType() throws Exception {
    File testFile = temporaryFolder.newFile("testEventType");
    GenericFileMessage<File> mockMessage = getMockMessage(testFile);

    // test that a new standard kind is created if storeRefKey == false
    assertThat(
        contentProducerDataAccessObject
            .getEventType(false, mockMessage)
            .equals(StandardWatchEventKinds.ENTRY_CREATE),
        is(true));

    // test that the sample kind is returned when storeRefKey == true
    assertThat(
        contentProducerDataAccessObject.getEventType(true, mockMessage).name().equals("example"),
        is(true));
  }

  private GenericFileMessage<File> getMockMessage(File testFile) {
    // set the mockGenericFile to return the test file when requested
    GenericFile<File> mockGenericFileFromBody = mock(GenericFile.class);
    doReturn(testFile).when(mockGenericFileFromBody).getFile();

    WatchEvent<Path> mockWatchEvent = mock(WatchEvent.class);

    // mock out the context that links to the file
    Path mockContext = mock(Path.class);
    doReturn(testFile).when(mockContext).toFile();

    // mock out with a sample kind
    WatchEvent.Kind mockKind = mock(WatchEvent.Kind.class);
    doReturn("example").when(mockKind).name();

    // return the kind or context for the mockWatchEvent
    doReturn(mockKind).when(mockWatchEvent).kind();
    doReturn(mockContext).when(mockWatchEvent).context();

    // return the mockWatchEvent when the file is called for
    GenericFile<File> mockGenericFile = mock(GenericFile.class);
    doReturn(mockWatchEvent).when(mockGenericFile).getFile();

    GenericFileMessage mockMessage = mock(GenericFileMessage.class);
    doReturn(mockGenericFileFromBody).when(mockMessage).getBody();
    doReturn(mockGenericFile).when(mockMessage).getGenericFile();

    return mockMessage;
  }

  @Test
  public void testGetMimeType() throws Exception {
    File testFile1 = temporaryFolder.newFile("testGetMimeType1.xml");
    File testFile2 = temporaryFolder.newFile("testGetMimeType2.txt");

    // mock out two different ways for getting mimeType
    MimeTypeMapper mockMimeTypeMapper = mock(MimeTypeMapper.class);
    doReturn("guess").when(mockMimeTypeMapper).guessMimeType(any(), any());
    doReturn("extension").when(mockMimeTypeMapper).getMimeTypeForFileExtension(any());

    // mock out Component that returns mockMimeTypeMapper when the mimeTypeMapper is requested
    ContentComponent mockComponent = mock(ContentComponent.class);
    doReturn(mockMimeTypeMapper).when(mockComponent).getMimeTypeMapper();

    // mock out ContentEndpoint that returns the mockComponent
    ContentEndpoint mockEndpoint = mock(ContentEndpoint.class);
    doReturn(mockComponent).when(mockEndpoint).getComponent();

    // assert the mock mimeTypeMappers are reached if/not xml
    assertThat(
        contentProducerDataAccessObject.getMimeType(mockEndpoint, testFile1).equals("guess"),
        is(true));
    assertThat(
        contentProducerDataAccessObject.getMimeType(mockEndpoint, testFile2).equals("extension"),
        is(true));
  }

  @Test
  public void testGetMimeTypeWithNullFile() throws Exception {
    assertThat(
        contentProducerDataAccessObject.getMimeType(mock(ContentEndpoint.class), null),
        is(nullValue()));
  }

  @Test
  public void testCreateContentItem() throws Exception {
    File testFile = temporaryFolder.newFile("testCreateContentItem.txt");

    // make sample list of metacard and set of keys
    List<MetacardImpl> metacardList = ImmutableList.of(new MetacardImpl());
    String uri = testFile.toURI().toASCIIString();
    Set<String> keys =
        new HashSet<>(Collections.singletonList(String.valueOf(DigestUtils.sha1Hex(uri))));

    // mock out responses for create, delete, update
    CreateResponse mockCreateResponse = mock(CreateResponse.class);
    doReturn(metacardList).when(mockCreateResponse).getCreatedMetacards();

    DeleteResponse mockDeleteResponse = mock(DeleteResponse.class);
    doReturn(metacardList).when(mockDeleteResponse).getDeletedMetacards();

    UpdateResponse mockUpdateResponse = mock(UpdateResponse.class);
    doReturn(metacardList).when(mockUpdateResponse).getUpdatedMetacards();

    // setup mockFileSystemPersistenceProvider
    FileSystemPersistenceProvider mockFileSystemPersistenceProvider =
        mock(FileSystemPersistenceProvider.class);
    doReturn(keys).when(mockFileSystemPersistenceProvider).loadAllKeys();
    doAnswer(invocationOnMock -> keys.remove(invocationOnMock.getArguments()[0]))
        .when(mockFileSystemPersistenceProvider)
        .delete(anyString());
    doReturn("sample")
        .when(mockFileSystemPersistenceProvider)
        .loadFromPersistence(any(String.class));

    // setup mockCatalogFramework
    CatalogFramework mockCatalogFramework = mock(CatalogFramework.class);
    doReturn(mockCreateResponse).when(mockCatalogFramework).create(any(CreateStorageRequest.class));
    doReturn(mockDeleteResponse).when(mockCatalogFramework).delete(any(DeleteRequest.class));

    // setup mockSourceInfo
    SourceInfoResponse mockSourceInfoResponse = mock(SourceInfoResponse.class);
    SourceDescriptor mockSourceDescriptor = mock(SourceDescriptor.class);

    when(mockSourceDescriptor.isAvailable()).thenReturn(true);
    when(mockSourceInfoResponse.getSourceInfo())
        .thenReturn(Collections.singleton(mockSourceDescriptor));
    when(mockCatalogFramework.getSourceInfo(any(SourceInfoRequest.class)))
        .thenReturn(mockSourceInfoResponse);

    // setup mockComponent
    ContentComponent mockComponent = mock(ContentComponent.class);
    doReturn(mockCatalogFramework).when(mockComponent).getCatalogFramework();

    // setup mockEndpoint
    ContentEndpoint mockEndpoint = mock(ContentEndpoint.class);
    doReturn(mockComponent).when(mockEndpoint).getComponent();

    WatchEvent.Kind<Path> kind;

    String mimeType = "txt";

    Map<String, Object> headers = new HashedMap();
    Map<String, Serializable> attributeOverrides = new HashMap<>();
    attributeOverrides.put("example", ImmutableList.of("something", "something1"));
    attributeOverrides.put("example2", ImmutableList.of("something2"));
    headers.put(Constants.ATTRIBUTE_OVERRIDES_KEY, attributeOverrides);
    headers.put(Constants.STORE_REFERENCE_KEY, uri);

    kind = StandardWatchEventKinds.ENTRY_CREATE;
    contentProducerDataAccessObject.createContentItem(
        mockFileSystemPersistenceProvider, mockEndpoint, testFile, kind, mimeType, headers);
    verify(mockCatalogFramework).create(any(CreateStorageRequest.class));

    kind = StandardWatchEventKinds.ENTRY_MODIFY;
    contentProducerDataAccessObject.createContentItem(
        mockFileSystemPersistenceProvider, mockEndpoint, testFile, kind, mimeType, headers);
    verify(mockCatalogFramework).update(any(UpdateStorageRequest.class));

    kind = StandardWatchEventKinds.ENTRY_DELETE;
    contentProducerDataAccessObject.createContentItem(
        mockFileSystemPersistenceProvider, mockEndpoint, testFile, kind, mimeType, headers);
    verify(mockCatalogFramework).delete(any(DeleteRequest.class));

    contentProducerDataAccessObject.createContentItem(
        mockFileSystemPersistenceProvider, mockEndpoint, testFile, kind, mimeType, headers);
    verify(mockCatalogFramework).delete(any(DeleteRequest.class));
  }

  @Test
  public void testPropertiesAreMutable() {
    HashMap<String, Serializable> properties =
        contentProducerDataAccessObject.getProperties(new HashMap<>());
    properties.put("test", "test");
    assertThat(properties.get("test"), is("test"));
  }
}
