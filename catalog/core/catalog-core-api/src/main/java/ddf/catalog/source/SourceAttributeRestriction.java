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

import java.util.Set;

/**
 * Any {@link Source} can optionally implement this interface to provide a set of attributes that it
 * supports querying on.
 *
 * <p>No guarantees are made of source behavior when it is queried using attributes other than those
 * specified here. It's possible that queries to a source containing attributes not in this set will
 * either be modified to maintain stability, lock up the source for undefined periods of time, or
 * fail outright. Not implementing this interface, or implementing but returning an empty set,
 * implies the source either:
 *
 * <ol>
 *   <li>Supports any attribute available to the system, or
 *   <li>can gracefully handle or safely ignore undesireable attributes in a query without
 *       introducing usability problems such as degraded service or contradictions in the result
 *       set.
 * </ol>
 *
 * <p><b>This interface should only be implemented by sources.</b> Information returned from this
 * interface will be exposed over the network to support validation scenarios prior to submitting a
 * query.
 */
public interface SourceAttributeRestriction {

  /**
   * Get the source that this supported attribute definition describes.
   *
   * @return the source defining these supported attributes. Will not be null.
   */
  Source getSource();

  /**
   * Get a list of attribute names that the source supports.
   *
   * @return a set of attribute names that can safely be used to query the source. Will not be null.
   */
  Set<String> getSupportedAttributes();
}
