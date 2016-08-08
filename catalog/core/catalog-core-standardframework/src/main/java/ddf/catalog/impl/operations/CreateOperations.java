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
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.content.plugin.PostCreateStoragePlugin;
import ddf.catalog.content.plugin.PreCreateStoragePlugin;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.history.Historian;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.OperationTransactionImpl;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
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

public class CreateOperations {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateOperations.class);

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

    public CreateOperations(FrameworkProperties frameworkProperties,
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

    public void setCatalogSupplier(Supplier<CatalogProvider> catalogSupplier) {
        this.catalogSupplier = catalogSupplier;
    }

    public void setStorageSupplier(Supplier<StorageProvider> storageSupplier) {
        this.storageSupplier = storageSupplier;
    }

    public void setHistorian(Historian historian) {
        this.historian = historian;
    }

    //
    // Delegate methods
    //
    public CreateResponse create(CreateRequest createRequest)
            throws IngestException, SourceUnavailableException {
        boolean catalogStoreRequest = opsCrudSupport.isCatalogStoreRequest(createRequest);
        queryOperations.setFlagsOnRequest(createRequest);

        validateCreateRequest(createRequest);

        if (Requests.isLocal(createRequest)
                && !sourceOperations.isSourceAvailable(catalogSupplier.get())) {
            SourceUnavailableException sourceUnavailableException = new SourceUnavailableException(
                    "Local provider is not available, cannot perform create operation.");
            if (INGEST_LOGGER.isWarnEnabled()) {
                INGEST_LOGGER.warn(
                        "Error on create operation, local provider not available. {} metacards failed to ingest. {}",
                        createRequest.getMetacards()
                                .size(),
                        buildIngestLog(createRequest),
                        sourceUnavailableException);
            }
            throw sourceUnavailableException;
        }

        CreateResponse createResponse = null;

        Exception ingestError = null;
        try {
            createRequest = injectAttributes(createRequest);

            setDefaultValues(createRequest);

            Map<String, Serializable> unmodifiablePropertiesMap = Collections.unmodifiableMap(
                    createRequest.getProperties());
            HashMap<String, Set<String>> requestPolicyMap = new HashMap<>();
            for (Metacard metacard : createRequest.getMetacards()) {
                HashMap<String, Set<String>> itemPolicyMap = new HashMap<>();
                for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
                    PolicyResponse policyResponse = plugin.processPreCreate(metacard,
                            unmodifiablePropertiesMap);
                    opsSecuritySupport.buildPolicyMap(itemPolicyMap,
                            policyResponse.itemPolicy()
                                    .entrySet());
                    opsSecuritySupport.buildPolicyMap(requestPolicyMap,
                            policyResponse.operationPolicy()
                                    .entrySet());
                }

                metacard.setAttribute(new AttributeImpl(Metacard.SECURITY, itemPolicyMap));
            }
            createRequest.getProperties()
                    .put(PolicyPlugin.OPERATION_SECURITY, requestPolicyMap);

            for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
                createRequest = plugin.processPreCreate(createRequest);
            }

            createRequest.getProperties()
                    .put(Constants.OPERATION_TRANSACTION_KEY,
                            new OperationTransactionImpl(OperationTransaction.OperationType.CREATE,
                                    Collections.emptyList()));

            for (PreIngestPlugin plugin : frameworkProperties.getPreIngest()) {
                try {
                    createRequest = plugin.process(createRequest);
                } catch (PluginExecutionException e) {
                    LOGGER.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }
            validateCreateRequest(createRequest);

            // Call the create on the catalog
            LOGGER.debug("Calling catalog.create() with {} entries.",
                    createRequest.getMetacards()
                            .size());
            if (Requests.isLocal(createRequest)) {
                createResponse = catalogSupplier.get()
                        .create(createRequest);
                createResponse = historian.version(createResponse);
            }

            if (catalogStoreRequest) {
                CreateResponse remoteCreateResponse = doRemoteCreate(createRequest);
                if (createResponse == null) {
                    createResponse = remoteCreateResponse;
                } else {
                    createResponse.getProperties()
                            .putAll(remoteCreateResponse.getProperties());
                    createResponse.getProcessingErrors()
                            .addAll(remoteCreateResponse.getProcessingErrors());
                }
            }

        } catch (IngestException iee) {
            INGEST_LOGGER.warn("Ingest error", iee);
            ingestError = iee;
            throw iee;
        } catch (StopProcessingException see) {
            LOGGER.warn(PRE_INGEST_ERROR, see);
            ingestError = see;
            throw new IngestException(PRE_INGEST_ERROR + see.getMessage());
        } catch (RuntimeException re) {
            LOGGER.warn("Exception during runtime while performing create", re);
            ingestError = re;
            throw new InternalIngestException("Exception during runtime while performing create");
        } finally {
            if (ingestError != null && INGEST_LOGGER.isWarnEnabled()) {
                INGEST_LOGGER.warn("Error on create operation. {} metacards failed to ingest. {}",
                        createRequest.getMetacards()
                                .size(),
                        buildIngestLog(createRequest),
                        ingestError);
            }
        }

        try {
            createResponse = validateFixCreateResponse(createResponse, createRequest);
            for (final PostIngestPlugin plugin : frameworkProperties.getPostIngest()) {
                try {
                    createResponse = plugin.process(createResponse);
                } catch (PluginExecutionException e) {
                    LOGGER.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }
        } catch (RuntimeException re) {
            LOGGER.warn(
                    "Exception during runtime while performing doing post create operations (plugins and pubsub)",
                    re);

        }

        // if debug is enabled then catalog might take a significant performance hit w/r/t string
        // building
        if (INGEST_LOGGER.isDebugEnabled()) {
            INGEST_LOGGER.debug("{} metacards were successfully ingested. {}",
                    createRequest.getMetacards()
                            .size(),
                    buildIngestLog(createRequest));
        }
        return createResponse;
    }

    public CreateResponse create(CreateStorageRequest streamCreateRequest)
            throws IngestException, SourceUnavailableException {
        opsCrudSupport.prepareStorageRequest(streamCreateRequest,
                streamCreateRequest::getContentItems);

        Optional<String> historianTransactionKey = Optional.empty();

        Map<String, Metacard> metacardMap = new HashMap<>();
        List<ContentItem> contentItems = new ArrayList<>(streamCreateRequest.getContentItems()
                .size());
        HashMap<String, Path> tmpContentPaths = new HashMap<>(streamCreateRequest.getContentItems()
                .size());
        opsCrudSupport.generateMetacardAndContentItems(streamCreateRequest,
                streamCreateRequest.getContentItems(),
                metacardMap,
                contentItems,
                tmpContentPaths);
        streamCreateRequest.getProperties()
                .put(CONTENT_PATHS, tmpContentPaths);

        // Get attributeOverrides, apply them and then remove them from the streamCreateRequest so they are not exposed to plugins
        Map<String, String> attributeOverrideHeaders =
                (HashMap<String, String>) streamCreateRequest.getProperties()
                        .get(Constants.ATTRIBUTE_OVERRIDES_KEY);
        applyAttributeOverridesToMetacardMap(attributeOverrideHeaders, metacardMap);
        streamCreateRequest.getProperties()
                .remove(Constants.ATTRIBUTE_OVERRIDES_KEY);

        CreateResponse createResponse = null;
        CreateStorageRequest createStorageRequest = null;
        CreateStorageResponse createStorageResponse;
        try {
            if (!contentItems.isEmpty()) {
                createStorageRequest = new CreateStorageRequestImpl(contentItems,
                        streamCreateRequest.getId(),
                        streamCreateRequest.getProperties());
                for (final PreCreateStoragePlugin plugin : frameworkProperties.getPreCreateStoragePlugins()) {
                    try {
                        createStorageRequest = plugin.process(createStorageRequest);
                    } catch (PluginExecutionException e) {
                        LOGGER.warn(
                                "Plugin processing failed. This is allowable. Skipping to next plugin.",
                                e);
                    }
                }

                historianTransactionKey = historian.version(createStorageRequest);

                try {
                    createStorageResponse = storageSupplier.get()
                            .create(createStorageRequest);
                    createStorageResponse.getProperties()
                            .put(CONTENT_PATHS, tmpContentPaths);
                } catch (StorageException e) {
                    throw new IngestException("Could not store content items.", e);
                }

                for (final PostCreateStoragePlugin plugin : frameworkProperties.getPostCreateStoragePlugins()) {
                    try {
                        createStorageResponse = plugin.process(createStorageResponse);
                    } catch (PluginExecutionException e) {
                        LOGGER.warn(
                                "Plugin processing failed. This is allowable. Skipping to next plugin.",
                                e);
                    }
                }

                for (ContentItem contentItem : createStorageResponse.getCreatedContentItems()) {
                    if (contentItem.getMetacard()
                            .getResourceURI() == null
                            && StringUtils.isBlank(contentItem.getQualifier())) {
                        contentItem.getMetacard()
                                .setAttribute(new AttributeImpl(Metacard.RESOURCE_URI,
                                        contentItem.getUri()));
                        contentItem.getMetacard()
                                .setAttribute(new AttributeImpl(Metacard.RESOURCE_SIZE,
                                        String.valueOf(contentItem.getSize())));
                    }
                    metacardMap.put(contentItem.getId(), contentItem.getMetacard());
                }
            }

            CreateRequest createRequest =
                    new CreateRequestImpl(new ArrayList<>(metacardMap.values()),
                            Optional.ofNullable(createStorageRequest)
                                    .map(StorageRequest::getProperties)
                                    .orElseGet(HashMap::new));

            createResponse = create(createRequest);
        } catch (Exception e) {
            opsCrudSupport.handleStorageException(createStorageRequest,
                    streamCreateRequest.getId(),
                    e);
        } finally {
            opsCrudSupport.commitAndCleanup(createStorageRequest,
                    historianTransactionKey,
                    tmpContentPaths);
        }

        return createResponse;
    }

    //
    // Private helper methods
    //
    private CreateRequest injectAttributes(CreateRequest request) {
        List<Metacard> metacards = request.getMetacards()
                .stream()
                .map((original) -> opsMetacardSupport.applyInjectors(original,
                        frameworkProperties.getAttributeInjectors()))
                .collect(Collectors.toList());

        return new CreateRequestImpl(metacards, request.getProperties(), request.getStoreIds());
    }

    private void applyAttributeOverridesToMetacardMap(Map<String, String> attributeOverrideMap,
            Map<String, Metacard> metacardMap) {

        if (MapUtils.isEmpty(attributeOverrideMap) || MapUtils.isEmpty(metacardMap)) {
            return;
        }

        metacardMap.values()
                .forEach(metacard -> attributeOverrideMap.keySet()
                        .stream()
                        .map(attributeName -> metacard.getMetacardType()
                                .getAttributeDescriptor(attributeName))
                        .filter(Objects::nonNull)
                        .map(ad -> overrideAttributeValue(ad,
                                attributeOverrideMap.get(ad.getName())))
                        .filter(Objects::nonNull)
                        .forEach(metacard::setAttribute));
    }

    private AttributeImpl overrideAttributeValue(AttributeDescriptor attributeDescriptor,
            String overrideValue) {
        try {
            Serializable newValue;
            switch (attributeDescriptor.getType()
                    .getAttributeFormat()) {
            case INTEGER:
                newValue = Integer.parseInt(overrideValue);
                break;
            case FLOAT:
                newValue = Float.parseFloat(overrideValue);
                break;
            case DOUBLE:
                newValue = Double.parseDouble(overrideValue);
                break;
            case SHORT:
                newValue = Short.parseShort(overrideValue);
                break;
            case LONG:
                newValue = Long.parseLong(overrideValue);
                break;
            case DATE:
                Calendar calendar = DatatypeConverter.parseDateTime(overrideValue);
                newValue = calendar.getTime();
                break;
            case BOOLEAN:
                newValue = Boolean.parseBoolean(overrideValue);
                break;
            case BINARY:
                newValue = overrideValue.getBytes(Charset.forName("UTF-8"));
                break;
            case OBJECT:
            case STRING:
            case GEOMETRY:
            case XML:
                newValue = overrideValue;
                break;

            default:
                return null;
            }
            return new AttributeImpl(attributeDescriptor.getName(), newValue);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void setDefaultValues(CreateRequest createRequest) {
        createRequest.getMetacards()
                .stream()
                .filter(Objects::nonNull)
                .forEach(opsCrudSupport::setDefaultValues);
    }

    /**
     * Validates that the {@link CreateRequest} is non-null and has a non-empty list of
     * {@link Metacard}s in it.
     *
     * @param createRequest the {@link CreateRequest}
     * @throws IngestException if the {@link CreateRequest} is null, or request has a null or empty list of
     *                         {@link Metacard}s
     */
    private void validateCreateRequest(CreateRequest createRequest) throws IngestException {
        if (createRequest == null) {
            throw new IngestException(
                    "CreateRequest was null, either passed in from endpoint, or as output from PreIngestPlugins");
        }
        List<Metacard> entries = createRequest.getMetacards();
        if (CollectionUtils.isEmpty(entries)) {
            throw new IngestException(
                    "Cannot perform ingest with null/empty entry list, either passed in from endpoint, or as output from PreIngestPlugins");
        }
    }

    /**
     * Helper method to build ingest log strings
     */
    private String buildIngestLog(CreateRequest createReq) {
        StringBuilder strBuilder = new StringBuilder();
        List<Metacard> metacards = createReq.getMetacards();
        final String newLine = System.lineSeparator();

        for (int i = 0; i < metacards.size(); i++) {
            Metacard card = metacards.get(i);
            strBuilder.append(newLine)
                    .append("Batch #: ")
                    .append(i + 1)
                    .append(" | ");
            if (card != null) {
                if (card.getTitle() != null) {
                    strBuilder.append("Metacard Title: ")
                            .append(card.getTitle())
                            .append(" | ");
                }
                if (card.getId() != null) {
                    strBuilder.append("Metacard ID: ")
                            .append(card.getId())
                            .append(" | ");
                }
            } else {
                strBuilder.append("Null Metacard");
            }
        }
        return strBuilder.toString();
    }

    private CreateResponse doRemoteCreate(CreateRequest createRequest) {
        HashSet<ProcessingDetails> exceptions = new HashSet<>();
        Map<String, Serializable> properties = new HashMap<>();

        List<CatalogStore> stores = opsCrudSupport.getCatalogStoresForRequest(createRequest,
                exceptions);

        for (CatalogStore store : stores) {
            try {
                if (!store.isAvailable()) {
                    exceptions.add(new ProcessingDetailsImpl(store.getId(),
                            null,
                            "CatalogStore is not available"));
                } else {
                    CreateResponse response = store.create(createRequest);
                    properties.put(store.getId(), new ArrayList<>(response.getCreatedMetacards()));
                }
            } catch (IngestException e) {
                INGEST_LOGGER.error("Error creating metacards for CatalogStore {}",
                        store.getId(),
                        e);
                exceptions.add(new ProcessingDetailsImpl(store.getId(), e));
            }
        }

        return new CreateResponseImpl(createRequest,
                properties,
                createRequest.getMetacards(),
                exceptions);
    }

    /**
     * Validates that the {@link CreateResponse} has one or more {@link Metacard}s in it that were
     * created in the catalog, and that the original {@link CreateRequest} is included in the
     * response.
     *
     * @param createResponse the original {@link CreateResponse} returned from the catalog provider
     * @param createRequest  the original {@link CreateRequest} sent to the catalog provider
     * @return the updated {@link CreateResponse}
     * @throws IngestException if original {@link CreateResponse} passed in is null or the {@link Metacard}s
     *                         list in the response is null
     */
    private CreateResponse validateFixCreateResponse(CreateResponse createResponse,
            CreateRequest createRequest) throws IngestException {
        if (createResponse != null) {
            if (createResponse.getCreatedMetacards() == null) {
                throw new IngestException(
                        "CatalogProvider returned null list of results from create method.");
            }
            if (createResponse.getRequest() == null) {
                createResponse = new CreateResponseImpl(createRequest,
                        createResponse.getProperties(),
                        createResponse.getCreatedMetacards());
            }
        } else {
            throw new IngestException("CatalogProvider returned null CreateResponse Object.");
        }
        return createResponse;
    }

}
