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

import ddf.catalog.data.Metacard;

/**
 * The DeliveryMethod provides the operation (created, updated, deleted) of how
 * a {@link Metacard} can be delivered.
 * 
 * @author ddf.isgs@lmco.com
 * 
 * @see Subscription
 */
public interface DeliveryMethod {

	/**
	 * This method will determine how to handle a {@link Metacard} when it is
	 * created/ingested.
	 * 
	 * @param metacard the {@link Metacard} that was ingested
	 */
	public void created(Metacard newMetacard);

	/**
	 * This method will determine how to handle a {@link Metacard} when it is updated.
	 * 
	 * @param newMetacard the {@link Metacard} after the update
	 * @param oldMetacard the {@link Metacard} before the updated
	 */
	public void updatedHit(Metacard newMetacard, Metacard oldMetacard);

	/**
	 * This method will determine how to handle a {@link Metacard} when it is updated.
	 * 
	 * @param newMetacard the {@link Metacard} after the update
     * @param oldMetacard the {@link Metacard} before the updated
	 */
	public void updatedMiss(Metacard newMetacard, Metacard oldMetacard);

	/**
	 * This method will determine how to handle a {@link Metacard} when it is deleted.
	 * 
	 * @param metacard the {@link Metacard} that was deleted
	 */
	public void deleted(Metacard oldMetacard);

}
