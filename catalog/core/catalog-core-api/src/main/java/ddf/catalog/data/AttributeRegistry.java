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

import java.util.Optional;

/**
 * Manages registered attribute types.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface AttributeRegistry {
  /**
   * Registers a new attribute.
   *
   * @param attributeDescriptor the {@link AttributeDescriptor} describing the attribute
   * @throws IllegalArgumentException if {@code attributeDescriptor} or {@link
   *     AttributeDescriptor#getName()} is null
   */
  void register(AttributeDescriptor attributeDescriptor);

  /**
   * Removes an attribute from the registry.
   *
   * <p>Does nothing if the attributeDescriptor does not exist in the registry.
   *
   * @param attributeDescriptor an attributeDescriptor for the attribute
   * @throws IllegalArgumentException if {@code attributeDescriptor} is null
   */
  void deregister(AttributeDescriptor attributeDescriptor);

  /**
   * Gets the {@link AttributeDescriptor} for the attribute with the given name.
   *
   * <p>Returns an empty {@link Optional} if no attribute by the name {@code name} exists in the
   * registry.
   *
   * @param name the name of the attribute
   * @return an {@link Optional} containing the registered {@link AttributeDescriptor}
   * @throws IllegalArgumentException if {@code name} is null
   */
  Optional<AttributeDescriptor> lookup(String name);
}
