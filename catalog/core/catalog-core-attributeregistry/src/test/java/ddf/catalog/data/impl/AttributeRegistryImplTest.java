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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.DateTimeAttributes;
import ddf.catalog.data.types.Core;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class AttributeRegistryImplTest {
  private AttributeRegistryImpl registry;

  @Before
  public void setup() {
    registry = new AttributeRegistryImpl();
    registry.registerMetacardType(generateMetacardType());
  }

  @Test
  public void testDeregisterMetacardType() {
    MetacardType metacardType = generateMetacardType();
    registry.deregisterMetacardType(generateMetacardType());
    metacardType
        .getAttributeDescriptors()
        .forEach(
            attributeDescriptor ->
                assertThat(registry.lookup(attributeDescriptor.getName()).isPresent(), is(false)));
  }

  @Test
  public void testDeregisterMetacardTypeWithDuplicateAttributes() {
    // Register a MetacardType with the same attributes
    registry.registerMetacardType(generateMetacardType());
    // Deregister the second MetacardType
    registry.deregisterMetacardType(generateMetacardType());
    // Assert that a duplicated attribute is still present in the registry
    assertThat(registry.lookup(Core.CREATED).isPresent(), is(true));
  }

  @Test
  public void testDeregisterMetacardTypeWithNulls() {
    registry = new AttributeRegistryImpl();
    registry.registerMetacardType(generateMetacardTypeWithNulls());
    registry.deregisterMetacardType(generateMetacardTypeWithNulls());
    assertThat(registry.lookup("test").isPresent(), is(false));
  }

  @Test
  public void testRegisterMetacardType() {
    generateMetacardType()
        .getAttributeDescriptors()
        .forEach(
            attributeDescriptor ->
                assertThat(registry.lookup(attributeDescriptor.getName()).isPresent(), is(true)));
  }

  @Test
  public void testRegisterMetacardTypeWithNulls() {
    registry = new AttributeRegistryImpl();
    registry.registerMetacardType(generateMetacardTypeWithNulls());
    assertThat(registry.lookup("test").isPresent(), is(true));
  }

  @Test
  public void testNullMetacardType() {
    registry = new AttributeRegistryImpl();
    registry.registerMetacardType(null);
    assertThat(registry.lookup(Core.DATATYPE), is(Optional.empty()));
    registry.deregisterMetacardType(null);
    assertThat(registry.lookup(Core.DATATYPE), is(Optional.empty()));
  }

  @Test
  public void testAddAttribute() {
    final String attributeName = "test";
    final AttributeDescriptor descriptor =
        new AttributeDescriptorImpl(
            attributeName, true, false, true, false, BasicTypes.STRING_TYPE);
    registry.register(descriptor);

    final Optional<AttributeDescriptor> descriptorOptional = registry.lookup(attributeName);
    assertThat(descriptorOptional.isPresent(), is(true));
    assertThat(descriptorOptional.get(), is(descriptor));
  }

  @Test
  public void testRemoveAttribute() {
    final String attributeName = "attribute";
    final AttributeDescriptor descriptor =
        new AttributeDescriptorImpl(
            attributeName, true, false, true, false, BasicTypes.STRING_TYPE);
    registry.register(descriptor);

    Optional<AttributeDescriptor> descriptorOptional = registry.lookup(attributeName);
    assertThat(descriptorOptional.isPresent(), is(true));

    registry.deregister(descriptor);
    descriptorOptional = registry.lookup(attributeName);
    assertThat(descriptorOptional.isPresent(), is(false));
  }

  @Test
  public void testRemoveDuplicateAttribute() {
    final String attributeName = "foo";
    final AttributeDescriptor descriptor1 =
        new AttributeDescriptorImpl(attributeName, true, true, true, true, BasicTypes.STRING_TYPE);
    registry.register(descriptor1);
    registry.register(descriptor1);

    registry.deregister(descriptor1);
    final Optional<AttributeDescriptor> descriptorOptional = registry.lookup(attributeName);
    assertThat(descriptorOptional.isPresent(), is(true));
    assertThat(descriptorOptional.get(), is(descriptor1));

    registry.deregister(descriptor1);
    final Optional<AttributeDescriptor> descriptorOptional2 = registry.lookup(attributeName);
    assertThat(descriptorOptional2.isPresent(), is(false));
  }

  @Test
  public void testAddDuplicateAttribute() {
    final String attributeName = "foo";
    final AttributeDescriptor descriptor1 =
        new AttributeDescriptorImpl(attributeName, true, true, true, true, BasicTypes.STRING_TYPE);
    registry.register(descriptor1);
    registry.register(descriptor1);

    final Optional<AttributeDescriptor> descriptorOptional = registry.lookup(attributeName);
    assertThat(descriptorOptional.isPresent(), is(true));
    assertThat(descriptorOptional.get(), is(descriptor1));
  }

  @Test
  public void testAddAttributeWithSameName() {
    final String attributeName = "foo";
    final AttributeDescriptor descriptor1 =
        new AttributeDescriptorImpl(attributeName, true, true, true, true, BasicTypes.STRING_TYPE);
    registry.register(descriptor1);

    final AttributeDescriptor descriptor2 =
        new AttributeDescriptorImpl(
            attributeName, false, false, false, false, BasicTypes.BINARY_TYPE);
    registry.register(descriptor2);
    final Optional<AttributeDescriptor> descriptorOptional = registry.lookup(attributeName);
    assertThat(descriptorOptional.isPresent(), is(true));
    assertThat(descriptorOptional.get(), is(descriptor1));
  }

  @Test
  public void testAddRemoveAttributeWithSameName() {
    final String attributeName = "test";
    final AttributeDescriptor descriptor1 =
        new AttributeDescriptorImpl(attributeName, true, true, true, true, BasicTypes.STRING_TYPE);
    final AttributeDescriptor descriptor2 =
        new AttributeDescriptorImpl(
            attributeName, false, false, false, false, BasicTypes.STRING_TYPE);
    registry.register(descriptor1);
    registry.register(descriptor2);
    registry.deregister(descriptor1);

    final Optional<AttributeDescriptor> descriptorOptional = registry.lookup(attributeName);
    assertThat(descriptorOptional.isPresent(), is(true));
    assertThat(descriptorOptional.get(), is(descriptor2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullAttributeDescriptor() {
    registry.register(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullAttributeDescriptorName() {
    final AttributeDescriptor descriptor =
        new AttributeDescriptorImpl(null, true, false, true, false, BasicTypes.STRING_TYPE);
    registry.register(descriptor);
  }

  private static MetacardType generateMetacardType() {
    return new MetacardTypeImpl(
        "testMetacardType", Arrays.asList(new CoreAttributes(), new DateTimeAttributes()));
  }

  private static MetacardType generateMetacardTypeWithNulls() {
    Set<AttributeDescriptor> attributeDescriptorSet = new HashSet<>();
    attributeDescriptorSet.add(
        new AttributeDescriptorImpl(null, true, true, true, true, BasicTypes.STRING_TYPE));
    attributeDescriptorSet.add(
        new AttributeDescriptorImpl("test", true, true, true, true, BasicTypes.STRING_TYPE));
    attributeDescriptorSet.add(null);
    MetacardType metacardType =
        new MetacardTypeImpl("nullAttributeMetacardType", attributeDescriptorSet);
    return metacardType;
  }
}
