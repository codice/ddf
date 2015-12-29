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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Dictionary;

import org.codice.ddf.configuration.persistence.PersistenceStrategy;
import org.codice.ddf.configuration.status.ConfigurationFileException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that encapsulates a Managed Service Factory configuration file. Created by the
 * {@link ConfigurationFileFactory} class.
 */
public class ManagedServiceFactoryConfigurationFile extends ConfigurationFile {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ManagedServiceFactoryConfigurationFile.class);

    ManagedServiceFactoryConfigurationFile(Path configFilePath,
            Dictionary<String, Object> properties, ConfigurationAdmin configAdmin,
            PersistenceStrategy persistenceStrategy) {
        super(configFilePath, properties, configAdmin, persistenceStrategy);
    }

    @Override
    public void createConfig() throws ConfigurationFileException {

        String factoryPid = getFactoryPid();

        try {
            Configuration configuration = configAdmin.createFactoryConfiguration(factoryPid, null);
            configuration.update(properties);
        } catch (IOException e) {
            String message = String
                    .format("Unable to get or update Configuration for factory pid [%s].",
                            factoryPid);
            LOGGER.error(message, e);
            throw new ConfigurationFileException(message, e);
        }
    }

    private String getFactoryPid() {
        return (String) properties.get(ConfigurationAdmin.SERVICE_FACTORYPID);
    }

    public static class ManagedServiceFactoryConfigurationFileBuilder
            extends ConfigurationFileBuilder {

        public ManagedServiceFactoryConfigurationFileBuilder(ConfigurationAdmin configAdmin,
                PersistenceStrategy persistenceStrategy) {
            super(configAdmin, persistenceStrategy);
        }

        @Override
        public ConfigurationFile build() {
            return new ManagedServiceFactoryConfigurationFile(configFilePath, properties,
                    configAdmin, persistenceStrategy);
        }
    }
}
