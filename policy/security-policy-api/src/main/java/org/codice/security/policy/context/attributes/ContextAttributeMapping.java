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
package org.codice.security.policy.context.attributes;

import ddf.security.permission.CollectionPermission;

/**
 * Mapping between an attribute name and the String permission value for that attribute.
 *
 * It is up to the implementer to determine how to parse the String value into valid Shiro
 * permission objects.
 */
public interface ContextAttributeMapping {

    public String getAttributeName();

    public void setAttributeName(String name);

    public String getAttributeValue();

    public void setAttributeValue(String value);

    public CollectionPermission getAttributePermission();
}
