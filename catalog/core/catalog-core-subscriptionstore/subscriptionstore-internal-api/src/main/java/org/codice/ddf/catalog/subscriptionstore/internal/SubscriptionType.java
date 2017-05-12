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

/**
 * Uniquely identifies a consumer of subscription services. There should be a 1-to-1 correspondence
 * between a type and a consumer's {@link SubscriptionFactory} that gets registered with OSGi. Consumers
 * must ensure the value for {@link SubscriptionType#getTypeName()} is always:
 * <ol>
 * <li>Unique across all API consumers</li>
 * <li>Consistent across all {@link SubscriptionType}s, including {@link SubscriptionIdentifier}
 * and {@link SubscriptionFactory} implementations</li>
 * </ol>
 * <p>
 * <b>This interface is for internal use only and should not be implemented by a third party. </b>
 * <i>This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.</i>
 *
 * @see SubscriptionIdentifier
 */
public interface SubscriptionType {

    /**
     * Retrieve a unique type.
     *
     * @return the type identifier.
     */
    String getTypeName();
}
