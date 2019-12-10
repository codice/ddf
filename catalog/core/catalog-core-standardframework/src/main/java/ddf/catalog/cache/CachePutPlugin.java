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
package ddf.catalog.cache;

import ddf.catalog.data.Metacard;
import java.util.Optional;

public interface CachePutPlugin {

  /**
   * Modify (optional) a metacard prior to being put into the cache. Implementations are not
   * required to return the same metacard instance. Return {@link Optional#empty()} if the metacard
   * should not be put into the cache.
   *
   * @param metacard the metacard to be modified
   * @return the modifed metacard or Optional.empty()
   */
  Optional<Metacard> process(Metacard metacard);
}
