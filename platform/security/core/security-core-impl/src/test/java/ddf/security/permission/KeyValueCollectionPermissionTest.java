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
package ddf.security.permission;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ddf.security.permission.impl.KeyValueCollectionPermissionImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class KeyValueCollectionPermissionTest {

  private static final String CAR_PERM = "CAR_PERMISSIONS";

  private static final String BIKE_PERM = "BICYCLE_PERMISSIONS";

  @Test
  public void testCreateWithKeyMap() {
    List<String> carPermissions = new ArrayList<String>();
    carPermissions.add("START");
    carPermissions.add("ACCELERATE");
    carPermissions.add("PARK");

    List<String> bicyclePermissions = new ArrayList<String>();
    bicyclePermissions.add("PEDAL");

    Map<String, List<String>> map = new HashMap<String, List<String>>();

    map.put(CAR_PERM, carPermissions);
    map.put(BIKE_PERM, bicyclePermissions);

    KeyValueCollectionPermission kvcp = new KeyValueCollectionPermissionImpl("", map);

    List<KeyValuePermission> permissions = kvcp.getKeyValuePermissionList();
    for (KeyValuePermission curPerm : permissions) {
      if (curPerm.getKey().equals(CAR_PERM)) {
        for (String curValue : curPerm.getValues()) {
          assertTrue(carPermissions.contains(curValue));
        }
      } else if (curPerm.getKey().equals(BIKE_PERM)) {

        for (String curValue : curPerm.getValues()) {
          assertTrue(bicyclePermissions.contains(curValue));
        }
      } else {
        fail("Unknown permission type found: " + curPerm.toString());
      }
    }
  }
}
