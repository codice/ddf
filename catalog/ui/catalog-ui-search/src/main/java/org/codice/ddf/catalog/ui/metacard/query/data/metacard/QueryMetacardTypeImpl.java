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
package org.codice.ddf.catalog.ui.metacard.query.data.metacard;

import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.DETAIL_LEVEL;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.FACETS;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_CQL;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_ENTERPRISE;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_FEDERATION;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_FILTER_TREE;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_METACARD_TYPE_NAME;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_POLLING;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_SORTS;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_SOURCES;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_TYPE;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.SCHEDULES;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Core;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryMetacardTypeImpl extends MetacardTypeImpl {
  private static final Set<AttributeDescriptor> QUERY_DESCRIPTORS;

  private static final Set<String> QUERY_ATTRIBUTE_NAMES;

  static {
    QUERY_DESCRIPTORS = new HashSet<>();

    QUERY_DESCRIPTORS.add(MetacardImpl.BASIC_METACARD.getAttributeDescriptor(Core.ID));

    QUERY_DESCRIPTORS.add(MetacardImpl.BASIC_METACARD.getAttributeDescriptor(Core.TITLE));

    QUERY_DESCRIPTORS.add(MetacardImpl.BASIC_METACARD.getAttributeDescriptor(Core.CREATED));

    QUERY_DESCRIPTORS.add(MetacardImpl.BASIC_METACARD.getAttributeDescriptor(Core.MODIFIED));

    QUERY_DESCRIPTORS.add(MetacardImpl.BASIC_METACARD.getAttributeDescriptor(Core.METACARD_OWNER));

    QUERY_DESCRIPTORS.add(MetacardImpl.BASIC_METACARD.getAttributeDescriptor(Core.DESCRIPTION));

    QUERY_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            QUERY_CQL,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    QUERY_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            QUERY_FILTER_TREE,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    QUERY_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            QUERY_ENTERPRISE,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.BOOLEAN_TYPE));

    QUERY_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            QUERY_SOURCES,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            true /* multivalued */,
            BasicTypes.STRING_TYPE));

    QUERY_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            QUERY_SORTS,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            true /* multivalued */,
            BasicTypes.STRING_TYPE));

    QUERY_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            QUERY_POLLING,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.INTEGER_TYPE));

    QUERY_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            QUERY_FEDERATION,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    QUERY_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            QUERY_TYPE,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    QUERY_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            DETAIL_LEVEL,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    QUERY_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            SCHEDULES,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            true /* multivalued */,
            BasicTypes.XML_TYPE));

    QUERY_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            FACETS,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            true /* multivalued */,
            BasicTypes.STRING_TYPE));

    QUERY_ATTRIBUTE_NAMES =
        QUERY_DESCRIPTORS.stream().map(AttributeDescriptor::getName).collect(Collectors.toSet());
  }

  public QueryMetacardTypeImpl() {
    this(QUERY_METACARD_TYPE_NAME, QUERY_DESCRIPTORS);
  }

  public QueryMetacardTypeImpl(String name, Set<AttributeDescriptor> descriptors) {
    super(name, descriptors);
  }

  public static Set<String> getQueryAttributeNames() {
    return QUERY_ATTRIBUTE_NAMES;
  }
}
