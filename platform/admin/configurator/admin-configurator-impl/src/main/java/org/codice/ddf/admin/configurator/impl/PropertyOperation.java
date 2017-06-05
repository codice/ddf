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

import static org.codice.ddf.admin.configurator.impl.ConfigValidator.validateMap;
import static org.codice.ddf.admin.configurator.impl.ConfigValidator.validatePropertiesPath;

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
import org.codice.ddf.admin.configurator.Operation;
import org.codice.ddf.admin.configurator.Result;
import org.codice.ddf.internal.admin.configurator.actions.PropertyActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transactional handler for persisting property file changes.
 * <p>
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public abstract class PropertyOperation implements Operation<Void> {
    public static class Actions implements PropertyActions {
        @Override
        public PropertyOperation create(Path propFile, Map<String, String> configs)
                throws ConfiguratorException {
            validatePropertiesPath(propFile);
            validateMap(configs, "Missing properties");
            return new PropertyOperation.CreateHandler(propFile, configs);
        }

        @Override
        public PropertyOperation delete(Path propFile) throws ConfiguratorException {
            validatePropertiesPath(propFile);
            return new PropertyOperation.DeleteHandler(propFile);
        }

        @Override
        public PropertyOperation update(Path propFile, Map<String, String> configs,
                boolean keepIfNotPresent) throws ConfiguratorException {
            validatePropertiesPath(propFile);
            validateMap(configs, "Missing properties");
            return new PropertyOperation.UpdateHandler(propFile, configs, keepIfNotPresent);
        }

        @Override
        public Map<String, String> getProperties(Path propFile) throws ConfiguratorException {
            validatePropertiesPath(propFile);
            return new PropertyOperation.UpdateHandler(propFile,
                    Collections.emptyMap(),
                    true).readState();
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyOperation.class);

    /**
     * Transactional handler for creating property files
     */
    private static class CreateHandler extends PropertyOperation {
        private CreateHandler(Path configFile, Map<String, String> configs) {
            super(configFile, configs, false);
        }

        @Override
        public Result<Void> commit() throws ConfiguratorException {
            Map<String, String> propertyMap = new HashMap<>();
            propertyMap.putAll(configs);
            saveProperties(propertyMap);

            return ResultImpl.pass();
        }

        @Override
        public Result<Void> rollback() throws ConfiguratorException {
            boolean delete = configFile.delete();
            if (!delete) {
                LOGGER.debug("Problem deleting properties file {} for rollback", configFile);
            }

            return ResultImpl.rollback();
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
        public Result<Void> commit() throws ConfiguratorException {
            boolean delete = configFile.delete();
            if (!delete) {
                LOGGER.debug("Problem deleting properties file {}", configFile);
            }

            return ResultImpl.pass();
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
        public Result<Void> commit() throws ConfiguratorException {
            Map<String, String> propertyMap = new HashMap<>();

            if (keepIgnored) {
                propertyMap.putAll(currentProperties);
            }
            propertyMap.putAll(configs);
            saveProperties(propertyMap);

            return ResultImpl.pass();
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

    @Override
    public Result<Void> rollback() throws ConfiguratorException {
        saveProperties(currentProperties);

        return ResultImpl.rollback();
    }

    Map<String, String> readState() throws ConfiguratorException {
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
