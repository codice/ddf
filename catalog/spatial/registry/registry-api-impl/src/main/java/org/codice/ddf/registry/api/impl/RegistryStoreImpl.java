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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.api.internal.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.registry.schemabindings.helper.MetacardMarshaller;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.source.AbstractCswStore;
import org.opengis.filter.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.converters.Converter;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.delegate.TagsFilterDelegate;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.encryption.EncryptionService;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

public class RegistryStoreImpl extends AbstractCswStore implements RegistryStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryStoreImpl.class);

    private static final String PUSH_ALLOWED_PROPERTY = "pushAllowed";

    private static final String PULL_ALLOWED_PROPERTY = "pullAllowed";

    private static final String REMOTE_NAME = "remoteName";

    private static final String AUTO_PUSH = "autoPush";

    private boolean pushAllowed = true;

    private boolean pullAllowed = true;

    private boolean autoPush = true;

    private String registryId = "";

    private String remoteName = "";

    private MetacardMarshaller metacardMarshaller;

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
        map.put(AUTO_PUSH, value -> setAutoPush((Boolean) value));
        map.put(REMOTE_NAME, value -> setRemoteName((String) value));
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
    public boolean isAutoPush() {
        return autoPush;
    }

    @Override
    public CreateResponse create(CreateRequest request) throws IngestException {

        if (request.getMetacards()
                .stream()
                .map(RegistryUtility::getRegistryId)
                .anyMatch(Objects::isNull)) {
            throw new IngestException("One or more of the metacards is not a registry metacard");
        }

        List<Filter> regIdFilters = request.getMetacards()
                .stream()
                .map(e -> filterBuilder.attribute(RegistryObjectMetacardType.REMOTE_METACARD_ID)
                        .is()
                        .equalTo()
                        .text(e.getId()))
                .collect(Collectors.toList());
        Filter tagFilter = filterBuilder.attribute(Metacard.TAGS)
                .is()
                .equalTo()
                .text(RegistryConstants.REGISTRY_TAG_INTERNAL);
        QueryImpl query = new QueryImpl(filterBuilder.allOf(tagFilter,
                filterBuilder.attribute(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE)
                        .empty(),
                filterBuilder.anyOf(regIdFilters)));
        QueryRequest queryRequest = new QueryRequestImpl(query);
        try {
            SourceResponse queryResponse = super.query(queryRequest);

            Map<String, Metacard> responseMap = queryResponse.getResults()
                    .stream()
                    .collect(Collectors.toMap(e -> RegistryUtility.getRegistryId(e.getMetacard()),
                            Result::getMetacard));
            List<Metacard> metacardsToCreate = request.getMetacards()
                    .stream()
                    .filter(e -> !responseMap.containsKey(RegistryUtility.getRegistryId(e)))
                    .collect(Collectors.toList());
            List<Metacard> allMetacards = new ArrayList<>(responseMap.values());
            if (CollectionUtils.isNotEmpty(metacardsToCreate)) {
                CreateResponse createResponse =
                        super.create(new CreateRequestImpl(metacardsToCreate));
                allMetacards.addAll(createResponse.getCreatedMetacards());
            }
            return new CreateResponseImpl(request, request.getProperties(), allMetacards);
        } catch (UnsupportedQueryException e) {
            LOGGER.warn(
                    "Unable to perform pre-create remote query. Proceeding with original query. Error was {}",
                    e.getMessage());
        }
        return super.create(request);
    }

    @Override
    public UpdateResponse update(UpdateRequest request) throws IngestException {

        if (request.getUpdates()
                .stream()
                .map(e -> RegistryUtility.getRegistryId(e.getValue()))
                .anyMatch(Objects::isNull)) {
            throw new IngestException("One or more of the metacards is not a registry metacard");
        }

        Map<String, Metacard> updatedMetacards = request.getUpdates()
                .stream()
                .collect(Collectors.toMap(e -> RegistryUtility.getRegistryId(e.getValue()),
                        Map.Entry::getValue));

        Map<String, Metacard> origMetacards = ((OperationTransaction) request.getPropertyValue(
                Constants.OPERATION_TRANSACTION_KEY)).getPreviousStateMetacards()
                .stream()
                .collect(Collectors.toMap(RegistryUtility::getRegistryId, e -> e));

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
    public DeleteResponse delete(DeleteRequest request) throws IngestException {
        List<String> ids =
                ((OperationTransaction) request.getPropertyValue(Constants.OPERATION_TRANSACTION_KEY)).getPreviousStateMetacards()
                        .stream()
                        .map(Metacard::getId)
                        .collect(Collectors.toList());
        DeleteRequest newRequest = new DeleteRequestImpl(ids.toArray(new String[ids.size()]),
                request.getProperties());

        return super.delete(newRequest);
    }

    @Override
    public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {

        //This is a registry store so only allow registry requests through
        if (!filterAdapter.adapt(request.getQuery(),
                new TagsFilterDelegate(ImmutableSet.of(RegistryConstants.REGISTRY_TAG,
                        RegistryConstants.REGISTRY_TAG_INTERNAL), true))) {
            return new SourceResponseImpl(request, Collections.emptyList());
        }

        SourceResponse registryQueryResponse = super.query(request);
        for (Result singleResult : registryQueryResponse.getResults()) {
            if (registryId.equals(RegistryUtility.getRegistryId(singleResult.getMetacard()))) {
                String metacardTitle = singleResult.getMetacard()
                        .getTitle();
                if (metacardTitle != null && !remoteName.equals(metacardTitle)) {
                    remoteName = metacardTitle;
                    updateConfiguration();
                }
                break;
            }
        }
        return registryQueryResponse;
    }

    public void setAutoPush(boolean autoPush) {
        this.autoPush = autoPush;
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

    private void setMetacardExtID(Metacard metacard, String newId) throws ParserException {

        RegistryPackageType registryPackage = metacardMarshaller.getRegistryPackageFromMetacard(
                metacard);

        List<ExternalIdentifierType> currentExtIdList = registryPackage.getExternalIdentifier();
        currentExtIdList.stream()
                .filter(extId -> extId.getId()
                        .equals(RegistryConstants.REGISTRY_MCARD_ID_LOCAL))
                .findFirst()
                .ifPresent(extId -> extId.setValue(newId));

        metacardMarshaller.setMetacardRegistryPackage(metacard, registryPackage);

    }

    public void setMetacardMarshaller(MetacardMarshaller metacardMarshaller) {
        this.metacardMarshaller = metacardMarshaller;
    }

    public void init() {

        SourceMonitor registrySourceMonitor = new SourceMonitor() {
            @Override
            public void setAvailable() {
                try {
                    registryInfoQuery();
                } catch (UnsupportedQueryException e) {
                    LOGGER.debug("Unable to query registry configurations, ", e);
                }
            }

            @Override
            public void setUnavailable() {
            }
        };

        addSourceMonitor(registrySourceMonitor);
        super.init();
    }

    void registryInfoQuery() throws UnsupportedQueryException {
        List<Filter> filters = new ArrayList<>();
        filters.add(filterBuilder.attribute(Metacard.TAGS)
                .is()
                .equalTo()
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
            registryId = RegistryUtility.getRegistryId(identityMetacard.getResults()
                    .get(0)
                    .getMetacard());
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
            LOGGER.debug("Unable to update registry configurations, ", e);
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
