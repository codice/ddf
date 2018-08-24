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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class AccessControlPolicyPluginTest {

  private Map properties;

  private PolicyPlugin plugin;

  private static final String EMAIL = "a@b.c";

  private static final Serializable MOCK_ID = "100";

  @Before
  public void setUp() {
    properties = mock(Map.class);
    plugin = new AccessControlPolicyPlugin();
  }

  @Test
  public void testAdminOnCreateWithoutAccessControl() throws Exception {
    Metacard metacard = new MetacardImpl();
    AccessControlUtil.setOwner(metacard, EMAIL);
    PolicyResponse response = plugin.processPreCreate(metacard, properties);
    assertThat(response.itemPolicy(), is(Collections.emptyMap()));
  }

  @Test
  public void testAdminOnCreateWithAccessControl() throws Exception {
    Metacard metacard =
        AccessControlUtil.metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                MOCK_ID,
                Core.METACARD_OWNER,
                "owner",
                SecurityAttributes.ACCESS_ADMINISTRATORS,
                ImmutableList.of("owner"),
                SecurityAttributes.ACCESS_GROUPS,
                ImmutableList.of(""),
                SecurityAttributes.ACCESS_INDIVIDUALS,
                ImmutableList.of("")));

    PolicyResponse response = plugin.processPreUpdate(metacard, properties);
    assertThat(
        response.itemPolicy(),
        is(
            ImmutableMap.of(
                SecurityAttributes.ACCESS_GROUPS,
                Collections.singleton(""),
                SecurityAttributes.ACCESS_ADMINISTRATORS,
                Collections.singleton("owner"),
                SecurityAttributes.ACCESS_INDIVIDUALS,
                Collections.singleton(""),
                Core.METACARD_OWNER,
                Collections.singleton("owner"))));
  }
}
