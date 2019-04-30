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
package org.codice.ddf.catalog.ui.security.accesscontrol;

import static org.codice.ddf.catalog.ui.security.accesscontrol.AclTestSupport.metacardFromAttributes;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Security;
import org.junit.Test;

public class AclTestSupportTest {

  @Test
  public void testMetacardFromAttributes() {
    Metacard metacard =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                "123",
                Core.METACARD_OWNER,
                "owner",
                SecurityAttributes.ACCESS_ADMINISTRATORS,
                "owner"));

    assertThat(metacard.getAttribute(Core.ID).getValue(), is("123"));
    assertThat(metacard.getAttribute(Core.METACARD_OWNER).getValue(), is("owner"));
    assertThat(
        metacard.getAttribute(Security.ACCESS_ADMINISTRATORS).getValues(),
        is(ImmutableList.of("owner")));
  }
}
