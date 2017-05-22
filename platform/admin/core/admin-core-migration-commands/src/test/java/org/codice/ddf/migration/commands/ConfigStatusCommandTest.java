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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.codice.ddf.configuration.status.ConfigurationStatusService;
import org.codice.ddf.migration.MigrationWarning;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigStatusCommandTest {

    private static final String ERROR_RED = Ansi.ansi()
            .a(Attribute.RESET)
            .fg(Ansi.Color.RED)
            .toString();

    private static final String SUCCESS_GREEN = Ansi.ansi()
            .a(Attribute.RESET)
            .fg(Ansi.Color.GREEN)
            .toString();

    private static final Path FAILED_CONFIG_FILE1 = Paths.get("/path/to/configFile1.config");

    private static final Path FAILED_CONFIG_FILE2 = Paths.get("/path/to/configFile2.config");

    private static final Collection<MigrationWarning> CONFIG_STATUS_MSGS;

    static {
        Collection<MigrationWarning> configStatus = new ArrayList<>(2);
        MigrationWarning cs1 = new MigrationWarning(FAILED_CONFIG_FILE1.toString());
        configStatus.add(cs1);
        MigrationWarning cs2 = new MigrationWarning(FAILED_CONFIG_FILE2.toString());
        configStatus.add(cs2);
        CONFIG_STATUS_MSGS = Collections.unmodifiableCollection(configStatus);
    }

    @Mock
    private DirectoryStream<Path> mockDirectoryStream;

    @Mock
    private PrintStream mockConsole;

    @Mock
    private Iterator<Path> mockIterator;

    @Mock
    private ConfigurationStatusService mockConfigStatusService;

    @Test
    public void testExecuteReportFailedImports() throws Exception {
        // Setup
        when(mockConfigStatusService.getFailedConfigurationFiles()).thenReturn(CONFIG_STATUS_MSGS);
        MigrationStatusCommand migrationStatusCommand = new MigrationStatusCommand() {
            @Override
            PrintStream getConsole() {
                return mockConsole;
            }
        };
        migrationStatusCommand.setConfigStatusService(mockConfigStatusService);

        // Perform Test
        migrationStatusCommand.execute();

        // Verify
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mockConsole, times(4)).print(argument.capture());
        List<String> values = argument.getAllValues();
        assertThat(values.get(0), is(ERROR_RED));
        assertThat(values.get(1),
                is(String.format(MigrationStatusCommand.FAILED_IMPORT_MESSAGE,
                        FAILED_CONFIG_FILE1.toString())));
        assertThat(values.get(0), is(ERROR_RED));
        assertThat(values.get(3),
                is(String.format(MigrationStatusCommand.FAILED_IMPORT_MESSAGE,
                        FAILED_CONFIG_FILE2.toString())));
    }

    @Test
    public void testExecuteNoFailedImports() throws Exception {
        // Setup
        when(mockConfigStatusService.getFailedConfigurationFiles()).thenReturn(new ArrayList<>(0));
        MigrationStatusCommand migrationStatusCommand = new MigrationStatusCommand() {
            @Override
            PrintStream getConsole() {
                return mockConsole;
            }
        };
        migrationStatusCommand.setConfigStatusService(mockConfigStatusService);

        // Perform Test
        migrationStatusCommand.execute();

        // Verify
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mockConsole, times(2)).print(argument.capture());
        List<String> values = argument.getAllValues();
        assertThat(values.get(0), is(SUCCESS_GREEN));
        assertThat(values.get(1), is(MigrationStatusCommand.SUCCESSFUL_IMPORT_MESSAGE));
    }

    @Test
    public void testExecuteRuntimeExceptionWhileReadingFailedDirectory() throws Exception {
        // Setup
        doThrow(new RuntimeException()).when(mockConfigStatusService)
                .getFailedConfigurationFiles();
        MigrationStatusCommand migrationStatusCommand = new MigrationStatusCommand() {
            @Override
            PrintStream getConsole() {
                return mockConsole;
            }
        };
        migrationStatusCommand.setConfigStatusService(mockConfigStatusService);

        // Perform Test
        migrationStatusCommand.execute();

        // Verify
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mockConsole, times(2)).print(argument.capture());
        List<String> values = argument.getAllValues();
        assertThat(values.get(0), is(ERROR_RED));
        assertThat(values.get(1),
                containsString("An error was encountered while executing this command."));
    }

    @Test
    public void testExecuteNullConfigurationStatus() throws Exception {
        // Setup
        when(mockConfigStatusService.getFailedConfigurationFiles()).thenReturn(null);
        MigrationStatusCommand migrationStatusCommand = new MigrationStatusCommand() {
            @Override
            PrintStream getConsole() {
                return mockConsole;
            }
        };
        migrationStatusCommand.setConfigStatusService(mockConfigStatusService);

        // Perform Test
        migrationStatusCommand.execute();

        // Verify
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mockConsole, times(2)).print(argument.capture());
        List<String> values = argument.getAllValues();
        assertThat(values.get(0), is(ERROR_RED));
        assertThat(values.get(1), is(MigrationStatusCommand.NO_CONFIG_STATUS_MESSAGE));
    }
}
