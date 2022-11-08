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
package ddf.catalog.federation.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreFederatedQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.Source;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import org.geotools.filter.NullFilterImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengis.filter.sort.SortBy;

@RunWith(MockitoJUnitRunner.class)
public class SortedFederationStrategyTest {

  private static final long LONG_TIMEOUT = 1000;

  /** Constant to test contract of source latency prefix. */
  private static final String METRICS_SOURCE_ELAPSED_PREFIX = "metrics.source.elapsed.";

  private ExecutorService queryExecutor;

  @Mock private Query mockQuery;

  private SortedFederationStrategy strategy;

  @Mock private PreFederatedQueryPlugin preQueryPlugin;

  private MetacardImpl metacard;

  @Mock private SourceResponse mockResponse;

  @Mock private SortedQueryMonitor mockSortedQueryMonitor;

  private HashMap<String, Serializable> properties;

  private ArgumentCaptor<QueryRequestImpl> requestArgumentCaptor;

  ArgumentCaptor<QueryResponseImpl> responseArgumentCaptor;

  @Before
  public void setup() throws Exception {

    queryExecutor = MoreExecutors.newDirectExecutorService();

    when(preQueryPlugin.process(any(), any()))
        .thenAnswer(invocation -> invocation.getArguments()[1]);

    strategy =
        new SortedFederationStrategy(
            queryExecutor, Arrays.asList(preQueryPlugin), new ArrayList<>());

    responseArgumentCaptor = ArgumentCaptor.forClass(QueryResponseImpl.class);
    requestArgumentCaptor = ArgumentCaptor.forClass(QueryRequestImpl.class);

    when(mockQuery.getTimeoutMillis()).thenReturn(LONG_TIMEOUT);
    when(mockQuery.getPageSize()).thenReturn(-1);

    metacard = new MetacardImpl();
    metacard.setId("mock metacard");

    Result mockResult = new ResultImpl(metacard);

    List<Result> results = Collections.singletonList(mockResult);
    when(mockResponse.getResults()).thenReturn(results);

    // Set general properties
    properties = new HashMap<>();
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

    QueryResponse federateResponse = strategy.federate(sourceList, fedQueryRequest);

    assertThat(federateResponse.getResults().size(), is(sourceList.size()));
  }

  @Test
  public void testFederateLatencyMetrics() throws Exception {
    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, false, null, properties);

    when(mockResponse.getProperties()).thenReturn(properties);

    Source mockSource1 = getMockSource();
    Source mockSource2 = getMockSource();
    Source mockSource3 = getMockSource();

    List<Source> sourceList = ImmutableList.of(mockSource1, mockSource2, mockSource3);
    QueryResponse federateResponse = strategy.federate(sourceList, fedQueryRequest);

    List<Map.Entry<String, Serializable>> metrics =
        federateResponse.getProperties().entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(METRICS_SOURCE_ELAPSED_PREFIX))
            .collect(Collectors.toList());

    assertThat(metrics.size(), is(sourceList.size()));
  }

  @Test
  public void testFederateGetEmptyResults() throws Exception {
    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, false, null, properties);

    List<Source> sourceList = ImmutableList.of();

    QueryResponse federateResponse = strategy.federate(sourceList, fedQueryRequest);

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

    QueryResponse federateResponse = strategy.federate(sourceList, fedQueryRequest);

    assertThat(federateResponse.getHits(), is(numHits * sourceList.size()));
  }

  @Test
  public void testFederateGetEmptyHits() throws Exception {
    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, false, null, properties);

    List<Source> sourceList = ImmutableList.of();

    QueryResponse federateResponse = strategy.federate(sourceList, fedQueryRequest);

    assertThat(federateResponse.getHits(), is((long) 0));
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

    SortedQueryMonitorFactory mockSortedQueryMonitorFactory = mock(SortedQueryMonitorFactory.class);

    when(mockSortedQueryMonitorFactory.createMonitor(
            any(CompletionService.class),
            any(Map.class),
            responseArgumentCaptor.capture(),
            requestArgumentCaptor.capture(),
            any(List.class)))
        .thenReturn(mockSortedQueryMonitor);

    SortedFederationStrategy federateStrategy =
        new SortedFederationStrategy(
            queryExecutor,
            Arrays.asList(preQueryPlugin),
            new ArrayList<>(),
            mockSortedQueryMonitorFactory);

    // startIndex and pageSize must be > 1
    Query mockQ =
        new QueryImpl(
            mock(NullFilterImpl.class),
            SortedFederationStrategy.DEFAULT_MAX_START_INDEX + 5,
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

    QueryResponse federateResponse = federateStrategy.federate(sources, fedQueryRequest);

    assertThat(
        federateResponse.getRequest().getQuery().getStartIndex(),
        is(SortedFederationStrategy.DEFAULT_MAX_START_INDEX + 5));
    // Modified offset is set to 1
    verify(mockSortedQueryMonitorFactory)
        .createMonitor(
            any(CompletionService.class),
            any(Map.class),
            responseArgumentCaptor.capture(),
            requestArgumentCaptor.capture(),
            any(List.class));
    assertThat(requestArgumentCaptor.getValue().getQuery().getStartIndex(), is(1));
  }

  @Test
  public void testSortedQueryMonitorException() throws Exception {

    SortedQueryMonitorFactory mockSortedQueryMonitorFactory = mock(SortedQueryMonitorFactory.class);

    when(mockSortedQueryMonitorFactory.createMonitor(
            any(CompletionService.class),
            any(Map.class),
            responseArgumentCaptor.capture(),
            requestArgumentCaptor.capture(),
            any(List.class)))
        .thenReturn(
            () -> {
              throw new RuntimeException("Unhandled sorted query monitor exception");
            });

    SortedFederationStrategy federateStrategy =
        new SortedFederationStrategy(
            queryExecutor,
            Arrays.asList(preQueryPlugin),
            new ArrayList<>(),
            mockSortedQueryMonitorFactory);

    QueryRequest fedQueryRequest =
        new QueryRequestImpl(
            new QueryImpl(mock(NullFilterImpl.class), 1, 10, SortBy.NATURAL_ORDER, true, 1),
            properties);

    List<Source> sources = Collections.singletonList(mock(Source.class));

    QueryResponse federateResponse = federateStrategy.federate(sources, fedQueryRequest);

    assertThat(federateResponse.getResults().size(), is(0));
    assertThat(federateResponse.hasMoreResults(), is(false));
    assertThat(federateResponse.getProcessingDetails().size(), is(1));

    ProcessingDetails details = federateResponse.getProcessingDetails().iterator().next();
    assertThat(details.hasException(), is(true));
    assertThat(details.getException().getCause(), instanceOf(RuntimeException.class));
    assertThat(details.getSourceId(), is("unknown"));
  }

  @Test
  public void testOffsetResultHandler() throws Exception {
    QueryResponseImpl originalResults = mock(QueryResponseImpl.class);
    QueryResponseImpl offsetResultQueue = mock(QueryResponseImpl.class);
    int pageSize = 1, offset = 1;

    Result mockResult = mock(Result.class);
    MetacardImpl mockMetacard = mock(MetacardImpl.class);

    List<Result> results = new ArrayList<>();

    SortedFederationStrategy.OffsetResultHandler offsetResultHandler =
        new SortedFederationStrategy.OffsetResultHandler(
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
    verify(offsetResultQueue).closeResultQueue();
  }

  @Test
  public void testCatchPluginExecutionException() throws Exception {
    PreFederatedQueryPlugin mockPlug = mock(PreFederatedQueryPlugin.class);
    PreFederatedQueryPlugin mockPlug2 = mock(PreFederatedQueryPlugin.class);

    when(mockPlug.process(any(Source.class), any(QueryRequest.class)))
        .thenThrow(new PluginExecutionException());
    strategy =
        new SortedFederationStrategy(
            queryExecutor, Arrays.asList(mockPlug, mockPlug2), new ArrayList<>());

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
        new SortedFederationStrategy(
            queryExecutor, Arrays.asList(mockPlug, mockPlug2), new ArrayList<>());

    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, properties);

    strategy.federate(Arrays.asList(mock(Source.class)), fedQueryRequest);
    // First plugin throws exception, so second plugin is untouched
    verify(mockPlug).process(any(Source.class), any(QueryRequest.class));
    verifyNoInteractions(mockPlug2);
  }

  @Test
  public void testStartIndexLessThanZero() throws Exception {
    strategy.setMaxStartIndex(-5);
    assertThat(strategy.getMaxStartIndex(), is(SortedFederationStrategy.DEFAULT_MAX_START_INDEX));
  }

  @Test
  public void testStartIndexIsZero() throws Exception {
    strategy.setMaxStartIndex(0);
    assertThat(strategy.getMaxStartIndex(), is(SortedFederationStrategy.DEFAULT_MAX_START_INDEX));
  }

  @Test
  public void testStartIndexGreaterThanZero() throws Exception {
    strategy.setMaxStartIndex(5);
    assertThat(strategy.getMaxStartIndex(), is(5));
  }

  @Test
  public void testResponseWithProcessingDetails() throws UnsupportedQueryException {

    Source mockSource = mock(Source.class);

    String testSource = "test source";
    when(mockSource.getId()).thenReturn(testSource);

    SourceResponse mockResponseWithProcessingDetails = mock(SourceResponse.class);

    ProcessingDetails processingDetailsForNullPointer =
        new ProcessingDetailsImpl(testSource, null, "Look! A null pointer!");

    ProcessingDetails processingDetailsForUnsupportedQuery =
        new ProcessingDetailsImpl(testSource, null, "We do not support this query.");

    Set<ProcessingDetails> processingDetails = new HashSet<>();
    processingDetails.add(processingDetailsForNullPointer);
    processingDetails.add(processingDetailsForUnsupportedQuery);
    doReturn(processingDetails).when(mockResponseWithProcessingDetails).getProcessingDetails();

    QueryRequest fedQueryRequest = new QueryRequestImpl(mockQuery, properties);
    when(mockSource.query(any(QueryRequest.class))).thenReturn(mockResponseWithProcessingDetails);

    SourceResponse federatedResponse =
        strategy.federate(Collections.singletonList(mockSource), fedQueryRequest);

    assertThat(
        federatedResponse.getProcessingDetails(),
        containsInAnyOrder(processingDetailsForNullPointer, processingDetailsForUnsupportedQuery));
  }

  @Test(expected = NullPointerException.class)
  public void testNullQueryExecutorService() throws Exception {
    strategy = new SortedFederationStrategy(null, Arrays.asList(preQueryPlugin), new ArrayList<>());
  }

  @Test(expected = NullPointerException.class)
  public void testNullPreQueryPlugin() throws Exception {
    strategy = new SortedFederationStrategy(queryExecutor, null, new ArrayList<>());
  }

  @Test(expected = NullPointerException.class)
  public void testNullPreQueryPluginListContents() throws Exception {
    strategy = new SortedFederationStrategy(queryExecutor, Arrays.asList(null), new ArrayList<>());
  }

  @Test(expected = NullPointerException.class)
  public void testNullPostQueryPlugin() throws Exception {
    strategy = new SortedFederationStrategy(queryExecutor, Arrays.asList(preQueryPlugin), null);
  }

  @Test(expected = NullPointerException.class)
  public void testNullPostQueryPluginListContents() throws Exception {
    strategy =
        new SortedFederationStrategy(
            queryExecutor, Arrays.asList(preQueryPlugin), Arrays.asList(null));
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
}
