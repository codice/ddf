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
package org.codice.ddf.catalog.ui.security;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;

public class WorkspaceAccessPlugin implements AccessPlugin {

  protected Subject getSubject() {
    return SecurityUtils.getSubject();
  }

  private KeyValueCollectionPermission isOwnerPermission(String owner) {
    Map<String, Set<String>> securityAttributes =
        ImmutableMap.of(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of(owner));

    return new KeyValueCollectionPermission(CollectionPermission.UPDATE_ACTION, securityAttributes);
  }

  private boolean isSharingUpdated(WorkspaceMetacardImpl previous, WorkspaceMetacardImpl update) {
    return !update.diffSharing(previous).isEmpty();
  }

  private boolean isNotOwner(String owner) {
    return !getSubject().isPermitted(isOwnerPermission(owner));
  }

  @Override
  public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public UpdateRequest processPreUpdate(
      UpdateRequest input, Map<String, Metacard> existingMetacards) throws StopProcessingException {

    Function<WorkspaceMetacardImpl, WorkspaceMetacardImpl> previous =
        (update) -> WorkspaceMetacardImpl.from(existingMetacards.get(update.getId()));

    Set<String> notOwners =
        input
            .getUpdates()
            .stream()
            .map(Map.Entry::getValue)
            .filter(WorkspaceMetacardImpl::isWorkspaceMetacard)
            .map(WorkspaceMetacardImpl::from)
            .filter(update -> isSharingUpdated(previous.apply(update), update))
            .map(previous)
            .map(WorkspaceMetacardImpl::getOwner)
            .filter(this::isNotOwner)
            .collect(Collectors.toSet());

    if (!notOwners.isEmpty()) {
      throw new StopProcessingException(
          "Cannot update workspace. Subject cannot change sharing permissions because they are not the owner.");
    }

    return input;
  }

  @Override
  public DeleteRequest processPreDelete(DeleteRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public DeleteResponse processPostDelete(DeleteResponse input) throws StopProcessingException {
    return input;
  }

  @Override
  public QueryRequest processPreQuery(QueryRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public QueryResponse processPostQuery(QueryResponse input) throws StopProcessingException {
    return input;
  }

  @Override
  public ResourceRequest processPreResource(ResourceRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public ResourceResponse processPostResource(ResourceResponse input, Metacard metacard)
      throws StopProcessingException {
    return input;
  }
}
