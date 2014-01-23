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

import com.hazelcast.core.IMap;

import ddf.cache.Cache;
import ddf.cache.CacheException;

public class CacheImpl implements Cache {
    
    private String name;
    private IMap<Object, Object> map;
    private Map<String, Object> properties;

    public CacheImpl(String name, IMap<Object, Object> map) {
        this.name = name;
        this.map = map;
    }

    public CacheImpl(String name, IMap<Object, Object> map, Map<String, Object> properties) {
        this(name, map);
        this.properties = properties;
//        MapConfig mapConfig = getMapConfig(name, properties);
//        Config config = instance.getConfig();
//        config.addMapConfig(mapConfig);
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
    public void update(Object key, Object value) throws CacheException {
        if (key == null) {
            throw new CacheException("Cannot update an object in cache without a non-null key");
        }
        map.replace(key, value);
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
    public Map<Object, Object> list() {
        return map;
    }

}
