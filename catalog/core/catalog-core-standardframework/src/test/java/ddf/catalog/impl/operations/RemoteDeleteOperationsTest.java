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
package ddf.catalog.impl.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.cache.solr.impl.ValidationQueryFactory;
import ddf.catalog.content.impl.MockMemoryStorageProvider;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.impl.QueryResponsePostProcessor;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.util.impl.SourcePoller;
import ddf.mime.MimeTypeResolver;
import ddf.mime.mapper.MimeTypeMapperImpl;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import org.codice.ddf.catalog.transform.Transform;
import org.codice.ddf.catalog.transform.TransformResponse;
import org.junit.Before;
import org.junit.Test;

public class RemoteDeleteOperationsTest {
  DeleteOperations deleteOperations;

  DeleteRequest deleteRequest;

  DeleteResponse deleteResponse;

  List<String> fanoutTagBlacklist;

  RemoteDeleteOperations remoteDeleteOperations;

  OperationsCatalogStoreSupport opsCatStoreSupport;

  OperationsMetacardSupport opsMetacardSupport;

  OperationsSecuritySupport opsSecuritySupport;

  FrameworkProperties frameworkProperties;

  SourcePoller mockPoller;

  CatalogProvider provider;

  FederationStrategy mockFederationStrategy;

  ArrayList<PostIngestPlugin> postIngestPlugins;

  MockMemoryStorageProvider storageProvider;

  MimeTypeResolver mimeTypeResolver;

  Transform transform;

  PostResourcePlugin mockPostResourcePlugin;

  List<PostResourcePlugin> mockPostResourcePlugins;

  @Before
  public void setUp() throws Exception {
    setUpMocks();
    setUpDeleteRequest();
    setUpFrameworkProperties();
    setUpDeleteOperations();
    setUpBlacklist();
  }

  @Test
  public void testIsNotCatalogStoreRequestExpectsDeleteResponse() throws Exception {

    when(opsCatStoreSupport.isCatalogStoreRequest(deleteRequest)).thenReturn(false);
    remoteDeleteOperations.setOpsCatStoreSupport(opsCatStoreSupport);

    DeleteResponse resultDeleteResponse =
        remoteDeleteOperations.performRemoteDelete(deleteRequest, deleteResponse);

    assertEqualMetacards(
        "Assert return of identical deleteResponse",
        resultDeleteResponse.getDeletedMetacards(),
        deleteResponse.getDeletedMetacards());

    verify(opsCatStoreSupport).isCatalogStoreRequest(deleteRequest);
  }

  @Test
  public void testIsCatalogStoreRequestExpectsDeleteResponse() throws Exception {

    when(opsCatStoreSupport.isCatalogStoreRequest(deleteRequest)).thenReturn(true);
    remoteDeleteOperations.setOpsCatStoreSupport(opsCatStoreSupport);

    DeleteResponse resultDeleteResponse =
        remoteDeleteOperations.performRemoteDelete(deleteRequest, deleteResponse);

    assertEqualMetacards(
        "Assert return of identical deleteResponse",
        resultDeleteResponse.getDeletedMetacards(),
        deleteResponse.getDeletedMetacards());

    verify(opsCatStoreSupport).isCatalogStoreRequest(deleteRequest);
  }

  @Test
  public void testDeleteResponseNullExpectsEmptyResponse() throws Exception {

    when(opsCatStoreSupport.isCatalogStoreRequest(deleteRequest)).thenReturn(true);
    remoteDeleteOperations.setOpsCatStoreSupport(opsCatStoreSupport);

    deleteResponse = null;

    DeleteResponse resultDeleteResponse =
        remoteDeleteOperations.performRemoteDelete(deleteRequest, deleteResponse);

    assertThat(
        "Null request should return an empty response",
        resultDeleteResponse.getDeletedMetacards(),
        is(empty()));
    verify(opsCatStoreSupport).isCatalogStoreRequest(deleteRequest);
  }

  @Test
  public void testNonEmptyStoreNotAvailableExpectProcessingErrors() throws Exception {

    when(opsCatStoreSupport.isCatalogStoreRequest(deleteRequest)).thenReturn(true);
    remoteDeleteOperations.setOpsCatStoreSupport(opsCatStoreSupport);

    deleteResponse = null;

    CatalogStore catalogStoreMock = mock(CatalogStore.class);

    ArrayList<CatalogStore> stores = new ArrayList<>();
    stores.add(catalogStoreMock);

    when(opsCatStoreSupport.getCatalogStoresForRequest(any(), any())).thenReturn(stores);
    when(catalogStoreMock.isAvailable()).thenReturn(false);

    DeleteResponse resultDeleteResponse =
        remoteDeleteOperations.performRemoteDelete(deleteRequest, deleteResponse);
    assertThat(
        "Assert processing error occurs", resultDeleteResponse.getProcessingErrors().size() >= 1);

    verify(opsCatStoreSupport).isCatalogStoreRequest(deleteRequest);
    verify(opsCatStoreSupport).getCatalogStoresForRequest(any(), any());
    verify(catalogStoreMock).isAvailable();
  }

  @Test
  public void testNonEmptyStoreAvailableExpectsDeleteResponse() throws Exception {

    when(opsCatStoreSupport.isCatalogStoreRequest(deleteRequest)).thenReturn(true);
    remoteDeleteOperations.setOpsCatStoreSupport(opsCatStoreSupport);

    CatalogStore catalogStoreMock = mock(CatalogStore.class);

    ArrayList<CatalogStore> stores = new ArrayList<>();
    stores.add(catalogStoreMock);

    when(opsCatStoreSupport.getCatalogStoresForRequest(any(), any())).thenReturn(stores);
    when(catalogStoreMock.isAvailable()).thenReturn(true);
    when(catalogStoreMock.delete(any())).thenReturn(deleteResponse);

    DeleteResponse resultDeleteResponse =
        remoteDeleteOperations.performRemoteDelete(deleteRequest, deleteResponse);

    assertEqualMetacards(
        "Assert return of identical deleteResponse",
        resultDeleteResponse.getDeletedMetacards(),
        deleteResponse.getDeletedMetacards());

    verify(opsCatStoreSupport).isCatalogStoreRequest(deleteRequest);
    verify(opsCatStoreSupport).getCatalogStoresForRequest(any(), any());
    verify(catalogStoreMock).isAvailable();
    verify(catalogStoreMock).delete(any());
  }

  @Test
  public void testNonEmptyStoreAvailableExpectsCaughtIngestException() throws Exception {

    when(opsCatStoreSupport.isCatalogStoreRequest(deleteRequest)).thenReturn(true);
    remoteDeleteOperations.setOpsCatStoreSupport(opsCatStoreSupport);

    CatalogStore catalogStoreMock = mock(CatalogStore.class);

    ArrayList<CatalogStore> stores = new ArrayList<>();
    stores.add(catalogStoreMock);

    IngestException ingestException = new IngestException();

    when(opsCatStoreSupport.getCatalogStoresForRequest(any(), any())).thenReturn(stores);
    when(catalogStoreMock.isAvailable()).thenReturn(true);
    when(catalogStoreMock.delete(any())).thenThrow(ingestException);

    DeleteResponse resultDeleteResponse =
        remoteDeleteOperations.performRemoteDelete(deleteRequest, deleteResponse);
    assertThat(
        "Assert caught IngestException", resultDeleteResponse.getProcessingErrors().size() >= 1);

    verify(opsCatStoreSupport).isCatalogStoreRequest(deleteRequest);
    verify(opsCatStoreSupport).getCatalogStoresForRequest(any(), any());
    verify(catalogStoreMock).isAvailable();
    verify(catalogStoreMock).delete(any());
  }

  private void assertEqualMetacards(
      String reason, List<Metacard> metacardList1, List<Metacard> metacardList2) {

    List<String> idList1 = metacardList1.stream().map(Metacard::getId).collect(Collectors.toList());
    List<String> idList2 = metacardList2.stream().map(Metacard::getId).collect(Collectors.toList());
    List<String> titleList1 =
        metacardList1.stream().map(Metacard::getTitle).collect(Collectors.toList());
    List<String> titleList2 =
        metacardList2.stream().map(Metacard::getTitle).collect(Collectors.toList());

    assertThat(reason, idList1, containsInAnyOrder(idList2.toArray()));
    assertThat(reason, titleList1, containsInAnyOrder(titleList2.toArray()));
  }

  private void setUpFrameworkProperties() {
    frameworkProperties = new FrameworkProperties();
    frameworkProperties.setAccessPlugins(new ArrayList<>());
    frameworkProperties.setPolicyPlugins(new ArrayList<>());
    frameworkProperties.setSourcePoller(mockPoller);
    frameworkProperties.setCatalogProviders(Collections.singletonList((CatalogProvider) provider));
    frameworkProperties.setPostResource(mockPostResourcePlugins);
    frameworkProperties.setFederationStrategy(mockFederationStrategy);
    frameworkProperties.setFilterBuilder(new GeotoolsFilterBuilder());
    frameworkProperties.setPreIngest(new ArrayList<>());
    frameworkProperties.setPostIngest(postIngestPlugins);
    frameworkProperties.setPreQuery(new ArrayList<>());
    frameworkProperties.setPostQuery(new ArrayList<>());
    frameworkProperties.setPreResource(new ArrayList<>());
    frameworkProperties.setPostResource(new ArrayList<>());
    frameworkProperties.setQueryResponsePostProcessor(mock(QueryResponsePostProcessor.class));
    frameworkProperties.setStorageProviders(Collections.singletonList(storageProvider));
    frameworkProperties.setMimeTypeMapper(
        new MimeTypeMapperImpl(Collections.singletonList(mimeTypeResolver)));
    frameworkProperties.setTransform(transform);
    frameworkProperties.setValidationQueryFactory(
        new ValidationQueryFactory(new GeotoolsFilterAdapterImpl(), new GeotoolsFilterBuilder()));
  }

  private void setUpMocks()
      throws IOException, CatalogTransformerException, MetacardCreationException {
    String localProviderName = "ddf";

    mockPoller = mock(SourcePoller.class);
    when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

    provider = mock(CatalogProvider.class);
    when(provider.getId()).thenReturn(localProviderName);
    when(provider.isAvailable(isA(SourceMonitor.class))).thenReturn(true);
    when(provider.isAvailable()).thenReturn(true);

    mockPostResourcePlugin = mock(PostResourcePlugin.class);
    mockPostResourcePlugins = new ArrayList<PostResourcePlugin>();
    mockPostResourcePlugins.add(mockPostResourcePlugin);

    mockFederationStrategy = mock(FederationStrategy.class);

    postIngestPlugins = new ArrayList<>();

    storageProvider = new MockMemoryStorageProvider();
    mimeTypeResolver = mock(MimeTypeResolver.class);

    transform = mock(Transform.class);

    TransformResponse transformResponse = mock(TransformResponse.class);
    when(transformResponse.getParentMetacard()).thenReturn(Optional.of(new MetacardImpl()));

    when(transform.transform(
            any(MimeType.class),
            any(String.class),
            any(Supplier.class),
            any(String.class),
            any(File.class),
            any(String.class),
            any(Map.class)))
        .thenReturn(transformResponse);
  }

  private void setUpDeleteRequest() {
    MetacardImpl metacard = new MetacardImpl();
    ArrayList<Metacard> metacardList = new ArrayList<>();
    metacard.setId("Bob");
    metacard.setTitle("Bob's Title");
    metacardList.add(metacard);

    metacard = new MetacardImpl();
    metacard.setId("Bobbert");
    metacard.setTitle("Bobbert's Title");
    metacardList.add(metacard);

    deleteRequest = new DeleteRequestImpl(metacard.getId());
    deleteResponse = new DeleteResponseImpl(deleteRequest, new HashMap(), metacardList);
  }

  private void setUpBlacklist() {
    fanoutTagBlacklist = new ArrayList<>();
    fanoutTagBlacklist.add("");

    opsSecuritySupport = mock(OperationsSecuritySupport.class);

    opsMetacardSupport = mock(OperationsMetacardSupport.class);

    opsCatStoreSupport = mock(OperationsCatalogStoreSupport.class);

    remoteDeleteOperations =
        new RemoteDeleteOperations(frameworkProperties, opsMetacardSupport, opsCatStoreSupport);
  }

  private void setUpDeleteOperations() {
    SourceOperations sourceOperations = new SourceOperations(frameworkProperties);
    OperationsSecuritySupport opsSecurity = new OperationsSecuritySupport();
    OperationsMetacardSupport opsMetacard = new OperationsMetacardSupport(frameworkProperties);

    QueryOperations queryOperations =
        new QueryOperations(frameworkProperties, sourceOperations, opsSecurity, opsMetacard);

    OperationsCatalogStoreSupport opsCatStore =
        new OperationsCatalogStoreSupport(frameworkProperties, sourceOperations);

    deleteOperations =
        new DeleteOperations(
            frameworkProperties, queryOperations, sourceOperations, opsSecurity, opsMetacard);
  }
}
