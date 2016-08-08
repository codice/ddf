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
 **/
package org.codice.ddf.catalog.ui.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.catalog.ui.metacard.workspace.SharingMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardTypeImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceTransformer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;
import ddf.security.permission.CollectionPermission;

public class WorkspaceSharingPolicyPlugin implements PolicyPlugin {

    private final WorkspaceTransformer transformer;

    public WorkspaceSharingPolicyPlugin(WorkspaceTransformer transformer) {
        this.transformer = transformer;
    }

    private Map<String, Set<String>> getOwnerPermission(WorkspaceMetacardImpl workspace) {
        String owner = workspace.getOwner();

        if (StringUtils.isEmpty(owner)) {
            return Collections.emptyMap();
        }

        return ImmutableMap.of(WorkspaceMetacardTypeImpl.WORKSPACE_OWNER, ImmutableSet.of(owner));
    }

    private Map<String, Set<String>> getSharingPermissions(WorkspaceMetacardImpl workspace,
            String action) {
        return workspace.getSharing()
                .stream()
                .map(transformer::toMetacardFromXml)
                .filter(SharingMetacardImpl::isSharingMetacard)
                .map(SharingMetacardImpl::from)
                .filter(sharing -> action.equals(sharing.getAction()))
                .collect(Collectors.toMap(SharingMetacardImpl::getSharingAttribute,
                        s -> ImmutableSet.of(s.getValue()),
                        Sets::union));
    }

    private Map<String, Set<String>> getPolicy(WorkspaceMetacardImpl workspace, String action) {
        return Stream.of(getSharingPermissions(workspace, action), getOwnerPermission(workspace))
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Sets::union));
    }

    private Map<String, Set<String>> getPolicy(Metacard metacard, String action) {
        if (WorkspaceMetacardImpl.isWorkspaceMetacard(metacard)) {
            return getPolicy(WorkspaceMetacardImpl.from(metacard), action);
        }

        return Collections.emptyMap();
    }

    private Map<String, Set<String>> getPolicy(List<Metacard> metacards, String action) {
        return metacards.stream()
                .filter(WorkspaceMetacardImpl::isWorkspaceMetacard)
                .map(WorkspaceMetacardImpl::from)
                .map(workspace -> getPolicy(workspace, action))
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Sets::union));
    }

    @Override
    public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
            throws StopProcessingException {
        Metacard metacard = input.getMetacard();
        return new PolicyResponseImpl(Collections.emptyMap(),
                getPolicy(metacard, CollectionPermission.READ_ACTION));
    }

    @Override
    public PolicyResponse processPreUpdate(Metacard metacard, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl(Collections.emptyMap(),
                getPolicy(metacard, CollectionPermission.UPDATE_ACTION));
    }

    @Override
    public PolicyResponse processPreDelete(List<Metacard> metacards,
            Map<String, Serializable> properties) throws StopProcessingException {
        return new PolicyResponseImpl(Collections.emptyMap(),
                getPolicy(metacards, CollectionPermission.DELETE_ACTION));
    }

    @Override
    public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostDelete(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreQuery(Query query, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreResource(ResourceRequest resourceRequest)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }
}
