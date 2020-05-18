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
package ddf.security.permission.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class PermissionsTest {

  private PermissionsImpl permissions = new PermissionsImpl();

  @Test
  public void testParsePermissions() throws Exception {
    Map<String, Set<String>> map = permissions.parsePermissionsFromString((String) null);
    assertThat(map.size(), is(0));

    map = permissions.parsePermissionsFromString("someRandomString");
    assertThat(map.size(), is(0));

    map = permissions.parsePermissionsFromString("too=many=equals");
    assertThat(map.size(), is(0));

    map = permissions.parsePermissionsFromString("too=many=equals", "valid=permission");
    assertThat(map.size(), is(1));
    assertThat(map.get("valid").iterator().next(), equalTo("permission"));

    map = permissions.parsePermissionsFromString("name=value1");
    assertThat(map.size(), is(1));
    assertThat(map.get("name").iterator().next(), equalTo("value1"));

    map = permissions.parsePermissionsFromString("name=value1,value2");
    assertThat(map.size(), is(1));
    assertThat(map.get("name").size(), is(2));
  }

  @Test
  public void testGetPermissions() throws Exception {
    List<String> permStrings = permissions.getPermissionsAsStrings(null);
    assertThat(permStrings.size(), is(0));

    Map<String, Set<String>> map =
        permissions.parsePermissionsFromString("name=value1,value2", "name2=value3");
    permStrings = permissions.getPermissionsAsStrings(map);
    assertThat(
        permStrings.contains("name=value1,value2") || permStrings.contains("name=value2,value1"),
        is(true));
    assertThat(permStrings.contains("name2=value3"), is(true));
  }
}
