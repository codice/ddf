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
package ddf.camel.component.catalog.content;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Message;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileMessage;
import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import ddf.catalog.Constants;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.content.operation.impl.UpdateStorageRequestImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;

public class ContentProducerDataAccessObject {

    private UuidGenerator uuidGenerator;

    public ContentProducerDataAccessObject(UuidGenerator uuidGenerator) {
        this.uuidGenerator = uuidGenerator;
    }

    private static final transient Logger LOGGER = LoggerFactory.getLogger(
            ContentProducerDataAccessObject.class);

    public File getFileUsingRefKey(boolean storeRefKey, Message in)
            throws ContentComponentException {
        File ingestedFile;
        try {
            if (!storeRefKey) {
                ingestedFile = ((GenericFile<File>) in.getBody()).getFile();
            } else {
                WatchEvent<Path> pathWatchEvent =
                        (WatchEvent<Path>) ((GenericFileMessage) in).getGenericFile()
                                .getFile();
                ingestedFile = pathWatchEvent.context()
                        .toFile();
            }
        } catch (ClassCastException e) {
            throw new ContentComponentException(
                    "Unable to cast message body to Camel GenericFile, so unable to process ingested file");
        }
        return ingestedFile;
    }

    public WatchEvent.Kind<Path> getEventType(boolean storeRefKey, Message in) {
        if (storeRefKey) {
            WatchEvent<Path> pathWatchEvent =
                    (WatchEvent<Path>) ((GenericFileMessage) in).getGenericFile()
                            .getFile();
            return pathWatchEvent.kind();
        } else {
            return StandardWatchEventKinds.ENTRY_CREATE;
        }

    }

    public String getMimeType(ContentEndpoint endpoint, File ingestedFile)
            throws ContentComponentException {
        String fileExtension = FilenameUtils.getExtension(ingestedFile.getAbsolutePath());

        String mimeType = null;

        MimeTypeMapper mimeTypeMapper = endpoint.getComponent()
                .getMimeTypeMapper();
        if (mimeTypeMapper != null && ingestedFile.exists()) {
            try (InputStream inputStream = Files.asByteSource(ingestedFile)
                    .openStream()) {
                if (fileExtension.equalsIgnoreCase("xml")) {
                    mimeType = mimeTypeMapper.guessMimeType(inputStream, fileExtension);
                } else {
                    mimeType = mimeTypeMapper.getMimeTypeForFileExtension(fileExtension);
                }

            } catch (MimeTypeResolutionException | IOException e) {
                throw new ContentComponentException(e);
            }
        } else if (ingestedFile.exists()) {
            LOGGER.debug("Did not find a MimeTypeMapper service");
            throw new ContentComponentException(
                    "Unable to find a mime type for the ingested file " + ingestedFile.getName());
        }

        return mimeType;
    }

    public void createContentItem(FileSystemPersistenceProvider fileIdMap, ContentEndpoint endpoint,
            File ingestedFile, WatchEvent.Kind<Path> eventType, String mimeType,
            Map<String, Object> headers) throws SourceUnavailableException, IngestException {
        LOGGER.debug("Creating content item.");

        String key = String.valueOf(ingestedFile.getAbsolutePath()
                .hashCode());
        String id = fileIdMap.loadAllKeys()
                .contains(key) ? String.valueOf(fileIdMap.loadFromPersistence(key)) : null;

        if (StandardWatchEventKinds.ENTRY_CREATE.equals(eventType)) {
            CreateStorageRequest createRequest =
                    new CreateStorageRequestImpl(Collections.singletonList(new ContentItemImpl(
                            uuidGenerator.generateUuid(),
                            Files.asByteSource(ingestedFile),
                            mimeType,
                            ingestedFile.getName(),
                            ingestedFile.length(),
                            null)), null);

            processHeaders(headers, createRequest, ingestedFile);

            CreateResponse createResponse = endpoint.getComponent()
                    .getCatalogFramework()
                    .create(createRequest);
            if (createResponse != null) {
                List<Metacard> createdMetacards = createResponse.getCreatedMetacards();

                for (Metacard metacard : createdMetacards) {
                    fileIdMap.store(key, metacard.getId());
                    LOGGER.debug("content item created with id = {}", metacard.getId());
                }
            }
        } else if (StandardWatchEventKinds.ENTRY_MODIFY.equals(eventType)) {
            UpdateStorageRequest updateRequest =
                    new UpdateStorageRequestImpl(Collections.singletonList(new ContentItemImpl(id,
                            Files.asByteSource(ingestedFile),
                            mimeType,
                            ingestedFile.getName(),
                            0,
                            null)), null);

            processHeaders(headers, updateRequest, ingestedFile);

            UpdateResponse updateResponse = endpoint.getComponent()
                    .getCatalogFramework()
                    .update(updateRequest);
            if (updateResponse != null) {
                List<Update> updatedMetacards = updateResponse.getUpdatedMetacards();

                for (Update update : updatedMetacards) {
                    LOGGER.debug("content item updated with id = {}",
                            update.getNewMetacard()
                                    .getId());
                }
            }
        } else if (StandardWatchEventKinds.ENTRY_DELETE.equals(eventType)) {
            DeleteRequest deleteRequest = new DeleteRequestImpl(id);

            DeleteResponse deleteResponse = endpoint.getComponent()
                    .getCatalogFramework()
                    .delete(deleteRequest);
            if (deleteResponse != null) {
                List<Metacard> deletedMetacards = deleteResponse.getDeletedMetacards();

                for (Metacard delete : deletedMetacards) {
                    fileIdMap.delete(ingestedFile.getAbsolutePath());
                    LOGGER.debug("content item deleted with id = {}", delete.getId());
                }
            }
        }
    }

    public void processHeaders(Map<String, Object> headers, StorageRequest storageRequest,
            File ingestedFile) {
        Map<String, Serializable> attributeOverrideHeaders = new HashMap<>((Map) headers.get(
                Constants.ATTRIBUTE_OVERRIDES_KEY));
        if (!attributeOverrideHeaders.isEmpty()) {
            storageRequest.getProperties()
                    .put(Constants.ATTRIBUTE_OVERRIDES_KEY,
                            (Serializable) attributeOverrideHeaders);
        }
        if (headers.containsKey(Constants.STORE_REFERENCE_KEY)) {
            storageRequest.getProperties()
                    .put(Constants.STORE_REFERENCE_KEY, ingestedFile.getAbsolutePath());
        }
    }
}
