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
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.ProcessingDetails;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CreateResponseImpl contains the {@link ddf.catalog.operation.Response} information (created
 * metacards) on a {@link CreateRequest}.
 */
public class CreateResponseImpl extends ResponseImpl<CreateRequest> implements CreateResponse {

  /** The created metacards. */
  private List<Metacard> createdMetacards;

  private Set<ProcessingDetails> processingErrors = new HashSet<>();

  /**
   * Instantiates a new CreateResponsImpl
   *
   * @param request - {@link CreateRequest} used in the create operation
   * @param properties - the properties associated with the operation
   * @param createdMetacards - the created metacards
   */
  public CreateResponseImpl(
      CreateRequest request,
      Map<String, Serializable> properties,
      List<Metacard> createdMetacards) {
    this(request, properties, createdMetacards, new HashSet<>());
  }

  /**
   * Instantiates a new CreateResponsImpl
   *
   * @param request - {@link CreateRequest} used in the create operation
   * @param properties - the properties associated with the operation
   * @param createdMetacards - the created metacards
   * @param errors - the processing errors
   */
  public CreateResponseImpl(
      CreateRequest request,
      Map<String, Serializable> properties,
      List<Metacard> createdMetacards,
      Set<ProcessingDetails> errors) {
    super(request, properties);
    this.createdMetacards = createdMetacards;
    if (errors != null) {
      this.processingErrors = errors;
    }
  }

  @Override
  public Set<ProcessingDetails> getProcessingErrors() {
    return processingErrors;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.operation.CreateResponse#getCreatedMetacards()
   */
  @Override
  public List<Metacard> getCreatedMetacards() {
    return createdMetacards;
  }
}
