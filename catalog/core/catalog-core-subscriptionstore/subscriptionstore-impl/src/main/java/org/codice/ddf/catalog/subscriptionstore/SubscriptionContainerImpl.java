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

import static java.lang.String.format;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.integration.CompletionListener;
import javax.cache.spi.CachingProvider;

import org.cache2k.jcache.provider.JCacheProvider;
import org.codice.ddf.catalog.subscriptionstore.common.CachedSubscription;
import org.codice.ddf.catalog.subscriptionstore.common.SubscriptionMetadata;
import org.codice.ddf.catalog.subscriptionstore.internal.MarshalledSubscription;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionContainer;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionFactory;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionIdentifier;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionStoreException;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.event.Subscription;

/**
 * Container providing centralized, cached access to all {@link Subscription}s that can be registered
 * with the system from any endpoint or subscription provider.
 * <p>
 * This implementation uses a {@link javax.cache.Cache} with an {@link EternalExpiryPolicy} to store
 * instances of {@code Subscription} with the {@link org.codice.ddf.persistence.PersistentStore} as
 * the backing store for the cache data.
 * <p>
 * The following "container" operations are not atomic and must be synchronized:
 * <ul>
 * <li>Any CRUD operation on the subscription store</li>
 * <li>Initial loading of subscriptions from the store</li>
 * <li>Binding and unbinding of {@link SubscriptionFactory} implementations from container clients</li>
 * </ul>
 */
public class SubscriptionContainerImpl implements SubscriptionContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionContainerImpl.class);

    private static final String SINGLE_QUOTE = "'";

    private static final String SUBSCRIPTION_CACHE_NAME = "subscription_store";

    private final Cache<String, CachedSubscription> subscriptionsCache;

    private final Map<String, SubscriptionFactory> factories;

    public SubscriptionContainerImpl(SubscriptionCacheLoader cacheLoader,
            SubscriptionCacheWriter cacheWriter) {
        this.subscriptionsCache = createCache(cacheLoader, cacheWriter);
        this.factories = createFactoryMap();
    }

    /**
     * On startup, load existing subscriptions from the central store.
     * <p>
     * This is an implementation detail. We need to ensure CRUD operations, loading the cache when
     * Karaf starts, and the binding / unbinding of factories ... are ALL mutually exclusive.
     * <p>
     * Event handlers to remote cache events also fit into this category. We may need to consider
     * passing around a shared lock if this logic needs to be split out. In addition, event handlers
     * may need to invoke this class asynchronously if they are not asynchronous themselves, otherwise
     * we can incur deadlock by contesting the cache implementation's locking mechanism.
     * <p>
     * If event handlers are given this container impl, redundant writes and deletes to the backing
     * store can <b>NOT</b> be considered errors.
     *
     * @see SubscriptionCacheLoader#loadAll(Iterable)
     */
    public synchronized void init() {
        subscriptionsCache.loadAll(Collections.emptySet(),
                false,
                new CacheUpdateCompletionListener());

        StreamSupport.stream(subscriptionsCache.spliterator(), false)
                .map(Cache.Entry::getValue)
                .filter(CachedSubscription::isNotRegistered)
                .filter(sub -> factories.containsKey(sub.getMetadata()
                        .getTypeName()))
                .forEach(sub -> sub.registerSubscription(factories.get(sub.getMetadata()
                        .getTypeName())));
    }

    /**
     * {@inheritDoc}
     * <p>
     * An attempt is made to return the subscription object so long as:
     * <ol>
     * <li>The subscription being identified exists in the cache</li>
     * <li>The subscription's type matches the identifier's type</li>
     * <li>The subscription is registered with OSGi</li>
     * </ol>
     * <p>
     * One impact of an incorrect implementation of {@link SubscriptionFactory} will be incorrect
     * {@code null} results returned for client traffic that occurs during service initialization time.
     * This is because queries for non-registered subscriptions that do exist but haven't been initialized
     * yet return null. Subscription container clients that expose external services must ensure their
     * factory is available prior to, or at the same time as, those exposed services.
     *
     * @see SubscriptionFactory
     */
    @Nullable
    @Override
    public synchronized Subscription get(SubscriptionIdentifier identifier) {
        validateIdentifier(identifier);
        String subscriptionId = identifier.getId();
        String type = identifier.getTypeName();

        CachedSubscription cachedSubscription = subscriptionsCache.get(subscriptionId);

        if (cachedSubscription == null || cachedSubscription.isNotType(type)
                || cachedSubscription.isNotRegistered()) {
            return null;
        }

        return cachedSubscription.getSubscription()
                .orElseThrow(() -> new SubscriptionStoreException("Could not get subscription. "));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Register the subscription and write to the cache. If there is a problem, rollback the registration.
     */
    @Override
    public synchronized SubscriptionIdentifier insert(Subscription subscription,
            MarshalledSubscription marshalledSubscription, SubscriptionType type) {
        validateSubscription(subscription);
        validateSerialized(marshalledSubscription);
        validateType(type);

        SubscriptionMetadata metadata = new SubscriptionMetadata(type.getTypeName(),
                marshalledSubscription.getFilter(),
                marshalledSubscription.getCallbackAddress());

        doInsert(subscription, metadata);
        return metadata;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Updates are performed by a delete first, then an insertion using the same id. The operations
     * are identical to {@link #insert(Subscription, MarshalledSubscription, SubscriptionType)}
     * and to {@link #delete(SubscriptionIdentifier)} with the exception of preserving the id in the
     * {@link SubscriptionMetadata}.
     */
    @Override
    public synchronized void update(Subscription subscription,
            MarshalledSubscription marshalledSubscription, SubscriptionIdentifier identifier) {
        validateSubscription(subscription);
        validateSerialized(marshalledSubscription);
        validateContainment(identifier, "update");

        String subscriptionId = identifier.getId();
        doDelete(subscriptionId);

        SubscriptionMetadata metadata = new SubscriptionMetadata(identifier.getTypeName(),
                marshalledSubscription.getFilter(),
                marshalledSubscription.getCallbackAddress(),
                subscriptionId);

        doInsert(subscription, metadata);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Remove the subscription from the cache first, which will propogate the delete to the backing
     * store. If an issue occurs during the delete, the subscription will <b>not</b> be unregistered.
     */
    @Override
    public synchronized Subscription delete(SubscriptionIdentifier identifier) {
        validateContainment(identifier, "delete");
        String subscriptionId = identifier.getId();
        CachedSubscription cachedSubscription = doDelete(subscriptionId);
        return cachedSubscription.getSubscription()
                .orElseThrow(() -> new SubscriptionStoreException("Could not get subscription. "));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean contains(SubscriptionIdentifier identifier) {
        return get(identifier) != null;
    }

    /**
     * Called when the OSGi framework registers a factory, typically from the container's client. It
     * could be an endpoint, or any other subscription provider.
     *
     * @param factory the {@link SubscriptionFactory} that was registered and is now available.
     */
    public synchronized void bindFactory(SubscriptionFactory factory) {
        if (factory == null) {
            LOGGER.debug("Subscription container binding was given a null factory. ");
            return;
        }

        if (factories.containsKey(factory.getTypeName())) {
            throw new SubscriptionStoreException(
                    "Duplicate factory registered. This is an API client error");
        }

        factories.put(factory.getTypeName(), factory);
        LOGGER.debug("SubscriptionFactory registered for {}", factory.getTypeName());

        StreamSupport.stream(subscriptionsCache.spliterator(), false)
                .map(Cache.Entry::getValue)
                .filter(CachedSubscription::isNotRegistered)
                .filter(sub -> sub.isType(factory.getTypeName()))
                .forEach(sub -> sub.registerSubscription(factory));
    }

    /**
     * Called when the OSGi framework unregisters a factory.
     *
     * @param factory the {@link SubscriptionFactory} that was unregistered.
     * @throws SubscriptionStoreException if an unexpected registration tried to occur.
     */
    public synchronized void unbindFactory(SubscriptionFactory factory) {
        if (factory == null) {
            LOGGER.debug("Subscription container unbinding was given a null factory. ");
            return;
        }

        SubscriptionFactory removedFactory = factories.remove(factory.getTypeName());
        if (removedFactory == null) {
            throw new SubscriptionStoreException(format(
                    "Binding synchronization is wrong. No factory [%s] exists to unbind. ",
                    factory.getTypeName()));
        }
        LOGGER.debug("SubscriptionFactory removed for {}", factory.getTypeName());
    }

    /**
     * Helper method for core insert logic.
     */
    private void doInsert(Subscription subscription, SubscriptionMetadata metadata) {
        CachedSubscription cachedSubscription = createCachedSubscription(metadata);
        cachedSubscription.registerSubscription(subscription);
        try {
            subscriptionsCache.put(metadata.getId(), cachedSubscription);
        } catch (CacheException e) {
            cachedSubscription.unregisterSubscription();
            throw new SubscriptionStoreException("Problem writing to the cache. ", e);
        }
    }

    /**
     * Helper method for core delete logic.
     */
    private CachedSubscription doDelete(String subscriptionId) {
        CachedSubscription cachedSubscription = subscriptionsCache.get(subscriptionId);
        try {
            subscriptionsCache.remove(subscriptionId);
        } catch (CacheException e) {
            throw new SubscriptionStoreException("Problem deleting from cache. ", e);
        }
        cachedSubscription.unregisterSubscription();
        return cachedSubscription;
    }

    /**
     * Ensure the identifier points to a valid subscription, and that it exists in the container.
     *
     * @param operation customize the message based on where this validation is occurring.
     */
    private void validateContainment(SubscriptionIdentifier identifier, String operation) {
        if (get(identifier) == null) {
            LOGGER.debug("Target for subscription {} [ {} | {} ] does not exist",
                    operation,
                    identifier.getId(),
                    identifier.getTypeName());
            throw new SubscriptionStoreException(format("Subscription [%s] does not exist. ",
                    identifier.getId()));
        }
    }

    private void validateSubscription(Subscription subscription) {
        notNull(subscription, "Subscription object cannot be null. ");
    }

    private void validateIdentifier(SubscriptionIdentifier identifier) {
        notNull(identifier, "Subscription identifier cannot be null. ");
        notEmpty(identifier.getId(), "Subscription ID string cannot be null or empty. ");
        notEmpty(identifier.getTypeName(), "Subscription type string cannot be null or empty. ");

        // TODO: Get Approval - Backslash is not the ECQL escape character, thus we need to forbid single quotes
        if (identifier.getId()
                .contains(SINGLE_QUOTE)) {
            throw new SubscriptionStoreException("Subscription ID contains invalid characters");
        }
    }

    private void validateType(SubscriptionType type) {
        notNull(type, "Subscription type cannot be null. ");
        notEmpty(type.getTypeName(), "Subscription type string cannot be null or empty. ");
    }

    private void validateSerialized(MarshalledSubscription marshalledSubscription) {
        notNull(marshalledSubscription, "Serialized subscription cannot be null. ");
        notEmpty(marshalledSubscription.getFilter(),
                "Serialized filter string cannot be null or empty. ");
        notEmpty(marshalledSubscription.getCallbackAddress(),
                "Callback address cannot be null or empty. ");
    }

    /**
     * Configuration for the javax.cache (For now, Cache2K is used).
     */
    @SuppressWarnings("unchecked")
    Cache<String, CachedSubscription> createCache(SubscriptionCacheLoader cacheLoader,
            SubscriptionCacheWriter cacheWriter) {
        CachingProvider cachingProvider =
                Caching.getCachingProvider(JCacheProvider.class.getClassLoader());
        CacheManager cacheManager = cachingProvider.getCacheManager();

        MutableConfiguration<String, CachedSubscription> config =
                cleanMutableTypedConfig().setExpiryPolicyFactory(EternalExpiryPolicy::new)
                        .setCacheLoaderFactory(() -> cacheLoader)
                        .setCacheWriterFactory(() -> cacheWriter)
                        .setStoreByValue(false)
                        .setReadThrough(true)
                        .setWriteThrough(true);

        return cacheManager.createCache(SUBSCRIPTION_CACHE_NAME, config);
    }

    /**
     * Factory method for creating the factory map.
     */
    Map<String, SubscriptionFactory> createFactoryMap() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Factory method for creating cached subscriptions.
     */
    CachedSubscription createCachedSubscription(SubscriptionMetadata metadata) {
        return new CachedSubscription(metadata);
    }

    /**
     * Create a fresh starting point for the cache's configuration. Primarily to keep code clean.
     * The formatting gets pretty ugly otherwise.
     */
    private MutableConfiguration cleanMutableTypedConfig() {
        return new MutableConfiguration<String, CachedSubscription>().setTypes(String.class,
                CachedSubscription.class);
    }

    /**
     * Define the actions to take based on the result of a cache load.
     */
    private static class CacheUpdateCompletionListener implements CompletionListener {

        @Override
        public void onCompletion() {
            LOGGER.debug("Cache load complete. ");
        }

        @Override
        public void onException(Exception e) {
            throw new SubscriptionStoreException("Cache update threw an exception. ", e);
        }
    }
}
