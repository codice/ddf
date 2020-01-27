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
package ddf.catalog.source.solr;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.IndexQueryResponse;
import ddf.catalog.operation.IndexQueryResult;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.IndexQueryResponseImpl;
import ddf.catalog.operation.impl.IndexQueryResultImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.operation.impl.UpdateImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.source.IndexProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.StorageProvider;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import org.junit.Before;
import org.junit.Test;

public class AbstractCatalogProviderTest {

  private AbstractCatalogProvider abstractCatalogProvider;

  private IndexProvider indexProvider = mock(IndexProvider.class);

  private StorageProvider storageProvider = mock(StorageProvider.class);

  private static final String ID1 = "id-1";

  private static final String ID2 = "id-2";

  @Before
  public void setUp() {
    when(indexProvider.isAvailable()).thenReturn(true);
    when(storageProvider.isAvailable()).thenReturn(true);
    abstractCatalogProvider =
        new TestCatalogProvider(indexProvider, storageProvider, new GeotoolsFilterBuilder());
  }

  @Test
  public void testIsAvailable() {
    assertThat(abstractCatalogProvider.isAvailable(), is(true));

    when(indexProvider.isAvailable()).thenReturn(false);
    assertThat(abstractCatalogProvider.isAvailable(), is(false));

    when(indexProvider.isAvailable()).thenReturn(true);
    when(storageProvider.isAvailable()).thenReturn(false);
    assertThat(abstractCatalogProvider.isAvailable(), is(false));
  }

  @Test
  public void testQuery() throws UnsupportedQueryException {
    List<IndexQueryResult> ids =
        Arrays.asList(getQueryResult(ID1, 100.0), getQueryResult(ID2, 200.0));
    IndexQueryResponse indexResponse =
        new IndexQueryResponseImpl(mock(Request.class), ids, (long) ids.size());
    when(indexProvider.query(any())).thenReturn(indexResponse);

    List<Result> results = Arrays.asList(getResult(getMetacard(ID1)), getResult(getMetacard(ID2)));
    SourceResponse sourceResponse = new SourceResponseImpl(mock(QueryRequest.class), results);
    when(storageProvider.queryByIds(any(), any(), any())).thenReturn(sourceResponse);

    SourceResponse response = abstractCatalogProvider.query(mock(QueryRequest.class));
    assertThat(response.getResults().size(), is(2));
  }

  @Test
  public void testQueryNoResults() throws UnsupportedQueryException {
    IndexQueryResponse indexResponse =
        new IndexQueryResponseImpl(mock(Request.class), Collections.emptyList(), 0L);
    when(indexProvider.query(any())).thenReturn(indexResponse);

    SourceResponse sourceResponse =
        new SourceResponseImpl(mock(QueryRequest.class), Collections.emptyList());
    when(storageProvider.queryByIds(any(), any(), any())).thenReturn(sourceResponse);

    SourceResponse response = abstractCatalogProvider.query(mock(QueryRequest.class));
    assertThat(response.getResults().size(), is(0));
  }

  @Test
  public void testCreate() throws IngestException {
    List<Metacard> metacards = Arrays.asList(getMetacard(ID1), getMetacard(ID2));
    CreateResponse createResponse =
        new CreateResponseImpl(mock(CreateRequest.class), Collections.emptyMap(), metacards);
    when(storageProvider.create(any())).thenReturn(createResponse);
    when(indexProvider.create(any())).thenReturn(createResponse);

    CreateResponse response = abstractCatalogProvider.create(mock(CreateRequest.class));
    assertThat(response.getCreatedMetacards().size(), is(2));
  }

  @Test
  public void testDelete() throws IngestException {
    List<Metacard> metacards = Arrays.asList(getMetacard(ID1), getMetacard(ID2));
    DeleteResponse deleteResponse =
        new DeleteResponseImpl(mock(DeleteRequest.class), Collections.emptyMap(), metacards);
    when(storageProvider.delete(any())).thenReturn(deleteResponse);

    DeleteResponse response = abstractCatalogProvider.delete(mock(DeleteRequest.class));
    assertThat(response.getDeletedMetacards().size(), is(2));
  }

  @Test
  public void testUpdateById() throws IngestException {
    List<Update> updates = Collections.singletonList(getUpdate(getMetacard(ID1)));
    UpdateRequest updateRequest = new UpdateRequestImpl(ID1, getMetacard(ID1));
    UpdateResponse updateResponse =
        new UpdateResponseImpl(mock(UpdateRequest.class), Collections.emptyMap(), updates);
    when(storageProvider.update(any())).thenReturn(updateResponse);
    when(indexProvider.update(any())).thenReturn(updateResponse);

    UpdateResponse response = abstractCatalogProvider.update(updateRequest);
    assertThat(response.getUpdatedMetacards().size(), is(1));
  }

  @Test
  public void testUpdateByOther() throws IngestException, UnsupportedQueryException {
    String title = getTitle(ID1);
    List<Entry<Serializable, Metacard>> updateList =
        Collections.singletonList(new SimpleEntry(title, getMetacard(ID2)));
    List<Update> updates = Collections.singletonList(getUpdate(getMetacard(ID1)));
    UpdateRequest updateRequest =
        new UpdateRequestImpl(updateList, Core.TITLE, Collections.emptyMap());
    UpdateResponse updateResponse =
        new UpdateResponseImpl(mock(UpdateRequest.class), Collections.emptyMap(), updates);
    IndexQueryResponse indexResponse =
        new IndexQueryResponseImpl(
            mock(Request.class), Collections.singletonList(getQueryResult(ID1, 100.0)), 1L);
    List<Result> results = Collections.singletonList(getResult(getMetacard(ID1)));
    SourceResponse sourceResponse = new SourceResponseImpl(mock(QueryRequest.class), results);
    when(indexProvider.query(any())).thenReturn(indexResponse);
    when(storageProvider.queryByIds(any(), any(), any())).thenReturn(sourceResponse);
    when(storageProvider.update(any())).thenReturn(updateResponse);
    when(indexProvider.update(any())).thenReturn(updateResponse);

    UpdateResponse response = abstractCatalogProvider.update(updateRequest);
    assertThat(response.getUpdatedMetacards().size(), is(1));

    Metacard newCard = response.getUpdatedMetacards().iterator().next().getNewMetacard();
    assertThat(newCard.getAttribute(Core.TITLE).getValue(), is(getTitle(ID2)));
    assertThat(newCard.getId(), is(ID1));
  }

  private Metacard getMetacard(String id) {
    Metacard metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl(Core.ID, id));
    metacard.setAttribute(new AttributeImpl(Core.TITLE, getTitle(id)));
    return metacard;
  }

  private String getTitle(String id) {
    return id + "_title";
  }

  private Result getResult(Metacard metacard) {
    return new ResultImpl(metacard);
  }

  private IndexQueryResult getQueryResult(String id, Double score) {
    return new IndexQueryResultImpl(id, score);
  }

  private Update getUpdate(Metacard metacard) {
    return new UpdateImpl(metacard, metacard);
  }

  class TestCatalogProvider extends AbstractCatalogProvider {
    public TestCatalogProvider(
        IndexProvider indexProvider, StorageProvider storageProvider, FilterBuilder filterBuilder) {
      super(indexProvider, storageProvider, filterBuilder);
    }
  }
}
