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
package org.codice.ddf.configuration.admin;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.configuration.status.ConfigurationFileException;
import org.codice.ddf.configuration.status.MigrationException;
import org.codice.ddf.configuration.status.MigrationWarning;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@RunWith(MockitoJUnitRunner.class)
@PrepareForTest({ConfigurationAdminMigration.class, FileUtils.class, Files.class})
public class ConfigurationAdminMigrationTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private static final Path CONFIG_FILES_EXPORT_PATH = Paths.get("/root/etc/exported/etc");

    private static final Path PROCESSED_DIRECTORY_PATH = Paths.get("/root/etc/processed");

    private static final Path CONFIG_FILE_IN_PROCESSED_DIRECTORY = Paths.get("/root/etc/processed");

    private static final Path FAILED_DIRECTORY_PATH = Paths.get("/root/etc/failed");

    private static final Path CONFIG_FILE_IN_FAILED_DIRECTORY = Paths.get("/root/etc/failed");

    private static final Path EXPORTED_DIRECTORY_PATH = Paths.get("/root/etc/exported");

    private static final String CONFIGURATION_FILE_EXTENSION = ".config";

    private static final String FILE_FILTER = "*" + CONFIGURATION_FILE_EXTENSION;

    private static final String CONFIG_PID = "pid";

    private static final Path CONFIG_FILE_PATH = Paths.get(
            "/root/etc/exported/etc/" + CONFIG_PID + CONFIGURATION_FILE_EXTENSION);

    private static final String CONFIG_FILE_PATH1 = CONFIG_PID + "1" + CONFIGURATION_FILE_EXTENSION;

    private static final String CONFIG_FILE_PATH2 = CONFIG_PID + "2" + CONFIGURATION_FILE_EXTENSION;

    private static final Path CONFIG_PATH1 = (Paths.get("/root/etc")).resolve(CONFIG_FILE_PATH1);

    private static final Path CONFIG_PATH2 = (Paths.get("/root/etc")).resolve(CONFIG_FILE_PATH2);

    @Mock
    private DirectoryStream<Path> configurationDirectoryStream;

    @Mock
    private DirectoryStream<Path> failedDirectoryStream;

    @Mock
    private Iterator<Path> failedDirectoryStreamIterator;

    @Mock
    private File exportedDirectory;

    @Mock
    private ConfigurationFileFactory configurationFileFactory;

    @Mock
    private ConfigurationFilesPoller configurationFilePoller;

    @Mock
    private ConfigurationAdmin configurationAdmin;

    @Mock
    private Iterator<Path> configFilesIterator;

    @Mock
    private ConfigurationFile configFile1;

    @Mock
    private ConfigurationFile configFile2;

    @Mock
    private Configuration configuration;

    @Mock
    private MigrationWarning configStatus1;

    @Mock
    private MigrationWarning configStatus2;

    @Mock
    private Path failedDirectory;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Files.class);
        PowerMockito.mockStatic(FileUtils.class);

        when(configuration.getPid()).thenReturn(CONFIG_PID);
        when(configFile1.getConfigFilePath()).thenReturn(CONFIG_PATH1);
        when(configFile2.getConfigFilePath()).thenReturn(CONFIG_PATH2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDirectoryStream() {
        new ConfigurationAdminMigration(null,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullProcessedDirectory() {
        new ConfigurationAdminMigration(configurationDirectoryStream,
                null,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullFailedDirectory() {
        new ConfigurationAdminMigration(configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                null,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationFileFactory() {
        new ConfigurationAdminMigration(configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                null,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationFilePoller() {
        new ConfigurationAdminMigration(configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                null,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationAdmin() {
        new ConfigurationAdminMigration(configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                null,
                CONFIGURATION_FILE_EXTENSION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationFileExtension() {
        new ConfigurationAdminMigration(configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                null);
    }

    @Test(expected = IOException.class)
    public void testInitDoesNotCreateExistingProcessedAndFailedDirectories() throws IOException {

        PowerMockito.doThrow(new IOException())
                .when(FileUtils.class);
        FileUtils.forceMkdir(PROCESSED_DIRECTORY_PATH.toFile());

        ConfigurationAdminMigration configurationAdminMigration = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);

        configurationAdminMigration.init();

        verify(configurationDirectoryStream).close();

    }

    @Test(expected = IOException.class)
    public void testInitFailsWhenCreatingProcessedDirectoryFails() throws IOException {
        PowerMockito.doThrow(new IOException())
                .when(FileUtils.class);
        FileUtils.forceMkdir(PROCESSED_DIRECTORY_PATH.toFile());

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();

        configurationAdminMigration.init();

        verifyStatic();
        FileUtils.forceMkdir(PROCESSED_DIRECTORY_PATH.toFile());
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitFailsWhenCreatingFailedDirectoryFails() throws IOException {

        try {
            PowerMockito.doThrow(new IOException())
                    .when(FileUtils.class);
            FileUtils.forceMkdir(FAILED_DIRECTORY_PATH.toFile());

            ConfigurationAdminMigration configurationAdminMigration =
                    createConfigurationAdminMigratorWithNoFiles();

            configurationAdminMigration.init();
        } catch (IOException e) {

            verifyStatic();
            FileUtils.forceMkdir(FAILED_DIRECTORY_PATH.toFile());
        }
    }

    @Test
    public void testInitCreatesProcessedDirectory() throws IOException {

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();

        configurationAdminMigration.init();

        verifyStatic();
        FileUtils.forceMkdir(PROCESSED_DIRECTORY_PATH.toFile());
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitCreatesFailedDirectory() throws IOException {

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.init();

        verifyStatic();
        FileUtils.forceMkdir(FAILED_DIRECTORY_PATH.toFile());
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitRegistersForEvents() throws IOException {

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.init();

        verify(configurationFilePoller).register(configurationAdminMigration);
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitNoFiles() throws Exception {

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory, never()).createConfigurationFile(any(Path.class));
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitConfigurationFileCreationWhenMultipleFilesExist() throws Exception {

        setUpTwoConfigFileIterator(configurationDirectoryStream);
        setUpConfigurationFileFactoryForTwoFiles();

        ConfigurationAdminMigration configurationAdminMigration = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);

        configurationAdminMigration.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH1);
        verify(configFile1).createConfig();
        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH2);
        verify(configFile2).createConfig();
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitMovesFilesToProcessedDirectory() throws Exception {

        setUpTwoConfigFileIterator(configurationDirectoryStream);
        setUpConfigurationFileFactoryForTwoFiles();

        when(Files.move(CONFIG_PATH1, PROCESSED_DIRECTORY_PATH, REPLACE_EXISTING)).thenReturn(
                PROCESSED_DIRECTORY_PATH);

        when(Files.move(CONFIG_PATH2, PROCESSED_DIRECTORY_PATH, REPLACE_EXISTING)).thenReturn(
                PROCESSED_DIRECTORY_PATH);

        ConfigurationAdminMigration configurationAdminMigrator = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);

        configurationAdminMigrator.init();

        verifyStatic();
        Files.move(CONFIG_PATH1,
                CONFIG_FILE_IN_PROCESSED_DIRECTORY.resolve(CONFIG_FILE_PATH1),
                REPLACE_EXISTING);
        Files.move(CONFIG_PATH2, CONFIG_FILE_IN_PROCESSED_DIRECTORY, REPLACE_EXISTING);
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitProcessesAllFilesEvenIfFirstOneHasAnInvalidType() throws Exception {

        setUpTwoConfigFileIterator(configurationDirectoryStream);

        when(configurationFileFactory.createConfigurationFile(CONFIG_PATH1)).thenThrow(new ConfigurationFileException(
                ""));
        when(configurationFileFactory.createConfigurationFile(CONFIG_PATH2)).thenReturn(configFile2);

        ConfigurationAdminMigration configurationAdminMigrator = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);

        configurationAdminMigrator.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH1);
        verify(configFile1, never()).createConfig();
        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH2);
        verify(configFile2).createConfig();
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitProcessesAllFilesEvenIfFirstOneCannotBeRead() throws Exception {

        setUpTwoConfigFileIterator(configurationDirectoryStream);
        setUpConfigurationFileFactoryForTwoFiles();

        doThrow(new ConfigurationFileException("")).when(configFile1)
                .createConfig();

        ConfigurationAdminMigration configurationAdminMigrator = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);

        configurationAdminMigrator.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH1);
        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH2);
        verify(configFile2).createConfig();
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitMovesFilesWithAnInvalidTypeToFailedDirectory() throws Exception {

        setUpTwoConfigFileIterator(configurationDirectoryStream);

        when(configurationFileFactory.createConfigurationFile(CONFIG_PATH1)).thenThrow(new ConfigurationFileException(
                ""));
        when(configurationFileFactory.createConfigurationFile(CONFIG_PATH2)).thenThrow(new ConfigurationFileException(
                ""));
        when(Files.move(CONFIG_PATH1, PROCESSED_DIRECTORY_PATH, REPLACE_EXISTING)).thenReturn(
                FAILED_DIRECTORY_PATH);
        when(Files.move(CONFIG_PATH2, PROCESSED_DIRECTORY_PATH, REPLACE_EXISTING)).thenReturn(
                FAILED_DIRECTORY_PATH);

        ConfigurationAdminMigration configurationAdminMigration = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);

        configurationAdminMigration.init();

        verifyStatic();
        Files.move(CONFIG_PATH1,
                CONFIG_FILE_IN_FAILED_DIRECTORY.resolve(CONFIG_FILE_PATH1),
                REPLACE_EXISTING);
        Files.move(CONFIG_PATH2,
                CONFIG_FILE_IN_FAILED_DIRECTORY.resolve(CONFIG_FILE_PATH2),
                REPLACE_EXISTING);
        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH1);
        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH2);
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitMovesFilesThatCannotBeReadToFailedDirectory() throws Exception {

        setUpTwoConfigFileIterator(configurationDirectoryStream);
        setUpConfigurationFileFactoryForTwoFiles();

        doThrow(new ConfigurationFileException("")).when(configFile1)
                .createConfig();
        doThrow(new ConfigurationFileException("")).when(configFile2)
                .createConfig();

        when(Files.move(CONFIG_PATH1, PROCESSED_DIRECTORY_PATH, REPLACE_EXISTING)).thenReturn(
                FAILED_DIRECTORY_PATH);

        when(Files.move(CONFIG_PATH2, PROCESSED_DIRECTORY_PATH, REPLACE_EXISTING)).thenReturn(
                FAILED_DIRECTORY_PATH);

        ConfigurationAdminMigration configurationAdminMigrator = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);

        configurationAdminMigrator.init();

        verifyStatic();
        Files.move(CONFIG_PATH1,
                CONFIG_FILE_IN_FAILED_DIRECTORY.resolve(CONFIG_FILE_PATH1),
                REPLACE_EXISTING);
        Files.move(CONFIG_PATH2,
                CONFIG_FILE_IN_FAILED_DIRECTORY.resolve(CONFIG_FILE_PATH2),
                REPLACE_EXISTING);
        verify(configurationDirectoryStream).close();
    }

    @Test(expected = IOException.class)
    public void testInitStillProcessesFilesWhenTheyCannotBeMovedToProcessedDirectory()
            throws Exception {

        PowerMockito.doThrow(new IOException())
                .when(Files.class);
        Files.newDirectoryStream(any(Path.class), any(String.class));

        setUpTwoConfigFileIterator(configurationDirectoryStream);
        setUpConfigurationFileFactoryForTwoFiles();

        when(Files.newDirectoryStream(any(Path.class), any(String.class))).thenReturn(
                failedDirectoryStream);

        ConfigurationAdminMigration configurationAdminMigration = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);

        configurationAdminMigration.init();

        verifyStatic();
        Files.move(any(Path.class), any(Path.class));
        Files.newDirectoryStream(any(Path.class), any(String.class));

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH1);
        verify(configFile1).createConfig();
        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH2);
        verify(configFile2).createConfig();
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitStillProcessesFilesWhenTheyCannotBeMovedToFailedDirectory()
            throws Exception {

        setUpTwoConfigFileIterator(configurationDirectoryStream);

        when(configurationFileFactory.createConfigurationFile(CONFIG_PATH1)).thenThrow(new ConfigurationFileException(
                ""));
        when(configurationFileFactory.createConfigurationFile(CONFIG_PATH2)).thenThrow(new ConfigurationFileException(
                ""));

        PowerMockito.doThrow(new IOException())
                .when(Files.class);
        Files.move(any(Path.class), any(Path.class), any(StandardCopyOption.class));

        ConfigurationAdminMigration configurationAdminMigration = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);

        configurationAdminMigration.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH1);
        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH2);
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testNotifyWhenFileIsReprocessed() throws Exception {

        when(configurationFileFactory.createConfigurationFile(CONFIG_PATH1)).thenReturn(configFile1);

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorForNotify();

        configurationAdminMigration.notify(CONFIG_PATH1);

        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH1);
        verify(configFile1).createConfig();
        verifyStatic();
        Files.move(Paths.get("/root/etc/" + CONFIG_FILE_PATH1),
                Paths.get(PROCESSED_DIRECTORY_PATH.resolve(CONFIG_FILE_PATH1)
                        .toString()),
                REPLACE_EXISTING);

    }

    @Test
    public void testNotifyWithFileThatHasAnInvalidType() throws Exception {
        when(configurationFileFactory.createConfigurationFile(CONFIG_PATH1)).thenThrow(new ConfigurationFileException(
                ""));

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorForNotify();

        configurationAdminMigration.notify(CONFIG_PATH1);

        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH1);
        verify(configFile1, never()).createConfig();

        verifyStatic(times(1));
        Files.move(CONFIG_PATH1,
                CONFIG_FILE_IN_FAILED_DIRECTORY.resolve(CONFIG_FILE_PATH1),
                REPLACE_EXISTING);
    }

    @Test
    public void testNotifyWithFileThatCannotBeRead() throws Exception {
        when(configurationFileFactory.createConfigurationFile(CONFIG_PATH1)).thenReturn(configFile1);
        doThrow(new ConfigurationFileException("")).when(configFile1)
                .createConfig();

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorForNotify();

        configurationAdminMigration.notify(CONFIG_PATH1);

        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH1);
        verify(configFile1).createConfig();

        verifyStatic(times(1));
        Files.move(CONFIG_PATH1,
                CONFIG_FILE_IN_FAILED_DIRECTORY.resolve(CONFIG_FILE_PATH1),
                REPLACE_EXISTING);
    }

    @Test
    public void testNotifyWhenFileReadThrowsRuntimeException() throws Exception {
        when(configurationFileFactory.createConfigurationFile(CONFIG_PATH1)).thenReturn(configFile1);
        doThrow(new RuntimeException()).when(configFile1)
                .createConfig();

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorForNotify();

        configurationAdminMigration.notify(CONFIG_PATH1);

        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH1);
        verify(configFile1).createConfig();

        verifyStatic(times(1));
        Files.move(CONFIG_PATH1,
                CONFIG_FILE_IN_FAILED_DIRECTORY.resolve(CONFIG_FILE_PATH1),
                REPLACE_EXISTING);
    }

    @Test
    public void testNotifyFailsToMoveFileToProcessedDirectory() throws Exception {

        when(configurationFileFactory.createConfigurationFile(CONFIG_PATH1)).thenReturn(configFile1);
        when(Files.move(CONFIG_PATH1,
                CONFIG_FILE_IN_PROCESSED_DIRECTORY)).thenThrow(new IOException());

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorForNotify();

        configurationAdminMigration.notify(CONFIG_PATH1);

        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH1);
        verify(configFile1).createConfig();
    }

    @Test
    public void testNotifyFailsToMoveFileToFailedDirectory() throws Exception {

        doThrow(new ConfigurationFileException("")).when(configFile1)
                .createConfig();

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorForNotify();

        configurationAdminMigration.notify(CONFIG_PATH1);

        verify(configurationFileFactory).createConfigurationFile(CONFIG_PATH1);

    }

    @Test
    public void testGetFailedConfigurationFilesTwoFilesInFailedDirectory() throws Exception {

        when(Files.newDirectoryStream(any(Path.class), anyString())).thenReturn(
                failedDirectoryStream);

        setUpTwoConfigFileIterator(failedDirectoryStream);

        ConfigurationAdminMigration configurationAdminMigration = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);

        Collection<MigrationWarning> configurationStatusMessages =
                configurationAdminMigration.getFailedConfigurationFiles();

        Collection<String> actualPaths = new ArrayList<>();
        for (MigrationWarning configStatus : configurationStatusMessages) {
            actualPaths.add(configStatus.getMessage());
        }

        verifyStatic();
        Files.newDirectoryStream(FAILED_DIRECTORY_PATH, FILE_FILTER);

        assertThat(
                "Incorrect number for files returned from configurationAdminMigration.getFailedConfigurationFiles()",
                configurationStatusMessages.size(),
                is(2));
    }

    @Test
    public void testGetFailedConfigurationFilesNoFilesInFailedDirectory() throws Exception {

        when(failedDirectoryStreamIterator.hasNext()).thenReturn(false);
        when(failedDirectoryStream.iterator()).thenReturn(failedDirectoryStreamIterator);
        when(Files.newDirectoryStream(any(Path.class), any(String.class))).thenReturn(
                failedDirectoryStream);

        ConfigurationAdminMigration configurationAdminMigration = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);

        Collection<MigrationWarning> configurationStatusMessages =
                configurationAdminMigration.getFailedConfigurationFiles();

        assertThat("The failed directory does not contain the correct number of files",
                configurationStatusMessages,
                is(empty()));
    }

    @Test(expected = IOException.class)
    public void testGetFailedConfigurationFilesDirectoryStreamThrowsException() throws Exception {

        PowerMockito.doThrow(new IOException())
                .when(Files.class);
        Files.newDirectoryStream(any(Path.class), any(String.class));

        ConfigurationAdminMigration configurationAdminMigration = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);

        configurationAdminMigration.getFailedConfigurationFiles();

        verifyStatic();
        Files.newDirectoryStream(any(Path.class), any(String.class));

    }

    @Test
    public void testExport()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {

        when(configurationAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {
                configuration});
        when(configurationFileFactory.createConfigurationFile((Dictionary<String, Object>) anyObject())).thenReturn(
                configFile1);

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();

        configurationAdminMigration.export(EXPORTED_DIRECTORY_PATH);

        verifyStatic();
        FileUtils.forceMkdir(CONFIG_FILES_EXPORT_PATH.toFile());
        verify(configFile1, atLeastOnce()).exportConfig(CONFIG_FILE_PATH.toString());
    }

    @Test
    public void testExportWhenEtcDirectoryCreationFails() throws Exception {

        try {
            PowerMockito.doThrow(new IOException())
                    .when(FileUtils.class);
            FileUtils.forceMkdir(CONFIG_FILES_EXPORT_PATH.toFile());

            ConfigurationAdminMigration configurationAdminMigration =
                    createConfigurationAdminMigratorWithNoFiles();

            configurationAdminMigration.export(EXPORTED_DIRECTORY_PATH);

        } catch (IOException e) {
            verifyStatic();
            FileUtils.forceMkdir(CONFIG_FILES_EXPORT_PATH.toFile());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportWithNullExportDirectory() throws IOException, ConfigurationFileException {

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.export(null);
    }

    @Test(expected = MigrationException.class)
    public void testExportListConfigurationsIOException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {

        doThrow(new IOException()).when(configurationAdmin)
                .listConfigurations(anyString());
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.export(EXPORTED_DIRECTORY_PATH);
    }

    @Test(expected = MigrationException.class)
    public void testExportListConfigurationsInvalidSyntaxException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {

        doThrow(new InvalidSyntaxException("", "")).when(configurationAdmin)
                .listConfigurations(anyString());
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.export(EXPORTED_DIRECTORY_PATH);
    }

    @Test(expected = MigrationException.class)
    public void testExportConfigurationFileException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {

        when(configurationAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {
                configuration});
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        when(configurationFileFactory.createConfigurationFile((Dictionary<String, Object>) anyObject())).thenThrow(
                new ConfigurationFileException(""));
        configurationAdminMigration.export(EXPORTED_DIRECTORY_PATH);
    }

    @Test(expected = MigrationException.class)
    public void testConfigFileExportConfigIOException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {

        when(configurationAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {
                configuration});
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        when(configurationFileFactory.createConfigurationFile((Dictionary<String, Object>) anyObject())).thenReturn(
                configFile1);
        doThrow(new IOException()).when(configFile1)
                .exportConfig(anyString());
        configurationAdminMigration.export(EXPORTED_DIRECTORY_PATH);
    }

    @Test
    public void testExportWhenNoConfigurations()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {

        when(configurationAdmin.listConfigurations(anyString())).thenReturn(null);
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.export(EXPORTED_DIRECTORY_PATH);
        verify(configurationFileFactory,
                never()).createConfigurationFile((Dictionary<String, Object>) anyObject());
    }

    private ConfigurationAdminMigration createConfigurationAdminMigratorWithNoFiles() {
        when(configurationDirectoryStream.iterator()).thenReturn(configFilesIterator);
        when(configFilesIterator.hasNext()).thenReturn(false);

        return new ConfigurationAdminMigration(configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);
    }

    private ConfigurationAdminMigration createConfigurationAdminMigratorForNotify() {

        return new ConfigurationAdminMigration(configurationDirectoryStream,
                PROCESSED_DIRECTORY_PATH,
                FAILED_DIRECTORY_PATH,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                CONFIGURATION_FILE_EXTENSION);
    }

    private void setUpTwoConfigFileIterator(DirectoryStream<Path> stream) {
        when(stream.iterator()).thenReturn(configFilesIterator);
        when(configFilesIterator.hasNext()).thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);
        when(configFilesIterator.next()).thenReturn(CONFIG_PATH1)
                .thenReturn(CONFIG_PATH2);
    }

    private void setUpConfigurationFileFactoryForTwoFiles() throws ConfigurationFileException {
        when(configurationFileFactory.createConfigurationFile(CONFIG_PATH1)).thenReturn(configFile1);
        when(configurationFileFactory.createConfigurationFile(CONFIG_PATH2)).thenReturn(configFile2);
    }
}
