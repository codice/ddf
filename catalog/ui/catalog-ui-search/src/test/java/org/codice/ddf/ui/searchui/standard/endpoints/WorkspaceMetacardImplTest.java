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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

import ddf.catalog.data.impl.MetacardImpl;

public class WorkspaceMetacardImplTest {

    private WorkspaceMetacardImpl workspace;

    @Before
    public void setUp() {
        workspace = new WorkspaceMetacardImpl();
    }

    @Test
    public void testIsWorkspaceMetacard() {
        assertThat(WorkspaceMetacardImpl.isWorkspaceMetacard(null), is(false));
        assertThat(WorkspaceMetacardImpl.isWorkspaceMetacard(new MetacardImpl()), is(false));
        assertThat(WorkspaceMetacardImpl.isWorkspaceMetacard(workspace), is(true));
    }

    @Test
    public void testWorkspaceMetacardFrom() {
        WorkspaceMetacardImpl wrapped = WorkspaceMetacardImpl.from(new MetacardImpl(workspace));
        wrapped.setId("0");
        assertThat(wrapped.getId(), is("0"));
        assertThat(workspace.getId(), is("0"));
    }

    @Test
    public void testRoles() {
        Set<String> roles = Sets.newHashSet("Role A", "Role B");
        assertThat(workspace.setRoles(roles).getRoles(), is(roles));
    }

    @Test
    public void testOwner() {
        String owner = "owner@localhost";
        assertThat(workspace.setOwner(owner).getOwner(), is(owner));
    }

    @Test
    public void testQueries() {
        List<String> queries = Arrays.asList("Query 1", "Query 1");
        assertThat(workspace.setQueries(queries).getQueries(), is(queries));
    }
}