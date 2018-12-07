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
package org.codice.ddf.commands.catalog;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

public class SeedCommandTest extends CommandCatalogFrameworkCommon {

  private SeedCommand seedCommand;

  private CatalogFramework catalogFramework;

  private Metacard metacardMock;

  private Result resultMock;

  @Before
  public void setUp() throws Exception {
    catalogFramework = mock(CatalogFramework.class);
    doReturn(mockResourceResponse())
        .when(catalogFramework)
        .getResource(any(ResourceRequest.class), nullable(String.class));

    seedCommand = new SeedCommand();
    seedCommand.catalogFramework = catalogFramework;
    seedCommand.filterBuilder = new GeotoolsFilterBuilder();
  }

  @Test
  public void testBadResourceLimit() throws Exception {
    seedCommand.resourceLimit = 0;
    seedCommand.executeWithSubject();
    assertThat(consoleOutput.getOutput(), containsString("The limit must be greater than 0."));
  }

  @Test
  public void testEnterprise() throws Exception {
    mockQueryResponse(1, new String[0], new boolean[0]);
    runCommandAndVerifyQueryRequest(request -> assertThat(request.isEnterprise(), is(true)));
  }

  @Test
  public void testSources() throws Exception {
    final String source1 = "source1";
    final String source2 = "source2";
    seedCommand.sources = newArrayList(source1, source2);

    mockQueryResponse(1, new String[0], new boolean[0]);

    runCommandAndVerifyQueryRequest(
        request -> {
          assertThat(request.isEnterprise(), is(false));
          assertThat(request.getSourceIds(), is(newHashSet(source1, source2)));
        });
  }

  @Test
  public void testCql() throws Exception {
    final String cql = "modified AFTER 2016-07-21T00:00:00+00:00";
    seedCommand.cqlFilter = cql;

    mockQueryResponse(1, new String[0], new boolean[0]);

    runCommandAndVerifyQueryRequest(
        request -> {
          Query query = request.getQuery();
          assertThat(ECQL.toCQL(query), is(cql));
        });
  }

  @Test
  public void testNoCql() throws Exception {
    mockQueryResponse(1, new String[0], new boolean[0]);

    runCommandAndVerifyQueryRequest(
        request -> {
          Query query = request.getQuery();
          assertThat(ECQL.toCQL(query), is("anyText ILIKE '*'"));
        });
  }

  @Test
  public void testCacheMetacards() throws Exception {
    mockQueryResponse(1, new String[0], new boolean[0]);

    runCommandAndVerifyQueryRequest(
        request -> assertThat(request.getProperties(), hasEntry("mode", "update")));
  }

  private void runCommandAndVerifyQueryRequest(Consumer<QueryRequest> queryRequestAssertions)
      throws Exception {
    seedCommand.executeWithSubject();

    ArgumentCaptor<QueryRequest> queryCaptor = ArgumentCaptor.forClass(QueryRequest.class);
    verify(catalogFramework).query(queryCaptor.capture());

    QueryRequest request = queryCaptor.getValue();
    queryRequestAssertions.accept(request);
  }

  @Test
  public void testResourceLimit() throws Exception {
    final int limit = 2;
    seedCommand.resourceLimit = limit;

    final String id1 = "1";
    final String id2 = "2";
    mockQueryResponse(limit * 2 + 1, new String[] {id1, id2}, new boolean[] {false, false});

    runCommandAndVerifyResourceRequests(
        limit,
        resourceRequests -> {
          assertThat(resourceRequests, hasSize(limit));
          verifyResourceRequest(resourceRequests.get(0), id1);
          verifyResourceRequest(resourceRequests.get(1), id2);
        },
        siteNames -> assertThat(siteNames, is(newArrayList(id1, id2))));

    verify(catalogFramework, times(1)).query(any(QueryRequest.class));
  }

  @Test
  public void testFewerResourcesThanLimit() throws Exception {
    final int limit = 10;
    seedCommand.resourceLimit = limit;

    final String id1 = "1";
    final String id2 = "2";
    final String id3 = "3";
    mockQueryResponse(limit + 1, new String[] {id1, id2, id3}, new boolean[] {false, false, false});

    final int expectedResourceRequests = 3;
    runCommandAndVerifyResourceRequests(
        expectedResourceRequests,
        resourceRequests -> {
          assertThat(resourceRequests, hasSize(expectedResourceRequests));
          verifyResourceRequest(resourceRequests.get(0), id1);
          verifyResourceRequest(resourceRequests.get(1), id2);
          verifyResourceRequest(resourceRequests.get(2), id3);
        },
        siteNames -> assertThat(siteNames, is(newArrayList(id1, id2, id3))));

    verify(catalogFramework, times(2)).query(any(QueryRequest.class));
  }

  @Test
  public void testDoesNotDownloadCachedResource() throws Exception {
    final int limit = 3;
    seedCommand.resourceLimit = limit;

    final String id1 = "1";
    final String id2 = "2";
    mockQueryResponse(
        limit * 2 + 1, new String[] {id1, id2, "3"}, new boolean[] {false, false, true});
    final int expectedResourceRequests = 3;

    runCommandAndVerifyResourceRequests(
        expectedResourceRequests,
        resourceRequests -> {
          assertThat(resourceRequests, hasSize(expectedResourceRequests));
          verifyResourceRequest(resourceRequests.get(0), id1);
          verifyResourceRequest(resourceRequests.get(1), id2);
          verifyResourceRequest(resourceRequests.get(2), id1);
        },
        siteNames -> assertThat(siteNames, is(newArrayList(id1, id2, id1))));

    verify(catalogFramework, times(2)).query(any(QueryRequest.class));
  }

  /**
   * This test will verify that seed does not skip resources when the maximum page size is less than
   * the specified resource limit. This concern was raised after query results became limited to a
   * maximum in DDF-2872.
   *
   * @throws Exception
   */
  @Test
  public void testDoesNotSkipResources() throws Exception {

    int maxPageSize = 1000;
    int resourceLimit = maxPageSize * 2;

    String expected = resourceLimit + " resource download(s) started.";

    mockMultipleCatalogFrameworkQueries(maxPageSize, resourceLimit);

    seedCommand.executeWithSubject();
    assertThat(consoleOutput.getOutput(), containsString(expected));
  }

  private void runCommandAndVerifyResourceRequests(
      int expectedResourceRequests,
      Consumer<List<ResourceRequest>> requestAssertions,
      Consumer<List<String>> siteNameAssertions)
      throws Exception {
    seedCommand.executeWithSubject();

    ArgumentCaptor<ResourceRequest> resourceRequestCaptor =
        ArgumentCaptor.forClass(ResourceRequest.class);
    ArgumentCaptor<String> siteNameCaptor = ArgumentCaptor.forClass(String.class);
    verify(catalogFramework, times(expectedResourceRequests))
        .getResource(resourceRequestCaptor.capture(), siteNameCaptor.capture());

    List<ResourceRequest> resourceRequests = resourceRequestCaptor.getAllValues();
    requestAssertions.accept(resourceRequests);

    List<String> siteNames = siteNameCaptor.getAllValues();
    siteNameAssertions.accept(siteNames);
  }

  private void mockQueryResponse(int stopReturningResultsAtIndex, String[] ids, boolean[] cached)
      throws Exception {
    List<Result> results = new ArrayList<>();
    for (int i = 0; i < ids.length; ++i) {
      String id = ids[i];
      Result mockResult = mock(Result.class);
      MetacardImpl metacard = new MetacardImpl();
      metacard.setId(id);
      metacard.setSourceId(id);
      metacard.setAttribute("internal.local-resource", cached[i]);
      when(mockResult.getMetacard()).thenReturn(metacard);
      results.add(mockResult);
    }

    QueryResponse response = mock(QueryResponse.class);
    when(response.getResults()).thenReturn(results);
    doReturn(response)
        .when(catalogFramework)
        .query(
            argThat(
                queryWithStartIndex(
                    request -> request.getQuery().getStartIndex() < stopReturningResultsAtIndex)));

    QueryResponse noResults = mock(QueryResponse.class);
    when(noResults.getResults()).thenReturn(Collections.emptyList());
    doReturn(noResults)
        .when(catalogFramework)
        .query(
            argThat(
                queryWithStartIndex(
                    request -> request.getQuery().getStartIndex() >= stopReturningResultsAtIndex)));
  }

  private QueryWithStartIndex queryWithStartIndex(Predicate<QueryRequest> test) {
    return new QueryWithStartIndex(test);
  }

  private void verifyResourceRequest(ResourceRequest request, String expectedAttributeValue) {
    assertThat(request.getAttributeName(), is(Metacard.ID));
    assertThat(request.getAttributeValue(), is(expectedAttributeValue));
  }

  private ResourceResponse mockResourceResponse() throws IOException {
    InputStream mockInputStream = mock(InputStream.class);
    doReturn(-1).when(mockInputStream).read(any(byte[].class), anyInt(), anyInt());

    Resource mockResource = mock(Resource.class);
    when(mockResource.getInputStream()).thenReturn(mockInputStream);

    ResourceResponse mockResponse = mock(ResourceResponse.class);
    when(mockResponse.getResource()).thenReturn(mockResource);
    return mockResponse;
  }

  private class QueryWithStartIndex implements ArgumentMatcher<QueryRequest> {
    private final Predicate<QueryRequest> test;

    private QueryWithStartIndex(Predicate<QueryRequest> test) {
      this.test = test;
    }

    @Override
    public boolean matches(QueryRequest o) {
      return test.test(o);
    }
  }

  private void mockMultipleCatalogFrameworkQueries(int pageSize, int resourceLimit)
      throws Exception {

    mockResourceResponse();

    // Populate list of mock results, sized at resource limit
    List<Result> resultsList = populateResultMockList(resourceLimit);
    mockQueryGetResults(resultsList, pageSize);

    seedCommand.resourceLimit = resourceLimit;
  }

  private List<Result> populateResultMockList(int size) {

    List<Result> resultMockList = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      resultMock = mock(Result.class);
      metacardMock = mock(Metacard.class);

      when(resultMock.getMetacard()).thenReturn(metacardMock);
      when(metacardMock.getId()).thenReturn("MOCK METACARD " + (i + 1));

      resultMockList.add(resultMock);
    }
    return resultMockList;
  }

  /**
   * Divides results list by the page size. Used for thenReturn statements. The array being passed
   * will have doubles for every query after the first, for example: [ query1, query2, query2, ... ]
   * This is because the query result will need to be returned twice for every iteration, and the
   * first query is get inserted in the first parameter of thenReturn. i.e. thenReturn(query1, [
   * query1, query2, query2 ]).
   */
  private void mockQueryGetResults(List<Result> results, int pageSize) throws Exception {

    QueryResponse queryResponseMock = mock(QueryResponse.class);

    int queries = results.size() / pageSize;
    queries += (results.size() % pageSize > 0) ? 1 : 0;

    List<List<Result>> pageResults = Lists.partition(results, pageSize);
    List[] pageArray = new List[queries * 2 - 1];

    pageArray[0] = pageResults.get(0);

    for (int i = 1, j = 1; j < pageResults.size(); i += 2, j++) {
      pageArray[i] = pageResults.get(j);
      pageArray[i + 1] = pageResults.get(j);
    }

    when(catalogFramework.query(any())).thenReturn(queryResponseMock);
    when(queryResponseMock.getResults()).thenReturn(pageArray[0], pageArray);
  }
}
