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
package org.codice.ddf.spatial.ogc.wfs.catalog.endpoint.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServicePropertiesMap<T> implements Map<T, Map<String, Object>> {

    private static final String READ_ONLY_ERROR_MESSAGE = "This map is meant to be read only.";

    private Map<T, Map<String, Object>> serviceMap = Collections
            .synchronizedMap(new HashMap<T, Map<String, Object>>());

    /**
     * Adds the newly bound OSGi service and its properties to the internally maintained and sorted
     * serviceMap. This method is invoked when a service is bound (created/installed).
     * 
     * @param service
     *            the OSGi service reference
     * @param properties
     *            the properties of the service
     */
    public void bindService(T service, Map<String, Object> properties) {
        serviceMap.put(service, properties);
    }

    /**
     * Removes the newly bound OSGi service and its properties from the internally maintained and
     * sorted serviceMap. This method is invoked when a service is unbound (removed/uninstalled).
     * 
     * @param service
     *            the OSGi service reference
     */
    public void unbindService(T service) {
        serviceMap.remove(service);
    }

    @Override
    public int size() {
        return serviceMap.size();
    }

    @Override
    public boolean isEmpty() {
        return serviceMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return serviceMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return serviceMap.containsValue(value);
    }

    @Override
    public Map<String, Object> get(Object key) {
        return serviceMap.get(key);
    }

    @Override
    public Map<String, Object> put(T key, Map<String, Object> value) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    @Override
    public Map<String, Object> remove(Object key) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    @Override
    public void putAll(Map<? extends T, ? extends Map<String, Object>> m) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    @Override
    public Set<T> keySet() {
        return serviceMap.keySet();
    }

    @Override
    public Collection<Map<String, Object>> values() {
        return serviceMap.values();
    }

    @Override
    public Set<java.util.Map.Entry<T, Map<String, Object>>> entrySet() {
        return serviceMap.entrySet();
    }
}
