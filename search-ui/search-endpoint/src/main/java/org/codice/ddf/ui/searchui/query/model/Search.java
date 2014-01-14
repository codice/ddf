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
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

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

    protected static final Comparator<Result> DEFAULT_COMPARATOR = new RelevanceResultComparator(
            SortOrder.DESCENDING);

    private SearchRequest searchRequest;

    private Semaphore lock = new Semaphore(1, true);

    private QueryResponse compositeQueryResponse;

    private UUID guid;

    public boolean addQueryResponse(QueryResponse queryResponse) throws InterruptedException {
        lock.acquire();
        boolean changed = false;
        try {
            if(queryResponse != null) {
                if (compositeQueryResponse == null || queryResponse.getRequest().getQuery().getStartIndex() != compositeQueryResponse.getRequest().getQuery().getStartIndex()) {
                    compositeQueryResponse = queryResponse;
                } else {
                    List<Result> originalResultList = compositeQueryResponse.getResults();
                    List<Result> compositeResultList = new ArrayList<Result>(
                            compositeQueryResponse.getResults());
                    List<Result> latestResultList = queryResponse.getResults();

                    compositeResultList.addAll(latestResultList);

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

                    Collections.sort(compositeResultList, coreComparator);

                    compositeResultList = compositeResultList.subList(0, originalResultList.size());

                    if (!originalResultList.containsAll(compositeResultList)) {
                        changed = true;
                    }

                    compositeQueryResponse = new QueryResponseImpl(
                            compositeQueryResponse.getRequest(), compositeResultList, true,
                            compositeQueryResponse.getHits() + queryResponse.getHits());
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

    public UUID getGuid() {
        return guid;
    }

    public void setGuid(UUID guid) {
        this.guid = guid;
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
