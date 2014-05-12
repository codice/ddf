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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
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
import net.opengis.gml.v_3_1_1.CoordinatesType;
import net.opengis.gml.v_3_1_1.LinearRingType;
import net.opengis.gml.v_3_1_1.PolygonType;
import net.opengis.ows.v_1_0_0.DomainType;
import net.opengis.ows.v_1_0_0.Operation;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswJAXBElementProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.gml2.GMLWriter;

import ddf.catalog.data.Metacard;

public class TestCswFilterDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCswFilterDelegate.class);

    private static final JAXBContext jaxbContext = initJaxbContext();

    private final CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(initCswSourceConfiguration(false,
            CswRecordMetacardType.CSW_TYPE));

    private static final String FILTER_QNAME_LOCAL_PART = "Filter";

    private static final String DEFAULT_PROPERTY_NAME = "title";

    private final Date date = getDate();

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

    private final String nonNormalizedGeometryCollectionWkt = "GEOMETRYCOLLECTION(POINT(4\t\t\n   \n6),   LINESTRING(4    6,7\t10)\n,\t MULTIPOLYGON     (  \t(    (41\n\n\t-43.5, 2.0 .45, 45     30, 41\t-43.5)\n\t), ((\n\n\t   20.43545445 -35.3424354, -45.23232\t\t    20, 30 5, 1.4    0.1032, -10 -30\n\t, -0.6766677620 -3.55656566\t, 20.43545445 -35.3424354    ),\n\n\n ( -179.5435445 89.5443564656\t,\n-20.565 1.5, 30.0 20.0\n,\n-179.5435445\t\t89.5443564656\n\t)\n)\t) \t\t\t)";

    private final String convertedAndNormalizedGeometryCollectionWkt = "GEOMETRYCOLLECTION(POINT(6 4),LINESTRING(6 4,10 7),MULTIPOLYGON(((-43.5 41,.45 2.0,30 45,-43.5 41)),((-35.3424354 20.43545445,20 -45.23232,5 30,0.1032 1.4,-30 -10,-3.55656566 -0.6766677620,-35.3424354 20.43545445),(89.5443564656 -179.5435445,1.5 -20.565,20.0 30.0,89.5443564656 -179.5435445))))";

    private final double distance = 123.456;

    private static final QName OR_LOGIC_OPS_NAME = new QName("http://www.opengis.net/ogc", "Or");

    private static final QName AND_LOGIC_OPS_NAME = new QName("http://www.opengis.net/ogc", "And");

    private static final QName NOT_LOGIC_OPS_NAME = new QName("http://www.opengis.net/ogc", "Not");

    private static final String POINT_WKT = "POINT (30 10)";

    private static final String REPLACE_START_DATE = "REPLACE_START_DATE";

    private static final String REPLACE_END_DATE = "REPLACE_END_DATE";

    private static final String REPLACE_TEMPORAL_PROPERTY = "REPLACE_TEMPORAL_PROPERTY";

    private final String during = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://purl.org/dc/elements/1.1/\" xmlns:ns4=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns3=\"http://purl.org/dc/terms/\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.opengis.net/ogc\" xmlns:ns6=\"http://www.opengis.net/gml\" xmlns:ns7=\"http://www.w3.org/1999/xlink\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns5:PropertyIsBetween>"
            + "<ns5:PropertyName>"
            +  REPLACE_TEMPORAL_PROPERTY
            + "</ns5:PropertyName>"
            + "<ns5:LowerBoundary><ns5:Literal>" //2013-05-01T00:00:00.000-07:00
            + REPLACE_START_DATE
            + "</ns5:Literal></ns5:LowerBoundary>"
            + "<ns5:UpperBoundary><ns5:Literal>" //2013-12-31T00:00:00.000-07:00
            + REPLACE_END_DATE
            + "</ns5:Literal></ns5:UpperBoundary>"
            + "</ns5:PropertyIsBetween>"
            + "</ns5:Filter>";


    private final String propertyIsEqualToXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsEqualTo matchCase=\"false\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1</ns3:Literal>"
            + "</ns3:PropertyIsEqualTo>" + "</ns3:Filter>";

    private final String propertyIsEqualToXmlWithDecimal = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsEqualTo matchCase=\"false\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1.0</ns3:Literal>"
            + "</ns3:PropertyIsEqualTo>" + "</ns3:Filter>";

    private final String propertyIsEqualToXmlAnyText = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsEqualTo matchCase=\"false\">"
            + "<ns3:PropertyName>AnyText</ns3:PropertyName>"
            + "<ns3:Literal>1</ns3:Literal>"
            + "</ns3:PropertyIsEqualTo>" + "</ns3:Filter>";

    private final String propertyIsEqualToXmlContentType = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsEqualTo matchCase=\"false\">"
            + "<ns3:PropertyName>type</ns3:PropertyName>"
            + "<ns3:Literal>myType</ns3:Literal>"
            + "</ns3:PropertyIsEqualTo>" + "</ns3:Filter>";

    private final String propertyIsEqualToXmlWithDate = getPropertyIsEqualToXmlWithDate(getDate());

    private final String propertyIsEqualToXmlWithBoolean = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsEqualTo matchCase=\"false\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>true</ns3:Literal>"
            + "</ns3:PropertyIsEqualTo>" + "</ns3:Filter>";

    private final String propertyIsNotEqualToXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsNotEqualTo matchCase=\"false\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1</ns3:Literal>"
            + "</ns3:PropertyIsNotEqualTo>" + "</ns3:Filter>";

    private final String propertyIsNotEqualToXmlWithDecimal = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsNotEqualTo matchCase=\"false\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1.0</ns3:Literal>"
            + "</ns3:PropertyIsNotEqualTo>" + "</ns3:Filter>";

    private final String propertyIsNotEqualToXmlAnyText = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsNotEqualTo matchCase=\"false\">"
            + "<ns3:PropertyName>"
            + CswConstants.ANY_TEXT
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1</ns3:Literal>"
            + "</ns3:PropertyIsNotEqualTo>" + "</ns3:Filter>";

    private final String propertyIsNotEqualToXmlWithDate = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsNotEqualTo matchCase=\"false\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>2002-06-11T10:36:52.000-07:00</ns3:Literal>"
            + "</ns3:PropertyIsNotEqualTo>" + "</ns3:Filter>";

    private final String propertyIsNotEqualToXmlWithBoolean = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsNotEqualTo matchCase=\"false\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>true</ns3:Literal>"
            + "</ns3:PropertyIsNotEqualTo>" + "</ns3:Filter>";

    private final String propertyIsGreaterThanXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsGreaterThan>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1</ns3:Literal>"
            + "</ns3:PropertyIsGreaterThan>" + "</ns3:Filter>";

    private final String propertyIsGreaterThanXmlWithDecimal = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsGreaterThan>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1.0</ns3:Literal>"
            + "</ns3:PropertyIsGreaterThan>" + "</ns3:Filter>";

    private final String propertyIsGreaterThanXmlAnyText = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsGreaterThan>"
            + "<ns3:PropertyName>"
            + CswConstants.ANY_TEXT
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1</ns3:Literal>"
            + "</ns3:PropertyIsGreaterThan>" + "</ns3:Filter>";

    private final String propertyIsGreaterThanXmlWithDate = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsGreaterThan>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>2002-06-11T10:36:52.000-07:00</ns3:Literal>"
            + "</ns3:PropertyIsGreaterThan>" + "</ns3:Filter>";

    private final String propertyIsGreaterThanXmlWithBoolean = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsGreaterThan>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>true</ns3:Literal>"
            + "</ns3:PropertyIsGreaterThan>" + "</ns3:Filter>";

    private final String propertyIsGreaterThanOrEqualToXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsGreaterThanOrEqualTo>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1</ns3:Literal>"
            + "</ns3:PropertyIsGreaterThanOrEqualTo>" + "</ns3:Filter>";

    private final String propertyIsGreaterThanOrEqualToXmlWithDecimal = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsGreaterThanOrEqualTo>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1.0</ns3:Literal>"
            + "</ns3:PropertyIsGreaterThanOrEqualTo>" + "</ns3:Filter>";

    private final String propertyIsGreaterThanOrEqualToXmlAnyText = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsGreaterThanOrEqualTo>"
            + "<ns3:PropertyName>"
            + CswConstants.ANY_TEXT
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1</ns3:Literal>"
            + "</ns3:PropertyIsGreaterThanOrEqualTo>" + "</ns3:Filter>";

    private final String propertyIsGreaterThanOrEqualToXmlWithDate = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsGreaterThanOrEqualTo>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>2002-06-11T10:36:52.000-07:00</ns3:Literal>"
            + "</ns3:PropertyIsGreaterThanOrEqualTo>" + "</ns3:Filter>";

    private final String propertyIsGreaterThanOrEqualToXmlWithBoolean = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsGreaterThanOrEqualTo>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>true</ns3:Literal>"
            + "</ns3:PropertyIsGreaterThanOrEqualTo>" + "</ns3:Filter>";

    private final String propertyIsLessThanXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsLessThan>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1</ns3:Literal>"
            + "</ns3:PropertyIsLessThan>"
            + "</ns3:Filter>";

    private final String propertyIsLessThanXmlWithDecimal = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsLessThan>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1.0</ns3:Literal>"
            + "</ns3:PropertyIsLessThan>" + "</ns3:Filter>";

    private final String propertyIsLessThanXmlAnyText = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsLessThan>"
            + "<ns3:PropertyName>"
            + CswConstants.ANY_TEXT
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1</ns3:Literal>"
            + "</ns3:PropertyIsLessThan>"
            + "</ns3:Filter>";

    private final String propertyIsLessThanXmlWithDate = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsLessThan>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>2002-06-11T10:36:52.000-07:00</ns3:Literal>"
            + "</ns3:PropertyIsLessThan>" + "</ns3:Filter>";

    private final String propertyIsLessThanXmlWithBoolean = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsLessThan>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>true</ns3:Literal>"
            + "</ns3:PropertyIsLessThan>" + "</ns3:Filter>";

    private final String propertyIsLessThanOrEqualToXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsLessThanOrEqualTo>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1</ns3:Literal>"
            + "</ns3:PropertyIsLessThanOrEqualTo>" + "</ns3:Filter>";

    private final String propertyIsLessThanOrEqualToXmlWithDecimal = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsLessThanOrEqualTo>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1.0</ns3:Literal>"
            + "</ns3:PropertyIsLessThanOrEqualTo>" + "</ns3:Filter>";

    private final String propertyIsLessThanOrEqualToXmlAnyText = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsLessThanOrEqualTo>"
            + "<ns3:PropertyName>"
            + CswConstants.ANY_TEXT
            + "</ns3:PropertyName>"
            + "<ns3:Literal>1</ns3:Literal>"
            + "</ns3:PropertyIsLessThanOrEqualTo>" + "</ns3:Filter>";

    private final String propertyIsLessThanOrEqualToXmlWithDate = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsLessThanOrEqualTo>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>2002-06-11T10:36:52.000-07:00</ns3:Literal>"
            + "</ns3:PropertyIsLessThanOrEqualTo>" + "</ns3:Filter>";

    private final String propertyIsLessThanOrEqualToXmlWithBoolean = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsLessThanOrEqualTo>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>true</ns3:Literal>"
            + "</ns3:PropertyIsLessThanOrEqualTo>" + "</ns3:Filter>";

    private final String propertyIsBetweenXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsBetween>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:LowerBoundary>"
            + "<ns3:Literal>5</ns3:Literal>"
            + "</ns3:LowerBoundary>"
            + "<ns3:UpperBoundary>"
            + "<ns3:Literal>15</ns3:Literal>"
            + "</ns3:UpperBoundary>" + "</ns3:PropertyIsBetween>" + "</ns3:Filter>";

    private final String propertyIsBetweenXmlWithDecimal = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsBetween>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:LowerBoundary>"
            + "<ns3:Literal>5.0</ns3:Literal>"
            + "</ns3:LowerBoundary>"
            + "<ns3:UpperBoundary>"
            + "<ns3:Literal>15.0</ns3:Literal>"
            + "</ns3:UpperBoundary>" + "</ns3:PropertyIsBetween>" + "</ns3:Filter>";

    private final String propertyIsBetweenXmlAnyText = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsBetween>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:LowerBoundary>"
            + "<ns3:Literal>5</ns3:Literal>"
            + "</ns3:LowerBoundary>"
            + "<ns3:UpperBoundary>"
            + "<ns3:Literal>15</ns3:Literal>"
            + "</ns3:UpperBoundary>" + "</ns3:PropertyIsBetween>" + "</ns3:Filter>";

    private final String propertyIsBetweenXmlWithDate = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsBetween>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:LowerBoundary>"
            + "<ns3:Literal>5</ns3:Literal>"
            + "</ns3:LowerBoundary>"
            + "<ns3:UpperBoundary>"
            + "<ns3:Literal>15</ns3:Literal>"
            + "</ns3:UpperBoundary>" + "</ns3:PropertyIsBetween>" + "</ns3:Filter>";

    private final String propertyIsNullXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsNull>"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>" + "</ns3:PropertyIsNull>" + "</ns3:Filter>";

    private final String propertyIsLikeXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>*bar*</ns3:Literal>" + "</ns3:PropertyIsLike>" + "</ns3:Filter>";

    private final String propertyIsLikeXmlAnyText = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
            + "<ns3:PropertyName>"
            + CswConstants.ANY_TEXT
            + "</ns3:PropertyName>"
            + "<ns3:Literal>*bar*</ns3:Literal>" + "</ns3:PropertyIsLike>" + "</ns3:Filter>";

    private final String orComparisonOpsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Or>"
            + "<ns3:PropertyIsEqualTo matchCase=\"false\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>true</ns3:Literal>"
            + "</ns3:PropertyIsEqualTo>"
            + "<ns3:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>*bar*</ns3:Literal>"
            + "</ns3:PropertyIsLike>"
            + "</ns3:Or>"
            + "</ns3:Filter>";

    private final String intersectsPolygonXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Intersects>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:Polygon srsName=\"EPSG:4326\">"
            + "<ns4:exterior>"
            + "<ns4:LinearRing>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "</ns4:LinearRing>"
            + "</ns4:exterior>"
            + "</ns4:Polygon>" + "</ns3:Intersects>" + "</ns3:Filter>";

    private final String intersectsPolygonXmlPropertyOwsBoundingBoxLonLatIsKeptAsLonLat = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Intersects>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:Polygon srsName=\"EPSG:4326\">"
            + "<ns4:exterior>"
            + "<ns4:LinearRing>"
            + "<ns4:pos>30.0 -10.0</ns4:pos>"
            + "<ns4:pos>30.0 30.0</ns4:pos>"
            + "<ns4:pos>10.0 30.0</ns4:pos>"
            + "<ns4:pos>10.0 -10.0</ns4:pos>"
            + "<ns4:pos>30.0 -10.0</ns4:pos>"
            + "</ns4:LinearRing>"
            + "</ns4:exterior>"
            + "</ns4:Polygon>" + "</ns3:Intersects>" + "</ns3:Filter>";

    private final String bboxXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:BBOX>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:Envelope>"
            + "<ns4:lowerCorner>-10.0 10.0</ns4:lowerCorner>"
            + "<ns4:upperCorner>30.0 30.0</ns4:upperCorner>"
            + "</ns4:Envelope>"
            + "</ns3:BBOX>"
            + "</ns3:Filter>";

    private final String intersectsPolygonXmlPropertyDctSpatial = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Intersects>"
            + "<ns3:PropertyName>dct:Spatial</ns3:PropertyName>"
            + "<ns4:Polygon srsName=\"EPSG:4326\">"
            + "<ns4:exterior>"
            + "<ns4:LinearRing>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "</ns4:LinearRing>"
            + "</ns4:exterior>"
            + "</ns4:Polygon>" + "</ns3:Intersects>" + "</ns3:Filter>";

    private final String intersectsPointXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Intersects>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:Point srsName=\"EPSG:4326\">"
            + "<ns4:pos>"
            + "30.0 30.0"
            + "</ns4:pos>"
            + "</ns4:Point>" + "</ns3:Intersects>" + "</ns3:Filter>";

    private final String intersectsLineStringXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Intersects>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:LineString srsName=\"EPSG:4326\">"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 10.0</ns4:pos>"
            + "</ns4:LineString>" + "</ns3:Intersects>" + "</ns3:Filter>";

    private final String intersectsMultiPolygonXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Intersects>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:MultiPolygon srsName=\"EPSG:4326\">"
            + "<ns4:polygonMember>"
            + "<ns4:Polygon>"
            + "<ns4:exterior>"
            + "<ns4:LinearRing>"
            + "<ns4:pos>40.0 40.0</ns4:pos>"
            + "<ns4:pos>45.0 20.0</ns4:pos>"
            + "<ns4:pos>30.0 45.0</ns4:pos>"
            + "<ns4:pos>40.0 40.0</ns4:pos>"
            + "</ns4:LinearRing>"
            + "</ns4:exterior>"
            + "</ns4:Polygon>"
            + "</ns4:polygonMember>"
            + "<ns4:polygonMember>"
            + "<ns4:Polygon>"
            + "<ns4:exterior>"
            + "<ns4:LinearRing>"
            + "<ns4:pos>35.0 20.0</ns4:pos>"
            + "<ns4:pos>20.0 45.0</ns4:pos>"
            + "<ns4:pos>5.0 30.0</ns4:pos>"
            + "<ns4:pos>10.0 10.0</ns4:pos>"
            + "<ns4:pos>30.0 10.0</ns4:pos>"
            + "<ns4:pos>35.0 20.0</ns4:pos>"
            + "</ns4:LinearRing>"
            + "</ns4:exterior>"
            + "<ns4:interior>"
            + "<ns4:LinearRing>"
            + "<ns4:pos>20.0 30.0</ns4:pos>"
            + "<ns4:pos>25.0 20.0</ns4:pos>"
            + "<ns4:pos>15.0 20.0</ns4:pos>"
            + "<ns4:pos>20.0 30.0</ns4:pos>"
            + "</ns4:LinearRing>"
            + "</ns4:interior>"
            + "</ns4:Polygon>"
            + "</ns4:polygonMember>"
            + "</ns4:MultiPolygon>"
            + "</ns3:Intersects>" + "</ns3:Filter>";

    private final String intersectsMultiPointXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Intersects>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:MultiPoint srsName=\"EPSG:4326\">"
            + "<ns4:pointMember>"
            + "<ns4:Point>"
            + "<ns4:pos>"
            + "40.0 10.0"
            + "</ns4:pos>"
            + "</ns4:Point>"
            + "</ns4:pointMember>"
            + "<ns4:pointMember>"
            + "<ns4:Point>"
            + "<ns4:pos>"
            + "30.0 40.0"
            + "</ns4:pos>"
            + "</ns4:Point>"
            + "</ns4:pointMember>"
            + "<ns4:pointMember>"
            + "<ns4:Point>"
            + "<ns4:pos>"
            + "20.0 20.0"
            + "</ns4:pos>"
            + "</ns4:Point>"
            + "</ns4:pointMember>"
            + "<ns4:pointMember>"
            + "<ns4:Point>"
            + "<ns4:pos>"
            + "10.0 30.0"
            + "</ns4:pos>"
            + "</ns4:Point>"
            + "</ns4:pointMember>"
            + "</ns4:MultiPoint>"
            + "</ns3:Intersects>"
            + "</ns3:Filter>";

    private final String intersectsMultiLineStringXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Intersects>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:MultiLineString srsName=\"EPSG:4326\">"
            + "<ns4:lineStringMember>"
            + "<ns4:LineString>"
            + "<ns4:pos>10.0 10.0</ns4:pos>"
            + "<ns4:pos>20.0 20.0</ns4:pos>"
            + "<ns4:pos>40.0 10.0</ns4:pos>"
            + "</ns4:LineString>"
            + "</ns4:lineStringMember>"
            + "<ns4:lineStringMember>"
            + "<ns4:LineString>"
            + "<ns4:pos>40.0 40.0</ns4:pos>"
            + "<ns4:pos>30.0 30.0</ns4:pos>"
            + "<ns4:pos>20.0 40.0</ns4:pos>"
            + "<ns4:pos>10.0 30.0</ns4:pos>"
            + "</ns4:LineString>"
            + "</ns4:lineStringMember>"
            + "</ns4:MultiLineString>"
            + "</ns3:Intersects>" + "</ns3:Filter>";

    private final String intersectsEnvelopeXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns5:Filter xmlns:ns2=\"http://purl.org/dc/elements/1.1/\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://purl.org/dc/terms/\" xmlns:ns3=\"http://www.opengis.net/ows\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.opengis.net/ogc\" xmlns:ns6=\"http://www.opengis.net/gml\" xmlns:ns7=\"http://www.w3.org/1999/xlink\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns5:Intersects>"
            + "<ns5:PropertyName>ows:BoundingBox</ns5:PropertyName>"
            + "<ns6:Envelope>"
            + "<ns6:lowerCorner>-10.0 10.0</ns6:lowerCorner>"
            + "<ns6:upperCorner>30.0 30.0</ns6:upperCorner>"
            + "</ns6:Envelope>"
            + "</ns5:Intersects>" + "</ns5:Filter>";

    private final String crossesPolygonXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Crosses>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:Polygon srsName=\"EPSG:4326\">"
            + "<ns4:exterior>"
            + "<ns4:LinearRing>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "</ns4:LinearRing>"
            + "</ns4:exterior>"
            + "</ns4:Polygon>" + "</ns3:Crosses>" + "</ns3:Filter>";

    private final String withinPolygonXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Within>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:Polygon srsName=\"EPSG:4326\">"
            + "<ns4:exterior>"
            + "<ns4:LinearRing>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "</ns4:LinearRing>"
            + "</ns4:exterior>"
            + "</ns4:Polygon>" + "</ns3:Within>" + "</ns3:Filter>";

    private final String containsPolygonXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Contains>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:Polygon srsName=\"EPSG:4326\">"
            + "<ns4:exterior>"
            + "<ns4:LinearRing>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "</ns4:LinearRing>"
            + "</ns4:exterior>"
            + "</ns4:Polygon>" + "</ns3:Contains>" + "</ns3:Filter>";

    private final String disjointPolygonXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Disjoint>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:Polygon srsName=\"EPSG:4326\">"
            + "<ns4:exterior>"
            + "<ns4:LinearRing>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "</ns4:LinearRing>"
            + "</ns4:exterior>"
            + "</ns4:Polygon>" + "</ns3:Disjoint>" + "</ns3:Filter>";

    private final String notDisjointPolygonXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Not>"
            + "<ns3:Disjoint>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:Polygon srsName=\"EPSG:4326\">"
            + "<ns4:exterior>"
            + "<ns4:LinearRing>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "</ns4:LinearRing>"
            + "</ns4:exterior>"
            + "</ns4:Polygon>" + "</ns3:Disjoint>" + "</ns3:Not>" + "</ns3:Filter>";

    private final String overlapsPolygonXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Overlaps>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:Polygon srsName=\"EPSG:4326\">"
            + "<ns4:exterior>"
            + "<ns4:LinearRing>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "</ns4:LinearRing>"
            + "</ns4:exterior>"
            + "</ns4:Polygon>" + "</ns3:Overlaps>" + "</ns3:Filter>";

    private final String touchesPolygonXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Touches>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:Polygon srsName=\"EPSG:4326\">"
            + "<ns4:exterior>"
            + "<ns4:LinearRing>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 10.0</ns4:pos>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "</ns4:LinearRing>"
            + "</ns4:exterior>"
            + "</ns4:Polygon>" + "</ns3:Touches>" + "</ns3:Filter>";

    private final String beyondPointXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Beyond>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:Point srsName=\"EPSG:4326\">"
            + "<ns4:pos>"
            + "30.0 30.0"
            + "</ns4:pos>"
            + "</ns4:Point>"
            + "<ns3:Distance units=\"METERS\">123.456</ns3:Distance>"
            + "</ns3:Beyond>"
            + "</ns3:Filter>";

    private final String dwithinPolygonXmlPropertyOwsBoundingBox = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:DWithin>"
            + "<ns3:PropertyName>ows:BoundingBox</ns3:PropertyName>"
            + "<ns4:Polygon srsName=\"EPSG:4326\">"
            + "<ns4:exterior>"
            + "<ns4:LinearRing>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "<ns4:pos>30.0 30.0 </ns4:pos>"
            + "<ns4:pos>30.0 10.0</ns4:pos> "
            + "<ns4:pos>-10.0 10.0 </ns4:pos>"
            + "<ns4:pos>-10.0 30.0</ns4:pos>"
            + "</ns4:LinearRing>"
            + "</ns4:exterior>"
            + "</ns4:Polygon>"
            + "<ns3:Distance units=\"METERS\">123.456</ns3:Distance>"
            + "</ns3:DWithin>" + "</ns3:Filter>";

    private final String orLogicOpsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:Or>"
            + "<ns3:Not>"
            + "<ns3:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>*bar*</ns3:Literal>"
            + "</ns3:PropertyIsLike>"
            + "</ns3:Not>"
            + "<ns3:PropertyIsEqualTo matchCase=\"false\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>true</ns3:Literal>"
            + "</ns3:PropertyIsEqualTo>" + "</ns3:Or>" + "</ns3:Filter>";

    private final String orFeatureIdXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:FeatureId fid=\"cswRecord.5678\"/>"
            + "<ns3:FeatureId fid=\"cswRecord.1234\"/>"
            + "</ns3:Filter>";

    private final String orSpatialOpsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    		+ "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/cat/csw/2.0.2\" "
    		+ "xmlns=\"http://purl.org/dc/elements/1.1/\" "
    		+ "xmlns:ns4=\"http://purl.org/dc/terms/\" "
    		+ "xmlns:ns3=\"http://www.opengis.net/ows\" "
    		+ "xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" "
    		+ "xmlns:ns5=\"http://www.opengis.net/ogc\" "
    		+ "xmlns:ns6=\"http://www.opengis.net/gml\" "
    		+ "xmlns:ns7=\"http://www.w3.org/1999/xlink\" "
    		+ "xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
    		+ "<ns5:Or>"
    		+ "<ns5:DWithin>"
    		+ "<ns5:PropertyName>title</ns5:PropertyName>"
    		+ "<ns6:Point srsName=\"EPSG:4326\">"
    		+ "<ns6:pos>10.0 30.0</ns6:pos>"
    		+ "</ns6:Point>"
    		+ "<ns5:Distance units=\"METERS\">1000.0</ns5:Distance>"
    		+ "</ns5:DWithin"
    		+ "><ns5:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
    		+ "<ns5:PropertyName>title</ns5:PropertyName>"
    		+ "<ns5:Literal>*bar*</ns5:Literal>"
    		+ "</ns5:PropertyIsLike>"
    		+ "</ns5:Or>"
    		+ "</ns5:Filter>";

    private final String andComparisonOpsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:And>"
            + "<ns3:PropertyIsEqualTo matchCase=\"false\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>true</ns3:Literal>"
            + "</ns3:PropertyIsEqualTo>"
            + "<ns3:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>*bar*</ns3:Literal>"
            + "</ns3:PropertyIsLike>"
            + "</ns3:And>"
            + "</ns3:Filter>";

    private final String andLogicOpsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns3:And>"
            + "<ns3:Not>"
            + "<ns3:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>*bar*</ns3:Literal>"
            + "</ns3:PropertyIsLike>"
            + "</ns3:Not>"
            + "<ns3:PropertyIsEqualTo matchCase=\"false\">"
            + "<ns3:PropertyName>"
            + DEFAULT_PROPERTY_NAME
            + "</ns3:PropertyName>"
            + "<ns3:Literal>true</ns3:Literal>"
            + "</ns3:PropertyIsEqualTo>" + "</ns3:And>" + "</ns3:Filter>";

    private String dWithinFallbackToIntersects1 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns5:Filter xmlns:ns2=\"http://purl.org/dc/elements/1.1/\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://purl.org/dc/terms/\" xmlns:ns3=\"http://www.opengis.net/ows\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.opengis.net/ogc\" xmlns:ns6=\"http://www.opengis.net/gml\" xmlns:ns7=\"http://www.w3.org/1999/xlink\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns5:Intersects>"
            + "<ns5:PropertyName>ows:BoundingBox</ns5:PropertyName>"
            + "<ns6:Envelope>"
            + "<ns6:lowerCorner>29.998889735046777 29.998889735046777</ns6:lowerCorner>"
            + "<ns6:upperCorner>30.001110264953223 30.001110264953223</ns6:upperCorner>"
            + "</ns6:Envelope>" + "</ns5:Intersects>" + "</ns5:Filter>";

    private String dWithinFallbackToIntersects2 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns5:Filter xmlns:ns2=\"http://purl.org/dc/elements/1.1/\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://purl.org/dc/terms/\" xmlns:ns3=\"http://www.opengis.net/ows\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.opengis.net/ogc\" xmlns:ns6=\"http://www.opengis.net/gml\" xmlns:ns7=\"http://www.w3.org/1999/xlink\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns5:Intersects>"
            + "<ns5:PropertyName>ows:BoundingBox</ns5:PropertyName>"
            + "<ns6:Polygon srsName=\"EPSG:4326\">"
            + "<ns6:exterior>"
            + "<ns6:LinearRing>"
            + "<ns6:pos>30.0 30.001110264953223 </ns6:pos> "
            + "<ns6:pos>29.999783398052752 30.00108893152347 </ns6:pos> "
            + "<ns6:pos>29.999575119996866 30.00102575106595 </ns6:pos> "
            + "<ns6:pos>29.999383169841224 30.00092315157021 </ns6:pos> "
            + "<ns6:pos>29.99921492412266 30.00078507587734 </ns6:pos> "
            + "<ns6:pos>29.99907684842979 30.000616830158776 </ns6:pos> "
            + "<ns6:pos>29.99897424893405 30.000424880003134 </ns6:pos> "
            + "<ns6:pos>29.99891106847653 30.000216601947248 </ns6:pos> "
            + "<ns6:pos>29.998889735046777 30.0 </ns6:pos> "
            + "<ns6:pos>29.99891106847653 29.999783398052752 </ns6:pos> "
            + "<ns6:pos>29.99897424893405 29.999575119996866 </ns6:pos> "
            + "<ns6:pos>29.99907684842979 29.999383169841224 </ns6:pos> "
            + "<ns6:pos>29.99921492412266 29.99921492412266 </ns6:pos> "
            + "<ns6:pos>29.999383169841224 29.99907684842979 </ns6:pos> "
            + "<ns6:pos>29.999575119996866 29.99897424893405 </ns6:pos> "
            + "<ns6:pos>29.999783398052752 29.99891106847653 </ns6:pos> "
            + "<ns6:pos>30.0 29.998889735046777 </ns6:pos> "
            + "<ns6:pos>30.000216601947248 29.99891106847653 </ns6:pos> "
            + "<ns6:pos>30.000424880003134 29.99897424893405 </ns6:pos> "
            + "<ns6:pos>30.000616830158776 29.99907684842979 </ns6:pos> "
            + "<ns6:pos>30.00078507587734 29.99921492412266 </ns6:pos> "
            + "<ns6:pos>30.00092315157021 29.999383169841224 </ns6:pos> "
            + "<ns6:pos>30.00102575106595 29.999575119996866 </ns6:pos> "
            + "<ns6:pos>30.00108893152347 29.999783398052752 </ns6:pos> "
            + "<ns6:pos>30.001110264953223 30.0 </ns6:pos> "
            + "<ns6:pos>30.00108893152347 30.000216601947248 </ns6:pos> "
            + "<ns6:pos>30.00102575106595 30.000424880003134 </ns6:pos> "
            + "<ns6:pos>30.00092315157021 30.000616830158776 </ns6:pos> "
            + "<ns6:pos>30.00078507587734 30.00078507587734 </ns6:pos> "
            + "<ns6:pos>30.000616830158776 30.00092315157021 </ns6:pos> "
            + "<ns6:pos>30.000424880003134 30.00102575106595 </ns6:pos> "
            + "<ns6:pos>30.000216601947248 30.00108893152347 </ns6:pos> "
            + "<ns6:pos>30.0 30.001110264953223</ns6:pos> "
            + "</ns6:LinearRing>"
            + "</ns6:exterior>"
            + "</ns6:Polygon>" + "</ns5:Intersects>" + "</ns5:Filter>";

    private final String andSpatialOpsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    		+ "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/cat/csw/2.0.2\" "
    		+ "xmlns=\"http://purl.org/dc/elements/1.1/\" "
    		+ "xmlns:ns4=\"http://purl.org/dc/terms/\" "
    		+ "xmlns:ns3=\"http://www.opengis.net/ows\" "
    		+ "xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" "
    		+ "xmlns:ns5=\"http://www.opengis.net/ogc\" "
    		+ "xmlns:ns6=\"http://www.opengis.net/gml\" "
    		+ "xmlns:ns7=\"http://www.w3.org/1999/xlink\" "
    		+ "xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
    		+ "<ns5:And>"
    		+ "<ns5:DWithin>"
    		+ "<ns5:PropertyName>title</ns5:PropertyName>"
    		+ "<ns6:Point srsName=\"EPSG:4326\">"
    		+ "<ns6:pos>10.0 30.0</ns6:pos>"
    		+ "</ns6:Point>"
    		+ "<ns5:Distance units=\"METERS\">1000.0</ns5:Distance>"
    		+ "</ns5:DWithin>"
    		+ "<ns5:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
    		+ "<ns5:PropertyName>title</ns5:PropertyName>"
    		+ "<ns5:Literal>*bar*</ns5:Literal>"
    		+ "</ns5:PropertyIsLike>"
    		+ "</ns5:And>"
    		+ "</ns5:Filter>";

    private final String configurableContentTypeMapping = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns5:Filter xmlns:ns2=\"http://purl.org/dc/elements/1.1/\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://purl.org/dc/terms/\" xmlns:ns3=\"http://www.opengis.net/ows\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.opengis.net/ogc\" xmlns:ns6=\"http://www.opengis.net/gml\" xmlns:ns7=\"http://www.w3.org/1999/xlink\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
            + "<ns5:PropertyIsEqualTo matchCase=\"false\">"
            + "<ns5:PropertyName>format</ns5:PropertyName>"
            + "<ns5:Literal>myContentType</ns5:Literal>"
            + "</ns5:PropertyIsEqualTo>"
            + "</ns5:Filter>";

    private final String emptyFilterXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns5:Filter xmlns:ns2=\"http://purl.org/dc/elements/1.1/\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://purl.org/dc/terms/\" xmlns:ns3=\"http://www.opengis.net/ows\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.opengis.net/ogc\" xmlns:ns6=\"http://www.opengis.net/gml\" xmlns:ns7=\"http://www.w3.org/1999/xlink\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\"/>";

    /**
     * Property is equal to tests
     */
    @Test
    public void testPropertyIsEqualToStringLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, stringLiteral,
                isCaseSensitive);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    /**
     * The CSW Source should be able to map a csw:Record field to Content Type.
     * 
     * Verify that when an isEqualTo query is sent to the CswFilterDelegate Metacard.CONTENT_TYPE is
     * mapped to the configured content type mapping (eg. in this test Metacard.CONTENT_TYPE is
     * mapped to format).
     */
    @Test
    public void testConfigurableContentTypeMapping() throws JAXBException, SAXException,
        IOException {
        // Setup
        String contentTypeMapping = CswRecordMetacardType.CSW_FORMAT;
        String contentType = "myContentType";
        CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(initCswSourceConfiguration(false,
                contentTypeMapping));

        // Perform Test
        /**
         * Incoming query with Metacard.CONTENT_TYPE equal to myContentType. Metacard.CONTENT_TYPE
         * will be mapped to format in the CswFilterDelegate.
         */
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(Metacard.CONTENT_TYPE,
                contentType, isCaseSensitive);

        // Verify
        /**
         * Verify that a PropertyIsEqualTo filter is created with PropertyName of format and Literal
         * equal to myContentType
         */
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(configurableContentTypeMapping, xml);
        LOGGER.debug(writer.toString());
    }

    /**
     * Verify that when given a non ISO 8601 formatted date, the CswFilterDelegate converts the date
     * to ISO 8601 format (ie. the xml generated off of the filterType should have an ISO 8601
     * formatted date in it).
     */
    @Test
    public void testPropertyIsEqualToDateLiteral() throws JAXBException, ParseException,
        SAXException, IOException {
        LOGGER.debug("Input date: {}", date);
        LOGGER.debug("ISO 8601 formatted date: {}", convertDateToIso8601Format(getDate()));
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, date);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsEqualToXmlWithDate, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsEqualToStringLiteralAnyText() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyNameAnyText,
                stringLiteral, isCaseSensitive);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsEqualToXmlAnyText, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testDuring() throws JAXBException, SAXException, IOException {

        DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
        DateTime endDate =  new DateTime(2013, 12, 31, 0, 0, 0, 0);

        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE, effectiveDateMapping, createdDateMapping, modifiedDateMapping);

        CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
        FilterType filterType = cswFilterDelegate.during(propertyNameModified,
                startDate.toCalendar(null).getTime(), endDate.toCalendar(null).getTime());
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();

        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String startDateStr = fmt.print(startDate);
        String endDateStr = fmt.print(endDate);
        String testResponse = during.replace(REPLACE_START_DATE, startDateStr).replace(REPLACE_END_DATE, endDateStr).replace(REPLACE_TEMPORAL_PROPERTY, modifiedDateMapping);
        assertXMLEqual(testResponse, xml);

    }

    @Test
    public void testDuringAlteredEffectiveDateMapping() throws JAXBException, SAXException, IOException {

        DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
        DateTime endDate =  new DateTime(2013, 12, 31, 0, 0, 0, 0);

        String replacedTemporalProperty = "myEffectiveDate";
        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE, replacedTemporalProperty, createdDateMapping, modifiedDateMapping);

        CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
        FilterType filterType = cswFilterDelegate.during(propertyNameEffective,
                startDate.toCalendar(null).getTime(), endDate.toCalendar(null).getTime());
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String startDateStr = fmt.print(startDate);
        String endDateStr = fmt.print(endDate);
        String testResponse = during.replace(REPLACE_START_DATE, startDateStr).replace(REPLACE_END_DATE, endDateStr).replace(REPLACE_TEMPORAL_PROPERTY, replacedTemporalProperty);
        assertXMLEqual(testResponse, xml);

    }

    @Test
    public void testDuringAlteredCreatedDateMapping() throws JAXBException, SAXException, IOException {

        DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
        DateTime endDate =  new DateTime(2013, 12, 31, 0, 0, 0, 0);

        String replacedTemporalProperty = "myCreatedDate";
        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE, effectiveDateMapping, replacedTemporalProperty, modifiedDateMapping);

        CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
        FilterType filterType = cswFilterDelegate.during(propertyNameCreated,
                startDate.toCalendar(null).getTime(), endDate.toCalendar(null).getTime());
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String startDateStr = fmt.print(startDate);
        String endDateStr = fmt.print(endDate);
        String testResponse = during.replace(REPLACE_START_DATE, startDateStr).replace(REPLACE_END_DATE, endDateStr).replace(REPLACE_TEMPORAL_PROPERTY, replacedTemporalProperty);
        assertXMLEqual(testResponse, xml);

    }

    @Test
    public void testDuringAlteredModifiedDateMapping() throws JAXBException, SAXException, IOException {

        DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
        DateTime endDate =  new DateTime(2013, 12, 31, 0, 0, 0, 0);

        String replacedTemporalProperty = "myModifiedDate";
        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE, effectiveDateMapping, createdDateMapping, replacedTemporalProperty);

        CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
        FilterType filterType = cswFilterDelegate.during(propertyNameModified,
                startDate.toCalendar(null).getTime(), endDate.toCalendar(null).getTime());
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String startDateStr = fmt.print(startDate);
        String endDateStr = fmt.print(endDate);
        String testResponse = during.replace(REPLACE_START_DATE, startDateStr).replace(REPLACE_END_DATE, endDateStr).replace(REPLACE_TEMPORAL_PROPERTY, replacedTemporalProperty);
        assertXMLEqual(testResponse, xml);

    }


    @Test
    public void testRelative() throws JAXBException, SAXException, IOException {
        long duration = 92000000000L;
        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE, effectiveDateMapping, createdDateMapping, modifiedDateMapping);
        CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
        FilterType filterType = cswFilterDelegate.relative(propertyNameModified,
                duration);

        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();

        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();

        String durationCompare = during.replace(REPLACE_START_DATE, "").replace(REPLACE_END_DATE, "").replace(REPLACE_TEMPORAL_PROPERTY, modifiedDateMapping);
        String pattern = "(?i)(<ns.:Literal>)(.+?)(</ns.:Literal>)";
        String compareXml = xml.replaceAll(pattern, "<ogc:Literal xmlns:ogc=\"http://www.opengis.net/ogc\"></ogc:Literal>");

        assertXMLEqual(durationCompare, compareXml);

    }

    @Test
    public void testPropertyIsEqualToStringLiteralNonQueryableProperty() throws JAXBException,
        SAXException, IOException {
        /**
         * See CswRecordMetacardType.java for queryable and non-queryable properties.
         */
        String nonQueryableProperty = Metacard.METADATA;
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(nonQueryableProperty,
                stringLiteral, isCaseSensitive);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(emptyFilterXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsEqualToStringLiteralType() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(Metacard.CONTENT_TYPE,
                contentTypeLiteral, isCaseSensitive);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsEqualToXmlContentType, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsEqualToIntLiteral() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, intLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsEqualToShortLiteral() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, shortLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsEqualToLongLiteral() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, longLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsEqualToFloatLiteral() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, floatLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsEqualToXmlWithDecimal, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsEqualToDoubleLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, doubleLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsEqualToXmlWithDecimal, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsEqualToBooleanLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, booleanLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsEqualToXmlWithBoolean, xml);
        LOGGER.debug(writer.toString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsEqualToByteArrayLiteral() throws JAXBException {

        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, byteArrayLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsEqualToObjectLiteral() throws JAXBException {

        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, objectLiteral);
    }

    /**
     * Property is not equal to tests
     */
    @Test
    public void testPropertyIsNotEqualToStringLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, stringLiteral,
                isCaseSensitive);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsNotEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsNotEqualToStringLiteralAnyText() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyNameAnyText,
                stringLiteral, isCaseSensitive);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsNotEqualToXmlAnyText, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsNotEqualToIntLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, intLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsNotEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsNotEqualToShortLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, shortLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsNotEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsNotEqualToLongLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, longLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsNotEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsNotEqualToFloatLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, floatLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsNotEqualToXmlWithDecimal, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsNotEqualToDoubleLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, doubleLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsNotEqualToXmlWithDecimal, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsNotEqualToBooleanLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate
                .propertyIsNotEqualTo(propertyName, booleanLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsNotEqualToXmlWithBoolean, xml);
        LOGGER.debug(writer.toString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsNotEqualToByteArrayLiteral() throws JAXBException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName,
                byteArrayLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsNotEqualToObjectLiteral() throws JAXBException {

        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, objectLiteral);
    }

    /**
     * Property is greater than tests
     */
    @Test
    public void testPropertyIsGreaterThanStringLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate
                .propertyIsGreaterThan(propertyName, stringLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsGreaterThanXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsGreaterThanStringLiteralAnyText() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyNameAnyText,
                stringLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsGreaterThanXmlAnyText, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsGreaterThanIntLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName, intLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsGreaterThanXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsGreaterThanShortLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName, shortLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsGreaterThanXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsGreaterThanLongLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName, longLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsGreaterThanXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsGreaterThanFloatLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName, floatLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsGreaterThanXmlWithDecimal, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsGreaterThanDoubleLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate
                .propertyIsGreaterThan(propertyName, doubleLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsGreaterThanXmlWithDecimal, xml);
        LOGGER.debug(writer.toString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsGreaterThanBooleanLiteral() throws JAXBException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName,
                booleanLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsGreaterThanByteArrayLiteral() throws JAXBException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName,
                byteArrayLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsGreaterThanObjectLiteral() throws JAXBException {
        FilterType filterType = cswFilterDelegate
                .propertyIsGreaterThan(propertyName, objectLiteral);
    }

    /**
     * Property is greater than or equal to tests
     */
    @Test
    public void testPropertyIsGreaterThanOrEqualToStringLiteral() throws JAXBException,
        SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                stringLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsGreaterThanOrEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToStringLiteralAnyText() throws JAXBException,
        SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(
                propertyNameAnyText, stringLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsGreaterThanOrEqualToXmlAnyText, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToIntLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                intLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsGreaterThanOrEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToShortLiteral() throws JAXBException,
        SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                shortLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsGreaterThanOrEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToLongLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                longLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsGreaterThanOrEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToFloatLiteral() throws JAXBException,
        SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                floatLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsGreaterThanOrEqualToXmlWithDecimal, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToDoubleLiteral() throws JAXBException,
        SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                doubleLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsGreaterThanOrEqualToXmlWithDecimal, xml);
        LOGGER.debug(writer.toString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsGreaterThanOrEqualToBooleanLiteral() throws JAXBException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                booleanLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsGreaterThanOrEqualToByteArrayLiteral() throws JAXBException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                byteArrayLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsGreaterThanOrEqualToObjectLiteral() throws JAXBException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                objectLiteral);
    }

    /**
     * Property is less than tests
     */
    @Test
    public void testPropertyIsLessThanStringLiteral() throws JAXBException, SAXException,
        IOException {

        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, stringLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLessThanXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsLessThanStringLiteralAnyText() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyNameAnyText,
                stringLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLessThanXmlAnyText, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsLessThanIntLiteral() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, intLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLessThanXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsLessThanShortLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, shortLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLessThanXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsLessThanLongLiteral() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, longLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLessThanXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsLessThanFloatLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, floatLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLessThanXmlWithDecimal, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsLessThanDoubleLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, doubleLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLessThanXmlWithDecimal, xml);
        LOGGER.debug(writer.toString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsLessThanBooleanLiteral() throws JAXBException {

        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, booleanLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsLessThanByteArrayLiteral() throws JAXBException {
        FilterType filterType = cswFilterDelegate
                .propertyIsLessThan(propertyName, byteArrayLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsLessThanObjectLiteral() throws JAXBException {

        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, objectLiteral);
    }

    /**
     * Property is less than or equal to tests
     */
    @Test
    public void testPropertyIsLessThanOrEqualToStringLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                stringLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLessThanOrEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsLessThanOrEqualToStringLiteralAnyText() throws JAXBException,
        SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyNameAnyText,
                stringLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLessThanOrEqualToXmlAnyText, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsLessThanOrEqualToIntLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                intLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLessThanOrEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsLessThanOrEqualToShortLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                intLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLessThanOrEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsLessThanOrEqualToLongLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                shortLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLessThanOrEqualToXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsLessThanOrEqualToFloatLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                floatLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLessThanOrEqualToXmlWithDecimal, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyIsLessThanOrEqualToDoubleLiteral() throws JAXBException, SAXException,
        IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                doubleLiteral);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLessThanOrEqualToXmlWithDecimal, xml);
        LOGGER.debug(writer.toString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsLessThanOrEqualToBooleanLiteral() throws JAXBException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                booleanLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsLessThanOrEqualToByteArrayLiteral() throws JAXBException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                byteArrayLiteral);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsLessThanOrEqualToObjectLiteral() throws JAXBException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                objectLiteral);
    }

    /**
     * Property is between tests
     */
    @Test
    public void testPropertyBetweenStringLiterals() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsBetween(propertyName,
                stringLowerBoundary, stringUpperBoundary);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsBetweenXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyBetweenIntLiterals() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsBetween(propertyName, intLowerBoundary,
                intUpperBoundary);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsBetweenXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyBetweenShortLiterals() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsBetween(propertyName,
                shortLowerBoundary, shortUpperBoundary);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsBetweenXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyBetweenLongLiterals() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsBetween(propertyName,
                longLowerBoundary, longUpperBoundary);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsBetweenXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyBetweenFloatLiterals() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsBetween(propertyName,
                floatLowerBoundary, floatUpperBoundary);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsBetweenXmlWithDecimal, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyBetweenDoubleLiterals() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsBetween(propertyName,
                doubleLowerBoundary, doubleUpperBoundary);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsBetweenXmlWithDecimal, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyNull() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsNull(propertyName);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsNullXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyLike() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLike(propertyName, likeLiteral,
                isCaseSensitive);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLikeXml, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testPropertyLikeAnyText() throws JAXBException, SAXException, IOException {
        FilterType filterType = cswFilterDelegate.propertyIsLike(propertyNameAnyText, likeLiteral,
                isCaseSensitive);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        assertXMLEqual(propertyIsLikeXmlAnyText, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testComparisonOpsOr() throws JAXBException, SAXException, IOException {
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);
        FilterType propertyIsEqualFilter = cswFilterDelegate.propertyIsEqualTo(propertyName,
                booleanLiteral);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(propertyIsEqualFilter);
        filters.add(propertyIsLikeFilter);
        FilterType filter = cswFilterDelegate.or(filters);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
        String xml = writer.toString();

        assertXMLEqual(orComparisonOpsXml, xml);

    }

    @Test
    public void testLogicOpsFiltersOr() throws JAXBException, SAXException, IOException {
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);
        FilterType notFilter = cswFilterDelegate.not(propertyIsLikeFilter);
        FilterType propertyIsEqualFilter = cswFilterDelegate.propertyIsEqualTo(propertyName,
                booleanLiteral);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(notFilter);
        filters.add(propertyIsEqualFilter);
        FilterType filter = cswFilterDelegate.or(filters);

        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
        String xml = writer.toString();

        assertXMLEqual(orLogicOpsXml, xml);
    }

    @Test
    public void testSpatialOpsOr() throws JAXBException, SAXException, IOException {
        FilterType spatialFilter = cswFilterDelegate.dwithin(propertyName, POINT_WKT,
                Double.valueOf(1000));
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(spatialFilter);
        filters.add(propertyIsLikeFilter);

        FilterType filter = cswFilterDelegate.or(filters);

        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();

        marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
        String xml = writer.toString();

        LOGGER.debug(writer.toString());
        assertXMLEqual(orSpatialOpsXml, xml);
    }

    @Test
    public void testFeatureIdOr() throws JAXBException, SAXException, IOException {
        ObjectFactory filterObjectFactory = new ObjectFactory();
        FeatureIdType fidType = new FeatureIdType();
        fidType.setFid("cswRecord.1234");
        List<JAXBElement<? extends AbstractIdType>> fidFilters = new ArrayList<JAXBElement<? extends AbstractIdType>>();
        fidFilters.add(filterObjectFactory.createFeatureId(fidType));
        FilterType idFilter = new FilterType();
        idFilter.setId(fidFilters);

        FeatureIdType fidType2 = new FeatureIdType();
        fidType2.setFid("cswRecord.5678");
        List<JAXBElement<? extends AbstractIdType>> fidFilters2 = new ArrayList<JAXBElement<? extends AbstractIdType>>();
        fidFilters2.add(filterObjectFactory.createFeatureId(fidType2));
        FilterType idFilter2 = new FilterType();
        idFilter2.setId(fidFilters2);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(idFilter);
        filters.add(idFilter2);

        FilterType filter = cswFilterDelegate.or(filters);

        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
        String xml = writer.toString();

        assertXMLEqual(orFeatureIdXml, xml);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFeatureIdAndComparisonOpsOr() throws JAXBException, SAXException, IOException {

        ObjectFactory filterObjectFactory = new ObjectFactory();
        FeatureIdType fidType = new FeatureIdType();
        fidType.setFid("cswRecord.1234");
        List<JAXBElement<? extends AbstractIdType>> fidFilters = new ArrayList<JAXBElement<? extends AbstractIdType>>();
        fidFilters.add(filterObjectFactory.createFeatureId(fidType));

        FilterType idFilter = new FilterType();
        idFilter.setId(fidFilters);

        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);

        List<FilterType> filterList = new ArrayList<FilterType>();
        filterList.add(idFilter);
        filterList.add(propertyIsLikeFilter);

        FilterType filter = cswFilterDelegate.or(filterList);

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyLikeOrEmptyList() {
        List<FilterType> filters = new ArrayList<FilterType>();
        FilterType filter = cswFilterDelegate.or(filters);
    }

    @Test
    public void testComparisonOpsAnd() throws JAXBException, SAXException, IOException {
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);
        FilterType propertyIsEqualFilter = cswFilterDelegate.propertyIsEqualTo(propertyName,
                booleanLiteral);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(propertyIsEqualFilter);
        filters.add(propertyIsLikeFilter);
        FilterType filter = cswFilterDelegate.and(filters);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
        String xml = writer.toString();

        LOGGER.debug(writer.toString());
        assertXMLEqual(andComparisonOpsXml, xml);

    }

    @Test
    public void testLogicOpsFiltersAnd() throws JAXBException, SAXException, IOException {
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);
        FilterType notFilter = cswFilterDelegate.not(propertyIsLikeFilter);
        FilterType propertyIsEqualFilter = cswFilterDelegate.propertyIsEqualTo(propertyName,
                booleanLiteral);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(notFilter);
        filters.add(propertyIsEqualFilter);
        FilterType filter = cswFilterDelegate.and(filters);

        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
        String xml = writer.toString();

        assertXMLEqual(andLogicOpsXml, xml);
    }

    @Test
    public void testSpatialOpsAnd() throws JAXBException, SAXException, IOException {
        FilterType spatialFilter = cswFilterDelegate.dwithin(propertyName, POINT_WKT,
                Double.valueOf(1000));
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(spatialFilter);
        filters.add(propertyIsLikeFilter);

        FilterType filter = cswFilterDelegate.and(filters);

        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();

        marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
        String xml = writer.toString();

        LOGGER.debug(writer.toString());
        assertXMLEqual(andSpatialOpsXml, xml);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsLikeAndEmptyList() {
        List<FilterType> filters = new ArrayList<FilterType>();
        cswFilterDelegate.and(filters);
    }

    @Test
    public void testAndEmptyFilter() {
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);
        FilterType emptyFilter = new FilterType();

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(emptyFilter);
        filters.add(propertyIsLikeFilter);

        FilterType filter = cswFilterDelegate.and(filters);
        assertNotNull(filter.getComparisonOps());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAndInvalidFilter() {
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(null);
        filters.add(propertyIsLikeFilter);

        cswFilterDelegate.and(filters);

    }

    @Test
    public void testOrEmptyFilter() {
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(new FilterType());
        filters.add(propertyIsLikeFilter);

        FilterType filter = cswFilterDelegate.or(filters);

        assertNotNull(filter.getComparisonOps());
        assertNull(filter.getLogicOps());

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testOrInvalidFilter() {
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(null);
        filters.add(propertyIsLikeFilter);

        FilterType filter = cswFilterDelegate.or(filters);

    }

    @Test
    public void testPropertyIsLikeNot() throws JAXBException, SAXException, IOException {
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);

        FilterType filter = cswFilterDelegate.not(propertyIsLikeFilter);

        assertNotNull(filter);
        assertEquals(filter.getLogicOps().getName(), NOT_LOGIC_OPS_NAME);
        UnaryLogicOpType ulot = (UnaryLogicOpType) filter.getLogicOps().getValue();
        assertNotNull(ulot);

    }

    // @Test
    // public void testConvertWktFromLonLatToLatLon_NonNormalizedWkt() {
    // String convertedWkt =
    // cswFilterDelegate.convertWktToLatLonOrdering(nonNormalizedGeometryCollectionWkt);
    // LOGGER.debug("Original WKT : {}", nonNormalizedGeometryCollectionWkt);
    // LOGGER.debug("Converted WKT: {}", convertedWkt);
    // assertEquals(convertedAndNormalizedGeometryCollectionWkt, convertedWkt);
    // }

    @Test
    public void testIntersectsPropertyAnyGeo() throws JAXBException, SAXException, IOException {
        String propName = Metacard.ANY_GEO;
        FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(intersectsPolygonXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testIntersectsPropertyDctSpatial() throws JAXBException, SAXException, IOException {
        String propName = CswConstants.SPATIAL_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(intersectsPolygonXmlPropertyDctSpatial, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxPolygon() throws JAXBException, SAXException,
        IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(intersectsPolygonXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testIntersectsFallbackToBBoxPropertyOwsBoundingBox() throws JAXBException,
        SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(
                getMockFilterCapabilitiesForSpatialFallback(
                        Arrays.asList(SpatialOperatorNameType.BBOX), Arrays.asList("Envelope")),
                initCswSourceConfiguration(false, CswRecordMetacardType.CSW_TYPE));
        FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalizeWhitespace(true);
        Diff diff = XMLUnit.compareXML(bboxXmlPropertyOwsBoundingBox, xml);
        assertTrue("XML Similar", diff.similar());
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testIntersectsFallbackToNotDisjointPropertyOwsBoundingBox() throws JAXBException,
        SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        CswSourceConfiguration cswSourceConfiguration = new CswSourceConfiguration();
        cswSourceConfiguration.setContentTypeMapping(CswRecordMetacardType.CSW_TYPE);
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(
                getMockFilterCapabilitiesForSpatialFallback(
                        Arrays.asList(SpatialOperatorNameType.DISJOINT), Arrays.asList("Polygon")),
                initCswSourceConfiguration(false, CswRecordMetacardType.CSW_TYPE));
        FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(notDisjointPolygonXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    /**
     * In the following case, when DWithin falls back to Intersects, the pointWkt gets turned into a
     * linear ring ("circular" polygon) with radius "distance" (the buffer). In this case, the CSW
     * endpoint only supports "Envelope" (its spatial capabilities), so we fall back from "Geometry"
     * to "Envelope" (the next best choice) and create an envelope around the linear ring. So, the
     * resulting filter should contain an envelope that bounds the linear ring.
     */
    @Test
    public void testDWitinFallbackToIntersectsEnvelopeIntersectsCswGeometryPropertyOwsBoundingBox()
        throws JAXBException, SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(
                getMockFilterCapabilitiesForSpatialFallback(
                        Arrays.asList(SpatialOperatorNameType.INTERSECTS),
                        Arrays.asList("Envelope")), initCswSourceConfiguration(false, CswRecordMetacardType.CSW_TYPE));
        FilterType filterType = cswFilterDelegate.dwithin(propName, pointWkt, distance);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(dWithinFallbackToIntersects1, xml);
        LOGGER.debug(writer.toString());
    }

    /**
     * In the following case, when DWithin falls back to Intersects, the pointWkt gets turned into a
     * linear ring ("circular" polygon) with radius "distance" (the buffer). In this case, the CSW
     * endpoint supports "Polygon" (its spatial capabilities), so the resulting filter should
     * contain the linear ring.
     */
    @Test
    public void testDWitinFallbackToIntersectsPolygonIntersectsCswGeometryPropertyOwsBoundingBox()
        throws JAXBException, SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(
                getMockFilterCapabilitiesForSpatialFallback(
                        Arrays.asList(SpatialOperatorNameType.INTERSECTS), Arrays.asList("Polygon")),
                initCswSourceConfiguration(false, CswRecordMetacardType.CSW_TYPE));
        FilterType filterType = cswFilterDelegate.dwithin(propName, pointWkt, distance);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        /**
         * See http://xmlunit.sourceforge.net/userguide/html/ar01s03.html Section: 3.8.1 Whitespace
         * Handling
         * 
         * If you set XMLUnit.setNormalizeWhitespace to true then XMLUnit will replace any kind of
         * whitespace found in character content with a SPACE character and collapse consecutive
         * whitespace characters to a single SPACE. It will also trim the resulting character
         * content on both ends.
         * 
         * If I don't set this, the unit test fails as it complains that the content of
         * <ns6:coordinates>...</ns6:coordinates> in the control xml doesn't match that of the test
         * xml.
         */
        XMLUnit.setNormalizeWhitespace(true);
        assertXMLEqual(dWithinFallbackToIntersects2, xml);
        LOGGER.debug(xml);
    }

    @Test
    public void testIntersectsUsingPolygonAndEnvelopePropertyOwsBoundingBox() throws JAXBException,
        SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(
                getMockFilterCapabilitiesForSpatialFallback(
                        Arrays.asList(SpatialOperatorNameType.INTERSECTS),
                        Arrays.asList("Envelope")), initCswSourceConfiguration(false, CswRecordMetacardType.CSW_TYPE));
        FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(intersectsEnvelopeXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIntersectsUnsupportedOperation() throws JAXBException, SAXException,
        IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(null, initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE));
        cswFilterDelegate.intersects(propName, polygonWkt);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBeyondUnsupportedOperation() throws JAXBException, SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(null, initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE));
        cswFilterDelegate.beyond(propName, polygonWkt, distance);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDWithinUnsupportedOperation() throws JAXBException, SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(null, initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE));
        cswFilterDelegate.dwithin(propName, polygonWkt, distance);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testContainsUnsupportedOperation() throws JAXBException, SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(null, initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE));
        cswFilterDelegate.contains(propName, polygonWkt);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCrossesUnsupportedOperation() throws JAXBException, SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(null, initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE));
        cswFilterDelegate.crosses(propName, polygonWkt);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDisjointUnsupportedOperation() throws JAXBException, SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(null, initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE));
        cswFilterDelegate.disjoint(propName, polygonWkt);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testOverlapsUnsupportedOperation() throws JAXBException, SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(null, initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE));
        cswFilterDelegate.overlaps(propName, polygonWkt);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testTouchesUnsupportedOperation() throws JAXBException, SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(null, initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE));
        cswFilterDelegate.touches(propName, polygonWkt);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testWithinUnsupportedOperation() throws JAXBException, SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(null, initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE));
        cswFilterDelegate.within(propName, polygonWkt);
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxPoint() throws JAXBException, SAXException,
        IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, pointWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(intersectsPointXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    /**
     * If the CswSource is in its default configuration for coord order (LAT/LON), verify that the
     * coords in the outgoing filter are converted to LAT/LON. Remember, incoming WKT is in LON/LAT.
     */
    @Test
    public void testIntersectsPolygonLonLatIsConvertedToLatLon() throws JAXBException,
        SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(intersectsPolygonXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    /**
     * If the CswSource is configured for LON/LAT, verify that the coords in the outgoing filter are
     * kept as-is. Remember, incoming WKT is in LON/LAT.
     */
    @Test
    public void testIntersectsPolygonLonLatIsKeptAsLonLat() throws JAXBException, SAXException,
        IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = this.initDefaultCswFilterDelegate(initCswSourceConfiguration(true,
                CswRecordMetacardType.CSW_TYPE));
        FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(intersectsPolygonXmlPropertyOwsBoundingBoxLonLatIsKeptAsLonLat, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxLineString() throws JAXBException,
        SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, lineStringWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(intersectsLineStringXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxMultiPolygon() throws JAXBException,
        SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, multiPolygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(intersectsMultiPolygonXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxMultiPoint() throws JAXBException,
        SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, multiPointWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(intersectsMultiPointXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxMultiLineString() throws JAXBException,
        SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, multiLineStringWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(intersectsMultiLineStringXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testCrossesPropertyOwsBoundingBoxPolygon() throws JAXBException, SAXException,
        IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.crosses(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(crossesPolygonXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testWithinPropertyOwsBoundingBoxPolygon() throws JAXBException, SAXException,
        IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.within(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(withinPolygonXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testContainsPropertyOwsBoundingBoxPolygon() throws JAXBException, SAXException,
        IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.contains(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(containsPolygonXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testWithinFallbackToContainsPropertyOwsBoundingBox() throws JAXBException,
        SAXException, IOException {
        String propName = CswConstants.BBOX_PROP;
        CswFilterDelegate cswFilterDelegate = initCswFilterDelegate(
                getMockFilterCapabilitiesForSpatialFallback(
                        Arrays.asList(SpatialOperatorNameType.CONTAINS), Arrays.asList("Polygon")),
                initCswSourceConfiguration(false, CswRecordMetacardType.CSW_TYPE));
        FilterType filterType = cswFilterDelegate.within(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(containsPolygonXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testBeyondPropertyOwsBoundingBoxPoint() throws JAXBException, SAXException,
        IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.beyond(propName, pointWkt, distance);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(beyondPointXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testDWithinPropertyOwsBoundingBoxPolygon() throws JAXBException, SAXException,
        IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.dwithin(propName, polygonWkt, distance);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(dwithinPolygonXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testTouchesPropertyOwsBoundingBoxPolygon() throws JAXBException, SAXException,
        IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.touches(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(touchesPolygonXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testOverlapsPropertyOwsBoundingBoxPolygon() throws JAXBException, SAXException,
        IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.overlaps(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(overlapsPolygonXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Test
    public void testDisjointPropertyOwsBoundingBoxPolygon() throws JAXBException, SAXException,
        IOException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.disjoint(propName, polygonWkt);
        Writer writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(disjointPolygonXmlPropertyOwsBoundingBox, xml);
        LOGGER.debug(writer.toString());
    }

    @Ignore
    @Test
    public void testWktToJaxbConversion() throws JAXBException, SAXException, IOException,
        com.vividsolutions.jts.io.ParseException {
        String wkt = "POLYGON ((30 -10, 30 30, 10 30, 10 -10, 30 -10))";
        WKTReader wktReader = new WKTReader();
        Geometry geometry = wktReader.read(wkt);
        GMLWriter writer = new GMLWriter();
        writer.setNamespace(true);
        String xml = writer.write(geometry);
        LOGGER.debug("\nxml:\n {}", xml);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Reader reader = new StringReader(xml);
        @SuppressWarnings("unchecked")
        JAXBElement<PolygonType> jaxbPolygonElement = (JAXBElement<PolygonType>) unmarshaller
                .unmarshal(reader);
        PolygonType polygon = jaxbPolygonElement.getValue();
        LinearRingType linearRing = (LinearRingType) polygon.getExterior().getValue().getRing()
                .getValue();
        CoordinatesType coordsType = linearRing.getCoordinates();
        LOGGER.debug("coords: {}", coordsType.getValue().trim());
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
        intersectsSpatialOperator.setName(SpatialOperatorNameType.INTERSECTS);
        spatialOperatorList.add(intersectsSpatialOperator);
        SpatialOperatorType bboxSpatialOperator = new SpatialOperatorType();
        bboxSpatialOperator.setName(SpatialOperatorNameType.BBOX);
        spatialOperatorList.add(bboxSpatialOperator);
        SpatialOperatorType crossesSpatialOperator = new SpatialOperatorType();
        crossesSpatialOperator.setName(SpatialOperatorNameType.CROSSES);
        spatialOperatorList.add(crossesSpatialOperator);
        SpatialOperatorType withinSpatialOperator = new SpatialOperatorType();
        withinSpatialOperator.setName(SpatialOperatorNameType.WITHIN);
        spatialOperatorList.add(withinSpatialOperator);
        SpatialOperatorType containsSpatialOperator = new SpatialOperatorType();
        containsSpatialOperator.setName(SpatialOperatorNameType.CONTAINS);
        spatialOperatorList.add(containsSpatialOperator);
        SpatialOperatorType beyondSpatialOperator = new SpatialOperatorType();
        beyondSpatialOperator.setName(SpatialOperatorNameType.BEYOND);
        spatialOperatorList.add(beyondSpatialOperator);
        SpatialOperatorType dwithinSpatialOperator = new SpatialOperatorType();
        dwithinSpatialOperator.setName(SpatialOperatorNameType.D_WITHIN);
        spatialOperatorList.add(dwithinSpatialOperator);
        SpatialOperatorType disjointSpatialOperator = new SpatialOperatorType();
        disjointSpatialOperator.setName(SpatialOperatorNameType.DISJOINT);
        spatialOperatorList.add(disjointSpatialOperator);
        SpatialOperatorType overlapsSpatialOperator = new SpatialOperatorType();
        overlapsSpatialOperator.setName(SpatialOperatorNameType.OVERLAPS);
        spatialOperatorList.add(overlapsSpatialOperator);
        SpatialOperatorType touchesSpatialOperator = new SpatialOperatorType();
        touchesSpatialOperator.setName(SpatialOperatorNameType.TOUCHES);
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
            // QName polygonQName = new QName(nameSpaceUri, "Polygon", prefix);
            QName polygonQName = new QName(nameSpaceUri, geometry, prefix);
            mockGeometryOperands.add(polygonQName);
        }

        // QName polygonQName = new QName(nameSpaceUri, "Polygon", prefix);
        // mockGeometryOperands.add(polygonQName);
        // QName pointQName = new QName(nameSpaceUri, "Point", prefix);
        // mockGeometryOperands.add(pointQName);
        // QName lineStringQName = new QName(nameSpaceUri, "LineString", prefix);
        // mockGeometryOperands.add(lineStringQName);
        // QName multiPolygonQName = new QName(nameSpaceUri, "MultiPolygon", prefix);
        // mockGeometryOperands.add(multiPolygonQName);
        // QName multiPointQName = new QName(nameSpaceUri, "MultiPoint", prefix);
        // mockGeometryOperands.add(multiPointQName);
        // QName multiLineStringQName = new QName(nameSpaceUri, "MultiLineString", prefix);
        // mockGeometryOperands.add(multiLineStringQName);
        // QName envelopeQName = new QName(nameSpaceUri, "Envelope", prefix);
        // mockGeometryOperands.add(envelopeQName);

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

    private JAXBElement<FeatureIdType> getFeatureIdTypeJaxBElement(FeatureIdType fidType) {
        JAXBElement<FeatureIdType> fidTypeJaxbElement = new JAXBElement<FeatureIdType>(new QName(
                "http://www.opengis.net/ogc", "FeatureID"), FeatureIdType.class, fidType);
        return fidTypeJaxbElement;
    }

    private Date getDate() {
        // String dateString = "Thu Jun 11 10:36:52 MST 2002";
        String dateString = "Jun 11 2002";
        // SimpleDateFormat formatter = new
        // SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy");
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d yyyy");
        Date date = null;
        try {
            date = formatter.parse(dateString);
        } catch (ParseException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return date;
    }

    private String getPropertyIsEqualToXmlWithDate(Date date) {
        String propertyIsEqualToXmlWithDate = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<ns3:Filter xmlns:ns2=\"http://www.opengis.net/ows\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
                + "<ns3:PropertyIsEqualTo matchCase=\"false\">"
                + "<ns3:PropertyName>"
                + DEFAULT_PROPERTY_NAME
                + "</ns3:PropertyName>"
                + "<ns3:Literal>"
                + convertDateToIso8601Format(date)
                + "</ns3:Literal>"
                + "</ns3:PropertyIsEqualTo>"
                + "</ns3:Filter>";

        return propertyIsEqualToXmlWithDate;
    }

    private DateTime convertDateToIso8601Format(Date inputDate) {
        DateTime outputDate = new DateTime(inputDate);
        return outputDate;
    }

    private static JAXBContext initJaxbContext() {
        JAXBContext jaxbContext = null;

        // JAXB context path
        // "net.opengis.cat.csw.v_2_0_2:net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1:net.opengis.ows.v_1_0_0"
        String contextPath = StringUtils.join(new String[] {CswConstants.OGC_CSW_PACKAGE,
            CswConstants.OGC_FILTER_PACKAGE, CswConstants.OGC_GML_PACKAGE,
            CswConstants.OGC_OWS_PACKAGE}, ":");

        try {
            LOGGER.debug("Creating JAXB context with context path: {}", contextPath);
            jaxbContext = JAXBContext.newInstance(contextPath,
                    CswJAXBElementProvider.class.getClassLoader());
        } catch (JAXBException e) {
            LOGGER.error("Unable to create JAXB context using contextPath: {}", contextPath, e);
        }

        return jaxbContext;
    }

    private CswFilterDelegate initDefaultCswFilterDelegate(CswSourceConfiguration cswSourceConfiguration) {

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

    private CswSourceConfiguration initCswSourceConfiguration(boolean isLonLatOrder, String contentType) {
        CswSourceConfiguration cswSourceConfiguration = new CswSourceConfiguration();
        cswSourceConfiguration.setIsLonLatOrder(isLonLatOrder);
        cswSourceConfiguration.setContentTypeMapping(contentType);
        return cswSourceConfiguration;
    }

    private CswSourceConfiguration initCswSourceConfiguration(boolean isLonLatOrder, String contentType, String effectiveDateMapping, String createdDateMapping, String modifiedDateMapping) {
        CswSourceConfiguration cswSourceConfiguration = new CswSourceConfiguration();
        cswSourceConfiguration.setIsLonLatOrder(isLonLatOrder);
        cswSourceConfiguration.setContentTypeMapping(contentType);
        cswSourceConfiguration.setEffectiveDateMapping(effectiveDateMapping);
        cswSourceConfiguration.setCreatedDateMapping(createdDateMapping);
        cswSourceConfiguration.setModifiedDateMapping(modifiedDateMapping);
        return cswSourceConfiguration;
    }

}
