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

import java.nio.file.Path;

import javax.validation.constraints.NotNull;

/**
 * This interface provides the mechanism for implementers to define how their data shall be
 * exported for later import into a new system.
 *
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
public interface Migratable {

    /**
     * Export all migratable data to the specified directory
     *
     * @param exportPath Path where {@link Migratable} specific data must be exported.
     *                   exportPath will never be null.
     *
     * @return Metadata describing the results of the migration
     */
    @NotNull
    public MigrationMetadata export(@NotNull Path exportPath) throws MigrationException;

    /**
     * Get a description of the migratable data. This description will be
     * used for display purposes by consumers of {@link Migratable} services.
     * @return A short description of the migratable data
     */
    @NotNull
    public String getDescription();

    /**
     * Determines if the exported data from this {@link Migratable} is optional or required.
     * This can be used by consumers of {@link Migratable} services to determine if the data
     * of this export is necessary when importing into a new system.
     * @return Status of whether or not the export is optional
     */
    public boolean isOptional();
}
