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
package org.codice.ddf.configuration.migration;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationReport;

/**
 * Service that provides a way to migrate configurations from one instance of DDF to another. This
 * includes exporting and importing of configurations.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface ConfigurationMigrationService {
  /**
   * Exports configurations to the specified path.
   *
   * @param exportDirectory path to export configurations to
   * @return a migration report for the export operation
   * @throws IllegalArgumentException if <code>exportDirectory</code> is <code>null</code>
   */
  MigrationReport doExport(Path exportDirectory);

  /**
   * Exports configurations to the specified path.
   *
   * <p><i>Note:</i> This version of the <code>doExport()</code> method will callback the provided
   * consumer every time a migration message is recorded. The message will also be recorded with the
   * report returned at the completion of the operation.
   *
   * @param exportDirectory path to export configurations to
   * @param consumer a consumer to call whenever a new migration message is recorded during the
   *     operation
   * @return a migration report for the export operation
   * @throws IllegalArgumentException if <code>exportDirectory</code> or <code>consumer</code> is
   *     <code>null</code>
   */
  MigrationReport doExport(Path exportDirectory, Consumer<MigrationMessage> consumer);

  /**
   * Imports configurations from the specified path.
   *
   * @param exportDirectory path to import configurations from
   * @return a migration report for the import operation
   * @throws IllegalArgumentException if <code>exportDirectory</code> is <code>null</code>
   */
  MigrationReport doImport(Path exportDirectory);

  /**
   * Imports configurations from the specified path.
   *
   * <p><i>Note:</i> This version of the <code>doImport()</code> method will callback the provided
   * consumer every time a migration message is recorded. The message will also be recorded with the
   * report returned at the completion of the operation.
   *
   * @param exportDirectory path to import configurations from
   * @param consumer a consumer to call whenever a new migration message is recorded during the
   *     operation
   * @return a migration report for the import operation
   * @throws IllegalArgumentException if <code>exportDirectory</code> or <code>consumer</code> is
   *     <code>null</code>
   */
  MigrationReport doImport(Path exportDirectory, Consumer<MigrationMessage> consumer);

  /**
   * Imports configurations from the specified path.
   *
   * <p><i>Note:</i> This version of the <code>doImport()</code> method provides a way to promote
   * optional migratables (see {@link org.codice.ddf.migration.OptionalMigratable}) as mandatory
   * such that they not be skipped during the import phase. It will callback the provided consumer
   * every time a migration message is recorded. The message will also be recorded with the report
   * returned at the completion of the operation.
   *
   * @param exportDirectory path to import configurations from
   * @param mandatoryMigratables a set of mandatory migratable identifiers
   * @param consumer a consumer to call whenever a new migration message is recorded during the
   *     operation
   * @return a migration report for the import operation
   * @throws IllegalArgumentException if <code>exportDirectory</code>, <code>mandatoryMigratables
   *     </code>, or <code>consumer</code> is <code>null</code>
   */
  MigrationReport doImport(
      Path exportDirectory, Set<String> mandatoryMigratables, Consumer<MigrationMessage> consumer);

  /**
   * Decrypts an exported file from the specified path.
   *
   * @param exportDirectory path to decrypt configurations from
   * @return a migration report for the decrypt operation
   * @throws IllegalArgumentException if <code>exportDirectory</code> is <code>null</code>
   */
  MigrationReport doDecrypt(Path exportDirectory);

  /**
   * Decrypts an exported file from the specified path.
   *
   * <p><i>Note:</i> This version of the <code>doDecrypt()</code> method will callback the provided
   * consumer every time a migration message is recorded. The message will also be recorded with the
   * report returned at the completion of the operation.
   *
   * @param exportDirectory path to decrypt configurations from
   * @param consumer a consumer to call whenever a new migration message is recorded during the
   *     operation
   * @return a migration report for the decrypt operation
   * @throws IllegalArgumentException if <code>exportDirectory</code> or <code>consumer</code> is
   *     <code>null</code>
   */
  MigrationReport doDecrypt(Path exportDirectory, Consumer<MigrationMessage> consumer);
}
