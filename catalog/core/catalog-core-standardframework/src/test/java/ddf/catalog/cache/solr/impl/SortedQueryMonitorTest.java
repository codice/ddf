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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.extractor.Extractors.byName;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

public class SortedQueryMonitorTest {

  private CachingFederationStrategy cachingFederationStrategy;

  private CompletionService completionService;

  private QueryResponseImpl queryResponse;

  private QueryRequest queryRequest;

  private Map<Future<SourceResponse>, QueryRequest> futures;

  private Query query;

  @Before
  public void setUp() throws Exception {
    cachingFederationStrategy = mock(CachingFederationStrategy.class);
    completionService = mock(CompletionService.class);
    queryRequest = mock(QueryRequest.class);
    queryResponse = new QueryResponseImpl(queryRequest);
    query = mock(Query.class);

    // Enforce insertion order for testing purposes
    futures = new LinkedHashMap<>();
    for (int i = 0; i < 4; i++) {
      SourceResponse sourceResponseMock = null;
      Future futureMock = mock(Future.class);
      QueryRequest queryRequest = mock(QueryRequest.class);
      when(queryRequest.getSourceIds()).thenReturn(Collections.singleton("Source-" + i));

      switch (i) {
        case 1:
          sourceResponseMock = mock(SourceResponse.class);
          when(sourceResponseMock.getResults())
              .thenReturn(
                  Lists.newArrayList(mock(Result.class), mock(Result.class), mock(Result.class)));
          when(sourceResponseMock.getHits()).thenReturn(3L);
          break;
        case 2:
          sourceResponseMock = mock(SourceResponse.class);
          when(sourceResponseMock.getResults()).thenReturn(Lists.newArrayList(mock(Result.class)));
          when(sourceResponseMock.getHits()).thenReturn(1L);
          break;
        case 3:
          sourceResponseMock = mock(SourceResponse.class);
          when(sourceResponseMock.getResults()).thenReturn(Lists.<Result>emptyList());
          when(sourceResponseMock.getHits()).thenReturn(0L);
          break;
      }

      when(futureMock.get()).thenReturn(sourceResponseMock);
      futures.put(futureMock, queryRequest);
    }
  }

  @Test
  public void noQueryTimeout() throws Exception {
    when(query.getTimeoutMillis()).thenReturn(0L);
    when(queryRequest.getQuery()).thenReturn(query);

    SortedQueryMonitor queryMonitor =
        new SortedQueryMonitor(
            cachingFederationStrategy,
            completionService,
            futures,
            queryResponse,
            queryRequest,
            new ArrayList<>());

    final Iterator<Future<SourceResponse>> futureIter = getFutureIterator();
    when(completionService.take()).thenAnswer((invocationOnMock -> futureIter.next()));
    queryMonitor.run();
    verify(completionService, times(4)).take();
    verify(completionService, never()).poll(anyLong(), eq(TimeUnit.MILLISECONDS));

    assertThat(queryResponse.getHits()).isEqualTo(4);
    assertThat(queryResponse.getResults().size()).isEqualTo(4);
    HashMap<String, Long> hitsPerSource =
        (HashMap<String, Long>) queryResponse.getProperties().get("hitsPerSource");
    assertThat(hitsPerSource.size()).isEqualTo(3);
    for (int[] idAndCount : new int[][] {{1, 3}, {2, 1}, {3, 0}}) {
      assertThat(hitsPerSource.get("Source-" + idAndCount[0])).isEqualTo(idAndCount[1]);
    }

    assertThat(queryResponse.getProcessingDetails())
        .extracting(byName("exception"))
        .extracting(byName("class"))
        .contains(NullPointerException.class);
    assertThat(queryResponse.getProcessingDetails())
        .extracting(byName("sourceId"))
        .contains("Source-0");
  }

  @Test
  public void shortQueryTimeout() throws Exception {
    when(query.getTimeoutMillis()).thenReturn(5000L);
    when(queryRequest.getQuery()).thenReturn(query);

    SortedQueryMonitor queryMonitor =
        new SortedQueryMonitor(
            cachingFederationStrategy,
            completionService,
            futures,
            queryResponse,
            queryRequest,
            new ArrayList<>());

    // Put the first two futures into the list and then set the third
    // value to be null in order to mock the effect of a null return from
    // the CompletionService.poll() method
    Iterator<Future<SourceResponse>> keysIter = futures.keySet().iterator();
    List<Future<SourceResponse>> futureKeys =
        Lists.newArrayList(keysIter.next(), keysIter.next(), null);
    final Iterator<Future<SourceResponse>> futureIter = futureKeys.iterator();

    when(completionService.poll(anyLong(), eq(TimeUnit.MILLISECONDS)))
        .thenAnswer((invocationOnMock -> futureIter.next()));
    queryMonitor.run();
    verify(completionService, times(3)).poll(anyLong(), eq(TimeUnit.MILLISECONDS));
    verify(completionService, never()).take();

    assertThat(queryResponse.getResults().size()).isEqualTo(3);
    assertThat(queryResponse.getHits()).isEqualTo(3);
    HashMap<String, Long> hitsPerSource =
        (HashMap<String, Long>) queryResponse.getProperties().get("hitsPerSource");
    assertThat(hitsPerSource.size()).isEqualTo(1);
    assertThat(hitsPerSource.get("Source-1")).isEqualTo(3);
    assertThat(queryResponse.getProcessingDetails())
        .extracting(byName("exception"))
        .extracting(byName("class"))
        .contains(NullPointerException.class, TimeoutException.class, TimeoutException.class);
  }

  @Test
  public void interruptThirdFuture() throws Exception {
    when(query.getTimeoutMillis()).thenReturn(5000L);
    when(queryRequest.getQuery()).thenReturn(query);

    Iterator<Future<SourceResponse>> iter = futures.keySet().iterator();
    iter.next(); // Source-0
    iter.next(); // Source-1
    Future<SourceResponse> future = iter.next();
    when(future.get()).thenThrow(new InterruptedException("neener-neener"));

    SortedQueryMonitor queryMonitor =
        new SortedQueryMonitor(
            cachingFederationStrategy,
            completionService,
            futures,
            queryResponse,
            queryRequest,
            new ArrayList<>());

    final Iterator<Future<SourceResponse>> futureIter = getFutureIterator();
    when(completionService.poll(anyLong(), eq(TimeUnit.MILLISECONDS)))
        .thenAnswer((invocationOnMock -> futureIter.next()));
    queryMonitor.run();
    verify(completionService, times(3)).poll(anyLong(), eq(TimeUnit.MILLISECONDS));
    verify(completionService, never()).take();

    assertThat(queryResponse.getResults().size()).isEqualTo(3);
    HashMap<String, Long> hitsPerSource =
        (HashMap<String, Long>) queryResponse.getProperties().get("hitsPerSource");
    assertThat(hitsPerSource.size()).isEqualTo(1);
    assertThat(hitsPerSource.get("Source-1")).isEqualTo(3);
    assertThat(queryResponse.getProcessingDetails())
        .extracting(byName("exception"))
        .extracting(byName("class"))
        .contains(
            NullPointerException.class, InterruptedException.class, InterruptedException.class);
  }

  public Iterator<Future<SourceResponse>> getFutureIterator() {
    List<Future<SourceResponse>> futureKeys = new ArrayList<>();
    futureKeys.addAll(futures.keySet());
    return futureKeys.iterator();
  }
}
