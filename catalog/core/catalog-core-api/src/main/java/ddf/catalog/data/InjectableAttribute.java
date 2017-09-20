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
package ddf.catalog.data;

import java.util.Set;

/**
 * Describes an 'injectable' attribute (i.e., an {@link AttributeDescriptor} that should be injected
 * into specific {@link MetacardType}s or into all {@link MetacardType}s).
 *
 * <p><b>This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library.</b>
 */
public interface InjectableAttribute {
  /**
   * Returns the name of the {@link AttributeDescriptor} to be injected.
   *
   * @return the name of the {@link AttributeDescriptor} to be injected
   */
  String attribute();

  /**
   * Returns the names of the {@link MetacardType}s into which this attribute should be injected.
   *
   * <p>Should return an empty set if this attribute should be injected into all {@link
   * MetacardType}s.
   *
   * @return the names of the {@link MetacardType}s into which this attribute should be injected, or
   *     an empty set if this attribute should be injected into all {@link MetacardType}s
   */
  Set<String> metacardTypes();
}
