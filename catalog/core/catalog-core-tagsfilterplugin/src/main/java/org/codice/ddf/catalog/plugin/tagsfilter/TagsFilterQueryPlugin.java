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
package org.codice.ddf.catalog.plugin.tagsfilter;

import java.util.ArrayList;
import java.util.List;

import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.delegate.TagsFilterDelegate;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreFederatedQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceCache;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * When no {@code tags} filter is present on a query, adds a default tag of {@code resource}.
 * A filter will also be added to include metacards without any tags attribute to support
 * backwards compatibility.
 */
public class TagsFilterQueryPlugin implements PreFederatedQueryPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(TagsFilterQueryPlugin.class);

    private final List<CatalogProvider> catalogProviders;

    private final FilterAdapter filterAdapter;

    private final FilterBuilder filterBuilder;

    public TagsFilterQueryPlugin(List<CatalogProvider> catalogProviders,
            FilterAdapter filterAdapter, FilterBuilder filterBuilder) {
        this.catalogProviders = catalogProviders;
        this.filterAdapter = filterAdapter;
        this.filterBuilder = filterBuilder;
    }

    private boolean isCacheSource(Source source) {
        return source instanceof SourceCache;
    }

    private boolean isCatalogProvider(String id) {
        return id != null && catalogProviders.stream()
                .map(CatalogProvider::getId)
                .anyMatch(id::equals);
    }

    /**
     * Given a source, determine if it is a registered catalog provider or a cache.
     */
    private boolean isLocalSource(Source source) {
        return isCacheSource(source) || isCatalogProvider(source.getId());
    }

    @Override
    public QueryRequest process(Source source, QueryRequest input)
            throws PluginExecutionException, StopProcessingException {
        if (!isLocalSource(source)) {
            return input;
        }

        QueryRequest request = input;
        try {
            Query query = request.getQuery();
            if (filterAdapter.adapt(query, new TagsFilterDelegate())) {
                return request;
            }

            List<Filter> filters = new ArrayList<>();
            //no tags filter given in props or in query. Add the default ones.
            filters.add(filterBuilder.attribute(Metacard.TAGS)
                    .is()
                    .like()
                    .text(Metacard.DEFAULT_TAG));
            filters.add(filterBuilder.attribute(Metacard.TAGS)
                    .empty());
            Filter newFilter = filterBuilder.allOf(filterBuilder.anyOf(filters), query);

            QueryImpl newQuery = new QueryImpl(newFilter,
                    query.getStartIndex(),
                    query.getPageSize(),
                    query.getSortBy(),
                    query.requestsTotalResultsCount(),
                    query.getTimeoutMillis());
            request = new QueryRequestImpl(newQuery,
                    request.isEnterprise(),
                    request.getSourceIds(),
                    request.getProperties());
        } catch (UnsupportedQueryException uqe) {
            LOGGER.info("Unable to update query with default tags filter");
        }
        return request;
    }
}
