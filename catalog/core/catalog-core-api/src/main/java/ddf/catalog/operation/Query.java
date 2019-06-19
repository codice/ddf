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
package ddf.catalog.operation;

import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * A {@link Query} contains the basic aspects of executing a search. A {@link Query} extends {@link
 * Filter} to provide access to the underlying tree of criteria, selection, sorting, and
 * projections.
 */
public interface Query extends Filter {

  public static final int DEFAULT_MAXIMUM_RETURNED_RESULTS = 200;

  /**
   * Get the offset where the query results will begin.
   *
   * <p>Start index is required to be 1-based. For example if the query specifies a start index of 5
   * then the query results will start with the 5th result discovered by the query.
   *
   * <p>If a value less than 1 is returned, {@link ddf.catalog.source.Source} implementations will
   * throw {@link ddf.catalog.source.UnsupportedQueryException}.
   *
   * @return int - the start index for the query results
   */
  public int getStartIndex();

  /**
   * The page size represents the maximum amount of results the query will return. Page sizes of
   * less than 1 (0 or a negative number) should return the maximum number of results supported by
   * the catalog or the maximum supported by each {@link ddf.catalog.source.Source}, whichever is
   * smaller.
   *
   * @return the page size - the maximum result size
   */
  public int getPageSize();

  /**
   * The sortBy determines how the results will be sorted.
   *
   * @return {@link SortBy}. Null if no sortBy is specified.
   * @see SortBy
   */
  public SortBy getSortBy();

  /**
   * Determines whether the total number of results should be returned
   *
   * @return true, if the count should be returned
   * @return false, if the count should not be returned
   */
  public boolean requestsTotalResultsCount();

  /**
   * The timeout is specified in milliseconds. This will cause the query to timeout and return
   * results by the specified timeout, if the query has not done so already. <br>
   * Return 0 if no timeout should occur.
   *
   * @return max time to wait for query results in milliseconds, 0 if no timeout should occur.
   */
  public long getTimeoutMillis();

  /**
   * Creates a new instance of the query with the same properties but a new filter
   *
   * @param newFilter Filter to be associated with the new query
   * @return the new query object
   */
  public Query newInstanceWithFilter(Filter newFilter);
}
