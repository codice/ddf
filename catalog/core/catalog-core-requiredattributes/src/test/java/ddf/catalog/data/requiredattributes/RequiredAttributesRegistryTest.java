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
package ddf.catalog.data.requiredattributes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ddf.catalog.data.RequiredAttributes;
import ddf.catalog.validation.impl.validator.RequiredAttributesMetacardValidator;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

public class RequiredAttributesRegistryTest {

  private static RequiredAttributesRegistryImpl requiredAttributesRegistry;

  private static final String METACARD_TYPE_1 = "metacardType1";

  private static final String METACARD_TYPE_2 = "metacardType2";

  private static final String ATTRIBUTE_1 = "attribute1";

  private static final String ATTRIBUTE_2 = "attribute2";

  private static final Set<String> REQUIRED_ATTRIBUTES_TYPE_1 = Collections.singleton(ATTRIBUTE_1);

  private static final Set<String> REQUIRED_ATTRIBUTES_TYPE_2 =
      Stream.of(ATTRIBUTE_1, ATTRIBUTE_2).collect(Collectors.toSet());

  @Before
  public void setup() {
    requiredAttributesRegistry = new RequiredAttributesRegistryImpl();
  }

  @Test
  public void testBindUnbind() {

    RequiredAttributes requiredAttributes1 =
        new RequiredAttributesMetacardValidator(METACARD_TYPE_1, REQUIRED_ATTRIBUTES_TYPE_1);
    RequiredAttributes requiredAttributes2 =
        new RequiredAttributesMetacardValidator(METACARD_TYPE_2, REQUIRED_ATTRIBUTES_TYPE_2);

    requiredAttributesRegistry.bind(requiredAttributes1);
    assertTrue(requiredAttributesRegistry.isRequired(METACARD_TYPE_1, ATTRIBUTE_1));
    assertFalse(requiredAttributesRegistry.isRequired(METACARD_TYPE_1, ATTRIBUTE_2));
    assertTrue(
        requiredAttributesRegistry.getRequiredAttributes(METACARD_TYPE_1).contains(ATTRIBUTE_1));
    assertFalse(
        requiredAttributesRegistry.getRequiredAttributes(METACARD_TYPE_1).contains(ATTRIBUTE_2));
    assertTrue(requiredAttributesRegistry.getRequiredAttributes(METACARD_TYPE_2).isEmpty());
    assertFalse(requiredAttributesRegistry.isRequired(METACARD_TYPE_2, ATTRIBUTE_1));

    requiredAttributesRegistry.bind(requiredAttributes2);
    assertTrue(requiredAttributesRegistry.isRequired(METACARD_TYPE_2, ATTRIBUTE_1));
    assertTrue(requiredAttributesRegistry.isRequired(METACARD_TYPE_2, ATTRIBUTE_2));
    assertTrue(
        requiredAttributesRegistry.getRequiredAttributes(METACARD_TYPE_2).contains(ATTRIBUTE_1));
    assertTrue(
        requiredAttributesRegistry.getRequiredAttributes(METACARD_TYPE_2).contains(ATTRIBUTE_2));

    requiredAttributesRegistry.unbind(requiredAttributes1);
    assertFalse(requiredAttributesRegistry.isRequired(METACARD_TYPE_1, ATTRIBUTE_1));
    assertTrue(requiredAttributesRegistry.getRequiredAttributes(METACARD_TYPE_1).isEmpty());
    assertFalse(requiredAttributesRegistry.getRequiredAttributes(METACARD_TYPE_2).isEmpty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIsRequiredNullType() {
    requiredAttributesRegistry.isRequired(null, "test");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIsRequiredNullAttribute() {
    requiredAttributesRegistry.isRequired("test", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetRequiredNullType() {
    requiredAttributesRegistry.getRequiredAttributes(null);
  }
}
