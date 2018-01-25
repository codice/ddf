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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.types.Core;
import java.util.Map;
import java.util.Set;

public class WorkspaceAttributes implements MetacardType {

  public static final String WORKSPACE_TAG = "workspace";

  public static final String WORKSPACE_METACARDS = "metacards";

  public static final String WORKSPACE_QUERIES = "queries";

  public static final String WORKSPACE_SHARING = "metacard.sharing";

  public static final String WORKSPACE_LISTS = "lists";

  private static final String NAME = "workspace";

  // @formatter:off
  private static final Map<String, AttributeDescriptor> DESCRIPTORS =
      ImmutableMap.of(
          Core.METACARD_OWNER,
          new AttributeDescriptorImpl(
              Core.METACARD_OWNER,
              true /* indexed */,
              true /* stored */,
              true /* tokenized */,
              false /* multivalued */,
              BasicTypes.STRING_TYPE),
          WORKSPACE_QUERIES,
          new AttributeDescriptorImpl(
              WORKSPACE_QUERIES,
              false /* indexed */,
              true /* stored */,
              false /* tokenized */,
              true /* multivalued */,
              BasicTypes.XML_TYPE),
          WORKSPACE_LISTS,
          new AttributeDescriptorImpl(
              WORKSPACE_LISTS,
              false /* indexed */,
              true /* stored */,
              false /* tokenized */,
              true /* multivalued */,
              BasicTypes.XML_TYPE),
          WORKSPACE_SHARING,
          new AttributeDescriptorImpl(
              WORKSPACE_SHARING,
              false /* indexed */,
              true /* stored */,
              false /* tokenized */,
              true /* multivalued */,
              BasicTypes.XML_TYPE));
  // @formatter:on

  @Override
  public Set<AttributeDescriptor> getAttributeDescriptors() {
    return ImmutableSet.copyOf(DESCRIPTORS.values());
  }

  @Override
  public AttributeDescriptor getAttributeDescriptor(String name) {
    return DESCRIPTORS.get(name);
  }

  @Override
  public String getName() {
    return NAME;
  }
}
