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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.catalog.async.data.api.internal.ProcessCreateItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessDeleteItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessRequest;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResourceItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem;
import org.codice.ddf.catalog.async.plugin.api.internal.PostProcessPlugin;
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

    private static final String OUTPUT_DIRECTORY_PROPERTY = "outputDirectory";

    private static final String KEEP_DELETED_METACARDS_PROPERTY = "keepDeletedMetacards";

    private static final String METACARD_TRANSFORMER_ID_PROPERTY = "metacardTransformerId";

    private Boolean keepDeletedMetacards = false;

    private String metacardTransformerId;

    private MetacardTransformer metacardTransformer;

    private String outputDirectory;

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

        if (StringUtils.isEmpty(outputDirectory)) {
            throw new PluginExecutionException(
                    "Unable to delete backup ingested metacard; no output directory specified.");
        }

        List<ProcessDeleteItem> processUpdateItems = processRequest.getProcessItems();
        for (ProcessDeleteItem processUpdateItem : processUpdateItems) {
            Metacard metacard = processUpdateItem.getMetacard();
            deleteBackupIfPresent(metacard.getId());
        }

        return processRequest;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
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

    public void refresh(Map<String, Object> properties) {
        Object outputDirectory = properties.get(OUTPUT_DIRECTORY_PROPERTY);
        if (outputDirectory instanceof String && StringUtils.isNotBlank((String) outputDirectory)) {
            this.outputDirectory = (String) outputDirectory;
            LOGGER.debug("Updating {} with {}", OUTPUT_DIRECTORY_PROPERTY, outputDirectory);
        }

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
        if (StringUtils.isEmpty(outputDirectory)) {
            throw new PluginExecutionException(
                    "Unable to backup ingested metacard; no outputDirectory.");
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
                BinaryContent binaryContent = metacardTransformer.transform(metacard, null);
                copyBackupToOutputDirectory(binaryContent, metacard.getId());
            } catch (CatalogTransformerException e) {
                LOGGER.debug("Unable to transform metacard with id {}.", metacard.getId(), e);
                throw new PluginExecutionException(String.format(
                        "Unable to transform metacard with id %s.",
                        metacard.getId()));
            }
        }
    }

    private void copyBackupToOutputDirectory(BinaryContent content, String metacardId)
            throws PluginExecutionException {
        if (content == null || content.getInputStream() == null) {
            LOGGER.debug("No content for transformed metacard {}.", metacardId);
            throw new PluginExecutionException(String.format(
                    "No content for transformed metacard %s",
                    metacardId));
        }

        Path metacardPath = getMetacardDirectory(metacardId);
        if (metacardPath == null) {
            throw new PluginExecutionException(String.format(
                    "Unable to create metacard path directory for %s",
                    metacardId));
        }

        try {
            Path parent = metacardPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.createFile(metacardPath);
        } catch (IOException e) {
            LOGGER.debug("Unable to create backup file {}.  File may already exist.",
                    metacardPath,
                    e);
        }

        LOGGER.trace("Writing backup from {} to file {}", metacardId, metacardPath.toString());

        try (OutputStream outputStream = new FileOutputStream(metacardPath.toFile())) {
            IOUtils.write(content.getByteArray(), outputStream);
        } catch (IOException e) {
            LOGGER.warn("Unable to backup {} to {}.  The directory may be full.",
                    metacardId,
                    metacardPath.toString(),
                    e);
        }
    }

    private void deleteBackupIfPresent(String filename) throws PluginExecutionException {
        Path metacardPath = getMetacardDirectory(filename);
        if (metacardPath == null) {
            throw new PluginExecutionException(String.format("Unable to delete backup for  %s",
                    filename));
        }

        try {
            Files.deleteIfExists(metacardPath);
            while (metacardPath.getParent() != null && !metacardPath.getParent()
                    .toString()
                    .equals(outputDirectory)) {
                metacardPath = metacardPath.getParent();
                if (isDirectoryEmpty(metacardPath)) {
                    FileUtils.deleteDirectory(metacardPath.toFile());
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Unable to delete backup file {}", metacardPath, e);
            throw new PluginExecutionException(String.format(
                    "Unable to delete backup file for  %s",
                    filename));
        }
    }

    private boolean isDirectoryEmpty(Path dir) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator()
                    .hasNext();
        } catch (IOException e) {
            LOGGER.debug("Unable to open directory stream for {}", dir.toString(), e);
            throw e;
        }
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

    Path getMetacardDirectory(String id) {
        if (id.length() < 6) {
            id = StringUtils.rightPad(id, 6, "0");
        }

        try {
            return Paths.get(outputDirectory, id.substring(0, 3), id.substring(3, 6), id);
        } catch (InvalidPathException e) {
            LOGGER.debug("Unable to create path from id {}", outputDirectory, e);
            return null;
        }
    }
}
