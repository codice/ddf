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
import static org.codice.ddf.admin.configurator.impl.ConfigValidator.validateString;
import static org.codice.ddf.admin.configurator.impl.OsgiUtils.getConfigAdminMBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.management.MalformedObjectNameException;

import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.configurator.Operation;
import org.codice.ddf.admin.configurator.Result;
import org.codice.ddf.internal.admin.configurator.actions.ServiceActions;
import org.codice.ddf.ui.admin.api.ConfigurationAdminMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transactional handler for persisting bundle configuration file changes.
 */
public class ServiceOperation implements Operation<Void> {
    public static class Actions implements ServiceActions {
        @Override
        public ServiceOperation build(String serviceId, Map<String, Object> configs,
                boolean keepIfNotPresent) throws ConfiguratorException {
            validateString(serviceId, "Missing config id");
            validateMap(configs, "Missing configuration properties");
            return new ServiceOperation(serviceId,
                    configs,
                    keepIfNotPresent,
                    getConfigAdminMBean());
        }

        @Override
        public Map<String, Object> read(String serviceId) throws ConfiguratorException {
            validateString(serviceId, "Missing config id");
            return new ServiceOperation(serviceId,
                    Collections.emptyMap(),
                    true,
                    getConfigAdminMBean()).readState();
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceOperation.class);

    private final String pid;

    private final Map<String, Object> configs;

    private final boolean keepIgnored;

    private final ConfigurationAdminMBean cfgAdmMbean;

    private final Map<String, Object> currentProperties;

    private ServiceOperation(String pid, Map<String, Object> configs, boolean keepIgnored,
            ConfigurationAdminMBean cfgAdmMbean) {
        this.pid = pid;
        this.configs = new HashMap<>(configs);
        this.keepIgnored = keepIgnored;
        this.cfgAdmMbean = cfgAdmMbean;

        try {
            currentProperties = readState(this.pid, this.cfgAdmMbean);
        } catch (ConfiguratorException e) {
            LOGGER.debug("Error getting current configuration for pid {}", pid, e);
            throw new ConfiguratorException("Internal error");
        }
    }

    @Override
    public Result<Void> commit() throws ConfiguratorException {
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

        return ResultImpl.pass();
    }

    @Override
    public Result<Void> rollback() throws ConfiguratorException {
        try {
            saveConfigs(currentProperties);
        } catch (IOException | MalformedObjectNameException e) {
            throw new ConfiguratorException(String.format("Error rolling back configuration for %s",
                    pid));
        }

        return ResultImpl.rollback();
    }

    Map<String, Object> readState() throws ConfiguratorException {
        return readState(pid, cfgAdmMbean);
    }

    private void saveConfigs(Map<String, Object> properties)
            throws MalformedObjectNameException, IOException {
        cfgAdmMbean.update(pid, properties);
    }

    private static Map<String, Object> readState(String pid, ConfigurationAdminMBean cfgAdmMbean)
            throws ConfiguratorException {
        try {
            Map<String, Object> configResults = cfgAdmMbean.getProperties(pid);
            if (configResults.isEmpty()) {
                Optional<Map<String, Object>> defaultMetatypeValues = cfgAdmMbean.listServices()
                        .stream()
                        .filter(service -> service.get("id") != null && service.get("id")
                                .equals(pid))
                        .findFirst();

                List<Map<String, Object>> metatypes = new ArrayList<>();
                if (defaultMetatypeValues.isPresent()) {
                    metatypes = (List) defaultMetatypeValues.get()
                            .get("metatype");
                }

                return metatypes.stream()
                        .collect(Collectors.toMap(field -> (String) field.get("id"),
                                field -> field.get("defaultValue")));
            } else {
                return configResults;
            }
            // return getConfigAdminMBean().getProperties(pid);
        } catch (IOException e) {
            throw new ConfiguratorException(String.format("Unable to find configuration for pid, %s",
                    pid), e);
        }
    }
}
