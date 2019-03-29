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
package org.codice.ddf.catalog.ui.metacard.workspace.transformer.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import java.util.Collections;
import java.util.List;
import org.codice.ddf.catalog.ui.metacard.query.data.metacard.QueryMetacardImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssociatedQueryMetacardsHandlerTest {

  @Mock private CatalogFramework catalogFramework;

  private AssociatedQueryMetacardsHandler queryMetacardsHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    this.catalogFramework = mock(CatalogFramework.class);
    this.queryMetacardsHandler = new AssociatedQueryMetacardsHandler(catalogFramework);
  }

  @Test
  public void testCreateQueryMetacards() throws IngestException, SourceUnavailableException {
    List<Metacard> updatedQueryMetacards = Collections.singletonList(new MetacardImpl());
    doReturn(mock(CreateResponse.class)).when(catalogFramework).create(any(CreateRequest.class));

    queryMetacardsHandler.create(Collections.emptyList(), updatedQueryMetacards);

    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
  }

  @Test
  public void testCreateQueryMetacardsNoCreations()
      throws IngestException, SourceUnavailableException {
    queryMetacardsHandler.create(Collections.emptyList(), Collections.emptyList());

    verify(catalogFramework, times(0)).create(any(CreateRequest.class));
  }

  @Test
  public void testDeleteQueryMetacards() throws IngestException, SourceUnavailableException {
    queryMetacardsHandler.delete(Collections.singletonList("queryId"), Collections.emptyList());

    verify(catalogFramework, times(1)).delete(any(DeleteRequest.class));
  }

  @Test
  public void testDeleteQueryMetacardsNoDeletions()
      throws IngestException, SourceUnavailableException {
    queryMetacardsHandler.delete(Collections.emptyList(), Collections.emptyList());

    verify(catalogFramework, times(0)).delete(any(DeleteRequest.class));
  }

  @Test
  public void testUpdateQueryMetacards() throws IngestException, SourceUnavailableException {
    QueryMetacardImpl existingQuery = createQueryMetacard("old title", "queryId");
    QueryMetacardImpl updatedQuery = createQueryMetacard("new title", "queryId");

    queryMetacardsHandler.update(
        Collections.singletonList("queryId"),
        Collections.singletonList(existingQuery),
        Collections.singletonList(updatedQuery));

    verify(catalogFramework, times(1)).update(any(UpdateRequest.class));
  }

  @Test
  public void testUpdateQueryMetacardsNoUpdates()
      throws IngestException, SourceUnavailableException {
    QueryMetacardImpl existingQuery = mock(QueryMetacardImpl.class);
    doReturn("queryId").when(existingQuery).getId();
    doReturn(new AttributeImpl("title", "title")).when(existingQuery).getAttribute("title");
    QueryMetacardImpl updatedQuery = existingQuery;

    queryMetacardsHandler.update(
        Collections.singletonList("queryId"),
        Collections.singletonList(existingQuery),
        Collections.singletonList(updatedQuery));

    verify(catalogFramework, times(0)).update(any(UpdateRequest.class));
  }

  @Test
  public void testUpdateQueryMetacardsNoUpdatesEmptyQueries()
      throws IngestException, SourceUnavailableException {
    queryMetacardsHandler.update(
        Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    verify(catalogFramework, times(0)).update(any(UpdateRequest.class));
  }

  private QueryMetacardImpl createQueryMetacard(String title, String id) {
    QueryMetacardImpl query = new QueryMetacardImpl(title);
    query.setId(id);
    return query;
  }
}
