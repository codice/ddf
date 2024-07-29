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
package ddf.catalog.data.requiredattributes;

import static org.apache.commons.lang.Validate.notNull;

import ddf.catalog.data.RequiredAttributesRegistry;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RequiredAttributesRegistryImpl implements RequiredAttributesRegistry {
  private final Map<String, Set<String>> requiredAttributesMap = new ConcurrentHashMap<>();

  @Override
  public void addRequiredAttribute(String metacardTypeName, String attributeName) {
    notNull(metacardTypeName, "The metacard type name cannot be null.");
    notNull(attributeName, "The attribute name cannot be null.");

    requiredAttributesMap.compute(
        metacardTypeName,
        (name, requiredAttributes) -> {
          if (requiredAttributes == null) {
            final Set<String> newAttributes = new HashSet<>();
            newAttributes.add(attributeName);
            return newAttributes;
          } else {
            requiredAttributes.add(attributeName);
            return requiredAttributes;
          }
        });
  }

  @Override
  public void addRequiredAttributes(String metacardTypeName, Set<String> attributeNames) {
    requiredAttributesMap.compute(
        metacardTypeName,
        (name, requiredAttributes) -> {
          if (requiredAttributes == null) {
            return attributeNames;
          } else {
            requiredAttributes.addAll(attributeNames);
            return requiredAttributes;
          }
        });
  }

  @Override
  public boolean isRequired(String metacardTypeName, String attributeName) {
    notNull(metacardTypeName, "The metacard type name cannot be null.");
    notNull(attributeName, "The attribute name cannot be null.");

    return requiredAttributesMap
        .getOrDefault(metacardTypeName, Collections.emptySet())
        .contains(attributeName);
  }

  @Override
  public void removeRequiredAttribute(String metacardTypeName, String attributeName) {
    notNull(metacardTypeName, "The metacard type name cannot be null.");
    notNull(attributeName, "The attribute name cannot be null.");

    requiredAttributesMap.computeIfPresent(
        metacardTypeName,
        (name, requiredAttributes) -> {
          requiredAttributes.remove(attributeName);
          return requiredAttributes;
        });
  }

  @Override
  public void removeRequiredAttributes(String metacardTypeName) {
    notNull(metacardTypeName, "The metacard type name cannot be null.");

    requiredAttributesMap.remove(metacardTypeName);
  }
}
