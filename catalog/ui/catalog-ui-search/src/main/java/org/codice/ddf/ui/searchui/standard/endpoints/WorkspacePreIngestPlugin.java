/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.ui.searchui.standard.endpoints;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;

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

public class WorkspacePreIngestPlugin implements PreIngestPlugin {

    /**
     * Converts a list of Map.Entry into a Map.
     *
     * @param request
     * @return
     */
    private static Map<String, Metacard> getUpdatedMetacards(UpdateRequest request) {
        return request.getUpdates()
                .stream()
                .collect(Collectors.toMap(entry -> (String) entry.getKey(), Map.Entry::getValue));
    }

    private static List<Metacard> getPreviousMetacards(UpdateRequest request) {
        OperationTransaction operationTransaction = (OperationTransaction) request.getProperties()
                .get(Constants.OPERATION_TRANSACTION_KEY);

        return operationTransaction.getPreviousStateMetacards();
    }

    protected String getSubjectEmail() {
        return SubjectUtils.getEmailAddress(SecurityUtils.getSubject());
    }

    protected String getSubjectName() {
        return SubjectUtils.getName(SecurityUtils.getSubject());
    }

    protected void warn(String message) {
        SecurityLogger.auditWarn(message, SecurityUtils.getSubject());
    }

    /**
     * Attaches an owner to a workspace metcard on initial creation.
     *
     * @param request the {@link CreateRequest} to process
     * @return
     * @throws PluginExecutionException
     * @throws StopProcessingException  - if the current subject doesn't have an email attribute
     */
    @Override
    public CreateRequest process(CreateRequest request)
            throws PluginExecutionException, StopProcessingException {

        String email = getSubjectEmail();

        List<WorkspaceMetacardImpl> workspaces = request.getMetacards()
                .stream()
                .filter(WorkspaceMetacardImpl::isWorkspaceMetacard)
                .map(WorkspaceMetacardImpl::from)
                .collect(Collectors.toList());

        if (!workspaces.isEmpty() && StringUtils.isEmpty(email)) {
            throw new StopProcessingException(String.format(
                    "Cannot create workspace. Subject with name '%s' not permitted because they have no email.",
                    getSubjectName()));
        }

        workspaces.stream()
                .forEach(workspace -> workspace.setOwner(email));

        return request;
    }

    /**
     * Ensures the owner doesn't change.
     *
     * @param request the {@link UpdateRequest} to process
     * @return
     * @throws PluginExecutionException
     * @throws StopProcessingException
     */
    @Override
    public UpdateRequest process(UpdateRequest request)
            throws PluginExecutionException, StopProcessingException {

        Map<String, Metacard> newMetacards = getUpdatedMetacards(request);

        getPreviousMetacards(request).stream()
                .filter(WorkspaceMetacardImpl::isWorkspaceMetacard)
                .map(WorkspaceMetacardImpl::from)
                .forEach(previous -> {
                    WorkspaceMetacardImpl updated = WorkspaceMetacardImpl.from(newMetacards.get(
                            previous.getId()));

                    String oldOwner = previous.getOwner();
                    String newOwner = updated.getOwner();

                    if (!StringUtils.isEmpty(newOwner) && !newOwner.equals(oldOwner)) {
                        warn("Subject is trying to change the owner of a workspace metacard.");
                    }

                    updated.setOwner(oldOwner);
                });

        return request;
    }

    @Override
    public DeleteRequest process(DeleteRequest input)
            throws PluginExecutionException, StopProcessingException {
        return input;
    }
}
