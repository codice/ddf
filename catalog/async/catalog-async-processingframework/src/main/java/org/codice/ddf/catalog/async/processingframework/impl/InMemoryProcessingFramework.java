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
package org.codice.ddf.catalog.async.processingframework.impl;

import static org.apache.commons.lang3.Validate.notNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.async.data.api.internal.ProcessCreateItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessDeleteItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessRequest;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResource;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResourceItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem;
import org.codice.ddf.catalog.async.plugin.api.internal.PostProcessPlugin;
import org.codice.ddf.catalog.async.processingframework.api.internal.ProcessingFramework;
import org.codice.ddf.security.common.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.impl.UpdateStorageRequestImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;

/**
 * The {@code InMemoryProcessingFramework} processes requests using a thread pool and submits the
 * results back to the {@link CatalogFramework} after processing is complete, when appropriate. If no changes
 * are detected after processing, no results will be sent back to the {@link CatalogFramework}. Currently,
 * partial updates are not supported, meaning that any results being returned by the {@link InMemoryProcessingFramework}
 * will override the metadata in the catalog.
 */
public class InMemoryProcessingFramework implements ProcessingFramework {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryProcessingFramework.class);

    private final CatalogFramework catalogFramework;

    private final ExecutorService threadPool;

    private List<PostProcessPlugin> postProcessPlugins;

    public InMemoryProcessingFramework(CatalogFramework catalogFramework,
            ExecutorService threadPool) {
        notNull(catalogFramework, "The catalog framework must not be null");
        notNull(threadPool, "The threadPool must not be null");

        this.catalogFramework = catalogFramework;
        this.threadPool = threadPool;
    }

    public void cleanUp() {
        LOGGER.debug("Stopping PostProcessPlugin thread pool.");
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(600, TimeUnit.SECONDS)) {
                    LOGGER.debug(
                            "Some asynchronous plugins did not finish running. Waited 10 minutes. Metacards may not be in intended state. Attempting to shutdown now.");
                    threadPool.shutdownNow();

                    if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                        LOGGER.debug(
                                "InMemoryProcessingFramework asynchronous processing did not terminate.");
                    }
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread()
                        .interrupt();

                LOGGER.debug(
                        "Interrupted while shutting down InMemoryProcessingFramework asynchronous ThreadPool.");
            }
        } else {
            LOGGER.debug("InMemoryProcessingFramework asynchronous ThreadPool already shutdown.");
        }
    }

    @Override
    public void submitCreate(ProcessRequest<ProcessCreateItem> input) {
        if (postProcessPlugins == null || postProcessPlugins.isEmpty()) {
            LOGGER.debug("postProcessPlugins is empty. Not starting post process thread");
        } else {
            threadPool.submit(() -> {
                ProcessRequest<ProcessCreateItem> request = input;

                for (PostProcessPlugin plugin : postProcessPlugins) {
                    try {
                        request = plugin.processCreate(request);
                    } catch (PluginExecutionException e) {
                        LOGGER.debug("Unable to process create request through plugin: {}",
                                plugin.getClass()
                                        .getCanonicalName(),
                                e);
                    }
                }

                storeProcessRequest(request);
                closeInputStream(request);
            });
        }
    }

    @Override
    public void submitUpdate(ProcessRequest<ProcessUpdateItem> input) {
        if (postProcessPlugins == null || postProcessPlugins.isEmpty()) {
            LOGGER.debug("postProcessPlugins is empty. Not starting post process thread");
        } else {
            threadPool.submit(() -> {
                ProcessRequest<ProcessUpdateItem> request = input;

                for (PostProcessPlugin plugin : postProcessPlugins) {
                    try {
                        request = plugin.processUpdate(request);
                    } catch (PluginExecutionException e) {
                        LOGGER.debug("Unable to process update request through plugin: {}",
                                plugin.getClass()
                                        .getCanonicalName(),
                                e);
                    }
                }

                storeProcessRequest(request);
                closeInputStream(request);
            });
        }
    }

    @Override
    public void submitDelete(ProcessRequest<ProcessDeleteItem> input) {
        if (postProcessPlugins == null || postProcessPlugins.isEmpty()) {
            LOGGER.debug("postProcessPlugins is empty. Not starting post process thread");
        } else {
            threadPool.submit(() -> {
                ProcessRequest<ProcessDeleteItem> request = input;

                for (PostProcessPlugin plugin : postProcessPlugins) {
                    try {
                        request = plugin.processDelete(request);
                    } catch (PluginExecutionException e) {
                        LOGGER.debug("Unable to process request through plugin: {}",
                                plugin.getClass()
                                        .getCanonicalName(),
                                e);
                    }
                }
            });
        }
    }

    private <T extends ProcessResourceItem> void closeInputStream(ProcessRequest<T> request) {
        request.getProcessItems()
                .stream()
                .map(ProcessResourceItem::getProcessResource)
                .filter(Objects::nonNull)
                .forEach(resource -> {
                    try {
                        resource.getInputStream()
                                .close();
                    } catch (IOException e) {
                        LOGGER.debug(
                                "Failed to close stream of asynchronous request. There may be temporary files left in data/tmp.");
                    }
                });
    }

    private <T extends ProcessResourceItem> void storeProcessRequest(
            ProcessRequest<T> processRequest) {
        LOGGER.trace("Storing update request post processing change(s)");

        Set<ContentItem> contentItemsToUpdate = new HashSet<>();
        Set<Metacard> metacardsToUpdate = new HashSet<>();

        for (T item : processRequest.getProcessItems()) {
            if (item.getProcessResource() == null && item.isMetacardModified()) {
                metacardsToUpdate.add(item.getMetacard());
            }

            final ProcessResource processResource = item.getProcessResource();
            if (processResource != null && processResource.isModified()) {
                try {
                    byte[] byteArray = IOUtils.toByteArray(processResource.getInputStream());
                    ByteSource byteSource = ByteSource.wrap(byteArray);

                    ContentItem contentItem = new ContentItemImpl(item.getMetacard()
                            .getId(),
                            processResource.getQualifier(),
                            byteSource,
                            processResource.getMimeType(),
                            processResource.getName(),
                            processResource.getSize(),
                            item.getMetacard());

                    contentItemsToUpdate.add(contentItem);
                } catch (IOException e) {
                    LOGGER.debug("Unable to store process request", e);
                }
            }
        }

        storeContentItemUpdates(contentItemsToUpdate, processRequest.getProperties());
        storeMetacardUpdates(metacardsToUpdate, processRequest.getProperties());
    }

    private void storeContentItemUpdates(Set<ContentItem> contentItemsToUpdate,
            Map<String, Serializable> properties) {
        if (CollectionUtils.isNotEmpty(contentItemsToUpdate)) {
            LOGGER.trace("Storing content item updates(s)");

            UpdateStorageRequest updateStorageRequest =
                    new UpdateStorageRequestImpl(new ArrayList<>(contentItemsToUpdate), properties);

            Security.runAsAdmin(() -> {
                try {
                    catalogFramework.update(updateStorageRequest);
                    LOGGER.debug("Successfully completed update storage request");
                } catch (IngestException | SourceUnavailableException e) {
                    LOGGER.debug("Unable to complete update storage request", e);
                }

                return null;
            });
        } else {
            LOGGER.debug("No content items to update");
        }
    }

    private void storeMetacardUpdates(Set<Metacard> metacardsToUpdate,
            Map<String, Serializable> properties) {
        if (CollectionUtils.isNotEmpty(metacardsToUpdate)) {
            LOGGER.trace("Storing metacard updates");

            List<Map.Entry<Serializable, Metacard>> updateList = metacardsToUpdate.stream()
                    .map(metacard -> new AbstractMap.SimpleEntry<Serializable, Metacard>(metacard.getId(),
                            metacard))
                    .collect(Collectors.toList());

            UpdateRequest updateMetacardsRequest = new UpdateRequestImpl(updateList,
                    UpdateRequest.UPDATE_BY_ID,
                    properties);

            Security.runAsAdmin(() -> {
                try {
                    catalogFramework.update(updateMetacardsRequest);
                    LOGGER.debug("Successfully completed update metacards request");
                } catch (IngestException | SourceUnavailableException e) {
                    LOGGER.debug("Unable to complete update storage request", e);
                }

                return null;
            });
        } else {
            LOGGER.debug("No metacards to update");
        }
    }

    public void setPostProcessPlugins(List<PostProcessPlugin> postProcessPlugins) {
        this.postProcessPlugins = postProcessPlugins;
    }
}