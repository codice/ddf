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
package org.codice.ddf.catalog.content.monitor;

import javax.annotation.Nullable;

/**
 * Persistent store used to serialize and load objects from a key.
 *
 * @implNote Implementations should, given the same key, write and return the same object.
 */
public interface ObjectPersistentStore {

  /**
   * Stores an object with a given key. Subsequent calls to {@link #store(String, Object)} with the
   * same key should overwrite the old Object.
   *
   * @param key
   * @param toStore
   */
  void store(String key, Object toStore);

  /**
   * Given a key, returns the object that was stored from a call to {@link #store(String, Object)}.
   * If there are no objects that exist under that key and object type, returns {@code null}
   *
   * @param key
   * @param objectClass class of the object that was stored.
   * @param <T>
   * @return The stored object, or {@code null}
   */
  @Nullable
  <T> T load(String key, Class<T> objectClass);
}
