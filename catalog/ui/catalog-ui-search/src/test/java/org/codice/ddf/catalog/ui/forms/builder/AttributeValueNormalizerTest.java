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
package org.codice.ddf.catalog.ui.forms.builder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import java.util.Optional;
import org.codice.ddf.catalog.ui.forms.filter.FilterProcessingException;
import org.junit.Before;
import org.junit.Test;

public class AttributeValueNormalizerTest {

  private static final String PROPERTY_NAME = "anyText";

  private static final String PROPERTY_NAME_WITH_QUOTES = "\"anyText\"";

  private static final String NON_DATE_INPUT = "hello";

  private static final String VALID_ISO_8601_DATE_STRING = "2018-12-10T13:09:40Z";

  private static final String VALID_ISO_8601_DATE_STRING_WITH_OFFSET = "2007-12-03T10:15:30+01:00";

  private static final String VALID_MS_SINCE_EPOCH_DATE_STRING = "1544447380000";

  private static final String VALID_MS_SINCE_EPOCH_DATE_STRING_WITH_OFFSET = "1196673330000";

  private static final String VALID_RELATIVE_FUNCTION = "RELATIVE(P1D)";

  private static final String INVALID_RELATIVE_FUNCTION = "RELATIVE()";

  private static final AttributeDescriptor DATE_DESCRIPTOR = createDateDescriptor();

  private AttributeValueNormalizer normalizer;

  private AttributeRegistry registry;

  @Before
  public void setUp() {
    registry = mock(AttributeRegistry.class);
    when(registry.lookup(anyString())).thenReturn(Optional.empty());
    normalizer = new AttributeValueNormalizer(registry);
  }

  @Test
  public void testNullPropertyNullValueToJson() {
    assertThat(normalizer.normalizeForJson(null, null), is(nullValue()));
  }

  @Test
  public void testNullPropertyNullValueToXml() {
    assertThat(normalizer.normalizeForXml(null, null), is(nullValue()));
  }

  @Test
  public void testNonNullPropertyNullValueToJson() {
    assertThat(normalizer.normalizeForJson(PROPERTY_NAME, null), is(nullValue()));
  }

  @Test
  public void testNonNullPropertyNullValueToXml() {
    assertThat(normalizer.normalizeForXml(PROPERTY_NAME, null), is(nullValue()));
  }

  @Test
  public void testNullPropertyNonNullValueToJson() {
    assertThat(
        normalizer.normalizeForJson(null, VALID_MS_SINCE_EPOCH_DATE_STRING),
        is(equalTo(VALID_MS_SINCE_EPOCH_DATE_STRING)));
  }

  @Test
  public void testNullPropertyNonNullValueToXml() {
    assertThat(
        normalizer.normalizeForXml(null, VALID_ISO_8601_DATE_STRING),
        is(equalTo(VALID_ISO_8601_DATE_STRING)));
  }

  @Test
  public void testNonDateValueToJson() {
    when(registry.lookup(eq(PROPERTY_NAME))).thenReturn(Optional.empty());
    assertThat(
        normalizer.normalizeForJson(PROPERTY_NAME, NON_DATE_INPUT), is(equalTo(NON_DATE_INPUT)));
  }

  @Test
  public void testNonDateValueToXml() {
    when(registry.lookup(eq(PROPERTY_NAME))).thenReturn(Optional.empty());
    assertThat(
        normalizer.normalizeForXml(PROPERTY_NAME, NON_DATE_INPUT), is(equalTo(NON_DATE_INPUT)));
  }

  @Test
  public void testEpochDateValueToJson() {
    when(registry.lookup(eq(PROPERTY_NAME))).thenReturn(Optional.of(DATE_DESCRIPTOR));
    assertThat(
        normalizer.normalizeForJson(PROPERTY_NAME, VALID_MS_SINCE_EPOCH_DATE_STRING),
        is(equalTo(VALID_ISO_8601_DATE_STRING)));
  }

  @Test
  public void testIsoDateValueToJson() {
    when(registry.lookup(eq(PROPERTY_NAME))).thenReturn(Optional.of(DATE_DESCRIPTOR));
    assertThat(
        normalizer.normalizeForJson(PROPERTY_NAME, VALID_ISO_8601_DATE_STRING),
        is(equalTo(VALID_ISO_8601_DATE_STRING)));
  }

  @Test
  public void testUnexpectedDateValueToJson() {
    when(registry.lookup(eq(PROPERTY_NAME))).thenReturn(Optional.of(DATE_DESCRIPTOR));
    assertThat(
        normalizer.normalizeForJson(PROPERTY_NAME, NON_DATE_INPUT), is(equalTo(NON_DATE_INPUT)));
  }

  @Test
  public void testEpochDateValueToXml() {
    when(registry.lookup(eq(PROPERTY_NAME))).thenReturn(Optional.of(DATE_DESCRIPTOR));
    assertThat(
        normalizer.normalizeForXml(PROPERTY_NAME, VALID_MS_SINCE_EPOCH_DATE_STRING),
        is(equalTo(VALID_MS_SINCE_EPOCH_DATE_STRING)));
  }

  @Test
  public void testIsoDateValueToXml() {
    when(registry.lookup(eq(PROPERTY_NAME))).thenReturn(Optional.of(DATE_DESCRIPTOR));
    assertThat(
        normalizer.normalizeForXml(PROPERTY_NAME, VALID_ISO_8601_DATE_STRING),
        is(equalTo(VALID_MS_SINCE_EPOCH_DATE_STRING)));
  }

  @Test
  public void testIsoDateWithOffsetValueToXml() {
    when(registry.lookup(eq(PROPERTY_NAME))).thenReturn(Optional.of(DATE_DESCRIPTOR));
    assertThat(
        normalizer.normalizeForXml(PROPERTY_NAME, VALID_ISO_8601_DATE_STRING_WITH_OFFSET),
        is(equalTo(VALID_MS_SINCE_EPOCH_DATE_STRING_WITH_OFFSET)));
  }

  @Test
  public void testBadPropertyNameToJson() {
    when(registry.lookup(eq(PROPERTY_NAME_WITH_QUOTES))).thenReturn(Optional.empty());
    assertThat(
        normalizer.normalizeForJson(PROPERTY_NAME_WITH_QUOTES, VALID_ISO_8601_DATE_STRING),
        is(equalTo(VALID_ISO_8601_DATE_STRING)));
  }

  @Test(expected = FilterProcessingException.class)
  public void testBadPropertyNameQuotesToXml() {
    normalizer.normalizeForXml(PROPERTY_NAME_WITH_QUOTES, VALID_ISO_8601_DATE_STRING);
  }

  @Test(expected = FilterProcessingException.class)
  public void testBadPropertyNameNumbersToXml() {
    normalizer.normalizeForXml("anyText2", VALID_ISO_8601_DATE_STRING);
  }

  @Test
  public void testBadPropertyNameSymbolsToXml() {
    assertThrows(
        FilterProcessingException.class,
        () -> normalizer.normalizeForXml("anyText!", VALID_ISO_8601_DATE_STRING));
    assertThrows(
        FilterProcessingException.class,
        () -> normalizer.normalizeForXml("anyText@", VALID_ISO_8601_DATE_STRING));
    assertThrows(
        FilterProcessingException.class,
        () -> normalizer.normalizeForXml("anyText#", VALID_ISO_8601_DATE_STRING));
    assertThrows(
        FilterProcessingException.class,
        () -> normalizer.normalizeForXml("anyText$", VALID_ISO_8601_DATE_STRING));
    assertThrows(
        FilterProcessingException.class,
        () -> normalizer.normalizeForXml("anyText%", VALID_ISO_8601_DATE_STRING));
    assertThrows(
        FilterProcessingException.class,
        () -> normalizer.normalizeForXml("anyText^", VALID_ISO_8601_DATE_STRING));
    assertThrows(
        FilterProcessingException.class,
        () -> normalizer.normalizeForXml("anyText&", VALID_ISO_8601_DATE_STRING));
    assertThrows(
        FilterProcessingException.class,
        () -> normalizer.normalizeForXml("anyText*", VALID_ISO_8601_DATE_STRING));
    assertThrows(
        FilterProcessingException.class,
        () -> normalizer.normalizeForXml("anyText>", VALID_ISO_8601_DATE_STRING));
    assertThrows(
        FilterProcessingException.class,
        () -> normalizer.normalizeForXml("anyText<", VALID_ISO_8601_DATE_STRING));
  }

  @Test
  public void testGoodPropertyNameBasicToXml() {
    assertThat(
        normalizer.normalizeForXml("anyText", VALID_ISO_8601_DATE_STRING),
        is(equalTo(VALID_ISO_8601_DATE_STRING)));
  }

  @Test
  public void testGoodPropertyNameCompoundDotOnceToXml() {
    assertThat(
        normalizer.normalizeForXml("metacard.version", VALID_ISO_8601_DATE_STRING),
        is(equalTo(VALID_ISO_8601_DATE_STRING)));
  }

  @Test
  public void testGoodPropertyNameCompoundDotTwiceToXml() {
    assertThat(
        normalizer.normalizeForXml("metacard.version.action", VALID_ISO_8601_DATE_STRING),
        is(equalTo(VALID_ISO_8601_DATE_STRING)));
  }

  @Test
  public void testGoodPropertyNameCompoundDashOnceToXml() {
    assertThat(
        normalizer.normalizeForXml("versioned-on", VALID_ISO_8601_DATE_STRING),
        is(equalTo(VALID_ISO_8601_DATE_STRING)));
  }

  @Test
  public void testGoodPropertyNameCompoundDashTwiceToXml() {
    assertThat(
        normalizer.normalizeForXml("versioned-not-after", VALID_ISO_8601_DATE_STRING),
        is(equalTo(VALID_ISO_8601_DATE_STRING)));
  }

  @Test
  public void testGoodPropertyNameCompoundBothToXml() {
    assertThat(
        normalizer.normalizeForXml("metacard.version.versioned-on", VALID_ISO_8601_DATE_STRING),
        is(equalTo(VALID_ISO_8601_DATE_STRING)));
  }

  @Test(expected = FilterProcessingException.class)
  public void testUnexpectedDateValueToXml() {
    when(registry.lookup(eq(PROPERTY_NAME))).thenReturn(Optional.of(DATE_DESCRIPTOR));
    normalizer.normalizeForXml(PROPERTY_NAME, NON_DATE_INPUT);
  }

  @Test
  public void testRelativeFunctionToJson() {
    when(registry.lookup(eq(PROPERTY_NAME))).thenReturn(Optional.of(DATE_DESCRIPTOR));
    assertThat(
        normalizer.normalizeForJson(PROPERTY_NAME, VALID_RELATIVE_FUNCTION),
        is(equalTo(VALID_RELATIVE_FUNCTION)));
  }

  @Test
  public void testInvalidRelativeFunctionToJson() {
    when(registry.lookup(eq(PROPERTY_NAME))).thenReturn(Optional.of(DATE_DESCRIPTOR));
    assertThat(
        normalizer.normalizeForJson(PROPERTY_NAME, INVALID_RELATIVE_FUNCTION),
        is(equalTo(INVALID_RELATIVE_FUNCTION)));
  }

  @Test
  public void testRelativeFunctionToXml() {
    when(registry.lookup(eq(PROPERTY_NAME))).thenReturn(Optional.of(DATE_DESCRIPTOR));
    assertThat(
        normalizer.normalizeForXml(PROPERTY_NAME, VALID_RELATIVE_FUNCTION),
        is(equalTo(VALID_RELATIVE_FUNCTION)));
  }

  @Test(expected = FilterProcessingException.class)
  public void testInvalidRelativeFunctionToXml() {
    when(registry.lookup(eq(PROPERTY_NAME))).thenReturn(Optional.of(DATE_DESCRIPTOR));
    normalizer.normalizeForXml(PROPERTY_NAME, INVALID_RELATIVE_FUNCTION);
  }

  private static AttributeDescriptor createDateDescriptor() {
    return new AttributeDescriptorImpl("created", true, true, true, false, BasicTypes.DATE_TYPE);
  }

  private static <T> void assertThrows(Class<T> clazz, Runnable op) {
    boolean wasThrown = false;
    try {
      op.run();
    } catch (Exception e) {
      wasThrown = true;
      assertThat(
          "Thrown exception " + e.getClass() + " did not satisfy provided class " + clazz,
          clazz.isAssignableFrom(e.getClass()));
    }
    assertThat("Runnable finished with no exception thrown", wasThrown);
  }
}
