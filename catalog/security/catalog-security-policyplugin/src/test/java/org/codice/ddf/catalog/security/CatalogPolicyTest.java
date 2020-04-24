/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.util.Assert.isTrue;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.permission.impl.PermissionsImpl;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class CatalogPolicyTest {

  CatalogPolicy policyPlugin;

  @Before
  public void setup() {
    policyPlugin = new CatalogPolicy(new PermissionsImpl());
  }

  @Test
  public void testParseEmptyCreatePermissions() throws Exception {
    policyPlugin.setCreatePermissions(new String[] {});
    assertThat(policyPlugin.getCreatePermissionMap().size(), equalTo(0));
  }

  @Test
  public void testParseEmptyUpdatePermissions() throws Exception {
    policyPlugin.setUpdatePermissions(new String[] {});
    assertThat(policyPlugin.getUpdatePermissionMap().size(), equalTo(0));
  }

  @Test
  public void testParseEmptyDeletePermissions() throws Exception {
    policyPlugin.setDeletePermissions(new String[] {});
    assertThat(policyPlugin.getDeletePermissionMap().size(), equalTo(0));
  }

  @Test
  public void testParseEmptyReadPermissions() throws Exception {
    policyPlugin.setReadPermissions(new String[] {});
    assertThat(policyPlugin.getReadPermissionMap().size(), equalTo(0));
  }

  @Test
  public void testParseNullCreatePermissions() throws Exception {
    policyPlugin.setCreatePermissions(null);
    assertThat(policyPlugin.getCreatePermissionMap().size(), equalTo(0));
  }

  @Test
  public void testParseNullUpdatePermissions() throws Exception {
    policyPlugin.setUpdatePermissions(null);
    assertThat(policyPlugin.getUpdatePermissionMap().size(), equalTo(0));
  }

  @Test
  public void testParseNullDeletePermissions() throws Exception {
    policyPlugin.setDeletePermissions(null);
    assertThat(policyPlugin.getDeletePermissionMap().size(), equalTo(0));
  }

  @Test
  public void testParseNullReadPermissions() throws Exception {
    policyPlugin.setReadPermissions(null);
    assertThat(policyPlugin.getReadPermissionMap().size(), equalTo(0));
  }

  @Test
  public void testParsePermissionsSingleCondition() throws Exception {
    policyPlugin.setCreatePermissions(new String[] {"role=admin"});
    Map<String, Set<String>> perms = policyPlugin.getCreatePermissionMap();
    assertThat(perms.size(), equalTo(1));
    assertThat(perms.get("role").iterator().next(), is(equalTo("admin")));
  }

  @Test
  public void testParsePermissionsMultiCondition() throws Exception {
    policyPlugin.setCreatePermissions(new String[] {"role=admin", "name=myname"});
    Map<String, Set<String>> perms = policyPlugin.getCreatePermissionMap();
    assertThat(perms.size(), equalTo(2));
    assertThat(perms.get("role").iterator().next(), is(equalTo("admin")));
    assertThat(perms.get("name").iterator().next(), is(equalTo("myname")));
  }

  @Test
  public void testParsePermissionsBadFormat() throws Exception {
    policyPlugin.setCreatePermissions(new String[] {"role->admin"});
    Map<String, Set<String>> perms = policyPlugin.getCreatePermissionMap();
    assertThat(perms.size(), equalTo(0));
  }

  @Test
  public void testPostQuery() throws StopProcessingException {
    PolicyResponse response = policyPlugin.processPostQuery(mock(Result.class), new HashMap<>());
    assertThat(response.itemPolicy().size(), equalTo(0));
    assertThat(response.operationPolicy().size(), equalTo(0));
  }

  @Test
  public void testPostResources() throws StopProcessingException {
    ResourceResponse resourceResponse = mock(ResourceResponse.class);
    PolicyResponse response =
        policyPlugin.processPostResource(resourceResponse, mock(Metacard.class));
    assertThat(response.itemPolicy().size(), equalTo(0));
    assertThat(response.operationPolicy().size(), equalTo(0));
  }

  @Test
  public void testPreCreate() throws StopProcessingException {
    policyPlugin.setCreatePermissions(new String[] {"role=admin"});
    PolicyResponse response = policyPlugin.processPreCreate(mock(Metacard.class), new HashMap<>());
    assertThat(response.itemPolicy().size(), equalTo(0));
    assertThat(response.operationPolicy().size(), equalTo(1));
    response =
        policyPlugin.processPreCreate(
            mock(Metacard.class), Collections.singletonMap(Constants.LOCAL_DESTINATION_KEY, false));
    assertThat(response.itemPolicy().size(), equalTo(0));
    assertThat(response.operationPolicy().size(), equalTo(0));
  }

  @Test
  public void testPreUpdate() throws StopProcessingException {
    policyPlugin.setUpdatePermissions(new String[] {"role=admin"});
    PolicyResponse response = policyPlugin.processPreUpdate(mock(Metacard.class), new HashMap<>());
    assertThat(response.itemPolicy().size(), equalTo(0));
    assertThat(response.operationPolicy().size(), equalTo(1));
    response =
        policyPlugin.processPreUpdate(
            mock(Metacard.class), Collections.singletonMap(Constants.LOCAL_DESTINATION_KEY, false));
    assertThat(response.itemPolicy().size(), equalTo(0));
    assertThat(response.operationPolicy().size(), equalTo(0));
  }

  @Test
  public void testPreDelete() throws StopProcessingException {
    policyPlugin.setDeletePermissions(new String[] {"role=admin"});
    PolicyResponse response = policyPlugin.processPreDelete(new ArrayList<>(), new HashMap<>());
    assertThat(response.itemPolicy().size(), equalTo(0));
    assertThat(response.operationPolicy().size(), equalTo(1));
    response =
        policyPlugin.processPreDelete(
            new ArrayList<>(), Collections.singletonMap(Constants.LOCAL_DESTINATION_KEY, false));
    assertThat(response.itemPolicy().size(), equalTo(0));
    assertThat(response.operationPolicy().size(), equalTo(0));
  }

  @Test
  public void testPreQuery() throws StopProcessingException {
    policyPlugin.setReadPermissions(new String[] {"role=admin"});
    PolicyResponse response = policyPlugin.processPreQuery(mock(Query.class), new HashMap<>());
    assertThat(response.itemPolicy().size(), equalTo(0));
    assertThat(response.operationPolicy().size(), equalTo(1));
    response =
        policyPlugin.processPreQuery(
            mock(Query.class), Collections.singletonMap(Constants.LOCAL_DESTINATION_KEY, false));
    assertThat(response.itemPolicy().size(), equalTo(0));
    assertThat(response.operationPolicy().size(), equalTo(0));
  }

  @Test
  public void testPreResource() throws StopProcessingException {
    policyPlugin.setReadPermissions(new String[] {"role=admin"});
    ResourceRequest mock = mock(ResourceRequest.class);
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(Constants.LOCAL_DESTINATION_KEY, true);
    when(mock.getProperties()).thenReturn(properties);
    PolicyResponse response = policyPlugin.processPreResource(mock);
    assertThat(response.itemPolicy().size(), equalTo(0));
    assertThat(response.operationPolicy().size(), equalTo(1));
    properties.put(Constants.LOCAL_DESTINATION_KEY, false);
    response = policyPlugin.processPreResource(mock);
    assertThat(response.itemPolicy().size(), equalTo(0));
    assertThat(response.operationPolicy().size(), equalTo(0));
  }

  @Test
  public void testGetPerms() throws StopProcessingException {
    policyPlugin.setCreatePermissions(new String[] {"role=admin"});
    String[] permissionStrings = policyPlugin.getCreatePermissions();
    assertThat(permissionStrings.length, equalTo(1));
  }

  @Test
  public void testUnusedMethods() throws Exception {
    isTrue(policyPlugin.getReadPermissions() == null);
    isTrue(policyPlugin.getCreatePermissions() == null);
    isTrue(policyPlugin.getUpdatePermissions() == null);
    isTrue(policyPlugin.getDeletePermissions() == null);
    policyPlugin.setReadPermissions(new String[] {"role=admin"});
    policyPlugin.setCreatePermissions(new String[] {"role=admin"});
    policyPlugin.setUpdatePermissions(new String[] {"role=admin"});
    policyPlugin.setDeletePermissions(new String[] {"role=admin"});
    assertThat(policyPlugin.getReadPermissions()[0], equalTo("role=admin"));
    assertThat(policyPlugin.getCreatePermissions()[0], equalTo("role=admin"));
    assertThat(policyPlugin.getUpdatePermissions()[0], equalTo("role=admin"));
    assertThat(policyPlugin.getDeletePermissions()[0], equalTo("role=admin"));

    assertThat(policyPlugin.processPostDelete(null, null).itemPolicy().isEmpty(), is(true));
    assertThat(policyPlugin.processPostDelete(null, null).operationPolicy().isEmpty(), is(true));
  }
}
