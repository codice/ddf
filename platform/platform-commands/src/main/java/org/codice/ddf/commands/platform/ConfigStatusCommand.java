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

import static org.apache.commons.lang.Validate.notNull;

import java.io.IOException;
import java.util.Collection;

import javax.validation.constraints.NotNull;

import org.apache.felix.gogo.commands.Command;
import org.codice.ddf.configuration.status.ConfigurationStatusService;
import org.codice.ddf.configuration.status.MigrationWarning;

@Command(scope = PlatformCommands.NAMESPACE, name = "config-status", description = "Lists import status of configuration files.")
public class ConfigStatusCommand extends PlatformCommands {

    static final String SUCCESSFUL_IMPORT_MESSAGE = "All config files imported successfully.";

    static final String FAILED_IMPORT_MESSAGE = "Failed to import file [%s]. ";

    static final String NO_CONFIG_STATUS_MESSAGE = "No Configuration Status returned from Configuration Status Service.";

    private ConfigurationStatusService configStatusService;

    public ConfigStatusCommand(@NotNull ConfigurationStatusService configStatusService) {
        notNull(configStatusService, "Configuration Status Service cannot be null");
        this.configStatusService = configStatusService;
    }

    @Override
    protected Object doExecute() {
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
}
