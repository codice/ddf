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
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.content.impl.MockMemoryStorageProvider;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.impl.QueryResponsePostProcessor;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreAuthorizationPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.resource.DataUsageLimitExceededException;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.download.ReliableResourceDownloadManager;
import ddf.catalog.resourceretriever.ResourceRetriever;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.SourcePoller;
import ddf.mime.MimeTypeResolver;
import ddf.mime.MimeTypeToTransformerMapper;
import ddf.mime.mapper.MimeTypeMapperImpl;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codice.ddf.catalog.resource.download.DownloadException;
import org.junit.Before;
import org.junit.Test;

public class ResourceOperationsTest {
  ResourceOperations resourceOperations;

  FrameworkProperties frameworkProperties;

  SourcePoller mockPoller;

  FederationStrategy mockFederationStrategy;

  ArrayList<PostIngestPlugin> postIngestPlugins;

  MockMemoryStorageProvider storageProvider;

  MimeTypeResolver mimeTypeResolver;

  MimeTypeToTransformerMapper mimeTypeToTransformerMapper;

  List<PostResourcePlugin> mockPostResourcePlugins;

  List<PreAuthorizationPlugin> mockPreAuthorizationPlugins = new ArrayList<>();

  ResourceRequest resourceRequestMock;

  QueryResponse queryResponseMock;

  QueryOperations queryOperationsMock;

  URI testUri;

  Map<String, Serializable> requestPropertiesMock;

  MetacardImpl metacard;

  QueryRequest queryRequestMock;

  ReliableResourceDownloadManager reliableResourceDownloadManagerMock;

  ResourceResponse resourceResponseMock;

  Result resultMock;

  boolean isEnterprise;

  boolean fanoutEnabled;

  String resourceName;

  @Before
  public void setUp() throws Exception {
    setUpFrameworkProperties();
    setUpResourceOperationsAndMocks();
  }

  private void setUpResourceOperationsAndMocks() throws URISyntaxException {
    queryOperationsMock = mock(QueryOperations.class);
    queryResponseMock = mock(QueryResponse.class);
    queryRequestMock = mock(QueryRequest.class);
    resourceRequestMock = mock(ResourceRequest.class);
    reliableResourceDownloadManagerMock = mock(ReliableResourceDownloadManager.class);
    resourceResponseMock = mock(ResourceResponse.class);
    resultMock = mock(Result.class);

    testUri = new URI("bobUri", "test", "fragment");

    requestPropertiesMock = mock(Map.class);

    metacard = new MetacardImpl();
    metacard.setId("Bobbert");
    metacard.setTitle("Bobbert's Title");
    metacard.setResourceURI(testUri);

    resourceOperations = new ResourceOperations(frameworkProperties, queryOperationsMock, null);

    isEnterprise = true;
    fanoutEnabled = true;
    resourceName = "bob";
  }

  private void setUpFrameworkProperties() {
    frameworkProperties = new FrameworkProperties();
    frameworkProperties.setAccessPlugins(new ArrayList<>());
    frameworkProperties.setPolicyPlugins(new ArrayList<>());
    frameworkProperties.setSourcePoller(mockPoller);
    frameworkProperties.setPreAuthorizationPlugins(mockPreAuthorizationPlugins);
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
  public void testGetResource() throws Exception {

    ArrayList<Result> mockResultList = new ArrayList<>();
    mockResultList.add(resultMock);

    setGetResourceMocks();
    when(queryResponseMock.getResults()).thenReturn(mockResultList);
    when(queryResponseMock.getResults().get(0).getMetacard()).thenReturn(metacard);

    when(reliableResourceDownloadManagerMock.download(
            nullable(ResourceRequest.class),
            nullable(MetacardImpl.class),
            nullable(ResourceRetriever.class)))
        .thenReturn(resourceResponseMock);

    Resource resourceMock = mock(Resource.class);
    when(resourceResponseMock.getResource()).thenReturn(resourceMock);

    frameworkProperties.setReliableResourceDownloadManager(reliableResourceDownloadManagerMock);

    resourceOperations.getResource(resourceRequestMock, isEnterprise, resourceName, fanoutEnabled);

    verify(resourceResponseMock).getResource();
  }

  @Test
  public void testValidateFixGetResourceReturnsResourceResponse() throws Exception {

    ArrayList<Result> mockResultList = new ArrayList<>();
    mockResultList.add(resultMock);

    setGetResourceMocks();
    when(queryResponseMock.getResults()).thenReturn(mockResultList);
    when(queryResponseMock.getResults().get(0).getMetacard()).thenReturn(metacard);

    when(reliableResourceDownloadManagerMock.download(
            nullable(ResourceRequest.class),
            nullable(MetacardImpl.class),
            nullable(ResourceRetriever.class)))
        .thenReturn(resourceResponseMock);

    Resource resourceMock = mock(Resource.class);
    when(resourceResponseMock.getResource()).thenReturn(resourceMock);

    frameworkProperties.setReliableResourceDownloadManager(reliableResourceDownloadManagerMock);

    resourceOperations.getResource(resourceRequestMock, isEnterprise, resourceName, fanoutEnabled);
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testValidateFixGetResourceReturnsResourceResponseThrowsResourceNotFoundException()
      throws Exception {

    ArrayList<Result> mockResultList = new ArrayList<>();
    mockResultList.add(resultMock);

    setGetResourceMocks();
    when(queryResponseMock.getResults()).thenReturn(mockResultList);
    when(queryResponseMock.getResults().get(0).getMetacard()).thenReturn(metacard);

    when(reliableResourceDownloadManagerMock.download(
            nullable(ResourceRequest.class),
            nullable(MetacardImpl.class),
            nullable(ResourceRetriever.class)))
        .thenReturn(resourceResponseMock);

    frameworkProperties.setReliableResourceDownloadManager(reliableResourceDownloadManagerMock);

    resourceOperations.getResource(resourceRequestMock, isEnterprise, resourceName, fanoutEnabled);

    verify(queryResponseMock).getResults();
    verify(reliableResourceDownloadManagerMock)
        .download(
            nullable(ResourceRequest.class),
            nullable(MetacardImpl.class),
            nullable(ResourceRetriever.class));
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testValidateFixGetResourceThrowsResourceNotFoundException() throws Exception {

    ArrayList<Result> mockResultList = new ArrayList<>();
    mockResultList.add(resultMock);

    setGetResourceMocks();

    when(queryResponseMock.getResults()).thenReturn(mockResultList);
    when(queryResponseMock.getResults().get(0).getMetacard()).thenReturn(metacard);

    when(reliableResourceDownloadManagerMock.download(
            nullable(ResourceRequest.class),
            nullable(MetacardImpl.class),
            nullable(ResourceRetriever.class)))
        .thenReturn(resourceResponseMock);

    frameworkProperties.setReliableResourceDownloadManager(reliableResourceDownloadManagerMock);

    resourceOperations.getResource(resourceRequestMock, isEnterprise, resourceName, fanoutEnabled);

    verify(queryResponseMock).getResults();
    verify(reliableResourceDownloadManagerMock)
        .download(
            nullable(ResourceRequest.class),
            nullable(MetacardImpl.class),
            nullable(ResourceRetriever.class));
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetResourceThrowsDownloadException() throws Exception {

    ArrayList<Result> mockResultList = new ArrayList<>();
    mockResultList.add(resultMock);

    setGetResourceMocks();

    when(queryResponseMock.getResults()).thenReturn(mockResultList);
    when(queryResponseMock.getResults().get(0).getMetacard()).thenReturn(metacard);

    when(reliableResourceDownloadManagerMock.download(
            nullable(ResourceRequest.class),
            nullable(MetacardImpl.class),
            nullable(ResourceRetriever.class)))
        .thenThrow(DownloadException.class);

    Resource resourceMock = mock(Resource.class);
    when(resourceResponseMock.getResource()).thenReturn(resourceMock);

    frameworkProperties.setReliableResourceDownloadManager(reliableResourceDownloadManagerMock);

    resourceOperations.getResource(resourceRequestMock, isEnterprise, resourceName, fanoutEnabled);

    verify(queryResponseMock).getResults();
    verify(reliableResourceDownloadManagerMock)
        .download(
            nullable(ResourceRequest.class),
            nullable(MetacardImpl.class),
            nullable(ResourceRetriever.class));
  }

  @Test(expected = DataUsageLimitExceededException.class)
  public void testGetResourceCatchesDataUsageLimitExceededException() throws Exception {

    when(resourceRequestMock.getProperties()).thenThrow(DataUsageLimitExceededException.class);
    getResourceRequestAttributeUris();

    resourceOperations.getResource(resourceRequestMock, isEnterprise, resourceName, fanoutEnabled);

    verifyResourceRequestAttributes();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetResourceCatchesRuntimeException() throws Exception {

    when(resourceRequestMock.getProperties()).thenThrow(RuntimeException.class);
    getResourceRequestAttributeUris();

    resourceOperations.getResource(resourceRequestMock, isEnterprise, resourceName, fanoutEnabled);

    verifyResourceRequestAttributes();
  }

  @Test(expected = ResourceNotSupportedException.class)
  public void testGetResourceCatchesStopProcessingException() throws Exception {
    try {
      PreAuthorizationPlugin preAuthorizationPlugin = mock(PreAuthorizationPlugin.class);
      mockPreAuthorizationPlugins.add(preAuthorizationPlugin);
      when(preAuthorizationPlugin.processPreResource(nullable(ResourceRequest.class)))
          .thenThrow(StopProcessingException.class);
      getResourceRequestAttributeUris();

      resourceOperations.getResource(
          resourceRequestMock, isEnterprise, resourceName, fanoutEnabled);

      verifyResourceRequestAttributes();
    } finally {
      mockPreAuthorizationPlugins.clear();
    }
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetResourceNullSourceNameNotEnterpriseThrowsResourceNotFoundException()
      throws Exception {

    boolean isEnterprise = false;
    boolean fanoutEnabled = false;
    String resourceName = null;

    resourceOperations.getResource(resourceRequestMock, isEnterprise, resourceName, fanoutEnabled);

    verifyGetResourceMocks();
  }

  @Test(expected = ResourceNotSupportedException.class)
  public void testGetResourceInfoThrowsResourceNotSupportedException() throws Exception {

    when(resourceRequestMock.getAttributeValue()).thenReturn("bob");
    when(resourceRequestMock.getAttributeName()).thenReturn("bob");

    resourceOperations.getResource(resourceRequestMock, isEnterprise, resourceName, fanoutEnabled);

    verify(resourceRequestMock).getAttributeValue();
    verify(resourceRequestMock).getAttributeName();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testGetResourceInfoUriNotBlank() throws Exception {

    getResourceRequestAttributeUris();

    resourceOperations.getResource(resourceRequestMock, isEnterprise, resourceName, fanoutEnabled);

    verify(resourceRequestMock).getAttributeValue();
    verify(resourceRequestMock).getAttributeName();
  }

  @Test
  public void testGetResourceInfoWithFragment() throws Exception {
    when(resourceRequestMock.getAttributeValue()).thenReturn(testUri);
    when(resourceRequestMock.getAttributeName()).thenReturn("resource-uri");
    StringBuilder federatedSite = new StringBuilder();
    Map<String, Serializable> requestProperties = new HashMap<>();
    ArrayList<Result> mockResultList = new ArrayList<>();
    mockResultList.add(resultMock);
    when(resultMock.getMetacard()).thenReturn(metacard);
    when(queryOperationsMock.query(
            nullable(QueryRequest.class),
            nullable(FederationStrategy.class),
            anyBoolean(),
            anyBoolean()))
        .thenReturn(queryResponseMock);
    when(queryResponseMock.getResults()).thenReturn(mockResultList);

    ResourceOperations resourceOperationsSpy = spy(resourceOperations);
    resourceOperationsSpy.getResourceInfo(
        resourceRequestMock, "site", isEnterprise, federatedSite, requestProperties, false);
    verify(resourceOperationsSpy)
        .createPropertyStartsWithQuery(Metacard.RESOURCE_URI, "bobUri:test");
  }

  @Test
  public void testGetResourceInfoWithEscapedCharacters() throws Exception {
    URI uri =
        new URI(
            "scheme:///"
                + URLEncoder.encode("part1?file://some/pathto/file", "UTF-8")
                + "#fragment");
    when(resourceRequestMock.getAttributeValue()).thenReturn(uri);
    when(resourceRequestMock.getAttributeName()).thenReturn("resource-uri");
    StringBuilder federatedSite = new StringBuilder();
    Map<String, Serializable> requestProperties = new HashMap<>();
    ArrayList<Result> mockResultList = new ArrayList<>();
    mockResultList.add(resultMock);
    when(resultMock.getMetacard()).thenReturn(metacard);
    when(queryOperationsMock.query(
            nullable(QueryRequest.class),
            nullable(FederationStrategy.class),
            anyBoolean(),
            anyBoolean()))
        .thenReturn(queryResponseMock);
    when(queryResponseMock.getResults()).thenReturn(mockResultList);

    ResourceOperations resourceOperationsSpy = spy(resourceOperations);
    ResourceOperations.ResourceInfo resourceInfo =
        resourceOperationsSpy.getResourceInfo(
            resourceRequestMock, "site", isEnterprise, federatedSite, requestProperties, false);
    verify(resourceOperationsSpy)
        .createPropertyStartsWithQuery(
            Metacard.RESOURCE_URI,
            "scheme:///" + URLEncoder.encode("part1?file://some/pathto/file"));
    assertThat(resourceInfo.getResourceUri(), equalTo(uri));
  }

  @Test
  public void testGetResourceInfoWithFragmentAndMultipleResults() throws Exception {
    when(resourceRequestMock.getAttributeValue()).thenReturn(testUri);
    when(resourceRequestMock.getAttributeName()).thenReturn("resource-uri");
    StringBuilder federatedSite = new StringBuilder();
    Map<String, Serializable> requestProperties = new HashMap<>();
    Result mockResult1 = mock(Result.class);
    Result mockResult2 = mock(Result.class);
    ArrayList<Result> mockResultList = new ArrayList<>();
    mockResultList.add(mockResult1);
    mockResultList.add(mockResult2);
    MetacardImpl wrongMetacard = new MetacardImpl();
    wrongMetacard.setId("wrong");
    wrongMetacard.setTitle("wrong title");
    wrongMetacard.setResourceURI(new URI("bobUri", "test", "wrongfragment"));
    when(mockResult1.getMetacard()).thenReturn(wrongMetacard);
    when(mockResult2.getMetacard()).thenReturn(metacard);
    when(queryOperationsMock.query(
            nullable(QueryRequest.class),
            nullable(FederationStrategy.class),
            anyBoolean(),
            anyBoolean()))
        .thenReturn(queryResponseMock);
    when(queryResponseMock.getResults()).thenReturn(mockResultList);

    ResourceOperations resourceOperationsSpy = spy(resourceOperations);
    ResourceOperations.ResourceInfo resourceInfo =
        resourceOperationsSpy.getResourceInfo(
            resourceRequestMock, "site", isEnterprise, federatedSite, requestProperties, false);
    verify(resourceOperationsSpy)
        .createPropertyStartsWithQuery(Metacard.RESOURCE_URI, "bobUri:test");
    assertThat(resourceInfo.getResourceUri(), equalTo(testUri));
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testAnyTagIsNotEnterpriseNorSiteId() throws Exception {
    boolean isEnterprise = false;
    boolean fanoutEnabled = false;
    String resourceName = "";

    setGetResourceMocks();

    resourceOperations.getResource(resourceRequestMock, isEnterprise, resourceName, fanoutEnabled);

    verifyGetResourceMocks();
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testAnyTagReturnsQuery() throws Exception {
    boolean isEnterprise = true;
    boolean fanoutEnabled = false;
    String resourceName = "";

    helperGetResource();
  }

  private void helperGetResource()
      throws UnsupportedQueryException, FederationException, ResourceNotFoundException, IOException,
          ResourceNotSupportedException {

    setGetResourceMocks();

    resourceOperations.getResource(resourceRequestMock, isEnterprise, resourceName, fanoutEnabled);

    verifyGetResourceMocks();
  }

  private void setGetResourceMocks() throws FederationException, UnsupportedQueryException {
    getResourceRequestAttributeUris();
    when(queryOperationsMock.query(
            nullable(QueryRequest.class),
            nullable(FederationStrategy.class),
            anyBoolean(),
            anyBoolean()))
        .thenReturn(queryResponseMock);
  }

  private void verifyGetResourceMocks() throws FederationException, UnsupportedQueryException {
    verify(resourceRequestMock).getAttributeValue();
    verify(resourceRequestMock).getAttributeName();
    verify(queryOperationsMock)
        .query(
            nullable(QueryRequest.class),
            nullable(FederationStrategy.class),
            anyBoolean(),
            anyBoolean());
  }

  private void verifyResourceRequestAttributes() {
    verify(resourceRequestMock).getProperties();
    verify(resourceRequestMock).getAttributeValue();
    verify(resourceRequestMock).getAttributeName();
  }

  private void getResourceRequestAttributeUris() {
    when(resourceRequestMock.getAttributeValue()).thenReturn(testUri);
    when(resourceRequestMock.getAttributeName()).thenReturn("resource-uri");
  }
}
