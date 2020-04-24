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
package org.codice.ddf.catalog.ui.security.accesscontrol;

import static ddf.catalog.Constants.OPERATION_TRANSACTION_KEY;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.ACCESS_ADMIN_HAS_CHANGED;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.ACCESS_INDIVIDUALS_HAS_CHANGED;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.ACCESS_INDIVIDUALS_READ_HAS_CHANGED;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.CONTAINS_ACL_ATTRIBUTES;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.attributeHasChanged;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.getValuesOrEmpty;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.isAnyObjectNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Security;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.impl.PolicyResponseImpl;
import ddf.security.SubjectIdentity;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.shiro.SecurityUtils;

public class AccessControlPolicyPlugin implements PolicyPlugin {

  private Supplier<String> subjectSupplier;

  public AccessControlPolicyPlugin(SubjectIdentity subjectIdentity) {
    this.subjectSupplier = () -> subjectIdentity.getUniqueIdentifier(SecurityUtils.getSubject());
  }

  private Map<String, Set<String>> getAccessAdministratorPermission(Metacard metacard) {
    return ImmutableMap.of(
        Security.ACCESS_ADMINISTRATORS, AccessControlUtil.getAccessAdministrators(metacard));
  }

  private Map<String, Set<String>> getIndividualPermission(Metacard metacard) {
    return ImmutableMap.of(
        Security.ACCESS_INDIVIDUALS, AccessControlUtil.getAccessIndividuals(metacard));
  }

  private Map<String, Set<String>> getReadOnlyIndividualPermission(Metacard metacard) {
    return ImmutableMap.of(
        Security.ACCESS_INDIVIDUALS_READ, AccessControlUtil.getAccessReadOnlyIndividuals(metacard));
  }

  private Map<String, Set<String>> getGroupPermission(Metacard metacard) {
    return ImmutableMap.of(Security.ACCESS_GROUPS, AccessControlUtil.getAccessGroups(metacard));
  }

  private Map<String, Set<String>> getReadOnlyGroupPermission(Metacard metacard) {
    return ImmutableMap.of(
        Security.ACCESS_GROUPS_READ, AccessControlUtil.getAccessReadOnlyGroups(metacard));
  }

  private Map<String, Set<String>> getOwner(Metacard metacard) {
    return ImmutableMap.of(Core.METACARD_OWNER, AccessControlUtil.getOwnerOrEmptySet(metacard));
  }

  private Map<String, Set<String>> getPolicyForMetacard(Metacard metacard) {
    return Stream.of(
            getOwner(metacard),
            getAccessAdministratorPermission(metacard),
            getGroupPermission(metacard),
            getReadOnlyGroupPermission(metacard),
            getReadOnlyIndividualPermission(metacard),
            getIndividualPermission(metacard))
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .filter(entry -> !entry.getValue().isEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Sets::union));
  }

  private Map<String, Set<String>> getPolicy(Metacard metacard) {
    return Optional.of(metacard)
        .filter(CONTAINS_ACL_ATTRIBUTES)
        .map(this::getPolicyForMetacard)
        .orElseGet(ImmutableMap::of);
  }

  private Map<String, Set<String>> getPolicy(List<Metacard> metacards) {
    return metacards
        .stream()
        .filter(CONTAINS_ACL_ATTRIBUTES)
        .map(this::getPolicy)
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Sets::union));
  }

  @Override
  public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties) {
    Metacard metacard = input.getMetacard();
    return new PolicyResponseImpl(Collections.emptyMap(), getPolicy(metacard));
  }

  private boolean isUserOnlyRemoved(Metacard prev, Metacard updated, String attribute) {
    List<String> prevValues = new ArrayList<>(getValuesOrEmpty(prev, attribute));
    List<String> updatedValues = getValuesOrEmpty(updated, attribute);
    if (prevValues.size() > updatedValues.size()) {
      prevValues.removeAll(updatedValues);
      return prevValues.size() == 1 && prevValues.contains(subjectSupplier.get());
    }
    return false;
  }

  private boolean isOnlyUserRemovedFromAccessControlAttribute(
      Metacard prev, Metacard updated, String attribute) {
    if (!attributeHasChanged(prev, updated, attribute)) {
      return true;
    }
    return (Security.ACCESS_ADMINISTRATORS.equals(attribute)
            || Security.ACCESS_INDIVIDUALS.equals(attribute)
            || Security.ACCESS_INDIVIDUALS_READ.equals(attribute))
        && isUserOnlyRemoved(prev, updated, attribute);
  }

  private boolean onlyUserAccessControlRemoved(Metacard prev, Metacard updated) {
    if (isAnyObjectNull(prev, updated)
        || getOwner(prev).equals(subjectSupplier.get())
        || (!ACCESS_ADMIN_HAS_CHANGED.apply(prev, updated)
            && !ACCESS_INDIVIDUALS_HAS_CHANGED.apply(prev, updated)
            && !ACCESS_INDIVIDUALS_READ_HAS_CHANGED.apply(prev, updated))) {
      return false;
    }

    Set<AttributeDescriptor> attributeDescriptors =
        new HashSet<>(prev.getMetacardType().getAttributeDescriptors());
    attributeDescriptors.addAll(updated.getMetacardType().getAttributeDescriptors());

    return attributeDescriptors
        .stream()
        .allMatch(
            attribute ->
                isOnlyUserRemovedFromAccessControlAttribute(prev, updated, attribute.getName()));
  }

  @Override
  public PolicyResponse processPreUpdate(Metacard metacard, Map<String, Serializable> properties) {
    Map<String, Set<String>> policy = getPolicy(metacard);
    Optional<Metacard> oldMetacard =
        ((OperationTransaction) properties.get(OPERATION_TRANSACTION_KEY))
            .getPreviousStateMetacards()
            .stream()
            .filter(m -> metacard.getId().equals(m.getId()))
            .findFirst();
    if (oldMetacard.isPresent() && onlyUserAccessControlRemoved(oldMetacard.get(), metacard)) {
      policy.put("remove-user-access", Collections.singleton(subjectSupplier.get()));
    }
    return new PolicyResponseImpl(Collections.emptyMap(), policy);
  }

  @Override
  public PolicyResponse processPreDelete(
      List<Metacard> metacards, Map<String, Serializable> properties) {
    return new PolicyResponseImpl(getPolicy(metacards), getPolicy(metacards));
  }

  @Override
  public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties) {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostDelete(Metacard input, Map<String, Serializable> properties) {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreQuery(Query query, Map<String, Serializable> properties) {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreResource(ResourceRequest resourceRequest) {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard) {
    return new PolicyResponseImpl();
  }
}
