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
package org.codice.ddf.catalog.security.logging;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.plugin.PostCreateStoragePlugin;
import ddf.catalog.content.plugin.PostUpdateStoragePlugin;
import ddf.catalog.content.plugin.PreCreateStoragePlugin;
import ddf.catalog.content.plugin.PreUpdateStoragePlugin;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.Response;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostFederatedQueryPlugin;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreFederatedQueryPlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.Source;
import ddf.catalog.util.impl.Requests;
import ddf.security.common.audit.SecurityLogger;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;

/** Logs the current operation being performed to the security logger. */
public class SecurityLoggingPlugin
    implements PreIngestPlugin,
        PostIngestPlugin,
        PreQueryPlugin,
        PostQueryPlugin,
        PreFederatedQueryPlugin,
        PostFederatedQueryPlugin,
        PreResourcePlugin,
        PostResourcePlugin,
        PreCreateStoragePlugin,
        PreUpdateStoragePlugin,
        PostCreateStoragePlugin,
        PostUpdateStoragePlugin {

  @Override
  public CreateRequest process(CreateRequest input)
      throws PluginExecutionException, StopProcessingException {
    String additional =
        input.getMetacards().stream()
            .map(Metacard::getId)
            .collect(Collectors.joining(",", "Creating metacards: ", ""));
    logOperation(CatalogOperationType.INGEST_REQUEST, input, additional);
    return input;
  }

  @Override
  public UpdateRequest process(UpdateRequest input)
      throws PluginExecutionException, StopProcessingException {
    String additional =
        input.getUpdates().stream()
            .map(Map.Entry::getValue)
            .map(Metacard::getId)
            .collect(Collectors.joining(",", "Updating metacards: ", ""));
    logOperation(CatalogOperationType.UPDATE_REQUEST, input, additional);
    return input;
  }

  @Override
  public DeleteRequest process(DeleteRequest input)
      throws PluginExecutionException, StopProcessingException {
    String additional = "Deleting: " + input.getAttributeName();
    additional += " with values: " + StringUtils.join(input.getAttributeValues(), ",");
    logOperation(CatalogOperationType.DELETE_REQUEST, input, additional);
    return input;
  }

  @Override
  public QueryRequest process(QueryRequest input)
      throws PluginExecutionException, StopProcessingException {
    String additional = "";
    Query query = input.getQuery();
    if (query instanceof QueryImpl) {
      additional = ((QueryImpl) query).getFilter().toString();
    }
    logOperation(CatalogOperationType.QUERY_REQUEST, input, additional);
    return input;
  }

  @Override
  public ResourceRequest process(ResourceRequest input)
      throws PluginExecutionException, StopProcessingException {
    String additional =
        "Request resource: "
            + input.getAttributeName()
            + " with value "
            + input.getAttributeValue();
    logOperation(CatalogOperationType.RESOURCE_REQUEST, input, additional);
    return input;
  }

  @Override
  public CreateResponse process(CreateResponse input) throws PluginExecutionException {
    String additional =
        input.getCreatedMetacards().stream()
            .map(Metacard::getId)
            .collect(Collectors.joining(",", "Created metacards: ", ""));
    logOperation(CatalogOperationType.INGEST_RESPONSE, input, additional);
    return input;
  }

  @Override
  public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
    String additional =
        input.getUpdatedMetacards().stream()
            .map(Update::getNewMetacard)
            .map(Metacard::getId)
            .collect(Collectors.joining(",", "Updated metacards: ", ""));
    logOperation(CatalogOperationType.UPDATE_RESPONSE, input, additional);
    return input;
  }

  @Override
  public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
    String additional =
        input.getDeletedMetacards().stream()
            .map(Metacard::getId)
            .collect(Collectors.joining(",", "Deleted metacards: ", ""));
    logOperation(CatalogOperationType.DELETE_RESPONSE, input, additional);
    return input;
  }

  @Override
  public QueryResponse process(QueryResponse input)
      throws PluginExecutionException, StopProcessingException {
    String addtional =
        input.getResults().stream()
            .map(Result::getMetacard)
            .map(Metacard::getId)
            .collect(Collectors.joining(",", "Queried metacards: ", ""));
    logOperation(CatalogOperationType.QUERY_RESPONSE, input, addtional);
    return input;
  }

  @Override
  public ResourceResponse process(ResourceResponse input)
      throws PluginExecutionException, StopProcessingException {
    String addtional = "Return resource: " + input.getResource().getName();
    logOperation(CatalogOperationType.RESOURCE_RESPONSE, input, addtional);
    return input;
  }

  @Override
  public QueryRequest process(Source source, QueryRequest input)
      throws PluginExecutionException, StopProcessingException {
    String additional = "";
    Query query = input.getQuery();
    if (query instanceof QueryImpl) {
      additional = ((QueryImpl) query).getFilter().toString() + " for source " + source.getId();
    }
    logOperation(CatalogOperationType.QUERY_REQUEST, input, additional);
    return input;
  }

  @Override
  public CreateStorageRequest process(CreateStorageRequest input) throws PluginExecutionException {
    String additional =
        input.getContentItems().stream()
            .map(ContentItem::getMetacard)
            .map(Metacard::getId)
            .collect(Collectors.joining(",", "Creating products: ", ""));
    logOperation(CatalogOperationType.INGEST_REQUEST, input, additional);
    return input;
  }

  @Override
  public UpdateStorageRequest process(UpdateStorageRequest input) throws PluginExecutionException {
    String additional =
        input.getContentItems().stream()
            .map(ContentItem::getMetacard)
            .map(Metacard::getId)
            .collect(Collectors.joining(",", "Updating products: ", ""));
    logOperation(CatalogOperationType.UPDATE_REQUEST, input, additional);
    return input;
  }

  @Override
  public CreateStorageResponse process(CreateStorageResponse input)
      throws PluginExecutionException {
    String additional =
        input.getCreatedContentItems().stream()
            .map(ContentItem::getMetacard)
            .map(Metacard::getId)
            .collect(Collectors.joining(",", "Created product: ", ""));
    logOperation(CatalogOperationType.INGEST_RESPONSE, input, additional);
    return input;
  }

  @Override
  public UpdateStorageResponse process(UpdateStorageResponse input)
      throws PluginExecutionException {
    String additional =
        input.getUpdatedContentItems().stream()
            .map(ContentItem::getMetacard)
            .map(Metacard::getId)
            .collect(Collectors.joining(",", "Updated products: ", ""));
    logOperation(CatalogOperationType.UPDATE_RESPONSE, input, additional);
    return input;
  }

  private void logOperation(
      CatalogOperationType operationType, Request request, String additionalInfo) {
    if (Requests.isLocal(request)) {
      SecurityLogger.audit("Performing {} operation {} on catalog.", operationType, additionalInfo);
    } else {
      SecurityLogger.audit(
          "Performing {} operation {} on {}", operationType, additionalInfo, request.getStoreIds());
    }
  }

  private void logOperation(
      CatalogOperationType operationType, Response response, String additionalInfo) {
    if (Requests.isLocal(response.getRequest())) {
      SecurityLogger.audit(
          "Receiving results of {} operation {} on catalog.", operationType, additionalInfo);
    } else {
      SecurityLogger.audit(
          "Receiving results of {} operation {} on {}",
          operationType,
          additionalInfo,
          response.getRequest().getStoreIds());
    }
  }

  private enum CatalogOperationType {
    INGEST_REQUEST,
    UPDATE_REQUEST,
    DELETE_REQUEST,
    QUERY_REQUEST,
    RESOURCE_REQUEST,
    INGEST_RESPONSE,
    UPDATE_RESPONSE,
    DELETE_RESPONSE,
    QUERY_RESPONSE,
    RESOURCE_RESPONSE
  }
}
