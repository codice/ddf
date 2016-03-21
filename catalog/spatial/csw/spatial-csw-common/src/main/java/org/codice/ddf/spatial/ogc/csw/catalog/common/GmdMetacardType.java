/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import java.util.HashMap;
import java.util.Map;

import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class GmdMetacardType extends MetacardTypeImpl {

    private static final String GMD_ATTRIBUTE_PREFIX = "gmd.";

    public static final String GMD_METACARD_TYPE_NAME = "gmd.metadata";

    public static final String GMD_NAMESPACE = "http://www.isotc211.org/2005/gmd";

    // attributes
    public static final String GMD_SUBJECT = GMD_ATTRIBUTE_PREFIX + "subject";

    public static final String GMD_FORMAT = GMD_ATTRIBUTE_PREFIX + "format";

    public static final String GMD_IDENTIFIER = GMD_ATTRIBUTE_PREFIX + "Identifier";

    public static final String GMD_MODIFIED = GMD_ATTRIBUTE_PREFIX + "modified";

    public static final String GMD_BOUNDING_BOX = GMD_ATTRIBUTE_PREFIX + "BoundingBox";

    public static final String GMD_CRS = GMD_ATTRIBUTE_PREFIX + "crs";

    public static final String GMD_PUBLISHER = GMD_ATTRIBUTE_PREFIX + "publisher";

    /**
     * Indicates Metacard Type's attribute is queryable, i.e., is indexed.
     */
    public static final boolean QUERYABLE = true;

    private static final long serialVersionUID = 1L;

    private Map<String, String> attributeMap = new HashMap<>();

    public GmdMetacardType() {
        super(GMD_METACARD_TYPE_NAME, null);

        addDdfMetacardAttributes();
        addGmdMetacardAttributes();
    }

    public GmdMetacardType(String sourceId) {
        super(sourceId + "." + GMD_METACARD_TYPE_NAME, null);

        addDdfMetacardAttributes();
        addGmdMetacardAttributes();
    }

    private void addDdfMetacardAttributes() {
        descriptors.addAll(BasicTypes.BASIC_METACARD.getAttributeDescriptors());
    }

    /**
     * Adds GMD specific attributes to metacard type.
     * Note: queryable attributes defined in "OpenGIS® Catalogue Services Specification 2.0.2 -
     * ISO Metadata Application Profile" Table 6 and Table 9
     */
    private void addGmdMetacardAttributes() {
        descriptors.add(new AttributeDescriptorImpl(GMD_SUBJECT,
                QUERYABLE /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(GMD_PUBLISHER,
                QUERYABLE /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(GMD_FORMAT,
                QUERYABLE /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(GMD_IDENTIFIER,
                QUERYABLE /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(GMD_MODIFIED,
                QUERYABLE /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.DATE_TYPE));

        descriptors.add(new AttributeDescriptorImpl(GMD_BOUNDING_BOX,
                QUERYABLE /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.GEO_TYPE));

        descriptors.add(new AttributeDescriptorImpl(GMD_CRS,
                QUERYABLE /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));

    }

}
