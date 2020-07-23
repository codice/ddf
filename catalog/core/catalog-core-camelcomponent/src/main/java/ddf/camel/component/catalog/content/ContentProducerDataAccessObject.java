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
package ddf.camel.component.catalog.content;

import com.google.common.collect.Maps;
import com.google.common.io.Files;
import ddf.catalog.CatalogFramework;
import ddf.catalog.Constants;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
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
import ddf.catalog.operation.impl.SourceInfoRequestLocal;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.Predicate;
import org.apache.camel.Message;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileMessage;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentProducerDataAccessObject {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ContentProducerDataAccessObject.class);
  public static final Kind<Path> ENTRY_CREATE = StandardWatchEventKinds.ENTRY_CREATE;
  public static final Kind<Path> ENTRY_MODIFY = StandardWatchEventKinds.ENTRY_MODIFY;
  public static final Kind<Path> ENTRY_DELETE = StandardWatchEventKinds.ENTRY_DELETE;

  private UuidGenerator uuidGenerator;

  public ContentProducerDataAccessObject(UuidGenerator uuidGenerator) {
    this.uuidGenerator = uuidGenerator;
  }

  public File getFileUsingRefKey(boolean storeRefKey, Message in) throws ContentComponentException {
    File ingestedFile = null;
    try {
      if (!storeRefKey) {
        ingestedFile = ((GenericFile<File>) in.getBody()).getFile();
      } else {
        WatchEvent<Path> pathWatchEvent =
            (WatchEvent<Path>) ((GenericFileMessage) in).getGenericFile().getFile();

        if (pathWatchEvent != null && pathWatchEvent.context() != null) {
          ingestedFile = pathWatchEvent.context().toFile();
        }
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
          (WatchEvent<Path>) ((GenericFileMessage) in).getGenericFile().getFile();
      return pathWatchEvent.kind();
    } else {
      return ENTRY_CREATE;
    }
  }

  public String getMimeType(ContentEndpoint endpoint, File ingestedFile)
      throws ContentComponentException {
    if (ingestedFile == null) {
      return null;
    }

    String fileExtension = FilenameUtils.getExtension(ingestedFile.getAbsolutePath());

    String mimeType = null;

    MimeTypeMapper mimeTypeMapper = endpoint.getComponent().getMimeTypeMapper();
    if (mimeTypeMapper != null && ingestedFile.exists()) {
      try (InputStream inputStream = Files.asByteSource(ingestedFile).openStream()) {
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

  public void createContentItem(
      FileSystemPersistenceProvider fileIdMap,
      ContentEndpoint endpoint,
      File ingestedFile,
      WatchEvent.Kind<Path> eventType,
      String mimeType,
      Map<String, Object> headers)
      throws SourceUnavailableException, IngestException {
    LOGGER.debug("Creating content item.");

    if (!eventType.equals(ENTRY_DELETE) && ingestedFile == null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Ingested File was null with eventType [{}]. Doing nothing.", eventType.name());
      }
      return;
    }

    String refKey = (String) headers.get(Constants.STORE_REFERENCE_KEY);
    String safeKey = null;
    String id = null;

    // null if the file is being stored in the content store
    // not null if the file lives outside the content store (external reference)
    if (refKey != null) {
      // guards against impermissible filesystem characters
      safeKey = DigestUtils.sha1Hex(refKey);
      if (fileIdMap.loadAllKeys().contains(safeKey)) {
        id = String.valueOf(fileIdMap.loadFromPersistence(safeKey));
      } else if (!ENTRY_CREATE.equals(eventType)) {
        LOGGER.warn("Unable to look up id for {}, not performing {}", refKey, eventType.name());
        return;
      }
    }
    if (ENTRY_CREATE.equals(eventType)) {
      CreateStorageRequest createRequest =
          new CreateStorageRequestImpl(
              Collections.singletonList(
                  new ContentItemImpl(
                      uuidGenerator.generateUuid(),
                      Files.asByteSource(ingestedFile),
                      mimeType,
                      ingestedFile.getName(),
                      ingestedFile.length(),
                      null)),
              getProperties(headers));

      CatalogFramework catalogFramework = endpoint.getComponent().getCatalogFramework();

      waitForAvailableSource(catalogFramework);

      CreateResponse createResponse = catalogFramework.create(createRequest);

      if (createResponse != null) {
        List<Metacard> createdMetacards = createResponse.getCreatedMetacards();

        if (safeKey != null) {
          fileIdMap.store(safeKey, createdMetacards.get(0).getId());
        }
        logIds(createdMetacards, "created");
      }
    } else if (ENTRY_MODIFY.equals(eventType)) {
      UpdateStorageRequest updateRequest =
          new UpdateStorageRequestImpl(
              Collections.singletonList(
                  new ContentItemImpl(
                      id,
                      Files.asByteSource(ingestedFile),
                      mimeType,
                      ingestedFile.getName(),
                      0,
                      null)),
              getProperties(headers));

      UpdateResponse updateResponse =
          endpoint.getComponent().getCatalogFramework().update(updateRequest);
      if (updateResponse != null) {
        List<Update> updatedMetacards = updateResponse.getUpdatedMetacards();

        logIds(
            updatedMetacards.stream().map(Update::getNewMetacard).collect(Collectors.toList()),
            "updated");
      }
    } else if (ENTRY_DELETE.equals(eventType)) {
      DeleteRequest deleteRequest = new DeleteRequestImpl(id);

      DeleteResponse deleteResponse =
          endpoint.getComponent().getCatalogFramework().delete(deleteRequest);
      if (deleteResponse != null) {
        List<Metacard> deletedMetacards = deleteResponse.getDeletedMetacards();

        if (safeKey != null) {
          fileIdMap.delete(safeKey);
        }
        logIds(deletedMetacards, "deleted");
      }
    }
  }

  protected void logIds(List<Metacard> metacards, String action) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "content item(s) {} with id = {}",
          action,
          metacards.stream()
              .map(Metacard::getId)
              .filter(Objects::nonNull)
              .collect(Collectors.joining(", ")));
    }
  }

  protected HashMap<String, Serializable> getProperties(Map<String, Object> headers) {
    return Maps.newHashMap(Maps.transformValues(headers, Serializable.class::cast));
  }

  private void waitForAvailableSource(CatalogFramework catalogFramework)
      throws SourceUnavailableException {
    RetryPolicy retryPolicy =
        new RetryPolicy()
            .withDelay(3, TimeUnit.SECONDS)
            .withMaxDuration(3, TimeUnit.MINUTES)
            .retryIf((Predicate<Set>) Set::isEmpty)
            .retryIf(
                (Set<SourceDescriptor> result) -> !result.stream().findFirst().get().isAvailable());

    Failsafe.with(retryPolicy)
        .get(
            () ->
                catalogFramework.getSourceInfo(new SourceInfoRequestLocal(false)).getSourceInfo());
  }
}
