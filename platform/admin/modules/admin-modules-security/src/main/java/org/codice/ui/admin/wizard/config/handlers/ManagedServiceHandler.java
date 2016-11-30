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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.validation.constraints.NotNull;

import org.codice.ddf.ui.admin.api.ConfigurationAdmin;
import org.codice.ddf.ui.admin.api.ConfigurationAdminMBean;
import org.codice.ui.admin.wizard.config.ConfigHandler;
import org.codice.ui.admin.wizard.config.ConfiguratorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ManagedServiceHandler implements ConfigHandler<String, Void> {
    private static class DeleteHandler extends ManagedServiceHandler {
        private final String configPid;

        private Map<String, Object> currentProperties;

        private DeleteHandler(String configPid) {
            super();

            this.configPid = configPid;

            try {
                ConfigurationAdminMBean configAdmin = getConfigAdminMBean();
                factoryPid = configAdmin.getFactoryPid(configPid);
                currentProperties = configAdmin.getProperties(configPid);
            } catch (MalformedObjectNameException | IOException e) {
                ManagedServiceHandler.LOGGER.debug("Error getting current configuration for pid {}",
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

    private static class CreateHandler extends ManagedServiceHandler {
        private final Map<String, Object> configs;

        private String newConfigPid;

        private CreateHandler(String factoryPid, Map<String, Object> configs) {
            super();

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

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedServiceHandler.class);

    protected String factoryPid;

    public static ManagedServiceHandler forCreate(String factoryPid,
            @NotNull Map<String, Object> configs) {
        return new CreateHandler(factoryPid, configs);
    }

    public static ManagedServiceHandler forDelete(String pid) {
        return new DeleteHandler(pid);
    }

    @Override
    public Void readState() throws ConfiguratorException {
        return null;
    }

    protected void deleteByPid(String configPid) {
        try {
            ConfigurationAdmin configAdmin = getConfigAdmin();
            configAdmin.delete(configPid);
        } catch (IOException e) {
            LOGGER.debug("Error deleting managed service with pid {}", configPid, e);
            throw new ConfiguratorException("Internal error");
        }
    }

    protected String createManagedService(Map<String, Object> properties) {
        ConfigurationAdmin configAdmin = getConfigAdmin();
        try {
            String configPid = configAdmin.createFactoryConfiguration(factoryPid);
            getConfigAdminMBean().update(configPid, properties);
            return configPid;
        } catch (IOException | MalformedObjectNameException e) {
            LOGGER.debug("Error creating managed service for factoryPid {}", factoryPid, e);
            throw new ConfiguratorException("Internal error");
        }
    }
}
