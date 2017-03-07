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
package org.codice.ddf.catalog.plugin.metadata.backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * The MetadataBackupPlugin asynchronously backs up Metacard Metadata to the file system.  It implements
 * the PostProcessPlugin in order to maintain synchronization with the catalog (CRUD).
 * <p>
 * The root backup directory and subdirectory levels can be configured in the
 * MetadataBackupPlugin section in the admin console.
 */

public class MetadataBackupPlugin implements PostProcessPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataBackupPlugin.class);

    private static final String OUTPUT_DIRECTORY_PROPERTY = "outputDirectory";

    private static final String FOLDER_DEPTH_PROPERTY = "folderDepth";

    private static final String KEEP_DELETED_METACARDS_PROPERTY = "keepDeletedMetacards";

    private static final String METACARD_TRANSFORMER_ID_PROPERTY = "metacardTransformerId";

    private static final Integer MAX_FOLDER_DEPTH = 4;

    private static final Integer MIN_FOLDER_DEPTH = 0;

    private Boolean keepDeletedMetacards = false;

    private String metacardTransformerId;

    private MetacardTransformer metacardTransformer;

    private String outputDirectory;

    private Integer folderDepth = MIN_FOLDER_DEPTH;

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
                    "Unable to delete backup ingested metadata; no output directory specified.");
        }

        List<ProcessDeleteItem> processUpdateItems = processRequest.getProcessItems();
        for (ProcessDeleteItem processUpdateItem : processUpdateItems) {
            Metacard metacard = processUpdateItem.getMetacard();
            deleteMetadataIfPresent(metacard.getId());
        }

        return processRequest;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setFolderDepth(Integer folderDepth) {
        folderDepth = Math.max(folderDepth, MIN_FOLDER_DEPTH);
        folderDepth = Math.min(folderDepth, MAX_FOLDER_DEPTH);
        this.folderDepth = folderDepth;
    }

    public Integer getFolderDepth() {
        return folderDepth;
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
        if (metacardTransformerProperty instanceof String && StringUtils.isNotBlank((String) metacardTransformerProperty)) {
            setMetacardTransformerId((String) metacardTransformerProperty);
            LOGGER.debug("Updating {} with {}",
                    METACARD_TRANSFORMER_ID_PROPERTY, metacardTransformerProperty);
        }

        Object keepDeletedMetacards = properties.get(KEEP_DELETED_METACARDS_PROPERTY);
        if (keepDeletedMetacards instanceof Boolean) {
            this.keepDeletedMetacards = (Boolean) keepDeletedMetacards;
            LOGGER.debug("Updating {} with {}", KEEP_DELETED_METACARDS_PROPERTY,
                    keepDeletedMetacards);
        }

        Object folderDepth = properties.get(FOLDER_DEPTH_PROPERTY);
        if (folderDepth instanceof Integer) {
            setFolderDepth((Integer) folderDepth);
            LOGGER.debug("Updating {} with {}", FOLDER_DEPTH_PROPERTY, folderDepth);
        }
    }

    private void processRequest(ProcessRequest<? extends ProcessResourceItem> processRequest)
            throws PluginExecutionException {
        LOGGER.trace("Backing up metadata");
        if (StringUtils.isEmpty(outputDirectory)) {
            throw new PluginExecutionException(
                    "Unable to backup ingested metadata; no outputDirectory.");
        }

        if (metacardTransformer == null) {
            throw new PluginExecutionException(
                    "Unable to backup ingested metadata; no Metacard Transformer found.");
        }

        List<? extends ProcessResourceItem> processResourceItems = processRequest.getProcessItems();
        processResourceItems.forEach(processResourceItem -> {
            Metacard metacard = processResourceItem.getMetacard();
            try {
                LOGGER.trace("Backing up metadata for metacard : {}", metacard.getId());
                BinaryContent binaryContent = metacardTransformer.transform(metacard, null);
                copyMetadataToOutputDirectory(binaryContent, metacard.getId());
            } catch (CatalogTransformerException e) {
                LOGGER.debug("Unable to transform metacard with id {}.", metacard.getId(), e);
            }
        });
    }

    private void copyMetadataToOutputDirectory(BinaryContent content, String metacardId) {
        if (content == null || content.getInputStream() == null) {
            LOGGER.debug("No content for transformed metacard {}.", metacardId);
            return;
        }

        File parent = getCompleteDirectory(getFolderDepth(), metacardId);
        if (!parent.exists()) {
            try {
                FileUtils.forceMkdir(parent);
            } catch (IOException e) {
                LOGGER.debug("Unable to make directory {}", parent.getPath(), e);
            }
        }

        validateDirectory(parent);
        File file = new File(parent.getPath() + File.separator + metacardId);
        LOGGER.trace("Writing metadata from {} to file {}", metacardId, file.getPath());

        try (OutputStream outputStream = new FileOutputStream(file)) {
            IOUtils.write(content.getByteArray(), outputStream);
        } catch (IOException e) {
            LOGGER.warn("Unable to backup {} to {}.  The directory may be full.",
                    metacardId,
                    file.getAbsolutePath(),
                    e);
        }
    }

    private void deleteMetadataIfPresent(String filename) {
        File parent = getCompleteDirectory(getFolderDepth(), filename);
        validateDirectory(parent);
        File file = new File(parent.getPath() + File.separator + filename);
        LOGGER.trace("Deleting metadata file {}", file.getPath());
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            LOGGER.debug("Unable to backup metadata {}. ", filename, e);
        }
        clearEmptyDirectories(parent);
    }

    private File getCompleteDirectory(int depth, String id) {
        File parent = new File(getOutputDirectory());
        for (int i = 0; i < depth && id.length() > (i * 2 + 2); i++) {
            parent = new File(parent, id.substring(i * 2, i * 2 + 2));
        }
        return parent;
    }

    private void clearEmptyDirectories(File file) {
        while (file != null && !file.getPath()
                .equals(outputDirectory)) {
            if (isDirectoryEmpty(file.toPath())) {
                try {
                    FileUtils.deleteDirectory(file);
                } catch (IOException e) {
                    LOGGER.debug("Unable to delete directory {}", file.toPath(), e);
                }
            }
            file = file.getParentFile();
        }
    }

    private boolean isDirectoryEmpty(Path dir) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator()
                    .hasNext();
        } catch (IOException e) {
            LOGGER.debug("Unable to open directory stream for {}", dir.toString(), e);
        }
        return false;
    }

    private void validateDirectory(File path) {
        if (!(path.isDirectory() && path.canWrite())) {
            LOGGER.debug("{} is not a valid directory", path.getPath());
            throw new IllegalArgumentException("Directory " + path.getAbsolutePath()
                    + " does not exist or is not writable");
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
                        "Unable to resolve MetacardTransformer {}.  Metadata backup will not be performed.",
                        metacardTransformerId,
                        e);
            }
        }
        return null;
    }
}
