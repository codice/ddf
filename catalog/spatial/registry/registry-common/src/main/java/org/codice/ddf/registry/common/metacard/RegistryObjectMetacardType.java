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

import java.util.Set;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;

/**
 * Base MetacardType for all registry MetacardTypes.
 */
public class RegistryObjectMetacardType extends MetacardTypeImpl {

    public static final String REGISTRY_METACARD_TYPE_NAME = "registry";

    public static final String METACARD_TYPE = "metacard-type";

    public static final String ORGANIZATION_NAME = "organization-name";

    public static final String ORGANIZATION_ADDRESS = "organization-address";

    public static final String ORGANIZATION_PHONE_NUMBER = "organization-phone-number";

    public static final String ORGANIZATION_EMAIL = "organization-email";

    public static final String ENTRY_TYPE = "entry-type";

    public static final String SECURITY_LEVEL = "security-level";

    public static final String LIVE_DATE = "live-date";

    public static final String DATA_START_DATE = "data-start-date";

    public static final String DATA_END_DATE = "data-end-date";

    public static final String LINKS = "links";

    public static final String REGION = "region";

    public static final String DATA_SOURCES = "input-data-sources";

    public static final String DATA_TYPES = "data-types";

    //list of all the service binding ids
    public static final String SERVICE_BINDINGS = "service-bindings";

    //list of bindingType fields from all the service bindings
    public static final String SERVICE_BINDING_TYPES = "service-binding-types";

    public static final String REGISTRY_ID = "registry-id";

    public static final String REGISTRY_IDENTITY_NODE = "registry-identity-node";

    public static final String PUBLISHED_LOCATIONS = "published-locations";

    public static final String REGISTRY_BASE_URL = "registry-base-url";

    public RegistryObjectMetacardType() {
        this(REGISTRY_METACARD_TYPE_NAME, null);
    }

    public RegistryObjectMetacardType(String name, Set<AttributeDescriptor> descriptors) {
        super(name, descriptors);
        addRegistryAttributes();
    }

    private void addRegistryAttributes() {
        addQueryableString(Metacard.TAGS, true);
        addQueryableString(Metacard.ID, false);
        addQueryableString(Metacard.CONTENT_TYPE, false);
        addXml(Metacard.METADATA, true);
        addQueryableDate(Metacard.CREATED);
        addQueryableDate(Metacard.MODIFIED);
        addQueryableString(Metacard.TITLE, false); //name
        addQueryableString(Metacard.DESCRIPTION, false);
        addQueryableString(SECURITY_LEVEL, true); //securityLevel
        addQueryableString(METACARD_TYPE, false);
        addQueryableString(ENTRY_TYPE, false);  //objectType
        addQueryableString(Metacard.CONTENT_TYPE_VERSION, false); // version
        addQueryableString(ORGANIZATION_NAME, false);
        addQueryableString(ORGANIZATION_ADDRESS, false);
        addQueryableString(ORGANIZATION_PHONE_NUMBER, true);
        addQueryableString(ORGANIZATION_EMAIL, true);
        addQueryableString(Metacard.POINT_OF_CONTACT, false);
        addQueryableDate(LIVE_DATE);
        addQueryableDate(DATA_START_DATE);
        addQueryableDate(DATA_END_DATE);
        addQueryableString(LINKS, true);
        addQueryableGeo(Metacard.GEOGRAPHY, false);
        addQueryableString(REGION, false);
        addQueryableString(DATA_SOURCES, true);
        addQueryableString(DATA_TYPES, true);
        addQueryableString(SERVICE_BINDINGS, true);
        addQueryableString(SERVICE_BINDING_TYPES, true);
        addQueryableString(REGISTRY_ID, false);
        addQueryableBoolean(REGISTRY_IDENTITY_NODE, false);
        addQueryableString(REGISTRY_BASE_URL, false);
        addQueryableString(PUBLISHED_LOCATIONS, true);
    }

    /**
     * Method to add a queryable string to the descriptors of this metacard type. Can be used to
     * dynamically add additional descriptors to the base set.
     *
     * @param name        Name of the descriptor
     * @param multivalued Whether or not this descriptor represents several values (true) or one value (false)
     */
    public void addQueryableString(String name, boolean multivalued) {
        addDescriptor(name, true, multivalued, BasicTypes.STRING_TYPE);
    }

    /**
     * Method to add a queryable date to the descriptors of this metacard type. Can be used to
     * dynamically add additional descriptors to the base set.
     *
     * @param name Name of the descriptor
     */
    public void addQueryableDate(String name) {
        addDescriptor(name, true, false, BasicTypes.DATE_TYPE);
    }

    /**
     * Method to add a queryable boolean to the descriptors of this metacard type. Can be used to
     * dynamically add additional descriptors to the base set.
     *
     * @param name Name of the descriptor
     * @param multivalued Whether or not this descriptor represents several values (true) or one value (false)
     */
    public void addQueryableBoolean(String name, boolean multivalued) {
        addDescriptor(name, true, multivalued, BasicTypes.BOOLEAN_TYPE);
    }

    /**
     * Method to add an XML entry to the descriptors of the metacard type. Can be used to
     * dynamically add additional descriptors to the base set.
     *
     * @param name      Name of the descriptor
     * @param queryable Whether or not this descriptor should be queryable.
     */
    public void addXml(String name, boolean queryable) {
        addDescriptor(name, queryable, false, BasicTypes.XML_TYPE);
    }

    protected void addQueryableGeo(String name, boolean multivalued) {
        addDescriptor(name, true, multivalued, BasicTypes.GEO_TYPE);
    }

    protected void addDescriptor(String name, boolean queryable, boolean multivalued,
            AttributeType<?> type) {
        descriptors.add(new AttributeDescriptorImpl(name,
                queryable /* indexed */,
                true /* stored */,
                false /* tokenized */,
                multivalued /* multivalued */,
                type));
    }
}
