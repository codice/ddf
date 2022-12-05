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
package ddf.catalog.plugin;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;
import java.util.Map;

/**
 * An AccessPlugin allows or denies access to the Catalog operation or response. AccessPlugin are
 * independent and should not rely on ordering of other plugins
 */
@Deprecated
public interface AccessPlugin {

  /**
   * Processes a {@link CreateRequest}, prior to {@link
   * ddf.catalog.source.CatalogProvider#create(CreateRequest)}, to determine whether or not the user
   * can access the Catalog operation
   *
   * @param input the {@link CreateRequest} to process
   * @return the value of the processed {@link CreateRequest} to pass to the next {@link
   *     AccessPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException;

  /**
   * Processes an {@link UpdateRequest}, prior to execution of the {@link
   * ddf.catalog.operation.Update}, to determine whether or not the user can access the Catalog
   * operation
   *
   * @param input the {@link UpdateRequest} to process
   * @param existingMetacards the Map of {@link Metacard}s that currently exist
   * @return the value of the processed {@link UpdateRequest} to pass to the next {@link
   *     AccessPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  UpdateRequest processPreUpdate(UpdateRequest input, Map<String, Metacard> existingMetacards)
      throws StopProcessingException;

  /**
   * Processes a {@link DeleteRequest}, prior to {@link
   * ddf.catalog.source.CatalogProvider#delete(DeleteRequest)}, to determine whether or not the user
   * can access the Catalog operation
   *
   * @param input the {@link DeleteRequest} to process
   * @return the value of the processed {@link DeleteRequest} to pass to the next {@link
   *     AccessPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  DeleteRequest processPreDelete(DeleteRequest input) throws StopProcessingException;

  /**
   * Processes a {@link DeleteResponse}, following the execution of {@link
   * ddf.catalog.source.CatalogProvider#delete(DeleteRequest)}, to determine whether or not the user
   * can access the Catalog operation
   *
   * @param input the {@link DeleteResponse} to process
   * @return the value of the processed {@link DeleteResponse} to pass to the next {@link
   *     AccessPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  DeleteResponse processPostDelete(DeleteResponse input) throws StopProcessingException;

  /**
   * Processes a {@link QueryRequest}, prior to execution of the {@link
   * ddf.catalog.operation.Query}, to determine whether or not the user can access the Catalog
   * operation. The AccessPlugins will be run in the same order for both processPreQuery and
   * processPostQuery
   *
   * @param input the {@link QueryRequest} to process
   * @return the value of the processed {@link QueryRequest} to pass to the next {@link
   *     AccessPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  QueryRequest processPreQuery(QueryRequest input) throws StopProcessingException;

  /**
   * Processes a {@link QueryResponse}, following the execution of the {@link
   * ddf.catalog.operation.Query}, to determine whether or not the user can access the Response. The
   * AccessPlugins will be run in the same order for both processPreQuery and processPostQuery
   *
   * @param input the {@link QueryResponse} to process
   * @return the value of the processed {@link QueryResponse} to pass to the next {@link
   *     AccessPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  QueryResponse processPostQuery(QueryResponse input) throws StopProcessingException;

  /**
   * Processes a {@link ResourceRequest}, prior to execution of the {@link
   * ddf.catalog.operation.ResourceRequest}, to determine whether or not the user can access the
   * Catalog operation. The AccessPlugins will be run in the same order for both processPreQuery and
   * processPostQuery
   *
   * @param input the {@link ResourceRequest} to process
   * @return the value of the processed {@link QueryRequest} to pass to the next {@link
   *     AccessPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  ResourceRequest processPreResource(ResourceRequest input) throws StopProcessingException;

  /**
   * Processes a {@link ResourceResponse}, following the execution of the {@link
   * ddf.catalog.operation.ResourceRequest}, to determine whether or not the user can access the
   * Response. The AccessPlugins will be run in the same order for both processPreResource and
   * processPostResource
   *
   * @param input the {@link ResourceResponse} to process
   * @return the value of the processed {@link ResourceResponse} to pass to the next {@link
   *     AccessPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  ResourceResponse processPostResource(ResourceResponse input, Metacard metacard)
      throws StopProcessingException;
}
