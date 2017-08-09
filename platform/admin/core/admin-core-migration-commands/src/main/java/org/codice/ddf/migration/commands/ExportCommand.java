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
import java.util.Optional;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import ddf.security.service.SecurityServiceException;

/**
 * Command class used to export the system configuration and data.
 */
@Service
@Command(scope = MigrationCommands.NAMESPACE, name = "export", description =
        "The export command delegates to all "
                + "registered Migratable services to export bundle specific configuration and data.")
public class ExportCommand extends MigrationCommands {
    private static final String ERROR_EXPORT_MESSAGE =
            "An error was encountered while executing this command. %s";

    @Argument(index = 0, name = "exportDirectory", description = "Path to directory where to store the exported file", required = false, multiValued = false)
    String exportDirectoryArgument;

    public ExportCommand() {
    }

    @Override
    public Object execute() {
        Path exportDirectory;

        if (exportDirectoryArgument == null || exportDirectoryArgument.isEmpty()) {
            exportDirectory = defaultExportDirectory;
        } else {
            exportDirectory = Paths.get(exportDirectoryArgument);
        }
        try {
            security.runWithSubjectOrElevate(() -> configurationMigrationService.doExport(
                    exportDirectory,
                    Optional.of(this::outputMessage)));
        } catch (SecurityServiceException e) {
            outputErrorMessage(String.format(ERROR_EXPORT_MESSAGE, e));
        } catch (InvocationTargetException e) {
            outputErrorMessage(String.format(ERROR_EXPORT_MESSAGE, e.getCause()));
        }
        return null;
    }
}
