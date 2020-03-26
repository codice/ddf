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

import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValuePermission;
import java.util.Collection;
import org.apache.shiro.authz.Permission;

/**
 * Permission class handling the match one case. Shiro permissions always "match all" attributes.
 * This class extends the CollectionPermission and overrides the implies method to perform the
 * implies for a "match one" condition.
 */
public class MatchOneCollectionPermission extends CollectionPermissionImpl {
  public MatchOneCollectionPermission(Collection<Permission> permissions) {
    super(null, permissions);
  }

  /**
   * Overrides the implies method to handle checking for the existence of one attribute - the "match
   * one" scenario rather than the "match all" behavior of the overridden classes. Specifically,
   * this permission will imply another permission if that permission matches at least one of our
   * permission attributes.
   *
   * @param p the permission to check for behavior/functionality comparison.
   * @return {@code true} if this current instance <em>implies</em> the specified {@code Permission}
   *     argument, {@code false} otherwise.
   */
  @Override
  public boolean implies(Permission p) {
    if (permissionList.isEmpty()) {
      return false;
    }

    if (p instanceof CollectionPermission) {
      for (Permission perm : ((CollectionPermission) p).getPermissionList()) {
        boolean result = false;
        for (Permission ourPerm : permissionList) {
          // we only care about the key value permission here, because that one can have
          // multiple values
          // mapped to a single key. In the case of "match one" we only need one of those
          // values to satisfy
          // the permission.
          if (ourPerm instanceof KeyValuePermission) {
            for (String value : ((KeyValuePermission) ourPerm).getValues()) {
              // Since this is "match one" we know that only one of these values needs
              // to match in order
              // for the entire permission at that key to be implied
              // So here we loop through all of the values assigned to that key and
              // create new
              // single valued key value permissions
              KeyValuePermission kvp =
                  new KeyValuePermissionImpl(((KeyValuePermission) ourPerm).getKey());
              kvp.addValue(value);
              if (perm.implies(kvp)) {
                result = true;
                break;
              }
            }
            // Currently we use key value permissions for everything. However, we still need
            // to be able to handle
            // permissions other than KV, so this else block will serve as the catch all for
            // everything else.
          } else {
            // Shiro permissions are always a "match all" condition so we need to flip
            // the implies to make it match one
            if (perm.implies(ourPerm)) {
              result = true;
              break;
            }
          }
        }
        if (!result) {
          return false;
        }
      }
      return true;
    }

    // default catch all permission check
    for (Permission permission : permissionList) {
      // Shiro permissions are always a "match all" condition so we need to flip the implies
      // to make it match one
      if (p.implies(permission)) {
        return true;
      }
    }
    return false;
  }
}
