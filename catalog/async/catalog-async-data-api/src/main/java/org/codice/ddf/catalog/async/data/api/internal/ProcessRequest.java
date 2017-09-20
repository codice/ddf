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

import ddf.catalog.operation.Operation;
import java.util.List;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */

/**
 * The {@code ProcessRequest} is a request that is capable of being processed by {@link
 * PostProcessPlugin}s in a {@link ProcessingFramework}.
 *
 * @param <T> A {@link ProcessItem} that contains the data to be processed.
 */
public interface ProcessRequest<T extends ProcessItem> extends Operation {

  /**
   * Gets the {@link ProcessItem}s that contain the data required for processing by the {@link
   * ProcessingFramework}. The available {@link ProcessItem}s are:
   *
   * <ul>
   *   <li>{@link ProcessCreateItem} which contains a {@link ddf.catalog.data.Metacard} and the
   *       {@link ProcessResource}
   *   <li>{@link ProcessUpdateItem} which contains an updated {@link ddf.catalog.data.Metacard},
   *       the original {@link ddf.catalog.data.Metacard}, and the {@link ProcessResource}
   *   <li>{@link ProcessDeleteItem} which contains the {@link ddf.catalog.data.Metacard} being
   *       deleted
   * </ul>
   *
   * @return a {@link List} of {@link ProcessItem}s
   */
  List<T> getProcessItems();
}
