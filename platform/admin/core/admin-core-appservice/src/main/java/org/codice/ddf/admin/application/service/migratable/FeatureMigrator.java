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
import java.util.stream.Stream;
import org.apache.commons.lang.Validate;
import org.apache.karaf.features.FeaturesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility classes capable of managing features during export or import. */
public class FeatureMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureMigrator.class);

  private final FeaturesService service;

  private final FeatureProcessor processor;

  /**
   * Constructs a new feature migrator.
   *
   * @param service the features service to use
   * @param processor the feature processor to use
   * @throws IllegalArgumentException if <code>service</code> or <code>processor</code> is <code>
   *     null</code>
   */
  public FeatureMigrator(FeaturesService service, FeatureProcessor processor) {
    Validate.notNull(service, "invalid null features service");
    Validate.notNull(processor, "invalid null feature processor");
    this.service = service;
    this.processor = processor;
  }

  /**
   * Exports all features in memory into their corresponding Json format.
   *
   * @return a list of the features to export
   */
  public List<JsonFeature> exportFeatures() {
    return Stream.of(processor.listFeatures("Export"))
        .map(f -> new JsonFeature(f, service))
        .collect(Collectors.toList()); // preserve order
  }

  /**
   * Import the specified features.
   *
   * <p>The implementation will loop until it fails to find a feature in memory or it fails to
   * change the state of a feature or again until the state of the features in memory matches the
   * state of the features on the original system.
   *
   * @param report the report where to record errors
   * @param jprofile the profile from which to import exported features
   * @return <code>true</code> if all were imported successfully; <code>false</code> otherwise
   */
  public boolean importFeatures(ProfileMigrationReport report, JsonProfile jprofile) {
    final TaskList tasks = newTaskList(report);

    // loop until we can determine that all features that should be started. stopped, installed, or
    // uninstalled are or until we get an error or exceeds the max number of attempts
    while (true) {
      processor.processFeaturesAndPopulateTaskList(jprofile, tasks);
      if (tasks.isEmpty()) {
        LOGGER.debug("No (or no more) features to import");
        return true;
      } else if (!tasks.execute()) {
        LOGGER.debug("Failed to execute some feature import tasks");
        // errors would already have been recorded
        return false;
      }
    }
  }

  @VisibleForTesting
  TaskList newTaskList(ProfileMigrationReport report) {
    return new TaskList("feature", report);
  }
}
