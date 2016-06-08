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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.schemabindings.EbrimConstants;
import org.codice.ddf.security.common.Security;
import org.opengis.filter.Filter;

import com.google.common.base.Charsets;

import ddf.catalog.CatalogFramework;
import ddf.catalog.Constants;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.Requests;
import ddf.security.SecurityConstants;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;

/**
 * IdentificationPlugin is a Pre/PostIngestPlugin that assigns a localID when a metacard is added to the
 * catalog and an originID to a registry metacard during creation. It also ensures that duplicate
 * registry-ids are not added to the catalog.
 */
public class IdentificationPlugin implements PreIngestPlugin, PostIngestPlugin {

    private Parser parser;

    private CatalogFramework catalogFramework;

    private FilterBuilder filterBuilder;

    private ParserConfigurator marshalConfigurator;

    private ParserConfigurator unmarshalConfigurator;

    private Set<String> registryIds = ConcurrentHashMap.newKeySet();

    @Override
    public CreateRequest process(CreateRequest input)
            throws PluginExecutionException, StopProcessingException {
        if (Requests.isLocal(input)) {
            for (Metacard metacard : input.getMetacards()) {
                if (metacard.getTags()
                        .contains(RegistryConstants.REGISTRY_TAG)) {
                    if (registryIds.contains(getRegistryId(metacard))) {
                        throw new StopProcessingException(String.format(
                                "Duplication error. Can not create metacard with registry-id %s since it already exists",
                                getRegistryId(metacard)));
                    }
                    setMetacardExtID(metacard);
                }
            }
        }
        return input;
    }

    @Override
    public UpdateRequest process(UpdateRequest input)
            throws PluginExecutionException, StopProcessingException {

        OperationTransaction operationTransaction = (OperationTransaction) input.getProperties()
                .get(Constants.OPERATION_TRANSACTION_KEY);

        if (operationTransaction == null) {
            throw new PluginExecutionException(
                    "Unable to get OperationTransaction from UpdateRequest");
        }

        if (Requests.isLocal(input)) {
            List<Metacard> previousMetacards = operationTransaction.getPreviousStateMetacards();

            Map<String, Metacard> previousMetacardsMap = previousMetacards.stream()
                    .collect(Collectors.toMap(Metacard::getId, Function.identity()));

            ArrayList<Map.Entry<Serializable, Metacard>> entriesToRemove = new ArrayList<>();

            for (Map.Entry<Serializable, Metacard> entry : input.getUpdates()) {
                Metacard updateMetacard = entry.getValue();
                Metacard existingMetacard = previousMetacardsMap.get(updateMetacard.getId());

                if (existingMetacard != null) {
                    if (existingMetacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                            .getValue()
                            .equals(updateMetacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                                    .getValue())) {

                        if (!updateMetacard.getModifiedDate()
                                .before(existingMetacard.getModifiedDate())) {
                            for (String transientAttributeKey : RegistryObjectMetacardType.TRANSIENT_ATTRIBUTES) {
                                Attribute transientAttribute = updateMetacard.getAttribute(
                                        transientAttributeKey);
                                if (transientAttribute == null) {
                                    transientAttribute = existingMetacard.getAttribute(
                                            transientAttributeKey);
                                    if (transientAttribute != null) {
                                        updateMetacard.setAttribute(transientAttribute);
                                    }
                                }
                            }
                        } else {
                            entriesToRemove.add(entry);
                        }
                    }
                }
            }

            input.getUpdates()
                    .removeAll(entriesToRemove);

        }
        return input;
    }

    @Override
    public DeleteRequest process(DeleteRequest input)
            throws PluginExecutionException, StopProcessingException {
        return input;
    }

    @Override
    public CreateResponse process(CreateResponse input) throws PluginExecutionException {
        if (Requests.isLocal(input.getRequest())) {
            registryIds.addAll(input.getCreatedMetacards()
                    .stream()
                    .filter(metacard -> metacard.getTags()
                            .contains(RegistryConstants.REGISTRY_TAG))
                    .map(this::getRegistryId)
                    .collect(Collectors.toList()));
        }
        return input;
    }

    @Override
    public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
        return input;
    }

    @Override
    public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
        if (Requests.isLocal(input.getRequest())) {
            for (Metacard metacard : input.getDeletedMetacards()) {
                if (metacard.getTags()
                        .contains(RegistryConstants.REGISTRY_TAG)) {
                    registryIds.remove(getRegistryId(metacard));
                }
            }
        }
        return input;
    }

    private void setMetacardExtID(Metacard metacard) throws StopProcessingException {

        boolean extOriginFound = false;
        String metacardID = metacard.getId();
        String metadata = metacard.getMetadata();
        String registryID = getRegistryId(metacard);

        InputStream inputStream = new ByteArrayInputStream(metadata.getBytes(Charsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            JAXBElement<RegistryObjectType> registryObjectTypeJAXBElement = parser.unmarshal(
                    unmarshalConfigurator,
                    JAXBElement.class,
                    inputStream);

            if (registryObjectTypeJAXBElement != null) {
                RegistryObjectType registryObjectType = registryObjectTypeJAXBElement.getValue();

                if (registryObjectType != null) {

                    List<ExternalIdentifierType> extIdList = new ArrayList<>();

                    //check if external ids are already present
                    if (registryObjectType.isSetExternalIdentifier()) {
                        List<ExternalIdentifierType> currentExtIdList =
                                registryObjectType.getExternalIdentifier();

                        for (ExternalIdentifierType extId : currentExtIdList) {
                            extId.setRegistryObject(registryID);
                            if (extId.getId()
                                    .equals(RegistryConstants.REGISTRY_MCARD_LOCAL_ID)) {
                                //update local id
                                extId.setValue(metacardID);
                            } else if (extId.getId()
                                    .equals(RegistryConstants.REGISTRY_MCARD_ORIGIN_ID)) {
                                extOriginFound = true;
                            }
                            extIdList.add(extId);
                        }

                        if (!extOriginFound) {
                            ExternalIdentifierType originExtId = new ExternalIdentifierType();
                            originExtId.setId(RegistryConstants.REGISTRY_MCARD_ORIGIN_ID);
                            originExtId.setRegistryObject(registryID);
                            originExtId.setIdentificationScheme(RegistryConstants.REGISTRY_METACARD_ID_CLASS);
                            originExtId.setValue(metacardID);

                            extIdList.add(originExtId);
                        }

                    } else {
                        //create both ids
                        extIdList = new ArrayList<>(2);

                        ExternalIdentifierType localExtId = new ExternalIdentifierType();
                        localExtId.setId(RegistryConstants.REGISTRY_MCARD_LOCAL_ID);
                        localExtId.setRegistryObject(registryID);
                        localExtId.setIdentificationScheme(RegistryConstants.REGISTRY_METACARD_ID_CLASS);
                        localExtId.setValue(metacardID);

                        ExternalIdentifierType originExtId = new ExternalIdentifierType();
                        originExtId.setId(RegistryConstants.REGISTRY_MCARD_ORIGIN_ID);
                        originExtId.setRegistryObject(registryID);
                        originExtId.setIdentificationScheme(RegistryConstants.REGISTRY_METACARD_ID_CLASS);
                        originExtId.setValue(metacardID);

                        extIdList.add(localExtId);
                        extIdList.add(originExtId);

                    }

                    registryObjectType.setExternalIdentifier(extIdList);
                    registryObjectTypeJAXBElement.setValue(registryObjectType);
                    parser.marshal(marshalConfigurator,
                            registryObjectTypeJAXBElement,
                            outputStream);

                    metacard.setAttribute(new AttributeImpl(Metacard.METADATA,
                            new String(outputStream.toByteArray(), Charsets.UTF_8)));
                }
            }

        } catch (ParserException e) {
            throw new StopProcessingException(
                    "Unable to access Registry Metadata. Parser exception caught");
        }
    }

    private String getRegistryId(Metacard mcard) {
        return mcard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                .getValue()
                .toString();
    }

    public void init()
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            PluginExecutionException {

        List<Metacard> registryMetacards;
        Filter registryFilter = filterBuilder.attribute(Metacard.TAGS)
                .is()
                .like()
                .text(RegistryConstants.REGISTRY_TAG);
        QueryImpl query = new QueryImpl(registryFilter);
        query.setPageSize(1000);
        QueryRequest request = new QueryRequestImpl(query);

        request.getProperties()
                .put(SecurityConstants.SECURITY_SUBJECT,
                        Security.runAsAdmin(() -> Security.getInstance()
                                .getSystemSubject()));

        QueryResponse response = catalogFramework.query(request);

        if (response == null) {
            throw new PluginExecutionException(
                    "Plugin failed to initialize plugin, unable to query identity node.");
        }

        registryMetacards = response.getResults()
                .stream()
                .map(Result::getMetacard)
                .collect(Collectors.toList());
        registryIds.addAll(registryMetacards.stream()
                .map(this::getRegistryId)
                .collect(Collectors.toList()));

    }

    public void setFilterBuilder(FilterBuilder filterBuilder) {
        this.filterBuilder = filterBuilder;
    }

    public void setCatalogFramework(CatalogFramework framework) {
        this.catalogFramework = framework;
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
        this.unmarshalConfigurator = parser.configureParser(contextPath, classLoader);
        this.marshalConfigurator = parser.configureParser(contextPath, classLoader);
        this.marshalConfigurator.addProperty(Marshaller.JAXB_FRAGMENT, true);
        this.parser = parser;
    }
}
