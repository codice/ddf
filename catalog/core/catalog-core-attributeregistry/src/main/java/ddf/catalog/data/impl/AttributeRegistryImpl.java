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

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.MetacardType;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeRegistryImpl implements AttributeRegistry {
  private static final Logger LOGGER = LoggerFactory.getLogger(AttributeRegistryImpl.class);

  private final ListMultimap<String, AttributeDescriptor> attributeMap =
      Multimaps.synchronizedListMultimap(LinkedListMultimap.create());

  @Override
  public void register(final AttributeDescriptor attributeDescriptor) {
    notNull(attributeDescriptor, "The attribute descriptor cannot be null.");
    notNull(attributeDescriptor.getName(), "The attribute name cannot be null.");
    attributeMap.put(attributeDescriptor.getName(), attributeDescriptor);
  }

  @Override
  public void deregister(final AttributeDescriptor attributeDescriptor) {
    notNull(attributeDescriptor, "The attribute descriptor cannot be null.");
    notNull(attributeDescriptor.getName(), "The attribute name cannot be null.");
    attributeMap.remove(attributeDescriptor.getName(), attributeDescriptor);
  }

  @Override
  public Optional<AttributeDescriptor> lookup(final String name) {
    notNull(name, "The attribute name cannot be null.");
    return attributeMap.get(name).stream().findFirst();
  }

  public void registerMetacardType(MetacardType metacardType) {
    if (metacardType != null) {
      registerMetacardTypeAttributes(metacardType);
    }
  }

  public void deregisterMetacardType(MetacardType metacardType) {
    if (metacardType != null) {
      deregisterMetacardTypeAttributes(metacardType);
    }
  }

  private void registerMetacardTypeAttributes(MetacardType metacardType) {
    LOGGER.debug("Adding attributes from {} metacard type", metacardType.getName());
    metacardType.getAttributeDescriptors().stream()
        .filter(attributeDescriptor -> attributeDescriptor != null)
        .filter(attributeDescriptor -> attributeDescriptor.getName() != null)
        .forEach(this::register);
  }

  private void deregisterMetacardTypeAttributes(MetacardType metacardType) {
    LOGGER.debug("Removing attributes from {} metacard type", metacardType.getName());
    metacardType.getAttributeDescriptors().stream()
        .filter(attributeDescriptor -> attributeDescriptor != null)
        .filter(attributeDescriptor -> attributeDescriptor.getName() != null)
        .forEach(this::deregister);
  }
}
