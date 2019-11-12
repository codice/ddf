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

import static ddf.catalog.cache.impl.CachedResourceMetacardComparator.isSame;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.map.merge.PassThroughMergePolicy;
import ddf.catalog.cache.ResourceCacheInterface;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.data.ReliableResource;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.configuration.PropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceCacheImpl implements ResourceCacheInterface {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCacheImpl.class);

  private static final String PRODUCT_CACHE_NAME = "Product_Cache";

  private static final String HAZELCAST_STORAGE_PROPERTY = "storage";

  private static final int HAZELCAST_PORT = 5701;

  private static final boolean HAZELCAST_PORT_AUTO_INCREMENT = true;

  private static final long BYTES_IN_MEGABYTES = FileUtils.ONE_MB;

  private static final long DEFAULT_MAX_CACHE_DIR_SIZE_BYTES = 10737418240L; // 10 GB

  private List<String> pendingCache = new ArrayList<>();

  /** Directory for products cached to file system */
  private String productCacheDirectory;

  private HazelcastInstance instance;

  private IMap<Object, Object> cache;

  private ProductCacheDirListener<Object, Object> cacheListener =
      new ProductCacheDirListener<>(DEFAULT_MAX_CACHE_DIR_SIZE_BYTES);

  public ResourceCacheImpl(String productCacheDirectory) {
    this.productCacheDirectory = productCacheDirectory;
    initCache();
  }

  /** Called after all parameters are set */
  public void setCache(HazelcastInstance instance) {
    LOGGER.trace("ENTERING: setCache()");
    this.instance = instance;
    if (this.instance == null) {
      Config cfg = initHazelcastConfig(productCacheDirectory);
      cfg.setClassLoader(getClass().getClassLoader());
      this.instance = Hazelcast.newHazelcastInstance(cfg);
    }

    cache = this.instance.getMap(PRODUCT_CACHE_NAME);
    cacheListener.setHazelcastInstance(this.instance);
    cache.addEntryListener(cacheListener, true);
  }

  public void initCache() {
    // if a hazelcast instance is already running, shut it down first
    if (instance != null) {
      teardownCache();
    }
    setCache(null);
  }

  public void teardownCache() {
    instance.shutdown();
  }

  public long getCacheDirMaxSizeMegabytes() {
    LOGGER.debug("Getting max size for cache directory.");
    return cacheListener.getMaxDirSizeBytes() / BYTES_IN_MEGABYTES;
  }

  public void setCacheDirMaxSizeMegabytes(long cacheDirMaxSizeMegabytes) {
    LOGGER.debug("Setting max size for cache directory: {}", cacheDirMaxSizeMegabytes);
    cacheListener.setMaxDirSizeBytes(cacheDirMaxSizeMegabytes * BYTES_IN_MEGABYTES);
  }

  public String getProductCacheDirectory() {
    return productCacheDirectory;
  }

  public void setProductCacheDirectory(String productCacheDirectory) {
    this.productCacheDirectory = new PropertyResolver(productCacheDirectory).getResolvedString();
    initCache();
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
    return pendingCache.contains(key);
  }

  /**
   * Called by ReliableResourceDownloadManager when resource has completed being cached to disk and
   * is ready to be added to the cache map.
   *
   * @param reliableResource the resource to add to the cache map
   */
  @Override
  public void put(ReliableResource reliableResource) {
    LOGGER.trace("ENTERING: put(ReliableResource)");
    reliableResource.setLastTouchedMillis(System.currentTimeMillis());
    cache.put(reliableResource.getKey(), reliableResource);
    removePendingCacheEntry(reliableResource.getKey());

    LOGGER.trace("EXITING: put(ReliableResource)");
  }

  @Override
  public void removePendingCacheEntry(String cacheKey) {
    if (!pendingCache.remove(cacheKey)) {
      LOGGER.debug("Did not find pending cache entry with key = {}", cacheKey);
    } else {
      LOGGER.debug("Removed pending cache entry with key = {}", cacheKey);
    }
  }

  @Override
  public void addPendingCacheEntry(ReliableResource reliableResource) {
    String cacheKey = reliableResource.getKey();
    if (isPending(cacheKey)) {
      LOGGER.debug("Cache entry with key = {} is already pending", cacheKey);
    } else if (containsValid(cacheKey, reliableResource.getMetacard())) {
      LOGGER.debug("Cache entry with key = {} is already in cache", cacheKey);
    } else {
      pendingCache.add(cacheKey);
    }
  }

  /**
   * @param key
   * @return Resource, {@code null} if not found.
   */
  @Override
  public Resource getValid(String key, Metacard latestMetacard) {
    LOGGER.trace("ENTERING: get()");
    if (key == null) {
      throw new IllegalArgumentException("Must specify non-null key");
    }
    if (latestMetacard == null) {
      throw new IllegalArgumentException("Must specify non-null metacard");
    }
    LOGGER.debug("key {}", key);

    ReliableResource cachedResource = (ReliableResource) cache.get(key);

    // Check that ReliableResource actually maps to a file (product) in the
    // product cache directory. This check handles the case if the product
    // cache directory has had files deleted from it.
    if (cachedResource != null) {
      if (!validateCacheEntry(cachedResource, latestMetacard)) {
        LOGGER.debug(
            "Entry found in cache was out-of-date or otherwise invalid.  Will need to be re-cached.  Entry key: {}",
            key);
        return null;
      }

      if (cachedResource.hasProduct()) {
        LOGGER.trace("EXITING: get() for key {}", key);
        return cachedResource;
      } else {
        cache.remove(key);
        LOGGER.debug(
            "Entry found in the cache, but no product found in cache directory for key = {}", key);
        return null;
      }
    } else {
      LOGGER.debug("No product found in cache for key = {}", key);
      return null;
    }
  }

  /**
   * States whether an item is in the cache or not.
   *
   * @param key
   * @return {@code true} if items exists in cache.
   */
  @Override
  public boolean containsValid(String key, Metacard latestMetacard) {
    if (key == null) {
      return false;
    }
    ReliableResource cachedResource = (ReliableResource) cache.get(key);
    return (cachedResource != null) && (validateCacheEntry(cachedResource, latestMetacard));
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
    LOGGER.trace("ENTERING: validateCacheEntry");
    if (cachedResource == null || latestMetacard == null) {
      throw new IllegalArgumentException(
          "Neither the cachedResource nor the metacard retrieved from the catalog can be null.");
    }

    Metacard cachedMetacard = cachedResource.getMetacard();
    MetacardImpl latestMetacardImpl = new MetacardImpl(latestMetacard);

    if (isSame(cachedMetacard, latestMetacardImpl)) {
      LOGGER.debug("Metacard has not changed");
      LOGGER.trace("EXITING: validateCacheEntry");
      return true;
    }

    LOGGER.debug("Metacard has changed");

    File cachedFile = new File(cachedResource.getFilePath());

    if (!FileUtils.deleteQuietly(cachedFile)) {
      LOGGER.debug(
          "File was not removed from cache directory.  File Path: {}",
          cachedResource.getFilePath());
    }

    cache.remove(cachedResource.getKey());
    LOGGER.trace("EXITING: validateCacheEntry");
    return false;
  }

  private Config initHazelcastConfig(String productCacheDirectory) {
    Config cfg = new Config();

    JoinConfig joinConfig = cfg.getNetworkConfig().getJoin();
    joinConfig.getMulticastConfig().setEnabled(false);

    MapConfig defaultMapConfig = cfg.getMapConfig("default");
    defaultMapConfig.setBackupCount(2);
    defaultMapConfig.setEvictionPolicy(EvictionPolicy.LRU);
    defaultMapConfig.setMergePolicy(PassThroughMergePolicy.class.getCanonicalName());

    MapConfig productCacheMapConfig = cfg.getMapConfig(PRODUCT_CACHE_NAME);
    productCacheMapConfig.setBackupCount(0);

    MapStoreConfig productCacheMapStoreConfig = new MapStoreConfig();

    productCacheMapStoreConfig.setEnabled(true);
    productCacheMapStoreConfig.setFactoryClassName(
        FileSystemMapStoreFactory.class.getCanonicalName());
    productCacheMapStoreConfig.setWriteDelaySeconds(0);
    productCacheMapStoreConfig.setProperty(HAZELCAST_STORAGE_PROPERTY, productCacheDirectory);

    productCacheMapConfig.setMapStoreConfig(productCacheMapStoreConfig);

    cfg.getNetworkConfig()
        .setPort(HAZELCAST_PORT)
        .setPortAutoIncrement(HAZELCAST_PORT_AUTO_INCREMENT)
        .setOutboundPorts(Arrays.asList(0))
        .setJoin(joinConfig)
        .setSSLConfig(new SSLConfig());
    cfg.addMapConfig(defaultMapConfig);
    cfg.addMapConfig(productCacheMapConfig);

    return cfg;
  }
}
