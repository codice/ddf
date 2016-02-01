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

package ddf.catalog.registry.metacard.filter;

import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.delegate.CopyFilterDelegate;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.registry.common.filter.RegistryQueryDelegate;
import ddf.catalog.source.UnsupportedQueryException;

public class RegistryMetacardFilterPlugin implements PreQueryPlugin {

    public static final String REGISTRY_CONTENT_TYPE = "registry";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RegistryMetacardFilterPlugin.class);

    private static final String ENTERING = "ENTERING {}";

    private static final String EXITING = "EXITING {}";

    private FilterAdapter filterAdapter;

    private FilterBuilder filterBuilder;

    public RegistryMetacardFilterPlugin(FilterBuilder filterBuilder, FilterAdapter filterAdapter) {
        LOGGER.trace("INSIDE: RegistryMetacardFilterPlugin constructor");
        this.filterAdapter = filterAdapter;
        this.filterBuilder = filterBuilder;
    }

    @Override
    public QueryRequest process(QueryRequest input) throws PluginExecutionException {
        String methodName = "process";
        LOGGER.trace(ENTERING, methodName);
        QueryRequest newQueryRequest = input;
        Query query = input.getQuery();
        try {
            if (query != null) {
                if (!filterAdapter.adapt(query, new RegistryQueryDelegate())) {
                    FilterDelegate<Filter> delegate = new CopyFilterDelegate(filterBuilder);
                    Filter copiedFilter = filterAdapter.adapt(query, delegate);
                    /*
                    If this is NOT a query for registry metacards,
                    filter out registry metacards in the query
                     */

                    Filter registryContentTypeFilter = filterBuilder.not(filterBuilder.attribute(
                            Metacard.CONTENT_TYPE)
                            .is()
                            .like()
                            .text(REGISTRY_CONTENT_TYPE + RegistryQueryDelegate.WILDCARD_CHAR));
                    LOGGER.debug("Filtering out registry metacards with filter : {}",
                            registryContentTypeFilter.toString());
                    Filter filter = filterBuilder.allOf(registryContentTypeFilter, copiedFilter);
                    LOGGER.debug("Registry metacard filter combined with the original filter : {}",
                            filter.toString());

                    QueryImpl newQuery = new QueryImpl(filter,
                            query.getStartIndex(),
                            query.getPageSize(),
                            query.getSortBy(),
                            query.requestsTotalResultsCount(),
                            query.getTimeoutMillis());
                    newQueryRequest = new QueryRequestImpl(newQuery,
                            input.isEnterprise(),
                            input.getSourceIds(),
                            input.getProperties());
                }
            }
        } catch (UnsupportedQueryException e) {
            throw new PluginExecutionException(e);
        }
        LOGGER.trace(EXITING, methodName);
        return newQueryRequest;
    }
}
