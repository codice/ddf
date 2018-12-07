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

import com.google.common.net.HttpHeaders;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;

public final class CswConstants {

  private CswConstants() {}

  /**
   * Used as prefix on attribute names that clash with basic Metacard attribute names e.g., "title"
   * vs. "csw.title"
   */
  public static final String CSW_ATTRIBUTE_PREFIX = "csw.";

  public static final String CSW_NAMESPACE_URI = "http://www.opengis.net/cat/csw/2.0.2";

  public static final String CSW_METACARD_TYPE_NAME = "csw:Record";

  public static final String CSW_IDENTIFIER = "identifier";

  /** Substitution name for "identifier" */
  public static final String CSW_BIBLIOGRAPHIC_CITATION = "bibliographicCitation";

  public static final String CSW_TITLE = CSW_ATTRIBUTE_PREFIX + "title";

  /** Substitution name for "title" */
  public static final String CSW_ALTERNATIVE = "alternative";

  public static final String CSW_TYPE = "type";

  public static final String CSW_SUBJECT = "subject";

  public static final String CSW_FORMAT = "format";

  /** Substitution name for "format" */
  public static final String CSW_EXTENT = "extent";

  /** Substitution name for "format" */
  public static final String CSW_MEDIUM = "medium";

  public static final String CSW_RELATION = "relation";

  /** Substitution name for "relation" */
  public static final String CSW_CONFORMS_TO = "conformsTo";

  /** Substitution name for "relation" */
  public static final String CSW_HAS_FORMAT = "hasFormat";

  /** Substitution name for "relation" */
  public static final String CSW_HAS_PART = "hasPart";

  /** Substitution name for "relation" */
  public static final String CSW_HAS_VERSION = "hasVersion";

  /** Substitution name for "relation" */
  public static final String CSW_IS_FORMAT_OF = "isFormatOf";

  /** Substitution name for "relation" */
  public static final String CSW_IS_PART_OF = "isPartOf";

  /** Substitution name for "relation" */
  public static final String CSW_IS_REFERENCED_BY = "isReferencedBy";

  /** Substitution name for "relation" */
  public static final String CSW_IS_REPLACED_BY = "isReplacedBy";

  /** Substitution name for "relation" */
  public static final String CSW_IS_REQUIRED_BY = "isRequiredBy";

  /** Substitution name for "relation" */
  public static final String CSW_IS_VERSION_OF = "isVersionOf";

  /** Substitution name for "relation" */
  public static final String CSW_REFERENCES = "references";

  /** Substitution name for "relation" */
  public static final String CSW_REPLACES = "replaces";

  /** Substitution name for "relation" */
  public static final String CSW_REQUIRES = "requires";

  public static final String CSW_DATE = "date";

  /** Substitution name for "date" */
  public static final String CSW_MODIFIED = CSW_ATTRIBUTE_PREFIX + "modified";

  /** Substitution name for "date" */
  public static final String CSW_CREATED = CSW_ATTRIBUTE_PREFIX + "created";

  /** Substitution name for "date" */
  public static final String CSW_DATE_ACCEPTED = "dateAccepted";

  /** Substitution name for "date" */
  public static final String CSW_DATE_COPYRIGHTED = "dateCopyrighted";

  /** Substitution name for "date" */
  public static final String CSW_DATE_SUBMITTED = "dateSubmitted";

  /** Substitution name for "date" */
  public static final String CSW_ISSUED = "issued";

  /** Substitution name for "date" */
  public static final String CSW_VALID = "valid";

  // Synonyms: abstract, tableOfContents
  public static final String CSW_DESCRIPTION = CSW_ATTRIBUTE_PREFIX + "description";

  /** Substitution name for "description" */
  public static final String CSW_ABSTRACT = "abstract";

  /** Substitution name for "description" */
  public static final String CSW_TABLE_OF_CONTENTS = "tableOfContents";

  // coverage: temporal and/or spatial info
  public static final String CSW_COVERAGE = "coverage";

  public static final String CSW_SPATIAL = "spatial";

  public static final String CSW_TEMPORAL = "temporal";

  public static final String OWS_BOUNDING_BOX = "BoundingBox";

  public static final String CSW_CREATOR = "creator";

  public static final String CSW_PUBLISHER = "publisher";

  public static final String CSW_CONTRIBUTOR = "contributor";

  public static final String CSW_LANGUAGE = "language";

  public static final String CSW_RIGHTS = "rights";

  /** Substitution name for "rights" */
  public static final String CSW_ACCESS_RIGHTS = "accessRights";

  /** Substitution name for "rights" */
  public static final String CSW_LICENSE = "license";

  public static final String CSW_SOURCE = "source";

  public static final String ELEMENT_SET_TYPE = "ELEMENT_SET_TYPE";

  public static final String ELEMENT_NAMES = "ELEMENT_NAMES";

  public static final String IS_BY_ID_QUERY = "IS_BY_ID_QUERY";

  public static final String WRITE_NAMESPACES = "WRITE_NAMESPACES";

  public static final String NAMESPACE_DECLARATIONS = "NAMESPACE_DECLARATIONS";

  public static final String ROOT_NODE_NAME = "ROOT_NODE_NAME";

  public static final String CSW_MAPPING = "CSW_MAPPING";

  public static final String AXIS_ORDER_PROPERTY = "AXIS_ORDER";

  public static final String OMIT_XML_DECLARATION = "OMIT_XML_DECLARATION";

  public static final String GET_CAPABILITIES = "GetCapabilities";

  public static final String DESCRIBE_RECORD = "DescribeRecord";

  public static final String GET_RECORDS = "GetRecords";

  public static final String GET_RECORDS_RESPONSE = "GetRecordsResponse";

  public static final String GET_RECORD_BY_ID = "GetRecordById";

  public static final String GET_RECORD_BY_ID_RESPONSE = "GetRecordByIdResponse";

  public static final String CAPABILITIES = "Capabilities";

  public static final String TRANSACTION = "Transaction";

  public static final String CSW = "CSW";

  public static final String XML = "XML";

  public static final String SERVICE = "service";

  public static final String VERSION = "version";

  public static final String VERBOSE_RESPONSE = "verboseResponse";

  public static final String VERSION_2_0_2 = "2.0.2";

  public static final String VERSION_2_0_1 = "2.0.1";

  public static final String OUTPUT_FORMAT_PARAMETER = "OutputFormat";

  public static final String ELEMENT_SET_NAME_PARAMETER = "ElementSetName";

  public static final String RESULT_TYPE_PARAMETER = "ResultType";

  public static final String FEDERATED_CATALOGS = "FederatedCatalogs";

  public static final String PRODUCT_RETRIEVAL_HTTP_HEADER = "X-Csw-Product";

  public static final String BYTES = "bytes";

  public static final String BYTES_EQUAL = "bytes=";

  public static final String BYTES_TO_SKIP = "BytesToSkip";

  public static final String RANGE_HEADER = HttpHeaders.RANGE;

  public static final String ACCEPT_RANGES_HEADER = HttpHeaders.ACCEPT_RANGES;

  /*
   * typeName vs typeNames: typeName applies to DescribeRecord, where typeNames applies to
   * getRecords. However, throughout the csw 2.0.2 specification, in particular in section 10.8,
   * the use of typeName vs typeNames is inconsistent. Therefore, when parsing getCapabilities for
   * getRecords, both are accepted.
   *
   * TODO: If this proves to be a continuing problem amongst supported services, getRecordsRequest
   * may need to support typeName as well.
   */
  public static final String TYPE_NAME_PARAMETER = "typeName";

  public static final String TYPE_NAMES_PARAMETER = "typeNames";

  public static final String HANDLE_PARAMETER = "handle";

  public static final String OUTPUT_SCHEMA_PARAMETER = "OutputSchema";

  public static final String TRANSFORMER_LOOKUP_KEY = "TransformerLookupKey";

  public static final String TRANSFORMER_LOOKUP_VALUE = "TransformerLookupValue";

  public static final String CONSTRAINT_LANGUAGE_PARAMETER = "ConstraintLanguage";

  public static final String CONSTRAINT_LANGUAGE_FILTER = "Filter";

  public static final String CONSTRAINT_LANGUAGE_CQL = "CQL_Text";

  public static final List<String> CONSTRAINT_LANGUAGES =
      Arrays.asList(CONSTRAINT_LANGUAGE_FILTER, CONSTRAINT_LANGUAGE_CQL);

  public static final String OWS_NAMESPACE = "http://www.opengis.net/ows";

  public static final QName POST = new QName(OWS_NAMESPACE, "Post");

  public static final QName GET = new QName(OWS_NAMESPACE, "Get");

  public static final String POST_ENCODING = "PostEncoding";

  public static final String XMLNS = "xmlns";

  public static final String XMLNS_DEFINITION_PREFIX = XMLNS + "(";

  public static final String XMLNS_DEFINITION_POSTFIX = ")";

  public static final String ESCAPE = "!";

  public static final String SINGLE_CHAR = "#";

  public static final String WILD_CARD = "*";

  public static final String COMMA = ",";

  public static final String NAMESPACE_DELIMITER = ":";

  public static final String EQUALS_CHAR = "=";

  // NOTE: "csw:" prefix is not needed for AnyText queries
  public static final String ANY_TEXT = "AnyText";

  public static final String XML_SCHEMA_NAMESPACE_PREFIX = "xs";

  public static final String XML_SCHEMA_INSTANCE_NAMESPACE_PREFIX = "xsi";

  public static final String OWS_NAMESPACE_PREFIX = "ows";

  public static final String OGC_NAMESPACE_PREFIX = "ogc";

  public static final String GML_NAMESPACE_PREFIX = "gml";

  public static final String CSW_NAMESPACE_PREFIX = "csw";

  public static final String EBRIM_NAMESPACE_PREFIX = "rim";

  public static final String DUBLIN_CORE_NAMESPACE_PREFIX = "dc";

  public static final String DUBLIN_CORE_TERMS_NAMESPACE_PREFIX = "dct";

  public static final String CSW_RECORD_LOCAL_NAME = "Record";

  public static final String EBRIM_RECORD_LOCAL_NAME = "RegistryPackage";

  public static final String CSW_SUMMARY_RECORD_LOCAL_NAME = "SummaryRecord";

  public static final String CSW_BRIEF_RECORD_LOCAL_NAME = "BriefRecord";

  public static final String CSW_RECORD =
      CSW_NAMESPACE_PREFIX + NAMESPACE_DELIMITER + CSW_RECORD_LOCAL_NAME;

  public static final String CSW_SUMMARY_RECORD =
      CSW_NAMESPACE_PREFIX + NAMESPACE_DELIMITER + CSW_SUMMARY_RECORD_LOCAL_NAME;

  public static final String CSW_BRIEF_RECORD =
      CSW_NAMESPACE_PREFIX + NAMESPACE_DELIMITER + CSW_BRIEF_RECORD_LOCAL_NAME;

  public static final String CSW_OUTPUT_SCHEMA = "http://www.opengis.net/cat/csw/2.0.2";

  public static final String OGC_SCHEMA = "http://www.opengis.net/ogc";

  public static final String GML_SCHEMA = "http://www.opengis.net/gml";

  public static final String EBRIM_SCHEMA = "urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0";

  public static final String DUBLIN_CORE_SCHEMA = "http://purl.org/dc/elements/1.1/";

  public static final String DUBLIN_CORE_TERMS_SCHEMA = "http://purl.org/dc/terms/";

  public static final QName DC_IDENTIFIER_QNAME = new QName(DUBLIN_CORE_SCHEMA, "identifier");

  public static final QName DC_TITLE_QNAME = new QName(DUBLIN_CORE_SCHEMA, "title");

  public static final String CONSTRAINT_VERSION = "1.1.0";

  public static final String OGC_CSW_PACKAGE = "net.opengis.cat.csw.v_2_0_2";

  public static final String OGC_FILTER_PACKAGE = "net.opengis.filter.v_1_1_0";

  public static final String OGC_GML_PACKAGE = "net.opengis.gml.v_3_1_1";

  public static final String OGC_OWS_PACKAGE = "net.opengis.ows.v_1_0_0";

  public static final String XML_SCHEMA_LANGUAGE = "http://www.w3.org/XML/Schema";

  public static final String CSW_NO_PREFIX_TITLE = "title";

  public static final String CSW_NO_PREFIX_MODIFIED = "modified";

  public static final String CSW_NO_PREFIX_CREATED = "created";

  public static final String GML_POINT = "Point";

  public static final String GML_LINESTRING = "LineString";

  public static final String GML_POLYGON = "Polygon";

  public static final String BBOX_PROP = "ows:BoundingBox";

  public static final String SPATIAL_PROP = "dct:Spatial";

  public static final String METERS = "METERS";

  public static final String CRS = "crs";

  public static final String SRS_NAME = "EPSG:4326";

  public static final String SRS_URL = "urn:x-ogc:def:crs:EPSG:6.11:4326";

  public static final String OWS_UPPER_CORNER = "UpperCorner";

  public static final String OWS_LOWER_CORNER = "LowerCorner";

  public static final double DEGREES_TO_RADIANS = Math.PI / 180.0;

  public static final double RADIANS_TO_DEGREES = 1 / DEGREES_TO_RADIANS;

  public static final double EARTH_MEAN_RADIUS_METERS = 6371008.7714;

  public static final String CSW_TRANSACTION = "csw:Transaction";

  public static final String CSW_TRANSACTION_INSERT_NODE = "csw:Insert";

  public static final String CSW_TRANSACTION_UPDATE_NODE = "csw:Update";

  public static final String CSW_TRANSACTION_DELETE_NODE = "csw:Delete";

  public static final String CSW_CONSTRAINT = "csw:Constraint";

  public static final String CSW_CQL_TEXT = "csw:CqlText";

  // Exception Codes
  public static final String VERSION_NEGOTIATION_FAILED = "VersionNegotiationFailed";

  public static final String MISSING_PARAMETER_VALUE = "MissingParameterValue";

  public static final String INVALID_PARAMETER_VALUE = "InvalidParameterValue";

  public static final String OPERATION_NOT_SUPPORTED = "OperationNotSupported";

  public static final String NO_APPLICABLE_CODE = "NoApplicableCode";

  public static final String TRANSACTION_FAILED = "TransactionFailed";

  public static final String OUTPUT_FORMAT_XML = MediaType.APPLICATION_XML;

  public static final String SCHEMA_LANGUAGE_X_SCHEMA_2001 = "http://www.w3.org/2001/XMLSchema";

  public static final String SCHEMA_LANGUAGE_X_SCHEMA = "http://www.w3.org/XMLSchema";

  public static final String SCHEMA_LANGUAGE_XML_SCHEMA = "http://www.w3.org/XML/Schema";

  public static final String SCHEMA_LANGUAGE_XML_TR = "http://www.w3.org/TR/xmlschema-1/";

  public static final QName CSW_IDENTIFIER_QNAME;

  public static final QName CSW_BIBLIOGRAPHIC_CITATION_QNAME;

  public static final QName CSW_TITLE_QNAME;

  public static final QName CSW_ALTERNATIVE_QNAME;

  public static final QName CSW_TYPE_QNAME;

  public static final QName CSW_SUBJECT_QNAME;

  public static final QName CSW_FORMAT_QNAME;

  public static final QName CSW_EXTENT_QNAME;

  public static final QName CSW_MEDIUM_QNAME;

  public static final QName CSW_RELATION_QNAME;

  public static final QName CSW_CONFORMS_TO_QNAME;

  public static final QName CSW_HAS_FORMAT_QNAME;

  public static final QName CSW_HAS_PART_QNAME;

  public static final QName CSW_HAS_VERSION_QNAME;

  public static final QName CSW_IS_FORMAT_OF_QNAME;

  public static final QName CSW_IS_PART_OF_QNAME;

  public static final QName CSW_IS_REFERENCED_BY_QNAME;

  public static final QName CSW_IS_REPLACED_BY_QNAME;

  public static final QName CSW_IS_REQUIRED_BY_QNAME;

  public static final QName CSW_IS_VERSION_OF_QNAME;

  public static final QName CSW_REFERENCES_QNAME;

  public static final QName CSW_REPLACES_QNAME;

  public static final QName CSW_REQUIRES_QNAME;

  public static final QName CSW_DATE_QNAME;

  public static final QName CSW_MODIFIED_QNAME;

  public static final QName CSW_CREATED_QNAME;

  public static final QName CSW_DATE_ACCEPTED_QNAME;

  public static final QName CSW_DATE_COPYRIGHTED_QNAME;

  public static final QName CSW_DATE_SUBMITTED_QNAME;

  public static final QName CSW_ISSUED_QNAME;

  public static final QName CSW_VALID_QNAME;

  public static final QName CSW_DESCRIPTION_QNAME;

  public static final QName CSW_ABSTRACT_QNAME;

  public static final QName CSW_TABLE_OF_CONTENTS_QNAME;

  public static final QName CSW_COVERAGE_QNAME;

  public static final QName CSW_SPATIAL_QNAME;

  public static final QName CSW_TEMPORAL_QNAME;

  public static final QName OWS_BOUNDING_BOX_QNAME;

  public static final QName CSW_CREATOR_QNAME;

  public static final QName CSW_PUBLISHER_QNAME;

  public static final QName CSW_CONTRIBUTOR_QNAME;

  public static final QName CSW_LANGUAGE_QNAME;

  public static final QName CSW_RIGHTS_QNAME;

  public static final QName CSW_ACCESS_RIGHTS_QNAME;

  public static final QName CSW_LICENSE_QNAME;

  public static final QName CSW_SOURCE_QNAME;

  public static final List<QName> REQUIRED_FIELDS;

  public static final List<QName> BRIEF_CSW_RECORD_FIELDS;

  public static final List<QName> SUMMARY_CSW_RECORD_FIELDS;

  public static final List<QName> FULL_CSW_RECORD_FIELDS;

  public static final List<String> VALID_SCHEMA_LANGUAGES =
      new ArrayList<>(
          Arrays.asList(
              SCHEMA_LANGUAGE_X_SCHEMA,
              SCHEMA_LANGUAGE_XML_SCHEMA,
              SCHEMA_LANGUAGE_X_SCHEMA_2001,
              SCHEMA_LANGUAGE_XML_TR));

  public enum BinarySpatialOperand {
    GEOMETRY,
    ENVELOPE,
    NONE
  }

  static {
    CSW_TITLE_QNAME = createDublinCoreQName(CSW_NO_PREFIX_TITLE);
    CSW_MODIFIED_QNAME = createDublinCoreTermQName(CSW_NO_PREFIX_MODIFIED);
    CSW_CREATED_QNAME = createDublinCoreTermQName(CSW_NO_PREFIX_CREATED);

    CSW_IDENTIFIER_QNAME = createDublinCoreQName(CSW_IDENTIFIER);
    CSW_TYPE_QNAME = createDublinCoreQName(CSW_TYPE);
    CSW_SUBJECT_QNAME = createDublinCoreQName(CSW_SUBJECT);
    CSW_FORMAT_QNAME = createDublinCoreQName(CSW_FORMAT);
    CSW_RELATION_QNAME = createDublinCoreQName(CSW_RELATION);
    CSW_DATE_QNAME = createDublinCoreQName(CSW_DATE);
    CSW_DESCRIPTION_QNAME = createDublinCoreQName(CSW_DESCRIPTION);
    CSW_COVERAGE_QNAME = createDublinCoreQName(CSW_COVERAGE);
    CSW_CREATOR_QNAME = createDublinCoreQName(CSW_CREATOR);
    CSW_PUBLISHER_QNAME = createDublinCoreQName(CSW_PUBLISHER);
    CSW_CONTRIBUTOR_QNAME = createDublinCoreQName(CSW_CONTRIBUTOR);
    CSW_LANGUAGE_QNAME = createDublinCoreQName(CSW_LANGUAGE);
    CSW_RIGHTS_QNAME = createDublinCoreQName(CSW_RIGHTS);
    CSW_SOURCE_QNAME = createDublinCoreQName(CSW_SOURCE);

    CSW_ALTERNATIVE_QNAME = createDublinCoreTermQName(CSW_ALTERNATIVE);
    CSW_BIBLIOGRAPHIC_CITATION_QNAME = createDublinCoreTermQName(CSW_BIBLIOGRAPHIC_CITATION);
    CSW_EXTENT_QNAME = createDublinCoreTermQName(CSW_EXTENT);
    CSW_MEDIUM_QNAME = createDublinCoreTermQName(CSW_MEDIUM);
    CSW_CONFORMS_TO_QNAME = createDublinCoreTermQName(CSW_CONFORMS_TO);
    CSW_HAS_FORMAT_QNAME = createDublinCoreTermQName(CSW_HAS_FORMAT);
    CSW_HAS_PART_QNAME = createDublinCoreTermQName(CSW_HAS_PART);
    CSW_HAS_VERSION_QNAME = createDublinCoreTermQName(CSW_HAS_VERSION);
    CSW_IS_FORMAT_OF_QNAME = createDublinCoreTermQName(CSW_IS_FORMAT_OF);
    CSW_IS_PART_OF_QNAME = createDublinCoreTermQName(CSW_IS_PART_OF);
    CSW_IS_REFERENCED_BY_QNAME = createDublinCoreTermQName(CSW_IS_REFERENCED_BY);
    CSW_IS_REPLACED_BY_QNAME = createDublinCoreTermQName(CSW_IS_REPLACED_BY);
    CSW_IS_REQUIRED_BY_QNAME = createDublinCoreTermQName(CSW_IS_REQUIRED_BY);
    CSW_IS_VERSION_OF_QNAME = createDublinCoreTermQName(CSW_IS_VERSION_OF);
    CSW_REFERENCES_QNAME = createDublinCoreTermQName(CSW_REFERENCES);
    CSW_REPLACES_QNAME = createDublinCoreTermQName(CSW_REPLACES);
    CSW_REQUIRES_QNAME = createDublinCoreTermQName(CSW_REQUIRES);
    CSW_DATE_ACCEPTED_QNAME = createDublinCoreTermQName(CSW_DATE_ACCEPTED);
    CSW_DATE_COPYRIGHTED_QNAME = createDublinCoreTermQName(CSW_DATE_COPYRIGHTED);
    CSW_DATE_SUBMITTED_QNAME = createDublinCoreTermQName(CSW_DATE_SUBMITTED);
    CSW_ISSUED_QNAME = createDublinCoreTermQName(CSW_ISSUED);
    CSW_VALID_QNAME = createDublinCoreTermQName(CSW_VALID);
    CSW_ABSTRACT_QNAME = createDublinCoreTermQName(CSW_ABSTRACT);
    CSW_TABLE_OF_CONTENTS_QNAME = createDublinCoreTermQName(CSW_TABLE_OF_CONTENTS);
    CSW_SPATIAL_QNAME = createDublinCoreTermQName(CSW_SPATIAL);
    CSW_TEMPORAL_QNAME = createDublinCoreTermQName(CSW_TEMPORAL);
    CSW_ACCESS_RIGHTS_QNAME = createDublinCoreTermQName(CSW_ACCESS_RIGHTS);
    CSW_LICENSE_QNAME = createDublinCoreTermQName(CSW_LICENSE);

    OWS_BOUNDING_BOX_QNAME = new QName(OWS_NAMESPACE, OWS_BOUNDING_BOX, OWS_NAMESPACE_PREFIX);

    REQUIRED_FIELDS = Arrays.asList(CSW_IDENTIFIER_QNAME, CSW_TITLE_QNAME);

    BRIEF_CSW_RECORD_FIELDS =
        Arrays.asList(
            CSW_IDENTIFIER_QNAME, CSW_TITLE_QNAME, CSW_TYPE_QNAME, OWS_BOUNDING_BOX_QNAME);

    SUMMARY_CSW_RECORD_FIELDS =
        Arrays.asList(
            CSW_IDENTIFIER_QNAME,
            CSW_TITLE_QNAME,
            CSW_TYPE_QNAME,
            CSW_SUBJECT_QNAME,
            CSW_FORMAT_QNAME,
            CSW_RELATION_QNAME,
            CSW_MODIFIED_QNAME,
            CSW_ABSTRACT_QNAME,
            CSW_SPATIAL_QNAME,
            OWS_BOUNDING_BOX_QNAME);

    FULL_CSW_RECORD_FIELDS =
        Arrays.asList(
            CSW_IDENTIFIER_QNAME,
            CSW_BIBLIOGRAPHIC_CITATION_QNAME,
            CSW_TITLE_QNAME,
            CSW_ALTERNATIVE_QNAME,
            CSW_TYPE_QNAME,
            CSW_SUBJECT_QNAME,
            CSW_FORMAT_QNAME,
            CSW_EXTENT_QNAME,
            CSW_MEDIUM_QNAME,
            CSW_RELATION_QNAME,
            CSW_CONFORMS_TO_QNAME,
            CSW_HAS_FORMAT_QNAME,
            CSW_HAS_PART_QNAME,
            CSW_HAS_VERSION_QNAME,
            CSW_IS_FORMAT_OF_QNAME,
            CSW_IS_PART_OF_QNAME,
            CSW_IS_REFERENCED_BY_QNAME,
            CSW_IS_REPLACED_BY_QNAME,
            CSW_IS_REQUIRED_BY_QNAME,
            CSW_IS_VERSION_OF_QNAME,
            CSW_REFERENCES_QNAME,
            CSW_REPLACES_QNAME,
            CSW_REQUIRES_QNAME,
            CSW_DATE_QNAME,
            CSW_MODIFIED_QNAME,
            CSW_CREATED_QNAME,
            CSW_DATE_ACCEPTED_QNAME,
            CSW_DATE_COPYRIGHTED_QNAME,
            CSW_DATE_SUBMITTED_QNAME,
            CSW_ISSUED_QNAME,
            CSW_VALID_QNAME,
            CSW_DESCRIPTION_QNAME,
            CSW_ABSTRACT_QNAME,
            CSW_TABLE_OF_CONTENTS_QNAME,
            CSW_COVERAGE_QNAME,
            CSW_SPATIAL_QNAME,
            CSW_TEMPORAL_QNAME,
            OWS_BOUNDING_BOX_QNAME,
            CSW_CREATOR_QNAME,
            CSW_PUBLISHER_QNAME,
            CSW_CONTRIBUTOR_QNAME,
            CSW_LANGUAGE_QNAME,
            CSW_RIGHTS_QNAME,
            CSW_ACCESS_RIGHTS_QNAME,
            CSW_LICENSE_QNAME,
            CSW_SOURCE_QNAME,
            OWS_BOUNDING_BOX_QNAME);
  }

  private static QName createDublinCoreQName(final String field) {
    return new QName(DUBLIN_CORE_SCHEMA, field, DUBLIN_CORE_NAMESPACE_PREFIX);
  }

  private static QName createDublinCoreTermQName(final String field) {
    return new QName(DUBLIN_CORE_TERMS_SCHEMA, field, DUBLIN_CORE_TERMS_NAMESPACE_PREFIX);
  }
}
