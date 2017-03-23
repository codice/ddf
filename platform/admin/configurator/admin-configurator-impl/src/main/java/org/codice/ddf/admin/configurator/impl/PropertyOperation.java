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
package org.codice.ddf.admin.configurator.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.codice.ddf.admin.configurator.ConfiguratorException;

/**
 * Transactional handler for persisting property file changes.
 * <p>
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public abstract class PropertyOperation
        implements OperationBase, Operation<Void, Map<String, String>> {
    /**
     * Transactional handler for creating property files
     */
    private static class CreateHandler extends PropertyOperation {
        private CreateHandler(Path configFile, Map<String, String> configs) {
            super(configFile, configs, false);
        }

        @Override
        public Void commit() throws ConfiguratorException {
            Map<String, String> propertyMap = new HashMap<>();
            propertyMap.putAll(configs);
            saveProperties(propertyMap);

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
    private static class DeleteHandler extends PropertyOperation {
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
    private static class UpdateHandler extends PropertyOperation {
        private final boolean keepIgnored;

        UpdateHandler(Path configFile, Map<String, String> configs, boolean keepIgnored) {
            super(configFile, configs, true);

            this.keepIgnored = keepIgnored;
        }

        @Override
        public Void commit() throws ConfiguratorException {
            Map<String, String> propertyMap = new HashMap<>();

            if (keepIgnored) {
                propertyMap.putAll(currentProperties);
            }
            propertyMap.putAll(configs);
            saveProperties(propertyMap);

            return null;
        }
    }

    final File configFile;

    protected final Map<String, String> configs;

    final Map<String, String> currentProperties;

    private PropertyOperation(Path configFile, Map<String, String> configs,
            boolean loadCurrentProps) {
        this.configFile = configFile.toFile();
        this.configs = new HashMap<>(configs);

        currentProperties = new HashMap<>();
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

            result.keySet()
                    .stream()
                    .map(String.class::cast)
                    .forEach(k -> currentProperties.put(k, result.getProperty(k)));
        }
    }

    /**
     * Creates a handler for persisting property file changes to a new property file.
     *
     * @param configFile the property file to be created
     * @param configs    map of key:value pairs to be written to the property file
     * @return instance of this class
     */
    public static PropertyOperation forCreate(Path configFile, Map<String, String> configs) {
        return new PropertyOperation.CreateHandler(configFile, configs);
    }

    /**
     * Creates a handler for deleting a property file.
     *
     * @param configFile the property file to be deleted
     * @return instance of this class
     */
    public static Operation forDelete(Path configFile) {
        return new PropertyOperation.DeleteHandler(configFile);
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
    public static PropertyOperation forUpdate(Path configFile, Map<String, String> configs,
            boolean keepIgnored) {
        return new PropertyOperation.UpdateHandler(configFile, configs, keepIgnored);
    }

    @Override
    public Void rollback() throws ConfiguratorException {
        saveProperties(currentProperties);

        return null;
    }

    @Override
    public Map<String, String> readState() throws ConfiguratorException {
        return Collections.unmodifiableMap(currentProperties);
    }

    void saveProperties(Map<String, String> propertyMap) throws ConfiguratorException {
        Properties properties = new Properties();
        properties.putAll(propertyMap);
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            properties.store(out, null);
        } catch (IOException e) {
            LOGGER.debug("Error writing properties to file {}", configFile, e);
            throw new ConfiguratorException("Error writing properties to file");
        }
    }
}
