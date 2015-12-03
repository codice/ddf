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
package org.codice.ddf.commands.platform;

import org.apache.felix.gogo.commands.Command;
import org.codice.ddf.configuration.store.ConfigurationMigrationService;

/**
 * Executes the export method in {@link ConfigurationMigrationService}.  Configurations
 * are exported to the directory specified in the implementation of the service
 * (see {@link ConfigurationFileDirectory} for an example).
 */
@Command(scope = PlatformCommands.NAMESPACE, name = "config-export", description = "Exports configurations")
public class ExportCommand extends PlatformCommands {

    protected final ConfigurationMigrationService configurationMigrationService;

    public ExportCommand(ConfigurationMigrationService configurationMigrationService) {
        this.configurationMigrationService = configurationMigrationService;
    }

    @Override
    protected Object doExecute() throws Exception {
        configurationMigrationService.export();
        return null;
    }

}
