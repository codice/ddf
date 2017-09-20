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
package org.codice.ddf.platform.services.common;

/**
 * Describable is used to capture a basic description of a service. This provides valuable runtime
 * information to the user regarding the application bundles and OSGi.
 *
 * <p>It is expected that the children are services that perform specific tasks.
 */
public interface Describable {
  /**
   * Returns the version.
   *
   * @return the version of the item being described (example: 1.0)
   */
  String getVersion();

  /**
   * Returns the name, aka ID, of the describable item. The name should be unique for each instance
   * within the scope of a service or a component. For example, this is unique for any Migratable in
   * a set of Migratables. It is not necessarily unique between Migratables and Metacards.
   *
   * <p>Format should be [<b>product</b>].[<b>component</b>] such as ddf.metacards, or ddf.platform;
   * while the [<b>component</b>] within a [<b>product</b>] may simply be a module or bundle name,
   * the [<b>product</b>] itself should be the unique name of the plug-in or integration that
   * belongs to the organization listed in {@link Describable#getOrganization()}. Note that 'ddf' as
   * a [<b>product</b>] is reserved for core features only.
   *
   * @return ID of the item
   */
  String getId();

  /**
   * Returns the title of the describable item. It is generally more verbose than the name (aka ID).
   *
   * @return title of the item (example: File System Provider)
   */
  String getTitle();

  /**
   * Returns a description of the describable item.
   *
   * @return description of the item (example: Provider that returns back static results)
   */
  String getDescription();

  /**
   * Returns the organization associated with the describable item.
   *
   * @return organizational name or acronym (example: USAF)
   */
  String getOrganization();
}
