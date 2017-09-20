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

import ddf.catalog.data.types.Validation;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.delegate.ValidationQueryDelegate;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.ArrayList;
import java.util.List;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationQueryFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationQueryFactory.class);

  private FilterAdapter adapter;

  private FilterBuilder builder;

  public ValidationQueryFactory(FilterAdapter filterAdapter, FilterBuilder filterBuilder) {
    adapter = filterAdapter;
    builder = filterBuilder;
  }

  QueryRequest getQueryRequestWithValidationFilter(
      QueryRequest input, Boolean showErrors, Boolean showWarnings) {
    Query inputQuery = input.getQuery();
    try {
      if ((showErrors && showWarnings)
          || adapter.adapt(input.getQuery(), new ValidationQueryDelegate())) {
        return input;
      }

    } catch (UnsupportedQueryException e) {
      LOGGER.info("This attribute filter is not supported by ValidationQueryDelegate.", e);
    }
    List<Filter> filters = new ArrayList<>();
    if (!showErrors) {
      filters.add(builder.attribute(Validation.VALIDATION_ERRORS).is().empty());
    }
    if (!showWarnings) {
      filters.add(builder.attribute(Validation.VALIDATION_WARNINGS).is().empty());
    }
    QueryImpl query =
        new QueryImpl(
            builder.allOf(builder.allOf(filters), inputQuery),
            inputQuery.getStartIndex(),
            inputQuery.getPageSize(),
            inputQuery.getSortBy(),
            inputQuery.requestsTotalResultsCount(),
            inputQuery.getTimeoutMillis());
    return new QueryRequestImpl(
        query, input.isEnterprise(), input.getSourceIds(), input.getProperties());
  }

  public Filter getFilterWithValidationFilter() {
    return builder.anyOf(
        builder
            .attribute(Validation.VALIDATION_ERRORS)
            .is()
            .like()
            .text(FilterDelegate.WILDCARD_CHAR),
        builder.attribute(Validation.VALIDATION_ERRORS).empty(),
        builder
            .attribute(Validation.VALIDATION_WARNINGS)
            .is()
            .like()
            .text(FilterDelegate.WILDCARD_CHAR),
        builder.attribute(Validation.VALIDATION_WARNINGS).empty());
  }

  QueryRequest getQueryRequestWithValidationFilter(QueryRequest input) {
    return getQueryRequestWithValidationFilter(input, false, true);
  }
}
