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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.Validate;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Effectively a cursor over the results of a filter that automatically pages through all results
 * <br/>
 * Throws a {@link CatalogRuntimeException} if anything goes wrong during iteration or
 * querying
 */
public class QueryResultIterable implements Iterable<Result> {
    private final CatalogFramework catalog;

    private final QueryRequestImpl queryRequest;

    /**
     * For paging through a single filter with a default PAGE_SIZE of 64
     *
     * @param catalog      catalog to query
     * @param queryRequest A filter to query by
     */
    public QueryResultIterable(CatalogFramework catalog, QueryRequestImpl queryRequest) {
        Validate.notNull(catalog, "Catalog is null");
        Validate.notNull(queryRequest, "queryRequest is null");

        this.catalog = catalog;
        this.queryRequest = queryRequest;
    }

    @Override
    public Iterator<Result> iterator() {
        return new ResultQueryIterator();
    }

    // TODO: 6/7/17 Use Spliterator to implement paging design
    @Override
    public Spliterator<Result> spliterator() {
        int characteristics = Spliterator.DISTINCT;
        return Spliterators.spliteratorUnknownSize(this.iterator(), characteristics);
    }

    public Stream<Result> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    // TODO: 6/5/17 Cover with unit test(s) through public API
    class ResultQueryIterator implements Iterator<Result> {
        private int pageIndex = 1;

        private int missingResults = 0;

        private boolean finished = false;

        private SourceResponse response = null;

        private Iterator<Result> results = null;

        private static final int PAGE_SIZE = 64;

        @Override
        public boolean hasNext() {
            ensureInitialized();
            if (results.hasNext()) {
                return true;
            }
            if (finished) {
                return false;
            }

            pageIndex += (PAGE_SIZE - missingResults);
            missingResults = 0;
            queryNext();
            return hasNext();
        }

        @Override
        public Result next() {
            ensureInitialized();
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return results.next();
        }

        private void queryNext() {
            try {
                Map<String, Serializable> props = new HashMap<>();
                // Avoid caching all results while dumping with native query mode
                props.put("mode", "native");
                response = catalog.query(queryRequest);
                missingResults = PAGE_SIZE - response.getResults()
                        .size();

            } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
                throw new CatalogRuntimeException(e);
            }
            List<Result> queryResults = response.getResults();
            this.results = queryResults.iterator();

            int size = queryResults.size();
            if (size == 0 || size < PAGE_SIZE) {
                // TODO: 6/5/17 Address edge cases of this size comparison

                finished = true;
            }
        }

        private void ensureInitialized() {
            if (response != null || results != null) {
                return;
            }
            queryNext();
        }
    }
}