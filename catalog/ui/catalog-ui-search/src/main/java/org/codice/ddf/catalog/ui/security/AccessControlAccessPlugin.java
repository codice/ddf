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

import static org.codice.ddf.catalog.ui.security.AccessControlUtil.accessAdminHasChanged;
import static org.codice.ddf.catalog.ui.security.AccessControlUtil.accessGroupsHasChanged;
import static org.codice.ddf.catalog.ui.security.AccessControlUtil.accessIndividualsHasChanged;
import static org.codice.ddf.catalog.ui.security.AccessControlUtil.attributeToSet;
import static org.codice.ddf.catalog.ui.security.AccessControlUtil.isAnyObjectNull;

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.data.Metacard;
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
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

public class AccessControlAccessPlugin implements AccessPlugin {

  private final SubjectIdentity subjectIdentity;

  public AccessControlAccessPlugin(SubjectIdentity subjectIdentity) {
    this.subjectIdentity = subjectIdentity;
  }

  protected Subject getSubject() {
    return SecurityUtils.getSubject();
  }

  private String getSubjectIdentifier() {
    return subjectIdentity.getUniqueIdentifier(getSubject());
  }

  // Equivalent to doing a set intersection of the subject with the access-admin list
  private final Predicate<Metacard> subjectIsAccessAdmin =
      (newMetacard) ->
          attributeToSet
              .apply(newMetacard, Security.ACCESS_ADMINISTRATORS)
              .contains(getSubjectIdentifier());

  @VisibleForTesting
  public boolean isAccessControlUpdated(Metacard prev, Metacard updated) {
    return !isAnyObjectNull(prev, updated)
        && accessAdminHasChanged.apply(prev, updated)
        && accessIndividualsHasChanged.apply(prev, updated)
        && accessGroupsHasChanged.apply(prev, updated);
  }

  @Override
  public CreateRequest processPreCreate(CreateRequest input) {
    return input;
  }

  @Override
  public UpdateRequest processPreUpdate(
      UpdateRequest input, Map<String, Metacard> existingMetacards) throws StopProcessingException {

    Function<Metacard, Metacard> oldVersionOfMetacard =
        (update) -> Optional.of(existingMetacards.get(update.getId())).orElse(null);

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
            .filter(subjectIsAccessAdmin.negate())
            .findFirst()
            .isPresent();

    if (foundInaccessibleMetacard) {
      throw new StopProcessingException(
          "Cannot update metacard(s). Subject cannot change sharing permissions because they are not in the assigned access-administrators list.");
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
