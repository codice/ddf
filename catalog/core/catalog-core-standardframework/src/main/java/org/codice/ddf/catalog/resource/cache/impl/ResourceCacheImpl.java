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
package org.codice.ddf.catalog.resource.cache.impl;

import java.util.Optional;

import org.apache.commons.lang.Validate;
import org.codice.ddf.catalog.resource.cache.ResourceCache;

import ddf.catalog.cache.ResourceCacheInterface;
import ddf.catalog.cache.impl.CacheKey;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.resource.Resource;

/**
 * Metacard resource cache. Currently delegates to {@link ddf.catalog.cache.impl.ResourceCacheImpl}.
 */
public class ResourceCacheImpl implements ResourceCache {
    private final ResourceCacheInterface delegate;

    public ResourceCacheImpl(ResourceCacheInterface delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<Resource> get(Metacard metacard) {
        Validate.notNull(metacard, "Metacard cannot be null");
        return get(metacard, new ResourceRequestById(metacard.getId()));
    }

    @Override
    public Optional<Resource> get(Metacard metacard, ResourceRequest resourceRequest) {
        return Optional.ofNullable(delegate.getValid(new CacheKey(metacard,
                resourceRequest).generateKey(), metacard));
    }

    @Override
    public boolean contains(Metacard metacard) {
        Validate.notNull(metacard, "Metacard cannot be null");
        return contains(metacard, new ResourceRequestById(metacard.getId()));
    }

    @Override
    public boolean contains(Metacard metacard, ResourceRequest resourceRequest) {
        return delegate.containsValid(new CacheKey(metacard, resourceRequest).generateKey(),
                metacard);
    }
}
