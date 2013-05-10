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
import java.util.Collections;
import java.util.Set;

/**
 * This class describes the set of attributes a {@link Metacard} can contain.
 * 
 * @author Michael Menousek
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public interface MetacardType extends Serializable {

	/**
     * Default name of the default {@link MetacardType}
	 */
    String DEFAULT_METACARD_TYPE_NAME = "ddf.metacard";

	/**
	 * Gets the name of this {@code MetacardType}. A MetacardType name must be
	 * unique. Two separate MetacardType objects that do not have the same set
	 * of AttributeDescriptors should not have the same name.
	 * 
	 * @return the name of this {@link MetacardType}, default is
	 *         {@link MetacardType#DEFAULT_METACARD_TYPE_NAME};
	 */
    public String getName();

	/**
     * Returns the unmodifiable {@link Set} of {@link AttributeDescriptor}s for
     * this {@link MetacardType}.
	 * 
     * @see Collections#unmodifiableSet(Set)
     * @return
	 */
    public Set<AttributeDescriptor> getAttributeDescriptors();

	/**
     * Get the {@link AttributeDescriptor} for a particular {@link Attribute} name.
     * @param attributeName
     * @return the {@link AttributeDescriptor}, <code>null</code> if not found.
	 */
    public AttributeDescriptor getAttributeDescriptor(String attributeName);

}
