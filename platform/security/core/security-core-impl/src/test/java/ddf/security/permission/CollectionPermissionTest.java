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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ddf.security.permission.impl.CollectionPermissionImpl;
import ddf.security.permission.impl.KeyValuePermissionImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.shiro.authz.Permission;
import org.junit.Test;

public class CollectionPermissionTest {

  @Test
  public void testCreateCollectionFromList() {
    ArrayList<KeyValuePermission> permissionList = new ArrayList<KeyValuePermission>();
    KeyValuePermission perm1 = new KeyValuePermissionImpl("key1", Arrays.asList("val1"));
    KeyValuePermission perm2 = new KeyValuePermissionImpl("key2", Arrays.asList("val2"));

    permissionList.add(perm1);
    permissionList.add(perm2);

    CollectionPermission collection = new CollectionPermissionImpl("", permissionList);

    List<Permission> incomingList = collection.getPermissionList();
    assertFalse(incomingList.isEmpty());
    assertEquals(permissionList.size(), incomingList.size());
    assertEquals(perm1, incomingList.get(0));
    assertEquals(perm2, incomingList.get(1));
  }

  @Test
  public void testCreateCollectionFromArray() {
    KeyValuePermission perm1 = new KeyValuePermissionImpl("key1", Arrays.asList("val1"));
    KeyValuePermission perm2 = new KeyValuePermissionImpl("key2", Arrays.asList("val2"));

    CollectionPermission collection = new CollectionPermissionImpl("", perm1, perm2);

    List<Permission> incomingList = collection.getPermissionList();
    assertFalse(incomingList.isEmpty());
    assertEquals(2, incomingList.size());
    assertEquals(perm1, incomingList.get(0));
    assertEquals(perm2, incomingList.get(1));
  }

  /** Tests the collection implying permissions and other collections. */
  @Test
  public void testCollectionImplies() {
    // Permissions of the user
    ArrayList<KeyValuePermission> permissionList = new ArrayList<KeyValuePermission>();
    permissionList.add(new KeyValuePermissionImpl("key1", Arrays.asList("val1")));
    permissionList.add(new KeyValuePermissionImpl("key2", Arrays.asList("val2")));
    permissionList.add(new KeyValuePermissionImpl("key3", Arrays.asList("val3")));

    CollectionPermission userPermission = new CollectionPermissionImpl("", permissionList);

    // user can create
    assertTrue(userPermission.implies(new KeyValuePermissionImpl("key1", Arrays.asList("val1"))));

    // user cannot delete
    assertFalse(
        userPermission.implies(new KeyValuePermissionImpl("key2", Arrays.asList("somevalue"))));

    // user can create and query
    CollectionPermission task1Permission =
        new CollectionPermissionImpl(
            "",
            new KeyValuePermissionImpl("key1", Arrays.asList("val1")),
            new KeyValuePermissionImpl("key2", Arrays.asList("val2")));
    assertTrue(userPermission.implies(task1Permission));

    // user cannot create AND delete
    CollectionPermission task2Permission =
        new CollectionPermissionImpl(
            "",
            new KeyValuePermissionImpl("key1", Arrays.asList("val1")),
            new KeyValuePermissionImpl("somekey", Arrays.asList("somevalue")));
    assertFalse(userPermission.implies(task2Permission));

    // test empty collection (should always return false)
    assertFalse(new CollectionPermissionImpl().implies(userPermission));
  }

  /** Tests that the string output of the collection has correct permissions. */
  @Test
  public void testCollectionToString() {
    ArrayList<KeyValuePermission> permissionList = new ArrayList<KeyValuePermission>();
    permissionList.add(new KeyValuePermissionImpl("key1", Arrays.asList("val1")));
    permissionList.add(new KeyValuePermissionImpl("key2", Arrays.asList("val2")));

    CollectionPermission collection = new CollectionPermissionImpl("", permissionList);

    // String outputs the correct collection permissions.
    assertTrue(collection.toString().indexOf("key2") != -1);

    // String does not output extra permissions
    assertFalse(collection.toString().indexOf("key3") != -1);
  }

  /** Tests that the permission list which comes back is the same as what was put in. */
  @Test
  public void testGetPermissionList() {
    ArrayList<KeyValuePermission> permissionList = new ArrayList<KeyValuePermission>();
    permissionList.add(new KeyValuePermissionImpl("key1", Arrays.asList("val1")));
    permissionList.add(new KeyValuePermissionImpl("key2", Arrays.asList("val2")));
    CollectionPermission collection = new CollectionPermissionImpl("", permissionList);

    assertEquals(permissionList, collection.getPermissionList());
  }

  /** Tests that the permission list that comes back cannot be modified. */
  @Test(expected = UnsupportedOperationException.class)
  public void testModifyPermissionList() {
    ArrayList<KeyValuePermission> permissionList = new ArrayList<KeyValuePermission>();
    permissionList.add(new KeyValuePermissionImpl("key1", Arrays.asList("val1")));
    permissionList.add(new KeyValuePermissionImpl("key2", Arrays.asList("val2")));
    CollectionPermission collection = new CollectionPermissionImpl("", permissionList);

    collection.getPermissionList().clear();
    fail("Returned list should not be able to modify.");
  }

  /** Tests that the collection was properly cleared out after calling clear. */
  @Test
  public void testClearCollection() {
    ArrayList<KeyValuePermission> permissionList = new ArrayList<KeyValuePermission>();
    permissionList.add(new KeyValuePermissionImpl("key1", Arrays.asList("val1")));
    permissionList.add(new KeyValuePermissionImpl("key2", Arrays.asList("val2")));
    CollectionPermission collection = new CollectionPermissionImpl("", permissionList);
    collection.clear();
    assertTrue(collection.getPermissionList().isEmpty());
  }

  /** Tests that all of the items were added when calling addAll(). */
  @Test
  public void testAddAllCollection() {
    CollectionPermission collection = new CollectionPermissionImpl();

    assertTrue(collection.getPermissionList().isEmpty());

    ArrayList<KeyValuePermission> permissionList = new ArrayList<KeyValuePermission>();
    permissionList.add(new KeyValuePermissionImpl("key1", Arrays.asList("val1")));
    permissionList.add(new KeyValuePermissionImpl("key2", Arrays.asList("val2")));

    collection.addAll(permissionList);

    assertFalse(collection.getPermissionList().isEmpty());
    assertEquals(permissionList.size(), collection.getPermissionList().size());
  }
}
