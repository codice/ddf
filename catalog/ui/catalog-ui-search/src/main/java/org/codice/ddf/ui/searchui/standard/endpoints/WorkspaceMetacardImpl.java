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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ddf.catalog.data.impl.MetacardImpl;

public class WorkspaceMetacardImpl extends MetacardImpl {

    private static final WorkspaceMetacardTypeImpl TYPE = new WorkspaceMetacardTypeImpl();

    public WorkspaceMetacardImpl() {
        super(TYPE);
        setTags(Collections.singleton(WorkspaceMetacardTypeImpl.WORKSPACE_TAG));
    }

    public void setMetacards(List<String> items) {
        setAttribute(WorkspaceMetacardTypeImpl.WORKSPACE_METACARDS, new ArrayList<>(items));
    }

    private List<String> getValues(String attribute) {
        return getAttribute(attribute).getValues()
                .stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    public List<String> getSavedItems() {
        return getValues(WorkspaceMetacardTypeImpl.WORKSPACE_METACARDS);
    }

    public void setQueries(List<String> queries) {
        setAttribute(WorkspaceMetacardTypeImpl.WORKSPACE_QUERIES, new ArrayList<>(queries));
    }

    public List<String> getQueries() {
        return getValues(WorkspaceMetacardTypeImpl.WORKSPACE_QUERIES);
    }

}
