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
package ddf.catalog.validation.impl;

import com.google.common.base.Preconditions;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.AttributeValidatorRegistry;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.collections.CollectionUtils;

public class AttributeValidatorRegistryImpl implements AttributeValidatorRegistry {
  private final Map<String, Set<AttributeValidator>> attributeValidatorMap =
      new ConcurrentHashMap<>();

  private static final String NULL_ATTR_NAME_MSG = "The attribute name cannot be null.";

  @Override
  public void registerValidators(
      final String attributeName, final Set<? extends AttributeValidator> validators) {
    Preconditions.checkArgument(attributeName != null, NULL_ATTR_NAME_MSG);
    Preconditions.checkArgument(
        CollectionUtils.isNotEmpty(validators), "Must register at least one validator.");

    attributeValidatorMap.compute(
        attributeName,
        (name, registeredValidators) -> {
          if (registeredValidators == null) {
            return new HashSet<>(validators);
          } else {
            registeredValidators.addAll(validators);
            return registeredValidators;
          }
        });
  }

  @Override
  public void deregisterValidators(final String attributeName) {
    Preconditions.checkArgument(attributeName != null, NULL_ATTR_NAME_MSG);

    attributeValidatorMap.remove(attributeName);
  }

  @Override
  public Set<AttributeValidator> getValidators(final String attributeName) {
    Preconditions.checkArgument(attributeName != null, NULL_ATTR_NAME_MSG);

    return Collections.unmodifiableSet(
        attributeValidatorMap.getOrDefault(attributeName, Collections.emptySet()));
  }
}
