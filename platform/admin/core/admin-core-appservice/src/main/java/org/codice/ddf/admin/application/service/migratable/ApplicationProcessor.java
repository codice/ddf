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

import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.migration.MigrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class used to process applications by comparing the exported version with the memory one
 * and determining if it should be started.
 *
 * <p><i>Note:</i> Applications are only started and not stopped. This is because the application
 * service will will reported stopped as soon as at least one feature defined in the application is
 * not started. In cases where all the features are not started (i.e. one was specifically stopped),
 * the whole app would be reported as not started and if we end up stopping the app, we would stop
 * all the features that are supposed to be started.
 */
public class ApplicationProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationProcessor.class);

  private final ApplicationService service;

  /**
   * Constructs a new application processor.
   *
   * @param service the application service to use
   * @throws IllegalArgumentException if <code>service</code> is <code>null</code>
   */
  public ApplicationProcessor(ApplicationService service) {
    Validate.notNull(service, "invalid null application service");
    this.service = service;
  }

  /**
   * Gets an application from memory if available.
   *
   * @param report the report where to record errors if the application is not found
   * @param name the name of the application to retrieve
   * @return the corresponding application or <code>null</code> if it is not available
   */
  @Nullable
  public Application getApplication(ProfileMigrationReport report, String name) {
    final Application app = service.getApplication(name);

    if (app == null) {
      LOGGER.debug("application '{}' not installed", name);
      report.record(
          new MigrationException(
              "Import error: failed to start application [%s]; application is not installed.",
              name));
    }
    return app;
  }

  /**
   * Starts the specified application.
   *
   * @param report the report where to record errors if unable to start the application
   * @param app the application to start
   * @return <code>true</code> if the application was started successfully; <code>false</code>
   *     otherwise
   */
  @SuppressWarnings({"squid:S3516" /* not all paths return the same value */})
  public boolean startApplication(ProfileMigrationReport report, Application app) {
    final String name = app.getName();
    final String attempt = report.getApplicationAttemptString(Operation.START, name);

    LOGGER.debug("Starting application '{}'{}", name, attempt);
    report.record("Starting application [%s]%s.", name, attempt);
    try {
      service.startApplication(app);
    } catch (ApplicationServiceException e) {
      report.recordOnFinalAttempt(
          new MigrationException("Import error: failed to start application [%s]; %s.", name, e));
      return false;
    }
    return true;
  }

  /**
   * Processes applications by recording tasks to start applications that were originally started.
   *
   * @param report the report where to record errors
   * @param jprofile the profile where to retrieve the set of applications from the original system
   * @param tasks the task list where to record tasks to be executed
   * @return <code>false</code> if we failed to retrieve from memory an application that existed on
   *     the original system; <code>true</code> if everything was ok
   */
  public boolean processApplications(
      ProfileMigrationReport report, JsonProfile jprofile, TaskList tasks) {
    LOGGER.debug("Processing applications import");
    final Map<String, JsonApplication> jappsMap =
        jprofile
            .applications()
            .collect( // linked map to preserve order
                org.codice.ddf.admin.application.service.migratable.Collectors.toLinkedMap(
                    JsonApplication::getName, Function.identity()));

    processMemoryApplications(jappsMap, tasks);
    return processLeftoverExportedApplications(report, jappsMap, tasks);
  }

  /**
   * Processes applications in memory by recording tasks to start applications that were originally
   * started.
   *
   * <p><i>Note:</i> Any applications found in memory are removed from the provided map since they
   * have been processed.
   *
   * @param japps the set of apps on the original system
   * @param tasks the task list where to record tasks to be executed
   */
  @SuppressWarnings("squid:S3776" /* additional branches used only for tracing */)
  public void processMemoryApplications(Map<String, JsonApplication> japps, TaskList tasks) {
    LOGGER.debug("Processing applications defined in memory");
    for (final Application app : service.getApplications()) {
      final String name = app.getName();
      final JsonApplication japp = japps.remove(name); // remove from japps when we find it

      processApplication(japp, app, tasks);
    }
  }

  /**
   * Processes applications that were left over after having dealt with what is in memory. The
   * implementation will try to find the missing applications (although unlikely to be found). If
   * found, they will be processed by recording tasks to start them if they were originally started.
   *
   * @param report the report where to record errors
   * @param japps the set of apps on the original system that were not yet processed from memory
   * @param tasks the task list where to record tasks to be executed
   * @return <code>false</code> if we failed to retrieve from memory a left over application; <code>
   *     true</code> if everything was ok
   */
  public boolean processLeftoverExportedApplications(
      ProfileMigrationReport report, Map<String, JsonApplication> japps, TaskList tasks) {
    LOGGER.debug("Processing leftover exported applications");
    boolean allProcessed = true; // until proven otherwise

    // check if there are anything left from the exported info that should be started or that is
    // not installed
    for (final JsonApplication japp : japps.values()) {
      final String name = japp.getName();

      if (japp.isStarted()) {
        final Application app = getApplication(report, name);

        if (app == null) { // app not available!!!
          allProcessed = false;
        } else {
          processApplication(japp, app, tasks);
        }
      } else { // we shall let the features and bundles handle the low-level stuff
        LOGGER.debug("Skipping application '{}'; not installed", name);
      }
    }
    return allProcessed;
  }

  /**
   * Processes the specified application by comparing its state in memory to the one from the
   * original system and determining if it needs to be started.
   *
   * @param japp the original application information or <code>null</code> if it was not installed
   * @param app the current application from memory
   * @param tasks the task list where to record tasks to be executed
   */
  public void processApplication(@Nullable JsonApplication japp, Application app, TaskList tasks) {
    final String name = app.getName();

    if (japp != null) {
      if (japp.isStarted()) {
        final boolean started = service.isApplicationStarted(app);

        if (!processStartedApplication(app, name, started, tasks)) {
          LOGGER.debug("Skipping application '{}'; already started", name);
        }
      } else {
        // we shall let the features and bundles handle the low-level stopping
        LOGGER.debug("Skipping application '{}'; was not started", name);
      }
    } else { // we shall let the features and bundles handle the low-level stopping
      LOGGER.debug("Skipping application '{}'; was not installed", name);
    }
  }

  /**
   * Processes the specified application for startup if the applciation in memory is not started.
   *
   * @param app the current application from memory
   * @param name the application name
   * @param started <code>true</code> if the application in memory is started; <code>false</code>
   *     otherwise
   * @param tasks the task list where to record tasks to be executed
   * @return <code>true</code> if processed; <code>false</code> otherwise
   */
  public boolean processStartedApplication(
      Application app, String name, boolean started, TaskList tasks) {
    if (!started) {
      tasks.add(Operation.START, name, r -> startApplication(r, app));
      return true;
    }
    return false;
  }
}
