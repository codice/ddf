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
package ddf.catalog.security.ingest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;

public class IngestPluginTest {

    IngestPlugin ingestPlugin;

    @Before
    public void setup() {
        ingestPlugin = new IngestPlugin();
    }

    @Test
    public void testParseEmptyPermissions() throws Exception {
        ingestPlugin.setPermissionStrings(new String[] {});
        assertThat(ingestPlugin.getPermissions()
                .size(), equalTo(0));
    }

    @Test
    public void testParseNullPermissions() throws Exception {
        ingestPlugin.setPermissionStrings(null);
        assertThat(ingestPlugin.getPermissions()
                .size(), equalTo(0));
    }

    @Test
    public void testParsePermissionsSingleCondition() throws Exception {
        ingestPlugin.setPermissionStrings(new String[] {"role=admin"});
        Map<String, Set<String>> perms = ingestPlugin.getPermissions();
        assertThat(perms.size(), equalTo(1));
        assertThat(perms.get("role")
                .iterator()
                .next(), is(equalTo("admin")));
    }

    @Test
    public void testParsePermissionsMultiCondition() throws Exception {
        ingestPlugin.setPermissionStrings(new String[] {"role=admin", "name=myname"});
        Map<String, Set<String>> perms = ingestPlugin.getPermissions();
        assertThat(perms.size(), equalTo(2));
        assertThat(perms.get("role")
                .iterator()
                .next(), is(equalTo("admin")));
        assertThat(perms.get("name")
                .iterator()
                .next(), is(equalTo("myname")));
    }

    @Test
    public void testParsePermissionsBadFormat() throws Exception {
        ingestPlugin.setPermissionStrings(new String[] {"role->admin"});
        Map<String, Set<String>> perms = ingestPlugin.getPermissions();
        assertThat(perms.size(), equalTo(0));
    }

    @Test
    public void testPreQuery() throws StopProcessingException {
        PolicyResponse response = ingestPlugin.processPreQuery(mock(Query.class), new HashMap<>());
        assertThat(response.itemPolicy()
                .size(), equalTo(0));
        assertThat(response.operationPolicy()
                .size(), equalTo(0));
    }

    @Test
    public void testPreResources() throws StopProcessingException {
        ResourceRequest resourceRequest = mock(ResourceRequest.class);
        PolicyResponse response = ingestPlugin.processPreResource(resourceRequest);
        assertThat(response.itemPolicy()
                .size(), equalTo(0));
        assertThat(response.operationPolicy()
                .size(), equalTo(0));
    }

    @Test
    public void testPostQuery() throws StopProcessingException {
        PolicyResponse response = ingestPlugin.processPostQuery(mock(Result.class), new HashMap<>());
        assertThat(response.itemPolicy()
                .size(), equalTo(0));
        assertThat(response.operationPolicy()
                .size(), equalTo(0));
    }

    @Test
    public void testPostResources() throws StopProcessingException {
        ResourceResponse resourceResponse = mock(ResourceResponse.class);
        PolicyResponse response = ingestPlugin.processPostResource(resourceResponse,
                mock(Metacard.class));
        assertThat(response.itemPolicy()
                .size(), equalTo(0));
        assertThat(response.operationPolicy()
                .size(), equalTo(0));
    }

    @Test
    public void testPreCreate() throws StopProcessingException {
        ingestPlugin.setPermissionStrings(new String[] {"role=admin"});
        PolicyResponse response = ingestPlugin.processPreCreate(mock(Metacard.class),
                new HashMap<>());
        assertThat(response.itemPolicy()
                .size(), equalTo(0));
        assertThat(response.operationPolicy()
                .size(), equalTo(1));
        response = ingestPlugin.processPreCreate(mock(Metacard.class), Collections.singletonMap(Constants.LOCAL_DESTINATION_KEY, false));
        assertThat(response.itemPolicy()
                .size(), equalTo(0));
        assertThat(response.operationPolicy()
                .size(), equalTo(0));

    }

    @Test
    public void testPreUpdate() throws StopProcessingException {
        ingestPlugin.setPermissionStrings(new String[] {"role=admin"});
        PolicyResponse response = ingestPlugin.processPreUpdate(mock(Metacard.class),
                new HashMap<>());
        assertThat(response.itemPolicy()
                .size(), equalTo(0));
        assertThat(response.operationPolicy()
                .size(), equalTo(1));
        response = ingestPlugin.processPreUpdate(mock(Metacard.class), Collections.singletonMap(Constants.LOCAL_DESTINATION_KEY, false));
        assertThat(response.itemPolicy()
                .size(), equalTo(0));
        assertThat(response.operationPolicy()
                .size(), equalTo(0));
    }

    @Test
    public void testPreDelete() throws StopProcessingException {
        ingestPlugin.setPermissionStrings(new String[] {"role=admin"});
        PolicyResponse response = ingestPlugin.processPreDelete("blah", new ArrayList<>(),
                new HashMap<>());
        assertThat(response.itemPolicy()
                .size(), equalTo(0));
        assertThat(response.operationPolicy()
                .size(), equalTo(1));
        response = ingestPlugin.processPreDelete("blah", new ArrayList<>(), Collections.singletonMap(Constants.LOCAL_DESTINATION_KEY, false));
        assertThat(response.itemPolicy()
                .size(), equalTo(0));
        assertThat(response.operationPolicy()
                .size(), equalTo(0));
    }

    @Test
    public void testGetPerms() throws StopProcessingException {
        ingestPlugin.setPermissionStrings(new String[] {"role=admin"});
        String[] permissionStrings = ingestPlugin.getPermissionStrings();
        assertThat(permissionStrings.length, equalTo(1));
    }
}