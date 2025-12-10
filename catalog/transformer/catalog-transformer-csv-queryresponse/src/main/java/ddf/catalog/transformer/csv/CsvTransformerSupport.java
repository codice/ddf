/*
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer.csv;

import static ddf.catalog.transformer.csv.common.CsvTransformer.createResponse;
import static ddf.catalog.transformer.csv.common.CsvTransformer.getNonEmptyValueAttributes;
import static ddf.catalog.transformer.csv.common.CsvTransformer.getOnlyRequestedAttributes;
import static ddf.catalog.transformer.csv.common.CsvTransformer.sortAttributes;
import static ddf.catalog.transformer.csv.common.CsvTransformer.writeMetacardsToCsv;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

class CsvTransformerSupport {
  static final String COLUMN_ORDER_KEY = "columnOrder";

  static final String COLUMN_ALIAS_KEY = "aliases";

  static final String HIDDEN_FIELDS_KEY = "hiddenFields";

  private CsvTransformerSupport() {}

  static BinaryContent transformWithArguments(
      final List<Metacard> metacards, final Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    final Set<String> hiddenFields =
        Optional.ofNullable((Set<String>) arguments.get(HIDDEN_FIELDS_KEY))
            .orElse(Collections.emptySet());

    final List<String> attributeOrder = getColumnOrder(arguments);

    final Map<String, String> columnAliasMap =
        Optional.ofNullable((Map<String, String>) arguments.get(COLUMN_ALIAS_KEY))
            .orElse(Collections.emptyMap());

    final Set<String> requestedFields = new HashSet<>(attributeOrder);

    final Set<AttributeDescriptor> requestedAttributeDescriptors =
        requestedFields.isEmpty()
            ? getNonEmptyValueAttributes(metacards)
            : getOnlyRequestedAttributes(metacards, requestedFields);

    if (shouldInjectMetacardType(requestedFields)) {
      injectMetacardType(requestedAttributeDescriptors);
    }

    final Set<AttributeDescriptor> filteredAttributeDescriptors =
        requestedAttributeDescriptors.stream()
            .filter(desc -> !hiddenFields.contains(desc.getName()))
            .collect(Collectors.toSet());

    final List<AttributeDescriptor> sortedAttributeDescriptors =
        sortAttributes(filteredAttributeDescriptors, attributeOrder);

    final Appendable csv =
        writeMetacardsToCsv(metacards, sortedAttributeDescriptors, columnAliasMap);

    return createResponse(csv);
  }

  private static List<String> getColumnOrder(final Map<String, Serializable> arguments) {
    final Object columnOrder = arguments.get(COLUMN_ORDER_KEY);
    if (columnOrder instanceof String && StringUtils.isNotBlank((String) columnOrder)) {
      return Arrays.asList(((String) columnOrder).split(","));
    }
    return Optional.ofNullable(columnOrder)
        .filter(value -> value instanceof List)
        .map(value -> (List<String>) value)
        .orElse(new ArrayList<>());
  }

  private static void injectMetacardType(Collection<AttributeDescriptor> descriptors) {
    descriptors.add(
        new AttributeDescriptorImpl(
            MetacardType.METACARD_TYPE, false, false, false, false, BasicTypes.STRING_TYPE));
  }

  private static boolean shouldInjectMetacardType(Collection<String> attributes) {
    return CollectionUtils.isEmpty(attributes) || attributes.contains(MetacardType.METACARD_TYPE);
  }
}
