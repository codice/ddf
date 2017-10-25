/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.async.processingframework.impl;

import static org.apache.commons.lang3.Validate.notNull;

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
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.async.data.api.internal.InaccessibleResourceException;
import org.codice.ddf.catalog.async.data.api.internal.ProcessCreateItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessDeleteItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessRequest;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResource;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResourceItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem;
import org.codice.ddf.catalog.async.plugin.api.internal.PostProcessPlugin;
import org.codice.ddf.catalog.async.processingframework.api.internal.ProcessingFramework;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code InMemoryProcessingFramework} processes requests using a thread pool and submits the
 * results back to the {@link CatalogFramework} after processing is complete, when appropriate. If
 * no changes are detected after processing, no results will be sent back to the {@link
 * CatalogFramework}. Currently, partial updates are not supported, meaning that any results being
 * returned by the {@link InMemoryProcessingFramework} will override the metadata in the catalog.
 */
public class InMemoryProcessingFramework implements ProcessingFramework {

  private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryProcessingFramework.class);

  private final CatalogFramework catalogFramework;

  private final ExecutorService threadPool;

  private List<PostProcessPlugin> postProcessPlugins;

  public InMemoryProcessingFramework(
      CatalogFramework catalogFramework, ExecutorService threadPool) {
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
            LOGGER.debug("InMemoryProcessingFramework asynchronous processing did not terminate.");
          }
        }
      } catch (InterruptedException e) {
        threadPool.shutdownNow();
        Thread.currentThread().interrupt();

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
      threadPool.submit(
          () -> {
            ProcessRequest<ProcessCreateItem> request = input;

            for (PostProcessPlugin plugin : postProcessPlugins) {
              try {
                request = plugin.processCreate(request);
              } catch (PluginExecutionException e) {
                LOGGER.debug(
                    "Unable to process create request through plugin: {}",
                    plugin.getClass().getCanonicalName(),
                    e);
              } catch (InaccessibleResourceException e) {
                LOGGER.debug(
                    "Unable to process create request. The resource is not available. Failing the entire process request.",
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
      threadPool.submit(
          () -> {
            ProcessRequest<ProcessUpdateItem> request = input;

            for (PostProcessPlugin plugin : postProcessPlugins) {
              try {
                request = plugin.processUpdate(request);
              } catch (PluginExecutionException e) {
                LOGGER.debug(
                    "Unable to process update request through plugin: {}",
                    plugin.getClass().getCanonicalName(),
                    e);
              } catch (InaccessibleResourceException e) {
                LOGGER.debug(
                    "Unable to process update request. The resource is not available. Failing the entire process request.",
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
      threadPool.submit(
          () -> {
            ProcessRequest<ProcessDeleteItem> request = input;

            for (PostProcessPlugin plugin : postProcessPlugins) {
              try {
                request = plugin.processDelete(request);
              } catch (PluginExecutionException e) {
                LOGGER.debug(
                    "Unable to process request through plugin: {}",
                    plugin.getClass().getCanonicalName(),
                    e);
              } catch (InaccessibleResourceException e) {
                LOGGER.debug(
                    "Unable to process delete request. The resource is not available. Failing the entire process request.",
                    e);
              }
            }
          });
    }
  }

  private <T extends ProcessResourceItem> void closeInputStream(ProcessRequest<T> request) {
    request
        .getProcessItems()
        .stream()
        .map(ProcessResourceItem::getProcessResource)
        .filter(Objects::nonNull)
        .forEach(ProcessResource::close);
  }

  private <T extends ProcessResourceItem> void storeProcessRequest(
      ProcessRequest<T> processRequest) {
    LOGGER.trace("Storing update request post processing change(s)");

    Map<String, ContentItem> contentItemsToUpdate = new HashMap<>();
    Map<String, Metacard> metacardsToUpdate = new HashMap<>();
    List<TemporaryFileBackedOutputStream> tfbosToCleanUp = new ArrayList<>();

    for (T item : processRequest.getProcessItems()) {
      final ProcessResource processResource = item.getProcessResource();
      if ((processResource == null || !processResource.isModified()) && item.isMetacardModified()) {
        metacardsToUpdate.put(item.getMetacard().getId(), item.getMetacard());
      }

      TemporaryFileBackedOutputStream tfbos = null;
      if (processResource != null
          && processResource.isModified()
          && !contentItemsToUpdate.containsKey(
              getContentItemKey(item.getMetacard(), processResource))) {
        try {
          tfbos = new TemporaryFileBackedOutputStream();
          long numberOfBytes = IOUtils.copyLarge(processResource.getInputStream(), tfbos);
          LOGGER.debug("Copied {} bytes to TemporaryFileBackedOutputStream.", numberOfBytes);
          ByteSource byteSource = tfbos.asByteSource();

          ContentItem contentItem =
              new ContentItemImpl(
                  item.getMetacard().getId(),
                  processResource.getQualifier(),
                  byteSource,
                  processResource.getMimeType(),
                  processResource.getName(),
                  processResource.getSize(),
                  item.getMetacard());

          contentItemsToUpdate.put(
              getContentItemKey(item.getMetacard(), processResource), contentItem);
          tfbosToCleanUp.add(tfbos);
        } catch (IOException | RuntimeException e) {
          LOGGER.debug("Unable to store process request", e);
          if (tfbos != null) {
            close(tfbos);
          }
        }
      }
    }

    storeContentItemUpdates(contentItemsToUpdate, processRequest.getProperties());
    storeMetacardUpdates(metacardsToUpdate, processRequest.getProperties());
    closeTfbos(tfbosToCleanUp);
  }

  private void storeContentItemUpdates(
      Map<String, ContentItem> contentItemsToUpdate, Map<String, Serializable> properties) {
    if (MapUtils.isNotEmpty(contentItemsToUpdate)) {
      LOGGER.trace("Storing content item updates(s)");

      UpdateStorageRequest updateStorageRequest =
          new UpdateStorageRequestImpl(new ArrayList<>(contentItemsToUpdate.values()), properties);

      Subject subject =
          (Subject) updateStorageRequest.getProperties().get(SecurityConstants.SECURITY_SUBJECT);
      if (subject == null) {
        LOGGER.debug(
            "No subject to send UpdateStorageRequest. Updates will not be sent back to the catalog");
      } else {
        subject.execute(
            () -> {
              try {
                catalogFramework.update(updateStorageRequest);
                LOGGER.debug("Successfully completed update storage request");
              } catch (IngestException | SourceUnavailableException | RuntimeException e) {
                LOGGER.info("Unable to complete update storage request", e);
              }

              return null;
            });
      }
    } else {
      LOGGER.debug("No content items to update");
    }
  }

  private void storeMetacardUpdates(
      Map<String, Metacard> metacardsToUpdate, Map<String, Serializable> properties) {
    if (MapUtils.isNotEmpty(metacardsToUpdate)) {
      LOGGER.trace("Storing metacard updates");

      List<Map.Entry<Serializable, Metacard>> updateList =
          metacardsToUpdate
              .values()
              .stream()
              .map(
                  metacard ->
                      new AbstractMap.SimpleEntry<Serializable, Metacard>(
                          metacard.getId(), metacard))
              .collect(Collectors.toList());

      UpdateRequest updateMetacardsRequest =
          new UpdateRequestImpl(updateList, UpdateRequest.UPDATE_BY_ID, properties);

      Subject subject =
          (Subject) updateMetacardsRequest.getProperties().get(SecurityConstants.SECURITY_SUBJECT);

      if (subject == null) {
        LOGGER.debug(
            "No subject to send UpdateRequest. Updates will not be sent back to the catalog.");
      } else {
        subject.execute(
            () -> {
              try {
                catalogFramework.update(updateMetacardsRequest);
                LOGGER.debug("Successfully completed update metacards request");
              } catch (IngestException | SourceUnavailableException | RuntimeException e) {
                LOGGER.info("Unable to complete update request", e);
              }

              return null;
            });
      }
    } else {
      LOGGER.debug("No metacards to update");
    }
  }

  private void closeTfbos(List<TemporaryFileBackedOutputStream> tfbosToCleanUp) {
    tfbosToCleanUp.forEach(this::close);
  }

  private void close(TemporaryFileBackedOutputStream tfbos) {
    try {
      tfbos.close();
    } catch (IOException e) {
      LOGGER.debug("Failed to cleanup temporary file.");
    }
  }

  private String getContentItemKey(Metacard metacard, ProcessResource processResource) {
    return metacard.getId() + processResource.getQualifier();
  }

  public void setPostProcessPlugins(List<PostProcessPlugin> postProcessPlugins) {
    this.postProcessPlugins = postProcessPlugins;
  }
}
