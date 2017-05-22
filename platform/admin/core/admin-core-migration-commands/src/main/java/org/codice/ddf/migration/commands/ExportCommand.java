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
package org.codice.ddf.migration.commands;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.configuration.migration.ConfigurationMigrationService;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.security.common.Security;

import ddf.security.service.SecurityServiceException;

/**
 * Command class used to export the system configuration and data.
 */
@Service
@Command(scope = MigrationCommands.NAMESPACE, name = "export", description =
        "The export command delegates to all "
                + "registered Migratable services to export bundle specific configuration and data.")
public class ExportCommand extends MigrationCommands {
    private static final String STARTING_EXPORT_MESSAGE = "Exporting current configurations to %s.";

    private static final String SUCCESSFUL_EXPORT_MESSAGE =
            "Successfully exported all configurations.";

    private static final String FAILED_EXPORT_MESSAGE =
            "Failed to export all configurations to %s.";

    private static final String ERROR_EXPORT_MESSAGE =
            "An error was encountered while executing this command. %s";

    @Reference
    private ConfigurationMigrationService configurationMigrationService;

    private Security security;

    private Path defaultExportDirectory = Paths.get(System.getProperty("ddf.home"),
            "etc",
            "exported");

    @Argument(index = 0, name = "exportDirectory", description = "Path to directory to store export", required = false, multiValued = false)
    String exportDirectoryArgument;

    public ExportCommand() {
        this.security = Security.getInstance();
    }

    @Override
    public Object execute() {
        Path exportDirectory;

        if (exportDirectoryArgument == null || exportDirectoryArgument.isEmpty()) {
            exportDirectory = defaultExportDirectory;
        } else {
            exportDirectory = Paths.get(exportDirectoryArgument);
        }

        outputInfoMessage(String.format(STARTING_EXPORT_MESSAGE, exportDirectory));

        try {
            Collection<MigrationWarning> migrationWarnings =
                    security.runWithSubjectOrElevate(() -> configurationMigrationService.export(
                            exportDirectory));

            if (migrationWarnings.isEmpty()) {
                outputSuccessMessage(SUCCESSFUL_EXPORT_MESSAGE);
            } else {
                for (MigrationWarning migrationWarning : migrationWarnings) {
                    outputWarningMessage(migrationWarning.getMessage());
                }

                outputWarningMessage(String.format(FAILED_EXPORT_MESSAGE, exportDirectory));
            }
        } catch (SecurityServiceException e) {
            outputErrorMessage(String.format(ERROR_EXPORT_MESSAGE, e.getMessage()));
        } catch (InvocationTargetException e) {
            outputErrorMessage(String.format(ERROR_EXPORT_MESSAGE,
                    e.getCause()
                            .getMessage()));
        }

        return null;
    }

    public void setDefaultExportDirectory(Path path) {
        this.defaultExportDirectory = path;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public void setConfigurationMigrationService(
            ConfigurationMigrationService configurationMigrationService) {
        this.configurationMigrationService = configurationMigrationService;
    }

}
