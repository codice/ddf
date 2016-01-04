/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.configuration.migration;

import java.nio.file.Path;
import java.util.Collection;

import javax.validation.constraints.NotNull;

import org.codice.ddf.configuration.status.MigrationException;
import org.codice.ddf.configuration.status.MigrationWarning;

/**
 * Service that provides a way to migrate configurations from one instance of DDF to another.  This
 * includes exporting and importing of configurations.
 */
public interface ConfigurationMigrationService {

    /**
     * Exports configurations to specified path
     *
     * @param exportDirectory path to export configurations
     * @throws MigrationException thrown if one or more Configurations couldn't be exported
     * @return MigrationWarning returned if there were non-fatal issues when exporting
     */
    Collection<MigrationWarning> export(@NotNull Path exportDirectory) throws MigrationException;
}
