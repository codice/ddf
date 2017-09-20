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

import com.google.common.collect.ImmutableSet;
import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SubjectUtils;
import ddf.security.common.audit.SecurityLogger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.system.alerts.NoticePriority;
import org.codice.ddf.system.alerts.SystemNotice;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class WorkspacePreIngestPlugin implements PreIngestPlugin {

  private static final String ALERT_TITLE = "User missing owner claim for creating workspaces";

  private static final String OWNER_ATTRIBUTE_MESSAGE = "The owner attribute value is [%s].";

  private static final String USER_MESSAGE = "The user trying to create the workspace is [%s].";

  public static final String EXCEPTION_MESSAGE =
      "Cannot create workspace. Missing required attributes.";

  private final WorkspaceSecurityConfiguration config;

  private final EventAdmin eventAdmin;

  public WorkspacePreIngestPlugin(WorkspaceSecurityConfiguration config, EventAdmin eventAdmin) {
    this.config = config;
    this.eventAdmin = eventAdmin;
  }

  private static Map<String, WorkspaceMetacardImpl> getPreviousWorkspaces(UpdateRequest request) {
    OperationTransaction operationTransaction =
        (OperationTransaction) request.getProperties().get(Constants.OPERATION_TRANSACTION_KEY);

    return operationTransaction
        .getPreviousStateMetacards()
        .stream()
        .filter(WorkspaceMetacardImpl::isWorkspaceMetacard)
        .map(WorkspaceMetacardImpl::from)
        .collect(Collectors.toMap(Metacard::getId, m -> m));
  }

  protected Map<String, SortedSet<String>> getSubjectAttributes() {
    return SubjectUtils.getSubjectAttributes(SecurityUtils.getSubject());
  }

  private SortedSet<String> getSubjectAttribute(String key) {
    Map<String, SortedSet<String>> attrs = getSubjectAttributes();

    if (attrs.containsKey(key)) {
      return attrs.get(key);
    }

    return Collections.emptySortedSet();
  }

  protected String getSubjectName() {
    return SubjectUtils.getName(SecurityUtils.getSubject());
  }

  protected void alertMissingClaim(String ownerAttribute) {
    String attribute = String.format(OWNER_ATTRIBUTE_MESSAGE, ownerAttribute);
    String subject = String.format(USER_MESSAGE, getSubjectName());
    SystemNotice notice =
        new SystemNotice(
            WorkspacePolicyExtension.class.toString(),
            NoticePriority.CRITICAL,
            ALERT_TITLE,
            ImmutableSet.of(attribute, subject));
    eventAdmin.postEvent(
        new Event(SystemNotice.SYSTEM_NOTICE_BASE_TOPIC.concat("audit"), notice.getProperties()));
    SecurityLogger.auditWarn(ALERT_TITLE, SecurityUtils.getSubject());
  }

  /**
   * Ensures a workspace has an owner.
   *
   * @param request the {@link CreateRequest} to process
   * @return
   * @throws PluginExecutionException
   * @throws StopProcessingException - if the current subject doesn't have an email attribute
   */
  @Override
  public CreateRequest process(CreateRequest request)
      throws PluginExecutionException, StopProcessingException {

    final String ownerAttr = config.getOwnerAttribute();

    Optional<String> owner = getSubjectAttribute(ownerAttr).stream().findFirst();

    List<WorkspaceMetacardImpl> workspaces =
        request
            .getMetacards()
            .stream()
            .filter(WorkspaceMetacardImpl::isWorkspaceMetacard)
            .map(WorkspaceMetacardImpl::from)
            .filter(workspace -> StringUtils.isEmpty(workspace.getOwner()))
            .collect(Collectors.toList());

    if (!workspaces.isEmpty() && !owner.isPresent()) {
      alertMissingClaim(ownerAttr);
      throw new StopProcessingException(EXCEPTION_MESSAGE);
    }

    workspaces.stream().forEach(workspace -> workspace.setOwner(owner.get()));

    return request;
  }

  /**
   * Ensures the owner attribute is always present.
   *
   * @param request the {@link UpdateRequest} to process
   * @return
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  @Override
  public UpdateRequest process(UpdateRequest request)
      throws PluginExecutionException, StopProcessingException {

    Map<String, WorkspaceMetacardImpl> previous = getPreviousWorkspaces(request);

    request
        .getUpdates()
        .stream()
        .map(Map.Entry::getValue)
        .filter(WorkspaceMetacardImpl::isWorkspaceMetacard)
        .map(WorkspaceMetacardImpl::from)
        .filter(workspace -> StringUtils.isEmpty(workspace.getOwner()))
        .forEach(workspace -> workspace.setOwner(previous.get(workspace.getId()).getOwner()));

    return request;
  }

  @Override
  public DeleteRequest process(DeleteRequest input)
      throws PluginExecutionException, StopProcessingException {
    return input;
  }
}
