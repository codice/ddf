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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;

public class WorkspacePolicyPlugin implements PolicyPlugin {

    public static final String ROLE_CLAIM =
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    private static Boolean isWorkspaceMetacard(Metacard metacard) {
        if (metacard != null) {
            for (Serializable s : metacard.getTags()) {
                if (WorkspaceMetacardTypeImpl.WORKSPACE_TAG.equals(s)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static Set<String> getWorkspaceRoles(Metacard metacard) {
        if (isWorkspaceMetacard(metacard)) {
            Attribute attr = metacard.getAttribute(WorkspaceMetacardTypeImpl.WORKSPACE_ROLES);
            if (attr != null) {
                Set<String> roles = attr.getValues()
                        .stream()
                        .map(String::valueOf)
                        .collect(Collectors.toSet());
                if (roles.size() > 0) {
                    return roles;
                }
            }
        }

        return Collections.emptySet();
    }

    private static Set<String> getWorkspaceRoles(List<Metacard> metacards) {
        return metacards.stream()
                .map(WorkspacePolicyPlugin::getWorkspaceRoles)
                .reduce(new HashSet<>(), (acc, val) -> {
                    acc.addAll(val);
                    return acc;
                });
    }

    private static PolicyResponse getPolicyResponse(Set<String> roles) {
        if (!roles.isEmpty()) {
            return new PolicyResponseImpl(new HashMap<>(), Collections.singletonMap(ROLE_CLAIM,
                    roles));
        }

        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreCreate(Metacard metacard, Map<String, Serializable> properties)
            throws StopProcessingException {
        return getPolicyResponse(getWorkspaceRoles(metacard));
    }

    @Override
    public PolicyResponse processPreUpdate(Metacard metacard, Map<String, Serializable> properties)
            throws StopProcessingException {
        return getPolicyResponse(getWorkspaceRoles(metacard));
    }

    @Override
    public PolicyResponse processPreDelete(List<Metacard> metacards,
            Map<String, Serializable> properties) throws StopProcessingException {
        return getPolicyResponse(getWorkspaceRoles(metacards));
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
    public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return getPolicyResponse(getWorkspaceRoles(input.getMetacard()));
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
