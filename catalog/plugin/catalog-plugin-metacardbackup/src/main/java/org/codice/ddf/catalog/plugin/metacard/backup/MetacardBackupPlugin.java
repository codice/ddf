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
package org.codice.ddf.catalog.plugin.metacard.backup;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.catalog.async.data.api.internal.ProcessCreateItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessDeleteItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessRequest;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResourceItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem;
import org.codice.ddf.catalog.async.plugin.api.internal.PostProcessPlugin;
import org.codice.ddf.catalog.plugin.metacard.backup.storage.api.MetacardBackupException;
import org.codice.ddf.catalog.plugin.metacard.backup.storage.api.MetacardBackupStorageProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;

/**
 * The MetacardBackupPlugin asynchronously backs up a Metacard using a configured transformer to the file system.
 * It implements the PostProcessPlugin in order to maintain synchronization with the catalog (CRUD).
 * <p>
 * The root backup directory can be configured in the MetacardBackupPlugin section in the admin console.
 */

public class MetacardBackupPlugin implements PostProcessPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardBackupPlugin.class);

    private static final String KEEP_DELETED_METACARDS_PROPERTY = "keepDeletedMetacards";

    private static final String METACARD_TRANSFORMER_ID_PROPERTY = "metacardTransformerId";

    private Boolean keepDeletedMetacards = false;

    private String metacardTransformerId;

    private MetacardTransformer metacardTransformer;

    private List<MetacardBackupStorageProvider> storageBackupPlugins = Collections.emptyList();

    @Override
    public ProcessRequest<ProcessCreateItem> processCreate(
            ProcessRequest<ProcessCreateItem> processRequest) throws PluginExecutionException {
        processRequest(processRequest);
        return processRequest;
    }

    @Override
    public ProcessRequest<ProcessUpdateItem> processUpdate(
            ProcessRequest<ProcessUpdateItem> processRequest) throws PluginExecutionException {
        processRequest(processRequest);
        return processRequest;
    }

    @Override
    public ProcessRequest<ProcessDeleteItem> processDelete(
            ProcessRequest<ProcessDeleteItem> processRequest) throws PluginExecutionException {
        if (keepDeletedMetacards) {
            return processRequest;
        }

        if (CollectionUtils.isEmpty(storageBackupPlugins)) {
            throw new PluginExecutionException(
                    "Unable to delete backup ingested metacard; no metacard backup storage provider configured.");
        }

        List<ProcessDeleteItem> processUpdateItems = processRequest.getProcessItems();
        for (ProcessDeleteItem processUpdateItem : processUpdateItems) {
            Metacard metacard = processUpdateItem.getMetacard();
            try {
                for (MetacardBackupStorageProvider storageProvider : storageBackupPlugins) {
                    storageProvider.deleteData(metacard.getId());
                }
            } catch (IOException | MetacardBackupException e) {
                LOGGER.debug("Unable to delete backed up metacard data for metacard: {}",
                        metacard.getId(),
                        e);
            }
        }

        return processRequest;
    }

    public void setKeepDeletedMetacards(Boolean keepDeletedMetacards) {
        this.keepDeletedMetacards = keepDeletedMetacards;
    }

    public Boolean getKeepDeletedMetacards() {
        return keepDeletedMetacards;
    }

    public void setMetacardTransformerId(String metacardTransformerId) {
        this.metacardTransformerId = metacardTransformerId;
        this.metacardTransformer = lookupTransformerReference();
    }

    public void setMetacardTransformer(MetacardTransformer metacardTransformer) {
        this.metacardTransformer = metacardTransformer;
    }

    public String getMetacardTransformerId() {
        return metacardTransformerId;
    }

    public void setStorageBackupPlugins(List<MetacardBackupStorageProvider> storageBackupPlugins) {
        if (storageBackupPlugins != null) {
            this.storageBackupPlugins = storageBackupPlugins;
        } else {
            this.storageBackupPlugins = Collections.emptyList();
        }

    }

    public List<MetacardBackupStorageProvider> getStorageBackupPlugins() {
        return storageBackupPlugins;
    }

    public void refresh(Map<String, Object> properties) {
        Object metacardTransformerProperty = properties.get(METACARD_TRANSFORMER_ID_PROPERTY);
        if (metacardTransformerProperty instanceof String
                && StringUtils.isNotBlank((String) metacardTransformerProperty)) {
            setMetacardTransformerId((String) metacardTransformerProperty);
            LOGGER.debug("Updating {} with {}",
                    METACARD_TRANSFORMER_ID_PROPERTY,
                    metacardTransformerProperty);
        }

        Object keepDeletedMetacards = properties.get(KEEP_DELETED_METACARDS_PROPERTY);
        if (keepDeletedMetacards instanceof Boolean) {
            this.keepDeletedMetacards = (Boolean) keepDeletedMetacards;
            LOGGER.debug("Updating {} with {}",
                    KEEP_DELETED_METACARDS_PROPERTY,
                    keepDeletedMetacards);
        }
    }

    private void processRequest(ProcessRequest<? extends ProcessResourceItem> processRequest)
            throws PluginExecutionException {
        LOGGER.trace("Backing up metacard");
        if (CollectionUtils.isEmpty(storageBackupPlugins)) {
            throw new PluginExecutionException(
                    "Unable to backup ingested metacard; no metacard backup storage provider configured.");
        }

        if (metacardTransformer == null) {
            throw new PluginExecutionException(
                    "Unable to backup ingested metacard; no Metacard Transformer found.");
        }

        List<? extends ProcessResourceItem> processResourceItems = processRequest.getProcessItems();
        for (ProcessResourceItem processResourceItem : processResourceItems) {
            Metacard metacard = processResourceItem.getMetacard();
            try {
                LOGGER.trace("Backing up metacard : {}", metacard.getId());
                BinaryContent binaryContent = metacardTransformer.transform(metacard,
                        Collections.emptyMap());
                backupData(binaryContent, metacard.getId());
            } catch (CatalogTransformerException e) {
                LOGGER.debug("Unable to transform metacard with id {}.", metacard.getId(), e);
                throw new PluginExecutionException(String.format(
                        "Unable to transform metacard with id %s.",
                        metacard.getId()));
            }
        }
    }

    private void backupData(BinaryContent content, String metacardId)
            throws PluginExecutionException {
        byte[] contentBytes = getContentBytes(content, metacardId);

        LOGGER.trace("Writing backup from {} to backup provider", metacardId);

        try {
            for (MetacardBackupStorageProvider storageProvider : storageBackupPlugins) {
                storageProvider.storeData(metacardId, contentBytes);
            }
        } catch (IOException | MetacardBackupException e) {
            LOGGER.warn("Unable to backup {} to backup provider.", metacardId, e);
        }

    }

    byte[] getContentBytes(BinaryContent content, String metacardId)
            throws PluginExecutionException {
        return Optional.ofNullable(content)
                .map(c -> {
                    try {
                        return c.getByteArray();
                    } catch (IOException e) {
                        return null;
                    }
                })
                .orElseThrow(() -> {
                    LOGGER.debug("No content for transformed metacard {}", metacardId);
                    return new PluginExecutionException(String.format(
                            "No content for transformed metacard %s",
                            metacardId));
                });
    }

    private MetacardTransformer lookupTransformerReference() {
        Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        if (bundle != null) {
            BundleContext bundleContext = bundle.getBundleContext();
            try {
                Collection<ServiceReference<MetacardTransformer>> transformerReference =
                        bundleContext.getServiceReferences(MetacardTransformer.class,
                                "(id=" + metacardTransformerId + ")");
                return bundleContext.getService(transformerReference.iterator()
                        .next());
            } catch (InvalidSyntaxException | NoSuchElementException e) {
                LOGGER.warn(
                        "Unable to resolve MetacardTransformer {}.  Backup will not be performed.",
                        metacardTransformerId,
                        e);
            }
        }
        return null;
    }
}
