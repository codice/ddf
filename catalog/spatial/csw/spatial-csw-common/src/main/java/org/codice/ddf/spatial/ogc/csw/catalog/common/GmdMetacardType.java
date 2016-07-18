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
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;

/**
 * Metacard to GMD MD_Metadata Mapping
 * <p/>
 * {@value ddf.catalog.data.Metacard#ID} -> {@link #FILE_IDENTIFIER_PATH}
 * {@value ddf.catalog.data.Metacard#TITLE} -> {@link #TITLE_PATH}
 * {@value ddf.catalog.data.Metacard#DESCRIPTION} -> {@link #ABSTRACT_PATH}
 * {@value ddf.catalog.data.Metacard#CREATED} -> {@link #CODE_LIST_VALUE_PATH}
 * {@value ddf.catalog.data.Metacard#MODIFIED} -> {@link #CODE_LIST_VALUE_PATH}
 * {@value ddf.catalog.data.Metacard#EFFECTIVE} -> {@link #CODE_LIST_VALUE_PATH}
 * {@value ddf.catalog.data.Metacard#CONTENT_TYPE} -> {@link #CODE_LIST_VALUE_PATH}
 * {@value org.codice.ddf.spatial.ogc.csw.catalog.common.GmdMetacardType#GMD_CRS} -> urn:ogc:def:crs:{@link #CRS_AUTHORITY_PATH}:{@link #CRS_CODE_PATH}:{@link #CRS_VERSION_PATH}
 * {@value ddf.catalog.data.Metacard#METADATA} -> entire GMD MD_Metadata document
 * {@value ddf.catalog.data.Metacard#POINT_OF_CONTACT} -> {@link #POINT_OF_CONTACT_PATH}
 * {@value org.codice.ddf.spatial.ogc.csw.catalog.common.GmdMetacardType#GMD_PUBLISHER} ->  {@link #POINT_OF_CONTACT_PATH}
 * {@value org.codice.ddf.spatial.ogc.csw.catalog.common.GmdMetacardType#GMD_FORMAT} ->  {@link #FORMAT_PATH}
 * {@value ddf.catalog.data.Metacard#RESOURCE_URI} -> {@link #LINKAGE_URI_PATH}
 * {@value org.codice.ddf.spatial.ogc.csw.catalog.common.GmdMetacardType#GMD_SUBJECT} ->  {@link #KEYWORD_PATH} and {@link #TOPIC_CATEGORY_PATH}
 * {@value ddf.catalog.data.Metacard#RESOURCE_SIZE} -> (unmapped)
 * {@value ddf.catalog.data.Metacard#EXPIRATION} -> (unmapped)
 */
public class GmdMetacardType extends MetacardTypeImpl {

    public static final String GMD_PREFIX = "gmd";

    public static final String GMD_LOCAL_NAME = "MD_Metadata";

    public static final String GMD_METACARD_TYPE_NAME = GMD_PREFIX + "." + GMD_LOCAL_NAME;

    public static final String GMD_NAMESPACE = "http://www.isotc211.org/2005/gmd";

    public static final String GCO_NAMESPACE = "http://www.isotc211.org/2005/gco";

    public static final String GCO_PREFIX = "gco";

    public static final String METACARD_URI = "urn:catalog:metacard";

    public static final String APISO_PREFIX = "apiso.";

    public static final String APISO_BOUNDING_BOX = APISO_PREFIX + "BoundingBox";

    // attributes
    public static final String GMD_SUBJECT = GMD_PREFIX + ".subject";

    public static final String GMD_FORMAT = GMD_PREFIX + "format";

    public static final String GMD_CRS = GMD_PREFIX + ".crs";

    public static final String FILE_IDENTIFIER_PATH =
            "/MD_Metadata/fileIdentifier/gco:CharacterString";

    public static final String GMD_PUBLISHER = GMD_PREFIX + "publisher";

    public static final List<QName> QNAME_LIST;

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

    public static final String DATE_TIME_STAMP_PATH = "/MD_Metadata/dateStamp/gco:DateTime";

    public static final String DATE_STAMP_PATH = "/MD_Metadata/dateStamp/gco:Date";

    public static final String CODE_LIST_VALUE_PATH =
            "/MD_Metadata/hierarchyLevel/MD_ScopeCode/@codeListValue";

    public static final String CODE_LIST_PATH =
            "/MD_Metadata/hierarchyLevel/MD_ScopeCode/@codeList";

    public static final String CRS_AUTHORITY_PATH =
            "/MD_Metadata/referenceSystemInfo/MD_ReferenceSystem/referenceSystemIdentifier/RS_Identifier/codeSpace/gco:CharacterString";

    public static final String CRS_VERSION_PATH =
            "/MD_Metadata/referenceSystemInfo/MD_ReferenceSystem/referenceSystemIdentifier/RS_Identifier/version";

    public static final String CRS_CODE_PATH =
            "/MD_Metadata/referenceSystemInfo/MD_ReferenceSystem/referenceSystemIdentifier/RS_Identifier/code/gco:CharacterString";

    public static final String TITLE_PATH =
            "/MD_Metadata/identificationInfo/MD_DataIdentification/citation/CI_Citation/title/gco:CharacterString";

    public static final String CREATED_DATE_PATH =
            "/MD_Metadata/identificationInfo/MD_DataIdentification/citation/CI_Citation/date/CI_Date/date/gco:DateTime";

    public static final String CREATED_DATE_TYPE_CODE_VALUE_PATH =
            "/MD_Metadata/identificationInfo/MD_DataIdentification/citation/CI_Citation/date/CI_Date/dateType/CI_DateTypeCode/@codeListValue";

    public static final String CREATED_DATE_TYPE_CODE_PATH =
            "/MD_Metadata/identificationInfo/MD_DataIdentification/citation/CI_Citation/date/CI_Date/dateType/CI_DateTypeCode/@codeList";

    public static final String ABSTRACT_PATH =
            "/MD_Metadata/identificationInfo/MD_DataIdentification/abstract/gco:CharacterString";

    public static final String FORMAT_PATH =
            "/MD_Metadata/distributionInfo/MD_Distribution/distributionFormat/MD_Format/name/gco:CharacterString";

    public static final String DISTRIBUTOR_CONTACT_PATH =
            "/MD_Metadata/distributionInfo/MD_Distribution/distributor/MD_Distributor/distributorContact";

    public static final String LINKAGE_URI_PATH =
            "/MD_Metadata/distributionInfo/MD_Distribution/transferOptions/MD_DigitalTransferOptions/onLine/CI_OnlineResource/linkage/URL";

    public static final String KEYWORD_PATH =
            "/MD_Metadata/identificationInfo/MD_DataIdentification/descriptiveKeywords/MD_Keywords/keyword/gco:CharacterString";

    public static final String TOPIC_CATEGORY_PATH =
            "/MD_Metadata/identificationInfo/MD_DataIdentification/topicCategory/MD_TopicCategoryCode";

    public static final String BBOX_WEST_LON_PATH =
            "/MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/geographicElement/EX_GeographicBoundingBox/westBoundLongitude/gco:Decimal";

    public static final String BBOX_EAST_LON_PATH =
            "/MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/geographicElement/EX_GeographicBoundingBox/eastBoundLongitude/gco:Decimal";

    public static final String BBOX_SOUTH_LAT_PATH =
            "/MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/geographicElement/EX_GeographicBoundingBox/southBoundLatitude/gco:Decimal";

    public static final String BBOX_NORTH_LAT_PATH =
            "/MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/geographicElement/EX_GeographicBoundingBox/northBoundLatitude/gco:Decimal";

    public static final String POINT_OF_CONTACT_PATH =
            "/MD_Metadata/identificationInfo/MD_DataIdentification/pointOfContact/CI_ResponsibleParty/organisationName/gco:CharacterString";

    public static final String POINT_OF_CONTACT_ROLE_PATH =
            "/MD_Metadata/identificationInfo/MD_DataIdentification/pointOfContact/CI_ResponsibleParty/role";

    public static final String LANGUAGE_PATH =
            "/MD_Metadata/identificationInfo/MD_DataIdentification/language/gco:CharacterString";

    public static final String CONTACT_PATH = "/MD_Metadata/contact";

    /**
     * Indicates Metacard Type's attribute is queryable, i.e., is indexed.
     */
    public static final boolean QUERYABLE = true;

    private static final long serialVersionUID = 1L;

    public GmdMetacardType() {
        super(GMD_METACARD_TYPE_NAME, (Set<AttributeDescriptor>) null);

        addDdfMetacardAttributes();
        addGmdMetacardAttributes();
    }

    public GmdMetacardType(String sourceId) {
        super(sourceId + "." + GMD_METACARD_TYPE_NAME, (Set<AttributeDescriptor>) null);

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

        descriptors.add(new AttributeDescriptorImpl(GMD_CRS,
                QUERYABLE /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));

    }

    private static QName createGMDQName(final String field) {
        return new QName(GMD_NAMESPACE, field, GmdMetacardType.GMD_PREFIX);
    }
}
