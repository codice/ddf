/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.policy.context.attributes;

import ddf.security.permission.KeyValuePermission;

/**
 * Mapping between an attribute name and the String permission value for that attribute.
 *
 * <p>It is up to the implementer to determine how to parse the String value into valid Shiro
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
   * Gets the attribute value for this mapping. This should return the raw, un-parsed value.
   *
   * @return attribute value
   */
  public String getAttributeValue();

  /**
   * Returns a {@link ddf.security.permission.KeyValuePermission} object that has been created from
   * the parsed attribute value.
   *
   * @return permission
   */
  public KeyValuePermission getAttributePermission();

  /**
   * Returns the context of the attribute
   *
   * @return context
   */
  public String getContext();
}
