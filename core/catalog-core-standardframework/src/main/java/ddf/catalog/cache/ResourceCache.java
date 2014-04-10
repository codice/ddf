/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.cache;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.cache.Cache;
import ddf.cache.CacheException;
import ddf.cache.CacheManager;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.download.ReliableResource;

public class ResourceCache {

    private static final String KARAF_HOME = "karaf.home";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCache.class);

    private static final String PRODUCT_CACHE_NAME = "Product_Cache";

    /**
     * Default location for product-cache directory, <INSTALL_DIR>/data/product-cache
     */
    public static final String DEFAULT_PRODUCT_CACHE_DIRECTORY =
            "data" + File.separator + "product-cache";

    private CacheManager cacheManager;

    private Cache cache;

    private List<String> pendingCache = new ArrayList<String>();

    /** Directory for products cached to file system */
    private String productCacheDirectory;


    public void setCacheManager(CacheManager cacheManager) {
        LOGGER.debug("Setting cacheManager");
        this.cacheManager = cacheManager;
        this.cache = this.cacheManager.getCache(PRODUCT_CACHE_NAME);
    }

    public void setProductCacheDirectory(final String productCacheDirectory) {
        String newProductCacheDirectoryDir = "";

        if (!productCacheDirectory.isEmpty()) {
            String path = FilenameUtils.normalize(productCacheDirectory);
            File directory = new File(path);

            // Create the directory if it doesn't exist
            if ((!directory.exists() && directory.mkdirs())
                    || (directory.isDirectory() && directory.canRead())) {
                LOGGER.debug("Setting product cache directory to: {}", path);
                newProductCacheDirectoryDir = path;
            }
        }

        // if productCacheDirectory is invalid or productCacheDirectory is
        // an empty string, default to the DEFAULT_PRODUCT_CACHE_DIRECTORY in <karaf.home>
        if (newProductCacheDirectoryDir.isEmpty()) {
            try {
                final File karafHomeDir = new File(System.getProperty(KARAF_HOME));

                if (karafHomeDir.isDirectory()) {
                    final File fspDir = new File(karafHomeDir + File.separator
                            + DEFAULT_PRODUCT_CACHE_DIRECTORY);

                    // if directory does not exist, try to create it
                    if (fspDir.isDirectory() || fspDir.mkdirs()) {
                        LOGGER.debug("Setting product cache directory to: {}",
                                fspDir.getAbsolutePath());
                        newProductCacheDirectoryDir = fspDir.getAbsolutePath();
                    } else {
                        LOGGER.warn(
                                "Unable to create directory: {}. Please check for proper permissions to create this folder. Instead using default folder.",
                                fspDir.getAbsolutePath());
                    }
                } else {
                    LOGGER.warn(
                            "Karaf home folder defined by system property {} is not a directory.  Using default folder.",
                            KARAF_HOME);
                }
            } catch (NullPointerException npe) {
                LOGGER.warn(
                        "Unable to create FileSystemProvider folder - {} system property not defined. Using default folder.",
                        KARAF_HOME);
            }
        }

        this.productCacheDirectory = newProductCacheDirectoryDir;

        LOGGER.debug("Set product cache directory to: {}", this.productCacheDirectory);
    }

    public String getProductCacheDirectory() {
        return productCacheDirectory;
    }
    
    /**
     * Returns true if resource with specified cache key is already in the process of
     * being cached. This check helps clients prevent attempting to cache the same resource
     * multiple times.
     * 
     * @param key
     * @return
     */
    public boolean isPending(String key) {
        return pendingCache.contains(key);
    }

    /**
     * Called by CachedResource when resource has completed being cached to disk and is ready to
     * be added to the cache map.
     *
     * @param cachedResource
     * @throws CacheException
     */
    public void put(ReliableResource reliableResource) throws CacheException {
        LOGGER.trace("ENTERING: put(ReliableResource)");
        cache.put(reliableResource.getKey(), reliableResource);
        removePendingCacheEntry(reliableResource.getKey());

        LOGGER.trace("EXITING: put(ReliableResource)");
    }

    public void removePendingCacheEntry(String cacheKey) {
        if (!pendingCache.remove(cacheKey)) {
            LOGGER.debug("Did not find pending cache entry with key = {}", cacheKey);
        } else {
            LOGGER.debug("Removed pending cache entry with key = {}", cacheKey);
        }
    }

    public void addPendingCacheEntry(String cacheKey) {
        if (isPending(cacheKey)) {
            LOGGER.debug("Cache entry with key = {} is already pending", cacheKey);
        } else if (contains(cacheKey)) {
            LOGGER.debug("Cache entry with key = {} is already in cache", cacheKey);
        } else {
            pendingCache.add(cacheKey);
        }
    }

    /**
     *
     * @param key
     * @return Resource, {@code null} if not found.
     * @throws CacheException
     *             if no Resource found
     */
    public Resource get(String key) throws CacheException {
        LOGGER.debug("ENTERING: get()");
        if (key == null) {
            throw new CacheException("Must specify non-null key");
        }
        LOGGER.debug("key {}", key);

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        ReliableResource cachedResource = null;
        try {
            Thread.currentThread().setContextClassLoader(
                  getClass().getClassLoader());
            cachedResource = (ReliableResource) cache.get(key);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        // Check that ReliableResource actually maps to a file (product) in the
        // product cache directory. This check handles the case if the product
        // cache directory has had files deleted from it.
        if (cachedResource != null) {
            if (cachedResource.hasProduct()) {
                LOGGER.debug("EXITING: get() for key {}", key);
                return cachedResource;
            } else {
                LOGGER.debug("Entry found in the cache, but no product found in cache directory for key = {}", key);
                tccl = Thread.currentThread().getContextClassLoader();

                try {
                    Thread.currentThread().setContextClassLoader(
                          getClass().getClassLoader());
                    cache.remove(key);
                } finally {
                    Thread.currentThread().setContextClassLoader(tccl);
                }
                throw new CacheException(
                        "Entry found in the cache, but no product found in cache directory for key = "
                                + key);
            }
        } else {
            LOGGER.debug("No product found in cache for key = {}", key);
            throw new CacheException("No product found in cache for key = " + key);
        }

    }

    /**
     * States whether an item is in the cache or not.
     *
     * @param key
     * @return {@code true} if items exists in cache.
     */
    public boolean contains(String key) {
        if (key == null) {
            return false;
        }
        ReliableResource cachedResource = null;
        try {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(
                      getClass().getClassLoader());
                cachedResource = (ReliableResource) cache.get(key);
            } finally {
                Thread.currentThread().setContextClassLoader(tccl);
            }
        } catch (CacheException e) {
            LOGGER.debug("Could not find key {} in cache", key, e);
            return false;
        }

        return cachedResource != null;
    }
}
