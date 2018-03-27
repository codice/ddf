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
package org.codice.ddf.catalog.ui.forms.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import org.boon.core.reflection.fields.FieldAccess;
import org.boon.json.serializers.JsonSerializerInternal;
import org.boon.primitive.CharBuf;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FilterNodeValueSerializerTest {
  @Mock private FieldAccess mockFieldAccess;

  private CharBuf builder;

  private FilterNodeValueSerializer serializer;

  private Example example;

  @Before
  public void setup() {
    serializer = new FilterNodeValueSerializer();
    example = new Example(null);
    builder = new CharBuf(new char[20]);
  }

  @Test
  public void testWrongFieldName() throws Exception {
    when(mockFieldAccess.getField())
        .thenReturn(ExampleForReflectionWrongField.class.getDeclaredField("integer"));
    assertThat(
        "Processing should only occur when the field name is 'value' or 'defaultValue'",
        invokeSerializer(),
        is(false));
  }

  @Test
  public void testWrongFieldType() throws Exception {
    when(mockFieldAccess.getField())
        .thenReturn(ExampleForReflectionWrongType.class.getDeclaredField("value"));
    assertThat(
        "Processing should not occur if the type of the field is not a String",
        invokeSerializer(),
        is(false));
  }

  @Test(expected = FilterNodeValueSerializer.UncheckedIllegalAccessException.class)
  public void testAccessDenied() throws Exception {
    when(mockFieldAccess.getField()).thenReturn(getFieldWithValue(null, false));
    assertThat(
        "Processing should not occur if an IllegalAccessException gets thrown during field access",
        invokeSerializer(),
        is(false));
  }

  @Test
  public void testNullFieldValue() throws Exception {
    when(mockFieldAccess.getField()).thenReturn(getFieldWithValue(null));
    assertThat(
        "Processing should not occur when a field's value is null, there's nothing to serialize",
        invokeSerializerUsingExample(),
        is(false));
  }

  @Test
  public void testParseInt() throws Exception {
    when(mockFieldAccess.getField()).thenReturn(getFieldWithValue("3"));
    assertThat(
        "Processing should occur when parsing an integer within the string",
        invokeSerializerUsingExample(),
        is(true));

    assertThat(
        "Expected builder state not found, need field 'value' to be an int = 3",
        builder.equals(new CharBuf(new char[20]).addJsonFieldName("value").addInt(3)));
  }

  @Test
  public void testParseDouble() throws Exception {
    when(mockFieldAccess.getField()).thenReturn(getFieldWithValue("0.3478253"));
    assertThat(
        "Processing should occur when parsing a double within the string",
        invokeSerializerUsingExample(),
        is(true));

    assertThat(
        "Expected builder state not found, need field 'value' to be a double = 0.3478253",
        builder.equals(new CharBuf(new char[20]).addJsonFieldName("value").addDouble(0.3478253)));
  }

  @Test
  public void testParseTrue() throws Exception {
    when(mockFieldAccess.getField()).thenReturn(getFieldWithValue("true"));
    assertThat(
        "Processing should occur when parsing a boolean within the string",
        invokeSerializerUsingExample(),
        is(true));

    assertThat(
        "Expected builder state not found, need field 'value' to be a boolean = true",
        builder.equals(new CharBuf(new char[20]).addJsonFieldName("value").addBoolean(true)));
  }

  @Test
  public void testParseFalse() throws Exception {
    when(mockFieldAccess.getField()).thenReturn(getFieldWithValue("false"));
    assertThat(
        "Processing should occur when parsing a boolean within the string",
        invokeSerializerUsingExample(),
        is(true));

    assertThat(
        "Expected builder state not found, need field 'value' to be a boolean = false",
        builder.equals(new CharBuf(new char[20]).addJsonFieldName("value").addBoolean(false)));
  }

  @Test
  public void testParseString() throws Exception {
    when(mockFieldAccess.getField()).thenReturn(getFieldWithValue("some-string"));
    assertThat(
        "Processing should occur when parsing any format of string",
        invokeSerializerUsingExample(),
        is(true));

    assertThat(
        "Expected builder state not found, need field 'value' to be a string = 'some-string'",
        builder.equals(
            new CharBuf(new char[20]).addJsonFieldName("value").addQuoted("some-string")));
  }

  private boolean invokeSerializer() {
    return serializer.serializeField(
        mock(JsonSerializerInternal.class), null, mockFieldAccess, builder);
  }

  private boolean invokeSerializerUsingExample() {
    return serializer.serializeField(
        mock(JsonSerializerInternal.class), example, mockFieldAccess, builder);
  }

  private Field getFieldWithValue(String fieldValue) throws NoSuchFieldException {
    return getFieldWithValue(fieldValue, true);
  }

  private Field getFieldWithValue(String fieldValue, boolean accessible)
      throws NoSuchFieldException {
    example.setValue(fieldValue);
    Field field = example.getClass().getDeclaredField("value");
    field.setAccessible(accessible);
    return field;
  }

  private static class ExampleForReflectionWrongField {
    private Integer integer;
  }

  private static class ExampleForReflectionWrongType {
    private Integer value;
  }

  private static class Example {
    private String value;

    Example(String value) {
      this.value = value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }
}
