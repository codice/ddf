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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.codice.ddf.configuration.migration.ConfigurationMigrationService;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.security.common.Security;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;

import ddf.security.service.SecurityServiceException;

@RunWith(MockitoJUnitRunner.class)
public class ExportCommandTest {

    private static final String ERROR_RED = Ansi.ansi()
            .a(Attribute.RESET)
            .fg(Ansi.Color.RED)
            .toString();

    private static final String WARNING_YELLOW = Ansi.ansi()
            .a(Attribute.RESET)
            .fg(Ansi.Color.YELLOW)
            .toString();

    private static final String INFO_WHITE = Ansi.ansi()
            .a(Attribute.RESET)
            .fg(Ansi.Color.WHITE)
            .toString();

    private static final String SUCCESS_GREEN = Ansi.ansi()
            .a(Attribute.RESET)
            .fg(Ansi.Color.GREEN)
            .toString();

    private static final String STARTING_EXPORT_MESSAGE = "Exporting current configurations to %s.";

    private static final String SUCCESSFUL_EXPORT_MESSAGE =
            "Successfully exported all configurations.";

    private static final String FAILED_EXPORT_MESSAGE =
            "Failed to export all configurations to %s.";

    private static final String ERROR_EXPORT_MESSAGE =
            "An error was encountered while executing this command. %s";

    private static final String INSUFFICIENT_PRIVILEGES_MESSAGE =
            "Current user doesn't have sufficient privileges to run this command";

    private static final String MIGRATION_WARNING_MESSAGE = "Warning";

    private static final String MIGRATION_EXCEPTION_MESSAGE = "Exception";

    private static final Collection<MigrationWarning> CONFIG_STATUS_MSGS =
            ImmutableList.of(new MigrationWarning(MIGRATION_WARNING_MESSAGE));

    @Mock
    private PrintStream mockConsole;

    @Mock
    private ConfigurationMigrationService mockConfigurationMigrationService;

    @Mock
    private Security security;

    @Mock
    private Path mockDefaultExportDirectory;

    @Test
    public void testExecuteReportWarnings() throws Exception {
        // Setup
        when(security.runWithSubjectOrElevate(any(Callable.class))).thenAnswer(this::delegateToCallable);
        when(mockConfigurationMigrationService.export(mockDefaultExportDirectory)).thenReturn(
                CONFIG_STATUS_MSGS);
        ExportCommand exportCommand = new ExportCommandUnderTest(mockConfigurationMigrationService,
                mockDefaultExportDirectory);

        // Perform Test
        exportCommand.execute();

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
    public void testExecuteNoWarnings() throws Exception {
        // Setup
        when(security.runWithSubjectOrElevate(any(Callable.class))).thenAnswer(this::delegateToCallable);
        when(mockConfigurationMigrationService.export(mockDefaultExportDirectory)).thenReturn(
                ImmutableList.of());
        ExportCommand exportCommand = new ExportCommandUnderTest(mockConfigurationMigrationService,
                mockDefaultExportDirectory);

        // Perform Test
        exportCommand.execute();

        // Verify
        assertSuccessMessage(mockDefaultExportDirectory);
    }

    @Test
    public void testExecuteWithExportDirectoryArgument() throws Exception {
        // Setup
        String exportDirectory = "export/directory";
        Path exportDirectoryPath = Paths.get(exportDirectory);

        when(security.runWithSubjectOrElevate(any(Callable.class))).thenAnswer(this::delegateToCallable);
        when(mockConfigurationMigrationService.export(exportDirectoryPath)).thenReturn(ImmutableList.of());

        ExportCommand exportCommand = new ExportCommandUnderTest(mockConfigurationMigrationService,
                exportDirectoryPath);
        exportCommand.exportDirectoryArgument = exportDirectory;

        // Perform Test
        exportCommand.execute();

        // Verify
        assertSuccessMessage(exportDirectoryPath);
    }

    @Test
    public void testExecuteWithEmptyExportDirectoryArgument() throws Exception {
        // Setup
        when(security.runWithSubjectOrElevate(any(Callable.class))).thenAnswer(this::delegateToCallable);
        when(mockConfigurationMigrationService.export(mockDefaultExportDirectory)).thenReturn(
                ImmutableList.of());

        ExportCommand exportCommand = new ExportCommandUnderTest(mockConfigurationMigrationService,
                mockDefaultExportDirectory);
        exportCommand.exportDirectoryArgument = "";

        // Perform Test
        exportCommand.execute();

        // Verify
        assertSuccessMessage(mockDefaultExportDirectory);
    }

    @Test
    public void testExecuteErrorOccurred() throws Exception {
        // Setup
        when(security.runWithSubjectOrElevate(any(Callable.class))).thenThrow(new InvocationTargetException(
                new MigrationException(MIGRATION_EXCEPTION_MESSAGE)));
        ExportCommand exportCommand = new ExportCommandUnderTest(mockConfigurationMigrationService,
                mockDefaultExportDirectory);

        // Perform Test
        exportCommand.execute();

        // Verify
        assertErrorMessage(MIGRATION_EXCEPTION_MESSAGE);
    }

    private Collection<MigrationWarning> delegateToCallable(InvocationOnMock invocation)
            throws Exception {

        @SuppressWarnings("unchecked")
        Callable<Collection<MigrationWarning>> callable =
                (Callable<Collection<MigrationWarning>>) invocation.getArguments()[0];
        return callable.call();
    }

    @Test
    public void testExecuteWhenSecurityExceptionIsThrown() throws Exception {
        // Setup
        when(security.runWithSubjectOrElevate(any(Callable.class))).thenThrow(new SecurityServiceException(
                INSUFFICIENT_PRIVILEGES_MESSAGE));
        ExportCommand exportCommand = new ExportCommandUnderTest(mockConfigurationMigrationService,
                mockDefaultExportDirectory);

        // Perform Test
        exportCommand.execute();

        // Verify
        assertErrorMessage(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }

    private void assertSuccessMessage(Path exportDirectory) {
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mockConsole, times(4)).print(argument.capture());
        List<String> values = argument.getAllValues();
        assertThat(values.get(0), is(INFO_WHITE));
        assertThat(values.get(1), is(String.format(STARTING_EXPORT_MESSAGE, exportDirectory)));
        assertThat(values.get(2), is(SUCCESS_GREEN));
        assertThat(values.get(3), is(SUCCESSFUL_EXPORT_MESSAGE));
    }

    private void assertErrorMessage(String expectedErrorMessage) {
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mockConsole, times(4)).print(argument.capture());
        List<String> values = argument.getAllValues();
        assertThat(values.get(0), is(INFO_WHITE));
        assertThat(values.get(1),
                is(String.format(STARTING_EXPORT_MESSAGE, mockDefaultExportDirectory)));
        assertThat(values.get(2), is(ERROR_RED));
        assertThat(values.get(3), is(String.format(ERROR_EXPORT_MESSAGE, expectedErrorMessage)));
    }

    private class ExportCommandUnderTest extends ExportCommand {
        public ExportCommandUnderTest(ConfigurationMigrationService service, Path exportPath) {
            setDefaultExportDirectory(exportPath);
            setSecurity(security);
            setConfigurationMigrationService(service);
        }

        @Override
        PrintStream getConsole() {
            return mockConsole;
        }
    }
}
