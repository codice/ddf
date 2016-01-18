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
package org.codice.ddf.configuration.admin;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Dictionary;

import org.codice.ddf.configuration.persistence.PersistenceStrategy;
import org.codice.ddf.configuration.status.ConfigurationFileException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationFileTest {

    @Mock
    private Path path;

    @Mock
    private Dictionary<String, Object> properties;

    @Mock
    private ConfigurationAdmin configAdmin;

    @Mock
    private PersistenceStrategy persistenceStrategy;

    @Mock
    private FileOutputStream fileOutputStream;

    private class ConfigurationFileUnderTest extends ConfigurationFile {
        public ConfigurationFileUnderTest(Path configFilePath,
                Dictionary<String, Object> properties, ConfigurationAdmin configAdmin,
                PersistenceStrategy persistenceStrategy) {
            super(configFilePath, properties, configAdmin, persistenceStrategy);
        }

        @Override
        public void createConfig() throws ConfigurationFileException {
        }

        @Override
        public FileOutputStream getOutputStream(String destination) throws FileNotFoundException {
            return fileOutputStream;
        }
    }

    @Test
    public void constructor() {
        ConfigurationFileUnderTest configurationFile = new ConfigurationFileUnderTest(path,
                properties, configAdmin, persistenceStrategy);
        assertThat(configurationFile.getConfigFilePath(), sameInstance(path));
    }

    @Test
    public void testExportConfig() throws IOException {
        ConfigurationFileUnderTest configurationFile = new ConfigurationFileUnderTest(path,
                properties, configAdmin, persistenceStrategy);
        configurationFile.exportConfig("");
        verify(persistenceStrategy, atLeastOnce())
                .write(any(FileOutputStream.class), same(properties));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportWithNullDestination() throws IOException {
        ConfigurationFileUnderTest configurationFile = new ConfigurationFileUnderTest(path,
                properties, configAdmin, persistenceStrategy);
        configurationFile.exportConfig(null);
    }
}
