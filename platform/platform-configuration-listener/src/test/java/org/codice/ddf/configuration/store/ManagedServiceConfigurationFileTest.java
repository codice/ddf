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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(MockitoJUnitRunner.class)
public class ManagedServiceConfigurationFileTest {

    private static final String PID = "my.pid";

    @Mock
    private Path mockPath;

    @Mock
    private ConfigurationAdmin mockConfigAdmin;

    @Mock
    private PersistenceStrategy mockPersistenceStrategy;

    @Mock
    private Configuration mockConfiguration;

    private Dictionary<String, Object> properties;

    @Before
    public void setUp() {
        properties = new Hashtable<>(1);
        properties.put(Constants.SERVICE_PID, PID);
    }

    @Test
    public void testCreateConfig() throws Exception {
        // Setup
        when(mockConfigAdmin.getConfiguration(PID, null)).thenReturn(mockConfiguration);
        ConfigurationFile configFile = new ManagedServiceConfigurationFile(mockPath, properties,
                mockConfigAdmin, mockPersistenceStrategy);

        // Perform Test
        configFile.createConfig();

        // Verify
        verify(mockConfiguration).update(properties);
    }

    @Test(expected = ConfigurationFileException.class)
    public void testCreateConfigGetConfigurationFails() throws Exception {
        // Setup
        when(mockConfigAdmin.getConfiguration(PID, null)).thenThrow(new IOException());
        ConfigurationFile configFile = new ManagedServiceConfigurationFile(mockPath, properties,
                mockConfigAdmin, mockPersistenceStrategy);

        // Perform Test
        configFile.createConfig();
    }

    @Test(expected = ConfigurationFileException.class)
    public void testCreateConfigConfigurationUpdateFails() throws Exception {
        // Setup
        doThrow(IOException.class).when(mockConfiguration).update(properties);
        when(mockConfigAdmin.getConfiguration(PID, null)).thenReturn(mockConfiguration);
        ConfigurationFile configFile = new ManagedServiceConfigurationFile(mockPath, properties,
                mockConfigAdmin, mockPersistenceStrategy);

        // Perform Test
        configFile.createConfig();
    }
}
