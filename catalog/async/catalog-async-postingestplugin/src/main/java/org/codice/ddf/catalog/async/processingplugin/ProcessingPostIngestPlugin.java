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
package org.codice.ddf.catalog.async.processingplugin;

import static org.apache.commons.lang.Validate.notNull;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.Response;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.codice.ddf.catalog.async.data.api.internal.ProcessCreateItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessDeleteItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessRequest;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResource;
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem;
import org.codice.ddf.catalog.async.data.impl.LazyProcessResourceImpl;
import org.codice.ddf.catalog.async.data.impl.ProcessCreateItemImpl;
import org.codice.ddf.catalog.async.data.impl.ProcessDeleteItemImpl;
import org.codice.ddf.catalog.async.data.impl.ProcessRequestImpl;
import org.codice.ddf.catalog.async.data.impl.ProcessUpdateItemImpl;
import org.codice.ddf.catalog.async.processingframework.api.internal.ProcessingFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code ProcessingPostIngestPlugin} is a {@link PostIngestPlugin} that is responsible for
 * submitting {@link ProcessRequest}s to the {@link ProcessingFramework}.
 */
public class ProcessingPostIngestPlugin implements PostIngestPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingPostIngestPlugin.class);

  private static final String POST_PROCESS_COMPLETE =
      "catalog-async-processing-plugin:post-process-complete";

  private ProcessingFramework processingFramework;

  private CatalogFramework catalogFramework;

  public ProcessingPostIngestPlugin(
      CatalogFramework catalogFramework, ProcessingFramework processingFramework) {
    notNull(catalogFramework, "The catalog framework must not be null");
    notNull(processingFramework, "The processing framework must not be null");

    this.catalogFramework = catalogFramework;
    this.processingFramework = processingFramework;
  }

  @Override
  public CreateResponse process(CreateResponse input) throws PluginExecutionException {
    if (input != null && input.getCreatedMetacards() != null && !isAlreadyPostProcessed(input)) {
      processingFramework.submitCreate(createCreateRequest(input));
    }
    return input;
  }

  @Override
  public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
    if (input != null && input.getUpdatedMetacards() != null && !isAlreadyPostProcessed(input)) {
      processingFramework.submitUpdate(createUpdateRequest(input));
    }
    return input;
  }

  @Override
  public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
    if (input != null && input.getDeletedMetacards() != null && !isAlreadyPostProcessed(input)) {
      processingFramework.submitDelete(createDeleteRequest(input));
    }
    return input;
  }

  private static boolean isAlreadyPostProcessed(Response response) {
    Map<String, Serializable> properties = response.getRequest().getProperties();
    if (properties.containsKey(POST_PROCESS_COMPLETE)) {
      Serializable prop = properties.get(POST_PROCESS_COMPLETE);
      if (prop instanceof Boolean) {
        return (boolean) prop;
      } else {
        LOGGER.debug(
            "{} request property was not a boolean. Clearing the property and returning true. PostProcessingPlugins will not be run as an infinite loop may occur.",
            POST_PROCESS_COMPLETE);
        properties.remove(POST_PROCESS_COMPLETE);
        return true;
      }
    }
    return false;
  }

  private static Map<String, Serializable> putPostProcessCompleteFlagAndGet(
      Map<String, Serializable> properties) {
    Map<String, Serializable> newProperties = new HashMap<>(properties);
    newProperties.put(POST_PROCESS_COMPLETE, true);
    return newProperties;
  }

  private ProcessRequest<ProcessCreateItem> createCreateRequest(CreateResponse createResponse) {
    List<ProcessCreateItem> processCreateItems;

    processCreateItems =
        createResponse
            .getCreatedMetacards()
            .stream()
            .map(
                metacard ->
                    new ProcessCreateItemImpl(
                        getProcessResource(metacard, getSubject(createResponse)), metacard, false))
            .collect(Collectors.toList());

    return new ProcessRequestImpl(
        processCreateItems, putPostProcessCompleteFlagAndGet(createResponse.getProperties()));
  }

  private ProcessRequest<ProcessUpdateItem> createUpdateRequest(UpdateResponse updateResponse) {
    List<Update> updates = updateResponse.getUpdatedMetacards();
    List<ProcessUpdateItem> processUpdateItems = new ArrayList<>();

    for (Update update : updates) {
      Metacard oldCard = update.getOldMetacard();
      Metacard newCard = update.getNewMetacard();
      ProcessUpdateItem processItem =
          new ProcessUpdateItemImpl(
              getProcessResource(newCard, getSubject(updateResponse)), newCard, oldCard, false);
      processUpdateItems.add(processItem);
    }

    return new ProcessRequestImpl(
        processUpdateItems, putPostProcessCompleteFlagAndGet(updateResponse.getProperties()));
  }

  private Subject getSubject(Response response) {
    return (Subject) response.getProperties().get(SecurityConstants.SECURITY_SUBJECT);
  }

  private ProcessRequest<ProcessDeleteItem> createDeleteRequest(DeleteResponse deleteResponse) {
    List<ProcessDeleteItem> processDeleteItems =
        deleteResponse
            .getDeletedMetacards()
            .stream()
            .map(ProcessDeleteItemImpl::new)
            .collect(Collectors.toList());

    return new ProcessRequestImpl(
        processDeleteItems, putPostProcessCompleteFlagAndGet(deleteResponse.getProperties()));
  }

  private ProcessResource getProcessResource(Metacard metacard, Subject subject) {
    if (subject == null) {
      LOGGER.debug("No available subject to fetch metacard resource. Returning null");
      return null;
    }

    Supplier<Resource> resourceSupplier = getResourceSupplier(metacard, subject);

    LazyProcessResourceImpl lazyProcessResource =
        new LazyProcessResourceImpl(metacard.getId(), resourceSupplier);

    populateProcessResourceFromMetacard(lazyProcessResource, metacard);

    return lazyProcessResource;
  }

  private Supplier<Resource> getResourceSupplier(Metacard metacard, Subject subject) {
    return () ->
        subject.execute(
            () -> {
              LOGGER.trace(
                  "Attempting to retrieve process resource metacard with id \"{}\" and sourceId \"{}\".",
                  metacard.getId(),
                  metacard.getSourceId());

              ResourceRequest request = new ResourceRequestById(metacard.getId());
              try {
                ResourceResponse response =
                    catalogFramework.getResource(request, metacard.getSourceId());

                return response.getResource();

              } catch (IOException
                  | ResourceNotFoundException
                  | ResourceNotSupportedException
                  | RuntimeException e) {
                LOGGER.debug(
                    "Unable to get resource id:{}, sourceId:{}. Returning null",
                    metacard.getId(),
                    metacard.getSourceId(),
                    e);
              }
              return null;
            });
  }

  private void populateProcessResourceFromMetacard(
      LazyProcessResourceImpl processResource, Metacard metacard) {
    String value = getMetacardStringValue(metacard, Core.RESOURCE_SIZE);
    if (StringUtils.isNotBlank(value) && NumberUtils.isNumber(value)) {
      processResource.setSize(NumberUtils.toLong(value));
    }

    value = getMetacardStringValue(metacard, Core.RESOURCE_URI);
    if (StringUtils.isNotBlank(value)) {
      try {
        URI uri = new URI(value);
        processResource.setUri(uri);
      } catch (URISyntaxException e) {
        LOGGER.debug("Error creating URI from string: {}. Caught an exception: {}", value, e);
      }
    }
  }

  private String getMetacardStringValue(Metacard metacard, String attributeName) {
    Attribute attribute = metacard.getAttribute(attributeName);
    if (attribute == null) {
      return null;
    }

    return Stream.of(attribute.getValue())
        .filter(Objects::nonNull)
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .findFirst()
        .orElse(null);
  }
}
