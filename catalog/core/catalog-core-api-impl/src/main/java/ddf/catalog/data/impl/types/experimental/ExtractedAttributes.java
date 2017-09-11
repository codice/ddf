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
package ddf.catalog.data.impl.types.experimental;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.types.experimental.Extracted;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ExtractedAttributes implements Extracted, MetacardType {
  private static final Set<AttributeDescriptor> DESCRIPTORS;

  static {
    Set<AttributeDescriptor> descriptors = new HashSet<>();
    descriptors.add(
        new AttributeDescriptorImpl(
            EXTRACTED_TEXT,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    DESCRIPTORS = Collections.unmodifiableSet(descriptors);
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Set<AttributeDescriptor> getAttributeDescriptors() {
    return DESCRIPTORS;
  }

  @Override
  public AttributeDescriptor getAttributeDescriptor(String attributeName) {
    for (AttributeDescriptor attributeDescriptor : DESCRIPTORS) {
      if (attributeDescriptor.getName().equals(attributeName)) {
        return attributeDescriptor;
      }
    }
    return null;
  }
}
