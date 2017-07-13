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
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * This class controls paging of {@link CatalogFramework} queries by batching them into lists of
 * results, independent of the number of results the Catalog Framework returns with each query.
 * This helps clients guarantee a particular batch size rather than getting fewer results than
 * expected from the Catalog Framework.
 */
class QueryResultPaginator {

    private final CatalogFramework catalogFramework;

    private QueryRequestImpl queryRequestCopy;

    // Used to query the catalog framework and adjust the starting index of the query without
    // modifying the original query.
    private QueryImpl queryCopy;

    // Used to track the start index of the next query and ensure that the correct query results
    // are returned with no repetition of results.
    private int indexOffset;

    private final int pageSize;

    // Set to true when a query returns zero results and no more results can be fetched from
    // Catalog Framework.
    private boolean noMoreCatalogFrameworkResultsToFetch;

    // Used to buffer fetched results from the CatalogFramework and hold them until they can be
    // drained into a result list to return to the client.
    private BlockingQueue<Result> queriedResultsBuffer;

    /**
     * Creates a paginator based on the {@link Query} provided.
     * <p>
     * The page size provided in the {@link Query} is the guaranteed size of any list of results
     * returned to a client.
     * </p>
     *
     * @param catalogFramework reference to the {@link CatalogFramework}
     * @param queryRequest     client query. Will be used to query the {@link CatalogFramework}
     *                         and guarantee that the number of results returned will always match
     */
    public QueryResultPaginator(CatalogFramework catalogFramework, QueryRequest queryRequest) {
        Validate.notNull(catalogFramework);
        Validate.notNull(queryRequest);

        this.catalogFramework = catalogFramework;
        copyQueryRequestAndQuery(queryRequest);

        noMoreCatalogFrameworkResultsToFetch = false;
        pageSize = queryCopy.getPageSize();
        indexOffset = queryCopy.getStartIndex();
        queriedResultsBuffer = new LinkedBlockingDeque<>();
    }

    public boolean hasNext() {

        if (notEmpty()) {
            return true;
        }

        if (noMoreCatalogFrameworkResultsToFetch) {
            return false;
        }

        // Buffer is empty, but query might not be exhausted.
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

    private boolean notEmpty() {
        return !queriedResultsBuffer.isEmpty();
    }

    private void copyQueryRequestAndQuery(QueryRequest queryRequest) {

        Query query = queryRequest.getQuery();

        this.queryCopy = new QueryImpl(query,
                query.getStartIndex(),
                query.getPageSize(),
                query.getSortBy(),
                query.requestsTotalResultsCount(),
                query.getTimeoutMillis());

        this.queryRequestCopy = new QueryRequestImpl(queryCopy,
                queryRequest.isEnterprise(),
                queryRequest.getSourceIds(),
                queryRequest.getProperties());
    }

    /**
     * Gets the next batch of results and adds them to the {@link #queriedResultsBuffer}.
     * Assumes the current index is pointed to the first result that has not been retrieved.
     * Updates the current index after the results are fetched.
     * If no results were returned, assumes there are no more results.
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
            return catalogFramework.query(queryRequestCopy)
                    .getResults();
        } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
            throw new CatalogQueryException(e);
        }
    }

    private int getBufferSize() {
        return queriedResultsBuffer.size();
    }
}
