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

import ddf.catalog.data.Metacard;
import ddf.catalog.source.RemoteSource;

/**
 * This interface is used to describe:
 * <ul>
 * <li>a method of creating, updating, and deleting subscriptions.
 * <li>a method of broadcasting events when {@link Metacard}s are
 * created, updated, or deleted.
 * </ul>
 * 
 * @author ddf.isgs@lmco.com
 */
public interface EventProcessor {


	//TODO move to implementation and undeprecate - not part of specification.
	/**
	 * @deprecated This constant is implementation specific and will be removed from API
	 */
	public final static String EVENT_METACARD = "ddf.catalog.event.metacard";
	/**
	 * @deprecated This constant is implementation specific and will be removed from API
	 */
	public final static String EVENT_OLD_METACARD = "ddf.catalog.event.old-metacard";
	/**
	 * @deprecated This constant is implementation specific and will be removed from API
	 */
	public final static String EVENT_TIME = "ddf.catalog.event.time";
	/**
	 * @deprecated This constant is implementation specific and will be removed from API
	 */
	public final static String EVENTS_TOPIC_CREATED = "ddf/catalog/event/CREATED";
	/**
	 * @deprecated This constant is implementation specific and will be removed from API
	 */
	public final static String EVENTS_TOPIC_UPDATED = "ddf/catalog/event/UPDATED";
	/**
	 * @deprecated This constant is implementation specific and will be removed from API
	 */
	public final static String EVENTS_TOPIC_DELETED = "ddf/catalog/event/DELETED";

	/**
	 * Create a {@link Subscription} with an automatically-generated id.
	 * 
	 * @see #createSubscription(Subscription, String)
	 * @param subscription the {@link Subscription} to register
	 * @return {@link String} an id for the registered {@link Subscription}
	 *         that can be used later by calling {@link #unsubscribe(String)}
	 * @throws InvalidSubscriptionException if this {@link EventProcessor} can not support the
	 *             {@link Filter} of the provided {@link Subscription}.
	 */
	public String createSubscription(Subscription subscription)
			throws InvalidSubscriptionException;

	/**
	 * Register a {@link Subscription} with this {@link CatalogFramework}.
	 * <p>
	 * <b>Note:</b> <em>{@link Subscription}s are transient, not durable,
	 * i.e., only maintained in memory and will be lost if DDF is shutdown.</em><br/>
	 * Durability must be implemented by the client bundle (typically an
	 * endpoint - refer to {@link DurableSubscription} for an example).
	 * </p>
	 * 
	 * <p>
	 * <b>Note:</b>
	 * <em>A {@link Subscription} can also be registered using the whiteboard model:</em>
	 * <br/>
	 * To register a {@link Subscription} via the whiteboard model, simply
	 * register any implementation of {@link Subscription} with OSGi registry
	 * under the interface {@link Subscription}. To unregister the
	 * {@link Subscription}, simply unregister the service that was originally
	 * registered. {@link Subscription}s registered in this manner do not get a
	 * subscription id and cannot be unsubscribed via the {@link EventProcessor}
	 * </p>
	 * 
	 * Implementations of this method <em>must</em> call the
	 * {@link PreSubscriptionPlugin#process(Subscription)} method for each
	 * 
	 * @param subscription the {@link Subscription} to register
	 * @param subscriptionId the desired id
	 * @throws InvalidSubscriptionException if this {@link EventProcessor} can not support the
	 *             {@link Filter} of the provided {@link Subscription}.
	 * @throws SubscriptionExistsException if a subscription with this ID already exists
	 */
	public void createSubscription(Subscription subscription,
			String subscriptionId) throws InvalidSubscriptionException,
			SubscriptionExistsException;

	/**
	 * Updates the subscription associated with the given id.
	 * 
	 * @param subscription the subscription to update
	 * @param subcriptionId the subscription id to identify the subscription
	 * @throws SubscriptionNotFoundException if the subscription was not found
	 */
	public void updateSubscription(Subscription subscription,
			String subcriptionId) throws SubscriptionNotFoundException;

	/**
	 * Deletes the subscription associated with the given id.
	 * 
	 * @param user deleting the subscription
	 * @param subscriptionId the subscription id to identify the subscription to delete
	 * @throws SubscriptionNotFoundException if the subscription was not found
	 */
	public void deleteSubscription(String subscriptionId)
			throws SubscriptionNotFoundException;

	/**
	 * Notify this {@code EventProcessor} that a {@link Metacard} (or
	 * equivalent) has been created in a {@link RemoteSource}.
	 * 
	 * <p>
	 * <b>Implementations of this method must:</b>
	 * <ol>
	 * <li>Call the {@link PreDeliveryPlugin#processCreate(Metacard)} method for
	 * all of the registered {@link PreDeliveryPlugin}s
	 * <li>Call the {@link DeliveryMethod#created(Metacard)} method of the {@link DeliveryMethod} 
	 * of matching {@link Subscription}s with the new {@link Metacard} created.
	 * </ol>
	 * </p>
	 * 
	 * @param newMetacard the newly created {@link Metacard}
	 * 
	 * @see RemoteSource
	 * @see FederatedSource
	 * @see ConnectedSource
	 */
	public void notifyCreated(Metacard newMetacard);

	/**
	 * Notify this {@code EventProcessor} that a {@link Metacard} (or
	 * equivalent) has been updated in a {@link RemoteSource}.
	 * 
	 * <p>
	 * <b>Implementations of this method must:</b>
	 * <ol>
	 * <li/>Call {@link PreDeliveryPlugin#processUpdateHit(Update)} method of
	 * all registered {@link PreDeliveryPlugin}s when a {@link Metacard} has 
	 * been updated and the new version matches a {@link Subscription}
     * <li/>Call {@link PreDeliveryPlugin#processUpdateMiss(Update)} method of
     * all registered {@link PreDeliveryPlugin}s when a {@link Metacard} has 
     * been updated and the new version matches a {@link Subscription} but the 
     * old version does not
	 * <li>Call all registered implementations of {@link DeliveryMethod} with
	 * the updated {@link Metacard}.
	 * </ol>
	 * </p>
	 * 
	 * @param newMetacard the new version of the {@link Metacard}
	 * @param oldMetacard the previous version of the {@link Metacard} (optional, 
	 * pass {@code null} if not relevant)
	 * 
     * @see FederatedSource
     * @see RemoteSource
     * @see ConnectedSource
	 */
	public void notifyUpdated(Metacard newMetacard, Metacard oldMetacard);

	/**
	 * Notify this {@link EventProcessor} that a {@link Metacard} (or
	 * equivalent) has been deleted in a {@link RemoteSource}.
	 * 
	 * <p>
	 * <b>Implementations of this method must:</b>
	 * <li>If possible (that is, the {@link Metacard} is fully populated) call all
	 * active matching {@link Subscription}s associated
	 * {@link DeliveryMethod#deleted(Metacard)} with the {@link Metacard} that
	 * was deleted.
	 * <li>If impossible (this is, the {@link Metacard} only has
	 * {@link Metacard#getId()}, call all active {@link Subscription}s. </ol>
	 * </p>
	 * 
	 * @param oldMetacard the deleted {@link Metacard}
	 * 
	 * @see RemoteSource
	 * @see FederatedSource
	 * @see ConnectedSource
	 */
	public void notifyDeleted(Metacard oldMetacard);

}
