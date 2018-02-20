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
package ddf.catalog.data.impl;

import static ddf.catalog.data.impl.MetacardImpl.BASIC_METACARD;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.types.AssociationsAttributes;
import ddf.catalog.data.impl.types.ContactAttributes;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.DateTimeAttributes;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.impl.types.MediaAttributes;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.impl.types.TopicAttributes;
import ddf.catalog.data.impl.types.ValidationAttributes;
import ddf.catalog.data.impl.types.VersionAttributes;
import ddf.catalog.data.types.Core;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import org.junit.Test;

public class MetacardTypeImplTest {

  private static final String ID = "id";

  private static final String NAME = "name";

  private static final String TEST_NAME = "testType";

  private static final String METACARD_TYPE = "metacardType";

  private static final AssociationsAttributes ASSOCIATIONS_ATTRIBUTES =
      new AssociationsAttributes();

  private static final ContactAttributes CONTACT_ATTRIBUTES = new ContactAttributes();

  private static final DateTimeAttributes DATE_TIME_ATTRIBUTES = new DateTimeAttributes();

  private static final VersionAttributes VERSION_ATTRIBUTES = new VersionAttributes();

  private static final LocationAttributes LOCATION_ATTRIBUTES = new LocationAttributes();

  private static final MediaAttributes MEDIA_ATTRIBUTES = new MediaAttributes();

  private static final TopicAttributes TOPIC_ATTRIBUTES = new TopicAttributes();

  private static final ValidationAttributes VALIDATION_ATTRIBUTES = new ValidationAttributes();

  private static final CoreAttributes CORE_ATTRIBUTES = new CoreAttributes();

  private static final SecurityAttributes SECURITY_ATTRIBUTES = new SecurityAttributes();

  @Test
  public void testNullAttributeDescriptors() {
    MetacardType mt = new MetacardTypeImpl(NAME, (Set<AttributeDescriptor>) null);
    assertThat(mt.getAttributeDescriptors(), hasSize(0));
  }

  @Test
  public void testNullMetacardTypes() {
    MetacardType mt = new MetacardTypeImpl(TEST_NAME, (List<MetacardType>) null);
    assertMetacardAttributes(mt, CORE_ATTRIBUTES.getAttributeDescriptors());
  }

  @Test
  public void testSerializationSingle() throws IOException, ClassNotFoundException {
    HashSet<AttributeDescriptor> descriptors = new HashSet<>();

    descriptors.add(
        new AttributeDescriptorImpl(ID, true, true, false, false, BasicTypes.STRING_TYPE));

    MetacardTypeImpl metacardType = new MetacardTypeImpl("basic", descriptors);

    String fileLocation = "target/metacardType.ser";

    Serializer<MetacardType> serializer = new Serializer<>();

    serializer.serialize(metacardType, fileLocation);

    MetacardType readMetacardType = serializer.deserialize(fileLocation);

    assertThat(metacardType.getName(), is(readMetacardType.getName()));

    assertThat(
        metacardType.getAttributeDescriptor(ID).getName(),
        is(readMetacardType.getAttributeDescriptor(ID).getName()));

    assertEquals(
        metacardType.getAttributeDescriptor(ID).getType().getBinding(),
        readMetacardType.getAttributeDescriptor(ID).getType().getBinding());

    assertThat(
        metacardType.getAttributeDescriptor(ID).getType().getAttributeFormat(),
        is(readMetacardType.getAttributeDescriptor(ID).getType().getAttributeFormat()));

    Set<AttributeDescriptor> oldAd = metacardType.getAttributeDescriptors();
    Set<AttributeDescriptor> newAd = readMetacardType.getAttributeDescriptors();

    assertThat(oldAd.iterator().next(), is(newAd.iterator().next()));
  }

  @Test
  public void testSerializationNullDescriptors() throws IOException, ClassNotFoundException {
    MetacardTypeImpl metacardType = new MetacardTypeImpl("basic", (Set<AttributeDescriptor>) null);

    String fileLocation = "target/metacardType.ser";

    Serializer<MetacardType> serializer = new Serializer<>();

    serializer.serialize(metacardType, fileLocation);

    MetacardType readMetacardType = serializer.deserialize(fileLocation);

    assertThat(metacardType.getName(), is(readMetacardType.getName()));

    Set<AttributeDescriptor> oldAd = metacardType.getAttributeDescriptors();
    Set<AttributeDescriptor> newAd = readMetacardType.getAttributeDescriptors();

    assertThat(oldAd.isEmpty(), is(true));
    assertThat(newAd.isEmpty(), is(true));
  }

  @Test
  public void testEquals() {
    MetacardTypeImpl metacardType1 = generateMetacardType(METACARD_TYPE, 0);
    MetacardTypeImpl metacardType2 = generateMetacardType(METACARD_TYPE, 0);
    assertThat(metacardType1.equals(metacardType2), is(true));
    assertThat(metacardType2.equals(metacardType1), is(true));
  }

  @Test
  public void testHashCode() {
    MetacardTypeImpl metacardType1 = generateMetacardType("test", 0);
    MetacardTypeImpl metacardType2 = generateMetacardType("test", 0);
    assertThat(metacardType1.hashCode(), is(metacardType2.hashCode()));
  }

  @Test
  public void testHashCodeDifferentDescriptors() {
    MetacardTypeImpl metacardType1 = generateMetacardType("test", 0);
    MetacardTypeImpl metacardType2 = generateMetacardType("test", 1);
    assertThat(metacardType1.hashCode(), is(not(metacardType2.hashCode())));
  }

  @Test
  public void testHashCodeDifferentNames() {
    MetacardTypeImpl metacardType1 = generateMetacardType("foo", 0);
    MetacardTypeImpl metacardType2 = generateMetacardType("bar", 0);
    assertThat(metacardType1.hashCode(), is(not(metacardType2.hashCode())));
  }

  @Test
  public void testEqualsDifferentDescriptors() {
    MetacardTypeImpl metacardType1 = generateMetacardType(METACARD_TYPE, 0);
    MetacardTypeImpl metacardType2 = generateMetacardType(METACARD_TYPE, 1);
    assertThat(metacardType1.equals(metacardType2), is(false));
    assertThat(metacardType2.equals(metacardType1), is(false));
  }

  @Test
  public void testEqualsDifferentNames() {
    MetacardTypeImpl metacardType1 = generateMetacardType("differentName", 0);
    MetacardTypeImpl metacardType2 = generateMetacardType(METACARD_TYPE, 0);
    assertThat(metacardType1.equals(metacardType2), is(false));
    assertThat(metacardType2.equals(metacardType1), is(false));
  }

  @Test
  public void testEqualsNullNames() {
    MetacardTypeImpl metacardType1 = generateMetacardType(null, 0);
    MetacardTypeImpl metacardType2 = generateMetacardType(null, 0);
    assertThat(metacardType1.equals(metacardType2), is(true));
    assertThat(metacardType2.equals(metacardType1), is(true));
  }

  @Test
  public void testEqualsNullDescriptors() {
    MetacardTypeImpl metacardType1 = generateMetacardType(NAME, 2);
    MetacardTypeImpl metacardType2 = generateMetacardType(NAME, 2);
    assertThat(metacardType1.equals(metacardType2), is(true));
    assertThat(metacardType2.equals(metacardType1), is(true));
  }

  @Test
  public void testEqualsSubClass() {
    HashSet<AttributeDescriptor> descriptors = new HashSet<>();
    descriptors.add(
        new AttributeDescriptorImpl(ID, true, true, false, false, BasicTypes.STRING_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl("title", true, true, false, false, BasicTypes.STRING_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl("frequency", true, true, false, false, BasicTypes.DOUBLE_TYPE));
    MetacardTypeImplExtended extendedMetacardType =
        new MetacardTypeImplExtended(
            "metacard-type-extended", descriptors, "description of metacard type extended");

    MetacardTypeImpl metacardType = generateMetacardType("metacard-type-extended", 0);

    assertThat(extendedMetacardType, is(metacardType));
    assertThat(metacardType, is(extendedMetacardType));
  }

  @Test
  public void testExtendingMetacardTypeCombinesDescriptors() {
    final Set<AttributeDescriptor> additionalDescriptors = new HashSet<>();
    additionalDescriptors.add(
        new AttributeDescriptorImpl("foo", true, false, true, false, BasicTypes.BOOLEAN_TYPE));
    additionalDescriptors.add(
        new AttributeDescriptorImpl("bar", false, true, false, true, BasicTypes.STRING_TYPE));

    final String metacardTypeName = "extended";
    final MetacardType extended =
        new MetacardTypeImpl(metacardTypeName, BASIC_METACARD, additionalDescriptors);

    assertThat(extended.getName(), is(metacardTypeName));

    final Set<AttributeDescriptor> expectedDescriptors =
        new HashSet<>(BASIC_METACARD.getAttributeDescriptors());
    expectedDescriptors.addAll(additionalDescriptors);
    assertThat(extended.getAttributeDescriptors(), is(expectedDescriptors));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExtendingNullMetacardTypeThrowsException() {
    new MetacardTypeImpl(NAME, null, BASIC_METACARD.getAttributeDescriptors());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExtendingMetacardTypeWithNullAdditionalDescriptorsThrowsException() {
    new MetacardTypeImpl(NAME, BASIC_METACARD, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExtendingMetacardTypeWithEmptyAdditionalDescriptorsThrowsException() {
    new MetacardTypeImpl(NAME, BASIC_METACARD, Collections.emptySet());
  }

  @Test
  public void testExtendedMetacardTypeEqualsEquivalentMetacardType() {
    compareExtendedMetacardTypeToEquivalentMetacardType(
        (extended, equivalent) -> {
          assertThat(extended, is(equivalent));
          assertThat(equivalent, is(extended));
        });
  }

  @Test
  public void testHashCodeExtendedMetacardType() {
    compareExtendedMetacardTypeToEquivalentMetacardType(
        (extended, equivalent) -> {
          assertThat(extended.hashCode(), is(equivalent.hashCode()));
        });
  }

  @Test
  public void testBasicType() {
    MetacardType metacardType = new MetacardTypeImpl(TEST_NAME, new ArrayList<>());
    assertMetacardAttributes(metacardType, CORE_ATTRIBUTES.getAttributeDescriptors());
  }

  @Test
  public void testContactType() {
    List<MetacardType> metacardTypeList = new ArrayList<>();
    metacardTypeList.add(CONTACT_ATTRIBUTES);
    MetacardType metacardType = new MetacardTypeImpl(TEST_NAME, metacardTypeList);
    assertMetacardAttributes(metacardType, CORE_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, CONTACT_ATTRIBUTES.getAttributeDescriptors());
  }

  @Test
  public void testLocationType() {
    List<MetacardType> metacardTypeList = new ArrayList<>();
    metacardTypeList.add(LOCATION_ATTRIBUTES);
    MetacardType metacardType = new MetacardTypeImpl(TEST_NAME, metacardTypeList);
    assertMetacardAttributes(metacardType, CORE_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, LOCATION_ATTRIBUTES.getAttributeDescriptors());
  }

  @Test
  public void testDateTimeType() {
    List<MetacardType> metacardTypeList = new ArrayList<>();
    metacardTypeList.add(DATE_TIME_ATTRIBUTES);
    MetacardType metacardType = new MetacardTypeImpl(TEST_NAME, metacardTypeList);
    assertMetacardAttributes(metacardType, CORE_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, DATE_TIME_ATTRIBUTES.getAttributeDescriptors());
  }

  @Test
  public void testMediaType() {
    List<MetacardType> metacardTypeList = new ArrayList<>();
    metacardTypeList.add(MEDIA_ATTRIBUTES);
    MetacardType metacardType = new MetacardTypeImpl(TEST_NAME, metacardTypeList);
    assertMetacardAttributes(metacardType, CORE_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, MEDIA_ATTRIBUTES.getAttributeDescriptors());
  }

  @Test
  public void testTopicType() {
    List<MetacardType> metacardTypeList = new ArrayList<>();
    metacardTypeList.add(TOPIC_ATTRIBUTES);
    MetacardType metacardType = new MetacardTypeImpl(TEST_NAME, metacardTypeList);
    assertMetacardAttributes(metacardType, CORE_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, TOPIC_ATTRIBUTES.getAttributeDescriptors());
  }

  @Test
  public void testValidationType() {
    List<MetacardType> metacardTypeList = new ArrayList<>();
    metacardTypeList.add(VALIDATION_ATTRIBUTES);
    MetacardType metacardType = new MetacardTypeImpl(TEST_NAME, metacardTypeList);
    assertMetacardAttributes(metacardType, CORE_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, VALIDATION_ATTRIBUTES.getAttributeDescriptors());
  }

  @Test
  public void testHistoryType() {
    List<MetacardType> metacardTypeList = new ArrayList<>();
    metacardTypeList.add(VERSION_ATTRIBUTES);
    MetacardType metacardType = new MetacardTypeImpl(TEST_NAME, metacardTypeList);
    assertMetacardAttributes(metacardType, CORE_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, VERSION_ATTRIBUTES.getAttributeDescriptors());
  }

  @Test
  public void testAssociationsType() {
    List<MetacardType> metacardTypeList = new ArrayList<>();
    metacardTypeList.add(ASSOCIATIONS_ATTRIBUTES);
    MetacardType metacardType = new MetacardTypeImpl(TEST_NAME, metacardTypeList);
    assertMetacardAttributes(metacardType, CORE_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, ASSOCIATIONS_ATTRIBUTES.getAttributeDescriptors());
  }

  @Test
  public void testAllTypes() {
    List<MetacardType> metacardTypeList = new ArrayList<>();
    metacardTypeList.add(DATE_TIME_ATTRIBUTES);
    metacardTypeList.add(MEDIA_ATTRIBUTES);
    metacardTypeList.add(LOCATION_ATTRIBUTES);
    metacardTypeList.add(CONTACT_ATTRIBUTES);
    metacardTypeList.add(TOPIC_ATTRIBUTES);
    metacardTypeList.add(VERSION_ATTRIBUTES);
    metacardTypeList.add(ASSOCIATIONS_ATTRIBUTES);
    metacardTypeList.add(VALIDATION_ATTRIBUTES);
    MetacardType metacardType = new MetacardTypeImpl(TEST_NAME, metacardTypeList);
    assertMetacardAttributes(metacardType, CORE_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, DATE_TIME_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, MEDIA_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, LOCATION_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, CONTACT_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, TOPIC_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, VERSION_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, ASSOCIATIONS_ATTRIBUTES.getAttributeDescriptors());
    assertMetacardAttributes(metacardType, VALIDATION_ATTRIBUTES.getAttributeDescriptors());
  }

  @Test
  public void testDuplicateAttributes() {
    MetacardType duplicateAttributeMetacardType =
        new MetacardTypeImpl(
            "testType",
            Collections.singleton(
                new AttributeDescriptorImpl(
                    Core.CHECKSUM,
                    true /* indexed */,
                    true /* stored */,
                    false /* tokenized */,
                    false /* multivalued */,
                    BasicTypes.SHORT_TYPE)));
    List<MetacardType> metacardTypeList = new ArrayList<>();
    metacardTypeList.add(DATE_TIME_ATTRIBUTES);
    metacardTypeList.add(duplicateAttributeMetacardType);

    MetacardType metacardType = new MetacardTypeImpl(TEST_NAME, metacardTypeList);

    int expectedSize =
        DATE_TIME_ATTRIBUTES.getAttributeDescriptors().size()
            + CORE_ATTRIBUTES.getAttributeDescriptors().size()
            + SECURITY_ATTRIBUTES.getAttributeDescriptors().size();

    assertThat(metacardType.getAttributeDescriptors().size(), is(expectedSize));
  }

  private void assertMetacardAttributes(
      MetacardType metacardType, Set<AttributeDescriptor> expected) {
    Set<AttributeDescriptor> actual = metacardType.getAttributeDescriptors();
    assertThat(metacardType.getName(), is(TEST_NAME));
    for (AttributeDescriptor attributeDescriptor : expected) {
      assertThat(actual, hasItem(attributeDescriptor));
    }
  }

  private void compareExtendedMetacardTypeToEquivalentMetacardType(
      BiConsumer<MetacardType, MetacardType> assertions) {
    final Set<AttributeDescriptor> originalDescriptors =
        new HashSet<>(BASIC_METACARD.getAttributeDescriptors());

    final MetacardType baseMetacardType = new MetacardTypeImpl("base", originalDescriptors);

    final Set<AttributeDescriptor> additionalDescriptors = new HashSet<>();
    additionalDescriptors.add(
        new AttributeDescriptorImpl("foo", true, false, true, false, BasicTypes.BOOLEAN_TYPE));
    additionalDescriptors.add(
        new AttributeDescriptorImpl("bar", false, true, false, true, BasicTypes.STRING_TYPE));

    final MetacardType extendedMetacardType =
        new MetacardTypeImpl("type", baseMetacardType, additionalDescriptors);

    final Set<AttributeDescriptor> combinedDescriptors = new HashSet<>(originalDescriptors);
    combinedDescriptors.addAll(additionalDescriptors);

    final MetacardType equivalentMetacardType = new MetacardTypeImpl("type", combinedDescriptors);

    assertions.accept(extendedMetacardType, equivalentMetacardType);
  }

  private MetacardTypeImpl generateMetacardType(String name, int descriptorSetIndex) {

    HashSet<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();
    switch (descriptorSetIndex) {
      case 0:
        descriptors.add(
            new AttributeDescriptorImpl(ID, true, true, false, false, BasicTypes.STRING_TYPE));
        descriptors.add(
            new AttributeDescriptorImpl("title", true, true, false, false, BasicTypes.STRING_TYPE));
        descriptors.add(
            new AttributeDescriptorImpl(
                "frequency", true, true, false, false, BasicTypes.DOUBLE_TYPE));
        break;
      case 1:
        descriptors.add(
            new AttributeDescriptorImpl(ID, true, true, false, false, BasicTypes.STRING_TYPE));
        descriptors.add(
            new AttributeDescriptorImpl("title", true, true, false, false, BasicTypes.STRING_TYPE));
        descriptors.add(
            new AttributeDescriptorImpl(
                "height", true, true, false, false, BasicTypes.DOUBLE_TYPE));
        break;
      case 2:
        descriptors = null;
        break;
    }

    return new MetacardTypeImpl(name, descriptors);
  }

  private class MetacardTypeImplExtended extends MetacardTypeImpl {

    private String description;

    public MetacardTypeImplExtended(
        String name, Set<AttributeDescriptor> descriptors, String description) {
      super(name, descriptors);
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }
}
