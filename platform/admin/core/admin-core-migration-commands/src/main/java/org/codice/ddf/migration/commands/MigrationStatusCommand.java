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
package org.codice.ddf.migration.commands;

import java.io.IOException;
import java.util.Collection;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.configuration.status.ConfigurationStatusService;
import org.codice.ddf.migration.MigrationWarning;

@Service
@Command(scope = MigrationCommands.NAMESPACE, name = "status", description =
        "Lists import status of configuration files "
                + "that were imported through the Migration Service. Files that failed to import correctly will be moved to the "
                + "etc/failed directory. All successful imports will be moved to the etc/processed directory")
public class MigrationStatusCommand extends MigrationCommands {

    static final String SUCCESSFUL_IMPORT_MESSAGE = "All config files imported successfully.";

    static final String FAILED_IMPORT_MESSAGE = "Failed to import file [%s]. ";

    static final String NO_CONFIG_STATUS_MESSAGE =
            "No Configuration Status returned from Configuration Status Service.";

    @Reference
    private ConfigurationStatusService configStatusService;

    @Override
    public Object execute() {
        try {
            Collection<MigrationWarning> configStatusMessages = getFailedImports();

            if (configStatusMessages == null) {
                outputErrorMessage(NO_CONFIG_STATUS_MESSAGE);
                return null;
            }

            if (configStatusMessages.isEmpty()) {
                outputSuccessMessage(SUCCESSFUL_IMPORT_MESSAGE);
                return null;
            }

            for (MigrationWarning configStatus : configStatusMessages) {
                outputErrorMessage(constructErrorMessage(configStatus));
            }
        } catch (IOException | RuntimeException e) {
            String message =
                    "An error was encountered while executing this command. " + e.getMessage();
            outputErrorMessage(message);
        }

        return null;
    }

    private Collection<MigrationWarning> getFailedImports() throws IOException {
        return configStatusService.getFailedConfigurationFiles();
    }

    private String constructErrorMessage(MigrationWarning configStatus) {
        return String.format(FAILED_IMPORT_MESSAGE, configStatus.getMessage());
    }

    public void setConfigStatusService(ConfigurationStatusService configStatusService) {
        this.configStatusService = configStatusService;
    }
}
