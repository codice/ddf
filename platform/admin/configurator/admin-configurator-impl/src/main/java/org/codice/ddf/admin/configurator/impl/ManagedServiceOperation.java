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
import static org.codice.ddf.admin.configurator.impl.OsgiUtils.getConfigAdmin;
import static org.codice.ddf.admin.configurator.impl.OsgiUtils.getConfigAdminMBean;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.configurator.Operation;
import org.codice.ddf.admin.configurator.Result;
import org.codice.ddf.internal.admin.configurator.actions.ManagedServiceActions;
import org.codice.ddf.ui.admin.api.ConfigurationAdmin;
import org.codice.ddf.ui.admin.api.ConfigurationAdminMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transactional handler factory for creating and deleting managed services.
 * <p>
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public abstract class ManagedServiceOperation implements Operation<String> {
    public static class Actions implements ManagedServiceActions {
        @Override
        public ManagedServiceOperation create(String factoryPid, Map<String, Object> configs)
                throws ConfiguratorException {
            validateString(factoryPid, "Missing factory id");
            validateMap(configs, "Missing configuration properties");
            return new CreateHandler(factoryPid, configs, getConfigAdmin(), getConfigAdminMBean());
        }

        @Override
        public ManagedServiceOperation delete(String pid) throws ConfiguratorException {
            validateString(pid, "Missing config id");
            return new DeleteHandler(pid, getConfigAdmin(), getConfigAdminMBean());
        }

        @Override
        public Map<String, Map<String, Object>> read(String factoryPid)
                throws ConfiguratorException {
            return new CreateHandler(factoryPid,
                    Collections.emptyMap(),
                    getConfigAdmin(),
                    getConfigAdminMBean()).readState();
        }
    }

    /**
     * Transactional handler for deleting managed services.
     */
    private static class DeleteHandler extends ManagedServiceOperation {
        private final String configPid;

        private final Map<String, Object> currentProperties;

        private static String getFactoryPid(String configPid, ConfigurationAdminMBean cfgAdmMbean) {
            try {
                return cfgAdmMbean.getFactoryPid(configPid);
            } catch (IOException e) {
                ManagedServiceOperation.LOGGER.debug(
                        "Error getting current configuration for pid {}",
                        configPid,
                        e);
                throw new ConfiguratorException("Internal error");
            }
        }

        private DeleteHandler(String configPid, ConfigurationAdmin configAdmin,
                ConfigurationAdminMBean cfgAdmMbean) {
            super(getFactoryPid(configPid, cfgAdmMbean), configAdmin, cfgAdmMbean);

            this.configPid = configPid;

            try {
                currentProperties = cfgAdmMbean.getProperties(configPid);
            } catch (IOException e) {
                ManagedServiceOperation.LOGGER.debug(
                        "Error getting current configuration for pid {}",
                        configPid,
                        e);
                throw new ConfiguratorException("Internal error");
            }
        }

        @Override
        public Result<String> commit() throws ConfiguratorException {
            deleteByPid(configPid);

            return ResultImpl.passWithData(configPid);
        }

        @Override
        public Result<String> rollback() throws ConfiguratorException {
            String configPid = createManagedService(currentProperties);

            return ResultImpl.rollbackWithData(configPid);
        }
    }

    /**
     * Transactional handler for creating managed services.
     */
    private static class CreateHandler extends ManagedServiceOperation {
        private final Map<String, Object> configs;

        private String newConfigPid;

        private CreateHandler(String factoryPid, Map<String, Object> configs,
                ConfigurationAdmin configAdmin, ConfigurationAdminMBean cfgAdmMbean) {
            super(factoryPid, configAdmin, cfgAdmMbean);

            this.configs = new HashMap<>(configs);
        }

        @Override
        public Result<String> commit() throws ConfiguratorException {
            newConfigPid = createManagedService(configs);

            return ResultImpl.passWithData(newConfigPid);
        }

        @Override
        public Result<String> rollback() throws ConfiguratorException {
            deleteByPid(newConfigPid);

            return ResultImpl.rollbackWithData(null);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedServiceOperation.class);

    private final String factoryPid;

    private final ConfigurationAdmin configAdmin;

    private final ConfigurationAdminMBean cfgAdmMbean;

    private ManagedServiceOperation(String factoryPid, ConfigurationAdmin configAdmin,
            ConfigurationAdminMBean cfgAdmMbean) {
        this.configAdmin = configAdmin;
        this.cfgAdmMbean = cfgAdmMbean;
        this.factoryPid = factoryPid;
    }

    Map<String, Map<String, Object>> readState() throws ConfiguratorException {
        try {
            String[][] configurations = getConfigAdmin().getConfigurations(String.format(
                    "(service.factoryPid=%s)",
                    factoryPid));
            if (configurations == null || configurations.length == 0) {
                return Collections.emptyMap();
            }

            HashMap<String, Map<String, Object>> retVal = new HashMap<>();
            ConfigurationAdminMBean configAdminMBean = getConfigAdminMBean();

            for (String[] configuration : configurations) {
                String configPid = configuration[0];
                retVal.put(configPid, configAdminMBean.getProperties(configPid));
            }

            return retVal;
        } catch (IOException e) {
            LOGGER.debug("Error retrieving configurations for factoryPid, {}", factoryPid, e);
            throw new ConfiguratorException("Error retrieving configurations");
        }
    }

    void deleteByPid(String configPid) {
        try {
            if (factoryPid == null) {
                ManagedServiceOperation.LOGGER.debug("Error getting factory for pid {}", configPid);
                throw new ConfiguratorException("Internal error");
            }
            cfgAdmMbean.delete(configPid);
        } catch (IOException e) {
            LOGGER.debug("Error deleting managed service with pid {}", configPid, e);
            throw new ConfiguratorException("Internal error");
        }
    }

    String createManagedService(Map<String, Object> properties) {
        try {
            String configPid = configAdmin.createFactoryConfiguration(factoryPid);
            cfgAdmMbean.update(configPid, properties);
            return configPid;
        } catch (IOException e) {
            LOGGER.debug("Error creating managed service for factoryPid {}", factoryPid, e);
            throw new ConfiguratorException("Internal error");
        }
    }
}
