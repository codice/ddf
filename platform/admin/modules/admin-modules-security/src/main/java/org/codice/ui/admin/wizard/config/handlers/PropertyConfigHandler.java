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
package org.codice.ui.admin.wizard.config.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.codice.ui.admin.wizard.config.ConfigHandler;
import org.codice.ui.admin.wizard.config.ConfiguratorException;

/**
 * Transactional handler for persisting property file changes.
 */
public class PropertyConfigHandler implements ConfigHandler<Void> {
    private final File configFile;

    private final Map<String, String> configs;

    private final boolean keepIgnored;

    private Properties currentProperties;

    private PropertyConfigHandler(Path configFile, Map<String, String> configs,
            boolean keepIgnored) {
        this.configFile = configFile.toFile();
        this.configs = new HashMap<>(configs);
        this.keepIgnored = keepIgnored;

        try {
            try (FileInputStream in = new FileInputStream(configFile.toFile())) {
                currentProperties = new Properties();
                currentProperties.load(in);
            }
        } catch (IOException e) {
            throw new ConfiguratorException(String.format("Error reading configuration from file %s",
                    configFile.toFile()
                            .getName()));
        }
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
    public static PropertyConfigHandler instance(Path configFile, Map<String, String> configs,
            boolean keepIgnored) {
        return new PropertyConfigHandler(configFile, configs, keepIgnored);
    }

    @Override
    public Void commit() throws ConfiguratorException {
        Properties properties = new Properties();

        if (keepIgnored) {
            properties.putAll(currentProperties);
        }
        properties.putAll(configs);

        try {
            saveProperties(properties);
        } catch (IOException e) {
            throw new ConfiguratorException(String.format("Error writing configuration to file %s",
                    configFile.getName()));
        }

        return null;
    }

    @Override
    public Void rollback() throws ConfiguratorException {
        try {
            saveProperties(currentProperties);
        } catch (IOException e) {
            throw new ConfiguratorException(String.format(
                    "Error rolling back configuration to file %s",
                    configFile.getName()));
        }

        return null;
    }

    private void saveProperties(Properties properties) throws IOException {
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            properties.store(out, null);
        }
    }
}
