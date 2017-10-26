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
package org.codice.ddf.catalog.async.data.api.internal;

import javax.annotation.Nullable;

/**
 * The {@code ProcessResourceItem} represents the data that will be processed by the {@link
 * ProcessingFramework}. It maps a specific {@link ddf.catalog.data.Metacard} to an associated
 * {@link ProcessResource}.
 */
public interface ProcessResourceItem extends ProcessItem {

  /**
   * Gets the {@code ProcessResource} that corresponds to the associated {@link
   * ddf.catalog.data.Metacard} retrieved from {@link ProcessItem#getMetacard()}.
   *
   * @return {@code ProcessResource}
   */
  @Nullable
  ProcessResource getProcessResource();

  /**
   * Determines whether or not the metacard from {@link ProcessItem#getMetacard()} has been modified
   * by any of the {@link PostProcessPlugin}s during processing by the {@link ProcessingFramework}.
   * This is used to determine whether or not a (@link ddf.catalog.operation.UpdateRequest} for this
   * {@link ProcessItem#getMetacard()} needs to be made back to the {@link
   * ddf.catalog.CatalogFramework}.
   *
   * @return {@code true} if modified, {@code false} otherwise
   */
  boolean isMetacardModified();

  /** Mark the metacard as modified. */
  void markMetacardAsModified();
}
