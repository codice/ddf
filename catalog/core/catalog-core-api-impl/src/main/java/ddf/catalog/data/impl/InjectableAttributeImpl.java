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
package ddf.catalog.data.impl;

import static org.apache.commons.lang.Validate.notNull;

import ddf.catalog.data.InjectableAttribute;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

public class InjectableAttributeImpl implements InjectableAttribute {
  private final String attribute;

  private final Set<String> metacardTypes = new HashSet<>();

  /**
   * Constructs an {@link InjectableAttributeImpl} with the given attribute name and metacard type
   * names.
   *
   * <p>If this attribute should be injected into all {@link ddf.catalog.data.MetacardType}s, then
   * {@code metacardTypes} should be null or empty.
   *
   * @param attribute the name of the attribute that this {@code InjectableAttributeImpl}
   *     represents, cannot be null
   * @param metacardTypes the names of the {@link ddf.catalog.data.MetacardType}s into which the
   *     attribute named {@code attribute} should be injected, can be null
   * @throws IllegalArgumentException if {@code attribute} is null
   */
  public InjectableAttributeImpl(String attribute, @Nullable Collection<String> metacardTypes) {
    notNull(attribute, "The attribute name cannot be null.");
    this.attribute = attribute;

    Optional.ofNullable(metacardTypes).ifPresent(this.metacardTypes::addAll);
  }

  @Override
  public String attribute() {
    return attribute;
  }

  @Override
  public Set<String> metacardTypes() {
    return Collections.unmodifiableSet(metacardTypes);
  }
}
