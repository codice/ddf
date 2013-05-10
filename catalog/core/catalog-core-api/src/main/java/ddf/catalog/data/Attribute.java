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
package ddf.catalog.data;

import java.io.Serializable;
import java.util.List;

/**
 * An instance of an {@link AttributeType}.
 * 
 * @author ddf.isgs@lmco.com
 * 
 * @see AttributeType
 */
public interface Attribute extends Serializable{

	/**
	 * The name of this {@link Attribute}.
	 * 
	 * @return {@link String} - the name of this {@link Attribute}
	 */
	public String getName();

	/**
	 * The value of this {@link Attribute}.
	 * <p>
	 * If this {@link Attribute} is multivalued (
	 * {@link AttributeDescriptor#isMultiValued()}==true), this method must
	 * return the first, if any, value as an instance of the Class identified in
	 * the associated {@link AttributeDescriptor#getClass()} method.
	 * </p>
	 * 
	 * @return {@link Serializable} - the value of this {@link Attribute}
	 */
	public Serializable getValue();

	/**
	 * The value(s) of this {@link Attribute} as a {@link List}.
	 * <p>
	 * If this {@link Attribute} is <em>NOT</em> multivalued, this method must
	 * return either null or a {@link List} with 0 or 1 entries depending on whether the value
	 * is null or not.
	 * </p>
	 * 
	 * @return the value(s) of this {@link Attribute} as a {@link List}
	 */
	public List<Serializable> getValues();

}