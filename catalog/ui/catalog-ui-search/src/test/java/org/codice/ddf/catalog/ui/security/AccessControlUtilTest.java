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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;
import org.junit.Test;

public class AccessControlUtilTest {

  @Test
  public void getValuesOrEmpty() {}

  @Test
  public void getAccessIndividuals() {}

  @Test
  public void getAccessGroups() {}

  @Test
  public void getAccessAdministrators() {}

  @Test
  public void setOwner() {
    String id = "0";

    Metacard before =
        AccessControlUtil.metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                id,
                Core.METACARD_OWNER,
                "before",
                SecurityAttributes.ACCESS_GROUPS,
                ImmutableSet.of("admin")));

    Metacard after =
        AccessControlUtil.metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                id,
                Core.METACARD_OWNER,
                "before",
                SecurityAttributes.ACCESS_GROUPS,
                ImmutableSet.of("admin", "guest")));
  }

  @Test
  public void getOwner() {}

  @Test
  public void metacardFromAttributes() {}

  @Test
  public void isAnyObjectNull() {}
}
