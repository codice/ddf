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
package org.codice.ddf.transformer.xml.streaming.lib;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import java.util.Set;

public class DynamicMetacardType implements MetacardType {
  private Set<AttributeDescriptor> attributeDescriptors;

  String name;

  public DynamicMetacardType(Set<AttributeDescriptor> attDesc, String name) {
    this.attributeDescriptors = attDesc;
    this.name = name + ".metacard";
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Set<AttributeDescriptor> getAttributeDescriptors() {
    return attributeDescriptors;
  }

  @Override
  public AttributeDescriptor getAttributeDescriptor(String attributeName) {
    return attributeDescriptors.stream()
        .filter(p -> p.getName().equals(attributeName))
        .findFirst()
        .orElse(null);
  }
}
