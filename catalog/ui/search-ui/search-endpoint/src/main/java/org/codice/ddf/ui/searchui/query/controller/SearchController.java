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
package org.codice.ddf.ui.searchui.query.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.codice.ddf.ui.searchui.query.controller.search.CacheQueryRunnable;
import org.codice.ddf.ui.searchui.query.controller.search.FilteringSolrIndexCallable;
import org.codice.ddf.ui.searchui.query.controller.search.SourceQueryRunnable;
import org.codice.ddf.ui.searchui.query.model.Search;
import org.codice.ddf.ui.searchui.query.model.SearchRequest;
import org.codice.ddf.ui.searchui.query.solr.FilteringSolrIndex;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.ServerMessageImpl;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

import ddf.action.ActionRegistry;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.Query;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.security.Subject;

/**
 * The {@link SearchController} class handles all of the query threads for asynchronous queries.
 */
public class SearchController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchController.class);

    private final ExecutorService executorService;

    private final FilterAdapter filterAdapter;

    private Boolean cacheDisabled = false;

    private Boolean normalizationDisabled = false;

    private CatalogFramework framework;

    private ActionRegistry actionRegistry;

    private BayeuxServer bayeuxServer;

    /**
     * Create a new SearchController
     *
     * @param framework     {@link CatalogFramework} that will be handling the actual queries
     * @param filterAdapter
     */
    public SearchController(CatalogFramework framework, ActionRegistry actionRegistry,
            FilterAdapter filterAdapter, ExecutorService executorService) {
        this.framework = framework;
        this.actionRegistry = actionRegistry;
        this.filterAdapter = filterAdapter;
        this.executorService = executorService;
    }

    /**
     * Destroys this controller. This controller may not be used again after this method is called.
     */
    public void destroy() {
        executorService.shutdown();
    }

    /**
     * Immediately publish results out to clients
     *
     * @param id            request id
     * @param jsonData
     * @param serverSession
     */
    public synchronized void publishResults(String id, Map<String, Object> jsonData,
            ServerSession serverSession) {
        String channel = "/" + id;

        LOGGER.debug("Creating channel if it doesn't exist: {}", channel);

        bayeuxServer.createChannelIfAbsent(channel,
                (ConfigurableServerChannel.Initializer) serverChannel -> serverChannel.setPersistent(
                        true));

        jsonData.put(Search.SUCCESSFUL, true);

        ServerMessage.Mutable reply = new ServerMessageImpl();
        reply.setChannel(channel);
        reply.setData(jsonData);

        LOGGER.debug("Sending results to subscribers on: {}", channel);
        bayeuxServer.getChannel(channel)
                .publish(serverSession, reply);
    }

    /**
     * Execute all of the queries contained within the {@link SearchRequest}.
     *
     * @param request {@link SearchRequest} containing a query for 1 or more sources
     * @param session Cometd {@link ServerSession}
     */
    public void executeQuery(final SearchRequest request, final ServerSession session,
            final Subject subject) {

        final Search search = new Search(request, actionRegistry);
        final Map<String, Result> results =
                Collections.synchronizedMap(new HashMap<String, Result>());

        final Future<FilteringSolrIndex> solrIndexFuture;
        if (shouldNormalizeRelevance(request)) {
            // Create in memory Solr instance asynchronously
            solrIndexFuture = executorService.submit(new FilteringSolrIndexCallable(request,
                    filterAdapter));
        } else {
            solrIndexFuture = Futures.immediateFuture(null);
        }

        final Future cacheFuture;
        if (!cacheDisabled) {
            // Send any previously cached results
            cacheFuture = executorService.submit(new CacheQueryRunnable(this,
                    request,
                    subject,
                    search,
                    session,
                    results,
                    solrIndexFuture));
        } else {
            cacheFuture = Futures.immediateFuture(null);
        }

        long deadline = System.currentTimeMillis() + request.getQuery()
                .getTimeoutMillis();
        Map<String, Future> futures = new HashMap<>(request.getSourceIds()
                .size());

        for (final String sourceId : request.getSourceIds()) {
            // Send the latest results from each source
            futures.put(sourceId,
                    executorService.submit(new SourceQueryRunnable(this,
                            sourceId,
                            request,
                            subject,
                            results,
                            search,
                            session,
                            cacheFuture,
                            solrIndexFuture)));
        }

        for (Map.Entry<String, Future> entry : futures.entrySet()) {
            String sourceId = entry.getKey();
            Future future = entry.getValue();
            try {
                long timeRemaining = Math.max(0, deadline - System.currentTimeMillis());
                future.get(timeRemaining, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOGGER.info("Query interrupted for source {}", sourceId, e);
                failSource(request, session, search, sourceId, e);
            } catch (TimeoutException e) {
                LOGGER.info("Query timed out for source {}", sourceId, e);
                failSource(request, session, search, sourceId, e);
            } catch (ExecutionException e) {
                LOGGER.warn("Query failed for source {}", sourceId, e.getCause());
                failSource(request, session, search, sourceId, e);
            }
        }
    }

    private void failSource(SearchRequest request, ServerSession session, Search search,
            String sourceId, Exception cause) {
        search.failSource(sourceId, cause);
        try {
            publishResults(request.getId(), search.transform(request.getId()), session);
        } catch (CatalogTransformerException e) {
            LOGGER.error("Failed to transform failed search results.", e);
        }
    }

    public boolean shouldNormalizeDistance(SearchRequest request) {
        return Result.DISTANCE.equals(getSortBy(request.getQuery())) && shouldNormalize(request);
    }

    public boolean shouldNormalizeRelevance(SearchRequest request) {
        return Result.RELEVANCE.equals(getSortBy(request.getQuery())) && shouldNormalize(request);
    }

    private boolean shouldNormalize(SearchRequest request) {
        return request.getSourceIds()
                .size() > 1 && !normalizationDisabled;
    }

    public String getSortBy(Query query) {
        String result = null;
        SortBy sortBy = query.getSortBy();

        if (sortBy != null && sortBy.getPropertyName() != null) {
            result = sortBy.getPropertyName()
                    .getPropertyName();
        }

        return result;
    }

    public CatalogFramework getFramework() {
        return framework;
    }

    public synchronized void setBayeuxServer(BayeuxServer bayeuxServer) {
        this.bayeuxServer = bayeuxServer;
    }

    public void setCacheDisabled(Boolean cacheDisabled) {
        this.cacheDisabled = cacheDisabled;
    }

    public Boolean getCacheDisabled() {
        return cacheDisabled;
    }

    public FilterAdapter getFilterAdapter() {
        return filterAdapter;
    }

    public void setNormalizationDisabled(Boolean normalizationDisabled) {
        this.normalizationDisabled = normalizationDisabled;
    }
}
