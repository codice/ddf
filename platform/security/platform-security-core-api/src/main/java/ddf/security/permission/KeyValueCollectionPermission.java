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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** An extension of ColectionPermission that exclusively holds KeyValuePermission objects. */
public class KeyValueCollectionPermission extends CollectionPermission {

  /** Creates an empty collection that can hold KeyValuePermission objects. */
  public KeyValueCollectionPermission() {}

  /**
   * Creates an empty collection with an associated action
   *
   * @param action
   */
  public KeyValueCollectionPermission(String action) {
    super(action);
  }

  /**
   * Creates a new collection of KeyValuePermission objects and adds the provided permissions to the
   * newly created collection.
   *
   * @param action Action associated with this collection of permissions
   * @param permissions KeyValuePermission objects to be added to the newly created collection
   */
  public KeyValueCollectionPermission(String action, KeyValuePermission... permissions) {
    super(action, permissions);
  }

  /**
   * Creates a new collection of KeyValuePermission objects from an existing map of keys and values.
   * Each key and associated list of values is turned into a KeyValuePermission and added to the
   * newly created collection.
   *
   * @param action Action associated with this collection of permissions
   * @param map collection of keys and their associated list of values to be added to the newly
   *     created collection
   */
  public KeyValueCollectionPermission(
      String action, Map<String, ? extends Collection<String>> map) {
    super(action);
    addAll(map);
  }

  /**
   * Creates a new collection of KeyValuePermission objects from an existing collection of
   * KeyValuePermission objects. All KeyValuePermission objects in the provided collection are added
   * to the newly created collection.
   *
   * @param action Action associated with this collection of permissions
   * @param permissions existing collection of KeyValuePermission objects
   */
  public KeyValueCollectionPermission(String action, Collection<KeyValuePermission> permissions) {
    super(action, permissions);
  }

  /**
   * Returns the KeyValuePermission collection as a List of KeyValuePermission objects.
   *
   * @param <T> specified by the type of the calling object - should be KeyValuePermission to avoid
   *     class cast exceptions
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
   * @param map collection of keys and their associated list of values to be added to the newly
   *     created collection
   */
  public void addAll(Map<String, ? extends Collection<String>> map) {
    permissionList.addAll(
        map.entrySet().stream()
            .map(entry -> new KeyValuePermission(entry.getKey(), new HashSet<>(entry.getValue())))
            .collect(Collectors.toList()));
  }
}
