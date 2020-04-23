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

import static org.codice.ddf.catalog.ui.forms.data.AttributeGroupType.ATTRIBUTE_GROUP_TAG;
import static org.codice.ddf.catalog.ui.forms.data.QueryTemplateType.QUERY_TEMPLATE_TAG;
import static org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants.WORKSPACE_TAG;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.ACCESS_ADMIN_HAS_CHANGED;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.ACCESS_GROUPS_HAS_CHANGED;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.ACCESS_GROUPS_READ_HAS_CHANGED;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.ACCESS_INDIVIDUALS_HAS_CHANGED;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.ACCESS_INDIVIDUALS_READ_HAS_CHANGED;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.ATTRIBUTE_TO_SET;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.OWNER_HAS_CHANGED;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil.isAnyObjectNull;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Security;
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
import ddf.security.SubjectIdentity;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.shiro.SecurityUtils;

public class AccessControlAccessPlugin implements AccessPlugin {

  private static final String FAILURE_OWNER_CANNOT_CHANGE =
      "Cannot update metacard(s). Owner cannot be changed.";

  private static final String FAILURE_NOT_ADMIN_OR_OWNER =
      "Cannot update metacard(s). "
          + "Subject cannot change access control permissions because they are not in the assigned "
          + "access-administrators list.";

  private Supplier<String> subjectSupplier;

  public AccessControlAccessPlugin(SubjectIdentity subjectIdentity) {
    this.subjectSupplier = () -> subjectIdentity.getUniqueIdentifier(SecurityUtils.getSubject());
  }

  // Because embedding '!' in the following predicates might be easily overlooked
  private static boolean invert(boolean b) {
    return !b;
  }

  // Equivalent to doing a set intersection of the subject with the access-admin list
  private final Predicate<Metacard> subjectIsNotAccessAdmin =
      newMetacard ->
          invert(
              ATTRIBUTE_TO_SET
                  .apply(newMetacard, Security.ACCESS_ADMINISTRATORS)
                  .contains(subjectSupplier.get()));

  private final Predicate<Metacard> subjectIsNotOwner =
      newMetacard ->
          invert(
              ATTRIBUTE_TO_SET
                  .apply(newMetacard, Core.METACARD_OWNER)
                  .contains(subjectSupplier.get()));

  private final Predicate<Metacard> hasIntrigueTag =
      metacard ->
          metacard.getTags().contains(WORKSPACE_TAG)
              || metacard.getTags().contains(QUERY_TEMPLATE_TAG)
              || metacard.getTags().contains(ATTRIBUTE_GROUP_TAG);

  private boolean isAccessControlUpdated(Metacard prev, Metacard updated) {
    return !isAnyObjectNull(prev, updated)
        && (ACCESS_ADMIN_HAS_CHANGED.apply(prev, updated)
            || ACCESS_INDIVIDUALS_HAS_CHANGED.apply(prev, updated)
            || ACCESS_INDIVIDUALS_READ_HAS_CHANGED.apply(prev, updated)
            || ACCESS_GROUPS_HAS_CHANGED.apply(prev, updated)
            || ACCESS_GROUPS_READ_HAS_CHANGED.apply(prev, updated));
  }

  private boolean isOwnerChanged(Metacard prev, Metacard updated) {
    return !isAnyObjectNull(prev, updated) && OWNER_HAS_CHANGED.apply(prev, updated);
  }

  private boolean hasRemoveAccess(Metacard updated) {
    Attribute attr = updated.getAttribute(Metacard.SECURITY);
    if (attr == null) {
      return false;
    }
    Map<String, Set<String>> map = (Map<String, Set<String>>) attr.getValue();
    if (map == null) {
      return false;
    }
    KeyValueCollectionPermission securityPermission =
        new KeyValueCollectionPermission(CollectionPermission.UPDATE_ACTION, map);
    return securityPermission
        .getPermissionList()
        .stream()
        .filter(p -> p instanceof KeyValuePermission)
        .map(p -> (KeyValuePermission) p)
        .anyMatch(
            p ->
                "remove-user-access".equals(p.getKey())
                    && p.getValues().contains(subjectSupplier.get()));
  }

  @Override
  public CreateRequest processPreCreate(CreateRequest input) {
    return input;
  }

  @Override
  public UpdateRequest processPreUpdate(
      UpdateRequest input, Map<String, Metacard> existingMetacards) throws StopProcessingException {
    Map<String, Metacard> filteredExistingMetacards =
        existingMetacards
            .values()
            .stream()
            .filter(hasIntrigueTag)
            .collect(Collectors.toMap(Metacard::getId, Function.identity()));

    Function<Metacard, Metacard> oldMetacard =
        update ->
            Optional.ofNullable(update.getId()).map(filteredExistingMetacards::get).orElse(null);

    List<Metacard> newMetacards =
        input.getUpdates().stream().map(Map.Entry::getValue).collect(Collectors.toList());

    if (newMetacards.stream().anyMatch(m -> isOwnerChanged(oldMetacard.apply(m), m))) {
      throw new StopProcessingException(FAILURE_OWNER_CANNOT_CHANGE);
    }

    if (newMetacards
        .stream()
        .filter(
            newVersionOfMetacard ->
                isAccessControlUpdated(
                    oldMetacard.apply(newVersionOfMetacard), newVersionOfMetacard))
        .filter(newVersionOfMetacard -> !hasRemoveAccess(newVersionOfMetacard))
        .filter(Objects::nonNull)
        .map(oldMetacard)
        .filter(Objects::nonNull)
        .filter(subjectIsNotOwner)
        .anyMatch(subjectIsNotAccessAdmin)) {
      throw new StopProcessingException(FAILURE_NOT_ADMIN_OR_OWNER);
    }

    return input;
  }

  @Override
  public DeleteRequest processPreDelete(DeleteRequest input) {
    return input;
  }

  @Override
  public DeleteResponse processPostDelete(DeleteResponse input) {
    return input;
  }

  @Override
  public QueryRequest processPreQuery(QueryRequest input) {
    return input;
  }

  @Override
  public QueryResponse processPostQuery(QueryResponse input) {
    return input;
  }

  @Override
  public ResourceRequest processPreResource(ResourceRequest input) {
    return input;
  }

  @Override
  public ResourceResponse processPostResource(ResourceResponse input, Metacard metacard) {
    return input;
  }
}
