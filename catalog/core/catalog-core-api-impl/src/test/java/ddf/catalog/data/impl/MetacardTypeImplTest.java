/**
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
package ddf.catalog.data.impl;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ddf.catalog.data.impl.BasicTypes.BASIC_METACARD;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import org.junit.Test;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;

public class MetacardTypeImplTest {

    @Test
    public void testNullDescriptors() {

        MetacardType mt = new MetacardTypeImpl("name", null);

        assertTrue(mt.getAttributeDescriptors()
                .isEmpty());

    }

    @Test
    public void testSerializationSingle() throws IOException, ClassNotFoundException {

        HashSet<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();

        descriptors.add(new AttributeDescriptorImpl("id",
                true,
                true,
                false,
                false,
                BasicTypes.STRING_TYPE));

        MetacardTypeImpl metacardType = new MetacardTypeImpl("basic", descriptors);

        String fileLocation = "target/metacardType.ser";

        Serializer<MetacardType> serializer = new Serializer<MetacardType>();

        serializer.serialize(metacardType, fileLocation);

        MetacardType readMetacardType = serializer.deserialize(fileLocation);

        assertEquals(metacardType.getName(), readMetacardType.getName());

        assertEquals(metacardType.getAttributeDescriptor("id")
                        .getName(),
                readMetacardType.getAttributeDescriptor("id")
                        .getName());

        assertEquals(metacardType.getAttributeDescriptor("id")
                        .getType()
                        .getBinding(),
                readMetacardType.getAttributeDescriptor("id")
                        .getType()
                        .getBinding());

        assertEquals(metacardType.getAttributeDescriptor("id")
                        .getType()
                        .getAttributeFormat(),
                readMetacardType.getAttributeDescriptor("id")
                        .getType()
                        .getAttributeFormat());

        Set<AttributeDescriptor> oldAd = metacardType.getAttributeDescriptors();
        Set<AttributeDescriptor> newAd = readMetacardType.getAttributeDescriptors();

        assertTrue(oldAd.iterator()
                .next()
                .equals(newAd.iterator()
                        .next()));

    }

    @Test
    public void testSerializationNullDescriptors() throws IOException, ClassNotFoundException {
        MetacardTypeImpl metacardType = new MetacardTypeImpl("basic", null);

        String fileLocation = "target/metacardType.ser";

        Serializer<MetacardType> serializer = new Serializer<MetacardType>();

        serializer.serialize(metacardType, fileLocation);

        MetacardType readMetacardType = serializer.deserialize(fileLocation);

        assertEquals(metacardType.getName(), readMetacardType.getName());

        Set<AttributeDescriptor> oldAd = metacardType.getAttributeDescriptors();
        Set<AttributeDescriptor> newAd = readMetacardType.getAttributeDescriptors();

        assertTrue(oldAd.isEmpty());
        assertTrue(newAd.isEmpty());
    }

    @Test
    public void testEquals() {

        MetacardTypeImpl metacardType1 = generateMetacardType("metacardType", 0);

        MetacardTypeImpl metacardType2 = generateMetacardType("metacardType", 0);

        assertTrue(metacardType1.equals(metacardType2));
        assertTrue(metacardType2.equals(metacardType1));
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

        MetacardTypeImpl metacardType1 = generateMetacardType("metacardType", 0);

        MetacardTypeImpl metacardType2 = generateMetacardType("metacardType", 1);

        assertTrue(!metacardType1.equals(metacardType2));
        assertTrue(!metacardType2.equals(metacardType1));
    }

    @Test
    public void testEqualsDifferentNames() {

        MetacardTypeImpl metacardType1 = generateMetacardType("differentName", 0);

        MetacardTypeImpl metacardType2 = generateMetacardType("metacardType", 0);

        assertTrue(!metacardType1.equals(metacardType2));
        assertTrue(!metacardType2.equals(metacardType1));
    }

    @Test
    public void testEqualsNullNames() {

        MetacardTypeImpl metacardType1 = generateMetacardType(null, 0);

        MetacardTypeImpl metacardType2 = generateMetacardType(null, 0);

        assertTrue(metacardType1.equals(metacardType2));
        assertTrue(metacardType2.equals(metacardType1));
    }

    @Test
    public void testEqualsNullDescriptors() {

        MetacardTypeImpl metacardType1 = generateMetacardType("name", 2);

        MetacardTypeImpl metacardType2 = generateMetacardType("name", 2);

        assertTrue(metacardType1.equals(metacardType2));
        assertTrue(metacardType2.equals(metacardType1));
    }

    @Test
    public void testEqualsSubClass() {
        HashSet<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();
        descriptors.add(new AttributeDescriptorImpl("id",
                true,
                true,
                false,
                false,
                BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl("title",
                true,
                true,
                false,
                false,
                BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl("frequency",
                true,
                true,
                false,
                false,
                BasicTypes.DOUBLE_TYPE));
        MetacardTypeImplExtended extendedMetacardType = new MetacardTypeImplExtended(
                "metacard-type-extended",
                descriptors,
                "description of metacard type extended");

        MetacardTypeImpl metacardType = generateMetacardType("metacard-type-extended", 0);

        assertTrue(extendedMetacardType.equals(metacardType));
        assertTrue(metacardType.equals(extendedMetacardType));
    }

    @Test
    public void testExtendingMetacardTypeCombinesDescriptors() {
        final Set<AttributeDescriptor> additionalDescriptors = new HashSet<>();
        additionalDescriptors.add(new AttributeDescriptorImpl("foo",
                true,
                false,
                true,
                false,
                BasicTypes.BOOLEAN_TYPE));
        additionalDescriptors.add(new AttributeDescriptorImpl("bar",
                false,
                true,
                false,
                true,
                BasicTypes.STRING_TYPE));

        final String metacardTypeName = "extended";
        final MetacardType extended = new MetacardTypeImpl(metacardTypeName,
                BASIC_METACARD,
                additionalDescriptors);

        assertThat(extended.getName(), is(metacardTypeName));

        final Set<AttributeDescriptor> expectedDescriptors =
                new HashSet<>(BASIC_METACARD.getAttributeDescriptors());
        expectedDescriptors.addAll(additionalDescriptors);
        assertThat(extended.getAttributeDescriptors(), is(expectedDescriptors));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtendingNullMetacardTypeThrowsException() {
        new MetacardTypeImpl("name", null, BASIC_METACARD.getAttributeDescriptors());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtendingMetacardTypeWithNullAdditionalDescriptorsThrowsException() {
        new MetacardTypeImpl("name", BASIC_METACARD, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtendingMetacardTypeWithEmptyAdditionalDescriptorsThrowsException() {
        new MetacardTypeImpl("name", BASIC_METACARD, Collections.emptySet());
    }

    @Test
    public void testExtendedMetacardTypeEqualsEquivalentMetacardType() {
        compareExtendedMetacardTypeToEquivalentMetacardType((extended, equivalent) -> {
            assertThat(extended, is(equivalent));
            assertThat(equivalent, is(extended));
        });
    }

    @Test
    public void testHashCodeExtendedMetacardType() {
        compareExtendedMetacardTypeToEquivalentMetacardType((extended, equivalent) -> {
            assertThat(extended.hashCode(), is(equivalent.hashCode()));
        });
    }

    private void compareExtendedMetacardTypeToEquivalentMetacardType(
            BiConsumer<MetacardType, MetacardType> assertions) {
        final Set<AttributeDescriptor> originalDescriptors =
                new HashSet<>(BASIC_METACARD.getAttributeDescriptors());

        final MetacardType baseMetacardType = new MetacardTypeImpl("base", originalDescriptors);

        final Set<AttributeDescriptor> additionalDescriptors = new HashSet<>();
        additionalDescriptors.add(new AttributeDescriptorImpl("foo",
                true,
                false,
                true,
                false,
                BasicTypes.BOOLEAN_TYPE));
        additionalDescriptors.add(new AttributeDescriptorImpl("bar",
                false,
                true,
                false,
                true,
                BasicTypes.STRING_TYPE));

        final MetacardType extendedMetacardType = new MetacardTypeImpl("type",
                baseMetacardType,
                additionalDescriptors);

        final Set<AttributeDescriptor> combinedDescriptors = new HashSet<>(originalDescriptors);
        combinedDescriptors.addAll(additionalDescriptors);

        final MetacardType equivalentMetacardType = new MetacardTypeImpl("type",
                combinedDescriptors);

        assertions.accept(extendedMetacardType, equivalentMetacardType);
    }

    private MetacardTypeImpl generateMetacardType(String name, int descriptorSetIndex) {

        HashSet<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();
        switch (descriptorSetIndex) {
        case 0:
            descriptors.add(new AttributeDescriptorImpl("id",
                    true,
                    true,
                    false,
                    false,
                    BasicTypes.STRING_TYPE));
            descriptors.add(new AttributeDescriptorImpl("title",
                    true,
                    true,
                    false,
                    false,
                    BasicTypes.STRING_TYPE));
            descriptors.add(new AttributeDescriptorImpl("frequency",
                    true,
                    true,
                    false,
                    false,
                    BasicTypes.DOUBLE_TYPE));
            break;
        case 1:
            descriptors.add(new AttributeDescriptorImpl("id",
                    true,
                    true,
                    false,
                    false,
                    BasicTypes.STRING_TYPE));
            descriptors.add(new AttributeDescriptorImpl("title",
                    true,
                    true,
                    false,
                    false,
                    BasicTypes.STRING_TYPE));
            descriptors.add(new AttributeDescriptorImpl("height",
                    true,
                    true,
                    false,
                    false,
                    BasicTypes.DOUBLE_TYPE));
            break;
        case 2:
            descriptors = null;
            break;
        }

        return new MetacardTypeImpl(name, descriptors);
    }

    private class MetacardTypeImplExtended extends MetacardTypeImpl {

        private String description;

        public MetacardTypeImplExtended(String name, Set<AttributeDescriptor> descriptors,
                String description) {
            super(name, descriptors);
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

    }

}
