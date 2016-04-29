/**
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
package ddf.catalog.cache.solr.impl;

import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.delegate.ValidationQueryDelegate;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;

public class ValidationQueryFactory {

    private FilterAdapter adapter;

    private FilterBuilder builder;

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationQueryFactory.class);

    public ValidationQueryFactory(FilterAdapter filterAdapter, FilterBuilder filterBuilder) {
        adapter = filterAdapter;
        builder = filterBuilder;
    }

    public QueryRequest getQueryRequestWithValidationFilter(QueryRequest input,
            boolean showInvalidMetacards) {
        Query inputQuery = input.getQuery();
        try {
            if (showInvalidMetacards || adapter.adapt(input.getQuery(),
                    new ValidationQueryDelegate())) {
                return input;
            }

        } catch (UnsupportedQueryException e) {
            LOGGER.warn("This attribute filter is not supported by ValidationQueryDelegate.", e);
        }
        QueryImpl query = new QueryImpl(builder.allOf(builder.allOf(
                builder.attribute(BasicTypes.VALIDATION_ERRORS)
                        .is()
                        .empty(),
                builder.attribute(BasicTypes.VALIDATION_WARNINGS)
                        .is()
                        .empty()), inputQuery),
                inputQuery.getStartIndex(),
                inputQuery.getPageSize(),
                inputQuery.getSortBy(),
                inputQuery.requestsTotalResultsCount(),
                inputQuery.getTimeoutMillis());
        return new QueryRequestImpl(query,
                input.isEnterprise(),
                input.getSourceIds(),
                input.getProperties());
    }

    public Filter getFilterWithValidationFilter() {
        return builder.anyOf(builder.attribute(BasicTypes.VALIDATION_ERRORS)
                        .is()
                        .like()
                        .text(FilterDelegate.WILDCARD_CHAR),
                builder.attribute(BasicTypes.VALIDATION_ERRORS)
                        .empty(),
                builder.attribute(BasicTypes.VALIDATION_WARNINGS)
                        .is()
                        .like()
                        .text(FilterDelegate.WILDCARD_CHAR),
                builder.attribute(BasicTypes.VALIDATION_WARNINGS)
                        .empty());
    }

    public QueryRequest getQueryRequestWithValidationFilter(QueryRequest input) {
        return getQueryRequestWithValidationFilter(input, false);
    }

}
