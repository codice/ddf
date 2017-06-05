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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;

import org.codice.ddf.catalog.subscriptionstore.common.CachedSubscription;
import org.codice.ddf.catalog.subscriptionstore.common.SubscriptionMetadata;
import org.codice.ddf.catalog.subscriptionstore.common.SubscriptionPersistor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link CacheLoader} implementation for the {@link javax.cache.Cache} in
 * {@link SubscriptionContainerImpl}. All inner exceptions will be handled by the cache
 * implementation, or rethrown as a {@link javax.cache.CacheException}.
 */
public class SubscriptionCacheLoader implements CacheLoader<String, CachedSubscription> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionCacheLoader.class);

    private final SubscriptionPersistor persistor;

    public SubscriptionCacheLoader(SubscriptionPersistor persistor) {
        this.persistor = persistor;
    }

    @Override
    public CachedSubscription load(String key) throws CacheLoaderException {
        LOGGER.debug("Populating cache with entity for key [{}]", key);
        Map<String, SubscriptionMetadata> subscriptions = persistor.getSubscriptions();
        return loadFromMap(subscriptions, key);
    }

    /**
     * Implementation detail. We need to ignore the keys because we don't know them at load time.
     * If we want to honor the keys, and know them at load time, we will need to ping the persistent
     * store twice.
     * But there's no point to doing that (for the sake of an API). The keys don't matter - we're
     * loading everything that we can load regardless of the keys provided.
     */
    @Override
    public Map<String, CachedSubscription> loadAll(Iterable<? extends String> keys)
            throws CacheLoaderException {
        LOGGER.debug("Populating cache with all entries from the backing store");
        return persistor.getSubscriptions()
                .values()
                .stream()
                .map(CachedSubscription::new)
                .collect(Collectors.toMap(sub -> sub.getMetadata()
                        .getId(), Function.identity()));
    }

    /**
     * Subscription loading boilerplate.
     */
    private CachedSubscription loadFromMap(Map<String, SubscriptionMetadata> map, String key) {
        SubscriptionMetadata metadata = map.get(key);
        if (metadata == null) {
            return null;
        }
        return new CachedSubscription(metadata);
    }
}
