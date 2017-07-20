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

import static org.apache.commons.lang.Validate.notNull;
import static ddf.catalog.Constants.DEFAULT_PAGE_SIZE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Class used to iterate over the {@link Result} objects contained in a
 * {@link ddf.catalog.operation.QueryResponse} returned when executing a {@link QueryRequest}. The
 * class will fetch new results as needed until all results that match the query provided have been
 * exhausted.
 * <p>
 * Since the class may use the page size provided in the {@link Query} to fetch the results, its
 * value should be carefully set to avoid any memory or performance issues.
 * </p>
 */
public class ResultIterable implements Iterable<Result> {

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
    public ResultIterable(CatalogFramework catalogFramework, QueryRequest queryRequest) {
        notNull(catalogFramework, "Catalog framework reference cannot be null");
        notNull(queryRequest, "Query request cannot be null");

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
    public ResultIterable(QueryFunction queryFunction, QueryRequest queryRequest) {
        notNull(queryFunction, "Query function cannot be null");
        notNull(queryRequest, "Query request cannot be null");

        this.queryFunction = queryFunction;
        this.queryRequest = queryRequest;
    }

    @Override
    public Iterator<Result> iterator() {
        return new QueryResultIterator(queryFunction, queryRequest);
    }

    public Stream<Result> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(),
                Spliterator.ORDERED), false);
    }

    private static class QueryResultIterator implements Iterator<Result> {

        private static final Iterator<Result> RESULTS_EXHAUSTED =
                new ArrayList<Result>().iterator();

        private final QueryFunction queryFunction;

        private int currentIndex;

        private QueryImpl queryCopy;

        private QueryRequestImpl queryRequestCopy;

        private Iterator<Result> results = Collections.emptyIterator();

        QueryResultIterator(QueryFunction queryFunction, QueryRequest queryRequest) {
            this.queryFunction = queryFunction;

            copyQueryRequestAndQuery(queryRequest);

            this.currentIndex = queryCopy.getStartIndex();
        }

        @Override
        public boolean hasNext() {
            if (results == RESULTS_EXHAUSTED) {
                return false;
            }

            if (results.hasNext()) {
                return true;
            }

            fetchNextResults();

            return results.hasNext();
        }

        @Override
        public Result next() {
            if (results == RESULTS_EXHAUSTED) {
                throw new NoSuchElementException("No more results match the specified query");
            }

            if (results.hasNext()) {
                return results.next();
            }

            fetchNextResults();

            if (!results.hasNext()) {
                throw new NoSuchElementException("No more results match the specified query");
            }

            return results.next();
        }

        private void fetchNextResults() {
            queryCopy.setStartIndex(currentIndex);

            try {
                SourceResponse response = queryFunction.query(queryRequestCopy);

                if (response.getResults()
                        .size() == 0) {
                    results = RESULTS_EXHAUSTED;
                    return;
                }

                results = response.getResults()
                        .iterator();
                currentIndex += response.getResults()
                        .size();
            } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
                throw new CatalogQueryException(e);
            }
        }

        private void copyQueryRequestAndQuery(QueryRequest queryRequest) {
            Query query = queryRequest.getQuery();

            int pageSize = query.getPageSize() > 1 ? query.getPageSize() : DEFAULT_PAGE_SIZE;

            this.queryCopy = new QueryImpl(query,
                    query.getStartIndex(),
                    pageSize,
                    query.getSortBy(),
                    query.requestsTotalResultsCount(),
                    query.getTimeoutMillis());

            this.queryRequestCopy = new QueryRequestImpl(queryCopy,
                    queryRequest.isEnterprise(),
                    queryRequest.getSourceIds(),
                    queryRequest.getProperties());
        }
    }
}
