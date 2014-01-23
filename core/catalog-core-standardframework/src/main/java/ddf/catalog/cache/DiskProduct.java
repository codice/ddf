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
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.cache.CacheException;
import ddf.catalog.resource.Resource;

public class DiskProduct {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DiskProduct.class);
    
    /** Directory for products cached to file system */
    private String productCacheDirectory;

    public DiskProduct(String productCacheDirectory) {
        this.productCacheDirectory = productCacheDirectory;
    }
    
    public String store(Resource resource) throws CacheException {
        LOGGER.debug("ENTERING: store() where resource name = " + resource.getName());
        
        String filepath = FilenameUtils.concat(productCacheDirectory, resource.getName());
        LOGGER.debug("Copying resource to filepath = " + filepath);
        try {
            // Copy product's InputStream to product cache file.
            // Product cache file will be created if it doesn't exist, or overwritten if
            // it already exists.
            // The product's InputStream will be closed when copy completes.
            FileUtils.copyInputStreamToFile(resource.getInputStream(), new File(filepath));
        } catch (IOException e) {
            throw new CacheException("Unable to store product file " + filepath);
        }
        
        LOGGER.debug("EXITING: store()");
        
        return filepath;
    }
    
}
