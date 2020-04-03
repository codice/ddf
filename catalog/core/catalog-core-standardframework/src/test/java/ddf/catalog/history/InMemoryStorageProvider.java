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
package ddf.catalog.history;

import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.DeleteStorageRequest;
import ddf.catalog.content.operation.DeleteStorageResponse;
import ddf.catalog.content.operation.ReadStorageRequest;
import ddf.catalog.content.operation.ReadStorageResponse;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.operation.impl.CreateStorageResponseImpl;
import ddf.catalog.content.operation.impl.DeleteStorageResponseImpl;
import ddf.catalog.content.operation.impl.ReadStorageResponseImpl;
import ddf.catalog.content.operation.impl.UpdateStorageResponseImpl;
import ddf.catalog.data.Metacard;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InMemoryStorageProvider implements StorageProvider {
  Map<String, ContentItem> storageMap = new HashMap<>();

  Map<String, ContentItem> deleteMap = new HashMap<>();

  Map<String, ContentItem> updateMap = new HashMap<>();

  @Override
  public CreateStorageResponse create(CreateStorageRequest createStorageRequest)
      throws StorageException {
    if (createStorageRequest == null) {
      throw new StorageException("storage request can't be null");
    }

    for (ContentItem item : createStorageRequest.getContentItems()) {
      updateMap.put(item.getUri(), item);
    }

    return new CreateStorageResponseImpl(
        createStorageRequest, createStorageRequest.getContentItems());
  }

  @Override
  public ReadStorageResponse read(ReadStorageRequest readRequest) throws StorageException {
    if (readRequest == null) {
      throw new StorageException("read request can't be null");
    }

    return new ReadStorageResponseImpl(
        readRequest, storageMap.get(readRequest.getResourceUri().toASCIIString()));
  }

  @Override
  public UpdateStorageResponse update(UpdateStorageRequest updateStorageRequest)
      throws StorageException {
    if (updateStorageRequest == null) {
      throw new StorageException("update storage request can't be null");
    }

    for (ContentItem item : updateStorageRequest.getContentItems()) {
      if (storageMap.containsKey(item.getUri())) {
        updateMap.put(item.getUri(), item);
      } else {
        throw new StorageException("can't update an item that isn't stored");
      }
    }

    return new UpdateStorageResponseImpl(
        updateStorageRequest, updateStorageRequest.getContentItems());
  }

  @Override
  public DeleteStorageResponse delete(DeleteStorageRequest deleteRequest) throws StorageException {
    if (deleteRequest == null) {
      throw new StorageException("delete request can't be null");
    }

    List<ContentItem> itemsToDelete = new ArrayList<>();
    for (Metacard metacard : deleteRequest.getMetacards()) {
      List<ContentItem> tmp =
          storageMap.values().stream()
              .filter(item -> item.getMetacard().getId().equals(metacard.getId()))
              .collect(Collectors.toList());

      if (tmp.isEmpty()) {
        throw new StorageException("can't delete a metacard that isn't stored");
      }

      itemsToDelete.addAll(tmp);
    }

    for (ContentItem item : itemsToDelete) {
      deleteMap.put(item.getUri(), item);
    }

    return new DeleteStorageResponseImpl(deleteRequest, itemsToDelete);
  }

  @Override
  public void commit(StorageRequest id) throws StorageException {
    if (id == null) {
      throw new StorageException("storage request id can't be null");
    }

    for (String key : deleteMap.keySet()) {
      storageMap.remove(key);
    }
    storageMap.putAll(updateMap);

    updateMap.clear();
    deleteMap.clear();
  }

  @Override
  public void rollback(StorageRequest id) throws StorageException {
    updateMap.clear();
    deleteMap.clear();
  }
}
