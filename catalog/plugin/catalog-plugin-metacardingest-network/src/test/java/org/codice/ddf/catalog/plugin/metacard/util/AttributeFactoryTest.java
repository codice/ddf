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
package org.codice.ddf.catalog.plugin.metacard.util;

import static ddf.catalog.data.AttributeType.AttributeFormat.BINARY;
import static ddf.catalog.data.AttributeType.AttributeFormat.BOOLEAN;
import static ddf.catalog.data.AttributeType.AttributeFormat.DATE;
import static ddf.catalog.data.AttributeType.AttributeFormat.DOUBLE;
import static ddf.catalog.data.AttributeType.AttributeFormat.FLOAT;
import static ddf.catalog.data.AttributeType.AttributeFormat.GEOMETRY;
import static ddf.catalog.data.AttributeType.AttributeFormat.INTEGER;
import static ddf.catalog.data.AttributeType.AttributeFormat.LONG;
import static ddf.catalog.data.AttributeType.AttributeFormat.OBJECT;
import static ddf.catalog.data.AttributeType.AttributeFormat.SHORT;
import static ddf.catalog.data.AttributeType.AttributeFormat.STRING;
import static ddf.catalog.data.AttributeType.AttributeFormat.XML;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import java.io.Serializable;
import java.util.List;
import javax.xml.bind.DatatypeConverter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/** Validate the behavior for {@link AttributeFactory}. */
@RunWith(MockitoJUnitRunner.class)
public class AttributeFactoryTest {

  private static final String SAMPLE_JSON_STRING = "{ \"id\": \"j8k767bjav592j2\"}";

  private static final String LANGUAGE = "language";

  private static final String ENGLISH = "English";

  private static final String FRENCH = "French";

  private static final String GERMAN = "German";

  private static final String COMMA_SEPARATED_INTS_WITH_STRING = "8, 3, 103, star";

  private static final String COMMA_SEPARATED_WHITE_SPACE = "  ,  ,  ,  ";

  private static final String COMMA_SEPARATED_LANGUAGES =
      format("%s, %s, %s", ENGLISH, FRENCH, GERMAN);

  @Mock private AttributeDescriptor mockDescriptor;

  @Mock private AttributeType mockType;

  private AttributeFactory attributeFactory;

  @Before
  public void setup() throws Exception {
    when(mockDescriptor.getType()).thenReturn(mockType);
    attributeFactory = new AttributeFactory();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateAttributeWithIllegalArgument() throws Exception {
    when(mockType.getAttributeFormat()).thenReturn(INTEGER);
    attributeFactory.createAttribute(mockDescriptor, "1874xyz");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateAttributeWithNullDescriptor() throws Exception {
    attributeFactory.createAttribute(null, "value");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateAttributeWithNullParseValue() throws Exception {
    attributeFactory.createAttribute(mockDescriptor, null);
  }

  @Test
  public void testCreateMultiValuedAttributeFromCommaSeparatedList() throws Exception {
    when(mockDescriptor.isMultiValued()).thenReturn(true);
    when(mockDescriptor.getName()).thenReturn(LANGUAGE);
    when(mockType.getAttributeFormat()).thenReturn(STRING);

    Attribute attribute =
        attributeFactory.createAttribute(mockDescriptor, COMMA_SEPARATED_LANGUAGES);
    List<Serializable> languages = attribute.getValues();

    assertThat(languages, hasSize(3));
    assertThat(languages, containsInAnyOrder(ENGLISH, FRENCH, GERMAN));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateMultiValuedAttributeWithTypeMismatch() throws Exception {
    when(mockDescriptor.isMultiValued()).thenReturn(true);
    when(mockDescriptor.getName()).thenReturn(LANGUAGE);
    when(mockType.getAttributeFormat()).thenReturn(INTEGER);

    attributeFactory.createAttribute(mockDescriptor, COMMA_SEPARATED_INTS_WITH_STRING);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateMultiValuedAttributesWithEmptyElements() throws Exception {
    when(mockDescriptor.isMultiValued()).thenReturn(true);
    when(mockDescriptor.getName()).thenReturn(LANGUAGE);

    attributeFactory.createAttribute(mockDescriptor, COMMA_SEPARATED_WHITE_SPACE);
  }

  @Test
  public void testParseAttributeValueInteger() throws Exception {
    runParameterizedAttributeValueTest(INTEGER, "186", 186);
  }

  @Test
  public void testParseAttributeValueDouble() throws Exception {
    runParameterizedAttributeValueTest(DOUBLE, "0.8866432", 0.8866432);
  }

  @Test
  public void testParseAttributeValueFloat() throws Exception {
    runParameterizedAttributeValueTest(FLOAT, "0.123", 0.123f);
  }

  @Test
  public void testParseAttributeValueShort() throws Exception {
    runParameterizedAttributeValueTest(SHORT, "16", (short) 16);
  }

  @Test
  public void testParseAttributeValueLong() throws Exception {
    runParameterizedAttributeValueTest(LONG, "123456789", 123456789L);
  }

  @Test
  public void testParseAttributeValueBoolean() throws Exception {
    runParameterizedAttributeValueTest(BOOLEAN, "true", true);
  }

  @Test
  public void testParseAttributeValueBinary() throws Exception {
    String binaryString = "0100101010110110010010110110010";
    runParameterizedAttributeValueTest(BINARY, binaryString, binaryString.getBytes());
  }

  @Test
  public void testParseAttributeValueDate() throws Exception {
    String lexicalXsdDateTime = "2001-10-26T21:32:52";
    runParameterizedAttributeValueTest(
        DATE, lexicalXsdDateTime, DatatypeConverter.parseDateTime(lexicalXsdDateTime).getTime());
  }

  @Test
  public void testParseAttributeValueString() throws Exception {
    runParameterizedAttributeValueTest(STRING, SAMPLE_JSON_STRING, SAMPLE_JSON_STRING);
  }

  @Test
  public void testParseAttributeValueObject() throws Exception {
    runParameterizedAttributeValueTest(OBJECT, SAMPLE_JSON_STRING, SAMPLE_JSON_STRING);
  }

  @Test
  public void testParseAttributeValueGeometry() throws Exception {
    runParameterizedAttributeValueTest(GEOMETRY, SAMPLE_JSON_STRING, SAMPLE_JSON_STRING);
  }

  @Test
  public void testParseAttributeValueXml() throws Exception {
    runParameterizedAttributeValueTest(XML, SAMPLE_JSON_STRING, SAMPLE_JSON_STRING);
  }

  @Test
  public void testParseAttributeValueWithIllegalArgument() throws Exception {
    when(mockType.getAttributeFormat()).thenReturn(INTEGER);
    Serializable serializable = attributeFactory.parseAttributeValue(mockDescriptor, "1874xyz");
    assertNull(serializable);
  }

  @Test
  public void testParseAttributeValueWithNullDescriptor() throws Exception {
    attributeFactory.parseAttributeValue(null, "anyValue");
  }

  @Test
  public void testParseAttributeValueWithNullValue() throws Exception {
    attributeFactory.parseAttributeValue(mockDescriptor, null);
  }

  private void runParameterizedAttributeValueTest(
      AttributeType.AttributeFormat format, String rawValue, Object value) {
    when(mockType.getAttributeFormat()).thenReturn(format);
    Serializable serializable = attributeFactory.parseAttributeValue(mockDescriptor, rawValue);
    assertThat(serializable, is(value));
  }
}
