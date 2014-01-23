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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.util.impl.DistanceResultComparator;
import ddf.catalog.util.impl.RelevanceResultComparator;
import ddf.catalog.util.impl.TemporalResultComparator;

/**
 * This class represents the cached asynchronous query response from all sources.
 */
public class Search {

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(Search.class));

    protected static final Comparator<Result> DEFAULT_COMPARATOR = new RelevanceResultComparator(
            SortOrder.DESCENDING);

    private SearchRequest searchRequest;

    private QueryResponse compositeQueryResponse;

    private List<Result> resultList = new ArrayList<Result>(100);

    private long hits = 0;

    private long startIndex = 0;

    /**
     * Adds a query response to the cached set of results.
     * 
     * @param queryResponse
     *            - Query response to add
     * @return true, if the response for this paging set is different from the last time it was sent
     * @throws InterruptedException
     */
    public synchronized boolean addQueryResponse(QueryResponse queryResponse) throws InterruptedException {
        boolean changed = false;
        if (queryResponse != null) {
            if (compositeQueryResponse == null) {
                LOGGER.debug("Creating new composite query response");
                compositeQueryResponse = queryResponse;
                resultList.addAll(compositeQueryResponse.getResults());
                hits = compositeQueryResponse.getHits();
                startIndex = compositeQueryResponse.getRequest().getQuery().getStartIndex();
                changed = true;
            } else {
                LOGGER.debug("Updating existing composite query response");
                startIndex = queryResponse.getRequest().getQuery().getStartIndex();
                List<Result> latestResultList = queryResponse.getResults();
                List<Result> originalResultList = compositeQueryResponse.getResults();

                resultList.addAll(latestResultList);

                // if there wasn't at least 1 request there, we wouldn't be here in the first place
                SortBy sortBy = searchRequest.getQueryRequests().get(0).getSortBy();
                // Prepare the Comparators that we will use
                Comparator<Result> coreComparator = DEFAULT_COMPARATOR;

                if (sortBy != null && sortBy.getPropertyName() != null) {
                    PropertyName sortingProp = sortBy.getPropertyName();
                    String sortType = sortingProp.getPropertyName();
                    SortOrder sortOrder = (sortBy.getSortOrder() == null) ? SortOrder.DESCENDING
                            : sortBy.getSortOrder();

                    // Temporal searches are currently sorted by the effective time
                    if (Metacard.EFFECTIVE.equals(sortType) || Result.TEMPORAL.equals(sortType)) {
                        coreComparator = new TemporalResultComparator(sortOrder);
                    } else if (Result.DISTANCE.equals(sortType)) {
                        coreComparator = new DistanceResultComparator(sortOrder);
                    } else if (Result.RELEVANCE.equals(sortType)) {
                        coreComparator = new RelevanceResultComparator(sortOrder);
 }
                }

                Collections.sort(resultList, coreComparator);

                List<Result> compositeResultList;
                if (resultList.size() >= queryResponse.getRequest().getQuery().getStartIndex()
                        + compositeQueryResponse.getRequest().getQuery().getPageSize()) {
                    int start = queryResponse.getRequest().getQuery().getStartIndex() - 1;
                    int end = start + compositeQueryResponse.getRequest().getQuery().getPageSize();
                    compositeResultList = resultList.subList(start, end);
                } else {
                    int start = queryResponse.getRequest().getQuery().getStartIndex() - 1;
                    int end = resultList.size();
                    compositeResultList = resultList.subList(start, end);
                }

                // we want to make sure we pass back any initial results so any UI building a list
                // can
                // actually know when all searches have returned
                // each subsequent query is coming from cached results, so it doesn't really matter
                // if we send back every response from a source after that
                if (!originalResultList.containsAll(compositeResultList) || startIndex == 1) {
                    changed = true;
                } else {
                    LOGGER.debug("Response list has not changed, so message sent will be empty");
                }

                // Just look for all the responses that come back with a start index of 1 so we
                // don't update
                // too many times
                if (queryResponse.getRequest().getQuery().getStartIndex() == 1) {
                    hits += queryResponse.getHits();
                }

                compositeQueryResponse = new QueryResponseImpl(queryResponse.getRequest(), compositeResultList, true,
                        hits);
            }
        }
        return changed;
    }

    /**
     * Returns an empty query response. This is used to notify a client that a source has returned
     * without sending duplicate results.
     * 
     * @return QueryResponse
     */
    public QueryResponse getEmptyQueryResponse() {
        QueryResponse response = new QueryResponseImpl(
                compositeQueryResponse.getRequest(), new ArrayList<Result>(), true,
                hits);

        return response;
    }

    public SearchRequest getSearchRequest() {
        return searchRequest;
    }

    public void setSearchRequest(SearchRequest searchRequest) {
        this.searchRequest = searchRequest;
    }

    public QueryResponse getCompositeQueryResponse() {
        return compositeQueryResponse;
    }
}
