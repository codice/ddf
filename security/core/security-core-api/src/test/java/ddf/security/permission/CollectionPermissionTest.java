/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.security.permission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.shiro.authz.Permission;
import org.junit.Test;

public class CollectionPermissionTest {

    @Test
    public void testCreateCollectionFromList() {
        ArrayList<ActionPermission> permissionList = new ArrayList<ActionPermission>();
        ActionPermission perm1 = new ActionPermission(ActionPermission.CREATE_ACTION);
        ActionPermission perm2 = new ActionPermission(ActionPermission.QUERY_ACTION);

        permissionList.add(perm1);
        permissionList.add(perm2);

        CollectionPermission collection = new CollectionPermission(permissionList);

        List<Permission> incomingList = collection.getPermissionList();
        assertFalse(incomingList.isEmpty());
        assertEquals(permissionList.size(), incomingList.size());
        assertEquals(perm1, incomingList.get(0));
        assertEquals(perm2, incomingList.get(1));
    }

    @Test
    public void testCreateCollectionFromArray() {
        ActionPermission perm1 = new ActionPermission(ActionPermission.CREATE_ACTION);
        ActionPermission perm2 = new ActionPermission(ActionPermission.QUERY_ACTION);

        CollectionPermission collection = new CollectionPermission(perm1, perm2);

        List<Permission> incomingList = collection.getPermissionList();
        assertFalse(incomingList.isEmpty());
        assertEquals(2, incomingList.size());
        assertEquals(perm1, incomingList.get(0));
        assertEquals(perm2, incomingList.get(1));
    }

    /**
     * Tests the collection implying permissions and other collections.
     */
    @Test
    public void testCollectionImplies() {
        // Permissions of the user
        ArrayList<ActionPermission> permissionList = new ArrayList<ActionPermission>();
        permissionList.add(new ActionPermission(ActionPermission.CREATE_ACTION));
        permissionList.add(new ActionPermission(ActionPermission.QUERY_ACTION));
        permissionList.add(new ActionPermission(ActionPermission.UPDATE_ACTION));

        CollectionPermission userPermission = new CollectionPermission(permissionList);

        // user can create
        assertTrue(userPermission.implies(new ActionPermission(ActionPermission.CREATE_ACTION)));

        // user cannot delete
        assertFalse(userPermission.implies(new ActionPermission(ActionPermission.DELETE_ACTION)));

        // user can create and query
        CollectionPermission task1Permission = new CollectionPermission(new ActionPermission(
                ActionPermission.CREATE_ACTION),
                new ActionPermission(ActionPermission.QUERY_ACTION));
        assertTrue(userPermission.implies(task1Permission));

        // user cannot create AND delete
        CollectionPermission task2Permission = new CollectionPermission(new ActionPermission(
                ActionPermission.CREATE_ACTION), new ActionPermission(
                ActionPermission.DELETE_ACTION));
        assertFalse(userPermission.implies(task2Permission));

        // test empty collection (should always return false)
        assertFalse(new CollectionPermission().implies(userPermission));
    }

    /**
     * Tests that the string output of the collection has correct permissions.
     */
    @Test
    public void testCollectionToString() {
        ArrayList<ActionPermission> permissionList = new ArrayList<ActionPermission>();
        permissionList.add(new ActionPermission(ActionPermission.CREATE_ACTION));
        permissionList.add(new ActionPermission(ActionPermission.QUERY_ACTION));

        CollectionPermission collection = new CollectionPermission(permissionList);

        // String outputs the correct collection permissions.
        assertTrue(collection.toString().indexOf(ActionPermission.CREATE_ACTION) != -1);

        // String does not output extra permissions
        assertFalse(collection.toString().indexOf(ActionPermission.DELETE_ACTION) != -1);
    }

    /**
     * Tests that the permission list which comes back is the same as what was put in.
     */
    @Test
    public void testGetPermissionList() {
        ArrayList<ActionPermission> permissionList = new ArrayList<ActionPermission>();
        permissionList.add(new ActionPermission(ActionPermission.CREATE_ACTION));
        permissionList.add(new ActionPermission(ActionPermission.QUERY_ACTION));
        CollectionPermission collection = new CollectionPermission(permissionList);

        assertEquals(permissionList, collection.getPermissionList());
    }

    /**
     * Tests that the permission list that comes back cannot be modified.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testModifyPermissionList() {
        ArrayList<ActionPermission> permissionList = new ArrayList<ActionPermission>();
        permissionList.add(new ActionPermission(ActionPermission.CREATE_ACTION));
        permissionList.add(new ActionPermission(ActionPermission.QUERY_ACTION));
        CollectionPermission collection = new CollectionPermission(permissionList);

        collection.getPermissionList().clear();
        fail("Returned list should not be able to modify.");
    }

    /**
     * Tests that the collection was properly cleared out after calling clear.
     */
    @Test
    public void testClearCollection() {
        ArrayList<ActionPermission> permissionList = new ArrayList<ActionPermission>();
        permissionList.add(new ActionPermission(ActionPermission.CREATE_ACTION));
        permissionList.add(new ActionPermission(ActionPermission.QUERY_ACTION));
        CollectionPermission collection = new CollectionPermission(permissionList);
        collection.clear();
        assertTrue(collection.getPermissionList().isEmpty());
    }

    /**
     * Tests that all of the items were added when calling addAll().
     */
    @Test
    public void testAddAllCollection() {
        CollectionPermission collection = new CollectionPermission();

        assertTrue(collection.getPermissionList().isEmpty());

        ArrayList<ActionPermission> permissionList = new ArrayList<ActionPermission>();
        permissionList.add(new ActionPermission(ActionPermission.CREATE_ACTION));
        permissionList.add(new ActionPermission(ActionPermission.QUERY_ACTION));

        collection.addAll(permissionList);

        assertFalse(collection.getPermissionList().isEmpty());
        assertEquals(permissionList.size(), collection.getPermissionList().size());
    }
}
