/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.permission;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An extension of ColectionPermission that exclusively holds KeyValuePermission objects.
 */
public class KeyValueCollectionPermission extends CollectionPermission {

    /**
     * Creates an empty collection that can hold KeyValuePermission objects.
     */
    public KeyValueCollectionPermission() {
    }

    /**
     * Creates a new collection of KeyValuePermission objects and adds the provided permissions to
     * the newly created collection.
     *
     * @param permissions
     *            KeyValuePermission objects to be added to the newly created collection
     */
    public KeyValueCollectionPermission(KeyValuePermission... permissions) {
        super(permissions);
    }

    /**
     * Creates a new collection of KeyValuePermission objects from an existing map of keys and
     * values. Each key and associated list of values is turned into a KeyValuePermission and added
     * to the newly created collection.
     *
     * @param map
     *            collection of keys and their associated list of values to be added to the newly
     *            created collection
     */
    public KeyValueCollectionPermission(Map<String, List<String>> map) {
        addAll(map);
    }

    /**
     * Creates a new collection of KeyValuePermission objects from an existing collection of
     * KeyValuePermission objects. All KeyValuePermission objects in the provided collection are
     * added to the newly created collection.
     *
     * @param permissions
     *            existing collection of KeyValuePermission objects
     */
    public KeyValueCollectionPermission(Collection<KeyValuePermission> permissions) {
        super(permissions);
    }

    /**
     * Returns the KeyValuePermission collection as a List of KeyValuePermission objects.
     *
     * @param <T>
     *            specified by the type of the calling object - should be KeyValuePermission to
     *            avoid class cast exceptions
     * @return List of KeyValuePermission that represent the permission in this collection
     */
    public <T> List<T> getKeyValuePermissionList() {
        return (List<T>) Collections.unmodifiableList(permissionList);
    }

    /**
     * Adds all of the incoming key value map entries to this KeyValueCollectionPermission. Each key
     * and associated list of values is turned into a KeyValuePermission and added to the newly
     * created collection.
     *
     * @param map
     *            collection of keys and their associated list of values to be added to the newly
     *            created collection
     */
    public void addAll(Map<String, List<String>> map) {
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            permissionList.add(new KeyValuePermission(entry.getKey(), entry.getValue()));
        }
    }

}
