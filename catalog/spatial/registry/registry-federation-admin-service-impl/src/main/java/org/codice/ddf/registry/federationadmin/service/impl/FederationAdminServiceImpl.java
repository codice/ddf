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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConstants;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.security.common.Security;
import org.geotools.filter.FilterFactoryImpl;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.InternationalStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.LocalizedStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ObjectFactory;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ValueListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.VersionInfoType;

public class FederationAdminServiceImpl implements FederationAdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FederationAdminServiceImpl.class);

    private static final FilterFactory FILTER_FACTORY = new FilterFactoryImpl();

    private static final ObjectFactory RIM_FACTORY = new ObjectFactory();

    private CatalogFramework catalogFramework;

    private InputTransformer registryTransformer;

    private Parser parser;

    private ParserConfigurator configurator;

    private FilterBuilder filterBuilder;

    private final Security security;

    public FederationAdminServiceImpl() {
        this(Security.getInstance());
    }

    FederationAdminServiceImpl(Security security) {
        this.security = security;
    }

    @Override
    public void addLocalEntry(Metacard metacard)
            throws SourceUnavailableException, IngestException {
        Subject systemSubject = security.getSystemSubject();
        CreateRequest createRequest = new CreateRequestImpl(metacard);
        createRequest.getProperties()
                .put(SecurityConstants.SECURITY_SUBJECT, systemSubject);

        catalogFramework.create(createRequest);
    }

    @Override
    public List<Metacard> getRegistryMetacards()
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        List<Metacard> registryMetacards = new ArrayList<>();

        Filter filter =
                FILTER_FACTORY.and(FILTER_FACTORY.like(FILTER_FACTORY.property(Metacard.CONTENT_TYPE),
                        RegistryConstants.REGISTRY_NODE_METACARD_TYPE_NAME),
                        FILTER_FACTORY.like(FILTER_FACTORY.property(Metacard.TAGS),
                                RegistryConstants.REGISTRY_TAG));
        SortBy sortBy = FILTER_FACTORY.sort(Metacard.CREATED, SortOrder.ASCENDING);

        Query query = new QueryImpl(filter);
        ((QueryImpl) query).setSortBy(sortBy);

        Subject systemSubject = security.getSystemSubject();
        QueryRequest queryRequest = new QueryRequestImpl(query);
        queryRequest.getProperties()
                .put(SecurityConstants.SECURITY_SUBJECT, systemSubject);

        QueryResponse queryResponse = catalogFramework.query(queryRequest);

        List<Result> results = queryResponse.getResults();

        for (Result result : results) {
            Metacard metacard = result.getMetacard();
            if (metacard != null) {
                registryMetacards.add(metacard);
            }
        }

        return registryMetacards;
    }

    private Metacard getRegistryIdentityMetacard()
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        Metacard identityMetacard = null;

        List<Filter> filters = new ArrayList<>();
        filters.add(FILTER_FACTORY.like(FILTER_FACTORY.property(Metacard.CONTENT_TYPE),
                RegistryConstants.REGISTRY_NODE_METACARD_TYPE_NAME));
        filters.add(FILTER_FACTORY.like(FILTER_FACTORY.property(Metacard.TAGS),
                RegistryConstants.REGISTRY_TAG));
        filters.add(filterBuilder.attribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE)
                .is()
                .bool(true));

        Filter filter = FILTER_FACTORY.and(filters);
        SortBy sortBy = FILTER_FACTORY.sort(Metacard.CREATED, SortOrder.ASCENDING);

        Query query = new QueryImpl(filter);
        ((QueryImpl) query).setSortBy(sortBy);

        Subject systemSubject = security.getSystemSubject();
        QueryRequest queryRequest = new QueryRequestImpl(query);
        queryRequest.getProperties()
                .put(SecurityConstants.SECURITY_SUBJECT, systemSubject);

        QueryResponse queryResponse = catalogFramework.query(queryRequest);

        List<Result> results = queryResponse.getResults();
        if (CollectionUtils.isNotEmpty(results)) {
            identityMetacard = results.get(0)
                    .getMetacard();

            if (results.size() > 1) {
                LOGGER.error("There should only be one identity node. Found these: {}", results);
            }
        }

        return identityMetacard;
    }

    public void init() {
        try {
            if (getRegistryIdentityMetacard() == null) {
                createIdentityNode();
            }
        } catch (SourceUnavailableException | UnsupportedQueryException | FederationException e) {
            LOGGER.error("There was an error executing a query. Failed to come up properly.");
        } catch (IngestException e) {
            LOGGER.error(
                    "There was an error creating the identity node. Failed to come up properly.");
        }
    }

    private void createIdentityNode() throws SourceUnavailableException, IngestException {

        String registryPackageId = UUID.randomUUID()
                .toString()
                .replaceAll("-", "");
        RegistryPackageType registryPackage = RIM_FACTORY.createRegistryPackageType();
        registryPackage.setId(registryPackageId);
        registryPackage.setObjectType(RegistryConstants.REGISTRY_NODE_OBJECT_TYPE);

        ExtrinsicObjectType extrinsicObject = RIM_FACTORY.createExtrinsicObjectType();
        extrinsicObject.setObjectType(RegistryConstants.REGISTRY_NODE_OBJECT_TYPE);

        String extrinsicObjectId = UUID.randomUUID()
                .toString()
                .replaceAll("-", "");
        extrinsicObject.setId(extrinsicObjectId);

        String siteName = SystemInfo.getSiteName();
        if (StringUtils.isNotBlank(siteName)) {
            extrinsicObject.setName(getInternationalStringTypeFromString(siteName));
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
        ValueListType valueList = RIM_FACTORY.createValueListType();
        valueList.getValue()
                .add(rightNow);

        SlotType1 lastUpdated = RIM_FACTORY.createSlotType1();
        lastUpdated.setValueList(RIM_FACTORY.createValueList(valueList));
        lastUpdated.setSlotType(DatatypeConstants.DATETIME.toString());
        lastUpdated.setName(RegistryConstants.XML_LAST_UPDATED_NAME);
        extrinsicObject.getSlot()
                .add(lastUpdated);

        SlotType1 liveDate = RIM_FACTORY.createSlotType1();
        liveDate.setValueList(RIM_FACTORY.createValueList(valueList));
        liveDate.setSlotType(DatatypeConstants.DATETIME.toString());
        liveDate.setName(RegistryConstants.XML_LIVE_DATE_NAME);
        extrinsicObject.getSlot()
                .add(liveDate);

        if (registryPackage.getRegistryObjectList() == null) {
            registryPackage.setRegistryObjectList(RIM_FACTORY.createRegistryObjectListType());
        }

        registryPackage.getRegistryObjectList()
                .getIdentifiable()
                .add(RIM_FACTORY.createIdentifiable(extrinsicObject));

        Metacard identityMetacard = jaxbToMetacard(registryPackage);
        if (identityMetacard != null) {
            Attribute registryNodeAttribute =
                    new AttributeImpl(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, true);
            identityMetacard.setAttribute(registryNodeAttribute);

            addLocalEntry(identityMetacard);
        }
    }

    private Metacard jaxbToMetacard(RegistryPackageType registryPackage) {
        Metacard metacard;

        try {
            JAXBElement<RegistryPackageType> jaxbRegistryObjectType =
                    RIM_FACTORY.createRegistryPackage(registryPackage);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            parser.marshal(configurator, jaxbRegistryObjectType, baos);
            InputStream xmlInputStream = new ByteArrayInputStream(baos.toByteArray());
            metacard = registryTransformer.transform(xmlInputStream);

        } catch (IOException | CatalogTransformerException | ParserException e) {
            LOGGER.error("Error creating metacard from registry package.", e);
            metacard = null;
        }

        return metacard;
    }

    private InternationalStringType getInternationalStringTypeFromString(
            String internationalizeThis) {
        InternationalStringType ist = RIM_FACTORY.createInternationalStringType();
        LocalizedStringType lst = RIM_FACTORY.createLocalizedStringType();
        lst.setValue(internationalizeThis);
        ist.setLocalizedString(Collections.singletonList(lst));

        return ist;
    }

    public void setCatalogFramework(CatalogFramework catalogFramework) {
        this.catalogFramework = catalogFramework;
    }

    public void setFilterBuilder(FilterBuilder filterBuilder) {
        this.filterBuilder = filterBuilder;
    }

    public void setParser(Parser parser) {
        this.configurator =
                parser.configureParser(Arrays.asList(RegistryObjectType.class.getPackage()
                                .getName(),
                        net.opengis.ogc.ObjectFactory.class.getPackage()
                                .getName(),
                        net.opengis.gml.v_3_1_1.ObjectFactory.class.getPackage()
                                .getName()), FederationAdminServiceImpl.class.getClassLoader());
        this.configurator.addProperty(Marshaller.JAXB_FRAGMENT, true);
        this.parser = parser;
    }

    public void setRegistryTransformer(InputTransformer inputTransformer) {
        this.registryTransformer = inputTransformer;
    }
}
