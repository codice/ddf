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
package org.codice.ddf.persistence.attributes;

import java.util.List;
import java.util.Map;
import org.codice.ddf.persistence.PersistenceException;

public interface AttributesStore {
  String DATA_USAGE_KEY = "data_usage";

  String DATA_USAGE_LIMIT_KEY = "data_limit";

  String USER_KEY = "user";

  /**
   * Returns the user's current data usage from the persistent store
   *
   * @param username
   * @return data usage
   * @throws PersistenceException
   */
  long getCurrentDataUsageByUser(String username) throws PersistenceException;

  /**
   * Returns the user's current data limit from the persistent store
   *
   * @param username
   * @return
   * @throws PersistenceException
   */
  long getDataLimitByUser(final String username) throws PersistenceException;

  /**
   * Adds the specified data usage in bytes to the user's data usage in the persistent store
   *
   * @param username
   * @param dataUsage
   * @throws PersistenceException
   */
  void updateUserDataUsage(String username, long dataUsage) throws PersistenceException;

  /**
   * Resets the user's data usage in the persistent store to the usage specified in bytes
   *
   * @param username
   * @param dataUsage
   * @throws PersistenceException
   */
  void setDataUsage(String username, long dataUsage) throws PersistenceException;

  /**
   * Resets the user's data limit in the persistent store to the size specified in bytes
   *
   * @param username
   * @param dataLimit
   * @throws PersistenceException
   */
  void setDataLimit(String username, long dataLimit) throws PersistenceException;

  /**
   * Gets a list of all users and their attributes
   *
   * @return a list of users and their data usage properties
   * @throws PersistenceException
   */
  List<Map<String, Object>> getAllUsers() throws PersistenceException;

  /**
   * Resets all known user's data usages to 0.
   *
   * @throws PersistenceException
   */
  void resetUserDataUsages() throws PersistenceException;
}
