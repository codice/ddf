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

package org.codice.ddf.registry.common.metacard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;

public class TestRegistryObjectMetacardType {

    private static final String[] ATTRIBUTE_DESCRIPTORS =
            {Metacard.TAGS, Metacard.ID, Metacard.CONTENT_TYPE, Metacard.METADATA, Metacard.CREATED,
                    Metacard.MODIFIED, Metacard.TITLE, Metacard.DESCRIPTION,
                    RegistryObjectMetacardType.SECURITY_LEVEL,
                    RegistryObjectMetacardType.METACARD_TYPE, RegistryObjectMetacardType.ENTRY_TYPE,
                    Metacard.CONTENT_TYPE_VERSION, RegistryObjectMetacardType.ORGANIZATION_NAME,
                    RegistryObjectMetacardType.ORGANIZATION_ADDRESS,
                    RegistryObjectMetacardType.ORGANIZATION_PHONE_NUMBER,
                    RegistryObjectMetacardType.ORGANIZATION_EMAIL, Metacard.POINT_OF_CONTACT,
                    RegistryObjectMetacardType.LIVE_DATE,
                    RegistryObjectMetacardType.DATA_START_DATE,
                    RegistryObjectMetacardType.DATA_END_DATE, RegistryObjectMetacardType.LINKS,
                    Metacard.GEOGRAPHY, RegistryObjectMetacardType.REGION,
                    RegistryObjectMetacardType.DATA_SOURCES, RegistryObjectMetacardType.DATA_TYPES,
                    RegistryObjectMetacardType.SERVICE_BINDINGS,
                    RegistryObjectMetacardType.SERVICE_BINDING_TYPES,
                    RegistryObjectMetacardType.REGISTRY_ID,
                    RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE,
                    RegistryObjectMetacardType.REGISTRY_LOCAL_NODE,
                    RegistryObjectMetacardType.REGISTRY_BASE_URL,
                    RegistryObjectMetacardType.PUBLISHED_LOCATIONS,
                    RegistryObjectMetacardType.LAST_PUBLISHED};

    @Test
    public void testConstruction() {
        RegistryObjectMetacardType registryObjectMetacardType = new RegistryObjectMetacardType();

        assertThat(registryObjectMetacardType, not(nullValue()));
        Set<AttributeDescriptor> descriptors = registryObjectMetacardType.getAttributeDescriptors();
        assertThat(descriptors, not(nullValue()));
        assertThat(CollectionUtils.isEmpty(descriptors), is(false));

        assertThat(registryObjectMetacardType.getAttributeDescriptor(Metacard.ID)
                .isMultiValued(), is(false));
    }

    @Test
    public void testMetacardHasBasicDescriptorsAsIsStoredTrue() {
        RegistryObjectMetacardType registryObjectMetacardType = new RegistryObjectMetacardType();
        for (String attrDesc : ATTRIBUTE_DESCRIPTORS) {
            assertThat(registryObjectMetacardType.getAttributeDescriptor(attrDesc)
                    .isStored(), is(true));
        }
    }
}
