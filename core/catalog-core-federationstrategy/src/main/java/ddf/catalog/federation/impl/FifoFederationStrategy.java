/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.federation.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostFederatedQueryPlugin;
import ddf.catalog.plugin.PreFederatedQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.Source;

/**
 * The Class {@code FifoFederationStrategy} represents a First In First Out (FIFO) federation
 * strategy that returns results in the order they are received. This means that the first results
 * received by this strategy are the first results sent back to the client.
 * 
 */
public class FifoFederationStrategy implements FederationStrategy {

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(FifoFederationStrategy.class));

    private static final int DEFAULT_MAX_START_INDEX = 50000;

    private int maxStartIndex;

    private ExecutorService queryExecutorService = null;

    private List<PreFederatedQueryPlugin> preQuery;

    private List<PostFederatedQueryPlugin> postQuery;

    /**
     * Instantiates a {@code FifoFederationStrategy} with the provided {@link ExecutorService}.
     * 
     * @param queryExecutorService
     *            the {@link ExecutorService} for queries
     */
    public FifoFederationStrategy(ExecutorService queryExecutorService,
            List<PreFederatedQueryPlugin> preQuery, List<PostFederatedQueryPlugin> postQuery) {
        this.queryExecutorService = queryExecutorService;
        this.preQuery = preQuery;
        this.postQuery = postQuery;
        this.maxStartIndex = DEFAULT_MAX_START_INDEX;
    }

    @Override
    public QueryResponse federate(List<Source> sources, final QueryRequest queryRequest) {

        Query originalQuery = queryRequest.getQuery();

        int offset = originalQuery.getStartIndex();
        // limit offset to max value
        if (offset > this.maxStartIndex) {
            offset = this.maxStartIndex;
        }

        final int pageSize = originalQuery.getPageSize();

        QueryResponseImpl queryResponse = new QueryResponseImpl(queryRequest, null);

        Map<Source, Future<SourceResponse>> futures = new HashMap<Source, Future<SourceResponse>>();

        Query modifiedQuery = getModifiedQuery(originalQuery, sources.size(), offset, pageSize);
        QueryRequest modifiedQueryRequest = new QueryRequestImpl(modifiedQuery,
                queryRequest.isEnterprise(), queryRequest.getSourceIds(),
                queryRequest.getProperties());

        executeSourceQueries(sources, futures, modifiedQueryRequest);

        int resultsToSkip = 0;
        if (offset > 1 && sources.size() > 1) {
            resultsToSkip = offset - 1;
        }

        queryExecutorService.submit(new FifoQueryMonitor(queryExecutorService, futures,
                queryResponse, modifiedQueryRequest.getQuery(), resultsToSkip));

        return executePostFederationPlugins(queryResponse);
    }

    protected QueryResponse executePostFederationPlugins(QueryResponse queryResponse) {
        try {
            for (PostFederatedQueryPlugin service : postQuery) {
                try {
                    queryResponse = service.process(queryResponse);
                } catch (PluginExecutionException e) {
                    LOGGER.warn("Error executing PostFederatedQueryPlugin: " + e.getMessage(), e);
                }
            }
        } catch (StopProcessingException e) {
            LOGGER.warn("Plugin stopped processing: ", e);
        }
        return queryResponse;
    }

    protected void executeSourceQueries(List<Source> sources,
            Map<Source, Future<SourceResponse>> futures, QueryRequest modifiedQueryRequest) {
        // Do NOT call source.isAvailable() when checking sources
        for (final Source source : sources) {
            if (source != null) {
                if (!futures.containsKey(source)) {
                    try {
                        for (PreFederatedQueryPlugin service : preQuery) {
                            try {
                                modifiedQueryRequest = service
                                        .process(source, modifiedQueryRequest);
                            } catch (PluginExecutionException e) {
                                LOGGER.warn(
                                        "Error executing PreFederatedQueryPlugin: "
                                                + e.getMessage(), e);
                            }
                        }
                    } catch (StopProcessingException e) {
                        LOGGER.warn("Plugin stopped processing: ", e);
                    }
                    futures.put(source, queryExecutorService.submit(new CallableSourceResponse(
                            source, modifiedQueryRequest.getQuery(), modifiedQueryRequest
                                    .getProperties())));
                } else {
                    LOGGER.warn("Duplicate source found with name " + source.getId()
                            + ". Ignoring second one.");
                }
            }
        }
    }

    protected Query getModifiedQuery(Query originalQuery, int numberOfSources, int offset,
            int pageSize) {

        Query query = null;

        // If offset is not specified, our offset is 1
        if (offset > 1 && numberOfSources > 1) {

            final int modifiedOffset = 1;
            int modifiedPageSize = offset + pageSize - 1;

            /**
             * Federated sources always query from offset of 1. When all query results are received
             * from all federated sources and merged together - then the offset is applied.
             */
            query = new QueryImpl(originalQuery, modifiedOffset, modifiedPageSize,
                    originalQuery.getSortBy(), originalQuery.requestsTotalResultsCount(),
                    originalQuery.getTimeoutMillis());
        } else {
            query = originalQuery;
        }

        return query;
    }

    private class CallableSourceResponse implements Callable<SourceResponse> {

        private Query query = null;

        private Source source = null;

        private Map<String, Serializable> properties = null;

        public CallableSourceResponse(Source source, Query query,
                Map<String, Serializable> properties) {
            this.source = source;
            this.query = query;
            this.properties = properties;
        }

        @Override
        public SourceResponse call() throws Exception {
            long startTime = System.currentTimeMillis();
            SourceResponse sourceResponse = source.query(new QueryRequestImpl(query, properties));
            long elapsedTime = System.currentTimeMillis() - startTime;
            LOGGER.debug("The source {} responded to the query in {} milliseconds", source.getId(),
                    elapsedTime);
            sourceResponse.getProperties().put(QueryResponse.ELAPSED_TIME, elapsedTime);
            return sourceResponse;
        };
    }

    /**
     * Gets the time remaining before the timeout on a query
     * 
     * @param deadline
     *            - the deadline for the timeout to occur
     * @return the time remaining prior to the timeout
     */
    private class FifoQueryMonitor implements Runnable {

        private QueryResponseImpl returnResults;

        private Map<Source, Future<SourceResponse>> futures;

        private Query query;

        private ExecutorService pool;

        private AtomicInteger sites = new AtomicInteger();

        private AtomicInteger resultsToSkip = null;

        public FifoQueryMonitor(ExecutorService pool, Map<Source, Future<SourceResponse>> futuress,
                QueryResponseImpl returnResults, Query query, int resultsToSkip) {
            this.pool = pool;
            this.returnResults = returnResults;
            this.query = query;
            this.futures = futuress;
            this.resultsToSkip = new AtomicInteger(resultsToSkip);
        }

        private int updateSites(int addition) {
            return sites.addAndGet(addition);
        }

        @Override
        public void run() {
            int pageSize = query.getPageSize() > 0 ? query.getPageSize() : Integer.MAX_VALUE;
            for (final Map.Entry<Source, Future<SourceResponse>> entry : futures.entrySet()) {
                Source site = entry.getKey();
                // Add a List of siteIds so endpoints know what sites got queried
                Serializable siteListObject = returnResults.getProperties().get(
                        QueryResponse.SITE_LIST);
                if (siteListObject != null && siteListObject instanceof List<?>) {
                    ((List) siteListObject).add(site.getId());
                } else {
                    siteListObject = new ArrayList<String>();
                    ((List) siteListObject).add(site.getId());
                    returnResults.getProperties().put(QueryResponse.SITE_LIST,
                            (Serializable) siteListObject);
                }
                updateSites(1);
                pool.submit(new SourceQueryThread(site, entry.getValue(), returnResults, pageSize));
            }
        }

        private long getTimeRemaining(long deadline) {
            long timeleft;
            if (System.currentTimeMillis() > deadline) {
                timeleft = 0;
            } else {
                timeleft = deadline - System.currentTimeMillis();
            }
            return timeleft;
        }

        private class SourceQueryThread implements Runnable {

            private long maxResults = 0;

            Future<SourceResponse> curFuture = null;

            QueryResponseImpl returnResults = null;

            private Source site = null;

            public SourceQueryThread(Source site, Future<SourceResponse> curFuture,
                    QueryResponseImpl returnResults, long maxResults) {
                this.curFuture = curFuture;
                this.returnResults = returnResults;
                this.site = site;
                this.maxResults = maxResults;
            }

            @Override
            public void run() {
                SourceResponse sourceResponse = null;
                Set<ProcessingDetails> processingDetails = returnResults.getProcessingDetails();
                try {
                    sourceResponse = query.getTimeoutMillis() < 1 ? curFuture.get() : curFuture
                            .get(getTimeRemaining(System.currentTimeMillis()
                                    + query.getTimeoutMillis()), TimeUnit.MILLISECONDS);
                    sourceResponse = curFuture.get();
                } catch (Exception e) {
                    LOGGER.warn("Federated query returned exception " + e.getMessage());
                    processingDetails.add(new ProcessingDetailsImpl(site.getId(), e));
                }
                long sourceHits = 0;
                if (sourceResponse != null) {

                    sourceHits = sourceResponse.getHits();

                    // Check if we have hit the maximum number
                    // of results
                    List<Result> results = sourceResponse.getResults();
                    int resultsReturned = results.size();

                    Map<String, Serializable> newSourceProperties = new HashMap<String, Serializable>();
                    newSourceProperties.put(QueryResponse.TOTAL_HITS, sourceHits);
                    newSourceProperties.put(QueryResponse.TOTAL_RESULTS_RETURNED, resultsReturned);

                    synchronized (returnResults) {
                        long sentTotal = returnResults.getHits();
                        returnResults.setHits(sourceHits + sentTotal);
                        for (Result result : results) {
                            if (sentTotal >= maxResults) {
                                LOGGER.debug("Received max number of results so ending polling");
                                break;
                            } else if (resultsToSkip.get() == 0) {
                                returnResults.addResult(result, false);
                                sentTotal++;
                            } else {
                                resultsToSkip.decrementAndGet();
                                sentTotal++;
                            }
                        }

                        if (sentTotal >= maxResults) {
                            returnResults.closeResultQueue();
                            LOGGER.debug("sending terminator for fifo federation strategy.");
                        }
                    }

                    returnResults.getProperties().put(site.getId(),
                            (Serializable) newSourceProperties);
                    Map<String, Serializable> originalSourceProperties = sourceResponse
                            .getProperties();
                    if (originalSourceProperties != null) {
                        Serializable object = originalSourceProperties
                                .get(QueryResponse.ELAPSED_TIME);
                        if (object != null && object instanceof Long) {
                            newSourceProperties.put(QueryResponse.ELAPSED_TIME, (Long) object);
                            originalSourceProperties.remove(QueryResponse.ELAPSED_TIME);
                            LOGGER.debug(
                                    "Setting the elapsedTime responseProperty to {} for source {}",
                                    object, site.getId());
                        }

                        returnResults.getProperties().putAll(originalSourceProperties);
                    }
                }

                if (updateSites(-1) == 0) {
                    LOGGER.debug("sending terminator for fifo federation strategy.");
                    returnResults.closeResultQueue();
                }

            }
        }
    }

}
