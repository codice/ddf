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

import ddf.catalog.data.Metacard;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 *
 * <p>The {@code ProcessUpdateItem} represents the data that will be processed by the {@link
 * PostProcessPlugin}s after a {@link ddf.catalog.data.Metacard} has been updated in the catalog.
 */
public interface ProcessUpdateItem extends ProcessResourceItem {

  /**
   * Gets the original {@link Metacard} before the update.
   *
   * @return the original {@link Metacard}
   */
  Metacard getOldMetacard();
}
