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
 **/
package org.codice.ddf.ui.searchui.query.controller.search;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.codice.ddf.ui.searchui.query.controller.SearchController;
import org.codice.ddf.ui.searchui.query.model.Search;
import org.codice.ddf.ui.searchui.query.model.SearchRequest;
import org.codice.ddf.ui.searchui.query.solr.FilteringSolrIndex;
import org.cometd.bayeux.server.ServerSession;

import ddf.catalog.data.Result;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.security.Subject;

public class CacheQueryRunnable extends QueryRunnable {

    private final Future<FilteringSolrIndex> solrIndexFuture;

    public CacheQueryRunnable(SearchController searchController, SearchRequest request,
            Subject subject, Search search, ServerSession session, Map<String, Result> results,
            Future<FilteringSolrIndex> solrIndexFuture) {
        super(searchController, request, subject, search, session, results);
        this.solrIndexFuture = solrIndexFuture;
    }

    @Override
    public void run() {
        // check if there are any currently cached results
        QueryResponse response = queryCatalog(null,
                request,
                subject,
                new HashMap<>(CACHE_PROPERTIES));

        search.update(response);

        try {
            searchController.publishResults(request.getId(),
                    search.transform(request.getId()),
                    session);
        } catch (CatalogTransformerException e) {
            LOGGER.error("Failed to transform cached search results.", e);
        }

        addResults(response.getResults());
        indexResults(response);
    }

    private void indexResults(QueryResponse response) {
        if (searchController.shouldNormalizeRelevance(request)) {
            try {
                solrIndexFuture.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .add(response.getResults());
            } catch (InterruptedException | IngestException e) {
                LOGGER.error("Failed adding cached search results.", e);
            } catch (ExecutionException | TimeoutException e) {
                LOGGER.error("Failed to create index.", e);
            }
        }
    }
}
