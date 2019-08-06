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

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.delegate.TagsFilterDelegate;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PreFederatedLocalProviderQueryPlugin;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.Source;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.List;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When no {@code tags} filter is present on a query, adds a default tag of {@code resource}. A
 * filter will also be added to include metacards without any tags attribute to support backwards
 * compatibility.
 */
public class TagsFilterQueryPlugin extends PreFederatedLocalProviderQueryPlugin {
  private static final Logger LOGGER = LoggerFactory.getLogger(TagsFilterQueryPlugin.class);

  private final FilterAdapter filterAdapter;

  private final FilterBuilder filterBuilder;

  public TagsFilterQueryPlugin(
      List<CatalogProvider> catalogProviders,
      FilterAdapter filterAdapter,
      FilterBuilder filterBuilder) {
    super(catalogProviders);
    this.filterAdapter = filterAdapter;
    this.filterBuilder = filterBuilder;
  }

  @Override
  public QueryRequest process(Source source, QueryRequest input) {
    if (!isLocalSource(source)) {
      return input;
    }

    QueryRequest request = input;
    try {
      Query query = request.getQuery();
      if (filterAdapter.adapt(query, new TagsFilterDelegate())) {
        return request;
      }

      Filter newFilter =
          filterBuilder.allOf(
              query, filterBuilder.attribute(Metacard.TAGS).is().like().text(Metacard.DEFAULT_TAG));

      QueryImpl newQuery =
          new QueryImpl(
              newFilter,
              query.getStartIndex(),
              query.getPageSize(),
              query.getSortBy(),
              query.requestsTotalResultsCount(),
              query.getTimeoutMillis());
      request =
          new QueryRequestImpl(
              newQuery, request.isEnterprise(), request.getSourceIds(), request.getProperties());
    } catch (UnsupportedQueryException uqe) {
      LOGGER.debug("Unable to update query with default tags filter", uqe);
    }
    return request;
  }
}
