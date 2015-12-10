/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.ui.admin.api;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.ui.admin.api.module.AdminModule;
import org.codice.ddf.ui.admin.api.module.ValidationDecorator;
import org.codice.ddf.ui.admin.api.plugin.ConfigurationAdminPlugin;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.metatype.AttributeDefinition;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import com.github.drapostolos.typeparser.TypeParser;

/**
 * This class provides convenience methods for interacting with
 * OSGi/Felix ConfigurationAdmin services.
 */
public class ConfigurationAdmin implements ConfigurationAdminMBean {
    private static final String NEW_FACTORY_PID = "newFactoryPid";

    private static final String NEW_PID = "newPid";

    private static final String ORIGINAL_PID = "originalPid";

    private static final String ORIGINAL_FACTORY_PID = "originalFactoryPid";

    private static final String DISABLED = "_disabled";

    private static final String SERVICE_PID = "service.pid";

    private static final String SERVICE_FACTORYPID = "service.factoryPid";

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(ConfigurationAdmin.class));

    private final org.osgi.service.cm.ConfigurationAdmin configurationAdmin;

    private final ConfigurationAdminExt configurationAdminExt;

    private GuestClaimsHandlerExt guestClaimsHandlerExt;

    private ObjectName objectName;

    private MBeanServer mBeanServer;

    private List<String> filterList;

    private List<ConfigurationAdminPlugin> configurationAdminPluginList;

    private List<AdminModule> moduleList;

    /**
     * Constructor for use in unit tests. Needed for testing listServices() and getService().
     *
     * @param configurationAdmin    instance of org.osgi.service.cm.ConfigurationAdmin service
     * @param configurationAdminExt mocked instance of ConfigurationAdminExt
     */
    public ConfigurationAdmin(org.osgi.service.cm.ConfigurationAdmin configurationAdmin,
            ConfigurationAdminExt configurationAdminExt) {
        this.configurationAdmin = configurationAdmin;
        this.configurationAdminExt = configurationAdminExt;
    }

    /**
     * Constructs a ConfigurationAdmin implementation
     *
     * @param configurationAdmin instance of org.osgi.service.cm.ConfigurationAdmin service
     */
    public ConfigurationAdmin(org.osgi.service.cm.ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
        configurationAdminExt = new ConfigurationAdminExt(configurationAdmin);
    }

    /**
     * Initialize this MBean and register it with the MBean server
     */
    public void init() {
        try {
            if (objectName == null) {
                objectName = new ObjectName(ConfigurationAdminMBean.OBJECTNAME);
            }
            if (mBeanServer == null) {
                mBeanServer = ManagementFactory.getPlatformMBeanServer();
            }
            try {
                mBeanServer.registerMBean(this, objectName);
            } catch (InstanceAlreadyExistsException iaee) {
                // Try to remove and re-register
                LOGGER.info("Re-registering SchemaLookup MBean");
                mBeanServer.unregisterMBean(objectName);
                mBeanServer.registerMBean(this, objectName);
            }
        } catch (Exception e) {
            LOGGER.warn("Exception during initialization: ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Unregister this MBean with the MBean server
     */
    public void destroy() {
        try {
            if (objectName != null && mBeanServer != null) {
                mBeanServer.unregisterMBean(objectName);
            }
        } catch (Exception e) {
            LOGGER.warn("Exception unregistering mbean: ", e);
            throw new RuntimeException(e);
        }
    }

    public List<ConfigurationAdminPlugin> getConfigurationAdminPluginList() {
        return configurationAdminPluginList;
    }

    public void setConfigurationAdminPluginList(
            List<ConfigurationAdminPlugin> configurationAdminPluginList) {
        this.configurationAdminPluginList = configurationAdminPluginList;
        configurationAdminExt.setConfigurationAdminPluginList(configurationAdminPluginList);
    }

    public List<AdminModule> getModuleList() {
        return moduleList;
    }

    public void setModuleList(List<AdminModule> moduleList) {
        this.moduleList = moduleList;
    }

    public List<Map<String, Object>> listServices() {
        return configurationAdminExt
                .listServices(getDefaultFactoryLdapFilter(), getDefaultLdapFilter());
    }

    public Map<String, Object> getService(String filter) {
        List<Map<String, Object>> services = configurationAdminExt.listServices(filter, filter);

        Map<String, Object> service = null;

        if (services.size() > 0) {
            //just grab the first one, they should have specified a filter that returned just a single result
            //if not, that is not our problem
            service = services.get(0);
        }

        return service;
    }

    public List<Map<String, Object>> listModules() {
        List<ValidationDecorator> adminModules = ValidationDecorator.wrap(moduleList);
        Collections.sort(adminModules);
        List<Map<String, Object>> modules = new ArrayList<Map<String, Object>>();

        for (ValidationDecorator module : adminModules) {
            if (module.isValid()) {
                modules.add(module.toMap());
            } else {
                LOGGER.warn("Couldn't add invalid module, {}", module.getName());
            }
        }

        if (modules.size() > 0) {
            modules.get(0).put("active", true);
        }
        return modules;
    }

    private String getDefaultFactoryLdapFilter() {
        if (CollectionUtils.isNotEmpty(filterList)) {
            StringBuilder ldapFilter = new StringBuilder();
            ldapFilter.append("(");
            ldapFilter.append("|");

            for (String fpid : filterList) {
                ldapFilter.append("(");
                ldapFilter.append(SERVICE_FACTORYPID);
                ldapFilter.append("=");
                ldapFilter.append(fpid);
                ldapFilter.append(")");
            }

            ldapFilter.append(")");

            return ldapFilter.toString();
        }
        return "(" + SERVICE_FACTORYPID + "=" + "*)";
    }

    private String getDefaultLdapFilter() {
        if (CollectionUtils.isNotEmpty(filterList)) {
            StringBuilder ldapFilter = new StringBuilder();
            ldapFilter.append("(");
            ldapFilter.append("|");

            for (String fpid : filterList) {
                ldapFilter.append("(");
                ldapFilter.append(SERVICE_PID);
                ldapFilter.append("=");
                ldapFilter.append(fpid);
                ldapFilter.append("*");
                ldapFilter.append(")");
            }

            ldapFilter.append(")");

            return ldapFilter.toString();
        }
        return "(" + SERVICE_PID + "=" + "*)";
    }

    /**
     * @see ConfigurationAdminMBean#createFactoryConfiguration(java.lang.String)
     */
    public String createFactoryConfiguration(String factoryPid) throws IOException {
        return createFactoryConfigurationForLocation(factoryPid, null);
    }

    /**
     * @see ConfigurationAdminMBean#createFactoryConfigurationForLocation(java.lang.String,
     * java.lang.String)
     */
    public String createFactoryConfigurationForLocation(String factoryPid, String location)
            throws IOException {
        if (StringUtils.isBlank(factoryPid)) {
            throw new IOException("Argument factoryPid cannot be null or empty");
        }
        Configuration config = configurationAdmin.createFactoryConfiguration(factoryPid);
        config.setBundleLocation(location);
        return config.getPid();
    }

    /**
     * @see ConfigurationAdminMBean#delete(java.lang.String)
     */
    public void delete(String pid) throws IOException {
        deleteForLocation(pid, null);
    }

    /**
     * @see ConfigurationAdminMBean#deleteForLocation(java.lang.String, java.lang.String)
     */
    public void deleteForLocation(String pid, String location) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        config.delete();
    }

    /**
     * @see ConfigurationAdminMBean#deleteConfigurations(java.lang.String)
     */
    public void deleteConfigurations(String filter) throws IOException {
        if (filter == null || filter.length() < 1) {
            throw new IOException("Argument filter cannot be null or empty");
        }
        Configuration[] configuations;
        try {
            configuations = configurationAdmin.listConfigurations(filter);
        } catch (InvalidSyntaxException e) {
            throw new IOException("Invalid filter [" + filter + "] : " + e);
        }
        if (configuations != null) {
            for (Configuration config : configuations) {
                config.delete();
            }
        }
    }

    /**
     * @see ConfigurationAdminMBean#getBundleLocation(java.lang.String)
     */
    public String getBundleLocation(String pid) throws IOException {
        if (StringUtils.isBlank(pid)) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, null);
        String bundleLocation = (config.getBundleLocation() == null) ?
                "Configuration is not yet bound to a bundle location" :
                config.getBundleLocation();
        return bundleLocation;
    }

    /**
     * @see ConfigurationAdminMBean#getConfigurations(java.lang.String)
     */
    public String[][] getConfigurations(String filter) throws IOException {
        if (filter == null || filter.length() < 1) {
            throw new IOException("Argument filter cannot be null or empty");
        }
        List<String[]> result = new ArrayList<>();
        Configuration[] configurations;
        try {
            configurations = configurationAdmin.listConfigurations(filter);
        } catch (InvalidSyntaxException e) {
            throw new IOException("Invalid filter [" + filter + "] : " + e);
        }
        if (configurations != null) {
            for (Configuration config : configurations) {
                result.add(new String[] {config.getPid(), config.getBundleLocation()});
            }
        }
        return result.toArray(new String[result.size()][]);
    }

    /**
     * @see ConfigurationAdminMBean#getFactoryPid(java.lang.String)
     */
    public String getFactoryPid(String pid) throws IOException {
        return getFactoryPidForLocation(pid, null);
    }

    /**
     * @see ConfigurationAdminMBean#getFactoryPidForLocation(java.lang.String, java.lang.String)
     */
    public String getFactoryPidForLocation(String pid, String location) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        return config.getFactoryPid();
    }

    /**
     * @see ConfigurationAdminMBean#getProperties(java.lang.String)
     */
    public Map<String, Object> getProperties(String pid) throws IOException {
        return getPropertiesForLocation(pid, null);
    }

    /**
     * @see ConfigurationAdminMBean#getPropertiesForLocation(java.lang.String, java.lang.String)
     */
    public Map<String, Object> getPropertiesForLocation(String pid, String location)
            throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        Map<String, Object> propertiesTable = new HashMap<>();
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        Dictionary<String, Object> properties = config.getProperties();
        if (properties != null) {
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                propertiesTable.put(key, properties.get(key));
            }
        }
        return propertiesTable;
    }

    /**
     * @see ConfigurationAdminMBean#setBundleLocation(java.lang.String, java.lang.String)
     */
    public void setBundleLocation(String pid, String location) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument factoryPid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, null);
        config.setBundleLocation(location);
    }

    /**
     * @see ConfigurationAdminMBean#update(java.lang.String, java.util.Map)
     */
    public boolean update(String pid, Map<String, Object> configurationTable) throws IOException {
        updateForLocation(pid, null, configurationTable);
        return true;
    }

    /**
     * @see ConfigurationAdminMBean#updateForLocation(java.lang.String, java.lang.String,
     * java.util.Map)
     */
    public void updateForLocation(final String pid, String location,
            Map<String, Object> configurationTable) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw loggedException("Argument pid cannot be null or empty");
        }
        if (configurationTable == null) {
            throw loggedException("Argument configurationTable cannot be null");
        }

        Configuration config = configurationAdmin.getConfiguration(pid, location);

        final List<Map<String, Object>> metatype = findMetatypeForConfig(config);
        List<Map.Entry<String, Object>> configEntries = new ArrayList<>();

        CollectionUtils.addAll(configEntries, configurationTable.entrySet().iterator());

        if (metatype == null) {
            throw loggedException("Could not find metatype for " + pid);
        }
        // now we have to filter each property based on its cardinality
        CollectionUtils.transform(configEntries, new CardinalityTransformer(metatype, pid));

        Dictionary<String, Object> configurationProperties = new Hashtable<>();
        for (Map.Entry<String, Object> configEntry : configEntries) {
            configurationProperties.put(configEntry.getKey(), configEntry.getValue());
        }
        config.update(configurationProperties);
    }

    // unfortunately listServices returns a bunch of nested untyped objects
    private List<Map<String, Object>> findMetatypeForConfig(Configuration config) {
        List<Map<String, Object>> services = listServices();
        List<Map<String, Object>> tempMetatype = null;
        for (Map<String, Object> service : services) {
            String id = String.valueOf(service.get(ConfigurationAdminExt.MAP_ENTRY_ID));
            if (id.equals(config.getPid()) || ((id.equals(config.getFactoryPid()) || (id + "_disabled").equals(config.getFactoryPid())) && Boolean
                    .valueOf(String.valueOf(service.get(ConfigurationAdminExt.MAP_FACTORY))))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> mapList = (List<Map<String, Object>>) service
                        .get(ConfigurationAdminExt.MAP_ENTRY_METATYPE);
                tempMetatype = mapList;
                break;
            }
        }
        return tempMetatype;
    }

    public List<String> getFilterList() {
        return filterList;
    }

    public void setFilterList(List<String> filterList) {
        this.filterList = filterList;
    }

    public Map<String, Object> disableConfiguration(String servicePid) throws IOException {
        if (StringUtils.isEmpty(servicePid)) {
            throw new IOException(
                    "Service PID of Source to be disabled must be specified.  Service PID provided: "
                            + servicePid);
        }

        Configuration originalConfig = configurationAdminExt.getConfiguration(servicePid);

        if (originalConfig == null) {
            throw new IOException("No Source exists with the service PID: " + servicePid);
        }

        Dictionary<String, Object> properties = originalConfig.getProperties();
        String originalFactoryPid = (String) properties
                .get(org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID);
        if (StringUtils.endsWith(originalFactoryPid, DISABLED)) {
            throw new IOException("Source is already disabled.");
        }

        // Copy configuration from the original configuration and change its factory PID to end with
        // "disabled"
        String disabledServiceFactoryPid = originalFactoryPid + DISABLED;
        properties.put(org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID,
                disabledServiceFactoryPid);
        Configuration disabledConfig = configurationAdmin
                .createFactoryConfiguration(disabledServiceFactoryPid, null);
        disabledConfig.update(properties);

        // remove original configuration
        originalConfig.delete();

        Map<String, Object> rval = new HashMap<>();
        rval.put(ORIGINAL_PID, servicePid);
        rval.put(ORIGINAL_FACTORY_PID, originalFactoryPid);
        rval.put(NEW_PID, disabledConfig.getPid());
        rval.put(NEW_FACTORY_PID, disabledServiceFactoryPid);
        return rval;
    }

    @Override
    public Map<String, Object> getClaimsConfiguration(String filter) {
        Map<String, Object> config = this.getService(filter);
        if (config != null && guestClaimsHandlerExt != null) {
            config.put("claims", guestClaimsHandlerExt.getClaims());
            config.put("profiles", guestClaimsHandlerExt.getClaimsProfiles());
        }
        return config;
    }

    public Map<String, Object> enableConfiguration(String servicePid) throws IOException {
        if (StringUtils.isEmpty(servicePid)) {
            throw new IOException(
                    "Service PID of Source to be disabled must be specified.  Service PID provided: "
                            + servicePid);
        }

        Configuration disabledConfig = configurationAdminExt.getConfiguration(servicePid);

        if (disabledConfig == null) {
            throw new IOException("No Source exists with the service PID: " + servicePid);
        }

        Dictionary<String, Object> properties = disabledConfig.getProperties();
        String disabledFactoryPid = (String) properties
                .get(org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID);
        if (!StringUtils.endsWith(disabledFactoryPid, DISABLED)) {
            throw new IOException("Source is already enabled.");
        }

        String enabledFactoryPid = StringUtils.removeEnd(disabledFactoryPid, DISABLED);
        properties
                .put(org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID, enabledFactoryPid);
        Configuration enabledConfiguration = configurationAdmin
                .createFactoryConfiguration(enabledFactoryPid, null);
        enabledConfiguration.update(properties);

        disabledConfig.delete();

        Map<String, Object> rval = new HashMap<>();
        rval.put(ORIGINAL_PID, servicePid);
        rval.put(ORIGINAL_FACTORY_PID, disabledFactoryPid);
        rval.put(NEW_PID, enabledConfiguration.getPid());
        rval.put(NEW_FACTORY_PID, enabledFactoryPid);
        return rval;
    }

    /**
     * Setter method for mBeanServer. Needed for testing init() and destroy().
     */
    void setMBeanServer(MBeanServer server) {
        mBeanServer = server;
    }

    private static class CardinalityTransformer implements Transformer {
        private final List<Map<String, Object>> metatype;

        private final String pid;

        public CardinalityTransformer(List<Map<String, Object>> metatype, String pid) {
            this.metatype = metatype;
            this.pid = pid;
        }

        @Override
        // the method signature precludes a safer parameter type
        public Object transform(Object input) {
            if (!(input instanceof Map.Entry)) {
                throw loggedException("Cannot transform " + input);
            }
            @SuppressWarnings("unchecked")
            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) input;
            String attrId = entry.getKey();
            if (attrId == null) {
                throw loggedException("Found null key for " + pid);
            }
            Integer cardinality = null;
            Integer type = null;
            for (Map<String, Object> property : metatype) {
                if (attrId.equals(property.get(ConfigurationAdminExt.MAP_ENTRY_ID))) {
                    cardinality = Integer.valueOf(String.valueOf(
                            property.get(ConfigurationAdminExt.MAP_ENTRY_CARDINALITY)));
                    type = (Integer) property.get(ConfigurationAdminExt.MAP_ENTRY_TYPE);
                }
            }
            if (cardinality == null || type == null) {
                LOGGER.debug("Could not find property {} in metatype for config {}", attrId, pid);
                cardinality = 0;
                type = TYPE.STRING.getType();
            }
            Object value = entry.getValue();

            // ensure we don't allow any empty values
            if (value == null || StringUtils.isEmpty(String.valueOf(value))) {
                value = "";
            }
            // negative cardinality means a vector, 0 is a string, and positive is an array
            CardinalityEnforcer cardinalityEnforcer = TYPE.forType(type).getCardinalityEnforcer();
            if (value instanceof String && cardinality != 0) {
                try {
                    value = new JSONParser().parse(String.valueOf(value));
                } catch (ParseException e) {
                    LOGGER.debug("{} is not a JSON array.", value, e);
                }
            }
            if (cardinality < 0) {
                value = cardinalityEnforcer.negativeCardinality(value);
            } else if (cardinality == 0) {
                value = cardinalityEnforcer.zerothCardinality(value);
            } else if (cardinality > 0) {
                value = cardinalityEnforcer.positiveCardinality(value);
            }

            entry.setValue(value);

            return entry;
        }
    }

    private static class CardinalityEnforcer<T> {
        private final Class<T> clazz;

        public CardinalityEnforcer(Class<T> clazz) {
            this.clazz = clazz;
        }

        @SuppressWarnings("unchecked")
        public T[] positiveCardinality(Object value) {
            Vector<T> vector = negativeCardinality(value);
            return vector.toArray((T[]) Array.newInstance(clazz, vector.size()));
        }

        public Vector<T> negativeCardinality(Object value) {
            if (!(value.getClass().isArray() || value instanceof Collection)) {
                if (String.valueOf(value).isEmpty()) {
                    value = new Object[] {};
                } else {
                    value = new Object[] {value};
                }
            }
            Vector<T> ret = new Vector<>();
            for (int i = 0; i < CollectionUtils.size(value); i++) {
                Object currentValue = CollectionUtils.get(value, i);
                ret.add(zerothCardinality(currentValue));
            }
            return ret;
        }

        public T zerothCardinality(Object value) {
            if (value.getClass().isArray() || value instanceof Collection) {
                if (CollectionUtils.size(value) != 1) {
                    throw loggedException(
                            "Attempt on 0-cardinality property to set multiple values:" + value);
                }
                value = CollectionUtils.get(value, 0);
            }
            if (!clazz.isInstance(value)) {
                value = TypeParser.newBuilder().build().parse(String.valueOf(value), clazz);
            }
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }
            throw loggedException("Failed to parse " + value + " as " + clazz);
        }
    }

    private static IllegalArgumentException loggedException(String message) {
        IllegalArgumentException exception = new IllegalArgumentException(message);
        LOGGER.error(message, exception);
        return exception;
    }

    // felix won't take Object[] or Vector<Object>, so here we
    // map all the osgi constants to strongly typed arrays/vectors
    enum TYPE {
        STRING(AttributeDefinition.STRING, String.class) {
        }, LONG(AttributeDefinition.LONG, Long.class) {
        }, INTEGER(AttributeDefinition.INTEGER, Integer.class) {
        }, SHORT(AttributeDefinition.SHORT, Short.class) {
        }, CHARACTER(AttributeDefinition.CHARACTER, Character.class) {
        }, BYTE(AttributeDefinition.BYTE, Byte.class) {
        }, DOUBLE(AttributeDefinition.DOUBLE, Double.class) {
        }, FLOAT(AttributeDefinition.FLOAT, Float.class) {
        }, BIGINTEGER(AttributeDefinition.BIGINTEGER, BigInteger.class) {
        }, BIGDECIMAL(AttributeDefinition.BIGDECIMAL, BigDecimal.class) {
        }, BOOLEAN(AttributeDefinition.BOOLEAN, Boolean.class) {
        }, PASSWORD(AttributeDefinition.PASSWORD, String.class) {
        };

        private final int type;

        private final Class clazz;

        TYPE(int type, Class clazz) {
            this.type = type;
            this.clazz = clazz;
        }

        @SuppressWarnings("unchecked")
        public CardinalityEnforcer getCardinalityEnforcer() {
            return new CardinalityEnforcer(clazz);
        }

        public int getType() {
            return type;
        }

        public static TYPE forType(int type) {
            for (TYPE theType : TYPE.values()) {
                if (theType.getType() == type) {
                    return theType;
                }
            }
            return STRING;
        }
    }

    public void setGuestClaimsHandlerExt(GuestClaimsHandlerExt guestClaimsHandlerExt) {
        this.guestClaimsHandlerExt = guestClaimsHandlerExt;
    }

}
