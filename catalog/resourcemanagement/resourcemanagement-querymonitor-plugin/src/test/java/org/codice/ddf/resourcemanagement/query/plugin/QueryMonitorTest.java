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
package org.codice.ddf.resourcemanagement.query.plugin;

import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.CatalogProvider;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Test;

public class QueryMonitorTest {

  QueryMonitorPluginImpl qmpi = new QueryMonitorPluginImpl();

  ActiveSearch as = null;

  UUID uuid = null;

  ConcurrentHashMap<String, Serializable> propertyMap = null;

  private static final String QUERY_TEXT = "queryText";

  private static final String QUERY_REQUEST_TEXT = "queryRequestText";

  private static final String CQL_TEXT = "cqlText";

  private static final String CLIENT_TEXT = "clientText";

  @Test
  public void testActiveSearchConstructorAllParametersNull() {
    as = new ActiveSearch(null, null, null, null);
    uuid = UUID.randomUUID();
    assertThat(as.getSource(), is(nullValue()));
    assertThat(as.getUniqueID(), (is(notNullValue())));
  }

  @Test
  public void testActiveSearchConstructorNullSource() {
    uuid = UUID.randomUUID();
    as = new ActiveSearch(QUERY_TEXT, null, uuid, CLIENT_TEXT);
    assertThat(as.getCQL(), is(QUERY_TEXT));
    assertThat(as.getSource(), is(nullValue()));
    assertThat(as.getUniqueID(), is(uuid));
    assertThat(as.getClientInfo(), is(CLIENT_TEXT));
  }

  @Test
  public void testActiveSearchConstructorNullUUID() {
    CatalogProvider mockSource = mock(CatalogProvider.class);
    as = new ActiveSearch(QUERY_TEXT, mockSource, null, CLIENT_TEXT);
    assertThat(as.getCQL(), is(QUERY_TEXT));
    assertThat(as.getSource(), is(mockSource));
    assertThat(as.getUniqueID(), instanceOf(UUID.class));
    assertThat(as.getClientInfo(), is(CLIENT_TEXT));
  }

  @Test
  public void testActiveSearchConstructorNullQueryString() {
    CatalogProvider mockSource = mock(CatalogProvider.class);
    uuid = UUID.randomUUID();
    as = new ActiveSearch(null, mockSource, uuid, CLIENT_TEXT);
    assertThat(as.getSource(), is(mockSource));
    assertThat(as.getUniqueID(), is(uuid));
    assertThat(as.getClientInfo(), is(CLIENT_TEXT));
  }

  @Test
  public void testActiveSearchConstructorValidInputs() {
    CatalogProvider mockSource = mock(CatalogProvider.class);
    uuid = UUID.randomUUID();
    as = new ActiveSearch(CQL_TEXT, mockSource, uuid, CLIENT_TEXT);
    assertThat(as.getCQL(), is(CQL_TEXT));
    assertThat(as.getSource(), is(mockSource));
    assertThat(as.getUniqueID(), is(uuid));
    assertThat(as.getClientInfo(), is(CLIENT_TEXT));
  }

  @Test
  public void testQueryMonitorAddActiveSearchParameterNull() {
    as = null;
    assertThat(qmpi.addActiveSearch(as), is(false));
  }

  @Test
  public void testQueryMonitorAddActiveSearchWithValidInput() {
    as = new ActiveSearch(null, null, null, null);
    assertThat(qmpi.addActiveSearch(as), is(true));
  }

  @Test
  public void testQueryMonitorPluginImplRemoveActiveSearchUsingUUIDNullUUID()
      throws StopProcessingException, PluginExecutionException {
    QueryRequest mockQR = mock(QueryRequest.class);
    Query mockQuery = mock(Query.class);
    propertyMap = new ConcurrentHashMap<>();
    when(mockQR.getProperties()).thenReturn(propertyMap);
    when(mockQR.toString()).thenReturn(QUERY_REQUEST_TEXT);
    when(mockQR.getQuery()).thenReturn(mockQuery);
    when(mockQuery.accept(any(), any())).thenReturn(new StringBuilder());
    qmpi.setRemoveSearchAfterComplete(true);
    qmpi.process(null, mockQR);
    UUID u = null;
    assertThat(qmpi.removeActiveSearch(u), is(false));
    UUID uuid = qmpi.getActiveSearches().values().iterator().next().getUniqueID();
    assertThat(qmpi.removeActiveSearch(uuid), is(true));
  }

  @Test
  public void testQueryMonitorPluginImplRemoveActiveSearchUsingUUIDValidUUID()
      throws StopProcessingException, PluginExecutionException {
    QueryRequest mockQR = mock(QueryRequest.class);
    Query mockQuery = mock(Query.class);
    propertyMap = new ConcurrentHashMap<>();
    when(mockQR.getProperties()).thenReturn(propertyMap);
    when(mockQR.toString()).thenReturn(QUERY_REQUEST_TEXT);
    when(mockQR.getQuery()).thenReturn(mockQuery);
    when(mockQuery.accept(any(), any())).thenReturn(new StringBuilder());
    qmpi.setRemoveSearchAfterComplete(true);
    qmpi.process(null, mockQR);
    Map<UUID, ActiveSearch> activeSearchTable = qmpi.getActiveSearches();
    assertThat(activeSearchTable, notNullValue());
    assertThat(activeSearchTable.size(), is(1));
    UUID u = activeSearchTable.keySet().iterator().next();
    assertThat(qmpi.removeActiveSearch(u), is(true));
  }

  @Test
  public void testQueryMonitorAddActiveSearchNullParameters() {
    QueryRequest mockQR = mock(QueryRequest.class);
    propertyMap = new ConcurrentHashMap<>();
    when(mockQR.getProperties()).thenReturn(propertyMap);
    when(mockQR.toString()).thenReturn(QUERY_REQUEST_TEXT);
    as = new ActiveSearch(null, null, null, null);
    qmpi.addActiveSearch(as);
    Map<UUID, ActiveSearch> activeSearchTable = qmpi.getActiveSearches();
    assertThat(activeSearchTable, hasValue(as));
  }
}
