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
package org.codice.ddf.catalog.harvest;

/**
 * {@link Listener} implementations must be idempotent, i.e., receiving the same create, update or
 * delete event multiple times for the same {@link HarvestedResource} must produce the same result.
 *
 * <p>{@link Harvester}s may thread out calls to {@code Listener}s if needed, so {@code Listener}s
 * don't have to thread out calls.
 *
 * <p><b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface Listener {

  /**
   * Called when a {@link Harvester} receives a newly-created file on a remote source.
   *
   * @param resource the {@link HarvestedResource} created on the remote source
   */
  void onCreate(HarvestedResource resource);

  /**
   * Called when a {@link Harvester} receives an updated file on a remote source.
   *
   * @param resource the {@link HarvestedResource} updated on the remote source
   */
  void onUpdate(HarvestedResource resource);

  /**
   * Called when a {@link Harvester} receives a deleted file on a remote source. On a delete event,
   * the resource may not be available.
   *
   * @param uri of the {@link HarvestedResource} deleted on the remote source
   */
  void onDelete(String uri);
}
