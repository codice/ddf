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
package org.codice.ddf.spatial.ogc.csw.catalog.source;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static net.opengis.filter.v_1_1_0.SpatialOperatorNameType.BBOX;
import static net.opengis.filter.v_1_1_0.SpatialOperatorNameType.BEYOND;
import static net.opengis.filter.v_1_1_0.SpatialOperatorNameType.CONTAINS;
import static net.opengis.filter.v_1_1_0.SpatialOperatorNameType.CROSSES;
import static net.opengis.filter.v_1_1_0.SpatialOperatorNameType.D_WITHIN;
import static net.opengis.filter.v_1_1_0.SpatialOperatorNameType.DISJOINT;
import static net.opengis.filter.v_1_1_0.SpatialOperatorNameType.INTERSECTS;
import static net.opengis.filter.v_1_1_0.SpatialOperatorNameType.OVERLAPS;
import static net.opengis.filter.v_1_1_0.SpatialOperatorNameType.TOUCHES;
import static net.opengis.filter.v_1_1_0.SpatialOperatorNameType.WITHIN;
import static net.opengis.filter.v_1_1_0.ComparisonOperatorType.BETWEEN;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import net.opengis.filter.v_1_1_0.AbstractIdType;
import net.opengis.filter.v_1_1_0.ComparisonOperatorType;
import net.opengis.filter.v_1_1_0.ComparisonOperatorsType;
import net.opengis.filter.v_1_1_0.FeatureIdType;
import net.opengis.filter.v_1_1_0.FilterCapabilities;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.GeometryOperandsType;
import net.opengis.filter.v_1_1_0.LogicalOperators;
import net.opengis.filter.v_1_1_0.ObjectFactory;
import net.opengis.filter.v_1_1_0.ScalarCapabilitiesType;
import net.opengis.filter.v_1_1_0.SpatialCapabilitiesType;
import net.opengis.filter.v_1_1_0.SpatialOperatorNameType;
import net.opengis.filter.v_1_1_0.SpatialOperatorType;
import net.opengis.filter.v_1_1_0.SpatialOperatorsType;
import net.opengis.filter.v_1_1_0.UnaryLogicOpType;
import net.opengis.ows.v_1_0_0.DomainType;
import net.opengis.ows.v_1_0_0.Operation;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswJAXBElementProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geotools.geometry.jts.Geometries;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ddf.catalog.data.Metacard;

public class TestCswFilterDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCswFilterDelegate.class);

    private static final JAXBContext jaxbContext = initJaxbContext();

    private final CswFilterDelegate cswFilterDelegateLatLon = createCswFilterDelegate(
            initCswSourceConfiguration(false, false, CswRecordMetacardType.CSW_TYPE));
    private final CswFilterDelegate cswFilterDelegateLatLonPosList = createCswFilterDelegate(
            initCswSourceConfiguration(false, true, CswRecordMetacardType.CSW_TYPE));

    private static final String FILTER_QNAME_LOCAL_PART = "Filter";

    private static final String DEFAULT_PROPERTY_NAME = "title";

    private final String propertyName = DEFAULT_PROPERTY_NAME;

    private final String propertyNameAnyText = Metacard.ANY_TEXT;

    private final String propertyNameModified = Metacard.MODIFIED;

    private final String propertyNameEffective = Metacard.EFFECTIVE;

    private final String propertyNameCreated = Metacard.CREATED;

    private final String effectiveDateMapping = "created";

    private final String modifiedDateMapping = "modified";

    private final String createdDateMapping = "dateSubmitted";

    private final String contentTypeLiteral = "myType";

    private final String stringLiteral = "1";

    private final short shortLiteral = 1;

    private final int intLiteral = 1;

    private final long longLiteral = 1L;

    private final float floatLiteral = 1.0F;

    private final double doubleLiteral = 1.0;

    private final boolean booleanLiteral = true;

    private final boolean isCaseSensitive = false;

    private final String stringLowerBoundary = "5";

    private final String stringUpperBoundary = "15";

    private final int intLowerBoundary = 5;

    private final int intUpperBoundary = 15;

    private final short shortLowerBoundary = 5;

    private final short shortUpperBoundary = 15;

    private final float floatLowerBoundary = 5.0F;

    private final float floatUpperBoundary = 15.0F;

    private final double doubleLowerBoundary = 5.0F;

    private final float doubleUpperBoundary = 15.0F;

    private final long longLowerBoundary = 5L;

    private final long longUpperBoundary = 15L;

    private final Object objectLiteral = new Object();

    private final byte[] byteArrayLiteral = new String("myBytes").getBytes();

    private final String likeLiteral = "*bar*";

    private final String polygonWkt = "POLYGON ((30 -10, 30 30, 10 30, 10 -10, 30 -10))";

    private final String pointWkt = "POINT (30 30)";

    private final String lineStringWkt = "LINESTRING (30 -10, 30 30, 10 30, 10 -10)";

    private final String multiPolygonWkt = "MULTIPOLYGON (((40 40, 20 45, 45 30, 40 40)),((20 35, 45 20, 30 5, 10 10, 10 30, 20 35),(30 20, 20 25, 20 15, 30 20)))";

    private final String multiPointWkt = "MULTIPOINT ((10 40), (40 30), (20 20), (30 10))";

    private final String multiLineStringWkt = "MULTILINESTRING ((10 10, 20 20, 10 40),(40 40, 30 30, 40 20, 30 10))";

    private static final double SAMPLE_DISTANCE = 123.456;

    private static final QName NOT_LOGIC_OPS_NAME = new QName("http://www.opengis.net/ogc", "Not");

    private static final String POINT_WKT = "POINT (30 10)";

    private static final String REPLACE_START_DATE = "REPLACE_START_DATE";

    private static final String REPLACE_END_DATE = "REPLACE_END_DATE";

    private static final String REPLACE_TEMPORAL_PROPERTY = "REPLACE_TEMPORAL_PROPERTY";
    
    private static final String LAT_LON_LINESTRING_COORDINATES_STRING = 
            "-10.0 30.0 30.0 30.0 30.0 10.0 -10.0 10.0";
    private static final String LAT_LON_POLYGON_COORDINATES_STRING = 
            LAT_LON_LINESTRING_COORDINATES_STRING + " -10.0 30.0";
    private static final String LON_LAT_POLYGON_COORDINATES_STRING = 
            "30.0 -10.0 30.0 30.0 10.0 30.0 10.0 -10.0 30.0 -10.0";

    private static final String USE_POS_LIST_GEO_FILTER_PROP_MAP_KEY = "usePosList";
    private static final String DISTANCE_GEO_FILTER_PROP_MAP_KEY = "distance";
    private static final Date SAMPLE_NON_ISO_8601_DATE;
    static {
        try {
            SAMPLE_NON_ISO_8601_DATE = new SimpleDateFormat("MMM d yyyy").parse("Jun 11 2003");
        } catch (ParseException pe) {
            LOGGER.error("Unable to instantiate SAMPLE_NON_ISO_8601_DATE", pe);
            throw new RuntimeException();
        }
    }
    
    // NOTE: The contents of these maps ARE mutable
    private static final Map<String, String> POS_LIST_GEO_FILTER_PROP_MAP = new HashMap<String, String>();
    private static final Map<String, String> SAMPLE_DISTANCE_GEO_FILTER_PROP_MAP = new HashMap<String, String>();  
    private static final Map<String, String> SAMPLE_DISTANCE_POS_LIST_GEO_FILTER_PROP_MAP = new HashMap<String, String>();
    private static final Map<String, String> THOUSAND_METER_DISTANCE_GEO_FILTER_PROP_MAP = new HashMap<String, String>();
       
    private final DateTime testStartDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
    private final DateTime testEndDate =  new DateTime(2013, 12, 31, 0, 0, 0, 0); 
    private StringWriter writer = null;
    private static Marshaller marshaller = null;
    
    private enum CompoundExpressionOperator {
        AND, OR, NOT;
        
        @Override
        public String toString() {
            // Convert to CamelCase
            return WordUtils.capitalizeFully(this.name());
        }
    }
    
    private enum ComparisonOperator {
        PROPERTY_IS_BETWEEN,
        PROPERTY_IS_EQUAL_TO,
        PROPERTY_IS_NOT_EQUAL_TO,
        PROPERTY_IS_NULL,
        PROPERTY_IS_GREATER_THAN,
        PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO,
        PROPERTY_IS_LESS_THAN,
        PROPERTY_IS_LESS_THAN_OR_EQUAL_TO,
        PROPERTY_IS_LIKE;
        
        private enum ComparisonStringOperatorType {
            NO_OP, NUMERIC, NUMERIC_RANGE, STRING, WILDCARD;
        }
        
        @Override
        public String toString() {
            char[] delimiters = {'_'};
            return WordUtils.capitalizeFully(this.name(), delimiters).replaceAll("_", "");
        }
        
        /**
         * Returns the data type against which this ComparisonOperator can
         * be performed.
         */
        public ComparisonStringOperatorType getComparatorType() {
            ComparisonStringOperatorType returnType = null;
            switch (this) {
            
            case PROPERTY_IS_GREATER_THAN:
            case PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO:
            case PROPERTY_IS_LESS_THAN:
            case PROPERTY_IS_LESS_THAN_OR_EQUAL_TO:
                returnType = ComparisonStringOperatorType.NUMERIC;
                break;
            case PROPERTY_IS_BETWEEN:
                returnType = ComparisonStringOperatorType.NUMERIC_RANGE;
                break;
            case PROPERTY_IS_EQUAL_TO:
            case PROPERTY_IS_NOT_EQUAL_TO:
                returnType = ComparisonStringOperatorType.STRING;
                break;
            case PROPERTY_IS_LIKE:
                returnType = ComparisonStringOperatorType.WILDCARD;
                break;
            case PROPERTY_IS_NULL:
            default:
                returnType = ComparisonStringOperatorType.NO_OP;
                break;
            }
            
            return returnType;
        }
        
        /**
         * Returns the number of arguments required to perform a comparison
         * of this type.
         */
        public int getNumArgs() {
            int numArgs = 0;
            
            switch (this) {
            case PROPERTY_IS_BETWEEN:
                numArgs = 2;
                break;
            case PROPERTY_IS_EQUAL_TO:
            case PROPERTY_IS_NOT_EQUAL_TO:
            case PROPERTY_IS_GREATER_THAN:
            case PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO:
            case PROPERTY_IS_LESS_THAN:
            case PROPERTY_IS_LESS_THAN_OR_EQUAL_TO:
            case PROPERTY_IS_LIKE:
                numArgs = 1;
                break;
            case PROPERTY_IS_NULL:
            default:
                break;
                    
            }
            
            return numArgs;
        }
    }
    
    /**
     * Used to represent values that can be used within the 
     * <PropertyName> elements of Filter strings.
     */
    private enum GeospatialPropertyName {
        BOUNDING_BOX("ows", "BoundingBox"),
        SPATIAL("dct", "Spatial");
        
        String namespace = "";
        String propName = "";
        
        GeospatialPropertyName(String namespace, String propName) {
            this.namespace = namespace;
            this.propName = propName;
        }
        
        @Override
        public String toString() {
            return (namespace + ":" + propName);
        }
    }

    /**
     * Returns the standard XML Filter header string
     * 
     * @return
     */
    private String getXmlHeaderString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">";
    }

    /**
     * Returns the standard XML Filter footer string
     * 
     * @return
     */
    private String getXmlFooterString() {
        return "</ns3:Filter>";
    }

    /**
     * Creates a property comparison Filter string without the standard XML
     * header and footer.
     * 
     * @param comparisonOp The comparison operator to use when building the
     *      Filter string
     * @param propertyName The name of the property to include in the 
     *      <PropertyName>
     * @param args Zero or more strings to be used within the comparison
     *      Filter. For example, a PROPERTY_IS_BETWEEN comparison operator 
     *      requires two arguments, a string representing a lower boundary and a 
     *      string representing an upper boundary, so two Strings must be 
     *      supplied as parameters to this method.
     *      
     * @return
     */
    private String createComparisonFilterStringWithoutHeaderAndFooter(
            ComparisonOperator comparisonOp, String propertyName, String...args) {
        
        String expression = 
                "<ns3:" + comparisonOp;
        
        if (comparisonOp.getComparatorType() == 
                ComparisonOperator.ComparisonStringOperatorType.WILDCARD) {
            expression += " wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\"";
        } else if (comparisonOp.getComparatorType() == ComparisonOperator.ComparisonStringOperatorType.STRING) {
            expression += " matchCase=\"false\"";
        }
        
        expression += ">"
               +  "<ns3:PropertyName>" + propertyName + "</ns3:PropertyName>";
        
        if (args.length == comparisonOp.getNumArgs()) {
            switch (comparisonOp.getComparatorType()) {
            case NUMERIC_RANGE:
                expression +=  
                   "<ns3:LowerBoundary>"
                +     "<ns3:Literal>" + args[0] + "</ns3:Literal>"
                +  "</ns3:LowerBoundary>"
                +  "<ns3:UpperBoundary>"
                +     "<ns3:Literal>" + args[1] + "</ns3:Literal>"
                +  "</ns3:UpperBoundary>";
                break;
            case NO_OP:
                // Do Nothing
                break;
            default:
                expression += 
                   "<ns3:Literal>" + args[0] + "</ns3:Literal>";
            }
        }
    
        expression +=
                "</ns3:" + comparisonOp + ">";
        
        return expression;
    }

    /**
     * Creates a property comparison Filter string 
     * 
     * @param comparisonOp The comparison operator to use when building the
     *      Filter string
     * @param propertyName The name of the property to include in the 
     *      <PropertyName>
     * @param args See {@link #createComparisonFilterStringWithoutHeaderAndFooter(ComparisonOperator, String, String...)
     *      for description of this argument.
     * @return A comparison Filter string
     */
    private String createComparisonFilterString(ComparisonOperator comparisonOp, 
            String propertyName, String...args) {
        String compString = getXmlHeaderString() 
                + createComparisonFilterStringWithoutHeaderAndFooter(comparisonOp, propertyName, args)
                + getXmlFooterString();
        
        return compString;
    }

    /**
     * Creates a compound expression Filter string by wrapping the received 
     * expressions with a compound expression operator (e.g., AND, OR, NOT).
     * 
     * @param compoundOperator The {@code CompoundExpressionOperator} with which
     *      to wrap the expressions
     * @param expressions The expressions to be wrapped. If the expressions
     *      contain a header/footer, it will be removed (the
     *      header/footer will be added to the compound expression) 
     * @return 
     */
    private String createCompoundExpressionFilterString(
            CompoundExpressionOperator compoundOperator, String...expressions) {
        String compoundExpression = null;
    
        if (compoundOperator != null && expressions != null && 
                expressions.length > 0) {
            compoundExpression = getXmlHeaderString() + "<ns3:" + compoundOperator + ">";
            
            for (String expression : expressions) {
                // If the expression was compiled with a header and footer, 
                // remove them (so as not to nest headers/footers inside of
                // the compound expression)
                compoundExpression += expression.replace(getXmlHeaderString(), "")
                        .replace(getXmlFooterString(), "");
            }
    
            compoundExpression += "</ns3:" + compoundOperator + ">"
                    + getXmlFooterString();
        }
        
        return compoundExpression;
    }

    /**
     * Converts a string of coordinates into <pos> elements
     * 
     * @param coordinatesString A string of coordinate pairs. Each coordinate 
     *      pair is converted into a <pos> element in the order it appears in
     *      the string. For example:
     *      
     *      "10.0 20.0 0.0 20.0"
     *      
     *      Will be converted to:
     *      
     *      "<ns4:pos>10.0 20.0</ns4:pos><ns4:pos>0.0 20.0"</ns4:pos>"
     *      
     * @return A string of <pos> elements representing the received coordinates
     */
    private String createPosElementsString(String coordinatesString) {
        String pointStr = "";
        
        String[] coordinatesArray = coordinatesString.split(" ");
        
        if (coordinatesArray.length % 2 != 0) {
            throw new IllegalArgumentException("Coordinates string is malformed: "
                    + "Missing a coordinate - Cannot form a complete coordinate pair.");
        }
        
        for (int ii = 0; ii + 1 < coordinatesArray.length; ii = ii + 2) {
            pointStr += "<ns4:pos>" + coordinatesArray[ii] + " " + 
                    coordinatesArray[ii + 1] + "</ns4:pos>";
        }
        
        return pointStr;
    }

    /**
     * Creates a LinearRing Filter string. 
     * 
     * @param usePosList If true, will construct the LinearRing using a 
     *      single <posList> element, rather than including a <pos> element for
     *      each coordinate pair in the coordinates string.
     * @param coordinatesString A string of ordered coordinate pairs that 
     *      represent the polygon. For an example see 
     *      {@link #LAT_LON_POLYGON_COORDINATES_STRING}.
     * @return
     */
    private String createLinearRingFilterString(boolean usePosList, 
            String coordinatesString) {
        String linearRingStr = "<ns4:LinearRing>";
        
        if (usePosList) {
            linearRingStr += "<ns4:posList>" + coordinatesString + "</ns4:posList>";
        } else {
            linearRingStr += createPosElementsString(coordinatesString);
        }
        
        linearRingStr += "</ns4:LinearRing>";
        
        return linearRingStr;
    }

    /**
     * Given a point string of the form "x y" or "y x", returns the corresponding
     * XML point.
     * 
     * @param pointCoordinatesString A string containing a single coordinates
     *      pair
     * @return A string of the form "<ns4:Point>x y</ns4:Point>
     */
    private String createPointFilterString(String pointCoordinatesString) {
        int numCoords = pointCoordinatesString.split(" ").length;
        if (numCoords != 2) {
            throw new IllegalArgumentException("Invalid pointString \"" + 
                    pointCoordinatesString + "\"");
        }
        
        return  "<ns4:Point>" + createPosElementsString(pointCoordinatesString) 
                + "</ns4:Point>";
    }

    /**
     * Creates a MultiPoint Filter string. 
     * 
     * @param multiPointCoordinatesString A space-separated list of coordinates 
     *      pairs. For an example see {@link #LAT_LON_LINESTRING_COORDINATES_STRING} 
     * @return A string of the form 
     *      "<ns4:pointMember><ns4:Point>x y</ns4:Point></ns4:pointMember>
     *       <ns4:pointMember><ns4:Point>y z</ns4:Point></ns4:pointMember>"
     */
    private String createMultiPointMembersFilterString(String multiPointCoordinatesString) {
        String[] coordinates = multiPointCoordinatesString.split(" ");
        
        if (coordinates.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Coordinates string contains invalid number of coordinates");
        }
        
        String multiPointMembersFilter = "";
        
        for (int ii = 0; ii + 1 < coordinates.length; ii = ii + 2) {
            multiPointMembersFilter += 
                    "<ns4:pointMember>" + createPointFilterString(
                            coordinates[ii] + " " + coordinates[ii + 1]) + 
                    "</ns4:pointMember>";
        }
        
        return multiPointMembersFilter;
    }
    
    /**
     * Creates an Envelop Filter string
     * 
     * @param spatialOperator The spatial operator to use in the Filter string
     * @param propertyName The PropertyName to use in the Filter string
     * @param lowerCornerPointCoords A pair of coordinates of the form "x y"
     *      that represent the lower corner.
     * @param upperCornerPointCoords A pair of coordinates of the form "x y"
     *      that represent the upper corner.
     * @return
     */
    private String createEnvelopeFilterString(SpatialOperatorNameType spatialOperator,
            GeospatialPropertyName propertyName, String lowerCornerPointCoords,
            String upperCornerPointCoords) {

        String spatialOpName;
        
        switch (spatialOperator) {
        case BBOX:
            // Leave in ALL CAPS
            spatialOpName = spatialOperator.name();
            break;
        default:
            spatialOpName = WordUtils.capitalizeFully(spatialOperator.name());
            break;
        }
        
        String envelopeFilterString = getXmlHeaderString()
                + "<ns3:" + spatialOpName + ">" 
                +    "<ns3:PropertyName>" + propertyName + "</ns3:PropertyName>"
                +    "<ns4:Envelope>"
                +       "<ns4:lowerCorner>" + lowerCornerPointCoords +"</ns4:lowerCorner>"
                +       "<ns4:upperCorner>" + upperCornerPointCoords +"</ns4:upperCorner>"
                +    "</ns4:Envelope>"
                + "</ns3:" + spatialOpName + ">" 
                + getXmlFooterString();
        
        return envelopeFilterString;
        
    }

    /**
     * A convenience method for calling {@code createGeospatialFilterString} 
     * without a property map.
     */
    private String createGeospatialFilterString(SpatialOperatorNameType spatialOperator,
            GeospatialPropertyName propertyName, Geometries geoType, String coordinatesString) {
        return createGeospatialFilterString(spatialOperator, propertyName, 
                geoType, coordinatesString, null);
    }

    /**
     * Creates a geospatial Filter string. 
     * 
     * @param spatialOperator The spatial operator (e.g., BBOX, INTERSECTS, 
     *      WITHIN) to use in the Filter string
     * @param propertyName he PropertyName to use in the Filter string
     * @param geoType The type of geometry (e.g., LINESTRING, POINT, POLYGON)
     *      the Filter string will represent
     * @param coordinatesString A string of space-separated coordinates to
     *      use when constructing the geometry Filter string
     * @param propertyMap A map of additional properties. Currently valid
     *      properties that will be used when applicable include:
     *      
     *      {@link #USE_POS_LIST_GEO_FILTER_PROP_MAP_KEY} A string with a value
     *      of either "true" or "false." When true, a single <posList> element, 
     *      rather than a set of <pos> elements, is used in building a
     *      <LinearRing>.
     *      
     *      {@link #DISTANCE_GEO_FILTER_PROP_MAP_KEY} When present, a 
     *      <Distance> element will be included in the geospatial Filter string
     *      using the distance value included in the property map.
     * 
     * @return A string representing a geospatial Filter
     */
    private String createGeospatialFilterString(SpatialOperatorNameType spatialOperator,
            GeospatialPropertyName propertyName, Geometries geoType, String coordinatesString, 
            Map<String, String> propertyMap) {
        
        if (null == propertyMap) {
            propertyMap = new HashMap<String, String>();
        }
        
        char[] delimiters = {'_'};
        String spatialOpName = WordUtils.capitalizeFully(
                spatialOperator.name(), delimiters).replaceAll("_", "");
        
        String geoTypeName = null;
        switch (geoType) {
        case LINESTRING:
            geoTypeName = "LineString";
            break;
        case MULTIPOINT:
            geoTypeName = "MultiPoint";
            break;
        default:
            geoTypeName = WordUtils.capitalizeFully(geoType.name());
        }
        
        String geoFilterStr = getXmlHeaderString() 
                + "<ns3:" + spatialOpName + ">"
                +    "<ns3:PropertyName>" + propertyName + "</ns3:PropertyName>"
                +    "<ns4:" + geoTypeName;
        
    
        switch (geoType) {
        case LINESTRING:
        case MULTIPOINT:
        case POINT:
        case POLYGON:
            geoFilterStr += " srsName=\"EPSG:4326\"";
            break;
        default:
            break;
        }
        
        geoFilterStr +=    ">";
        
        switch (geoType) {
        case LINESTRING:
        case POINT:
            geoFilterStr += createPosElementsString(coordinatesString);
            break;
        case MULTIPOINT:
            geoFilterStr += createMultiPointMembersFilterString(coordinatesString);
            break;
        case POLYGON:
            geoFilterStr += "<ns4:exterior>" + createLinearRingFilterString(
                    Boolean.valueOf(propertyMap.get(USE_POS_LIST_GEO_FILTER_PROP_MAP_KEY)), 
                    coordinatesString) + "</ns4:exterior>";
            break;
        default:
            // Do nothing    
        }    
        
        geoFilterStr += "</ns4:" + geoTypeName + ">";
        
        String distance = propertyMap.get(DISTANCE_GEO_FILTER_PROP_MAP_KEY);
        if (!StringUtils.isBlank(distance)) {
            geoFilterStr += "<ns3:Distance units=\"METERS\">" 
                    + distance + "</ns3:Distance>";
        }
        
        geoFilterStr += "</ns3:" + spatialOpName + ">" + getXmlFooterString();
        
        LOGGER.debug("geoFilterString: {}", geoFilterStr);
        return geoFilterStr;
    }

    private final String duringXml = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_BETWEEN, REPLACE_TEMPORAL_PROPERTY, 
            REPLACE_START_DATE, REPLACE_END_DATE);

    private final String propertyIsEqualToXml = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_EQUAL_TO, DEFAULT_PROPERTY_NAME, "1");

    private final String propertyIsEqualToXmlWithDecimal = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_EQUAL_TO, DEFAULT_PROPERTY_NAME, "1.0");

    private final String propertyIsEqualToXmlAnyText = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_EQUAL_TO, CswConstants.ANY_TEXT, "1");

    private final String propertyIsEqualToXmlContentType = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_EQUAL_TO, "type", "myType");

    private final String propertyIsEqualToXmlWithNonIso8601Date = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_EQUAL_TO, DEFAULT_PROPERTY_NAME, 
            convertDateToIso8601Format(SAMPLE_NON_ISO_8601_DATE).toString());

    private final String propertyIsEqualToXmlWithBoolean = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_EQUAL_TO, DEFAULT_PROPERTY_NAME, "true");

    private final String propertyIsNotEqualToXml = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_NOT_EQUAL_TO, DEFAULT_PROPERTY_NAME, "1");

    private final String propertyIsNotEqualToXmlWithDecimal = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_NOT_EQUAL_TO, DEFAULT_PROPERTY_NAME, "1.0");

    private final String propertyIsNotEqualToXmlAnyText = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_NOT_EQUAL_TO, CswConstants.ANY_TEXT, "1");

    private final String propertyIsNotEqualToXmlWithBoolean = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_NOT_EQUAL_TO, DEFAULT_PROPERTY_NAME, "true");

    private final String propertyIsGreaterThanXml = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_GREATER_THAN, DEFAULT_PROPERTY_NAME, "1");

    private final String propertyIsGreaterThanXmlWithDecimal = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_GREATER_THAN, DEFAULT_PROPERTY_NAME, "1.0");

    private final String propertyIsGreaterThanXmlAnyText = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_GREATER_THAN, CswConstants.ANY_TEXT, "1");

    private final String propertyIsGreaterThanOrEqualToXml = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO, DEFAULT_PROPERTY_NAME, "1");

    private final String propertyIsGreaterThanOrEqualToXmlWithDecimal = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO, DEFAULT_PROPERTY_NAME, "1.0");

    private final String propertyIsGreaterThanOrEqualToXmlAnyText = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO, CswConstants.ANY_TEXT, "1");

    private final String propertyIsLessThanXml = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_LESS_THAN, DEFAULT_PROPERTY_NAME, "1");

    private final String propertyIsLessThanXmlWithDecimal = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_LESS_THAN, DEFAULT_PROPERTY_NAME, "1.0");

    private final String propertyIsLessThanXmlAnyText = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_LESS_THAN, CswConstants.ANY_TEXT, "1");

    private final String propertyIsLessThanOrEqualToXml = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO, DEFAULT_PROPERTY_NAME, "1");

    private final String propertyIsLessThanOrEqualToXmlWithDecimal = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO, DEFAULT_PROPERTY_NAME, "1.0");

    private final String propertyIsLessThanOrEqualToXmlAnyText = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO, CswConstants.ANY_TEXT, "1");

    private final String propertyIsBetweenXml = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_BETWEEN, DEFAULT_PROPERTY_NAME, "5", "15");

    private final String propertyIsBetweenXmlWithDecimal = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_BETWEEN, DEFAULT_PROPERTY_NAME, "5.0", "15.0");

    private final String propertyIsNullXml = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_NULL, DEFAULT_PROPERTY_NAME);

    private final String propertyIsLikeXml = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_LIKE, DEFAULT_PROPERTY_NAME, "*bar*");

    private final String propertyIsLikeXmlAnyText = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_LIKE, CswConstants.ANY_TEXT, "*bar*");

    private final String orComparisonOpsXml = createCompoundExpressionFilterString(
            CompoundExpressionOperator.OR, 
            createComparisonFilterStringWithoutHeaderAndFooter(ComparisonOperator.PROPERTY_IS_EQUAL_TO, DEFAULT_PROPERTY_NAME, "true"),
            createComparisonFilterStringWithoutHeaderAndFooter(ComparisonOperator.PROPERTY_IS_LIKE, DEFAULT_PROPERTY_NAME, "*bar*"));
    
    private final String intersectsPolygonXmlPropertyOwsBoundingBox = 
            createGeospatialFilterString(INTERSECTS, 
                    GeospatialPropertyName.BOUNDING_BOX, Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING);
    
    private final String intersectsPolygonXmlPropertyOwsBoundingBoxPosList = 
            createGeospatialFilterString(INTERSECTS, 
                    GeospatialPropertyName.BOUNDING_BOX, Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING, POS_LIST_GEO_FILTER_PROP_MAP);
    
    private final String intersectsPolygonXmlPropertyOwsBoundingBoxLonLatIsKeptAsLonLat = 
            createGeospatialFilterString(INTERSECTS, 
                    GeospatialPropertyName.BOUNDING_BOX, Geometries.POLYGON, 
                    LON_LAT_POLYGON_COORDINATES_STRING);

    private final String bboxXmlPropertyOwsBoundingBox = 
            createEnvelopeFilterString(BBOX, GeospatialPropertyName.BOUNDING_BOX, 
                    "-10.0 10.0", "30.0 30.0");

    private final String intersectsPolygonXmlPropertyDctSpatial = 
            createGeospatialFilterString(INTERSECTS, 
                    GeospatialPropertyName.SPATIAL, Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING);
    
    private final String intersectsPolygonXmlPropertyDctSpatialPosList = 
            createGeospatialFilterString(INTERSECTS, 
                    GeospatialPropertyName.SPATIAL, Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING, POS_LIST_GEO_FILTER_PROP_MAP);

    private final String intersectsPointXmlPropertyOwsBoundingBox = 
            createGeospatialFilterString(INTERSECTS, 
                    GeospatialPropertyName.BOUNDING_BOX, Geometries.POINT, 
                    "30.0 30.0");

    private final String intersectsLineStringXmlPropertyOwsBoundingBox = 
            createGeospatialFilterString(INTERSECTS, 
                    GeospatialPropertyName.BOUNDING_BOX, 
                    Geometries.LINESTRING, LAT_LON_LINESTRING_COORDINATES_STRING);

    private final String intersectsMultiPolygonXmlPropertyOwsBoundingBox = 
            getXmlHeaderString()
            + "<ns3:Intersects>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:MultiPolygon srsName=\"EPSG:4326\">"
            + "<ns4:polygonMember>"
            + "<ns4:Polygon>"
            + "<ns4:exterior>"
            + createLinearRingFilterString(false, "40.0 40.0 45.0 20.0 30.0 45.0 40.0 40.0")
            + "</ns4:exterior>"
            + "</ns4:Polygon>"
            + "</ns4:polygonMember>"
            + "<ns4:polygonMember>"
            + "<ns4:Polygon>"
            + "<ns4:exterior>"
            + createLinearRingFilterString(false, "35.0 20.0 20.0 45.0 5.0 30.0 10.0 10.0 30.0 10.0 35.0 20.0")
            + "</ns4:exterior>"
            + "<ns4:interior>"
            + createLinearRingFilterString(false, "20.0 30.0 25.0 20.0 15.0 20.0 20.0 30.0")
            + "</ns4:interior>"
            + "</ns4:Polygon>"
            + "</ns4:polygonMember>"
            + "</ns4:MultiPolygon>"
            + "</ns3:Intersects>" 
            + getXmlFooterString();

    private final String intersectsMultiPointXmlPropertyOwsBoundingBox = 
            createGeospatialFilterString(INTERSECTS, 
                    GeospatialPropertyName.BOUNDING_BOX, 
                    Geometries.MULTIPOINT, 
                    "40.0 10.0 30.0 40.0 20.0 20.0 10.0 30.0");

    private final String intersectsMultiLineStringXmlPropertyOwsBoundingBox = 
            getXmlHeaderString()
            + "<ns3:Intersects>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:MultiLineString srsName=\"EPSG:4326\">"
            + "<ns4:lineStringMember>"
            + "<ns4:LineString>"
            + createPosElementsString("10.0 10.0 20.0 20.0 40.0 10.0")
            + "</ns4:LineString>"
            + "</ns4:lineStringMember>"
            + "<ns4:lineStringMember>"
            + "<ns4:LineString>"
            + createPosElementsString("40.0 40.0 30.0 30.0 20.0 40.0 10.0 30.0")
            + "</ns4:LineString>"
            + "</ns4:lineStringMember>"
            + "</ns4:MultiLineString>"
            + "</ns3:Intersects>" 
            + getXmlFooterString();

    private final String intersectsEnvelopeXmlPropertyOwsBoundingBox = 
            createEnvelopeFilterString(INTERSECTS, 
                    GeospatialPropertyName.BOUNDING_BOX, "-10.0 10.0", "30.0 30.0");


    private final String crossesPolygonXmlPropertyOwsBoundingBox = 
            createGeospatialFilterString(CROSSES, 
                    GeospatialPropertyName.BOUNDING_BOX, 
                    Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING);
    
    private final String crossesPolygonXmlPropertyOwsBoundingBoxPosList = 
            createGeospatialFilterString(CROSSES, 
                    GeospatialPropertyName.BOUNDING_BOX, Geometries.POLYGON,
                    LAT_LON_POLYGON_COORDINATES_STRING, POS_LIST_GEO_FILTER_PROP_MAP);

    private final String withinPolygonXmlPropertyOwsBoundingBox = 
            createGeospatialFilterString(WITHIN, 
                    GeospatialPropertyName.BOUNDING_BOX, 
                    Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING);

    private final String withinPolygonXmlPropertyOwsBoundingBoxPosList = 
            createGeospatialFilterString(WITHIN, 
                    GeospatialPropertyName.BOUNDING_BOX, 
                    Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING,
                    POS_LIST_GEO_FILTER_PROP_MAP);
    
    private final String containsPolygonXmlPropertyOwsBoundingBox = 
            createGeospatialFilterString(CONTAINS, 
                    GeospatialPropertyName.BOUNDING_BOX, 
                    Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING);
    
    private final String containsPolygonXmlPropertyOwsBoundingBoxPosList = 
            createGeospatialFilterString(CONTAINS, 
            GeospatialPropertyName.BOUNDING_BOX, Geometries.POLYGON, 
            LAT_LON_POLYGON_COORDINATES_STRING, POS_LIST_GEO_FILTER_PROP_MAP);

    private final String disjointPolygonXmlPropertyOwsBoundingBox = 
            createGeospatialFilterString(DISJOINT, 
                    GeospatialPropertyName.BOUNDING_BOX, 
                    Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING);
    
    private final String disjointPolygonXmlPropertyOwsBoundingBoxPosList = 
            createGeospatialFilterString(DISJOINT, 
                    GeospatialPropertyName.BOUNDING_BOX, Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING, POS_LIST_GEO_FILTER_PROP_MAP);

    private final String notDisjointPolygonXmlPropertyOwsBoundingBox = 
            createCompoundExpressionFilterString(CompoundExpressionOperator.NOT, 
                    createGeospatialFilterString(DISJOINT, 
                            GeospatialPropertyName.BOUNDING_BOX, 
                            Geometries.POLYGON, 
                            LAT_LON_POLYGON_COORDINATES_STRING));


    private final String overlapsPolygonXmlPropertyOwsBoundingBox = 
            createGeospatialFilterString(OVERLAPS, 
                    GeospatialPropertyName.BOUNDING_BOX, 
                    Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING);
    
    private final String overlapsPolygonXmlPropertyOwsBoundingBoxPosList = 
            createGeospatialFilterString(OVERLAPS, 
                    GeospatialPropertyName.BOUNDING_BOX, Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING, POS_LIST_GEO_FILTER_PROP_MAP);

    private final String touchesPolygonXmlPropertyOwsBoundingBox = 
            createGeospatialFilterString(TOUCHES, 
                    GeospatialPropertyName.BOUNDING_BOX, 
                    Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING);
    
    private final String touchesPolygonXmlPropertyOwsBoundingBoxPosList = 
            createGeospatialFilterString(TOUCHES, 
                    GeospatialPropertyName.BOUNDING_BOX, Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING, POS_LIST_GEO_FILTER_PROP_MAP);

    private final String beyondPointXmlPropertyOwsBoundingBox = 
            createGeospatialFilterString(BEYOND, 
                    GeospatialPropertyName.BOUNDING_BOX, Geometries.POINT, 
                    "30.0 30.0", SAMPLE_DISTANCE_GEO_FILTER_PROP_MAP);

    private final String dwithinPolygonXmlPropertyOwsBoundingBox = 
            createGeospatialFilterString(D_WITHIN, 
                    GeospatialPropertyName.BOUNDING_BOX, Geometries.POLYGON, 
                    LAT_LON_POLYGON_COORDINATES_STRING, 
                    SAMPLE_DISTANCE_GEO_FILTER_PROP_MAP);
    
    private final String dwithinPolygonXmlPropertyOwsBoundingBoxPosList = 
            createGeospatialFilterString(D_WITHIN, 
                GeospatialPropertyName.BOUNDING_BOX, Geometries.POLYGON, 
                LAT_LON_POLYGON_COORDINATES_STRING, 
                SAMPLE_DISTANCE_POS_LIST_GEO_FILTER_PROP_MAP);

    // (NOT PropertyIsLike) OR PropertyIsEqualTo
    private final String orLogicOpsXml =
            createCompoundExpressionFilterString(CompoundExpressionOperator.OR, 
                    createCompoundExpressionFilterString(CompoundExpressionOperator.NOT, 
                            createComparisonFilterStringWithoutHeaderAndFooter(ComparisonOperator.PROPERTY_IS_LIKE, DEFAULT_PROPERTY_NAME, "*bar*")), 
                    createComparisonFilterStringWithoutHeaderAndFooter(ComparisonOperator.PROPERTY_IS_EQUAL_TO, DEFAULT_PROPERTY_NAME, "true"));
            
    // DWithin OR PropertyIsLike
    private final String orSpatialOpsXml = createCompoundExpressionFilterString(
            CompoundExpressionOperator.OR, 
            createGeospatialFilterString(D_WITHIN, 
                    GeospatialPropertyName.BOUNDING_BOX, Geometries.POINT, "10.0 30.0", 
                    THOUSAND_METER_DISTANCE_GEO_FILTER_PROP_MAP),
            createComparisonFilterStringWithoutHeaderAndFooter(ComparisonOperator.PROPERTY_IS_LIKE, 
                    DEFAULT_PROPERTY_NAME, "*bar*"));

    // PropertyIsEqualTo AND PropertyIsLike
    private final String andComparisonOpsXml = createCompoundExpressionFilterString(
            CompoundExpressionOperator.AND, 
                createComparisonFilterStringWithoutHeaderAndFooter(ComparisonOperator.PROPERTY_IS_EQUAL_TO, DEFAULT_PROPERTY_NAME, "true"),
                createComparisonFilterStringWithoutHeaderAndFooter(ComparisonOperator.PROPERTY_IS_LIKE, DEFAULT_PROPERTY_NAME, "*bar*"));

    // (NOT PropertyIsLike) AND PropertyIsEqualTo
    private final String andLogicOpsXml =
            createCompoundExpressionFilterString(CompoundExpressionOperator.AND, 
                    createCompoundExpressionFilterString(CompoundExpressionOperator.NOT, 
                            createComparisonFilterStringWithoutHeaderAndFooter(ComparisonOperator.PROPERTY_IS_LIKE, DEFAULT_PROPERTY_NAME, "*bar*")), 
                    createComparisonFilterStringWithoutHeaderAndFooter(ComparisonOperator.PROPERTY_IS_EQUAL_TO, DEFAULT_PROPERTY_NAME, "true"));

    private String dWithinFallbackToIntersects1 = createEnvelopeFilterString(
            INTERSECTS, GeospatialPropertyName.BOUNDING_BOX, 
            "29.998889735046777 29.998889735046777", 
            "30.001110264953223 30.001110264953223");

    private String dWithinFallbackToIntersects2 = 
            createGeospatialFilterString(INTERSECTS, 
                    GeospatialPropertyName.BOUNDING_BOX, 
                    Geometries.POLYGON, 
                       "30.0 30.001110264953223"
                    + " 29.999783398052752 30.00108893152347"
                    + " 29.999575119996866 30.00102575106595"
                    + " 29.999383169841224 30.00092315157021"
                    + " 29.99921492412266 30.00078507587734"
                    + " 29.99907684842979 30.000616830158776"
                    + " 29.99897424893405 30.000424880003134"
                    + " 29.99891106847653 30.000216601947248"
                    + " 29.998889735046777 30.0"
                    + " 29.99891106847653 29.999783398052752"
                    + " 29.99897424893405 29.999575119996866"
                    + " 29.99907684842979 29.999383169841224"
                    + " 29.99921492412266 29.99921492412266"
                    + " 29.999383169841224 29.99907684842979"
                    + " 29.999575119996866 29.99897424893405"
                    + " 29.999783398052752 29.99891106847653"
                    + " 30.0 29.998889735046777"
                    + " 30.000216601947248 29.99891106847653"
                    + " 30.000424880003134 29.99897424893405"
                    + " 30.000616830158776 29.99907684842979"
                    + " 30.00078507587734 29.99921492412266"
                    + " 30.00092315157021 29.999383169841224"
                    + " 30.00102575106595 29.999575119996866"
                    + " 30.00108893152347 29.999783398052752"
                    + " 30.001110264953223 30.0"
                    + " 30.00108893152347 30.000216601947248"
                    + " 30.00102575106595 30.000424880003134"
                    + " 30.00092315157021 30.000616830158776"
                    + " 30.00078507587734 30.00078507587734"
                    + " 30.000616830158776 30.00092315157021"
                    + " 30.000424880003134 30.00102575106595"
                    + " 30.000216601947248 30.00108893152347"
                    + " 30.0 30.001110264953223");

    // DWithin AND PropertyIsLike
    private final String andSpatialOpsXml = createCompoundExpressionFilterString(
            CompoundExpressionOperator.AND, 
            createGeospatialFilterString(D_WITHIN, 
                    GeospatialPropertyName.BOUNDING_BOX, Geometries.POINT, "10.0 30.0", 
                    THOUSAND_METER_DISTANCE_GEO_FILTER_PROP_MAP),
            createComparisonFilterStringWithoutHeaderAndFooter(ComparisonOperator.PROPERTY_IS_LIKE, 
                    DEFAULT_PROPERTY_NAME, "*bar*"));

    private final String configurableContentTypeMappingXml = createComparisonFilterString(
            ComparisonOperator.PROPERTY_IS_EQUAL_TO, "format", "myContentType");

    private final String emptyFilterXml = getXmlHeaderString() + getXmlFooterString();
    
    @BeforeClass
    public static void setupTestClass() throws JAXBException, ParseException {
        XMLUnit.setIgnoreWhitespace(true);
        marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        
        POS_LIST_GEO_FILTER_PROP_MAP.put(USE_POS_LIST_GEO_FILTER_PROP_MAP_KEY, "true");
        
        SAMPLE_DISTANCE_GEO_FILTER_PROP_MAP.put(DISTANCE_GEO_FILTER_PROP_MAP_KEY, 
                Double.toString(SAMPLE_DISTANCE));
        
        SAMPLE_DISTANCE_POS_LIST_GEO_FILTER_PROP_MAP.put(USE_POS_LIST_GEO_FILTER_PROP_MAP_KEY, "true");
        SAMPLE_DISTANCE_POS_LIST_GEO_FILTER_PROP_MAP.put(DISTANCE_GEO_FILTER_PROP_MAP_KEY, 
                Double.toString(SAMPLE_DISTANCE));
        
        THOUSAND_METER_DISTANCE_GEO_FILTER_PROP_MAP.put(DISTANCE_GEO_FILTER_PROP_MAP_KEY, "1000.0");

        // XPath query support.
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("", "http://www.opengis.net/cat/csw/2.0.2");
        map.put("ogc", "http://www.opengis.net/ogc");
        NamespaceContext ctx = new SimpleNamespaceContext(map);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    
    @Before 
    public void preTest() throws JAXBException {
        writer = new StringWriter();
    }
    
    /**
     * Property is equal to tests
     */
    @Test
    public void testPropertyIsEqualToStringLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsEqualTo(
                propertyName, stringLiteral, isCaseSensitive);
        assertXMLEqual(propertyIsEqualToXml, getXmlFromMarshaller(filterType));
    }

    /**
     * The CSW Source should be able to map a csw:Record field to Content Type.
     * 
     * Verify that when an isEqualTo query is sent to the CswFilterDelegate Metacard.CONTENT_TYPE is
     * mapped to the configured content type mapping (eg. in this test Metacard.CONTENT_TYPE is
     * mapped to format).
     */
    @Test
    public void testConfigurableContentTypeMapping() 
        throws JAXBException, SAXException, IOException {
        
        // Setup
        String contentTypeMapping = CswRecordMetacardType.CSW_FORMAT;
        String contentType = "myContentType";
        CswFilterDelegate localCswFilterDelegate = createCswFilterDelegate(
                initCswSourceConfiguration(false, false, contentTypeMapping));

        // Perform Test
        /**
         * Incoming query with Metacard.CONTENT_TYPE equal to myContentType. Metacard.CONTENT_TYPE
         * will be mapped to format in the CswFilterDelegate.
         */
        FilterType filterType = localCswFilterDelegate.propertyIsEqualTo(Metacard.CONTENT_TYPE,
                contentType, isCaseSensitive);

        // Verify
        /**
         * Verify that a PropertyIsEqualTo filter is created with PropertyName of format and Literal
         * equal to myContentType
         */
        assertXMLEqual(configurableContentTypeMappingXml, getXmlFromMarshaller(filterType));
    }

    /**
     * Verify that when given a non ISO 8601 formatted date, the CswFilterDelegate converts the date
     * to ISO 8601 format (ie. the xml generated off of the filterType should have an ISO 8601
     * formatted date in it).
     */
    @Test
    public void testPropertyIsEqualToDateLiteral() 
        throws JAXBException, ParseException, SAXException, IOException {
        
        LOGGER.debug("Input date: {}", SAMPLE_NON_ISO_8601_DATE);
        LOGGER.debug("ISO 8601 formatted date: {}", convertDateToIso8601Format(SAMPLE_NON_ISO_8601_DATE));
        FilterType filterType = cswFilterDelegateLatLon.propertyIsEqualTo(propertyName, SAMPLE_NON_ISO_8601_DATE);
        assertXMLEqual(propertyIsEqualToXmlWithNonIso8601Date, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsEqualToStringLiteralAnyText() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsEqualTo(
                propertyNameAnyText, stringLiteral, isCaseSensitive);
        assertXMLEqual(propertyIsEqualToXmlAnyText, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testDuring() throws JAXBException, SAXException, IOException {
        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(
                false, false, CswRecordMetacardType.CSW_TYPE, effectiveDateMapping, 
                createdDateMapping, modifiedDateMapping);

        CswFilterDelegate localCswFilterDelegate = createCswFilterDelegate(cswSourceConfiguration);
        
        String xml = getXmlProperty(localCswFilterDelegate, propertyNameModified, 
                BETWEEN, testStartDate.toCalendar(null).getTime(), 
                testEndDate.toCalendar(null).getTime());

        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String startDateStr = fmt.print(testStartDate);
        String endDateStr = fmt.print(testEndDate);
        String testResponse = duringXml.replace(REPLACE_START_DATE, startDateStr).replace(REPLACE_END_DATE, endDateStr).replace(REPLACE_TEMPORAL_PROPERTY, modifiedDateMapping);
        assertXMLEqual(testResponse, xml);

    }

    @Test
    public void testDuringAlteredEffectiveDateMapping() 
        throws JAXBException, SAXException, IOException {

        String replacedTemporalProperty = "myEffectiveDate";
        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(
                false, false, CswRecordMetacardType.CSW_TYPE, replacedTemporalProperty, createdDateMapping, modifiedDateMapping);

        CswFilterDelegate localCswFilterDelegate = createCswFilterDelegate(cswSourceConfiguration);
        
        String xml = getXmlProperty(localCswFilterDelegate, propertyNameEffective,
                BETWEEN, testStartDate.toCalendar(null).getTime(), 
                testEndDate.toCalendar(null).getTime());
        
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String startDateStr = fmt.print(testStartDate);
        String endDateStr = fmt.print(testEndDate);
        String testResponse = duringXml.replace(REPLACE_START_DATE, startDateStr).replace(REPLACE_END_DATE, endDateStr).replace(REPLACE_TEMPORAL_PROPERTY, replacedTemporalProperty);
        assertXMLEqual(testResponse, xml);
    }

    @Test
    public void testDuringAlteredCreatedDateMapping() 
        throws JAXBException, SAXException, IOException {
        
        String replacedTemporalProperty = "myCreatedDate";
        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(
                false, false, CswRecordMetacardType.CSW_TYPE, effectiveDateMapping, 
                replacedTemporalProperty, modifiedDateMapping);

        CswFilterDelegate localCswFilterDelegate = createCswFilterDelegate(cswSourceConfiguration);
        
        String xml = getXmlProperty(localCswFilterDelegate, propertyNameCreated,
                BETWEEN, testStartDate.toCalendar(null).getTime(), 
                testEndDate.toCalendar(null).getTime());
        
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String startDateStr = fmt.print(testStartDate);
        String endDateStr = fmt.print(testEndDate);
        String testResponse = duringXml.replace(REPLACE_START_DATE, startDateStr).replace(REPLACE_END_DATE, endDateStr).replace(REPLACE_TEMPORAL_PROPERTY, replacedTemporalProperty);
        assertXMLEqual(testResponse, xml);

    }

    @Test
    public void testDuringAlteredModifiedDateMapping() 
        throws JAXBException, SAXException, IOException {
        
        String replacedTemporalProperty = "myModifiedDate";
        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(
                false, false, CswRecordMetacardType.CSW_TYPE, effectiveDateMapping, 
                createdDateMapping, replacedTemporalProperty);

        CswFilterDelegate localCswFilterDelegate = createCswFilterDelegate(cswSourceConfiguration);
        
        String xml = getXmlProperty(localCswFilterDelegate, propertyNameModified, 
                BETWEEN, testStartDate.toCalendar(null).getTime(), 
                testEndDate.toCalendar(null).getTime());
        
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String startDateStr = fmt.print(testStartDate);
        String endDateStr = fmt.print(testEndDate);
        String testResponse = duringXml.replace(REPLACE_START_DATE, startDateStr).replace(REPLACE_END_DATE, endDateStr).replace(REPLACE_TEMPORAL_PROPERTY, replacedTemporalProperty);
        assertXMLEqual(testResponse, xml);
    }


    @Test
    public void testRelative() throws JAXBException, SAXException, IOException {
        long duration = 92000000000L;
        
        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(
                false, false, CswRecordMetacardType.CSW_TYPE, effectiveDateMapping, 
                createdDateMapping, modifiedDateMapping);
        CswFilterDelegate localCswFilterDelegate = createCswFilterDelegate(cswSourceConfiguration);
        
        Map<String, Object> propMap = new HashMap<String, Object>();
        propMap.put("extendedComparisonOp", "relative");
        propMap.put("duration", new Long(duration));
        
        String xml = getXmlProperty(localCswFilterDelegate, propertyNameModified, 
                BETWEEN, testStartDate.toCalendar(null).getTime(), 
                testEndDate.toCalendar(null).getTime(), propMap);

        String durationCompare = duringXml.replace(REPLACE_START_DATE, "").replace(REPLACE_END_DATE, "").replace(REPLACE_TEMPORAL_PROPERTY, modifiedDateMapping);
        String pattern = "(?i)(<ns.:Literal>)(.+?)(</ns.:Literal>)";
        String compareXml = xml.replaceAll(pattern, "<ogc:Literal xmlns:ogc=\"http://www.opengis.net/ogc\"></ogc:Literal>");

        assertXMLEqual(durationCompare, compareXml);

    }

    @Test
    public void testPropertyIsEqualToStringLiteralNonQueryableProperty() 
        throws JAXBException, SAXException, IOException {
        
        /**
         * See CswRecordMetacardType.java for queryable and non-queryable properties.
         */
        String nonQueryableProperty = Metacard.METADATA;
        FilterType filterType = cswFilterDelegateLatLon.propertyIsEqualTo(nonQueryableProperty,
                stringLiteral, isCaseSensitive);
        assertXMLEqual(emptyFilterXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsEqualToStringLiteralType() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsEqualTo(Metacard.CONTENT_TYPE,
                contentTypeLiteral, isCaseSensitive);
        assertXMLEqual(propertyIsEqualToXmlContentType, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsEqualToIntLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsEqualTo(propertyName, intLiteral);
        assertXMLEqual(propertyIsEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsEqualToShortLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsEqualTo(propertyName, shortLiteral);
        assertXMLEqual(propertyIsEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsEqualToLongLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsEqualTo(propertyName, longLiteral);
        assertXMLEqual(propertyIsEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsEqualToFloatLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsEqualTo(propertyName, floatLiteral);
        assertXMLEqual(propertyIsEqualToXmlWithDecimal, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsEqualToDoubleLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegateLatLon.propertyIsEqualTo(propertyName, doubleLiteral);
        assertXMLEqual(propertyIsEqualToXmlWithDecimal, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsEqualToBooleanLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegateLatLon.propertyIsEqualTo(propertyName, booleanLiteral);
        assertXMLEqual(propertyIsEqualToXmlWithBoolean, getXmlFromMarshaller(filterType));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsEqualToByteArrayLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsEqualTo(propertyName, byteArrayLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsEqualToObjectLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsEqualTo(propertyName, objectLiteral);
    }

    /**
     * Property is not equal to tests
     */
    @Test
    public void testPropertyIsNotEqualToStringLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsNotEqualTo(
                propertyName, stringLiteral, isCaseSensitive);
        assertXMLEqual(propertyIsNotEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsNotEqualToStringLiteralAnyText() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsNotEqualTo(propertyNameAnyText,
                stringLiteral, isCaseSensitive);
        assertXMLEqual(propertyIsNotEqualToXmlAnyText, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsNotEqualToIntLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsNotEqualTo(propertyName, intLiteral);
        assertXMLEqual(propertyIsNotEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsNotEqualToShortLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsNotEqualTo(propertyName, shortLiteral);
        assertXMLEqual(propertyIsNotEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsNotEqualToLongLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsNotEqualTo(propertyName, longLiteral);
        assertXMLEqual(propertyIsNotEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsNotEqualToFloatLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsNotEqualTo(propertyName, floatLiteral);
        assertXMLEqual(propertyIsNotEqualToXmlWithDecimal, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsNotEqualToDoubleLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsNotEqualTo(propertyName, doubleLiteral);
        assertXMLEqual(propertyIsNotEqualToXmlWithDecimal, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsNotEqualToBooleanLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon
                .propertyIsNotEqualTo(propertyName, booleanLiteral);
        assertXMLEqual(propertyIsNotEqualToXmlWithBoolean, getXmlFromMarshaller(filterType));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsNotEqualToByteArrayLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsNotEqualTo(propertyName, byteArrayLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsNotEqualToObjectLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsNotEqualTo(propertyName, objectLiteral);
    }

    /**
     * Property is greater than tests
     */
    @Test
    public void testPropertyIsGreaterThanStringLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsGreaterThan(
                propertyName, stringLiteral);
        assertXMLEqual(propertyIsGreaterThanXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsGreaterThanStringLiteralAnyText() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsGreaterThan(
                propertyNameAnyText, stringLiteral);
        assertXMLEqual(propertyIsGreaterThanXmlAnyText, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsGreaterThanIntLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsGreaterThan(propertyName, intLiteral);
        assertXMLEqual(propertyIsGreaterThanXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsGreaterThanShortLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsGreaterThan(propertyName, shortLiteral);
        assertXMLEqual(propertyIsGreaterThanXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsGreaterThanLongLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsGreaterThan(propertyName, longLiteral);
        assertXMLEqual(propertyIsGreaterThanXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsGreaterThanFloatLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsGreaterThan(propertyName, floatLiteral);
        assertXMLEqual(propertyIsGreaterThanXmlWithDecimal, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsGreaterThanDoubleLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsGreaterThan(
                propertyName, doubleLiteral);
        assertXMLEqual(propertyIsGreaterThanXmlWithDecimal, getXmlFromMarshaller(filterType));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsGreaterThanBooleanLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsGreaterThan(propertyName, booleanLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsGreaterThanByteArrayLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsGreaterThan(propertyName, byteArrayLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsGreaterThanObjectLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsGreaterThan(propertyName, objectLiteral);
    }

    /**
     * Property is greater than or equal to tests
     */
    @Test
    public void testPropertyIsGreaterThanOrEqualToStringLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsGreaterThanOrEqualTo(
                propertyName, stringLiteral);
        assertXMLEqual(propertyIsGreaterThanOrEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToStringLiteralAnyText() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsGreaterThanOrEqualTo(
                propertyNameAnyText, stringLiteral);
        assertXMLEqual(propertyIsGreaterThanOrEqualToXmlAnyText, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToIntLiteral() 
        throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegateLatLon.propertyIsGreaterThanOrEqualTo(
                propertyName, intLiteral);
        assertXMLEqual(propertyIsGreaterThanOrEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToShortLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsGreaterThanOrEqualTo(
                propertyName, shortLiteral);
        assertXMLEqual(propertyIsGreaterThanOrEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToLongLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsGreaterThanOrEqualTo(
                propertyName, longLiteral);
        assertXMLEqual(propertyIsGreaterThanOrEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToFloatLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsGreaterThanOrEqualTo(
                propertyName, floatLiteral);
        assertXMLEqual(propertyIsGreaterThanOrEqualToXmlWithDecimal, 
                getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToDoubleLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsGreaterThanOrEqualTo(
                propertyName, doubleLiteral);
        assertXMLEqual(propertyIsGreaterThanOrEqualToXmlWithDecimal, 
                getXmlFromMarshaller(filterType));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsGreaterThanOrEqualToBooleanLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsGreaterThanOrEqualTo(propertyName, 
                booleanLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsGreaterThanOrEqualToByteArrayLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsGreaterThanOrEqualTo(propertyName,
                byteArrayLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsGreaterThanOrEqualToObjectLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsGreaterThanOrEqualTo(propertyName,
                objectLiteral);
    }

    /**
     * Property is less than tests
     */
    @Test
    public void testPropertyIsLessThanStringLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLessThan(
                propertyName, stringLiteral);
        assertXMLEqual(propertyIsLessThanXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsLessThanStringLiteralAnyText() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLessThan(propertyNameAnyText,
                stringLiteral);
        assertXMLEqual(propertyIsLessThanXmlAnyText, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsLessThanIntLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLessThan(
                propertyName, intLiteral);
        assertXMLEqual(propertyIsLessThanXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsLessThanShortLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLessThan(
                propertyName, shortLiteral);
        assertXMLEqual(propertyIsLessThanXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsLessThanLongLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLessThan(
                propertyName, longLiteral);
        assertXMLEqual(propertyIsLessThanXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsLessThanFloatLiteral() 
        throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLessThan(propertyName, floatLiteral);
        assertXMLEqual(propertyIsLessThanXmlWithDecimal, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsLessThanDoubleLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLessThan(propertyName, doubleLiteral);
        assertXMLEqual(propertyIsLessThanXmlWithDecimal, getXmlFromMarshaller(filterType));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsLessThanBooleanLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsLessThan(propertyName, booleanLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsLessThanByteArrayLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsLessThan(propertyName, byteArrayLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsLessThanObjectLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsLessThan(propertyName, objectLiteral);
    }

    /**
     * Property is less than or equal to tests
     */
    @Test
    public void testPropertyIsLessThanOrEqualToStringLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLessThanOrEqualTo(
                propertyName, stringLiteral);
        assertXMLEqual(propertyIsLessThanOrEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsLessThanOrEqualToStringLiteralAnyText() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLessThanOrEqualTo(propertyNameAnyText,
                stringLiteral);
        assertXMLEqual(propertyIsLessThanOrEqualToXmlAnyText, 
                getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsLessThanOrEqualToIntLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLessThanOrEqualTo(
                propertyName, intLiteral);
        assertXMLEqual(propertyIsLessThanOrEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsLessThanOrEqualToShortLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLessThanOrEqualTo(propertyName,
                intLiteral);
        assertXMLEqual(propertyIsLessThanOrEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsLessThanOrEqualToLongLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLessThanOrEqualTo(propertyName,
                shortLiteral);
        assertXMLEqual(propertyIsLessThanOrEqualToXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsLessThanOrEqualToFloatLiteral() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLessThanOrEqualTo(propertyName,
                floatLiteral);
        assertXMLEqual(propertyIsLessThanOrEqualToXmlWithDecimal, 
                getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyIsLessThanOrEqualToDoubleLiteral() 
        throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLessThanOrEqualTo(
                propertyName, doubleLiteral);
        assertXMLEqual(propertyIsLessThanOrEqualToXmlWithDecimal, getXmlFromMarshaller(filterType));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsLessThanOrEqualToBooleanLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsLessThanOrEqualTo(propertyName, booleanLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsLessThanOrEqualToByteArrayLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsLessThanOrEqualTo(propertyName, byteArrayLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsLessThanOrEqualToObjectLiteral() throws JAXBException {
        cswFilterDelegateLatLon.propertyIsLessThanOrEqualTo(propertyName, objectLiteral);
    }

    /**
     * Property is between tests
     */
    @Test
    public void testPropertyBetweenStringLiterals() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsBetween(
                propertyName, stringLowerBoundary, stringUpperBoundary);
        assertXMLEqual(propertyIsBetweenXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyBetweenIntLiterals() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsBetween(
                propertyName, intLowerBoundary, intUpperBoundary);
        assertXMLEqual(propertyIsBetweenXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyBetweenShortLiterals() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsBetween(
                propertyName, shortLowerBoundary, shortUpperBoundary);
        assertXMLEqual(propertyIsBetweenXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyBetweenLongLiterals() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsBetween(
                propertyName, longLowerBoundary, longUpperBoundary);
        assertXMLEqual(propertyIsBetweenXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyBetweenFloatLiterals() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsBetween(
                propertyName, floatLowerBoundary, floatUpperBoundary);
        assertXMLEqual(propertyIsBetweenXmlWithDecimal, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyBetweenDoubleLiterals() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsBetween(
                propertyName, doubleLowerBoundary, doubleUpperBoundary);
        assertXMLEqual(propertyIsBetweenXmlWithDecimal, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyNull() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsNull(propertyName);
        assertXMLEqual(propertyIsNullXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyLike() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLike(
                propertyName, likeLiteral,  isCaseSensitive);
        assertXMLEqual(propertyIsLikeXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testPropertyLikeAnyText() 
        throws JAXBException, SAXException, IOException {
        
        FilterType filterType = cswFilterDelegateLatLon.propertyIsLike(
                propertyNameAnyText, likeLiteral, isCaseSensitive);
        assertXMLEqual(propertyIsLikeXmlAnyText, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testComparisonOpsOr() 
        throws JAXBException, SAXException, IOException {
        
        FilterType propertyIsLikeFilter = cswFilterDelegateLatLon.propertyIsLike(
                propertyName, likeLiteral, isCaseSensitive);
        FilterType propertyIsEqualFilter = cswFilterDelegateLatLon.propertyIsEqualTo(
                propertyName, booleanLiteral);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(propertyIsEqualFilter);
        filters.add(propertyIsLikeFilter);
        FilterType filterType = cswFilterDelegateLatLon.or(filters);

        assertXMLEqual(orComparisonOpsXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testLogicOpsFiltersOr() 
        throws JAXBException, SAXException, IOException {
        
        FilterType propertyIsLikeFilter = cswFilterDelegateLatLon.propertyIsLike(
                propertyName, likeLiteral, isCaseSensitive);
        FilterType notFilter = cswFilterDelegateLatLon.not(propertyIsLikeFilter);
        FilterType propertyIsEqualFilter = cswFilterDelegateLatLon.propertyIsEqualTo(
                propertyName, booleanLiteral);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(notFilter);
        filters.add(propertyIsEqualFilter);
        FilterType filterType = cswFilterDelegateLatLon.or(filters);

        assertXMLEqual(orLogicOpsXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testSpatialOpsOr() 
        throws JAXBException, SAXException, IOException {
        
        FilterType spatialFilter = cswFilterDelegateLatLon.dwithin(
                GeospatialPropertyName.BOUNDING_BOX.toString(), POINT_WKT, 
                Double.valueOf(1000));
        FilterType propertyIsLikeFilter = cswFilterDelegateLatLon.propertyIsLike(
                propertyName, likeLiteral, isCaseSensitive);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(spatialFilter);
        filters.add(propertyIsLikeFilter);

        FilterType filterType = cswFilterDelegateLatLon.or(filters);
        assertXMLEqual(orSpatialOpsXml, getXmlFromMarshaller(filterType));
    }

    @Test

    public void testFeatureIdOr() 
        throws JAXBException, SAXException, IOException, XpathException {
        
        ObjectFactory filterObjectFactory = new ObjectFactory();
        FeatureIdType fidType = new FeatureIdType();
        fidType.setFid("cswRecord.1234");
        List<JAXBElement<? extends AbstractIdType>> fidFilters = 
                new ArrayList<JAXBElement<? extends AbstractIdType>>();
        fidFilters.add(filterObjectFactory.createFeatureId(fidType));
        FilterType idFilter = new FilterType();
        idFilter.setId(fidFilters);

        FeatureIdType fidType2 = new FeatureIdType();
        fidType2.setFid("cswRecord.5678");
        List<JAXBElement<? extends AbstractIdType>> fidFilters2 = 
                new ArrayList<JAXBElement<? extends AbstractIdType>>();
        fidFilters2.add(filterObjectFactory.createFeatureId(fidType2));
        FilterType idFilter2 = new FilterType();
        idFilter2.setId(fidFilters2);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(idFilter);
        filters.add(idFilter2);

        FilterType filterType = cswFilterDelegateLatLon.or(filters);

        String xml = getXmlFromMarshaller(filterType);
        assertXpathExists("/ogc:Filter/ogc:FeatureId[@fid='cswRecord.1234']", xml);
        assertXpathExists("/ogc:Filter/ogc:FeatureId[@fid='cswRecord.5678']", xml);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFeatureIdAndComparisonOpsOr() 
        throws JAXBException, SAXException, IOException {

        ObjectFactory filterObjectFactory = new ObjectFactory();
        FeatureIdType fidType = new FeatureIdType();
        fidType.setFid("cswRecord.1234");
        List<JAXBElement<? extends AbstractIdType>> fidFilters = new ArrayList<JAXBElement<? extends AbstractIdType>>();
        fidFilters.add(filterObjectFactory.createFeatureId(fidType));

        FilterType idFilter = new FilterType();
        idFilter.setId(fidFilters);

        FilterType propertyIsLikeFilter = cswFilterDelegateLatLon.propertyIsLike(
                propertyName, likeLiteral, isCaseSensitive);

        List<FilterType> filterList = new ArrayList<FilterType>();
        filterList.add(idFilter);
        filterList.add(propertyIsLikeFilter);

        cswFilterDelegateLatLon.or(filterList);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyLikeOrEmptyList() {
        cswFilterDelegateLatLon.or(new ArrayList<FilterType>());
    }

    @Test
    public void testComparisonOpsAnd() 
        throws JAXBException, SAXException, IOException {
        FilterType propertyIsLikeFilter = cswFilterDelegateLatLon.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);
        FilterType propertyIsEqualFilter = cswFilterDelegateLatLon.propertyIsEqualTo(propertyName,
                booleanLiteral);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(propertyIsEqualFilter);
        filters.add(propertyIsLikeFilter);
        FilterType filterType = cswFilterDelegateLatLon.and(filters);
        
        assertXMLEqual(andComparisonOpsXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testLogicOpsFiltersAnd() 
        throws JAXBException, SAXException, IOException {
        FilterType propertyIsLikeFilter = cswFilterDelegateLatLon.propertyIsLike(
                propertyName, likeLiteral, isCaseSensitive);
        FilterType notFilter = cswFilterDelegateLatLon.not(propertyIsLikeFilter);
        FilterType propertyIsEqualFilter = cswFilterDelegateLatLon.propertyIsEqualTo(
                propertyName, booleanLiteral);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(notFilter);
        filters.add(propertyIsEqualFilter);
        FilterType filterType = cswFilterDelegateLatLon.and(filters);

        assertXMLEqual(andLogicOpsXml, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testSpatialOpsAnd() 
        throws JAXBException, SAXException, IOException {
        FilterType spatialFilter = cswFilterDelegateLatLon.dwithin(
                GeospatialPropertyName.BOUNDING_BOX.toString(), POINT_WKT, 
                Double.valueOf(1000));
        FilterType propertyIsLikeFilter = cswFilterDelegateLatLon.propertyIsLike(
                propertyName, likeLiteral, isCaseSensitive);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(spatialFilter);
        filters.add(propertyIsLikeFilter);

        FilterType filterType = cswFilterDelegateLatLon.and(filters);
        
        assertXMLEqual(andSpatialOpsXml, getXmlFromMarshaller(filterType));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsLikeAndEmptyList() {
        cswFilterDelegateLatLon.and(new ArrayList<FilterType>());
    }

    @Test
    public void testAndEmptyFilter() {
        FilterType propertyIsLikeFilter = cswFilterDelegateLatLon.propertyIsLike(
                propertyName, likeLiteral, isCaseSensitive);
        FilterType emptyFilter = new FilterType();

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(emptyFilter);
        filters.add(propertyIsLikeFilter);

        FilterType filter = cswFilterDelegateLatLon.and(filters);
        assertNotNull(filter.getComparisonOps());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAndInvalidFilter() {
        FilterType propertyIsLikeFilter = cswFilterDelegateLatLon.propertyIsLike(
                propertyName, likeLiteral, isCaseSensitive);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(null);
        filters.add(propertyIsLikeFilter);

        cswFilterDelegateLatLon.and(filters);
    }

    @Test
    public void testOrEmptyFilter() {
        FilterType propertyIsLikeFilter = cswFilterDelegateLatLon.propertyIsLike(
                propertyName, likeLiteral, isCaseSensitive);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(new FilterType());
        filters.add(propertyIsLikeFilter);

        FilterType filter = cswFilterDelegateLatLon.or(filters);

        assertNotNull(filter.getComparisonOps());
        assertNull(filter.getLogicOps());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testOrInvalidFilter() {
        FilterType propertyIsLikeFilter = cswFilterDelegateLatLon.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(null);
        filters.add(propertyIsLikeFilter);

        cswFilterDelegateLatLon.or(filters);
    }

    @Test
    public void testPropertyIsLikeNot() 
        throws JAXBException, SAXException, IOException {
        
        FilterType propertyIsLikeFilter = cswFilterDelegateLatLon.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);

        FilterType filter = cswFilterDelegateLatLon.not(propertyIsLikeFilter);

        assertNotNull(filter);
        assertEquals(filter.getLogicOps().getName(), NOT_LOGIC_OPS_NAME);
        UnaryLogicOpType ulot = (UnaryLogicOpType) filter.getLogicOps().getValue();
        assertNotNull(ulot);
    }
    
    @Test
    public void testIntersectsPropertyAnyGeo() 
        throws JAXBException, SAXException, IOException {
        
        String xml = getXmlProperty(cswFilterDelegateLatLon, Metacard.ANY_GEO, INTERSECTS, polygonWkt);
        assertXMLEqual(intersectsPolygonXmlPropertyOwsBoundingBox, xml);
    }
    
    @Test
    public void testIntersectsPropertyAnyGeoPosList() 
        throws JAXBException, SAXException, IOException  {
        
        String xml = getXmlProperty(cswFilterDelegateLatLonPosList, Metacard.ANY_GEO, INTERSECTS, polygonWkt);
        assertXMLEqual(intersectsPolygonXmlPropertyOwsBoundingBoxPosList, xml);
    }
    
    @Test
    public void testIntersectsPropertyDctSpatial() 
        throws JAXBException, SAXException, IOException {
        
        String xml = getXmlProperty(cswFilterDelegateLatLon, CswConstants.SPATIAL_PROP, INTERSECTS, polygonWkt);
        assertXMLEqual(intersectsPolygonXmlPropertyDctSpatial, xml);
    }

    @Test
    public void testIntersectsPropertyDctSpatialPosList() 
        throws JAXBException, SAXException, IOException {
        
        String xml = getXmlProperty(cswFilterDelegateLatLonPosList, CswConstants.SPATIAL_PROP, INTERSECTS, polygonWkt);
        assertXMLEqual(intersectsPolygonXmlPropertyDctSpatialPosList, xml);
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxPolygon() 
        throws JAXBException, SAXException, IOException {
        
        String xml = getXmlProperty(cswFilterDelegateLatLon, CswConstants.BBOX_PROP, INTERSECTS, polygonWkt);
        assertXMLEqual(intersectsPolygonXmlPropertyOwsBoundingBox, xml);
    }
    
    @Test
    public void testIntersectsPropertyOwsBoundingBoxPolygonPosList() 
        throws JAXBException, SAXException, IOException {
        
        String xml = getXmlProperty(cswFilterDelegateLatLonPosList, CswConstants.BBOX_PROP, INTERSECTS, polygonWkt);
        assertXMLEqual(intersectsPolygonXmlPropertyOwsBoundingBoxPosList, xml);
    }

    @Test
    public void testIntersectsFallbackToBBoxPropertyOwsBoundingBox() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(
                getMockFilterCapabilitiesForSpatialFallback(
                        Arrays.asList(BBOX), Arrays.asList("Envelope")),
                initCswSourceConfiguration(false, false, CswRecordMetacardType.CSW_TYPE));
        
        FilterType filterType = localCswFilterDelegate.intersects(propName, polygonWkt);
        Diff diff = XMLUnit.compareXML(bboxXmlPropertyOwsBoundingBox, 
                getXmlFromMarshaller(filterType));
        assertTrue("XML Similar", diff.similar());
    }

    @Test
    public void testIntersectsFallbackToNotDisjointPropertyOwsBoundingBox() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswSourceConfiguration cswSourceConfiguration = new CswSourceConfiguration();
        cswSourceConfiguration.setContentTypeMapping(CswRecordMetacardType.CSW_TYPE);
        
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(
                getMockFilterCapabilitiesForSpatialFallback(
                        Arrays.asList(DISJOINT), Arrays.asList("Polygon")),
                initCswSourceConfiguration(false, false, CswRecordMetacardType.CSW_TYPE));
        
        FilterType filterType = localCswFilterDelegate.intersects(propName, polygonWkt);
        assertXMLEqual(notDisjointPolygonXmlPropertyOwsBoundingBox, getXmlFromMarshaller(filterType));
    }

    /**
     * In the following case, when DWithin falls back to Intersects, the pointWkt gets turned into a
     * linear ring ("circular" polygon) with radius "SAMPLE_DISTANCE" (the buffer). In this case, the CSW
     * endpoint only supports "Envelope" (its spatial capabilities), so we fall back from "Geometry"
     * to "Envelope" (the next best choice) and create an envelope around the linear ring. So, the
     * resulting filter should contain an envelope that bounds the linear ring.
     */
    @Test
    public void testDWitinFallbackToIntersectsEnvelopeIntersectsCswGeometryPropertyOwsBoundingBox()
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(
                getMockFilterCapabilitiesForSpatialFallback(
                        Arrays.asList(INTERSECTS),
                        Arrays.asList("Envelope")), initCswSourceConfiguration(
                                false, false, CswRecordMetacardType.CSW_TYPE));
        
        FilterType filterType = localCswFilterDelegate.dwithin(propName, pointWkt, SAMPLE_DISTANCE);
        assertXMLEqual(dWithinFallbackToIntersects1, getXmlFromMarshaller(filterType));
    }

    /**
     * In the following case, when DWithin falls back to Intersects, the pointWkt gets turned into a
     * linear ring ("circular" polygon) with radius "SAMPLE_DISTANCE" (the buffer). In this case, the CSW
     * endpoint supports "Polygon" (its spatial capabilities), so the resulting filter should
     * contain the linear ring.
     */
    @Test
    public void testDWitinFallbackToIntersectsPolygonIntersectsCswGeometryPropertyOwsBoundingBox()
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(
                getMockFilterCapabilitiesForSpatialFallback(
                        Arrays.asList(INTERSECTS), Arrays.asList("Polygon")),
                initCswSourceConfiguration(false, false, CswRecordMetacardType.CSW_TYPE));
        
        FilterType filterType = localCswFilterDelegate.dwithin(propName, pointWkt, SAMPLE_DISTANCE);
        assertXMLEqual(dWithinFallbackToIntersects2, getXmlFromMarshaller(filterType));
    }

    @Test
    public void testIntersectsUsingPolygonAndEnvelopePropertyOwsBoundingBox() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(
                getMockFilterCapabilitiesForSpatialFallback(
                        Arrays.asList(INTERSECTS),
                        Arrays.asList("Envelope")), initCswSourceConfiguration(
                                false, false, CswRecordMetacardType.CSW_TYPE));
        
        FilterType filterType = localCswFilterDelegate.intersects(propName, polygonWkt);
        assertXMLEqual(intersectsEnvelopeXmlPropertyOwsBoundingBox, getXmlFromMarshaller(filterType));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIntersectsUnsupportedOperation() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(null, 
                initCswSourceConfiguration(false, false, CswRecordMetacardType.CSW_TYPE));
        localCswFilterDelegate.intersects(propName, polygonWkt);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBeyondUnsupportedOperation() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(null, 
                initCswSourceConfiguration(false, false, CswRecordMetacardType.CSW_TYPE));
        localCswFilterDelegate.beyond(propName, polygonWkt, SAMPLE_DISTANCE);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDWithinUnsupportedOperation() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(null, 
                initCswSourceConfiguration(false, false, CswRecordMetacardType.CSW_TYPE));
        localCswFilterDelegate.dwithin(propName, polygonWkt, SAMPLE_DISTANCE);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testContainsUnsupportedOperation() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(null, 
                initCswSourceConfiguration(false, false, CswRecordMetacardType.CSW_TYPE));
        localCswFilterDelegate.contains(propName, polygonWkt);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCrossesUnsupportedOperation() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(null, 
                initCswSourceConfiguration(false, false, CswRecordMetacardType.CSW_TYPE));
        localCswFilterDelegate.crosses(propName, polygonWkt);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDisjointUnsupportedOperation() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(null, 
                initCswSourceConfiguration(false, false, CswRecordMetacardType.CSW_TYPE));
        localCswFilterDelegate.disjoint(propName, polygonWkt);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testOverlapsUnsupportedOperation() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(null, 
                initCswSourceConfiguration(false, false, CswRecordMetacardType.CSW_TYPE));
        localCswFilterDelegate.overlaps(propName, polygonWkt);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testTouchesUnsupportedOperation() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(null, 
                initCswSourceConfiguration(false, false, CswRecordMetacardType.CSW_TYPE));
        localCswFilterDelegate.touches(propName, polygonWkt);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testWithinUnsupportedOperation() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(null, 
                initCswSourceConfiguration(false, false, CswRecordMetacardType.CSW_TYPE));
        localCswFilterDelegate.within(propName, polygonWkt);
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxPoint() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegateLatLon.intersects(propName, pointWkt);
        assertXMLEqual(intersectsPointXmlPropertyOwsBoundingBox, 
                getXmlFromMarshaller(filterType));
    }

    /**
     * If the CswSource is in its default configuration for coord order (LAT/LON), verify that the
     * coords in the outgoing filter are converted to LAT/LON. Remember, incoming WKT is in LON/LAT.
     */
    @Test
    public void testIntersectsPolygonLonLatIsConvertedToLatLon() 
        throws JAXBException, SAXException, IOException {
        
        String xml = getXmlProperty(cswFilterDelegateLatLon, CswConstants.BBOX_PROP, INTERSECTS, polygonWkt);
        assertXMLEqual(intersectsPolygonXmlPropertyOwsBoundingBox, xml);
    }

    /**
     * If the CswSource is configured for LON/LAT, verify that the coords in the outgoing filter are
     * kept as-is. Remember, incoming WKT is in LON/LAT.
     */
    @Test
    public void testIntersectsPolygonLonLatIsKeptAsLonLat() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate localCswFilterDelegate = this.createCswFilterDelegate(
                initCswSourceConfiguration(true, false, CswRecordMetacardType.CSW_TYPE));
        
        FilterType filterType = localCswFilterDelegate.intersects(propName, polygonWkt);
       
        assertXMLEqual(intersectsPolygonXmlPropertyOwsBoundingBoxLonLatIsKeptAsLonLat, 
                getXmlFromMarshaller(filterType));
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxLineString() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegateLatLon.intersects(propName, lineStringWkt);
        assertXMLEqual(intersectsLineStringXmlPropertyOwsBoundingBox, 
                getXmlFromMarshaller(filterType));
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxMultiPolygon() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegateLatLon.intersects(propName, 
                multiPolygonWkt);
        assertXMLEqual(intersectsMultiPolygonXmlPropertyOwsBoundingBox, 
                getXmlFromMarshaller(filterType));
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxMultiPoint() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegateLatLon.intersects(propName, multiPointWkt);
        assertXMLEqual(intersectsMultiPointXmlPropertyOwsBoundingBox, 
                getXmlFromMarshaller(filterType));
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxMultiLineString() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegateLatLon.intersects(propName, 
                multiLineStringWkt);
        assertXMLEqual(intersectsMultiLineStringXmlPropertyOwsBoundingBox, 
                getXmlFromMarshaller(filterType));
    }

    @Test
    public void testCrossesPropertyOwsBoundingBoxPolygon() 
        throws JAXBException, SAXException, IOException {
        
        assertXMLEqual(crossesPolygonXmlPropertyOwsBoundingBox, 
                getXmlProperty(cswFilterDelegateLatLon, CswConstants.BBOX_PROP, CROSSES, polygonWkt));
    }
    
    @Test
    public void testCrossesPropertyOwsBoundingBoxPolygonPosList() 
        throws JAXBException, SAXException, IOException {
        
        assertXMLEqual(crossesPolygonXmlPropertyOwsBoundingBoxPosList, 
                getXmlProperty(cswFilterDelegateLatLonPosList, CswConstants.BBOX_PROP, CROSSES, polygonWkt));
    }

    @Test
    public void testWithinPropertyOwsBoundingBoxPolygon() 
        throws JAXBException, SAXException, IOException {
        
        assertXMLEqual(withinPolygonXmlPropertyOwsBoundingBox, 
                getXmlProperty(cswFilterDelegateLatLon, CswConstants.BBOX_PROP, WITHIN, polygonWkt));
    }
    
    @Test
    public void testWithinPropertyOwsBoundingBoxPolygonPosList() 
        throws JAXBException, SAXException, IOException {
        
        assertXMLEqual(withinPolygonXmlPropertyOwsBoundingBoxPosList, 
                getXmlProperty(cswFilterDelegateLatLonPosList, CswConstants.BBOX_PROP, WITHIN, polygonWkt));
    }

    @Test
    public void testContainsPropertyOwsBoundingBoxPolygon() 
        throws JAXBException, SAXException, IOException {
        
        assertXMLEqual(containsPolygonXmlPropertyOwsBoundingBox, 
                getXmlProperty(cswFilterDelegateLatLon, CswConstants.BBOX_PROP, CONTAINS, polygonWkt));
    }
    
    @Test
    public void testContainsPropertyOwsBoundingBoxPolygonPosList() 
        throws JAXBException, SAXException, IOException {
        
        assertXMLEqual(containsPolygonXmlPropertyOwsBoundingBoxPosList, 
                getXmlProperty(cswFilterDelegateLatLonPosList, CswConstants.BBOX_PROP, CONTAINS, polygonWkt));
    }

    @Test
    public void testWithinFallbackToContainsPropertyOwsBoundingBox() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate localCswFilterDelegate = initCswFilterDelegate(
                getMockFilterCapabilitiesForSpatialFallback(
                        Arrays.asList(CONTAINS), Arrays.asList("Polygon")),
                        initCswSourceConfiguration(false, false, CswRecordMetacardType.CSW_TYPE));
        
        FilterType filterType = localCswFilterDelegate.within(propName, polygonWkt);
        assertXMLEqual(containsPolygonXmlPropertyOwsBoundingBox, 
                getXmlFromMarshaller(filterType));
    }

    @Test
    public void testBeyondPropertyOwsBoundingBoxPoint() 
        throws JAXBException, SAXException, IOException {
        
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegateLatLon.beyond(propName, pointWkt, SAMPLE_DISTANCE);
        assertXMLEqual(beyondPointXmlPropertyOwsBoundingBox, 
                getXmlFromMarshaller(filterType));
    }

    @Test
    public void testDWithinPropertyOwsBoundingBoxPolygon() 
        throws JAXBException, SAXException, IOException {
        
        assertXMLEqual(dwithinPolygonXmlPropertyOwsBoundingBox, 
                getXmlProperty(cswFilterDelegateLatLon, CswConstants.BBOX_PROP, 
                        D_WITHIN, polygonWkt));
    }
    
    @Test
    public void testDWithinPropertyOwsBoundingBoxPolygonPosList() 
        throws JAXBException, SAXException, IOException {
        
        assertXMLEqual(dwithinPolygonXmlPropertyOwsBoundingBoxPosList, 
                getXmlProperty(cswFilterDelegateLatLonPosList, 
                        CswConstants.BBOX_PROP, D_WITHIN, polygonWkt));
    }

    @Test
    public void testTouchesPropertyOwsBoundingBoxPolygon() 
        throws JAXBException, SAXException, IOException {
        
        assertXMLEqual(touchesPolygonXmlPropertyOwsBoundingBox, 
                getXmlProperty(cswFilterDelegateLatLon, CswConstants.BBOX_PROP, TOUCHES, polygonWkt));
    }
    
    public void testTouchesPropertyOwsBoundingBoxPolygonPosList() 
        throws JAXBException, SAXException, IOException {
        
        assertXMLEqual(touchesPolygonXmlPropertyOwsBoundingBoxPosList, 
                getXmlProperty(cswFilterDelegateLatLonPosList, CswConstants.BBOX_PROP, TOUCHES, polygonWkt));
    }

    @Test
    public void testOverlapsPropertyOwsBoundingBoxPolygon() 
        throws JAXBException, SAXException, IOException {
        
        assertXMLEqual(overlapsPolygonXmlPropertyOwsBoundingBox, 
                getXmlProperty(cswFilterDelegateLatLon, CswConstants.BBOX_PROP, OVERLAPS, polygonWkt));
    }

    @Test
    public void testOverlapsPropertyOwsBoundingBoxPolygonPosList() 
        throws JAXBException, SAXException, IOException {
        
        assertXMLEqual(overlapsPolygonXmlPropertyOwsBoundingBoxPosList, 
                getXmlProperty(cswFilterDelegateLatLonPosList, CswConstants.BBOX_PROP, OVERLAPS, polygonWkt));
    }
    
    @Test
    public void testDisjointPropertyOwsBoundingBoxPolygon() 
        throws JAXBException, SAXException, IOException {
        
        assertXMLEqual(disjointPolygonXmlPropertyOwsBoundingBox, 
                getXmlProperty(cswFilterDelegateLatLon, CswConstants.BBOX_PROP, DISJOINT, polygonWkt));
    }
    
    @Test
    public void testDisjointPropertyOwsBoundingBoxPolygonPosList() 
        throws JAXBException, SAXException, IOException {
        
        assertXMLEqual(disjointPolygonXmlPropertyOwsBoundingBoxPosList, 
                getXmlProperty(cswFilterDelegateLatLonPosList, CswConstants.BBOX_PROP, DISJOINT, polygonWkt));
    }

    private String getXmlProperty(CswFilterDelegate localCswFilterDelegate, 
            String propName, ComparisonOperatorType comparisonOp, 
            Date beginDate, Date endDate) throws JAXBException {
        
        return getXmlProperty(localCswFilterDelegate, propName, comparisonOp, 
                beginDate, endDate, null);
    }

    private String getXmlProperty(CswFilterDelegate localCswFilterDelegate, 
            String propName, ComparisonOperatorType comparisonOp, 
            Date beginDate, Date endDate, Map<String, Object> propMap) throws JAXBException {
        
        String extendedComparisonOp = null;    
        if (null != propMap) {
            extendedComparisonOp = (String) propMap.get("extendedComparisonOp");
        }
        
        FilterType filterType = null;
        
        switch (comparisonOp) {
        case BETWEEN:
            if (!StringUtils.isBlank(extendedComparisonOp) && 
                extendedComparisonOp.equals("relative")) {
                
                Object duration = propMap.get("duration");
                if (duration instanceof Long) {
                    filterType = localCswFilterDelegate.relative(propName, ((Long) duration).longValue());
                }
            } else {
                filterType = localCswFilterDelegate.during(propName, beginDate, endDate);
            }
            break;
        default:
            break;
        }
        
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        
        LOGGER.debug(xml);
        
        return xml;
    }

    private String getXmlFromMarshaller(FilterType filterType) throws JAXBException {
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        LOGGER.debug("XML returned by Marshaller:\n{}", xml);
        LOGGER.trace(Thread.currentThread().getStackTrace().toString());
        return xml;
    }

    private String getXmlProperty(CswFilterDelegate localCswFilterDelegate,
            String propName, SpatialOperatorNameType spatialOp, String wkt) 
        throws JAXBException {
        
        FilterType filterType = null;
        
        switch (spatialOp) {
        case CONTAINS:
            filterType = localCswFilterDelegate.contains(propName, wkt);
            break;
        case CROSSES:
            filterType = localCswFilterDelegate.crosses(propName, wkt);
            break;
        case D_WITHIN:
            filterType = localCswFilterDelegate.dwithin(propName, polygonWkt, SAMPLE_DISTANCE);
            break;
        case DISJOINT:
            filterType = localCswFilterDelegate.disjoint(propName, wkt);
            break;
        case INTERSECTS: 
            filterType = localCswFilterDelegate.intersects(propName, wkt);
            break;
        case OVERLAPS:
            filterType = localCswFilterDelegate.overlaps(propName, wkt);
            break;
        case TOUCHES:
            filterType = localCswFilterDelegate.touches(propName, polygonWkt);
            break;
        case WITHIN:
            filterType = localCswFilterDelegate.within(propName, wkt);
            break;
        
        default:
            break;
        }
        
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        
        String xml = writer.toString();
        
        LOGGER.debug("\nXml: {}", xml);
        return xml;
    }

    private static FilterCapabilities getMockFilterCapabilities() {
        FilterCapabilities mockFilterCapabilities = mock(FilterCapabilities.class);

        ComparisonOperatorsType mockComparisonOps = mock(ComparisonOperatorsType.class);
        when(mockComparisonOps.getComparisonOperator()).thenReturn(getFullComparisonOpsList());

        SpatialOperatorsType mockSpatialOperatorsType = mock(SpatialOperatorsType.class);
        when(mockSpatialOperatorsType.getSpatialOperator()).thenReturn(getSpatialOperatorsList());
        SpatialCapabilitiesType mockSpatialCapabilities = getAllSpatialCapabilities();
        when(mockSpatialCapabilities.getSpatialOperators()).thenReturn(mockSpatialOperatorsType);

        ScalarCapabilitiesType mockScalarCapabilities = mock(ScalarCapabilitiesType.class);
        when(mockScalarCapabilities.getComparisonOperators()).thenReturn(mockComparisonOps);
        when(mockFilterCapabilities.getScalarCapabilities()).thenReturn(mockScalarCapabilities);
        when(mockFilterCapabilities.getSpatialCapabilities()).thenReturn(mockSpatialCapabilities);
        when(mockScalarCapabilities.getLogicalOperators()).thenReturn(mock(LogicalOperators.class));

        return mockFilterCapabilities;
    }

    private static List<SpatialOperatorType> getSpatialOperatorsList() {
        List<SpatialOperatorType> spatialOperatorList = new ArrayList<SpatialOperatorType>();
        SpatialOperatorType intersectsSpatialOperator = new SpatialOperatorType();
        intersectsSpatialOperator.setName(INTERSECTS);
        spatialOperatorList.add(intersectsSpatialOperator);
        SpatialOperatorType bboxSpatialOperator = new SpatialOperatorType();
        bboxSpatialOperator.setName(BBOX);
        spatialOperatorList.add(bboxSpatialOperator);
        SpatialOperatorType crossesSpatialOperator = new SpatialOperatorType();
        crossesSpatialOperator.setName(CROSSES);
        spatialOperatorList.add(crossesSpatialOperator);
        SpatialOperatorType withinSpatialOperator = new SpatialOperatorType();
        withinSpatialOperator.setName(WITHIN);
        spatialOperatorList.add(withinSpatialOperator);
        SpatialOperatorType containsSpatialOperator = new SpatialOperatorType();
        containsSpatialOperator.setName(CONTAINS);
        spatialOperatorList.add(containsSpatialOperator);
        SpatialOperatorType beyondSpatialOperator = new SpatialOperatorType();
        beyondSpatialOperator.setName(BEYOND);
        spatialOperatorList.add(beyondSpatialOperator);
        SpatialOperatorType dwithinSpatialOperator = new SpatialOperatorType();
        dwithinSpatialOperator.setName(D_WITHIN);
        spatialOperatorList.add(dwithinSpatialOperator);
        SpatialOperatorType disjointSpatialOperator = new SpatialOperatorType();
        disjointSpatialOperator.setName(DISJOINT);
        spatialOperatorList.add(disjointSpatialOperator);
        SpatialOperatorType overlapsSpatialOperator = new SpatialOperatorType();
        overlapsSpatialOperator.setName(OVERLAPS);
        spatialOperatorList.add(overlapsSpatialOperator);
        SpatialOperatorType touchesSpatialOperator = new SpatialOperatorType();
        touchesSpatialOperator.setName(TOUCHES);
        spatialOperatorList.add(touchesSpatialOperator);

        return spatialOperatorList;
    }

    private static List<ComparisonOperatorType> getFullComparisonOpsList() {
        List<ComparisonOperatorType> comparisonOpsList = new ArrayList<ComparisonOperatorType>();
        comparisonOpsList.add(ComparisonOperatorType.EQUAL_TO);
        comparisonOpsList.add(ComparisonOperatorType.LIKE);
        comparisonOpsList.add(ComparisonOperatorType.NOT_EQUAL_TO);
        comparisonOpsList.add(ComparisonOperatorType.GREATER_THAN);
        comparisonOpsList.add(ComparisonOperatorType.GREATER_THAN_EQUAL_TO);
        comparisonOpsList.add(ComparisonOperatorType.LESS_THAN);
        comparisonOpsList.add(ComparisonOperatorType.LESS_THAN_EQUAL_TO);
        comparisonOpsList.add(ComparisonOperatorType.BETWEEN);
        comparisonOpsList.add(ComparisonOperatorType.NULL_CHECK);

        return comparisonOpsList;
    }

    private FilterCapabilities getMockFilterCapabilitiesForSpatialFallback(
            List<SpatialOperatorNameType> spatialOperatorNameTypes, List<String> geometries) {
        FilterCapabilities mockFilterCapabilities = mock(FilterCapabilities.class);

        ComparisonOperatorsType mockComparisonOps = mock(ComparisonOperatorsType.class);
        when(mockComparisonOps.getComparisonOperator()).thenReturn(getFullComparisonOpsList());

        List<SpatialOperatorType> spatialOperatorList = new ArrayList<SpatialOperatorType>();

        for (SpatialOperatorNameType spatialOperatorNameType : spatialOperatorNameTypes) {
            SpatialOperatorType spatialOperatorType = new SpatialOperatorType();
            spatialOperatorType.setName(spatialOperatorNameType);
            spatialOperatorList.add(spatialOperatorType);
        }

        SpatialOperatorsType mockSpatialOperatorsType = mock(SpatialOperatorsType.class);
        when(mockSpatialOperatorsType.getSpatialOperator()).thenReturn(spatialOperatorList);
        SpatialCapabilitiesType mockSpatialCapabilities = getSpatialCapabilities(geometries);
        when(mockSpatialCapabilities.getSpatialOperators()).thenReturn(mockSpatialOperatorsType);

        ScalarCapabilitiesType mockScalarCapabilities = mock(ScalarCapabilitiesType.class);
        when(mockScalarCapabilities.getComparisonOperators()).thenReturn(mockComparisonOps);
        when(mockFilterCapabilities.getScalarCapabilities()).thenReturn(mockScalarCapabilities);
        when(mockFilterCapabilities.getSpatialCapabilities()).thenReturn(mockSpatialCapabilities);
        when(mockScalarCapabilities.getLogicalOperators()).thenReturn(mock(LogicalOperators.class));

        return mockFilterCapabilities;
    }

    private static SpatialCapabilitiesType getSpatialCapabilities(List<String> geometries) {
        List<QName> mockGeometryOperands = new ArrayList<QName>();
        String nameSpaceUri = "http://www.opengis.net/gml";
        String prefix = "gml";

        for (String geometry : geometries) {
            QName polygonQName = new QName(nameSpaceUri, geometry, prefix);
            mockGeometryOperands.add(polygonQName);
        }

        GeometryOperandsType mockGeometryOperandsType = mock(GeometryOperandsType.class);
        when(mockGeometryOperandsType.getGeometryOperand()).thenReturn(mockGeometryOperands);
        SpatialCapabilitiesType mockSpatialCapabilitiesType = mock(SpatialCapabilitiesType.class);
        when(mockSpatialCapabilitiesType.getGeometryOperands())
                .thenReturn(mockGeometryOperandsType);

        return mockSpatialCapabilitiesType;
    }

    private static SpatialCapabilitiesType getAllSpatialCapabilities() {
        List<QName> mockGeometryOperands = new ArrayList<QName>();
        String nameSpaceUri = "http://www.opengis.net/gml";
        String prefix = "gml";

        QName polygonQName = new QName(nameSpaceUri, "Polygon", prefix);
        mockGeometryOperands.add(polygonQName);
        QName pointQName = new QName(nameSpaceUri, "Point", prefix);
        mockGeometryOperands.add(pointQName);
        QName lineStringQName = new QName(nameSpaceUri, "LineString", prefix);
        mockGeometryOperands.add(lineStringQName);
        QName multiPolygonQName = new QName(nameSpaceUri, "MultiPolygon", prefix);
        mockGeometryOperands.add(multiPolygonQName);
        QName multiPointQName = new QName(nameSpaceUri, "MultiPoint", prefix);
        mockGeometryOperands.add(multiPointQName);
        QName multiLineStringQName = new QName(nameSpaceUri, "MultiLineString", prefix);
        mockGeometryOperands.add(multiLineStringQName);
        QName envelopeQName = new QName(nameSpaceUri, "Envelope", prefix);
        mockGeometryOperands.add(envelopeQName);

        GeometryOperandsType mockGeometryOperandsType = mock(GeometryOperandsType.class);
        when(mockGeometryOperandsType.getGeometryOperand()).thenReturn(mockGeometryOperands);
        SpatialCapabilitiesType mockSpatialCapabilitiesType = mock(SpatialCapabilitiesType.class);
        when(mockSpatialCapabilitiesType.getGeometryOperands())
                .thenReturn(mockGeometryOperandsType);

        return mockSpatialCapabilitiesType;
    }

    private static Operation getOperation() {
        List<DomainType> getRecordsParameters = new ArrayList<DomainType>(6);
        DomainType typeName = new DomainType();
        typeName.setName(CswConstants.TYPE_NAME_PARAMETER);
        getRecordsParameters.add(typeName);
        DomainType outputSchema = new DomainType();
        outputSchema.setName(CswConstants.OUTPUT_SCHEMA_PARAMETER);
        getRecordsParameters.add(outputSchema);
        DomainType constraintLang = new DomainType();
        constraintLang.setName(CswConstants.CONSTRAINT_LANGUAGE_PARAMETER);
        getRecordsParameters.add(constraintLang);
        DomainType outputFormat = new DomainType();
        outputFormat.setName(CswConstants.OUTPUT_FORMAT_PARAMETER);
        getRecordsParameters.add(outputFormat);
        DomainType resultType = new DomainType();
        resultType.setName(CswConstants.RESULT_TYPE_PARAMETER);
        getRecordsParameters.add(resultType);
        DomainType elementSetName = new DomainType();
        elementSetName.setName(CswConstants.ELEMENT_SET_NAME_PARAMETER);
        getRecordsParameters.add(elementSetName);

        Operation getRecords = new Operation();
        getRecords.setName(CswConstants.GET_RECORDS);
        getRecords.setParameter(getRecordsParameters);
        List<Operation> operations = new ArrayList<Operation>(1);
        operations.add(getRecords);

        return getRecords;
    }

    private JAXBElement<FilterType> getFilterTypeJaxbElement(FilterType filterType) {
        JAXBElement<FilterType> filterTypeJaxbElement = new JAXBElement<FilterType>(new QName(
                "http://www.opengis.net/ogc", FILTER_QNAME_LOCAL_PART), FilterType.class,
                filterType);
        return filterTypeJaxbElement;
    }

    private DateTime convertDateToIso8601Format(Date inputDate) {
        DateTime outputDate = new DateTime(inputDate);
        return outputDate;
    }

    private static JAXBContext initJaxbContext() {
        JAXBContext localJaxbContext = null;

        // JAXB context path
        // "net.opengis.cat.csw.v_2_0_2:net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1:net.opengis.ows.v_1_0_0"
        String contextPath = StringUtils.join(new String[] {CswConstants.OGC_CSW_PACKAGE,
            CswConstants.OGC_FILTER_PACKAGE, CswConstants.OGC_GML_PACKAGE,
            CswConstants.OGC_OWS_PACKAGE}, ":");

        try {
            LOGGER.debug("Creating JAXB context with context path: {}", contextPath);
            localJaxbContext = JAXBContext.newInstance(contextPath,
                    CswJAXBElementProvider.class.getClassLoader());
        } catch (JAXBException e) {
            LOGGER.error("Unable to create JAXB context using contextPath: {}", contextPath, e);
        }

        return localJaxbContext;
    }

    private CswFilterDelegate createCswFilterDelegate(CswSourceConfiguration cswSourceConfiguration) {

        DomainType outputFormatValues = null;
        DomainType resultTypesValues = null;

        for (DomainType dt : getOperation().getParameter()) {
            if (dt.getName().equals(CswConstants.OUTPUT_FORMAT_PARAMETER)) {
                outputFormatValues = dt;
            } else if (dt.getName().equals(CswConstants.RESULT_TYPE_PARAMETER)) {
                resultTypesValues = dt;
            }
        }

        CswFilterDelegate cswFilterDelegate = new CswFilterDelegate(new CswRecordMetacardType(),
                getOperation(), getMockFilterCapabilities(), outputFormatValues, resultTypesValues,
                cswSourceConfiguration);
        return cswFilterDelegate;
    }

    private CswFilterDelegate initCswFilterDelegate(FilterCapabilities filterCapabilities,
        CswSourceConfiguration cswSourceConfiguration) {

        DomainType outputFormatValues = null;
        DomainType resultTypesValues = null;

        for (DomainType dt : getOperation().getParameter()) {
            if (dt.getName().equals(CswConstants.OUTPUT_FORMAT_PARAMETER)) {
                outputFormatValues = dt;
            } else if (dt.getName().equals(CswConstants.RESULT_TYPE_PARAMETER)) {
                resultTypesValues = dt;
            }
        }

        CswFilterDelegate cswFilterDelegate = new CswFilterDelegate(new CswRecordMetacardType(),
                getOperation(), filterCapabilities, outputFormatValues, resultTypesValues,
                cswSourceConfiguration);
        return cswFilterDelegate;
    }

    private CswSourceConfiguration initCswSourceConfiguration(boolean isLonLatOrder, 
            boolean usePosList, String contentType) {
        CswSourceConfiguration cswSourceConfiguration = new CswSourceConfiguration();
        cswSourceConfiguration.setIsLonLatOrder(isLonLatOrder);
        cswSourceConfiguration.setUsePosList(usePosList);
        cswSourceConfiguration.setContentTypeMapping(contentType);
        return cswSourceConfiguration;
    }

    private CswSourceConfiguration initCswSourceConfiguration(boolean isLonLatOrder, 
            boolean usePosList, String contentType, String effectiveDateMapping, 
            String createdDateMapping, String modifiedDateMapping) {
        CswSourceConfiguration cswSourceConfiguration = new CswSourceConfiguration();
        cswSourceConfiguration.setIsLonLatOrder(isLonLatOrder);
        cswSourceConfiguration.setUsePosList(usePosList);
        cswSourceConfiguration.setContentTypeMapping(contentType);
        cswSourceConfiguration.setEffectiveDateMapping(effectiveDateMapping);
        cswSourceConfiguration.setCreatedDateMapping(createdDateMapping);
        cswSourceConfiguration.setModifiedDateMapping(modifiedDateMapping);
        return cswSourceConfiguration;
    }

}
