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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.history.Historian;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.util.impl.Requests;

/**
 * Support class for working with {@code StorageRequest}s for the {@code CatalogFrameworkImpl}.
 * <p>
 * This class contains methods for management/manipulation for storage requests for the CFI and its
 * support classes. No operations/support methods should be added to this class except in support
 * of CFI, specific to storage requests.
 */
public class OperationsStorageSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsStorageSupport.class);

    private static final Logger INGEST_LOGGER =
            LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

    //
    // Injected propertiers
    //
    private final SourceOperations sourceOperations;

    private final QueryOperations queryOperations;

    private Historian historian;

    public OperationsStorageSupport(SourceOperations sourceOperations,
            QueryOperations queryOperations) {
        this.sourceOperations = sourceOperations;
        this.queryOperations = queryOperations;
    }

    public void setHistorian(Historian historian) {
        this.historian = historian;
    }

    <T extends StorageRequest> T prepareStorageRequest(T storageRequest,
            Supplier<List<ContentItem>> getContentItems)
            throws IngestException, SourceUnavailableException {
        validateStorageRequest(storageRequest, getContentItems);

        queryOperations.setFlagsOnRequest(storageRequest);

        if (Requests.isLocal(storageRequest) && (!sourceOperations.isSourceAvailable(
                sourceOperations.getCatalog())
                || !isStorageAvailable(sourceOperations.getStorage()))) {
            String message = "Local provider is not available, cannot perform storage operation.";
            SourceUnavailableException sourceUnavailableException = new SourceUnavailableException(
                    message);
            if (INGEST_LOGGER.isWarnEnabled()) {
                INGEST_LOGGER.warn(message, sourceUnavailableException);
            }
            throw sourceUnavailableException;
        }

        return storageRequest;
    }

    void commitAndCleanup(StorageRequest storageRequest, HashMap<String, Path> tmpContentPaths) {
        if (storageRequest != null) {
            try {
                sourceOperations.getStorage()
                        .commit(storageRequest);
            } catch (StorageException e) {
                LOGGER.info("Unable to commit content changes for id: {}",
                        storageRequest.getId(),
                        e);
                rollbackStorage(storageRequest);
            }
        }

        tmpContentPaths.values()
                .forEach(path -> FileUtils.deleteQuietly(path.toFile()));
        tmpContentPaths.clear();
    }

    private void rollbackStorage(StorageRequest storageRequest) {
        try {
            sourceOperations.getStorage()
                    .rollback(storageRequest);
        } catch (StorageException e1) {
            LOGGER.info("Unable to remove temporary content for id: {}",
                    storageRequest.getId(),
                    e1);
        }
    }

    /**
     * Validates that the {@link StorageRequest} is non-null and has a non-empty list of
     * {@link ContentItem}s in it.
     *
     * @param request the {@link StorageRequest}
     * @throws IngestException if the {@link StorageRequest} is null, or request has a null or empty list of
     *                         {@link ContentItem}s
     */
    private void validateStorageRequest(StorageRequest request,
            Supplier<List<ContentItem>> getContentItems) throws IngestException {
        if (request == null) {
            throw new IngestException("StorageRequest was null.");
        }
        List<ContentItem> contentItems = getContentItems.get();
        if (CollectionUtils.isEmpty(contentItems)) {
            throw new IngestException("Cannot perform ingest or update with null/empty entry list.");
        }
    }

    private boolean isStorageAvailable(StorageProvider storageProvider) {
        if (storageProvider == null) {
            LOGGER.warn("storageProvider is null, therefore not available");
            return false;
        }
        return true;
    }
}
