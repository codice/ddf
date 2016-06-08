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
package org.codice.ddf.registry.api.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.api.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.schemabindings.EbrimConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.source.AbstractCswStore;
import org.opengis.filter.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.thoughtworks.xstream.converters.Converter;

import ddf.catalog.Constants;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.filter.delegate.TagsFilterDelegate;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.encryption.EncryptionService;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;

public class RegistryStoreImpl extends AbstractCswStore implements RegistryStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryStoreImpl.class);

    public static final String PUSH_ALLOWED_PROPERTY = "pushAllowed";

    public static final String PULL_ALLOWED_PROPERTY = "pullAllowed";

    public static final String REMOTE_NAME = "remoteName";

    private boolean pushAllowed = true;

    private boolean pullAllowed = true;

    private String registryId = "";

    private String remoteName = "";

    private Parser parser;

    private ParserConfigurator marshalConfigurator;

    private ParserConfigurator unmarshalConfigurator;

    private ConfigurationAdmin configAdmin;

    public RegistryStoreImpl(BundleContext context, CswSourceConfiguration cswSourceConfiguration,
            Converter provider, SecureCxfClientFactory factory,
            EncryptionService encryptionService) {
        super(context, cswSourceConfiguration, provider, factory, encryptionService);
    }

    public RegistryStoreImpl(EncryptionService encryptionService) {
        super(encryptionService);
    }

    @Override
    protected Map<String, Consumer<Object>> getAdditionalConsumers() {
        Map<String, Consumer<Object>> map = new HashMap<>();
        map.put(PUSH_ALLOWED_PROPERTY, value -> setPushAllowed((Boolean) value));
        map.put(PULL_ALLOWED_PROPERTY, value -> setPullAllowed((Boolean) value));
        map.put(RegistryObjectMetacardType.REGISTRY_ID, value -> setRegistryId((String) value));
        return map;
    }

    @Override
    public boolean isPushAllowed() {
        return pushAllowed;
    }

    @Override
    public boolean isPullAllowed() {
        return pullAllowed;
    }

    @Override
    public UpdateResponse update(UpdateRequest request) throws IngestException {

        Map<String, Metacard> updatedMetacards = request.getUpdates()
                .stream()
                .collect(Collectors.toMap(e -> getRegistryId(e.getValue()), Map.Entry::getValue));

        Map<String, Metacard> origMetacards = ((OperationTransaction) request.getPropertyValue(
                Constants.OPERATION_TRANSACTION_KEY)).getPreviousStateMetacards()
                .stream()
                .collect(Collectors.toMap(e -> getRegistryId(e), e -> e));

        //update the new metacards with the id from the orig so that they can be found on the remote system
        try {
            for (Map.Entry<String, Metacard> entry : updatedMetacards.entrySet()) {
                setMetacardExtID(entry.getValue(),
                        origMetacards.get(entry.getKey())
                                .getId());
            }
        } catch (ParserException e) {
            throw new IngestException("Could not update metacards id", e);
        }

        return super.update(request);
    }

    @Override
    public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {

        //This is a registry store so only allow registry requests through
        if (!filterAdapter.adapt(request.getQuery(),
                new TagsFilterDelegate(Collections.singleton(RegistryConstants.REGISTRY_TAG),
                        true))) {
            return new SourceResponseImpl(request, Collections.emptyList());
        }

        SourceResponse registryQueryResponse = queryResponse(request);
        for (Result singleResult : registryQueryResponse.getResults()) {
            if (singleResult.getMetacard()
                    .getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                    .getValue()
                    .toString()
                    .equals(registryId)) {
                String metacardTitle = singleResult.getMetacard().getTitle();
                if (metacardTitle != null && !remoteName.equals(metacardTitle)) {
                    remoteName = metacardTitle;
                    updateConfiguration();
                }
                break;
            }
        }
        return registryQueryResponse;
    }

    /*
    * After reviewing the various ways to test the query method above, it was decided
    * that moving the super.query method call into its own method was the least offensive option.
    * This is due to the inability to mock or spy an abstract super class method that is being
    * overwritten without resorting to... Power Mock.
    */
    SourceResponse queryResponse(QueryRequest request) throws UnsupportedQueryException {
        return super.query(request);
    }

    public void setPushAllowed(boolean pushAllowed) {
        this.pushAllowed = pushAllowed;
    }

    public void setPullAllowed(boolean pullAllowed) {
        this.pullAllowed = pullAllowed;
    }

    public void setRegistryId(String registryId) {
        this.registryId = registryId;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

    private String getRegistryId(Metacard mcard) {
        Attribute attr = mcard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID);
        if (attr != null && attr.getValue() instanceof String) {
            return (String) attr.getValue();
        }
        return null;
    }

    private void setMetacardExtID(Metacard metacard, String newId) throws ParserException {

        String metadata = metacard.getMetadata();

        InputStream inputStream = new ByteArrayInputStream(metadata.getBytes(Charsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        JAXBElement<RegistryObjectType> registryObjectTypeJAXBElement = parser.unmarshal(
                unmarshalConfigurator,
                JAXBElement.class,
                inputStream);

        if (registryObjectTypeJAXBElement != null) {
            RegistryObjectType registryObjectType = registryObjectTypeJAXBElement.getValue();

            if (registryObjectType != null) {
                List<ExternalIdentifierType> currentExtIdList =
                        registryObjectType.getExternalIdentifier();
                currentExtIdList.stream()
                        .filter(extId -> extId.getId()
                                .equals(RegistryConstants.REGISTRY_MCARD_LOCAL_ID))
                        .findFirst()
                        .ifPresent(extId -> extId.setValue(newId));

                registryObjectTypeJAXBElement.setValue(registryObjectType);
                parser.marshal(marshalConfigurator, registryObjectTypeJAXBElement, outputStream);
                metacard.setAttribute(new AttributeImpl(Metacard.METADATA,
                        new String(outputStream.toByteArray(), Charsets.UTF_8)));
            }
        }
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

    public void init() {

        SourceMonitor registrySourceMonitor = new SourceMonitor() {
            @Override
            public void setAvailable() {
                try {
                    registryInfoQuery();
                } catch (UnsupportedQueryException e) {
                    LOGGER.error("Unable to query registry configurations, ", e);
                }
            }

            @Override
            public void setUnavailable() {
            }
        };

        addSourceMonitor(registrySourceMonitor);
        super.init();
        isAvailable();
    }

    void registryInfoQuery() throws UnsupportedQueryException {
        List<Filter> filters = new ArrayList<>();
        filters.add(filterBuilder.attribute(Metacard.TAGS)
                .is()
                .like()
                .text(RegistryConstants.REGISTRY_TAG));
        filters.add(filterBuilder.not(filterBuilder.attribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE)
                .empty()));
        Filter filter = filterBuilder.allOf(filters);
        Query newQuery = new QueryImpl(filter);
        QueryRequest queryRequest = new QueryRequestImpl(newQuery);
        SourceResponse identityMetacard = query(queryRequest);
        if (identityMetacard.getResults()
                .size() > 0) {
            remoteName = identityMetacard.getResults()
                    .get(0)
                    .getMetacard()
                    .getTitle();
            registryId = identityMetacard.getResults()
                    .get(0)
                    .getMetacard()
                    .getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                    .getValue()
                    .toString();
        }
        updateConfiguration();
    }

    private void updateConfiguration() {
        String currentPid = getConfigurationPid();
        try {
            Configuration currentConfig = configAdmin.getConfiguration(currentPid);
            Dictionary<String, Object> currentProperties = currentConfig.getProperties();
            currentProperties.put(REMOTE_NAME, remoteName);
            currentProperties.put(RegistryObjectMetacardType.REGISTRY_ID, registryId);
            currentConfig.update(currentProperties);
        } catch (IOException e) {
            LOGGER.error("Unable to update registry configurations, ", e);
        }
    }

    public void setConfigAdmin(ConfigurationAdmin config) {
        this.configAdmin = config;
    }

    @Override
    public String getRegistryId() {
        return registryId;
    }

    public String getRemoteName() {
        return remoteName;
    }
}
