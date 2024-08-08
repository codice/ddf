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
package ddf.catalog.data;

import java.util.Set;

/** Manages required attribute values, which correspond to specific metacard types. */
public interface RequiredAttributesRegistry {

  /**
   * Checks if the given attribute is required for the given metacard type.
   *
   * @param metacardTypeName the name of the metacard type, cannot be null
   * @param attributeName the name of the attribute belonging to the metacard type, cannot be null
   * @return a boolean denoting if the attribute is required for the given {@param
   *     metacardTypeName}.
   * @throws IllegalArgumentException if either argument is null
   */
  boolean isRequired(String metacardTypeName, String attributeName);

  /**
   * Checks if the given attribute is required for the given metacard type.
   *
   * @param metacardTypeName the name of the metacard type, cannot be null
   * @return a set of strings which contains all required attributes for the given {@param
   *     metacardTypeName}.
   * @throws IllegalArgumentException if {@code metacardTypeName} is null
   */
  Set<String> getRequiredAttributes(String metacardTypeName);
}
