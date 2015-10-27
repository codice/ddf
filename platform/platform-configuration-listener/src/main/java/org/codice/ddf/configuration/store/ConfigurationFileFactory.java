
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

    private Path processedDirectory;

    private Path failedDirectory;

    public ConfigurationFileFactory(PersistenceStrategy pesistenceStrategy,
            Path processedDirectory, Path failedDirectory) {
        this.pesistenceStrategy = pesistenceStrategy;
    }

    public ConfigurationFile createConfigurationFile(Path configurationFile) throws IOException {
        Dictionary<String, Object> properties = pesistenceStrategy
                .read(getInputStream(configurationFile));
        if (isManagedServiceFactoryConfiguration(properties)) {
            return new ManagedServiceFactoryConfigurationFile(configurationFile,
                    processedDirectory, failedDirectory, properties);
        } else if (isManagedServiceConfiguration(properties)) {
            return new ManagedServiceConfigurationFile(configurationFile, processedDirectory,
                    failedDirectory, properties);
        } else {
            LOGGER.error("ERROR");
            return null;
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
}
