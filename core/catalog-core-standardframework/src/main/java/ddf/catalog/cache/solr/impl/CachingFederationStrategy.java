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
package ddf.catalog.cache.solr.impl;

import ddf.catalog.data.Metacard;
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
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.DistanceResultComparator;
import ddf.catalog.util.impl.RelevanceResultComparator;
import ddf.catalog.util.impl.TemporalResultComparator;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Phaser;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class represents a {@link ddf.catalog.federation.FederationStrategy} based on sorting {@link ddf.catalog.data.Metacard}s. The
 * sorting is based on the {@link ddf.catalog.operation.Query}'s {@link org.opengis.filter.sort.SortBy} propertyName. The possible sorting values
 * are {@link ddf.catalog.data.Metacard.EFFECTIVE}, {@link ddf.catalog.data.Result.TEMPORAL}, {@link ddf.catalog.data.Result.DISTANCE}, or
 * {@link ddf.catalog.data.Result.RELEVANCE} . The supported ordering includes {@link org.opengis.filter.sort.SortOrder.DESCENDING} and
 * {@link org.opengis.filter.sort.SortOrder.ASCENDING}. For this class to function properly a sort value and sort order must
 * be provided.
 *
 * @see ddf.catalog.data.Metacard
 * @see ddf.catalog.operation.Query
 * @see org.opengis.filter.sort.SortBy
 */
public class CachingFederationStrategy implements FederationStrategy {

    /**
     * The default comparator for sorting by {@link ddf.catalog.data.Result.RELEVANCE}, {@link org.opengis.filter.sort.SortOrder.DESCENDING}
     */
    protected static final Comparator<Result> DEFAULT_COMPARATOR = new RelevanceResultComparator(
            SortOrder.DESCENDING);

    protected static final String QUERY_MODE = "mode";

    protected static final String CACHE_QUERY_MODE = "cache";

    protected static final String INDEX_QUERY_MODE = "index";

    private static XLogger logger = new XLogger(
            LoggerFactory.getLogger(CachingFederationStrategy.class));

    private final SolrCache cache;

    private ExecutorService cacheExecutorService = Executors.newCachedThreadPool();

    private ExecutorService queryExecutorService;

    private static final int DEFAULT_MAX_START_INDEX = 50000;

    private int maxStartIndex;

    // register one party for scheduled phase advancer
    private CacheCommitPhaser phaser = new CacheCommitPhaser(1);

    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * The {@link List} of pre-federated query plugins to execute on the query request before the
     * query is executed on the {@link Source}.
     */
    protected List<PreFederatedQueryPlugin> preQuery;

    /**
     * The {@link List} of post-federated query plugins to execute on the query request after the
     * query is executed on the {@link Source}.
     */
    protected List<PostFederatedQueryPlugin> postQuery;

    /**
     * Instantiates an {@code AbstractFederationStrategy} with the provided {@link ExecutorService}.
     *
     * @param queryExecutorService
     *            the {@link ExecutorService} for queries
     */
    public CachingFederationStrategy(ExecutorService queryExecutorService,
            List<PreFederatedQueryPlugin> preQuery, List<PostFederatedQueryPlugin> postQuery,
            SolrCache cache) {
        this.queryExecutorService = queryExecutorService;
        this.preQuery = preQuery;
        this.postQuery = postQuery;
        this.maxStartIndex = DEFAULT_MAX_START_INDEX;
        this.cache = cache;
        // phase advancer blocks waiting for next phase advance, delay 1 second between advances
        scheduler.scheduleWithFixedDelay(new PhaseAdvancer(phaser), 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public QueryResponse federate(List<Source> sources, QueryRequest queryRequest) {
        Set<String> sourceIds = new HashSet<String>();
        for (Source source : sources) {
            sourceIds.add(source.getId());
        }
        QueryRequest modifiedQueryRequest = new QueryRequestImpl(queryRequest.getQuery(),
                queryRequest.isEnterprise(), sourceIds,
                queryRequest.getProperties());

        if (queryRequest.getProperties().containsKey(QUERY_MODE) && CACHE_QUERY_MODE.equals(
                queryRequest.getProperties().get(QUERY_MODE))) {
            return queryCache(modifiedQueryRequest);
        } else {
            return sourceFederate(sources, modifiedQueryRequest);
        }
    }

    private QueryResponse queryCache(QueryRequest queryRequest) {
        final QueryResponseImpl queryResponse = new QueryResponseImpl(queryRequest);
        try {
            SourceResponse result = cache.query(queryRequest);
            queryResponse.setHits(result.getHits());
            queryResponse.setProperties(result.getProperties());
            queryResponse.addResults(result.getResults(), true);
        } catch (UnsupportedQueryException e) {
            queryResponse.getProcessingDetails().add(new ProcessingDetailsImpl("cache",
                    e));
        }
        return queryResponse;
    }

    private QueryResponse sourceFederate(List<Source> sources, final QueryRequest queryRequest) {
        if (logger.isDebugEnabled()) {
            for (Source source : sources) {
                if (source != null) {
                    logger.debug("source to query: {}", source.getId());
                }
            }
        }

        Query originalQuery = queryRequest.getQuery();

        int offset = originalQuery.getStartIndex();
        final int pageSize = originalQuery.getPageSize();

        // limit offset to max value
        if (offset > this.maxStartIndex) {
            offset = this.maxStartIndex;
        }

        final QueryResponseImpl queryResponseQueue = new QueryResponseImpl(queryRequest, null);

        Map<Future<SourceResponse>, Source> futures = new HashMap<Future<SourceResponse>, Source>();

        Query modifiedQuery = getModifiedQuery(originalQuery, sources.size(), offset, pageSize);
        QueryRequest modifiedQueryRequest = new QueryRequestImpl(modifiedQuery,
                queryRequest.isEnterprise(), queryRequest.getSourceIds(),
                queryRequest.getProperties());

        CompletionService<SourceResponse> queryCompletion
                = new ExecutorCompletionService<SourceResponse>(queryExecutorService);

        // Do NOT call source.isAvailable() when checking sources
        for (final Source source : sources) {
            if (source != null) {
                if (!futures.containsKey(source)) {
                    logger.debug("running query on source: {}", source.getId());

                    try {
                        for (PreFederatedQueryPlugin service : preQuery) {
                            try {
                                modifiedQueryRequest = service
                                        .process(source, modifiedQueryRequest);
                            } catch (PluginExecutionException e) {
                                logger.warn(
                                        "Error executing PreFederatedQueryPlugin", e);
                            }
                        }
                    } catch (StopProcessingException e) {
                        logger.warn("Plugin stopped processing", e);
                    }

                    futures.put(queryCompletion.submit(new CallableSourceResponse(
                            source, modifiedQueryRequest)), source);
                } else {
                    logger.warn("Duplicate source found with name {}. Ignoring second one.",
                            source.getId());
                }
            }
        }

        QueryResponseImpl offsetResults = null;
        // If there are offsets and more than one source, we have to get all the
        // results back and then
        // transfer them into a different Queue. That is what the
        // OffsetResultHandler does.
        if (offset > 1 && sources.size() > 1) {
            offsetResults = new QueryResponseImpl(queryRequest, null);
            queryExecutorService.submit(new OffsetResultHandler(queryResponseQueue, offsetResults,
                    pageSize, offset));
        }

        queryExecutorService.submit(createMonitor(queryCompletion, futures,
                queryResponseQueue, modifiedQueryRequest));

        QueryResponse queryResponse = null;
        if (offset > 1 && sources.size() > 1) {
            queryResponse = offsetResults;
            logger.debug("returning offsetResults");
        } else {
            queryResponse = queryResponseQueue;
            logger.debug("returning returnResults: {}", queryResponse);
        }

        try {
            for (PostFederatedQueryPlugin service : postQuery) {
                try {
                    queryResponse = service.process(queryResponse);
                } catch (PluginExecutionException e) {
                    logger.warn("Error executing PostFederatedQueryPlugin", e);
                }
            }
        } catch (StopProcessingException e) {
            logger.warn("Plugin stopped processing", e);
        }

        logger.debug("returning Query Results: {}", queryResponse);
        return queryResponse;
    }

    private Query getModifiedQuery(Query originalQuery, int numberOfSources, int offset,
            int pageSize) {

        Query query = null;

        // If offset is not specified, our offset is 1
        if (offset > 1 && numberOfSources > 1) {

            final int modifiedOffset = 1;
            int modifiedPageSize = computeModifiedPageSize(offset, pageSize);

            logger.debug("Creating new query for federated sources to query each source from {} " +
                    "to {}.", modifiedOffset, modifiedPageSize);
            logger.debug("original offset: {}", offset);
            logger.debug("original page size: {}", pageSize);
            logger.debug("modified offset: {}", modifiedOffset);
            logger.debug("modified page size: {}", modifiedPageSize);

            /**
             * Federated sources always query from offset of 1. When all query results are received
             * from all federated sources and merged together - then the offset is applied.
             *
             */
            query = new QueryImpl(originalQuery, modifiedOffset, modifiedPageSize,
                    originalQuery.getSortBy(), originalQuery.requestsTotalResultsCount(),
                    originalQuery.getTimeoutMillis());
        } else {
            query = originalQuery;
        }

        return query;
    }

    /**
     * Base 1 offset, hence page size is one less.
     */
    private int computeModifiedPageSize(int offset, int pageSize) {
        return offset + pageSize - 1;
    }

    private class CallableSourceResponse implements Callable<SourceResponse> {

        private final QueryRequest request;

        private final Source source;

        public CallableSourceResponse(Source source, QueryRequest request) {
            this.source = source;
            this.request = request;
        }

        @Override
        public SourceResponse call() throws Exception {
            final SourceResponse sourceResponse = source.query(new QueryRequestImpl(request.getQuery(),
                    request.getProperties()));

            if (INDEX_QUERY_MODE.equals(request.getPropertyValue(QUERY_MODE))) {
                // block next phase
                phaser.register();
                // cache results
                cache.create(sourceResponse.getResults());
                // unblock phase and wait for all other parties to unblock phase
                phaser.awaitAdvance(phaser.arriveAndDeregister());
            } else {
                cacheExecutorService.submit(new Runnable() {
                    @Override public void run() {
                        cache.create(sourceResponse.getResults());
                    }
                });
            }

            return sourceResponse;
        };
    }

    private static class OffsetResultHandler implements Runnable {

        private QueryResponseImpl originalResults = null;

        private QueryResponseImpl offsetResultQueue = null;

        private int pageSize = 0;

        private int offset = 1;

        private OffsetResultHandler(QueryResponseImpl originalResults,
                QueryResponseImpl offsetResultQueue, int pageSize, int offset) {
            this.originalResults = originalResults;
            this.offsetResultQueue = offsetResultQueue;
            this.pageSize = pageSize;
            this.offset = offset;
        }

        @Override
        public void run() {
            int queryResultIndex = 1;
            int resultsSent = 0;
            Result result;

            while (resultsSent < pageSize && originalResults.hasMoreResults()
                    && (result = originalResults.take()) != null) {
                if (queryResultIndex >= offset) {
                    offsetResultQueue.addResult(result, false);
                    resultsSent++;
                }
                queryResultIndex++;
            }

            logger.debug("Closing Queue and setting the total count");
            offsetResultQueue.setHits(originalResults.getHits());
            offsetResultQueue.closeResultQueue();
        }
    }

    /**
     * To be set via Spring/Blueprint
     *
     * @param maxStartIndex
     *            the new default max start index value
     */
    public void setMaxStartIndex(int maxStartIndex) {
        this.maxStartIndex = DEFAULT_MAX_START_INDEX;

        if (maxStartIndex > 0) {
            this.maxStartIndex = maxStartIndex;
        } else {
            logger.debug("Invalid max start index input. Reset to default value: {}",
                    this.maxStartIndex);
        }
    }

    protected Runnable createMonitor(final CompletionService<SourceResponse> completionService,
            final Map<Future<SourceResponse>, Source> futures,
            final QueryResponseImpl returnResults, final QueryRequest request) {

        return new SortedQueryMonitor(completionService, futures, returnResults, request);
    }

    private class SortedQueryMonitor implements Runnable {

        private final QueryRequest request;

        private final CompletionService<SourceResponse> completionService;

        private QueryResponseImpl returnResults;

        private Map<Future<SourceResponse>, Source> futures;

        private Query query;

        public SortedQueryMonitor(CompletionService<SourceResponse> completionService,
                Map<Future<SourceResponse>, Source> futures,
                QueryResponseImpl returnResults,
                QueryRequest request) {

            this.completionService = completionService;
            this.returnResults = returnResults;
            this.request = request;
            this.query = request.getQuery();
            this.futures = futures;
        }

        @Override
        public void run() {
            SortBy sortBy = query.getSortBy();
            // Prepare the Comparators that we will use
            Comparator<Result> coreComparator = DEFAULT_COMPARATOR;

            if (sortBy != null && sortBy.getPropertyName() != null) {
                PropertyName sortingProp = sortBy.getPropertyName();
                String sortType = sortingProp.getPropertyName();
                SortOrder sortOrder = (sortBy.getSortOrder() == null) ? SortOrder.DESCENDING
                        : sortBy.getSortOrder();
                logger.debug("Sorting type: {}", sortType);
                logger.debug("Sorting order: {}", sortBy.getSortOrder());

                // Temporal searches are currently sorted by the effective time
                if (Metacard.EFFECTIVE.equals(sortType) || Result.TEMPORAL.equals(sortType)) {
                    coreComparator = new TemporalResultComparator(sortOrder);
                } else if (Result.DISTANCE.equals(sortType)) {
                    coreComparator = new DistanceResultComparator(sortOrder);
                } else if (Result.RELEVANCE.equals(sortType)) {
                    coreComparator = new RelevanceResultComparator(sortOrder);
                }
            }

            List<Result> resultList = new ArrayList<Result>();
            long totalHits = 0;
            Set<ProcessingDetails> processingDetails = returnResults.getProcessingDetails();

            long deadline = System.currentTimeMillis() + query.getTimeoutMillis();

            Map<String, Serializable> returnProperties = returnResults.getProperties();
            for (int i = 0; i < futures.size(); i++) {

                Source source = null;
                SourceResponse sourceResponse = null;
                try {
                    Future<SourceResponse> future = null;
                    if (query.getTimeoutMillis() < 1) {
                        future = completionService.take();
                    } else {
                        future = completionService.poll(getTimeRemaining(deadline),
                                TimeUnit.MILLISECONDS);
                    }

                    source = futures.get(future);
                    sourceResponse = future.get();

                    if (sourceResponse == null) {
                        logger.info("Search timed out for {}", source.getId());
                        processingDetails.add(new ProcessingDetailsImpl(source.getId(),
                                new TimeoutException()));
                    } else {
                        resultList.addAll(sourceResponse.getResults());
                        totalHits += sourceResponse.getHits();

                        Map<String, Serializable> properties = sourceResponse.getProperties();
                        returnProperties.putAll(properties);
                    }

                } catch (InterruptedException e) {
                    logger.warn(
                            "Couldn't get results from completed federated query on for {}",
                            source.getId(), e);
                    processingDetails.add(new ProcessingDetailsImpl(source.getId(), e));
                } catch (ExecutionException e) {
                    String sourceId;
                    if (source != null) {
                        sourceId = source.getId();
                    } else {
                        sourceId = "Unknown Source";
                    }
                    logger.warn("Couldn't get results from completed federated query for " + sourceId, e);
                    processingDetails.add(new ProcessingDetailsImpl(sourceId, e));
                }
            }
            logger.debug("All sources finished returning results: {}", resultList.size());

            if (INDEX_QUERY_MODE.equals(request.getPropertyValue(QUERY_MODE))) {
                QueryResponse result = queryCache(request);
                returnResults.setHits(totalHits);
                returnResults.addResults(result.getResults(), true);
            } else {
                Collections.sort(resultList, coreComparator);

                returnResults.setHits(totalHits);
                int maxResults = Integer.MAX_VALUE;
                if (query.getPageSize() > 0) {
                    maxResults = query.getPageSize();
                }

                returnResults
                        .addResults(resultList.size() > maxResults ? resultList.subList(0, maxResults)
                                : resultList, true);
            }
        }

        private long getTimeRemaining(long deadline) {
            long timeLeft;
            if (System.currentTimeMillis() > deadline) {
                timeLeft = 0;
            } else {
                timeLeft = deadline - System.currentTimeMillis();
            }
            return timeLeft;
        }

    }

    // Phaser that forces all added documents to commit to the cache on phase advance
    private class CacheCommitPhaser extends Phaser {

        public CacheCommitPhaser(int parties) {
            super(parties);
        }

        @Override
        protected boolean onAdvance(int phase, int registeredParties) {
            // registeredParties should be 1 since all parties other than the first
            // will arriveAndDeregister when advancing
            cache.forceCommit();

            return super.onAdvance(phase, registeredParties);
        }

    }

    // Runnable that makes one party arrive to a phaser on run
    private static class PhaseAdvancer implements Runnable {

        private final Phaser phaser;

        public PhaseAdvancer(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arriveAndAwaitAdvance();
        }
    }

    public void shutdown() {
        phaser.forceTermination();
        scheduler.shutdown();
    }

}
