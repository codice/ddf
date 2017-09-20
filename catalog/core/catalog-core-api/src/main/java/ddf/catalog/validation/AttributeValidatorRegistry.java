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
package ddf.catalog.validation;

import java.util.Set;

/**
 * Manages registered attribute validators.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface AttributeValidatorRegistry {
  /**
   * Registers the given {@link AttributeValidator}(s) to the given attribute name.
   *
   * <p>This method doesn't care whether an attribute with the given name actually exists; it is
   * only responsible for managing the validators associated with a given attribute name.
   *
   * <p>Note that the {@link AttributeValidator} implementations registered should implement {@link
   * Object#hashCode()} and {@link Object#equals(Object)} so that duplicate validators aren't
   * registered for an attribute.
   *
   * @param attributeName the name of the attribute that the given validators will validate
   * @param validators the {@link AttributeValidator}s to apply to the attribute
   * @throws IllegalArgumentException if {@code attributeName} is null, or if no {@link
   *     AttributeValidator}s were passed
   */
  void registerValidators(String attributeName, Set<? extends AttributeValidator> validators);

  /**
   * Deregisters all {@link AttributeValidator}s associated with the given attribute name.
   *
   * <p>Does nothing if there are no validators registered to the given attribute name.
   *
   * @param attributeName the name of the attribute whose validators will be deregistered, cannot be
   *     null
   * @throws IllegalArgumentException if {@code attributeName} is null
   */
  void deregisterValidators(String attributeName);

  /**
   * Gets all the {@link AttributeValidator}s associated with the given attribute name.
   *
   * @param attributeName the name of the attribute, cannot be null
   * @return the set of {@link AttributeValidator}s registered to the attribute, or an empty set if
   *     no attribute by that name exists.
   * @throws IllegalArgumentException if {@code attributeName} is null
   */
  Set<AttributeValidator> getValidators(String attributeName);
}
