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

/** Result of performing filtering on a {@link Metacard} or {@link Response}. */
@Deprecated
public interface FilterResult {
  /**
   * Returns a filtered {@link Metacard}.
   *
   * @return {@link Metacard}
   */
  Metacard metacard();

  /**
   * Returns a filtered {@link Response}
   *
   * @return {@link Response}
   */
  Response response();

  /**
   * Returns true if this result was processed.
   *
   * @return true if this result was processed
   */
  boolean processed();
}
