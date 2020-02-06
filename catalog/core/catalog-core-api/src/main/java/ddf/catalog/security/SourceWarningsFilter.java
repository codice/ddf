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
package ddf.catalog.security;

import ddf.catalog.operation.ProcessingDetails;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface SourceWarningsFilter {

  /**
   * This method determines whether this {@code SourceWarningsFilter} can correctly {@link
   * #filter(ProcessingDetails) filter} the {@link ProcessingDetails} passed to it
   *
   * @param details the {@link ProcessingDetails} which this method examines as part of its
   *     determination of whether this {@code SourceWarningsFilter} can correctly {@link
   *     #filter(ProcessingDetails) filter} these details and not encounter an error or exception in
   *     the process
   * @return {@code true} if this {@code SourceWarningsFilter} can correctly {@link
   *     #filter(ProcessingDetails) filter} the details passed in; otherwise, {@code false}
   */
  boolean canFilter(ProcessingDetails details);

  /**
   * If this {@code SourceWarningsFilter} {@link #canFilter(ProcessingDetails) can filter} the
   * {@link ProcessingDetails} passed to it, then this method applies a filter to the {@code
   * warnings} of the {@link ProcessingDetails}
   *
   * @param details the {@link ProcessingDetails} which contain the {@code warnings} which this
   *     method filters
   * @return new {@link ProcessingDetails} which correspond to the details passed in except that the
   *     {@code warnings} of the new {@link ProcessingDetails} contain only the warnings of the
   *     original details which this method recognizes as useful to the user
   * @throws NullPointerException if {@link #canFilter(ProcessingDetails) canFilter(details)}
   *     determines that this {@code SourceWarningsFilter} cannot filter the details
   */
  ProcessingDetails filter(ProcessingDetails details);
}
