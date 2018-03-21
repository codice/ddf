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

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of QueryResponseTransformer that produces CSV output.
 *
 * @see ddf.catalog.transform.QueryResponseTransformer
 */
public class CsvQueryResponseTransformer implements QueryResponseTransformer {
  private static final Logger LOGGER = LoggerFactory.getLogger(CsvQueryResponseTransformer.class);

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

    Set<AttributeDescriptor> allAttributeDescriptors =
        getAllRequestedAttributes(upstreamResponse.getResults(), hiddenFields, requestedFields);

    List<AttributeDescriptor> sortedAttributeDescriptors =
        sortAttributes(allAttributeDescriptors, attributeOrder);

    Appendable csv =
        writeSearchResultsToCsv(upstreamResponse, columnAliasMap, sortedAttributeDescriptors);

    return createResponse(csv);
  }

  private BinaryContent createResponse(final Appendable csv) {
    InputStream inputStream =
        new ByteArrayInputStream(csv.toString().getBytes(Charset.defaultCharset()));
    BinaryContent binaryContent = new BinaryContentImpl(inputStream);
    return binaryContent;
  }

  private Appendable writeSearchResultsToCsv(
      final SourceResponse upstreamResponse,
      Map<String, String> columnAliasMap,
      List<AttributeDescriptor> sortedAttributeDescriptors)
      throws CatalogTransformerException {
    StringBuilder stringBuilder = new StringBuilder();

    try {
      CSVPrinter csvPrinter = new CSVPrinter(stringBuilder, CSVFormat.RFC4180);
      printColumnHeaders(csvPrinter, sortedAttributeDescriptors, columnAliasMap);

      upstreamResponse
          .getResults()
          .stream()
          .map(Result::getMetacard)
          .forEach(mc -> printMetacard(csvPrinter, mc, sortedAttributeDescriptors));

      return csvPrinter.getOut();
    } catch (IOException ioe) {
      throw new CatalogTransformerException(ioe.getMessage(), ioe);
    }
  }

  private void printMetacard(
      final CSVPrinter csvPrinter,
      final Metacard metacard,
      final List<AttributeDescriptor> attributeDescriptors) {

    Iterator<Serializable> metacardIterator = new MetacardIterator(metacard, attributeDescriptors);

    printData(csvPrinter, metacardIterator);
  }

  private void printColumnHeaders(
      final CSVPrinter csvPrinter,
      final List<AttributeDescriptor> attributeDescriptors,
      Map<String, String> aliasMap) {
    Iterator<String> columnHeaderIterator =
        new ColumnHeaderIterator(attributeDescriptors, aliasMap);

    printData(csvPrinter, columnHeaderIterator);
  }

  private void printData(final CSVPrinter csvPrinter, final Iterator iterator) {
    try {
      csvPrinter.printRecord(() -> iterator);
    } catch (IOException ioe) {
      LOGGER.error(ioe.getMessage(), ioe);
    }
  }

  private List<AttributeDescriptor> sortAttributes(
      final Set<AttributeDescriptor> attributeSet, final List<String> attributeOrder) {
    CsvAttributeDescriptorComparator attributeComparator =
        new CsvAttributeDescriptorComparator(attributeOrder);

    return attributeSet.stream().sorted(attributeComparator).collect(Collectors.toList());
  }

  private Set<AttributeDescriptor> getAllRequestedAttributes(
      final List<Result> results,
      final Set<String> hiddenFields,
      final Set<String> requestedFields) {

    Set<AttributeDescriptor> allAttributes = new HashSet<>();

    results
        .stream()
        .map(Result::getMetacard)
        .map(Metacard::getMetacardType)
        .map(MetacardType::getAttributeDescriptors)
        .forEach(
            descriptorSet ->
                descriptorSet
                    .stream()
                    .filter(
                        desc ->
                            !AttributeType.AttributeFormat.BINARY.equals(
                                desc.getType().getAttributeFormat()))
                    .filter(
                        desc ->
                            !AttributeType.AttributeFormat.OBJECT.equals(
                                desc.getType().getAttributeFormat()))
                    .filter(desc -> !hiddenFields.contains(desc.getName()))
                    .filter(
                        desc ->
                            requestedFields.isEmpty() || requestedFields.contains(desc.getName()))
                    .forEach(allAttributes::add));

    return allAttributes;
  }
}
