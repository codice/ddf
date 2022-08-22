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

package ddf.catalog.transformer.csv.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

public class MetacardIteratorTest {

  private static final String SOURCE = "SOURCE";

  private static final String METACARDTYPE = "METACARD_TYPE";

  private static final Object[][] ATTRIBUTE_DATA = {
    {"attribute1", AttributeFormat.STRING, "value1"},
    {"attribute2", AttributeFormat.INTEGER, new Integer(101)},
    {"attribute3", AttributeFormat.DOUBLE, new Double(3.14159)}
  };

  private static final Map<String, Serializable> METACARD_DATA_MAP = new HashMap<>();

  private static final List<AttributeDescriptor> ATTRIBUTE_DESCRIPTOR_LIST = new ArrayList<>();

  @Before
  public void setUp() {
    ATTRIBUTE_DESCRIPTOR_LIST.clear();
    METACARD_DATA_MAP.clear();

    for (Object[] entry : ATTRIBUTE_DATA) {
      String attributeName = entry[0].toString();
      AttributeFormat attributeFormat = (AttributeFormat) entry[1];
      Serializable attributeValue = (Serializable) entry[2];
      Attribute attribute = buildAttribute(attributeName, attributeValue);
      METACARD_DATA_MAP.put(attributeName, attribute);
      ATTRIBUTE_DESCRIPTOR_LIST.add(
          buildAttributeDescriptor(attributeName, attributeFormat, false));
    }

    Attribute attribute = buildAttribute("skipMe", "value");
    METACARD_DATA_MAP.put("skipMe", attribute);
  }

  @Test
  public void testColumnHeaderIterator() {
    Metacard metacard = buildMetacard();
    Iterator<Serializable> iterator = new MetacardIterator(metacard, ATTRIBUTE_DESCRIPTOR_LIST);

    for (int i = 0; i < ATTRIBUTE_DATA.length; i++) {
      assertThat(iterator.hasNext(), is(true));
      assertThat(iterator.next(), is(ATTRIBUTE_DATA[i][2]));
    }

    assertThat(iterator.hasNext(), is(false));
  }

  @Test
  public void testColumnHeaderIteratorWithMultivaluedAttribute() {
    ATTRIBUTE_DESCRIPTOR_LIST.clear();
    METACARD_DATA_MAP.clear();

    String attributeName = "multivalued";
    List<Serializable> values = Arrays.asList("value1", "value2", "value3");
    Attribute attribute = buildAttribute(attributeName, values);
    METACARD_DATA_MAP.put(attributeName, attribute);
    ATTRIBUTE_DESCRIPTOR_LIST.add(
        buildAttributeDescriptor(attributeName, AttributeFormat.STRING, true));

    Metacard metacard = buildMetacard();
    Iterator<Serializable> iterator = new MetacardIterator(metacard, ATTRIBUTE_DESCRIPTOR_LIST);
    assertThat(iterator.hasNext(), is(true));
    assertThat(iterator.next(), is("value1\nvalue2\nvalue3"));
  }

  @Test
  public void testSourceId() {
    ATTRIBUTE_DESCRIPTOR_LIST.clear();
    METACARD_DATA_MAP.clear();

    ATTRIBUTE_DESCRIPTOR_LIST.add(
        buildAttributeDescriptor(Core.SOURCE_ID, AttributeFormat.STRING, false));

    Metacard metacard = buildMetacard();
    when(metacard.getSourceId()).thenReturn(SOURCE);
    Iterator<Serializable> iterator = new MetacardIterator(metacard, ATTRIBUTE_DESCRIPTOR_LIST);
    assertThat(iterator.hasNext(), is(true));
    assertThat(iterator.next(), is(SOURCE));
  }

  @Test
  public void testMetacardType() {
    ATTRIBUTE_DESCRIPTOR_LIST.clear();
    METACARD_DATA_MAP.clear();

    ATTRIBUTE_DESCRIPTOR_LIST.add(
        buildAttributeDescriptor(MetacardType.METACARD_TYPE, AttributeFormat.STRING, false));

    Metacard metacard = buildMetacard();
    MetacardType metacardType = mock(MetacardType.class);
    when(metacardType.getName()).thenReturn(METACARDTYPE);
    when(metacard.getMetacardType()).thenReturn(metacardType);
    Iterator<Serializable> iterator = new MetacardIterator(metacard, ATTRIBUTE_DESCRIPTOR_LIST);
    assertThat(iterator.hasNext(), is(true));
    assertThat(iterator.next(), is(METACARDTYPE));
  }

  @Test
  public void testDateIsFormattedAsUTC() {
    ATTRIBUTE_DESCRIPTOR_LIST.clear();
    METACARD_DATA_MAP.clear();

    Date now = new Date();
    Attribute attribute = buildAttribute(Core.CREATED, now);
    METACARD_DATA_MAP.put(Core.CREATED, attribute);
    ATTRIBUTE_DESCRIPTOR_LIST.add(
        buildAttributeDescriptor(Core.CREATED, AttributeFormat.DATE, false));

    Metacard metacard = buildMetacard();
    Iterator<Serializable> iterator = new MetacardIterator(metacard, ATTRIBUTE_DESCRIPTOR_LIST);
    assertThat(iterator.hasNext(), is(true));

    String date = (String) iterator.next();
    OffsetDateTime offsetDateTime = OffsetDateTime.parse(date);
    assertThat(offsetDateTime.getOffset(), is(ZoneOffset.UTC));
    assertThat(Date.from(offsetDateTime.toInstant()), is(now));
  }

  @Test
  public void testBinaryIsFormattedAsBase64() {
    ATTRIBUTE_DESCRIPTOR_LIST.clear();
    METACARD_DATA_MAP.clear();

    byte[] binary = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    Attribute attribute = buildAttribute(Core.THUMBNAIL, binary);
    METACARD_DATA_MAP.put(Core.THUMBNAIL, attribute);
    ATTRIBUTE_DESCRIPTOR_LIST.add(
        buildAttributeDescriptor(Core.THUMBNAIL, AttributeFormat.BINARY, false));

    Metacard metacard = buildMetacard();
    Iterator<Serializable> iterator = new MetacardIterator(metacard, ATTRIBUTE_DESCRIPTOR_LIST);

    assertThat(iterator.hasNext(), is(true));
    assertThat(iterator.next(), is(Base64.getEncoder().encodeToString(binary)));
  }

  @Test
  public void testNullAttributeValue() {
    ATTRIBUTE_DESCRIPTOR_LIST.clear();
    METACARD_DATA_MAP.clear();

    Attribute attribute = buildAttribute(Core.DESCRIPTION, (Serializable) null);
    METACARD_DATA_MAP.put(Core.DESCRIPTION, attribute);
    ATTRIBUTE_DESCRIPTOR_LIST.add(
        buildAttributeDescriptor(Core.DESCRIPTION, AttributeFormat.STRING, false));

    Metacard metacard = buildMetacard();
    Iterator<Serializable> iterator = new MetacardIterator(metacard, ATTRIBUTE_DESCRIPTOR_LIST);

    assertThat(iterator.hasNext(), is(true));
    assertThat(iterator.next().toString(), isEmptyString());
  }

  @Test(expected = NoSuchElementException.class)
  public void testHasNext() {
    Metacard metacard = buildMetacard();
    Iterator<Serializable> iterator = new MetacardIterator(metacard, ATTRIBUTE_DESCRIPTOR_LIST);

    while (iterator.hasNext()) {
      iterator.next();
    }

    iterator.next();
  }

  private Metacard buildMetacard() {
    Metacard metacard = mock(Metacard.class);

    Answer<Serializable> answer =
        invocation -> {
          String key = invocation.getArgument(0);
          return METACARD_DATA_MAP.get(key);
        };

    when(metacard.getAttribute(anyString())).thenAnswer(answer);
    return metacard;
  }

  private AttributeDescriptor buildAttributeDescriptor(
      String name, AttributeFormat format, boolean isMultiValued) {
    AttributeType attributeType = mock(AttributeType.class);
    when(attributeType.getAttributeFormat()).thenReturn(format);

    AttributeDescriptor attributeDescriptor = mock(AttributeDescriptor.class);
    when(attributeDescriptor.getName()).thenReturn(name);
    when(attributeDescriptor.getType()).thenReturn(attributeType);
    when(attributeDescriptor.isMultiValued()).thenReturn(isMultiValued);
    return attributeDescriptor;
  }

  private Attribute buildAttribute(String name, Serializable value) {
    Attribute attribute = mock(Attribute.class);
    when(attribute.getName()).thenReturn(name);
    when(attribute.getValue()).thenReturn(value);
    return attribute;
  }

  private Attribute buildAttribute(String name, List<Serializable> values) {
    Attribute attribute = mock(Attribute.class);
    when(attribute.getName()).thenReturn(name);
    when(attribute.getValue()).thenReturn(values.get(0));
    when(attribute.getValues()).thenReturn(values);
    return attribute;
  }

  private Attribute buildEmptyAttribute(String name) {
    Attribute attribute = mock(Attribute.class);
    when(attribute.getName()).thenReturn(name);
    return attribute;
  }
}
