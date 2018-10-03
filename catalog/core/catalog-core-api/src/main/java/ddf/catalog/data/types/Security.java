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
package ddf.catalog.data.types;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface Security {

  /** {@link ddf.catalog.data.Attribute} name for storing groups to enforce access controls upon */
  String ACCESS_GROUPS = "security.access-groups";

  /**
   * {@link ddf.catalog.data.Attribute} name for adding more granularity for groups to discern who
   * can _only_ read
   */
  String ACCESS_GROUPS_READ = "security.access-groups-read";

  /**
   * {@link ddf.catalog.data.Attribute} name for storing the email addresses of users to enforce
   * access controls upon
   */
  String ACCESS_INDIVIDUALS = "security.access-individuals";

  /**
   * {@link ddf.catalog.data.Attribute} name for adding more granularity for individuals to discern
   * who can _only_ read
   */
  String ACCESS_INDIVIDUALS_READ = "security.access-individuals-read";

  /**
   * {@link ddf.catalog.data.Attribute} name for storing list of individuals who have the ability to
   * modify the list of permissions for a particular metacard
   */
  String ACCESS_ADMINISTRATORS = "security.access-administrators";
}
