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
package org.codice.ddf.catalog.ui.metacard.workspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

public class WorkspaceMetacardImpl extends MetacardImpl {

    private static final WorkspaceMetacardTypeImpl TYPE = new WorkspaceMetacardTypeImpl();

    public WorkspaceMetacardImpl() {
        super(TYPE);
        setTags(Collections.singleton(WorkspaceMetacardTypeImpl.WORKSPACE_TAG));
    }

    public void setMetacards(List<String> items) {
        setAttribute(Metacard.RELATED, new ArrayList<>(items));
    }

    public List<String> getMetacards() {
        return getValues(Metacard.RELATED);
    }

    public WorkspaceMetacardImpl(String id) {
        this();
        setId(id);
    }

    public WorkspaceMetacardImpl(Metacard metacard) {
        super(metacard);
    }

    /**
     * Wrap any metacard as a WorkspaceMetacardImpl.
     *
     * @param metacard
     * @return
     */
    public static WorkspaceMetacardImpl from(Metacard metacard) {
        return new WorkspaceMetacardImpl(metacard);
    }

    /**
     * Get a copy of a worksapce metacard.
     *
     * @param metacard
     * @return
     */
    public static WorkspaceMetacardImpl clone(Metacard metacard) {
        WorkspaceMetacardImpl worksapce = new WorkspaceMetacardImpl();

        metacard.getMetacardType()
                .getAttributeDescriptors()
                .stream()
                .forEach(descriptor -> worksapce.setAttribute(metacard.getAttribute(descriptor.getName())));

        return worksapce;
    }

    /**
     * Check if a given metacard is a workspace metacard by checking the tags metacard attribute.
     *
     * @param metacard
     * @return
     */
    public static boolean isWorkspaceMetacard(Metacard metacard) {
        if (metacard != null) {
            return metacard.getTags()
                    .stream()
                    .filter(WorkspaceMetacardTypeImpl.WORKSPACE_TAG::equals)
                    .findFirst()
                    .isPresent();
        }

        return false;
    }

    private List<String> getValues(String attribute) {
        Attribute attr = getAttribute(attribute);

        if (attr != null) {
            return attr.getValues()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public List<String> getQueries() {
        return getValues(WorkspaceMetacardTypeImpl.WORKSPACE_QUERIES);
    }

    public WorkspaceMetacardImpl setQueries(List<String> queries) {
        setAttribute(WorkspaceMetacardTypeImpl.WORKSPACE_QUERIES, new ArrayList<>(queries));
        return this;
    }

    public String getOwner() {
        List<String> values = getValues(WorkspaceMetacardTypeImpl.WORKSPACE_OWNER);

        if (!values.isEmpty()) {
            return values.get(0);
        }

        return null;
    }

    public WorkspaceMetacardImpl setOwner(String email) {
        setAttribute(WorkspaceMetacardTypeImpl.WORKSPACE_OWNER, email);
        return this;
    }

    public Set<String> getRoles() {
        return new HashSet<>(getValues(WorkspaceMetacardTypeImpl.WORKSPACE_ROLES));
    }

    public WorkspaceMetacardImpl setRoles(Set<String> roles) {
        setAttribute(WorkspaceMetacardTypeImpl.WORKSPACE_ROLES, new ArrayList<>(roles));
        return this;
    }

}
