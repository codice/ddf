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
package ddf.catalog.source;

import ddf.catalog.data.ContentType;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.util.Describable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The Source is used to represent a system that has Metacards in a catalog. A Source may be remote
 * or local. A local source may be a {@link CatalogProvider} or {@link ConnectedSource}. A remote
 * source is typically a {@link FederatedSource}, but can be a {@link ConnectedSource}.
 */
public interface Source extends Describable {

  /**
   * This method states whether this source is available, typically connecting and performing some
   * sort of simple query or ping to the native catalog.
   *
   * <p><b>This is expected to be an expensive operation, possibly involving network I/O.</b>
   * Typically only {@link ddf.catalog.CatalogFramework} implementations will call this and only
   * periodically.
   *
   * @return true - if the site is available (up), false - if the site is unavailable (down)
   * @see ddf.catalog.util.impl.SourcePoller
   */
  public boolean isAvailable();

  /**
   * This method is the same as {@link #isAvailable()} but allows a caller to provide a {@link
   * SourceMonitor} callback object which is meant to be used as a way for the {@link Source} to
   * dynamically contact the caller with its availability. The intent is to return a boolean as to
   * whether the {@link Source} is available at this very moment and also to use the {@link
   * SourceMonitor} object to update the caller of this method if this Source's availability changes
   * later in the future.
   *
   * @param callback - used to notify the caller of this method when the {@link Source} object wants
   *     to update its availability.
   * @return true - if the site is available (up), false - if the site is unavailable (down)
   */
  public boolean isAvailable(SourceMonitor callback);

  /**
   * @param request the query to execute
   * @return a {@link SourceResponse} with query results and query response details
   * @throws UnsupportedQueryException when the query is not understood, malformed, or not supported
   *     by a {@link Source}
   */
  public SourceResponse query(QueryRequest request) throws UnsupportedQueryException;

  /**
   * Gets the {@link ContentType}s that are currently stored by the {@link Source}. Notice the
   * return object is a {@link Set}, meaning it returns all unique content types found in the Source
   * without duplications.
   *
   * @return a {@link Set} of {@link ContentType}s currently available from this {@link Source}.
   */
  public Set<ContentType> getContentTypes();

  /**
   * Gets the map of security attributes associated with this source
   *
   * @return a map of security attributes
   */
  default Map<String, Set<String>> getSecurityAttributes() {
    return new HashMap<>();
  }
}
