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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.plugin.PolicyResponse;

public class WorkspacePolicyPluginTest {

    private WorkspacePolicyPlugin wpp;

    @Before
    public void setup() {
        wpp = new WorkspacePolicyPlugin();
    }

    private Attribute getRoles() {
        return new AttributeImpl(WorkspaceMetacardTypeImpl.WORKSPACE_ROLES, Arrays.asList("admin"));
    }

    @Test
    public void testNonWorkspaceMetacardWithRoles() throws Exception {
        Metacard metacard = new MetacardImpl();
        metacard.setAttribute(getRoles());
        PolicyResponse pr = wpp.processPreUpdate(metacard, null);
        assertThat(pr.itemPolicy()
                .get(WorkspacePolicyPlugin.ROLE_CLAIM), nullValue());
    }

    @Test
    public void testNonWorkspaceMetacardWithNoRoles() throws Exception {
        Metacard metacard = new MetacardImpl();
        PolicyResponse pr = wpp.processPreUpdate(metacard, null);
        assertThat(pr.itemPolicy()
                .get(WorkspacePolicyPlugin.ROLE_CLAIM), nullValue());
    }

    @Test
    public void testWorkspaceMetacardWithNoRoles() throws Exception {
        Metacard metacard = new WorkspaceMetacardImpl();
        PolicyResponse pr = wpp.processPreUpdate(metacard, null);
        assertThat(pr.itemPolicy()
                .get(WorkspacePolicyPlugin.ROLE_CLAIM), nullValue());
    }

    @Test
    public void testWorkspaceMetacardWithRoles() throws Exception {
        Metacard metacard = new WorkspaceMetacardImpl();
        metacard.setAttribute(getRoles());
        PolicyResponse pr = wpp.processPreUpdate(metacard, null);
        assertThat(pr.itemPolicy()
                .get(WorkspacePolicyPlugin.ROLE_CLAIM), is(new HashSet(Arrays.asList("admin"))));
    }

}