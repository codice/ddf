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

import java.util.List;

/** This class is used to find all capabilities that correspond to a certain source. */
public interface SourceCapabilityRegistry {

  /**
   * Used to retrieve all capabilities that can be applied to a given {@link Source}.
   *
   * @param source - the {@link Source} in which a capability is accociated
   * @return {@link List<String>} containing capabilities that can be applied from the given source,
   *     otherwise an empty list if no capabilities can be applied.
   */
  public List<String> list(Source source);
}
