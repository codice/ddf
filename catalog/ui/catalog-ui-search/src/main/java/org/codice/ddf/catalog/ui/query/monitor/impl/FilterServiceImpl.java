/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.query.monitor.impl;

import java.util.Date;

import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardTypeImpl;
import org.codice.ddf.catalog.ui.query.monitor.api.FilterService;
import org.opengis.filter.Filter;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterBuilder;

public class FilterServiceImpl implements FilterService {

    private final FilterBuilder filterBuilder;

    public FilterServiceImpl(FilterBuilder filterBuilder) {
        this.filterBuilder = filterBuilder;
    }

    @Override
    public Filter buildWorkspaceTagFilter() {
        return filterBuilder.attribute(Metacard.TAGS)
                .is()
                .like()
                .text(WorkspaceMetacardTypeImpl.WORKSPACE_TAG);
    }

    @Override
    public Filter buildMetacardIdFilter(String id) {
        return filterBuilder.attribute(Metacard.ID)
                .is()
                .equalTo()
                .text(id);
    }

    @Override
    public Filter getModifiedDateFilter(Date lastCheckDate) {
        return filterBuilder.attribute(Metacard.MODIFIED)
                .after()
                .date(lastCheckDate);
    }

    @Override
    public String toString() {
        return "FilterServiceImpl{" +
                "filterBuilder=" + filterBuilder +
                '}';
    }
}
