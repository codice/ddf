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

package org.codice.ddf.admin.api.configurator.operations;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.validation.constraints.NotNull;

import org.codice.ddf.admin.api.configurator.Operation;
import org.codice.ddf.admin.api.configurator.ConfiguratorException;
import org.codice.ddf.ui.admin.api.ConfigurationAdmin;
import org.codice.ddf.ui.admin.api.ConfigurationAdminMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transactional handler factory for creating and deleting managed services.
 */
public abstract class ManagedServiceOperation
        implements Operation<String, Map<String, Map<String, Object>>> {
    /**
     * Transactional handler for deleting managed services.
     */
    private static class DeleteHandler extends ManagedServiceOperation {
        private final String configPid;

        private Map<String, Object> currentProperties;

        private DeleteHandler(String configPid, ConfigurationAdmin configAdmin,
                ConfigurationAdminMBean cfgAdmMbean) {
            super(configAdmin, cfgAdmMbean);

            this.configPid = configPid;

            try {
                factoryPid = cfgAdmMbean.getFactoryPid(configPid);
                currentProperties = cfgAdmMbean.getProperties(configPid);
            } catch (IOException e) {
                ManagedServiceOperation.LOGGER.debug("Error getting current configuration for pid {}",
                        configPid,
                        e);
                throw new ConfiguratorException("Internal error");
            }
        }

        @Override
        public String commit() throws ConfiguratorException {
            deleteByPid(configPid);
            return null;
        }

        @Override
        public String rollback() throws ConfiguratorException {
            return createManagedService(currentProperties);
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
            super(configAdmin, cfgAdmMbean);

            this.factoryPid = factoryPid;
            this.configs = new HashMap<>(configs);
        }

        @Override
        public String commit() throws ConfiguratorException {
            newConfigPid = createManagedService(configs);
            return newConfigPid;
        }

        @Override
        public String rollback() throws ConfiguratorException {
            deleteByPid(newConfigPid);
            return null;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedServiceOperation.class);

    protected String factoryPid;

    protected final ConfigurationAdmin configAdmin;

    protected final ConfigurationAdminMBean cfgAdmMbean;

    private ManagedServiceOperation(ConfigurationAdmin configAdmin,
            ConfigurationAdminMBean cfgAdmMbean) {
        this.configAdmin = configAdmin;
        this.cfgAdmMbean = cfgAdmMbean;
    }

    /**
     * Creates a handler that will create a managed service as part of a transaction.
     *
     * @param factoryPid  the PID of the service factory
     * @param configs     the configuration properties to apply to the service
     * @param configAdmin service wrapper needed for OSGi interaction
     * @param cfgAdmMbean mbean needed for saving configuration data
     * @return
     */
    public static ManagedServiceOperation forCreate(String factoryPid,
            @NotNull Map<String, Object> configs, ConfigurationAdmin configAdmin,
            ConfigurationAdminMBean cfgAdmMbean) {
        return new CreateHandler(factoryPid, configs, configAdmin, cfgAdmMbean);
    }

    /**
     * Creates a handler that will delete a managed service as part of a transaction.
     *
     * @param pid         the PID of the instance to be deleted
     * @param configAdmin service wrapper needed for OSGi interaction
     * @param cfgAdmMbean mbean needed for saving configuration data
     * @return
     */
    public static ManagedServiceOperation forDelete(String pid, ConfigurationAdmin configAdmin,
            ConfigurationAdminMBean cfgAdmMbean) {
        return new DeleteHandler(pid, configAdmin, cfgAdmMbean);
    }

    @Override
    public Map<String, Map<String, Object>> readState() throws ConfiguratorException {
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
        } catch (IOException | MalformedObjectNameException e) {
            LOGGER.debug("Error retrieving configurations for factoryPid, {}", factoryPid, e);
            throw new ConfiguratorException("Error retrieving configurations");
        }
    }

    protected void deleteByPid(String configPid) {
        try {
            cfgAdmMbean.delete(configPid);
        } catch (IOException e) {
            LOGGER.debug("Error deleting managed service with pid {}", configPid, e);
            throw new ConfiguratorException("Internal error");
        }
    }

    protected String createManagedService(Map<String, Object> properties) {
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
