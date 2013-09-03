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

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.source.CatalogProvider;

/**
 * A request to delete {@link Metacard}s from the {@link CatalogProvider}
 * 
 * @author ddf.isgs@lmco.com
 * 
 */
public interface DeleteRequest extends Request {

    public static final String DELETE_BY_ID = Metacard.ID;
    public static final String DELETE_BY_PRODUCT_URI = Metacard.RESOURCE_URI;
	
	/**
	 * The attribute name tells the {@link CatalogProvider} implementer what
	 * type of attribute values are in the ({@link #getAttributeValues()})
	 * {@code List}. For instance, if the attribute name was "id," then the
	 * {@link CatalogProvider} implementer would know that the attribute values
	 * in the list were id values (such as
	 * {@code 575aa9625fa24b338bd3c439f2613709}).
	 * 
	 * @return the attribute name, the attribute name must correspond to an
	 *         {@link Attribute} that has an {@link AttributeFormat} of
	 *         {@link AttributeFormat#STRING}
	 */
	public String getAttributeName();

	/**
	 * Describes which {@link Metacard}s to delete.
	 * @return the values
	 */
	public List<? extends Serializable> getAttributeValues();
}
