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
 * The {@link PreAuthorizationPlugin} extension point allows for request/response processing before
 * any security rules (policy and access) are implemented at the beginning of the plugin chain. That
 * is, pre-processing occurs before policy, access, and the catalog framework. Post-processing
 * occurs after the catalog framework but before the second round of policy and access.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
@Deprecated
public interface PreAuthorizationPlugin {

  /**
   * Process a {@link CreateRequest} for use cases that occur prior to security rules.
   *
   * @param input the {@link CreateRequest} to process
   * @return the value of the processed {@link CreateRequest} to pass to the next {@link
   *     PreAuthorizationPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException;

  /**
   * Process an {@link UpdateRequest} for use cases that occur prior to security rules.
   *
   * @param input the {@link UpdateRequest} to process
   * @param existingMetacards the Map of {@link Metacard}s that currently exist
   * @return the value of the processed {@link UpdateRequest} to pass to the next {@link
   *     PreAuthorizationPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  UpdateRequest processPreUpdate(UpdateRequest input, Map<String, Metacard> existingMetacards)
      throws StopProcessingException;

  /**
   * Process a {@link DeleteRequest} for use cases that occur prior to security rules.
   *
   * @param input the {@link DeleteRequest} to process
   * @return the value of the processed {@link DeleteRequest} to pass to the next {@link
   *     PreAuthorizationPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  DeleteRequest processPreDelete(DeleteRequest input) throws StopProcessingException;

  /**
   * Process a {@link DeleteResponse} for use cases that occur prior to security rules.
   *
   * @param input the {@link DeleteResponse} to process
   * @return the value of the processed {@link DeleteResponse} to pass to the next {@link
   *     PreAuthorizationPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  DeleteResponse processPostDelete(DeleteResponse input) throws StopProcessingException;

  /**
   * Process a {@link QueryRequest} for use cases that occur prior to security rules.
   *
   * @param input the {@link QueryRequest} to process
   * @return the value of the processed {@link QueryRequest} to pass to the next {@link
   *     PreAuthorizationPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  QueryRequest processPreQuery(QueryRequest input) throws StopProcessingException;

  /**
   * Process a {@link QueryResponse} for use cases that occur prior to security rules.
   *
   * @param input the {@link QueryResponse} to process
   * @return the value of the processed {@link QueryResponse} to pass to the next {@link
   *     PreAuthorizationPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  QueryResponse processPostQuery(QueryResponse input) throws StopProcessingException;

  /**
   * Process a {@link ResourceRequest} for use cases that occur prior to security rules.
   *
   * @param input the {@link ResourceRequest} to process
   * @return the value of the processed {@link QueryRequest} to pass to the next {@link
   *     PreAuthorizationPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  ResourceRequest processPreResource(ResourceRequest input) throws StopProcessingException;

  /**
   * Process a {@link ResourceResponse} for use cases that occur prior to security rules.
   *
   * @param input the {@link ResourceResponse} to process
   * @return the value of the processed {@link ResourceResponse} to pass to the next {@link
   *     PreAuthorizationPlugin}
   * @throws StopProcessingException thrown to halt processing when a critical issue occurs during
   *     processing. This is intended to prevent other plugins from processing as well.
   */
  ResourceResponse processPostResource(ResourceResponse input, Metacard metacard)
      throws StopProcessingException;
}
