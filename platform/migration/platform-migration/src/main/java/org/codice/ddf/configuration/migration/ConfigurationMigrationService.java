/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.configuration.migration;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.platform.services.common.Describable;

/**
 * Service that provides a way to migrate configurations from one instance of DDF to another.  This
 * includes exporting and importing of configurations.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public interface ConfigurationMigrationService {
    /**
     * Exports configurations to the specified path.
     *
     * @param exportDirectory path to export configurations to
     * @return a migration report for the export operation
     * @throws IllegalArgumentException if <code>exportDirectory</code> is <code>null</code>
     */
    default MigrationReport doExport(Path exportDirectory) throws MigrationException {
        return doExport(exportDirectory, Optional.empty());
    }

    /**
     * Exports configurations to the specified path.
     * <p>
     * <i>Note:</i> This version of the <code>doExport()</code> method will callback the provided
     * consumer every time a migration message is recorded. The message will also be recorded with
     * the report returned at the completion of the operation.
     *
     * @param exportDirectory path to export configurations to
     * @param consumer        an optional consumer to call whenever a new migration message is recorded
     *                        during the operation
     * @return a migration report for the export operation
     * @throws IllegalArgumentException if <code>exportDirectory</code> is <code>null</code>
     */
    MigrationReport doExport(Path exportDirectory, Optional<Consumer<MigrationMessage>> consumer)
            throws MigrationException;

    /**
     * Imports configurations from the specified path.
     *
     * @param exportDirectory path to import configurations from
     * @return a migration report for the import operation
     * @throws IllegalArgumentException if <code>exportDirectory</code> is <code>null</code>
     */
    default MigrationReport doImport(Path exportDirectory)  {
        return doImport(exportDirectory, Optional.empty());
    }

    /**
     * Imports configurations from the specified path.
     * <p>
     * <i>Note:</i> This version of the <code>doImport()</code> method will callback the provided
     * consumer every time a migration message is recorded. The message will also be recorded with
     * the report returned at the completion of the operation.
     *
     * @param exportDirectory path to import configurations from
     * @param consumer        an optional consumer to call whenever a new migration message is recorded
     *                        during the operation
     * @return a migration report for the import operation
     * @throws IllegalArgumentException if <code>exportDirectory</code> is <code>null</code>
     */
    MigrationReport doImport(Path exportDirectory, Optional<Consumer<MigrationMessage>> consumer);

    /**
     * Gets detailed information about all the {@link org.codice.ddf.migration.DataMigratable}
     * services currently registered.
     *
     * @return A collection of type {@link Describable}.
     */
    Collection<Describable> getOptionalMigratableInfo();
}
