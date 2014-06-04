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

import ddf.cache.CacheException;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.data.ReliableResource;

/**
 * Interface defining a cache of resources or references to resources.
 * 
 * @author ddf.isgs@lmco.com
 *
 */
public interface ResourceCacheInterface {
    
    /**
     * Adds a resource to the cache.
     * 
     * @param reliableResource
     * @throws CacheException
     */
    void put(ReliableResource reliableResource) throws CacheException;
    
    /**
     * Gets resource from the cache.
     * 
     * @param key
     * @return Resource obtained from cache
     * @throws CacheException
     */
    Resource get(String key) throws CacheException;

    /**
     * Queries cache to determine if it contains a resource with the provided key.
     * 
     * @param key
     * @return
     */
    boolean contains(String key);

    /**
     * Returns true if resource with specified cache key is already in the process of
     * being cached. This check helps clients prevent attempting to cache the same resource
     * multiple times.
     * 
     * @param key 
     * @return 
     */
    boolean isPending(String key);

    /**
     * Removes resource from list of pending resources being added to cache.  
     * This can help when multiple clients may be interacting with the same cache in order to 
     * prevent multiple copies of the same resource being cached.
     * 
     * @param cacheKey
     */
    void removePendingCacheEntry(String cacheKey);
    
    /**
     * Adds resource to list of resources in process of being cached.
     * This can help when multiple clients may be interacting with the same cache in order to 
     * prevent multiple copies of the same resource being cached.
     * 
     * @param cacheKey
     */
    void addPendingCacheEntry(String cacheKey);
    
}
