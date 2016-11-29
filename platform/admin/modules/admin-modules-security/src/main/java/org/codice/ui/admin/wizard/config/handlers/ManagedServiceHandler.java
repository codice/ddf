package org.codice.ui.admin.wizard.config.handlers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.codice.ddf.ui.admin.api.ConfigurationAdmin;
import org.codice.ddf.ui.admin.api.ConfigurationAdminMBean;
import org.codice.ui.admin.wizard.config.ConfigHandler;
import org.codice.ui.admin.wizard.config.ConfiguratorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transactional handler factory for creating and deleting managed services.
 */
public abstract class ManagedServiceHandler implements ConfigHandler<String> {
    /**
     * Transactional handler for deleting managed services.
     */
    private static class DeleteHandler extends ManagedServiceHandler {
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

    /**
     * Transactional handler for creating managed services.
     */
    private static class CreateHandler extends ManagedServiceHandler {
        private final Map<String, Object> configs;

        private String newConfigPid;

        private CreateHandler(String factoryPid, Map<String, String> configs,
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedServiceHandler.class);

    protected String factoryPid;

    protected final ConfigurationAdmin configAdmin;

    protected final ConfigurationAdminMBean cfgAdmMbean;

    private ManagedServiceHandler(ConfigurationAdmin configAdmin,
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
    public static ManagedServiceHandler forCreate(String factoryPid,
            @NotNull Map<String, String> configs, ConfigurationAdmin configAdmin,
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
    public static ManagedServiceHandler forDelete(String pid, ConfigurationAdmin configAdmin,
            ConfigurationAdminMBean cfgAdmMbean) {
        return new DeleteHandler(pid, configAdmin, cfgAdmMbean);
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
