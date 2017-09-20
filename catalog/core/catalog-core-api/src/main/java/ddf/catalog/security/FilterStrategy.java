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

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.Response;

/** A strategy for filtering a Metacard. */
public interface FilterStrategy {
  /**
   * Returns a {@link FilterResult} with a processed or unprocessed {@link Metacard} or {@link
   * Response}
   *
   * @param response {@link Response} to filter
   * @param metacard {@link Metacard} to filter
   * @return
   */
  FilterResult process(Response response, Metacard metacard);
}
