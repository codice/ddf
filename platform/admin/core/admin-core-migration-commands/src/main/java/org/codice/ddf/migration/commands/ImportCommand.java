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

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import ddf.security.service.SecurityServiceException;

/**
 * Command class used to import the system configuration exported via the {@link ExportCommand} command.
 */
@Service
@Command(scope = MigrationCommands.NAMESPACE, name = "import", description =
        "The import command delegates to all "
                + "registered Migratable services to import bundle specific configuration and data.")
public class ImportCommand extends MigrationCommands {
    private static final String ERROR_IMPORT_MESSAGE =
            "An error was encountered while executing this command. %s";

    @Argument(index = 0, name = "importDirectory", description = "Path to directory where to find the file to import", required = false, multiValued = false)
    String exportDirectoryArgument;

    public ImportCommand() {
    }

    @Override
    public Object execute() {
        final Path exportDirectory;

        if (StringUtils.isEmpty(exportDirectoryArgument)) {
            exportDirectory = defaultExportDirectory;
        } else {
            exportDirectory = Paths.get(exportDirectoryArgument);
        }
        try {
            security.runWithSubjectOrElevate(() -> configurationMigrationService.doImport(
                    exportDirectory,
                    Optional.of(this::outputMessage)));
        } catch (SecurityServiceException e) {
            outputErrorMessage(String.format(ERROR_IMPORT_MESSAGE, e));
        } catch (InvocationTargetException e) {
            outputErrorMessage(String.format(ERROR_IMPORT_MESSAGE, e.getCause()));
        }
        return null;
    }
}
