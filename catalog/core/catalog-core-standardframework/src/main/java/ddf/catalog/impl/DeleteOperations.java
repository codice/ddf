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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.operation.DeleteStorageRequest;
import ddf.catalog.content.operation.impl.DeleteStorageRequestImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.history.Historian;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.OperationTransactionImpl;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.InternalIngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.util.impl.Requests;

public class DeleteOperations {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteOperations.class);

    private static final Logger INGEST_LOGGER =
            LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

    private static final String PRE_INGEST_ERROR =
            "Error during pre-ingest:\n\n";

    private Supplier<CatalogProvider> catalogSupplier;

    private Supplier<StorageProvider> storageSupplier;

    // Inject properties
    private FrameworkProperties frameworkProperties;

    private QueryOperations queryOperations;

    private SourceOperations sourceOperations;

    private OperationsSecuritySupport opsSecuritySupport;

    private OperationsMetacardSupport opsMetacardSupport;

    private OperationsCrudSupport opsCrudSupport;

    private Historian historian;

    public DeleteOperations(FrameworkProperties frameworkProperties,
            QueryOperations queryOperations, SourceOperations sourceOperations,
            OperationsSecuritySupport opsSecuritySupport,
            OperationsMetacardSupport opsMetacardSupport, OperationsCrudSupport opsCrudSupport) {
        this.frameworkProperties = frameworkProperties;
        this.queryOperations = queryOperations;
        this.sourceOperations = sourceOperations;
        this.opsSecuritySupport = opsSecuritySupport;
        this.opsMetacardSupport = opsMetacardSupport;
        this.opsCrudSupport = opsCrudSupport;
    }

    void setCatalogSupplier(Supplier<CatalogProvider> catalogSupplier) {
        this.catalogSupplier = catalogSupplier;
    }

    void setStorageSupplier(Supplier<StorageProvider> storageSupplier) {
        this.storageSupplier = storageSupplier;
    }

    public void setHistorian(Historian historian) {
        this.historian = historian;
    }

    //
    // Delegate methods
    //
    DeleteResponse delete(DeleteRequest deleteRequest)
            throws IngestException, SourceUnavailableException {
        boolean catalogStoreRequest = opsCrudSupport.isCatalogStoreRequest(deleteRequest);
        queryOperations.setFlagsOnRequest(deleteRequest);

        validateDeleteRequest(deleteRequest);

        if (Requests.isLocal(deleteRequest) && (
                !sourceOperations.isSourceAvailable(catalogSupplier.get())
                        || !opsCrudSupport.isStorageAvailable(storageSupplier.get()))) {
            throw new SourceUnavailableException(
                    "Local provider is not available, cannot perform delete operation.");
        }

        DeleteStorageRequest deleteStorageRequest = null;

        DeleteResponse deleteResponse = null;
        try {
            List<Filter> idFilters = new ArrayList<>(deleteRequest.getAttributeValues().size());
            for (Serializable serializable : deleteRequest.getAttributeValues()) {
                idFilters.add(frameworkProperties.getFilterBuilder()
                        .attribute(deleteRequest.getAttributeName())
                        .is()
                        .equalTo()
                        .text(serializable.toString()));
            }

            QueryImpl queryImpl = new QueryImpl(opsCrudSupport.getFilterWithAdditionalFilters(
                    idFilters));
            queryImpl.setStartIndex(1);
            queryImpl.setPageSize(deleteRequest.getAttributeValues()
                    .size());
            QueryRequestImpl queryRequest = new QueryRequestImpl(queryImpl,
                    deleteRequest.getStoreIds());

            QueryResponse query;
            List<Metacard> metacards = new ArrayList<>(deleteRequest.getAttributeValues()
                    .size());
            if (!frameworkProperties.getPolicyPlugins()
                    .isEmpty()) {
                try {
                    query = queryOperations.doQuery(queryRequest,
                            frameworkProperties.getFederationStrategy(),
                            false);
                    metacards.addAll(query.getResults()
                            .stream()
                            .map(Result::getMetacard)
                            .collect(Collectors.toList()));
                } catch (FederationException e) {
                    LOGGER.warn("Unable to complete query for updated metacards.", e);
                    throw new IngestException("Exception during runtime while performing delete");
                }

                if (metacards.size() < deleteRequest.getAttributeValues()
                        .size()) {
                    throw new StopProcessingException(
                            "Unable to remove all metacards contained in request.");
                }
            }

            deleteStorageRequest = new DeleteStorageRequestImpl(metacards,
                    deleteRequest.getProperties());

            HashMap<String, Set<String>> requestPolicyMap = new HashMap<>();
            Map<String, Serializable> unmodifiableProperties = Collections.unmodifiableMap(
                    deleteRequest.getProperties());
            for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
                PolicyResponse policyResponse = plugin.processPreDelete(metacards,
                        unmodifiableProperties);
                opsSecuritySupport.buildPolicyMap(requestPolicyMap,
                        policyResponse.operationPolicy()
                                .entrySet());
            }
            deleteRequest.getProperties()
                    .put(PolicyPlugin.OPERATION_SECURITY, requestPolicyMap);

            for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
                deleteRequest = plugin.processPreDelete(deleteRequest);
            }

            deleteRequest.getProperties()
                    .put(Constants.OPERATION_TRANSACTION_KEY,
                            new OperationTransactionImpl(OperationTransaction.OperationType.DELETE,
                                    metacards));

            for (PreIngestPlugin plugin : frameworkProperties.getPreIngest()) {
                try {
                    deleteRequest = plugin.process(deleteRequest);
                } catch (PluginExecutionException e) {
                    LOGGER.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }
            validateDeleteRequest(deleteRequest);

            // Call the Provider delete method
            LOGGER.debug("Calling catalog.delete() with {} entries.",
                    deleteRequest.getAttributeValues()
                            .size());

            if (Requests.isLocal(deleteRequest)) {
                try {
                    storageSupplier.get()
                            .delete(deleteStorageRequest);
                } catch (StorageException e) {
                    LOGGER.error(
                            "Unable to delete stored content items. Not removing stored metacards",
                            e);
                    throw new InternalIngestException(
                            "Unable to delete stored content items. Not removing stored metacards.",
                            e);
                }
                deleteResponse = catalogSupplier.get()
                        .delete(deleteRequest);
                deleteResponse = injectAttributes(deleteResponse);
                historian.version(deleteResponse);
            }

            if (catalogStoreRequest) {
                DeleteResponse remoteDeleteResponse = doRemoteDelete(deleteRequest);
                if (deleteResponse == null) {
                    deleteResponse = remoteDeleteResponse;
                    deleteResponse = injectAttributes(deleteResponse);
                } else {
                    deleteResponse.getProperties()
                            .putAll(remoteDeleteResponse.getProperties());
                    deleteResponse.getProcessingErrors()
                            .addAll(remoteDeleteResponse.getProcessingErrors());
                }
            }

            HashMap<String, Set<String>> responsePolicyMap = new HashMap<>();
            unmodifiableProperties = Collections.unmodifiableMap(deleteRequest.getProperties());
            if (deleteResponse != null && deleteResponse.getDeletedMetacards() != null) {
                for (Metacard metacard : deleteResponse.getDeletedMetacards()) {
                    HashMap<String, Set<String>> itemPolicyMap = new HashMap<>();
                    for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
                        PolicyResponse policyResponse = plugin.processPostDelete(metacard,
                                unmodifiableProperties);
                        opsSecuritySupport.buildPolicyMap(itemPolicyMap,
                                policyResponse.itemPolicy()
                                        .entrySet());
                        opsSecuritySupport.buildPolicyMap(responsePolicyMap,
                                policyResponse.operationPolicy()
                                        .entrySet());
                    }
                    metacard.setAttribute(new AttributeImpl(Metacard.SECURITY, itemPolicyMap));
                }
            }
            deleteRequest.getProperties()
                    .put(PolicyPlugin.OPERATION_SECURITY, responsePolicyMap);

            for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
                deleteResponse = plugin.processPostDelete(deleteResponse);
            }

            // Post results to be available for pubsub
            deleteResponse = validateFixDeleteResponse(deleteResponse, deleteRequest);
            for (final PostIngestPlugin plugin : frameworkProperties.getPostIngest()) {
                try {
                    deleteResponse = plugin.process(deleteResponse);
                } catch (PluginExecutionException e) {
                    LOGGER.info("Plugin exception", e);
                }
            }

        } catch (StopProcessingException see) {
            LOGGER.warn(PRE_INGEST_ERROR + see.getMessage(), see);
            throw new IngestException(PRE_INGEST_ERROR + see.getMessage());

        } catch (RuntimeException re) {
            LOGGER.warn("Exception during runtime while performing delete", re);
            throw new InternalIngestException("Exception during runtime while performing delete");

        } finally {
            if (deleteStorageRequest != null) {
                try {
                    storageSupplier.get()
                            .commit(deleteStorageRequest);
                } catch (StorageException e) {
                    LOGGER.error("Unable to remove stored content items.", e);
                }
            }
        }

        return deleteResponse;
    }

    //
    // Private helper methods
    //

    private DeleteResponse injectAttributes(DeleteResponse response) {
        List<Metacard> deletedMetacards = response.getDeletedMetacards()
                .stream()
                .map((original) -> opsMetacardSupport.applyInjectors(original,
                        frameworkProperties.getAttributeInjectors()))
                .collect(Collectors.toList());

        return new DeleteResponseImpl(response.getRequest(),
                response.getProperties(),
                deletedMetacards,
                response.getProcessingErrors());
    }

    /**
     * Validates that the {@link DeleteRequest} is non-null, has a non-empty list of
     * {@link Metacard}s in it, and a non-null attribute name (which specifies if the delete is
     * being done by product URI or ID).
     *
     * @param deleteRequest the {@link DeleteRequest}
     * @throws IngestException if the {@link DeleteRequest} is null, or has null or empty {@link Metacard} list,
     *                         or a null attribute name
     */
    private void validateDeleteRequest(DeleteRequest deleteRequest) throws IngestException {
        if (deleteRequest == null) {
            throw new IngestException(
                    "DeleteRequest was null, either passed in from endpoint, or as output from PreIngestPlugins");
        }
        List<?> entries = deleteRequest.getAttributeValues();
        if (CollectionUtils.isEmpty(entries) || deleteRequest.getAttributeName() == null) {
            throw new IngestException(
                    "Cannot perform delete with null/empty attribute value list or null attributeName, "
                            + "either passed in from endpoint, or as output from PreIngestPlugins");
        }
    }

    private DeleteResponse doRemoteDelete(DeleteRequest deleteRequest) {
        HashSet<ProcessingDetails> exceptions = new HashSet<>();
        Map<String, Serializable> properties = new HashMap<>();

        List<CatalogStore> stores = opsCrudSupport.getCatalogStoresForRequest(deleteRequest,
                exceptions);

        List<Metacard> metacards = new ArrayList<>();
        for (CatalogStore store : stores) {
            try {
                if (!store.isAvailable()) {
                    exceptions.add(new ProcessingDetailsImpl(store.getId(),
                            null,
                            "CatalogStore is not available"));
                } else {
                    DeleteResponse response = store.delete(deleteRequest);
                    properties.put(store.getId(), new ArrayList<>(response.getDeletedMetacards()));
                    metacards = response.getDeletedMetacards();
                }
            } catch (IngestException e) {
                INGEST_LOGGER.error("Error deleting metacards for CatalogStore {}",
                        store.getId(),
                        e);
                exceptions.add(new ProcessingDetailsImpl(store.getId(), e));
            }
        }

        return new DeleteResponseImpl(deleteRequest, properties, metacards, exceptions);
    }

    /**
     * Validates that the {@link DeleteResponse} has one or more {@link Metacard}s in it that were
     * deleted in the catalog, and that the original {@link DeleteRequest} is included in the
     * response.
     *
     * @param deleteResponse the original {@link DeleteResponse} returned from the catalog provider
     * @param deleteRequest  the original {@link DeleteRequest} sent to the catalog provider
     * @return the updated {@link DeleteResponse}
     * @throws IngestException if original {@link DeleteResponse} passed in is null or the {@link Metacard}s
     *                         list in the response is null
     */
    private DeleteResponse validateFixDeleteResponse(DeleteResponse deleteResponse,
            DeleteRequest deleteRequest) throws IngestException {
        DeleteResponse delResponse = deleteResponse;
        if (delResponse != null) {
            if (delResponse.getDeletedMetacards() == null) {
                throw new IngestException(
                        "CatalogProvider returned null list of results from delete method.");
            }
            if (delResponse.getRequest() == null) {
                delResponse = new DeleteResponseImpl(deleteRequest,
                        delResponse.getProperties(),
                        delResponse.getDeletedMetacards());
            }
        } else {
            throw new IngestException("CatalogProvider returned null DeleteResponse Object.");
        }
        return delResponse;
    }
}
