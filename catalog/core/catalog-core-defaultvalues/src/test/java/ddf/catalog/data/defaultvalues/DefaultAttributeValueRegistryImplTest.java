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
package ddf.catalog.data.defaultvalues;

import static ddf.catalog.data.Metacard.POINT_OF_CONTACT;
import static ddf.catalog.data.impl.MetacardImpl.BASIC_METACARD;
import static ddf.catalog.data.types.Core.TITLE;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.DefaultAttributeValueRegistry;
import java.io.Serializable;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class DefaultAttributeValueRegistryImplTest {
  private static final String BASIC_METACARD_NAME = BASIC_METACARD.getName();

  private static final String OTHER_METACARD_NAME = "other";

  private static final String DEFAULT_1 = "foo";

  private static final String DEFAULT_2 = "bar";

  private static final String DEFAULT_3 = "foobar";

  private DefaultAttributeValueRegistry registry;

  @Before
  public void setUp() {
    registry = new DefaultAttributeValueRegistryImpl();
  }

  @Test
  public void testSetDefaultValue() {
    registry.setDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_1);
    verifyRegistryDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_1);
  }

  @Test
  public void testSetDefaultValuesForDifferentMetacardTypes() {
    registry.setDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_1);
    registry.setDefaultValue(OTHER_METACARD_NAME, TITLE, DEFAULT_2);

    verifyRegistryDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_1);
    verifyRegistryDefaultValue(OTHER_METACARD_NAME, TITLE, DEFAULT_2);
  }

  @Test
  public void testOverwriteDefaultValue() {
    registry.setDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_1);
    registry.setDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_2);
    verifyRegistryDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_2);
  }

  @Test
  public void testGetUnregisteredMetacardType() {
    verifyRegistryDefaultValueNotPresent(BASIC_METACARD_NAME, TITLE);
  }

  @Test
  public void testGetUnregisteredAttribute() {
    registry.setDefaultValue(BASIC_METACARD_NAME, POINT_OF_CONTACT, DEFAULT_1);
    verifyRegistryDefaultValueNotPresent(BASIC_METACARD_NAME, TITLE);
  }

  @Test
  public void testRemoveSingleDefaultValue() {
    registry.setDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_1);
    registry.setDefaultValue(BASIC_METACARD_NAME, POINT_OF_CONTACT, DEFAULT_2);

    registry.removeDefaultValue(BASIC_METACARD_NAME, TITLE);

    verifyRegistryDefaultValue(BASIC_METACARD_NAME, POINT_OF_CONTACT, DEFAULT_2);
    verifyRegistryDefaultValueNotPresent(BASIC_METACARD_NAME, TITLE);
  }

  @Test
  public void testRemoveAllDefaultValuesForMetacardType() {
    registry.setDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_1);
    registry.setDefaultValue(BASIC_METACARD_NAME, POINT_OF_CONTACT, DEFAULT_2);
    registry.setDefaultValue(OTHER_METACARD_NAME, TITLE, DEFAULT_3);

    registry.removeDefaultValues(BASIC_METACARD_NAME);

    verifyRegistryDefaultValueNotPresent(BASIC_METACARD_NAME, TITLE);
    verifyRegistryDefaultValueNotPresent(BASIC_METACARD_NAME, POINT_OF_CONTACT);
    verifyRegistryDefaultValue(OTHER_METACARD_NAME, TITLE, DEFAULT_3);
  }

  @Test
  public void testGlobalDefaults() {
    registry.setDefaultValue(TITLE, DEFAULT_1);
    registry.setDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_3);
    registry.setDefaultValue(POINT_OF_CONTACT, DEFAULT_2);

    verifyRegistryDefaultValue(BASIC_METACARD_NAME, TITLE, DEFAULT_3);
    verifyRegistryDefaultValue(OTHER_METACARD_NAME, TITLE, DEFAULT_1);
    verifyRegistryDefaultValue(BASIC_METACARD_NAME, POINT_OF_CONTACT, DEFAULT_2);
    verifyRegistryDefaultValue(OTHER_METACARD_NAME, POINT_OF_CONTACT, DEFAULT_2);
  }

  @Test
  public void testRemoveGlobalDefault() {
    registry.setDefaultValue(TITLE, DEFAULT_1);
    registry.setDefaultValue(POINT_OF_CONTACT, DEFAULT_2);

    registry.removeDefaultValue(TITLE);

    verifyRegistryDefaultValue(BASIC_METACARD_NAME, POINT_OF_CONTACT, DEFAULT_2);
    verifyRegistryDefaultValueNotPresent(BASIC_METACARD_NAME, TITLE);
  }

  @Test
  public void testRemoveAllGlobalDefaults() {
    registry.setDefaultValue(TITLE, DEFAULT_1);
    registry.setDefaultValue(POINT_OF_CONTACT, DEFAULT_2);

    registry.removeDefaultValues();

    verifyRegistryDefaultValueNotPresent(BASIC_METACARD_NAME, TITLE);
    verifyRegistryDefaultValueNotPresent(BASIC_METACARD_NAME, POINT_OF_CONTACT);
  }

  private void verifyRegistryDefaultValue(
      String metacardTypeName, String attributeName, String expectedValue) {
    final Optional<Serializable> defaultValueOptional =
        registry.getDefaultValue(metacardTypeName, attributeName);
    assertThat(defaultValueOptional.isPresent(), is(true));
    assertThat(defaultValueOptional.get(), is(expectedValue));
  }

  private void verifyRegistryDefaultValueNotPresent(String metacardTypeName, String attributeName) {
    final Optional<Serializable> defaultValueOptional =
        registry.getDefaultValue(metacardTypeName, attributeName);
    assertThat(defaultValueOptional.isPresent(), is(false));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRegisterGlobalDefaultNullAttribute() {
    registry.setDefaultValue(null, DEFAULT_1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRegisterGlobalDefaultNullValue() {
    registry.setDefaultValue(TITLE, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRegisterMetacardDefaultNullMetacardType() {
    registry.setDefaultValue(null, TITLE, DEFAULT_1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRegisterMetacardDefaultNullAttribute() {
    registry.setDefaultValue(BASIC_METACARD_NAME, null, DEFAULT_1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRegisterMetacardDefaultNullValue() {
    registry.setDefaultValue(BASIC_METACARD_NAME, TITLE, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetDefaultValueNullMetacardType() {
    registry.getDefaultValue(null, TITLE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetDefaultValueNullAttribute() {
    registry.getDefaultValue(BASIC_METACARD_NAME, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveGlobalDefaultValueNullAttribute() {
    registry.removeDefaultValue(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveMetacardDefaultNullMetacardType() {
    registry.removeDefaultValue(null, TITLE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveMetacardDefaultNullAttribute() {
    registry.removeDefaultValue(BASIC_METACARD_NAME, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveAllMetacardDefaultsNullMetacardType() {
    registry.removeDefaultValues(null);
  }
}
