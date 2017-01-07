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
 **/
package org.codice.ddf.admin.api.persist.handlers;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.codice.ddf.admin.api.persist.ConfigHandler;
import org.codice.ddf.admin.api.persist.ConfiguratorException;

/**
 * Transactional handler for persisting property file changes.
 */
public abstract class PropertyConfigHandler implements ConfigHandler<Void, Properties> {
    /**
     * Transactional handler for creating property files
     */
    private static class CreateHandler extends PropertyConfigHandler {
        private CreateHandler(Path configFile, Map<String, String> configs) {
            super(configFile, configs, false);
        }

        @Override
        public Void commit() throws ConfiguratorException {
            Properties properties = new Properties();
            properties.putAll(configs);
            saveProperties(properties);

            return null;
        }

        @Override
        public Void rollback() throws ConfiguratorException {
            boolean delete = configFile.delete();
            if (!delete) {
                LOGGER.debug("Problem deleting properties file {} for rollback", configFile);
            }

            return null;
        }
    }

    /**
     * Transactional handler for deleting property files
     */
    private static class DeleteHandler extends PropertyConfigHandler {
        private DeleteHandler(Path configFile) {
            super(configFile, Collections.emptyMap(), true);
        }

        @Override
        public Void commit() throws ConfiguratorException {
            boolean delete = configFile.delete();
            if (!delete) {
                LOGGER.debug("Problem deleting properties file {} for rollback", configFile);
            }

            return null;
        }
    }

    /**
     * Transactional handler for updating property files
     */
    private static class UpdateHandler extends PropertyConfigHandler {
        private final boolean keepIgnored;

        UpdateHandler(Path configFile, Map<String, String> configs, boolean keepIgnored) {
            super(configFile, configs, true);

            this.keepIgnored = keepIgnored;
        }

        @Override
        public Void commit() throws ConfiguratorException {
            Properties properties = new Properties();

            if (keepIgnored) {
                properties.putAll(currentProperties);
            }
            properties.putAll(configs);
            saveProperties(properties);

            return null;
        }
    }

    protected final File configFile;

    protected final Map<String, String> configs;

    protected final Properties currentProperties;

    private PropertyConfigHandler(Path configFile, Map<String, String> configs,
            boolean loadCurrentProps) {
        this.configFile = configFile.toFile();
        this.configs = new HashMap<>(configs);

        if (loadCurrentProps) {
            Properties result;
            try {
                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream(configFile.toFile())) {
                    props.load(in);
                }

                result = props;
            } catch (IOException e) {
                throw new ConfiguratorException(String.format(
                        "Error reading configuration from file %s",
                        configFile.toFile()
                                .getName()));
            }

            currentProperties = result;
        } else {
            currentProperties = new Properties();
        }
    }

    /**
     * Creates a handler for persisting property file changes to a new property file.
     *
     * @param configFile the property file to be created
     * @param configs    map of key:value pairs to be written to the property file
     * @return instance of this class
     */
    public static PropertyConfigHandler forCreate(Path configFile, Map<String, String> configs) {
        return new PropertyConfigHandler.CreateHandler(configFile, configs);
    }

    /**
     * Creates a handler for deleting a property file.
     *
     * @param configFile the property file to be deleted
     * @return instance of this class
     */
    public static ConfigHandler forDelete(Path configFile) {
        return new PropertyConfigHandler.DeleteHandler(configFile);
    }

    /**
     * Creates a handler for persisting property file changes to an existing property file.
     *
     * @param configFile  the property file to be updated
     * @param configs     map of key:value pairs to be written to the property file
     * @param keepIgnored if true, any keys in the current property file that are not in the
     *                    {@code configs} map will be left with their initial values; if false, they
     *                    will be removed from the file
     * @return instance of this class
     */
    public static PropertyConfigHandler forUpdate(Path configFile, Map<String, String> configs,
            boolean keepIgnored) {
        return new PropertyConfigHandler.UpdateHandler(configFile, configs, keepIgnored);
    }


    @Override
    public Void rollback() throws ConfiguratorException {
        saveProperties(currentProperties);

        return null;
    }

    @Override
    public Properties readState() throws ConfiguratorException {
        return currentProperties;
    }

    protected void saveProperties(Properties properties) throws ConfiguratorException {
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            properties.store(out, null);
        } catch (IOException e) {
            LOGGER.debug("Error writing properties to file {}", configFile, e);
            throw new ConfiguratorException("Error writing properties to file");
        }
    }
}
