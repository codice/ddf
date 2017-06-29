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
 * QueryResultPaginator controls paging of CatalogFramework queries by batching them into
 * lists of results to return to an iterator class, independent of the number of results
 * the Catalog Framework returns with each query. This helps clients guarantee a particular
 * batch size rather than operate at the mercy of the Catalog Framework.
 * <p>
 * The pageSize is the guaranteed size of any list of results returned to a client.
 * <p>
 * The indexOffset is tracked to ensure that the correct query results are returned
 * and no repetition of results occurs.
 * <p>
 * The queryCopy exists to help adjust the starting index of the query without modifying
 * the original query.
 * <p>
 * The BlockingQueue of Result type is used to buffer fetched results from the
 * Catalog Framework and hold them until they can be drained into a result list to
 * return to the client.
 * <p>
 * The boolean `noMoreCatalogFrameworkResultsToFetch` is updated to true when no more
 * results can be fetched from Catalog Framework.
 */
public class QueryResultPaginator {

    private final CatalogFramework catalogFramework;

    private final QueryImpl queryCopy;

    private int indexOffset;

    private int pageSize;

    //Set to true when a query returns zero results.
    private boolean noMoreCatalogFrameworkResultsToFetch;

    private BlockingQueue<Result> queriedResultsBuffer;

    public QueryResultPaginator(CatalogFramework catalogFramework, QueryImpl query) {

        Validate.notNull(catalogFramework);
        Validate.notNull(query);

        this.catalogFramework = catalogFramework;
        this.queryCopy = copy(query);

        noMoreCatalogFrameworkResultsToFetch = false;
        pageSize = queryCopy.getPageSize();
        indexOffset = queryCopy.getStartIndex();

        Validate.inclusiveBetween(1,
                Integer.MAX_VALUE,
                indexOffset,
                "Query request start index must be greater than zero");

        queriedResultsBuffer = new LinkedBlockingDeque<>();
    }

    public boolean hasNext() {

        if (notEmpty()) {
            return true;
        }

        if (noMoreCatalogFrameworkResultsToFetch) {
            return false;
        }

        //Buffer is empty, but query might not be exhausted.
        fetchNext();
        return notEmpty();
    }

    public List<Result> next() throws NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        while (!noMoreCatalogFrameworkResultsToFetch && getBufferSize() < pageSize) {
            fetchNext();
        }

        List<Result> queriedResultsBufferCopy = new ArrayList<>(pageSize);
        queriedResultsBuffer.drainTo(queriedResultsBufferCopy, pageSize);
        return queriedResultsBufferCopy;
    }

    protected boolean notEmpty() {
        return !queriedResultsBuffer.isEmpty();
    }

    QueryImpl copy(QueryImpl query) {
        return new QueryImpl(query.getFilter(),
                query.getStartIndex(),
                query.getPageSize(),
                query.getSortBy(),
                query.requestsTotalResultsCount(),
                query.getTimeoutMillis());
    }

    int getBufferSize() {
        return queriedResultsBuffer.size();
    }

    int getIndexOffset() {
        return indexOffset;
    }

    /**
     * Get the next batch of results.
     * Assume the current index is pointed to the first result
     * that has not been retrieved.
     * Update the current index after the results are fetched.
     * Add the fetched results to the queriedResultsBuffer.
     * If no results were returned, assume there are no more results possible.
     */
    private void fetchNext() {

        queryCopy.setStartIndex(indexOffset);
        List<Result> resultList = queryCatalogFramework();

        if (resultList.isEmpty()) {
            noMoreCatalogFrameworkResultsToFetch = true;
        } else {
            indexOffset += resultList.size();
            queriedResultsBuffer.addAll(resultList);
        }
    }
    
    private List<Result> queryCatalogFramework() {
        try {
            return catalogFramework.query(new QueryRequestImpl(queryCopy))
                    .getResults();
        } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
            throw new CatalogQueryException(e);
        }
    }
}
