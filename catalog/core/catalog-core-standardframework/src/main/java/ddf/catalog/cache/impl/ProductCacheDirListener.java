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

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MapEvent;
import com.hazelcast.query.PagingPredicate;
import ddf.catalog.resource.data.ReliableResource;
import ddf.catalog.resource.data.ReliableResourceComparator;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductCacheDirListener<K, V> implements EntryListener<K, V>, HazelcastInstanceAware {

  private static final String CACHE_DIR_SIZE = "cache.dir.size";

  private static final String PRODUCT_CACHE_NAME = "Product_Cache";

  private static final int DEFAULT_PAGE_SIZE = 10;

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductCacheDirListener.class);

  private IMap<String, ReliableResource> map;

  private IAtomicLong cacheDirSize;

  private long maxDirSizeBytes;

  private Set<String> manuallyEvictedEntries = Collections.synchronizedSet(new HashSet<String>());

  /**
   * Constructor for new Hazelcast listener
   *
   * @param maxDirSizeBytes: If 0, no size limit will be enforced.
   */
  public ProductCacheDirListener(final long maxDirSizeBytes) {
    synchronized (this) {
      this.maxDirSizeBytes = maxDirSizeBytes;
    }
  }

  @Override
  public void setHazelcastInstance(HazelcastInstance hc) {
    LOGGER.trace("Setting hazelcast instance");
    synchronized (this) {
      this.map = hc.getMap(PRODUCT_CACHE_NAME);
    }
    this.cacheDirSize = hc.getAtomicLong(CACHE_DIR_SIZE);
  }

  @Override
  public synchronized void entryAdded(EntryEvent<K, V> event) {
    V value = event.getValue();
    if (value.getClass().isAssignableFrom(ReliableResource.class)) {
      ReliableResource resource = (ReliableResource) value;
      LOGGER.debug("entry added event triggered: {}", resource.getKey());

      long currentCacheDirSize = cacheDirSize.addAndGet(resource.getSize());
      if (maxDirSizeBytes > 0 && maxDirSizeBytes < currentCacheDirSize) {
        PagingPredicate pp =
            new PagingPredicate(new ReliableResourceComparator(), DEFAULT_PAGE_SIZE);
        Collection<ReliableResource> lruResourceEntries = map.values(pp);

        Iterator<ReliableResource> itr = lruResourceEntries.iterator();
        while (maxDirSizeBytes < currentCacheDirSize) {
          if (itr.hasNext()) {
            ReliableResource rr = itr.next();
            deleteFromCache(map, rr);
            currentCacheDirSize -= rr.getSize();
          } else {
            pp.nextPage();
            lruResourceEntries = map.values(pp);
            itr = lruResourceEntries.iterator();
          }
        }
      }
    }
  }

  @Override
  public void entryRemoved(EntryEvent<K, V> event) {
    V value = event.getValue();
    if (value.getClass().isAssignableFrom(ReliableResource.class)) {
      ReliableResource resource = (ReliableResource) value;
      LOGGER.debug("entry removed event triggered: {}", resource.getKey());
      if (manuallyEvictedEntries.contains(resource.getKey())) {
        manuallyEvictedEntries.remove(resource.getKey());
      } else {
        cacheDirSize.addAndGet(-resource.getSize());
      }
    }
  }

  @Override
  public void entryUpdated(EntryEvent<K, V> event) {
    LOGGER.debug("entry updated event triggered");
  }

  @Override
  public void entryEvicted(EntryEvent<K, V> event) {
    V value = event.getValue();
    if (value.getClass().isAssignableFrom(ReliableResource.class)) {
      ReliableResource resource = (ReliableResource) value;
      LOGGER.debug("entry evicted event triggered: {}", resource.getKey());
      cacheDirSize.addAndGet(-resource.getSize());
    }
  }

  @Override
  public void mapCleared(MapEvent mapEvent) {
    // TODO: 2018-12-07 What needs to happen here?
  }

  @Override
  public void mapEvicted(MapEvent mapEvent) {
    // TODO: 2018-12-07 What needs to happen here?
  }

  private void deleteFromCache(IMap<String, ReliableResource> cacheMap, ReliableResource rr) {
    LOGGER.debug("entry being deleted: {}", rr.getKey());
    manuallyEvictedEntries.add(rr.getKey());

    // delete form cache
    cacheMap.delete(rr.getKey());

    // delete from file system cache
    File cachedFile = new File(rr.getFilePath());
    if (cachedFile.exists()) {
      boolean success = cachedFile.delete();
      if (!success) {
        LOGGER.info("Could not delete file {}", cachedFile.getAbsolutePath());
      }
    }
    cacheDirSize.addAndGet(-rr.getSize());
  }

  public synchronized long getMaxDirSizeBytes() {
    return maxDirSizeBytes;
  }

  public synchronized void setMaxDirSizeBytes(long maxDirSizeBytes) {
    this.maxDirSizeBytes = maxDirSizeBytes;
  }
}
