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
package ddf.catalog.history;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.DeleteStorageRequest;
import ddf.catalog.content.operation.ReadStorageRequest;
import ddf.catalog.content.operation.ReadStorageResponse;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.operation.impl.DeleteStorageRequestImpl;
import ddf.catalog.core.versioning.DeletedMetacard;
import ddf.catalog.core.versioning.MetacardVersion;
import ddf.catalog.core.versioning.impl.DeletedMetacardImpl;
import ddf.catalog.core.versioning.impl.MetacardVersionImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.UpdateImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;
import ddf.security.SubjectIdentity;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.apache.shiro.subject.ExecutionException;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.codice.ddf.security.common.Security;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class HistorianTest {

  private static final String METACARD_ID = "METACARD_ID";

  private static final String DELETED_METACARD_ID = "DELETED_ID";

  private static final String VERSIONED_METACARD_ID = "VERSIONED_ID";

  private static final String RESOURCE_URI = "content:example.com";

  private static final String UPDATE_DESCRIPTION = "This is an updated description.";

  private CatalogProvider catalogProvider;

  private InMemoryStorageProvider storageProvider;

  private Historian historian;

  private UuidGenerator uuidGenerator;

  @Before
  public void setup() {
    historian = new Historian();

    uuidGenerator = mock(UuidGenerator.class);
    when(uuidGenerator.generateUuid()).thenReturn(UUID.randomUUID().toString());
    historian.setUuidGenerator(uuidGenerator);

    catalogProvider = mock(CatalogProvider.class);
    historian.setCatalogProviders(Collections.singletonList(catalogProvider));

    storageProvider = new InMemoryStorageProvider();
    historian.setStorageProviders(Collections.singletonList(storageProvider));

    historian.setFilterBuilder(new GeotoolsFilterBuilder());

    historian.setMetacardTypes(Collections.singletonList(MetacardImpl.BASIC_METACARD));

    SubjectIdentity subjectIdentity = mock(SubjectIdentity.class);
    when(subjectIdentity.getUniqueIdentifier(any())).thenReturn("test");
    historian.setSubjectIdentity(subjectIdentity);

    Security security = mock(Security.class);
    Subject subject = mock(MockSubject.class);
    when(subject.execute(any(Callable.class))).thenCallRealMethod();
    when(security.runAsAdmin(any(PrivilegedAction.class))).thenReturn(subject);
    historian.setSecurity(security);
  }

  @Test
  public void testUpdateResponseHistorianDisabled()
      throws SourceUnavailableException, IngestException {
    historian.setHistoryEnabled(false);
    historian.version(mock(UpdateResponse.class));

    verifyZeroInteractions(catalogProvider);
  }

  @Test
  public void testUpdateResponseSetSkipFlag() throws SourceUnavailableException, IngestException {
    Map<String, Serializable> properties = new HashMap<>();
    UpdateResponse updateResponse = createUpdateResponse(properties);

    historian.version(updateResponse);

    assertThat(properties, hasEntry(MetacardVersion.SKIP_VERSIONING, true));
  }

  @Test
  public void testUpdateResponseSkipProperty() throws SourceUnavailableException, IngestException {
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(MetacardVersion.SKIP_VERSIONING, true);

    UpdateResponse updateResponse = createUpdateResponse(properties);

    historian.version(updateResponse);
    verifyZeroInteractions(catalogProvider);
  }

  @Test(expected = NullPointerException.class)
  public void testUpdateResponse() throws Exception {
    UpdateResponse updateResponse = createUpdateResponse(null);
    List<Update> updateList = createUpdatedMetacardList();
    when(updateResponse.getUpdatedMetacards()).thenReturn(updateList);

    historian.version(updateResponse);
    ArgumentCaptor<CreateRequest> createRequest = ArgumentCaptor.forClass(CreateRequest.class);
    verify(catalogProvider).create(createRequest.capture());

    Metacard versionedMetacard = createRequest.getValue().getMetacards().get(0);
    assertThat(
        versionedMetacard.getAttribute(MetacardVersion.VERSION_OF_ID).getValue(),
        equalTo(METACARD_ID));
  }

  @Test
  public void testUpdateStorageResponseHistorianDisabled()
      throws SourceUnavailableException, IngestException, UnsupportedQueryException {
    historian.setHistoryEnabled(false);
    historian.version(
        mock(UpdateStorageRequest.class),
        mock(UpdateStorageResponse.class),
        mock(UpdateResponse.class));

    verifyZeroInteractions(catalogProvider);
  }

  @Test
  public void testUpdateStorageResponseSetSkipFlag()
      throws SourceUnavailableException, IngestException, StorageException,
          UnsupportedQueryException {
    Map<String, Serializable> storageRequestProperties = new HashMap<>();
    Map<String, Serializable> storageResponseProperties = new HashMap<>();

    // The metacard and updated metacard
    List<Metacard> metacards = getMetacardUpdatePair();

    // Parameters for historian
    UpdateStorageRequest storageRequest = mock(UpdateStorageRequest.class);
    when(storageRequest.getProperties()).thenReturn(storageRequestProperties);

    UpdateStorageResponse storageResponse = mock(UpdateStorageResponse.class);
    when(storageResponse.getProperties()).thenReturn(storageResponseProperties);

    UpdateResponse updateResponse = mock(UpdateResponse.class);
    Update update1 = mock(Update.class);
    when(update1.getOldMetacard()).thenReturn(metacards.get(0));
    when(updateResponse.getUpdatedMetacards()).thenReturn(ImmutableList.of(update1));

    storeMetacard(metacards.get(0));

    // send a request to update the metacard
    updateMetacard(storageRequest, storageResponse, metacards.get(1));
    storageProvider.update(storageRequest);

    mockQuery(metacards.get(1));
    historian.version(storageRequest, storageResponse, updateResponse);

    assertThat(storageRequestProperties, hasEntry(MetacardVersion.SKIP_VERSIONING, true));
    assertThat(storageResponseProperties, hasEntry(MetacardVersion.SKIP_VERSIONING, true));
  }

  @Test
  public void testUpdateStorageResponseSkipProperty()
      throws SourceUnavailableException, IngestException, UnsupportedQueryException {
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(MetacardVersion.SKIP_VERSIONING, true);

    UpdateStorageResponse updateStorageResponse = mock(UpdateStorageResponse.class);
    when(updateStorageResponse.getProperties()).thenReturn(properties);

    historian.version(
        mock(UpdateStorageRequest.class), updateStorageResponse, mock(UpdateResponse.class));
    verifyZeroInteractions(catalogProvider);
  }

  @Test
  public void testUpdateStorageResponse()
      throws UnsupportedQueryException, SourceUnavailableException, IngestException,
          URISyntaxException, StorageException {
    // The metacard and updated metacard
    List<Metacard> metacards = getMetacardUpdatePair();

    // Parameters for historian
    UpdateStorageRequest storageRequest = mock(UpdateStorageRequest.class);
    UpdateStorageResponse storageResponse = mock(UpdateStorageResponse.class);
    UpdateResponse updateResponse = mock(UpdateResponse.class);
    Update update1 = mock(Update.class);
    when(update1.getOldMetacard()).thenReturn(metacards.get(0));
    when(updateResponse.getUpdatedMetacards()).thenReturn(ImmutableList.of(update1));

    storeMetacard(metacards.get(0));

    // send a request to update the metacard
    updateMetacard(storageRequest, storageResponse, metacards.get(1));
    storageProvider.update(storageRequest);

    mockQuery(metacards.get(1));
    historian.version(storageRequest, storageResponse, updateResponse);

    // Verify that the metacard updated
    Metacard update = readMetacard();

    assertThat(update, equalTo(metacards.get(1)));
    assertThat(
        storageResponse.getUpdatedContentItems().get(0).getUri(), not(equalTo(RESOURCE_URI)));
  }

  @Test
  public void testUpdateStorageResponseWithOnlyQualifiedContentItems()
      throws UnsupportedQueryException, SourceUnavailableException, IngestException,
          URISyntaxException, StorageException {
    // Parameters for historian
    UpdateStorageResponse storageResponse = mock(UpdateStorageResponse.class);
    ContentItem hasQualifier = mock(ContentItem.class);
    when(hasQualifier.getQualifier()).thenReturn("some-qualifier");
    ContentItem anotherHasQualifier = mock(ContentItem.class);
    when(anotherHasQualifier.getQualifier()).thenReturn("another-qualifier");
    when(storageResponse.getUpdatedContentItems())
        .thenReturn(Arrays.asList(hasQualifier, anotherHasQualifier));

    historian.version(
        mock(UpdateStorageRequest.class), storageResponse, mock(UpdateResponse.class));
    verifyZeroInteractions(catalogProvider);
  }

  @Test
  public void testUpdateStorageResponseNoContentItems()
      throws StorageException, UnsupportedQueryException, SourceUnavailableException,
          IngestException, URISyntaxException {
    // The metacard and updated metacard
    List<Metacard> metacards = getMetacardUpdatePair();

    // Parameters for historian
    UpdateStorageRequest storageRequest = mock(UpdateStorageRequest.class);
    UpdateStorageResponse storageResponse = mock(UpdateStorageResponse.class);
    UpdateResponse updateResponse = mock(UpdateResponse.class);

    // send a request to update the metacard
    updateMetacard(storageRequest, storageResponse, metacards.get(1));

    mockQuery(metacards.get(1));
    historian.version(storageRequest, storageResponse, updateResponse);

    assertThat(storageProvider.storageMap.size(), equalTo(0));
  }

  @Test
  public void testDeleteResponseHistorianDisabled()
      throws SourceUnavailableException, IngestException, UnsupportedQueryException {
    historian.setHistoryEnabled(false);
    historian.version(mock(DeleteResponse.class));

    verifyZeroInteractions(catalogProvider);
  }

  @Test
  public void testDeleteResponseSetSkipFlag()
      throws SourceUnavailableException, IngestException, StorageException {
    Metacard metacard = getMetacardUpdatePair().get(0);
    storeMetacard(metacard);

    // Send a delete request
    DeleteStorageRequest deleteStorageRequest =
        new DeleteStorageRequestImpl(Collections.singletonList(metacard), new HashMap<>());
    storageProvider.delete(deleteStorageRequest);

    // Version delete request
    DeleteRequest deleteRequest = new DeleteRequestImpl("deleteRequest");
    DeleteResponse deleteResponse =
        new DeleteResponseImpl(deleteRequest, new HashMap<>(), Collections.singletonList(metacard));
    historian.version(deleteResponse);

    assertThat(deleteResponse.getProperties(), hasEntry(MetacardVersion.SKIP_VERSIONING, true));
  }

  @Test
  public void testDeleteResponseSkipProperty() throws SourceUnavailableException, IngestException {
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(MetacardVersion.SKIP_VERSIONING, true);

    DeleteResponse deleteResponse = mock(DeleteResponse.class);
    when(deleteResponse.getProperties()).thenReturn(properties);

    historian.version(deleteResponse);
    verifyZeroInteractions(catalogProvider);
  }

  @Test
  public void testDeleteResponse()
      throws SourceUnavailableException, IngestException, StorageException {
    Metacard metacard = getMetacardUpdatePair().get(0);
    storeMetacard(metacard);

    // Send a delete request
    DeleteStorageRequest deleteStorageRequest =
        new DeleteStorageRequestImpl(Collections.singletonList(metacard), new HashMap<>());
    storageProvider.delete(deleteStorageRequest);

    // Version delete request
    DeleteRequest deleteRequest = new DeleteRequestImpl("deleteRequest");
    DeleteResponse deleteResponse =
        new DeleteResponseImpl(deleteRequest, new HashMap<>(), Collections.singletonList(metacard));
    historian.version(deleteResponse);

    // Only the version metacard is left
    assertThat(storageProvider.storageMap.size(), equalTo(1));
  }

  @Test
  public void testDeleteResponseNoContentItems()
      throws SourceUnavailableException, IngestException, StorageException {
    Metacard metacard = getMetacardUpdatePair().get(0);
    DeleteRequest deleteRequest = new DeleteRequestImpl("deleteRequest");
    DeleteResponse deleteResponse =
        new DeleteResponseImpl(deleteRequest, new HashMap<>(), Collections.singletonList(metacard));

    historian.version(deleteResponse);
    assertThat(storageProvider.storageMap.size(), equalTo(0));
  }

  @Test
  public void testNullContentItemStorageReadRequest()
      throws StorageException, UnsupportedQueryException, SourceUnavailableException,
          IngestException, URISyntaxException {
    List<Metacard> metacards = getMetacardUpdatePair();

    // Parameters for historian
    UpdateStorageRequest storageRequest = mock(UpdateStorageRequest.class);
    UpdateStorageResponse storageResponse = mock(UpdateStorageResponse.class);
    UpdateResponse updateResponse = mock(UpdateResponse.class);

    // Make content item null
    storageProvider.storageMap.put(METACARD_ID, null);

    mockQuery(metacards.get(1));
    historian.version(storageRequest, storageResponse, updateResponse);

    // Verify that the content item DIDN't updated
    ReadStorageRequest request = mock(ReadStorageRequest.class);
    when(request.getResourceUri()).thenReturn(new URI(RESOURCE_URI));
    ContentItem item = storageProvider.read(request).getContentItem();

    assertThat(item, nullValue());
  }

  @Test
  public void testReadStorageException()
      throws StorageException, SourceUnavailableException, IngestException,
          UnsupportedQueryException, URISyntaxException {
    StorageProvider exceptionStorageProvider = mock(StorageProvider.class);
    when(exceptionStorageProvider.read(any())).thenThrow(StorageException.class);
    historian.setStorageProviders(Collections.singletonList(exceptionStorageProvider));

    Metacard metacard = getMetacardUpdatePair().get(0);

    // Parameters for historian
    UpdateStorageRequest storageRequest = mock(UpdateStorageRequest.class);
    UpdateStorageResponse storageResponse = mock(UpdateStorageResponse.class);
    UpdateResponse updateResponse = mock(UpdateResponse.class);

    // create the update request
    updateMetacard(storageRequest, storageResponse, metacard);

    mockQuery(metacard);
    historian.version(storageRequest, storageResponse, updateResponse);

    // Verify that it wasn't updated
    verify(catalogProvider, times(0)).create(anyObject());
  }

  @Test(expected = IngestException.class)
  public void testTryCommitStorageException()
      throws StorageException, UnsupportedQueryException, SourceUnavailableException,
          IngestException, URISyntaxException {
    List<Metacard> metacards = getMetacardUpdatePair();

    // Mock out a bad storage provider
    StorageProvider exceptionStorageProvider = mock(StorageProvider.class);
    doThrow(StorageException.class).when(exceptionStorageProvider).commit(any());

    ContentItem item = mock(ContentItem.class);
    when(item.getId()).thenReturn(METACARD_ID);
    when(item.getUri()).thenReturn(RESOURCE_URI);
    when(item.getMetacard()).thenReturn(metacards.get(0));

    ReadStorageResponse readStorageResponse = mock(ReadStorageResponse.class);
    when(readStorageResponse.getContentItem()).thenReturn(item);
    when(exceptionStorageProvider.read(any())).thenReturn(readStorageResponse);

    when(exceptionStorageProvider.create(any())).thenReturn(mock(CreateStorageResponse.class));

    historian.setStorageProviders(Collections.singletonList(exceptionStorageProvider));

    // Parameters for historian
    UpdateStorageRequest storageRequest = mock(UpdateStorageRequest.class);
    UpdateStorageResponse storageResponse = mock(UpdateStorageResponse.class);
    UpdateResponse updateResponse = mock(UpdateResponse.class);

    Update update1 = mock(Update.class);
    when(update1.getOldMetacard()).thenReturn(metacards.get(0));
    when(updateResponse.getUpdatedMetacards()).thenReturn(ImmutableList.of(update1));

    // send a request to update the metacard
    updateMetacard(storageRequest, storageResponse, metacards.get(1));

    mockQuery(metacards.get(1));
    historian.version(storageRequest, storageResponse, updateResponse);
    verify(exceptionStorageProvider).rollback(any(StorageRequest.class));
  }

  @Test(expected = IngestException.class)
  public void testRollbackFailed()
      throws StorageException, UnsupportedQueryException, SourceUnavailableException,
          IngestException {
    List<Metacard> metacards = getMetacardUpdatePair();

    // Mock out a bad storage provider
    StorageProvider exceptionStorageProvider = mock(StorageProvider.class);
    doThrow(StorageException.class).when(exceptionStorageProvider).commit(any());
    doThrow(StorageException.class).when(exceptionStorageProvider).rollback(any());

    ContentItem item = mock(ContentItem.class);
    when(item.getId()).thenReturn(METACARD_ID);
    when(item.getUri()).thenReturn(RESOURCE_URI);
    when(item.getMetacard()).thenReturn(metacards.get(0));

    ReadStorageResponse readStorageResponse = mock(ReadStorageResponse.class);
    when(readStorageResponse.getContentItem()).thenReturn(item);
    when(exceptionStorageProvider.read(any())).thenReturn(readStorageResponse);

    when(exceptionStorageProvider.create(any())).thenReturn(mock(CreateStorageResponse.class));

    historian.setStorageProviders(Collections.singletonList(exceptionStorageProvider));

    // Parameters for historian
    UpdateStorageRequest storageRequest = mock(UpdateStorageRequest.class);
    UpdateStorageResponse storageResponse = mock(UpdateStorageResponse.class);
    UpdateResponse updateResponse = mock(UpdateResponse.class);

    Update update1 = mock(Update.class);
    when(update1.getOldMetacard()).thenReturn(metacards.get(0));
    when(updateResponse.getUpdatedMetacards()).thenReturn(ImmutableList.of(update1));

    // send a request to update the metacard
    updateMetacard(storageRequest, storageResponse, metacards.get(1));

    mockQuery(metacards.get(1));
    historian.version(storageRequest, storageResponse, updateResponse);
    verify(exceptionStorageProvider).rollback(any(StorageRequest.class));
  }

  @Test
  public void testBadContentItemSize()
      throws StorageException, UnsupportedQueryException, SourceUnavailableException,
          IngestException, URISyntaxException, IOException {
    // The metacard and updated metacard
    List<Metacard> metacards = getMetacardUpdatePair();

    // Parameters for historian
    UpdateStorageRequest storageRequest = mock(UpdateStorageRequest.class);
    UpdateStorageResponse storageResponse = mock(UpdateStorageResponse.class);
    UpdateResponse updateResponse = mock(UpdateResponse.class);

    Update update1 = mock(Update.class);
    when(update1.getOldMetacard()).thenReturn(metacards.get(0));
    when(updateResponse.getUpdatedMetacards()).thenReturn(ImmutableList.of(update1));

    storeMetacard(metacards.get(0));

    // send a request to update the metacard
    updateMetacard(storageRequest, storageResponse, metacards.get(1));
    storageProvider.update(storageRequest);
    when(storageProvider.storageMap.get(RESOURCE_URI).getSize()).thenThrow(IOException.class);

    mockQuery(metacards.get(1));
    historian.version(storageRequest, storageResponse, updateResponse);

    // Verify that the metacard updated
    Metacard update = readMetacard();

    assertThat(update, equalTo(metacards.get(1)));
  }

  private UpdateResponse createUpdateResponse(Map<String, Serializable> responseProperties) {
    Map<String, Serializable> requestProperties = new HashMap<>();
    if (responseProperties == null) {
      responseProperties = new HashMap<>();
    }

    UpdateResponse updateResponse = mock(UpdateResponse.class);
    UpdateRequest request = mock(UpdateRequest.class);

    when(request.getProperties()).thenReturn(requestProperties);
    when(updateResponse.getRequest()).thenReturn(request);
    when(updateResponse.getProperties()).thenReturn(responseProperties);

    return updateResponse;
  }

  private List<Update> createUpdatedMetacardList() {
    List<Metacard> metacards = getMetacardUpdatePair();
    return Collections.singletonList(new UpdateImpl(metacards.get(1), metacards.get(0)));
  }

  private List<Metacard> getMetacardUpdatePair() {
    Metacard old = new MetacardImpl();
    old.setAttribute(new AttributeImpl(Metacard.ID, METACARD_ID));
    old.setAttribute(new AttributeImpl(Metacard.RESOURCE_URI, RESOURCE_URI));

    Metacard update = new MetacardImpl();
    update.setAttribute(new AttributeImpl(Metacard.ID, METACARD_ID));
    update.setAttribute(new AttributeImpl(Metacard.RESOURCE_URI, RESOURCE_URI));
    update.setAttribute(new AttributeImpl(Metacard.DESCRIPTION, UPDATE_DESCRIPTION));

    return Arrays.asList(old, update);
  }

  private void mockQuery(Metacard metacard) throws UnsupportedQueryException {
    SourceResponse sourceResponse = mock(SourceResponse.class);
    Result result = mock(Result.class);
    when(result.getMetacard()).thenReturn(metacard);
    Result noResult = mock(Result.class);

    Result deletedResult = mock(Result.class);
    DeletedMetacard deletedMetacard = mock(DeletedMetacard.class);
    when(deletedMetacard.getId()).thenReturn(DELETED_METACARD_ID);
    when(deletedMetacard.getMetacardType())
        .thenReturn(DeletedMetacardImpl.getDeletedMetacardType());
    when(deletedResult.getMetacard()).thenReturn(deletedMetacard);

    Result notVersionedResult = mock(Result.class);
    Metacard versionedMetacard = mock(MetacardVersion.class);
    when(versionedMetacard.getId()).thenReturn(VERSIONED_METACARD_ID);
    when(versionedMetacard.getMetacardType())
        .thenReturn(MetacardVersionImpl.getMetacardVersionType());
    when(notVersionedResult.getMetacard()).thenReturn(versionedMetacard);

    when(sourceResponse.getResults())
        .thenReturn(Arrays.asList(deletedResult, result, noResult, notVersionedResult));
    when(catalogProvider.query(any())).thenReturn(sourceResponse);
  }

  private void storeMetacard(Metacard metacard) {
    ContentItem item = mock(ContentItem.class);
    when(item.getId()).thenReturn(METACARD_ID);
    when(item.getUri()).thenReturn(RESOURCE_URI);
    when(item.getMetacard()).thenReturn(metacard);
    storageProvider.storageMap.put(item.getUri(), item);
  }

  private void updateMetacard(
      UpdateStorageRequest request, UpdateStorageResponse response, Metacard update) {
    ContentItem noMetacard = mock(ContentItem.class);
    ContentItem updatedItem = mock(ContentItem.class);
    ContentItem hasQualifier = mock(ContentItem.class);
    ContentItem emptyQualifier = mock(ContentItem.class);

    when(hasQualifier.getQualifier()).thenReturn("some-qualifier");
    when(emptyQualifier.getQualifier()).thenReturn("");

    when(updatedItem.getId()).thenReturn(METACARD_ID);
    when(updatedItem.getUri()).thenReturn(RESOURCE_URI);
    when(updatedItem.getMetacard()).thenReturn(update);

    when(request.getContentItems()).thenReturn(Collections.singletonList(updatedItem));
    when(response.getUpdatedContentItems())
        .thenReturn(Arrays.asList(noMetacard, updatedItem, hasQualifier, emptyQualifier));
  }

  private Metacard readMetacard() throws StorageException, URISyntaxException {
    ReadStorageRequest request = mock(ReadStorageRequest.class);
    when(request.getResourceUri()).thenReturn(new URI(RESOURCE_URI));
    return storageProvider.read(request).getContentItem().getMetacard();
  }

  private abstract class MockSubject implements Subject {
    public <V> V execute(Callable<V> callable) throws ExecutionException {
      try {
        return callable.call();
      } catch (Throwable t) {
        throw new ExecutionException(t);
      }
    }

    public void execute(Runnable runnable) {
      runnable.run();
    }
  }
}
