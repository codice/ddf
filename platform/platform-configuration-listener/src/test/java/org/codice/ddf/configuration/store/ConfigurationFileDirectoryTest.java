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
package org.codice.ddf.configuration.store;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationFileDirectoryTest {

    @Mock
    private DirectoryStream<Path> configurationDirectoryStream;

    @Mock
    private Path processedDirectoryPath;

    @Mock
    private File processedDirectory;

    @Mock
    private Path failedDirectoryPath;

    @Mock
    private File failedDirectory;

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

    @Mock
    private ConfigurationFile configFile1;

    @Mock
    private ConfigurationFile configFile2;

    @Mock
    private Configuration configuration;

    private static class ConfigurationFileDirectoryUnderTest extends ConfigurationFileDirectory {

        public final Map<Path, Path> filesMoved = new HashMap<>();

        public ConfigurationFileDirectoryUnderTest(
                @NotNull DirectoryStream<Path> configurationDirectory, Path processedDirectory,
                Path failedDirectory, @NotNull ConfigurationFileFactory configurationFileFactory,
                @NotNull ConfigurationFilesPoller poller,
                @NotNull ConfigurationAdmin configurationAdmin,
                @NotNull String configurationFileExtension) {
            super(configurationDirectory, processedDirectory, failedDirectory,
                    configurationFileFactory, poller, configurationAdmin,
                    configurationFileExtension);
        }

        @Override
        void moveFile(Path source, Path destination) throws IOException {
            filesMoved.put(source, destination);
        }
    }

    @Before
    public void setUp() {
        when(processedDirectoryPath.toFile()).thenReturn(processedDirectory);
        when(failedDirectoryPath.toFile()).thenReturn(failedDirectory);
        when(exportedDirectoryPath.toFile()).thenReturn(exportedDirectory);
        when(configFile1.getConfigFilePath()).thenReturn(configPath1);
        when(configFile2.getConfigFilePath()).thenReturn(configPath2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDirectoryStream() {
        new ConfigurationFileDirectory(null, processedDirectoryPath, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullProcessedDirectory() {
        new ConfigurationFileDirectory(configurationDirectoryStream, null, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullFailedDirectory() {
        new ConfigurationFileDirectory(configurationDirectoryStream, processedDirectoryPath, null,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationFileFactory() {
        new ConfigurationFileDirectory(configurationDirectoryStream, processedDirectoryPath,
                failedDirectoryPath, null, configurationFilePoller, configurationAdmin,
                configurationFileExtension);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationFilePoller() {
        new ConfigurationFileDirectory(configurationDirectoryStream, processedDirectoryPath,
                failedDirectoryPath, configurationFileFactory, null, configurationAdmin,
                configurationFileExtension);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationAdmin() {
        new ConfigurationFileDirectory(configurationDirectoryStream, processedDirectoryPath,
                failedDirectoryPath, configurationFileFactory, configurationFilePoller, null,
                configurationFileExtension);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfigurationFileExtension() {
        new ConfigurationFileDirectory(configurationDirectoryStream, processedDirectoryPath,
                failedDirectoryPath, configurationFileFactory, configurationFilePoller,
                configurationAdmin, null);
    }

    @Test
    public void testInitDoesNotCreateExistingProcessedAndFailedAndExportedDirectories()
            throws IOException {
        when(processedDirectory.exists()).thenReturn(true);
        when(processedDirectory.isDirectory()).thenReturn(true);
        when(failedDirectory.exists()).thenReturn(true);
        when(failedDirectory.isDirectory()).thenReturn(true);

        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        configurationFileDirectory.init();

        verify(processedDirectory, never()).mkdir();
        verify(failedDirectory, never()).mkdir();
    }

    @Test(expected = IOException.class)
    public void testInitFailsWhenProcessedDirectoryExistsButIsNotADirectory() throws IOException {
        when(processedDirectory.exists()).thenReturn(true);
        when(processedDirectory.isDirectory()).thenReturn(false);
        when(failedDirectory.exists()).thenReturn(true);
        when(failedDirectory.isDirectory()).thenReturn(true);

        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        configurationFileDirectory.init();
    }

    @Test(expected = IOException.class)
    public void testInitFailsWhenFailedDirectoryExistsButIsNotADirectory() throws IOException {
        when(processedDirectory.exists()).thenReturn(true);
        when(processedDirectory.isDirectory()).thenReturn(true);
        when(failedDirectory.exists()).thenReturn(true);
        when(failedDirectory.isDirectory()).thenReturn(false);

        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        configurationFileDirectory.init();
    }

    @Test
    public void testInitCreatesProcessedDirectory() throws IOException {
        when(processedDirectory.exists()).thenReturn(false);
        when(processedDirectory.mkdir()).thenReturn(true);
        when(failedDirectory.exists()).thenReturn(true);
        when(failedDirectory.isDirectory()).thenReturn(true);

        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        configurationFileDirectory.init();

        verify(processedDirectory).mkdir();
    }

    @Test
    public void testInitCreatesFailedDirectory() throws IOException {
        when(processedDirectory.exists()).thenReturn(true);
        when(processedDirectory.isDirectory()).thenReturn(true);
        when(failedDirectory.exists()).thenReturn(false);
        when(failedDirectory.mkdir()).thenReturn(true);

        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        configurationFileDirectory.init();

        verify(failedDirectory).mkdir();
    }

    @Test(expected = IOException.class)
    public void testInitFailsToCreateProcessedDirectory() throws IOException {
        when(processedDirectory.exists()).thenReturn(false);
        when(processedDirectory.mkdir()).thenReturn(false);
        when(failedDirectory.exists()).thenReturn(false);

        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        configurationFileDirectory.init();
    }

    @Test(expected = IOException.class)
    public void testInitFailsToCreateFailedDirectory() throws IOException {
        when(processedDirectory.exists()).thenReturn(false);
        when(failedDirectory.exists()).thenReturn(false);
        when(failedDirectory.mkdir()).thenReturn(false);

        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        configurationFileDirectory.init();
    }

    @Test
    public void testInitRegistersForEvents() throws IOException {
        setUpDefaultDirectoryExpectations();

        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        configurationFileDirectory.init();

        verify(configurationFilePoller).register(configurationFileDirectory);
    }

    @Test
    public void testInitNoFiles() throws Exception {
        setUpDefaultDirectoryExpectations();

        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        configurationFileDirectory.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory, never()).createConfigurationFile((Path) anyObject());
    }

    @Test
    public void testInitConfigurationFileCreationWhenMultipleFilesExist() throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator();
        setUpConfigurationFileFactoryForTwoFiles();

        ConfigurationFileDirectoryUnderTest configurationFileDirectory = new ConfigurationFileDirectoryUnderTest(
                configurationDirectoryStream, processedDirectoryPath, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension);

        configurationFileDirectory.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1).createConfig();
        verify(configurationFileFactory).createConfigurationFile(configPath2);
        verify(configFile2).createConfig();
    }

    @Test
    public void testInitMovesFilesToProcessedDirectory() throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator();
        setUpConfigurationFileFactoryForTwoFiles();

        ConfigurationFileDirectoryUnderTest configurationFileDirectory = new ConfigurationFileDirectoryUnderTest(
                configurationDirectoryStream, processedDirectoryPath, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension);

        configurationFileDirectory.init();

        assertThat("Too many files moved", configurationFileDirectory.filesMoved.keySet(),
                hasSize(2));
        assertThat("Configuration file 1 not moved to the processed directory",
                configurationFileDirectory.filesMoved,
                hasEntry(configPath1, processedDirectoryPath));
        assertThat("Configuration file 2 not moved to the processed directory",
                configurationFileDirectory.filesMoved,
                hasEntry(configPath2, processedDirectoryPath));
    }

    @Test
    public void testInitProcessesAllFilesEvenIfFirstOneHasAnInvalidType() throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator();

        when(configurationFileFactory.createConfigurationFile(configPath1))
                .thenThrow(new ConfigurationFileException(""));
        when(configurationFileFactory.createConfigurationFile(configPath2)).thenReturn(configFile2);

        ConfigurationFileDirectoryUnderTest configurationFileDirectory = new ConfigurationFileDirectoryUnderTest(
                configurationDirectoryStream, processedDirectoryPath, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension);

        configurationFileDirectory.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1, never()).createConfig();
        verify(configurationFileFactory).createConfigurationFile(configPath2);
        verify(configFile2).createConfig();
    }

    @Test
    public void testInitProcessesAllFilesEvenIfFirstOneCannotBeRead() throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator();
        setUpConfigurationFileFactoryForTwoFiles();

        doThrow(new ConfigurationFileException("")).when(configFile1).createConfig();

        ConfigurationFileDirectoryUnderTest configurationFileDirectory = new ConfigurationFileDirectoryUnderTest(
                configurationDirectoryStream, processedDirectoryPath, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension);

        configurationFileDirectory.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configurationFileFactory).createConfigurationFile(configPath2);
        verify(configFile2).createConfig();
    }

    @Test
    public void testInitMovesFilesWithAnInvalidTypeToFailedDirectory() throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator();

        when(configurationFileFactory.createConfigurationFile(configPath1))
                .thenThrow(new ConfigurationFileException(""));
        when(configurationFileFactory.createConfigurationFile(configPath2))
                .thenThrow(new ConfigurationFileException(""));

        ConfigurationFileDirectoryUnderTest configurationFileDirectory = new ConfigurationFileDirectoryUnderTest(
                configurationDirectoryStream, processedDirectoryPath, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension);

        configurationFileDirectory.init();

        assertThat("Too many files moved", configurationFileDirectory.filesMoved.keySet(),
                hasSize(2));
        assertThat("Configuration file 1 not moved to the failed directory",
                configurationFileDirectory.filesMoved, hasEntry(configPath1, failedDirectoryPath));
        assertThat("Configuration file 2 not moved to the failed directory",
                configurationFileDirectory.filesMoved, hasEntry(configPath2, failedDirectoryPath));
    }

    @Test
    public void testInitMovesFilesThatCannotBeReadToFailedDirectory() throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator();
        setUpConfigurationFileFactoryForTwoFiles();

        doThrow(new ConfigurationFileException("")).when(configFile1).createConfig();
        doThrow(new ConfigurationFileException("")).when(configFile2).createConfig();

        ConfigurationFileDirectoryUnderTest configurationFileDirectory = new ConfigurationFileDirectoryUnderTest(
                configurationDirectoryStream, processedDirectoryPath, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension);

        configurationFileDirectory.init();

        assertThat("Too many files moved", configurationFileDirectory.filesMoved.keySet(),
                hasSize(2));
        assertThat("Configuration file 1 not moved to the failed directory",
                configurationFileDirectory.filesMoved, hasEntry(configPath1, failedDirectoryPath));
        assertThat("Configuration file 2 not moved to the failed directory",
                configurationFileDirectory.filesMoved, hasEntry(configPath2, failedDirectoryPath));
    }

    @Test
    public void testInitStillProcessesFilesWhenTheyCannotBeMovedToProcessedDirectory()
            throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator();
        setUpConfigurationFileFactoryForTwoFiles();

        ConfigurationFileDirectory configurationFileDirectory = new ConfigurationFileDirectory(
                configurationDirectoryStream, processedDirectoryPath, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension) {
            @Override
            void moveFile(Path source, Path destination) throws IOException {
                throw new IOException();
            }
        };

        configurationFileDirectory.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1).createConfig();
        verify(configurationFileFactory).createConfigurationFile(configPath2);
        verify(configFile2).createConfig();
    }

    @Test
    public void testInitStillProcessesFilesWhenTheyCannotBeMovedToFailedDirectory()
            throws Exception {
        setUpDefaultDirectoryExpectations();
        setUpTwoConfigFileIterator();

        when(configurationFileFactory.createConfigurationFile(configPath1))
                .thenThrow(new ConfigurationFileException(""));
        when(configurationFileFactory.createConfigurationFile(configPath2))
                .thenThrow(new ConfigurationFileException(""));

        ConfigurationFileDirectory configurationFileDirectory = new ConfigurationFileDirectory(
                configurationDirectoryStream, processedDirectoryPath, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension) {
            @Override
            void moveFile(Path source, Path destination) throws IOException {
                throw new IOException();
            }
        };

        configurationFileDirectory.init();

        verify(configurationDirectoryStream).iterator();
        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configurationFileFactory).createConfigurationFile(configPath2);
    }

    @Test
    public void testNotifyWithValidFile() throws Exception {
        when(configurationFileFactory.createConfigurationFile(configPath1)).thenReturn(configFile1);

        ConfigurationFileDirectoryUnderTest configurationFileDirectory = new ConfigurationFileDirectoryUnderTest(
                configurationDirectoryStream, processedDirectoryPath, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension);
        configurationFileDirectory.notify(configPath1);

        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1).createConfig();
        assertThat("Too many files moved", configurationFileDirectory.filesMoved.keySet(),
                hasSize(1));
        assertThat("Configuration file 1 not moved to the processed directory",
                configurationFileDirectory.filesMoved,
                hasEntry(configPath1, processedDirectoryPath));
    }

    @Test
    public void testNotifyWithFileThatHasAnInvalidType() throws Exception {
        when(configurationFileFactory.createConfigurationFile(configPath1))
                .thenThrow(new ConfigurationFileException(""));

        ConfigurationFileDirectoryUnderTest configurationFileDirectory = new ConfigurationFileDirectoryUnderTest(
                configurationDirectoryStream, processedDirectoryPath, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension);
        configurationFileDirectory.notify(configPath1);

        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1, never()).createConfig();
        assertThat("Too many files moved", configurationFileDirectory.filesMoved.keySet(),
                hasSize(1));
        assertThat("Configuration file 1 not moved to the failed directory",
                configurationFileDirectory.filesMoved, hasEntry(configPath1, failedDirectoryPath));
    }

    @Test
    public void testNotifyWithFileThatCannotBeRead() throws Exception {
        when(configurationFileFactory.createConfigurationFile(configPath1)).thenReturn(configFile1);
        doThrow(new ConfigurationFileException("")).when(configFile1).createConfig();

        ConfigurationFileDirectoryUnderTest configurationFileDirectory = new ConfigurationFileDirectoryUnderTest(
                configurationDirectoryStream, processedDirectoryPath, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension);
        configurationFileDirectory.notify(configPath1);

        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1).createConfig();
        assertThat("Too many files moved", configurationFileDirectory.filesMoved.keySet(),
                hasSize(1));
        assertThat("Configuration file 1 not moved to the failed directory",
                configurationFileDirectory.filesMoved, hasEntry(configPath1, failedDirectoryPath));
    }

    @Test
    public void testNotifyWhenFileReadThrowsRuntimeException() throws Exception {
        when(configurationFileFactory.createConfigurationFile(configPath1)).thenReturn(configFile1);
        doThrow(new RuntimeException()).when(configFile1)
                .createConfig();

        ConfigurationFileDirectoryUnderTest configurationFileDirectory =
                new ConfigurationFileDirectoryUnderTest(configurationDirectoryStream,
                        processedDirectoryPath,
                        failedDirectoryPath,
                        configurationFileFactory,
                        configurationFilePoller,
                        configurationAdmin,
                        configurationFileExtension);
        configurationFileDirectory.notify(configPath1);

        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1).createConfig();
        assertThat("Too many files moved",
                configurationFileDirectory.filesMoved.keySet(),
                hasSize(1));
        assertThat("Configuration file 1 not moved to the failed directory",
                configurationFileDirectory.filesMoved,
                hasEntry(configPath1, failedDirectoryPath));
    }

    @Test
    public void testNotifyFailsToMoveFileToProcessedDirectory() throws Exception {
        when(configurationFileFactory.createConfigurationFile(configPath1)).thenReturn(configFile1);

        ConfigurationFileDirectory configurationFileDirectory = new ConfigurationFileDirectory(
                configurationDirectoryStream, processedDirectoryPath, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension) {
            @Override
            void moveFile(Path source, Path destination) throws IOException {
                throw new IOException();
            }
        };

        configurationFileDirectory.notify(configPath1);

        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1).createConfig();
    }

    @Test
    public void testNotifyFailsToMoveFileToFailedDirectory() throws Exception {
        when(configurationFileFactory.createConfigurationFile(configPath1)).thenReturn(configFile1);
        doThrow(new ConfigurationFileException("")).when(configFile1).createConfig();

        ConfigurationFileDirectory configurationFileDirectory = new ConfigurationFileDirectory(
                configurationDirectoryStream, processedDirectoryPath, failedDirectoryPath,
                configurationFileFactory, configurationFilePoller, configurationAdmin,
                configurationFileExtension) {
            @Override
            void moveFile(Path source, Path destination) throws IOException {
                throw new IOException();
            }
        };

        configurationFileDirectory.notify(configPath1);

        verify(configurationFileFactory).createConfigurationFile(configPath1);
        verify(configFile1).createConfig();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportWithNullExportDirectory() throws IOException, ConfigurationFileException {
        setUpDefaultDirectoryExpectations();
        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        configurationFileDirectory.export(null);
    }

    @Test(expected = IOException.class)
    public void testExportListConfigurationsIOException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {
        setUpDefaultDirectoryExpectations();
        doThrow(new IOException()).when(configurationAdmin).listConfigurations(anyString());
        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        configurationFileDirectory.export(exportedDirectoryPath);
    }

    @Test(expected = ConfigurationFileException.class)
    public void testExportListConfigurationsInvalidSyntaxException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {
        setUpDefaultDirectoryExpectations();
        doThrow(new InvalidSyntaxException("", "")).when(configurationAdmin)
                .listConfigurations(anyString());
        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        configurationFileDirectory.export(exportedDirectoryPath);
    }

    @Test(expected = ConfigurationFileException.class)
    public void testExportConfigurationFileException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {
        setUpDefaultDirectoryExpectations();
        when(configurationAdmin.listConfigurations(anyString()))
                .thenReturn(new Configuration[] {configuration});
        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        when(configurationFileFactory
                .createConfigurationFile((Dictionary<String, Object>) anyObject()))
                .thenThrow(new ConfigurationFileException(""));
        configurationFileDirectory.export(exportedDirectoryPath);
    }

    @Test(expected = IOException.class)
    public void testConfigFileExportConfigIOException()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {
        setUpDefaultDirectoryExpectations();
        when(configurationAdmin.listConfigurations(anyString()))
                .thenReturn(new Configuration[] {configuration});
        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        when(configurationFileFactory
                .createConfigurationFile((Dictionary<String, Object>) anyObject()))
                .thenReturn(configFile1);
        doThrow(new IOException()).when(configFile1).exportConfig(anyString());
        configurationFileDirectory.export(exportedDirectoryPath);
    }

    @Test
    public void testExportWhenNoConfigurations()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {
        setUpDefaultDirectoryExpectations();
        when(configurationAdmin.listConfigurations(anyString())).thenReturn(null);
        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        configurationFileDirectory.export(exportedDirectoryPath);
        verify(configurationFileFactory, never())
                .createConfigurationFile((Dictionary<String, Object>) anyObject());
    }

    @Test
    public void testExport()
            throws IOException, InvalidSyntaxException, ConfigurationFileException {
        setUpDefaultDirectoryExpectations();
        when(configurationAdmin.listConfigurations(anyString()))
                .thenReturn(new Configuration[] {configuration});
        ConfigurationFileDirectory configurationFileDirectory = createConfigurationFileDirectoryWithNoFiles();
        when(configurationFileFactory
                .createConfigurationFile((Dictionary<String, Object>) anyObject()))
                .thenReturn(configFile1);
        configurationFileDirectory.export(exportedDirectoryPath);
        verify(configFile1, atLeastOnce()).exportConfig(anyString());
    }

    private ConfigurationFileDirectory createConfigurationFileDirectoryWithNoFiles() {
        when(configurationDirectoryStream.iterator()).thenReturn(configFilesIterator);
        when(configFilesIterator.hasNext()).thenReturn(false);

        return new ConfigurationFileDirectory(configurationDirectoryStream, processedDirectoryPath,
                failedDirectoryPath, configurationFileFactory, configurationFilePoller,
                configurationAdmin, configurationFileExtension);
    }

    private void setUpDefaultDirectoryExpectations() {
        when(processedDirectory.exists()).thenReturn(false);
        when(processedDirectory.mkdir()).thenReturn(true);
        when(failedDirectory.exists()).thenReturn(false);
        when(failedDirectory.mkdir()).thenReturn(true);
        when(exportedDirectory.exists()).thenReturn(false);
        when(exportedDirectory.mkdir()).thenReturn(true);
    }

    private void setUpTwoConfigFileIterator() {
        when(configurationDirectoryStream.iterator()).thenReturn(configFilesIterator);
        when(configFilesIterator.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(configFilesIterator.next()).thenReturn(configPath1).thenReturn(configPath2);
    }

    private void setUpConfigurationFileFactoryForTwoFiles() throws ConfigurationFileException {
        when(configurationFileFactory.createConfigurationFile(configPath1)).thenReturn(configFile1);
        when(configurationFileFactory.createConfigurationFile(configPath2)).thenReturn(configFile2);
    }
}
