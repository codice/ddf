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
package org.codice.ddf.catalog.ui.alias;

import com.google.common.collect.ImmutableMap;

/**
 * A collection of mappings between internal attribute names to their user-friendly aliases,
 * typically used for display in the UI.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface AttributeAliases {

  /**
   * Checks if the internal attribute {@code attributeName} has a configured alias.
   *
   * @param attributeName - the internal name of the attribute
   * @return true if {@code attributeName} has a configured alias, false otherwise.
   */
  boolean hasAlias(String attributeName);

  /**
   * Gets the configured alias for the internal attribute {@code attributeName}. If no alias is
   * configured, {@code null} is returned.
   *
   * @param attributeName - the internal name of the attribute
   * @return the alias, if configured, null otherwise.
   */
  String getAlias(String attributeName);

  /**
   * Gets a read-only reference to all the alias mappings. This map will never be {@code null}.
   *
   * @return an immutable map containing all internal attribute to alias mappings.
   */
  ImmutableMap<String, String> getAliasMap();
}
