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

import java.util.Map;
import java.util.Set;

public interface Cache {

    /**
     * Get the name of the cache
     * 
     * @return
     */
    public String getName();
    
    /**
     * Get the configuration properties of the cache
     * 
     * @return
     */
    public Map<String, Object> getConfiguration();
    
    /**
     * Put an object in the cache using the specified key
     * 
     * @param key
     * @param value
     * @throws CacheException
     */
    public void put(Object key, Object value) throws CacheException;
    
    /**
     * Get an object from the cache that corresponds to the specified key
     * 
     * @param key
     * @return
     * @throws CacheException
     */
    public Object get(Object key) throws CacheException;
    
    /**
     * Remove object from the cache that corresponds to the specified key
     * 
     * @param key
     * @throws CacheException
     */
    public void remove(Object key) throws CacheException;
    
    /**
     * Set of all keys in the Cache
     * 
     * @return
     */
    public Set<Object> getKeys();
}
