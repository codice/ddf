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
package org.codice.ddf.spatial.process.transformer;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.codice.ddf.spatial.process.api.BadRequestException;
import org.codice.ddf.spatial.process.api.Operation;
import org.codice.ddf.spatial.process.api.Process;
import org.codice.ddf.spatial.process.api.ProcessException;
import org.codice.ddf.spatial.process.api.ProcessMonitor;
import org.codice.ddf.spatial.process.api.description.ExecutionDescription;
import org.codice.ddf.spatial.process.api.description.Metadata;
import org.codice.ddf.spatial.process.api.request.Data;
import org.codice.ddf.spatial.process.api.request.ExecutionRequest;
import org.codice.ddf.spatial.process.api.request.Literal;
import org.codice.ddf.spatial.process.api.request.ProcessResult;
import org.codice.ddf.spatial.process.api.request.ProcessStatus;
import org.codice.ddf.spatial.process.api.request.Reference;
import org.opengis.filter.Filter;

public class MetacardTransformerProcess implements Process {

  public static final String METACARD_ID = "metacardId";

  private static final EnumSet<Operation> OPERATIONS = EnumSet.of(Operation.SYNC_EXEC);

  private MetacardTransformer metacardTransformer;

  private CatalogFramework catalogFramework;

  private FilterBuilder filterBuilder;

  private String title;

  private ExecutionDescription executionDescription;

  private String description;

  private String id;

  private Metadata metadata;

  private String version;

  public MetacardTransformerProcess(
      CatalogFramework catalogFramework,
      FilterBuilder filterBuilder,
      String id,
      MetacardTransformer metacardTransformer,
      ExecutionDescription executionDescription) {
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
    this.id = id;
    this.metacardTransformer = metacardTransformer;
    this.executionDescription = executionDescription;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public Metadata getMetadata() {
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  @Override
  public Set<Operation> getOperations() {
    return OPERATIONS;
  }

  @Override
  public ExecutionDescription getExecutionDescription() {
    return executionDescription;
  }

  @Override
  public ProcessStatus asyncExecute(ExecutionRequest executionRequest) {
    throw new UnsupportedOperationException("Async Execute is not supported.");
  }

  /**
   * @param executionRequest
   * @return
   * @throws ProcessException
   */
  @Override
  public ProcessResult syncExecute(ExecutionRequest executionRequest) {

    Map<String, Serializable> arguments = getArguments(executionRequest);
    if (arguments.get(METACARD_ID) == null) {
      // the inputs should already have been validated but checking again here.
      throw new BadRequestException(
          Collections.singleton(METACARD_ID), "Missing Metacard Id argument");
    }
    String metacardId = arguments.get(METACARD_ID).toString();
    try {
      QueryResponse queryResponse = queryById(metacardId);
      if (!queryResponse.getResults().isEmpty()) {
        Result result = queryResponse.getResults().get(0);

        BinaryContent content = metacardTransformer.transform(result.getMetacard(), arguments);
        if (content != null) {
          return executionRequest
              .getRequestResultBuilder()
              .add("output", content.getInputStream())
              .build();
        } else {
          throw new ProcessException("Transformer returned null.");
        }
      }
    } catch (CatalogTransformerException
        | UnsupportedQueryException
        | SourceUnavailableException
        | FederationException e) {
      throw new ProcessException(e);
    }
    throw new BadRequestException(Collections.singleton(METACARD_ID), "Metacard not found.");
  }

  // this assumes that an input is not repeated which will be true for transformers and that its a
  // literal value
  private Map<String, Serializable> getArguments(ExecutionRequest executionRequest) {
    Map<String, Serializable> arguments = new HashMap<>();
    for (Data inputData : executionRequest.getInputData()) {
      if (inputData instanceof Literal) {
        arguments.put(inputData.getId(), ((Literal) inputData).getValue());
      } else if (inputData instanceof Reference) {
        // the client can send data by reference or value no way to require one or the other.
        throw new BadRequestException(
            Collections.singleton(inputData.getId()), "Reference Input values not supported");
      }
    }
    return arguments;
  }

  public QueryResponse queryById(String id)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    Filter filter = filterBuilder.attribute(Core.ID).is().equalTo().text(id);
    return catalogFramework.query(new QueryRequestImpl(new QueryImpl(filter), false));
  }

  @Override
  public Optional<ProcessMonitor> getProcessMonitor() {
    throw new UnsupportedOperationException("Status and Dismiss are not supported.");
  }
}
