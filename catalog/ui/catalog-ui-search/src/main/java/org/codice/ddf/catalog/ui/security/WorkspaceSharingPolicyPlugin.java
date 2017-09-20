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
import com.google.common.collect.Sets;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Security;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;

public class WorkspaceSharingPolicyPlugin implements PolicyPlugin {

  private static Map<String, Set<String>> getWorkspaceSecurityTag() {
    return ImmutableMap.of(Constants.IS_WORKSPACE, ImmutableSet.of(Constants.IS_WORKSPACE));
  }

  private static Map<String, Set<String>> getOwnerPermission(String owner) {
    return ImmutableMap.of(Core.METACARD_OWNER, ImmutableSet.of(owner));
  }

  private Map<String, Set<String>> getOwnerPermission(WorkspaceMetacardImpl workspace) {
    return Optional.of(workspace)
        .map(WorkspaceMetacardImpl::getOwner)
        .filter(StringUtils::isNotEmpty)
        .map(WorkspaceSharingPolicyPlugin::getOwnerPermission)
        .orElseGet(ImmutableMap::of);
  }

  private Map<String, Set<String>> getIndividualPermission(WorkspaceMetacardImpl workspace) {
    return ImmutableMap.of(
        Security.ACCESS_INDIVIDUALS,
        Optional.of(workspace)
            .map(w -> w.getAttribute(Security.ACCESS_INDIVIDUALS))
            .map(Attribute::getValues)
            .orElseGet(Collections::emptyList)
            .stream()
            .map(Object::toString)
            .collect(Collectors.toSet()));
  }

  private Map<String, Set<String>> getGroupPermission(WorkspaceMetacardImpl workspace) {
    return ImmutableMap.of(
        Security.ACCESS_GROUPS,
        Optional.of(workspace)
            .map(w -> w.getAttribute(Security.ACCESS_GROUPS))
            .map(Attribute::getValues)
            .orElseGet(Collections::emptyList)
            .stream()
            .map(Object::toString)
            .collect(Collectors.toSet()));
  }

  private Map<String, Set<String>> getPolicy(WorkspaceMetacardImpl workspace) {
    return Stream.of(
            getWorkspaceSecurityTag(),
            getOwnerPermission(workspace),
            getGroupPermission(workspace),
            getIndividualPermission(workspace))
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .filter(entry -> !entry.getValue().isEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Sets::union));
  }

  private Map<String, Set<String>> getPolicy(Metacard metacard) {
    return Optional.of(metacard)
        .filter(WorkspaceMetacardImpl::isWorkspaceMetacard)
        .map(WorkspaceMetacardImpl::from)
        .map(this::getPolicy)
        .orElseGet(ImmutableMap::of);
  }

  private Map<String, Set<String>> getPolicy(List<Metacard> metacards) {
    return metacards
        .stream()
        .filter(WorkspaceMetacardImpl::isWorkspaceMetacard)
        .map(WorkspaceMetacardImpl::from)
        .map(this::getPolicy)
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Sets::union));
  }

  @Override
  public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
      throws StopProcessingException {
    Metacard metacard = input.getMetacard();
    return new PolicyResponseImpl(Collections.emptyMap(), getPolicy(metacard));
  }

  @Override
  public PolicyResponse processPreUpdate(Metacard metacard, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl(Collections.emptyMap(), getPolicy(metacard));
  }

  @Override
  public PolicyResponse processPreDelete(
      List<Metacard> metacards, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl(getPolicy(metacards), getPolicy(metacards));
  }

  @Override
  public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
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
  public PolicyResponse processPreResource(ResourceRequest resourceRequest)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }
}
