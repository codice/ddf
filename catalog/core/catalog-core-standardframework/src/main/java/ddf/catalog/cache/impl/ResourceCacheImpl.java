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
package ddf.catalog.cache.impl;

import ddf.catalog.cache.ResourceCacheInterface;
import ddf.catalog.data.Metacard;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.data.ReliableResource;

public class ResourceCacheImpl implements ResourceCacheInterface {

  public ResourceCacheImpl() {}

  public void teardownCache() {
    // no-op
  }

  public String getProductCacheDirectory() {
    return null;
  }

  public void setProductCacheDirectory(String productCacheDirectory) {
    // no-op
  }

  /**
   * Returns true if resource with specified cache key is already in the process of being cached.
   * This check helps clients prevent attempting to cache the same resource multiple times.
   *
   * @param key
   * @return
   */
  @Override
  public boolean isPending(String key) {
    return false;
  }

  /**
   * Called by ReliableResourceDownloadManager when resource has completed being cached to disk and
   * is ready to be added to the cache map.
   *
   * @param reliableResource the resource to add to the cache map
   */
  @Override
  public void put(ReliableResource reliableResource) {
    // no-op
  }

  @Override
  public void removePendingCacheEntry(String cacheKey) {
    // no-op
  }

  @Override
  public void addPendingCacheEntry(ReliableResource reliableResource) {
    // no-op
  }

  /**
   * @param key
   * @return Resource, {@code null} if not found.
   */
  @Override
  public Resource getValid(String key, Metacard latestMetacard) {
    if (key == null) {
      throw new IllegalArgumentException("Must specify non-null key");
    }
    if (latestMetacard == null) {
      throw new IllegalArgumentException("Must specify non-null metacard");
    }
    return null;
  }

  /**
   * States whether an item is in the cache or not.
   *
   * @param key
   * @return {@code true} if items exists in cache.
   */
  @Override
  public boolean containsValid(String key, Metacard latestMetacard) {
    return false;
  }

  /**
   * Compares the {@link Metacard} in a {@link ReliableResource} pulled from cache with a Metacard
   * obtained directly from the Catalog to ensure they are the same. Typically used to determine if
   * the cache entry is out-of-date based on the Catalog having an updated Metacard.
   *
   * @param cachedResource
   * @param latestMetacard
   * @return true if the cached ReliableResource still matches the most recent Metacard from the
   *     Catalog, false otherwise
   */
  protected boolean validateCacheEntry(ReliableResource cachedResource, Metacard latestMetacard) {
    if (cachedResource == null || latestMetacard == null) {
      throw new IllegalArgumentException(
          "Neither the cachedResource nor the metacard retrieved from the catalog can be null.");
    }
    return false;
  }
}
