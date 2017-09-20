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
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.operation.impl.ResponseImpl;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class UpdateStorageResponseImpl extends ResponseImpl<UpdateStorageRequest>
    implements UpdateStorageResponse {
  private List<ContentItem> updatedContentItems;

  /**
   * Instantiates a new ResponseImpl
   *
   * @param request - the original request
   * @param properties
   */
  public UpdateStorageResponseImpl(
      UpdateStorageRequest request, Map<String, Serializable> properties) {
    super(request, properties);
  }

  /**
   * Instantiates a new ResponseImpl
   *
   * @param request - the original request
   * @param updatedContentItems
   */
  public UpdateStorageResponseImpl(
      UpdateStorageRequest request, List<ContentItem> updatedContentItems) {
    super(request, null);
    this.updatedContentItems = updatedContentItems;
  }

  @Override
  public List<ContentItem> getUpdatedContentItems() {
    return updatedContentItems;
  }

  @Override
  public StorageRequest getStorageRequest() {
    return request;
  }
}
