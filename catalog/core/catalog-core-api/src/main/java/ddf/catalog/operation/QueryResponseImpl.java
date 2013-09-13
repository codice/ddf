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
package ddf.catalog.operation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;

public class QueryResponseImpl extends ResponseImpl<QueryRequest> implements QueryResponse {

    private static XLogger logger = new XLogger(LoggerFactory.getLogger(QueryResponseImpl.class));

    protected static Result POISON_PILL_RESULT = new POISON_PILL_RESULT();

    protected long hits;

    protected Set<ProcessingDetails> details = new HashSet<ProcessingDetails>();

    protected boolean isQueueClosed = false;

    LinkedBlockingQueue<Result> queue = null;

    List<Result> resultList = null;

    /**
     * Instantiates a new QueryResponseImpl with a $(@link QueryRequest)
     * 
     * @param request
     *            the request
     */
    public QueryResponseImpl(QueryRequest request) {
        this(request, new HashMap<String, Serializable>());
    }

    /**
     * Instantiates a new QueryResponseImpl with a $(@link QueryRequest) and and a ${@link Map} of
     * properties
     * 
     * @param request
     *            the request
     * @param properties
     */
    public QueryResponseImpl(QueryRequest request, Map<String, Serializable> properties) {
        this(request, null, false, 0, properties);
    }

    /**
     * Instantiates a new QueryResponseImpl with a $(@link QueryRequest) and and a ${@link List} of
     * results
     * 
     * @param request
     *            the request
     * @param results
     *            the results
     */
    public QueryResponseImpl(QueryRequest request, List<Result> results, long totalHits) {
        this(request, results, true, totalHits, null);
    }

    /**
     * Instantiates a new QueryResponseImpl with a $(@link QueryRequest), a ${@link List} of
     * results, a closeResultQueue indicator, and a number of hits to return
     * 
     * @param request
     *            the request
     * @param results
     *            the results
     * @param hits
     *            the hits
     */
    public QueryResponseImpl(QueryRequest request, List<Result> results, boolean closeResultQueue,
            long hits) {
        this(request, results, closeResultQueue, hits, null);
    }

    /**
     * Instantiates a new QueryResponseImpl with a $(@link QueryRequest), a ${@link List} of
     * results, a closeResultQueue indicator, a number of hits to return, and a ${@link Map} of
     * properties
     * 
     * @param request
     *            the request
     * @param results
     *            the results
     * @param hits
     *            the hits
     * @param properties
     *            the properties
     */
    public QueryResponseImpl(QueryRequest request, List<Result> results, boolean closeResultQueue,
            long hits, Map<String, Serializable> properties) {
        super(request, properties);
        this.hits = hits;
        queue = results == null ? new LinkedBlockingQueue<Result>()
                : new LinkedBlockingQueue<Result>(results);
        resultList = new ArrayList<Result>();
        if (closeResultQueue) {
            closeResultQueue();
        }
    }

    /**
     * Construct from an underlying {@link SourceResponse}
     * 
     * @param response
     * @param sourceId
     */
    public QueryResponseImpl(SourceResponse response, String sourceId) {
        this(response == null ? null : response.getRequest(), response == null ? null : response
                .getResults(), response == null ? -1 : response.getHits());
        Set<? extends SourceProcessingDetails> sourceDetails = null;
        if (response != null) {
            sourceDetails = response.getProcessingDetails();
            this.setProperties(response.getProperties());
        } else {
            setProperties(new HashMap<String, Serializable>());
        }
        // Not every response will contain details
        if (sourceDetails != null) {
            for (SourceProcessingDetails detail : sourceDetails) {
                this.details.add(new ProcessingDetailsImpl(detail, sourceId));
            }
        }
    }

    // /**
    // * Wrap a {@link QueryResponse} and add additional {@link
    // ProcessingDetails}
    // *
    // * @param response
    // * @param exceptions
    // */
    // public QueryResponseImpl(QueryResponse response,
    // Set<ProcessingDetails> exceptions) {
    //
    //
    // }

    @Override
    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    @Override
    public Set<ProcessingDetails> getProcessingDetails() {
        return details;
    }

    public void setProcessingDetails(Set<ProcessingDetails> set) {
        this.details = set;
    }

    @Override
    public List<Result> getResults() {
        Result result = null;

        while (hasMoreResults() && (result = take()) != null) {
            resultList.add(result);
        }

        return resultList;
    }

    /**
     * Adds a ${@link Result} to this QueryResponse, and specifies whether or not to close the queue
     * 
     * @param result
     *            the result
     * @param closeQueue
     *            the indicator for closing of the queue
     */
    public void addResult(Result result, boolean closeQueue) {
        if (result != null) {
            if (isQueueClosed) {
                throw new IllegalStateException(
                        "Cannot add new Results after the Queue has been closed");
            } else {
                if (closeQueue) {
                    synchronized (queue) {
                        queue.add(result);
                        closeResultQueue();
                    }
                } else {
                    queue.add(result);
                }
            }
        } else {
            throw new IllegalArgumentException("Result cannot be null");
        }
    }

    /**
     * Adds a ${@link List} of ${@link Result}s to this QueryResponse, and specifies whether or not
     * to close the queue
     * 
     * @param results
     *            the results
     * @param closeQueue
     *            the indicator for closing of the queue
     */
    public void addResults(List<Result> results, boolean closeQueue) {
        if (results != null) {
            if (isQueueClosed) {
                throw new IllegalStateException(
                        "Cannot add new Results after the Queue has been closed");
            } else {
                if (closeQueue) {
                    synchronized (queue) {
                        queue.addAll(results);
                        closeResultQueue();
                    }
                } else {
                    queue.addAll(results);
                }
            }
        }
    }

    @Override
    public boolean hasMoreResults() {
        return !queue.isEmpty() || !isQueueClosed;
    }

    public void closeResultQueue() {
        isQueueClosed = true;
        queue.add(POISON_PILL_RESULT);
    }

    @Override
    public Result poll() {
        return hasMoreResults() ? queue.poll() : null;
    }

    @Override
    public Result poll(long timeout) {
        Result result = null;
        if (hasMoreResults()) {
            try {
                result = queue.poll(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.warn("QueryResponseImpl queue thread was interrputed.  Returning null for last result");
            }
        }
        return result;
    }

    @Override
    public Result take() {
        return hasMoreResults() ? handleTake() : null;
    }

    @Override
    public List<Result> take(long size) {
        List<Result> results = new ArrayList<Result>();
        if (hasMoreResults()) {
            Result result = null;
            for (int i = 0; i < size && (result = handleTake()) != null; i++) {
                results.add(result);
            }
        }
        return results;
    }

    /**
     * Returns a result off of the queue
     * 
     * @return result the result
     */
    private Result handleTake() {
        Result result = null;
        try {
            result = queue.take();
            if (result == POISON_PILL_RESULT) {
                result = null;
            }
        } catch (InterruptedException e) {
            logger.warn("QueryResponseImpl queue thread was interrputed.  Returning null for last result");
        }
        return result;
    }

    protected static class POISON_PILL_RESULT implements Result {

        @Override
        public Double getRelevanceScore() {
            return null;
        }

        @Override
        public Double getDistanceInMeters() {
            return null;
        }

        @Override
        public Metacard getMetacard() {
            return null;
        }

    }

}
