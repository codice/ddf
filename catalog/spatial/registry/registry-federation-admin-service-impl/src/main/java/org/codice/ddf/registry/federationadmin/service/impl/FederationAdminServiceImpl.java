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
package org.codice.ddf.registry.federationadmin.service.impl;

import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConstants;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.registry.schemabindings.EbrimConstants;
import org.codice.ddf.registry.schemabindings.helper.InternationalStringTypeHelper;
import org.codice.ddf.registry.schemabindings.helper.SlotTypeHelper;
import org.codice.ddf.security.common.Security;
import org.geotools.filter.SortByImpl;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.PropertyNameImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.security.service.SecurityServiceException;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.VersionInfoType;

@SuppressWarnings("PackageAccessibility")
public class FederationAdminServiceImpl implements FederationAdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FederationAdminServiceImpl.class);

    private static final int PAGE_SIZE = 1000;

    private CatalogFramework catalogFramework;

    private InputTransformer registryTransformer;

    private Parser parser;

    private ParserConfigurator marshalConfigurator;

    private ParserConfigurator unmarshalConfigurator;

    private FilterBuilder filterBuilder;

    private final Security security;

    private SlotTypeHelper slotTypeHelper = new SlotTypeHelper();

    private InternationalStringTypeHelper internationalStringTypeHelper =
            new InternationalStringTypeHelper();

    public FederationAdminServiceImpl() {
        this(Security.getInstance());
    }

    FederationAdminServiceImpl(Security security) {
        this(null, security);
    }

    FederationAdminServiceImpl(CatalogFramework catalogFramework) {
        this(catalogFramework, Security.getInstance());
    }

    FederationAdminServiceImpl(CatalogFramework catalogFramework, Security security) {
        if (catalogFramework != null) {
            this.catalogFramework = catalogFramework;
        }
        this.security = security;
    }

    @Override
    public String addRegistryEntry(String xml) throws FederationAdminException {
        Metacard metacard = getRegistryMetacardFromString(xml);
        return addRegistryEntry(metacard);
    }

    @Override
    public String addRegistryEntry(String xml, Set<String> destinations)
            throws FederationAdminException {
        Metacard metacard = getRegistryMetacardFromString(xml);
        return addRegistryEntry(metacard, destinations);
    }

    @Override
    public String addRegistryEntry(Metacard metacard) throws FederationAdminException {
        return addRegistryEntry(metacard, null);
    }

    @Override
    public List<String> addRegistryEntries(List<Metacard> metacards, Set<String> destinations)
            throws FederationAdminException {
        validateRegistryMetacards(metacards);

        List<String> registryIds;
        Map<String, Serializable> properties = new HashMap<>();
        CreateRequest createRequest = new CreateRequestImpl(metacards, properties, destinations);

        try {
            CreateResponse createResponse =
                    security.runWithSubjectOrElevate(() -> catalogFramework.create(createRequest));
            //loop through to get id's
            if (!createResponse.getProcessingErrors()
                    .isEmpty()) {
                throw new FederationAdminException(
                        "Processing error occurred while creating registry entry.");
            }
            registryIds = createResponse.getCreatedMetacards()
                    .stream()
                    .map(e -> e.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                            .getValue()
                            .toString())
                    .collect(Collectors.toList());
        } catch (SecurityServiceException | InvocationTargetException e) {
            throw new FederationAdminException("Error adding local registry entry.", e);
        }

        return registryIds;
    }

    @Override
    public String addRegistryEntry(Metacard metacard, Set<String> destinations)
            throws FederationAdminException {
        List<String> registryIds = addRegistryEntries(Collections.singletonList(metacard),
                destinations);
        if (CollectionUtils.isNotEmpty(registryIds)) {
            return registryIds.get(0);
        }
        return null;
    }

    @Override
    public void updateRegistryEntry(String xml) throws FederationAdminException {
        Metacard updateMetacard = getRegistryMetacardFromString(xml);
        updateRegistryEntry(updateMetacard);
    }

    @Override
    public void updateRegistryEntry(String xml, Set<String> destinations)
            throws FederationAdminException {
        Metacard updateMetacard = getRegistryMetacardFromString(xml);
        updateRegistryEntry(updateMetacard, destinations);
    }

    @Override
    public void updateRegistryEntry(Metacard updateMetacard) throws FederationAdminException {
        updateRegistryEntry(updateMetacard, null);
    }

    @Override
    public void updateRegistryEntry(Metacard updateMetacard, Set<String> destinations)
            throws FederationAdminException {
        validateRegistryMetacards(Collections.singletonList(updateMetacard));
        String registryId = updateMetacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                .getValue()
                .toString();

        List<Metacard> existingMetacards =
                this.getRegistryMetacardsByRegistryIds(Collections.singletonList(registryId));

        if (existingMetacards.size() > 1) {
            String message = "Error updating registry entry. Multiple registry metacards found.";
            List<String> metacardIds = existingMetacards.stream()
                    .map(Metacard::getId)
                    .collect(Collectors.toList());
            LOGGER.error("{} Matching registry metacard ids: {}", message, metacardIds);

            throw new FederationAdminException(message);
        }
        if (existingMetacards.size() == 0) {
            String message =
                    "Error updating registry entry. No corresponding registry entry found.";
            LOGGER.error("{}", message);
            throw new FederationAdminException(message);
        }
        Metacard existingMetacard = existingMetacards.get(0);
        boolean transientUpdate = false;
        for (String transientAttributeKey : RegistryObjectMetacardType.TRANSIENT_ATTRIBUTES) {
            Attribute transientAttribute = updateMetacard.getAttribute(transientAttributeKey);
            if (transientAttribute == null) {
                transientAttribute = existingMetacard.getAttribute(transientAttributeKey);
                if (transientAttribute != null) {
                    updateMetacard.setAttribute(transientAttribute);
                }
            } else {
                Attribute existingAttribute = existingMetacard.getAttribute(transientAttributeKey);
                if (existingAttribute == null) {
                    transientUpdate = true;
                    continue;
                }
                if (transientAttribute.getValues()
                        .size() != existingAttribute.getValues()
                        .size()) {
                    transientUpdate = true;
                    continue;
                }
                for (Serializable value : transientAttribute.getValues()) {
                    if (!existingAttribute.getValues()
                            .contains(value)) {
                        transientUpdate = true;
                        break;
                    }
                }
            }
        }

        Map<String, Serializable> properties = new HashMap<>();

        if (transientUpdate) {
            properties.put(RegistryConstants.TRANSIENT_ATTRIBUTE_UPDATE, true);
        }

        List<Map.Entry<Serializable, Metacard>> updateList = new ArrayList<>();
        updateList.add(new AbstractMap.SimpleEntry<>(registryId, updateMetacard));

        UpdateRequest updateRequest = new UpdateRequestImpl(updateList,
                RegistryObjectMetacardType.REGISTRY_ID,
                properties,
                destinations);

        try {
            UpdateResponse updateResponse =
                    security.runWithSubjectOrElevate(() -> catalogFramework.update(updateRequest));
            if (!updateResponse.getProcessingErrors()
                    .isEmpty()) {
                throw new FederationAdminException(
                        "Processing error occurred while updating registry entry.");
            }
        } catch (SecurityServiceException | InvocationTargetException e) {
            String message = "Error updating registry entry.";
            LOGGER.error("{} Metacard ID: {}", message, updateMetacard.getId());
            throw new FederationAdminException(message, e);
        }

    }

    @Override
    public void deleteRegistryEntriesByRegistryIds(List<String> registryIds)
            throws FederationAdminException {
        deleteRegistryEntriesByRegistryIds(registryIds, null);
    }

    @Override
    public void deleteRegistryEntriesByRegistryIds(List<String> registryIds,
            Set<String> destinations) throws FederationAdminException {
        if (CollectionUtils.isEmpty(registryIds)) {
            throw new FederationAdminException(
                    "An empty list of registry ids to be deleted was received. Nothing to delete.");
        }

        List<Serializable> serializableIds = new ArrayList<>(registryIds);
        Map<String, Serializable> properties = new HashMap<>();
        DeleteRequest deleteRequest = new DeleteRequestImpl(serializableIds,
                RegistryObjectMetacardType.REGISTRY_ID,
                properties,
                destinations);
        try {
            DeleteResponse deleteResponse =
                    security.runWithSubjectOrElevate(() -> catalogFramework.delete(deleteRequest));
            if (!deleteResponse.getProcessingErrors()
                    .isEmpty()) {
                throw new FederationAdminException(
                        "Processing error occurred while deleting registry entry.");
            }
        } catch (SecurityServiceException | InvocationTargetException e) {
            String message = "Error deleting registry entries by registry id.";
            LOGGER.error("{} Registry Ids provided: {}", message, registryIds);
            throw new FederationAdminException(message, e);
        }
    }

    @Override
    public void deleteRegistryEntriesByMetacardIds(List<String> metacardIds)
            throws FederationAdminException {
        deleteRegistryEntriesByMetacardIds(metacardIds, null);
    }

    @Override
    public void deleteRegistryEntriesByMetacardIds(List<String> metacardIds,
            Set<String> destinations) throws FederationAdminException {
        if (CollectionUtils.isEmpty(metacardIds)) {
            throw new FederationAdminException(
                    "An empty list of metacard ids to be deleted was received. Nothing to delete.");
        }

        List<Serializable> serializableIds = new ArrayList<>(metacardIds);
        Map<String, Serializable> properties = new HashMap<>();
        DeleteRequest deleteRequest = new DeleteRequestImpl(serializableIds,
                Metacard.ID,
                properties,
                destinations);
        try {
            DeleteResponse deleteResponse =
                    security.runWithSubjectOrElevate(() -> catalogFramework.delete(deleteRequest));
            if (!deleteResponse.getProcessingErrors()
                    .isEmpty()) {
                throw new FederationAdminException(
                        "Processing error occurred while deleting registry entry.");
            }
        } catch (SecurityServiceException | InvocationTargetException e) {
            String message = "Error deleting registry entries by metacard ids.";
            LOGGER.error("{} Metacard Ids provided: {}", message, metacardIds);
            throw new FederationAdminException(message, e);
        }
    }

    @Override
    public List<Metacard> getRegistryMetacards() throws FederationAdminException {

        Filter filter = filterBuilder.allOf(getBasicFilter());

        return getRegistryMetacardsByFilter(filter);
    }

    @Override
    public List<Metacard> getLocalRegistryMetacards() throws FederationAdminException {

        List<Filter> filters = getBasicFilter();
        filters.add(filterBuilder.attribute(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE)
                .is()
                .bool(true));
        Filter filter = filterBuilder.allOf(filters);
        return getRegistryMetacardsByFilter(filter);
    }

    @Override
    public List<Metacard> getLocalRegistryMetacardsByRegistryIds(List<String> ids)
            throws FederationAdminException {
        if (CollectionUtils.isEmpty(ids)) {
            throw new FederationAdminException(
                    "Error getting local registry metacards by registry ids. Null list of Ids provided.");
        }
        List<Filter> idFilters = ids.stream()
                .map(id -> filterBuilder.attribute(RegistryObjectMetacardType.REGISTRY_ID)
                        .is()
                        .equalTo()
                        .text(id))
                .collect(Collectors.toList());
        List<Filter> filters = getBasicFilter();
        filters.add(filterBuilder.attribute(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE)
                .is()
                .bool(true));
        Filter filter = filterBuilder.allOf(filters);
        filter = filterBuilder.allOf(filter, filterBuilder.anyOf(idFilters));
        return getRegistryMetacardsByFilter(filter);
    }

    @Override
    public List<Metacard> getRegistryMetacardsByRegistryIds(List<String> ids)
            throws FederationAdminException {
        if (CollectionUtils.isEmpty(ids)) {
            throw new FederationAdminException(
                    "Error getting registry metacards by registry ids. Null list of Ids provided.");
        }

        List<Filter> idFilters = ids.stream()
                .map(id -> filterBuilder.attribute(RegistryObjectMetacardType.REGISTRY_ID)
                        .is()
                        .equalTo()
                        .text(id))
                .collect(Collectors.toList());
        List<Filter> filters = getBasicFilter();
        Filter filter = filterBuilder.allOf(filters);
        filter = filterBuilder.allOf(filter, filterBuilder.anyOf(idFilters));
        return getRegistryMetacardsByFilter(filter);
    }

    @Override
    public List<RegistryPackageType> getLocalRegistryObjects() throws FederationAdminException {
        List<RegistryPackageType> registryEntries = new ArrayList<>();

        for (Metacard metacard : getLocalRegistryMetacards()) {
            String xml = metacard.getMetadata();
            registryEntries.add(getRegistryPackageFromString(xml));
        }
        return registryEntries;
    }

    @Override
    public List<RegistryPackageType> getRegistryObjects() throws FederationAdminException {
        List<RegistryPackageType> registryEntries = new ArrayList<>();
        for (Metacard metacard : getRegistryMetacards()) {
            String xml = metacard.getMetadata();
            registryEntries.add(getRegistryPackageFromString(xml));
        }
        return registryEntries;
    }

    @Override
    public RegistryPackageType getRegistryObjectByRegistryId(String registryId)
            throws FederationAdminException {
        return getRegistryObjectByRegistryId(registryId, null);
    }

    @Override
    public RegistryPackageType getRegistryObjectByRegistryId(String registryId,
            List<String> sourceIds) throws FederationAdminException {
        if (StringUtils.isBlank(registryId)) {
            throw new FederationAdminException(
                    "Error getting registry object by metacard id. Empty id provided.");
        }
        List<Filter> filters = getBasicFilter();
        filters.add(filterBuilder.attribute(RegistryObjectMetacardType.REGISTRY_ID)
                .is()
                .equalTo()
                .text(registryId));

        Filter filter = filterBuilder.allOf(filters);

        List<Metacard> metacards = getRegistryMetacardsByFilter(filter, sourceIds);

        if (CollectionUtils.isEmpty(metacards)) {
            String message = "Error getting registry object by metacard id. No result returned.";
            LOGGER.debug("{} For metacard ID: {}, optional sources: {}",
                    message,
                    registryId,
                    sourceIds);
            throw new FederationAdminException(message);
        }

        if (metacards.size() > 1) {
            String message =
                    "Error getting registry object by metacard id. More than one metacards were returned.";
            LOGGER.error("{} For metacard ID: {}, optional sources: {}",
                    message,
                    registryId,
                    sourceIds);
            throw new FederationAdminException(message);
        }

        String xml = metacards.get(0)
                .getMetadata();
        return getRegistryPackageFromString(xml);
    }

    public void init() {
        org.codice.ddf.security.common.Security.runAsAdmin(() -> {
            try {
                if (getRegistryIdentityMetacard() == null) {
                    createIdentityNode();
                }
            } catch (FederationAdminException e) {
                LOGGER.error("There was an error bringing up the Federation Admin Service.", e);
            }
            return null;
        });
    }

    private void createIdentityNode() throws FederationAdminException {

        String registryPackageId = RegistryConstants.GUID_PREFIX + UUID.randomUUID()
                .toString()
                .replaceAll("-", "");
        RegistryPackageType registryPackage = RIM_FACTORY.createRegistryPackageType();
        registryPackage.setId(registryPackageId);
        registryPackage.setObjectType(RegistryConstants.REGISTRY_NODE_OBJECT_TYPE);

        ExtrinsicObjectType extrinsicObject = RIM_FACTORY.createExtrinsicObjectType();
        extrinsicObject.setObjectType(RegistryConstants.REGISTRY_NODE_OBJECT_TYPE);

        String extrinsicObjectId = RegistryConstants.GUID_PREFIX + UUID.randomUUID()
                .toString()
                .replaceAll("-", "");
        extrinsicObject.setId(extrinsicObjectId);

        String siteName = SystemInfo.getSiteName();
        if (StringUtils.isNotBlank(siteName)) {
            extrinsicObject.setName(internationalStringTypeHelper.create(siteName));
        }

        String home = SystemBaseUrl.getBaseUrl();
        if (StringUtils.isNotBlank(home)) {
            extrinsicObject.setHome(home);
        }

        String version = SystemInfo.getVersion();
        if (StringUtils.isNotBlank(version)) {
            VersionInfoType versionInfo = RIM_FACTORY.createVersionInfoType();
            versionInfo.setVersionName(version);

            extrinsicObject.setVersionInfo(versionInfo);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of(ZoneOffset.UTC.toString()));
        String rightNow = now.toString();

        SlotType1 lastUpdated = slotTypeHelper.create(RegistryConstants.XML_LAST_UPDATED_NAME,
                rightNow,
                DatatypeConstants.DATETIME.toString());
        extrinsicObject.getSlot()
                .add(lastUpdated);

        SlotType1 liveDate = slotTypeHelper.create(RegistryConstants.XML_LIVE_DATE_NAME,
                rightNow,
                DatatypeConstants.DATETIME.toString());
        extrinsicObject.getSlot()
                .add(liveDate);

        if (registryPackage.getRegistryObjectList() == null) {
            registryPackage.setRegistryObjectList(RIM_FACTORY.createRegistryObjectListType());
        }

        registryPackage.getRegistryObjectList()
                .getIdentifiable()
                .add(RIM_FACTORY.createIdentifiable(extrinsicObject));

        Metacard identityMetacard = getRegistryMetacardFromRegistryPackage(registryPackage);
        if (identityMetacard != null) {
            identityMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE,
                    true));
            identityMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE,
                    true));

            addRegistryEntry(identityMetacard);
        }
    }

    private void validateRegistryMetacards(List<Metacard> metacards)
            throws FederationAdminException {

        if (metacards == null) {
            throw new FederationAdminException(
                    "Error creating local registry entry. Null metacard provided.");
        }
        for (Metacard metacard : metacards) {
            if (metacard == null) {
                throw new FederationAdminException("ValidationError: Metacard was null.");
            }
            Attribute registryId = metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID);
            if (registryId == null) {
                throw new FederationAdminException(
                        "ValidationError: Metacard does not have a registry id.");
            }

            Set<String> tags = metacard.getTags();
            if (!tags.contains(RegistryConstants.REGISTRY_TAG)) {
                throw new FederationAdminException(
                        "ValidationError: Metacard does not have a registry tag.");
            }
        }
    }

    private Metacard getRegistryIdentityMetacard() throws FederationAdminException {
        Metacard identityMetacard = null;

        List<Filter> filters = getBasicFilter();
        filters.add(filterBuilder.attribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE)
                .is()
                .bool(true));
        Filter filter = filterBuilder.allOf(filters);

        List<Metacard> identityMetacards = getRegistryMetacardsByFilter(filter);
        if (CollectionUtils.isNotEmpty(identityMetacards)) {

            if (identityMetacards.size() > 1) {
                String message =
                        "Error getting registry identity metacard. More than one result found.";
                LOGGER.error("{} Found these: {}", message, identityMetacards);
                throw new FederationAdminException(message);
            }

            identityMetacard = identityMetacards.get(0);
        }

        return identityMetacard;
    }

    private List<Metacard> getRegistryMetacardsByFilter(Filter filter)
            throws FederationAdminException {
        return getRegistryMetacardsByFilter(filter, null);
    }

    private List<Metacard> getRegistryMetacardsByFilter(Filter filter, List<String> sourceIds)
            throws FederationAdminException {
        if (filter == null) {
            throw new FederationAdminException(
                    "Error getting registry metacards. Null filter provided.");
        }
        List<Metacard> registryMetacards = new ArrayList<>();
        PropertyName propertyName = new PropertyNameImpl(Metacard.MODIFIED);
        SortBy sortBy = new SortByImpl(propertyName, SortOrder.ASCENDING);
        QueryImpl query = new QueryImpl(filter);
        query.setSortBy(sortBy);
        query.setPageSize(PAGE_SIZE);
        QueryRequest queryRequest = new QueryRequestImpl(query, sourceIds);

        try {
            QueryResponse queryResponse =
                    security.runWithSubjectOrElevate(() -> catalogFramework.query(queryRequest));
            List<Result> results = queryResponse.getResults();
            registryMetacards = results.stream()
                    .map(Result::getMetacard)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (SecurityServiceException | InvocationTargetException e) {
            String message = "Error querying for registry metacards.";
            LOGGER.error("{} For Filter: {}", message, filter);
            throw new FederationAdminException(message, e);
        }
        return registryMetacards;
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

    private RegistryPackageType getRegistryPackageFromString(String xml)
            throws FederationAdminException {
        if (StringUtils.isBlank(xml)) {
            throw new FederationAdminException(
                    "Error unmarshalling xml string to RegistryPackage. Empty string was provided.");
        }

        RegistryPackageType registryPackage = null;

        try {
            JAXBElement<RegistryPackageType> jaxbRegistryPackage = parser.unmarshal(
                    unmarshalConfigurator,
                    JAXBElement.class,
                    IOUtils.toInputStream(xml));
            if (jaxbRegistryPackage != null && jaxbRegistryPackage.getValue() != null) {
                registryPackage = jaxbRegistryPackage.getValue();
            }
        } catch (ParserException e) {
            String message = "Error getting local registry objects. Couldn't unmarshal the string.";
            LOGGER.error("{} String: {}", message, xml);
            throw new FederationAdminException(message, e);
        }

        return registryPackage;
    }

    private Metacard getRegistryMetacardFromString(String xml) throws FederationAdminException {
        if (StringUtils.isBlank(xml)) {
            throw new FederationAdminException(
                    "Error unmarshalling string to Metacard. Empty string was provided.");
        }
        Metacard metacard;

        try {
            metacard = registryTransformer.transform(IOUtils.toInputStream(xml));
        } catch (IOException | CatalogTransformerException e) {
            String message = "Error transforming xml string to metacard.";
            LOGGER.error("{}. XML: {}", message, xml);
            throw new FederationAdminException(message);
        }

        return metacard;
    }

    BundleContext getBundleContext() {
        Bundle bundle = FrameworkUtil.getBundle(this.getClass());

        if (bundle != null) {
            return bundle.getBundleContext();
        }

        return null;
    }

    private List<Filter> getBasicFilter() {
        List<Filter> filters = new ArrayList<>();
        filters.add(filterBuilder.attribute(Metacard.CONTENT_TYPE)
                .is()
                .equalTo()
                .text(RegistryConstants.REGISTRY_NODE_METACARD_TYPE_NAME));
        filters.add(filterBuilder.attribute(Metacard.TAGS)
                .is()
                .equalTo()
                .text(RegistryConstants.REGISTRY_TAG));
        return filters;
    }

    public void setCatalogFramework(CatalogFramework catalogFramework) {
        this.catalogFramework = catalogFramework;
    }

    public void setFilterBuilder(FilterBuilder filterBuilder) {
        this.filterBuilder = filterBuilder;
    }

    public void setParser(Parser parser) {
        List<String> contextPath = Arrays.asList(RegistryObjectType.class.getPackage()
                        .getName(),
                EbrimConstants.OGC_FACTORY.getClass()
                        .getPackage()
                        .getName(),
                EbrimConstants.GML_FACTORY.getClass()
                        .getPackage()
                        .getName());

        ClassLoader classLoader = this.getClass()
                .getClassLoader();

        this.marshalConfigurator = parser.configureParser(contextPath, classLoader);
        this.marshalConfigurator.addProperty(Marshaller.JAXB_FRAGMENT, true);

        this.unmarshalConfigurator = parser.configureParser(contextPath, classLoader);

        this.parser = parser;
    }

    public void setRegistryTransformer(InputTransformer inputTransformer) {
        this.registryTransformer = inputTransformer;
    }
}
