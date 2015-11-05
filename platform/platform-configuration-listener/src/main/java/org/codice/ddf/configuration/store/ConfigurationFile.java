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

import java.nio.file.Path;
import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Base class for configuration file objects. Configuration file objects can be created using the
 * {@link ConfigurationFileFactory} class.
 */
public abstract class ConfigurationFile {

    protected final Dictionary<String, Object> properties;

    protected final ConfigurationAdmin configAdmin;

    protected final Path configFilePath;

    /**
     * Constructor called by {@link ConfigurationFileFactory}. Assumes that none of the parameters
     * are {@code null}.
     *
     * @param configFilePath path to the configuration file
     * @param properties     properties associated with the configuration file
     * @param configAdmin    reference to OSGi's {@link ConfigurationAdmin}
     */
    ConfigurationFile(Path configFilePath, Dictionary<String, Object> properties,
            ConfigurationAdmin configAdmin) {
        this.configFilePath = configFilePath;
        this.properties = properties;
        this.configAdmin = configAdmin;
    }

    /**
     * Gets the configuration file location
     *
     * @return configuration file location
     */
    public Path getConfigFilePath() {
        return configFilePath;
    }

    public abstract void createConfig() throws ConfigurationFileException;
}
