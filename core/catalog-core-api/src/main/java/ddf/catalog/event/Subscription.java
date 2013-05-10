/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.event;

import org.opengis.filter.Filter;

import ddf.catalog.federation.Federatable;

/**
 * A Subscription is used for a "subscriber" to subscribe to events based on
 * a particular type of filter. For example, a Subscription can be created
 * to receive events within the last hour.
 * 
 * Since the {@code Subscription} extends the {@link Filter} class, the
 * {@code Subscription} must conform to the OGC Filter structure.
 * 
 * @author ddf.isgs@lmco.com
 */
public interface Subscription extends Filter, Federatable {

	/**
	 * Gets the delivery method for the subscription. The delivery
	 * method will determine how the subscription handles 
	 * created/updated/deleted operations on {@link Metacard}s.
	 * 
	 * @return the delivery method
	 */
	public DeliveryMethod getDeliveryMethod();

}
