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
 */

package org.codice.ddf.catalog.admin.poller;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.shiro.util.CollectionUtils;
import org.codice.ddf.ui.admin.api.ConfigurationAdminExt;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;

public class AdminPollerServiceBean implements AdminPollerServiceBeanMBean {
    static final String META_TYPE_NAME = "org.osgi.service.metatype.MetaTypeService";

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminPollerServiceBean.class);

    private static final String MAP_ENTRY_ID = "id";

    private static final String MAP_ENTRY_ENABLED = "enabled";

    private static final String MAP_ENTRY_FPID = "fpid";

    private static final String MAP_ENTRY_NAME = "name";

    private static final String MAP_ENTRY_BUNDLE_NAME = "bundle_name";

    private static final String MAP_ENTRY_BUNDLE_LOCATION = "bundle_location";

    private static final String MAP_ENTRY_BUNDLE = "bundle";

    private static final String MAP_ENTRY_PROPERTIES = "properties";

    private static final String MAP_ENTRY_CONFIGURATIONS = "configurations";

    private static final String DISABLED = "_disabled";

    private static final String SERVICE_NAME = ":service=admin-source-poller-service";

    private static final String MAP_ENTRY_REPORT_ACTIONS = "report_actions";

    private static final String MAP_ENTRY_OPERATION_ACTIONS = "operation_actions";

    private static final String MAP_ENTRY_ACTION_ID = "id";

    private static final String MAP_ENTRY_ACTION_TITLE = "title";

    private static final String MAP_ENTRY_ACTION_DESCRIPTION = "description";

    private static final String MAP_ENTRY_ACTION_URL = "url";

    private final ObjectName objectName;

    private final MBeanServer mBeanServer;

    private final AdminSourceHelper helper;

    private List<ActionProvider> reportActionProviders;

    private List<ActionProvider> operationActionProviders;

    public AdminPollerServiceBean(ConfigurationAdmin configurationAdmin) {
        helper = getHelper();
        helper.configurationAdmin = configurationAdmin;

        mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName objName = null;
        try {
            objName = new ObjectName(AdminPollerServiceBean.class.getName() + SERVICE_NAME);
        } catch (MalformedObjectNameException e) {
            LOGGER.error("Unable to create Admin Source Poller Service MBean with name [{}].",
                    AdminPollerServiceBean.class.getName() + SERVICE_NAME,
                    e);
        }
        objectName = objName;
    }

    public void init() {
        try {
            try {
                mBeanServer.registerMBean(this, objectName);
                LOGGER.info(
                        "Registered Admin Source Poller Service Service MBean under object name: {}",
                        objectName.toString());
            } catch (InstanceAlreadyExistsException e) {
                // Try to remove and re-register
                mBeanServer.unregisterMBean(objectName);
                mBeanServer.registerMBean(this, objectName);
                LOGGER.info("Re-registered Admin Source Poller Service Service MBean");
            }
        } catch (Exception e) {
            LOGGER.error("Could not register MBean [{}].", objectName.toString(), e);
        }
    }

    public void destroy() {
        try {
            if (objectName != null && mBeanServer != null) {
                mBeanServer.unregisterMBean(objectName);
                LOGGER.info("Unregistered Admin Source Poller Service Service MBean");
            }
        } catch (Exception e) {
            LOGGER.error("Exception unregistering MBean [{}].", objectName.toString(), e);
        }
    }

    @Override
    public boolean sourceStatus(String servicePID) {
        try {
            List<Source> sources = helper.getSources();
            for (Source source : sources) {
                if (source instanceof ConfiguredService) {
                    ConfiguredService cs = (ConfiguredService) source;
                    try {
                        Configuration config = helper.getConfiguration(cs);
                        if (config != null && config.getProperties()
                                .get("service.pid")
                                .equals(servicePID)) {
                            try {
                                return source.isAvailable();
                            } catch (Exception e) {
                                LOGGER.warn("Couldn't get availability on source {}: {}",
                                        servicePID,
                                        e);
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Couldn't find configuration for source '{}'", source.getId());
                    }
                } else {
                    LOGGER.warn("Source '{}' not a configured service", source.getId());
                }
            }
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Could not get service reference list");
        }

        return false;
    }

    @Override
    public List<Map<String, Object>> allSourceInfo() {
        // Get list of metatypes
        List<Map<String, Object>> metatypes = helper.getMetatypes();

        // Loop through each metatype and find its configurations
        for (Map metatype : metatypes) {
            try {
                List<Configuration> configs = helper.getConfigurations(metatype);

                ArrayList<Map<String, Object>> configurations = new ArrayList<>();
                if (configs != null) {
                    for (Configuration config : configs) {
                        Map<String, Object> source = new HashMap<>();

                        boolean disabled = config.getPid()
                                .contains(DISABLED);
                        source.put(MAP_ENTRY_ID, config.getPid());
                        source.put(MAP_ENTRY_ENABLED, !disabled);
                        source.put(MAP_ENTRY_FPID, config.getFactoryPid());

                        if (!disabled) {
                            source.put(MAP_ENTRY_NAME, helper.getName(config));
                            source.put(MAP_ENTRY_BUNDLE_NAME, helper.getBundleName(config));
                            source.put(MAP_ENTRY_BUNDLE_LOCATION, config.getBundleLocation());
                            source.put(MAP_ENTRY_BUNDLE, helper.getBundleId(config));
                        } else {
                            source.put(MAP_ENTRY_NAME, config.getPid());
                        }

                        Dictionary<String, Object> properties = config.getProperties();
                        Map<String, Object> plist = new HashMap<>();
                        for (String key : Collections.list(properties.keys())) {
                            plist.put(key, properties.get(key));
                        }
                        source.put(MAP_ENTRY_PROPERTIES, plist);
                        source.put(MAP_ENTRY_REPORT_ACTIONS,
                                getActions(config, reportActionProviders));
                        source.put(MAP_ENTRY_OPERATION_ACTIONS,
                                getActions(config, operationActionProviders));
                        configurations.add(source);
                    }
                    metatype.put(MAP_ENTRY_CONFIGURATIONS, configurations);
                }
            } catch (Exception e) {
                LOGGER.warn("Error getting source info: {}", e.getMessage());
            }
        }

        Collections.sort(metatypes, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                return ((String) o1.get("id")).compareToIgnoreCase((String) o2.get("id"));
            }
        });
        return metatypes;
    }

    private List<Map<String, String>> getActions(Configuration config,
            List<ActionProvider> providers) {
        List<Map<String, String>> actions = new ArrayList<>();
        for (ActionProvider provider : providers) {
            if (!provider.canHandle(config)) {
                continue;
            }

            List<Action> curActionList = provider.getActions(config);
            for (Action action : curActionList) {
                Map<String, String> actionProperties = new HashMap<>();
                actionProperties.put(MAP_ENTRY_ACTION_ID, action.getId());
                actionProperties.put(MAP_ENTRY_ACTION_TITLE, action.getTitle());
                actionProperties.put(MAP_ENTRY_ACTION_DESCRIPTION, action.getDescription());
                actionProperties.put(MAP_ENTRY_ACTION_URL,
                        action.getUrl()
                                .toString());
                actions.add(actionProperties);
            }

        }
        return actions;
    }

    protected AdminSourceHelper getHelper() {
        return new AdminSourceHelper();
    }

    protected class AdminSourceHelper {
        protected ConfigurationAdmin configurationAdmin;

        private BundleContext getBundleContext() {
            Bundle bundle = FrameworkUtil.getBundle(AdminPollerServiceBean.class);
            if (bundle != null) {
                return bundle.getBundleContext();
            }
            return null;
        }

        protected List<Source> getSources() throws org.osgi.framework.InvalidSyntaxException {
            List<Source> sources = new ArrayList<>();
            List<ServiceReference<? extends Source>> refs = new ArrayList<>();
            refs.addAll(helper.getBundleContext()
                    .getServiceReferences(FederatedSource.class, null));
            refs.addAll(helper.getBundleContext()
                    .getServiceReferences(ConnectedSource.class, null));

            for (ServiceReference<? extends Source> ref : refs) {
                sources.add(getBundleContext().getService(ref));
            }

            return sources;
        }

        protected List<Map<String, Object>> getMetatypes() {
            ConfigurationAdminExt configAdminExt = new ConfigurationAdminExt(configurationAdmin);
            return configAdminExt.addMetaTypeNamesToMap(configAdminExt.getFactoryPidObjectClasses(),
                    "(|(service.factoryPid=*source*)(service.factoryPid=*Source*)(service.factoryPid=*service*)(service.factoryPid=*Service*))",
                    "service.factoryPid");
        }

        protected List getConfigurations(Map metatype) throws InvalidSyntaxException, IOException {
            return CollectionUtils.asList(configurationAdmin.listConfigurations(
                    "(|(service.factoryPid=" + metatype.get(MAP_ENTRY_ID) + ")(service.factoryPid="
                            + metatype.get(MAP_ENTRY_ID) + DISABLED + "))"));
        }

        protected Configuration getConfiguration(ConfiguredService cs) throws IOException {
            return configurationAdmin.getConfiguration(cs.getConfigurationPid());
        }

        protected String getBundleName(Configuration config) {
            ConfigurationAdminExt configAdminExt = new ConfigurationAdminExt(configurationAdmin);
            return configAdminExt.getName(helper.getBundleContext()
                    .getBundle(config.getBundleLocation()));
        }

        protected long getBundleId(Configuration config) {
            return getBundleContext().getBundle(config.getBundleLocation())
                    .getBundleId();
        }

        protected String getName(Configuration config) {
            ConfigurationAdminExt configAdminExt = new ConfigurationAdminExt(configurationAdmin);
            return ((ObjectClassDefinition) configAdminExt.getFactoryPidObjectClasses()
                    .get(config.getFactoryPid())).getName();
        }
    }

    public void setReportActionProviders(List<ActionProvider> reportActionProviders) {
        this.reportActionProviders = reportActionProviders;
    }

    public void setOperationActionProviders(List<ActionProvider> operationActionProviders) {
        this.operationActionProviders = operationActionProviders;
    }
}
