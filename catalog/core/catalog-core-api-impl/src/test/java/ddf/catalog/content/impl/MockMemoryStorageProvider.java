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
package ddf.catalog.content.impl;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MockMemoryStorageProvider implements StorageProvider {

  Map<String, ContentItem> itemMap = new HashMap<>();

  Map<String, ContentItem> tempItemMap = new HashMap<>();

  @Override
  public CreateStorageResponse create(CreateStorageRequest createStorageRequest)
      throws StorageException {
    for (ContentItem contentItem : createStorageRequest.getContentItems()) {
      tempItemMap.put(contentItem.getId(), contentItem);
    }
    return new CreateStorageResponseImpl(
        createStorageRequest, createStorageRequest.getContentItems());
  }

  @Override
  public ReadStorageResponse read(ReadStorageRequest readRequest) throws StorageException {
    return new ReadStorageResponseImpl(
        readRequest, itemMap.get(readRequest.getResourceUri().getSchemeSpecificPart()));
  }

  @Override
  public UpdateStorageResponse update(UpdateStorageRequest updateStorageRequest)
      throws StorageException {
    for (ContentItem contentItem : updateStorageRequest.getContentItems()) {
      tempItemMap.put(contentItem.getId(), contentItem);
    }
    return new UpdateStorageResponseImpl(
        updateStorageRequest, updateStorageRequest.getContentItems());
  }

  @Override
  public DeleteStorageResponse delete(DeleteStorageRequest deleteRequest) throws StorageException {
    List<ContentItem> contentItems =
        deleteRequest.getMetacards().stream()
            .map(metacard -> tempItemMap.remove(metacard.getId()))
            .collect(Collectors.toList());
    return new DeleteStorageResponseImpl(deleteRequest, contentItems);
  }

  @Override
  public void commit(StorageRequest request) throws StorageException {
    itemMap.putAll(tempItemMap);
    new HashSet<>(tempItemMap.keySet()).forEach(tempItemMap::remove);
  }

  @Override
  public void rollback(StorageRequest request) throws StorageException {
    new HashSet<>(tempItemMap.keySet()).forEach(tempItemMap::remove);
  }

  public int size() {
    return itemMap.size();
  }
}
