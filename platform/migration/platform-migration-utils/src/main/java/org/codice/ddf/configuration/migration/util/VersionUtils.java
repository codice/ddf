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
package org.codice.ddf.configuration.migration.util;

import static java.lang.Float.parseFloat;
import static org.codice.ddf.migration.Migratable.IMPORT_UNSUPPORTED_MIGRATABLE_VERSION_ERROR;

import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.MigrationException;

/** Utility class to help with migratable version checking */
public class VersionUtils {

  /** Prevents instantiation. */
  private VersionUtils() {}

  /**
   * Will compare the exported migratable version with this system's migratable version.
   *
   * @param context The import migration context
   * @param exportedVersion the migratable version from the export
   * @param currentVersion the migratable version for this system
   * @param migratableId the migratable id
   * @return <code>false</code> if the exported version is greater than the current version or if
   *     either of the versions cannot be parsed to a float. Otherwise, return <code>true</code>.
   */
  public static boolean isValidMigratableVersion(
      ImportMigrationContext context,
      String exportedVersion,
      String currentVersion,
      String migratableId) {
    boolean isValidMigratableVersion;
    try {
      isValidMigratableVersion = parseFloat(exportedVersion) <= parseFloat(currentVersion);
    } catch (NumberFormatException e) {
      isValidMigratableVersion = false;
    }

    if (!isValidMigratableVersion) {
      context
          .getReport()
          .record(
              new MigrationException(
                  IMPORT_UNSUPPORTED_MIGRATABLE_VERSION_ERROR,
                  exportedVersion,
                  migratableId,
                  currentVersion));
    }

    return isValidMigratableVersion;
  }
}
