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
package ddf.cache;

import java.util.List;
import java.util.Map;

/**
 * Cache Manager
 * 
 */
public interface CacheManager {

    public static final String CONFIG_CACHE_NAME = "cacheName";

    public static final String CONFIG_BACKUP_COUNT = "backupCount";

    public static final String CONFIG_MAX_CACHE_SIZE = "maxCacheSize";

    public static final String CONFIG_TIME_TO_LIVE = "ttl";

    public static final String CONFIG_EVICTION_POLICY = "evictionPolicy";

    public static final String EVICTION_POLICY_LRU = "LRU";

    public static final String EVICTION_POLICY_LFU = "LFU";
    
    public static final String CONFIG_MAP_STORE = "mapStore";
    
    /**
     * Creates a new cache
     *     
     * @param name
     * @return
     */
    public Cache getCache(String name);
    
    /**
     * Creates a new cache with specified properties
     * 
     * @param name
     * @param properties
     * @return
     */
    public Cache getCache(String name, Map<String, Object> properties);
    
    /**
     * Get the configuration of the cache, which includes its @MapConfig and
     * @MapStoreConfig (if present).
     * 
     * @param cacheName
     * @return
     */
    public Map<String, Object> getCacheConfiguration(String cacheName);
    
    /**
     * Removes specified cache from the system
     * 
     * @param cacheName
     */
    public void removeCache(String cacheName);

    /**
     * List all of the available caches
     * 
     * @return
     */
    public List<String> listCaches();

    /**
     * Shutdown the Cache Manager
     */
    public void shutdown();
}
