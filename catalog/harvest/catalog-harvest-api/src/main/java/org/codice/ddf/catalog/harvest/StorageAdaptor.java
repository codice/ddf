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
 * A {@code StorageAdaptor} is responsible for persisting {@link HarvestedResource}s to a data
 * store.
 *
 * <p><b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface StorageAdaptor {

  /**
   * Stores the {@link HarvestedResource} in this {@code StorageAdaptor}'s data store.
   *
   * @param resource the {@link HarvestedResource} to store
   * @return a unique identifier for the created data
   * @throws HarvestException if the resource could not be created
   */
  String create(HarvestedResource resource) throws HarvestException;

  /**
   * Updates the {@link HarvestedResource} in this {@code StorageAdaptor}'s data store.
   *
   * @param resource the {@link HarvestedResource} to update the existing data in the store
   * @param id the unique identifier for the stored data to update
   * @throws HarvestException if the resource could not be updated
   */
  void update(HarvestedResource resource, String id) throws HarvestException;

  /**
   * Deletes data from this {@code StorageAdaptor}'s data store.
   *
   * @param id the unique identifier for the stored data to delete
   * @throws HarvestException if the resource could not be deleted
   */
  void delete(String id) throws HarvestException;
}
