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
package org.codice.ddf.catalog.async.processingplugin;

import static org.apache.commons.lang3.Validate.notNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.catalog.async.data.ProcessCreateItemImpl;
import org.codice.ddf.catalog.async.data.ProcessDeleteItemImpl;
import org.codice.ddf.catalog.async.data.ProcessRequestImpl;
import org.codice.ddf.catalog.async.data.ProcessResourceImpl;
import org.codice.ddf.catalog.async.data.ProcessUpdateItemImpl;
import org.codice.ddf.catalog.async.data.api.internal.ProcessCreateItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessDeleteItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessRequest;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResource;
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem;
import org.codice.ddf.catalog.async.processingframework.api.internal.ProcessingFramework;
import org.codice.ddf.security.common.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
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

/**
 * The {@code ProcessingPostIngestPlugin} is a {@link PostIngestPlugin} that is responsible for
 * submitting {@link ProcessRequest}s to the {@link ProcessingFramework}.
 */
public class ProcessingPostIngestPlugin implements PostIngestPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingPostIngestPlugin.class);

    private static final String POST_PROCESS_COMPLETE = "post-process-complete";

    private ProcessingFramework processingFramework;

    private CatalogFramework catalogFramework;

    public ProcessingPostIngestPlugin(CatalogFramework catalogFramework,
            ProcessingFramework processingFramework) {
        notNull(catalogFramework, "The catalog framework must not be null");
        notNull(processingFramework, "The processing framework must not be null");

        this.catalogFramework = catalogFramework;
        this.processingFramework = processingFramework;
    }

    private static boolean isAlreadyPostProcessed(Response response) {
        Map<String, Serializable> properties = response.getRequest()
                .getProperties();
        if (properties.containsKey(POST_PROCESS_COMPLETE)) {
            Serializable prop = properties.get(POST_PROCESS_COMPLETE);
            if (prop instanceof Boolean) {
                return (boolean) prop;
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

    @Override
    public CreateResponse process(CreateResponse input) throws PluginExecutionException {
        if (input != null && input.getCreatedMetacards() != null
                && !isAlreadyPostProcessed(input)) {
            processingFramework.submitCreate(createCreateRequest(input));
        }
        return input;
    }

    @Override
    public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
        if (input != null && input.getUpdatedMetacards() != null
                && !isAlreadyPostProcessed(input)) {
            processingFramework.submitUpdate(createUpdateRequest(input));
        }
        return input;
    }

    @Override
    public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
        if (input != null && input.getDeletedMetacards() != null
                && !isAlreadyPostProcessed(input)) {
            processingFramework.submitDelete(createDeleteRequest(input));
        }
        return input;
    }

    private ProcessRequest<ProcessCreateItem> createCreateRequest(CreateResponse createResponse) {
        List<ProcessCreateItem> processCreateItems = new ArrayList<>();

        for (Metacard metacard : createResponse.getCreatedMetacards()) {
            ProcessCreateItem processCreateItem = new ProcessCreateItemImpl(getProcessResource(
                    metacard), metacard, false);
            processCreateItems.add(processCreateItem);
        }
        return new ProcessRequestImpl(processCreateItems,
                putPostProcessCompleteFlagAndGet(createResponse.getProperties()));
    }

    private ProcessRequest<ProcessUpdateItem> createUpdateRequest(UpdateResponse updateResponse) {
        List<Update> updates = updateResponse.getUpdatedMetacards();
        List<ProcessUpdateItem> processUpdateItems = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(updates)) {
            for (Update update : updates) {
                Metacard oldCard = update.getOldMetacard();
                Metacard newCard = update.getNewMetacard();
                ProcessUpdateItem processItem =
                        new ProcessUpdateItemImpl(getProcessResource(newCard),
                                newCard,
                                oldCard,
                                false);
                processUpdateItems.add(processItem);
            }
        }

        return new ProcessRequestImpl(processUpdateItems,
                putPostProcessCompleteFlagAndGet(updateResponse.getProperties()));
    }

    private ProcessRequest<ProcessDeleteItem> createDeleteRequest(DeleteResponse deleteResponse) {
        List<ProcessDeleteItem> processDeleteItems = new ArrayList<>();

        for (Metacard metacard : deleteResponse.getDeletedMetacards()) {
            ProcessDeleteItem processDeleteItem = new ProcessDeleteItemImpl(metacard);
            processDeleteItems.add(processDeleteItem);
        }

        return new ProcessRequestImpl(processDeleteItems,
                putPostProcessCompleteFlagAndGet(deleteResponse.getProperties()));
    }

    private ProcessResource getProcessResource(Metacard metacard) {
        LOGGER.trace(
                "Getting process resource from catalog framework for metacard with id:{}, sourceId:{}.",
                metacard.getId(),
                metacard.getSourceId());

        ResourceRequest request = new ResourceRequestById(metacard.getId());

        return Security.runAsAdmin(() -> {
            try {
                ResourceResponse response = catalogFramework.getResource(request,
                        metacard.getSourceId());
                Resource resource = response.getResource();

                ProcessResource processResource = new ProcessResourceImpl(metacard.getId(),
                        resource.getInputStream(),
                        resource.getMimeTypeValue(),
                        resource.getName(),
                        resource.getSize(),
                        false);

                return processResource;
            } catch (IOException | ResourceNotFoundException | ResourceNotSupportedException e) {
                LOGGER.debug("Unable to get resource id:{}, sourceId:{}.",
                        metacard.getId(),
                        metacard.getSourceId(),
                        e);
            }
            return null;
        });
    }
}
