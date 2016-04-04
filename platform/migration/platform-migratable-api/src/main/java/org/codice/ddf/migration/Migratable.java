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

import org.codice.ddf.platform.services.common.Describable;

/**
 * This interface provides the mechanism for implementers to define how their data shall be
 * exported for later import into a new system. The framework that handles the Migratables
 * ensures that no two Migratable's methods are running at the same time. Implementors do not
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
     * Exports all migratable data to the specified directory
     *
     * @param exportPath path where {@link Migratable} specific data must be exported.
     *                   Will never be {@code null}.
     * @return metadata describing the results of the migration
     */
    @NotNull
    MigrationMetadata export(@NotNull Path exportPath) throws MigrationException;

}
