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

import static ddf.catalog.Constants.ADDITIONAL_SORT_BYS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.Mockito.mock;

import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codice.ddf.catalog.ui.query.utility.CqlRequest;
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

  private static final String ASC_SORT_ORDER = "asc";

  private static final String DESC_SORT_ORDER = "desc";

  private static final String SORT_PROPERTY = "DISTANCE";

  private static final String CQL = "\"anyText\\\" ILIKE '%'";

  private static final String BAD_CQL = "unknownFunction(1,2)";

  private FilterBuilder filterBuilder;
  private List<CqlRequest.Sort> sorts;

  @Before
  public void setUp() {
    cqlRequest = new CqlRequest();
    cqlRequest.setCql(CQL);
    cqlRequest.setCount(10);
    cqlRequest.setId("anId");
    cqlRequest.setNormalize(true);
    sorts = Collections.singletonList(new CqlRequest.Sort(SORT_PROPERTY, ASC_SORT_ORDER));
    cqlRequest.setSorts(sorts);
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
    assertThat(cqlRequest.getSorts(), is(sorts));
    assertThat(cqlRequest.getSource(), is("source"));
    assertThat(cqlRequest.getStart(), is(1));
    assertThat(cqlRequest.getTimeout(), is(1000L));
  }

  @Test
  public void testMultipleSorts() {
    List<CqlRequest.Sort> sorts = new ArrayList<>();
    sorts.add(new CqlRequest.Sort(SORT_PROPERTY, ASC_SORT_ORDER));
    sorts.add(new CqlRequest.Sort("foobar", DESC_SORT_ORDER));
    cqlRequest.setSorts(sorts);
    QueryRequest queryRequest = cqlRequest.createQueryRequest("ddf.distribution", filterBuilder);
    SortBy firstSort = queryRequest.getQuery().getSortBy();
    assertThat(firstSort.getPropertyName().getPropertyName(), is(SORT_PROPERTY));
    assertThat(firstSort.getSortOrder(), is(SortOrder.ASCENDING));
    SortBy[] sortBys = (SortBy[]) queryRequest.getProperties().get(ADDITIONAL_SORT_BYS);
    assertThat(sortBys.length, is(1));
    SortBy secondSort = sortBys[0];
    assertThat(secondSort.getPropertyName().getPropertyName(), is("foobar"));
    assertThat(secondSort.getSortOrder(), is(SortOrder.DESCENDING));
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
    cqlRequest.setSorts(
        Collections.singletonList(new CqlRequest.Sort(SORT_PROPERTY, DESC_SORT_ORDER)));
    cqlRequest.setSrc(CACHE_SOURCE);
    QueryRequest queryRequest = cqlRequest.createQueryRequest(CACHE_SOURCE, filterBuilder);
    SortBy sortBy = queryRequest.getQuery().getSortBy();
    assertThat(sortBy.getPropertyName().getPropertyName(), is(SORT_PROPERTY));
    assertThat(sortBy.getSortOrder(), is(SortOrder.DESCENDING));
    assertThat(queryRequest.getPropertyValue(MODE), is(CACHE_SOURCE));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadSortOrder() {
    cqlRequest.setSorts(Collections.singletonList(new CqlRequest.Sort(SORT_PROPERTY, "bad")));
    cqlRequest.createQueryRequest(CACHE_SOURCE, filterBuilder);
  }

  @Test
  public void testBadSortOrderString() {
    cqlRequest.setSorts(Collections.singletonList(new CqlRequest.Sort(SORT_PROPERTY, null)));
    QueryRequest queryRequest = cqlRequest.createQueryRequest(LOCAL_SOURCE, filterBuilder);
    assertDefaultSortBy(queryRequest);
  }

  @Test
  public void testBlankSortOrder() {
    cqlRequest.setSorts(Collections.emptyList());
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
  public void testMultipleSources() {
    ArrayList<String> sources = new ArrayList<>();
    sources.add("local");
    sources.add("source2");
    cqlRequest.setSrcs(sources);
    cqlRequest.createQueryRequest("SOURCE1", filterBuilder);
    assertThat(cqlRequest.getSrcs(), contains("SOURCE1", "source2"));
    assertThat(cqlRequest.getSrcs().size(), is(2));
  }

  @Test
  public void testSingleSourceResponseString() {
    cqlRequest.createQueryRequest("", filterBuilder);
    assertThat(cqlRequest.getSourceResponseString(), is("source"));
  }

  @Test
  public void testMultipleSourceResponseString() {
    ArrayList<String> sources = new ArrayList<>();
    sources.add("source1");
    sources.add("source2");
    cqlRequest.setSrcs(sources);
    cqlRequest.createQueryRequest("source1", filterBuilder);
    assertThat(cqlRequest.getSourceResponseString(), is("source1, source2"));
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
