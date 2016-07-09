/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.data;

import java.io.Serializable;
import java.util.Set;

/**
 * Used to register a default attribute values that apply to all metacard types and default attribute values
 * that apply to specific metacard types.
 * <p>
 * Default attribute values are separated into two categories:
 * <ol>
 * <li>'Global' default values (default values that are tied to an attribute but not to a
 * specific metacard type)</li>
 * <li>Default values that apply to specific metacard types</li>
 * </ol>
 * In the event that an attribute has a 'global' default value as well as a default value for a
 * specific metacard type, the default value for the specific metacard type will be returned (i.e.,
 * the more specific default value wins).
 *
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface DefaultAttributeValue {

    /**
     * Returns the metacard types of the registered DefaultAttribute will be empty in the case of
     * global attributes
     */
    Set<String> getMetacardTypeNames();

    /**
     * Returns the attribute name of the registered DefaultAttribute should never be null
     */
    String getAttributeName();

    /**
     * Returns the default attribute value of the registered DefaultAttribute should never be null
     */
    Serializable getDefaultValue();
}
