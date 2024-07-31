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

/**
 * Manages default attribute values that apply to all metacard types and default attribute values
 * that apply to specific metacard types.
 *
 * <p>Default attribute values are separated into two categories:
 *
 * <ol>
 *   <li>'Global' default values (default values that are tied to an attribute but not to a specific
 *       metacard type)
 *   <li>Default values that apply to specific metacard types
 * </ol>
 *
 * In the event that an attribute has a 'global' default value as well as a default value for a
 * specific metacard type, the default value for the specific metacard type will be returned (i.e.,
 * the more specific default value wins).
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface RequiredAttributesRegistry {

  /**
   * Registers the given attribute as required for the given metacard type.
   *
   * @param metacardTypeName the name of the metacard type, cannot be null
   * @param attributeName the name of the attribute belonging to the metacard type, cannot be null
   * @throws IllegalArgumentException if any arguments are null
   */
  void addRequiredAttribute(String metacardTypeName, String attributeName);

  /**
   * Registers the given set of attributes as required for the given metacard type.
   *
   * @param metacardTypeName the name of the metacard type, cannot be null
   * @param attributeName the name of the attribute belonging to the metacard type, cannot be null
   * @throws IllegalArgumentException if any arguments are null
   */
  void addRequiredAttributes(String metacardTypeName, Set<String> attributeName);

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
   * @throws IllegalArgumentException if either argument is null
   */
  Set<String> getRequiredAttributes(String metacardTypeName);

  /**
   * Removes the required attribute for the given metacard type.
   *
   * @param metacardTypeName the name of the metacard type, cannot be null
   * @param attributeName the name of the required attribute to be removed, cannot be null
   * @throws IllegalArgumentException if either argument is null
   */
  void removeRequiredAttribute(String metacardTypeName, String attributeName);

  /**
   * Removes all required attributes for the given metacard type.
   *
   * @param metacardTypeName the name of the metacard type whose required attributes will be
   *     removed, cannot be null
   * @throws IllegalArgumentException if {@code metacardTypeName} is null
   */
  void removeRequiredAttributes(String metacardTypeName);
}
