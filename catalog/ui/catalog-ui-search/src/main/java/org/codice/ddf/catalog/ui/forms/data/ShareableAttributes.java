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
import ddf.catalog.data.types.Core;
import java.util.Set;

/**
 * Set of attributes needed to enable sharing on Metacards. Should be refactored to live in a more
 * central location at a later time.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 *
 * <p>TODO DDF-3671 Revisit sharing functionality for metacards
 */
@SuppressWarnings("squid:S1135" /* Action to-do has a ticket number and will be addressed later */)
public class ShareableAttributes extends MetacardTypeImpl {
  public static final String SHAREABLE_TAG = "shareable";

  public static final String SHAREABLE_METADATA = "metacard.sharing";

  // @formatter:off
  private static final Set<AttributeDescriptor> SHAREABLE_ATTRIBUTES =
      ImmutableSet.of(
          new AttributeDescriptorImpl(
              Core.METACARD_OWNER,
              true /* indexed */,
              true /* stored */,
              true /* tokenized */,
              false /* multivalued */,
              BasicTypes.STRING_TYPE),
          new AttributeDescriptorImpl(
              SHAREABLE_METADATA,
              false /* indexed */,
              true /* stored */,
              false /* tokenized */,
              true /* multivalued */,
              BasicTypes.XML_TYPE));
  // @formatter:on

  public ShareableAttributes() {
    super(SHAREABLE_TAG, SHAREABLE_ATTRIBUTES);
  }
}
