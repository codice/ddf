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
package org.codice.ddf.catalog.plugin.tagsfilter;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import ddf.catalog.data.Metacard;
import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.ContextualExpressionBuilder;
import ddf.catalog.filter.ExpressionBuilder;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceCache;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.And;
import org.opengis.filter.Filter;

public class TagsFilterQueryPluginTest {
  private TagsFilterQueryPlugin plugin;

  private QueryRequest queryRequest;

  private Source source;

  private SourceCache cache;

  private FilterAdapter filterAdapter;

  private FilterBuilder filterBuilder;

  private Query query;

  @Before
  public void setup() {
    CatalogProvider catProvider1 = mock(CatalogProvider.class);
    CatalogProvider catProvider2 = mock(CatalogProvider.class);
    CatalogProvider catProvider3 = mock(CatalogProvider.class);
    when(catProvider1.getId()).thenReturn("cat1");
    when(catProvider2.getId()).thenReturn("cat2");
    when(catProvider3.getId()).thenReturn("cat3");

    ImmutableList<CatalogProvider> catalogProviders =
        ImmutableList.of(catProvider1, catProvider2, catProvider3);
    filterAdapter = mock(FilterAdapter.class);
    filterBuilder = mock(FilterBuilder.class);

    plugin = new TagsFilterQueryPlugin(catalogProviders, filterAdapter, filterBuilder);

    source = mock(CatalogProvider.class);
    when(source.getId()).thenReturn("cat2");

    cache = mock(SourceCache.class);
    when(cache.getId()).thenReturn("cache");

    queryRequest = mock(QueryRequest.class);
    query = mock(Query.class);
    when(queryRequest.getQuery()).thenReturn(query);
  }

  @Test
  public void notACatalogProvider() throws Exception {
    when(source.getId()).thenReturn("not a catalog provider");
    QueryRequest process = plugin.process(source, queryRequest);

    assertThat(process, is(queryRequest));
    verify(filterAdapter, never()).adapt(any(), any());
  }

  @Test
  public void adapterTrue() throws Exception {
    when(filterAdapter.adapt(any(), any())).thenReturn(true);
    QueryRequest process = plugin.process(source, queryRequest);

    assertThat(process, is(queryRequest));
    verify(filterBuilder, never()).attribute(anyString());
  }

  @Test
  public void addTagsToCatalogProvider() throws Exception {
    AttributeBuilder attributeBuilder = mock(AttributeBuilder.class);
    ExpressionBuilder expressionBuilder = mock(ExpressionBuilder.class);
    ContextualExpressionBuilder contextualExpressionBuilder =
        mock(ContextualExpressionBuilder.class);
    Filter defaultTagFilter = mock(Filter.class);

    when(attributeBuilder.is()).thenReturn(expressionBuilder);
    when(expressionBuilder.like()).thenReturn(contextualExpressionBuilder);
    when(contextualExpressionBuilder.text(Metacard.DEFAULT_TAG)).thenReturn(defaultTagFilter);
    when(filterBuilder.allOf(query, defaultTagFilter)).thenReturn(mock(And.class));

    when(filterAdapter.adapt(any(), any())).thenReturn(false);
    when(filterBuilder.attribute(Metacard.TAGS)).thenReturn(attributeBuilder);
    QueryRequest process = plugin.process(source, queryRequest);

    assertThat(process, not(queryRequest));
  }

  @Test
  public void addTagsToCacheSource() throws Exception {
    AttributeBuilder attributeBuilder = mock(AttributeBuilder.class);
    ExpressionBuilder expressionBuilder = mock(ExpressionBuilder.class);
    ContextualExpressionBuilder contextualExpressionBuilder =
        mock(ContextualExpressionBuilder.class);
    Filter defaultTagFilter = mock(Filter.class);

    when(attributeBuilder.is()).thenReturn(expressionBuilder);
    when(expressionBuilder.like()).thenReturn(contextualExpressionBuilder);
    when(contextualExpressionBuilder.text(Metacard.DEFAULT_TAG)).thenReturn(defaultTagFilter);
    when(filterBuilder.allOf(query, defaultTagFilter)).thenReturn(mock(And.class));

    when(filterAdapter.adapt(any(), any())).thenReturn(false);
    when(filterBuilder.attribute(Metacard.TAGS)).thenReturn(attributeBuilder);
    QueryRequest process = plugin.process(cache, queryRequest);

    assertThat(process, not(queryRequest));
  }
}
