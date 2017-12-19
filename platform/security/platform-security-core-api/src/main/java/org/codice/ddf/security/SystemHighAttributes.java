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
package org.codice.ddf.security;

import java.util.Set;

/**
 * Service to retrieve system high attribute values.
 *
 * <p>This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.
 */
public interface SystemHighAttributes {

  /**
   * @return a singleton {@link Set<String>} if the attribute has single value, a multi-valued
   *     {@link Set<String>} if the attribute has multiple values, or an empty {@link Set<String>}
   *     if the attribute is not present
   */
  Set<String> getValues(final String attributeName);
}
