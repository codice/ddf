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
 **/
package org.codice.ddf.registry.common;

public class RegistryConstants {
    public static final String REGISTRY_TAG = "registry";

    public static final String REGISTRY_TAG_INTERNAL = "registry-remote";

    public static final String REGISTRY_NODE_METACARD_TYPE_NAME = "registry.federation.node";

    public static final String XML_DATE_TIME_TYPE = "date";

    public static final String XML_GEO_TYPE = "GM_Point";

    public static final String XML_BOUNDS_TYPE = "GM_Envelope";

    public static final String REGISTRY_ID_PROPERTY = "org.codice.ddf.registry.identity-id";

    //ebrim constants
    //Object types
    public static final String REGISTRY_NODE_OBJECT_TYPE = "urn:registry:federation:node";

    public static final String REGISTRY_SERVICE_OBJECT_TYPE = "urn:registry:federation:service";

    public static final String REGISTRY_CONTENT_COLLECTION_OBJECT_TYPE = "urn:registry:content:collection";

    //object ids
    public static final String REGISTRY_MCARD_ID_LOCAL = "urn:registry:metacard:local-id";

    public static final String REGISTRY_MCARD_ID_ORIGIN = "urn:registry:metacard:origin-id";

    public static final String REGISTRY_ID_ORIGIN = "urn:registry:origin-id";


    //classifications
    public static final String REGISTRY_METACARD_ID_CLASS = "MetacardId";

    public static final String REGISTRY_ID_CLASS = "RegistryId";

    public static final String REGISTRY_ASSOCIATION_CLASS = "RelatedTo";

    //xml slot names
    public static final String XML_LIVE_DATE_NAME = "liveDate";

    public static final String XML_LAST_UPDATED_NAME = "lastUpdated";

    public static final String TRANSIENT_ATTRIBUTE_UPDATE = "transient.attribute.update";

    public static final String GUID_PREFIX = "urn:uuid:";

}
