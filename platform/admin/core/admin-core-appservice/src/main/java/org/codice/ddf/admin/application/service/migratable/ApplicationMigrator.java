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
package org.codice.ddf.admin.application.service.migratable;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang.Validate;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class capable of managing applications during export or import. */
public class ApplicationMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationMigrator.class);

  private final ApplicationService service;

  private final ApplicationProcessor processor;

  /**
   * Constructs a new application migrator.
   *
   * @param service the application service to use
   * @param processor the application migrator to use
   * @throws IllegalArgumentException if <code>service</code> or <code>migrator</code> is <code>null
   *     </code>
   */
  public ApplicationMigrator(ApplicationService service, ApplicationProcessor processor) {
    Validate.notNull(service, "invalid null application service");
    Validate.notNull(service, "invalid null application processor");
    this.service = service;
    this.processor = processor;
  }

  /**
   * Exports all applications in memory into their corresponding Json format.
   *
   * @return a list of the Json applications to export
   */
  public List<JsonApplication> exportApplications() {
    return service
        .getApplications()
        .stream()
        .map(a -> new JsonApplication(a, service))
        .collect(Collectors.toList()); // preserve order
  }

  /**
   * Import the specified applications.
   *
   * <p>The implementation will loop until it fails to find an application in memory or it fails to
   * start an application or again until the state of the applications in memory matches the state
   * of the applications on the original system.
   *
   * @param report the report where to record errors
   * @param jprofile the profile from which to import exported applications
   * @return <code>true</code> if all were imported successfully; <code>false</code> otherwise
   */
  public boolean importApplications(ProfileMigrationReport report, JsonProfile jprofile) {
    final TaskList tasks = newTaskList(report);

    // loop until we can determine that all apps that should be started are or until we get an error
    while (true) {
      // populate the task list
      if (!processor.processApplications(report, jprofile, tasks)) {
        // missing installed apps; bail - no point in continuing
        // errors would already have been recorded
        return false;
      } else if (tasks.isEmpty()) { // nothing to do
        LOGGER.debug("No (or no more) applications to import");
        return true;
      } else if (!tasks.execute()) {
        LOGGER.debug("Failed to execute some application import tasks");
        // errors would already have been recorded
        return false;
      }
    }
  }

  @VisibleForTesting
  TaskList newTaskList(ProfileMigrationReport report) {
    return new TaskList("application", report);
  }
}
