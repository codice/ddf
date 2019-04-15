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
package org.codice.ddf.catalog.ui.security.accesscontrol;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.SecurityAttributes;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class AclTestSupport {
  private AclTestSupport() {
    // Should not be instantiated
  }

  /** Creates a metacard from a map of desired attributes */
  public static Metacard metacardFromAttributes(Map<String, Serializable> attributes) {
    Metacard metacard =
        new MetacardImpl(
            new MetacardTypeImpl(
                "type",
                ImmutableSet.<AttributeDescriptor>builder()
                    .addAll(new SecurityAttributes().getAttributeDescriptors())
                    .build()));

    attributes.forEach((key, value) -> metacard.setAttribute(createAttribute(key, value)));
    return metacard;
  }

  private static Attribute createAttribute(String key, Serializable value) {
    return new AttributeImpl(
        key, value instanceof Collection ? new ArrayList<>((Collection<?>) value) : value);
  }
}
