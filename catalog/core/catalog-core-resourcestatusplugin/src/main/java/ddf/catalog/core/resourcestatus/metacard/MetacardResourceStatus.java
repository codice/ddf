/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.core.resourcestatus.metacard;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.cache.ResourceCacheInterface;
import ddf.catalog.cache.impl.CacheKey;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.resource.data.ReliableResource;

/**
 * {@link PostQueryPlugin} that checks the {@link ddf.catalog.cache.impl.ResourceCache} for existence
 * of each {@link Metacard}'s related {@link ddf.catalog.resource.Resource} and
 * adds an {@link ddf.catalog.data.Attribute} to each {@link Metacard} in the {@link QueryResponse}.
 */
public class MetacardResourceStatus implements PostQueryPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardResourceStatus.class);

    private ResourceCacheInterface cache;

    public MetacardResourceStatus(ResourceCacheInterface cache) {
        this.cache = cache;
    }

    @Override
    public QueryResponse process(QueryResponse input)
            throws PluginExecutionException, StopProcessingException {
        List<Result> results = input.getResults();

        results.stream()
                .map(Result::getMetacard)
                .filter(metacard -> metacard != null)
                .forEach(this::addResourceCachedAttribute);

        return input;
    }

    private void addResourceCachedAttribute(Metacard metacard) {
        metacard.setAttribute(new AttributeImpl(Metacard.RESOURCE_CACHE_STATUS,
                isResourceCached(metacard, new ResourceRequestById(metacard.getId()))));
    }

    private boolean isResourceCached(Metacard metacard, ResourceRequest resourceRequest) {
        String key = getCacheKey(metacard, resourceRequest);

        ReliableResource cachedResource = (ReliableResource) cache.getValid(key, metacard);
        if (cachedResource != null) {
            return true;
        }
        return false;
    }

    private String getCacheKey(Metacard metacard, ResourceRequest resourceRequest) {
        CacheKey cacheKey = new CacheKey(metacard, resourceRequest);
        return cacheKey.generateKey();
    }
}