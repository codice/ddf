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

/**
 * 
 * Qualified and uniquely identified set of {@link AttributeDescriptor}s used to
 * specify and describe the valid {@link Attribute}s on a {@link Metacard}.
 * 
 * @author ianbarnett
 * @author ddf.isgs@lmco.com
 * 
 */
public interface QualifiedMetacardType extends MetacardType {

    /**
     * Default {@link MetacardType} namespace. This is used if no namespace is
     * defined on construction.
     */
    public static final String DEFAULT_METACARD_TYPE_NAMESPACE = "";
    
    /**
     * 
     * @return namespace that qualifies the {@link MetacardType} name
     */
    public String getNamespace();
}
