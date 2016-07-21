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
package ddf.catalog.data.defaultvalues;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ddf.catalog.data.Metacard.POINT_OF_CONTACT;
import static ddf.catalog.data.Metacard.TITLE;
import static ddf.catalog.data.impl.BasicTypes.BASIC_METACARD;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.DefaultAttributeValueImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;

public class DefaultAttributeValueRegistryTest {
    private static final String BASIC_METACARD_NAME = BASIC_METACARD.getName();

    private static final String OTHER_METACARD_NAME = "other";

    private static final String DEFAULT_1 = "foo";

    private static final String DEFAULT_2 = "bar";

    private static final String DEFAULT_3 = "foobar";

    private static final String CUSTOM_METACARD_TYPE_NAME = "custom";

    private static final String DEFAULT_TITLE = "Default Title";

    private static final String DEFAULT_TITLE_CUSTOM = "Custom Title";

    private static final Date DEFAULT_EXPIRATION = Date.from(Instant.now()
            .minus(1, DAYS));

    private static final Date DEFAULT_EXPIRATION_CUSTOM = Date.from(Instant.now()
            .minus(2, DAYS));

    private DefaultAttributeValueRegistry registry;

    @Before
    public void setUp() {
        registry = new DefaultAttributeValueRegistry();
    }

    @Test
    public void testaddDefaultAttribute() {
        registry.addDefaultValue(new DefaultAttributeValueImpl(BASIC_METACARD_NAME,
                TITLE,
                DEFAULT_1), null);
        verifyRegistryDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_1);
    }

    @Test
    public void testaddDefaultAttributesForDifferentMetacardTypes() {
        registry.addDefaultValue(new DefaultAttributeValueImpl(BASIC_METACARD_NAME,
                TITLE,
                DEFAULT_1), null);
        registry.addDefaultValue(new DefaultAttributeValueImpl(OTHER_METACARD_NAME,
                TITLE,
                DEFAULT_2), null);

        verifyRegistryDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_1);
        verifyRegistryDefaultValue(OTHER_METACARD_NAME, TITLE, DEFAULT_2);
    }

    @Test
    public void testOverwriteDefaultValue() {
        registry.addDefaultValue(new DefaultAttributeValueImpl(BASIC_METACARD_NAME,
                TITLE,
                DEFAULT_1), null);
        registry.addDefaultValue(new DefaultAttributeValueImpl(BASIC_METACARD_NAME,
                TITLE,
                DEFAULT_2), null);
        verifyRegistryDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_2);
    }

    @Test
    public void testGetUnregisteredMetacardType() {
        verifyRegistryDefaultValueNotPresent(BASIC_METACARD_NAME, TITLE);
    }

    @Test
    public void testGetUnregisteredAttribute() {
        registry.addDefaultValue(new DefaultAttributeValueImpl(BASIC_METACARD_NAME,
                POINT_OF_CONTACT,
                DEFAULT_1), null);
        verifyRegistryDefaultValueNotPresent(BASIC_METACARD_NAME, TITLE);
    }

    @Test
    public void testRemoveSingleDefaultValue() {
        registry.addDefaultValue(new DefaultAttributeValueImpl(TITLE, DEFAULT_1), null);
        registry.addDefaultValue(new DefaultAttributeValueImpl(BASIC_METACARD_NAME,
                TITLE,
                DEFAULT_1), null);
        registry.addDefaultValue(new DefaultAttributeValueImpl(BASIC_METACARD_NAME,
                POINT_OF_CONTACT,
                DEFAULT_2), null);

        registry.removeDefaultValue(new DefaultAttributeValueImpl(BASIC_METACARD_NAME,
                TITLE,
                DEFAULT_1), null);
        registry.removeDefaultValue(new DefaultAttributeValueImpl(TITLE, DEFAULT_1), null);

        verifyRegistryDefaultValue(BASIC_METACARD_NAME, POINT_OF_CONTACT, DEFAULT_2);
        verifyRegistryDefaultValueNotPresent(BASIC_METACARD_NAME, TITLE);
        verifyRegistryDefaultValueNotPresent(TITLE);
    }

    @Test
    public void testGlobalDefaults() {
        registry.addDefaultValue(new DefaultAttributeValueImpl(TITLE, DEFAULT_1), null);
        registry.addDefaultValue(new DefaultAttributeValueImpl(BASIC_METACARD_NAME,
                TITLE,
                DEFAULT_3), null);
        registry.addDefaultValue(new DefaultAttributeValueImpl(POINT_OF_CONTACT, DEFAULT_2), null);

        verifyRegistryDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_3);
        verifyRegistryDefaultValue(OTHER_METACARD_NAME, TITLE, DEFAULT_1);
        verifyRegistryDefaultValue(BASIC_METACARD_NAME, POINT_OF_CONTACT, DEFAULT_2);
        verifyRegistryDefaultValue(OTHER_METACARD_NAME, POINT_OF_CONTACT, DEFAULT_2);
    }

    @Test
    public void testFallbackToGlobalDefault() {
        registry.addDefaultValue(new DefaultAttributeValueImpl(TITLE, DEFAULT_1), null);
        registry.addDefaultValue(new DefaultAttributeValueImpl(BASIC_METACARD_NAME,
                TITLE,
                DEFAULT_2), null);
        verifyRegistryDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_2);

        registry.removeDefaultValue(new DefaultAttributeValueImpl(BASIC_METACARD_NAME,
                TITLE,
                DEFAULT_2), null);

        verifyRegistryDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_1);
    }

    private void verifyRegistryDefaultValue(String metacardTypeName, String attributeName,
            String expectedValue) {
        final Optional<Serializable> defaultValueOptional = registry.getDefaultValue(
                metacardTypeName,
                attributeName);
        assertThat(defaultValueOptional.isPresent(), is(true));
        assertThat(defaultValueOptional.get(), is(expectedValue));
    }

    private void verifyRegistryDefaultValueNotPresent(String metacardTypeName,
            String attributeName) {
        final Optional<Serializable> defaultValueOptional = registry.getDefaultValue(
                metacardTypeName,
                attributeName);
        assertThat(defaultValueOptional.isPresent(), is(false));
    }

    private void verifyRegistryDefaultValueNotPresent(String attributeName) {
        final Optional<Serializable> defaultValueOptional = registry.getDefaultValue(attributeName);
        assertThat(defaultValueOptional.isPresent(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDefaultValueNullAttribute() {
        registry.getDefaultValue(BASIC_METACARD_NAME, null);
    }

    @Test
    public void testCreateWithDefaultValues() throws IngestException, SourceUnavailableException {
        registerDefaults();

        final String title = "some title";
        final Date expiration = new Date();
        List<Metacard> metacards = getMetacards(title, expiration);
        metacards.forEach(registry::addDefaults);
        verifyDefaults(metacards,
                title,
                expiration,
                DEFAULT_TITLE,
                DEFAULT_EXPIRATION,
                DEFAULT_TITLE_CUSTOM,
                DEFAULT_EXPIRATION_CUSTOM);
    }

    private void registerDefaults() {
        registry.addDefaultValue(new DefaultAttributeValueImpl(Metacard.TITLE, DEFAULT_TITLE),
                null);
        registry.addDefaultValue(new DefaultAttributeValueImpl(CUSTOM_METACARD_TYPE_NAME,
                Metacard.TITLE,
                DEFAULT_TITLE_CUSTOM), null);
        registry.addDefaultValue(new DefaultAttributeValueImpl(Metacard.EXPIRATION,
                DEFAULT_EXPIRATION), null);
        registry.addDefaultValue(new DefaultAttributeValueImpl(CUSTOM_METACARD_TYPE_NAME,
                Metacard.EXPIRATION,
                DEFAULT_EXPIRATION_CUSTOM), null);
    }

    private void verifyDefaults(List<Metacard> metacards, String originalTitle,
            Date originalExpiration, String expectedDefaultTitle, Date expectedDefaultExpiration,
            String expectedDefaultTitleCustom, Date expectedDefaultDateCustom) {
        Metacard neitherDefault = metacards.get(0);
        assertThat(neitherDefault.getTitle(), is(originalTitle));
        assertThat(neitherDefault.getExpirationDate(), is(originalExpiration));

        Metacard expirationDefault = metacards.get(1);
        assertThat(expirationDefault.getTitle(), is(originalTitle));
        assertThat(expirationDefault.getExpirationDate(), is(expectedDefaultExpiration));

        Metacard titleDefault = metacards.get(2);
        assertThat(titleDefault.getTitle(), is(expectedDefaultTitle));
        assertThat(titleDefault.getExpirationDate(), is(originalExpiration));

        Metacard basicBothDefault = metacards.get(3);
        assertThat(basicBothDefault.getTitle(), is(expectedDefaultTitle));
        assertThat(basicBothDefault.getExpirationDate(), is(expectedDefaultExpiration));

        Metacard customBothDefault = metacards.get(4);
        assertThat(customBothDefault.getTitle(), is(expectedDefaultTitleCustom));
        assertThat(customBothDefault.getExpirationDate(), is(expectedDefaultDateCustom));
    }

    private List<Metacard> getMetacards(String title, Date expiration) {
        List<Metacard> metacards = new ArrayList<>();

        MetacardImpl basicMetacardHasBoth = new MetacardImpl(BasicTypes.BASIC_METACARD);
        basicMetacardHasBoth.setId("1");
        basicMetacardHasBoth.setTitle(title);
        basicMetacardHasBoth.setExpirationDate(expiration);
        metacards.add(basicMetacardHasBoth);

        MetacardImpl basicMetacardHasTitle = new MetacardImpl(BasicTypes.BASIC_METACARD);
        basicMetacardHasTitle.setId("2");
        basicMetacardHasTitle.setTitle(title);
        metacards.add(basicMetacardHasTitle);

        MetacardImpl basicMetacardHasExpiration = new MetacardImpl(BasicTypes.BASIC_METACARD);
        basicMetacardHasExpiration.setId("3");
        basicMetacardHasExpiration.setExpirationDate(expiration);
        metacards.add(basicMetacardHasExpiration);

        MetacardImpl basicMetacardHasNeither = new MetacardImpl(BasicTypes.BASIC_METACARD);
        basicMetacardHasNeither.setId("4");
        metacards.add(basicMetacardHasNeither);

        MetacardType customMetacardType = new MetacardTypeImpl(CUSTOM_METACARD_TYPE_NAME,
                BasicTypes.BASIC_METACARD.getAttributeDescriptors());
        MetacardImpl customMetacardHasNeither = new MetacardImpl(customMetacardType);
        customMetacardHasNeither.setId("5");
        metacards.add(customMetacardHasNeither);

        return metacards;
    }

}
