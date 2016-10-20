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

import static ddf.catalog.core.versioning.MetacardVersion.SKIP_VERSIONING;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.codice.ddf.security.common.Security;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;

import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.ReadStorageRequest;
import ddf.catalog.content.operation.ReadStorageResponse;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.content.operation.impl.ReadStorageRequestImpl;
import ddf.catalog.core.versioning.MetacardVersion;
import ddf.catalog.core.versioning.MetacardVersion.Action;
import ddf.catalog.core.versioning.impl.DeletedMetacardImpl;
import ddf.catalog.core.versioning.impl.MetacardVersionImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Operation;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.SubjectUtils;

/**
 * Class utilizing {@link StorageProvider} and {@link CatalogProvider} to version
 * {@link Metacard}s and associated {@link ContentItem}s.
 */
public class Historian {
    private static final Logger LOGGER = LoggerFactory.getLogger(Historian.class);

    private boolean historyEnabled = true;

    private List<StorageProvider> storageProviders;

    private List<CatalogProvider> catalogProviders;

    private List<MetacardType> metacardTypes;

    private FilterBuilder filterBuilder;

    /**
     * Versions metacards being updated based off of the <code>getOldMetacards</code> method on
     * {@link UpdateResponse}
     *
     * @param updateResponse Versioned metacards created from any old metacards
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
                .map(Update::getOldMetacard)
                .collect(Collectors.toList());

        final Map<String, Metacard> versionedMetacards = getVersionMetacards(inputMetacards,
                Action.VERSIONED,
                (Subject) updateResponse.getRequest()
                        .getProperties()
                        .get(SecurityConstants.SECURITY_SUBJECT));

        CreateResponse response = storeVersionMetacards(versionedMetacards);

        return updateResponse;
    }

    /**
     * Versions updated {@link Metacard}s and {@link ContentItem}s.
     *
     * @param streamUpdateRequest   Needed to pass {@link MetacardVersion#SKIP_VERSIONING}
     *                              flag into downstream update
     * @param updateStorageResponse Versions this response's updated items
     * @return The transaction key for using with {@link Historian#commit(String)} and
     * @throws IOException
     */
    public UpdateStorageResponse version(UpdateStorageRequest streamUpdateRequest,
            UpdateStorageResponse updateStorageResponse)
            throws UnsupportedQueryException, SourceUnavailableException, IngestException {
        if (doSkip(updateStorageResponse)) {
            return updateStorageResponse;
        }
        setSkipFlag(streamUpdateRequest);
        setSkipFlag(updateStorageResponse);

        Collection<ReadStorageRequest> ids = getReadStorageRequests(updateStorageResponse);
        Map<String, Metacard> metacards = query(forIds(fromStorageRequests(ids)));
        Map<String, List<ContentItem>> oldContent = getOldContent(ids);

        Map<String, Metacard> versionMetacards = getVersionMetacards(metacards.values(),
                Action.VERSIONED_CONTENT,
                (Subject) streamUpdateRequest.getProperties()
                        .get(SecurityConstants.SECURITY_SUBJECT));

        CreateStorageResponse createStorageResponse = versionContentItems(oldContent,
                versionMetacards);

        setResourceUriForContent(/*mutable*/ versionMetacards, createStorageResponse);

        CreateResponse createResponse = storeVersionMetacards(versionMetacards);

        return updateStorageResponse;
    }

    /**
     * Versions deleted {@link Metacard}s.
     *
     * @param deleteResponse Versions this responses deleted metacards
     */
    public DeleteResponse version(DeleteResponse deleteResponse)
            throws SourceUnavailableException, IngestException {
        if (doSkip(deleteResponse)) {
            return deleteResponse;
        }
        setSkipFlag(deleteResponse);

        Map<String, List<ContentItem>> contentItems = getContentItems(deleteResponse);
        Action action = contentItems.isEmpty() ? Action.DELETED : Action.DELETED_CONTENT;

        List<Metacard> versionedMetacards = deleteResponse.getDeletedMetacards()
                .stream()
                .map(mc -> new MetacardVersionImpl(mc,
                        action,
                        (Subject) deleteResponse.getRequest()
                                .getProperties()
                                .get(SecurityConstants.SECURITY_SUBJECT)))
                .collect(Collectors.toList());

        CreateResponse createResponse =
                executeAsSystem(() -> catalogProvider().create(new CreateRequestImpl(
                        versionedMetacards)));

        Map<String, Metacard> versionedMap = createResponse.getCreatedMetacards()
                .stream()
                .collect(Collectors.toMap(m -> (String) m.getAttribute(MetacardVersion.VERSION_OF_ID)
                        .getValue(), Function.identity()));

        CreateStorageResponse createStorageResponse = versionContentItems(contentItems,
                versionedMap);

        List<Metacard> deletedMetacards = versionedMap.keySet()
                .stream()
                .map(s -> new DeletedMetacardImpl(s,
                        SubjectUtils.getEmailAddress((Subject) deleteResponse.getProperties()
                                .get(SecurityConstants.SECURITY_SUBJECT)),
                        versionedMap.get(s)
                                .getId(),
                        MetacardVersionImpl.toMetacard(versionedMap.get(s), metacardTypes)))
                .collect(Collectors.toList());

        CreateResponse deleteTrackResponse =
                executeAsSystem(() -> catalogProvider().create(new CreateRequestImpl(
                        deletedMetacards,
                        new HashMap<>())));

        return deleteResponse;
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

    public void setFilterBuilder(FilterBuilder filterBuilder) {
        this.filterBuilder = filterBuilder;
    }

    private List<String> fromStorageRequests(Collection<ReadStorageRequest> requests) {
        return requests.stream()
                .map(StorageRequest::getId)
                .collect(Collectors.toList());
    }

    private Map<String, Metacard> query(Filter filter) throws UnsupportedQueryException {
        SourceResponse response = catalogProvider().query(new QueryRequestImpl(new QueryImpl(filter,
                1,
                250,
                null,
                false,
                TimeUnit.SECONDS.toMillis(10))));

        return response.getResults()
                .stream()
                .map(Result::getMetacard)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Metacard::getId, Function.identity()));
    }

    private Filter forIds(List<String> ids) {
        List<Filter> idFilters = ids.stream()
                .map(id -> filterBuilder.attribute(Metacard.ID)
                        .is()
                        .equalTo()
                        .text(id))
                .collect(Collectors.toList());

        return filterBuilder.anyOf(idFilters);
    }

    private Map<String, List<ContentItem>> getOldContent(Collection<ReadStorageRequest> ids) {
        return ids.stream()
                .map(this::getStorageItem)
                .filter(Objects::nonNull)
                .map(ReadStorageResponse::getContentItem)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ContentItem::getId,
                        ImmutableList::of,
                        (l, r) -> (List) ImmutableList.builder()
                                .addAll(l)
                                .addAll(r)
                                .build()));
    }

    private List<ReadStorageRequest> getReadStorageRequests(
            UpdateStorageResponse updateStorageResponse) {
        return getReadStorageRequests(updateStorageResponse.getUpdatedContentItems()
                .stream()
                .map(ContentItem::getMetacard)
                .collect(Collectors.toList()));
    }

    private List<ReadStorageRequest> getReadStorageRequests(List<Metacard> metacards) {
        return metacards.stream()
                .map(m -> new ReadStorageRequestImpl(m.getResourceURI(),
                        m.getId(),
                        new HashMap<>()))
                .collect(Collectors.toList());
    }

    private ReadStorageResponse getStorageItem(ReadStorageRequest r) {
        try {
            return storageProvider().read(r);
        } catch (StorageException e) {
            LOGGER.debug("could not get storage item for metacard (id: {})(uri: {})",
                    r.getId(),
                    r.getResourceUri(),
                    e);
        }
        return null;
    }

    private Map<String, List<ContentItem>> getContentItems(DeleteResponse deleteResponse) {
        return getOldContent(getReadStorageRequests(deleteResponse.getDeletedMetacards()));
    }

    private CreateStorageResponse versionContentItems(Map<String, List<ContentItem>> items,
            Map<String, Metacard> versionedMetacards)
            throws SourceUnavailableException, IngestException {
        List<ContentItem> contentItems = items.entrySet()
                .stream()
                .map(e -> getVersionedContentItems(e, versionedMetacards))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        if (contentItems.isEmpty()) {
            LOGGER.debug("No content items to version");
            return null;
        }

        CreateStorageResponse createStorageResponse =
                executeAsSystem(() -> storageProvider().create(new CreateStorageRequestImpl(
                        contentItems,
                        new HashMap<>())));
        tryCommitStorage(createStorageResponse);
        return createStorageResponse;
    }

    private void tryCommitStorage(CreateStorageResponse createStorageResponse)
            throws IngestException {
        try {
            storageProvider().commit(createStorageResponse.getStorageRequest());
        } catch (StorageException e) {
            try {
                storageProvider().rollback(createStorageResponse.getStorageRequest());
            } catch (StorageException e1) {
                LOGGER.debug("Could not rollback storage request", e1);
            }
            LOGGER.debug("Could not copy and store the previous resource", e);
            throw new IngestException("Error Updating Metacard");
        }
    }

    private List<ContentItemImpl> getVersionedContentItems(
            Map.Entry<String, List<ContentItem>> entry, Map<String, Metacard> versionedMetacards) {
        return entry.getValue()
                .stream()
                .map(content -> createContentItem(content, versionedMetacards))
                .collect(Collectors.toList());
    }

    private ContentItemImpl createContentItem(ContentItem content,
            Map<String, Metacard> versionedMetacards) {
        long size = 0;
        try {
            size = content.getSize();
        } catch (IOException e) {
            LOGGER.debug("Could not get size of file. (file: {}) (id: {})",
                    content.getFilename(),
                    content.getId(),
                    e);
        }
        return new ContentItemImpl(versionedMetacards.get(content.getId())
                .getId(),
                content.getQualifier(),
                new WrappedByteSource(content),
                content.getMimeTypeRawData(),
                content.getFilename(),
                size,
                versionedMetacards.get(content.getId()));
    }

    /*Map<MetacardVersion.VERSION_OF_ID -> MetacardVersion>*/
    private Map<String, Metacard> getVersionMetacards(Collection<Metacard> metacards,
            final Action action, Subject subject)
            throws SourceUnavailableException, IngestException {
        return metacards.stream()
                .filter(MetacardVersionImpl::isNotVersion)
                .map(metacard -> new MetacardVersionImpl(metacard, action, subject))
                .collect(Collectors.toMap(MetacardVersionImpl::getVersionOfId,
                        Function.identity()));
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

    private CreateResponse storeVersionMetacards(Map<String, Metacard> versionMetacards) {
        return executeAsSystem(() -> catalogProvider().create(new CreateRequestImpl(new ArrayList<>(
                versionMetacards.values()))));
    }

    private void setResourceUriForContent(/*mutable*/ Map<String, Metacard> versionMetacards,
            CreateStorageResponse createStorageResponse) {
        for (ContentItem contentItem : createStorageResponse.getCreatedContentItems()) {
            Metacard metacard = versionMetacards.values()
                    .stream()
                    .filter(m -> contentItem.getId()
                            .equals(m.getId()))
                    .findFirst()
                    .orElse(null);

            if (metacard == null) {
                LOGGER.info(
                        "Could not find version metacard to set resource URI for (contentItem id: {})",
                        contentItem.getId());
                continue;
            }
            metacard.setAttribute(new AttributeImpl(Metacard.RESOURCE_URI, contentItem.getUri()));
        }

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

    public void setMetacardTypes(List<MetacardType> metacardTypes) {
        this.metacardTypes = metacardTypes;
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
