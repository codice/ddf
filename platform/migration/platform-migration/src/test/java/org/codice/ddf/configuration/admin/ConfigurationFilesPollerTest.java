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

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationFilesPollerTest {

    private static final String FILE_EXT = ".config";

    private static final String PID = "my.pid";

    @Mock
    private ChangeListener mockChangeListener;

    @Mock
    private Path configFilePath;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Path directoryPath;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Path nonConfigFilePath;

    private File tempFile;

    @Before
    public void setup() throws IOException {
        File configFile = mock(File.class);

        when(configFilePath.toFile()).thenReturn(configFile);
        when(configFile.isDirectory()).thenReturn(false);
        when(configFilePath.getFileName()).thenReturn(configFilePath);
        when(configFilePath.endsWith(FILE_EXT)).thenReturn(true);
        when(configFilePath.toAbsolutePath()).thenReturn(Paths.get("foo"));

        when(directoryPath.toFile()
                .isDirectory()).thenReturn(true);

        when(nonConfigFilePath.toFile()
                .isDirectory()).thenReturn(true);
        when(nonConfigFilePath.endsWith(FILE_EXT)).thenReturn(false);
        tempFile = Files.createTempFile("foo", "bar")
                .toFile();
    }

    /**
     * Verify that an IllegalArgumentException is thrown when attempting to
     * register a null listener.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNullParameter() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        Path mockConfigurationDirectory = getMockConfigurationDirectoryPath(mockPath,
                mock(Path.class));
        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory,
                FILE_EXT);

        // Perform Test
        configurationFilesPoller.register(null);
    }

    /**
     * Verify that the ChangeListener is notified on file create events.
     */
    @Test
    public void testRunFileCreatedChangeListenerNotified() throws Exception {
        // Setup
        ConfigurationFilesPoller configurationFilesPoller =
                getConfigurationFilesPollerOverrideWait();

        // Perform Test
        configurationFilesPoller.register(mockChangeListener);
        configurationFilesPoller.onFileCreate(tempFile);

        // Verify
        verify(mockChangeListener).notify(Paths.get(tempFile.toURI()));
    }

    /**
     * Verify that the ChangeListener is not notified on file change events.
     */
    @Test
    public void testRunFileChangedChangeListenerNotNotified() throws Exception {
        // Setup
        ConfigurationFilesPoller configurationFilesPoller =
                getConfigurationFilesPollerOverrideWait();

        // Perform Test
        configurationFilesPoller.register(mockChangeListener);
        configurationFilesPoller.onFileChange(tempFile);

        // Verify
        verify(mockChangeListener, never()).notify(any());
    }

    /**
     * Verify that the ChangeListener is not notified on file delete events.
     */
    @Test
    public void testRunFileDeletedChangeListenerNotNotified() throws Exception {
        // Setup
        ConfigurationFilesPoller configurationFilesPoller =
                getConfigurationFilesPollerOverrideWait();

        // Perform Test
        configurationFilesPoller.register(mockChangeListener);
        configurationFilesPoller.onFileDelete(tempFile);

        // Verify
        verify(mockChangeListener, never()).notify(any());
    }

    /**
     * Verify that the ChangeListener is not notified on directory create events.
     */
    @Test
    public void testRunDirectoryCreatedChangeListenerNotNotified() throws Exception {
        // Setup
        ConfigurationFilesPoller configurationFilesPoller =
                getConfigurationFilesPollerOverrideWait();

        // Perform Test
        configurationFilesPoller.register(mockChangeListener);
        configurationFilesPoller.onDirectoryCreate(tempFile);

        // Verify
        verify(mockChangeListener, never()).notify(any());
    }

    /**
     * Verify that the ChangeListener is not notified on directory change events.
     */
    @Test
    public void testRunDirectoryChangedChangeListenerNotNotified() throws Exception {
        // Setup
        ConfigurationFilesPoller configurationFilesPoller =
                getConfigurationFilesPollerOverrideWait();

        // Perform Test
        configurationFilesPoller.register(mockChangeListener);
        configurationFilesPoller.onDirectoryChange(tempFile);

        // Verify
        verify(mockChangeListener, never()).notify(any());
    }

    /**
     * Verify that the ChangeListener is not notified on directory delete events.
     */
    @Test
    public void testRunDirectoryDeletedChangeListenerNotNotified() throws Exception {
        // Setup
        ConfigurationFilesPoller configurationFilesPoller =
                getConfigurationFilesPollerOverrideWait();

        // Perform Test
        configurationFilesPoller.register(mockChangeListener);
        configurationFilesPoller.onDirectoryDelete(tempFile);

        // Verify
        verify(mockChangeListener, never()).notify(any());
    }

    /**
     * Verify that the ChangeListener is not notified if a RuntimeException is thrown.
     */
    @Test
    public void testRuntimeExceptionThrownChangeListenerNotNotified() throws Exception {
        // Setup
        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                configFilePath,
                FILE_EXT);
        File file = mock(File.class);
        when(file.length()).thenThrow(new RuntimeException());

        // Perform Test
        configurationFilesPoller.register(mockChangeListener);
        configurationFilesPoller.onFileCreate(file);

        // Verify
        verify(mockChangeListener, never()).notify(any());
    }

    /**
     * Verify that the Poller waits once for a typical file length
     */
    @Test
    public void testWaitForFileToBeCompletelyWritten() throws Exception {
        // Setup
        ConfigurationFilesPoller configurationFilesPoller =
                getConfigurationFilesPollerOverrideSleepWithSpy();
        File file = mock(File.class);
        when(file.length()).thenReturn(1234L);

        // Perform Test
        configurationFilesPoller.waitForFileToBeCompletelyWritten(file);

        // Verify
        verify(configurationFilesPoller, times(1)).doSleep();
    }

    /**
     * Verify that the Poller does an extra wait to give typical buffered writers time to finish
     */
    @Test
    public void testWaitForFileToBeCompletelyWrittenByBufferedWriter() throws Exception {
        // Setup
        ConfigurationFilesPoller configurationFilesPoller =
                getConfigurationFilesPollerOverrideSleepWithSpy();
        File file = mock(File.class);
        when(file.length()).thenReturn(1024L);

        // Perform Test
        configurationFilesPoller.waitForFileToBeCompletelyWritten(file);

        // Verify
        verify(configurationFilesPoller, times(2)).doSleep();
    }

    private ConfigurationFilesPoller getConfigurationFilesPollerOverrideWait() {
        return new ConfigurationFilesPoller(configFilePath, FILE_EXT) {
            @Override
            void waitForFileToBeCompletelyWritten(File file) throws InterruptedException {
                return;
            }
        };
    }

    protected ConfigurationFilesPoller getConfigurationFilesPollerOverrideSleepWithSpy() {
        return Mockito.spy(new ConfigurationFilesPoller(configFilePath, FILE_EXT) {
            @Override
            void doSleep() throws InterruptedException {
                return;
            }
        });
    }

    private Path getMockPath(String baseFileName, String extension) {
        Path mockPath = mock(Path.class);
        when(mockPath.toString()).thenReturn(baseFileName + extension);
        return mockPath;
    }

    private Path getMockConfigurationDirectoryPath(Path mockPath, Path mockResolvedPath) {
        Path mockConfigurationDirectory = mock(Path.class);
        when(mockConfigurationDirectory.resolve(mockPath)).thenReturn(mockResolvedPath);
        File mockFile = mock(File.class);
        when(mockConfigurationDirectory.toFile()).thenReturn(mockFile);
        when(mockFile.list(any(FilenameFilter.class))).thenReturn(new String[0]);
        return mockConfigurationDirectory;
    }
}
