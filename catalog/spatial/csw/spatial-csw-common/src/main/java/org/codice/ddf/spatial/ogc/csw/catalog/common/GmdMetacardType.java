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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class GmdMetacardType extends MetacardTypeImpl {

    private static final String GMD_ATTRIBUTE_PREFIX = "gmd.";

    public static final String GMD_METACARD_TYPE_NAME = "gmd.metadata";

    public static final String GMD_NAMESPACE = "http://www.isotc211.org/2005/gmd";

    // attributes
    public static final String GMD_SUBJECT = GMD_ATTRIBUTE_PREFIX + "subject";

    public static final String GMD_TITLE = GMD_ATTRIBUTE_PREFIX + "title";

    public static final String GMD_ABSTRACT = GMD_ATTRIBUTE_PREFIX + "abstract";

    public static final String GMD_FORMAT = GMD_ATTRIBUTE_PREFIX + "format";

    public static final String GMD_IDENTIFIER = GMD_ATTRIBUTE_PREFIX + "Identifier";

    public static final String GMD_MODIFIED = GMD_ATTRIBUTE_PREFIX + "modified";

    public static final String GMD_TYPE = GMD_ATTRIBUTE_PREFIX + "type";

    public static final String GMD_BOUNDING_BOX = GMD_ATTRIBUTE_PREFIX + "BoundingBox";

    public static final String GMD_CRS = GMD_ATTRIBUTE_PREFIX + "crs";

    public static final String GMD_PUBLISHER = GMD_ATTRIBUTE_PREFIX + "publisher";

    public static final List<QName> QNAME_LIST;

    public static final String APISO_ATTRIBUTE_PREFIX = "apiso:";

    public static final String APISO_BOUNDING_BOX = APISO_ATTRIBUTE_PREFIX + "BoundingBox";

    public static final String GMD_REVISION_DATE = "RevisionDate";

    public static final String GMD_ALTERNATE_TITLE = "AlternateTitle";

    public static final String GMD_CREATION_DATE = "CreationDate";

    public static final String GMD_PUBLICATION_DATE = "PublicationDate";

    public static final String GMD_ORGANIZATION_NAME = "OrganisationName";

    public static final QName GMD_REVISION_DATE_QNAME;

    public static final QName GMD_ALTERNATE_TITLE_QNAME;

    public static final QName GMD_CREATION_DATE_QNAME;

    public static final QName GMD_PUBLICATION_DATE_QNAME;

    public static final QName GMD_ORGANIZATION_NAME_QNAME;

    static {
        GMD_REVISION_DATE_QNAME = createGMDQName(GMD_REVISION_DATE);

        GMD_ALTERNATE_TITLE_QNAME = createGMDQName(GMD_ALTERNATE_TITLE);

        GMD_CREATION_DATE_QNAME = createGMDQName(GMD_CREATION_DATE);

        GMD_PUBLICATION_DATE_QNAME = createGMDQName(GMD_PUBLICATION_DATE);

        GMD_ORGANIZATION_NAME_QNAME = createGMDQName(GMD_ORGANIZATION_NAME);

        QNAME_LIST = Arrays.asList(GMD_REVISION_DATE_QNAME, GMD_ALTERNATE_TITLE_QNAME);
    }

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

        descriptors.add(new AttributeDescriptorImpl(GMD_TITLE,
                QUERYABLE /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(GMD_ABSTRACT,
                QUERYABLE /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
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

        descriptors.add(new AttributeDescriptorImpl(GMD_TYPE,
                QUERYABLE /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));

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

    public String attributeToGmdMetadataField(final String attributeName) {
        return attributeMap.get(attributeName);

    }

    private static QName createGMDQName(final String field) {
        return new QName(GMD_NAMESPACE, field, CswConstants.GMD_PREFIX);
    }
}
