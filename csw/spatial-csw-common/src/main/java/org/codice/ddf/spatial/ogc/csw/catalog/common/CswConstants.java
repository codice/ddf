/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;

public interface CswConstants {

    String GET_CAPABILITIES = "GetCapabilities";

    String DESCRIBE_RECORD = "DescribeRecord";

    String GET_RECORDS = "GetRecords";

    String GET_RECORDS_RESPONSE = "GetRecordsResponse";

    String GET_RECORD_BY_ID = "GetRecordById";

    String GET_RECORD_BY_ID_RESPONSE = "GetRecordByIdResponse";

    String CAPABILITIES = "Capabilities";

    String CSW = "CSW";

    String XML = "XML";

    String SERVICE = "service";

    String VERSION = "version";

    String VERSION_2_0_2 = "2.0.2";

    String VERSION_2_0_1 = "2.0.1";

    String OUTPUT_FORMAT_PARAMETER = "OutputFormat";

    String ELEMENT_SET_NAME_PARAMETER = "ElementSetName";

    String RESULT_TYPE_PARAMETER = "ResultType";

    /*
     * typeName vs typeNames: typeName applies to DescribeRecord, where typeNames applies to
     * getRecords. However, throughout the csw 2.0.2 specification, in particular in section 10.8,
     * the use of typeName vs typeNames is inconsistent. Therefore, when parsing getCapabilities for
     * getRecords, both are accepted.
     * 
     * TODO: If this proves to be a continuing problem amongst supported services, getRecordsRequest
     * may need to support typeName as well.
     */
    String TYPE_NAME_PARAMETER = "typeName";

    String TYPE_NAMES_PARAMETER = "typeNames";

    String OUTPUT_SCHEMA_PARAMETER = "OutputSchema";

    String CONSTRAINT_LANGUAGE_PARAMETER = "ConstraintLanguage";

    String CONSTRAINT_LANGUAGE_FILTER = "Filter";

    String CONSTRAINT_LANGUAGE_CQL = "CQL_Text";

    String OWS_NAMESPACE = "http://www.opengis.net/ows";

    QName POST = new QName(OWS_NAMESPACE, "Post");

    QName GET = new QName(OWS_NAMESPACE, "Get");

    String POST_ENCODING = "PostEncoding";
    
    String XMLNS_DEFINITION_PREFIX = "xmlns(";

    String XMLNS_DEFINITION_POSTFIX = ")";

    String ESCAPE = "!";

    String SINGLE_CHAR = "#";

    String WILD_CARD = "*";

    public static final String COMMA = ",";

    public static final String NAMESPACE_DELIMITER = ":";

    public static final String EQUALS = "=";

    // NOTE: "csw:" prefix is not needed for AnyText queries 
    String ANY_TEXT = "AnyText";

    String XML_SCHEMA_NAMESPACE_PREFIX = "xs";
    
    String XML_SCHEMA_INSTANCE_NAMESPACE_PREFIX = "xsi";

    String OWS_NAMESPACE_PREFIX = "ows";

    String OGC_NAMESPACE_PREFIX = "ogc";

    String GML_NAMESPACE_PREFIX = "gml";

    String CSW_NAMESPACE_PREFIX = "csw";

    String DUBLIN_CORE_NAMESPACE_PREFIX = "dc";

    String DUBLIN_CORE_TERMS_NAMESPACE_PREFIX = "dct";

    String CSW_RECORD_LOCAL_NAME = "Record";

    String CSW_SUMMARY_RECORD_LOCAL_NAME = "SummaryRecord";

    String CSW_BRIEF_RECORD_LOCAL_NAME = "BriefRecord";

    String CSW_RECORD = CSW_NAMESPACE_PREFIX + NAMESPACE_DELIMITER + CSW_RECORD_LOCAL_NAME;

    String CSW_SUMMARY_RECORD = CSW_NAMESPACE_PREFIX + NAMESPACE_DELIMITER + CSW_SUMMARY_RECORD_LOCAL_NAME;

    String CSW_BRIEF_RECORD = CSW_NAMESPACE_PREFIX + NAMESPACE_DELIMITER + CSW_BRIEF_RECORD_LOCAL_NAME;

    String CSW_OUTPUT_SCHEMA = "http://www.opengis.net/cat/csw/2.0.2";

    String OGC_SCHEMA = "http://www.opengis.net/ogc";

    String GML_SCHEMA = "http://www.opengis.net/gml";

    String DUBLIN_CORE_SCHEMA = "http://purl.org/dc/elements/1.1/";

    String DUBLIN_CORE_TERMS_SCHEMA = "http://purl.org/dc/terms/";

    String CSW_OGCCORE_OUTPUT_SCHEMA = "OGCCORE";
    
    String CONSTRAINT_VERSION = "1.1.0";

    String OGC_CSW_PACKAGE = "net.opengis.cat.csw.v_2_0_2";

    String OGC_FILTER_PACKAGE = "net.opengis.filter.v_1_1_0";

    String OGC_GML_PACKAGE = "net.opengis.gml.v_3_1_1";

    String OGC_OWS_PACKAGE = "net.opengis.ows.v_1_0_0";
    
    String XML_SCHEMA_LANGUAGE = "http://www.w3.org/XML/Schema";

    String CSW_TYPE = "type";

    String CSW_TITLE = "title";

    String CSW_MODIFIED = "modified";
    
    String CSW_CREATED = "created";
    
    String GML_POINT = "Point";
    
    String GML_LINESTRING = "LineString";
    
    String GML_POLYGON = "Polygon";

    String BBOX_PROP = "ows:BoundingBox";

    String SPATIAL_PROP = "dct:Spatial";

    String METERS = "METERS";

    String CRS = "crs";
    
    String SRS_NAME = "EPSG:4326";
    
    String SRS_URL = "urn:x-ogc:def:crs:EPSG:6.11:4326";
    
    String OWS_UPPER_CORNER = "UpperCorner";
    
    String OWS_LOWER_CORNER = "LowerCorner";
    
    String WCS_PRODUCT_RETRIEVAL = "WCS";
    
    String SOURCE_URI_PRODUCT_RETRIEVAL = "Source URI";

    double DEGREES_TO_RADIANS = Math.PI / 180.0;

    double RADIANS_TO_DEGREES = 1 / DEGREES_TO_RADIANS;

    double EARTH_MEAN_RADIUS_METERS = 6371008.7714;
    
    // Exception Codes
    static final String VERSION_NEGOTIATION_FAILED = "VersionNegotiationFailed";
    static final String MISSING_PARAMETER_VALUE = "MissingParameterValue";
    static final String INVALID_PARAMETER_VALUE = "InvalidParameterValue";
    static final String NONEXISTENT_TYPE = "NonexistentType";
    static final String OPERATION_NOT_SUPPORTED = "OperationNotSupported";
    static final String NO_APPLICABLE_CODE = "NoApplicableCode";    

    public static final String OUTPUT_FORMAT_XML = MediaType.APPLICATION_XML;

//    public static final List<String> VALID_OUTPUT_FORMATS = new ArrayList<String>(Arrays.asList(
//            MediaType.TEXT_XML, MediaType.APPLICATION_XML));

    public static final String SCHEMA_LANGUAGE_X_SCHEMA_2001 = "http://www.w3.org/2001/XMLSchema";
    public static final String SCHEMA_LANGUAGE_X_SCHEMA = "http://www.w3.org/XMLSchema";
    public static final String SCHEMA_LANGUAGE_XML_SCHEMA = "http://www.w3.org/XML/Schema";
    public static final String SCHEMA_LANGUAGE_XML_TR = "http://www.w3.org/TR/xmlschema-1/";
    
    public static final List<String> VALID_SCHEMA_LANGUAGES = new ArrayList<String>(
            Arrays.asList(
                    SCHEMA_LANGUAGE_X_SCHEMA,
                    SCHEMA_LANGUAGE_XML_SCHEMA,
                    SCHEMA_LANGUAGE_X_SCHEMA_2001,
                    SCHEMA_LANGUAGE_XML_TR));

    enum BinarySpatialOperand {
        GEOMETRY, ENVELOPE, NONE
    }

    String ELEMENT_SET_TYPE = "ELEMENT_SET_TYPE";

    String ELEMENT_NAMES = "ELEMENT_NAMES";

    String IS_BY_ID_QUERY = "IS_BY_ID_QUERY";

    String IS_VALIDATE_QUERY = "IS_VALIDATE_QUERY";

    String WRITE_NAMESPACES = "WRITE_NAMESPACES";

    String ROOT_NODE_NAME = "ROOT_NODE_NAME";

    String CSW_MAPPING = "CSW_MAPPING";

    String PRODUCT_RETRIEVAL_METHOD = "PRODUCT_RETRIEVAL_METHOD";

    String IS_LON_LAT_ORDER_PROPERTY = "isLonLatOrder";

    String OMIT_XML_DECLARATION = "OMIT_XML_DECLARATION";
}
