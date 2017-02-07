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
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationFilesPollerTest {

    private static final String FILE_EXT = ".config";

    private static final String INVALID_FILE_EXT = ".invalid";

    private static final String PID = "my.pid";

    @Mock
    private ExecutorService mockExecutorService;

    @Mock
    private ChangeListener mockChangeListener;

    @Mock
    private Path configFilePath;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Path directoryPath;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Path nonConfigFilePath;

    @Before
    public void setup() {
        File configFile = mock(File.class);

        when(configFilePath.toFile()).thenReturn(configFile);
        when(configFile.isDirectory()).thenReturn(false);
        when(configFilePath.getFileName()).thenReturn(configFilePath);
        when(configFilePath.endsWith(FILE_EXT)).thenReturn(true);

        when(directoryPath.toFile()
                .isDirectory()).thenReturn(true);

        when(nonConfigFilePath.toFile()
                .isDirectory()).thenReturn(true);
        when(nonConfigFilePath.endsWith(FILE_EXT)).thenReturn(false);
    }

    /**
     * Verify that poller gets started by the ExecutorService.
     */
    @Test
    public void testInit() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath,
                StandardWatchEventKinds.ENTRY_CREATE);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        Path mockConfigurationDirectory = getMockConfigurationDirectoryPath(mockPath,
                mock(Path.class));
        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory,
                FILE_EXT,
                mockWatchService,
                mockExecutorService);

        // Perform Test
        configurationFilesPoller.init();

        // Verify
        verify(mockConfigurationDirectory).register(mockWatchService,
                StandardWatchEventKinds.ENTRY_CREATE);
        verify(mockExecutorService).execute(configurationFilesPoller);
    }

    /**
     * Verify that when the configurationDirectoryPath fails to register with the WatchService,
     * the init method fails.
     */
    @Test(expected = IOException.class)
    public void testInitFailsToRegisterPathWithWatchService() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath,
                StandardWatchEventKinds.ENTRY_CREATE);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        Path mockConfigurationDirectory = getMockConfigurationDirectoryPath(mockPath,
                mock(Path.class));
        when(mockConfigurationDirectory.register(mockWatchService,
                StandardWatchEventKinds.ENTRY_CREATE)).thenThrow(new IOException());
        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory,
                FILE_EXT,
                mockWatchService,
                mockExecutorService);

        // Perform Test
        try {
            configurationFilesPoller.init();
        } catch (IOException e) {
            // Verify
            verify(mockExecutorService, never()).execute(configurationFilesPoller);
            verify(mockWatchService, never()).take();
            throw e;
        }
    }

    @Test
    public void testResgiterFilesAlreadyExist() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath,
                StandardWatchEventKinds.ENTRY_CREATE);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        Path mockConfigurationDirectory = getMockConfigurationDirectoryPath(mockPath,
                mock(Path.class));

        Stream<Path> existingFiles = mock(Stream.class);
        when(existingFiles.filter(any(Predicate.class))).thenReturn(Stream.of(configFilePath,
                directoryPath,
                nonConfigFilePath));
        when(mockConfigurationDirectory.resolve(configFilePath)).thenReturn(configFilePath);

        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory,
                FILE_EXT,
                mockWatchService,
                mockExecutorService) {
            @Override
            Stream<Path> getExistingFiles() throws IOException {
                return existingFiles;
            }
        };

        // Perform Test
        configurationFilesPoller.register(mockChangeListener);

        // Verify
        verify(mockChangeListener).notify(configFilePath);
        verify(mockChangeListener, never()).notify(directoryPath);
        verify(mockChangeListener, never()).notify(nonConfigFilePath);
    }

    /**
     * Verify that an IllegalArgumentException is thrown when attempting to
     * register a null listener.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNullParameter() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath,
                StandardWatchEventKinds.ENTRY_CREATE);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        Path mockConfigurationDirectory = getMockConfigurationDirectoryPath(mockPath,
                mock(Path.class));
        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory,
                FILE_EXT,
                mockWatchService,
                mockExecutorService);

        // Perform Test
        configurationFilesPoller.register(null);
    }

    /**
     * Verify that file modification events are not sent to the ChangeListener.
     */
    @Test
    public void testRunFileModifiedChangeListenerNotNotified() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath,
                StandardWatchEventKinds.ENTRY_MODIFY);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        Path mockConfigurationDirectory = getMockConfigurationDirectoryPath(mockPath,
                mock(Path.class));
        ConfigurationFilesPoller configurationFilesPoller = createConfigurationFilesPoller(
                mockWatchService,
                mockConfigurationDirectory);
        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener, never()).notify(any(Path.class));
    }

    /**
     * Verify that overflow events are not sent to the ChangeListener.
     */
    @Test
    public void testRunOverflowChangeListenerNotNotified() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath,
                StandardWatchEventKinds.OVERFLOW);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        Path mockConfigurationDirectory = getMockConfigurationDirectoryPath(mockPath,
                mock(Path.class));
        ConfigurationFilesPoller configurationFilesPoller = createConfigurationFilesPoller(
                mockWatchService,
                mockConfigurationDirectory);
        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener, never()).notify(any(Path.class));
    }

    /**
     * Verify that file deletion events are not sent to the ChangeListener.
     */
    @Test
    public void testRunFileDeletedChangeListenerNotNotified() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath,
                StandardWatchEventKinds.ENTRY_DELETE);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        Path mockConfigurationDirectory = getMockConfigurationDirectoryPath(mockPath,
                mock(Path.class));

        ConfigurationFilesPoller configurationFilesPoller = createConfigurationFilesPoller(
                mockWatchService,
                mockConfigurationDirectory);
        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener, never()).notify(any(Path.class));
    }

    /**
     * Verify that the ChangeListener is notified on file creation events.
     */
    @Test
    public void testRunFileCreatedChangeListenerNotified() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath,
                StandardWatchEventKinds.ENTRY_CREATE);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        Path resolvedPath = mock(Path.class);
        Path mockConfigurationDirectory = getMockConfigurationDirectoryPath(mockPath, resolvedPath);
        ConfigurationFilesPoller configurationFilesPoller = createConfigurationFilesPoller(
                mockWatchService,
                mockConfigurationDirectory);
        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener).notify(resolvedPath);
    }

    /**
     * Verify that the ChangeListener is not notified for creation events
     * involving invalid file extensions.
     */
    @Test
    public void testRunInvalidFileExtension() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, INVALID_FILE_EXT);
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath,
                StandardWatchEventKinds.ENTRY_CREATE);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        Path mockConfigurationDirectory = getMockConfigurationDirectoryPath(mockPath,
                mock(Path.class));

        ConfigurationFilesPoller configurationFilesPoller = createConfigurationFilesPoller(
                mockWatchService,
                mockConfigurationDirectory);

        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener, never()).notify(any(Path.class));
    }

    /**
     * Verify that when no listener is registered, an attempt is not made to call its
     * notify method.
     */
    @Test
    public void testRunFileCreatedNoListenerRegistered() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath,
                StandardWatchEventKinds.ENTRY_CREATE);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        Path mockConfigurationDirectory = getMockConfigurationDirectoryPath(mockPath,
                mock(Path.class));
        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory,
                FILE_EXT,
                mockWatchService,
                mockExecutorService);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener, never()).notify(any(Path.class));
    }

    @Test
    public void testDestroyShutsdownAfterAwaitingTermination() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath,
                StandardWatchEventKinds.ENTRY_DELETE);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        when(mockExecutorService.awaitTermination(10, TimeUnit.SECONDS)).thenReturn(true);
        Path mockConfigurationDirectory = getMockConfigurationDirectoryPath(mockPath,
                mock(Path.class));
        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory,
                FILE_EXT,
                mockWatchService,
                mockExecutorService);

        // Perform Test
        configurationFilesPoller.destroy();

        // verify
        verify(mockWatchService).close();
        verify(mockExecutorService).shutdown();
        verify(mockExecutorService).awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void testDestroyShutsdownAfterSecondAwaitTermination() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath,
                StandardWatchEventKinds.ENTRY_DELETE);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        when(mockExecutorService.awaitTermination(10, TimeUnit.SECONDS)).thenReturn(false, true);
        Path mockConfigurationDirectory = getMockConfigurationDirectoryPath(mockPath,
                mock(Path.class));
        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory,
                FILE_EXT,
                mockWatchService,
                mockExecutorService);

        // Perform Test
        configurationFilesPoller.destroy();

        // verify
        verify(mockWatchService).close();
        verify(mockExecutorService).shutdown();
        verify(mockExecutorService).shutdownNow();
        verify(mockExecutorService, times(2)).awaitTermination(10, TimeUnit.SECONDS);
    }

    private ConfigurationFilesPoller createConfigurationFilesPoller(
            final WatchService mockWatchService, final Path mockConfigurationDirectory) {
        Stream<Path> existingFiles = mock(Stream.class);
        when(existingFiles.filter(any(Predicate.class))).thenReturn(existingFiles);

        return new ConfigurationFilesPoller(mockConfigurationDirectory,
                FILE_EXT,
                mockWatchService,
                mockExecutorService) {
            @Override
            Stream<Path> getExistingFiles() throws IOException {
                return existingFiles;
            }
        };
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

    private <T> WatchEvent<T> getMockWatchEvent(T mockPath, Kind<T> mockKind) {
        @SuppressWarnings("unchecked")
        WatchEvent<T> mockWatchEvent = mock(WatchEvent.class);
        when(mockWatchEvent.context()).thenReturn(mockPath);
        when(mockWatchEvent.kind()).thenReturn(mockKind);
        return mockWatchEvent;
    }

    private WatchKey getMockWatchKey(List<WatchEvent<?>> watchEvents) {
        WatchKey mockWatchKey = mock(WatchKey.class);
        when(mockWatchKey.pollEvents()).thenReturn(watchEvents);
        when(mockWatchKey.reset()).thenReturn(false);
        return mockWatchKey;
    }

    private WatchService getMockWatchService(WatchKey mockWatchKey) throws Exception {
        WatchService mockWatchService = mock(WatchService.class);
        when(mockWatchService.take()).thenReturn(mockWatchKey);
        return mockWatchService;
    }

    private List<WatchEvent<?>> getSingleMockWatchEvent(WatchEvent<?> mockWatchEvent) {
        List<WatchEvent<?>> watchEvents = new ArrayList<>(1);
        watchEvents.add(mockWatchEvent);
        return watchEvents;
    }
}
