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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.shiro.authz.Permission;

/**
 * Permission class handling a collection of permissions and handling the logic to determine if one
 * collection of permissions can imply another collection of permissions. Assumes the underlying
 * permissions can handle instances of differing permission types.
 */
public class CollectionPermission implements Permission {

  public static final String CREATE_ACTION = "create";

  public static final String READ_ACTION = "read";

  public static final String UPDATE_ACTION = "update";

  public static final String REMOVE_USER_ACCESS_ACTION = "remove-user-access";

  public static final String DELETE_ACTION = "delete";

  public static final String UNKNOWN_ACTION = "unknown-action";

  protected static final String PERMISSION_START_MSG = "Permission [";

  protected static final String PERMISSION_IMPLIES_MSG = "] implies permission [";

  protected static final String PERMISSION_NOT_IMPLIES_MSG = "] does not imply permission [";

  protected static final String PERMISSION_END_MSG = "].";

  protected List<Permission> permissionList = new ArrayList<Permission>();

  protected String action;

  /** Default constructor creating an empty collection of permissions. */
  public CollectionPermission() {}

  /** @param action Action associated with this collection of permissions */
  public CollectionPermission(String action) {
    this.action = action;
  }

  /**
   * Creates a new collection of permissions and adds the provided permissions to the collection.
   *
   * @param action Action associated with this collection of permissions
   * @param permissions permission objects to be added to the newly created collection
   */
  public CollectionPermission(String action, Permission... permissions) {
    this.action = action;
    Collections.addAll(permissionList, permissions);
  }

  /**
   * Creates a new collection of permissions from an existing collection of permissions. All
   * permissions in the provided collection are added to the newly created collection.
   *
   * @param action Action associated with this collection of permissions
   * @param permissions existing collection of permission objects
   */
  public CollectionPermission(String action, Collection<? extends Permission> permissions) {
    this.action = action;
    addAll(permissions);
  }

  /**
   * Returns {@code true} if this current instance <em>implies</em> all the functionality and/or
   * resource access described by the specified {@code Permission} argument, {@code false}
   * otherwise.
   *
   * <p>
   *
   * <p>That is, this current instance must be exactly equal to or a <em>superset</em> of the
   * functionalty and/or resource access described by the given {@code Permission} argument. Yet
   * another way of saying this would be:
   *
   * <p>
   *
   * <p>If &quot;permission1 implies permission2&quot;, i.e. <code>permission1.implies(permission2)
   * </code> , then any Subject granted {@code permission1} would have ability greater than or equal
   * to that defined by {@code permission2}.
   *
   * @param p the permission to check for behavior/functionality comparison.
   * @return {@code true} if this current instance <em>implies</em> all the functionality and/or
   *     resource access described by the specified {@code Permission} argument, {@code false}
   *     otherwise.
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
          if (ourPerm.implies(perm)) {
            result = true;
            break;
          }
        }
        if (!result) {
          return false;
        }
      }
      return true;
    }

    for (Permission permission : permissionList) {
      if (permission.implies(p)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns this collection as an unmodifiable list of permissions.
   *
   * @return unmodifiable List of permissions corresponding to this collection
   */
  public List<Permission> getPermissionList() {
    return Collections.unmodifiableList(permissionList);
  }

  /**
   * Returns true if the internal permissions list is empty otherwise returns false
   *
   * @return
   */
  public boolean isEmpty() {
    return permissionList.isEmpty();
  }

  /**
   * String representation of this collection of permissions. Depends on the toString method of each
   * permission.
   *
   * @return String representation of this collection of permissions
   */
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Permission perm : permissionList) {
      sb.append('[');
      sb.append(perm.toString());
      sb.append("] ");
    }
    return sb.toString();
  }

  /** Clears out all of the permissions currently in this collection. */
  public void clear() {
    permissionList.clear();
  }

  /**
   * Adds all of the incoming permissions to this collection.
   *
   * @param permissions The permissions that should be added.
   */
  public void addAll(Collection<? extends Permission> permissions) {
    permissionList.addAll(permissions);
  }

  /**
   * Returns the action associated with this collection of permissions
   *
   * @return
   */
  public String getAction() {
    return action;
  }

  /**
   * Sets the action for this collection of permissions
   *
   * @param action
   */
  public void setAction(String action) {
    this.action = action;
  }
}
