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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.junit.Before;
import org.junit.Test;

public class CsvTransformerTest {

  private static final List<AttributeDescriptor> ATTRIBUTE_DESCRIPTOR_LIST = new ArrayList<>();
  private static final int METACARD_COUNT = 2;
  private static final String CSV_ITEM_SEPARATOR_REGEX = "[\\n\\r,]";
  private static final List<ImmutableTriple<Object, Object, Object>> ATTRIBUTE_DATA =
      Arrays.asList(
          new ImmutableTriple<Object, Object, Object>(
              "attribute1", "value1", BasicTypes.STRING_TYPE),
          new ImmutableTriple<Object, Object, Object>(
              "attribute2", "value2", BasicTypes.STRING_TYPE),
          new ImmutableTriple<Object, Object, Object>("attribute3", 101, BasicTypes.INTEGER_TYPE),
          new ImmutableTriple<Object, Object, Object>(
              "attribute4", 3.14159, BasicTypes.DOUBLE_TYPE),
          new ImmutableTriple<Object, Object, Object>(
              "attribute5", "value,5", BasicTypes.STRING_TYPE),
          new ImmutableTriple<Object, Object, Object>(
              "attribute6", "value6", BasicTypes.STRING_TYPE),
          new ImmutableTriple<Object, Object, Object>(
              "attribute7", "OBJECT", BasicTypes.OBJECT_TYPE),
          new ImmutableTriple<Object, Object, Object>(
              "attribute8", "BINARY", BasicTypes.BINARY_TYPE),
          new ImmutableTriple<Object, Object, Object>("attribute9", "", BasicTypes.STRING_TYPE));
  private static Map<String, Attribute> metacardDataMap = new HashMap<>();
  private static List<Metacard> metacardList = new ArrayList<>();

  @Before
  public void setup() {
    ATTRIBUTE_DESCRIPTOR_LIST.clear();
    buildMetacardDataMap();
    buildMetacardList();
  }

  @Test
  public void getAllCsvAttributeDescriptors() {
    Set<AttributeDescriptor> allAttributes =
        CsvTransformer.getAllCsvAttributeDescriptors(metacardList);
    assertThat(allAttributes, hasSize(7));
    Set<String> allAttributeNames =
        allAttributes.stream().map(AttributeDescriptor::getName).collect(Collectors.toSet());
    // Binary and Object types are filtered
    final Set<String> expectedAttributes =
        Sets.newHashSet(
            "attribute1",
            "attribute2",
            "attribute3",
            "attribute4",
            "attribute5",
            "attribute6",
            "attribute9");
    assertThat(allAttributeNames, is(expectedAttributes));
  }

  @Test
  public void getOnlyRequestedAttributes() {

    Set<String> requestedAttributes = new HashSet<>();
    requestedAttributes.add("attribute1");

    Set<AttributeDescriptor> onlyRequestedAttributes =
        CsvTransformer.getOnlyRequestedAttributes(metacardList, requestedAttributes);

    assertThat(onlyRequestedAttributes, hasSize(1));
    final Optional<AttributeDescriptor> attributeDescriptorOptional =
        onlyRequestedAttributes.stream().findFirst();
    assertThat(attributeDescriptorOptional.isPresent(), is(true));

    final AttributeDescriptor attributeDescriptor = attributeDescriptorOptional.get();
    assertThat(attributeDescriptor, notNullValue());

    final String name = attributeDescriptor.getName();
    assertThat(name, is("attribute1"));
  }

  @Test
  public void writeSearchResultsToCsv() throws CatalogTransformerException {
    List<AttributeDescriptor> requestedAttributes = new ArrayList<>();
    requestedAttributes.add(buildAttributeDescriptor("attribute1", BasicTypes.STRING_TYPE));

    Appendable csvText =
        CsvTransformer.writeMetacardsToCsv(
            metacardList, requestedAttributes, Collections.emptyMap());

    Scanner scanner = new Scanner(csvText.toString());
    scanner.useDelimiter(CSV_ITEM_SEPARATOR_REGEX);

    String[] expectedHeaders = {"attribute1"};
    validate(scanner, expectedHeaders);

    String[] expectedValues = {"", "value1"};

    for (int i = 0; i < METACARD_COUNT; i++) {
      validate(scanner, expectedValues);
    }

    // final new line causes an extra "" value at end of file
    assertThat(scanner.hasNext(), is(true));
    assertThat(scanner.next(), is(""));
    assertThat(scanner.hasNext(), is(false));
  }

  @Test
  public void writeSearchResultsToCsvWithAliasMap() throws CatalogTransformerException {
    List<AttributeDescriptor> requestedAttributes = new ArrayList<>();
    requestedAttributes.add(buildAttributeDescriptor("attribute1", BasicTypes.STRING_TYPE));

    Map<String, String> aliasMap = ImmutableMap.of("attribute1", "column1");

    Appendable csvText =
        CsvTransformer.writeMetacardsToCsv(metacardList, requestedAttributes, aliasMap);

    Scanner scanner = new Scanner(csvText.toString());
    scanner.useDelimiter(CSV_ITEM_SEPARATOR_REGEX);

    String[] expectedHeaders = {"column1"};
    validate(scanner, expectedHeaders);

    String[] expectedValues = {"", "value1"};

    for (int i = 0; i < METACARD_COUNT; i++) {
      validate(scanner, expectedValues);
    }

    // final new line causes an extra "" value at end of file
    assertThat(scanner.hasNext(), is(true));
    assertThat(scanner.next(), is(""));
    assertThat(scanner.hasNext(), is(false));
  }

  @Test
  public void filterEmptyValueAttributes() {
    AttributeDescriptor attr9 = buildAttributeDescriptor("attribute9", BasicTypes.STRING_TYPE);
    Set<AttributeDescriptor> attributeDescriptors = new HashSet<>(ATTRIBUTE_DESCRIPTOR_LIST);
    attributeDescriptors.add(attr9);
    MetacardType metacardType = new MetacardTypeImpl("", attributeDescriptors);
    Metacard metacard = new MetacardImpl(metacardType);
    for (Attribute a : metacardDataMap.values()) {
      metacard.setAttribute(a);
    }
    metacard.setAttribute(new AttributeImpl("attribute9", ""));
    List<Metacard> metacards = metacardList.stream().collect(Collectors.toList());
    metacards.add(metacard);
    Set<AttributeDescriptor> nonEmptyValues = CsvTransformer.getNonEmptyValueAttributes(metacards);

    assertThat(nonEmptyValues, hasSize(6));
    final Optional<AttributeDescriptor> attributeDescriptorOptional =
        nonEmptyValues.stream().findFirst();
    assertThat(attributeDescriptorOptional.isPresent(), is(true));

    final AttributeDescriptor attributeDescriptor = attributeDescriptorOptional.get();
    assertThat(attributeDescriptor, notNullValue());
  }

  private Metacard buildMetacard() {
    MetacardType metacardType = new MetacardTypeImpl("", new HashSet<>(ATTRIBUTE_DESCRIPTOR_LIST));
    Metacard metacard = new MetacardImpl(metacardType);
    for (Attribute a : metacardDataMap.values()) {
      metacard.setAttribute(a);
    }
    return metacard;
  }

  private void buildMetacardList() {
    metacardList.clear();
    for (int i = 0; i < METACARD_COUNT; i++) {
      metacardList.add(buildMetacard());
    }
  }

  private void buildMetacardDataMap() {
    for (ImmutableTriple entry : ATTRIBUTE_DATA) {
      String attributeName = (String) entry.getLeft();
      Serializable attributeValue = (Serializable) entry.getMiddle();
      AttributeType attributeType = (AttributeType) entry.getRight();
      Attribute attribute = new AttributeImpl(attributeName, attributeValue);
      metacardDataMap.put(attributeName, attribute);
      ATTRIBUTE_DESCRIPTOR_LIST.add(buildAttributeDescriptor(attributeName, attributeType));
    }
  }

  private AttributeDescriptor buildAttributeDescriptor(String name, AttributeType type) {
    AttributeDescriptor attributeDescriptor = mock(AttributeDescriptor.class);
    when(attributeDescriptor.getName()).thenReturn(name);
    when(attributeDescriptor.getType()).thenReturn(type);
    return attributeDescriptor;
  }

  private void validate(Scanner scanner, String[] expectedValues) {
    for (String expectedValue : expectedValues) {
      assertThat(
          "Expected next value to be " + expectedValue + " but scanner.hasNext() returned false",
          scanner.hasNext(),
          is(true));
      assertThat(scanner.next(), is(expectedValue));
    }
  }
}
