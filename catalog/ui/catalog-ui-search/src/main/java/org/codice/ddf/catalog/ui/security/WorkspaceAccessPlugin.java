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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;

public class WorkspaceAccessPlugin implements AccessPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceAccessPlugin.class);

    public String getCanSetOwnerRole() {
        return canSetOwnerRole;
    }

    public void setCanSetOwnerRole(String canSetOwnerRole) {
        LOGGER.info("Updating role for setting owners on workspaces to '{}'.", canSetOwnerRole);
        this.canSetOwnerRole = canSetOwnerRole;
    }

    private String canSetOwnerRole = "system-workspace";

    protected Subject getSubject() {
        return SecurityUtils.getSubject();
    }

    private KeyValueCollectionPermission canSetOwnerPermission() {
        Map<String, Set<String>> securityAttributes = ImmutableMap.of(Constants.ROLES_CLAIM_URI,
                ImmutableSet.of(canSetOwnerRole));

        return new KeyValueCollectionPermission(CollectionPermission.UPDATE_ACTION,
                securityAttributes);
    }

    private KeyValueCollectionPermission isOwnerPermission(String owner) {
        Map<String, Set<String>> securityAttributes =
                ImmutableMap.of(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of(owner));

        return new KeyValueCollectionPermission(CollectionPermission.UPDATE_ACTION,
                securityAttributes);
    }

    private boolean isSharingUpdated(WorkspaceMetacardImpl previous, WorkspaceMetacardImpl update) {
        return !update.diffSharing(previous)
                .isEmpty();
    }

    private boolean isOwnerUpdated(WorkspaceMetacardImpl previous, WorkspaceMetacardImpl update) {
        Validate.notNull(previous);
        Validate.notNull(update);

        if (StringUtils.isEmpty(previous.getOwner())) {
            throw new IllegalStateException(String.format(
                    "Workspace with id = %s has owner that is null or empty.",
                    previous.getId()));
        }

        return !previous.getOwner()
                .equals(update.getOwner());
    }

    private boolean isNotOwner(String owner) {
        return !getSubject().isPermitted(isOwnerPermission(owner));
    }

    private boolean canSetOwner() {
        return getSubject().isPermitted(canSetOwnerPermission());
    }

    @Override
    public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {

        boolean notOwner = input.getMetacards()
                .stream()
                .filter(WorkspaceMetacardImpl::isWorkspaceMetacard)
                .map(WorkspaceMetacardImpl::from)
                .map(WorkspaceMetacardImpl::getOwner)
                .filter(Objects::nonNull)
                .filter(this::isNotOwner)
                .findFirst()
                .isPresent();

        if (notOwner && !canSetOwner()) {
            throw new StopProcessingException(
                    "Cannot create workspace. Subject does not have permission to set owners");
        }

        return input;
    }

    @Override
    public UpdateRequest processPreUpdate(UpdateRequest input,
            Map<String, Metacard> existingMetacards) throws StopProcessingException {

        Function<WorkspaceMetacardImpl, WorkspaceMetacardImpl> previous =
                (update) -> WorkspaceMetacardImpl.from(existingMetacards.get(update.getId()));

        List<WorkspaceMetacardImpl> workspaces = input.getUpdates()
                .stream()
                .map(Map.Entry::getValue)
                .filter(WorkspaceMetacardImpl::isWorkspaceMetacard)
                .map(WorkspaceMetacardImpl::from)
                .collect(Collectors.toList());

        boolean updatedOwner = workspaces.stream()
                .filter(update -> isOwnerUpdated(previous.apply(update), update))
                .findFirst()
                .isPresent();

        if (updatedOwner && !canSetOwner()) {
            throw new StopProcessingException(
                    "Cannot update workspace. Subject does not have permission to set owners.");
        }

        Set<String> notOwners = workspaces.stream()
                .filter(update -> isSharingUpdated(previous.apply(update), update))
                .map(previous)
                .map(WorkspaceMetacardImpl::getOwner)
                .filter(this::isNotOwner)
                .collect(Collectors.toSet());

        if (!notOwners.isEmpty()) {
            throw new StopProcessingException(
                    "Cannot update workspace. Subject cannot change sharing permissions because they are not the owner.");
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
