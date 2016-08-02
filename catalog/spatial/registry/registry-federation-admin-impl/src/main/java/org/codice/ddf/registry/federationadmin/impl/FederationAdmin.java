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
package org.codice.ddf.registry.federationadmin.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.xml.bind.JAXBElement;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.registry.federationadmin.FederationAdminMBean;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.registry.schemabindings.EbrimConstants;
import org.codice.ddf.registry.schemabindings.converter.type.RegistryPackageTypeConverter;
import org.codice.ddf.registry.schemabindings.converter.web.RegistryPackageWebConverter;
import org.codice.ddf.registry.schemabindings.helper.MetacardMarshaller;
import org.codice.ddf.registry.schemabindings.helper.SlotTypeHelper;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.endpoint.CatalogEndpoint;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.Source;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ValueListType;

public class FederationAdmin implements FederationAdminMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(FederationAdmin.class);

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

    private static final String TRANSIENT_VALUES_KEY = "TransientValues";

    private static final String AUTO_POPULATE_VALUES_KEY = "autoPopulateValues";

    private static final String SERVICE_BINDINGS_KEY = "ServiceBinding";

    private static final String CUSTOM_SLOTS_KEY = "customSlots";

    private static final String NODES_KEY = "nodes";

    private static final String KARAF_ETC = "karaf.etc";

    private static final String REGISTRY_CONFIG_DIR = "registry";

    private static final String REGISTRY_FIELDS_FILE = "registry-custom-slots.json";

    private static final String DATE_TIME = CswConstants.XML_SCHEMA_NAMESPACE_PREFIX.concat(
            ":dateTime");

    private final AdminHelper helper;

    private MBeanServer mbeanServer;

    private ObjectName objectName;

    private FederationAdminService federationAdminService;

    private InputTransformer registryTransformer;

    private MetacardMarshaller metacardMarshaller;

    private Map<String, Object> customSlots;

    private Map<String, Map<String, String>> endpointMap = new HashMap<>();

    private RegistryPackageTypeConverter registryTypeConverter;

    private RegistryPackageWebConverter registryMapConverter;

    private SlotTypeHelper slotHelper;

    public FederationAdmin(AdminHelper helper) {
        configureMBean();
        this.helper = helper;
    }

    @Override
    public String createLocalEntry(Map<String, Object> registryMap)
            throws FederationAdminException {

        Optional<RegistryPackageType> registryPackageOptional = registryTypeConverter.convert(
                registryMap);
        RegistryPackageType registryPackage =
                registryPackageOptional.orElseThrow(() -> new FederationAdminException(
                        "Error creating local registry entry. Couldn't convert registry map to a registry package."));

        if (!registryPackage.isSetHome()) {
            registryPackage.setHome(SystemBaseUrl.getBaseUrl());
        }

        if (!registryPackage.isSetObjectType()) {
            registryPackage.setObjectType(RegistryConstants.REGISTRY_NODE_OBJECT_TYPE);
        }

        if (!registryPackage.isSetId()) {
            String registryPackageId = RegistryConstants.GUID_PREFIX + UUID.randomUUID()
                    .toString()
                    .replaceAll("-", "");
            registryPackage.setId(registryPackageId);
        }

        updateDateFields(registryPackage);

        Metacard metacard = getRegistryMetacardFromRegistryPackage(registryPackage);
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE,
                true));
        return federationAdminService.addRegistryEntry(metacard);
    }

    @Override
    public String createLocalEntry(String base64EncodedXmlData) throws FederationAdminException {
        if (StringUtils.isBlank(base64EncodedXmlData)) {
            throw new FederationAdminException(
                    "Error creating local entry. String provided was blank.");
        }

        String metacardId;
        try (InputStream xmlStream = new ByteArrayInputStream(Base64.getDecoder()
                .decode(base64EncodedXmlData))) {
            Metacard metacard = getRegistryMetacardFromInputStream(xmlStream);
            metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE,
                    true));
            metacardId = federationAdminService.addRegistryEntry(metacard);
        } catch (IOException | IllegalArgumentException e) {
            throw new FederationAdminException("Error creating local entry. Couldn't decode string.",
                    e);
        }
        return metacardId;
    }

    @Override
    public void updateLocalEntry(Map<String, Object> registryObjectMap)
            throws FederationAdminException {

        Optional<RegistryPackageType> registryPackageOptional = registryTypeConverter.convert(
                registryObjectMap);
        RegistryPackageType registryPackage =
                registryPackageOptional.orElseThrow(() -> new FederationAdminException(
                        "Error updating local registry entry. Couldn't convert registry map to a registry package."));

        updateDateFields(registryPackage);

        List<Metacard> existingMetacards =
                federationAdminService.getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(
                        registryPackage.getId()));

        if (CollectionUtils.isEmpty(existingMetacards)) {
            String message = "Error updating local registry entry. Registry metacard not found.";
            LOGGER.error("{} Registry ID: {}", message, registryPackage.getId());
            throw new FederationAdminException(message);
        }

        if (existingMetacards.size() > 1) {
            throw new FederationAdminException(
                    "Error updating local registry entry. Multiple registry metacards found.");
        }
        Metacard existingMetacard = existingMetacards.get(0);

        Metacard updateMetacard = getRegistryMetacardFromRegistryPackage(registryPackage);
        updateMetacard.setAttribute(new AttributeImpl(Metacard.ID, existingMetacard.getId()));

        federationAdminService.updateRegistryEntry(updateMetacard);
    }

    @Override
    public void deleteLocalEntry(List<String> ids) throws FederationAdminException {
        if (CollectionUtils.isEmpty(ids)) {
            throw new FederationAdminException(
                    "Error deleting local registry entries. No ids provided.");
        }

        List<Metacard> localMetacards =
                federationAdminService.getRegistryMetacardsByRegistryIds(ids);
        List<String> metacardIds = new ArrayList<>();

        metacardIds.addAll(localMetacards.stream()
                .map(Metacard::getId)
                .collect(Collectors.toList()));
        if (ids.size() != metacardIds.size()) {
            String message = "Error deleting local registry entries. ";
            LOGGER.error("{} Registry Ids provided: {}. Registry metacard ids found: {}",
                    message,
                    ids,
                    metacardIds);
            throw new FederationAdminException(message);
        }

        federationAdminService.deleteRegistryEntriesByMetacardIds(metacardIds);
    }

    @Override
    public Map<String, Object> getLocalNodes() throws FederationAdminException {
        Map<String, Object> localNodes = new HashMap<>();
        List<Map<String, Object>> registryWebMaps;

        List<RegistryPackageType> registryPackages =
                federationAdminService.getLocalRegistryObjects();

        List<Metacard> metacards = federationAdminService.getLocalRegistryMetacards();
        Map<String, Metacard> metacardByRegistryIdMap = getRegistryIdMetacardMap(metacards);

        registryWebMaps = getWebMapsFromRegistryPackages(registryPackages, metacardByRegistryIdMap);

        if (customSlots != null) {
            localNodes.put(CUSTOM_SLOTS_KEY, customSlots);
        }

        Map<String, Object> autoPopulateMap = new HashMap<>();
        autoPopulateMap.put(SERVICE_BINDINGS_KEY, endpointMap.values());
        localNodes.put(AUTO_POPULATE_VALUES_KEY, autoPopulateMap);

        localNodes.put(NODES_KEY, registryWebMaps);

        return localNodes;
    }

    @Override
    public boolean registryStatus(String servicePID) {
        try {
            List<Source> sources = helper.getRegistrySources();
            for (Source source : sources) {
                if (source instanceof ConfiguredService) {
                    ConfiguredService cs = (ConfiguredService) source;
                    try {
                        Configuration config = helper.getConfiguration(cs);
                        if (config != null && config.getProperties()
                                .get(Constants.SERVICE_PID)
                                .equals(servicePID)) {
                            try {
                                return source.isAvailable();
                            } catch (Exception e) {
                                LOGGER.warn("Couldn't get availability on registry {}: {}",
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
    public List<Map<String, Object>> allRegistryInfo() {

        List<Map<String, Object>> metatypes = helper.getMetatypes();

        for (Map metatype : metatypes) {
            try {
                List<Configuration> configs = helper.getConfigurations(metatype);

                ArrayList<Map<String, Object>> configurations = new ArrayList<>();
                if (configs != null) {
                    for (Configuration config : configs) {
                        Map<String, Object> registry = new HashMap<>();

                        boolean disabled = config.getPid()
                                .endsWith(DISABLED);
                        registry.put(MAP_ENTRY_ID, config.getPid());
                        registry.put(MAP_ENTRY_ENABLED, !disabled);
                        registry.put(MAP_ENTRY_FPID, config.getFactoryPid());

                        if (!disabled) {
                            registry.put(MAP_ENTRY_NAME, helper.getName(config));
                            registry.put(MAP_ENTRY_BUNDLE_NAME, helper.getBundleName(config));
                            registry.put(MAP_ENTRY_BUNDLE_LOCATION, config.getBundleLocation());
                            registry.put(MAP_ENTRY_BUNDLE, helper.getBundleId(config));
                        } else {
                            registry.put(MAP_ENTRY_NAME, config.getPid());
                        }

                        Dictionary<String, Object> properties = config.getProperties();
                        Map<String, Object> plist = new HashMap<>();
                        for (String key : Collections.list(properties.keys())) {
                            plist.put(key, properties.get(key));
                        }
                        registry.put(MAP_ENTRY_PROPERTIES, plist);

                        configurations.add(registry);
                    }
                    metatype.put(MAP_ENTRY_CONFIGURATIONS, configurations);
                }
            } catch (InvalidSyntaxException | IOException e) {
                LOGGER.warn("Error getting registry info: {}", e.getMessage());
            }
        }

        Collections.sort(metatypes,
                (o1, o2) -> ((String) o1.get("id")).compareToIgnoreCase((String) o2.get("id")));
        return metatypes;
    }

    @Override
    public Map<String, Object> allRegistryMetacards() {
        Map<String, Object> nodes = new HashMap<>();
        List<Map<String, Object>> registryMetacardInfo = new ArrayList<>();

        try {
            List<RegistryPackageType> registryMetacardObjects =
                    federationAdminService.getRegistryObjects();

            List<Metacard> metacards = federationAdminService.getRegistryMetacards();
            Map<String, Metacard> metacardByRegistryIdMap = getRegistryIdMetacardMap(metacards);

            registryMetacardInfo = getWebMapsFromRegistryPackages(registryMetacardObjects,
                    metacardByRegistryIdMap);

        } catch (FederationAdminException e) {
            LOGGER.warn("Couldn't get remote registry metacards '{}'", e);
        }

        if (customSlots != null) {
            nodes.put(CUSTOM_SLOTS_KEY, customSlots);
        }

        Map<String, Object> autoPopulateMap = new HashMap<>();
        autoPopulateMap.put(SERVICE_BINDINGS_KEY, endpointMap.values());
        nodes.put(AUTO_POPULATE_VALUES_KEY, autoPopulateMap);

        nodes.put(NODES_KEY, registryMetacardInfo);

        return nodes;
    }

    private List<Map<String, Object>> getWebMapsFromRegistryPackages(
            List<RegistryPackageType> packages, Map<String, Metacard> metacardByRegistryIdMap) {
        List<Map<String, Object>> registryMaps = new ArrayList<>();
        for (RegistryPackageType registryPackage : packages) {
            Map<String, Object> registryWebMap = registryMapConverter.convert(registryPackage);

            Metacard metacard = metacardByRegistryIdMap.get(registryPackage.getId());
            Map<String, Object> transientValues = getTransientValuesMap(metacard);
            if (MapUtils.isNotEmpty(transientValues)) {
                registryWebMap.put(TRANSIENT_VALUES_KEY, transientValues);
            }

            if (MapUtils.isNotEmpty(registryWebMap)) {
                registryMaps.add(registryWebMap);
            }
        }
        return registryMaps;
    }

    private Metacard getRegistryMetacardFromRegistryPackage(RegistryPackageType registryPackage)
            throws FederationAdminException {
        if (registryPackage == null) {
            throw new FederationAdminException(
                    "Error creating metacard from registry package. Null package was received.");
        }
        Metacard metacard;

        try {
            metacard =
                    registryTransformer.transform(metacardMarshaller.getRegistryPackageAsInputStream(
                            registryPackage));
        } catch (IOException | CatalogTransformerException | ParserException e) {
            String message = "Error creating metacard from registry package.";
            LOGGER.error("{} Registry id: {}", message, registryPackage.getId());
            throw new FederationAdminException(message, e);
        }

        return metacard;
    }

    private Metacard getRegistryMetacardFromInputStream(InputStream inputStream)
            throws FederationAdminException {
        if (inputStream == null) {
            throw new FederationAdminException(
                    "Error converting input stream to a metacard. Null input stream provided.");
        }

        Metacard metacard;
        try {
            metacard = registryTransformer.transform(inputStream);
        } catch (IOException | CatalogTransformerException e) {
            throw new FederationAdminException(
                    "Error getting metacard. RegistryTransformer couldn't convert the input stream.",
                    e);
        }

        return metacard;
    }

    private Map<String, Metacard> getRegistryIdMetacardMap(List<Metacard> metacards) {
        Map<String, Metacard> registryIdMetacardMap = new HashMap<>();

        for (Metacard metacard : metacards) {
            String registryId = RegistryUtility.getRegistryId(metacard);
            if (registryId == null) {
                continue;
            }

            registryIdMetacardMap.put(registryId, metacard);
        }

        return registryIdMetacardMap;
    }

    private Map<String, Object> getTransientValuesMap(Metacard metacard) {
        Map<String, Object> transientValuesMap = new HashMap<>();
        if (metacard != null) {
            for (String transientAttributeKey : RegistryObjectMetacardType.TRANSIENT_ATTRIBUTES) {
                Attribute transientAttribute = metacard.getAttribute(transientAttributeKey);

                if (transientAttribute != null) {
                    transientValuesMap.put(transientAttributeKey, transientAttribute.getValues());
                }
            }
        }
        return transientValuesMap;
    }

    private void updateDateFields(RegistryPackageType rpt) {

        ExtrinsicObjectType nodeInfo = null;
        for (JAXBElement identifiable : rpt.getRegistryObjectList()
                .getIdentifiable()) {
            RegistryObjectType registryObject = (RegistryObjectType) identifiable.getValue();

            if (registryObject instanceof ExtrinsicObjectType
                    && RegistryConstants.REGISTRY_NODE_OBJECT_TYPE.equals(registryObject.getObjectType())) {
                nodeInfo = (ExtrinsicObjectType) registryObject;
                break;
            }
        }
        if (nodeInfo != null) {
            boolean liveDateFound = false;
            boolean lastUpdatedFound = false;

            OffsetDateTime now = OffsetDateTime.now(ZoneId.of(ZoneOffset.UTC.toString()));
            String rightNow = now.toString();

            for (SlotType1 slot : nodeInfo.getSlot()) {
                if (slot.getName()
                        .equals(RegistryConstants.XML_LIVE_DATE_NAME)) {
                    liveDateFound = true;
                } else if (slot.getName()
                        .equals(RegistryConstants.XML_LAST_UPDATED_NAME)) {
                    ValueListType valueList = EbrimConstants.RIM_FACTORY.createValueListType();
                    valueList.getValue()
                            .add(rightNow);
                    slot.setValueList(EbrimConstants.RIM_FACTORY.createValueList(valueList));
                    lastUpdatedFound = true;
                }
            }

            if (!liveDateFound) {
                SlotType1 liveDate = slotHelper.create(RegistryConstants.XML_LIVE_DATE_NAME,
                        rightNow,
                        DATE_TIME);

                nodeInfo.getSlot()
                        .add(liveDate);
            }

            if (!lastUpdatedFound) {
                SlotType1 lastUpdated = slotHelper.create(RegistryConstants.XML_LAST_UPDATED_NAME,
                        rightNow,
                        DATE_TIME);

                nodeInfo.getSlot()
                        .add(lastUpdated);
            }
        }
    }

    private void configureMBean() {
        mbeanServer = ManagementFactory.getPlatformMBeanServer();

        try {
            objectName = new ObjectName(FederationAdminMBean.OBJECT_NAME);
        } catch (MalformedObjectNameException e) {
            LOGGER.warn("Exception while creating object name: " + FederationAdminMBean.OBJECT_NAME,
                    e);
        }

        try {
            try {
                mbeanServer.registerMBean(new StandardMBean(this, FederationAdminMBean.class),
                        objectName);
            } catch (InstanceAlreadyExistsException e) {
                mbeanServer.unregisterMBean(objectName);
                mbeanServer.registerMBean(new StandardMBean(this, FederationAdminMBean.class),
                        objectName);
            }
        } catch (Exception e) {
            LOGGER.error("Could not register mbean.", e);
        }
    }

    public void destroy() {
        try {
            if (objectName != null && mbeanServer != null) {
                mbeanServer.unregisterMBean(objectName);
            }
        } catch (Exception e) {
            LOGGER.warn("Exception un registering mbean: ", e);
            throw new RuntimeException(e);
        }
    }

    public void init() {

        Path path = Paths.get(System.getProperty(KARAF_ETC),
                REGISTRY_CONFIG_DIR,
                REGISTRY_FIELDS_FILE);
        if (Files.exists(path)) {
            try {
                String registryFieldsJsonString = new String(Files.readAllBytes(path),
                        StandardCharsets.UTF_8);
                Gson gson = new Gson();
                customSlots = new HashMap<>();
                customSlots = (Map<String, Object>) gson.fromJson(registryFieldsJsonString,
                        customSlots.getClass());
            } catch (IOException e) {
                LOGGER.error(
                        "Error reading {}. This will result in no custom fields being shown for registry node editing",
                        path.toString(),
                        e);
            }
        }
    }

    public void bindEndpoint(ServiceReference reference) {
        BundleContext context = getContext();
        if (reference != null && context != null) {
            CatalogEndpoint endpoint = (CatalogEndpoint) context.getService(reference);
            Map<String, String> properties = endpoint.getEndpointProperties();
            endpointMap.put(properties.get(CatalogEndpoint.ID_KEY), properties);
        }
    }

    public void unbindEndpoint(ServiceReference reference) {
        BundleContext context = getContext();
        if (reference != null && context != null) {
            CatalogEndpoint endpoint = (CatalogEndpoint) context.getService(reference);
            Map<String, String> properties = endpoint.getEndpointProperties();
            endpointMap.remove(properties.get(CatalogEndpoint.ID_KEY));
        }
    }

    protected BundleContext getContext() {
        Bundle bundle = FrameworkUtil.getBundle(FederationAdmin.class);
        if (bundle != null) {
            return bundle.getBundleContext();
        }
        return null;
    }

    public void setFederationAdminService(FederationAdminService federationAdminService) {
        this.federationAdminService = federationAdminService;
    }

    public void setMetacardMarshaller(MetacardMarshaller metacardMarshaller) {
        this.metacardMarshaller = metacardMarshaller;
    }

    public void setRegistryTransformer(InputTransformer inputTransformer) {
        this.registryTransformer = inputTransformer;
    }

    public void setRegistryTypeConverter(RegistryPackageTypeConverter registryTypeConverter) {
        this.registryTypeConverter = registryTypeConverter;
    }

    public void setRegistryMapConverter(RegistryPackageWebConverter registryMapConverter) {
        this.registryMapConverter = registryMapConverter;
    }

    public void setSlotHelper(SlotTypeHelper slotHelper) {
        this.slotHelper = slotHelper;
    }

}
