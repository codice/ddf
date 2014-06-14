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

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.util.impl.DistanceResultComparator;
import ddf.catalog.util.impl.RelevanceResultComparator;
import ddf.catalog.util.impl.TemporalResultComparator;
import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.HashBag;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the cached asynchronous query response from all sources.
 */
public class Search {

    private static final Logger LOGGER = LoggerFactory.getLogger(Search.class);

    protected static final Comparator<Result> DEFAULT_COMPARATOR = new TemporalResultComparator(
            SortOrder.DESCENDING, Metacard.MODIFIED);

    public static final String HITS = "hits";

    public static final String GUID = "guid";

    public static final String DISTANCE = "distance";

    public static final String RELEVANCE = "relevance";

    public static final String METACARD = "metacard";

    public static final String RESULTS = "results";

    public static final String SUCCESSFUL = "successful";

    public static final String SOURCES = "sources";

    public static final String ID = "id";

    public static final String DONE = "done";

    public static final String ELAPSED = "elapsed";

    private SearchRequest searchRequest;

    private QueryResponse compositeQueryResponse;

    private List<Result> results = new ArrayList<Result>(100);

    private Map<String, QueryStatus> queryStatus = new HashMap<String, QueryStatus>();

    private long hits = 0;

    private long responseNum = 0;

    /**
     * Adds a query response to the cached set of results.
     *
     * @param sourceId      - Source ID of query response
     * @param queryResponse - Query response to add
     * @return true, if the response for this paging set is different from the last time it was sent
     * @throws InterruptedException
     */
    public synchronized void addQueryResponse(String sourceId, QueryResponse queryResponse) throws InterruptedException {
        updateStatus(sourceId, queryResponse);
        if (queryResponse != null) {
            Comparator<Result> coreComparator = DEFAULT_COMPARATOR;
            if (compositeQueryResponse == null) {
                initializeCompositeResponse(queryResponse, coreComparator);
            } else {
                addResultsToCompositeResult(queryResponse, coreComparator);
            }
            updateHitStatus(compositeQueryResponse.getResults());
        }
        responseNum++;
    }

    private void updateStatus(String sourceId, QueryResponse queryResponse) {
        if (!queryStatus.containsKey(sourceId)) {
            queryStatus.put(sourceId, new QueryStatus(sourceId));
        }
        QueryStatus status = queryStatus.get(sourceId);
        status.setDetails(queryResponse.getProcessingDetails());
        status.setTotalHits(queryResponse.getHits());
        status.setElapsed((Long) queryResponse.getProperties().get("elapsed"));
        status.setDone(true);
    }

    private void initializeCompositeResponse(QueryResponse queryResponse,
            Comparator<Result> coreComparator) {
        LOGGER.debug("Creating new composite query response");
        compositeQueryResponse = queryResponse;
        results.addAll(compositeQueryResponse.getResults());
        // Sort the initial results
        Collections.sort(compositeQueryResponse.getResults(), coreComparator);
        hits = compositeQueryResponse.getHits();
    }

    private void addResultsToCompositeResult(QueryResponse queryResponse,
            Comparator<Result> coreComparator) {
        LOGGER.debug("Updating existing composite query response");
        List<Result> latestResultList = queryResponse.getResults();

        results.addAll(latestResultList);

        // if there wasn't at least 1 request there, we wouldn't be here in the first place
        SortBy sortBy = searchRequest.getQuery().getSortBy();

        // Prepare the Comparators that we will use
        if (sortBy != null && sortBy.getPropertyName() != null) {
            PropertyName sortingProp = sortBy.getPropertyName();
            String sortType = sortingProp.getPropertyName();
            SortOrder sortOrder = (sortBy.getSortOrder() == null) ? SortOrder.DESCENDING
                    : sortBy.getSortOrder();

            if (Metacard.EFFECTIVE.equals(sortType)) {
                LOGGER.debug("Sorting by EFFECTIVE Date");
                coreComparator = new TemporalResultComparator(sortOrder, Metacard.EFFECTIVE);
            } else if (Result.DISTANCE.equals(sortType)) {
                LOGGER.debug("Sorting by DISTANCE");
                coreComparator = new DistanceResultComparator(sortOrder);
            } else if (Result.RELEVANCE.equals(sortType)) {
                LOGGER.debug("Sorting by RELEVANCE");
                coreComparator = new RelevanceResultComparator(sortOrder);
            }
        }

        // Sort the combination of all results we have received
        Collections.sort(results, coreComparator);

        List<Result> compositeResultList;
        int end;
        if (results.size() >= compositeQueryResponse.getRequest().getQuery().getPageSize()) {
            end = compositeQueryResponse.getRequest().getQuery().getPageSize();
        } else {
            end = results.size();
        }

        compositeResultList = results.subList(0, end);

        hits += queryResponse.getHits();

        compositeQueryResponse = new QueryResponseImpl(queryResponse.getRequest(), compositeResultList, true,
                hits);
    }

    private void updateHitStatus(List<Result> results) {
        Bag hitSourceCount = new HashBag();

        for (String sourceId : queryStatus.keySet()) {
            queryStatus.get(sourceId).setHits(0);
        }

        for (Result result : results) {
            hitSourceCount.add(result.getMetacard().getSourceId());
        }

        for (Object sourceId : hitSourceCount.uniqueSet()) {
            if (queryStatus.containsKey(sourceId)) {
                queryStatus.get(sourceId).setHits(hitSourceCount.getCount(sourceId));
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
}
