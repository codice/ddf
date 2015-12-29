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
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that encapsulates a Managed Service configuration file. Created by the
 * {@link ConfigurationFileFactory} class.
 */
public class ManagedServiceConfigurationFile extends ConfigurationFile {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ManagedServiceConfigurationFile.class);

    ManagedServiceConfigurationFile(Path configFilePath, Dictionary<String, Object> properties,
            ConfigurationAdmin configAdmin, PersistenceStrategy persistenceStrategy) {
        super(configFilePath, properties, configAdmin, persistenceStrategy);
    }

    @Override
    public void createConfig() throws ConfigurationFileException {

        String servicePid = getServicePid();

        try {
            Configuration configuration = configAdmin.getConfiguration(servicePid, null);
            configuration.update(properties);
        } catch (IOException e) {
            String message = String
                    .format("Unable to get or update Configuration for pid [%s].", servicePid);
            LOGGER.error(message, e);
            throw new ConfigurationFileException(message, e);
        }
    }

    private String getServicePid() {
        return (String) properties.get(Constants.SERVICE_PID);
    }

    public static class ManagedServiceConfigurationFileBuilder extends ConfigurationFileBuilder {

        public ManagedServiceConfigurationFileBuilder(ConfigurationAdmin configAdmin,
                PersistenceStrategy persistenceStrategy) {
            super(configAdmin, persistenceStrategy);
        }

        @Override
        public ConfigurationFile build() {
            return new ManagedServiceConfigurationFile(configFilePath, properties, configAdmin,
                    persistenceStrategy);
        }
    }
}
