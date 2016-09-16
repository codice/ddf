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

import static ddf.catalog.data.impl.BasicTypes.BASIC_METACARD;
import static ddf.catalog.data.impl.BasicTypes.BOOLEAN_TYPE;
import static ddf.catalog.data.impl.BasicTypes.DATE_TYPE;
import static ddf.catalog.data.impl.BasicTypes.STRING_TYPE;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;

/**
 * Base MetacardType for all registry MetacardTypes.
 */
public class RegistryObjectMetacardType extends MetacardTypeImpl {

    ////////////////////////////////////////////////////////////////////////////////////
    // ATTRIBUTE KEYS
    ////////////////////////////////////////////////////////////////////////////////////

    public static final String DATA_END_DATE = "data-end-date";

    public static final String DATA_SOURCES = "input-data-sources";

    public static final String DATA_START_DATE = "data-start-date";

    public static final String DATA_TYPES = "data-types";

    public static final String ENTRY_TYPE = "entry-type";

    public static final String LAST_PUBLISHED = "last-published";

    public static final String LINKS = "links";

    public static final String LIVE_DATE = "live-date";

    public static final String METACARD_TYPE = "metacard-type";

    public static final String ORGANIZATION_ADDRESS = "organization-address";

    public static final String ORGANIZATION_EMAIL = "organization-email";

    public static final String ORGANIZATION_NAME = "organization-name";

    public static final String ORGANIZATION_PHONE_NUMBER = "organization-phone-number";

    public static final String PUBLISHED_LOCATIONS = "published-locations";

    public static final String REGION = "region";

    public static final String REGISTRY_BASE_URL = "registry-base-url";

    public static final String REGISTRY_ID = "registry-id";

    public static final String REGISTRY_IDENTITY_NODE = "registry-identity-node";

    public static final String REGISTRY_LOCAL_NODE = "registry-local-node";

    public static final String REGISTRY_METACARD_TYPE_NAME = "registry";

    public static final String REMOTE_METACARD_ID = "remote-metacard-id";

    public static final String REMOTE_REGISTRY_ID = "remote-registry-id";

    public static final String SECURITY_LEVEL = "security-level";

    //list of bindingType fields from all the service bindings
    public static final String SERVICE_BINDING_TYPES = "service-binding-types";

    //list of all the service binding ids
    public static final String SERVICE_BINDINGS = "service-bindings";

    ////////////////////////////////////////////////////////////////////////////////////
    // ATTRIBUTE DEFINITIONS
    ////////////////////////////////////////////////////////////////////////////////////

    public static final Set<String> TRANSIENT_ATTRIBUTES = Stream.of(LAST_PUBLISHED,
            PUBLISHED_LOCATIONS,
            REGISTRY_IDENTITY_NODE,
            REGISTRY_LOCAL_NODE,
            REMOTE_METACARD_ID,
            REMOTE_REGISTRY_ID)
            .collect(Collectors.toSet());

    public static final Set<AttributeDescriptor> REGISTRY_ATTRIBUTES = Stream.of(//
            new AttributeDescriptorImpl(DATA_END_DATE, true, true, false, false, DATE_TYPE),
            new AttributeDescriptorImpl(DATA_SOURCES, true, true, false, true, STRING_TYPE),
            new AttributeDescriptorImpl(DATA_START_DATE, true, true, false, false, DATE_TYPE),
            new AttributeDescriptorImpl(DATA_TYPES, true, true, false, true, STRING_TYPE),
            new AttributeDescriptorImpl(ENTRY_TYPE, true, true, false, false, STRING_TYPE),
            new AttributeDescriptorImpl(LAST_PUBLISHED, true, true, false, false, DATE_TYPE),
            new AttributeDescriptorImpl(LINKS, true, true, false, true, STRING_TYPE),
            new AttributeDescriptorImpl(LIVE_DATE, true, true, false, false, DATE_TYPE),
            new AttributeDescriptorImpl(METACARD_TYPE, true, true, false, false, STRING_TYPE),
            new AttributeDescriptorImpl(ORGANIZATION_ADDRESS,
                    true,
                    true,
                    false,
                    false,
                    STRING_TYPE),
            new AttributeDescriptorImpl(ORGANIZATION_EMAIL, true, true, false, true, STRING_TYPE),
            new AttributeDescriptorImpl(ORGANIZATION_NAME, true, true, false, false, STRING_TYPE),
            new AttributeDescriptorImpl(ORGANIZATION_PHONE_NUMBER,
                    true,
                    true,
                    false,
                    true,
                    STRING_TYPE),
            new AttributeDescriptorImpl(PUBLISHED_LOCATIONS, true, true, false, true, STRING_TYPE),
            new AttributeDescriptorImpl(REGION, true, true, false, false, STRING_TYPE),
            new AttributeDescriptorImpl(REGISTRY_BASE_URL, true, true, false, false, STRING_TYPE),
            new AttributeDescriptorImpl(REGISTRY_ID, true, true, false, false, STRING_TYPE),
            new AttributeDescriptorImpl(REGISTRY_IDENTITY_NODE,
                    true,
                    true,
                    false,
                    false,
                    BOOLEAN_TYPE),
            new AttributeDescriptorImpl(REGISTRY_LOCAL_NODE,
                    true,
                    true,
                    false,
                    false,
                    BOOLEAN_TYPE),
            new AttributeDescriptorImpl(REMOTE_METACARD_ID, true, true, false, false, STRING_TYPE),
            new AttributeDescriptorImpl(REMOTE_REGISTRY_ID, true, true, false, false, STRING_TYPE),
            new AttributeDescriptorImpl(SECURITY_LEVEL, true, true, false, true, STRING_TYPE),
            new AttributeDescriptorImpl(SERVICE_BINDING_TYPES,
                    true,
                    true,
                    false,
                    true,
                    STRING_TYPE),
            new AttributeDescriptorImpl(SERVICE_BINDINGS, true, true, false, true, STRING_TYPE))
            .collect(Collectors.toSet());

    // END ATTRIBUTE DEFINITIONS

    public RegistryObjectMetacardType() {
        super(REGISTRY_METACARD_TYPE_NAME, BasicTypes.BASIC_METACARD, REGISTRY_ATTRIBUTES);
    }

    public RegistryObjectMetacardType(String name, Set<AttributeDescriptor> descriptors) {
        super(name, BASIC_METACARD, mixinDescriptors(descriptors));
    }

    private static Set<AttributeDescriptor> mixinDescriptors(Set<AttributeDescriptor> descriptors) {
        return Stream.concat(REGISTRY_ATTRIBUTES.stream(), descriptors.stream())
                .collect(Collectors.toSet());

    }
}
