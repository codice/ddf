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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.felix.cm.impl.CaseInsensitiveDictionary;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit test class for {@link FileHandlerImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class FileHandlerImplTest {

    private static final String FILE_EXT = ".cfg";

    private static final String FILE_PATH = "/some/valid/path";

    private static final String CONFIG_PID = "org.codice.ddf.MyConfig";

    @Mock
    private ConfigurationFilesPoller configurationFilesPoller;

    @Mock
    private PersistenceStrategy persistenceStrategy;

    @Mock
    private ConfigurationFileDirectory configurationFileDirectory;

    @Mock
    private FileInputStream inputStream;

    @Mock
    private FileOutputStream outputStream;

    @Mock
    private FileChannel fileChannel;

    @Mock
    private FileLock fileLock;

    @Mock
    private ReadWriteLock lock;

    @Mock
    private Lock readLock;

    @Mock
    private Lock writeLock;

    @Mock
    private ChangeListener changeListener;

    private Dictionary<String, Object> expectedProperties = new CaseInsensitiveDictionary();

    private Dictionary<String, Object> properties1 = new CaseInsensitiveDictionary();

    private Dictionary<String, Object> properties2 = new CaseInsensitiveDictionary();

    @Before
    public void setup() {
        expectedProperties.put("key1", "value1");
        expectedProperties.put("key2", "value2");
        properties1.put("A", "1");
        properties1.put("B", "2");
        properties2.put("C", "3");
        properties2.put("D", "4");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullFileDirectory() {
        new FileHandlerImpl(null, configurationFilesPoller, persistenceStrategy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullFilePoller() {
        new FileHandlerImpl(configurationFileDirectory, null, persistenceStrategy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullPersistenceStrategy() {
        new FileHandlerImpl(configurationFileDirectory, configurationFilesPoller, null);
    }

    @Test
    public void getConfigurationPids() {
        Collection<String> pids = new ArrayList<>();
        pids.add("pid1");

        when(configurationFileDirectory.listFiles()).thenReturn(pids);
        FileHandlerImpl fileHandler = new FileHandlerImpl(configurationFileDirectory,
                configurationFilesPoller, persistenceStrategy);

        assertThat(fileHandler.getConfigurationPids(), equalTo(pids));
        verify(configurationFileDirectory).listFiles();
    }

    @Test
    public void readConfigurationFile() throws IOException {
        when(configurationFileDirectory.createFileInputStream(CONFIG_PID)).thenReturn(inputStream);
        when(persistenceStrategy.read(inputStream)).thenReturn(expectedProperties);

        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        Dictionary<String, Object> properties = fileHandler.read(CONFIG_PID);

        assertThat(properties, equalTo(expectedProperties));

        verify(configurationFileDirectory).createFileInputStream(CONFIG_PID);
        verify(persistenceStrategy, times(1)).read(inputStream);
        verify(inputStream).close();
        verifyReadLockUsed();
    }

    @Test(expected = ConfigurationFileException.class)
    public void readConfigurationFileWhenInputStreamCannotBeCreated() throws FileNotFoundException {
        when(configurationFileDirectory.createFileInputStream(CONFIG_PID))
                .thenThrow(new FileNotFoundException());

        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        fileHandler.read(CONFIG_PID);

        verify(configurationFileDirectory).createFileInputStream(CONFIG_PID);
        verifyReadLockUsed();
    }

    @Test(expected = ConfigurationFileException.class)
    public void readConfigurationFileWhenReadFails() throws IOException {
        when(configurationFileDirectory.createFileInputStream(CONFIG_PID)).thenReturn(inputStream);
        when(persistenceStrategy.read(inputStream)).thenThrow(new IOException());

        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        fileHandler.read(CONFIG_PID);

        verify(configurationFileDirectory).createFileInputStream(CONFIG_PID);
        verify(inputStream).close();
        verifyReadLockUsed();
    }

    @Test(expected = IllegalArgumentException.class)
    public void readWithNullPid() {
        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        fileHandler.read(null);
    }

    @Test
    public void writeKnownConfigurationFile() throws IOException {
        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);

        readFile(fileHandler);
        writeFile(fileHandler, properties1);

        verify(fileChannel).tryLock();
        verify(persistenceStrategy).write(outputStream, properties1);
        verify(outputStream).close();
        verify(fileLock).close();
        verifyLocksUsed(1, 1);
    }

    @Test
    public void writeUnknownConfigurationFile() throws IOException {
        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);

        fileHandler.write(CONFIG_PID, properties1);

        verify(fileChannel, never()).tryLock();
        verify(persistenceStrategy, never()).write(outputStream, properties1);
        verifyLocksUsed(0, 1);
    }

    @Test
    public void writeConfigurationFileWithSameProperties() throws IOException {
        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);

        readFile(fileHandler);
        writeFile(fileHandler, properties1);
        fileHandler.write(CONFIG_PID, properties1);

        verify(fileChannel).tryLock();
        verify(persistenceStrategy).write(outputStream, properties1);
        verify(outputStream).close();
        verify(fileLock).close();
        verifyLocksUsed(1, 2);
    }

    @Test
    public void writeConfigurationFileWithDifferentProperties() throws IOException {
        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);

        readFile(fileHandler);
        writeFile(fileHandler, properties1);
        fileHandler.write(CONFIG_PID, properties2);

        verify(fileChannel, times(2)).tryLock();
        verify(persistenceStrategy).write(outputStream, properties1);
        verify(persistenceStrategy).write(outputStream, properties2);
        verify(outputStream, times(2)).close();
        verify(fileLock, times(2)).close();
        verifyLocksUsed(1, 2);
    }

    @Test(expected = ConfigurationFileException.class)
    public void writeConfigurationFileWhenOutputStreamCannotBeCreated() throws IOException {
        when(configurationFileDirectory.createFileOutputStream(CONFIG_PID))
                .thenThrow(new FileNotFoundException());

        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        readFile(fileHandler);

        try {
            fileHandler.write(CONFIG_PID, properties1);
        } finally {
            verify(persistenceStrategy, never()).write(outputStream, properties1);
            verifyLocksUsed(1, 1);
        }
    }

    @Test(expected = ConfigurationFileException.class)
    public void writeConfigurationWhenFileLockFails() throws IOException {
        when(configurationFileDirectory.createFileOutputStream(CONFIG_PID))
                .thenReturn(outputStream);
        when(outputStream.getChannel()).thenReturn(fileChannel);
        when(fileChannel.tryLock()).thenThrow(new IOException());

        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        readFile(fileHandler);

        try {
            fileHandler.write(CONFIG_PID, properties1);
        } finally {
            verify(fileChannel).tryLock();
            verify(persistenceStrategy, never()).write(outputStream, properties1);
            verify(outputStream).close();
            verifyLocksUsed(1, 1);
        }
    }

    @Test(expected = ConfigurationFileException.class)
    public void writeConfigurationWhenFileIsLocked() throws IOException {
        when(configurationFileDirectory.createFileOutputStream(CONFIG_PID))
                .thenReturn(outputStream);
        when(outputStream.getChannel()).thenReturn(fileChannel);
        when(fileChannel.tryLock()).thenReturn(null);

        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        readFile(fileHandler);

        try {
            fileHandler.write(CONFIG_PID, properties1);
        } finally {
            verify(fileChannel).tryLock();
            verify(persistenceStrategy, never()).write(outputStream, properties1);
            verify(outputStream).close();
            verifyLocksUsed(1, 1);
        }
    }

    @Test(expected = ConfigurationFileException.class)
    public void writeConfigurationFileWhenWriteFails() throws IOException {
        when(configurationFileDirectory.createFileOutputStream(CONFIG_PID))
                .thenReturn(outputStream);
        when(outputStream.getChannel()).thenReturn(fileChannel);
        when(fileChannel.tryLock()).thenReturn(fileLock);
        doThrow(new IOException()).when(persistenceStrategy).write(outputStream, properties1);

        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        readFile(fileHandler);

        try {
            fileHandler.write(CONFIG_PID, properties1);
        } finally {
            verify(fileChannel).tryLock();
            verify(outputStream).close();
            verifyLocksUsed(1, 1);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void writeWithNullPid() {
        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        fileHandler.write(null, expectedProperties);
    }

    @Test(expected = IllegalArgumentException.class)
    public void writeWithNullProperties() {
        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        fileHandler.write(CONFIG_PID, null);
    }

    @Test
    public void deleteExistingFile() {
        when(configurationFileDirectory.delete(CONFIG_PID)).thenReturn(true);

        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        fileHandler.delete(CONFIG_PID);

        verify(configurationFileDirectory).delete(CONFIG_PID);
        verifyLocksUsed(0, 1);
    }

    @Test
    public void deleteExistingFileAndRecreateFile() throws IOException {
        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);

        readFile(fileHandler);

        when(configurationFileDirectory.delete(CONFIG_PID)).thenReturn(true);

        fileHandler.delete(CONFIG_PID);

        writeFile(fileHandler, properties1);

        verify(configurationFileDirectory).delete(CONFIG_PID);
        verify(fileChannel).tryLock();
        verify(persistenceStrategy).write(outputStream, properties1);
        verify(outputStream).close();
        verify(fileLock).close();
        verifyLocksUsed(1, 2);
    }

    @Test
    public void deleteNonExistingFile() {
        when(configurationFileDirectory.delete(CONFIG_PID)).thenReturn(false);

        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        fileHandler.delete(CONFIG_PID);

        verify(configurationFileDirectory).delete(CONFIG_PID);
        verifyLocksUsed(0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteWithNullPid() {
        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        fileHandler.delete(null);
    }

    @Test
    public void registerListener() throws IOException {
        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);

        fileHandler.registerForChanges(changeListener);

        verify(configurationFilesPoller).register(changeListener);
    }

    @Test(expected = ConfigurationFileException.class)
    public void registerListenerFails() throws IOException {
        doThrow(new IOException()).when(configurationFilesPoller).register(changeListener);

        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);

        fileHandler.registerForChanges(changeListener);
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerNullListener() {
        FileHandlerImplUnderTest fileHandler = new FileHandlerImplUnderTest(
                configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        fileHandler.registerForChanges(null);
    }

    private void readFile(FileHandlerImplUnderTest fileHandler) throws IOException {
        when(configurationFileDirectory.createFileInputStream(CONFIG_PID)).thenReturn(inputStream);
        when(persistenceStrategy.read(inputStream)).thenReturn(expectedProperties);
        fileHandler.read(CONFIG_PID);
    }

    private void writeFile(FileHandlerImplUnderTest fileHandler,
            Dictionary<String, Object> properties) throws IOException {
        when(configurationFileDirectory.createFileOutputStream(CONFIG_PID))
                .thenReturn(outputStream);
        when(outputStream.getChannel()).thenReturn(fileChannel);
        when(fileChannel.tryLock()).thenReturn(fileLock);
        fileHandler.write(CONFIG_PID, properties);
    }

    private void verifyLocksUsed(int readTimes, int writeTimes) {
        verify(writeLock, times(writeTimes)).lock();
        verify(writeLock, times(writeTimes)).unlock();
        verify(readLock, times(readTimes)).lock();
        verify(readLock, times(readTimes)).unlock();
    }

    private void verifyReadLockUsed() {
        verify(readLock).lock();
        verify(readLock).unlock();
        verify(writeLock, never()).lock();
        verify(writeLock, never()).unlock();
    }

    private class FileHandlerImplUnderTest extends FileHandlerImpl {

        public FileHandlerImplUnderTest(ConfigurationFileDirectory configurationFileDirectory,
                ConfigurationFilesPoller configurationFilesPoller,
                PersistenceStrategy persistenceStrategy) {
            super(configurationFileDirectory, configurationFilesPoller, persistenceStrategy);
        }

        @Override
        ReadWriteLock createLock() {
            when(lock.readLock()).thenReturn(readLock);
            when(lock.writeLock()).thenReturn(writeLock);
            return lock;
        }
    }
}
