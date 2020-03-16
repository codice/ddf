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
package org.codice.ddf.catalog.security;

import static ddf.catalog.Constants.OPERATION_TRANSACTION_KEY;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;
import ddf.catalog.util.impl.Requests;
import ddf.security.permission.impl.Permissions;
import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restricts how resource URIs are updated and created. There are security risk to allowing users to
 * update URIs as well as allowing users to pass in a URI when the metacard is created.
 */
public class ResourceUriPolicy implements PolicyPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceUriPolicy.class);

  private String[] createPermissions;

  private String[] updatePermissions;

  public ResourceUriPolicy() {}

  public ResourceUriPolicy(String[] createPermissions, String[] updatePermissions) {
    setCreatePermissions(createPermissions);
    setUpdatePermissions(updatePermissions);
  }

  @Override
  public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {

    if (!Requests.isLocal(properties)) {
      return new PolicyResponseImpl();
    }

    if (input.getResourceURI() != null
        && StringUtils.isNotEmpty(input.getResourceURI().toString())) {
      return new PolicyResponseImpl(
          null, Permissions.parsePermissionsFromString(getCreatePermissions()));
    }

    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreUpdate(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {

    if (!Requests.isLocal(properties)) {
      return new PolicyResponseImpl();
    }

    PolicyResponseImpl policyResponse =
        new PolicyResponseImpl(
            null, Permissions.parsePermissionsFromString(getUpdatePermissions()));

    List<Metacard> previousStateMetacards =
        ((OperationTransaction) properties.get(OPERATION_TRANSACTION_KEY))
            .getPreviousStateMetacards();

    Metacard previous =
        previousStateMetacards
            .stream()
            .filter((x) -> x.getId().equals(input.getId()))
            .findFirst()
            .orElse(null);

    if (previous == null) {
      LOGGER.debug(
          "Cannot locate metacard {} for update. Applying permissions to the item", input.getId());
      return policyResponse;
    }

    return requiresPermission(input.getResourceURI(), previous.getResourceURI())
        ? policyResponse
        : new PolicyResponseImpl();
  }

  private boolean requiresPermission(URI input, URI catalog) {

    return !uriToString(input).equals(uriToString(catalog));
  }

  private String uriToString(URI uri) {
    return uri == null ? "" : uri.toString();
  }

  @Override
  public PolicyResponse processPreDelete(
      List<Metacard> metacards, Map<String, Serializable> properties)
      throws StopProcessingException {

    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostDelete(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreQuery(Query query, Map<String, Serializable> properties)
      throws StopProcessingException {

    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreResource(ResourceRequest resourceRequest)
      throws StopProcessingException {

    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  public String[] getCreatePermissions() {
    return createPermissions == null ? null : createPermissions.clone();
  }

  public void setCreatePermissions(String[] createPermissions) {

    this.createPermissions = createPermissions == null ? null : createPermissions.clone();
  }

  public String[] getUpdatePermissions() {
    return updatePermissions == null ? null : updatePermissions.clone();
  }

  public void setUpdatePermissions(String[] updatePermissions) {
    this.updatePermissions = updatePermissions == null ? null : updatePermissions.clone();
  }
}
