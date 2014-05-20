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
package ddf.cache.impl;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.IMap;
import com.hazelcast.query.SqlPredicate;

import ddf.cache.Cache;
import ddf.cache.CacheException;

public class HazelcastCache implements Cache {
    
    private static Logger LOGGER = LoggerFactory.getLogger(HazelcastCache.class);
    
    private String name;
    private IMap<Object, Object> map;
    private Map<String, Object> properties;

    public HazelcastCache(String name, IMap<Object, Object> map) {
        this.name = name;
        this.map = map;
    }

    public HazelcastCache(String name, IMap<Object, Object> map, Map<String, Object> properties) {
        this(name, map);
        this.properties = properties;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public Map<String, Object> getConfiguration() {
        return properties;
    }

    @Override
    public void put(Object key, Object value) throws CacheException {
        if (key == null) {
            throw new CacheException("Cannot put object in cache without a non-null key");
        }
        map.put(key, value);
    }

    @Override
    public Object get(Object key) throws CacheException {
        if (key == null) {
            throw new CacheException("Cannot get an object from cache without a non-null key");
        }
        return map.get(key);
    }

    @Override
    public void remove(Object key) throws CacheException {
        if (key == null) {
            throw new CacheException("Cannot remove an object from cache without a non-null key");
        }
        map.remove(key);
    }

    @Override
    public Set<Object> getKeys() {
        return map.keySet();
    }

    @Override
    public Object query(String searchCriteria) throws CacheException {
        LOGGER.info("searchCriteria = [{}]", searchCriteria);
        if (StringUtils.isBlank(searchCriteria)) {
            throw new CacheException("Cannot get an object from cache without a non-null search criteria");
        }
        
        return map.values(new SqlPredicate(searchCriteria));
    }

}
