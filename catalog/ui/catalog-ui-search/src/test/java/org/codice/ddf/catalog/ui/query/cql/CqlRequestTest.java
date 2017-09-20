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
package org.codice.ddf.catalog.ui.query.cql;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import spark.HaltException;

public class CqlRequestTest {

  private CqlRequest cqlRequest;

  private static final String LOCAL_SOURCE = "local";

  private static final String MODE = "mode";

  private static final String UPDATE = "update";

  private static final String CACHE_SOURCE = "cache";

  private static final String ASC_SORT_ORDER = ":asc";

  private static final String DESC_SORT_ORDER = ":desc";

  private static final String SORT_PROPERTY = "DISTANCE";

  private static final String CQL = "\"anyText\\\" ILIKE '%'";

  private static final String BAD_CQL = "unknownFunction(1,2)";

  private FilterBuilder filterBuilder;

  @Before
  public void setUp() {
    cqlRequest = new CqlRequest();
    cqlRequest.setCql(CQL);
    cqlRequest.setCount(10);
    cqlRequest.setId("anId");
    cqlRequest.setNormalize(true);
    cqlRequest.setSort(SORT_PROPERTY + ASC_SORT_ORDER);
    cqlRequest.setSrc("source");
    cqlRequest.setStart(1);
    cqlRequest.setTimeout(1000L);
    filterBuilder = mock(FilterBuilder.class);
  }

  @Test
  public void testGetters() {
    assertThat(cqlRequest.getCount(), is(10));
    assertThat(cqlRequest.getCql(), is(CQL));
    assertThat(cqlRequest.getId(), is("anId"));
    assertThat(cqlRequest.isNormalize(), is(true));
    assertThat(cqlRequest.getSort(), is(SORT_PROPERTY + ASC_SORT_ORDER));
    assertThat(cqlRequest.getSource(), is("source"));
    assertThat(cqlRequest.getStart(), is(1));
    assertThat(cqlRequest.getTimeout(), is(1000L));
  }

  @Test
  public void testCreateQueryWithLocalSource() {
    QueryRequest queryRequest = cqlRequest.createQueryRequest(LOCAL_SOURCE, filterBuilder);
    SortBy sortBy = queryRequest.getQuery().getSortBy();
    assertThat(sortBy.getPropertyName().getPropertyName(), is(SORT_PROPERTY));
    assertThat(sortBy.getSortOrder(), is(SortOrder.ASCENDING));
    assertThat(queryRequest.getPropertyValue(MODE), is(UPDATE));
  }

  @Test
  public void testCreateQueryWithCacheSource() {
    cqlRequest.setSort(SORT_PROPERTY + DESC_SORT_ORDER);
    cqlRequest.setSrc(CACHE_SOURCE);
    QueryRequest queryRequest = cqlRequest.createQueryRequest(CACHE_SOURCE, filterBuilder);
    SortBy sortBy = queryRequest.getQuery().getSortBy();
    assertThat(sortBy.getPropertyName().getPropertyName(), is(SORT_PROPERTY));
    assertThat(sortBy.getSortOrder(), is(SortOrder.DESCENDING));
    assertThat(queryRequest.getPropertyValue(MODE), is(CACHE_SOURCE));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadSortOrder() {
    cqlRequest.setSort(SORT_PROPERTY + ":bad");
    cqlRequest.createQueryRequest(CACHE_SOURCE, filterBuilder);
  }

  @Test
  public void testBadSortOrderString() {
    cqlRequest.setSort(SORT_PROPERTY);
    QueryRequest queryRequest = cqlRequest.createQueryRequest(LOCAL_SOURCE, filterBuilder);
    assertDefaultSortBy(queryRequest);
  }

  @Test
  public void testBlankSortOrder() {
    cqlRequest.setSort("");
    QueryRequest queryRequest = cqlRequest.createQueryRequest(LOCAL_SOURCE, filterBuilder);
    assertDefaultSortBy(queryRequest);
  }

  @Test(expected = HaltException.class)
  public void testBadCql() {
    cqlRequest.setCql(BAD_CQL);
    cqlRequest.createQueryRequest(LOCAL_SOURCE, filterBuilder);
  }

  @Test
  public void testBlankLocalSource() {
    cqlRequest.createQueryRequest("", filterBuilder);
    assertThat(cqlRequest.getSource(), is("source"));
  }

  @Test
  public void testLocalSource() {
    cqlRequest.setSrc(LOCAL_SOURCE);
    cqlRequest.createQueryRequest(LOCAL_SOURCE, filterBuilder);
    assertThat(cqlRequest.getSource(), is(LOCAL_SOURCE));
  }

  private void assertDefaultSortBy(QueryRequest queryRequest) {
    SortBy sortBy = queryRequest.getQuery().getSortBy();
    assertThat(sortBy.getPropertyName().getPropertyName(), is(Result.TEMPORAL));
    assertThat(sortBy.getSortOrder(), is(SortOrder.DESCENDING));
    assertThat(queryRequest.getPropertyValue(MODE), is(UPDATE));
  }
}
