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

package org.codice.ddf.configuration.listener;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.codice.ddf.configuration.store.ChangeListener;
import org.codice.ddf.configuration.store.ConfigurationFilesPoller;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ConfigurationFilesPollerTest {

    private static final String FILE_EXT = ".config";

    private static final String INVALID_FILE_EXT = ".xml";

    private static final String PID = "my.pid";

    @Test
    public void testRunFileModified() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        Kind<?> mockKind = getMockKind(ENTRY_MODIFY.toString());
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath, mockKind);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        ExecutorService mockExecutorService = mock(ExecutorService.class);
        Path mockConfigurationDirectory = mock(Path.class);

        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);
        ChangeListener mockChangeListener = getMockChangeListener();
        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener).update(PID, ChangeListener.ChangeType.UPDATED);
    }

    @Test
    public void testRunOverflow() throws Exception {
        // Setup
        Kind<?> mockKind = getMockKind(OVERFLOW.toString());
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(new Object(), mockKind);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        ExecutorService mockExecutorService = mock(ExecutorService.class);
        Path mockConfigurationDirectory = mock(Path.class);

        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);
        ChangeListener mockChangeListener = getMockChangeListener();
        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener, times(0)).update(PID, ChangeListener.ChangeType.UPDATED);
    }

    @Test
    public void testRunFileCreated() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        Kind<?> mockKind = getMockKind(ENTRY_CREATE.toString());
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath, mockKind);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        ExecutorService mockExecutorService = mock(ExecutorService.class);
        Path mockConfigurationDirectory = mock(Path.class);

        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);
        ChangeListener mockChangeListener = getMockChangeListener();
        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener).update(PID, ChangeListener.ChangeType.CREATED);
    }

    @Test
    public void testRunFileDeleted() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        Kind<?> mockKind = getMockKind(ENTRY_DELETE.toString());
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath, mockKind);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        ExecutorService mockExecutorService = mock(ExecutorService.class);
        Path mockConfigurationDirectory = mock(Path.class);

        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);
        ChangeListener mockChangeListener = getMockChangeListener();
        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener).update(PID, ChangeListener.ChangeType.DELETED);
    }
    
    @Test
    public void testDestroyShutsdownAfterFirstAwaitTermination() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        Kind<?> mockKind = getMockKind(ENTRY_DELETE.toString());
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath, mockKind);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        ExecutorService mockExecutorService = mock(ExecutorService.class);
        when(mockExecutorService.awaitTermination(10, TimeUnit.SECONDS)).thenReturn(true);
        Path mockConfigurationDirectory = mock(Path.class);

        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);

        // Perform Test
        configurationFilesPoller.destroy();

        // verify
        verify(mockWatchService).close();
        verify(mockExecutorService).shutdown();
        verify(mockExecutorService, times(1)).awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void testDestroyShutsdownAfterSecondAwaitTermination() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        Kind<?> mockKind = getMockKind(ENTRY_DELETE.toString());
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath, mockKind);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        ExecutorService mockExecutorService = mock(ExecutorService.class);
        when(mockExecutorService.awaitTermination(10, TimeUnit.SECONDS)).thenReturn(false, true);
        Path mockConfigurationDirectory = mock(Path.class);

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

    @Test
    public void testDestroyFailsToShutdownAfterSecondAwaitTermination() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, FILE_EXT);
        Kind<?> mockKind = getMockKind(ENTRY_DELETE.toString());
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath, mockKind);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        ExecutorService mockExecutorService = mock(ExecutorService.class);
        when(mockExecutorService.awaitTermination(10, TimeUnit.SECONDS)).thenReturn(false, false);
        Path mockConfigurationDirectory = mock(Path.class);

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

    @Test
    public void testRunInvalidFileExtension() throws Exception {
        // Setup
        Path mockPath = getMockPath(PID, INVALID_FILE_EXT);
        Kind<?> mockKind = getMockKind(ENTRY_MODIFY.toString());
        WatchEvent<?> mockWatchEvent = getMockWatchEvent(mockPath, mockKind);
        List<WatchEvent<?>> watchEvents = getSingleMockWatchEvent(mockWatchEvent);
        WatchKey mockWatchKey = getMockWatchKey(watchEvents);
        WatchService mockWatchService = getMockWatchService(mockWatchKey);
        ExecutorService mockExecutorService = mock(ExecutorService.class);
        Path mockConfigurationDirectory = mock(Path.class);

        ConfigurationFilesPoller configurationFilesPoller = new ConfigurationFilesPoller(
                mockConfigurationDirectory, FILE_EXT, mockWatchService, mockExecutorService);
        ChangeListener mockChangeListener = getMockChangeListener();
        configurationFilesPoller.register(mockChangeListener);

        // Perform Test
        configurationFilesPoller.run();

        // Verify
        verify(mockChangeListener, times(0)).update(PID, ChangeListener.ChangeType.UPDATED);
    }

    private Path getMockPath(String baseFileName, String extension) {
        Path mockPath = mock(Path.class);
        when(mockPath.toString()).thenReturn(baseFileName + extension);
        return mockPath;
    }

    private Kind<?> getMockKind(String kindName) {
        Kind<?> mockKind = mock(Kind.class);
        when(mockKind.name()).thenReturn(kindName);
        return mockKind;
    }

    private <T> WatchEvent<?> getMockWatchEvent(T mockPath, Kind<?> mockKind) {
        WatchEvent<?> mockWatchEvent = mock(WatchEvent.class);
        when(mockWatchEvent.context()).thenAnswer(createAnswer(mockPath));
        when(mockWatchEvent.kind()).thenAnswer(createAnswer(mockKind));
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

    private ChangeListener getMockChangeListener() {
        ChangeListener mockChangeListener = mock(ChangeListener.class);
        return mockChangeListener;
    }

    private <T> Answer<T> createAnswer(T value) {
        Answer<T> answer = new Answer<T>() {
            @Override
            public T answer(InvocationOnMock invocation) throws Throwable {
                return value;
            }
        };
        return answer;
    }
}
