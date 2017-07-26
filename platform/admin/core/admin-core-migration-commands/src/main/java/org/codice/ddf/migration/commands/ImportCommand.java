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

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.configuration.migration.ConfigurationMigrationService;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.security.common.Security;

import ddf.security.service.SecurityServiceException;

/**
 * Command class used to import the system configuration exported via the {@link ExportCommand} command.
 */
@Service
@Command(scope = MigrationCommands.NAMESPACE, name = "import", description =
        "The import command delegates to all "
                + "registered Migratable services to export bundle specific configuration and data.")
public class ImportCommand extends MigrationCommands {
    private static final String STARTING_IMPORT_MESSAGE = "Importing new configurations from %s.";

    private static final String SUCCESSFUL_IMPORT_MESSAGE =
            "Successfully imported all configurations.";

    private static final String SUCCESSFUL_IMPORT_WITH_WARNINGS_MESSAGE =
            "Successfully imported all configurations with warnings; make sure to review.";

    private static final String FAILED_IMPORT_MESSAGE =
            "Failed to import all configurations from %s.";

    private static final String ERROR_IMPORT_MESSAGE =
            "An error was encountered while executing this command. %s";

    @Argument(index = 0, name = "importDirectory", description = "Path to directory where to find the file to import", required = true, multiValued = false)
    String exportDirectoryArgument;

    public ImportCommand() {}

    @Override
    public Object execute() {
        final Path exportDirectory;

        if (StringUtils.isEmpty(exportDirectoryArgument)) {
            exportDirectory = defaultExportDirectory;
        } else {
            exportDirectory = Paths.get(exportDirectoryArgument);
        }
        outputInfoMessage(String.format(ImportCommand.STARTING_IMPORT_MESSAGE, exportDirectory));
        try {
            final MigrationReport report =
                    security.runWithSubjectOrElevate(() -> configurationMigrationService.doImport(
                            exportDirectory));

            if (report.hasErrors()) {
                outputWarningMessage(String.format(FAILED_IMPORT_MESSAGE, exportDirectory));
            } else if (report.hasWarnings()) {
                outputSuccessMessage(SUCCESSFUL_IMPORT_WITH_WARNINGS_MESSAGE);
            } else {
                outputSuccessMessage(SUCCESSFUL_IMPORT_MESSAGE);
            }
            report.errors()
                    .forEach(this::outputErrorMessage);
            report.warnings()
                    .forEach(this::outputWarningMessage);
        } catch (SecurityServiceException e) {
            outputErrorMessage(String.format(ERROR_IMPORT_MESSAGE, e));
        } catch (InvocationTargetException e) {
            outputErrorMessage(String.format(ERROR_IMPORT_MESSAGE, e.getCause()));
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
