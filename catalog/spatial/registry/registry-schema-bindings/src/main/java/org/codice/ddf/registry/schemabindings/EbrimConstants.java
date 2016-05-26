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
package org.codice.ddf.registry.schemabindings;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ObjectFactory;

/**
 * This class consists of constants used by the ebrim schema bindings.
 */
public final class EbrimConstants {

    public static final net.opengis.gml.v_3_1_1.ObjectFactory GML_FACTORY =
            new net.opengis.gml.v_3_1_1.ObjectFactory();

    public static final net.opengis.ogc.ObjectFactory OGC_FACTORY =
            new net.opengis.ogc.ObjectFactory();

    public static final ObjectFactory RIM_FACTORY = new ObjectFactory();

    public static final net.opengis.cat.wrs.v_1_0_2.ObjectFactory WRS_FACTORY =
            new net.opengis.cat.wrs.v_1_0_2.ObjectFactory();

    // Association converters constants
    public static final String ASSOCIATION_TYPE = "associationType";

    public static final String SOURCE_OBJECT = "sourceObject";

    public static final String TARGET_OBJECT = "targetObject";

    // Classification converters constants
    public static final String CLASSIFICATION_NODE = "classificationNode";

    public static final String CLASSIFIED_OBJECT = "classifiedObject";

    public static final String CLASSIFICATION_SCHEME = "classificationScheme";

    public static final String NODE_REPRESENTATION = "nodeRepresentation";

    // EmailAddress converters constants
    public static final String ADDRESS = "address";

    public static final String TYPE = "type";

    // ExternalIdentifier converters constants
    public static final String IDENTIFICATION_SCHEME = "identificationScheme";

    public static final String REGISTRY_OBJECT = "registryObject";

    //ExtrinsicObject converters constants
    public static final String CONTENT_VERSION_INFO = "ContentVersionInfo";

    public static final String IS_OPAQUE = "isOpaque";

    public static final String MIME_TYPE = "mimeType";

    // Organization converters constants
    public static final String PARENT = "parent";

    public static final String PRIMARY_CONTACT = "primaryContact";

    // PersonName converters constants
    public static final String FIRST_NAME = "firstName";

    public static final String MIDDLE_NAME = "middleName";

    public static final String LAST_NAME = "lastName";

    // Person converters constants
    public static final String PERSON_NAME_KEY = "PersonName";

    // PostalAddress converters constants
    public static final String CITY = "city";

    public static final String COUNTRY = "country";

    public static final String POSTAL_CODE = "postalCode";

    public static final String STATE_OR_PROVINCE = "stateOrProvince";

    public static final String STREET = "street";

    public static final String STREET_NUMBER = "streetNumber";

    // RegistryObjectList converters constants
    public static final String ASSOCIATION_KEY = "Association";

    public static final String EXTRINSIC_OBJECT_KEY = "ExtrinsicObject";

    public static final String ORGANIZATION_KEY = "Organization";

    public static final String PERSON_KEY = "Person";

    public static final String SERVICE_KEY = "Service";

    // RegistryObject converters constants
    public static final String CLASSIFICATION_KEY = "Classification";

    public static final String EXTERNAL_IDENTIFIER_KEY = "ExternalIdentifier";

    public static final String NAME_KEY = "Name";

    public static final String DESCRIPTION_KEY = "Description";

    public static final String VERSION_INFO_KEY = "VersionInfo";

    public static final String SLOT = "Slot";

    public static final String ID_KEY = "id";

    public static final String HOME_KEY = "home";

    public static final String LID_KEY = "Lid";

    public static final String STATUS_KEY = "Status";

    public static final String OBJECT_TYPE_KEY = "objectType";

    // RegistryPackage converters constants
    public static final String REGISTRY_OBJECT_LIST_KEY = "RegistryObjectList";

    // ServiceBinding converters constants
    public static final String ACCESS_URI = "accessUri";

    public static final String SERVICE = "service";

    public static final String TARGET_BINDING = "targetBinding";

    public static final String SPECIFICATION_LINK_KEY = "SpecificationLink";

    // Service converters constants
    public static final String SERVICE_BINDING_KEY = "ServiceBinding";

    // Slot converters constants
    public static final String SLOT_TYPE = "slotType";

    public static final String NAME = "name";

    public static final String POINT_KEY = "Point";

    public static final String SRS_DIMENSION = "srsDimension";

    public static final String SRS_NAME = "srsName";

    public static final String POSITION = "pos";

    // SpecificationLink converters constants
    public static final String SERVICE_BINDING = "serviceBinding";

    public static final String SPECIFICATION_OBJECT = "specificationObject";

    public static final String USAGE_DESCRIPTION = "UsageDescription";

    public static final String USAGE_PARAMETERS = "UsageParameters";

    // TelephoneNumber converters constants
    public static final String PHONE_COUNTRY_CODE = "countryCode";

    public static final String PHONE_TYPE = "phoneType";

    public static final String PHONE_AREA_CODE = "areaCode";

    public static final String PHONE_NUMBER = "number";

    public static final String PHONE_EXTENSION = "extension";

    // Common constants shared by ExternalIdentifier converters and Slot converters
    public static final String VALUE = "value";

    // Common constants shared by Organization converters and Person converters
    public static final String ADDRESS_KEY = "Address";

    public static final String EMAIL_ADDRESS_KEY = "EmailAddress";

    public static final String TELEPHONE_KEY = "TelephoneNumber";

    private EbrimConstants() {
    }

}
