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
package ddf.catalog.impl.operations;

import static ddf.catalog.Constants.CONTENT_PATHS;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import ddf.catalog.Constants;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.operation.impl.UpdateStorageRequestImpl;
import ddf.catalog.content.plugin.PostUpdateStoragePlugin;
import ddf.catalog.content.plugin.PreUpdateStoragePlugin;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.history.Historian;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.operation.Operation;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.OperationTransactionImpl;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PreAuthorizationPlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.InternalIngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.util.impl.Requests;
import ddf.security.SecurityConstants;
import ddf.security.Subject;

/**
 * Support class for update delegate operations for the {@code CatalogFrameworkImpl}.
 * <p>
 * This class contains two delegated update methods and methods to support them. No
 * operations/support methods should be added to this class except in support of CFI
 * update operations.
 */
public class UpdateOperations {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateOperations.class);

    private static final Logger INGEST_LOGGER =
            LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

    private static final String PRE_INGEST_ERROR = "Error during pre-ingest:\n\n";

    // Inject properties
    private final FrameworkProperties frameworkProperties;

    private final QueryOperations queryOperations;

    private final SourceOperations sourceOperations;

    private final OperationsSecuritySupport opsSecuritySupport;

    private final OperationsMetacardSupport opsMetacardSupport;

    private final OperationsCatalogStoreSupport opsCatStoreSupport;

    private final OperationsStorageSupport opsStorageSupport;

    private Historian historian;

    public UpdateOperations(FrameworkProperties frameworkProperties,
            QueryOperations queryOperations, SourceOperations sourceOperations,
            OperationsSecuritySupport opsSecuritySupport,
            OperationsMetacardSupport opsMetacardSupport,
            OperationsCatalogStoreSupport opsCatStoreSupport,
            OperationsStorageSupport opsStorageSupport) {
        this.frameworkProperties = frameworkProperties;
        this.queryOperations = queryOperations;
        this.sourceOperations = sourceOperations;
        this.opsSecuritySupport = opsSecuritySupport;
        this.opsMetacardSupport = opsMetacardSupport;
        this.opsCatStoreSupport = opsCatStoreSupport;
        this.opsStorageSupport = opsStorageSupport;
    }

    public void setHistorian(Historian historian) {
        this.historian = historian;
    }

    //
    // Delegate methods
    //
    public UpdateResponse update(UpdateRequest updateRequest)
            throws IngestException, SourceUnavailableException {
        updateRequest = queryOperations.setFlagsOnRequest(updateRequest);
        updateRequest = validateUpdateRequest(updateRequest);
        updateRequest = validateLocalSource(updateRequest);

        try {
            updateRequest = injectAttributes(updateRequest);
            updateRequest = setDefaultValues(updateRequest);

            updateRequest = populateMetacards(updateRequest);
            updateRequest = processPreAuthorizationPlugins(updateRequest);

            updateRequest = populateUpdateRequestPolicyMap(updateRequest);
            updateRequest = processPreUpdateAccessPlugins(updateRequest);

            updateRequest = processPreIngestPlugins(updateRequest);
            updateRequest = validateUpdateRequest(updateRequest);

            // Call the update on the catalog
            LOGGER.debug("Calling catalog.update() with {} updates.",
                    updateRequest.getUpdates()
                            .size());

            UpdateResponse updateResponse = performLocalUpdate(updateRequest);
            updateResponse = performRemoteUpdate(updateRequest, updateResponse);

            // Handle the posting of messages to pubsub
            updateResponse = validateFixUpdateResponse(updateResponse, updateRequest);
            updateResponse = processPostIngestPlugins(updateResponse);

            return updateResponse;
        } catch (StopProcessingException see) {
            throw new IngestException(PRE_INGEST_ERROR, see);
        } catch (RuntimeException re) {
            throw new InternalIngestException("Exception during runtime while performing update",
                    re);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Metacard> getUpdateMap(UpdateRequest updateRequest) {
        return (Map<String, Metacard>) Optional.of(updateRequest)
                .map(Operation::getProperties)
                .map(p -> p.get(Constants.ATTRIBUTE_UPDATE_MAP_KEY))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .orElseGet(HashMap::new);
    }

    public UpdateResponse update(UpdateStorageRequest streamUpdateRequest)
            throws IngestException, SourceUnavailableException {
        Map<String, Metacard> metacardMap = new HashMap<>();
        List<ContentItem> contentItems = new ArrayList<>(streamUpdateRequest.getContentItems()
                .size());
        HashMap<String, Path> tmpContentPaths = new HashMap<>(streamUpdateRequest.getContentItems()
                .size());
        UpdateResponse updateResponse = null;
        UpdateStorageRequest updateStorageRequest = null;
        UpdateStorageResponse updateStorageResponse;

        streamUpdateRequest = opsStorageSupport.prepareStorageRequest(streamUpdateRequest,
                streamUpdateRequest::getContentItems);

        // Operation populates the metacardMap, contentItems, and tmpContentPaths
        opsMetacardSupport.generateMetacardAndContentItems(streamUpdateRequest.getContentItems(),
                (Subject) streamUpdateRequest.getPropertyValue(SecurityConstants.SECURITY_SUBJECT),
                metacardMap,
                contentItems,
                tmpContentPaths);

        streamUpdateRequest.getProperties()
                .put(CONTENT_PATHS, tmpContentPaths);

        streamUpdateRequest = applyAttributeOverrides(streamUpdateRequest, metacardMap);

        try {
            if (!contentItems.isEmpty()) {
                updateStorageRequest = new UpdateStorageRequestImpl(contentItems,
                        streamUpdateRequest.getId(),
                        streamUpdateRequest.getProperties());
                updateStorageRequest = processPreUpdateStoragePlugins(updateStorageRequest);

                try {
                    updateStorageResponse = sourceOperations.getStorage()
                            .update(updateStorageRequest);
                    updateStorageResponse.getProperties()
                            .put(CONTENT_PATHS, tmpContentPaths);
                    updateStorageResponse = historian.version(streamUpdateRequest, updateStorageResponse);
                } catch (StorageException e) {
                    throw new IngestException(
                            "Could not store content items. Removed created metacards.",
                            e);
                }

                updateStorageResponse = processPostUpdateStoragePlugins(updateStorageResponse);

                for (ContentItem contentItem : updateStorageResponse.getUpdatedContentItems()) {
                    Metacard metacard = metacardMap.get(contentItem.getId());

                    Metacard overrideMetacard = contentItem.getMetacard();

                    Metacard updatedMetacard = OverrideAttributesSupport.overrideMetacard(metacard,
                            overrideMetacard,
                            true,
                            true);

                    updatedMetacard.setAttribute(new AttributeImpl(Metacard.RESOURCE_SIZE,
                            String.valueOf(contentItem.getSize())));

                    metacardMap.put(contentItem.getId(), updatedMetacard);
                }
            }

            UpdateRequestImpl updateRequest =
                    new UpdateRequestImpl(Iterables.toArray(metacardMap.values()
                            .stream()
                            .map(Metacard::getId)
                            .collect(Collectors.toList()), String.class),
                            new ArrayList<>(metacardMap.values()));
            updateRequest.setProperties(streamUpdateRequest.getProperties());
            updateResponse = update(updateRequest);
        } catch (Exception e) {
            if (updateStorageRequest != null) {
                try {
                    sourceOperations.getStorage()
                            .rollback(updateStorageRequest);
                } catch (StorageException e1) {
                    LOGGER.info("Unable to remove temporary content for id: {}",
                            updateStorageRequest.getId(),
                            e1);
                }
            }
            throw new IngestException(
                    "Unable to store products for request: " + streamUpdateRequest.getId(), e);

        } finally {
            opsStorageSupport.commitAndCleanup(updateStorageRequest, tmpContentPaths);
        }

        return updateResponse;
    }

    //
    // Private helper methods
    //

    private UpdateRequest rewriteRequestToAvoidHistoryConflicts(UpdateRequest updateRequest,
            QueryResponse response) {
        final String attributeName = updateRequest.getAttributeName();
        if (Metacard.ID.equals(attributeName)) {
            return updateRequest;
        }

        List<Map.Entry<Serializable, Metacard>> updatedList = response.getResults()
                .stream()
                .map(Result::getMetacard)
                .map(this::toEntryById)
                .collect(Collectors.toList());

        return new UpdateRequestImpl(updatedList,
                Metacard.ID,
                updateRequest.getProperties(),
                updateRequest.getStoreIds());
    }

    private boolean foundAllUpdateRequestMetacards(UpdateRequest updateRequest,
            QueryResponse response) {
        Set<String> originalKeys = updateRequest.getUpdates()
                .stream()
                .map(Map.Entry::getKey)
                .map(Object::toString)
                .collect(Collectors.toSet());
        Set<String> responseKeys = response.getResults()
                .stream()
                .map(Result::getMetacard)
                .map(m -> m.getAttribute(updateRequest.getAttributeName()))
                .filter(Objects::nonNull)
                .map(Attribute::getValue)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toSet());
        return originalKeys.equals(responseKeys);
    }

    private Map.Entry<Serializable, Metacard> toEntryById(Metacard metacard) {
        return new AbstractMap.SimpleEntry<>(metacard.getId(), metacard);
    }

    private UpdateRequest injectAttributes(UpdateRequest request) {
        request.getUpdates()
                .forEach(updateEntry -> {
                    Metacard original = updateEntry.getValue();
                    Metacard metacard = opsMetacardSupport.applyInjectors(original,
                            frameworkProperties.getAttributeInjectors());
                    updateEntry.setValue(metacard);
                });

        return request;
    }

    private UpdateRequest setDefaultValues(UpdateRequest updateRequest) {
        updateRequest.getUpdates()
                .stream()
                .filter(Objects::nonNull)
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .forEach(opsMetacardSupport::setDefaultValues);

        return updateRequest;
    }

    /**
     * Validates that the {@link UpdateRequest} is non-null, has a non-empty list of
     * {@link Metacard}s in it, and a non-null attribute name (which specifies if the update is
     * being done by product URI or ID).
     *
     * @param updateRequest the {@link UpdateRequest}
     * @throws IngestException if the {@link UpdateRequest} is null, or has null or empty {@link Metacard} list,
     *                         or a null attribute name.
     */
    private UpdateRequest validateUpdateRequest(UpdateRequest updateRequest)
            throws IngestException {
        if (updateRequest == null) {
            throw new IngestException(
                    "UpdateRequest was null, either passed in from endpoint, or as output from PreIngestPlugins");
        }
        List<Map.Entry<Serializable, Metacard>> entries = updateRequest.getUpdates();
        if (CollectionUtils.isEmpty(entries) || updateRequest.getAttributeName() == null) {
            throw new IngestException(
                    "Cannot perform update with null/empty attribute value list or null attributeName, "
                            + "either passed in from endpoint, or as output from PreIngestPlugins");
        }

        return updateRequest;
    }

    private UpdateResponse doRemoteUpdate(UpdateRequest updateRequest) {
        HashSet<ProcessingDetails> exceptions = new HashSet<>();
        Map<String, Serializable> properties = new HashMap<>();

        List<CatalogStore> stores = opsCatStoreSupport.getCatalogStoresForRequest(updateRequest,
                exceptions);

        List<Update> updates = new ArrayList<>();

        for (CatalogStore store : stores) {
            try {
                if (!store.isAvailable()) {
                    exceptions.add(new ProcessingDetailsImpl(store.getId(),
                            null,
                            "CatalogStore is not available"));
                } else {
                    UpdateResponse response = store.update(updateRequest);
                    properties.put(store.getId(), new ArrayList<>(response.getUpdatedMetacards()));
                    updates = response.getUpdatedMetacards();
                }
            } catch (IngestException e) {
                INGEST_LOGGER.error("Error updating metacards for CatalogStore {}",
                        store.getId(),
                        e);
                exceptions.add(new ProcessingDetailsImpl(store.getId(), e));
            }
        }

        return new UpdateResponseImpl(updateRequest, properties, updates, exceptions);
    }

    /**
     * Validates that the {@link UpdateResponse} has one or more {@link Metacard}s in it that were
     * updated in the catalog, and that the original {@link UpdateRequest} is included in the
     * response.
     *
     * @param updateResponse the original {@link UpdateResponse} returned from the catalog provider
     * @param updateRequest  the original {@link UpdateRequest} sent to the catalog provider
     * @return the updated {@link UpdateResponse}
     * @throws IngestException if original {@link UpdateResponse} passed in is null or the {@link Metacard}s
     *                         list in the response is null
     */
    private UpdateResponse validateFixUpdateResponse(UpdateResponse updateResponse,
            UpdateRequest updateRequest) throws IngestException {
        UpdateResponse updateResp = updateResponse;
        if (updateResp != null) {
            if (updateResp.getUpdatedMetacards() == null) {
                throw new IngestException(
                        "CatalogProvider returned null list of results from update method.");
            }
            if (updateResp.getRequest() == null) {
                updateResp = new UpdateResponseImpl(updateRequest,
                        updateResponse.getProperties(),
                        updateResponse.getUpdatedMetacards());
            }
        } else {
            throw new IngestException("CatalogProvider returned null UpdateResponse Object.");
        }
        return updateResp;
    }

    private UpdateResponse processPostIngestPlugins(UpdateResponse updateResponse) {
        for (final PostIngestPlugin plugin : frameworkProperties.getPostIngest()) {
            try {
                updateResponse = plugin.process(updateResponse);
            } catch (PluginExecutionException e) {
                LOGGER.info("Plugin exception", e);
            }
        }
        return updateResponse;
    }

    private UpdateResponse performRemoteUpdate(UpdateRequest updateRequest,
            UpdateResponse updateResponse) {
        if (opsCatStoreSupport.isCatalogStoreRequest(updateRequest)) {
            UpdateResponse remoteUpdateResponse = doRemoteUpdate(updateRequest);
            if (updateResponse == null) {
                updateResponse = remoteUpdateResponse;
            } else {
                updateResponse.getProperties()
                        .putAll(remoteUpdateResponse.getProperties());
                updateResponse.getProcessingErrors()
                        .addAll(remoteUpdateResponse.getProcessingErrors());
            }
        }

        return updateResponse;
    }

    private UpdateResponse performLocalUpdate(UpdateRequest updateRequest)
            throws IngestException, SourceUnavailableException {
        if (!Requests.isLocal(updateRequest)) {
            return null;
        }

        UpdateResponse updateResponse = sourceOperations.getCatalog()
                .update(updateRequest);
        updateResponse = historian.version(updateResponse);
        return updateResponse;
    }

    private UpdateRequest processPreIngestPlugins(UpdateRequest updateRequest)
            throws StopProcessingException {
        for (PreIngestPlugin plugin : frameworkProperties.getPreIngest()) {
            try {
                updateRequest = plugin.process(updateRequest);
            } catch (PluginExecutionException e) {
                LOGGER.debug("error processing update in PreIngestPlugin", e);
            }
        }
        return updateRequest;
    }

    private UpdateRequest processPreUpdateAccessPlugins(UpdateRequest updateRequest)
            throws StopProcessingException {
        Map<String, Metacard> metacardMap = getUpdateMap(updateRequest);
        for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
            updateRequest = plugin.processPreUpdate(updateRequest, metacardMap);
        }
        return updateRequest;
    }

    private UpdateRequest populateUpdateRequestPolicyMap(UpdateRequest updateRequest)
            throws StopProcessingException {
        Map<String, Metacard> metacardMap = getUpdateMap(updateRequest);
        HashMap<String, Set<String>> requestPolicyMap = new HashMap<>();
        for (Map.Entry<Serializable, Metacard> update : updateRequest.getUpdates()) {
            HashMap<String, Set<String>> itemPolicyMap = new HashMap<>();
            HashMap<String, Set<String>> oldItemPolicyMap = new HashMap<>();
            Metacard oldMetacard = metacardMap.get(update.getKey()
                    .toString());

            for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
                PolicyResponse updatePolicyResponse = plugin.processPreUpdate(update.getValue(),
                        Collections.unmodifiableMap(updateRequest.getProperties()));
                PolicyResponse oldPolicyResponse = plugin.processPreUpdate(oldMetacard,
                        Collections.unmodifiableMap(updateRequest.getProperties()));

                opsSecuritySupport.buildPolicyMap(itemPolicyMap,
                        updatePolicyResponse.itemPolicy()
                                .entrySet());
                opsSecuritySupport.buildPolicyMap(oldItemPolicyMap,
                        oldPolicyResponse.itemPolicy()
                                .entrySet());
                opsSecuritySupport.buildPolicyMap(requestPolicyMap,
                        updatePolicyResponse.operationPolicy()
                                .entrySet());
            }
            update.getValue()
                    .setAttribute(new AttributeImpl(Metacard.SECURITY, itemPolicyMap));
            if (oldMetacard != null) {
                oldMetacard.setAttribute(new AttributeImpl(Metacard.SECURITY, oldItemPolicyMap));
            }
        }
        updateRequest.getProperties()
                .put(PolicyPlugin.OPERATION_SECURITY, requestPolicyMap);

        return updateRequest;
    }

    private UpdateRequest populateMetacards(UpdateRequest updateRequest) throws IngestException {
        final String attributeName = updateRequest.getAttributeName();

        QueryRequestImpl queryRequest = createQueryRequest(updateRequest);
        QueryResponse query;
        try {
            query = queryOperations.doQuery(queryRequest,
                    frameworkProperties.getFederationStrategy());
        } catch (FederationException e) {
            LOGGER.debug("Unable to complete query for updated metacards.", e);
            throw new IngestException("Exception during runtime while performing update");
        }

        if (!foundAllUpdateRequestMetacards(updateRequest, query)) {
            logFailedQueryInfo(updateRequest, query);
            throw new IngestException("Could not find all metacards specified in request");
        }

        updateRequest = rewriteRequestToAvoidHistoryConflicts(updateRequest, query);

        HashMap<String, Metacard> metacardMap = new HashMap<>(query.getResults()
                .stream()
                .map(Result::getMetacard)
                .collect(Collectors.toMap(metacard -> getAttributeStringValue(metacard,
                        attributeName), Function.identity())));
        updateRequest.getProperties()
                .put(Constants.ATTRIBUTE_UPDATE_MAP_KEY, metacardMap);
        updateRequest.getProperties()
                .put(Constants.OPERATION_TRANSACTION_KEY,
                        new OperationTransactionImpl(OperationTransaction.OperationType.UPDATE,
                                metacardMap.values()));
        return updateRequest;
    }

    private UpdateRequest processPreAuthorizationPlugins(UpdateRequest updateRequest)
            throws StopProcessingException {
        Map<String, Metacard> metacardMap = getUpdateMap(updateRequest);
        for (PreAuthorizationPlugin plugin : frameworkProperties.getPreAuthorizationPlugins()) {
            updateRequest = plugin.processPreUpdate(updateRequest, metacardMap);
        }
        return updateRequest;
    }

    private QueryRequestImpl createQueryRequest(UpdateRequest updateRequest) {
        List<Filter> idFilters = updateRequest.getUpdates()
                .stream()
                .map(update -> frameworkProperties.getFilterBuilder()
                        .attribute(updateRequest.getAttributeName())
                        .is()
                        .equalTo()
                        .text(update.getKey()
                                .toString()))
                .collect(Collectors.toList());

        QueryImpl queryImpl =
                new QueryImpl(queryOperations.getFilterWithAdditionalFilters(idFilters),
                        1,  /* start index */
                        0,  /* page size */
                        null,
                        false, /* total result count */
                        0   /* timeout */);
        return new QueryRequestImpl(queryImpl, updateRequest.getStoreIds());
    }

    private UpdateRequest validateLocalSource(UpdateRequest updateRequest)
            throws SourceUnavailableException {
        if (Requests.isLocal(updateRequest)
                && !sourceOperations.isSourceAvailable(sourceOperations.getCatalog())) {
            throw new SourceUnavailableException(
                    "Local provider is not available, cannot perform update operation.");
        }

        return updateRequest;
    }

    private UpdateStorageResponse processPostUpdateStoragePlugins(
            UpdateStorageResponse updateStorageResponse) {
        for (final PostUpdateStoragePlugin plugin : frameworkProperties.getPostUpdateStoragePlugins()) {
            try {
                updateStorageResponse = plugin.process(updateStorageResponse);
            } catch (PluginExecutionException e) {
                LOGGER.debug("Plugin processing failed. This is allowable. Skipping to next plugin.",
                        e);
            }
        }
        return updateStorageResponse;
    }

    private UpdateStorageRequest processPreUpdateStoragePlugins(
            UpdateStorageRequest updateStorageRequest) {
        for (final PreUpdateStoragePlugin plugin : frameworkProperties.getPreUpdateStoragePlugins()) {
            try {
                updateStorageRequest = plugin.process(updateStorageRequest);
            } catch (PluginExecutionException e) {
                LOGGER.debug("Plugin processing failed. This is allowable. Skipping to next plugin.",
                        e);
            }
        }
        return updateStorageRequest;
    }

    private String getAttributeStringValue(Metacard mcard, String attribute) {
        return Optional.of(mcard)
                .map(m -> m.getAttribute(attribute))
                .map(Attribute::getValue)
                .map(Object::toString)
                .orElse("");
    }

    private void logFailedQueryInfo(UpdateRequest updateRequest, QueryResponse query) {
        if (LOGGER.isDebugEnabled()) {
            final String attributeName = updateRequest.getAttributeName();
            Set<String> queryResults = query.getResults()
                    .stream()
                    .map(Result::getMetacard)
                    .map(m -> m.getAttribute(attributeName))
                    .filter(Objects::nonNull)
                    .map(Attribute::getValue)
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toSet());

            LOGGER.debug(
                    "While rewriting the query, did not get a metacardId corresponding to every attribute.");
            LOGGER.debug("Original Update By attribute was: {}", attributeName);
            LOGGER.debug(
                    "Metacards unable to get Metacard ID from are: {}", updateRequest.getUpdates()
                            .stream()
                            .map(Map.Entry::getKey)
                            .map(Object::toString)
                            .filter(s -> !queryResults.contains(s))
                            .collect(Collectors.joining(", ", "[", "]")));
        }
    }

    private UpdateStorageRequest applyAttributeOverrides(UpdateStorageRequest updateStorageRequest,
            Map<String, Metacard> metacardMap) {
        OverrideAttributesSupport.overrideAttributes(updateStorageRequest.getContentItems(),
                metacardMap);
        return updateStorageRequest;
    }
}
