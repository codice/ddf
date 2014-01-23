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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.cache.Cache;
import ddf.cache.CacheException;
import ddf.cache.CacheManager;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.impl.ResourceImpl;

public class ProductCache {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCache.class);

    private static final String PRODUCT_CACHE_NAME = "Product_Cache";
    
    // Limit in bytes for size of a product that can be stored in 
    // cache's memory. Larger products will be stored on disk with uri
    // reference to them stored in cache.
    private static final int PRODUCT_CACHE_MEMORY_SIZE_LIMIT = 10000000;  // 10 MB
    
    public static final String DEFAULT_PRODUCT_CACHE_DIRECTORY = "product-cache";
    
    private CacheManager cacheManager;
    
    private Cache cache;
    
    /** Directory for products cached to file system */
    private String productCacheDirectory;

    public ProductCache() {
        
    }
    
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
                LOGGER.debug("Setting product cache directory to: " + path);
                newProductCacheDirectoryDir = path;
            }
        }

        // if invalid productCacheDirectory was provided or productCacheDirectory is
        // an empty string, default to the DEFAULT_PRODUCT_CACHE_DIRECTORY in <karaf.home>
        if (newProductCacheDirectoryDir.isEmpty()) {
            try {
                final File karafHomeDir = new File(System.getProperty("karaf.home"));

                if (karafHomeDir.isDirectory()) {
                    final File fspDir = new File(karafHomeDir + File.separator
                            + DEFAULT_PRODUCT_CACHE_DIRECTORY);

                    // if directory does not exist, try to create it
                    if (fspDir.isDirectory() || fspDir.mkdirs()) {
                        LOGGER.debug("Setting product cache directory to: "
                                + fspDir.getAbsolutePath());
                        newProductCacheDirectoryDir = fspDir.getAbsolutePath();
                    } else {
                        LOGGER.warn("Unable to create directory: "
                                + fspDir.getAbsolutePath()
                                + ". Please check that DDF has permissions to create this folder.  Using default folder.");
                    }
                } else {
                    LOGGER.warn("Karaf home folder defined by system property karaf.home is not a directory.  Using default folder.");
                }
            } catch (NullPointerException npe) {
                LOGGER.warn("Unable to create FileSystemProvider folder - karaf.home system property not defined. Using default folder.");
            }
        }

        this.productCacheDirectory = newProductCacheDirectoryDir;

        LOGGER.debug("Set product cache directory to: {}", this.productCacheDirectory);
    }

    public ResourceResponse put(Metacard metacard, ResourceResponse resourceResponse)
        throws CacheException {
        LOGGER.debug("ENTERING: put()");
        if (metacard == null) {
            throw new CacheException("Must specify non-null metacard");
        } else if (resourceResponse == null) {
            throw new CacheException("Must specify non-null resourceResponse");
        }
        LOGGER.debug("metacard ID = {}", metacard.getId());        
        
        // The key for any values put in the product cache will be the 
        // name of the source the product is from and the product's metacard ID
        String key = metacard.getSourceId() + "-" + metacard.getId();
        
        Resource resource = resourceResponse.getResource();
        DiskProduct diskProduct = new DiskProduct(productCacheDirectory);
        String filepath = diskProduct.store(resource);
        cache.put(key, filepath);
        cache.put(key + "-name", resource.getName());
        cache.put(key + "-mimetypevalue", resource.getMimeTypeValue());
        
        // Since the cache.put closed the stream must create a new InputStream
        // and add to a new ResourceResponse to be returned
        ResourceResponse newResourceResponse = null;
        try {
            Resource newResource = new ResourceImpl(new BufferedInputStream(FileUtils.openInputStream(new File(filepath))),
                    resource.getMimeTypeValue(), resource.getName());
            newResourceResponse = new ResourceResponseImpl(newResource);
        } catch (FileNotFoundException e) {
            throw new CacheException("Unable to create stream for " + filepath);
        }  catch (IOException e) {
            throw new CacheException("Unable to create stream for " + filepath);
        }        
        
        LOGGER.debug("EXITING: put() for metacard ID = {}", metacard.getId());
        
        return newResourceResponse;
    }
    
    public ResourceResponse get(Metacard metacard) throws CacheException {
        LOGGER.debug("ENTERING: get()");
        if (metacard == null) {
            throw new CacheException("Must specify non-null metacard");
        }
        LOGGER.debug("metacard ID = {}", metacard.getId());
        
        ResourceResponse resourceResponse = null;
        String key = metacard.getSourceId() + "-" + metacard.getId();
        String filepath = (String) cache.get(key);
        if (filepath != null) {
            try {
                InputStream is = new BufferedInputStream(FileUtils.openInputStream(new File(filepath)));
                String mimeTypeValue = (String) cache.get(key + "-mimetypevalue");
                String name = (String) cache.get(key + "-name");
                Resource resource = new ResourceImpl(is, mimeTypeValue, name);
                resourceResponse = new ResourceResponseImpl(resource);
            } catch (FileNotFoundException e) {
                throw new CacheException("Unable to read file " + filepath + " from product cache");
            } catch (IOException e) {
                throw new CacheException("Unable to read file " + filepath + " from product cache");
            }
        } else {
            LOGGER.debug("No product found in cache for key = " + key);
        }
        
        LOGGER.debug("EXITING: get() for metacard ID = {}", metacard.getId());
        
        return resourceResponse;
    }
}
