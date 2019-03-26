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
package org.codice.ddf.catalog.content.plugin.uri;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.StopProcessingException;
import java.io.Serializable;
import java.util.Map;

/**
 * The {@code ContentUriAccessPlugin} prevents a {@link Metacard}s {@link Core#RESOURCE_URI} with a
 * scheme of {@link ContentItem#CONTENT_SCHEME} from being overridden by an incoming {@link
 * UpdateRequest}.
 */
public class ContentUriAccessPlugin implements AccessPlugin {

  private static final String CANNOT_OVERWRITE_MESSAGE =
      "Cannot overwrite resource URI in content store.";

  @Override
  public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public UpdateRequest processPreUpdate(
      UpdateRequest input, Map<String, Metacard> existingMetacards) throws StopProcessingException {
    for (Map.Entry<Serializable, Metacard> entry : input.getUpdates()) {
      Metacard existingMetacard = existingMetacards.get(entry.getKey().toString());

      if (existingMetacard.getResourceURI() != null
          && !ContentItem.CONTENT_SCHEME.equals(existingMetacard.getResourceURI().getScheme())) {
        continue;
      }

      if (oneOrOtherUriNull(existingMetacard, entry.getValue())) {
        throw new StopProcessingException(CANNOT_OVERWRITE_MESSAGE);
      }

      if (entry.getValue().getResourceURI() != null
          && !existingMetacard.getResourceURI().equals(entry.getValue().getResourceURI())) {
        throw new StopProcessingException(CANNOT_OVERWRITE_MESSAGE);
      }
    }
    return input;
  }

  private boolean oneOrOtherUriNull(Metacard existingMetacard, Metacard updatedMetacard) {
    return (existingMetacard.getResourceURI() == null && updatedMetacard.getResourceURI() != null)
        || (existingMetacard.getResourceURI() != null && updatedMetacard.getResourceURI() == null);
  }

  @Override
  public DeleteRequest processPreDelete(DeleteRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public DeleteResponse processPostDelete(DeleteResponse input) throws StopProcessingException {
    return input;
  }

  @Override
  public QueryRequest processPreQuery(QueryRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public QueryResponse processPostQuery(QueryResponse input) throws StopProcessingException {
    return input;
  }

  @Override
  public ResourceRequest processPreResource(ResourceRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public ResourceResponse processPostResource(ResourceResponse input, Metacard metacard)
      throws StopProcessingException {
    return input;
  }
}
