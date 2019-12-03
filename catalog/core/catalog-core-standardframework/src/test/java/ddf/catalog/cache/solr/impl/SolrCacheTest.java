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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.cache.CachePutPlugin;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.SchemaFields;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import org.codice.solr.client.solrj.SolrClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.opengis.filter.Filter;

@RunWith(MockitoJUnitRunner.class)
public class SolrCacheTest {

  public static final String SOURCE_ID = "source-id";

  public static final String TEST_ID = "test-id";

  public static final String OTHER_ATTRIBUTE_NAME = "other-attribute-name";

  public static final String ID1 = "id1";

  public static final String ID2 = "id2";

  private SolrCache solrCache;

  @Mock private SolrClient mockSolrClient;

  @Mock private CacheSolrMetacardClient mockCacheSolrMetacardClient;

  @Mock private CachePutPlugin mockCachePutPlugin;

  @Before
  public void setUp() {
    solrCache =
        new SolrCache(
            mockSolrClient,
            mockCacheSolrMetacardClient,
            () -> mock(ScheduledExecutorService.class), // to disable configuration of the scheduler
            Collections.singletonList(mockCachePutPlugin));
  }

  /**
   * Verify that if the CachePutPlugin returns Optional.empty(), then the metacard is not added to
   * the cache.
   */
  @Test
  public void testUncacheableMetacardsAreNotCached() {

    mockEmptyCachePutPlugin();

    List<Metacard> metacards = new ArrayList<>();
    Metacard metacard = mock(Metacard.class);
    when(metacard.getSourceId()).thenReturn(TEST_ID);
    when(metacard.getId()).thenReturn(TEST_ID);
    metacards.add(metacard);

    solrCache.put(metacards);

    Mockito.verifyNoMoreInteractions(mockSolrClient, mockCacheSolrMetacardClient);
  }

  @Test
  public void testCachePutPluginModifiesMetacard() {

    String name = "attribute";
    String value = "value";

    List<Metacard> metacards = new ArrayList<>();
    Metacard metacard = mock(Metacard.class);
    when(metacard.getSourceId()).thenReturn(TEST_ID);
    when(metacard.getId()).thenReturn(TEST_ID);
    metacards.add(metacard);

    when(mockCachePutPlugin.process(any()))
        .thenAnswer(
            (Answer<Optional<Metacard>>)
                invocationOnMock -> {
                  Metacard metacardToModify = (Metacard) invocationOnMock.getArguments()[0];
                  metacardToModify.setAttribute(new AttributeImpl(name, value));
                  return Optional.of(metacardToModify);
                });

    solrCache.put(metacards);

    ArgumentCaptor<Attribute> argumentCaptor = ArgumentCaptor.forClass(Attribute.class);
    verify(metacard).setAttribute(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getName(), is(name));
    assertThat(argumentCaptor.getValue().getValue(), is(value));
  }

  @Test
  public void query() throws UnsupportedQueryException {
    QueryRequest mockQuery = mock(QueryRequest.class);
    SourceResponse expectedResponse = new QueryResponseImpl(mockQuery);
    when(mockCacheSolrMetacardClient.query(mockQuery)).thenReturn(expectedResponse);

    SourceResponse actualResponse = solrCache.query(mockQuery);

    assertThat(actualResponse, is(expectedResponse));

    verify(mockCacheSolrMetacardClient).query(mockQuery);
  }

  @Test(expected = UnsupportedQueryException.class)
  public void queryThrowsUnsupportedQueryException() throws UnsupportedQueryException {
    QueryRequest mockQuery = mock(QueryRequest.class);
    doThrow(new UnsupportedQueryException()).when(mockCacheSolrMetacardClient).query(mockQuery);

    solrCache.query(mockQuery);
  }

  @Test
  public void createWithNullMetacards() throws Exception {
    solrCache.put(null);
    Mockito.verifyNoMoreInteractions(mockSolrClient, mockCacheSolrMetacardClient);
  }

  @Test
  public void createWithEmptyMetacards() throws Exception {
    solrCache.put(Collections.emptyList());
    Mockito.verifyNoMoreInteractions(mockSolrClient, mockCacheSolrMetacardClient);
  }

  @Test
  public void createWithANullMetacard() throws Exception {
    mockIdentityCachePutPlugin();

    List<Metacard> metacards = new ArrayList<>();
    metacards.add(null);

    solrCache.put(metacards);

    Mockito.verifyNoMoreInteractions(mockSolrClient, mockCacheSolrMetacardClient);
  }

  @Test
  public void createWithMetacard() throws Exception {

    mockIdentityCachePutPlugin();

    List<Metacard> metacards = new ArrayList<>();
    Metacard metacard = mock(Metacard.class);
    when(metacard.getSourceId()).thenReturn(TEST_ID);
    when(metacard.getId()).thenReturn(TEST_ID);
    metacards.add(metacard);

    solrCache.put(metacards);

    ArgumentCaptor<List> updatedMetacardsCaptor = ArgumentCaptor.forClass(List.class);
    verify(mockCacheSolrMetacardClient).add(updatedMetacardsCaptor.capture(), eq(false));
    assertThat(updatedMetacardsCaptor.getValue().size(), is(1));
    assertThat(updatedMetacardsCaptor.getValue().get(0), is(metacard));
  }

  @Test
  public void createAbsorbsException() throws Exception {
    doThrow(new IOException()).when(mockCacheSolrMetacardClient).add(any(List.class), eq(false));

    solrCache.put(Collections.emptyList());
  }

  @Test
  public void deleteWithNullRequest() throws Exception {
    solrCache.delete(null);

    Mockito.verifyNoMoreInteractions(mockSolrClient, mockCacheSolrMetacardClient);
  }

  @Test
  public void deleteWithBlankAttributeName() throws Exception {
    DeleteRequest mockRequest = setupDeleteRequest(null);

    solrCache.delete(mockRequest);

    Mockito.verifyNoMoreInteractions(mockSolrClient, mockCacheSolrMetacardClient);
  }

  @Test
  public void deleteWithId() throws Exception {
    DeleteRequest mockRequest = setupDeleteRequest("id");

    solrCache.delete(mockRequest);

    verify(mockCacheSolrMetacardClient)
        .deleteByIds("original_id" + SchemaFields.TEXT_SUFFIX, null, false);
  }

  @Test
  public void deleteWithOtherAttribute() throws Exception {
    DeleteRequest mockRequest = setupDeleteRequest(OTHER_ATTRIBUTE_NAME);

    solrCache.delete(mockRequest);

    verify(mockCacheSolrMetacardClient)
        .deleteByIds(OTHER_ATTRIBUTE_NAME + SchemaFields.TEXT_SUFFIX, null, false);
  }

  @Test
  public void deleteAbsorbsException() throws Exception {
    DeleteRequest mockRequest = setupDeleteRequest(OTHER_ATTRIBUTE_NAME);
    doThrow(new IOException())
        .when(mockCacheSolrMetacardClient)
        .deleteByIds(anyString(), any(List.class), eq(false));

    solrCache.delete(mockRequest);

    verify(mockCacheSolrMetacardClient)
        .deleteByIds(OTHER_ATTRIBUTE_NAME + SchemaFields.TEXT_SUFFIX, null, false);
  }

  @Test
  public void removeAll() throws Exception {
    solrCache.removeAll();

    verify(mockCacheSolrMetacardClient).deleteByQuery("*:*");
  }

  @Test
  public void removeById() throws Exception {
    String[] ids = new String[] {ID1, ID2};
    solrCache.removeById(ids);

    ArgumentCaptor<List> idsListCaptor = ArgumentCaptor.forClass(List.class);
    verify(mockCacheSolrMetacardClient)
        .deleteByIds(
            eq("original_id" + SchemaFields.TEXT_SUFFIX), idsListCaptor.capture(), eq(false));
    assertThat(idsListCaptor.getValue().size(), is(2));
    assertThat(idsListCaptor.getValue().get(0), is(ID1));
    assertThat(idsListCaptor.getValue().get(1), is(ID2));
  }

  @Test
  public void queryWithEmptyResults() throws Exception {
    SourceResponse mockResponse = mock(SourceResponse.class);
    when(mockResponse.getResults()).thenReturn(Collections.emptyList());
    when(mockCacheSolrMetacardClient.query(any(QueryRequest.class))).thenReturn(mockResponse);

    List<Metacard> metacardsList = solrCache.query(mock(Filter.class));

    assertThat(metacardsList.size(), is(0));
  }

  @Test
  public void queryWithResults() throws Exception {
    SourceResponse mockResponse = mock(SourceResponse.class);
    Result mockResult = mock(Result.class);
    Metacard expectedMetacard = new MetacardImpl();
    when(mockResult.getMetacard()).thenReturn(expectedMetacard);

    List<Result> listOfResults = new ArrayList<>();
    listOfResults.add(mockResult);

    when(mockResponse.getResults()).thenReturn(listOfResults);
    when(mockCacheSolrMetacardClient.query(any(QueryRequest.class))).thenReturn(mockResponse);

    List<Metacard> metacardsList = solrCache.query(mock(Filter.class));

    assertThat(metacardsList.size(), is(1));
    assertThat(metacardsList.get(0), is(expectedMetacard));
  }

  private DeleteRequest setupDeleteRequest(String attributeToReturn) {
    DeleteRequest mockRequest = mock(DeleteRequest.class);
    when(mockRequest.getAttributeName()).thenReturn(attributeToReturn);
    when(mockRequest.getAttributeValues()).thenReturn(null);

    return mockRequest;
  }

  /** Mock the CachePutPlugin to always return the metacard unchanged. */
  private void mockIdentityCachePutPlugin() {
    when(mockCachePutPlugin.process(any()))
        .thenAnswer(
            (Answer<Optional<Metacard>>)
                invocationOnMock -> Optional.of((Metacard) invocationOnMock.getArguments()[0]));
  }

  /**
   * Mock the CachePutPlugin to always return Optional.empty(), which indicates that the metacard
   * should not be put into the cache.
   */
  private void mockEmptyCachePutPlugin() {
    when(mockCachePutPlugin.process(any())).thenReturn(Optional.empty());
  }
}
