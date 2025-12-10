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

import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.solr.SchemaFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.geotools.api.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheQueryFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheQueryFactory.class);

  private FilterBuilder builder;

  public CacheQueryFactory(FilterBuilder filterBuilder) {
    Validate.notNull(filterBuilder, "Valid FilterBuilder required.");
    builder = filterBuilder;
  }

  QueryRequest getQueryRequestWithSourcesFilter(QueryRequest input) {
    if (input.getProperties().get("cache-sources") == null) {
      LOGGER.debug("cache-sources property not provided. Searching all sources in cache.");
      return input;
    }

    List<String> sourceIds =
        Arrays.asList(input.getProperties().get("cache-sources").toString().split(","));
    QueryRequest queryWithSources = input;
    if (sourceIds.size() > 0) {
      List<Filter> sourceFilters = new ArrayList<>();
      for (String sourceId : sourceIds) {
        sourceFilters.add(
            builder
                .attribute(
                    StringUtils.removeEnd(SolrCache.METACARD_SOURCE_NAME, SchemaFields.TEXT_SUFFIX))
                .is()
                .equalTo()
                .text(sourceId));
      }
      QueryImpl sourceQuery =
          new QueryImpl(builder.allOf(input.getQuery(), builder.anyOf(sourceFilters)));
      sourceQuery.setPageSize(input.getQuery().getPageSize());
      sourceQuery.setStartIndex(input.getQuery().getStartIndex());
      sourceQuery.setSortBy(input.getQuery().getSortBy());
      sourceQuery.setTimeoutMillis(input.getQuery().getTimeoutMillis());
      queryWithSources =
          new QueryRequestImpl(
              sourceQuery, input.isEnterprise(), input.getSourceIds(), input.getProperties());
    }
    return queryWithSources;
  }
}
