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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);

        // Perform Test
        configurationFilesPoller.init();

        // Verify
        verify(mockExecutorService).execute(configurationFilesPoller);
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
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);

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
        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);
        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener, times(0)).notify(any(Path.class));
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
        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);
        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener, times(0)).notify(any(Path.class));
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
        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);
        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener, times(0)).notify(any(Path.class));
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
        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);
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
        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);
        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener, times(0)).notify(any(Path.class));
    }

    /**
     * Verify that when the configurationDirectoryPath fails to register with the WatchService,
     * the run method exits and the WatchService does not attempt to take any watch keys.
     */
    @Test
    public void testRunFailsToRegisterPathWithWatchService() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath,
                StandardWatchEventKinds.ENTRY_CREATE);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        Path mockConfigurationDirectory = mock(Path.class);
        when(mockConfigurationDirectory
                .register(mockWatchService, StandardWatchEventKinds.ENTRY_CREATE))
                .thenThrow(new IOException());
        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);
        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockWatchService, times(0)).take();
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
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener, times(0)).notify(any(Path.class));
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
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);

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
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);

        // Perform Test
        configurationFilesPoller.destroy();

        // verify
        verify(mockWatchService).close();
        verify(mockExecutorService).shutdown();
        verify(mockExecutorService).shutdownNow();
        verify(mockExecutorService, times(2)).awaitTermination(10, TimeUnit.SECONDS);
    }

    private Path getMockPath(String baseFileName, String extension) {
        Path mockPath = mock(Path.class);
        when(mockPath.toString()).thenReturn(baseFileName + extension);
        return mockPath;
    }

    private Path getMockConfigurationDirectoryPath(Path mockPath, Path mockResolvedPath) {
        Path mockConfigurationDirectory = mock(Path.class);
        when(mockConfigurationDirectory.resolve(mockPath)).thenReturn(mockResolvedPath);
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
