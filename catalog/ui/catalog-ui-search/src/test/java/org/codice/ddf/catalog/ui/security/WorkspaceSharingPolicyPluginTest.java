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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Map;

import org.codice.ddf.catalog.ui.metacard.workspace.SharingMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardTypeImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceTransformer;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.security.permission.CollectionPermission;

public class WorkspaceSharingPolicyPluginTest {

    private Map properties;

    private PolicyPlugin plugin;

    private WorkspaceTransformer transformer;

    @Before
    public void setUp() {
        properties = mock(Map.class);
        transformer = mock(WorkspaceTransformer.class);
        plugin = new WorkspaceSharingPolicyPlugin(transformer);
    }

    @Test
    public void testOwnerOnCreate() throws Exception {
        String email = "a@b.c";
        WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();
        workspace.setOwner(email);
        PolicyResponse response = plugin.processPreCreate(workspace, properties);
        assertThat(response.itemPolicy(), is(Collections.emptyMap()));
    }

    @Test
    public void testOwnerOnUpdate() throws Exception {
        String email = "a@b.c";
        WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();
        workspace.setOwner(email);
        PolicyResponse response = plugin.processPreUpdate(workspace, properties);
        assertThat(response.itemPolicy(),
                is(ImmutableMap.of(WorkspaceMetacardTypeImpl.WORKSPACE_OWNER,
                        ImmutableSet.of(email))));
    }

    @Test
    public void testSharingOnUpdate() throws Exception {
        WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();
        workspace.setSharing(ImmutableSet.of("<xml/>"));

        SharingMetacardImpl sharing = new SharingMetacardImpl();

        sharing.setAction(CollectionPermission.UPDATE_ACTION);
        sharing.setSharingAttribute("attribute");
        sharing.setValue("value");

        doReturn(sharing).when(transformer)
                .toMetacardFromXml(any(String.class));

        PolicyResponse response = plugin.processPreUpdate(workspace, properties);

        assertThat(response.itemPolicy(),
                is(ImmutableMap.of("attribute", ImmutableSet.of("value"))));

    }

    @Test
    public void testIgnoreDeleteActionOnUpdate() throws Exception {
        WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();

        SharingMetacardImpl sharing = new SharingMetacardImpl();

        sharing.setAction(CollectionPermission.DELETE_ACTION);
        sharing.setSharingAttribute("attribute");
        sharing.setValue("value");

        doReturn(sharing).when(transformer)
                .toMetacardFromXml(any());

        PolicyResponse response = plugin.processPreUpdate(workspace, properties);

        assertThat(response.itemPolicy(), is(Collections.emptyMap()));

    }
}