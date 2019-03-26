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
package ddf.catalog.plugin.groomer;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.util.impl.Requests;
import java.io.Serializable;
import java.util.Date;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class that can be extended to create another rule set for grooming a metacard.
 *
 * @author Ashraf Barakat
 */
public abstract class AbstractMetacardGroomerPlugin implements PreIngestPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMetacardGroomerPlugin.class);

  @Override
  public CreateRequest process(CreateRequest input)
      throws PluginExecutionException, StopProcessingException {

    if (input == null
        || input.getMetacards() == null
        || input.getMetacards().isEmpty()
        || !Requests.isLocal(input)) {
      return input;
    }

    Date timestamp = new Date();

    for (Metacard metacard : input.getMetacards()) {

      applyCreatedOperationRules(input, metacard, timestamp);
    }

    return input;
  }

  @Override
  public UpdateRequest process(UpdateRequest input)
      throws PluginExecutionException, StopProcessingException {

    if (input == null
        || input.getUpdates() == null
        || input.getUpdates().isEmpty()
        || !Requests.isLocal(input)) {
      return input;
    }

    Date timestamp = new Date();

    for (Entry<Serializable, Metacard> singleUpdate : input.getUpdates()) {

      if (singleUpdate == null
          || singleUpdate.getKey() == null
          || singleUpdate.getValue() == null) {
        LOGGER.debug(
            "Either the single ddf.catalog.operation.Update, the Update's identifier, or the Update's value is null, skipping preparation. No preparation necessary.");
        continue;
      }

      Metacard metacard = singleUpdate.getValue();

      applyUpdateOperationRules(input, singleUpdate, metacard, timestamp);
    }

    return input;
  }

  @Override
  public DeleteRequest process(DeleteRequest input)
      throws PluginExecutionException, StopProcessingException {

    return input;
  }

  /**
   * This method is called on each Metacard in the {@link CreateRequest}. It allows for the
   * modification of the {@link Metacard} object within the request.
   *
   * @param createRequest the entire {@link CreateRequest} object
   * @param aMetacard a {@link Metacard} within the request
   * @param timestamp a current {@link Date} timestamp to be optionally used to timestamp each
   *     {@link Metacard}
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  protected abstract void applyCreatedOperationRules(
      CreateRequest createRequest, Metacard aMetacard, Date timestamp)
      throws PluginExecutionException, StopProcessingException;

  /**
   * This method is called on each {@link Metacard} in the {@link UpdateRequest}. It allows for
   * modification of the {@link Metacard} object within the request.
   *
   * @param updateRequest the entire {@link UpdateRequest} object
   * @param anUpdate a single {@link ddf.catalog.operation.Update} within the {@link UpdateRequest}
   * @param aMetacard a {@link Metacard} within the request
   * @param timestamp a current {@link Date} timestamp to be optionally used to timestamp each
   *     Metacard, such as stamping each Metacard with the same {@link Core#MODIFIED} date.
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  protected abstract void applyUpdateOperationRules(
      UpdateRequest updateRequest,
      Entry<Serializable, Metacard> anUpdate,
      Metacard aMetacard,
      Date timestamp)
      throws PluginExecutionException, StopProcessingException;
}
