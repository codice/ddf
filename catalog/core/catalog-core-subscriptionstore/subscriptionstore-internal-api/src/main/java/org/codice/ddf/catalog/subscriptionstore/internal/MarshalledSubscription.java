/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.subscriptionstore.internal;

/**
 * Describes the pieces of a {@link ddf.catalog.event.Subscription} that can be immediately
 * serialized without any special factory logic.
 *
 * <p><b>This interface is for internal use only and should not be implemented by a third party.
 * </b> <i>This code is experimental. While this interface is functional and tested, it may change
 * or be removed in a future version of the library.</i>
 */
public interface MarshalledSubscription {

  /**
   * Get the subscription filter as a marshalled string. This can be JSON, XML with any schema, etc.
   * No guarantees are made to the format of this String, only that providing it to the correct
   * {@link SubscriptionFactory} will yield a valid instance of {@code Subscription}.
   *
   * @return the marshalled subscription filter.
   */
  String getFilter();

  /**
   * Get the subscription callback address. While current endpoints conform to using a {@link
   * java.net.URL} for their {@link ddf.catalog.event.DeliveryMethod}, this is <b>not</b> a
   * guarantee.
   *
   * @return the callback address to send events to for this {@code Subscription}.
   */
  String getCallbackAddress();
}
