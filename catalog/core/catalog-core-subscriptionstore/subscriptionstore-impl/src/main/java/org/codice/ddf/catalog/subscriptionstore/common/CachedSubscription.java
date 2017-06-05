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
package org.codice.ddf.catalog.subscriptionstore.common;

import static java.lang.String.format;
import static org.apache.commons.lang.Validate.notNull;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;

import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionFactory;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionRegistrationException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.event.Subscription;

/**
 * A data object for storing subscription information in a cache.
 * <p>
 * The data stored is a mix of persisted and locally cached information. This includes the
 * original {@link Subscription} either passed from the client or deserialized from stored data,
 * and the relevant {@link SubscriptionMetadata}.
 * <p>
 * The existence of a {@link CachedSubscription} object <i>between</i> subscription CRUD operations implies
 * that:
 * <ol>
 * <li>
 * The {@code SubscriptionMetadata} in this object is also in the {@code PersistentStore}.
 * </li>
 * <li>
 * The {@code Subscription} in this object is registered as a service with OSGi <b>only if</b>
 * {@link #isNotRegistered()} is false.
 * </li>
 * </ol>
 * {@code CachedSubscription} objects are not meant to be reused. They have a 1-to-1 correspondence with
 * a {@code SubscriptionMetadata} object in the backing store and are used to track that subscription's
 * lifecycle in OSGi.
 * <p>
 * Given the dynamic nature of Karaf and the need to handle {@link javax.cache.event.CacheEntryEvent}s,
 * there may be a brief period during system startup where cached subscriptions exist in the cache but
 * are not registered. This problem should rectify itself as additional {@link SubscriptionFactory}
 * instances are registered by subscription endpoints.
 * <p>
 * Currently, no guarantees are made regarding the state of this object <i>during</i> a subscription CRUD
 * operation. Only one container operation may be active at a time.
 *
 * @see org.codice.ddf.catalog.subscriptionstore.SubscriptionContainerImpl
 */
public class CachedSubscription {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedSubscription.class);

    private static final String SUBSCRIPTION_ID_OSGI = "subscription-id";

    private static final String EVENT_ENDPOINT = "event-endpoint";

    private static final Integer INITIAL_CAPACITY = 4;

    private final SubscriptionMetadata metadata;

    private transient Subscription subscription;

    private transient ServiceRegistration registration;

    public CachedSubscription(SubscriptionMetadata metadata) {
        notNull(metadata, "Subscription metadata cannot be null");
        this.metadata = metadata;

        this.subscription = null;
        this.registration = null;
    }

    public SubscriptionMetadata getMetadata() {
        return metadata;
    }

    public Optional<Subscription> getSubscription() {
        return Optional.ofNullable(subscription);
    }

    public boolean isType(String type) {
        return metadata.getTypeName()
                .equals(type);
    }

    public boolean isNotType(String type) {
        return !isType(type);
    }

    public synchronized boolean isNotRegistered() {
        return registration == null;
    }

    /**
     * Attempt to construct a {@link Subscription} using the provided factory. If the resultant
     * subscription is valid, register it as this cached subscription's OSGi service.
     *
     * @param factory subscription factory corresponding to this cached subscription's type.
     * @throws SubscriptionRegistrationException if the factory can't operate on this cached subscription.
     */
    public void registerSubscription(SubscriptionFactory factory) {
        if (isNotType(factory.getTypeName())) {
            throw new SubscriptionRegistrationException(format(
                    "Factory type mismatch for subscription type [%s]",
                    metadata.getTypeName()));
        }

        LOGGER.debug("Regenerating subscription [ {} | {} | {} ]",
                metadata.getId(),
                metadata.getTypeName(),
                metadata.getCallbackAddress());

        Subscription sub = factory.createSubscription(metadata);
        if (sub == null) {
            throw new SubscriptionRegistrationException("Unable to regenerate subscription");
        }
        registerSubscription(sub);
    }

    /**
     * Register the provided {@link Subscription} as this cached subscription's OSGi service.
     *
     * @param sub the subscription to register.
     * @throws SubscriptionRegistrationException if registration failed.
     */
    public synchronized void registerSubscription(Subscription sub) {
        if (registration != null) {
            throw new SubscriptionRegistrationException("Subscription already registered");
        }

        LOGGER.debug("Registering service [{}]", metadata.getId());

        Dictionary<String, String> properties = new Hashtable<>(INITIAL_CAPACITY);
        properties.put(SUBSCRIPTION_ID_OSGI, metadata.getId());
        properties.put(EVENT_ENDPOINT, metadata.getCallbackAddress());

        registration = getBundleContext().registerService(Subscription.class.getName(),
                sub,
                properties);

        if (registration != null) {
            LOGGER.debug("Subscription [ {} | {} | {} ] registered with bundle ID = {}",
                    metadata.getId(),
                    metadata.getTypeName(),
                    metadata.getCallbackAddress(),
                    registration.getReference()
                            .getBundle()
                            .getBundleId());
            subscription = sub;
        } else {
            throw new SubscriptionRegistrationException(format(
                    "Subscription registration attempt for id [%s] of type [%s] failed",
                    metadata.getId(),
                    metadata.getTypeName()));
        }
    }

    /**
     * Removes this cached subscriptions OSGi service from the service registry. Calling this method
     * on a cached subscription that {@link #isNotRegistered()} will throw an exception.
     *
     * @throws SubscriptionRegistrationException if this cached subscription did not have a service
     *                                           registered.
     */
    public synchronized void unregisterSubscription() {
        LOGGER.debug("Removing service [{}]", metadata.getId());
        if (registration == null) {
            throw new SubscriptionRegistrationException(format(
                    "Subscription [%s] had no registration and could not be unregistered",
                    metadata.getId()));
        }
        registration.unregister();
        registration = null;
    }

    protected BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(CachedSubscription.class)
                .getBundleContext();
    }
}
