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

import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.operation.QueryRequest;

/**
 * Class used to iterate over batches of {@link Result} objects contained in a
 * {@link ddf.catalog.operation.QueryResponse} returned when executing a {@link QueryRequest}. The
 * class will fetch new lists of results as needed based on the page size provided until all results
 * that match the query have been exhausted. The class guarantees that each {@link List<Result>}
 * returned will contain a number of results equal to the page size specified in the
 * {@link ddf.catalog.operation.Query} until all results have been exhausted.
 * <p>
 * Since the class will use the page size provided in the query to fetch the results, its
 * value should be carefully set to avoid any memory or performance issues.
 * </p>
 */
public class ResultListIterable implements Iterable<List<Result>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultListIterable.class);

    private final QueryFunction queryFunction;

    private final QueryRequest queryRequest;

    /**
     * Creates an iterable that will call the {@link CatalogFramework} to retrieve the results
     * that match the {@link QueryRequest} provided.
     *
     * @param catalogFramework reference to the {@link CatalogFramework} to call to retrieve the
     *                         results.
     * @param queryRequest     request used to retrieve the results.
     */
    public ResultListIterable(CatalogFramework catalogFramework, QueryRequest queryRequest) {
        notNull(catalogFramework, "Catalog framework reference cannot be null");
        notNull(queryRequest, "Query request cannot be null");
        isTrue(queryRequest.getQuery()
                .getPageSize() > 1, "Page size must be greater than 1");

        if (LOGGER.isDebugEnabled()) {
            logQueryRequest(queryRequest);
        }

        this.queryFunction = catalogFramework::query;
        this.queryRequest = queryRequest;
    }

    /**
     * Creates an iterable that will call a {@link QueryFunction} to retrieve the results
     * that match the {@link QueryRequest} provided.
     *
     * @param queryFunction reference to the {@link QueryFunction} to call to retrieve the
     *                      results.
     * @param queryRequest  request used to retrieve the results.
     */
    public ResultListIterable(QueryFunction queryFunction, QueryRequest queryRequest) {
        notNull(queryFunction, "Query function cannot be null");
        notNull(queryRequest, "Query request cannot be null");
        isTrue(queryRequest.getQuery()
                .getPageSize() > 1, "Page size must be greater than 1");

        if (LOGGER.isDebugEnabled()) {
            logQueryRequest(queryRequest);
        }

        this.queryFunction = queryFunction;
        this.queryRequest = queryRequest;
    }

    @Override
    public Iterator<List<Result>> iterator() {
        return new BatchedQueryResultIterator(createResultIterator(queryFunction, queryRequest),
                queryRequest.getQuery()
                        .getPageSize());
    }

    Iterator<Result> createResultIterator(QueryFunction queryFunction, QueryRequest queryRequest) {
        return new ResultIterable(queryFunction, queryRequest).iterator();
    }

    private void logQueryRequest(QueryRequest queryRequest) {
        LOGGER.debug("QueryRequest {}, Query {}",
                queryRequest.toString(),
                queryRequest.getQuery()
                        .toString());
    }

    private static class BatchedQueryResultIterator implements Iterator<List<Result>> {

        private final Iterator<Result> results;

        private final int pageSize;

        BatchedQueryResultIterator(Iterator<Result> results, int pageSize) {
            this.results = results;
            this.pageSize = pageSize;
        }

        @Override
        public boolean hasNext() {
            return results.hasNext();
        }

        @Override
        public List<Result> next() {
            if (!results.hasNext()) {
                throw new NoSuchElementException();
            }

            List<Result> batchedResults = new ArrayList<>(pageSize);

            for (int i = 0; i < pageSize && results.hasNext(); i++) {
                batchedResults.add(results.next());
            }

            return batchedResults;
        }
    }
}
