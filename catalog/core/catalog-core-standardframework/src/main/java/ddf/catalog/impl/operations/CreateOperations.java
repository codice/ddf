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

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;
import ddf.catalog.content.StorageException;
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
 * Support class for create delegate operations for the {@code CatalogFrameworkImpl}.
 * <p>
 * This class contains two delegated methods and methods to support them. No
 * operations/support methods should be added to this class except in support of CFI
 * create operations.
 */
public class CreateOperations {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateOperations.class);

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

    public CreateOperations(FrameworkProperties frameworkProperties,
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
    public CreateResponse create(CreateRequest createRequest)
            throws IngestException, SourceUnavailableException {
        CreateResponse createResponse = null;

        Exception ingestError = null;

        createRequest = queryOperations.setFlagsOnRequest(createRequest);
        createRequest = validateCreateRequest(createRequest);
        createRequest = validateLocalSource(createRequest);

        try {
            createRequest = injectAttributes(createRequest);
            createRequest = setDefaultValues(createRequest);
            createRequest = processPreAuthorizationPlugins(createRequest);
            createRequest = updateCreateRequestPolicyMap(createRequest);
            createRequest = processPrecreateAccessPlugins(createRequest);

            createRequest.getProperties()
                    .put(Constants.OPERATION_TRANSACTION_KEY,
                            new OperationTransactionImpl(OperationTransaction.OperationType.CREATE,
                                    Collections.emptyList()));

            createRequest = processPreIngestPlugins(createRequest);
            createRequest = validateCreateRequest(createRequest);
            createResponse = getCreateResponse(createRequest);
            createResponse = performRemoteCreate(createRequest, createResponse);

        } catch (IngestException iee) {
            INGEST_LOGGER.debug("Ingest error", iee);
            ingestError = iee;
            throw iee;
        } catch (StopProcessingException see) {
            ingestError = see;
            throw new IngestException(PRE_INGEST_ERROR, see);
        } catch (RuntimeException re) {
            ingestError = re;
            throw new InternalIngestException("Exception during runtime while performing create",
                    re);
        } finally {
            if (ingestError != null && INGEST_LOGGER.isInfoEnabled()) {
                INGEST_LOGGER.info("Error on create operation. {} metacards failed to ingest. {}",
                        createRequest.getMetacards()
                                .size(),
                        buildIngestLog(createRequest),
                        ingestError);
            }
        }

        try {
            createResponse = validateFixCreateResponse(createResponse, createRequest);
            createResponse = processPostIngestPlugins(createResponse);
        } catch (RuntimeException re) {
            LOGGER.info(
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

    public CreateResponse create(CreateStorageRequest streamCreateRequest,
            List<String> fanoutTagBlacklist) throws IngestException, SourceUnavailableException {
        Map<String, Metacard> metacardMap = new HashMap<>();
        List<ContentItem> contentItems = new ArrayList<>(streamCreateRequest.getContentItems()
                .size());
        HashMap<String, Path> tmpContentPaths = new HashMap<>(streamCreateRequest.getContentItems()
                .size());
        CreateResponse createResponse = null;
        CreateStorageRequest createStorageRequest = null;
        CreateStorageResponse createStorageResponse;

        streamCreateRequest = opsStorageSupport.prepareStorageRequest(streamCreateRequest,
                streamCreateRequest::getContentItems);

        // Operation populates the metacardMap, contentItems, and tmpContentPaths
        opsMetacardSupport.generateMetacardAndContentItems(streamCreateRequest.getContentItems(),
                (Subject) streamCreateRequest.getPropertyValue(SecurityConstants.SECURITY_SUBJECT),
                metacardMap,
                contentItems,
                tmpContentPaths);

        if (blockCreateMetacards(metacardMap.values(), fanoutTagBlacklist)) {
            String message =
                    "Fanout proxy does not support create operations with blacklisted metacard tag";
            LOGGER.debug("{}. Tags blacklist: {}", message, fanoutTagBlacklist);
            throw new IngestException(message);
        }

        streamCreateRequest.getProperties()
                .put(CONTENT_PATHS, tmpContentPaths);

        injectAttributes(metacardMap);
        setDefaultValues(metacardMap);
        streamCreateRequest = applyAttributeOverrides(streamCreateRequest, metacardMap);

        try {
            if (!contentItems.isEmpty()) {
                createStorageRequest = new CreateStorageRequestImpl(contentItems,
                        streamCreateRequest.getId(),
                        streamCreateRequest.getProperties());
                createStorageRequest = processPreCreateStoragePlugins(createStorageRequest);

                try {
                    createStorageResponse = sourceOperations.getStorage()
                            .create(createStorageRequest);
                    createStorageResponse.getProperties()
                            .put(CONTENT_PATHS, tmpContentPaths);
                } catch (StorageException e) {
                    throw new IngestException("Could not store content items.", e);
                }

                createStorageResponse = processPostCreateStoragePlugins(createStorageResponse);

                populateMetacardMap(metacardMap, createStorageResponse);
            }

            CreateRequest createRequest =
                    new CreateRequestImpl(new ArrayList<>(metacardMap.values()),
                            Optional.ofNullable(createStorageRequest)
                                    .map(StorageRequest::getProperties)
                                    .orElseGet(HashMap::new));

            createResponse = create(createRequest);
        } catch (IOException | RuntimeException e) {
            if (createStorageRequest != null) {
                try {
                    sourceOperations.getStorage()
                            .rollback(createStorageRequest);
                } catch (StorageException e1) {
                    LOGGER.info("Unable to remove temporary content for id: {}",
                            createStorageRequest.getId(),
                            e1);
                }
            }
            throw new IngestException(
                    "Unable to store products for request: " + streamCreateRequest.getId(), e);

        } finally {
            opsStorageSupport.commitAndCleanup(createStorageRequest, tmpContentPaths);
        }

        return createResponse;
    }

    //
    // Private helper methods
    //
    private boolean blockCreateMetacards(Collection<Metacard> metacards,
            List<String> fanoutBlacklist) {
        return metacards.stream()
                .anyMatch((metacard) -> isMetacardBlacklisted(metacard, fanoutBlacklist));
    }

    private boolean isMetacardBlacklisted(Metacard metacard, List<String> fanoutBlacklist) {
        Set<String> tags = new HashSet<>(metacard.getTags());

        if (tags.isEmpty()) {
            tags.add(Metacard.DEFAULT_TAG);
        }

        return CollectionUtils.containsAny(tags, fanoutBlacklist);
    }

    private void injectAttributes(Map<String, Metacard> metacardMap) {
        for (Map.Entry<String, Metacard> entry : metacardMap.entrySet()) {
            Metacard originalMetacard = entry.getValue();
            metacardMap.put(entry.getKey(),
                    opsMetacardSupport.applyInjectors(originalMetacard,
                            frameworkProperties.getAttributeInjectors()));
        }
    }

    private void setDefaultValues(Map<String, Metacard> metacardMap) {
        metacardMap.values()
                .forEach(opsMetacardSupport::setDefaultValues);
    }

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

    private CreateRequest setDefaultValues(CreateRequest createRequest) {
        createRequest.getMetacards()
                .stream()
                .filter(Objects::nonNull)
                .forEach(opsMetacardSupport::setDefaultValues);
        return createRequest;
    }

    /**
     * Validates that the {@link CreateRequest} is non-null and has a non-empty list of
     * {@link Metacard}s in it.
     *
     * @param createRequest the {@link CreateRequest}
     * @throws IngestException if the {@link CreateRequest} is null, or request has a null or empty list of
     *                         {@link Metacard}s
     */
    private CreateRequest validateCreateRequest(CreateRequest createRequest)
            throws IngestException {
        if (createRequest == null) {
            throw new IngestException(
                    "CreateRequest was null, either passed in from endpoint, or as output from PreIngestPlugins");
        }
        List<Metacard> entries = createRequest.getMetacards();
        if (CollectionUtils.isEmpty(entries)) {
            throw new IngestException(
                    "Cannot perform ingest with null/empty entry list, either passed in from endpoint, or as output from PreIngestPlugins");
        }

        return createRequest;
    }

    /**
     * Helper method to build ingest log strings
     */
    private String buildIngestLog(CreateRequest createReq) {
        StringBuilder strBuilder = new StringBuilder();
        List<Metacard> metacards = createReq.getMetacards();
        String metacardTitleLabel = "Metacard Title: ";
        String metacardIdLabel = "Metacard ID: ";

        for (int i = 0; i < metacards.size(); i++) {
            Metacard card = metacards.get(i);
            strBuilder.append(System.lineSeparator())
                    .append("Batch #: ")
                    .append(i + 1)
                    .append(" | ");
            if (card != null) {
                if (card.getTitle() != null) {
                    strBuilder.append(metacardTitleLabel)
                            .append(card.getTitle())
                            .append(" | ");
                }
                if (card.getId() != null) {
                    strBuilder.append(metacardIdLabel)
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

        List<CatalogStore> stores = opsCatStoreSupport.getCatalogStoresForRequest(createRequest,
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

    private CreateResponse processPostIngestPlugins(CreateResponse createResponse) {
        for (final PostIngestPlugin plugin : frameworkProperties.getPostIngest()) {
            try {
                createResponse = plugin.process(createResponse);
            } catch (PluginExecutionException e) {
                LOGGER.info("Plugin processing failed. This is allowable. Skipping to next plugin.",
                        e);
            }
        }
        return createResponse;
    }

    private CreateResponse performRemoteCreate(CreateRequest createRequest,
            CreateResponse createResponse) {
        if (!opsCatStoreSupport.isCatalogStoreRequest(createRequest)) {
            return createResponse;
        }

        CreateResponse remoteCreateResponse = doRemoteCreate(createRequest);
        if (createResponse == null) {
            createResponse = remoteCreateResponse;
        } else {
            createResponse.getProperties()
                    .putAll(remoteCreateResponse.getProperties());
            createResponse.getProcessingErrors()
                    .addAll(remoteCreateResponse.getProcessingErrors());
        }
        return createResponse;
    }

    private CreateResponse getCreateResponse(CreateRequest createRequest) throws IngestException {
        // Call the create on the catalog
        LOGGER.debug("Calling catalog.create() with {} entries.",
                createRequest.getMetacards()
                        .size());
        if (!Requests.isLocal(createRequest)) {
            return null;
        }

        return sourceOperations.getCatalog()
                .create(createRequest);
    }

    private CreateRequest processPreIngestPlugins(CreateRequest createRequest)
            throws StopProcessingException {
        for (PreIngestPlugin plugin : frameworkProperties.getPreIngest()) {
            try {
                createRequest = plugin.process(createRequest);
            } catch (PluginExecutionException e) {
                LOGGER.info("Plugin processing failed. This is allowable. Skipping to next plugin.",
                        e);
            }
        }
        return createRequest;
    }

    private CreateRequest processPrecreateAccessPlugins(CreateRequest createRequest)
            throws StopProcessingException {
        for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
            createRequest = plugin.processPreCreate(createRequest);
        }
        return createRequest;
    }

    private CreateRequest processPreAuthorizationPlugins(CreateRequest createRequest)
            throws StopProcessingException {
        for (PreAuthorizationPlugin plugin : frameworkProperties.getPreAuthorizationPlugins()) {
            createRequest = plugin.processPreCreate(createRequest);
        }
        return createRequest;
    }

    private CreateRequest updateCreateRequestPolicyMap(CreateRequest createRequest)
            throws StopProcessingException {
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

        return createRequest;
    }

    private CreateRequest validateLocalSource(CreateRequest createRequest)
            throws SourceUnavailableException {
        if (Requests.isLocal(createRequest)
                && !sourceOperations.isSourceAvailable(sourceOperations.getCatalog())) {
            SourceUnavailableException sourceUnavailableException = new SourceUnavailableException(
                    "Local provider is not available, cannot perform create operation.");
            if (INGEST_LOGGER.isInfoEnabled()) {
                INGEST_LOGGER.info(
                        "Error on create operation, local provider not available. {} metacards failed to ingest. {}",
                        createRequest.getMetacards()
                                .size(),
                        buildIngestLog(createRequest),
                        sourceUnavailableException);
            }
            throw sourceUnavailableException;
        }

        return createRequest;
    }

    private void populateMetacardMap(Map<String, Metacard> metacardMap,
            CreateStorageResponse createStorageResponse) throws IOException {
        for (ContentItem contentItem : createStorageResponse.getCreatedContentItems()) {
            if (StringUtils.isBlank(contentItem.getQualifier())) {
                Metacard metacard = metacardMap.get(contentItem.getId());

                Metacard overrideMetacard = contentItem.getMetacard();

                Metacard updatedMetacard = OverrideAttributesSupport.overrideMetacard(metacard,
                        overrideMetacard,
                        true,
                        true);

                updatedMetacard.setAttribute(new AttributeImpl(Metacard.RESOURCE_URI,
                        contentItem.getUri()));
                updatedMetacard.setAttribute(new AttributeImpl(Metacard.RESOURCE_SIZE,
                        String.valueOf(contentItem.getSize())));

                metacardMap.put(contentItem.getId(), updatedMetacard);
            }
        }
    }

    private CreateStorageResponse processPostCreateStoragePlugins(
            CreateStorageResponse createStorageResponse) {
        for (final PostCreateStoragePlugin plugin : frameworkProperties.getPostCreateStoragePlugins()) {
            try {
                createStorageResponse = plugin.process(createStorageResponse);
            } catch (PluginExecutionException e) {
                LOGGER.debug("Plugin processing failed. This is allowable. Skipping to next plugin.",
                        e);
            }
        }
        return createStorageResponse;
    }

    private CreateStorageRequest processPreCreateStoragePlugins(
            CreateStorageRequest createStorageRequest) {
        for (final PreCreateStoragePlugin plugin : frameworkProperties.getPreCreateStoragePlugins()) {
            try {
                createStorageRequest = plugin.process(createStorageRequest);
            } catch (PluginExecutionException e) {
                LOGGER.debug("Plugin processing failed. This is allowable. Skipping to next plugin.",
                        e);
            }
        }
        return createStorageRequest;
    }

    @SuppressWarnings("unchecked")
    private CreateStorageRequest applyAttributeOverrides(CreateStorageRequest createStorageRequest,
            Map<String, Metacard> metacardMap) {
        // Get attributeOverrides, apply them and then remove them from the streamCreateRequest so they are not exposed to plugins
        Map<String, String> attributeOverrideHeaders =
                (HashMap<String, String>) createStorageRequest.getProperties()
                        .get(Constants.ATTRIBUTE_OVERRIDES_KEY);
        applyAttributeOverridesToMetacardMap(attributeOverrideHeaders, metacardMap);
        createStorageRequest.getProperties()
                .remove(Constants.ATTRIBUTE_OVERRIDES_KEY);

        OverrideAttributesSupport.overrideAttributes(createStorageRequest.getContentItems(),
                metacardMap);

        return createStorageRequest;
    }
}
