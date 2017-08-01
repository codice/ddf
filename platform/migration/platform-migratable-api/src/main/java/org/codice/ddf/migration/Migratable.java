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
package org.codice.ddf.migration;

import org.codice.ddf.platform.services.common.Describable;

/**
 * This interface provides the mechanism for implementers to define how their data shall be
 * exported for later import into a new system. The framework that handles the migratables
 * ensures that no two migratable's methods are running at the same time. Implementors do not
 * need to program exports and imports with regard to reflexive thread-safety.
 * <p>
 * <b>This interface should not be implemented directly;</b> the appropriate extension should be chosen,
 * either {@link ConfigurationMigratable} or {@link DataMigratable}.
 * </p>
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public interface Migratable extends Describable {
    /**
     * Gets the current export version handled by this migratable.
     * <p>
     * When exporting data, the migration framework will export this version information and provide
     * it back to the {@link #doImport} method as part of the import migration context when importing
     * the corresponding exported data.
     *
     * @return the current version handled by this migratable
     */
    @Override
    public String getVersion();

    /**
     * Exports all required migratable data to the specified context.
     * <p>
     * Errors and/or warnings can be recorded along with the context's report. Doing so will not abort
     * the operation right away.
     *
     * @param context a migration context to export all migratable data to
     * @throws MigrationException to stop the export operation
     */
    public void doExport(ExportMigrationContext context);

    /**
     * Imports all exported migratable data provided by the specified context.
     * <p>
     * Errors and/or warnings can be recorded along with the context's report. Doing so will not abort
     * the operation right away.
     *
     * @param context a migration context to import all exported migratable data from
     * @throws MigrationException to stop the import operation
     */
    public void doImport(ImportMigrationContext context);
}
