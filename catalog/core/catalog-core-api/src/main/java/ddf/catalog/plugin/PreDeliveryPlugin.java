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
package ddf.catalog.plugin;

import ddf.catalog.data.Metacard;
import ddf.catalog.event.DeliveryMethod;
import ddf.catalog.event.Subscription;
import ddf.catalog.operation.Update;

/**
 * The PreDeliveryPlugin is executed prior to an event being delivered.
 * 
 * @author michael.menousek@lmco.com
 * 
 */
public interface PreDeliveryPlugin {

	/**
	 * Callback invoked when a {@link Metacard} has created
	 * 
	 * @param metacard
	 *            - the newly created {@link Metacard}
	 * @return the value of the {@link Metacard} to pass to the next
	 *         {@link PreDeliveryPlugin}, or if this is the last
	 *         {@link PreDeliveryPlugin} to be called, the
	 *         {@link DeliveryMethod}
	 * @throws PluginExecutionException
	 *             if there is a problem evaluating the update but processing
	 *             should continue
	 * @throws StopProcessingException
	 *             if this Catalog Plugin will not allow the current
	 *             notification to occur
	 */
	public Metacard processCreate(Metacard metacard)
			throws PluginExecutionException, StopProcessingException;

	/**
	 * Callback invoked when a {@link Metacard} has been updated and only the
	 * old version matches a {@link Subscription}
	 * 
	 * @param update
	 * @return the {@link Update} to pass to the next {@link PreDeliveryPlugin},
	 *         or if this is the last {@link PreDeliveryPlugin} to be called,
	 *         the {@link DeliveryMethod}
	 * @throws PluginExecutionException
	 *             if there is a problem evaluating the update but processing
	 *             should continue
	 * @throws StopProcessingException
	 *             if this Catalog Plugin will not allow the current
	 *             notification to occur
	 * 
	 */
	public Update processUpdateMiss(Update update)
			throws PluginExecutionException, StopProcessingException;

	/**
	 * Callback invoked when a {@link Metacard} has been updated and the new
	 * version matches a {@link Subscription}. The old version may or may not
	 * match the {@link Subscription}
	 * 
	 * @param update
	 * @return the {@link Update} to pass to the next {@link PreDeliveryPlugin},
	 *         or if this is the last {@link PreDeliveryPlugin} to be called,
	 *         the {@link DeliveryMethod}
	 * @throws PluginExecutionException
	 *             if there is a problem evaluating the update but processing
	 *             should continue
	 * @throws StopProcessingException
	 *             if this Catalog Plugin will not allow the current
	 *             notification to occur
	 */
	public Update processUpdateHit(Update update)
			throws PluginExecutionException, StopProcessingException;

	/**
	 * Callback invoked when a {@link Metacard} has been deleted
	 * 
	 * @param metacard
	 *            - the deleted {@link Metacard}
	 * @return the {@link Metacard} to pass to the next
	 *         {@link PreDeliveryPlugin}, or if this is the last
	 *         {@link PreDeliveryPlugin} to be called, the
	 *         {@link DeliveryMethod}
	 * @throws PluginExecutionException
	 *             if there is a problem evaluating the update but processing
	 *             should continue
	 * @throws StopProcessingException
	 *             if this Catalog Plugin will not allow the current
	 *             notification to occur
	 */
	public Metacard processDelete(Metacard metacard)
			throws PluginExecutionException, StopProcessingException;
}
