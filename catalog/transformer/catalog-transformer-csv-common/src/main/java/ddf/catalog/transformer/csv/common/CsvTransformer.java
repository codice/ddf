/*
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer.csv.common;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common Library used to transform {@link Metacard}s into CSV text.
 *
 * @see MetacardTransformer
 * @see Metacard
 * @see QueryResponseTransformer
 * @see QueryResponse
 * @see Attribute
 */
public class CsvTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvTransformer.class);

  private static final MimeType CSV_MIME_TYPE;

  static {
    try {
      CSV_MIME_TYPE = new MimeType("text/csv");
    } catch (MimeTypeParseException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static BinaryContent createResponse(final Appendable csv) {
    InputStream inputStream =
        new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8));
    return new BinaryContentImpl(inputStream, CSV_MIME_TYPE);
  }

  public static Appendable writeMetacardsToCsv(
      final List<Metacard> metacards,
      final List<AttributeDescriptor> sortedAttributeDescriptors,
      final Map<String, String> aliasMap)
      throws CatalogTransformerException {
    StringBuilder stringBuilder = new StringBuilder();

    try {
      CSVPrinter csvPrinter = new CSVPrinter(stringBuilder, CSVFormat.RFC4180);
      printColumnHeaders(csvPrinter, sortedAttributeDescriptors, aliasMap);

      metacards.forEach(m -> printMetacard(csvPrinter, m, sortedAttributeDescriptors));

      return csvPrinter.getOut();
    } catch (IOException ioe) {
      throw new CatalogTransformerException(ioe);
    }
  }

  private static void printMetacard(
      final CSVPrinter csvPrinter,
      final Metacard metacard,
      final List<AttributeDescriptor> sortedAttributeDescriptors) {

    Iterator<Serializable> metacardIterator =
        new MetacardIterator(metacard, sortedAttributeDescriptors);

    printData(csvPrinter, metacardIterator);
  }

  private static void printColumnHeaders(
      final CSVPrinter csvPrinter,
      final List<AttributeDescriptor> sortedAttributeDescriptors,
      final Map<String, String> aliasMap) {
    Iterator<String> columnHeaderIterator =
        new ColumnHeaderIterator(sortedAttributeDescriptors, aliasMap);

    printData(csvPrinter, columnHeaderIterator);
  }

  private static void printData(final CSVPrinter csvPrinter, final Iterator iterator) {
    try {
      csvPrinter.printRecord(() -> iterator);
    } catch (IOException ioe) {
      LOGGER.debug("Failed to print the CSV data.", ioe);
    }
  }

  public static List<AttributeDescriptor> sortAttributes(
      final Set<AttributeDescriptor> attributeSet, final List<String> attributeOrder) {
    CsvAttributeDescriptorComparator attributeComparator =
        new CsvAttributeDescriptorComparator(attributeOrder);

    return attributeSet.stream().sorted(attributeComparator).collect(toList());
  }

  /**
   * Given a list of {@link Metacard}s and a string set of hiddenFields, returns a set of {@link
   * AttributeDescriptor}s that contains all attributes that exist on the given metacard types
   * except those that should be excluded.
   */
  public static Set<AttributeDescriptor> getAllAttributes(
      final List<Metacard> metacards, final Set<String> excludedFields) {

    final Predicate<AttributeDescriptor> attributeNotBinary =
        attrDesc ->
            !AttributeType.AttributeFormat.BINARY.equals(attrDesc.getType().getAttributeFormat());

    final Predicate<AttributeDescriptor> attributeNotObject =
        attrDesc ->
            !AttributeType.AttributeFormat.OBJECT.equals(attrDesc.getType().getAttributeFormat());

    final Predicate<AttributeDescriptor> attributeNotExcluded =
        attrDesc -> !excludedFields.contains(attrDesc.getName());

    return metacards
        .stream()
        .filter(Objects::nonNull)
        .map(Metacard::getMetacardType)
        .map(MetacardType::getAttributeDescriptors)
        .flatMap(Set::stream)
        .filter(attributeNotBinary)
        .filter(attributeNotObject)
        .filter(attributeNotExcluded)
        .collect(toSet());
  }

  /**
   * Given a list of {@link Metacard}s, a string set of requested attributes and a string set of
   * excluded fields, returns a set of {@link AttributeDescriptor}s containing the requested
   * attributes, additionally filtering out excluded attributes.
   */
  public static Set<AttributeDescriptor> getOnlyRequestedAttributes(
      final List<Metacard> metacards,
      final Set<String> requestedAttributes,
      final Set<String> excluded) {

    // Get all attributes, filtering out excluded.
    final Set<AttributeDescriptor> attributes = getAllAttributes(metacards, excluded);

    // Filter out attributes not requested.
    attributes.removeIf(attrDesc -> !requestedAttributes.contains(attrDesc.getName()));

    return attributes;
  }
}
