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
package org.codice.ddf.catalog.subscriptionstore;

import javax.cache.Cache;

import org.codice.ddf.catalog.subscriptionstore.common.CachedSubscription;
import org.codice.ddf.catalog.subscriptionstore.common.SubscriptionMetadata;

public class CacheEntryTestImpl implements Cache.Entry<String, CachedSubscription> {

    private final String key;

    private final CachedSubscription subscription;

    public CacheEntryTestImpl(SubscriptionMetadata metadata) {
        this.key = metadata.getId();
        this.subscription = new CachedSubscription(metadata);
    }

    public CacheEntryTestImpl(CachedSubscription cachedSubscription) {
        this.key = cachedSubscription.getMetadata()
                .getId();
        this.subscription = cachedSubscription;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public CachedSubscription getValue() {
        return subscription;
    }

    @Override
    public Object unwrap(Class aClass) {
        throw new UnsupportedOperationException();
    }
}
