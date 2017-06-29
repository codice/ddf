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
import java.util.Spliterator;
import java.util.Spliterators;

import org.apache.commons.lang3.Validate;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.operation.impl.QueryImpl;

/**
 * Class that facilitates iteration over individual catalog results
 * <br/>
 * Throws a {@link CatalogQueryException} if anything goes wrong during iteration or
 * querying
 */
public class QueryResultIterable implements Iterable<Result> {
    private final CatalogFramework catalog;

    private final QueryImpl query;

    /**
     * Constructor for QueryResultIterable
     *
     * @param catalogFramework catalogFramework to query
     * @param query            Query request structure
     */
    public QueryResultIterable(CatalogFramework catalogFramework, QueryImpl query) {
        Validate.notNull(catalogFramework, "Catalog is null");
        Validate.notNull(query, "queryRequest is null");

        this.catalog = catalogFramework;
        this.query = query;
    }

    @Override
    public Iterator<Result> iterator() {
        return new ResultQueryIterator();
    }

    @Override
    public Spliterator<Result> spliterator() {
        int characteristics = Spliterator.DISTINCT;
        return Spliterators.spliteratorUnknownSize(this.iterator(), characteristics);
    }
    
    class ResultQueryIterator implements Iterator<Result> {

        private QueryResultPaginator queryResultPaginator;

        private Iterator<Result> queriedResults = null;

        public ResultQueryIterator() {
            this.queryResultPaginator = new QueryResultPaginator(catalog, query);
        }

        @Override
        public boolean hasNext() {

            if (isQueriedResultListNullOrEmpty()) {
                return queryResultPaginator.hasNext();
            }

            return true;
        }

        @Override
        public Result next() {

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
