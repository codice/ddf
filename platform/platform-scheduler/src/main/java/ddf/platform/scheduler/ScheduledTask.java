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
package ddf.platform.scheduler;

import java.util.Map;

/**
 * Interface used mostly for the Managed Service Factory that will create tasks on the fly and
 * expose them as Services.
 *
 * @author Ashraf Barakat
 */
public interface ScheduledTask {

  /** Creates and schedules new task */
  void newTask();

  /**
   * Removes a task completely so that it does not run or exist.
   *
   * @param code - not used
   * @see <a href="ARIES-1436">https://issues.apache.org/jira/browse/ARIES-1436</a>
   */
  void deleteTask(int code);

  /**
   * Updates an existing task with new properties.
   *
   * @param properties
   */
  void updateTask(Map<String, Object> properties);
}
