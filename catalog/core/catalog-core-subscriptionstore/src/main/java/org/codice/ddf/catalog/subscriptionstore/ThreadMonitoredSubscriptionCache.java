package org.codice.ddf.catalog.subscriptionstore;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Temporary measure for refreshing the cache.
 */
public class ThreadMonitoredSubscriptionCache extends SubscriptionCache {

    // TODO: Might abstract this into a cache invalidation strategy later
    private final Executor cacheThread = Executors.newSingleThreadExecutor();

    public ThreadMonitoredSubscriptionCache(SubscriptionStore subscriptionStore) {
        super(subscriptionStore);
    }

    public void synchronizeCacheWithBackend() {
        // TODO: Don't do anything without first acquiring the write lock
        Map<String, SubscriptionMetadata> backendCollection = subscriptionStore.getSubscriptions();
        subscriptions.keySet()
                .stream()
                .filter(key -> !backendCollection.containsKey(key))
                .forEach(this::deleteSubscriptionLocally);
        backendCollection.keySet()
                .stream()
                .filter(key -> !subscriptions.containsKey(key))
                .map(backendCollection::get)
                .forEach(this::createSubscriptionLocally);
        // TODO: Add processing / hashcode checking for update operations
        // Might need to rework this entire method
        // TODO: Release the write lock
    }
}
