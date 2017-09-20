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
package org.codice.ddf.catalog.ui.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.types.Core;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import java.util.Collections;
import java.util.Map;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.junit.Before;
import org.junit.Test;

public class WorkspaceSharingPolicyPluginTest {

  private Map properties;

  private PolicyPlugin plugin;

  @Before
  public void setUp() {
    properties = mock(Map.class);
    plugin = new WorkspaceSharingPolicyPlugin();
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
    assertThat(
        response.itemPolicy(),
        is(
            ImmutableMap.of(
                Core.METACARD_OWNER,
                ImmutableSet.of(email),
                Constants.IS_WORKSPACE,
                ImmutableSet.of(Constants.IS_WORKSPACE))));
  }
}
