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
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class capable of managing bundles during export or import. */
public class BundleMigrator {
  private static final Logger LOGGER = LoggerFactory.getLogger(BundleMigrator.class);

  private final BundleProcessor processor;

  /**
   * Constructs a new bundle migrator.
   *
   * @param processor the bundle processor to use
   * @throws IllegalArgumentException if <code>processor</code> is <code>null</code>
   */
  public BundleMigrator(BundleProcessor processor) {
    Validate.notNull(processor, "invalid null bundle processor");
    this.processor = processor;
  }

  /**
   * Exports all bundles in memory into their corresponding Json format.
   *
   * @return a list of the Json bundles to export
   */
  public List<JsonBundle> exportBundles() {
    return Stream.of(processor.listBundles(getBundleContext()))
        .map(JsonBundle::new)
        .collect(Collectors.toList()); // preserve order
  }

  /**
   * Import the specified bundles.
   *
   * <p>The implementation will loop until it fails to find a bundle in memory or it fails to change
   * the state of a bundle or again until the state of the bundles in memory matches the state of
   * the bundles on the original system.
   *
   * @param report the report where to record errors
   * @param jprofile the profile from which to import exported bundles
   * @return <code>true</code> if all were imported successfully; <code>false</code> otherwise
   */
  public boolean importBundles(ProfileMigrationReport report, JsonProfile jprofile) {
    final BundleContext context = getBundleContext();
    final TaskList tasks = newTaskList(report);

    // loop until we can determine that all bundles that should be started. stopped, installed, or
    // uninstalled are or until we get an error or exceeds the max number of attempts
    while (true) {
      processor.processBundlesAndPopulateTaskList(context, jprofile, tasks);
      if (tasks.isEmpty()) {
        LOGGER.debug("No (or no more) bundles to import");
        return true;
      } else if (!tasks.execute()) {
        LOGGER.debug("Failed to execute some bundle import tasks");
        // errors would already have been recorded
        return false;
      }
    }
  }

  @VisibleForTesting
  TaskList newTaskList(ProfileMigrationReport report) {
    return new TaskList("bundle", report);
  }

  @VisibleForTesting
  BundleContext getBundleContext() {
    return FrameworkUtil.getBundle(BundleMigrator.class).getBundleContext();
  }
}
