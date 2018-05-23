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
import static ddf.catalog.transformer.csv.common.CsvTransformer.getAllAttributes;
import static ddf.catalog.transformer.csv.common.CsvTransformer.sortAttributes;
import static ddf.catalog.transformer.csv.common.CsvTransformer.writeMetacardsToCsv;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation of QueryResponseTransformer that produces CSV output.
 *
 * @see ddf.catalog.transform.QueryResponseTransformer
 */
public class CsvQueryResponseTransformer implements QueryResponseTransformer {
  private static final String HIDDEN_FIELDS_KEY = "hiddenFields";

  private static final String COLUMN_ORDER_KEY = "columnOrder";

  private static final String COLUMN_ALIAS_KEY = "aliases";

  /**
   * @param upstreamResponse the SourceResponse to be converted.
   * @param arguments this transformer accepts 3 parameters in the 'arguments' map. 1) key:
   *     'hiddenFields' value: a java.util.Set containing Attribute names (as Strings) to be
   *     excluded from the output. 2) key: 'attributeOrder' value: a java.utilList containing
   *     Attribute name (as Strings) to identify the order that the columns will appear in the
   *     output. 3) key: 'aliases' value: a java.util.Map whose keys are attribute names and values
   *     are the how that attribute column should be aliased in the output. For example, if the key
   *     is 'title' and the value is 'Product' then the resulting CSV will have a column name of
   *     'Product' instead of 'title'.
   * @return a BinaryContent object that contains an InputStream with the CSV content.
   * @throws CatalogTransformerException during processing, the CSV output is written to an
   *     Appendable, whose 'append()' method signature declares that it throws IOException. When
   *     that Appendable throws IOException, this class will theoretically convert that into a
   *     CatalogTransformerException and raise that. Because this implementation uses a
   *     StringBuilder which doesn't throw IOException, this will never occur.
   */
  @Override
  public BinaryContent transform(
      SourceResponse upstreamResponse, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    List<Metacard> metacards =
        upstreamResponse
            .getResults()
            .stream()
            .map(Result::getMetacard)
            .collect(Collectors.toList());

    Set<String> hiddenFields =
        Optional.ofNullable((Set<String>) arguments.get(HIDDEN_FIELDS_KEY))
            .orElse(Collections.emptySet());

    List<String> attributeOrder =
        Optional.ofNullable((List<String>) arguments.get(COLUMN_ORDER_KEY))
            .orElse(Collections.emptyList());

    Map<String, String> columnAliasMap =
        Optional.ofNullable((Map<String, String>) arguments.get(COLUMN_ALIAS_KEY))
            .orElse(Collections.emptyMap());

    Set<AttributeDescriptor> allAttributeDescriptors = getAllAttributes(metacards, hiddenFields);

    Set<String> requestedFields = new HashSet<>(attributeOrder);

    // If requestedFields is not empty, additionally filter out all non-requested attributes.
    if (!requestedFields.isEmpty()) {
      allAttributeDescriptors =
          allAttributeDescriptors
              .stream()
              .filter(attrDesc -> requestedFields.contains(attrDesc.getName()))
              .collect(Collectors.toSet());
    }

    List<AttributeDescriptor> sortedAttributeDescriptors =
        sortAttributes(allAttributeDescriptors, attributeOrder);

    Appendable csv = writeMetacardsToCsv(metacards, sortedAttributeDescriptors, columnAliasMap);

    return createResponse(csv);
  }
}
