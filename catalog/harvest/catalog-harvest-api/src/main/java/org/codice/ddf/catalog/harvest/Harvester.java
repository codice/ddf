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
 * A {@code Harvester} is responsible for fetching remote resources and notifying any registered
 * {@link Listener}s of create, update, and delete events.
 *
 * <p>In the event that a {@code Harvester} goes down, it should make a best effort to remember the
 * last state of the external server it was harvesting from. Upon a {@code Harvester} coming back
 * up, it should notify registered {@link Listener}s of new events.
 *
 * <p><b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface Harvester {

  /**
   * Registers a new {@link Listener} to receive events from the remote source being harvested from.
   * Newly-registered {@link Listener}s will only receive new events from this {@code Harvester}. If
   * a {@link Listener} is already registered, the call to this method will be a no-op.
   *
   * @param listener the {@link Listener} to register
   * @throws IllegalArgumentException if the {@link Listener} cannot be registered
   */
  void registerListener(Listener listener);

  /**
   * Unregisters a {@link Listener}. Unregistering a {@link Listener} that isn't registered will be
   * silently ignored.
   *
   * @param listener the {@link Listener} to unregister
   */
  void unregisterListener(Listener listener);

  /** A unique, human-readable identifier. */
  String getId();
}
