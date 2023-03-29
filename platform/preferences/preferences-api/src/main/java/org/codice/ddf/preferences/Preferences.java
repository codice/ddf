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
package org.codice.ddf.preferences;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Preferences {

  // TODO need a "patch" method so individual prefs can be updated

  /**
   * Adds item with the specified properties.
   *
   * @param properties A map of properties making up the item. Property keys must have a suffix that
   *     identifies the type of value for the entry. The PersistentItem class should be used for
   *     creating this map.
   * @throws PreferencesException If the type is empty or there was an issue persisting the item.
   */
  void add(Map<String, Object> properties) throws PreferencesException;

  /**
   * Adds a collection of items.
   *
   * @param items A list of map properties making up the items. Property keys must have a suffix
   *     that identifies the type of value for the entry. The PersistentItem class should be used
   *     for creating these maps.
   * @throws PreferencesException If the type is empty or there was an issue persisting the item.
   */
  void add(Collection<Map<String, Object>> items) throws PreferencesException;

  /**
   * Get all items.
   *
   * @return
   * @throws PreferencesException
   */
  List<Map<String, Object>> get() throws PreferencesException;

  /**
   * Get items matching the ECQL query criteria.
   *
   * @param ecql
   * @return
   * @throws PreferencesException
   */
  List<Map<String, Object>> get(String ecql) throws PreferencesException;

  /**
   * Get all items matching the ECQL query criteria.
   *
   * @param ecql Query criteria.
   * @param startIndex Index to start query at.
   * @param pageSize Max number of results to return in single query.
   * @throws PreferencesException
   * @throws IllegalArgumentException if startIndex is less than 0 or if pageSize is greater than
   *     the max allowed.
   */
  List<Map<String, Object>> get(String ecql, int startIndex, int pageSize)
      throws PreferencesException;

  /**
   * Delete items matching the ECQL query criteria.
   *
   * @param ecql
   * @return Count of the items deleted
   * @throws PreferencesException
   */
  int delete(String ecql) throws PreferencesException;

  /**
   * @param ecql Query criteria.
   * @param startIndex Index to start query at.
   * @param pageSize Max number of results to return in single query.
   * @return Count of the items deleted
   * @throws PreferencesException
   * @throws IllegalArgumentException if startIndex is less than 0 or if pageSize is greater than
   *     the max allowed.
   */
  int delete(String ecql, int startIndex, int pageSize) throws PreferencesException;
}
