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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private SearchRequest searchRequest;

    private QueryResponse compositeQueryResponse;

    private List<Result> resultList = new ArrayList<Result>(100);

    private long hits = 0;

    private long responseNum = 0;

    /**
     * Adds a query response to the cached set of results.
     * 
     * @param queryResponse
     *            - Query response to add
     * @return true, if the response for this paging set is different from the last time it was sent
     * @throws InterruptedException
     */
    public synchronized void addQueryResponse(QueryResponse queryResponse) throws InterruptedException {
        if (queryResponse != null) {
            Comparator<Result> coreComparator = DEFAULT_COMPARATOR;
            if (compositeQueryResponse == null) {
                LOGGER.debug("Creating new composite query response");
                compositeQueryResponse = queryResponse;
                resultList.addAll(compositeQueryResponse.getResults());
                // Sort the initial results
                Collections.sort(compositeQueryResponse.getResults(), coreComparator);
                hits = compositeQueryResponse.getHits();
                responseNum++;
            } else {
                LOGGER.debug("Updating existing composite query response");
                List<Result> latestResultList = queryResponse.getResults();

                resultList.addAll(latestResultList);

                // if there wasn't at least 1 request there, we wouldn't be here in the first place
                SortBy sortBy = searchRequest.getQueryRequests().get(0).getSortBy();

                // Prepare the Comparators that we will use
                if (sortBy != null && sortBy.getPropertyName() != null) {
                    PropertyName sortingProp = sortBy.getPropertyName();
                    String sortType = sortingProp.getPropertyName();
                    SortOrder sortOrder = (sortBy.getSortOrder() == null) ? SortOrder.DESCENDING
                            : sortBy.getSortOrder();

                    // Temporal searches are currently sorted by the effective time
                    if (Metacard.EFFECTIVE.equals(sortType)) {
                        // if (Metacard.EFFECTIVE.equals(sortType) ||
                        // Result.TEMPORAL.equals(sortType)) {
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

                // Sort the combination of all results we have recieved
                Collections.sort(resultList, coreComparator);

                List<Result> compositeResultList;
                int end;
                if (resultList.size() >= compositeQueryResponse.getRequest().getQuery().getPageSize()) {
                    end = compositeQueryResponse.getRequest().getQuery().getPageSize();
                } else {
                    end = resultList.size();
                }

                compositeResultList = resultList.subList(0, end);

                hits += queryResponse.getHits();

                responseNum++;

                compositeQueryResponse = new QueryResponseImpl(queryResponse.getRequest(), compositeResultList, true,
                        hits);
            }
        }
    }

    public boolean isFinished() {
        return responseNum >= searchRequest.getQueryRequests().size();
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
