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
package ddf.catalog.registry.federationadmin.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.registry.common.RegistryConstants;
import ddf.catalog.registry.common.metacard.RegistryObjectMetacardType;
import ddf.catalog.registry.federationadmin.FederationAdminMBean;
import ddf.catalog.registry.federationadmin.converter.RegistryPackageWebConverter;
import ddf.catalog.registry.federationadmin.service.FederationAdminException;
import ddf.catalog.registry.federationadmin.service.FederationAdminService;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ObjectFactory;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

public class FederationAdmin implements FederationAdminMBean {

    private MBeanServer mbeanServer;

    private ObjectName objectName;

    private static final Logger LOGGER = LoggerFactory.getLogger(FederationAdmin.class);

    private static final ObjectFactory RIM_FACTORY = new ObjectFactory();

    private FederationAdminService federationAdminService;

    private InputTransformer registryTransformer;

    private Parser parser;

    private ParserConfigurator marshalConfigurator;

    public FederationAdmin() {
        configureMBean();
    }

    @Override
    public String createLocalEntry(Map<String, Object> registryMap)
            throws FederationAdminException {
        if (MapUtils.isEmpty(registryMap)) {
            throw new FederationAdminException(
                    "Error creating local registry entry. Null map provided.");
        }

        RegistryPackageType registryPackage =
                RegistryPackageWebConverter.getRegistryPackageFromWebMap(registryMap);

        if (registryPackage == null) {
            throw new FederationAdminException(
                    "Error creating local registry entry. Couldn't convert registry map to a registry package.");
        }

        if (!registryPackage.isSetHome()) {
            String home = SystemBaseUrl.getBaseUrl();
            if (StringUtils.isNotBlank(home)) {
                registryPackage.setHome(home);
            }
        }

        if (!registryPackage.isSetObjectType()) {
            registryPackage.setObjectType(RegistryConstants.REGISTRY_NODE_OBJECT_TYPE);
        }

        if (!registryPackage.isSetId()) {
            String registryPackageId = UUID.randomUUID()
                    .toString()
                    .replaceAll("-", "");
            registryPackage.setId(registryPackageId);
        }

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
            String message = "Error creating local entry. Couldn't decode string.";
            LOGGER.error("{} Base64 encoded xml: {}", message, base64EncodedXmlData);
            throw new FederationAdminException(message, e);
        }
        return metacardId;
    }

    @Override
    public void updateLocalEntry(Map<String, Object> registryObjectMap)
            throws FederationAdminException {
        if (MapUtils.isEmpty(registryObjectMap)) {
            throw new FederationAdminException(
                    "Error updating local registry entry. Null map provided.");
        }
        RegistryPackageType registryPackage =
                RegistryPackageWebConverter.getRegistryPackageFromWebMap(registryObjectMap);

        if (registryPackage == null) {
            String message =
                    "Error updating local registry entry. Couldn't convert registry map to a registry package.";
            LOGGER.error("{} Registry Map: {}", message, registryObjectMap);
            throw new FederationAdminException(message);
        }

        List<Metacard> existingMetacards =
                federationAdminService.getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(
                        registryPackage.getId()));

        if (CollectionUtils.isEmpty(existingMetacards)) {
            String message = "Error updating local registry entry. Registry metacard not found.";
            LOGGER.error("{} Registry ID: {}", message, registryPackage.getId());
            throw new FederationAdminException(message);
        }

        if (existingMetacards.size() > 1) {
            String message =
                    "Error updating local registry entry. Multiple registry metacards found.";

            List<String> metacardIds = new ArrayList<>();
            metacardIds.addAll(existingMetacards.stream()
                    .map(Metacard::getId)
                    .collect(Collectors.toList()));
            LOGGER.error("{} Matching registry metacard ids: {}", message, metacardIds);

            throw new FederationAdminException(message);
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
                federationAdminService.getLocalRegistryMetacardsByRegistryIds(ids);
        List<String> metacardIds = new ArrayList<>();

        metacardIds.addAll(localMetacards.stream()
                .map(Metacard::getId)
                .collect(Collectors.toList()));
        if (ids.size() != metacardIds.size()) {
            String message = "Error deleting local registry entries. Registry ids provided.";
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
        List<Map<String, Object>> registryWebMaps = new ArrayList<>();

        List<RegistryPackageType> registryPackages =
                federationAdminService.getLocalRegistryObjects();
        registryWebMaps.addAll(registryPackages.stream()
                .map(RegistryPackageWebConverter::getRegistryObjectWebMap)
                .collect(Collectors.toList()));

        localNodes.put("localNodes", registryWebMaps);

        return localNodes;
    }

    private Metacard getRegistryMetacardFromRegistryPackage(RegistryPackageType registryPackage)
            throws FederationAdminException {
        if (registryPackage == null) {
            throw new FederationAdminException(
                    "Error creating metacard from registry package. Null package was received.");
        }
        Metacard metacard;

        try {
            JAXBElement<RegistryPackageType> jaxbRegistryObjectType =
                    RIM_FACTORY.createRegistryPackage(registryPackage);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            parser.marshal(marshalConfigurator, jaxbRegistryObjectType, baos);
            InputStream xmlInputStream = new ByteArrayInputStream(baos.toByteArray());
            metacard = registryTransformer.transform(xmlInputStream);

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

    public void setFederationAdminService(FederationAdminService federationAdminService) {
        this.federationAdminService = federationAdminService;
    }

    public void setParser(Parser parser) {
        List<String> contextPath = Arrays.asList(RegistryObjectType.class.getPackage()
                        .getName(),
                net.opengis.ogc.ObjectFactory.class.getPackage()
                        .getName(),
                net.opengis.gml.v_3_1_1.ObjectFactory.class.getPackage()
                        .getName());

        ClassLoader classLoader = this.getClass()
                .getClassLoader();

        this.marshalConfigurator = parser.configureParser(contextPath, classLoader);
        this.marshalConfigurator.addProperty(Marshaller.JAXB_FRAGMENT, true);

        this.parser = parser;
    }

    public void setRegistryTransformer(InputTransformer inputTransformer) {
        this.registryTransformer = inputTransformer;
    }
}
