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

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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

import com.google.common.collect.Ordering;

import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.security.Subject;

public class SourceQueryRunnable extends QueryRunnable {

    private final String sourceId;

    private final Map<String, Serializable> queryProperties;

    private final Future cacheFuture;

    private final Future<FilteringSolrIndex> solrIndexFuture;

    private final Comparator<Result> sortComparator;

    private final int maxResults;

    private final boolean shouldNormalizeRelevance;

    private final boolean shouldNormalizeDistance;

    public SourceQueryRunnable(SearchController searchController, String sourceId,
            SearchRequest request, Subject subject, Map<String, Result> results, Search search,
            ServerSession session, Future cacheFuture, Future<FilteringSolrIndex> solrIndexFuture) {
        super(searchController, request, subject, search, session, results);
        this.sourceId = sourceId;
        this.cacheFuture = cacheFuture;
        this.solrIndexFuture = solrIndexFuture;

        sortComparator = getResultComparator(request.getQuery());
        maxResults = getMaxResults(request);
        shouldNormalizeRelevance = searchController.shouldNormalizeRelevance(request);
        shouldNormalizeDistance = searchController.shouldNormalizeDistance(request);

        if (searchController.getCacheDisabled()) {
            queryProperties = new HashMap<>();
        } else {
            queryProperties = new HashMap<>(UPDATE_PROPERTIES);
        }
    }

    @Override
    public void run() {
        LOGGER.debug("Executing query on: {}", sourceId);
        QueryResponse sourceResponse = queryCatalog(sourceId, request, subject, queryProperties);
        waitForCache();
        addResults(sourceResponse.getResults());
        normalize(request.getQuery(), sourceResponse.getResults());
        sort(sourceResponse.getResults());
        sendResults(sourceResponse);
        cleanup();
    }

    private void sendResults(QueryResponse sourceResponse) {
        search.update(sourceId, sourceResponse);
        try {
            searchController.publishResults(request.getId(),
                    search.transform(request.getId()),
                    session);
        } catch (CatalogTransformerException e) {
            LOGGER.error("Failed to transform federated search results.", e);
        }
    }

    private void waitForCache() {
        try {
            cacheFuture.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error("Cache query failed", e);
        }
    }

    private void cleanup() {
        if (shouldNormalizeRelevance && search.isFinished()) {
            try {
                solrIndexFuture.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .shutdown();
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                LOGGER.debug("Waiting for solr index failed", e);
            }
        }
    }

    private void sort(List<Result> responseResults) {
        List<Result> sortedResults = Ordering.from(sortComparator)
                .immutableSortedCopy(results.values());

        responseResults.clear();
        responseResults.addAll(sortedResults.size() > maxResults ?
                sortedResults.subList(0, maxResults) :
                sortedResults);
    }

    private void normalize(Query query, List<Result> responseResults) {
        if (shouldNormalizeRelevance) {
            FilteringSolrIndex index = null;
            try {
                index = solrIndexFuture.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                index.add(responseResults);

                List<Result> indexResults = index.query(new QueryRequestImpl(query))
                        .getResults();

                normalizeRelevance(indexResults, results);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOGGER.warn("Failed to get index for relevance normalization", e);
            } catch (UnsupportedQueryException e) {
                LOGGER.warn("Failed to parse query for relevance normalization", e);
            } catch (IngestException e) {
                LOGGER.warn("Failed to ingest results for relevance normalization", e);
            }
        } else if (shouldNormalizeDistance) {
            normalizeDistances(query, results);
        }
    }
}
