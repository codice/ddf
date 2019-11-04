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
import static org.awaitility.Awaitility.with;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

public class SortedQueryMonitorTest {

  private CachingFederationStrategy cachingFederationStrategy;

  private CompletionService completionService;

  private QueryResponseImpl queryResponse;

  private QueryRequest queryRequest;

  private Map<Future<SourceResponse>, QueryRequest> futures;

  private Query query;

  private static final String TEST_PROPERTY = "test";

  private static final Date TEST_DATE_1 = new Date(11000);
  private static final Date TEST_DATE_2 = new Date(30000);

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

    final Iterator<Future<SourceResponse>> futureIter =
        new ArrayList<>(futures.keySet()).iterator();
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

    final Iterator<Future<SourceResponse>> futureIter =
        new ArrayList<>(futures.keySet()).iterator();
    when(completionService.poll(anyLong(), eq(TimeUnit.MILLISECONDS)))
        .thenAnswer((invocationOnMock -> futureIter.next()));

    with()
        .await()
        .atMost(5, TimeUnit.MINUTES) // It won't wait 5 minutes
        .pollInterval(1, TimeUnit.MILLISECONDS)
        .until(
            () -> {
              queryMonitor.run();
              return true;
            });
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

  @Test
  public void testPersistenceOfProcessingDetails() throws InterruptedException, ExecutionException {

    Map<Future<SourceResponse>, QueryRequest> futuresWithProcessingDetails = new HashMap<>();

    Future mockedFuture = mock(Future.class);

    SourceResponse mockedSourceResponse = mock(SourceResponse.class);
    when(mockedFuture.get()).thenReturn(mockedSourceResponse);

    Set<ProcessingDetails> processingDetailsOfMockedSourceResponse = new HashSet<>();
    doReturn(processingDetailsOfMockedSourceResponse)
        .when(mockedSourceResponse)
        .getProcessingDetails();

    QueryRequest mockedQueryRequest = mock(QueryRequest.class);

    String idOfTestSource = "test source";
    when(mockedQueryRequest.getSourceIds()).thenReturn(Collections.singleton(idOfTestSource));

    List<String> errorFromMalformedQuery =
        Collections.singletonList("We do not support this query.");

    ProcessingDetails processingDetailsForMalformedQuery =
        new ProcessingDetailsImpl(idOfTestSource, null, errorFromMalformedQuery);
    processingDetailsOfMockedSourceResponse.add(processingDetailsForMalformedQuery);

    List<String> nonspecificError = Collections.singletonList("Something went wrong.");

    ProcessingDetails processingDetailsForNonspecificError =
        new ProcessingDetailsImpl(idOfTestSource, null, nonspecificError);
    processingDetailsOfMockedSourceResponse.add(processingDetailsForNonspecificError);
    futuresWithProcessingDetails.put(mockedFuture, mockedQueryRequest);

    when(queryRequest.getQuery()).thenReturn(query);
    when(query.getTimeoutMillis()).thenReturn(0L);

    SortedQueryMonitor queryMonitor =
        new SortedQueryMonitor(
            cachingFederationStrategy,
            completionService,
            futuresWithProcessingDetails,
            queryResponse,
            queryRequest,
            new ArrayList<>());

    final Iterator<Future<SourceResponse>> iterator =
        futuresWithProcessingDetails.keySet().iterator();
    when(completionService.take()).thenAnswer((invocationOnMock -> iterator.next()));

    queryMonitor.run();

    verify(completionService, times(1)).take();
    verify(completionService, never()).poll(anyLong(), eq(TimeUnit.MILLISECONDS));

    assertThat(queryResponse.getProcessingDetails())
        .extracting(byName("sourceId"))
        .contains(idOfTestSource);
    assertThat(queryResponse.getProcessingDetails())
        .extracting(byName("warnings"))
        .contains(errorFromMalformedQuery);
    assertThat(queryResponse.getProcessingDetails())
        .extracting(byName("warnings"))
        .contains(nonspecificError);
  }

  @Test
  public void testSortAscendingNullFirst() throws Exception {
    testSorting(new String[] {null, "a"}, new String[] {null, "a"}, SortOrder.ASCENDING);
  }

  @Test
  public void testSortAscendingNullLast() throws Exception {
    testSorting(new String[] {"a", null}, new String[] {null, "a"}, SortOrder.ASCENDING);
  }

  @Test
  public void testSortAscending() throws Exception {
    testSorting(new String[] {"b", "a"}, new String[] {"a", "b"}, SortOrder.ASCENDING);
  }

  @Test
  public void testSortDescendingNullFirst() throws Exception {
    testSorting(new String[] {null, "a"}, new String[] {"a", null}, SortOrder.DESCENDING);
  }

  @Test
  public void testSortDescendingNullLast() throws Exception {
    testSorting(new String[] {"a", null}, new String[] {"a", null}, SortOrder.DESCENDING);
  }

  @Test
  public void testSortDescending() throws Exception {
    testSorting(new String[] {"b", "a"}, new String[] {"b", "a"}, SortOrder.DESCENDING);
  }

  @Test
  public void testSortDateAscendingWithNull() throws Exception {
    testSorting(
        new Serializable[] {TEST_DATE_2, null},
        new Serializable[] {null, TEST_DATE_2},
        SortOrder.ASCENDING);
  }

  @Test
  public void testSortDateAscending() throws Exception {
    testSorting(
        new Serializable[] {TEST_DATE_2, TEST_DATE_1},
        new Serializable[] {TEST_DATE_1, TEST_DATE_2},
        SortOrder.ASCENDING);
  }

  @Test
  public void testSortDateDescendingWithNull() throws Exception {
    testSorting(
        new Serializable[] {null, TEST_DATE_2},
        new Serializable[] {TEST_DATE_2, null},
        SortOrder.DESCENDING);
  }

  @Test
  public void testSortDateDescending() throws Exception {
    testSorting(
        new Serializable[] {TEST_DATE_1, TEST_DATE_2},
        new Serializable[] {TEST_DATE_2, TEST_DATE_1},
        SortOrder.DESCENDING);
  }

  @Test
  public void testNonComparableAscending() throws Exception {
    Serializable o = new TestSerial();
    testSorting(
        new Serializable[] {TEST_DATE_1, o},
        new Serializable[] {o, TEST_DATE_1},
        SortOrder.ASCENDING);
  }

  @Test
  public void testNonComparableDescending() throws Exception {
    Serializable o = new TestSerial();
    testSorting(
        new Serializable[] {TEST_DATE_1, o},
        new Serializable[] {TEST_DATE_1, o},
        SortOrder.DESCENDING);
  }

  private void testSorting(
      Serializable[] inputArray, Serializable[] outputArray, SortOrder sortOrder) throws Exception {
    PropertyName propertyName = mock(PropertyName.class);
    when(propertyName.getPropertyName()).thenReturn(TEST_PROPERTY);

    SortBy sortBy = mock(SortBy.class);
    when(sortBy.getSortOrder()).thenReturn(sortOrder);
    when(sortBy.getPropertyName()).thenReturn(propertyName);

    CachingFederationStrategy cachingFederationStrategy = mock(CachingFederationStrategy.class);
    CompletionService completionService = mock(CompletionService.class);
    QueryRequest queryRequest = mock(QueryRequest.class);
    Query query = mock(Query.class);
    Map<Future<SourceResponse>, QueryRequest> futures = new LinkedHashMap<>();

    Future futureMock = mock(Future.class);
    SourceResponse sourceResponseMock = getMockedResponse(getResults(TEST_PROPERTY, inputArray));
    when(futureMock.get()).thenReturn(sourceResponseMock);
    when(query.getSortBy()).thenReturn(sortBy);
    when(query.getTimeoutMillis()).thenReturn(5000L);
    when(queryRequest.getQuery()).thenReturn(query);
    when(queryRequest.getSourceIds()).thenReturn(Collections.singleton("Sort-Source"));
    futures.put(futureMock, queryRequest);
    QueryResponseImpl queryResponse = new QueryResponseImpl(queryRequest);

    SortedQueryMonitor queryMonitor =
        new SortedQueryMonitor(
            cachingFederationStrategy,
            completionService,
            futures,
            queryResponse,
            queryRequest,
            new ArrayList<>());

    Future<SourceResponse> currFuture = futures.keySet().iterator().next();
    when(completionService.poll(anyLong(), any())).thenAnswer((invocationOnMock -> currFuture));
    when(completionService.take()).thenAnswer((invocationOnMock -> currFuture));
    queryMonitor.run();

    assertResults(queryResponse.getResults(), TEST_PROPERTY, outputArray);
  }

  private List<Result> getResults(String property, Serializable... values) {
    List<Result> results = new ArrayList<>();
    for (Serializable value : values) {
      Metacard metacard = new MetacardImpl();
      metacard.setAttribute(new AttributeImpl(property, value));
      results.add(new ResultImpl(metacard));
    }
    return results;
  }

  private SourceResponse getMockedResponse(List<Result> results) {
    SourceResponse response = mock(SourceResponse.class);
    when(response.getResults()).thenReturn(results);
    when(response.getHits()).thenReturn((long) results.size());
    return response;
  }

  private void assertResults(List<Result> results, String property, Serializable[] values) {
    assertThat(results.size()).isEqualTo(values.length);

    int idx = 0;
    for (Result result : results) {
      Attribute attr = result.getMetacard().getAttribute(property);
      if (values[idx] != null) {
        assertThat(attr.getValue()).isEqualTo(values[idx]);
      } else {
        assertThat(attr).isNull();
      }
      idx++;
    }
  }

  class TestSerial implements Serializable {
    public TestSerial() {}
  }
}
