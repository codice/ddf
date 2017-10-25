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
package ddf.catalog.operation.impl;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The UpdateResponseImpl represents a default implementation of an {@link UpdateRequest} */
public class UpdateResponseImpl extends ResponseImpl<UpdateRequest> implements UpdateResponse {

  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateResponseImpl.class.getName());

  protected List<Update> updatedMetacards;

  private Set<ProcessingDetails> processingErrors = new HashSet<>();

  /**
   * Instantiates a new UpdateResponseImpl.
   *
   * @param request the original request
   * @param properties the properties associated with the operation
   * @param updatedMetacards the updated metacards
   */
  public UpdateResponseImpl(
      UpdateRequest request, Map<String, Serializable> properties, List<Update> updatedMetacards) {
    this(request, properties, updatedMetacards, new HashSet<ProcessingDetails>());
  }

  /**
   * Instantiates a new UpdateResponseImpl.
   *
   * @param request the original request
   * @param properties the properties associated with the operation
   * @param updatedMetacards the updated metacards
   * @param errors the processing errors
   */
  public UpdateResponseImpl(
      UpdateRequest request,
      Map<String, Serializable> properties,
      List<Update> updatedMetacards,
      Set<ProcessingDetails> errors) {
    super(request, properties);
    this.updatedMetacards = updatedMetacards;
    if (errors != null) {
      this.processingErrors = errors;
    }
  }

  /**
   * Instantiates a new UpdateResponseImpl with old and new {@link Metacard}(s).
   *
   * @param request the original request
   * @param properties the properties associated with the operation
   * @param updatedMetacards the updated metacards
   * @param oldMetacards the old metacards (prior to the updates)
   */
  public UpdateResponseImpl(
      UpdateRequest request,
      Map<String, Serializable> properties,
      List<Metacard> updatedMetacards,
      List<Metacard> oldMetacards) {
    this(request, properties, updatedMetacards, oldMetacards, new HashSet<>());
  }

  /**
   * Instantiates a new UpdateResponseImpl with old and new {@link Metacard}(s).
   *
   * @param request the original request
   * @param properties the properties associated with the operation
   * @param updatedMetacards the updated metacards
   * @param oldMetacards the old metacards (prior to the updates)
   * @param errors the processing errors
   */
  public UpdateResponseImpl(
      UpdateRequest request,
      Map<String, Serializable> properties,
      List<Metacard> updatedMetacards,
      List<Metacard> oldMetacards,
      Set<ProcessingDetails> errors) {
    super(request, properties);
    if (updatedMetacards != null && oldMetacards != null) {
      int size = updatedMetacards.size();
      int oldSize = oldMetacards.size();
      LOGGER.trace("Updated Metacard size: {}", size);
      LOGGER.trace("old Metacard Size: {}", oldSize);

      if (size == oldSize) {
        this.updatedMetacards = new ArrayList<Update>(size);
        for (int i = 0; i < size; i++) {
          this.updatedMetacards.add(new UpdateImpl(updatedMetacards.get(i), oldMetacards.get(i)));
        }
      } else {
        throw new IllegalArgumentException(
            "UpdatedMetacard List and oldMetacardList must be the same size");
      }
    }
    if (errors != null) {
      this.processingErrors = errors;
    } else {
      this.processingErrors = new HashSet<>();
    }
  }

  @Override
  public Set<ProcessingDetails> getProcessingErrors() {
    return processingErrors;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.operation.UpdateResponse#getUpdatedMetacards()
   */
  @Override
  public List<Update> getUpdatedMetacards() {
    return updatedMetacards;
  }
}
