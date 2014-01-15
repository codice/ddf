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
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;

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
 * Created by tustisos on 12/11/13.
 */
public class Search {

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(Search.class));

    protected static final Comparator<Result> DEFAULT_COMPARATOR = new RelevanceResultComparator(
            SortOrder.DESCENDING);

    private SearchRequest searchRequest;

    private Semaphore lock = new Semaphore(1, true);

    private QueryResponse compositeQueryResponse;

    private List<Result> resultList = new ArrayList<Result>(100);

    private Calendar lastAccessedTime = Calendar.getInstance();

    private long hits = 0;

    private long startIndex = 0;

    public boolean addQueryResponse(QueryResponse queryResponse) throws InterruptedException {
        lock.acquire();
        boolean changed = false;
        try {
            if(queryResponse != null) {
                if (compositeQueryResponse == null) {
                    compositeQueryResponse = queryResponse;
                    resultList.addAll(compositeQueryResponse.getResults());
                    hits = compositeQueryResponse.getHits();
                    startIndex = compositeQueryResponse.getRequest().getQuery().getStartIndex();
                    lastAccessedTime = Calendar.getInstance();
                } else {
                    List<Result> latestResultList = queryResponse.getResults();
                    List<Result> originalResultList = compositeQueryResponse.getResults();

                    resultList.addAll(latestResultList);

                    SortBy sortBy = searchRequest.getLocalQueryRequest().getSortBy();
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
                    if(resultList.size() >= queryResponse.getRequest().getQuery().getStartIndex() + compositeQueryResponse.getRequest().getQuery().getPageSize()) {
                        int start = queryResponse.getRequest().getQuery().getStartIndex()-1;
                        int end = start + compositeQueryResponse.getRequest().getQuery().getPageSize();
                        compositeResultList = resultList.subList(start, end);
                    } else {
                        int start = queryResponse.getRequest().getQuery().getStartIndex()-1;
                        int end = resultList.size();
                        compositeResultList = resultList.subList(start, end);
                    }

                    if (!originalResultList.containsAll(compositeResultList)) {
                        changed = true;
                    }

                    if(queryResponse.getRequest().getQuery().getStartIndex() == startIndex) {
                        hits += queryResponse.getHits();
                    }

                    compositeQueryResponse = new QueryResponseImpl(
                            queryResponse.getRequest(), compositeResultList, true,
                            hits);

                    lastAccessedTime = Calendar.getInstance();
                }
            }
        } finally {
            lock.release();
        }
        return changed;
    }

    public Object toJSON() {
        //TODO: do something here
        return new Object();
    }

    public Calendar getLastAccessedTime() {
        return lastAccessedTime;
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
