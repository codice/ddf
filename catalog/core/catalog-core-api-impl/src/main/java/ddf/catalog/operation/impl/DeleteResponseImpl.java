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
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ProcessingDetails;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The DeleteResponseImpl represents a default implementation of the {@link DefaultResponse}. */
public class DeleteResponseImpl extends ResponseImpl<DeleteRequest> implements DeleteResponse {

  private List<Metacard> deletedMetacards;

  private Set<ProcessingDetails> processingErrors = new HashSet<>();

  /**
   * Instantiates a new DeleteResponseImpl.
   *
   * @param request the original request
   * @param properties the properties associated with the operation
   * @param deletedMetacards the deleted {@link Metacard}(s)
   */
  public DeleteResponseImpl(
      DeleteRequest request,
      Map<String, Serializable> properties,
      List<Metacard> deletedMetacards) {
    this(request, properties, deletedMetacards, new HashSet<>());
  }

  /**
   * Instantiates a new DeleteResponseImpl.
   *
   * @param request the original request
   * @param properties the properties associated with the operation
   * @param deletedMetacards the deleted {@link Metacard}(s)
   * @param errors the processing errors
   */
  public DeleteResponseImpl(
      DeleteRequest request,
      Map<String, Serializable> properties,
      List<Metacard> deletedMetacards,
      Set<ProcessingDetails> errors) {
    super(request, properties);
    this.deletedMetacards = deletedMetacards;
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
   * @see ddf.catalog.operation.DeleteResponse#getDeletedMetacards()
   */
  @Override
  public List<Metacard> getDeletedMetacards() {
    return deletedMetacards;
  }
}
