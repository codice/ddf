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

import java.util.Collection;

import javax.cache.Cache;
import javax.cache.integration.CacheWriter;
import javax.cache.integration.CacheWriterException;

import org.codice.ddf.catalog.subscriptionstore.common.CachedSubscription;
import org.codice.ddf.catalog.subscriptionstore.common.SubscriptionPersistor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link CacheWriter} implementation for the {@link Cache} in {@link SubscriptionContainerImpl}. All
 * inner exceptions will be handled by the cache implementation, or rethrown as a
 * {@link javax.cache.CacheException}.
 */
public class SubscriptionCacheWriter implements CacheWriter<String, CachedSubscription> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionCacheWriter.class);

    private final SubscriptionPersistor persistor;

    public SubscriptionCacheWriter(SubscriptionPersistor persistor) {
        this.persistor = persistor;
    }

    @Override
    public void write(Cache.Entry<? extends String, ? extends CachedSubscription> entry)
            throws CacheWriterException {
        LOGGER.debug("Writing through cache for key [{}]", entry.getKey());
        CachedSubscription cachedSubscription = entry.getValue();
        persistor.insert(cachedSubscription.getMetadata());
    }

    @Override
    public void writeAll(
            Collection<Cache.Entry<? extends String, ? extends CachedSubscription>> entries)
            throws CacheWriterException {
        entries.forEach(this::write);
    }

    @Override
    public void delete(Object obj) throws CacheWriterException {
        LOGGER.debug("Deleting through cache for key [{}]", obj);
        verifyString(obj);
        String key = (String) obj;
        persistor.delete(key);
    }

    @Override
    public void deleteAll(Collection<?> keys) throws CacheWriterException {
        keys.forEach(this::verifyString);
        keys.stream()
                .map(String.class::cast)
                .forEach(persistor::delete);
    }

    private void verifyString(Object obj) {
        if (!(obj instanceof String)) {
            throw new CacheWriterException("Subscription keys are expected to be of type String");
        }
    }
}
