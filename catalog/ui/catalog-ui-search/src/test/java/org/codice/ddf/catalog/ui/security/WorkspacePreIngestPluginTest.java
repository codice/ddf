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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.Serializable;
import java.util.Map;
import java.util.SortedSet;

import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.OperationTransactionImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.StopProcessingException;

public class WorkspacePreIngestPluginTest {

    private WorkspaceSecurityConfiguration config;

    private boolean alerted;

    @Before
    public void setUp() {
        alerted = false;
        config = new WorkspaceSecurityConfiguration();
    }

    private WorkspacePreIngestPlugin makePlugin(Map<String, SortedSet<String>> attrs) {
        return new WorkspacePreIngestPlugin(config, null) {
            protected Map<String, SortedSet<String>> getSubjectAttributes() {
                return attrs;
            }

            protected String getSubjectName() {
                return "test";
            }

            protected void alertMissingClaim(String ownerAttribute) {
                alerted = true;
            }
        };
    }

    @Test
    public void testCreateShouldIgnoreNonWorkspaceMetacards() throws Exception {
        WorkspacePreIngestPlugin wpip = makePlugin(ImmutableMap.of());
        Metacard metacard = new MetacardImpl();

        wpip.process(new CreateRequestImpl(metacard));
        assertThat(metacard.getAttribute(Core.METACARD_OWNER), nullValue());
    }

    @Test
    public void testCreateWorkspaceSuccess() throws Exception {
        Map<String, SortedSet<String>> attrs = ImmutableMap.of(config.getOwnerAttribute(),
                ImmutableSortedSet.of("owner"));

        WorkspacePreIngestPlugin wpip = makePlugin(attrs);
        WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();

        wpip.process(new CreateRequestImpl(workspace));
        assertThat(workspace.getOwner(), is("owner"));
    }

    @Test
    public void testCreateWorkspaceSuccessWithOverride() throws Exception {
        final String override = "override";

        Map<String, SortedSet<String>> attrs = ImmutableMap.of(override,
                ImmutableSortedSet.of("owner"));
        config.setOwnerAttribute(override);

        WorkspacePreIngestPlugin wpip = makePlugin(attrs);
        WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();

        wpip.process(new CreateRequestImpl(workspace));
        assertThat(workspace.getOwner(), is("owner"));
    }

    @Test(expected = StopProcessingException.class)
    public void testCreateWorkspaceFailure() throws Exception {
        WorkspacePreIngestPlugin wpip = makePlugin(ImmutableMap.of());
        wpip.process(new CreateRequestImpl(new WorkspaceMetacardImpl()));
    }

    @Test(expected = StopProcessingException.class)
    public void testCreateWorkspaceFailureWithOverride() throws Exception {
        final String override = "override";

        Map<String, SortedSet<String>> attrs = ImmutableMap.of(config.getOwnerAttribute(),
                ImmutableSortedSet.of("owner"));
        config.setOwnerAttribute(override);

        WorkspacePreIngestPlugin wpip = makePlugin(attrs);
        wpip.process(new CreateRequestImpl(new WorkspaceMetacardImpl()));
    }

    @Test(expected = StopProcessingException.class)
    public void testCreateAlertOnFailure() throws Exception {
        WorkspacePreIngestPlugin wpip = makePlugin(ImmutableMap.of());
        wpip.process(new CreateRequestImpl(new WorkspaceMetacardImpl()));
        assertThat(alerted, is(true));
    }

    private UpdateRequest updateRequest(Metacard prev, Metacard next) {
        UpdateRequestImpl request = new UpdateRequestImpl(next.getId(), next);

        OperationTransactionImpl tx =
                new OperationTransactionImpl(OperationTransaction.OperationType.UPDATE,
                        ImmutableList.of(prev));

        Map<String, Serializable> properties = ImmutableMap.of(Constants.OPERATION_TRANSACTION_KEY,
                tx);

        request.setProperties(properties);

        return request;
    }

    @Test
    public void testUpdateIgnoreNonWorkspaceMetacards() throws Exception {
        WorkspacePreIngestPlugin wpip = makePlugin(ImmutableMap.of());

        MetacardImpl prev = new MetacardImpl();
        prev.setId("id");
        prev.setAttribute(Core.METACARD_OWNER, "prev");
        MetacardImpl next = new MetacardImpl();
        next.setId("id");

        wpip.process(updateRequest(prev, next));
        assertThat(next.getAttribute(Core.METACARD_OWNER), nullValue());
    }

    @Test
    public void testUpdatePreserveOwner() throws Exception {
        WorkspacePreIngestPlugin wpip = makePlugin(ImmutableMap.of());

        WorkspaceMetacardImpl prev = new WorkspaceMetacardImpl("id");
        WorkspaceMetacardImpl next = WorkspaceMetacardImpl.clone(prev);

        prev.setOwner("prev");

        assertThat(next.getOwner(), nullValue());
        wpip.process(updateRequest(prev, next));
        assertThat(next.getOwner(), is(prev.getOwner()));
    }

    @Test
    public void testUpdateAllowOwnerChange() throws Exception {
        WorkspacePreIngestPlugin wpip = makePlugin(ImmutableMap.of());

        WorkspaceMetacardImpl prev = new WorkspaceMetacardImpl("id");
        WorkspaceMetacardImpl next = WorkspaceMetacardImpl.clone(prev);

        prev.setOwner("prev");
        next.setOwner("next");

        wpip.process(updateRequest(prev, next));
        assertThat(next.getOwner(), is("next"));
    }
}