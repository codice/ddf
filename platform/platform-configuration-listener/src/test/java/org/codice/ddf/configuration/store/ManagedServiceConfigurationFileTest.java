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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ManagedServiceConfigurationFileTest {

    private static final String PID = "my.pid";

    @Test
    public void testCreateConfig() throws Exception {
        // Setup
        Dictionary<String, Object> properties = getProperties();
        Path mockPath = getMockPath();
        Configuration mockConfiguration = getMockConfiguration();
        ConfigurationAdmin mockConfigAdmin = getMockConfigurationAdmin();
        when(mockConfigAdmin.getConfiguration(PID, null)).thenReturn(mockConfiguration);
        ConfigurationFile configFile = new ManagedServiceConfigurationFile(mockPath, properties,
                mockConfigAdmin);

        // Perform Test
        configFile.createConfig();

        // Verify
        verify(mockConfiguration).update(properties);
    }

    @Test(expected = ConfigurationFileException.class)
    public void testCreateConfigNullProperties() throws Exception {
        // Setup
        Dictionary<String, Object> properties = null;
        Path mockPath = getMockPath();
        Configuration mockConfiguration = getMockConfiguration();
        ConfigurationAdmin mockConfigAdmin = getMockConfigurationAdmin();
        when(mockConfigAdmin.getConfiguration(PID, null)).thenReturn(mockConfiguration);
        ConfigurationFile configFile = new ManagedServiceConfigurationFile(mockPath, properties,
                mockConfigAdmin);

        // Perform Test
        configFile.createConfig();
    }

    @Test(expected = ConfigurationFileException.class)
    public void testCreateConfigGetConfigurationFails() throws Exception {
        // Setup
        Dictionary<String, Object> properties = getProperties();
        Path mockPath = getMockPath();
        ConfigurationAdmin mockConfigAdmin = getMockConfigurationAdmin();
        when(mockConfigAdmin.getConfiguration(PID, null)).thenThrow(new IOException());
        ConfigurationFile configFile = new ManagedServiceConfigurationFile(mockPath, properties,
                mockConfigAdmin);

        // Perform Test
        configFile.createConfig();
    }

    @Test(expected = ConfigurationFileException.class)
    public void testCreateConfigConfigurationUpdateFails() throws Exception {
        // Setup
        Dictionary<String, Object> properties = getProperties();
        Path mockPath = getMockPath();
        Configuration mockConfiguration = getMockConfiguration();
        doThrow(IOException.class).when(mockConfiguration).update(properties);
        ConfigurationAdmin mockConfigAdmin = getMockConfigurationAdmin();
        when(mockConfigAdmin.getConfiguration(PID, null)).thenReturn(mockConfiguration);
        ConfigurationFile configFile = new ManagedServiceConfigurationFile(mockPath, properties,
                mockConfigAdmin);

        // Perform Test
        configFile.createConfig();
    }

    private ConfigurationAdmin getMockConfigurationAdmin() {
        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);
        return configAdmin;
    }

    private Configuration getMockConfiguration() {
        Configuration configuration = mock(Configuration.class);
        return configuration;
    }

    private Dictionary<String, Object> getProperties() {
        Dictionary<String, Object> properties = new Hashtable<>(1);
        properties.put(Constants.SERVICE_PID, PID);
        return properties;
    }

    private Path getMockPath() {
        Path path = mock(Path.class);
        return path;
    }
}
