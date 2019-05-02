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
package org.codice.ddf.migration;

import org.codice.ddf.platform.services.common.Describable;

/**
 * This interface provides the mechanism for implementers to define how their data (e.g. bundle
 * specific Java properties, XML or JSON configuration files) shall be exported for later import
 * into a new system. The information exported will allow a new system to be automatically
 * configured (cloned) like the exported system. Only bundle or feature specific settings need be
 * handled. All configurations stored in OSGi's {@code ConfigurationAdmin} will automatically be
 * migrated by a migratable specifically developed for this purpose and do not need to be managed by
 * implementors of this class. The framework that handles the migratables ensures that no two
 * migratable's methods are running at the same time. Implementors do not need to program exports
 * and imports with regard to reflexive thread-safety.
 *
 * <p><b>This is the only interface that should be implemented by implementers and registered as an
 * OSGI service. All other interfaces will be implemented by the framework that provides support for
 * migratables.</b>
 *
 * <p>During an import operation, only one of {@link #doImport}, {@link #doVersionUpgradeImport}, or
 * {@link #doMissingImport} methods will be called by the framework.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface Migratable extends Describable {
  public static final String IMPORT_UNSUPPORTED_MIGRATABLE_VERSION_ERROR =
      "Import error: unsupported exported version [%s] for migratable [%s]; currently supporting [%s].";

  /**
   * Gets the current export version handled by this migratable.
   *
   * <p>When exporting data, the migration framework will export this version information and
   * provide it back to the {@link #doVersionUpgradeImport} method if the exported version string
   * doesn't match the one provided by this method when importing the data for this migratable.
   *
   * <p>The format of the version string is migratable-dependent.
   *
   * @return the current version handled by this migratable
   */
  @Override
  public String getVersion();

  /**
   * Exports all required migratable data to the specified context.
   *
   * <p>Errors, warnings, and/or information messages can be recorded along with the context's
   * report. Doing so will not abort the operation.
   *
   * @param context a migration context to export all migratable data to
   * @throws MigrationException to stop the export operation
   */
  public void doExport(ExportMigrationContext context);

  /**
   * Imports all exported migratable data provided by the specified context when the current version
   * of this migratable (see {@link #getVersion}) matches the exported version.
   *
   * <p>Errors, warnings, and/or information messages can be recorded along with the context's
   * report. Doing so will not abort the operation.
   *
   * @param context a migration context to import all exported migratable data from
   * @throws MigrationException to stop the import operation
   */
  public void doImport(ImportMigrationContext context);

  /**
   * Imports all exported migratable data provided by the specified context when the current version
   * of this migratable (see {@link #getVersion}) is different than the exported version.
   *
   * <p>Errors, warnings, and/or information messages can be recorded along with the context's
   * report. Doing so will not abort the operation.
   *
   * <p><i>Note:</i> The default implementation provided here will record an incompatibility error
   * with the context's report.
   *
   * @param context a migration context to import all exported migratable data from
   * @param migratableVersion the exported version for the data to re-import
   * @throws MigrationException to stop the import operation
   */
  public default void doVersionUpgradeImport(
      ImportMigrationContext context, String migratableVersion) {
    context
        .getReport()
        .record(
            new MigrationException(
                IMPORT_UNSUPPORTED_MIGRATABLE_VERSION_ERROR,
                migratableVersion,
                getId(),
                getVersion()));
  }

  /**
   * Called when data for this migratable was not exported. This would happen when migrating to a
   * newer version of the product where this migratable is first being introduced.
   *
   * <p>Errors, warnings, and/or information messages can be recorded along with the context's
   * report. Doing so will not abort the operation.
   *
   * <p><i>Note:</i> The default implementation provided here will record an incompatibility error
   * with the context's report.
   *
   * @param context a migration context to import all exported migratable data from
   * @throws MigrationException to stop the import operation
   */
  public default void doMissingImport(ImportMigrationContext context) {
    context
        .getReport()
        .record(
            new MigrationException(
                "Incompatibility error: missing exported data for migratable [%s]; currently supporting [%s].",
                getId(), getVersion()));
  }
}
