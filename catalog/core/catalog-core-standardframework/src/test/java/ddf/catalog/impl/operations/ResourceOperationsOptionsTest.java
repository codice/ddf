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

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.content.impl.MockMemoryStorageProvider;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.impl.QueryResponsePostProcessor;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.SourcePoller;
import ddf.mime.MimeTypeResolver;
import ddf.mime.MimeTypeToTransformerMapper;
import ddf.mime.mapper.MimeTypeMapperImpl;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;

public class ResourceOperationsOptionsTest {
  ResourceOperations resourceOperations;

  FrameworkProperties frameworkProperties;

  SourcePoller mockPoller;

  FederationStrategy mockFederationStrategy;

  ArrayList<PostIngestPlugin> postIngestPlugins;

  MockMemoryStorageProvider storageProvider;

  MimeTypeResolver mimeTypeResolver;

  MimeTypeToTransformerMapper mimeTypeToTransformerMapper;

  List<PostResourcePlugin> mockPostResourcePlugins;

  QueryResponse queryResponseMock;

  QueryOperations queryOperationsMock;

  MetacardImpl metacard;

  ArrayList<Result> mockResultList;

  Result resultMock;

  @Before
  public void setUp() throws Exception {
    setUpFrameworkProperties();
    setUpResourceOperationsAndMocks();
  }

  private void setUpResourceOperationsAndMocks() throws URISyntaxException {
    queryOperationsMock = mock(QueryOperations.class);
    queryResponseMock = mock(QueryResponse.class);
    resultMock = mock(Result.class);

    metacard = new MetacardImpl();
    metacard.setId("Bobbert");
    metacard.setTitle("Bobbert's Title");

    mockResultList = new ArrayList<>();

    resourceOperations = new ResourceOperations(frameworkProperties, queryOperationsMock, null);

    when(queryResponseMock.getResults()).thenReturn(mockResultList);
    when(resultMock.getMetacard()).thenReturn(metacard);
  }

  private void setUpFrameworkProperties() {
    frameworkProperties = new FrameworkProperties();
    frameworkProperties.setAccessPlugins(new ArrayList<>());
    frameworkProperties.setPolicyPlugins(new ArrayList<>());
    frameworkProperties.setSourcePoller(mockPoller);
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
    frameworkProperties.setMimeTypeToTransformerMapper(mimeTypeToTransformerMapper);
  }

  @Test
  public void testGetEnterpriseResourceOptions() throws Exception {

    mockResultList.add(resultMock);
    whenQueried().thenReturn(queryResponseMock);
    helperGetEnterpriseResourceOptions();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetEnterpriseResourceOptionsEmptyResults() throws Exception {

    whenQueried().thenReturn(queryResponseMock);
    helperGetEnterpriseResourceOptions();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetEnterpriseResourceOptionsCatchesUnsupportedQueryException() throws Exception {

    whenQueried().thenThrow(UnsupportedQueryException.class);
    helperGetEnterpriseResourceOptions();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetEnterpriseResourceOptionsCatchesFederationException() throws Exception {

    whenQueried().thenThrow(FederationException.class);
    helperGetEnterpriseResourceOptions();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetEnterpriseResourceOptionsCatchesIllegalArgumentException() throws Exception {

    whenQueried().thenThrow(IllegalArgumentException.class);
    helperGetEnterpriseResourceOptions();
  }

  @Test
  public void testGetResourceOptions() throws Exception {

    mockResultList.add(resultMock);
    whenQueried().thenReturn(queryResponseMock);
    helperGetResourceOptions();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetResourceOptionsEmptyResults() throws Exception {

    whenQueried().thenReturn(queryResponseMock);
    helperGetResourceOptions();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetResourceOptionsCatchesUnsupportedQueryException() throws Exception {

    whenQueried().thenThrow(UnsupportedQueryException.class);
    helperGetResourceOptions();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetResourceOptionsCatchesFederationException() throws Exception {

    whenQueried().thenThrow(FederationException.class);
    helperGetResourceOptions();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetResourceOptionsCatchesIllegalArgumentException() throws Exception {

    whenQueried().thenThrow(IllegalArgumentException.class);
    helperGetResourceOptions();
  }

  @Test
  public void testGetLocalResourceOptions() throws Exception {

    mockResultList.add(resultMock);
    whenQueried().thenReturn(queryResponseMock);
    helperGetLocalResourceOptions();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetLocalResourceOptionsEmptyResults() throws Exception {

    whenQueried().thenReturn(queryResponseMock);
    helperGetLocalResourceOptions();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetLocalResourceOptionsCatchesUnsupportedQueryException() throws Exception {

    whenQueried().thenThrow(UnsupportedQueryException.class);
    helperGetLocalResourceOptions();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetLocalResourceOptionsCatchesFederationException() throws Exception {

    whenQueried().thenThrow(FederationException.class);
    helperGetLocalResourceOptions();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetLocalResourceOptionsCatchesIllegalArgumentException() throws Exception {

    whenQueried().thenThrow(IllegalArgumentException.class);
    helperGetLocalResourceOptions();
  }

  protected void helperGetLocalResourceOptions()
      throws ResourceNotFoundException, UnsupportedQueryException, FederationException {
    resourceOperations.getLocalResourceOptions(metacard.getId(), false);
    verifyQueryOperations();
  }

  protected void helperGetResourceOptions()
      throws ResourceNotFoundException, UnsupportedQueryException, FederationException {
    resourceOperations.getResourceOptions(metacard.getId(), metacard.getSourceId(), false);
    verifyQueryOperations();
  }

  protected void helperGetEnterpriseResourceOptions()
      throws ResourceNotFoundException, UnsupportedQueryException, FederationException {
    resourceOperations.getEnterpriseResourceOptions(metacard.getId(), false);
    verifyQueryOperations();
  }

  protected OngoingStubbing<QueryResponse> whenQueried() throws Exception {
    return when(
        queryOperationsMock.query(
            nullable(QueryRequest.class),
            nullable(FederationStrategy.class),
            anyBoolean(),
            anyBoolean()));
  }

  private void verifyQueryOperations() throws FederationException, UnsupportedQueryException {
    verify(queryOperationsMock)
        .query(
            nullable(QueryRequest.class),
            nullable(FederationStrategy.class),
            anyBoolean(),
            anyBoolean());
  }
}
