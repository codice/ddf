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
import org.apache.shiro.authz.Permission;

public interface CollectionPermission extends Permission {

  String CREATE_ACTION = "create";

  String READ_ACTION = "read";

  String UPDATE_ACTION = "update";

  String DELETE_ACTION = "delete";

  String UNKNOWN_ACTION = "unknown-action";

  String PERMISSION_START_MSG = "Permission [";

  String PERMISSION_IMPLIES_MSG = "] implies permission [";

  String PERMISSION_NOT_IMPLIES_MSG = "] does not imply permission [";

  String PERMISSION_END_MSG = "].";

  List<Permission> getPermissionList();

  boolean isEmpty();

  void clear();

  void addAll(Collection<? extends Permission> permissions);

  String getAction();

  void setAction(String action);
}
