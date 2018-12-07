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
package ddf.catalog.cache.solr.impl;

import static ddf.catalog.cache.solr.impl.CachingFederationStrategy.CACHE_QUERY_MODE;
import static ddf.catalog.cache.solr.impl.CachingFederationStrategy.INDEX_QUERY_MODE;
import static ddf.catalog.cache.solr.impl.CachingFederationStrategy.NATIVE_QUERY_MODE;
import static ddf.catalog.cache.solr.impl.CachingFederationStrategy.QUERY_MODE;
import static ddf.catalog.cache.solr.impl.CachingFederationStrategy.UPDATE_QUERY_MODE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.UpdateImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreFederatedQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.Source;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorService;
import org.codice.solr.client.solrj.SolrClient;
import org.geotools.filter.NullFilterImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengis.filter.sort.SortBy;

@RunWith(MockitoJUnitRunner.class)
public class CachingFederationStrategyTest {

  private static final long LONG_TIMEOUT = 1000;

  private static final String MOCK_RESPONSE_TITLE = "mock response";

  private ExecutorService cacheExecutor, queryExecutor;

  @Mock private Query mockQuery;

  private CachingFederationStrategy strategy;

  private CachingFederationStrategy federateStrategy;

  @Mock private PreFederatedQueryPlugin preQueryPlugin;

  private MetacardImpl metacard;

  @Mock private SourceResponse mockResponse;

  @Mock private SortedQueryMonitorFactory mockSortedQueryMonitorFactory;

  @Mock private SortedQueryMonitor mockSortedQueryMonitor;

  @Mock private SolrClient mockClient;

  @Mock private CacheSolrMetacardClient mockMetacardClient;

  @Mock private SolrCache cache;

  private HashMap<String, Serializable> properties;

  private ArgumentCaptor<QueryRequestImpl> requestArgumentCaptor;

  @Mock private CacheBulkProcessor cacheBulkProcessor;

  @Mock private CacheCommitPhaser cacheCommitPhaser;

  private ArgumentCaptor<List<Result>> cacheArgs;

  @Before
  public void setup() throws Exception {
    cacheExecutor = MoreExecutors.newDirectExecutorService();
    queryExecutor = MoreExecutors.newDirectExecutorService();

    when(preQueryPlugin.process(any(), any()))
        .thenAnswer(invocation -> invocation.getArguments()[1]);

    cacheArgs = ArgumentCaptor.forClass((Class) List.class);

    strategy =
        new CachingFederationStrategy(
            queryExecutor,
            Arrays.asList(preQueryPlugin),
            new ArrayList<>(),
            cache,
            cacheExecutor,
            new CacheQueryFactory(new GeotoolsFilterBuilder()));

    federateStrategy =
        new CachingFederationStrategy(
            queryExecutor,
            Arrays.asList(preQueryPlugin),
            new ArrayList<>(),
            cache,
            cacheExecutor,
            new CacheQueryFactory(new GeotoolsFilterBuilder()));

    ArgumentCaptor<QueryResponseImpl> responseArgumentCaptor =
        ArgumentCaptor.forClass(QueryResponseImpl.class);
    requestArgumentCaptor = ArgumentCaptor.forClass(QueryRequestImpl.class);

    when(mockSortedQueryMonitorFactory.createMonitor(
            any(CompletionService.class),
            any(Map.class),
            responseArgumentCaptor.capture(),
            requestArgumentCaptor.capture(),
            any(List.class)))
        .thenReturn(mockSortedQueryMonitor);

    strategy.setSortedQueryMonitorFactory(mockSortedQueryMonitorFactory);
    strategy.setCacheCommitPhaser(cacheCommitPhaser);
    strategy.setCacheBulkProcessor(cacheBulkProcessor);

    when(mockQuery.getTimeoutMillis()).thenReturn(LONG_TIMEOUT);
    when(mockQuery.getPageSize()).thenReturn(-1);

    metacard = new MetacardImpl();
    metacard.setId("mock metacard");

    Result mockResult = new ResultImpl(metacard);

    List<Result> results = Arrays.asList(mockResult);
    when(mockResponse.getResults()).thenReturn(results);

    // Set general properties
    properties = new HashMap<>();
  }

  @After
  public void tearDownClass() throws Exception {
    strategy.shutdown();
  }

  @Test
  public void testFederateGetResults() throws Exception {
    // The incoming QueryRequest's siteNames are checked before CachingFederationStrategy is called,
    // so they don't need to be set here.
    // CachingFederationStrategy will return all the results from
    // the sourceList and never touch the QueryRequest's siteNames
    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, false, null, properties);

    Source mockSource1 = getMockSource();
    Source mockSource2 = getMockSource();
    Source mockSource3 = getMockSource();

    List<Source> sourceList = ImmutableList.of(mockSource1, mockSource2, mockSource3);

    QueryResponse federateResponse = federateStrategy.federate(sourceList, fedQueryRequest);

    assertThat(federateResponse.getResults().size(), is(sourceList.size()));
  }

  @Test
  public void testFederateGetEmptyResults() throws Exception {
    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, false, null, properties);

    List<Source> sourceList = ImmutableList.of();

    QueryResponse federateResponse = federateStrategy.federate(sourceList, fedQueryRequest);

    assertThat(federateResponse.getResults().size(), is(0));
  }

  @Test
  public void testFederateGetHits() throws Exception {
    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, true, null, null);

    Source mockSource1 = getMockSource();
    Source mockSource2 = getMockSource();
    Source mockSource3 = getMockSource();

    List<Source> sourceList = ImmutableList.of(mockSource1, mockSource2, mockSource3);

    long numHits = 2;

    when(mockResponse.getHits()).thenReturn(numHits);

    QueryResponse federateResponse = federateStrategy.federate(sourceList, fedQueryRequest);

    assertThat(federateResponse.getHits(), is(numHits * sourceList.size()));
  }

  @Test
  public void testFederateGetEmptyHits() throws Exception {
    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, false, null, properties);

    List<Source> sourceList = ImmutableList.of();

    long numHits = 5;

    QueryResponse federateResponse = federateStrategy.federate(sourceList, fedQueryRequest);

    assertThat(federateResponse.getHits(), is((long) 0));
  }

  @Test
  public void testFederateQueryCache() throws Exception {
    properties.put(QUERY_MODE, CACHE_QUERY_MODE);

    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, properties);

    Source mockSource = mock(Source.class);

    QueryResponse federateResponse = strategy.federate(Arrays.asList(mockSource), fedQueryRequest);

    assertThat(requestArgumentCaptor.getValue().getPropertyValue(QUERY_MODE), is(CACHE_QUERY_MODE));

    verify(cache).query(any(QueryRequest.class));
    verify(mockSource, times(0)).query(any(QueryRequest.class));

    verifyCacheNotUpdated();

    assertThat(
        federateResponse.getRequest().getQuery(), is(requestArgumentCaptor.getValue().getQuery()));
  }

  @Test
  public void testFederateQueryNoUpdateToCache() throws Exception {
    properties.put(QUERY_MODE, NATIVE_QUERY_MODE);

    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, properties);

    Source mockSource = mock(Source.class);
    when(mockSource.query(any(QueryRequest.class))).thenReturn(mockResponse);

    QueryResponse federateResponse = strategy.federate(Arrays.asList(mockSource), fedQueryRequest);

    assertThat(
        requestArgumentCaptor.getValue().getPropertyValue(QUERY_MODE), is(NATIVE_QUERY_MODE));

    verify(mockSource).query(any(QueryRequest.class));
    verify(cache, times(0)).query(any(QueryRequest.class));

    verifyCacheNotUpdated();

    assertThat(
        federateResponse.getRequest().getQuery(), is(requestArgumentCaptor.getValue().getQuery()));
  }

  @Test
  public void testFederateQueryUpdateCacheBlocking() throws Exception {
    properties.put(QUERY_MODE, INDEX_QUERY_MODE);

    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, properties);

    Source mockSource = mock(Source.class);
    when(mockSource.query(any(QueryRequest.class))).thenReturn(mockResponse);

    doNothing().when(cacheCommitPhaser).add(cacheArgs.capture());

    QueryResponse federateResponse = strategy.federate(Arrays.asList(mockSource), fedQueryRequest);

    assertThat(requestArgumentCaptor.getValue().getPropertyValue(QUERY_MODE), is(INDEX_QUERY_MODE));

    verify(mockSource).query(any(QueryRequest.class));
    verify(cache, times(0)).query(any(QueryRequest.class));

    // CacheCommitPhaser.add() is called
    verify(cacheCommitPhaser).add(cacheArgs.getValue());
    verifyCacheUpdated();

    assertThat(
        federateResponse.getRequest().getQuery(), is(requestArgumentCaptor.getValue().getQuery()));
  }

  @Test
  public void testFederateQueryUpdateCacheNotBlocking() throws Exception {
    properties.put(QUERY_MODE, UPDATE_QUERY_MODE);

    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, properties);

    Source mockSource = mock(Source.class);
    when(mockSource.query(any(QueryRequest.class))).thenReturn(mockResponse);

    doNothing().when(cacheBulkProcessor).add(cacheArgs.capture());

    QueryResponse federateResponse = strategy.federate(Arrays.asList(mockSource), fedQueryRequest);

    assertThat(
        requestArgumentCaptor.getValue().getPropertyValue(QUERY_MODE), is(UPDATE_QUERY_MODE));

    verify(mockSource).query(any(QueryRequest.class));
    verify(cache, times(0)).query(any(QueryRequest.class));

    // CacheBulkProcessor.add() is called
    verify(cacheBulkProcessor).add(cacheArgs.getValue());
    verifyCacheUpdated();

    assertThat(
        federateResponse.getRequest().getQuery(), is(requestArgumentCaptor.getValue().getQuery()));
  }

  @Test
  public void testFederateQueryWithOffsetHandler() throws Exception {
    properties.put(QUERY_MODE, NATIVE_QUERY_MODE);

    // startIndex and pageSize must be > 1
    Query mockQ =
        new QueryImpl(mock(NullFilterImpl.class), 2, 2, mock(SortBy.class), true, LONG_TIMEOUT);

    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQ, properties);

    List<Source> sources = new ArrayList<>();

    // Multiple sources needed for OffsetResultHandler to be used
    for (int i = 0; i < 2; i++) {
      Source mockSource = mock(Source.class);
      when(mockSource.getId()).thenReturn("mock source " + i);
      sources.add(mockSource);
    }

    strategy.federate(sources, fedQueryRequest);

    // Verify the sources were queried
    for (Source source : sources) {
      verify(source).query(any(QueryRequest.class));
    }
  }

  @Test
  public void testConnectedSources() throws Exception {
    Query mockQ =
        new QueryImpl(mock(NullFilterImpl.class), 2, 2, mock(SortBy.class), true, LONG_TIMEOUT);

    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQ, properties);

    List<Source> sources = new ArrayList<>();

    // Multiple sources needed for OffsetResultHandler to be used
    for (int i = 0; i < 2; i++) {
      Source mockSource = mock(Source.class);
      when(mockSource.getId()).thenReturn("mock source");
      sources.add(mockSource);
    }

    strategy.federate(sources, fedQueryRequest);

    // Make sure both sources get called even though they have the same name
    verify(sources.get(0), atLeastOnce()).query(any(QueryRequest.class));
    verify(sources.get(1), atLeastOnce()).query(any(QueryRequest.class));
  }

  @Test
  public void testStartIndexGreaterThanMaxStartIndex() throws Exception {
    // startIndex and pageSize must be > 1
    Query mockQ =
        new QueryImpl(
            mock(NullFilterImpl.class),
            CachingFederationStrategy.DEFAULT_MAX_START_INDEX + 5,
            1,
            mock(SortBy.class),
            true,
            LONG_TIMEOUT);

    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQ, properties);

    List<Source> sources = new ArrayList<>();

    // Multiple sources needed for OffsetResultHandler to be used
    for (int i = 0; i < 2; i++) {
      Source mockSource = mock(Source.class);
      when(mockSource.getId()).thenReturn("mock source " + i);
      sources.add(mockSource);
    }

    QueryResponse federateResponse = strategy.federate(sources, fedQueryRequest);

    assertThat(
        federateResponse.getRequest().getQuery().getStartIndex(),
        is(CachingFederationStrategy.DEFAULT_MAX_START_INDEX + 5));
    // Modified offset is set to 1
    assertThat(requestArgumentCaptor.getValue().getQuery().getStartIndex(), is(1));
  }

  @Test
  public void testOffsetResultHandler() throws Exception {
    QueryResponseImpl originalResults = mock(QueryResponseImpl.class);
    QueryResponseImpl offsetResultQueue = mock(QueryResponseImpl.class);
    int pageSize = 1, offset = 1;

    Result mockResult = mock(Result.class);
    MetacardImpl mockMetacard = mock(MetacardImpl.class);

    List<Result> results = new ArrayList<>();

    CachingFederationStrategy.OffsetResultHandler offsetResultHandler =
        new CachingFederationStrategy.OffsetResultHandler(
            originalResults, offsetResultQueue, pageSize, offset);

    when(originalResults.hasMoreResults()).thenReturn(true);
    when(mockResult.getMetacard()).thenReturn(mockMetacard);
    when(mockMetacard.getId()).thenReturn("mock metacard");
    when(originalResults.take()).thenReturn(mockResult);

    doAnswer(invocation -> results.add(mockResult))
        .when(offsetResultQueue)
        .addResult(any(Result.class), any(Boolean.class));

    offsetResultHandler.run();

    assertThat(results.size(), is(1));
    assertThat(results.get(0).getMetacard().getId(), is("mock metacard"));
    verify(offsetResultQueue, atLeastOnce()).addResult(any(Result.class), any(Boolean.class));
  }

  @Test
  public void testCatchPluginExecutionException() throws Exception {
    PreFederatedQueryPlugin mockPlug = mock(PreFederatedQueryPlugin.class);
    PreFederatedQueryPlugin mockPlug2 = mock(PreFederatedQueryPlugin.class);

    when(mockPlug.process(any(Source.class), any(QueryRequest.class)))
        .thenThrow(new PluginExecutionException());
    strategy =
        new CachingFederationStrategy(
            queryExecutor,
            Arrays.asList(mockPlug, mockPlug2),
            new ArrayList<>(),
            cache,
            cacheExecutor,
            new CacheQueryFactory(new GeotoolsFilterBuilder()));

    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, properties);

    strategy.federate(Arrays.asList(mock(Source.class)), fedQueryRequest);
    verify(mockPlug).process(any(Source.class), any(QueryRequest.class));
    verify(mockPlug2).process(any(Source.class), any(QueryRequest.class));
  }

  @Test
  public void testCatchStopProcessingException() throws Exception {
    PreFederatedQueryPlugin mockPlug = mock(PreFederatedQueryPlugin.class);
    PreFederatedQueryPlugin mockPlug2 = mock(PreFederatedQueryPlugin.class);

    when(mockPlug.process(any(Source.class), any(QueryRequest.class)))
        .thenThrow(new StopProcessingException("test exception"));

    strategy =
        new CachingFederationStrategy(
            queryExecutor,
            Arrays.asList(mockPlug, mockPlug2),
            new ArrayList<>(),
            cache,
            cacheExecutor,
            new CacheQueryFactory(new GeotoolsFilterBuilder()));

    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, properties);

    strategy.federate(Arrays.asList(mock(Source.class)), fedQueryRequest);
    // First plugin throws exception, so second plugin is untouched
    verify(mockPlug).process(any(Source.class), any(QueryRequest.class));
    verifyZeroInteractions(mockPlug2);
  }

  @Test
  public void testStartIndexLessThanZero() throws Exception {
    strategy.setMaxStartIndex(-5);
    assertThat(strategy.getMaxStartIndex(), is(CachingFederationStrategy.DEFAULT_MAX_START_INDEX));
  }

  @Test
  public void testStartIndexIsZero() throws Exception {
    strategy.setMaxStartIndex(0);
    assertThat(strategy.getMaxStartIndex(), is(CachingFederationStrategy.DEFAULT_MAX_START_INDEX));
  }

  @Test
  public void testStartIndexGreaterThanZero() throws Exception {
    strategy.setMaxStartIndex(5);
    assertThat(strategy.getMaxStartIndex(), is(5));
  }

  @Test
  public void testProcessUpdateResponse() throws Exception {
    Map<String, Serializable> testMap = new HashMap<>();
    testMap.put(Constants.SERVICE_TITLE, MOCK_RESPONSE_TITLE);
    UpdateResponse response = mock(UpdateResponseImpl.class);

    UpdateRequest request = mock(UpdateRequestImpl.class);

    when(request.hasProperties()).thenReturn(true);
    when(request.getProperties()).thenReturn(testMap);

    when(response.getRequest()).thenReturn(request);

    MetacardImpl newMetacard = mock(MetacardImpl.class);

    UpdateImpl updateImpl = mock(UpdateImpl.class);
    when(updateImpl.getNewMetacard()).thenReturn(newMetacard);

    List<Update> cards = Arrays.asList(updateImpl);

    ArgumentCaptor<List<Metacard>> metacardsCaptor = ArgumentCaptor.forClass((Class) List.class);

    when(response.getUpdatedMetacards()).thenReturn(cards);

    doNothing().when(cache).create(metacardsCaptor.capture());

    assertThat(response, is(strategy.process(response)));

    assertThat(metacardsCaptor.getValue().contains(newMetacard), is(true));
  }

  @Test
  public void testProcessUpdateResponseRequestNotLocal() throws Exception {
    Map<String, Serializable> testMap = new HashMap<>();
    testMap.put(Constants.SERVICE_TITLE, MOCK_RESPONSE_TITLE);
    testMap.put(Constants.LOCAL_DESTINATION_KEY, false);

    UpdateResponse response = mock(UpdateResponseImpl.class);
    UpdateRequest request = mock(UpdateRequestImpl.class);

    when(request.hasProperties()).thenReturn(true);
    when(request.getProperties()).thenReturn(testMap);

    when(response.getRequest()).thenReturn(request);

    assertThat(response, is(strategy.process(response)));
  }

  @Test
  public void testProcessUpdateResponseSolrServiceTitle() throws Exception {
    Map<String, Serializable> testMap = new HashMap<>();
    SolrCacheSource cacheSource =
        new SolrCacheSource(new SolrCache(mockClient, mockMetacardClient));

    testMap.put(Constants.SERVICE_TITLE, cacheSource.getId());

    UpdateResponse response = mock(UpdateResponseImpl.class);
    UpdateRequest request = mock(UpdateRequestImpl.class);

    when(request.hasProperties()).thenReturn(true);
    when(request.getProperties()).thenReturn(testMap);

    when(response.getRequest()).thenReturn(request);

    assertThat(response, is(strategy.process(response)));
  }

  @Test
  public void testProcessDeleteResponse() throws Exception {
    Map<String, Serializable> testMap = new HashMap<>();
    testMap.put(Constants.SERVICE_TITLE, MOCK_RESPONSE_TITLE);
    DeleteResponse response =
        new DeleteResponseImpl(mock(DeleteRequestImpl.class), testMap, new ArrayList<>());

    response = strategy.process(response);

    assertThat(response.getPropertyValue(Constants.SERVICE_TITLE), is(MOCK_RESPONSE_TITLE));
  }

  @Test
  public void testProcessDeleteResponseNotLocal() throws Exception {
    Map<String, Serializable> testMap = new HashMap<>();
    testMap.put(Constants.SERVICE_TITLE, MOCK_RESPONSE_TITLE);
    testMap.put(Constants.LOCAL_DESTINATION_KEY, false);

    DeleteResponse response = mock(DeleteResponse.class);
    DeleteRequest request = mock(DeleteRequest.class);

    when(request.hasProperties()).thenReturn(true);
    when(request.getProperties()).thenReturn(testMap);

    when(response.getRequest()).thenReturn(request);

    assertThat(response, is(strategy.process(response)));
  }

  @Test
  public void testProcessDeleteResponseSolrServiceTitle() throws Exception {
    Map<String, Serializable> testMap = new HashMap<>();
    SolrCacheSource cacheSource =
        new SolrCacheSource(new SolrCache(mockClient, mockMetacardClient));

    testMap.put(Constants.SERVICE_TITLE, cacheSource.getId());

    DeleteResponse response = mock(DeleteResponse.class);
    DeleteRequest request = mock(DeleteRequest.class);

    when(request.hasProperties()).thenReturn(true);
    when(request.getProperties()).thenReturn(testMap);

    when(response.getRequest()).thenReturn(request);

    assertThat(response, is(strategy.process(response)));
  }

  @Test
  public void testProcessCreateResponse() throws Exception {
    CreateResponse response = mock(CreateResponseImpl.class);
    assertThat(response, is(strategy.process(response)));
  }

  @Test(expected = NullPointerException.class)
  public void testNullQueryExecutorService() throws Exception {
    strategy =
        new CachingFederationStrategy(
            null,
            Arrays.asList(preQueryPlugin),
            new ArrayList<>(),
            cache,
            cacheExecutor,
            new CacheQueryFactory(new GeotoolsFilterBuilder()));
  }

  @Test(expected = NullPointerException.class)
  public void testNullCacheExecutorService() throws Exception {
    strategy =
        new CachingFederationStrategy(
            queryExecutor,
            Arrays.asList(preQueryPlugin),
            new ArrayList<>(),
            cache,
            null,
            new CacheQueryFactory(new GeotoolsFilterBuilder()));
  }

  @Test(expected = NullPointerException.class)
  public void testNullPreQueryPlugin() throws Exception {
    strategy =
        new CachingFederationStrategy(
            queryExecutor,
            null,
            new ArrayList<>(),
            cache,
            cacheExecutor,
            new CacheQueryFactory(new GeotoolsFilterBuilder()));
  }

  @Test(expected = NullPointerException.class)
  public void testNullPreQueryPluginListContents() throws Exception {
    strategy =
        new CachingFederationStrategy(
            queryExecutor,
            Arrays.asList(null),
            new ArrayList<>(),
            cache,
            cacheExecutor,
            new CacheQueryFactory(new GeotoolsFilterBuilder()));
  }

  @Test(expected = NullPointerException.class)
  public void testNullPostQueryPlugin() throws Exception {
    strategy =
        new CachingFederationStrategy(
            queryExecutor,
            Arrays.asList(preQueryPlugin),
            null,
            cache,
            cacheExecutor,
            new CacheQueryFactory(new GeotoolsFilterBuilder()));
  }

  @Test(expected = NullPointerException.class)
  public void testNullPostQueryPluginListContents() throws Exception {
    strategy =
        new CachingFederationStrategy(
            queryExecutor,
            Arrays.asList(preQueryPlugin),
            Arrays.asList(null),
            cache,
            cacheExecutor,
            new CacheQueryFactory(new GeotoolsFilterBuilder()));
  }

  @Test(expected = NullPointerException.class)
  public void testNullSolrCache() throws Exception {
    strategy =
        new CachingFederationStrategy(
            queryExecutor,
            Arrays.asList(preQueryPlugin),
            new ArrayList<>(),
            null,
            cacheExecutor,
            new CacheQueryFactory(new GeotoolsFilterBuilder()));
  }

  @Test(expected = NullPointerException.class)
  public void testNullCacheQueryFactory() throws Exception {
    strategy =
        new CachingFederationStrategy(
            queryExecutor,
            Arrays.asList(preQueryPlugin),
            new ArrayList<>(),
            cache,
            cacheExecutor,
            null);
  }

  @Test(expected = NullPointerException.class)
  public void testNullRequest() throws Exception {
    strategy.federate(new ArrayList<>(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidCollectionWithNullSource() throws Exception {
    List<Source> sources = new ArrayList<>();
    sources.add(null);
    QueryRequest fedQueryRequest = mock(QueryRequest.class);
    strategy.federate(sources, fedQueryRequest);
  }

  private Source getMockSource() throws UnsupportedQueryException {
    Source mockSource = mock(Source.class);
    when(mockSource.getId()).thenReturn(UUID.randomUUID().toString());

    when(mockSource.query(any(QueryRequest.class))).thenReturn(mockResponse);

    return mockSource;
  }

  private void verifyCacheUpdated() {
    for (Result result : cacheArgs.getValue()) {
      assertThat(result.getMetacard().getId(), is(metacard.getId()));
    }
  }

  private void verifyCacheNotUpdated() {
    // Cache is not updated
    verifyZeroInteractions(cacheBulkProcessor);
    verifyZeroInteractions(cacheCommitPhaser);
  }
}
