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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service to convert permissions between a permission map and a string or array of string
 * permissions
 */
public interface Permissions {

  /**
   * Parses a string array representation of permission attributes into a map
   *
   * @param permStrings list of permission strings in the format "permName=val1,val2"
   * @return Map<String, Set<String>>
   */
  Map<String, Set<String>> parsePermissionsFromString(List<String> permStrings);

  /**
   * Parses a string array representation of permission attributes into a map
   *
   * @param permStrings array of permission strings in the format "permName=val1,val2"
   * @return Map<String, Set<String>>
   */
  Map<String, Set<String>> parsePermissionsFromString(String... permStrings);

  /**
   * Convert a permission map back to a list of string permissions in the format
   * "permName=val1,val2"
   *
   * @param attributes permission map
   * @return List<String>
   */
  List<String> getPermissionsAsStrings(Map<String, Set<String>> attributes);

  /**
   * Returns a KeyValueCollectionPermission initialized with the given KeyValuePermissions
   *
   * @param action
   * @param permissions
   * @return
   */
  KeyValueCollectionPermission buildKeyValueCollectionPermission(
      String action, KeyValuePermission... permissions);

  /**
   * Returns a KeyValueCollectionPermission initialized with the given permissions in string format
   *
   * @param action
   * @param map
   * @return
   */
  KeyValueCollectionPermission buildKeyValueCollectionPermission(
      String action, Map<String, ? extends Collection<String>> map);

  /**
   * Returns a KeyValueCollectionPermission initialized with the given KeyValuePermissions
   *
   * @param action
   * @param permissions
   * @return
   */
  KeyValueCollectionPermission buildKeyValueCollectionPermission(
      String action, Collection<KeyValuePermission> permissions);

  /**
   * Returns a KeyValuePermission with the provided key and values
   *
   * @param key
   * @param values
   * @return
   */
  KeyValuePermission buildKeyValuePermission(String key, Set<String> values);
}
