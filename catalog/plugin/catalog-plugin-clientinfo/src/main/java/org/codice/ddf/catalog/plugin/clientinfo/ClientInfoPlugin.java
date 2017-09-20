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
package org.codice.ddf.catalog.plugin.clientinfo;

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
import java.io.Serializable;
import java.util.Map;
import org.apache.shiro.util.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Injects client-specific information, if any exists for the current thread, into request
 * properties.
 */
public class ClientInfoPlugin implements PreAuthorizationPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientInfoPlugin.class);

  private static final String CLIENT_INFO_KEY = "client-info";

  @Override
  public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {
    injectClientInfo(input.getProperties());
    return input;
  }

  @Override
  public UpdateRequest processPreUpdate(
      UpdateRequest input, Map<String, Metacard> existingMetacards) throws StopProcessingException {
    injectClientInfo(input.getProperties());
    return input;
  }

  @Override
  public DeleteRequest processPreDelete(DeleteRequest input) throws StopProcessingException {
    injectClientInfo(input.getProperties());
    return input;
  }

  @Override
  public DeleteResponse processPostDelete(DeleteResponse input) throws StopProcessingException {
    injectClientInfo(input.getProperties());
    return input;
  }

  @Override
  public QueryRequest processPreQuery(QueryRequest input) throws StopProcessingException {
    injectClientInfo(input.getProperties());
    return input;
  }

  @Override
  public QueryResponse processPostQuery(QueryResponse input) throws StopProcessingException {
    injectClientInfo(input.getProperties());
    return input;
  }

  @Override
  public ResourceRequest processPreResource(ResourceRequest input) throws StopProcessingException {
    injectClientInfo(input.getProperties());
    return input;
  }

  @Override
  public ResourceResponse processPostResource(ResourceResponse input, Metacard metacard)
      throws StopProcessingException {
    injectClientInfo(input.getProperties());
    return input;
  }

  /**
   * Assuming a client info map was added to the shiro {@link ThreadContext}, we retrieve the value
   * and put it into the request properties. The corresponding CXF filter in {@code
   * platform-filter-clientinfo} is responsible for removing the data to prevent leak.
   *
   * @param properties the request properties for the catalog framework.
   */
  private void injectClientInfo(Map<String, Serializable> properties) {
    Object clientInfo = ThreadContext.get(CLIENT_INFO_KEY);
    if (clientInfo == null) {
      LOGGER.debug(
          "No client info was stored for this thread [{}]", Thread.currentThread().getName());
    } else if (!(clientInfo instanceof Serializable)) {
      LOGGER.debug("Provided client info to the ThreadContext was not Serializable");
    } else {
      properties.put(CLIENT_INFO_KEY, (Serializable) clientInfo);
    }
  }
}
