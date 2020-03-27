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
package org.codice.ddf.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PersistentStore {

  enum PersistenceType {
    METACARD_TYPE("metacard"),
    SAVED_QUERY_TYPE("saved_query"),
    NOTIFICATION_TYPE("notification"),
    ACTIVITY_TYPE("activity"),
    WORKSPACE_TYPE("workspace"),
    PREFERENCES_TYPE("preferences"),
    USER_ATTRIBUTE_TYPE("attributes"),
    SUBSCRIPTION_TYPE("subscriptions"),
    EVENT_SUBSCRIPTIONS_TYPE("event_subscriptions"),
    ALERT_TYPE("alerts"),
    DECANTER_TYPE("decanter"),
    RESULTS_TYPE("result_cache");
    private String type;

    PersistenceType(final String type) {
      this.type = type;
    }

    @Override
    public String toString() {
      return type;
    }
  }

  /**
   * Adds item of specified type with the specified properties.
   *
   * @param type A non-empty string identifying the type of item being added.
   * @param properties A map of properties making up the item. Property keys must have a suffix that
   *     identifies the type of value for the entry. The PersistentItem class should be used for
   *     creating this map.
   * @throws PersistenceException If the type is empty or there was an issue persisting the item.
   */
  void add(String type, Map<String, Object> properties) throws PersistenceException;

  /**
   * Adds a collection of items of specified type.
   *
   * @param type A non-empty string identifying the type of items being added.
   * @param items A list of map properties making up the items. Property keys must have a suffix
   *     that identifies the type of value for the entry. The PersistentItem class should be used
   *     for creating these maps.
   * @throws PersistenceException If the type is empty or there was an issue persisting the item.
   */
  void add(String type, Collection<Map<String, Object>> items) throws PersistenceException;

  /**
   * Get all of the items of the specified type.
   *
   * @param type
   * @return
   * @throws PersistenceException
   */
  List<Map<String, Object>> get(String type) throws PersistenceException;

  /**
   * Get items matching the ECQL query criteria.
   *
   * @param type
   * @param ecql
   * @return
   * @throws PersistenceException
   */
  List<Map<String, Object>> get(String type, String ecql) throws PersistenceException;

  /**
   * Get all items matching the ECQL query criteria.
   *
   * @param type A non-empty string identifying the type of item being retrieved.
   * @param ecql Query criteria.
   * @param startIndex Index to start query at.
   * @param pageSize Max number of results to return in single query.
   * @throws PersistenceException
   * @throws IllegalArgumentException if startIndex is less than 0 or if pageSize is greater than
   *     the max allowed.
   */
  List<Map<String, Object>> get(String type, String ecql, int startIndex, int pageSize)
      throws PersistenceException;

  /**
   * Delete items matching the ECQL query criteria.
   *
   * @param type
   * @param ecql
   * @return Count of the items deleted
   * @throws PersistenceException
   */
  int delete(String type, String ecql) throws PersistenceException;

  /**
   * @param type A non-empty string identifying the type of item being retrieved.
   * @param ecql Query criteria.
   * @param startIndex Index to start query at.
   * @param pageSize Max number of results to return in single query.
   * @return Count of the items deleted
   * @throws PersistenceException
   * @throws IllegalArgumentException if startIndex is less than 0 or if pageSize is greater than
   *     the max allowed.
   */
  int delete(String type, String ecql, int startIndex, int pageSize) throws PersistenceException;
}
