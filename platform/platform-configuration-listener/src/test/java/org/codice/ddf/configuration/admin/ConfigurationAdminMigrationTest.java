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
package org.codice.ddf.configuration.admin;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.configuration.status.ConfigurationFileException;
import org.codice.ddf.configuration.status.ConfigurationStatus;
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
@PrepareForTest({ConfigurationAdminMigration.class, FileUtils.class})
public class ConfigurationAdminMigrationTest {

    private static final String CONFIG_FILE_PATH = "/root/etc/exported/etc/pid.config";

    private static final String CONFIG_PID = "pid";

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Mock
    private DirectoryStream<Path> configurationDirectoryStream;

    @Mock
    private DirectoryStream<Path> failedDirectoryStream;

    @Mock
    private Iterator<Path> failedDirectoryStreamIterator;

    private Path configFilesExportPath = Paths.get("/root/etc/exported/etc");

    @Mock
    private Path processedDirectoryPath;

    @Mock
    private File processedDirectory;

    private Path configFileInProcessedDirectory =
            Paths.get("/path/to/processed/myConfigFile.config");

    @Mock
    private Path failedDirectoryPath;

    @Mock
    private File failedDirectory;

    private Path configFileInFailedDirectory = Paths.get("/path/to/failed/myConfigFile.config");

    @Mock
    private Path exportedDirectoryPath;

    @Mock
    private File exportedDirectory;

    @Mock
    private ConfigurationFileFactory configurationFileFactory;

    @Mock
    private ConfigurationFilesPoller configurationFilePoller;

    @Mock
    private ConfigurationAdmin configurationAdmin;

    private String configurationFileExtension = ".config";

    @Mock
    private Iterator<Path> configFilesIterator;

    @Mock
    private Path configPath1;

    @Mock
    private Path configPath2;

    private Path configFilePath = Paths.get(CONFIG_FILE_PATH);

    @Mock
    private ConfigurationFile configFile1;

    @Mock
    private ConfigurationFile configFile2;

    @Mock
    private Configuration configuration;

    @Mock
    private ConfigurationStatus configStatus1;

    @Mock
    private ConfigurationStatus configStatus2;

    // TODO - Remove class and use PowerMock if needed
    private static class ConfigurationAdminMigrationUnderTest extends ConfigurationAdminMigration {

        public final Map<Path, Path> filesMoved = new HashMap<>();

        public ConfigurationAdminMigrationUnderTest(DirectoryStream<Path> configurationDirectory,
                Path processedDirectory, Path failedDirectory,
                ConfigurationFileFactory configurationFileFactory, ConfigurationFilesPoller poller,
                ConfigurationAdmin configurationAdmin, String configurationFileExtension) {
            super(configurationDirectory,
                    processedDirectory,
                    failedDirectory,
                    configurationFileFactory,
                    poller,
                    configurationAdmin,
                    configurationFileExtension);
        }

        @Override
        void moveFile(Path source, Path destination) throws IOException {
            filesMoved.put(source, destination);
        }
    }

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Files.class);
        PowerMockito.mockStatic(FileUtils.class);

        when(exportedDirectoryPath.resolve("etc")).thenReturn(configFilesExportPath);
        when(configPath1.toString()).thenReturn(CONFIG_FILE_PATH);

        when(configuration.getPid()).thenReturn(CONFIG_PID);
        when(processedDirectoryPath.toFile()).thenReturn(processedDirectory);
        when(failedDirectoryPath.toFile()).thenReturn(failedDirectory);
        when(exportedDirectoryPath.toFile()).thenReturn(exportedDirectory);
        when(configFile1.getConfigFilePath()).thenReturn(configPath1);
        when(configFile2.getConfigFilePath()).thenReturn(configPath2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDirectoryStream() {
        new ConfigurationAdminMigration(null,
                processedDirectoryPath,
                failedDirectoryPath,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                configurationFileExtension);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullProcessedDirectory() {
        new ConfigurationAdminMigration(configurationDirectoryStream,
                null,
                failedDirectoryPath,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                configurationFileExtension);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullFailedDirectory() {
        new ConfigurationAdminMigration(configurationDirectoryStream,
                processedDirectoryPath,
                null,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                configurationFileExtension);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationFileFactory() {
        new ConfigurationAdminMigration(configurationDirectoryStream,
                processedDirectoryPath,
                failedDirectoryPath,
                null,
                configurationFilePoller,
                configurationAdmin,
                configurationFileExtension);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationFilePoller() {
        new ConfigurationAdminMigration(configurationDirectoryStream,
                processedDirectoryPath,
                failedDirectoryPath,
                configurationFileFactory,
                null,
                configurationAdmin,
                configurationFileExtension);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationAdmin() {
        new ConfigurationAdminMigration(configurationDirectoryStream,
                processedDirectoryPath,
                failedDirectoryPath,
                configurationFileFactory,
                configurationFilePoller,
                null,
                configurationFileExtension);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationFileExtension() {
        new ConfigurationAdminMigration(configurationDirectoryStream,
                processedDirectoryPath,
                failedDirectoryPath,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                null);
    }

    @Test
    public void testInitDoesNotCreateExistingProcessedAndFailedAndExportedDirectories()
            throws IOException {
        when(processedDirectory.exists()).thenReturn(true);
        when(processedDirectory.isDirectory()).thenReturn(true);
        when(failedDirectory.exists()).thenReturn(true);
        when(failedDirectory.isDirectory()).thenReturn(true);

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.init();

        verify(processedDirectory, never()).mkdir();
        verify(failedDirectory, never()).mkdir();
        verify(configurationDirectoryStream).close();
    }

    @Test(expected = IOException.class)
    public void testInitFailsWhenProcessedDirectoryExistsButIsNotADirectory() throws IOException {
        when(processedDirectory.exists()).thenReturn(true);
        when(processedDirectory.isDirectory()).thenReturn(false);
        when(failedDirectory.exists()).thenReturn(true);
        when(failedDirectory.isDirectory()).thenReturn(true);

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.init();
        verify(configurationDirectoryStream).close();
    }

    @Test(expected = IOException.class)
    public void testInitFailsWhenFailedDirectoryExistsButIsNotADirectory() throws IOException {
        when(processedDirectory.exists()).thenReturn(true);
        when(processedDirectory.isDirectory()).thenReturn(true);
        when(failedDirectory.exists()).thenReturn(true);
        when(failedDirectory.isDirectory()).thenReturn(false);

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.init();
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitCreatesProcessedDirectory() throws IOException {
        when(processedDirectory.exists()).thenReturn(false);
        when(processedDirectory.mkdir()).thenReturn(true);
        when(failedDirectory.exists()).thenReturn(true);
        when(failedDirectory.isDirectory()).thenReturn(true);

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.init();

        verify(processedDirectory).mkdir();
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitCreatesFailedDirectory() throws IOException {
        when(processedDirectory.exists()).thenReturn(true);
        when(processedDirectory.isDirectory()).thenReturn(true);
        when(failedDirectory.exists()).thenReturn(false);
        when(failedDirectory.mkdir()).thenReturn(true);

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.init();

        verify(failedDirectory).mkdir();
        verify(configurationDirectoryStream).close();
    }

    @Test(expected = IOException.class)
    public void testInitFailsToCreateProcessedDirectory() throws IOException {
        when(processedDirectory.exists()).thenReturn(false);
        when(processedDirectory.mkdir()).thenReturn(false);
        when(failedDirectory.exists()).thenReturn(false);

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.init();
        verify(configurationDirectoryStream).close();
    }

    @Test(expected = IOException.class)
    public void testInitFailsToCreateFailedDirectory() throws IOException {
        when(processedDirectory.exists()).thenReturn(false);
        when(failedDirectory.exists()).thenReturn(false);
        when(failedDirectory.mkdir()).thenReturn(false);

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.init();
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitRegistersForEvents() throws IOException {
        setUpDefaultDirectoryExpectations();

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.init();

        verify(configurationFilePoller).register(configurationAdminMigration);
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitNoFiles() throws Exception {
        setUpDefaultDirectoryExpectations();

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory, never()).createConfigurationFile(any(Path.class));
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitConfigurationFileCreationWhenMultipleFilesExist() throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator(configurationDirectoryStream);
        setUpConfigurationFileFactoryForTwoFiles();

        ConfigurationAdminMigrationUnderTest configurationAdminMigrator =
                new ConfigurationAdminMigrationUnderTest(configurationDirectoryStream,
                        processedDirectoryPath,
                        failedDirectoryPath,
                        configurationFileFactory,
                        configurationFilePoller,
                        configurationAdmin,
                        configurationFileExtension);

        configurationAdminMigrator.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1).createConfig();
        verify(configurationFileFactory).createConfigurationFile(configPath2);
        verify(configFile2).createConfig();
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitMovesFilesToProcessedDirectory() throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator(configurationDirectoryStream);
        setUpConfigurationFileFactoryForTwoFiles();

        ConfigurationAdminMigrationUnderTest configurationAdminMigrator =
                new ConfigurationAdminMigrationUnderTest(configurationDirectoryStream,
                        processedDirectoryPath,
                        failedDirectoryPath,
                        configurationFileFactory,
                        configurationFilePoller,
                        configurationAdmin,
                        configurationFileExtension);

        configurationAdminMigrator.init();

        assertThat("Too many files moved",
                configurationAdminMigrator.filesMoved.keySet(),
                hasSize(2));
        assertThat("Configuration file 1 not moved to the processed directory",
                configurationAdminMigrator.filesMoved,
                hasEntry(configPath1, processedDirectoryPath));
        assertThat("Configuration file 2 not moved to the processed directory",
                configurationAdminMigrator.filesMoved,
                hasEntry(configPath2, processedDirectoryPath));
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitProcessesAllFilesEvenIfFirstOneHasAnInvalidType() throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator(configurationDirectoryStream);

        when(configurationFileFactory.createConfigurationFile(configPath1)).thenThrow(new ConfigurationFileException(
                ""));
        when(configurationFileFactory.createConfigurationFile(configPath2)).thenReturn(configFile2);

        ConfigurationAdminMigrationUnderTest configurationAdminMigrator =
                new ConfigurationAdminMigrationUnderTest(configurationDirectoryStream,
                        processedDirectoryPath,
                        failedDirectoryPath,
                        configurationFileFactory,
                        configurationFilePoller,
                        configurationAdmin,
                        configurationFileExtension);

        configurationAdminMigrator.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1, never()).createConfig();
        verify(configurationFileFactory).createConfigurationFile(configPath2);
        verify(configFile2).createConfig();
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitProcessesAllFilesEvenIfFirstOneCannotBeRead() throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator(configurationDirectoryStream);
        setUpConfigurationFileFactoryForTwoFiles();

        doThrow(new ConfigurationFileException("")).when(configFile1)
                .createConfig();

        ConfigurationAdminMigrationUnderTest configurationAdminMigrator =
                new ConfigurationAdminMigrationUnderTest(configurationDirectoryStream,
                        processedDirectoryPath,
                        failedDirectoryPath,
                        configurationFileFactory,
                        configurationFilePoller,
                        configurationAdmin,
                        configurationFileExtension);

        configurationAdminMigrator.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configurationFileFactory).createConfigurationFile(configPath2);
        verify(configFile2).createConfig();
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitMovesFilesWithAnInvalidTypeToFailedDirectory() throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator(configurationDirectoryStream);

        when(configurationFileFactory.createConfigurationFile(configPath1)).thenThrow(new ConfigurationFileException(
                ""));
        when(configurationFileFactory.createConfigurationFile(configPath2)).thenThrow(new ConfigurationFileException(
                ""));

        ConfigurationAdminMigrationUnderTest configurationAdminMigrator =
                new ConfigurationAdminMigrationUnderTest(configurationDirectoryStream,
                        processedDirectoryPath,
                        failedDirectoryPath,
                        configurationFileFactory,
                        configurationFilePoller,
                        configurationAdmin,
                        configurationFileExtension);

        configurationAdminMigrator.init();

        assertThat("Too many files moved",
                configurationAdminMigrator.filesMoved.keySet(),
                hasSize(2));
        assertThat("Configuration file 1 not moved to the failed directory",
                configurationAdminMigrator.filesMoved,
                hasEntry(configPath1, failedDirectoryPath));
        assertThat("Configuration file 2 not moved to the failed directory",
                configurationAdminMigrator.filesMoved,
                hasEntry(configPath2, failedDirectoryPath));
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitMovesFilesThatCannotBeReadToFailedDirectory() throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator(configurationDirectoryStream);
        setUpConfigurationFileFactoryForTwoFiles();

        doThrow(new ConfigurationFileException("")).when(configFile1)
                .createConfig();
        doThrow(new ConfigurationFileException("")).when(configFile2)
                .createConfig();

        ConfigurationAdminMigrationUnderTest configurationAdminMigrator =
                new ConfigurationAdminMigrationUnderTest(configurationDirectoryStream,
                        processedDirectoryPath,
                        failedDirectoryPath,
                        configurationFileFactory,
                        configurationFilePoller,
                        configurationAdmin,
                        configurationFileExtension);

        configurationAdminMigrator.init();

        assertThat("Too many files moved",
                configurationAdminMigrator.filesMoved.keySet(),
                hasSize(2));
        assertThat("Configuration file 1 not moved to the failed directory",
                configurationAdminMigrator.filesMoved,
                hasEntry(configPath1, failedDirectoryPath));
        assertThat("Configuration file 2 not moved to the failed directory",
                configurationAdminMigrator.filesMoved,
                hasEntry(configPath2, failedDirectoryPath));
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitStillProcessesFilesWhenTheyCannotBeMovedToProcessedDirectory()
            throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator(configurationDirectoryStream);
        setUpConfigurationFileFactoryForTwoFiles();

        ConfigurationAdminMigration configurationAdminMigration = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                processedDirectoryPath,
                failedDirectoryPath,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                configurationFileExtension) {
            @Override
            void moveFile(Path source, Path destination) throws IOException {
                throw new IOException();
            }
        };

        configurationAdminMigration.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1).createConfig();
        verify(configurationFileFactory).createConfigurationFile(configPath2);
        verify(configFile2).createConfig();
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testInitStillProcessesFilesWhenTheyCannotBeMovedToFailedDirectory()
            throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator(configurationDirectoryStream);

        when(configurationFileFactory.createConfigurationFile(configPath1)).thenThrow(new ConfigurationFileException(
                ""));
        when(configurationFileFactory.createConfigurationFile(configPath2)).thenThrow(new ConfigurationFileException(
                ""));

        ConfigurationAdminMigration configurationAdminMigration = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                processedDirectoryPath,
                failedDirectoryPath,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                configurationFileExtension) {
            @Override
            void moveFile(Path source, Path destination) throws IOException {
                throw new IOException();
            }
        };

        configurationAdminMigration.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configurationFileFactory).createConfigurationFile(configPath2);
        verify(configurationDirectoryStream).close();
    }

    @Test
    public void testNotifyWithValidFile() throws Exception {
        when(configurationFileFactory.createConfigurationFile(configPath1)).thenReturn(configFile1);
        when(processedDirectoryPath.resolve(configFilePath)).thenReturn(
                configFileInProcessedDirectory);

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorForNotify();

        configurationAdminMigration.notify(configPath1);

        verifyStatic();
        FileUtils.deleteQuietly(configFileInFailedDirectory.toFile());

        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1).createConfig();

        verifyStatic(times(1));
        Files.move(configPath1, configFileInProcessedDirectory, REPLACE_EXISTING);
    }

    @Test
    public void testNotifyWithFileThatHasAnInvalidType() throws Exception {
        when(configurationFileFactory.createConfigurationFile(configPath1)).thenThrow(new ConfigurationFileException(
                ""));
        when(failedDirectoryPath.resolve(configFilePath)).thenReturn(configFileInFailedDirectory);

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorForNotify();

        configurationAdminMigration.notify(configPath1);

        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1, never()).createConfig();

        verifyStatic(times(1));
        Files.move(configPath1, configFileInFailedDirectory, REPLACE_EXISTING);
    }

    @Test
    public void testNotifyWithFileThatCannotBeRead() throws Exception {
        when(configurationFileFactory.createConfigurationFile(configPath1)).thenReturn(configFile1);
        doThrow(new ConfigurationFileException("")).when(configFile1)
                .createConfig();

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorForNotify();

        configurationAdminMigration.notify(configPath1);

        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1).createConfig();

        verifyStatic(times(1));
        Files.move(configPath1, configFileInFailedDirectory, REPLACE_EXISTING);
    }

    @Test
    public void testNotifyWhenFileReadThrowsRuntimeException() throws Exception {
        when(configurationFileFactory.createConfigurationFile(configPath1)).thenReturn(configFile1);
        doThrow(new RuntimeException()).when(configFile1)
                .createConfig();

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorForNotify();

        configurationAdminMigration.notify(configPath1);

        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1).createConfig();

        verifyStatic(times(1));
        Files.move(configPath1, configFileInFailedDirectory, REPLACE_EXISTING);
    }

    @Test
    public void testNotifyFailsToMoveFileToProcessedDirectory() throws Exception {
        when(configurationFileFactory.createConfigurationFile(configPath1)).thenReturn(configFile1);
        when(processedDirectoryPath.resolve(configFilePath)).thenReturn(
                configFileInProcessedDirectory);
        when(Files.move(configPath1, configFileInProcessedDirectory)).thenThrow(new IOException());

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorForNotify();

        configurationAdminMigration.notify(configPath1);

        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1).createConfig();
    }

    @Test
    public void testNotifyFailsToMoveFileToFailedDirectory() throws Exception {
        when(configurationFileFactory.createConfigurationFile(configPath1)).thenReturn(configFile1);
        doThrow(new ConfigurationFileException("")).when(configFile1)
                .createConfig();
        when(Files.move(configPath1, configFileInFailedDirectory)).thenThrow(new IOException());

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorForNotify();

        configurationAdminMigration.notify(configPath1);

        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1).createConfig();
    }

    @Test
    public void testGetFailedConfigurationFilesTwoFilesInFailedDirectory() throws Exception {
        when(configPath1.getFileName()).thenReturn(configPath1);
        when(configPath2.getFileName()).thenReturn(configPath2);
        Path[] expectedPaths = new Path[2];
        expectedPaths[0] = configPath1;
        expectedPaths[1] = configPath2;

        setUpTwoConfigFileIterator(failedDirectoryStream);
        ConfigurationAdminMigration configurationAdminMigration = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                processedDirectoryPath,
                failedDirectoryPath,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                configurationFileExtension) {
            @Override
            DirectoryStream<Path> getFailedDirectoryStream() throws IOException {
                return failedDirectoryStream;
            }
        };

        Collection<ConfigurationStatus> configurationStatusMessages =
                configurationAdminMigration.getFailedConfigurationFiles();
        Collection<Path> actualPaths = new ArrayList<>();
        for (ConfigurationStatus configStatus : configurationStatusMessages) {
            actualPaths.add(configStatus.getPath());
        }
        assertThat(
                "Incorrect number for files returned from configurationAdminMigration.getFailedConfigurationFiles()",
                configurationStatusMessages.size(),
                is(2));
        assertThat(
                "Incorrect files returned from configurationAdminMigration.getFailedConfigurationFiles()",
                actualPaths,
                hasItems(expectedPaths));
    }

    @Test
    public void testGetFailedConfigurationFilesNoFilesInFailedDirectory() throws Exception {
        when(failedDirectoryStreamIterator.hasNext()).thenReturn(false);
        when(failedDirectoryStream.iterator()).thenReturn(failedDirectoryStreamIterator);
        ConfigurationAdminMigration configurationAdminMigration = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                processedDirectoryPath,
                failedDirectoryPath,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                configurationFileExtension) {
            @Override
            DirectoryStream<Path> getFailedDirectoryStream() throws IOException {
                return failedDirectoryStream;
            }
        };

        Collection<ConfigurationStatus> configurationStatusMessages =
                configurationAdminMigration.getFailedConfigurationFiles();

        assertThat("The failed directory does not contain the correct number of files",
                configurationStatusMessages,
                is(empty()));
    }

    @Test(expected = IOException.class)
    public void testGetFailedConfigurationFilesDirectoryStreamThrowsException() throws Exception {
        ConfigurationAdminMigration configurationAdminMigration = new ConfigurationAdminMigration(
                configurationDirectoryStream,
                processedDirectoryPath,
                failedDirectoryPath,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                configurationFileExtension) {
            @Override
            DirectoryStream<Path> getFailedDirectoryStream() throws IOException {
                throw new IOException("IOException");
            }
        };

        configurationAdminMigration.getFailedConfigurationFiles();
    }

    @Test
    public void testExport()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {
        setUpDefaultDirectoryExpectations();

        when(configurationAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {
                configuration});
        when(configurationFileFactory.createConfigurationFile((Dictionary<String, Object>) anyObject())).thenReturn(
                configFile1);

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();

        configurationAdminMigration.export(exportedDirectoryPath);

        verifyStatic();
        FileUtils.forceMkdir(configFilesExportPath.toFile());

        verify(configFile1, atLeastOnce()).exportConfig(CONFIG_FILE_PATH);
    }

    @Test(expected = IOException.class)
    public void testExportWhenEtcDirectoryCreationFails() throws Exception {
        PowerMockito.doThrow(new IOException())
                .when(FileUtils.class);
        FileUtils.forceMkdir(configFilesExportPath.toFile());

        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();

        configurationAdminMigration.export(exportedDirectoryPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportWithNullExportDirectory() throws IOException, ConfigurationFileException {
        setUpDefaultDirectoryExpectations();
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.export(null);
    }

    @Test(expected = IOException.class)
    public void testExportListConfigurationsIOException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {
        setUpDefaultDirectoryExpectations();
        doThrow(new IOException()).when(configurationAdmin)
                .listConfigurations(anyString());
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.export(exportedDirectoryPath);
    }

    @Test(expected = ConfigurationFileException.class)
    public void testExportListConfigurationsInvalidSyntaxException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {
        setUpDefaultDirectoryExpectations();
        doThrow(new InvalidSyntaxException("", "")).when(configurationAdmin)
                .listConfigurations(anyString());
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.export(exportedDirectoryPath);
    }

    @Test(expected = ConfigurationFileException.class)
    public void testExportConfigurationFileException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {
        setUpDefaultDirectoryExpectations();
        when(configurationAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {
                configuration});
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        when(configurationFileFactory.createConfigurationFile((Dictionary<String, Object>) anyObject())).thenThrow(
                new ConfigurationFileException(""));
        configurationAdminMigration.export(exportedDirectoryPath);
    }

    @Test(expected = IOException.class)
    public void testConfigFileExportConfigIOException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {
        setUpDefaultDirectoryExpectations();
        when(configurationAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {
                configuration});
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        when(configurationFileFactory.createConfigurationFile((Dictionary<String, Object>) anyObject())).thenReturn(
                configFile1);
        doThrow(new IOException()).when(configFile1)
                .exportConfig(anyString());
        configurationAdminMigration.export(exportedDirectoryPath);
    }

    @Test
    public void testExportWhenNoConfigurations()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {
        setUpDefaultDirectoryExpectations();
        when(configurationAdmin.listConfigurations(anyString())).thenReturn(null);
        ConfigurationAdminMigration configurationAdminMigration =
                createConfigurationAdminMigratorWithNoFiles();
        configurationAdminMigration.export(exportedDirectoryPath);
        verify(configurationFileFactory,
                never()).createConfigurationFile((Dictionary<String, Object>) anyObject());
    }

    private ConfigurationAdminMigration createConfigurationAdminMigratorWithNoFiles() {
        when(configurationDirectoryStream.iterator()).thenReturn(configFilesIterator);
        when(configFilesIterator.hasNext()).thenReturn(false);

        return new ConfigurationAdminMigration(configurationDirectoryStream,
                processedDirectoryPath,
                failedDirectoryPath,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                configurationFileExtension);
    }

    private ConfigurationAdminMigration createConfigurationAdminMigratorForNotify() {
        when(failedDirectoryPath.resolve(configFilePath)).thenReturn(configFileInFailedDirectory);
        when(configPath1.getFileName()).thenReturn(configFilePath);

        return new ConfigurationAdminMigration(configurationDirectoryStream,
                processedDirectoryPath,
                failedDirectoryPath,
                configurationFileFactory,
                configurationFilePoller,
                configurationAdmin,
                configurationFileExtension);
    }

    private void setUpDefaultDirectoryExpectations() {
        when(processedDirectory.exists()).thenReturn(false);
        when(processedDirectory.mkdir()).thenReturn(true);
        when(failedDirectory.exists()).thenReturn(false);
        when(failedDirectory.mkdir()).thenReturn(true);
    }

    private void setUpTwoConfigFileIterator(DirectoryStream<Path> stream) {
        when(stream.iterator()).thenReturn(configFilesIterator);
        when(configFilesIterator.hasNext()).thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);
        when(configFilesIterator.next()).thenReturn(configPath1)
                .thenReturn(configPath2);
    }

    private void setUpConfigurationFileFactoryForTwoFiles() throws ConfigurationFileException {
        when(configurationFileFactory.createConfigurationFile(configPath1)).thenReturn(configFile1);
        when(configurationFileFactory.createConfigurationFile(configPath2)).thenReturn(configFile2);
    }
}
