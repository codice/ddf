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
package org.codice.ddf.registry.identification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.codice.ddf.parser.ParserException;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.registry.schemabindings.helper.MetacardMarshaller;

import ddf.catalog.Constants;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.util.impl.Requests;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

/**
 * IdentificationPlugin is a Pre/PostIngestPlugin that assigns a localID when a metacard is added to the
 * catalog and an originID to a registry metacard during creation. It also ensures that duplicate
 * registry-ids are not added to the catalog.
 */
public class IdentificationPlugin implements PreIngestPlugin {

    private MetacardMarshaller metacardMarshaller;

    private RegistryIdPostIngestPlugin registryIdPostIngestPlugin;

    private UuidGenerator uuidGenerator;

    public IdentificationPlugin(UuidGenerator uuidGenerator) {
        this.uuidGenerator = uuidGenerator;
    }

    /**
     * For registry metacards updates the tags and identifiers
     *
     * @param input the {@link CreateRequest} to process
     * @return
     * @throws PluginExecutionException
     * @throws StopProcessingException
     */
    @Override
    public CreateRequest process(CreateRequest input)
            throws PluginExecutionException, StopProcessingException {
        if (!Requests.isLocal(input)) {
            return input;
        }

        for (Metacard metacard : input.getMetacards()) {
            if (RegistryUtility.isRegistryMetacard(metacard)) {
                if (registryIdPostIngestPlugin.getLocalRegistryIds()
                        .contains(RegistryUtility.getRegistryId(metacard))) {
                    throw new StopProcessingException(
                            "Can't create duplicate local node registry entries.");
                }

                if (!RegistryUtility.hasAttribute(metacard,
                        RegistryObjectMetacardType.REMOTE_REGISTRY_ID)
                        && registryIdPostIngestPlugin.getRegistryIds()
                        .contains(RegistryUtility.getRegistryId(metacard))) {
                    throw new StopProcessingException(
                            "Can't create duplicate registry entries");
                }

                if (registryIdPostIngestPlugin.getRemoteMetacardIds()
                        .contains(RegistryUtility.getStringAttribute(metacard,
                                RegistryObjectMetacardType.REMOTE_METACARD_ID,
                                ""))) {
                    throw new StopProcessingException("Can't create duplicate registry entries.");
                }
                metacard.setAttribute(new AttributeImpl(Metacard.ID, uuidGenerator.generateUuid()));
                updateTags(metacard);
                updateIdentifiers(metacard, true);

            }
        }

        return input;
    }

    /**
     * For registry metacards verifies the update should take place by checking that the update
     * metacard is at least as up to date as the existing one. Also updates the tags, identifiers,
     * and transient attributes of the updated metacard.
     *
     * @param input the {@link UpdateRequest} to process
     * @return
     * @throws PluginExecutionException
     * @throws StopProcessingException
     */
    @Override
    public UpdateRequest process(UpdateRequest input)
            throws PluginExecutionException, StopProcessingException {
        if (!Requests.isLocal(input)) {
            return input;
        }

        OperationTransaction operationTransaction = (OperationTransaction) input.getProperties()
                .get(Constants.OPERATION_TRANSACTION_KEY);

        List<Metacard> previousMetacards = operationTransaction.getPreviousStateMetacards();

        Map<String, Metacard> previousMetacardsMap = previousMetacards.stream()
                .filter(e -> RegistryUtility.isRegistryMetacard(e)
                        || RegistryUtility.isInternalRegistryMetacard(e))
                .collect(Collectors.toMap(RegistryUtility::getRegistryId, Function.identity()));

        List<Map.Entry<Serializable, Metacard>> entriesToRemove = new ArrayList<>();

        List<Map.Entry<Serializable, Metacard>> registryUpdates = input.getUpdates()
                .stream()
                .filter(e -> RegistryUtility.isRegistryMetacard(e.getValue()))
                .collect(Collectors.toList());

        for (Map.Entry<Serializable, Metacard> entry : registryUpdates) {
            Metacard updateMetacard = entry.getValue();
            Metacard existingMetacard = previousMetacardsMap.get(RegistryUtility.getRegistryId(
                    updateMetacard));

            if (existingMetacard == null) {
                continue;
            }

            if (updateMetacard.getMetadata() != null && !updateMetacard.getModifiedDate()
                    .before(existingMetacard.getModifiedDate())) {

                updateMetacard.setAttribute(new AttributeImpl(Metacard.ID,
                        existingMetacard.getId()));
                copyTransientAttributes(updateMetacard, existingMetacard);
                updateTags(updateMetacard);
                if (isInternal(updateMetacard)) {
                    updateMetacard.setAttribute(existingMetacard.getAttribute(
                            RegistryObjectMetacardType.REMOTE_METACARD_ID));
                    updateMetacard.setAttribute(existingMetacard.getAttribute(
                            RegistryObjectMetacardType.REMOTE_REGISTRY_ID));
                }
                updateIdentifiers(updateMetacard, false);
            } else {
                entriesToRemove.add(entry);
            }

        }

        input.getUpdates()
                .removeAll(entriesToRemove);

        return input;
    }

    @Override
    public DeleteRequest process(DeleteRequest input)
            throws PluginExecutionException, StopProcessingException {
        return input;
    }

    private void updateTags(Metacard metacard) {
        if (RegistryUtility.hasAttribute(metacard, RegistryObjectMetacardType.REMOTE_REGISTRY_ID)
                && !RegistryUtility.isLocalNode(metacard)
                && !RegistryUtility.isInternalRegistryMetacard(metacard)) {
            List<Serializable> tags = new ArrayList<>();
            tags.addAll(metacard.getTags());
            tags.remove(RegistryConstants.REGISTRY_TAG);
            tags.add(RegistryConstants.REGISTRY_TAG_INTERNAL);
            metacard.setAttribute(new AttributeImpl(Metacard.TAGS, tags));
        }
    }

    private void copyTransientAttributes(Metacard updateMetacard, Metacard existingMetacard) {
        for (String transientAttributeKey : RegistryObjectMetacardType.TRANSIENT_ATTRIBUTES) {
            Attribute transientAttribute = updateMetacard.getAttribute(transientAttributeKey);
            if (transientAttribute == null) {
                transientAttribute = existingMetacard.getAttribute(transientAttributeKey);
                if (transientAttribute != null) {
                    updateMetacard.setAttribute(transientAttribute);
                }
            }
        }
    }

    private void updateIdentifiers(Metacard metacard, boolean create)
            throws StopProcessingException {

        boolean extOriginFound = false;
        boolean extRegIdFound = false;
        String metacardID = metacard.getId();
        String registryID = RegistryUtility.getRegistryId(metacard);
        String systemRegId = System.getProperty(RegistryConstants.REGISTRY_ID_PROPERTY);

        try {
            RegistryPackageType registryPackage = metacardMarshaller.getRegistryPackageFromMetacard(
                    metacard);

            List<ExternalIdentifierType> extIdList = new ArrayList<>();

            //check if external ids are already present
            if (registryPackage.isSetExternalIdentifier()) {
                List<ExternalIdentifierType> currentExtIdList =
                        registryPackage.getExternalIdentifier();

                for (ExternalIdentifierType extId : currentExtIdList) {
                    extId.setRegistryObject(registryID);
                    if (extId.getId()
                            .equals(RegistryConstants.REGISTRY_MCARD_ID_LOCAL)) {
                        if (isInternal(metacard) && create) {
                            metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REMOTE_METACARD_ID,
                                    extId.getValue()));
                        }
                        extId.setValue(metacardID);
                    } else if (extId.getId()
                            .equals(RegistryConstants.REGISTRY_MCARD_ID_ORIGIN)) {
                        extOriginFound = true;
                    } else if (extId.getId()
                            .equals(RegistryConstants.REGISTRY_ID_ORIGIN)) {
                        if (!systemRegId.equals(extId.getValue()) && isInternal(metacard)) {
                            metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REMOTE_REGISTRY_ID,
                                    extId.getValue()));
                        }
                        extId.setValue(systemRegId);
                        extRegIdFound = true;
                    }
                    extIdList.add(extId);
                }

                if (!extOriginFound) {
                    extIdList.add(createExternalIdentifier(RegistryConstants.REGISTRY_MCARD_ID_ORIGIN,
                            registryID,
                            RegistryConstants.REGISTRY_METACARD_ID_CLASS,
                            metacardID));
                }

                if (!extRegIdFound) {
                    extIdList.add(createExternalIdentifier(RegistryConstants.REGISTRY_ID_ORIGIN,
                            registryID,
                            RegistryConstants.REGISTRY_ID_CLASS,
                            systemRegId));
                }

            } else {
                extIdList.add(createExternalIdentifier(RegistryConstants.REGISTRY_MCARD_ID_LOCAL,
                        registryID,
                        RegistryConstants.REGISTRY_METACARD_ID_CLASS,
                        metacardID));
                extIdList.add(createExternalIdentifier(RegistryConstants.REGISTRY_MCARD_ID_ORIGIN,
                        registryID,
                        RegistryConstants.REGISTRY_METACARD_ID_CLASS,
                        metacardID));
                extIdList.add(createExternalIdentifier(RegistryConstants.REGISTRY_ID_ORIGIN,
                        registryID,
                        RegistryConstants.REGISTRY_ID_CLASS,
                        systemRegId));
            }

            registryPackage.setExternalIdentifier(extIdList);
            metacardMarshaller.setMetacardRegistryPackage(metacard, registryPackage);

        } catch (ParserException e) {
            throw new StopProcessingException(
                    "Unable to access Registry Metadata. Parser exception caught");
        }
    }

    private ExternalIdentifierType createExternalIdentifier(String id, String objId, String schema,
            String value) {
        ExternalIdentifierType extId = new ExternalIdentifierType();
        extId.setId(id);
        extId.setRegistryObject(objId);
        extId.setIdentificationScheme(schema);
        extId.setValue(value);
        return extId;
    }

    private boolean isInternal(Metacard metacard) {
        return RegistryUtility.isInternalRegistryMetacard(metacard);
    }

    public void setMetacardMarshaller(MetacardMarshaller helper) {
        this.metacardMarshaller = helper;
    }

    public void setRegistryIdPostIngestPlugin(
            RegistryIdPostIngestPlugin registryIdPostIngestPlugin) {
        this.registryIdPostIngestPlugin = registryIdPostIngestPlugin;
    }
}
