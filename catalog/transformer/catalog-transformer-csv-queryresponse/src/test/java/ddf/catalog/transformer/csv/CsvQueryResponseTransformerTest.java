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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

public class CsvQueryResponseTransformerTest {

  private static final Object[][] ATTRIBUTE_DATA = {
    {"attribute1", "value1", BasicTypes.STRING_TYPE},
    {"attribute2", 101, BasicTypes.INTEGER_TYPE},
    {"attribute3", 3.14159, BasicTypes.DOUBLE_TYPE},
    {"attribute4", "value,4", BasicTypes.STRING_TYPE},
    {"attribute5", "value5", BasicTypes.STRING_TYPE},
    {"attribute6", "OBJECT", BasicTypes.OBJECT_TYPE},
    {"attribute7", "BINARY", BasicTypes.BINARY_TYPE}
  };

  private static final Map<String, Serializable> METACARD_DATA_MAP = new HashMap<>();

  private static final List<AttributeDescriptor> ATTRIBUTE_DESCRIPTOR_LIST = new ArrayList<>();

  private static final List<Result> RESULT_LIST = new ArrayList<>();

  public static final int METACARD_COUNT = 10;

  private SourceResponse sourceResponse;

  private CsvQueryResponseTransformer transformer;

  @Before
  public void setUp() {
    RESULT_LIST.clear();
    ATTRIBUTE_DESCRIPTOR_LIST.clear();
    METACARD_DATA_MAP.clear();

    this.transformer = new CsvQueryResponseTransformer();
    buildMetacardDataMap();
    this.sourceResponse = buildSourceResponse();
    buildResultList();
  }

  @Test
  public void testCsvQueryResponseTransformer() throws CatalogTransformerException {
    Map<String, Serializable> argumentsMap = new HashMap<>();

    String[] hiddenFieldsArray = {"attribute3", "attribute5"};
    argumentsMap.put("hiddenFields", buildSet(hiddenFieldsArray));

    String[] columnOrderArray = {"attribute3", "attribute4", "attribute2"};
    argumentsMap.put("columnOrder", buildList(columnOrderArray));

    String[][] aliases = {{"attribute1", "column1"}, {"attribute2", "column2"}};
    argumentsMap.put("aliases", buildMap(aliases));

    assertThat(ATTRIBUTE_DESCRIPTOR_LIST.size(), is(ATTRIBUTE_DATA.length));

    BinaryContent bc = transformer.transform(sourceResponse, argumentsMap);
    Scanner scanner = new Scanner(bc.getInputStream());
    scanner.useDelimiter("\\n|\\r|,");

    /*
     * OBJECT types, BINARY types and hidden attributes will be filtered out
     * We want to ensure that the only output data matches the explicitly requested headers
     */

    String[] expectedHeaders = {"attribute4", "column2"};
    validate(scanner, expectedHeaders);

    // The scanner will split "value,4" into two tokens even though the CSVPrinter will
    // handle it correctly.
    String[] expectedValues = {"", "\"value", "4\"", "101"};

    for (int i = 0; i < METACARD_COUNT; i++) {
      validate(scanner, expectedValues);
    }

    // final new line causes an extra "" value at end of file
    assertThat(scanner.hasNext(), is(true));
    assertThat(scanner.next(), is(""));
    assertThat(scanner.hasNext(), is(false));
  }

  private void validate(Scanner scanner, String[] expectedValues) {
    for (int i = 0; i < expectedValues.length; i++) {
      assertThat(scanner.hasNext(), is(true));
      assertThat(scanner.next(), is(expectedValues[i]));
    }
  }

  private ArrayList<String> buildList(String[] entries) {
    ArrayList<String> arrayList = new ArrayList<>();
    arrayList.addAll(Arrays.asList(entries));
    return arrayList;
  }

  private HashMap<String, String> buildMap(String[][] entries) {
    HashMap<String, String> hashMap = new HashMap<>();

    for (String[] entry : entries) {
      hashMap.put(entry[0], entry[1]);
    }

    return hashMap;
  }

  private HashSet<String> buildSet(String[] attributes) {
    HashSet<String> hashSet = new HashSet<>();
    hashSet.addAll(Arrays.asList(attributes));
    return hashSet;
  }

  private void buildMetacardDataMap() {
    for (Object[] entry : ATTRIBUTE_DATA) {
      String attributeName = entry[0].toString();
      AttributeType attributeType = (AttributeType) entry[2];
      Serializable attributeValue = (Serializable) entry[1];
      Attribute attribute = buildAttribute(attributeName, attributeValue);
      METACARD_DATA_MAP.put(attributeName, attribute);
      ATTRIBUTE_DESCRIPTOR_LIST.add(buildAttributeDescriptor(attributeName, attributeType));
    }
  }

  private SourceResponse buildSourceResponse() {
    SourceResponse sourceResponse = mock(SourceResponse.class);
    when(sourceResponse.getResults()).thenReturn(RESULT_LIST);
    return sourceResponse;
  }

  private void buildResultList() {
    RESULT_LIST.clear();

    for (int i = 0; i < METACARD_COUNT; i++) {
      Result result = mock(Result.class);
      Metacard metacard = buildMetacard();
      when(result.getMetacard()).thenReturn(metacard);
      RESULT_LIST.add(result);
    }
  }

  private Metacard buildMetacard() {
    Metacard metacard = mock(Metacard.class);

    Answer<Serializable> answer =
        invocation -> {
          String key = invocation.getArgument(0);
          return METACARD_DATA_MAP.get(key);
        };

    when(metacard.getAttribute(anyString())).thenAnswer(answer);
    MetacardType metacardType = buildMetacardType();
    when(metacard.getMetacardType()).thenReturn(metacardType);
    return metacard;
  }

  private MetacardType buildMetacardType() {
    MetacardType metacardType = mock(MetacardType.class);
    when(metacardType.getAttributeDescriptors())
        .thenReturn(new HashSet<>(ATTRIBUTE_DESCRIPTOR_LIST));
    return metacardType;
  }

  private AttributeDescriptor buildAttributeDescriptor(String name, AttributeType type) {
    AttributeDescriptor attributeDescriptor = mock(AttributeDescriptor.class);
    when(attributeDescriptor.getName()).thenReturn(name);
    when(attributeDescriptor.getType()).thenReturn(type);
    return attributeDescriptor;
  }

  private Attribute buildAttribute(String name, Serializable value) {
    Attribute attribute = mock(Attribute.class);
    when(attribute.getName()).thenReturn(name);
    when(attribute.getValue()).thenReturn(value);
    return attribute;
  }
}
