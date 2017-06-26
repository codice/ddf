/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.util.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.commons.lang3.Validate;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Implements iterator. Acts like a decorator.
 * NOTE: This class is not thread safe. Do not share an object between multiple threads.
 * Create a new instance by passing a QueryRequestImpl and a CatalogFramework to the constructor.
 * The paginator will inspect the query's start index and desired page size.
 * Calling next() returns a list of Result objects in the desired page size.
 * If the page size is not evenly divisible by the number of results,
 * then the last page will be smaller than the desired page size.
 */
public class QueryResultPaginator implements Iterator<List<Result>> {

    private final CatalogFramework catalogFramework;

    private final QueryImpl copyOfQuery;

    private int currentIndex;

    private int pageSize;

    //Set to true when a query returns zero results.
    private boolean exhausted;

    private BlockingQueue<Result> buffer;

    public QueryResultPaginator(CatalogFramework catalogFramework, QueryImpl query) {
        Validate.notNull(catalogFramework);
        Validate.notNull(query);
        this.catalogFramework = catalogFramework;
        this.copyOfQuery = copy(query);
        pageSize = copyOfQuery.getPageSize();
        currentIndex = copyOfQuery.getStartIndex();
        Validate.inclusiveBetween(1,
                Integer.MAX_VALUE,
                currentIndex,
                "Query request start index must be greater than zero");
        exhausted = false;
        buffer = new LinkedBlockingDeque<>();
    }

    /**
     * @return true if there are more results for the client
     */
    public boolean hasNext() {

        boolean hasMoreResults;
        if (notEmpty()) {
            return true;
        } else {
            if (exhausted) {
                return false;
            } else {
                //Buffer is empty, but query might not be exhausted.
                fetchNext();
                return notEmpty();
            }
        }
    }

    protected boolean notEmpty() {
        return !buffer.isEmpty();
    }

    /**
     * Catalog Framework queried for results until current page is full of results
     * or endIndex is reached.
     *
     * @return Collection of results
     * @throws NoSuchElementException if the iteration has no more elements
     */
    public List<Result> next() throws NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        while (!exhausted && getBufferSize() < pageSize) {
            fetchNext();
        }

        List<Result> copy = new ArrayList<>(pageSize);
        buffer.drainTo(copy, pageSize);
        return copy;
    }

    int getBufferSize() {
        return buffer.size();
    }

    QueryImpl copy(QueryImpl query) {

        return new QueryImpl(query.getFilter(),
                query.getStartIndex(),
                query.getPageSize(),
                query.getSortBy(),
                query.requestsTotalResultsCount(), query.getTimeoutMillis());
    }

    int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Get the next batch of results.
     * Assume the current index is pointed to the first result
     * that has not been retrieved.
     * Update the current index after the results are fetched.
     * Add the fetched results to the buffer.
     * If no results were returned, assume there are no more results possible.
     */
    private void fetchNext() {
        copyOfQuery.setStartIndex(currentIndex);
        List<Result> results = queryCatalogFramework();
        if (results.isEmpty()) {
            exhausted = true;
        } else {
            currentIndex += results.size();
            buffer.addAll(results);
        }
    }

    private List<Result> queryCatalogFramework() {
        try {
            return catalogFramework.query(new QueryRequestImpl(copyOfQuery))
                    .getResults();
        } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
            throw new CatalogRuntimeException(e);
        }
    }
}