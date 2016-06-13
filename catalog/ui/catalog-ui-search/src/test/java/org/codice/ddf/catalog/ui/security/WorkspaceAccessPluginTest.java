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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import org.apache.shiro.authz.Permission;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardTypeImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.Subject;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;

public class WorkspaceAccessPluginTest {

    private AccessPlugin accessPlugin;

    private Subject subject = mock(Subject.class);

    @Before
    public void setUp() {
        accessPlugin = new WorkspaceAccessPlugin() {
            @Override
            protected Subject getSubject() {
                return subject;
            }

            @Override
            protected void warn(String message, Object... params) {
            }
        };
    }

    private UpdateRequest mockUpdateRequest(Map<String, Metacard> updates) {
        UpdateRequest update = mock(UpdateRequest.class);
        doReturn(new ArrayList(updates.entrySet())).when(update)
                .getUpdates();
        return update;
    }

    @Test
    public void testIgnoreNonWorkspaceMetacards() throws Exception {
        Map<String, Metacard> updates = ImmutableMap.of("rand", new MetacardImpl());
        UpdateRequest update = mockUpdateRequest(updates);

        // should ignore all non-workspace metacards and do nothing
        accessPlugin.processPreUpdate(update, updates);
    }

    @Test
    public void testIgnoreWorkspaceWithNoRoles() throws Exception {
        String id = "0";
        Map<String, Metacard> updates = ImmutableMap.of(id,
                WorkspaceMetacardImpl.from(ImmutableMap.of("id", id)));
        UpdateRequest update = mockUpdateRequest(updates);

        // should ignore any workspace metacards with roles
        accessPlugin.processPreUpdate(update, updates);
    }

    @Test
    public void testIgnoreWorkspaceWithNoUpdatedRoles() throws Exception {
        String id = "0";

        Map<String, Serializable> attrs = ImmutableMap.of("id",
                id,
                "roles",
                ImmutableList.of("guest"));

        Map<String, Metacard> updates = ImmutableMap.of(id, WorkspaceMetacardImpl.from(attrs));
        UpdateRequest update = mockUpdateRequest(updates);

        // should ignore any workspace metacards with no updated roles
        accessPlugin.processPreUpdate(update, updates);
    }

    @Test
    public void testPermittedWhenOwnerOnUpdatedRoles() throws Exception {
        String id = "0";

        WorkspaceMetacardImpl before = WorkspaceMetacardImpl.from(ImmutableMap.of(Metacard.ID,
                id,
                WorkspaceMetacardTypeImpl.WORKSPACE_OWNER,
                "before",
                WorkspaceMetacardTypeImpl.WORKSPACE_SHARING,
                ImmutableList.of()));

        WorkspaceMetacardImpl after = WorkspaceMetacardImpl.from(ImmutableMap.of(Metacard.ID,
                id,
                WorkspaceMetacardTypeImpl.WORKSPACE_OWNER,
                "after",
                WorkspaceMetacardTypeImpl.WORKSPACE_SHARING,
                ImmutableList.of("<xml/>")));

        UpdateRequest update = mockUpdateRequest(ImmutableMap.of(id, after));

        ArgumentCaptor<KeyValueCollectionPermission> args = ArgumentCaptor.forClass(
                KeyValueCollectionPermission.class);

        doReturn(true).when(subject)
                .isPermitted(args.capture());

        accessPlugin.processPreUpdate(update, ImmutableMap.of(id, before));

        KeyValuePermission permission = (KeyValuePermission) args.getValue()
                .getKeyValuePermissionList()
                .get(0);

        assertThat(permission.getKey(), is(WorkspacePolicyExtension.EMAIL_ADDRESS_CLAIM_URI));
        // NOTE: the permission should contain the owner of the before metacard, not after
        assertThat(permission.getValues(), is(ImmutableSet.of(before.getOwner())));
    }

    @Test(expected = StopProcessingException.class)
    public void testStopProcessingWhenNotOwner() throws Exception {
        String id = "0";

        WorkspaceMetacardImpl before = WorkspaceMetacardImpl.from(ImmutableMap.of(Metacard.ID,
                id,
                WorkspaceMetacardTypeImpl.WORKSPACE_OWNER,
                "owner",
                WorkspaceMetacardTypeImpl.WORKSPACE_SHARING,
                ImmutableList.of()));

        WorkspaceMetacardImpl after = WorkspaceMetacardImpl.from(ImmutableMap.of(Metacard.ID,
                id,
                WorkspaceMetacardTypeImpl.WORKSPACE_OWNER,
                "owner",
                WorkspaceMetacardTypeImpl.WORKSPACE_SHARING,
                ImmutableList.of("<xml/>")));

        UpdateRequest update = mockUpdateRequest(ImmutableMap.of(id, after));

        doReturn(false).when(subject)
                .isPermitted(any(Permission.class));

        accessPlugin.processPreUpdate(update, ImmutableMap.of(id, before));
    }

}