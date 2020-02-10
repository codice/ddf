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
package org.codice.ddf.catalog.ui.query.cql;

import ddf.catalog.data.AttributeDescriptor;
import org.codice.ddf.catalog.ui.query.utility.MetacardAttribute;

public class MetacardAttributeImpl implements MetacardAttribute {

  private final String format;

  private final boolean multivalued;

  private final boolean indexed;

  public MetacardAttributeImpl(AttributeDescriptor descriptor) {
    format = descriptor.getType().getAttributeFormat().toString();
    indexed = descriptor.isIndexed();
    multivalued = descriptor.isMultiValued();
  }

  public boolean getMultivalued() {
    return multivalued;
  }

  public boolean getIndexed() {
    return indexed;
  }

  public String getFormat() {
    return format;
  }
}
