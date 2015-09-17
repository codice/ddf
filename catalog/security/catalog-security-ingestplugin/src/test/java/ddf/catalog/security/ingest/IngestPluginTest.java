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
package ddf.catalog.security.ingest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.util.ThreadContext;
import org.junit.Test;

import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.Subject;
import ddf.security.permission.KeyValuePermission;

public class IngestPluginTest {

    @Test
    public void testParseEmptyPermissions() throws Exception {
        IngestPlugin ingestPlugin = new IngestPlugin();
        ingestPlugin.setPermissionStrings(new String[]{});
        assertThat(ingestPlugin.getPermissions().size(), equalTo(0));
    }

    @Test
    public void testParseNullPermissions() throws Exception {
        IngestPlugin ingestPlugin = new IngestPlugin();
        ingestPlugin.setPermissionStrings(null);
        assertThat(ingestPlugin.getPermissions().size(), equalTo(0));
    }

    @Test
    public void testParsePermissionsSingleCondition() throws Exception {
        IngestPlugin ingestPlugin = new IngestPlugin();
        ingestPlugin.setPermissionStrings(new String[]{"role=admin"});
        List<KeyValuePermission> perms = ingestPlugin.getPermissions();
        assertThat(perms.size(), equalTo(1));
        assertThat(perms.get(0).getKey(), is(equalTo("role")));
        assertThat(perms.get(0).getValues().size(), equalTo(1));
        assertThat(perms.get(0).getValues().get(0), is(equalTo("admin")));
    }

    @Test
    public void testParsePermissionsMultiCondition() throws Exception {
        IngestPlugin ingestPlugin = new IngestPlugin();
        ingestPlugin.setPermissionStrings(new String[]{"role=admin", "name=myname"});
        List<KeyValuePermission> perms = ingestPlugin.getPermissions();
        assertThat(perms.size(), equalTo(2));
        assertThat(perms.get(0).getKey(), is(equalTo("role")));
        assertThat(perms.get(0).getValues().size(), equalTo(1));
        assertThat(perms.get(0).getValues().get(0), is(equalTo("admin")));
        assertThat(perms.get(1).getKey(), is(equalTo("name")));
        assertThat(perms.get(1).getValues().size(), equalTo(1));
        assertThat(perms.get(1).getValues().get(0), is(equalTo("myname")));
    }

    @Test
    public void testParsePermissionsBadFormat() throws Exception {
        IngestPlugin ingestPlugin = new IngestPlugin();
        ingestPlugin.setPermissionStrings(new String[] {"role->admin"});
        List<KeyValuePermission> perms = ingestPlugin.getPermissions();
        assertThat(perms.size(), equalTo(0));
    }

    @Test
    public void testCreateProcessGoodSubject() throws Exception {
        IngestPlugin ingestPlugin = new IngestPlugin();
        ingestPlugin.setPermissionStrings(new String[]{"role=admin"});

        Subject subject = mock(Subject.class);
        when(subject.isPermitted(any(Permission.class))).thenReturn(true);
        ThreadContext.bind(subject);

        CreateRequest request = mock(CreateRequest.class);
        CreateRequest response = ingestPlugin.process(request);
        assertThat(response, not(equalTo(null)));

        ThreadContext.unbindSubject();
    }

    @Test
    public void testUpdateProcessGoodSubject() throws Exception {
        IngestPlugin ingestPlugin = new IngestPlugin();
        ingestPlugin.setPermissionStrings(new String[]{"role=admin"});

        Subject subject = mock(Subject.class);
        when(subject.isPermitted(any(Permission.class))).thenReturn(true);
        ThreadContext.bind(subject);

        UpdateRequest request = mock(UpdateRequest.class);
        UpdateRequest response = ingestPlugin.process(request);
        assertThat(response, not(equalTo(null)));

        ThreadContext.unbindSubject();
    }

    @Test
    public void testDeleteProcessGoodSubject() throws Exception {
        IngestPlugin ingestPlugin = new IngestPlugin();
        ingestPlugin.setPermissionStrings(new String[]{"role=admin"});

        Subject subject = mock(Subject.class);
        when(subject.isPermitted(any(Permission.class))).thenReturn(true);
        ThreadContext.bind(subject);

        DeleteRequest request = mock(DeleteRequest.class);
        DeleteRequest response = ingestPlugin.process(request);
        assertThat(response, not(equalTo(null)));

        ThreadContext.unbindSubject();
    }

    @Test(expected = StopProcessingException.class)
    public void testCreateProcessBadSubject() throws Exception {
        IngestPlugin ingestPlugin = new IngestPlugin();
        ingestPlugin.setPermissionStrings(new String[]{"role=admin"});

        Subject subject = mock(Subject.class);
        when(subject.isPermitted(any(Permission.class))).thenReturn(false);
        ThreadContext.bind(subject);

        CreateRequest request = mock(CreateRequest.class);
        CreateRequest response = ingestPlugin.process(request);

        ThreadContext.unbindSubject();
    }

    @Test(expected = StopProcessingException.class)
    public void testUpdateProcessBadSubject() throws Exception {
        IngestPlugin ingestPlugin = new IngestPlugin();
        ingestPlugin.setPermissionStrings(new String[]{"role=admin"});

        Subject subject = mock(Subject.class);
        when(subject.isPermitted(any(Permission.class))).thenReturn(false);
        ThreadContext.bind(subject);

        UpdateRequest request = mock(UpdateRequest.class);
        UpdateRequest response = ingestPlugin.process(request);

        ThreadContext.unbindSubject();
    }

    @Test(expected = StopProcessingException.class)
    public void testDeleteProcessBadSubject() throws Exception {
        IngestPlugin ingestPlugin = new IngestPlugin();
        ingestPlugin.setPermissionStrings(new String[]{"role=admin"});

        Subject subject = mock(Subject.class);
        when(subject.isPermitted(any(Permission.class))).thenReturn(false);
        ThreadContext.bind(subject);

        DeleteRequest request = mock(DeleteRequest.class);
        DeleteRequest response = ingestPlugin.process(request);

        ThreadContext.unbindSubject();
    }
}