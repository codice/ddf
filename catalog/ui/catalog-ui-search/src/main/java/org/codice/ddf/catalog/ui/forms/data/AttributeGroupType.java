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
package org.codice.ddf.catalog.ui.forms.data;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.SecurityAttributes;
import java.util.Set;

/**
 * A sharing list of attribute descriptor names. They can represent sets of interesting fields.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class AttributeGroupType extends MetacardTypeImpl {
  // If the tag changes, update the 'access-controlled-tag' service property as well
  public static final String ATTRIBUTE_GROUP_TAG = "attribute-group";

  public static final String ATTRIBUTE_GROUP_LIST = "ui.attribute-group";

  // @formatter:off
  private static final Set<AttributeDescriptor> ATTRIBUTE_GROUP_DESCRIPTORS =
      ImmutableSet.of(
          new AttributeDescriptorImpl(
              ATTRIBUTE_GROUP_LIST,
              false /* indexed */,
              true /* stored */,
              false /* tokenized */,
              true /* multivalued */,
              BasicTypes.STRING_TYPE));
  // @formatter:on

  public AttributeGroupType() {
    super(
        ATTRIBUTE_GROUP_TAG,
        ImmutableSet.<AttributeDescriptor>builder()
            .addAll(new CoreAttributes().getAttributeDescriptors())
            .addAll(new SecurityAttributes().getAttributeDescriptors())
            .addAll(ATTRIBUTE_GROUP_DESCRIPTORS)
            .build());
  }
}
