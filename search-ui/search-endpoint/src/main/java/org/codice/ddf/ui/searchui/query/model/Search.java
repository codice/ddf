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
package org.codice.ddf.ui.searchui.query.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.HashBag;
import org.codice.ddf.ui.searchui.query.model.QueryStatus.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Result;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryResponse;

/**
 * This class represents the cached asynchronous query response from all sources.
 */
public class Search {

    private static final Logger LOGGER = LoggerFactory.getLogger(Search.class);

    public static final String HITS = "hits";

    public static final String DISTANCE = "distance";

    public static final String RELEVANCE = "relevance";

    public static final String METACARD = "metacard";

    public static final String RESULTS = "results";

    public static final String TYPES = "types";

    public static final String SUCCESSFUL = "successful";

    public static final String STATUS = "status";

    public static final String STATE = "state";

    public static final String ID = "id";

    public static final String DONE = "done";

    public static final String ELAPSED = "elapsed";

    private SearchRequest searchRequest;

    private QueryResponse compositeQueryResponse;

    private Map<String, QueryStatus> queryStatus = new HashMap<String, QueryStatus>();

    private long hits = 0;

    private long responseNum = 0;

    /**
     * Adds a query response to the cached set of results.
     *
     * @param queryResponse - Query response to add
     *
     * @throws InterruptedException
     */
    public synchronized void addQueryResponse(QueryResponse queryResponse)
            throws InterruptedException {
        if (queryResponse != null) {
            compositeQueryResponse = queryResponse;
            updateResultStatus(queryResponse.getResults());
        }
    }

    public void updateStatus(String sourceId, QueryResponse queryResponse) {
        if (!queryStatus.containsKey(sourceId)) {
            queryStatus.put(sourceId, new QueryStatus(sourceId));
        }
        QueryStatus status = queryStatus.get(sourceId);
        status.setDetails(queryResponse.getProcessingDetails());
        status.setHits(queryResponse.getHits());
        hits += queryResponse.getHits();
        status.setElapsed((Long) queryResponse.getProperties().get("elapsed"));
        status.setState((isSuccessful(queryResponse.getProcessingDetails()) ? State.SUCCEEDED : State.FAILED));
        responseNum++;
    }

    private boolean isSuccessful(final Set<ProcessingDetails> details) {
        for (ProcessingDetails detail : details) {
            if (detail.hasException()) {
                return false;
            }
        }
        return true;
    }
    
    private void updateResultStatus(List<Result> results) {
        Bag hitSourceCount = new HashBag();

        for (String sourceId : queryStatus.keySet()) {
            queryStatus.get(sourceId).setResultCount(0);
        }

        for (Result result : results) {
            hitSourceCount.add(result.getMetacard().getSourceId());
        }

        for (Object sourceId : hitSourceCount.uniqueSet()) {
            if (queryStatus.containsKey(sourceId)) {
                queryStatus.get(sourceId).setResultCount(hitSourceCount.getCount(sourceId));
            }
        }
    }

    public boolean isFinished() {
        return responseNum >= searchRequest.getSourceIds().size();
    }

    public SearchRequest getSearchRequest() {
        return searchRequest;
    }

    public void setSearchRequest(SearchRequest request) {
        searchRequest = request;

        for (String sourceId : searchRequest.getSourceIds()) {
            queryStatus.put(sourceId, new QueryStatus(sourceId));
        }
    }

    public QueryResponse getCompositeQueryResponse() {
        return compositeQueryResponse;
    }

    public Map<String, QueryStatus> getQueryStatus() {
        return queryStatus;
    }

    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }
}
