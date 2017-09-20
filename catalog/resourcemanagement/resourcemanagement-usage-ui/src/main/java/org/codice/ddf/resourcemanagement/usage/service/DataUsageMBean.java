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
package org.codice.ddf.resourcemanagement.usage.service;

import java.util.List;
import java.util.Map;

public interface DataUsageMBean {
  /**
   * Queries the {@link org.codice.ddf.persistence.attributes.AttributesStore} for all known Users
   * and their Data Usages
   *
   * @return a map for each user in the Attributes Store
   */
  Map<String, List<Long>> userMap();

  /**
   * Updates the user properties for users that are known in the {@link
   * org.codice.ddf.persistence.attributes.AttributesStore}
   *
   * @param userMap a map from the front end for users
   */
  void updateUserDataLimit(Map<String, Long> userMap);

  /**
   * Updates the Cron Time with the given string
   *
   * @param cronTime - a String representing a Cron Time
   */
  void updateCronTime(String cronTime);

  /**
   * Gets the current configured Cron Time.
   *
   * @return a String representing the current Cron Time
   */
  String cronTime();
}
