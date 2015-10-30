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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationFileFactoryTest {

    private static final String PID = "my.pid";

    private static final String FACTORY_PID = "FactoryPid";

    @Mock
    private Path mockPath;

    @Mock
    private ConfigurationAdmin configAdmin;

    @Test
    public void testCreateConfigurationFileForManagedService() throws Exception {
        // Setup
        Dictionary<String, Object> properties = new Hashtable<>(1);
        properties.put(Constants.SERVICE_PID, PID);
        InputStream mockInputStream = getMockInputStream();
        PersistenceStrategy mockPersistenceStrategy = getMockPersistenceStrategy(mockInputStream,
                properties);
        ConfigurationFileFactory factory = new ConfigurationFileFactory(mockPersistenceStrategy,
                configAdmin) {
            @Override
            InputStream getInputStream(Path path) throws FileNotFoundException {
                return mockInputStream;
            }
        };

        // Perform Test
        ConfigurationFile configFile = factory.createConfigurationFile(mockPath);

        // Verify
        assertThat(configFile, instanceOf(ManagedServiceConfigurationFile.class));
    }

    @Test
    public void testCreateConfigurationFileForManagedServiceFactory() throws Exception {
        // Setup
        Dictionary<String, Object> properties = new Hashtable<>(1);
        properties.put(ConfigurationAdmin.SERVICE_FACTORYPID, FACTORY_PID);
        InputStream mockInputStream = getMockInputStream();
        PersistenceStrategy mockPersistenceStrategy = getMockPersistenceStrategy(mockInputStream,
                properties);
        ConfigurationFileFactory factory = new ConfigurationFileFactory(mockPersistenceStrategy,
                configAdmin) {
            @Override
            InputStream getInputStream(Path path) throws FileNotFoundException {
                return mockInputStream;
            }
        };

        // Perform Test
        ConfigurationFile configFile = factory.createConfigurationFile(mockPath);

        // Verify
        assertThat(configFile, instanceOf(ManagedServiceFactoryConfigurationFile.class));
    }

    @Test(expected = ConfigurationFileException.class)
    public void testCreateConfigurationFileNoServicePidOrFactoryPid() throws Exception {
        // Setup
        Dictionary<String, Object> properties = new Hashtable<>(1);
        properties.put("prop1", "value1");
        InputStream mockInputStream = getMockInputStream();
        PersistenceStrategy mockPersistenceStrategy = getMockPersistenceStrategy(mockInputStream,
                properties);
        ConfigurationFileFactory factory = new ConfigurationFileFactory(mockPersistenceStrategy,
                configAdmin) {
            @Override
            InputStream getInputStream(Path path) throws FileNotFoundException {
                return mockInputStream;
            }
        };

        // Perform Test
        factory.createConfigurationFile(mockPath);
    }

    @Test(expected = ConfigurationFileException.class)
    public void testCreateConfigurationFileIOExceptionOnReadOfConfigFile() throws Exception {
        // Setup
        PersistenceStrategy mockPersistenceStrategy = mock(PersistenceStrategy.class);
        InputStream mockInputStream = getMockInputStream();
        when(mockPersistenceStrategy.read(mockInputStream)).thenThrow(new IOException());
        ConfigurationFileFactory factory = new ConfigurationFileFactory(mockPersistenceStrategy,
                configAdmin) {
            @Override
            InputStream getInputStream(Path path) throws FileNotFoundException {
                return mockInputStream;
            }
        };

        // Perform Test
        factory.createConfigurationFile(mockPath);
    }

    @Test(expected = ConfigurationFileException.class)
    public void testCreateConfigurationFileFileNotFoundExceptionOnGetInputStream() throws Exception {
        // Setup
        Dictionary<String, Object> properties = new Hashtable<>(1);
        properties.put(Constants.SERVICE_PID, PID);
        InputStream mockInputStream = getMockInputStream();
        PersistenceStrategy mockPersistenceStrategy = getMockPersistenceStrategy(mockInputStream,
                properties);
        ConfigurationFileFactory factory = new ConfigurationFileFactory(mockPersistenceStrategy,
                configAdmin) {
            @Override
            InputStream getInputStream(Path path) throws FileNotFoundException {
                throw new FileNotFoundException();
            }
        };

        // Perform Test
        factory.createConfigurationFile(mockPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateConfigurationFileNullInput() throws Exception {
        // Setup
        PersistenceStrategy mockPersistenceStrategy = mock(PersistenceStrategy.class);
        ConfigurationFileFactory factory = new ConfigurationFileFactory(mockPersistenceStrategy,
                configAdmin);

        // Perform Test
        factory.createConfigurationFile(null);
    }

    private PersistenceStrategy getMockPersistenceStrategy(InputStream mockInputStream,
            Dictionary<String, Object> properties) throws Exception {
        PersistenceStrategy mockPersistenceStrategy = mock(PersistenceStrategy.class);
        when(mockPersistenceStrategy.read(mockInputStream)).thenReturn(properties);
        return mockPersistenceStrategy;
    }

    private InputStream getMockInputStream() {
        InputStream inputStream = mock(InputStream.class);
        return inputStream;
    }
}
