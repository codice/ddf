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
package org.codice.ddf.catalog.ui.metacard.notes;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NoteAttributes implements MetacardType {

  private static final Set<AttributeDescriptor> DESCRIPTORS;

  private static final String NAME = "note";

  static {
    Set<AttributeDescriptor> descriptors = new HashSet<>();
    descriptors.add(
        new AttributeDescriptorImpl(
            NoteConstants.PARENT_ID,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            NoteConstants.COMMENT,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            true /* multivalued */,
            BasicTypes.STRING_TYPE));
    DESCRIPTORS = Collections.unmodifiableSet(descriptors);
  }

  @Override
  public Set<AttributeDescriptor> getAttributeDescriptors() {
    return DESCRIPTORS;
  }

  @Override
  public AttributeDescriptor getAttributeDescriptor(String name) {
    for (AttributeDescriptor attributeDescriptor : DESCRIPTORS) {
      if (attributeDescriptor.getName().equals(name)) {
        return attributeDescriptor;
      }
    }
    return null;
  }

  @Override
  public String getName() {
    return NAME;
  }
}
