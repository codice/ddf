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
package ddf.catalog.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.codice.ddf.configuration.SystemInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.util.impl.DescribableImpl;
import ddf.catalog.util.impl.Masker;

/**
 * CatalogFrameworkImpl is the core class of DDF. It is used for query, create, update, delete, and
 * resource retrieval operations.
 */
@SuppressWarnings("deprecation")
public class CatalogFrameworkImpl extends DescribableImpl implements CatalogFramework {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogFrameworkImpl.class);

    private static final String FANOUT_MESSAGE =
            "Fanout proxy does not support create, update, and delete operations";

    // The local catalog provider, which is set to the first item in the {@code List} of
    // {@code CatalogProvider}s.
    // Keep this private to make sure subclasses don't use it.
    private CatalogProvider catalog;

    private StorageProvider storage;

    //
    // Injected properties
    //
    private boolean fanoutEnabled;

    private Masker masker;

    private FrameworkProperties frameworkProperties;

    private OperationsCrudSupport operationsCrudSupport;

    private CreateOperations createOperations;

    private UpdateOperations updateOperations;

    private DeleteOperations deleteOperations;

    private QueryOperations queryOperations;

    private ResourceOperations resourceOperations;

    private SourceOperations sourceOperations;

    private TransformOperations transformOperations;

    /**
     * Instantiates a new CatalogFrameworkImpl which delegates its work to surrogate operations classes.
     *
     * @param frameworkProperties   properties used to configure the framework
     * @param operationsCrudSupport support class for crud operations
     * @param createOperations      delegate that handles create operations
     * @param updateOperations      delegate that handles update operations
     * @param deleteOperations      delegate that handles delete operations
     * @param queryOperations       delegate that handles query operations
     * @param resourceOperations    delegate that handles resource operations
     * @param sourceOperations      delegate that handles source operations
     * @param transformOperations   delegate that handles transformation operations
     */
    public CatalogFrameworkImpl(FrameworkProperties frameworkProperties,
            OperationsCrudSupport operationsCrudSupport, CreateOperations createOperations,
            UpdateOperations updateOperations, DeleteOperations deleteOperations,
            QueryOperations queryOperations, ResourceOperations resourceOperations,
            SourceOperations sourceOperations, TransformOperations transformOperations) {
        this.frameworkProperties = frameworkProperties;
        this.operationsCrudSupport = operationsCrudSupport;

        this.createOperations = createOperations;
        this.updateOperations = updateOperations;
        this.deleteOperations = deleteOperations;
        this.queryOperations = queryOperations;
        this.resourceOperations = resourceOperations;
        this.sourceOperations = sourceOperations;
        this.transformOperations = transformOperations;

        setId(SystemInfo.getSiteName());
        setVersion(SystemInfo.getVersion());
        setOrganization(SystemInfo.getOrganization());
        registerBasicMetacard();

        if (this.operationsCrudSupport != null) {
            this.operationsCrudSupport.setCatalogSupplier(this::getCatalog);
            this.operationsCrudSupport.setStorageSupplier(this::getStorage);
        }
        if (this.createOperations != null) {
            this.createOperations.setCatalogSupplier(this::getCatalog);
            this.createOperations.setStorageSupplier(this::getStorage);
        }
        if (this.updateOperations != null) {
            this.updateOperations.setCatalogSupplier(this::getCatalog);
            this.updateOperations.setStorageSupplier(this::getStorage);
        }
        if (this.deleteOperations != null) {
            this.deleteOperations.setCatalogSupplier(this::getCatalog);
            this.deleteOperations.setStorageSupplier(this::getStorage);
        }
        if (this.queryOperations != null) {
            this.queryOperations.setCatalogSupplier(this::getCatalog);
        }
        if (this.resourceOperations != null) {
            this.resourceOperations.setCatalogSupplier(this::getCatalog);
        }
        if (this.sourceOperations != null) {
            this.sourceOperations.setCatalogSupplier(this::getCatalog);
        }
    }

    private void registerBasicMetacard() {
        Bundle bundle = FrameworkUtil.getBundle(CatalogFrameworkImpl.class);
        if (bundle != null && bundle.getBundleContext() != null) {
            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put("name", BasicTypes.BASIC_METACARD.getName());
            bundle.getBundleContext()
                    .registerService(MetacardType.class, BasicTypes.BASIC_METACARD, properties);
        }
    }

    /**
     * Invoked by blueprint when a {@link CatalogProvider} is created and bound to this
     * CatalogFramework instance.
     * <p/>
     * The local catalog provider will be set to the first item in the {@link java.util.List} of
     * {@link CatalogProvider}s bound to this CatalogFramework.
     *
     * @param catalogProvider the {@link CatalogProvider} being bound to this CatalogFramework instance
     */
    public void bind(CatalogProvider catalogProvider) {
        LOGGER.trace("ENTERING: bind");

        catalog = frameworkProperties.getCatalogProviders()
                .stream()
                .findFirst()
                .orElse(null);

        LOGGER.trace("EXITING: bind with catalog = {}", catalog);
    }

    /**
     * Invoked by blueprint when a {@link CatalogProvider} is deleted and unbound from this
     * CatalogFramework instance.
     * <p/>
     * The local catalog provider will be reset to the new first item in the {@link java.util.List} of
     * {@link CatalogProvider}s bound to this CatalogFramework. If this list of catalog providers is
     * currently empty, then the local catalog provider will be set to <code>null</code>.
     *
     * @param catalogProvider the {@link CatalogProvider} being unbound from this CatalogFramework instance
     */
    public void unbind(CatalogProvider catalogProvider) {
        LOGGER.trace("ENTERING: unbind");

        catalog = frameworkProperties.getCatalogProviders()
                .stream()
                .findFirst()
                .orElse(null);

        LOGGER.trace("EXITING: unbind with catalog = {}", catalog);
    }

    /**
     * Invoked by blueprint when a {@link StorageProvider} is created and bound to this
     * CatalogFramework instance.
     * <p/>
     * The local storage provider will be set to the first item in the {@link List} of
     * {@link StorageProvider}s bound to this CatalogFramework.
     *
     * @param storageProvider the {@link CatalogProvider} being bound to this CatalogFramework instance
     */
    public void bind(StorageProvider storageProvider) {
        List<StorageProvider> storageProviders = frameworkProperties.getStorageProviders();
        LOGGER.info("storage providers list size = {}", storageProviders.size());

        // The list of storage providers is sorted by OSGi service ranking, hence should
        // always set the local storage provider to the first item in the list.
        this.storage = storageProviders.get(0);
    }

    /**
     * Invoked by blueprint when a {@link StorageProvider} is deleted and unbound from this
     * CatalogFramework instance.
     * <p/>
     * The local storage provider will be reset to the new first item in the {@link List} of
     * {@link StorageProvider}s bound to this CatalogFramework. If this list of storage providers is
     * currently empty, then the local storage provider will be set to <code>null</code>.
     *
     * @param storageProvider the {@link StorageProvider} being unbound from this CatalogFramework instance
     */
    public void unbind(StorageProvider storageProvider) {
        List<StorageProvider> storageProviders = this.frameworkProperties.getStorageProviders();
        if (!storageProviders.isEmpty()) {
            LOGGER.info("storage providers list size = {}", storageProviders.size());
            LOGGER.info("Setting storage to first provider in list");

            // The list of storage providers is sorted by OSGi service ranking, hence should
            // always set the local storage provider to the first item in the list.
            this.storage = storageProviders.get(0);
        } else {
            LOGGER.info("Setting storage = NULL");
            this.storage = null;
        }
    }

    private CatalogProvider getCatalog() {
        return catalog;
    }

    private StorageProvider getStorage() {
        return storage;
    }

    public QueryOperations getQueryOperations() {
        return queryOperations;
    }

    public ResourceOperations getResourceOperations() {
        return resourceOperations;
    }

    public SourceOperations getSourceOperations() {
        return sourceOperations;
    }

    public TransformOperations getTransformOperations() {
        return transformOperations;
    }

    public void setFanoutEnabled(boolean fanoutEnabled) {
        this.fanoutEnabled = fanoutEnabled;
    }

    /**
     * Sets the {@link Masker}
     *
     * @param masker the {@link Masker} this framework will use
     */
    public void setMasker(Masker masker) {
        synchronized (this) {
            this.masker = masker;
            if (this.getId() != null) {
                masker.setId(getId());
            }
        }
    }

    /**
     * Sets the source id to identify this framework (DDF). This is also referred to as the site
     * name.
     *
     * @param sourceId the sourceId to set
     */
    @Override
    public void setId(String sourceId) {
        LOGGER.debug("Setting id = {}", sourceId);
        synchronized (this) {
            super.setId(sourceId);
            if (masker != null) {
                masker.setId(sourceId);
            }

            // Set the id of the describable delegate objects
            if (queryOperations != null) {
                queryOperations.setId(sourceId);
            }
            if (resourceOperations != null) {
                resourceOperations.setId(sourceId);
            }
            if (sourceOperations != null) {
                sourceOperations.setId(sourceId);
            }
        }
    }

    @Override
    public Set<String> getSourceIds() {
        return sourceOperations.getSourceIds(fanoutEnabled);
    }

    @Override
    public SourceInfoResponse getSourceInfo(SourceInfoRequest sourceInfoRequest)
            throws SourceUnavailableException {
        return sourceOperations.getSourceInfo(sourceInfoRequest, fanoutEnabled);
    }

    @Override
    public CreateResponse create(CreateStorageRequest createRequest)
            throws IngestException, SourceUnavailableException {
        if (fanoutEnabled) {
            throw new IngestException(FANOUT_MESSAGE);
        }

        return createOperations.create(createRequest);
    }

    @Override
    public CreateResponse create(CreateRequest createRequest)
            throws IngestException, SourceUnavailableException {
        if (fanoutEnabled) {
            throw new IngestException(FANOUT_MESSAGE);
        }

        return createOperations.create(createRequest);
    }

    @Override
    public UpdateResponse update(UpdateStorageRequest updateRequest)
            throws IngestException, SourceUnavailableException {
        if (fanoutEnabled) {
            throw new IngestException(FANOUT_MESSAGE);
        }

        return updateOperations.update(updateRequest);
    }

    @Override
    public UpdateResponse update(UpdateRequest updateRequest)
            throws IngestException, SourceUnavailableException {
        if (fanoutEnabled) {
            throw new IngestException(FANOUT_MESSAGE);
        }

        return updateOperations.update(updateRequest);
    }

    @Override
    public DeleteResponse delete(DeleteRequest deleteRequest)
            throws IngestException, SourceUnavailableException {
        if (fanoutEnabled) {
            throw new IngestException(FANOUT_MESSAGE);
        }

        return deleteOperations.delete(deleteRequest);
    }

    @Override
    public QueryResponse query(QueryRequest fedQueryRequest)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        return queryOperations.query(fedQueryRequest, fanoutEnabled);
    }

    @Override
    public QueryResponse query(QueryRequest queryRequest, FederationStrategy strategy)
            throws SourceUnavailableException, UnsupportedQueryException, FederationException {
        return queryOperations.query(queryRequest, strategy, fanoutEnabled);
    }

    @Override
    public BinaryContent transform(Metacard metacard, String transformerShortname,
            Map<String, Serializable> arguments) throws CatalogTransformerException {
        return transformOperations.transform(metacard, transformerShortname, arguments);
    }

    @Override
    public BinaryContent transform(SourceResponse response, String transformerShortname,
            Map<String, Serializable> arguments) throws CatalogTransformerException {
        return transformOperations.transform(response, transformerShortname, arguments);
    }

    @Override
    public ResourceResponse getLocalResource(ResourceRequest resourceRequest)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        return resourceOperations.getLocalResource(resourceRequest, fanoutEnabled);
    }

    @Override
    public ResourceResponse getResource(ResourceRequest resourceRequest, String resourceSiteName)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        return resourceOperations.getResource(resourceRequest, resourceSiteName, fanoutEnabled);
    }

    @Override
    public ResourceResponse getEnterpriseResource(ResourceRequest resourceRequest)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        return resourceOperations.getEnterpriseResource(resourceRequest, fanoutEnabled);
    }

    @Deprecated
    @Override
    public Map<String, Set<String>> getLocalResourceOptions(String metacardId)
            throws ResourceNotFoundException {
        return resourceOperations.getLocalResourceOptions(metacardId, fanoutEnabled);
    }

    @Deprecated
    @Override
    public Map<String, Set<String>> getEnterpriseResourceOptions(String metacardId)
            throws ResourceNotFoundException {
        return resourceOperations.getEnterpriseResourceOptions(metacardId, fanoutEnabled);
    }

    @Deprecated
    @Override
    public Map<String, Set<String>> getResourceOptions(String metacardId, String sourceId)
            throws ResourceNotFoundException {
        return resourceOperations.getResourceOptions(metacardId, sourceId, fanoutEnabled);
    }

    /**
     * String representation of this {@code CatalogFrameworkImpl}.
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
