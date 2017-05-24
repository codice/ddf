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

import ddf.catalog.event.Subscription;

/**
 * Provides a way for endpoints or other subscription providers to define the deserialization semantics
 * when new subscription metadata is received as a result of another node's processing. Endpoints or
 * other subscription providers should register their own implementation of this interface with the
 * OSGi Framework.
 * <p>
 * It is important that subscription providers guarantee that <b>if their services are up, their
 * factory is up</b>. This can be easily done by registering their factory in the same bundle as
 * their services. For example, consider the case of a REST endpoint. If an external client can use
 * the endpoint, then the OSGi Framework should have access to the factory.
 * <p>
 * <b>This interface is for internal use only and should not be implemented by a third party. </b>
 * <i>This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.</i>
 */
public interface SubscriptionFactory<S extends Subscription> extends SubscriptionType {

    /**
     * Revive a subscription from its serialized form.
     *
     * @param marshalledSubscription the subscription data to deserialize.
     * @return a valid instance of {@link S}.
     */
    S createSubscription(MarshalledSubscription marshalledSubscription);
}
