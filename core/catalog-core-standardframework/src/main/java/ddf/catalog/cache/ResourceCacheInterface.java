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

public interface ResourceCacheInterface {
    public boolean isPending(String key);
    
    public void put(ReliableResource reliableResource) throws CacheException;
    
    public void removePendingCacheEntry(String cacheKey);
    
    public void addPendingCacheEntry(String cacheKey);
    
    public Resource get(String key) throws CacheException;
}
