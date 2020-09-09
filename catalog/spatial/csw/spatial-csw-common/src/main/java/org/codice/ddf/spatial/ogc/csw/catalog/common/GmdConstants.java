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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import javax.xml.namespace.QName;

public class GmdConstants {

  public static final String GMD_REVISION_DATE = "RevisionDate";

  public static final String GMD_LANGUAGE = "Language";

  public static final String GMD_ABSTRACT = "Abstract";

  public static final String GMD_SUBJECT = "Subject";

  public static final String GMD_ALTERNATE_TITLE = "AlternateTitle";

  public static final String GMD_RESOURCE_LANGUAGE = "ResourceLanguage";

  public static final String GMD_CREATION_DATE = "CreationDate";

  public static final String GMD_PUBLICATION_DATE = "PublicationDate";

  public static final String GMD_ORGANIZATION_NAME = "OrganisationName";

  public static final String GMD_TYPE = "Type";

  public static final String GMD_MODIFIED = "Modified";

  public static final String GMD_FORMAT = "Format";

  public static final String GMD_PREFIX = "gmd";

  public static final String GMD_LOCAL_NAME = "MD_Metadata";

  public static final String GMD_METACARD_TYPE_NAME = GMD_PREFIX + "." + GMD_LOCAL_NAME;

  public static final String GMD_NAMESPACE = "http://www.isotc211.org/2005/gmd";

  public static final String GCO_NAMESPACE = "http://www.isotc211.org/2005/gco";

  public static final String METACARD_URI = "urn:catalog:metacard";

  public static final String APISO_PREFIX = "apiso.";

  public static final String APISO_BOUNDING_BOX = APISO_PREFIX + "BoundingBox";

  public static final String LAST_UPDATE = "lastUpdate";

  public static final String REVISION = "revision";

  public static final String CREATION = "creation";

  public static final String EXPIRY = "expiry";

  public static final String PUBLICATION = "publication";

  public static final String DATE_TIME_STAMP_PATH = "/MD_Metadata/dateStamp/gco:DateTime";

  public static final String DATE_STAMP_PATH = "/MD_Metadata/dateStamp/gco:Date";

  public static final String GCO_PREFIX = "gco";

  public static final String RESOURCE_STATUS = "ext.resource-status";

  public static final String METADATA_LANGUAGE_PATH = "/MD_Metadata/language/gco:CharacterString";

  public static final String CODE_LIST_VALUE_PATH =
      "/MD_Metadata/hierarchyLevel/MD_ScopeCode/@codeListValue";

  public static final String CODE_LIST_PATH = "/MD_Metadata/hierarchyLevel/MD_ScopeCode/@codeList";

  public static final String CRS_AUTHORITY_PATH =
      "/MD_Metadata/referenceSystemInfo/MD_ReferenceSystem/referenceSystemIdentifier/RS_Identifier/codeSpace/gco:CharacterString";

  public static final String CRS_CODE_PATH =
      "/MD_Metadata/referenceSystemInfo/MD_ReferenceSystem/referenceSystemIdentifier/RS_Identifier/code/gco:CharacterString";

  public static final String FORMAT_PATH =
      "/MD_Metadata/distributionInfo/MD_Distribution/distributionFormat/MD_Format/name/gco:CharacterString";

  public static final String FORMAT_VERSION_PATH =
      "/MD_Metadata/distributionInfo/MD_Distribution/distributionFormat/MD_Format/version/gco:CharacterString";

  public static final String DISTRIBUTOR_FORMAT_PATH =
      "/MD_Metadata/distributionInfo/MD_Distribution/distributor/MD_Distributor/distributorFormat/MD_Format/name/gco:CharacterString";

  public static final String DISTRIBUTOR_FORMAT_VERSION_PATH =
      "/MD_Metadata/distributionInfo/MD_Distribution/distributor/MD_Distributor/distributorFormat/MD_Format/version/gco:CharacterString";

  public static final String LINKAGE_URI_PATH =
      "/MD_Metadata/distributionInfo/MD_Distribution/transferOptions/MD_DigitalTransferOptions/onLine/CI_OnlineResource/linkage/URL";

  public static final String CONTACT_PHONE_PATH =
      "/MD_Metadata/contact/CI_ResponsibleParty/contactInfo/CI_Contact/phone/CI_Telephone/voice/gco:CharacterString";

  public static final String CONTACT_ADDRESS_DELIVERY_POINT_PATH =
      "/MD_Metadata/contact/CI_ResponsibleParty/contactInfo/CI_Contact/address/CI_Address/deliveryPoint/gco:CharacterString";

  public static final String CONTACT_ADDRESS_CITY_PATH =
      "/MD_Metadata/contact/CI_ResponsibleParty/contactInfo/CI_Contact/address/CI_Address/city/gco:CharacterString";

  public static final String CONTACT_ADDRESS_ADMINISTRATIVE_AREA_PATH =
      "/MD_Metadata/contact/CI_ResponsibleParty/contactInfo/CI_Contact/address/CI_Address/administrativeArea/gco:CharacterString";

  public static final String CONTACT_ADDRESS_POSTAL_CODE_PATH =
      "/MD_Metadata/contact/CI_ResponsibleParty/contactInfo/CI_Contact/address/CI_Address/postalCode/gco:CharacterString";

  public static final String CONTACT_ADDRESS_COUNTRY_PATH =
      "/MD_Metadata/contact/CI_ResponsibleParty/contactInfo/CI_Contact/address/CI_Address/country/gco:CharacterString";

  public static final String CONTACT_EMAIL_PATH =
      "/MD_Metadata/contact/CI_ResponsibleParty/contactInfo/CI_Contact/address/CI_Address/electronicMailAddress/gco:CharacterString";

  public static final String FILE_IDENTIFIER_PATH =
      "/MD_Metadata/fileIdentifier/gco:CharacterString";

  public static final String CONTACT_ORGANISATION_PATH =
      "/MD_Metadata/contact/CI_ResponsibleParty/organisationName/gco:CharacterString";

  public static final String CITATION_DATE_TYPE_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/citation/CI_Citation/date/CI_Date/dateType/CI_DateTypeCode/@codeListValue";

  public static final String BOUNDING_POLYGON_LINE_STRING_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/geographicElement/EX_BoundingPolygon/polygon/gml:LineString/gml:posList";

  public static final String BOUNDING_POLYGON_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/geographicElement/EX_BoundingPolygon/polygon/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList";

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

  public static final String LANGUAGE_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/language/gco:CharacterString";

  public static final String ASSOCIATION_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/aggregationInfo/MD_AggregateInformation/aggregateDataSetIdentifier/RS_Identifier/code/gco:CharacterString";

  public static final String ALTITUDE_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/verticalElement/EX_VerticalExtent/maximumValue/gco:Real";

  public static final String COUNTRY_CODE_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/geographicElement/EX_GeographicDescription/geographicIdentifier/RS_Identifier/code/gco:CharacterString";

  public static final String RESOURCE_STATUS_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/status/MD_ProgressCode/@codeListValue";

  public static final String TEMPORAL_START_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/temporalElement/EX_TemporalExtent/extent/gml:TimePeriod/gml:beginPosition";

  public static final String TEMPORAL_STOP_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/temporalElement/EX_TemporalExtent/extent/gml:TimePeriod/gml:endPosition";

  public static final String POINT_OF_CONTACT_ORGANISATION_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/pointOfContact/CI_ResponsibleParty/organisationName/gco:CharacterString";

  public static final String POINT_OF_CONTACT_PHONE_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/pointOfContact/CI_ResponsibleParty/contactInfo/CI_Contact/phone/CI_Telephone/voice/gco:CharacterString";

  public static final String POINT_OF_CONTACT_ADDRESS_DELIVERY_POINT_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/pointOfContact/CI_ResponsibleParty/contactInfo/CI_Contact/address/CI_Address/deliveryPoint/gco:CharacterString";

  public static final String POINT_OF_CONTACT_ADDRESS_CITY_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/pointOfContact/CI_ResponsibleParty/contactInfo/CI_Contact/address/CI_Address/city/gco:CharacterString";

  public static final String POINT_OF_CONTACT_ADDRESS_ADMINISTRATIVE_AREA_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/pointOfContact/CI_ResponsibleParty/contactInfo/CI_Contact/address/CI_Address/administrativeArea/gco:CharacterString";

  public static final String POINT_OF_CONTACT_ADDRESS_POSTAL_CODE_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/pointOfContact/CI_ResponsibleParty/contactInfo/CI_Contact/address/CI_Address/postalCode/gco:CharacterString";

  public static final String POINT_OF_CONTACT_ADDRESS_COUNTRY_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/pointOfContact/CI_ResponsibleParty/contactInfo/CI_Contact/address/CI_Address/country/gco:CharacterString";

  public static final String POINT_OF_CONTACT_EMAIL_PATH =
      "/MD_Metadata/identificationInfo/MD_DataIdentification/pointOfContact/CI_ResponsibleParty/contactInfo/CI_Contact/address/CI_Address/electronicMailAddress/gco:CharacterString";

  public static final String BOUNDING_POLYGON_POINT_PATH =
      "//MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/geographicElement/EX_BoundingPolygon/polygon/Point/pos";

  public static final String RESOURCE_DATE_PATH =
      "//MD_Metadata/identificationInfo/MD_DataIdentification/citation/CI_Citation/date";

  public static final QName GMD_REVISION_DATE_QNAME;

  public static final QName GMD_ABSTRACT_QNAME;

  public static final QName GMD_LANGUAGE_QNAME;

  public static final QName GMD_RESOURCE_LANGUAGE_QNAME;

  public static final QName GMD_ALTERNATE_TITLE_QNAME;

  public static final QName GMD_SUBJECT_QNAME;

  public static final QName GMD_CREATION_DATE_QNAME;

  public static final QName GMD_PUBLICATION_DATE_QNAME;

  public static final QName GMD_ORGANIZATION_NAME_QNAME;

  public static final QName GMD_FORMAT_QNAME;

  public static final QName GMD_MODIFIED_QNAME;

  public static final QName GMD_TYPE_QNAME;

  static {
    GMD_REVISION_DATE_QNAME = createGMDQName(GMD_REVISION_DATE);

    GMD_ALTERNATE_TITLE_QNAME = createGMDQName(GMD_ALTERNATE_TITLE);

    GMD_LANGUAGE_QNAME = createGMDQName(GMD_LANGUAGE);

    GMD_RESOURCE_LANGUAGE_QNAME = createGMDQName(GMD_RESOURCE_LANGUAGE);

    GMD_ABSTRACT_QNAME = createGMDQName(GMD_ABSTRACT);

    GMD_SUBJECT_QNAME = createGMDQName(GMD_SUBJECT);

    GMD_CREATION_DATE_QNAME = createGMDQName(GMD_CREATION_DATE);

    GMD_PUBLICATION_DATE_QNAME = createGMDQName(GMD_PUBLICATION_DATE);

    GMD_ORGANIZATION_NAME_QNAME = createGMDQName(GMD_ORGANIZATION_NAME);

    GMD_FORMAT_QNAME = createGMDQName(GMD_FORMAT);

    GMD_MODIFIED_QNAME = createGMDQName(GMD_MODIFIED);

    GMD_TYPE_QNAME = createGMDQName(GMD_TYPE);
  }

  private GmdConstants() {}

  private static QName createGMDQName(final String field) {
    return new QName(GMD_NAMESPACE, field, GMD_PREFIX);
  }
}
