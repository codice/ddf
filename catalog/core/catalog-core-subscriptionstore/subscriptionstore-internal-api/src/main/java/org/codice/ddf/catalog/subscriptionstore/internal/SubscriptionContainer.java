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
package org.codice.ddf.catalog.subscriptionstore.internal;

import javax.annotation.Nullable;

import ddf.catalog.event.Subscription;

/**
 * Defines a centralized container service for CRUD operations on subscriptions.
 * <p>
 * All consumers must register their own implementation of {@link SubscriptionFactory} as an OSGi service.
 * <p>
 * <b>This interface is for internal use only and should not be implemented by a third party. </b>
 * <i>This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.</i>
 *
 * @see SubscriptionType
 * @see SubscriptionFactory
 */
public interface SubscriptionContainer<S extends Subscription> {

    /**
     * Get the subscription that the provided identifier points to.
     *
     * @param identifier the unique identity of the requested subscription.
     * @return the requested subscription, or null if it does not exist.
     */
    @Nullable
    S get(SubscriptionIdentifier identifier);

    /**
     * Add a new subscription to the central store.
     *
     * @param subscription           the subscription object to be registered.
     * @param marshalledSubscription the serialized version of the subscription object.
     * @param type                   the new subscription's type.
     * @return the unique identity of the newly created subscription.
     * @throws SubscriptionStoreException if there is a problem adding the subscription.
     */
    SubscriptionIdentifier insert(S subscription, MarshalledSubscription marshalledSubscription,
            SubscriptionType type);

    /**
     * Update an existing subscription with newly serialized parameters while retaining the unique
     * identification data.
     *
     * @param subscription           the new subscription object to be registered. The old one will be lost.
     * @param marshalledSubscription the serialized components of the new subscription.
     * @param identifier             the unique identity of the subscription to update, which must exist in the
     *                               backing store.
     * @throws SubscriptionStoreException if there is a problem updating, or the identifier points to a
     *                                    subscription that does not exist.
     */
    void update(S subscription, MarshalledSubscription marshalledSubscription,
            SubscriptionIdentifier identifier);

    /**
     * TODO: Finish up here, double check impl classes, update csw endpoint, test everything out
     * TODO: Then write a unit test, and get the review process started
     * Remove an existing subscription from the central store.
     *
     * @param identifier the unique identity of the subscription to delete, which must exist in the
     *                   backing store.
     * @return the subscription that was removed.
     * @throws SubscriptionStoreException if there is a problem deleting, or the identifier points to a
     *                                    subscription that does not exist.
     */
    S delete(SubscriptionIdentifier identifier);

    /**
     * Determine if the provided identifier points to a subscription that exists in the central store.
     *
     * @param identifier the unique identity of a subscription.
     * @return true if the identifier points to a subscription that exists, false otherwise.
     */
    boolean contains(SubscriptionIdentifier identifier);
}
