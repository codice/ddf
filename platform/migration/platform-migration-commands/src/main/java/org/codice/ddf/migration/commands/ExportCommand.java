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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.codice.ddf.configuration.migration.ConfigurationMigrationService;
import org.codice.ddf.migration.MigrationWarning;

/**
 * Executes the export method in {@link ConfigurationMigrationService}.  Configurations
 * are exported to the directory specified in the implementation of the service
 * (see {@link ConfigurationFileDirectory} for an example).
 */
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

    protected final ConfigurationMigrationService configurationMigrationService;

    protected final Path defaultExportDirectory;

    @Argument(index = 0, name = "exportDirectory", description = "Path to directory to store export", required = false, multiValued = false)
    String exportDirectoryArgument;

    public ExportCommand(ConfigurationMigrationService configurationMigrationService,
            Path defaultExportDirectory) {
        this.configurationMigrationService = configurationMigrationService;
        this.defaultExportDirectory = defaultExportDirectory;
    }

    @Override
    protected Object doExecute() {
        Path exportDirectory;
        if (exportDirectoryArgument == null || exportDirectoryArgument.isEmpty()) {
            exportDirectory = defaultExportDirectory;
        } else {
            exportDirectory = Paths.get(exportDirectoryArgument);
        }

        outputInfoMessage(String.format(STARTING_EXPORT_MESSAGE, exportDirectory));
        try {
            Collection<MigrationWarning> migrationWarnings = configurationMigrationService.export(
                    exportDirectory);

            if (migrationWarnings.isEmpty()) {
                outputSuccessMessage(String.format(SUCCESSFUL_EXPORT_MESSAGE));
            } else {
                for (MigrationWarning migrationWarning : migrationWarnings) {
                    outputWarningMessage(migrationWarning.getMessage());
                }
                outputWarningMessage(String.format(FAILED_EXPORT_MESSAGE, exportDirectory));
            }
        } catch (Exception e) {
            outputErrorMessage(String.format(ERROR_EXPORT_MESSAGE, e.getMessage()));
        }
        return null;
    }

}
