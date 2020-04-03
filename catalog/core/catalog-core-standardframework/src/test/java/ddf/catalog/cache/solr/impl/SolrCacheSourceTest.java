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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Result;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.source.UnsupportedQueryException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;

public class SolrCacheSourceTest {

  private ContentType contentType;

  private QueryRequest queryRequest;

  private SourceResponse sourceResponse;

  private SolrCache solrCache;

  private SolrCacheSource solrCacheSource;

  private List<Result> results;

  @Before
  public void setup() throws Exception {
    contentType = mock(ContentType.class);
    when(contentType.getName()).thenReturn("Content-Type");
    when(contentType.getVersion()).thenReturn("1.0");
    when(contentType.getNamespace()).thenReturn(URI.create("http://foo.bar"));

    queryRequest = mock(QueryRequest.class);
    sourceResponse = mock(SourceResponse.class);
    when(sourceResponse.getHits()).thenReturn(5L);
    when(sourceResponse.getProperties())
        .thenReturn(ImmutableMap.of("key1", "val1", "key2", "val2"));
    results =
        IntStream.rangeClosed(1, 5).mapToObj(i -> mock(Result.class)).collect(Collectors.toList());
    when(sourceResponse.getResults()).thenReturn(results);

    solrCache = mock(SolrCache.class);
    when(solrCache.getContentTypes()).thenReturn(ImmutableSet.of(contentType));
    when(solrCache.query(queryRequest)).thenReturn(sourceResponse);

    solrCacheSource = new SolrCacheSource(solrCache);
  }

  @Test
  public void testDescribableProperties() {
    assertThat(solrCacheSource.getId(), is(SolrCacheSource.class.getName()));
    assertThat(solrCacheSource.getVersion(), is("1.0-TEST"));
    assertThat(solrCacheSource.getTitle(), is("SolrCacheSource"));
    assertThat(solrCacheSource.getDescription(), is("SolrCacheSource Test"));
    assertThat(solrCacheSource.getOrganization(), is("Codice"));
  }

  @Test
  public void testContentTypes() {
    assertThat(solrCacheSource.getContentTypes(), containsInAnyOrder(contentType));
  }

  @Test
  public void goodQuery() throws Exception {
    SourceResponse queryResponse = solrCacheSource.query(queryRequest);
    assertThat(queryResponse.getHits(), is(sourceResponse.getHits()));
    assertThat(queryResponse.getProperties().size(), is(sourceResponse.getProperties().size()));
    queryResponse.getProperties().entrySet().stream()
        .forEach(e -> assertThat(e.getValue(), is(sourceResponse.getProperties().get(e.getKey()))));

    assertThat(queryResponse.getResults().size(), is(sourceResponse.getResults().size()));
    queryResponse.getResults().stream()
        .forEach(r -> assertTrue(sourceResponse.getResults().contains(r)));
  }

  @Test
  public void badQuery() throws Exception {
    when(solrCache.query(queryRequest)).thenThrow(new UnsupportedQueryException("Failed"));
    SourceResponse queryResponse = solrCacheSource.query(queryRequest);
    assertThat(queryResponse.getProcessingDetails().size(), greaterThan(0));
    ProcessingDetails processingDetail =
        queryResponse.getProcessingDetails().stream()
            .filter(ProcessingDetails.class::isInstance)
            .map(ProcessingDetails.class::cast)
            .filter(pd -> pd.getSourceId().equals(solrCacheSource.getId()))
            .findFirst()
            .orElse(null);
    assertThat(processingDetail, notNullValue());
    assertThat(processingDetail.getException(), instanceOf(UnsupportedQueryException.class));
  }
}
