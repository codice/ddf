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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.management.MalformedObjectNameException;

import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.configurator.Operation;
import org.codice.ddf.ui.admin.api.ConfigurationAdminMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transactional handler for persisting bundle configuration file changes.
 */
public class AdminOperation implements OperationBase, Operation<Void, Map<String, Object>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminOperation.class);

    private final String pid;

    private final Map<String, Object> configs;

    private final boolean keepIgnored;

    private final ConfigurationAdminMBean cfgAdmMbean;

    private Map<String, Object> currentProperties;

    private AdminOperation(String pid, Map<String, Object> configs, boolean keepIgnored,
            ConfigurationAdminMBean cfgAdmMbean) {
        this.pid = pid;
        this.configs = new HashMap<>(configs);
        this.keepIgnored = keepIgnored;
        this.cfgAdmMbean = cfgAdmMbean;

        try {
            currentProperties = cfgAdmMbean.getProperties(pid);
        } catch (IOException e) {
            LOGGER.debug("Error getting current configuration for pid {}", pid, e);
            throw new ConfiguratorException("Internal error");
        }
    }

    /**
     * Creates a handler for persisting changes to a bundle configuration.
     *
     * @param pid                     the configPid of the bundle configuration to be updated
     * @param configs                 map of key:value pairs to be written to the configuration
     * @param keepIgnored             if true, any keys in the current config file that are not in the
     *                                {@code configs} map will be left with their initial values; if false, they
     *                                will be removed from the file
     * @param configurationAdminMBean mbean used for updating configuration
     * @return instance of this class
     */
    public static AdminOperation instance(String pid, Map<String, Object> configs,
            boolean keepIgnored, ConfigurationAdminMBean configurationAdminMBean) {
        return new AdminOperation(pid, configs, keepIgnored, configurationAdminMBean);
    }

    @Override
    public Void commit() throws ConfiguratorException {
        Map<String, Object> properties;
        if (keepIgnored) {
            properties = new HashMap<>(currentProperties);
        } else {
            properties = new HashMap<>();
        }
        properties.putAll(configs);

        try {
            saveConfigs(properties);
        } catch (IOException | MalformedObjectNameException e) {
            throw new ConfiguratorException(String.format("Error writing configuration for %s",
                    pid));
        }

        return null;
    }

    @Override
    public Void rollback() throws ConfiguratorException {
        try {
            saveConfigs(currentProperties);
        } catch (IOException | MalformedObjectNameException e) {
            throw new ConfiguratorException(String.format("Error rolling back configuration for %s",
                    pid));
        }

        return null;
    }

    @Override
    public Map<String, Object> readState() throws ConfiguratorException {
        try {
            Map<String, Object> configResults = getConfigAdminMBean().getProperties(pid);
            if (configResults.isEmpty()) {
                Optional<Map<String, Object>> defaultMetatypeValues =
                        getConfigAdminMBean().listServices()
                                .stream()
                                .filter(service -> service.get("id") != null && service.get("id")
                                        .equals(pid))
                                .findFirst();

                List<Map<String, Object>> metatypes = new ArrayList<>();
                if (defaultMetatypeValues.isPresent()) {
                    metatypes = (List) defaultMetatypeValues.get()
                            .get("metatype");
                }

                if (metatypes.isEmpty()) {
                    return new HashMap<>();
                }

                return metatypes.stream()
                        .collect(Collectors.toMap(field -> (String) field.get("id"),
                                field -> field.get("defaultValue")));
            } else {
                return configResults;
            }
            // return getConfigAdminMBean().getProperties(pid);
        } catch (IOException | MalformedObjectNameException e) {
            throw new ConfiguratorException(String.format("Unable to find configuration for pid, %s",
                    pid), e);
        }
    }

    private void saveConfigs(Map<String, Object> properties)
            throws MalformedObjectNameException, IOException {
        cfgAdmMbean.update(pid, properties);
    }
}
