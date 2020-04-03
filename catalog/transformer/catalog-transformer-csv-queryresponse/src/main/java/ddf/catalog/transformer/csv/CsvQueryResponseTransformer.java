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
import static ddf.catalog.transformer.csv.common.CsvTransformer.getAllCsvAttributeDescriptors;
import static ddf.catalog.transformer.csv.common.CsvTransformer.getOnlyRequestedAttributes;
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

  public static final String COLUMN_ORDER_KEY = "columnOrder";

  public static final String COLUMN_ALIAS_KEY = "aliases";

  private static final String HIDDEN_FIELDS_KEY = "hiddenFields";

  /**
   * @param upstreamResponse the SourceResponse to be converted.
   * @param arguments this transformer accepts 2 parameters in the 'arguments' map.
   *     <ol>
   *       <li>key: 'columnOrder' value: a {@link List} of attribute names (as strings) that
   *           specifies the order in which the columns will appear in the output.
   *       <li>key: 'aliases' value: a {@link Map} with keys that are attribute names and with
   *           values that are the corresponding column headers that will be printed in the output.
   *           For example, if the key is 'title' and the value is 'Product' then the resulting CSV
   *           will have a column name of 'Product' instead of 'title'.
   *     </ol>
   *
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
        upstreamResponse.getResults().stream()
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

    Set<String> requestedFields = new HashSet<>(attributeOrder);

    Set<AttributeDescriptor> requestedAttributeDescriptors =
        requestedFields.isEmpty()
            ? getAllCsvAttributeDescriptors(metacards)
            : getOnlyRequestedAttributes(metacards, requestedFields);

    Set<AttributeDescriptor> filteredAttributeDescriptors =
        requestedAttributeDescriptors.stream()
            .filter(desc -> !hiddenFields.contains(desc.getName()))
            .collect(Collectors.toSet());

    List<AttributeDescriptor> sortedAttributeDescriptors =
        sortAttributes(filteredAttributeDescriptors, attributeOrder);

    Appendable csv = writeMetacardsToCsv(metacards, sortedAttributeDescriptors, columnAliasMap);

    return createResponse(csv);
  }
}
