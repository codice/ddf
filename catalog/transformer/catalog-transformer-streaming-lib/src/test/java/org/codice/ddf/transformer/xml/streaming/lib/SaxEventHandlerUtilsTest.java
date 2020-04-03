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
package org.codice.ddf.transformer.xml.streaming.lib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.ValidationAttributes;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;

public class SaxEventHandlerUtilsTest {

  private static Set<AttributeDescriptor> descriptors = new HashSet<>();

  private SaxEventHandlerUtils saxEventHandlerUtils = new SaxEventHandlerUtils();

  private CoreAttributes coreAttributes = new CoreAttributes();

  @BeforeClass
  public static void setup() {
    CoreAttributes coreAttributes = new CoreAttributes();
    descriptors.add(coreAttributes.getAttributeDescriptor(CoreAttributes.ID));
    descriptors.add(coreAttributes.getAttributeDescriptor(CoreAttributes.LANGUAGE));
  }

  @Test
  public void testValidSingleValue() {
    List<Attribute> attributes = getTestAttributes();
    List<Attribute> combinedAttributes =
        saxEventHandlerUtils.getCombinedMultiValuedAttributes(descriptors, attributes);

    assertThat(combinedAttributes.size(), is(2));
    assertIdAttribute(combinedAttributes);
    assertLanguageAttribute(combinedAttributes, 1);
  }

  @Test
  public void testValidMultiValue() {
    List<Attribute> attributes = getTestAttributes();
    attributes.add(new AttributeImpl(CoreAttributes.LANGUAGE, "Dothraki"));
    List<Attribute> combinedAttributes =
        saxEventHandlerUtils.getCombinedMultiValuedAttributes(descriptors, attributes);

    assertThat(combinedAttributes.size(), is(2));
    assertIdAttribute(combinedAttributes);
    assertLanguageAttribute(combinedAttributes, 2);
  }

  @Test
  public void testInvalidMultiValue() {
    List<Attribute> attributes = getTestAttributes();
    attributes.add(new AttributeImpl(CoreAttributes.ID, "anotherId"));
    List<Attribute> combinedAttributes =
        saxEventHandlerUtils.getCombinedMultiValuedAttributes(descriptors, attributes);

    assertThat(combinedAttributes.size(), is(3));
    Attribute validationWarningAttr =
        getAttributeByName(combinedAttributes, ValidationAttributes.VALIDATION_WARNINGS);
    assertThat(validationWarningAttr, notNullValue());
    assertIdAttribute(combinedAttributes);
    assertLanguageAttribute(combinedAttributes, 1);
  }

  @Test
  public void testNullAttributes() {
    List<Attribute> combinedAttributes =
        saxEventHandlerUtils.getCombinedMultiValuedAttributes(descriptors, null);

    assertThat(combinedAttributes, nullValue());
  }

  @Test
  public void testNullAttributeDescriptors() {
    List<Attribute> attributes = getTestAttributes();
    attributes.add(new AttributeImpl(CoreAttributes.LANGUAGE, "Dothraki"));
    List<Attribute> combinedAttributes =
        saxEventHandlerUtils.getCombinedMultiValuedAttributes(null, attributes);

    assertThat(combinedAttributes.size(), is(3));
    Attribute validationWarningAttr =
        getAttributeByName(combinedAttributes, ValidationAttributes.VALIDATION_WARNINGS);
    assertThat(validationWarningAttr, notNullValue());
    assertAttributeContainsString(
        validationWarningAttr, "No attribute descriptor was found for attribute");
    assertAttributeContainsString(
        validationWarningAttr, "Multiple values found for a non-multi-valued attribute.");
    assertIdAttribute(combinedAttributes);
    assertLanguageAttribute(combinedAttributes, 1);
  }

  @Test
  public void testEmptyAttributes() {
    List<Attribute> combinedAttributes =
        saxEventHandlerUtils.getCombinedMultiValuedAttributes(descriptors, Collections.emptyList());

    assertThat(combinedAttributes, notNullValue());
    assertThat(combinedAttributes, is(empty()));
  }

  @Test
  public void testEmptyAttributeDescriptors() {
    List<Attribute> attributes = getTestAttributes();
    attributes.add(new AttributeImpl(CoreAttributes.LANGUAGE, "Dothraki"));
    List<Attribute> combinedAttributes =
        saxEventHandlerUtils.getCombinedMultiValuedAttributes(Collections.emptySet(), attributes);

    assertThat(combinedAttributes.size(), is(3));
    Attribute validationWarningAttr =
        getAttributeByName(combinedAttributes, ValidationAttributes.VALIDATION_WARNINGS);
    assertThat(validationWarningAttr, notNullValue());
    assertAttributeContainsString(
        validationWarningAttr, "No attribute descriptor was found for attribute");
    assertAttributeContainsString(
        validationWarningAttr, "Multiple values found for a non-multi-valued attribute.");
    assertIdAttribute(combinedAttributes);
    assertLanguageAttribute(combinedAttributes, 1);
  }

  @Test
  public void testGetMultiValuedNameMap() {
    Map<String, Boolean> multiValuedMap = saxEventHandlerUtils.getMultiValuedNameMap(descriptors);
    assertThat(
        multiValuedMap.get(CoreAttributes.ID),
        is(equalTo(coreAttributes.getAttributeDescriptor(CoreAttributes.ID).isMultiValued())));
    assertThat(
        multiValuedMap.get(CoreAttributes.LANGUAGE),
        is(
            equalTo(
                coreAttributes.getAttributeDescriptor(CoreAttributes.LANGUAGE).isMultiValued())));
  }

  @Test
  public void testGetMultiValuedNameMapWithNullDescriptor() {
    Set<AttributeDescriptor> modifiedDescriptors = new HashSet<>(descriptors);
    modifiedDescriptors.add(null);

    Map<String, Boolean> multiValuedMap = saxEventHandlerUtils.getMultiValuedNameMap(descriptors);
    assertThat(
        multiValuedMap.get(CoreAttributes.ID),
        is(equalTo(coreAttributes.getAttributeDescriptor(CoreAttributes.ID).isMultiValued())));
    assertThat(
        multiValuedMap.get(CoreAttributes.LANGUAGE),
        is(
            equalTo(
                coreAttributes.getAttributeDescriptor(CoreAttributes.LANGUAGE).isMultiValued())));

    assertThat(multiValuedMap.size(), is(equalTo(2)));
  }

  @Test
  public void testGetMultiValuedNameMapWithNullOrEmptyDescriptors() {
    Map<String, Boolean> emptyMap = saxEventHandlerUtils.getMultiValuedNameMap(null);
    assertThat(emptyMap.size(), is(equalTo(0)));

    emptyMap = saxEventHandlerUtils.getMultiValuedNameMap(Collections.emptySet());
    assertThat(emptyMap.size(), is(equalTo(0)));
  }

  private List<Attribute> getTestAttributes() {
    List<Attribute> attributes = new ArrayList<>();
    attributes.add(new AttributeImpl(CoreAttributes.ID, "id"));
    attributes.add(new AttributeImpl(CoreAttributes.LANGUAGE, "English"));

    return attributes;
  }

  private Attribute getAttributeByName(List<Attribute> attributes, String name) {
    return attributes.stream()
        .filter(attribute -> attribute.getName().equals(name))
        .findFirst()
        .orElseGet(null);
  }

  private void assertLanguageAttribute(List<Attribute> attributes, int size) {
    Attribute languageAttr = getAttributeByName(attributes, CoreAttributes.LANGUAGE);
    assertThat(languageAttr, notNullValue());
    assertThat(languageAttr.getValues().size(), is(size));
  }

  private void assertIdAttribute(List<Attribute> attributes) {
    Attribute idAttr = getAttributeByName(attributes, CoreAttributes.ID);
    assertThat(idAttr, notNullValue());
    assertThat(idAttr.getValues().size(), is(1));
  }

  private void assertAttributeContainsString(Attribute attribute, String findThisString) {
    List<Serializable> values = attribute.getValues();
    assertThat(values, is(not(empty())));

    long count =
        values.stream()
            .filter(serializable -> serializable instanceof String)
            .map(String.class::cast)
            .filter(string -> string.contains(findThisString))
            .count();
    assertThat(count, is(greaterThan(0L)));
  }
}
