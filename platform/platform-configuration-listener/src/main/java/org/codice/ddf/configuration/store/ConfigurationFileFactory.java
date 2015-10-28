
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Dictionary;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationFileFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFileFactory.class);

    private PersistenceStrategy pesistenceStrategy;

    private ConfigurationAdmin configAdmin;

    public ConfigurationFileFactory(PersistenceStrategy pesistenceStrategy,
            ConfigurationAdmin configAdmin) {
        this.pesistenceStrategy = pesistenceStrategy;
        this.configAdmin = configAdmin;
    }

    public ConfigurationFile createConfigurationFile(Path configurationFile)
        throws ConfigurationFileException {
        Dictionary<String, Object> properties = read(configurationFile);
        if (isManagedServiceFactoryConfiguration(properties)) {
            return new ManagedServiceFactoryConfigurationFile(configurationFile, properties,
                    configAdmin);
        } else if (isManagedServiceConfiguration(properties)) {
            return new ManagedServiceConfigurationFile(configurationFile, properties, configAdmin);
        } else {
            LOGGER.error(
                    "Unable to determine type of configuration. Unable to find property [{}] or property [{}] in [{}].",
                    Constants.SERVICE_PID, ConfigurationAdmin.SERVICE_FACTORYPID,
                    configurationFile.toString());
            throw new ConfigurationFileException(
                    "Unable to determine type of configuration. Unable to find property ["
                            + Constants.SERVICE_PID + "] or property ["
                            + ConfigurationAdmin.SERVICE_FACTORYPID + "] in ["
                            + configurationFile.toString() + "].");
        }
    }

    private boolean isManagedServiceFactoryConfiguration(Dictionary<String, Object> properties) {
        String factoryPid = (String) properties.get(ConfigurationAdmin.SERVICE_FACTORYPID);
        return factoryPid != null;
    }

    private boolean isManagedServiceConfiguration(Dictionary<String, Object> properties) {
        String servicePid = (String) properties.get(Constants.SERVICE_PID);
        return servicePid != null;
    }

    private InputStream getInputStream(Path path) throws FileNotFoundException {
        return new FileInputStream(path.toFile());
    }

    private Dictionary<String, Object> read(Path configurationFile)
        throws ConfigurationFileException {
        Dictionary<String, Object> properties = null;
        try {
            properties = pesistenceStrategy.read(getInputStream(configurationFile));
        } catch (IOException e) {
            throw new ConfigurationFileException("Unable to read configuration file ["
                    + configurationFile.toString() + "].", e);
        }
        return properties;
    }
}
