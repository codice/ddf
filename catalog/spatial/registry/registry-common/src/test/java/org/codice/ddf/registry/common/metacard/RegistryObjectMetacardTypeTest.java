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
package org.codice.ddf.registry.common.metacard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import ddf.catalog.data.AttributeDescriptor;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;

public class RegistryObjectMetacardTypeTest {

  private static final String[] ATTRIBUTE_DESCRIPTORS = {
    RegistryObjectMetacardType.SECURITY_LEVEL,
    RegistryObjectMetacardType.LINKS,
    RegistryObjectMetacardType.REGION,
    RegistryObjectMetacardType.DATA_SOURCES,
    RegistryObjectMetacardType.SERVICE_BINDINGS,
    RegistryObjectMetacardType.SERVICE_BINDING_TYPES,
    RegistryObjectMetacardType.REGISTRY_ID,
    RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE,
    RegistryObjectMetacardType.REGISTRY_LOCAL_NODE,
    RegistryObjectMetacardType.REGISTRY_BASE_URL,
    RegistryObjectMetacardType.REMOTE_METACARD_ID,
    RegistryObjectMetacardType.REMOTE_REGISTRY_ID,
    RegistryObjectMetacardType.PUBLISHED_LOCATIONS,
    RegistryObjectMetacardType.LAST_PUBLISHED
  };

  @Test
  public void testConstruction() {
    RegistryObjectMetacardType registryObjectMetacardType = new RegistryObjectMetacardType();

    assertThat(registryObjectMetacardType, not(nullValue()));
    Set<AttributeDescriptor> descriptors = registryObjectMetacardType.getAttributeDescriptors();
    assertThat(descriptors, not(nullValue()));
    assertThat(CollectionUtils.isEmpty(descriptors), is(false));

    assertThat(
        registryObjectMetacardType
            .getAttributeDescriptor(RegistryObjectMetacardType.REGISTRY_ID)
            .isMultiValued(),
        is(false));
  }

  @Test
  public void testMetacardHasBasicDescriptorsAsIsStoredTrue() {
    RegistryObjectMetacardType registryObjectMetacardType = new RegistryObjectMetacardType();
    for (String attrDesc : ATTRIBUTE_DESCRIPTORS) {
      assertThat(registryObjectMetacardType.getAttributeDescriptor(attrDesc).isStored(), is(true));
    }
  }

  @Test
  public void testAddAttributeMethods() {
    RegistryObjectMetacardType registryObjectMetacardType = new RegistryObjectMetacardType();

    registryObjectMetacardType.addXml("title1", false);
    registryObjectMetacardType.addQueryableGeo("title2", true);

    assertThat(registryObjectMetacardType.getAttributeDescriptor("title1").isStored(), is(true));
    assertThat(
        registryObjectMetacardType.getAttributeDescriptor("title1").isMultiValued(), is(false));

    assertThat(registryObjectMetacardType.getAttributeDescriptor("title2").isStored(), is(true));
    assertThat(
        registryObjectMetacardType.getAttributeDescriptor("title2").isMultiValued(), is(true));
  }
}
