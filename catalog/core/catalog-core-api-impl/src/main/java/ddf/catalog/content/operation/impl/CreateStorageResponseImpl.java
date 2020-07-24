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
package ddf.catalog.content.operation.impl;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.operation.impl.ResponseImpl;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CreateStorageResponseImpl extends ResponseImpl<CreateStorageRequest>
    implements CreateStorageResponse {

  private List<ContentItem> createdContentItems;

  /**
   * Instantiates a new ResponseImpl
   *
   * @param request the original request
   * @param properties
   */
  public CreateStorageResponseImpl(
      CreateStorageRequest request, Map<String, Serializable> properties) {
    super(request, properties);
  }

  /**
   * Instantiates a new ResponseImpl
   *
   * @param request the original request
   * @param createdContentItems
   */
  public CreateStorageResponseImpl(
      CreateStorageRequest request, List<ContentItem> createdContentItems) {
    super(request, null);
    this.createdContentItems = createdContentItems;
  }

  @Override
  public List<ContentItem> getCreatedContentItems() {
    return createdContentItems;
  }

  @Override
  public StorageRequest getStorageRequest() {
    return request;
  }

  @Override
  public String toString() {
    if (createdContentItems == null) {
      return "CreateStorageResponseImpl{createdContentItems=null}";
    }
    return String.format(
        "CreateStorageResponseImpl{createdContentItems=%s}",
        createdContentItems.stream()
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.joining(", ", "[", "]")));
  }
}
