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
import java.util.Set;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface SourceWarningsFilter {

  /**
   * Get the identifier of this {@code SourceWarningsFilter}
   *
   * @return either the identifier, or, if this {@code SourceWarningsFilter} has no identifier, the
   *     empty {@link String}
   */
  String getId();

  /**
   * Determine whether this {@code SourceWarningsFilter} can correctly filter out unreadable or
   * useless {@code warnings} contained in the {@link ProcessingDetails} passed to it. If it can,
   * filter the {@link ProcessingDetails}'s {@code warnings} and create a {@link Set} composed
   * exclusively of the {@code warnings} which pass through the filter.
   *
   * @param details the {@link ProcessingDetails} which contain the {@code warnings} which this
   *     method filters
   * @return either a {@link Set} composed exclusively of the parameter's filtered {@code warnings},
   *     or, if no {@code warnings} pass through the filter, the empty {@link Set}
   */
  Set<String> filter(ProcessingDetails details);
}
