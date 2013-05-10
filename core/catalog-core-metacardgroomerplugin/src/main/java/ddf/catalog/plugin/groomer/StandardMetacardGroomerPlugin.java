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
package ddf.catalog.plugin.groomer;

import java.io.Serializable;
import java.util.Date;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.log4j.Logger;

import ddf.catalog.data.AttributeImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.groomer.AbstractMetacardGroomerPlugin;

/**
 * Applies general Create and Update grooming rules such as populating the
 * {@link Metacard#ID}, {@link Metacard#MODIFIED}, and {@link Metacard#CREATED}
 * fields.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class StandardMetacardGroomerPlugin extends AbstractMetacardGroomerPlugin {

	private static final Logger LOGGER = Logger.getLogger(StandardMetacardGroomerPlugin.class);

	protected void applyCreatedOperationRules(CreateRequest createRequest, Metacard aMetacard, Date now) {
		LOGGER.debug("Applying standard rules on CreateRequest");
		aMetacard.setAttribute(new AttributeImpl(Metacard.ID, UUID.randomUUID().toString().replaceAll("-", "")));
		aMetacard.setAttribute(new AttributeImpl(Metacard.CREATED, now));
		aMetacard.setAttribute(new AttributeImpl(Metacard.MODIFIED, now));

	}

	protected void applyUpdateOperationRules(UpdateRequest updateRequest, Entry<Serializable, Metacard> anUpdate,
			Metacard aMetacard, Date now) {
		aMetacard.setAttribute(new AttributeImpl(Metacard.MODIFIED, now));

		if (UpdateRequest.UPDATE_BY_ID.equals(updateRequest.getAttributeName())
				&& !anUpdate.getKey().toString().equals(aMetacard.getId())) {

			LOGGER.info(Metacard.ID + " in metacard must match the Update " + Metacard.ID + ", overwriting metacard "
					+ Metacard.ID + " [" + aMetacard.getId() + "] with the update identifier [" + anUpdate.getKey()
					+ "]");
			aMetacard.setAttribute(new AttributeImpl(Metacard.ID, anUpdate.getKey()));

		}

		if (aMetacard.getCreatedDate() == null) {
			LOGGER.info(Metacard.CREATED
					+ " date should match the original metacard. Changing date to current timestamp so it is at least not null.");
			aMetacard.setAttribute(new AttributeImpl(Metacard.CREATED, now));
		}

	}

}
