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
 */
public interface SubscriptionContainer<S extends Subscription> {

    void registerSubscriptionFactory(String type, SubscriptionFactory<S> factory);

    @Nullable
    S get(String subscriptionId, String type);

    String insert(S subscription, String type, String messageBody, String callbackUrl);

    void update(S subscription, String type, String messageBody, String callbackUrl,
            String subscriptionId);

    @Nullable
    S delete(String subscriptionId, String type);

    boolean contains(String subscriptionId, String type);
}
