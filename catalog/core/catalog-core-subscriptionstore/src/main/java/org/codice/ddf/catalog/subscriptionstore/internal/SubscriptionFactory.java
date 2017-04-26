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
 * when new subscription metadata is received as a result of another node's processing.
 * <p>
 * <b>This interface is for internal use only and should not be implemented by a third party. </b>
 * <i>This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.</i>
 */
public interface SubscriptionFactory<S extends Subscription> {

    S createSubscription(String messageBody);
}
