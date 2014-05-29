/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.security.policy.context.attributes;

import ddf.security.permission.CollectionPermission;

/**
 * Mapping between an attribute name and the String permission value for that attribute.
 *
 * It is up to the implementer to determine how to parse the String value into valid Shiro
 * permission objects.
 */
public interface ContextAttributeMapping {

    /**
     * Returns the attribute name associated with this mapping.
     *
     * @return attribute name
     */
    public String getAttributeName();

    /**
     * Sets the attribute name for this mapping
     *
     * @param name
     */
    public void setAttributeName(String name);

    /**
     * Gets the attribute value for this mapping. This should return the raw, un-parsed value.
     *
     * @return attribute value
     */
    public String getAttributeValue();

    /**
     * Sets the attribute value for this mapping.
     *
     * @param value
     */
    public void setAttributeValue(String value);

    /**
     * Returns a {@link ddf.security.permission.CollectionPermission} object that has been created
     * from the parsed attribute value.
     *
     * @return permission
     */
    public CollectionPermission getAttributePermission();
}
