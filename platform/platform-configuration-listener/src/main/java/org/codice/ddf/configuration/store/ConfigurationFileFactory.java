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

import static org.apache.commons.lang.Validate.notNull;
import static org.codice.ddf.configuration.store.ConfigurationFile.ConfigurationFileBuilder;
import static org.codice.ddf.configuration.store.ManagedServiceConfigurationFile.ManagedServiceConfigurationFileBuilder;
import static org.codice.ddf.configuration.store.ManagedServiceFactoryConfigurationFile.ManagedServiceFactoryConfigurationFileBuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Dictionary;

import javax.validation.constraints.NotNull;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class used to create the proper {@link ConfigurationFile} sub-class based on a
 * configuration file content.
 */
public class ConfigurationFileFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFileFactory.class);

    private final PersistenceStrategy persistenceStrategy;

    private final ConfigurationAdmin configAdmin;

    /**
     * Constructor.
     *
     * @param persistenceStrategy object used to read in the configuration file content
     * @param configAdmin         configuration admin that will be used to create Managed Service Factory
     *                            configuration objects
     */
    public ConfigurationFileFactory(@NotNull PersistenceStrategy persistenceStrategy,
            @NotNull ConfigurationAdmin configAdmin) {
        notNull(persistenceStrategy, "persistenceStrategy cannot be null");
        notNull(configAdmin, "configAdmin cannot be null");

        this.persistenceStrategy = persistenceStrategy;
        this.configAdmin = configAdmin;
    }

    /**
     * Instantiates a new {@link ConfigurationFile} sub-class based on the content of the
     * configuration file provided.
     *
     * @param configurationFile path to the configuration file that will be used to create the new
     *                          {@link ConfigurationFile} object
     * @return new {@link ConfigurationFile} object of the proper type. Never {@code null},
     * @throws ConfigurationFileException thrown if the {@link ConfigurationFile} object couldn't
     *                                    be created because the type could not be determined or
     *                                    the configuration file couldn't be read
     * @throws IllegalArgumentException   thrown if the path provided is {@code null}
     */
    public ConfigurationFile createConfigurationFile(@NotNull Path configurationFile)
            throws ConfigurationFileException {
        notNull(configurationFile, "configurationFile cannot be null");

        return getConfigurationFileBuilder(read(configurationFile))
                .configFilePath(configurationFile).build();
    }

    /**
     * Instantiates a new {@link ConfigurationFile} sub-class based on the content of the
     * properties dictionary provided.
     *
     * @param properties dictionary of properties used to create the {@link ConfigurationFile} object
     * @return new {@link ConfigurationFile} object of the proper type. Never {@code null},
     * @throws ConfigurationFileException thrown if the {@link ConfigurationFile} object couldn't
     *                                    be created because the type could not be determined
     */
    public ConfigurationFile createConfigurationFile(@NotNull Dictionary<String, Object> properties)
            throws ConfigurationFileException {
        return getConfigurationFileBuilder(properties).build();
    }

    private ConfigurationFileBuilder getConfigurationFileBuilder(
            Dictionary<String, Object> properties) throws ConfigurationFileException {
        ConfigurationFileBuilder configurationFileBuilder;
        if (isManagedServiceFactoryConfiguration(properties)) {
            configurationFileBuilder = new ManagedServiceFactoryConfigurationFileBuilder(
                    configAdmin, persistenceStrategy);
        } else if (isManagedServiceConfiguration(properties)) {
            configurationFileBuilder = new ManagedServiceConfigurationFileBuilder(configAdmin,
                    persistenceStrategy);
        } else {
            String message = String.format("Unable to determine type of configuration. "
                            + "Unable to find property [%s] or property [%s] that contained [%s].",
                    Constants.SERVICE_PID, ConfigurationAdmin.SERVICE_FACTORYPID, properties);
            LOGGER.error(message);
            throw new ConfigurationFileException(message);
        }
        return configurationFileBuilder.properties(properties);
    }

    private boolean isManagedServiceFactoryConfiguration(Dictionary<String, Object> properties) {
        return properties.get(ConfigurationAdmin.SERVICE_FACTORYPID) != null;
    }

    private boolean isManagedServiceConfiguration(Dictionary<String, Object> properties) {
        return properties.get(Constants.SERVICE_PID) != null;
    }

    private Dictionary<String, Object> read(Path configurationFile)
            throws ConfigurationFileException {
        try (InputStream inputStream = getInputStream(configurationFile)) {
            return persistenceStrategy.read(inputStream);
        } catch (ConfigurationFileException | IOException e) {
            String message = String.format("Unable to read configuration file [%s].",
                    configurationFile.toString());
            LOGGER.error(message, e);
            throw new ConfigurationFileException(message, e);
        }
    }

    InputStream getInputStream(Path path) throws FileNotFoundException {
        return new FileInputStream(path.toFile());
    }
}
