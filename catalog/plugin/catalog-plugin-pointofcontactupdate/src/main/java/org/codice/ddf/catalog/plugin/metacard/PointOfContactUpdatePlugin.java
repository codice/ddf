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
package org.codice.ddf.catalog.plugin.metacard;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PreAuthorizationPlugin;
import ddf.catalog.plugin.StopProcessingException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Update plugin that sets Point of Contact to the previous value if the new value is null. */
public class PointOfContactUpdatePlugin implements PreAuthorizationPlugin {
  private static final Logger LOGGER = LoggerFactory.getLogger(PointOfContactUpdatePlugin.class);

  @Override
  public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public UpdateRequest processPreUpdate(
      UpdateRequest input, Map<String, Metacard> existingMetacards) throws StopProcessingException {
    input
        .getUpdates()
        .forEach(
            e -> {
              Metacard newMetacard = e.getValue();
              Metacard previous = getPreviousMetacardWithId(newMetacard.getId(), existingMetacards);

              if (previous != null
                  && newMetacard.getAttribute(Metacard.POINT_OF_CONTACT) == null
                  && isResourceMetacard(newMetacard)) {
                newMetacard.setAttribute(previous.getAttribute(Metacard.POINT_OF_CONTACT));
              }
            });

    return input;
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

  private boolean isResourceMetacard(Metacard metacard) {
    return metacard.getTags().isEmpty() || metacard.getTags().contains("resource");
  }

  private Metacard getPreviousMetacardWithId(String id, Map<String, Metacard> previousMetacards) {
    Metacard previous;
    previous =
        previousMetacards.entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(x -> x.getId().equals(id))
            .findFirst()
            .orElse(null);

    if (previous == null) {
      LOGGER.debug("Cannot locate metacard {} for update.", id);
      return null;
    }

    return previous;
  }
}
