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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;

import org.apache.commons.lang.Validate;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.operation.QueryRequest;

/**
 * Class that facilitates iteration over individual catalog results.
 * <br/>
 * Throws a {@link CatalogQueryException} if anything goes wrong during iteration or
 * querying.
 */
public class QueryResultIterable implements Iterable<Result> {
    private final CatalogFramework catalog;

    private final QueryRequest queryRequest;

    /**
     * Constructor for QueryResultIterable.
     *
     * @param catalogFramework catalog to query
     * @param queryRequest     queryRequest used to query the catalog framework for results. Query
     *                         parameters such as {@link ddf.catalog.operation.Query#getPageSize()} and
     *                         {@link ddf.catalog.operation.Query#getStartIndex()} will be used and are guaranteed to be
     *                         respected when valid, regardless of any limit imposed by the catalog
     *                         framework on the result size.
     */
    public QueryResultIterable(CatalogFramework catalogFramework, QueryRequest queryRequest) {
        Validate.notNull(catalogFramework, "Catalog is null");
        Validate.notNull(queryRequest, "queryRequest is null");

        this.catalog = catalogFramework;
        this.queryRequest = queryRequest;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The {@link Iterator} will retrieve results from the {@link CatalogFramework} as needed,
     * based on the start index and page size provided in the query.
     */
    @Override
    public Iterator<Result> iterator() {
        return new QueryResultIterator(new QueryResultPaginator(catalog, queryRequest));
    }

    @Override
    public Spliterator<Result> spliterator() {
        int characteristics = Spliterator.DISTINCT;
        return Spliterators.spliteratorUnknownSize(this.iterator(), characteristics);
    }

    static class QueryResultIterator implements Iterator<Result> {

        private QueryResultPaginator queryResultPaginator;

        private Iterator<Result> queriedResults = null;

        public QueryResultIterator(QueryResultPaginator queryResultPaginator) {
            this.queryResultPaginator = queryResultPaginator;
        }

        @Override
        public boolean hasNext() {

            if (isQueriedResultListNullOrEmpty()) {
                return queryResultPaginator.hasNext();
            }

            return true;
        }

        @Override
        public Result next() throws NoSuchElementException {

            if (isQueriedResultListNullOrEmpty()) {
                queriedResults = queryResultPaginator.next()
                        .iterator();
            }

            return queriedResults.next();
        }

        private boolean isQueriedResultListNullOrEmpty() {
            return queriedResults == null || !queriedResults.hasNext();
        }
    }
}
