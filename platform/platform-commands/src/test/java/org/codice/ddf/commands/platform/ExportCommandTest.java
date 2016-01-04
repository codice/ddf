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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.codice.ddf.configuration.migration.ConfigurationMigrationService;
import org.codice.ddf.configuration.status.MigrationException;
import org.codice.ddf.configuration.status.MigrationWarning;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExportCommandTest {

    private static final String ERROR_RED = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.RED)
            .toString();

    private static final String WARNING_YELLOW = Ansi.ansi().a(Attribute.RESET)
            .fg(Ansi.Color.YELLOW).toString();

    private static final String INFO_WHITE = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.WHITE)
            .toString();

    private static final String SUCCESS_GREEN = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.GREEN)
            .toString();

    private static final String STARTING_EXPORT_MESSAGE = "Exporting current configurations to %s.";

    private static final String SUCCESSFUL_EXPORT_MESSAGE = "Successfully exported all configurations.";

    private static final String FAILED_EXPORT_MESSAGE = "Failed to export all configurations to %s.";

    private static final String ERROR_EXPORT_MESSAGE = "An error was encountered while executing this command. %s";

    private static final String MIGRATION_WARNING_MESSAGE = "Warning";

    private static final String MIGRATION_EXCEPTION_MESSAGE = "Exception";

    private static final Collection<MigrationWarning> CONFIG_STATUS_MSGS;

    static {
        Collection<MigrationWarning> configStatus = new ArrayList<>(2);
        MigrationWarning cs1 = new MigrationWarning(MIGRATION_WARNING_MESSAGE);
        configStatus.add(cs1);
        CONFIG_STATUS_MSGS = Collections.unmodifiableCollection(configStatus);
    }

    @Mock
    private DirectoryStream<Path> mockDirectoryStream;

    @Mock
    private PrintStream mockConsole;

    @Mock
    private Iterator<Path> mockIterator;

    @Mock
    private ConfigurationMigrationService mockConfigurationMigrationService;

    @Mock
    private Path mockDefaultExportDirectory;

    @Test
    public void testDoExecuteReportWarnings() throws Exception {
        // Setup
        when(mockConfigurationMigrationService.export(Matchers.any()))
                .thenReturn(CONFIG_STATUS_MSGS);
        ExportCommand exportCommand = new ExportCommand(mockConfigurationMigrationService,
                mockDefaultExportDirectory) {
            @Override
            PrintStream getConsole() {
                return mockConsole;
            }
        };

        // Perform Test
        exportCommand.doExecute();

        // Verify
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mockConsole, times(6)).print(argument.capture());
        List<String> values = argument.getAllValues();
        assertThat(values.get(0), is(INFO_WHITE));
        assertThat(values.get(1),
                is(String.format(STARTING_EXPORT_MESSAGE, mockDefaultExportDirectory)));
        assertThat(values.get(2), is(WARNING_YELLOW));
        assertThat(values.get(3), is(MIGRATION_WARNING_MESSAGE));
        assertThat(values.get(4), is(WARNING_YELLOW));
        assertThat(values.get(5),
                is(String.format(FAILED_EXPORT_MESSAGE, mockDefaultExportDirectory)));
    }

    @Test
    public void testDoExecuteNoWarnings() throws Exception {
        // Setup
        when(mockConfigurationMigrationService.export(Matchers.any()))
                .thenReturn(new ArrayList<>());
        ExportCommand exportCommand = new ExportCommand(mockConfigurationMigrationService,
                mockDefaultExportDirectory) {
            @Override
            PrintStream getConsole() {
                return mockConsole;
            }
        };

        // Perform Test
        exportCommand.doExecute();

        // Verify
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mockConsole, times(4)).print(argument.capture());
        List<String> values = argument.getAllValues();
        assertThat(values.get(0), is(INFO_WHITE));
        assertThat(values.get(1),
                is(String.format(STARTING_EXPORT_MESSAGE, mockDefaultExportDirectory)));
        assertThat(values.get(2), is(SUCCESS_GREEN));
        assertThat(values.get(3), is(SUCCESSFUL_EXPORT_MESSAGE));
    }

    @Test
    public void testDoExecuteErrorOccurred() throws Exception {
        // Setup
        when(mockConfigurationMigrationService.export(Matchers.any()))
                .thenThrow(new MigrationException(MIGRATION_EXCEPTION_MESSAGE));
        ExportCommand exportCommand = new ExportCommand(mockConfigurationMigrationService,
                mockDefaultExportDirectory) {
            @Override
            PrintStream getConsole() {
                return mockConsole;
            }
        };

        // Perform Test
        exportCommand.doExecute();

        // Verify
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mockConsole, times(4)).print(argument.capture());
        List<String> values = argument.getAllValues();
        assertThat(values.get(0), is(INFO_WHITE));
        assertThat(values.get(1),
                is(String.format(STARTING_EXPORT_MESSAGE, mockDefaultExportDirectory)));
        assertThat(values.get(2), is(ERROR_RED));
        assertThat(values.get(3),
                is(String.format(ERROR_EXPORT_MESSAGE, MIGRATION_EXCEPTION_MESSAGE)));
    }
}
