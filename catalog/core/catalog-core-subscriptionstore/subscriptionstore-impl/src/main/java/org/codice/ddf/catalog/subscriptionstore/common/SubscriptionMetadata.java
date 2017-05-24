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

import java.util.UUID;

import org.codice.ddf.catalog.subscriptionstore.internal.MarshalledSubscription;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An immutable data object for storing subscriptions in the {@link org.codice.ddf.persistence.PersistentStore}.
 * <p>
 * {@link SubscriptionMetadata} wraps a serialized {@link ddf.catalog.event.Subscription} with no assumption as
 * to the specific serialization format, only the guarantee that metadata objects with the same <i>type</i> are
 * the same format. That means metadata objects with the same <i>type</i> can be serialized and deserialized the
 * same way, using the same instance of {@link org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionFactory}.
 */
public class SubscriptionMetadata implements SubscriptionIdentifier, MarshalledSubscription {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionMetadata.class);

    private static final String URN_UUID = "urn:uuid:";

    private final String id;

    private final String typeName;

    private final String filter;

    private final String callbackAddress;

    public SubscriptionMetadata(String typeName, String filter, String callbackAddress) {
        this(typeName,
                filter,
                callbackAddress,
                URN_UUID + UUID.randomUUID()
                        .toString());
    }

    public SubscriptionMetadata(String typeName, String filter, String callbackAddress, String id) {
        this.typeName = typeName;
        this.filter = filter;
        this.callbackAddress = callbackAddress;
        this.id = id;

        LOGGER.trace("Created subscription metadata object: {} | {} | {}",
                id,
                typeName,
                callbackAddress);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public String getFilter() {
        return filter;
    }

    @Override
    public String getCallbackAddress() {
        return callbackAddress;
    }
}
