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
package ddf.catalog.util.impl;

import static com.google.common.collect.Iterators.limit;
import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notNull;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;

/**
 * Class used to iterate over the {@link Result} objects contained in a {@link
 * ddf.catalog.operation.QueryResponse} returned when executing a {@link QueryRequest}. The class
 * will fetch new results as needed until all results that match the query provided have been
 * exhausted.
 *
 * <p>Since the class may use the page size provided in the {@link Query} to fetch the results, its
 * value should be carefully set to avoid any memory or performance issues.
 */
public class ResultIterable implements Iterable<Result> {
  public static final int DEFAULT_PAGE_SIZE = 64;

  private final QueryFunction queryFunction;

  private final QueryRequest queryRequest;

  private final int maxResultCount;

  private ResultIterable(
      CatalogFramework catalogFramework, QueryRequest queryRequest, int maxResultCount) {
    this(catalogFramework::query, queryRequest, maxResultCount);
  }

  private ResultIterable(
      QueryFunction queryFunction, QueryRequest queryRequest, int maxResultCount) {
    notNull(queryFunction, "Query function cannot be null");
    notNull(queryRequest, "Query request cannot be null");
    isTrue(maxResultCount >= 0, "Max Results cannot be negative", maxResultCount);

    this.queryFunction = queryFunction;
    this.queryRequest = queryRequest;
    this.maxResultCount = maxResultCount;
  }

  /**
   * Creates an iterable that will call the {@link CatalogFramework} to retrieve the results that
   * match the {@link QueryRequest} provided. There will be no limit to the number of results
   * returned.
   *
   * @param catalogFramework reference to the {@link CatalogFramework} to call to retrieve the
   *     results.
   * @param queryRequest request used to retrieve the results.
   */
  public static ResultIterable resultIterable(
      CatalogFramework catalogFramework, QueryRequest queryRequest) {
    notNull(catalogFramework, "CatalogFramework cannot be null");
    return new ResultIterable(catalogFramework, queryRequest, 0);
  }

  /**
   * Creates an iterable that will call a {@link QueryFunction} to retrieve the results that match
   * the {@link QueryRequest} provided. There will be no limit to the number of results returned.
   *
   * @param queryFunction reference to the {@link QueryFunction} to call to retrieve the results.
   * @param queryRequest request used to retrieve the results.
   */
  public static ResultIterable resultIterable(
      QueryFunction queryFunction, QueryRequest queryRequest) {
    return new ResultIterable(queryFunction, queryRequest, 0);
  }

  /**
   * Creates an iterable that will call the {@link CatalogFramework} to retrieve the results that
   * match the {@link QueryRequest} provided.
   *
   * @param catalogFramework reference to the {@link CatalogFramework} to call to retrieve the
   *     results.
   * @param queryRequest request used to retrieve the results.
   * @param maxResultCount a positive integer indicating the maximum number of results in total to
   *     query for
   */
  public static ResultIterable resultIterable(
      CatalogFramework catalogFramework, QueryRequest queryRequest, int maxResultCount) {
    notNull(catalogFramework, "CatalogFramework cannot be null");
    isTrue(maxResultCount > 0, "Max Results must be a positive integer", maxResultCount);
    return new ResultIterable(catalogFramework, queryRequest, maxResultCount);
  }

  /**
   * Creates an iterable that will call a {@link QueryFunction} to retrieve the results that match
   * the {@link QueryRequest} provided. If the query request does not include a sort-by, then the
   * code will default to sorting by the metacard ID in descending order.
   *
   * @param queryFunction reference to the {@link QueryFunction} to call to retrieve the results.
   * @param queryRequest request used to retrieve the results.
   * @param maxResultCount a positive integer indicating the maximum number of results in total to
   *     query for
   */
  public static ResultIterable resultIterable(
      QueryFunction queryFunction, QueryRequest queryRequest, int maxResultCount) {
    isTrue(maxResultCount > 0, "Max Results must be a positive integer", maxResultCount);
    return new ResultIterable(queryFunction, queryRequest, maxResultCount);
  }

  private static Stream<Result> stream(Iterator<Result> iterator) {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
  }

  @Override
  public Iterator<Result> iterator() {
    if (maxResultCount > 0) {
      return limit(new ResultIterator(queryFunction, queryRequest), maxResultCount);
    }
    return new ResultIterator(queryFunction, queryRequest);
  }

  public Stream<Result> stream() {
    return stream(iterator());
  }

  private static class ResultIterator implements Iterator<Result> {

    private final QueryFunction queryFunction;
    private final Set<String> foundIds = new HashSet<>(2048);
    private int currentIndex;
    private QueryImpl queryCopy;
    private QueryRequestImpl queryRequestCopy;
    private Iterator<Result> results = Collections.emptyIterator();
    private boolean finished = false;

    ResultIterator(QueryFunction queryFunction, QueryRequest queryRequest) {
      this.queryFunction = queryFunction;

      copyQueryRequestAndQuery(queryRequest);

      this.currentIndex = queryCopy.getStartIndex();
    }

    @Override
    public boolean hasNext() {
      if (results.hasNext()) {
        return true;
      }

      if (finished) {
        return false;
      }

      fetchNextResults();

      // Recurse to ensure we continue querying even if we get 1 or more completely filtered pages.
      return hasNext();
    }

    @Override
    public Result next() {
      if (results.hasNext()) {
        return results.next();
      }

      if (finished) {
        throw new NoSuchElementException("No more results match the specified query");
      }

      fetchNextResults();

      if (!results.hasNext()) {
        throw new NoSuchElementException("No more results match the specified query");
      }

      return results.next();
    }

    @SuppressWarnings("squid:CommentedOutCodeLine")
    private void fetchNextResults() {
      queryCopy.setStartIndex(currentIndex);

      try {
        SourceResponse response = queryFunction.query(queryRequestCopy);

        final List<Result> resultList = response.getResults();

        // Because some of the results may be filtered out by the catalog framework's
        // plugins, we need a way to know the actual page size and increment currentIndex based
        // on that number instead of using the result list size.
        // If the property is not present, we will have no option but to fallback to the size
        // of the (potentially filtered) resultList.
        //
        // This means that if the filtered results size is zero, but the raw number of results
        // had been greater than zero, we will not find results beyond the filtered gap. In practice
        // this should not happen, as queries will run through the QueryOperations.query() method;
        // however, should a user ever construct a QueryFunction that does NOT rely on that method,
        // there is no guarantee that this property will be properly set.
        int actualResultSize =
            Optional.ofNullable(response.getProperties())
                .map(m -> m.get("actualResultSize"))
                .filter(Integer.class::isInstance)
                .map(Integer.class::cast)
                .orElse(resultList.size());

        if (actualResultSize == 0) {
          finished = true;
          return;
        }
        currentIndex += actualResultSize;

        List<Result> dedupedResults = new ArrayList<>(resultList.size());
        for (Result result : resultList) {
          if (isDistinctResult(result)) {
            dedupedResults.add(result);
          }
          Optional.ofNullable(result)
              .map(Result::getMetacard)
              .map(Metacard::getId)
              .ifPresent(foundIds::add);
        }

        this.results = dedupedResults.iterator();

        if (response.getHits() >= 0 && currentIndex > response.getHits()) {
          finished = true;
        }
      } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
        throw new CatalogQueryException(e);
      }
    }

    private boolean isDistinctResult(@Nullable Result result) {
      return result != null
          && (result.getMetacard() == null
              || result.getMetacard().getId() == null
              || !foundIds.contains(result.getMetacard().getId()));
    }

    private void copyQueryRequestAndQuery(QueryRequest queryRequest) {
      Query query = queryRequest.getQuery();

      int pageSize = query.getPageSize() > 1 ? query.getPageSize() : DEFAULT_PAGE_SIZE;

      SortBy sortBy = query.getSortBy();
      if (sortBy == null) {
        sortBy = new SortByImpl(Core.ID, SortOrder.DESCENDING);
      }

      this.queryCopy =
          new QueryImpl(
              query,
              query.getStartIndex(),
              pageSize,
              sortBy,
              true,
              // always get the hit count
              query.getTimeoutMillis());

      this.queryRequestCopy =
          new QueryRequestImpl(
              queryCopy,
              queryRequest.isEnterprise(),
              queryRequest.getSourceIds(),
              queryRequest.getProperties());
    }
  }
}
