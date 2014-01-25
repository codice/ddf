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

import java.util.Set;

import ddf.cache.CacheException;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;

public class CacheKey {

    private Metacard metacard;

    private ResourceRequest resourceRequest;

    public CacheKey(Metacard metacard, ResourceRequest resourceRequest) {
        this.metacard = metacard;
        this.resourceRequest = resourceRequest;
    }

    // TODO check for characters that cannot be in a filepath
    /**
     * Key is comprised of the source, the metacard ID, and request properties if properties are
     * found. <br/>
     * Sample: <br/>
     * {@code <sourceId>-<metacardId>[_<propKey1>-<propVal1>_<propKey2>-<propVal2> ... ]}
     * 
     * @return key
     */
    public String generateKey() throws CacheException {

        if (metacard == null) {
            throw new CacheException("Metacard must not be null.");
        }

        Set<String> names = resourceRequest.getPropertyNames();

        String properties = "";

        for (String propertyName : names) {
            properties = "_" + propertyName + "-" + resourceRequest.getPropertyValue(propertyName);
        }

        return metacard.getSourceId() + "-" + metacard.getId() + properties;
    }
}
