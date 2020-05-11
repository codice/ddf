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

import static java.util.stream.Collectors.toList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.CatalogFramework;
import ddf.catalog.cache.SolrCacheMBean;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

public class RemoveCommandTest extends ConsoleOutputCommon {

  public static final String DUMMY_FILTER = "title like '*'";

  private static final int BATCH_SIZE = 250;

  private List<Metacard> metacardList;

  private RemoveCommand removeCommand;

  private DeleteResponse deleteResponse;

  private List<QueryResponse> queryResponseBatch;

  private int sizeOfDeleteRequest;

  @Before
  public void setup() {
    metacardList = populateMetacardList(5);
    SolrCacheMBean solrCacheMock = mock(SolrCacheMBean.class);
    deleteResponse = mock(DeleteResponse.class);
    removeCommand =
        new RemoveCommand() {
          @Override
          protected SolrCacheMBean getCacheProxy() {
            return solrCacheMock;
          }
        };
    removeCommand.catalogFramework = mock(CatalogFramework.class);
    removeCommand.cache = false;
  }

  @Test
  public void testDeleteSingleMetacardFromCache() throws Exception {
    Set<String> ids = oneIdToDelete();
    removeCommand.ids = ids;
    removeCommand.cache = true;
    removeCommand.executeWithSubject();
    String[] idsArray = new String[ids.size()];
    idsArray = ids.toArray(idsArray);
    verify(removeCommand.getCacheProxy(), times(1)).removeById(idsArray);
  }

  @Test
  public void testDeleteMultipleMetacardsFromCache() throws Exception {
    Set<String> ids = threeIdsToDelete();
    removeCommand.ids = ids;
    removeCommand.cache = true;
    removeCommand.executeWithSubject();
    String[] idsArray = new String[ids.size()];
    idsArray = ids.toArray(idsArray);
    verify(removeCommand.getCacheProxy(), times(1)).removeById(idsArray);
  }

  /**
   * Tests condition where number of results returned by each catalog framework query is more than
   * batchSize being used.
   */
  @Test
  public void testQueryGreaterThanBatchSize() throws Exception {
    int sizeOfDeleteRequest = 255;
    configureQueryMock(sizeOfDeleteRequest);
    configureDeleteMock();
    removeCommand.cqlFilter = DUMMY_FILTER;
    removeCommand.executeWithSubject();
    ArgumentCaptor<DeleteRequest> deleteRequestArgumentCaptor =
        ArgumentCaptor.forClass(DeleteRequest.class);
    verify(removeCommand.catalogFramework, times(2)).delete(deleteRequestArgumentCaptor.capture());
    List<DeleteRequest> values = deleteRequestArgumentCaptor.getAllValues();
    int totalDeleted = 0;
    for (DeleteRequest request : values) {
      totalDeleted += request.getAttributeValues().size();
    }
    assertThat(totalDeleted, equalTo(sizeOfDeleteRequest));
  }

  @Test
  public void testNullList() throws Exception {
    removeCommand.ids = null;
    removeCommand.executeWithSubject();
    assertThat(consoleOutput.getOutput(), containsString("No IDs or filter provided"));
  }

  @Test
  public void testDeleteSingleMetacardFromStore() throws Exception {
    sizeOfDeleteRequest = 1;
    configureQueryMock(sizeOfDeleteRequest);
    configureDeleteMock();
    removeCommand.ids = oneIdToDelete();
    removeCommand.executeWithSubject();
    ArgumentCaptor<DeleteRequest> deleteRequestArgumentCaptor =
        ArgumentCaptor.forClass(DeleteRequest.class);
    verify(removeCommand.catalogFramework, times(1)).delete(deleteRequestArgumentCaptor.capture());
    List<DeleteRequest> values = deleteRequestArgumentCaptor.getAllValues();
    int totalDeleted = 0;
    for (DeleteRequest request : values) {
      totalDeleted += request.getAttributeValues().size();
    }
    assertThat(totalDeleted, equalTo(sizeOfDeleteRequest));
  }

  @Test
  public void testFilterMatchesNothing() throws Exception {
    sizeOfDeleteRequest = 0;
    configureQueryMock(sizeOfDeleteRequest);
    configureDeleteMock();
    removeCommand.cqlFilter = "title like 'fake-filter'";
    removeCommand.executeWithSubject();
    ArgumentCaptor<DeleteRequest> deleteRequestArgumentCaptor =
        ArgumentCaptor.forClass(DeleteRequest.class);
    verify(removeCommand.catalogFramework, times(0)).delete(deleteRequestArgumentCaptor.capture());
    List<DeleteRequest> values = deleteRequestArgumentCaptor.getAllValues();
    int totalDeleted = 0;
    for (DeleteRequest request : values) {
      totalDeleted += request.getAttributeValues().size();
    }
    assertThat(totalDeleted, equalTo(sizeOfDeleteRequest));
  }

  @Test
  public void testCacheAndFilter() throws Exception {
    removeCommand.cqlFilter = DUMMY_FILTER;
    removeCommand.cache = true;
    removeCommand.executeWithSubject();
    assertThat(consoleOutput.getOutput(), containsString("Cache does not support filtering."));
  }

  @Test
  public void testBothIdsAndFilter() throws Exception {
    configureQueryMock(3);
    configureDeleteMock();
    removeCommand.ids = ImmutableSet.of("1");
    removeCommand.cqlFilter = DUMMY_FILTER;
    removeCommand.executeWithSubject();
    ArgumentCaptor<DeleteRequest> deleteRequestArgumentCaptor =
        ArgumentCaptor.forClass(DeleteRequest.class);
    verify(removeCommand.catalogFramework, times(1)).delete(deleteRequestArgumentCaptor.capture());
    List<DeleteRequest> values = deleteRequestArgumentCaptor.getAllValues();
    int totalDeleted = 0;
    for (DeleteRequest request : values) {
      totalDeleted += request.getAttributeValues().size();
    }
    assertThat(totalDeleted, equalTo(1));
  }

  private void configureDeleteMock() throws Exception {
    when(removeCommand.catalogFramework.delete(isA(DeleteRequest.class)))
        .thenAnswer(
            (Answer<DeleteResponse>)
                invocation -> {
                  DeleteRequest request = (DeleteRequest) invocation.getArguments()[0];
                  long numResults = request.getAttributeValues().size();
                  when(deleteResponse.getDeletedMetacards())
                      .thenReturn(populateMetacardList((int) numResults));
                  return deleteResponse;
                });
  }

  private void configureQueryMock(int numberOfQueriedMetacards) throws Exception {
    queryResponseBatch = getQueryResponseBatch(BATCH_SIZE, numberOfQueriedMetacards);
    QueryResponse[] qrRest =
        queryResponseBatch.subList(1, queryResponseBatch.size()).toArray(new QueryResponse[0]);
    when(removeCommand.catalogFramework.query(isA(QueryRequest.class)))
        .thenReturn(queryResponseBatch.get(0), qrRest);
  }

  private List<Metacard> populateMetacardList(int size) {
    return Stream.generate(this::newRandomMetacard).limit(size).collect(toList());
  }

  private List<QueryResponse> getQueryResponseBatch(int batchSize, int total) {
    Queue<Result> results = new ArrayDeque<>();
    for (int i = 1; i <= total; i++) {
      MetacardImpl metacard = new MetacardImpl();
      metacard.setId(i + "");
      results.add(new ResultImpl(metacard));
    }

    List<QueryResponse> queryResponses = new ArrayList<>();
    while (!results.isEmpty()) {
      List<Result> batchList = new ArrayList<>();
      for (int i = 0; i < batchSize; i++) {
        Result result = results.poll();
        if (result == null) {
          break;
        }
        batchList.add(result);
      }
      queryResponses.add(new QueryResponseImpl(null, batchList, total));
    }

    // Add one empty response list to the end
    queryResponses.add(new QueryResponseImpl(null, Collections.emptyList(), 0));
    return queryResponses;
  }

  private MetacardImpl newRandomMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId(UUID.randomUUID().toString());
    return metacard;
  }

  private Set<String> threeIdsToDelete() {
    HashSet<String> ids = new HashSet<>();
    ids.add(metacardList.get(0).getId());
    ids.add(metacardList.get(1).getId());
    ids.add(metacardList.get(2).getId());
    return ids;
  }

  private Set<String> oneIdToDelete() {
    HashSet<String> ids = new HashSet<>();
    ids.add(metacardList.get(0).getId());
    return ids;
  }
}
