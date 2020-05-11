/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.commands.util;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Effectively a cursor over the results of a filter that automatically pages through all results
 * <br>
 * Throws a {@link CatalogCommandRuntimeException} if anything goes wrong during iteration or
 * querying
 */
public class QueryResultIterable implements Iterable<Result> {
  private final CatalogFramework catalog;

  private final Function<Integer, QueryRequest> filter;

  private final int pageSize;

  /**
   * For paging through a single filter with a default pageSize of 64
   *
   * @param catalog catalog to query
   * @param filter A dynamic supplier of a filter that takes the current index such that the caller
   *     can control iteration based on their own logic
   */
  public QueryResultIterable(CatalogFramework catalog, Function<Integer, QueryRequest> filter) {
    this(catalog, filter, 64);
  }

  /**
   * For paging through a single filter.
   *
   * @param catalog catalog to query
   * @param filter A dynamic supplier of a filter that takes the current index such that the caller
   *     can control iteration based on their own logic
   * @param pageSize How many results should each page hold
   */
  public QueryResultIterable(
      CatalogFramework catalog, Function<Integer, QueryRequest> filter, int pageSize) {
    this.catalog = catalog;
    this.filter = filter;
    this.pageSize = pageSize;
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

  public Stream<Result> stream() {
    return StreamSupport.stream(this.spliterator(), false);
  }

  class ResultQueryIterator implements Iterator<Result> {
    private int pageIndex = 1;

    private boolean finished = false;

    private SourceResponse response = null;

    private Iterator<Result> results = null;

    ResultQueryIterator() {
      if (pageSize <= 0) {
        this.finished = true;
      }
    }

    @Override
    public boolean hasNext() {
      ensureInitialized();
      if (results.hasNext()) {
        return true;
      }
      if (finished) {
        return false;
      }

      pageIndex += pageSize;
      queryNext(pageIndex);
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

    private void queryNext(int index) {
      try {
        response = catalog.query(filter.apply(index));
      } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
        throw new CatalogCommandRuntimeException(e);
      }
      List<Result> queryResults = response.getResults();
      this.results = queryResults.iterator();

      int size = queryResults.size();
      if (size == 0 || size < pageSize) {
        finished = true;
      }
    }

    private void ensureInitialized() throws CatalogCommandRuntimeException {
      if (response != null || results != null) {
        return;
      }
      queryNext(pageIndex);
    }
  }
}
