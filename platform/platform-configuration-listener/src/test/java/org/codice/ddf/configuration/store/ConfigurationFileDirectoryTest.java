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

//import static org.hamcrest.Matchers.empty;
//import static org.hamcrest.Matchers.equalTo;
//import static org.hamcrest.Matchers.hasItems;
//import static org.hamcrest.Matchers.is;
//import static org.hamcrest.Matchers.not;
//import static org.hamcrest.Matchers.notNullValue;
//import static org.hamcrest.Matchers.nullValue;
//import static org.junit.Assert.assertThat;
//import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
//import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationFileDirectoryTest {

    private static final String FILE_EXT = ".cfg";

    private static final String PID = "org.codice.ddf.ConfigurationTest";

    @Mock
    private File directory;
    
    @Test
    public void test1() {
        
    }

//    @Test(expected = IllegalArgumentException.class)
//    public void constructorWithNullDirectory() {
//        new ConfigurationFileDirectory(null, FILE_EXT);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void constructorWithNonExistentConfigurationDirectory() {
//        setupConfigurationDirectoryExpectations(false, true, true);
//        new ConfigurationFileDirectory(directory, FILE_EXT);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void constructorWithNonReadableDirectory() {
//        setupConfigurationDirectoryExpectations(true, false, true);
//        new ConfigurationFileDirectory(directory, FILE_EXT);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void constructorWithNonWritableDirectory() {
//        setupConfigurationDirectoryExpectations(true, true, false);
//        new ConfigurationFileDirectory(directory, FILE_EXT);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void constructorWithNullExtension() {
//        setupConfigurationDirectoryExpectations(true, true, true);
//        new ConfigurationFileDirectory(directory, null);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void constructorWithExtensionTooShort() {
//        setupConfigurationDirectoryExpectations(true, true, true);
//        new ConfigurationFileDirectory(directory, ".");
//    }
//
//    @Test
//    public void createFileInputStream() throws IOException {
//        Path tempDirectory = createTempConfigFile();
//
//        ConfigurationFileDirectory configurationFileDirectory = new ConfigurationFileDirectory(
//                tempDirectory.toFile(), FILE_EXT);
//        FileInputStream fileInputStream = configurationFileDirectory.createFileInputStream(PID);
//
//        assertThat(fileInputStream, is(not(nullValue())));
//        assertThat(fileInputStream.getFD().valid(), is(true));
//        assertThat(fileInputStream.getChannel().size(), equalTo(0L));
//    }
//
//    @Test(expected = FileNotFoundException.class)
//    public void createFileInputStreamWithNonExistingFile() throws IOException {
//        Path tempDirectory = createTempDir();
//        ConfigurationFileDirectory configurationFileDirectory = new ConfigurationFileDirectory(
//                tempDirectory.toFile(), FILE_EXT);
//        configurationFileDirectory.createFileInputStream(PID);
//    }
//
//    @Test
//    public void createFileOutputStream() throws IOException {
//        Path tempDirectory = createTempDir();
//
//        try {
//            ConfigurationFileDirectory configurationFileDirectory = new ConfigurationFileDirectory(
//                    tempDirectory.toFile(), FILE_EXT);
//            FileOutputStream fileOutputStream = configurationFileDirectory
//                    .createFileOutputStream(PID);
//            fileOutputStream.write(new byte[] {32});
//
//            assertThat(fileOutputStream, is(not(nullValue())));
//            assertThat(fileOutputStream.getFD().valid(), is(true));
//            assertThat(fileOutputStream.getChannel().size(), equalTo(1L));
//        } finally {
//            getTempConfigFileName(tempDirectory).toFile().delete();
//        }
//    }
//
//    @Test(expected = FileNotFoundException.class)
//    public void createFileOutputStreamInReadOnlyDirectory() throws IOException {
//        Path tempDirectory = createTempDir();
//        ConfigurationFileDirectory configurationFileDirectory = new ConfigurationFileDirectory(
//                tempDirectory.toFile(), FILE_EXT);
//        assertThat("Failing the test because we couldn't make the temporary directory read-only.",
//                tempDirectory.toFile().setReadOnly(), is(true));
//        configurationFileDirectory.createFileOutputStream(PID);
//    }
//
//    @Test
//    public void fileExists() throws IOException {
//        Path tempDirectory = createTempConfigFile();
//        ConfigurationFileDirectory configurationFileDirectory = new ConfigurationFileDirectory(
//                tempDirectory.toFile(), FILE_EXT);
//        assertThat(configurationFileDirectory.exists(PID), is(true));
//    }
//
//    @Test
//    public void fileDoesNotExist() throws IOException {
//        Path tempDirectory = createTempDir();
//        ConfigurationFileDirectory configurationFileDirectory = new ConfigurationFileDirectory(
//                tempDirectory.toFile(), FILE_EXT);
//        assertThat(configurationFileDirectory.exists(PID), is(false));
//    }
//
//    @Test
//    public void deleteExistingFile() throws IOException {
//        Path tempDirectory = createTempConfigFile();
//        ConfigurationFileDirectory configurationFileDirectory = new ConfigurationFileDirectory(
//                tempDirectory.toFile(), FILE_EXT);
//        assertThat(configurationFileDirectory.delete(PID), is(true));
//        assertThat(getTempConfigFileName(tempDirectory).toFile().exists(), is(false));
//    }
//
//    @Test
//    public void deleteNonExistingFile() throws IOException {
//        Path tempDirectory = createTempDir();
//        ConfigurationFileDirectory configurationFileDirectory = new ConfigurationFileDirectory(
//                tempDirectory.toFile(), FILE_EXT);
//        assertThat(configurationFileDirectory.delete(PID), is(false));
//        assertThat(getTempConfigFileName(tempDirectory).toFile().exists(), is(false));
//    }
//
//    @Test
//    public void listFiles() throws IOException {
//        setupConfigurationDirectoryExpectations(true, true, true);
//        ConfigurationFileDirectory configurationFileDirectory = new ConfigurationFileDirectory(
//                directory, FILE_EXT);
//
//        Collection<String> pids = configurationFileDirectory.listFiles();
//        assertThat(pids, is(notNullValue()));
//        assertThat(pids.size(), equalTo(0));
//        verify(directory)
//                .listFiles(Matchers.any(ConfigurationFileDirectory.ConfigurationFileFilter.class));
//    }
//
//    @Test
//    public void fileFilterMatches() {
//        Collection<String> pids = new ArrayList<>();
//        ConfigurationFileDirectory.ConfigurationFileFilter fileFilter = new ConfigurationFileDirectory.ConfigurationFileFilter(
//                pids, FILE_EXT);
//        assertThat(fileFilter.accept(new File("/"), PID + FILE_EXT), is(true));
//        assertThat(pids, hasItems(PID));
//    }
//
//    @Test
//    public void fileFilterDoesNotMatche() {
//        Collection<String> pids = new ArrayList<>();
//        ConfigurationFileDirectory.ConfigurationFileFilter fileFilter = new ConfigurationFileDirectory.ConfigurationFileFilter(
//                pids, FILE_EXT);
//        assertThat(fileFilter.accept(new File("/"), PID + ".config"), is(false));
//        assertThat(pids, is(empty()));
//    }

    private Path createTempDir() throws IOException {
        Path tempDirectory = Files.createTempDirectory("configTest");
        tempDirectory.toFile().deleteOnExit();
        return tempDirectory;
    }

    private Path createTempConfigFile() throws IOException {
        Path tempDirectory = createTempDir();

        Path configFile = getTempConfigFileName(tempDirectory);
        configFile.toFile().createNewFile();
        configFile.toFile().deleteOnExit();

        return tempDirectory;
    }

    private Path getTempConfigFileName(Path tempDirectory) {
        return tempDirectory.resolve(PID + FILE_EXT);
    }

    private void setupConfigurationDirectoryExpectations(boolean exists, boolean canRead,
            boolean canWrite) {
        when(directory.exists()).thenReturn(exists);
        when(directory.canRead()).thenReturn(canRead);
        when(directory.canWrite()).thenReturn(canWrite);
    }
}
