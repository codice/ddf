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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nullable;

import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionContainer;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionFactory;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionStoreException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.event.Subscription;

/**
 * Container providing centralized, cached access to all subscriptions that can be registered with
 * the system from any endpoint or subscription provider.
 */
public class SubscriptionCache implements SubscriptionContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionCache.class);

    private static final String EVENT_ENDPOINT = "event-endpoint";

    private static final String SUBSCRIPTION_ID = "subscription-id";

    final SubscriptionStore subscriptionStore;

    // TODO: Replace with javax.cache.Cache ?
    final Map<String, SubscriptionMetadata> subscriptions;

    private final Map<String, SubscriptionFactory> factories;

    private final ReadWriteLock readWriteLock;

    public SubscriptionCache(SubscriptionStore subscriptionStore) {
        this.subscriptionStore = subscriptionStore;
        this.readWriteLock = new ReentrantReadWriteLock(true);

        this.subscriptions = new ConcurrentHashMap<>();
        this.factories = new ConcurrentHashMap<>();
    }

    @Override
    public void registerSubscriptionFactory(String type, SubscriptionFactory factory) {
        notEmpty(type, "Subscription type cannot be empty");
        notNull(factory, "Subscription factory cannot be null");

        if (factories.containsKey(type)) {
            throw new SubscriptionStoreException(format(
                    "Factory already exists for subscription type [%s]",
                    type));
        }

        factories.put(type, factory);
    }

    @Nullable
    @Override
    public Subscription get(String subscriptionId, String type) {
        notEmpty(subscriptionId, "subscriptionId cannot be null or empty");
        notEmpty(type, "type cannot be null or empty");

        if (!subscriptions.containsKey(subscriptionId)) {
            return null;
        }

        SubscriptionMetadata metadata = subscriptions.get(subscriptionId);

        if (!type.equals(metadata.getType())) {
            return null;
        }

        return metadata.getSubscription()
                .orElseThrow(() -> new SubscriptionStoreException(format(
                        "Subscription [%s] was not cached",
                        metadata.getId())));
    }

    @Override
    public String insert(Subscription subscription, String type, String messageBody,
            String callbackUrl) {
        notNull(subscription, "subscription cannot be null");
        notEmpty(type, "type cannot be null or empty");
        notEmpty(messageBody, "messageBody cannot be null or empty");
        notEmpty(callbackUrl, "callbackUrl cannot be null or empty");

        SubscriptionMetadata metadata = new SubscriptionMetadata(type, messageBody, callbackUrl);

        createSubscriptionLocally(metadata, subscription);
        subscriptionStore.insert(metadata);

        return metadata.getId();
    }

    @Override
    public void update(Subscription subscription, String type, String messageBody,
            String callbackUrl, String subscriptionId) {
        notNull(subscription, "subscription cannot be null");
        notEmpty(type, "type cannot be null or empty");
        notEmpty(messageBody, "messageBody cannot be null or empty");
        notEmpty(callbackUrl, "callbackUrl cannot be null or empty");
        notEmpty(subscriptionId, "subscriptionId cannot be null or empty");

        if (!contains(subscriptionId, type)) {
            LOGGER.debug("Target for subscription update [{}|{}] does not exist",
                    subscriptionId,
                    type);
            return;
        }

        delete(subscriptionId, type);

        SubscriptionMetadata metadata = new SubscriptionMetadata(type,
                messageBody,
                callbackUrl,
                subscriptionId);

        createSubscriptionLocally(metadata, subscription);
        subscriptionStore.insert(metadata);
    }

    @Nullable
    @Override
    public Subscription delete(String subscriptionId, String type) {
        notEmpty(subscriptionId, "subscriptionId cannot be null or empty");
        notEmpty(type, "type cannot be null or empty");

        if (!contains(subscriptionId, type)) {
            return null;
        }

        SubscriptionMetadata metadata = deleteSubscriptionLocally(subscriptionId);
        subscriptionStore.delete(subscriptionId);

        return metadata.getSubscription()
                .orElseThrow(() -> new SubscriptionStoreException(format(
                        "Subscription [%s] was not cached",
                        metadata.getId())));
    }

    @Override
    public boolean contains(String subscriptionId, String type) {
        return subscriptions.containsKey(subscriptionId) && type.equals(subscriptions.get(
                subscriptionId)
                .getType());
    }

    void createSubscriptionLocally(SubscriptionMetadata metadata) {
        createSubscriptionLocally(metadata, reviveSubscription(metadata));
    }

    private void createSubscriptionLocally(SubscriptionMetadata metadata,
            Subscription subscription) {
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put(SUBSCRIPTION_ID, metadata.getId());
        properties.put(EVENT_ENDPOINT,
                metadata.getCallbackUrl()
                        .toString());

        ServiceRegistration registration =
                getBundleContext().registerService(Subscription.class.getName(),
                        subscription,
                        properties);

        if (registration != null) {
            LOGGER.debug("Subscription [{}|{}|{}] registered with bundle ID = {} ",
                    metadata.getId(),
                    metadata.getType(),
                    metadata.getCallbackUrl()
                            .toString(),
                    registration.getReference()
                            .getBundle()
                            .getBundleId());
            metadata.setRegistration(registration);
            subscriptions.put(metadata.getId(), metadata);
        } else {
            throw new SubscriptionStoreException(format(
                    "Subscription registration attempt for id [%s] of type [%s] was null and will not be persisted",
                    metadata.getId(),
                    metadata.getType()));
        }
    }

    SubscriptionMetadata deleteSubscriptionLocally(String key) {
        SubscriptionMetadata metadata = subscriptions.remove(key);
        ServiceRegistration registration = metadata.getRegistration()
                .orElseThrow(() -> new SubscriptionStoreException(format(
                        "Subscription [%s] had no registration. ",
                        key)));
        registration.unregister();
        return metadata;
    }

    private Subscription reviveSubscription(SubscriptionMetadata metadata) {
        if (!factories.containsKey(metadata.getType())) {
            throw new SubscriptionStoreException(format(
                    "No factory registered for subscription type [%s]",
                    metadata.getType()));
        }

        SubscriptionFactory factory = factories.get(metadata.getType());
        return factory.createSubscription(metadata.getMessageBody());
    }

    private BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(SubscriptionStore.class)
                .getBundleContext();
    }
}
