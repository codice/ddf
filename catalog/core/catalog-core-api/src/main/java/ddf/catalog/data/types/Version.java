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
public interface Version {

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the action associated with a history
   * {@link Metacard}. <br>
   */
  String ACTION = "version.action";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the editor of a history {@link Metacard}.
   * <br>
   */
  String EDITED_BY = "version.edited-by";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the version id for a history {@link
   * Metacard}. <br>
   */
  String ID = "version.id";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the version tags for a history {@link
   * Metacard}. <br>
   */
  String TAGS = "version.tags";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the version types for a history {@link
   * Metacard}. <br>
   */
  String TYPE = "version.types";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the version types in binary for a history
   * {@link Metacard}. <br>
   */
  String TYPE_BINARY = "version.type-binary";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the versioned date of a history {@link
   * Metacard}. <br>
   */
  String VERSIONED_BY = "version.versioned-by";
}
