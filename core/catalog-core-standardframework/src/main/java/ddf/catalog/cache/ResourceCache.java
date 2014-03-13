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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.cache.Cache;
import ddf.cache.CacheException;
import ddf.cache.CacheManager;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resourceretriever.ResourceRetriever;

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

    public ResourceResponse put(Metacard metacard, ResourceResponse resourceResponse, ResourceRetriever retriever)
        throws CacheException {

        LOGGER.debug("ENTERING: put()");

        // run checks
        if (metacard == null) {
            throw new CacheException("Must specify non-null metacard");
        } else if (resourceResponse == null) {
            throw new CacheException("Must specify non-null resourceResponse");
        } else if (retriever == null) {
            throw new CacheException("Must specify non-null ResourceRetriever");
        } 
        else if (StringUtils.isBlank(metacard.getId())) {
            throw new CacheException("Metacard must have unique id.");
        }

        ResourceRequest resourceRequest = resourceResponse.getRequest();

        LOGGER.debug("metacard ID = {}", metacard.getId());
        
        CacheKey keyMaker = new CacheKey(metacard, resourceResponse.getRequest());

        String key = keyMaker.generateKey();

        if (pendingCache.contains(key)) {
            LOGGER.info(
                    "Cache file with key = {} is already pending caching - return resourceResponse that was passed in",
                    key);
            return resourceResponse;
        } else {
            LOGGER.info("Caching file with key {} ", key);
        }

        CachedResource cachedResource = new CachedResource(productCacheDirectory);
        
        // store() will return a new Resource with its input stream now set to
        // the PipedInputStream that caching thread will write to simultaneously
        // as it writes the product input stream to cache (disk).
        Resource newResource = cachedResource.store(key, resourceResponse, this, retriever);
        
        //Moved into CachedResource class (that's why "ResourceCache" argument passed into store() above)
        //until figure out better way.
        //cache.put(cachedResource.getKey(), cachedResource);
        
        // Used to prevent caching same product twice, i.e., if same product requested while it is
        // in process of being cached
        pendingCache.add(key);

        // Create new ResourceResponse with the PipedInputStream that the client will
        // read simultaneously as the file is cached to disk
        ResourceResponse newResourceResponse = new ResourceResponseImpl(resourceRequest,
                resourceResponse.getProperties(), newResource);

        LOGGER.debug("EXITING: put() for metacard ID = {}", metacard.getId());

        return newResourceResponse;
    }
    
    public void put(CachedResource cachedResource) throws CacheException {
        LOGGER.info("ENTERING: put(CachedResource)");
        cache.put(cachedResource.getKey(), cachedResource);
        if (!pendingCache.remove(cachedResource.getKey())) {
            LOGGER.info("Did not find pending cache entry with key = {}", cachedResource.getKey());
        } else {
            LOGGER.info("Removed pending cache entry with key = {}", cachedResource.getKey());
        }
        
        LOGGER.info("EXITING: put(CachedResource)");
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
        
        CachedResource cachedResource = null;
        try {
            Thread.currentThread().setContextClassLoader(
                  getClass().getClassLoader());
            cachedResource = (CachedResource) cache.get(key);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }        
        
        // Check that CachedResource actually maps to a file (product) in the
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
        CachedResource cachedResource = null;
        try {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            
            try {
                Thread.currentThread().setContextClassLoader(
                      getClass().getClassLoader());
                cachedResource = (CachedResource) cache.get(key);
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
