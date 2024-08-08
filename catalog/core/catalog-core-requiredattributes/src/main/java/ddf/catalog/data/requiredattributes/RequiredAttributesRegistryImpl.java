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

import ddf.catalog.data.RequiredAttributes;
import ddf.catalog.data.RequiredAttributesRegistry;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequiredAttributesRegistryImpl implements RequiredAttributesRegistry {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(RequiredAttributesRegistryImpl.class);
  private final Map<String, Set<String>> requiredAttributesMap = new ConcurrentHashMap<>();

  @Override
  public Set<String> getRequiredAttributes(String metacardTypeName) {
    notNull(metacardTypeName, "The metacard type name cannot be null.");
    return requiredAttributesMap.getOrDefault(metacardTypeName, Collections.emptySet());
  }

  @Override
  public boolean isRequired(String metacardTypeName, String attributeName) {
    notNull(metacardTypeName, "The metacard type name cannot be null.");
    notNull(attributeName, "The attribute name cannot be null.");

    return requiredAttributesMap
        .getOrDefault(metacardTypeName, Collections.emptySet())
        .contains(attributeName);
  }

  /**
   * Adds a new {@code RequiredAttributes} to the {@code requiredAttributesMap} map. Called by
   * blueprint when a new {@code RequiredAttributes} is registered as a service.
   *
   * @param requiredAttributes the new {@code RequiredAttributes} to be registered.
   */
  public void bind(RequiredAttributes requiredAttributes) {
    if (requiredAttributes != null) {
      String metacardType = requiredAttributes.getMetacardType();
      LOGGER.trace("Binding new RequiredAttributes instance for metacard type {}", metacardType);
      requiredAttributesMap.compute(
          metacardType,
          (name, attributes) -> {
            if (attributes == null) {
              return requiredAttributes.getRequiredAttributes();
            } else {
              attributes.addAll(requiredAttributes.getRequiredAttributes());
              return attributes;
            }
          });
    }
  }

  /**
   * Removes an existing {@code RequiredAttributes} from the {@code requiredAttributesMap} map.
   * Called by blueprint when an existing {@code RequiredAttributes} service is removed.
   *
   * @param requiredAttributes the {@code RequiredAttributes} to be removed from the collection.
   */
  public void unbind(RequiredAttributes requiredAttributes) {
    if (requiredAttributes != null) {
      String metacardType = requiredAttributes.getMetacardType();
      LOGGER.trace("Unbinding RequiredAttributes instance for metacard type {}", metacardType);
      requiredAttributesMap.remove(metacardType);
    }
  }
}
