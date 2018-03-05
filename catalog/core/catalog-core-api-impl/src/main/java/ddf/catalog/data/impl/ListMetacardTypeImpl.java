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

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListMetacardTypeImpl extends MetacardTypeImpl {

  public static final String LIST_TAG = "list";

  public static final String LIST_METACARD_TYPE_NAME = "metacard.list";

  public static final String LIST_CQL = "list.cql";

  public static final String LIST_ICON = "list.icon";

  public static final String LIST_BOOKMARKS = "list.bookmarks";

  public static final List<String> STANDARD_LIST_TYPES =
      Collections.unmodifiableList(
          Arrays.asList(
              "folder",
              "target",
              "video",
              "text",
              "word",
              "powerpoint",
              "excel",
              "pdf",
              "image",
              "audio",
              "code",
              "archive",
              "tasks"));

  private static final Set<AttributeDescriptor> LIST_DESCRIPTORS;

  static {
    LIST_DESCRIPTORS = new HashSet<>();

    LIST_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            Metacard.ID,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    LIST_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            Metacard.TITLE,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    LIST_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            LIST_CQL,
            false /* indexed */,
            true /* stored */,
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
  }

  public ListMetacardTypeImpl() {
    this(LIST_METACARD_TYPE_NAME, LIST_DESCRIPTORS);
  }

  public ListMetacardTypeImpl(String name, Set<AttributeDescriptor> descriptors) {
    super(name, descriptors);
  }
}
