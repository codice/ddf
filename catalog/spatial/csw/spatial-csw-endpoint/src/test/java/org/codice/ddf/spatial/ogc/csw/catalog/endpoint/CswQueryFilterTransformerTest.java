/*
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.mappings.MetacardCswRecordMap;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer.CswQueryFilterTransformer;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

public class CswQueryFilterTransformerTest {
  private CswQueryFilterTransformer transformer;

  @Before
  public void setUp() {
    transformer =
        new CswQueryFilterTransformer(new MetacardCswRecordMap(), mock(AttributeRegistry.class));
  }

  @Test
  public void testSameQueryProperties() {
    Query originalQuery = mockQuery(1, 10, null, true, 1000);
    QueryRequest request = mock(QueryRequest.class);
    when(request.getQuery()).thenReturn(originalQuery);

    QueryRequest result = transformer.transform(request, null);
    Query query = result.getQuery();

    assertThat(query.getPageSize(), equalTo(originalQuery.getPageSize()));
    assertThat(query.getSortBy(), equalTo(originalQuery.getSortBy()));
    assertThat(query.getStartIndex(), equalTo(originalQuery.getStartIndex()));
    assertThat(query.getTimeoutMillis(), equalTo(originalQuery.getTimeoutMillis()));
    assertThat(
        query.requestsTotalResultsCount(), equalTo(originalQuery.requestsTotalResultsCount()));
  }

  private Query mockQuery(
      int startIndex, int pageSize, SortBy sortBy, boolean requestTotalCount, long timeout) {
    Filter filter = new GeotoolsFilterBuilder().attribute("title").is().like().text("something");

    if (sortBy == null) {
      sortBy = mock(SortBy.class);
    }

    Query query = new QueryImpl(filter, startIndex, pageSize, sortBy, requestTotalCount, timeout);

    return query;
  }
}
