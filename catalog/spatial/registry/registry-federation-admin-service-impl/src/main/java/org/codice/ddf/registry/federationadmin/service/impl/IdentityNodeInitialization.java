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

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.xml.datatype.DatatypeConstants;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.registry.schemabindings.helper.InternationalStringTypeHelper;
import org.codice.ddf.registry.schemabindings.helper.MetacardMarshaller;
import org.codice.ddf.registry.schemabindings.helper.SlotTypeHelper;
import org.codice.ddf.security.common.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.VersionInfoType;

/**
 * Creates a registry identity node when DDF Registry is first instantiated.
 * If a previous registry node is not already found, then a registry identity metacard and its
 * attributes are created. FederationAdminService adds the newly created identity metacard as a registry entry.
 */
public class IdentityNodeInitialization {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityNodeInitialization.class);

    private static final String UNKNOWN_SITE_NAME = "Unknown Site Name";

    private FederationAdminService federationAdminService;

    private InputTransformer registryTransformer;

    private MetacardMarshaller metacardMarshaller;

    private SlotTypeHelper slotTypeHelper = new SlotTypeHelper();

    private InternationalStringTypeHelper internationalStringTypeHelper =
            new InternationalStringTypeHelper();

    public void init() throws PrivilegedActionException {
        Security.runAsAdminWithException(() -> {
            if (!federationAdminService.getLocalRegistryIdentityMetacard()
                    .isPresent()) {
                createIdentityNode();
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
        } else {
            extrinsicObject.setName(internationalStringTypeHelper.create(UNKNOWN_SITE_NAME));
        }

        String home = SystemBaseUrl.getBaseUrl();
        extrinsicObject.setHome(home);

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

            federationAdminService.addRegistryEntry(identityMetacard);
        }
    }

    private Metacard getRegistryMetacardFromRegistryPackage(RegistryPackageType registryPackage)
            throws FederationAdminException {
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

    public void setMetacardMarshaller(MetacardMarshaller helper) {
        this.metacardMarshaller = helper;
    }

    public void setRegistryTransformer(InputTransformer inputTransformer) {
        this.registryTransformer = inputTransformer;
    }

    public void setFederationAdminService(FederationAdminService federationAdminService) {
        this.federationAdminService = federationAdminService;
    }
}
