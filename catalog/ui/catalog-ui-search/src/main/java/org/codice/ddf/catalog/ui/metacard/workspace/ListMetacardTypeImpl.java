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

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Core;
import java.util.HashSet;
import java.util.Set;

public class ListMetacardTypeImpl extends MetacardTypeImpl {

  public static final String LIST_TAG = "list";

  public static final String LIST_METACARD_TYPE_NAME = "metacard.list";

  public static final String LIST_CQL = "list.cql";

  public static final String LIST_ICON = "list.icon";

  public static final String LIST_BOOKMARKS = "list.bookmarks";

  public static final String LIST_FILTERS = "list.filters";

  private static final Set<AttributeDescriptor> LIST_DESCRIPTORS;

  static {
    LIST_DESCRIPTORS = new HashSet<>();

    LIST_DESCRIPTORS.add(MetacardImpl.BASIC_METACARD.getAttributeDescriptor(Core.ID));

    LIST_DESCRIPTORS.add(MetacardImpl.BASIC_METACARD.getAttributeDescriptor(Core.TITLE));

    LIST_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            LIST_CQL,
            false /* indexed */,
            false /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    LIST_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            LIST_ICON,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    LIST_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            LIST_BOOKMARKS,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            true /* multivalued */,
            BasicTypes.STRING_TYPE));

    LIST_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            LIST_FILTERS,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));
  }

  public ListMetacardTypeImpl() {
    this(LIST_METACARD_TYPE_NAME, LIST_DESCRIPTORS);
  }

  public ListMetacardTypeImpl(String name, Set<AttributeDescriptor> descriptors) {
    super(name, descriptors);
  }
}
