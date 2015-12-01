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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.codice.ddf.platform.util.Exceptions;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.Source;
import ddf.catalog.util.impl.DistanceResultComparator;
import ddf.catalog.util.impl.RelevanceResultComparator;
import ddf.catalog.util.impl.TemporalResultComparator;

class SortedQueryMonitor implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(SortedQueryMonitor.class);

    private CachingFederationStrategy cachingFederationStrategy;

    private final QueryRequest request;

    private final CompletionService<SourceResponse> completionService;

    private QueryResponseImpl returnResults;

    private Map<Future<SourceResponse>, Source> futures;

    private Query query;

    private long deadline;

    public SortedQueryMonitor(CachingFederationStrategy cachingFederationStrategy,
            CompletionService<SourceResponse> completionService,
            Map<Future<SourceResponse>, Source> futures, QueryResponseImpl returnResults,
            QueryRequest request) {
        this.cachingFederationStrategy = cachingFederationStrategy;

        this.completionService = completionService;
        this.returnResults = returnResults;
        this.request = request;
        this.query = request.getQuery();
        this.futures = futures;

        deadline = System.currentTimeMillis() + query.getTimeoutMillis();
    }

    @Override
    public void run() {
        SortBy sortBy = query.getSortBy();
        // Prepare the Comparators that we will use
        Comparator<Result> coreComparator = CachingFederationStrategy.DEFAULT_COMPARATOR;

        if (sortBy != null && sortBy.getPropertyName() != null) {
            PropertyName sortingProp = sortBy.getPropertyName();
            String sortType = sortingProp.getPropertyName();
            SortOrder sortOrder = (sortBy.getSortOrder() == null) ?
                    SortOrder.DESCENDING :
                    sortBy.getSortOrder();
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

        List<Result> resultList = new ArrayList<>();
        long totalHits = 0;
        Set<ProcessingDetails> processingDetails = returnResults.getProcessingDetails();

        Map<String, Serializable> returnProperties = returnResults.getProperties();
        HashMap<String, Long> hitsPerSource = new HashMap<>();

        for (int i = futures.size(); i > 0; i--) {
            String sourceId = "Unknown Source";
            Source source = null;
            try {
                Future<SourceResponse> future;
                if (query.getTimeoutMillis() < 1) {
                    future = completionService.take();
                } else {
                    future = completionService
                            .poll(getTimeRemaining(deadline), TimeUnit.MILLISECONDS);
                    if (future == null) {
                        timeoutRemainingSources(processingDetails);
                        break;
                    }
                }

                source = futures.remove(future);
                if (source != null) {
                    sourceId = source.getId();
                }

                SourceResponse sourceResponse = future.get();

                if (sourceResponse == null) {
                    logger.info("Source {} returned null response", sourceId);
                    processingDetails
                            .add(new ProcessingDetailsImpl(sourceId, new NullPointerException()));
                } else {
                    resultList.addAll(sourceResponse.getResults());
                    long hits = sourceResponse.getHits();
                    totalHits += hits;
                    hitsPerSource.merge(sourceId, hits, (l1, l2) -> l1 + l2);

                    Map<String, Serializable> properties = sourceResponse.getProperties();
                    returnProperties.putAll(properties);
                }
            } catch (InterruptedException e) {
                if (source != null) {
                    // First, add interrupted processing detail for this source
                    logger.info("Search interrupted for {}", source.getId());
                    processingDetails.add(new ProcessingDetailsImpl(source.getId(), e));
                }

                // Then add the interrupted exception for the remaining sources
                interruptRemainingSources(processingDetails, e);
                break;
            } catch (ExecutionException e) {
                logger.warn("Couldn't get results from completed federated query. {}, {}", sourceId,
                        Exceptions.getFullMessage(e), e);

                processingDetails.add(new ProcessingDetailsImpl(sourceId,
                        new Exception(Exceptions.getFullMessage(e))));
            }
        }
        returnProperties.put("hitsPerSource", hitsPerSource);
        logger.debug("All sources finished returning results: {}", resultList.size());

        returnResults.setHits(totalHits);
        if (CachingFederationStrategy.INDEX_QUERY_MODE
                .equals(request.getPropertyValue(CachingFederationStrategy.QUERY_MODE))) {
            QueryResponse result = cachingFederationStrategy.queryCache(request);
            returnResults.addResults(result.getResults(), true);
        } else {
            returnResults.addResults(sortedResults(resultList, coreComparator), true);
        }
    }

    List<Result> sortedResults(List<Result> results, Comparator<? super Result> comparator) {
        Collections.sort(results, comparator);

        int maxResults = Integer.MAX_VALUE;
        if (query.getPageSize() > 0) {
            maxResults = query.getPageSize();
        }

        return results.size() > maxResults ? results.subList(0, maxResults) : results;
    }

    private void timeoutRemainingSources(Set<ProcessingDetails> processingDetails) {
        for (Source expiredSource : futures.values()) {
            if (expiredSource != null) {
                logger.info("Search timed out for {}", expiredSource.getId());
                processingDetails.add(new ProcessingDetailsImpl(expiredSource.getId(),
                        new TimeoutException()));
            }
        }
    }

    private void interruptRemainingSources(Set<ProcessingDetails> processingDetails,
            InterruptedException interruptedException) {
        for (Source interruptedSource : futures.values()) {
            if (interruptedSource != null) {
                logger.info("Search interrupted for {}", interruptedSource.getId());
                processingDetails.add(new ProcessingDetailsImpl(interruptedSource.getId(),
                        interruptedException));
            }
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
