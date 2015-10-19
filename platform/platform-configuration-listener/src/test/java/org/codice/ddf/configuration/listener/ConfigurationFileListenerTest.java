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

package org.codice.ddf.configuration.listener;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.codice.ddf.configuration.store.ChangeListener.ChangeType;
import org.codice.ddf.configuration.store.FileHandler;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigurationFileListenerTest {

    private static final String PID = "my.pid";

    private static final Dictionary<String, Object> PROPERTIES = new Hashtable<String, Object>(1);

    static {
        PROPERTIES.put("myKey", "myValue");
    }

    @Test
    public void testInit() throws Exception {
        // Setup
        FileHandler mockFileHandler = getMockFileHandler(false);
        Configuration mockConfiguration = getMockConfiguration();
        ConfigurationAdmin mockConfigurationAdmin = getMockConfigurationAdmin(mockConfiguration,
                false);

        ConfigurationFileListener configurationFileListener = new ConfigurationFileListener(
                mockFileHandler, mockConfigurationAdmin);

        // Perform Test
        configurationFileListener.init();

        // Verify
        verify(mockFileHandler).read(PID);
        verify(mockConfigurationAdmin).getConfiguration(PID, null);
        verify(mockConfiguration).update(PROPERTIES);
        verify(mockFileHandler).registerForChanges(configurationFileListener);
    }

    @Test
    public void testUpdateCreatedChangeType() throws Exception {
        // Setup
        FileHandler mockFileHandler = getMockFileHandler(false);
        Configuration mockConfiguration = getMockConfiguration();
        ConfigurationAdmin mockConfigurationAdmin = getMockConfigurationAdmin(mockConfiguration,
                false);

        ConfigurationFileListener configurationFileListener = new ConfigurationFileListener(
                mockFileHandler, mockConfigurationAdmin);

        // Perform Test
        configurationFileListener.update(PID, ChangeType.CREATED);

        // Verify
        verify(mockFileHandler).read(PID);
        verify(mockConfigurationAdmin).getConfiguration(PID, null);
        verify(mockConfiguration).update(PROPERTIES);
    }

    @Test
    public void testUpdateUpdatedChangeType() throws Exception {
        // Setup
        FileHandler mockFileHandler = getMockFileHandler(false);
        Configuration mockConfiguration = getMockConfiguration();
        ConfigurationAdmin mockConfigurationAdmin = getMockConfigurationAdmin(mockConfiguration,
                false);

        ConfigurationFileListener configurationFileListener = new ConfigurationFileListener(
                mockFileHandler, mockConfigurationAdmin);

        // Perform Test
        configurationFileListener.update(PID, ChangeType.UPDATED);

        // Verify
        verify(mockFileHandler).read(PID);
        verify(mockConfigurationAdmin).getConfiguration(PID, null);
        verify(mockConfiguration).update(PROPERTIES);
    }

    @Test
    public void testUpdateUpdatedChangeTypeConfigAdminThrowsIOException() throws Exception {
        // Setup
        FileHandler mockFileHandler = getMockFileHandler(false);
        Configuration mockConfiguration = getMockConfiguration();
        ConfigurationAdmin mockConfigurationAdmin = getMockConfigurationAdmin(mockConfiguration,
                true);

        ConfigurationFileListener configurationFileListener = new ConfigurationFileListener(
                mockFileHandler, mockConfigurationAdmin);

        // Perform Test
        configurationFileListener.update(PID, ChangeType.UPDATED);

        // Verify
        verify(mockFileHandler).read(PID);
        verify(mockConfigurationAdmin).getConfiguration(PID, null);
        verify(mockConfiguration, times(0)).update(PROPERTIES);
    }

    @Test
    public void testUpdateUpdatedChangeTypeFileHandlerThrowsRuntimeException() throws Exception {
        // Setup
        FileHandler mockFileHandler = getMockFileHandler(true);
        Configuration mockConfiguration = getMockConfiguration();
        ConfigurationAdmin mockConfigurationAdmin = getMockConfigurationAdmin(mockConfiguration,
                false);

        ConfigurationFileListener configurationFileListener = new ConfigurationFileListener(
                mockFileHandler, mockConfigurationAdmin);

        // Perform Test
        configurationFileListener.update(PID, ChangeType.UPDATED);

        // Verify
        verify(mockFileHandler).read(PID);
    }

    @Test
    public void testUpdateDeletedChangeType() throws Exception {
        // Setup
        FileHandler mockFileHandler = getMockFileHandler(false);
        Configuration mockConfiguration = getMockConfiguration();
        ConfigurationAdmin mockConfigurationAdmin = getMockConfigurationAdmin(mockConfiguration,
                false);

        ConfigurationFileListener configurationFileListener = new ConfigurationFileListener(
                mockFileHandler, mockConfigurationAdmin);

        // Perform Test
        configurationFileListener.update(PID, ChangeType.DELETED);

        // Verify
        verify(mockConfigurationAdmin).getConfiguration(PID, null);
        verify(mockConfiguration).delete();
    }

    @Test
    public void testUpdateDeletedChangeTypeConfigAdminThrowsIOException() throws Exception {
        // Setup
        FileHandler mockFileHandler = getMockFileHandler(false);
        Configuration mockConfiguration = getMockConfiguration();
        ConfigurationAdmin mockConfigurationAdmin = getMockConfigurationAdmin(mockConfiguration,
                true);

        ConfigurationFileListener configurationFileListener = new ConfigurationFileListener(
                mockFileHandler, mockConfigurationAdmin);

        // Perform Test
        configurationFileListener.update(PID, ChangeType.DELETED);

        // Verify
        verify(mockConfigurationAdmin).getConfiguration(PID, null);
        verify(mockConfiguration, times(0)).delete();
    }

    private FileHandler getMockFileHandler(boolean throwRuntimeException) {
        Collection<String> pids = new ArrayList<String>(1);
        pids.add(PID);
        FileHandler mockFileHandler = mock(FileHandler.class);
        when(mockFileHandler.getConfigurationPids()).thenReturn(pids);
        if (throwRuntimeException) {
            doThrow(new RuntimeException()).when(mockFileHandler).read(PID);
        } else {
            when(mockFileHandler.read(PID)).thenReturn(PROPERTIES);
        }
        return mockFileHandler;
    }

    private ConfigurationAdmin getMockConfigurationAdmin(Configuration mockConfiguration,
            boolean throwIOException) throws Exception {
        ConfigurationAdmin mockConfigurationAdmin = mock(ConfigurationAdmin.class);
        if (throwIOException) {
            doThrow(new IOException()).when(mockConfigurationAdmin).getConfiguration(PID, null);
        } else {
            when(mockConfigurationAdmin.getConfiguration(PID, null)).thenReturn(mockConfiguration);
        }
        return mockConfigurationAdmin;
    }

    private Configuration getMockConfiguration() {
        Configuration mockConfiguration = mock(Configuration.class);
        return mockConfiguration;
    }
}
