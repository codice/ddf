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
package ddf.catalog.operation;

import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.source.CatalogProvider;

/**
 * Interface representing a request to update a Metacard.  Requests may be
 * batched, but each Metacard/Attribute pair will only update a single Metacard.
 * 
 * @see CatalogProvider
 * @author ddf.isgs@lmco.com
 * 
 */
public interface UpdateRequest extends Request {

    /**
     * Shortcut for the {@link Attribute} name used for updating {@link Metacard}s by ID.
     * @see Metacard#ID
     */
    public static final String UPDATE_BY_ID = Metacard.ID;

    /**
     * Shortcut for the {@link Attribute} name used for updating {@link Metacard}s by the {@link String} value of the {@link Metacard}'s {@link Metacard#RESOURCE_URI Resource URI}.
     * @see Metacard#RESOURCE_URI
     */
    public static final String UPDATE_BY_PRODUCT_URI = Metacard.RESOURCE_URI;

    /**
	 * The attribute name tells the {@link CatalogProvider} what type of
	 * attribute values are the {@link Entry} keys in the update list. For
	 * instance, if the attribute name was "id," then the
	 * {@link CatalogProvider} would know that the {@link Entry} keys in the
	 * update list ( {@code  List<Entry<Serializable, Metacard>>}) were id values
	 * (such as {@code 575aa9625fa24b338bd3c439f2613709}) and could create the
	 * appropriate backend search.
	 * 
	 * @return the name of the attribute, the attribute name must correspond to
	 *         an {@link Attribute} that has an AttributeFormat of
	 *         {@link AttributeFormat#STRING}
	 */
	public String getAttributeName();

    /**
	 * Get the updates to be made. Any {@link Metacard}s that have an
	 * {@link Attribute#getName()} that matches the name returned by
	 * {@link #getAttributeName()} and a {@link Attribute#getValue()} that
	 * matches the {@link Serializable} value in an {@link Entry} of this
	 * {@link List} will be updated with the value of the associated
	 * {@link Metacard} in the {@link List}. An entry's key in the {@code List}
	 * must match zero metacards or one metacard in the Source.
	 * 
	 * @return List - pairs of {@link Attribute} values and associated new
	 *         {@link Metacard}s to update if a match is found.
	 */
    public List<Entry<Serializable, Metacard>> getUpdates();
}
