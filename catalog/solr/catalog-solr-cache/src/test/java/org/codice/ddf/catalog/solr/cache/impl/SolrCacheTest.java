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
package org.codice.ddf.catalog.solr.cache.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
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
import org.apache.solr.client.solrj.SolrServerException;
import org.codice.ddf.catalog.solr.cache.CachePutPlugin;
import org.codice.solr.client.solrj.SolrClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

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
  public void testCachePutPluginModifiesMetacard()
      throws SolrServerException, MetacardCreationException, IOException {

    String name = "attribute";
    String value = "value";

    List<Metacard> metacards = new ArrayList<>();
    MetacardImpl metacard = new MetacardImpl();
    metacard.setSourceId(TEST_ID);
    metacard.setId(TEST_ID);
    metacards.add(metacard);

    when(mockCachePutPlugin.process(any()))
        .thenAnswer(
            invocationOnMock -> {
              Metacard metacardToModify = (Metacard) invocationOnMock.getArguments()[0];
              metacardToModify.setAttribute(new AttributeImpl(name, value));
              return Optional.of(metacardToModify);
            });

    solrCache.put(metacards);

    ArgumentCaptor<List> updatedMetacardsCaptor = ArgumentCaptor.forClass(List.class);
    verify(mockCacheSolrMetacardClient).add(updatedMetacardsCaptor.capture(), eq(false));
    assertThat(updatedMetacardsCaptor.getValue().size(), is(1));
    assertThat(updatedMetacardsCaptor.getValue().get(0), is(metacard));
    Metacard capturedMetacard = (Metacard) updatedMetacardsCaptor.getValue().get(0);
    assertThat(capturedMetacard.getAttribute(name), notNullValue());
    assertThat(capturedMetacard.getAttribute(name).getValue(), is(value));
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

    solrCache.delete(mockRequest);

    verify(mockCacheSolrMetacardClient)
        .deleteByIds(OTHER_ATTRIBUTE_NAME + SchemaFields.TEXT_SUFFIX, null, false);
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
