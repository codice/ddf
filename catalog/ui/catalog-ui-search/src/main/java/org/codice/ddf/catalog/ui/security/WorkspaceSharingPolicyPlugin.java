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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.codice.ddf.catalog.ui.metacard.workspace.SharingMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceTransformer;

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

    private Map<String, Set<String>> getSharingPermissions(List<Metacard> metacards,
            String action) {
        return metacards.stream()
                .filter(WorkspaceMetacardImpl::isWorkspaceMetacard)
                .map(WorkspaceMetacardImpl::from)
                .map(WorkspaceMetacardImpl::getSharing)
                .flatMap(Set::stream)
                .map(transformer::toMetacardFromXml)
                .filter(SharingMetacardImpl::isSharingMetacard)
                .map(SharingMetacardImpl::from)
                .filter(sharing -> action.equals(sharing.getAction()))
                .collect(Collectors.toMap(SharingMetacardImpl::getSharingAttribute,
                        s -> ImmutableSet.of(s.getValue()),
                        Sets::union));
    }

    private Map<String, Set<String>> getSharingPermissions(Metacard metacard, String action) {
        return getSharingPermissions(Collections.singletonList(metacard), action);
    }

    @Override
    public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
            throws StopProcessingException {
        Metacard metacard = input.getMetacard();
        return new PolicyResponseImpl(Collections.emptyMap(),
                getSharingPermissions(metacard, CollectionPermission.READ_ACTION));
    }

    @Override
    public PolicyResponse processPreUpdate(Metacard metacard, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl(Collections.emptyMap(),
                getSharingPermissions(metacard, CollectionPermission.UPDATE_ACTION));
    }

    @Override
    public PolicyResponse processPreDelete(List<Metacard> metacards,
            Map<String, Serializable> properties) throws StopProcessingException {
        return new PolicyResponseImpl(Collections.emptyMap(),
                getSharingPermissions(metacards, CollectionPermission.DELETE_ACTION));
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
