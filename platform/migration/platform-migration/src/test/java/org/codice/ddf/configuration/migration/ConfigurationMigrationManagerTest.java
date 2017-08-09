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
package org.codice.ddf.configuration.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.karaf.system.SystemService;
import org.codice.ddf.migration.ConfigurationMigratable;
import org.codice.ddf.migration.DataMigratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.migration.UnexpectedMigrationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationMigrationManagerTest extends AbstractMigrationTest {

    public static final String TEST_VERSION = "1.0";

    public static final String TEST_MESSAGE = "Test message.";

    public static final String TEST_DIRECTORY = "exported";

    private ConfigurationMigrationManager configurationMigrationManager;

    private ObjectName configMigrationServiceObjectName;

    private List<ConfigurationMigratable> configurationMigratables;

    private List<DataMigratable> dataMigratables;

    @Mock
    private MBeanServer mockMBeanServer;

    @Mock
    private SystemService mockSystemService;

    @Before
    public void setup() throws MalformedObjectNameException {
        configMigrationServiceObjectName = new ObjectName(
                ConfigurationMigrationManager.class.getName() + ":service=configuration-migration");
        configurationMigratables = Collections.emptyList();
        dataMigratables = Collections.emptyList();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullMBeanServer() {
        new ConfigurationMigrationManager(null,
                new ArrayList<>(),
                new ArrayList<>(),
                mockSystemService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullConfigurationMigratablesList() {
        new ConfigurationMigrationManager(mockMBeanServer,
                null,
                new ArrayList<>(),
                mockSystemService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullDataMigratablesList() {
        new ConfigurationMigrationManager(mockMBeanServer,
                new ArrayList<>(),
                null,
                mockSystemService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullSystemService() {
        new ConfigurationMigrationManager(mockMBeanServer,
                new ArrayList<>(),
                new ArrayList<>(),
                null);
    }

    @Test(expected = IOError.class)
    public void constructorWithoutProductVersion() {
        new ConfigurationMigrationManager(mockMBeanServer,
                new ArrayList<>(),
                new ArrayList<>(),
                mockSystemService);
    }

    @Test
    public void init() throws Exception {
        configurationMigrationManager = getConfigurationMigrationManager();
        configurationMigrationManager.init();

        verify(mockMBeanServer).registerMBean(configurationMigrationManager,
                configMigrationServiceObjectName);
    }

    @Test
    public void initWhenServiceAlreadyRegisteredAsMBean() throws Exception {
        configurationMigrationManager = getConfigurationMigrationManager();

        when(mockMBeanServer.registerMBean(configurationMigrationManager,
                configMigrationServiceObjectName)).thenThrow(new InstanceAlreadyExistsException())
                .thenReturn(null);

        configurationMigrationManager.init();

        verify(mockMBeanServer, times(2)).registerMBean(configurationMigrationManager,
                configMigrationServiceObjectName);
        verify(mockMBeanServer).unregisterMBean(configMigrationServiceObjectName);
    }

    @Test
    public void doExportWithStringSucceedsWithWarnings() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());
        MigrationReport mockReport = mock(MigrationReport.class);

        when(mockReport.warnings()).thenReturn(Stream.of(new MigrationWarning(TEST_MESSAGE)));
        doReturn(mockReport).when(configurationMigrationManager)
                .doExport(any(Path.class));

        Collection<MigrationWarning> warnings = configurationMigrationManager.doExport(
                TEST_DIRECTORY);

        reportHasWarningMessage(warnings.stream(), TEST_MESSAGE);
        verify(configurationMigrationManager).doExport(any(Path.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void doExportWithNullString() throws Exception {
        configurationMigrationManager = getConfigurationMigrationManager();

        configurationMigrationManager.doExport((String) null);
    }

    @Test(expected = MigrationException.class)
    public void doExportWithStringThrowsException() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());
        MigrationReport mockReport = mock(MigrationReport.class);
        doThrow(MigrationException.class).when(mockReport)
                .verifyCompletion();
        doReturn(mockReport).when(configurationMigrationManager)
                .doExport(any(Path.class));

        configurationMigrationManager.doExport(TEST_DIRECTORY);
    }

    @Test
    public void doExportSucceeds() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());

        expectExportDelegationIsSuccessful();

        MigrationReport report = configurationMigrationManager.doExport(DDF_HOME.resolve(
                TEST_DIRECTORY));

        assertThat("Export was not successful", report.wasSuccessful(), is(true));
        verify(configurationMigrationManager).delegateToExportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
    }

    @Test
    public void doExportSucceedsWithWarnings() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());

        doAnswer(invocation -> {
            MigrationReport report = invocation.getArgument(0);
            report.record(new MigrationWarning(TEST_MESSAGE));
            return null;
        }).when(configurationMigrationManager)
                .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));

        MigrationReport report = configurationMigrationManager.doExport(DDF_HOME.resolve(
                TEST_DIRECTORY));

        assertThat("Export was not successful", report.wasSuccessful(), is(true));
        reportHasWarningMessage(report.warnings(), TEST_MESSAGE);
        reportHasWarningMessage(report.warnings(), "warnings");
        verify(configurationMigrationManager).delegateToExportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void doExportWithNullPath() throws Exception {
        configurationMigrationManager = getConfigurationMigrationManager();

        configurationMigrationManager.doExport((Path) null);
    }

    @Test
    public void doExportFailsToCreateDirectory() throws Exception {
        configurationMigrationManager = getConfigurationMigrationManager();

        MigrationReport report = configurationMigrationManager.doExport(DDF_HOME.resolve(
                "//invalid-directory"));

        reportHasErrorOfTypeWithMessage(report,
                UnexpectedMigrationException.class,
                "unable to create directory");
    }

    @Test
    public void doExportRecordsErrorForMigrationException() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());

        doThrow(new MigrationException(TEST_MESSAGE)).when(configurationMigrationManager)
                .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));

        MigrationReport report = configurationMigrationManager.doExport(DDF_HOME.resolve(
                TEST_DIRECTORY));

        reportHasErrorOfTypeWithMessage(report, MigrationException.class, TEST_MESSAGE);
        verify(configurationMigrationManager).delegateToExportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
    }

    @Test
    public void doExportRecordsErrorForIOException() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());

        doThrow(IOException.class).when(configurationMigrationManager)
                .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));

        MigrationReport report = configurationMigrationManager.doExport(DDF_HOME.resolve(
                TEST_DIRECTORY));

        reportHasErrorOfTypeWithMessage(report,
                UnexpectedMigrationException.class,
                "failed closing file");
        verify(configurationMigrationManager).delegateToExportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
    }

    @Test
    public void doExportRecordsErrorForRuntimeException() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());

        doThrow(RuntimeException.class).when(configurationMigrationManager)
                .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));

        MigrationReport report = configurationMigrationManager.doExport(DDF_HOME.resolve(
                TEST_DIRECTORY));

        reportHasErrorOfTypeWithMessage(report,
                UnexpectedMigrationException.class,
                "internal error occurred");
        verify(configurationMigrationManager).delegateToExportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
    }

    @Test
    public void doImportWithStringSucceedsWithWarnings() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());
        MigrationReport mockReport = mock(MigrationReport.class);

        when(mockReport.warnings()).thenReturn(Stream.of(new MigrationWarning(TEST_MESSAGE)));
        doReturn(mockReport).when(configurationMigrationManager)
                .doImport(any(Path.class));

        Collection<MigrationWarning> warnings = configurationMigrationManager.doImport(
                TEST_DIRECTORY);

        reportHasWarningMessage(warnings.stream(), TEST_MESSAGE);
        verify(configurationMigrationManager).doImport(any(Path.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void doImportWithNullString() throws Exception {
        configurationMigrationManager = getConfigurationMigrationManager();

        configurationMigrationManager.doImport((String) null);
    }

    @Test(expected = MigrationException.class)
    public void doImportWithStringThrowsException() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());
        MigrationReport mockReport = mock(MigrationReport.class);

        doThrow(MigrationException.class).when(mockReport)
                .verifyCompletion();
        doReturn(mockReport).when(configurationMigrationManager)
                .doImport(any(Path.class));

        configurationMigrationManager.doImport(TEST_DIRECTORY);
    }

    @Test
    public void doImportSucceeds() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());

        expectExportDelegationIsSuccessful();
        expectImportDelegationIsSuccessful();

        MigrationReport report = configurationMigrationManager.doImport(DDF_HOME.resolve(
                TEST_DIRECTORY));

        assertThat("Import was not successful", report.wasSuccessful(), is(true));
        assertThat("Restart system property was not set",
                System.getProperty("karaf.restart.jvm"),
                equalTo("true"));
        verify(mockSystemService).reboot(any(String.class), any(SystemService.Swipe.class));
        verify(configurationMigrationManager).delegateToExportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
        verify(configurationMigrationManager).delegateToImportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
    }

    @Test
    public void doImportSucceedsWithWarnings() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());

        doAnswer(invocation -> {
            MigrationReport report = invocation.getArgument(0);
            report.record(new MigrationWarning(TEST_MESSAGE));
            return null;
        }).when(configurationMigrationManager)
                .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
        expectImportDelegationIsSuccessful();

        MigrationReport report = configurationMigrationManager.doImport(DDF_HOME.resolve(
                TEST_DIRECTORY));

        assertThat("Import was successful", report.wasSuccessful(), is(true));
        reportHasWarningMessage(report.warnings(), TEST_MESSAGE);
        reportHasWarningMessage(report.warnings(), "warnings");
        verify(configurationMigrationManager).delegateToExportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
        verify(configurationMigrationManager).delegateToImportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
        verifyZeroInteractions(mockSystemService);
    }

    @Test
    public void doImportSucceedsAndFailsToReboot() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());

        expectExportDelegationIsSuccessful();
        expectImportDelegationIsSuccessful();
        doThrow(Exception.class).when(mockSystemService)
                .reboot(any(String.class), any(SystemService.Swipe.class));

        MigrationReport report = configurationMigrationManager.doImport(DDF_HOME.resolve(
                TEST_DIRECTORY));

        assertThat("Import was successful", report.wasSuccessful(), is(true));
        assertThat("Restart system property was set",
                System.getProperty("karaf.restart.jvm"),
                equalTo("true"));
        verify(mockSystemService).reboot(any(String.class), any(SystemService.Swipe.class));
        verify(configurationMigrationManager).delegateToExportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
        verify(configurationMigrationManager).delegateToImportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
    }

    @Test
    public void doImportDowngradesExportErrors() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());

        doAnswer(invocation -> {
            MigrationReport report = invocation.getArgument(0);
            report.record(new MigrationException(TEST_MESSAGE));
            return null;
        }).when(configurationMigrationManager)
                .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
        expectImportDelegationIsSuccessful();

        MigrationReport report = configurationMigrationManager.doImport(DDF_HOME.resolve(
                TEST_DIRECTORY));

        assertThat("Import was successful", report.wasSuccessful(), is(true));
        reportHasWarningMessage(report.warnings(), TEST_MESSAGE);
        verify(configurationMigrationManager).delegateToExportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
        verify(configurationMigrationManager).delegateToImportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
        verifyZeroInteractions(mockSystemService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doImportWithNullPath() throws Exception {
        configurationMigrationManager = getConfigurationMigrationManager();

        configurationMigrationManager.doImport((Path) null);
    }

    @Test
    public void doImportRecordsErrorForMigrationException() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());

        expectExportDelegationIsSuccessful();
        doThrow(new MigrationException(TEST_MESSAGE)).when(configurationMigrationManager)
                .delegateToImportMigrationManager(any(MigrationReportImpl.class), any(Path.class));

        MigrationReport report = configurationMigrationManager.doImport(DDF_HOME.resolve(
                TEST_DIRECTORY));

        reportHasErrorOfTypeWithMessage(report, MigrationException.class, TEST_MESSAGE);
        verify(configurationMigrationManager).delegateToExportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
        verify(configurationMigrationManager).delegateToImportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
        verifyZeroInteractions(mockSystemService);
    }

    @Test
    public void doImportRecordsErrorForRuntimeException() throws Exception {
        configurationMigrationManager = spy(getConfigurationMigrationManager());

        expectExportDelegationIsSuccessful();
        doThrow(RuntimeException.class).when(configurationMigrationManager)
                .delegateToImportMigrationManager(any(MigrationReportImpl.class), any(Path.class));

        MigrationReport report = configurationMigrationManager.doImport(DDF_HOME.resolve(
                TEST_DIRECTORY));

        reportHasErrorOfTypeWithMessage(report,
                UnexpectedMigrationException.class,
                "internal error occurred");
        verify(configurationMigrationManager).delegateToExportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
        verify(configurationMigrationManager).delegateToImportMigrationManager(any(
                MigrationReportImpl.class), any(Path.class));
        verifyZeroInteractions(mockSystemService);
    }

    @Test
    public void getOptionalMigratableInfo() throws Exception {
        configurationMigrationManager = getConfigurationMigrationManager();
        assertThat("Has no data migratables",
                configurationMigrationManager.getOptionalMigratableInfo()
                        .size(),
                is(0));
    }

    private void expectExportDelegationIsSuccessful() throws IOException {
        doNothing().when(configurationMigrationManager)
                .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
    }

    private void expectImportDelegationIsSuccessful() {
        doReturn(mock(ImportMigrationManagerImpl.class)).when(configurationMigrationManager)
                .delegateToImportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
    }

    private void reportHasWarningMessage(Stream<MigrationWarning> warnings, String message) {
        warnings.filter((w) -> w.getMessage()
                .contains(message))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "There is no matching warning in the migration report"));
    }

    private void reportHasErrorOfTypeWithMessage(MigrationReport report,
            Class<? extends MigrationException> exceptionClass, String message) {
        assertThat("Report has an error message", report.hasErrors(), is(true));
        MigrationException exception = report.errors()
                .findFirst()
                .orElseThrow(() -> new AssertionError("There is no error in the migration report"));

        assertThat(exceptionClass.equals(exception.getClass()), is(true));
        assertThat(exception.getMessage()
                .contains(message), is(true));
    }

    private ConfigurationMigrationManager getConfigurationMigrationManager() throws IOException {
        File versionFile = new File(DDF_HOME.resolve("Version.txt")
                .toString());
        versionFile.createNewFile();
        Files.write(versionFile.toPath(), TEST_VERSION.getBytes(), StandardOpenOption.APPEND);

        return new ConfigurationMigrationManager(mockMBeanServer,
                configurationMigratables,
                dataMigratables,
                mockSystemService);
    }
}
