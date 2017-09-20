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
import ddf.catalog.content.operation.DeleteStorageRequest;
import ddf.catalog.content.operation.DeleteStorageResponse;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.operation.impl.ResponseImpl;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class DeleteStorageResponseImpl extends ResponseImpl<DeleteStorageRequest>
    implements DeleteStorageResponse {
  private List<ContentItem> deletedContentItems;

  /**
   * Instantiates a new ResponseImpl
   *
   * @param request - the original request
   * @param properties
   */
  public DeleteStorageResponseImpl(
      DeleteStorageRequest request, Map<String, Serializable> properties) {
    super(request, properties);
  }

  /**
   * Instantiates a new ResponseImpl
   *
   * @param request - the original request
   * @param deletedContentItems
   */
  public DeleteStorageResponseImpl(
      DeleteStorageRequest request, List<ContentItem> deletedContentItems) {
    super(request, null);
    this.deletedContentItems = deletedContentItems;
  }

  @Override
  public List<ContentItem> getDeletedContentItems() {
    return deletedContentItems;
  }

  @Override
  public StorageRequest getStorageRequest() {
    return request;
  }
}
