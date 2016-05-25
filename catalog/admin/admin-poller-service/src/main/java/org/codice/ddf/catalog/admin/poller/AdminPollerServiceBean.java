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
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.shiro.util.CollectionUtils;
import org.codice.ddf.ui.admin.api.ConfigurationAdminExt;
import org.opengis.filter.Filter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

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

    private static final String REGISTRY_ID = "registry-id";

    private static final String PUBLISHED_LOCATIONS = "published-locations";

    private final ObjectName objectName;

    private final MBeanServer mBeanServer;

    private final AdminSourceHelper helper;

    private CatalogFramework catalogFramework;

    private FilterBuilder filterBuilder;

    private Map<String, CatalogStore> catalogStoreMap;

    public AdminPollerServiceBean(ConfigurationAdmin configurationAdmin,
            CatalogFramework catalogFramework, FilterBuilder filterBuilder,
            Map<String, CatalogStore> catalogStoreMap) {
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
        this.catalogFramework = catalogFramework;
        this.filterBuilder = filterBuilder;
        this.catalogStoreMap = catalogStoreMap;
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

                        Dictionary<String, Object> configProperties = config.getProperties();
                        List<Map<String, Object>> metatypeProperties =
                                (List<Map<String, Object>>) metatype.get("metatype");
                        Map<String, Object> plist = new HashMap<>();
                        Object propertyValue;

                        for (String key : Collections.list(configProperties.keys())) {
                            propertyValue = configProperties.get(key);
                            // If the configuration property is a password, set its value to
                            // "password" if it's not empty, so that the real password value will
                            // be hidden. (Passwords within source configs are usually optional,
                            // thus it should have the ability to update to an empty string).
                            if (!"".equals(propertyValue)) {
                                for (Map<String, Object> metatypeProperty : metatypeProperties) {
                                    if (metatypeProperty.get("id")
                                            .equals(key) && AttributeDefinition.PASSWORD
                                            == (Integer) metatypeProperty.get("type")) {
                                        propertyValue = "password";
                                        break;
                                    }
                                }
                            }
                            plist.put(key, propertyValue);
                        }
                        source.put(MAP_ENTRY_PROPERTIES, plist);

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

    @Override
    public List<Serializable> updatePublications(String source, List<String> destinations)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        List<Serializable> updatedPublishedLocations = new ArrayList<>();

        if (CollectionUtils.isEmpty(destinations)) {
            return updatedPublishedLocations;
        }

        Filter filter = filterBuilder.attribute(REGISTRY_ID)
                .is()
                .equalTo()
                .text(source);
        Query query = new QueryImpl(filter);

        QueryResponse queryResponse = catalogFramework.query(new QueryRequestImpl(query));

        List<Result> metacards = queryResponse.getResults();
        if (!CollectionUtils.isEmpty(metacards)) {
            Metacard metacard = metacards.get(0)
                    .getMetacard();
            if (metacard == null) {
                return updatedPublishedLocations;
            }
            List<Serializable> currentlyPublishedLocations = metacard.getAttribute(
                    PUBLISHED_LOCATIONS)
                    .getValues();

            List<String> publishLocations = destinations.stream()
                    .filter(destination -> !currentlyPublishedLocations.contains(destination))
                    .collect(Collectors.toList());

            List<String> unpublishLocations = currentlyPublishedLocations.stream()
                    .map(destination -> (String) destination)
                    .filter(destination -> !destinations.contains(destination))
                    .collect(Collectors.toList());

            updatedPublishedLocations.addAll(destinations.stream()
                    .filter(destination -> !publishLocations.contains(destination))
                    .collect(Collectors.toList()));

            for (String id : publishLocations) {
                CreateRequest createRequest = new CreateRequestImpl(metacard);
                try {
                    CreateResponse createResponse = catalogStoreMap.get(id)
                            .create(createRequest);
                    if (createResponse.getProcessingErrors()
                            .isEmpty()) {
                        updatedPublishedLocations.addAll(createResponse.getCreatedMetacards()
                                .stream()
                                .filter(createdMetacard -> createdMetacard.getAttribute(REGISTRY_ID)
                                        .equals(metacard.getAttribute(REGISTRY_ID)))
                                .map(createdMetacard -> id)
                                .collect(Collectors.toList()));
                    }
                } catch (IngestException e) {
                    LOGGER.error("Unable to create metacard in catalogStore {}", id, e);
                    if (checkIfMetacardExists(id,
                            metacard.getAttribute(REGISTRY_ID)
                                    .getValue()
                                    .toString())) {
                        //The metacard is still in the catalogStore, thus it was persisted
                        updatedPublishedLocations.add(id);
                    }
                }
            }
            for (String id : unpublishLocations) {
                DeleteRequest deleteRequest = new DeleteRequestImpl(metacard.getId());
                try {
                    DeleteResponse deleteResponse = catalogStoreMap.get(id)
                            .delete(deleteRequest);
                } catch (IngestException e) {
                    LOGGER.error("Unable to delete metacard from catalogStore {}", id, e);
                    if (checkIfMetacardExists(id,
                            metacard.getAttribute(REGISTRY_ID)
                                    .getValue()
                                    .toString())) {
                        //The metacard is still in the catalogStore, thus wasn't unpublished
                        updatedPublishedLocations.add(id);
                    }

                }
            }

            metacard.setAttribute(new AttributeImpl(PUBLISHED_LOCATIONS,
                    updatedPublishedLocations));
            try {
                catalogFramework.update(new UpdateRequestImpl(metacard.getId(), metacard));
            } catch (IngestException e) {
                LOGGER.error("Unable to update metacard", e);
            } catch (SourceUnavailableException e) {
                LOGGER.error("Unable to update metacard, source unavailable", e);
            }
        }

        return updatedPublishedLocations;
    }

    private Boolean checkIfMetacardExists(String storeId, String registryId)
            throws UnsupportedQueryException {
        Filter filter = filterBuilder.attribute(REGISTRY_ID)
                .is()
                .equalTo()
                .text(registryId);
        Query query = new QueryImpl(filter);
        SourceResponse queryResponse = catalogStoreMap.get(storeId)
                .query(new QueryRequestImpl(query));
        return queryResponse.getHits() > 0;
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
}
