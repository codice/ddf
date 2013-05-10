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




/**
 * A Subscriber is used to identify a service that is capable of deleting
 * subscriptions. The Subscriber must have created the subscription being deleted
 * and maintained a reference to its ServiceRegistration in order to be able to
 * delete the subscription.
 *  
 * @author ddf.isgs@lmco.com
 */
public interface Subscriber
{
    /**
     * Deletes a subscription with the specified ID. Subscription deletion includes
     * unregistering the Subscription's OSGi service, its associated DurableSubscription
     * OSGi service (if any), and removing the subscription from the internal hashmaps
     * maintained by the Subscriber.
     * 
     * @param subscriptionId the globally unique UUID for a subscription
     * @return true if subscription successfully deleted; false otherwise
     */
    public boolean deleteSubscription(String subscriptionId);
}
