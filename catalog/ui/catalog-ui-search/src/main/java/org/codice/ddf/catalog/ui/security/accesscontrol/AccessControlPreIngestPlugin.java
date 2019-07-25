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

import static org.codice.ddf.catalog.ui.security.Constants.SYSTEM_TEMPLATE;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Security;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SubjectIdentity;
import ddf.security.SubjectUtils;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.catalog.ui.forms.data.AttributeGroupType;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateType;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants;

public class AccessControlPreIngestPlugin implements PreIngestPlugin {

  private final SubjectIdentity subjectIdentity;

  /**
   * This set is final and not supposed to be added to. This plugin is designed to support backwards
   * compatability for the existing "ACL" types. Currently, the only requirement for something to be
   * ACL capable is to have at least one security attribute set on the metacard.
   *
   * <p>- access-individuals - access-groups - access-administrators
   *
   * <p>This plugin specifically ensures that guests cannot create metacards that are ACL
   * controlled, which pertains to the subset of metacards directly that are (workspaces, query
   * templates, attribute group types). For extensibility, this plugin is irrelevant. For new
   * metacards that need to be access-controlled, this plugin is irrelevant.
   */
  private final Set<String> aclMetacardTypes =
      ImmutableSet.of(
          WorkspaceConstants.WORKSPACE_TAG,
          AttributeGroupType.ATTRIBUTE_GROUP_TAG,
          QueryTemplateType.QUERY_TEMPLATE_TAG);

  public AccessControlPreIngestPlugin(SubjectIdentity subjectIdentity) {
    this.subjectIdentity = subjectIdentity;
  }

  /**
   * Ensures an ACL metacard has an owner.
   *
   * @param request the {@link CreateRequest} to process
   * @throws StopProcessingException - if the current subject doesn't have an owner attribute
   */
  @Override
  public CreateRequest process(CreateRequest request) throws StopProcessingException {
    Subject ownerSubject = getSubject();

    List<Metacard> metacards =
        request
            .getMetacards()
            .stream()
            .filter(m -> aclMetacardTypes.contains(m.getMetacardType().getName()))
            .filter(m -> !m.getTags().contains(SYSTEM_TEMPLATE))
            .collect(Collectors.toList());

    boolean missingOwner =
        metacards
            .stream()
            .anyMatch(metacard -> StringUtils.isEmpty(AccessControlUtil.getOwner(metacard)));

    if (missingOwner && SubjectUtils.isGuest(ownerSubject)) {
      throw new StopProcessingException(
          "Guest user not allowed to create access-controlled resources");
    }

    metacards.forEach(
        metacard -> {
          String owner =
              AccessControlUtil.getOwner(metacard) != null
                  ? AccessControlUtil.getOwner(metacard)
                  : subjectIdentity.getUniqueIdentifier(ownerSubject);
          AccessControlUtil.setOwner(metacard, owner);
          setAccessAdministrator(metacard, owner);
        });

    return request;
  }

  @Override
  @SuppressWarnings("squid:S1854" /*previous is used and makes the stream forEach more efficient*/)
  public UpdateRequest process(UpdateRequest request) {
    return request;
  }

  @Override
  public DeleteRequest process(DeleteRequest input) {
    return input;
  }

  protected Subject getSubject() {
    return SecurityUtils.getSubject();
  }

  private static void setAccessAdministrator(Metacard metacard, String subjectIdentity) {
    metacard.setAttribute(
        new AttributeImpl(
            Security.ACCESS_ADMINISTRATORS, Collections.singletonList(subjectIdentity)));
  }
}
