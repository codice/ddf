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
package ddf.catalog.history;

import static com.google.common.base.Strings.isNullOrEmpty;
import static ddf.catalog.Constants.CONTENT_PATHS;
import static ddf.catalog.core.versioning.MetacardVersion.HISTORY_METACARDS_PROPERTY;
import static ddf.catalog.core.versioning.MetacardVersion.SKIP_VERSIONING;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.security.common.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.core.versioning.MetacardVersion;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Operation;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.security.Subject;

/**
 * Class utilizing {@link StorageProvider} and {@link CatalogProvider} to version
 * {@link Metacard}s and associated {@link ContentItem}s.
 */
public class Historian {
    private static final Logger LOGGER = LoggerFactory.getLogger(Historian.class);

    private final Map<String, List<Callable<Boolean>>> staged = new ConcurrentHashMap<>();

    private boolean historyEnabled = false;

    private List<StorageProvider> storageProviders;

    private List<CatalogProvider> catalogProviders;

    private Supplier<org.apache.shiro.subject.Subject> getSubject = SecurityUtils::getSubject;

    /**
     * Versions {@link Metacard}s from the given {@link CreateResponse}.
     *
     * @param createResponse Response to create versioned metacards from
     * @return The original {@link CreateResponse}
     * @throws IngestException
     */
    public CreateResponse version(CreateResponse createResponse) throws IngestException {
        if (doSkip(createResponse)) {
            return createResponse;
        }
        setSkipFlag(createResponse);

        List<Metacard> versionedMetacards = createResponse.getCreatedMetacards()
                .stream()
                .filter(MetacardVersion::isNotVersion)
                .map(m -> new MetacardVersion(m, MetacardVersion.Action.CREATED, getSubject.get()))
                .collect(Collectors.toList());

        if (!versionedMetacards.isEmpty()) {
            executeAsSystem(() -> catalogProvider().create(new CreateRequestImpl(versionedMetacards)));
        }

        return createResponse;
    }

    /**
     * Version updated metacards based off of the successfully created metacards found in the
     * {@link UpdateResponse}
     *
     * @param updateResponse Versioned metacards created from any created metacards in this
     * @return The original UpdateResponse
     * @throws SourceUnavailableException
     * @throws IngestException
     */
    public UpdateResponse version(UpdateResponse updateResponse)
            throws SourceUnavailableException, IngestException {
        if (doSkip(updateResponse)) {
            return updateResponse;
        }
        setSkipFlag(updateResponse);

        List<Metacard> inputMetacards = updateResponse.getUpdatedMetacards()
                .stream()
                .map(Update::getNewMetacard)
                .collect(Collectors.toList());

        CreateResponse createdHistory = versionMetacards(inputMetacards,
                MetacardVersion.Action.UPDATED);
        if (createdHistory == null) {
            return updateResponse;
        }

        if (updateResponse.getProperties() != null) {
            updateResponse.getProperties()
                    .put(HISTORY_METACARDS_PROPERTY,
                            new ArrayList<>(createdHistory.getCreatedMetacards()));
        }

        return updateResponse;
    }

    /**
     * Versions created {@link Metacard}s and {@link ContentItem}s.
     *
     * @param createStorageRequest Versions this requests items
     * @return The transaction key for using with {@link Historian#commit(String)} and
     * {@link Historian#rollback(String)}
     * @throws IngestException
     * @throws StorageException
     */
    public Optional<String> version(CreateStorageRequest createStorageRequest)
            throws IngestException, StorageException {
        if (doSkip(createStorageRequest)) {
            return Optional.empty();
        }
        setSkipFlag(createStorageRequest);

        String transactionKey = UUID.randomUUID()
                .toString();
        List<Callable<Boolean>> preStaged = new ArrayList<>(2);
        staged.put(transactionKey, preStaged);

        @SuppressWarnings("unchecked")
        Map<String, Path> tmpContentPaths = (Map<String, Path>) createStorageRequest.getProperties()
                .getOrDefault(CONTENT_PATHS, new HashMap<>());

        List<ContentItem> versionedContentItems =
                versionContentItems(createStorageRequest.getContentItems(),
                        tmpContentPaths,
                        MetacardVersion.Action.CREATED_CONTENT);

        setResourceUriIfMissing(versionedContentItems);

        CreateStorageRequestImpl versionStorageRequest = new CreateStorageRequestImpl(
                versionedContentItems,
                createStorageRequest.getId(),
                createStorageRequest.getProperties());

        // These are being staged for delayed execution, to be ran after the storage/catalog has
        // successfully committed/updated
        preStaged.add(() -> {
            executeAsSystem(() -> {
                storageProvider().create(versionStorageRequest);
                storageProvider().commit(versionStorageRequest);
                return true;
            });
            return true;
        });
        preStaged.add(() -> {
            executeAsSystem(() -> {
                catalogProvider().create(new CreateRequestImpl(versionedContentItems.stream()
                        .map(ContentItem::getMetacard)
                        .distinct()
                        .collect(Collectors.toList())));
                return true;
            });
            return true;
        });

        return Optional.of(transactionKey);
    }

    /**
     * Versions updated {@link Metacard}s and {@link ContentItem}s.
     *
     * @param streamUpdateRequest   Needed to pass {@link MetacardVersion#SKIP_VERSIONING}
     *                              flag into downstream update
     * @param updateStorageResponse Versions this response's updated items
     * @param tmpContentPaths       The temporary content paths needed to duplicate the
     *                              {@link ContentItem}s
     * @return The transaction key for using with {@link Historian#commit(String)} and
     * {@link Historian#rollback(String)}
     * @throws IOException
     */
    public Optional<String> version(UpdateStorageRequest streamUpdateRequest,
            UpdateStorageResponse updateStorageResponse, HashMap<String, Path> tmpContentPaths)
            throws IOException {
        if (doSkip(updateStorageResponse)) {
            return Optional.empty();
        }
        setSkipFlag(streamUpdateRequest);
        setSkipFlag(updateStorageResponse);

        String transactionKey = UUID.randomUUID()
                .toString();
        List<Callable<Boolean>> preStaged = new ArrayList<>(2);
        staged.put(transactionKey, preStaged);

        List<ContentItem> versionedContentItems =
                versionContentItems(updateStorageResponse.getUpdatedContentItems(),
                        tmpContentPaths,
                        MetacardVersion.Action.UPDATED_CONTENT);

        List<Metacard> versionedMetacards = versionedContentItems.stream()
                .map(ContentItem::getMetacard)
                .distinct()
                .collect(Collectors.toList());

        // These are being staged for delayed execution, to be ran after the storage/catalog has
        // successfully committed/updated
        preStaged.add(() -> {
            CreateStorageRequestImpl createStorageRequest = new CreateStorageRequestImpl(
                    versionedContentItems,
                    new HashMap<>());
            setSkipFlag(createStorageRequest);
            CreateStorageResponse createStorageResponseResult = executeAsSystem(() -> {
                CreateStorageResponse createStorageResponse = storageProvider().create(
                        createStorageRequest);
                storageProvider().commit(createStorageRequest);
                overwriteResourceUris(createStorageResponse);
                return createStorageResponse;
            });

            return Optional.ofNullable(createStorageResponseResult)
                    .map(CreateStorageResponse::getProcessingErrors)
                    .map(Set::isEmpty)
                    .orElse(false);
        });

        preStaged.add(() -> {
            Map<String, Serializable> props = new HashMap<>();
            props.put(SKIP_VERSIONING, true);
            CreateResponse createResponse =
                    executeAsSystem(() -> catalogProvider().create(new CreateRequestImpl(
                            versionedMetacards,
                            props)));
            return Optional.ofNullable(createResponse)
                    .map(CreateResponse::getProcessingErrors)
                    .map(Set::isEmpty)
                    .orElse(false);
        });

        return Optional.of(transactionKey);
    }

    /**
     * Versions deleted {@link Metacard}s.
     *
     * @param deleteResponse Versions this responses deleted metacards
     */
    public void version(DeleteResponse deleteResponse) {
        if (doSkip(deleteResponse)) {
            return;
        }
        setSkipFlag(deleteResponse);

        List<Metacard> versionedMetacards = deleteResponse.getDeletedMetacards()
                .stream()
                .map(mc -> new MetacardVersion(mc,
                        MetacardVersion.Action.DELETED,
                        getSubject.get()))
                .collect(Collectors.toList());

        CreateResponse createResponse =
                executeAsSystem(() -> catalogProvider().create(new CreateRequestImpl(
                        versionedMetacards)));
    }

    /**
     * Commits a set of staged operations.
     *
     * @param historianTransactionKey The key associated with a set of operations to commit
     * @return A List of exceptions, if any, generated by the commit operations
     */
    public List<Exception> commit(String historianTransactionKey) {
        List<Callable<Boolean>> ops = staged.remove(historianTransactionKey);
        if (ops == null) {
            LOGGER.warn("There was no operations staged for historian transaction key [{}]",
                    historianTransactionKey);
            return null;
        }

        ArrayList<Exception> exceptions = new ArrayList<>();
        for (Callable<Boolean> op : ops) {
            try {
                op.call();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        return exceptions;
    }

    /**
     * Removes a set of staged operations.
     * </p>
     * Calling multiple times or on a non-existent key has no effect.
     *
     * @param historianTransactionKey The key associated with the set of operations to upstage.
     */
    public void rollback(String historianTransactionKey) {
        staged.remove(historianTransactionKey);
    }

    public boolean isHistoryEnabled() {
        return historyEnabled;
    }

    public void setHistoryEnabled(boolean historyEnabled) {
        this.historyEnabled = historyEnabled;
    }

    public List<StorageProvider> getStorageProviders() {
        return storageProviders;
    }

    public void setStorageProviders(List<StorageProvider> storageProviders) {
        this.storageProviders = storageProviders;
    }

    public List<CatalogProvider> getCatalogProviders() {
        return catalogProviders;
    }

    public void setCatalogProviders(List<CatalogProvider> catalogProviders) {
        this.catalogProviders = catalogProviders;
    }

    private StorageProvider storageProvider() {
        return storageProviders.stream()
                .findFirst()
                .orElse(null);
    }

    private CatalogProvider catalogProvider() {
        return catalogProviders.stream()
                .findFirst()
                .orElse(null);
    }

    private void overwriteResourceUris(CreateStorageResponse createStorageResponse) {
        createStorageResponse.getCreatedContentItems()
                .forEach((ci) -> {
                    try {
                        ci.getMetacard()
                                .setAttribute(new AttributeImpl(Metacard.RESOURCE_URI,
                                        ci.getUri()));
                        ci.getMetacard()
                                .setAttribute(new AttributeImpl(Metacard.RESOURCE_SIZE,
                                        ci.getSize()));
                    } catch (IOException e) {
                        LOGGER.warn("Could not get size", e);
                    }

                });
    }

    private List<ContentItem> versionContentItems(List<ContentItem> contentItems,
            Map<String, Path> tmpContentPaths, MetacardVersion.Action action) {
        return contentItems.stream()
                .map(ContentItem::getMetacard)
                .distinct()
                .flatMap(mc -> getVersionedContent(mc,
                        contentItems,
                        action,
                        tmpContentPaths).stream())
                .collect(Collectors.toList());
    }

    private void setResourceUriIfMissing(List<ContentItem> newContentItems) {
        newContentItems.stream()
                .filter(ci -> ci.getMetacard()
                        .getResourceURI() == null)
                .filter(ci -> StringUtils.isBlank(ci.getQualifier()))
                .forEach(ci -> {
                    ci.getMetacard()
                            .setAttribute(new AttributeImpl(Metacard.RESOURCE_URI, ci.getUri()));
                    try {
                        ci.getMetacard()
                                .setAttribute(new AttributeImpl(Metacard.RESOURCE_SIZE,
                                        ci.getSize()));
                    } catch (IOException e) {
                        LOGGER.warn("Could not get size of content item", e);
                    }
                });
    }

    private CreateResponse versionMetacards(List<Metacard> metacards,
            final MetacardVersion.Action action)
            throws SourceUnavailableException, IngestException {
        final List<Metacard> versionedMetacards = metacards.stream()
                .filter(MetacardVersion::isNotVersion)
                .map(metacard -> new MetacardVersion(metacard, action, getSubject.get()))
                .collect(Collectors.toList());

        if (versionedMetacards.isEmpty()) {
            return null;
        }

        return executeAsSystem(() -> catalogProvider().create(new CreateRequestImpl(
                versionedMetacards)));
    }

    private List<ContentItem> getVersionedContent(Metacard root, List<ContentItem> contentItems,
            MetacardVersion.Action versionAction, Map<String, Path> tmpContentPaths) {
        String id = root.getId();
        MetacardVersion rootVersion = new MetacardVersion(root, versionAction, getSubject.get());
        Supplier<Stream<ContentItem>> relatedContent = () -> contentItems.stream()
                .filter(ci -> ci.getId()
                        .equals(id));

        ContentItem rootItem = relatedContent.get()
                .filter(ci -> !isNullOrEmpty(ci.getUri()))
                .filter(ci -> !ci.getUri()
                        .contains("#"))
                .findAny()
                .orElseThrow(() -> new RuntimeException(
                        "Could not find root content item for: " + id));

        List<ContentItem> derivedContent = relatedContent.get()
                .filter(ci -> !ci.equals(rootItem))
                .collect(Collectors.toList());

        List<ContentItem> resultItems = new ArrayList<>();

        // version root content item
        long size = 0;
        try {
            size = rootItem.getSize();
        } catch (IOException e) {
            LOGGER.warn("Could not get size of item [{}].", rootItem.getId(), e);
        }
        resultItems.add(new ContentItemImpl(rootVersion.getId(),
                Files.asByteSource(tmpContentPaths.get(rootItem.getId())
                        .toFile()),
                rootItem.getMimeTypeRawData(),
                rootItem.getFilename(),
                size,
                rootVersion));

        // version derived content items
        for (ContentItem contentItem : derivedContent) {
            size = 0;
            try {
                size = contentItem.getSize();
            } catch (IOException e) {
                LOGGER.warn("Could not get size of item [{}].", rootItem.getId(), e);
            }
            resultItems.add(new ContentItemImpl(rootVersion.getId(),
                    contentItem.getQualifier(),
                    new WrappedByteSource(contentItem),
                    contentItem.getMimeTypeRawData(),
                    contentItem.getFilename(),
                    size,
                    rootVersion));
        }

        return resultItems;
    }

    /**
     * Caution should be used with this, as it elevates the permissions to the System user.
     *
     * @param func What to execute as the System
     * @param <T>  Generic return type of func
     * @return result of the callable func
     */
    private <T> T executeAsSystem(Callable<T> func) {
        Subject systemSubject = Security.runAsAdmin(() -> Security.getInstance()
                .getSystemSubject());
        if (systemSubject == null) {
            throw new RuntimeException("Could not get systemSubject to version metacards.");
        }
        return systemSubject.execute(func);
    }

    private boolean doSkip(@Nullable Operation op) {
        return !historyEnabled || ((boolean) Optional.ofNullable(op)
                .map(Operation::getProperties)
                .orElse(Collections.emptyMap())
                .getOrDefault(SKIP_VERSIONING, false));
    }

    private void setSkipFlag(@Nullable Operation op) {
        Optional.ofNullable(op)
                .map(Operation::getProperties)
                .ifPresent(p -> p.put(SKIP_VERSIONING, true));
    }

    private static class WrappedByteSource extends ByteSource {
        private ContentItem contentItem;

        private WrappedByteSource(ContentItem contentItem) {
            this.contentItem = contentItem;
        }

        @Override
        public InputStream openStream() throws IOException {
            return contentItem.getInputStream();
        }
    }
}
