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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.validation.constraints.NotNull;

import org.codice.ddf.configuration.persistence.PersistenceStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationFileFactoryTest {

    private static final String PID = "my.pid";

    @Mock
    private ConfigurationAdmin configAdmin;

    private class ConfigurationFileFactoryUnderTest extends ConfigurationFileFactory {

        public ConfigurationFileFactoryUnderTest(@NotNull PersistenceStrategy persistenceStrategy,
                @NotNull ConfigurationAdmin configAdmin) {
            super(persistenceStrategy, configAdmin);
        }

    }

    @Test
    public void testCreateConfigurationFileFromProperties() throws Exception {
        // Setup
        Dictionary<String, Object> properties = new Hashtable<>(1);
        properties.put(Constants.SERVICE_PID, PID);
        PersistenceStrategy mockPersistenceStrategy = getMockPersistenceStrategy();
        ConfigurationFileFactory factory = new ConfigurationFileFactoryUnderTest(
                mockPersistenceStrategy,
                configAdmin);

        // Perform Test
        ConfigurationFile configFile = factory.createConfigurationFile(properties);

        // Verify
        assertThat(configFile, instanceOf(ManagedServiceConfigurationFile.class));
    }

    private PersistenceStrategy getMockPersistenceStrategy() {
        return mock(PersistenceStrategy.class);
    }
}
