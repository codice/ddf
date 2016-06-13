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
package org.codice.ddf.catalog.ui.security;

import java.util.Map;
import java.util.Set;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;

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
import ddf.security.common.audit.SecurityLogger;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;

public class WorkspaceAccessPlugin implements AccessPlugin {

    @Override
    public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {
        return input;
    }

    protected Subject getSubject() {
        return SecurityUtils.getSubject();
    }

    protected void warn(String message, Object... params) {
        SecurityLogger.auditWarn(message, getSubject(), params);
    }

    private boolean authorized(WorkspaceMetacardImpl updated, WorkspaceMetacardImpl existing) {
        if (updated.diffSharing(existing)
                .isEmpty()) {
            return false;
        }

        Map<String, Set<String>> securityAttributes =
                ImmutableMap.of(WorkspacePolicyExtension.EMAIL_ADDRESS_CLAIM_URI,
                        ImmutableSet.of(existing.getOwner()));

        KeyValueCollectionPermission kvCollection = new KeyValueCollectionPermission(
                CollectionPermission.UPDATE_ACTION,
                securityAttributes);

        if (!getSubject().isPermitted(kvCollection)) {
            warn("Subject not permitted to update roles for workspace {}", existing);
            return true;
        }

        return false;
    }

    @Override
    public UpdateRequest processPreUpdate(UpdateRequest input,
            Map<String, Metacard> existingMetacards) throws StopProcessingException {

        boolean authorized = input.getUpdates()
                .stream()
                .map(Map.Entry::getValue)
                .filter(WorkspaceMetacardImpl::isWorkspaceMetacard)
                .map(WorkspaceMetacardImpl::from)
                .filter(updated -> authorized(updated,
                        WorkspaceMetacardImpl.from(existingMetacards.get(updated.getId()))))
                .count() == 0;

        if (!authorized) {
            throw new StopProcessingException("Cannot update workspace.");
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
    public ResourceRequest processPreResource(ResourceRequest input)
            throws StopProcessingException {
        return input;
    }

    @Override
    public ResourceResponse processPostResource(ResourceResponse input, Metacard metacard)
            throws StopProcessingException {
        return input;
    }

}
