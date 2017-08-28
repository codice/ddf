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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import com.google.common.annotations.VisibleForTesting;

import ddf.security.service.SecurityServiceException;

/**
 * Command class used to export the system configuration and data.
 */
@Service
@Command(scope = MigrationCommand.NAMESPACE, name = "export", description = "Exports the system configuration and profile.")
public class ExportCommand extends MigrationCommand {
    @VisibleForTesting
    @Argument(index = 0, name = "exportDirectory", description = "Path to directory where to store the exported file", required = false, valueToShowInHelp = MigrationCommand.EXPORTED, multiValued = false)
    String exportDirectoryArgument;

    @Override
    public Object execute() {
        Path exportDirectory;

        try {
            if (exportDirectoryArgument == null || exportDirectoryArgument.isEmpty()) {
                exportDirectory = defaultExportDirectory;
            } else {
                exportDirectory = Paths.get(exportDirectoryArgument);
            }
            security.runWithSubjectOrElevate(() -> configurationMigrationService.doExport(
                    exportDirectory,
                    this::outputMessage));
        } catch (InvalidPathException e) {
            outputErrorMessage(String.format(ERROR_MESSAGE,
                    String.format("invalid path [%s] (%s)",
                            exportDirectoryArgument,
                            e.getMessage())));
        } catch (SecurityServiceException e) {
            outputErrorMessage(String.format(ERROR_MESSAGE, e.getMessage()));
        } catch (InvocationTargetException e) {
            outputErrorMessage(String.format(ERROR_MESSAGE,
                    e.getCause()
                            .getMessage()));
        }
        return null;
    }
}
