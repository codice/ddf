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

import static org.codice.ddf.catalog.ui.security.AccessControlUtil.ACCESS_ADMIN_HAS_CHANGED;
import static org.codice.ddf.catalog.ui.security.AccessControlUtil.ACCESS_GROUPS_HAS_CHANGED;
import static org.codice.ddf.catalog.ui.security.AccessControlUtil.ACCESS_INDIVIDUALS_HAS_CHANGED;
import static org.codice.ddf.catalog.ui.security.AccessControlUtil.ATTRIBUTE_TO_SET;
import static org.codice.ddf.catalog.ui.security.AccessControlUtil.CONTAINS_ACL_ATTRIBUTES;
import static org.codice.ddf.catalog.ui.security.AccessControlUtil.isAnyObjectNull;

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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

public class AccessControlAccessPlugin implements AccessPlugin {

  private Supplier<String> subjectIdentitySupplier;

  private Supplier<Subject> subjectSupplier;

  public AccessControlAccessPlugin(SubjectIdentity subjectIdentity) {
    this.subjectIdentitySupplier =
        () -> subjectIdentity.getUniqueIdentifier(SecurityUtils.getSubject());
    this.subjectSupplier = () -> SecurityUtils.getSubject();
  }

  // Equivalent to doing a set intersection of the subject with the access-admin list
  private final Predicate<Metacard> subjectIsAccessAdmin =
      (newMetacard) ->
          ATTRIBUTE_TO_SET
              .apply(newMetacard, Security.ACCESS_ADMINISTRATORS)
              .contains(subjectIdentitySupplier.get());

  private final Predicate<Metacard> subjectHasRole =
      (newMetacard) -> {
        if (newMetacard.getAttribute(Security.ACCESS_GROUPS) == null) {
          return false;
        }
        for (String item : ATTRIBUTE_TO_SET.apply(newMetacard, Security.ACCESS_GROUPS)) {
          if (subjectSupplier.get().hasRole(item)) {
            return true;
          }
        }
        return false;
      };

  private final Predicate<Metacard> subjectHasIndividual =
      (newMetacard) ->
          ATTRIBUTE_TO_SET
              .apply(newMetacard, Security.ACCESS_INDIVIDUALS)
              .contains(subjectIdentitySupplier.get());

  private final Predicate<Metacard> subjectHasWritePerms =
      (newMetacard) -> subjectHasRole.test(newMetacard) || subjectHasIndividual.test(newMetacard);

  private final Predicate<Metacard> subjectIsOwner =
      (newMetacard) ->
          ATTRIBUTE_TO_SET
              .apply(newMetacard, Core.METACARD_OWNER)
              .contains(subjectIdentitySupplier.get());

  private boolean isAccessControlUpdated(Metacard prev, Metacard updated) {
    return !isAnyObjectNull(prev, updated)
        && (ACCESS_ADMIN_HAS_CHANGED.apply(prev, updated)
            || ACCESS_INDIVIDUALS_HAS_CHANGED.apply(prev, updated)
            || ACCESS_GROUPS_HAS_CHANGED.apply(prev, updated));
  }

  @Override
  public CreateRequest processPreCreate(CreateRequest input) {
    return input;
  }

  @Override
  public UpdateRequest processPreUpdate(
      UpdateRequest input, Map<String, Metacard> existingMetacards) throws StopProcessingException {

    Function<Metacard, Metacard> oldVersionOfMetacard =
        (update) -> {
          Optional<Metacard> oldMetacard =
              Optional.ofNullable(existingMetacards.get(update.getId()));
          if (oldMetacard.isPresent()) {
            return oldMetacard.get();
          }
          return null;
        };

    Predicate<Metacard> nonWriteableMetacard =
        (mc) ->
            !subjectIsAccessAdmin.test(mc)
                && !subjectIsOwner.test(mc)
                && !subjectHasWritePerms.test(mc);

    /**
     * Since this is an update request, it implies that the metacard is changing. The only way for a
     * metacard to change is if the requesting subject has the perms to do so. The required perms
     * require to be one of the following: <br>
     *
     * <ul>
     *   <li>Access Administrator
     *   <li>Current owner of the metacard
     *   <li>Be explicitly placed in the R/W group on the security attributes
     * </ul>
     */
    boolean foundNonWriteableMetacard =
        input
            .getUpdates()
            .stream()
            .map(Map.Entry::getValue)
            .filter(Objects::nonNull)
            .filter(CONTAINS_ACL_ATTRIBUTES)
            .filter(nonWriteableMetacard)
            .findFirst()
            .isPresent();

    if (foundNonWriteableMetacard) {
      throw new StopProcessingException(
          "Cannot update metacard(s). Subject cannot change access control permissions because they are not in set of users with writeable permissions.");
    }

    boolean foundInaccessibleMetacard =
        input
            .getUpdates()
            .stream()
            .map(Map.Entry::getValue)
            .filter(
                newVersionOfMetacard ->
                    isAccessControlUpdated(
                        oldVersionOfMetacard.apply(newVersionOfMetacard), newVersionOfMetacard))
            .filter(Objects::nonNull)
            .filter(mc -> !subjectIsAccessAdmin.test(mc) && !subjectIsOwner.test(mc))
            .findFirst()
            .isPresent();

    if (foundInaccessibleMetacard) {
      throw new StopProcessingException(
          "Cannot update metacard(s). Subject cannot change access control permissions because they are not in the assigned access-administrators list.");
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
