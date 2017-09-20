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
package org.codice.ddf.catalog.ui.metacard.workspace;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.types.AssociationsAttributes;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.SecurityAttributes;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class WorkspaceMetacardType implements MetacardType {

  private static final Set<AttributeDescriptor> ATTRIBUTE_DESCRIPTORS =
      ImmutableSet.of(
              new CoreAttributes(),
              new SecurityAttributes(),
              new WorkspaceAttributes(),
              new AssociationsAttributes())
          .stream()
          .map(MetacardType::getAttributeDescriptors)
          .flatMap(Collection::stream)
          .collect(Collectors.toSet());

  @Override
  public String getName() {
    return WorkspaceAttributes.WORKSPACE_TAG;
  }

  @Override
  public Set<AttributeDescriptor> getAttributeDescriptors() {
    return ATTRIBUTE_DESCRIPTORS;
  }

  @Override
  public AttributeDescriptor getAttributeDescriptor(String s) {
    for (AttributeDescriptor attributeDescriptor : ATTRIBUTE_DESCRIPTORS) {
      if (attributeDescriptor.getName().equals(s)) {
        return attributeDescriptor;
      }
    }
    return null;
  }
}
