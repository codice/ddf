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
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadState;
import org.codice.ddf.configuration.migration.ConfigurationMigrationService;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationWarning;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import com.google.common.collect.ImmutableList;

import ddf.security.Subject;
import ddf.security.common.util.Security;

@PrepareForTest(Security.class)
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

    private static ThreadState subjectThreadState;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Mock
    private Subject subject;

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

    @Before
    public void setUp() {
        initMocks(this);
        mockStatic(Security.class);
    }

    @Test
    public void testDoExecuteReportWarnings() throws Exception {
        // Setup
        setSubject(subject);
        when(subject.execute(any(Callable.class))).thenAnswer(this::delegateToCallable);
        when(mockConfigurationMigrationService.export(mockDefaultExportDirectory)).thenReturn(
                CONFIG_STATUS_MSGS);
        ExportCommand exportCommand = new ExportCommandUnderTest(mockConfigurationMigrationService,
                mockDefaultExportDirectory);

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
        setSubject(subject);
        when(subject.execute(any(Callable.class))).thenAnswer(this::delegateToCallable);
        when(mockConfigurationMigrationService.export(mockDefaultExportDirectory)).thenReturn(
                ImmutableList.of());
        ExportCommand exportCommand = new ExportCommandUnderTest(mockConfigurationMigrationService,
                mockDefaultExportDirectory);

        // Perform Test
        exportCommand.doExecute();

        // Verify
        assertSuccessMessage(mockDefaultExportDirectory);
    }

    @Test
    public void testDoExecuteWithExportDirectoryArgument() throws Exception {
        // Setup
        setSubject(subject);
        String exportDirectory = "export/directory";
        Path exportDirectoryPath = Paths.get(exportDirectory);

        when(subject.execute(any(Callable.class))).thenAnswer(this::delegateToCallable);
        when(mockConfigurationMigrationService.export(exportDirectoryPath)).thenReturn(ImmutableList.of());

        ExportCommand exportCommand = new ExportCommandUnderTest(mockConfigurationMigrationService,
                exportDirectoryPath);
        exportCommand.exportDirectoryArgument = exportDirectory;

        // Perform Test
        exportCommand.doExecute();

        // Verify
        assertSuccessMessage(exportDirectoryPath);
    }

    @Test
    public void testDoExecuteErrorOccurred() throws Exception {
        // Setup
        setSubject(subject);
        when(subject.execute(any(Callable.class))).thenAnswer(this::delegateToCallable);
        when(mockConfigurationMigrationService.export(mockDefaultExportDirectory)).thenThrow(new MigrationException(
                MIGRATION_EXCEPTION_MESSAGE));
        ExportCommand exportCommand = new ExportCommandUnderTest(mockConfigurationMigrationService,
                mockDefaultExportDirectory);

        // Perform Test
        exportCommand.doExecute();

        // Verify
        assertErrorMessage(MIGRATION_EXCEPTION_MESSAGE);
    }

    @Test
    public void executeWithShiroSubject() throws Exception {
        // Setup
        setSubject(subject);
        when(subject.execute(any(Callable.class))).thenAnswer(this::delegateToCallable);
        when(mockConfigurationMigrationService.export(mockDefaultExportDirectory)).thenReturn(
                ImmutableList.of());
        ExportCommand exportCommand = new ExportCommandUnderTest(mockConfigurationMigrationService,
                mockDefaultExportDirectory);

        // Perform Test
        exportCommand.doExecute();

        // Verify
        assertSuccessMessage(mockDefaultExportDirectory);
    }

    @Test
    public void executeWithSystemSubject() throws Exception {
        // Setup
        when(Security.javaSubjectHasAdminRole()).thenReturn(true);
        when(Security.getSystemSubject()).thenReturn(subject);

        when(subject.execute(any(Callable.class))).thenAnswer(this::delegateToCallable);
        when(mockConfigurationMigrationService.export(mockDefaultExportDirectory)).thenReturn(
                ImmutableList.of());
        ExportCommand exportCommand = new ExportCommandUnderTest(mockConfigurationMigrationService,
                mockDefaultExportDirectory);

        // Perform Test
        exportCommand.doExecute();

        // Verify
        assertSuccessMessage(mockDefaultExportDirectory);
    }

    @Test
    public void executeWithNoSubject() throws Exception {
        // Setup
        when(Security.javaSubjectHasAdminRole()).thenReturn(true);
        when(Security.getSystemSubject()).thenReturn(null);

        when(subject.execute(any(Callable.class))).thenAnswer(this::delegateToCallable);
        when(mockConfigurationMigrationService.export(mockDefaultExportDirectory)).thenReturn(
                ImmutableList.of());
        ExportCommand exportCommand = new ExportCommandUnderTest(mockConfigurationMigrationService,
                mockDefaultExportDirectory);

        // Perform Test
        exportCommand.doExecute();

        // Verify
        assertErrorMessage(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }

    @Test
    public void executeWhenSubjectHasNoAdminRole() throws Exception {
        // Setup
        when(Security.javaSubjectHasAdminRole()).thenReturn(false);

        when(subject.execute(any(Callable.class))).thenAnswer(this::delegateToCallable);
        when(mockConfigurationMigrationService.export(mockDefaultExportDirectory)).thenReturn(
                ImmutableList.of());
        ExportCommand exportCommand = new ExportCommandUnderTest(mockConfigurationMigrationService,
                mockDefaultExportDirectory);

        // Perform Test
        exportCommand.doExecute();

        // Verify
        assertErrorMessage(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }

    @After
    public void tearDownSubject() {
        clearSubject();
    }

    private Collection<MigrationWarning> delegateToCallable(InvocationOnMock invocation)
            throws Exception {

        @SuppressWarnings("unchecked")
        Callable<Collection<MigrationWarning>> callable =
                (Callable<Collection<MigrationWarning>>) invocation.getArguments()[0];
        return callable.call();
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

    private void setSubject(Subject subject) {
        clearSubject();
        subjectThreadState = new SubjectThreadState(subject);
        subjectThreadState.bind();
    }

    private static void clearSubject() {
        if (subjectThreadState != null) {
            subjectThreadState.clear();
            subjectThreadState = null;
        }
    }

    private class ExportCommandUnderTest extends ExportCommand {
        public ExportCommandUnderTest(ConfigurationMigrationService service, Path exportPath) {
            super(service, exportPath);
        }

        @Override
        PrintStream getConsole() {
            return mockConsole;
        }
    }
}
