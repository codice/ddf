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

import static org.codice.ddf.catalog.ui.security.AclTestSupport.metacardFromAttributes;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;
import org.junit.Test;

public class AccessControlUtilTest {

  @Test
  public void getAccessIndividuals() {
    Metacard metacard =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                "123",
                Core.METACARD_OWNER,
                "owner",
                SecurityAttributes.ACCESS_INDIVIDUALS,
                ImmutableList.of("person1", "person2")));
    assertThat(
        AccessControlUtil.getAccessIndividuals(metacard),
        is(ImmutableSet.of("person1", "person2")));
  }

  @Test
  public void getAccessGroups() {
    Metacard metacard =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                "123",
                Core.METACARD_OWNER,
                "owner",
                SecurityAttributes.ACCESS_GROUPS,
                ImmutableList.of("person1", "person2")));
    assertThat(
        AccessControlUtil.getAccessGroups(metacard), is(ImmutableSet.of("person1", "person2")));
  }

  @Test
  public void getAccessAdministrators() {
    Metacard metacard =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                "123",
                Core.METACARD_OWNER,
                "owner",
                SecurityAttributes.ACCESS_ADMINISTRATORS,
                ImmutableList.of("person1", "person2")));
    assertThat(
        AccessControlUtil.getAccessAdministrators(metacard),
        is(ImmutableSet.of("person1", "person2")));
  }

  @Test
  public void setOwner() {
    String id = "0";
    Metacard before =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                id,
                Core.METACARD_OWNER,
                "before",
                SecurityAttributes.ACCESS_GROUPS,
                ImmutableSet.of("admin")));

    assertThat(before.getAttribute(Core.METACARD_OWNER).getValue(), is("before"));
    AccessControlUtil.setOwner(before, "newowner");
    assertThat(before.getAttribute(Core.METACARD_OWNER).getValue(), is("newowner"));
  }

  @Test
  public void getOwner() {
    Metacard metacard =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                "123",
                Core.METACARD_OWNER,
                "owner",
                SecurityAttributes.ACCESS_ADMINISTRATORS,
                "owner"));
    assertThat(metacard.getAttribute(Core.METACARD_OWNER).getValue(), is("owner"));
  }

  @Test
  public void isAnyObjectNull() {
    assertThat(AccessControlUtil.isAnyObjectNull(null, 1, 4, "test"), is(true));
    assertThat(AccessControlUtil.isAnyObjectNull(3, 1, 4, "test"), is(false));
  }
}
