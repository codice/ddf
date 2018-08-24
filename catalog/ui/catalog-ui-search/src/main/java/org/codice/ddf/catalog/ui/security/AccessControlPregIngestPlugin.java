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

import static org.codice.ddf.catalog.ui.security.AccessControlUtil.containsACLAttributes;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SubjectIdentity;
import ddf.security.principal.GuestPrincipal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

public class AccessControlPregIngestPlugin implements PreIngestPlugin {

  private final SubjectIdentity subjectIdentity;

  public AccessControlPregIngestPlugin(SubjectIdentity subjectIdentity) {
    this.subjectIdentity = subjectIdentity;
  }

  private static Map<String, Metacard> getPreviousMetacards(UpdateRequest request) {
    OperationTransaction operationTransaction =
        (OperationTransaction) request.getProperties().get(Constants.OPERATION_TRANSACTION_KEY);

    return operationTransaction
        .getPreviousStateMetacards()
        .stream()
        .filter(containsACLAttributes)
        .collect(Collectors.toMap(Metacard::getId, m -> m));
  }

  private static void copyOwner(Metacard source, Metacard target) {
    if (source == null || target == null) {
      return;
    }
    AccessControlUtil.setOwner(target, AccessControlUtil.getOwner(source));
  }

  /**
   * Ensures a sharing metacard has an owner.
   *
   * @param request the {@link CreateRequest} to process
   * @throws PluginExecutionException
   * @throws StopProcessingException - if the current subject doesn't have an email attribute
   */
  @Override
  // TODO: Wanna just copy the owner to the access admin list now?
  public CreateRequest process(CreateRequest request) throws StopProcessingException {
    Subject ownerSubject = getSubject();
    final String owner = subjectIdentity.getUniqueIdentifier(ownerSubject);

    List<Metacard> metacards =
        request
            .getMetacards()
            .stream()
            .filter(containsACLAttributes)
            .filter(metacard -> StringUtils.isEmpty(AccessControlUtil.getOwner(metacard)))
            .collect(Collectors.toList());

    if (!metacards.isEmpty() && isGuest(ownerSubject)) {
      throw new StopProcessingException(
          "Guest user not allowed to create access-controlled resources");
    }

    metacards.forEach(metacard -> AccessControlUtil.setOwner(metacard, owner));

    return request;
  }

  /**
   * Ensures the owner attribute is always present.
   *
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  @Override
  @SuppressWarnings("squid:S1854" /*previous is used and makes the stream forEach more efficient*/)
  public UpdateRequest process(UpdateRequest request) {
    // TODO: Data cleansng for adding owner to access admin list?
    request
        .getUpdates()
        .stream()
        .map(Map.Entry::getValue)
        .filter(containsACLAttributes)
        .filter(metacard -> StringUtils.isEmpty(AccessControlUtil.getOwner(metacard)))
        .forEach(
            shareableMetacard ->
                copyOwner(
                    getPreviousMetacards(request).get(shareableMetacard.getId()),
                    shareableMetacard));

    return request;
  }

  @Override
  public DeleteRequest process(DeleteRequest input) {
    return input;
  }

  protected Subject getSubject() {
    return SecurityUtils.getSubject();
  }

  private boolean isGuest(Subject subject) {
    return subject.getPrincipal() instanceof GuestPrincipal;
  }
}
