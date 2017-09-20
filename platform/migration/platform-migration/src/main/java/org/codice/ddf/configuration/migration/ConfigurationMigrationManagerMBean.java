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

import java.util.Collection;
import org.codice.ddf.migration.MigrationWarning;

/** Interface to expose {@link ConfigurationMigrationManager} as an MBean. */
public interface ConfigurationMigrationManagerMBean {
  /**
   * Exports configurations to specified path
   *
   * @param exportDirectory path to export configurations
   * @return a collection of {@link MigrationWarning} returned if there were non-fatal issues when
   *     exporting
   * @throws org.codice.ddf.migration.MigrationException thrown if one or more configurations
   *     couldn't be exported
   * @throws IllegalArgumentException if <code>exportDirectory</code> is <code>null</code>
   */
  Collection<MigrationWarning> doExport(String exportDirectory);

  /**
   * Imports configurations from the specified path
   *
   * @param exportDirectory path to import configurations from
   * @return a collection of {@link MigrationWarning} returned if there were non-fatal issues when
   *     importing
   * @throws org.codice.ddf.migration.MigrationException thrown if one or more configurations
   *     couldn't be imported
   * @throws IllegalArgumentException if <code>exportDirectory</code> is <code>null</code>
   */
  Collection<MigrationWarning> doImport(String exportDirectory);
}
