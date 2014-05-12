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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import net.opengis.filter.v_1_1_0.ComparisonOperatorType;
import net.opengis.filter.v_1_1_0.ComparisonOperatorsType;
import net.opengis.filter.v_1_1_0.FilterCapabilities;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.GeometryOperandsType;
import net.opengis.filter.v_1_1_0.LogicalOperators;
import net.opengis.filter.v_1_1_0.ScalarCapabilitiesType;
import net.opengis.filter.v_1_1_0.SpatialCapabilitiesType;
import net.opengis.filter.v_1_1_0.SpatialOperatorNameType;
import net.opengis.filter.v_1_1_0.SpatialOperatorType;
import net.opengis.filter.v_1_1_0.SpatialOperatorsType;
import net.opengis.ows.v_1_0_0.DomainType;
import net.opengis.ows.v_1_0_0.Operation;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswJAXBElementProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat; 
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.source.UnsupportedQueryException;

public class TestCswCqlFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCswCqlFilter.class);

    private final CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(initCswSourceConfiguration(
            true, CswRecordMetacardType.CSW_TYPE));

    private static final String DEFAULT_PROPERTY_NAME = "title";
    
    private static final String EQUALS = "=";

    private static final String SPACE = " ";

    private static final String LESS_THAN = "<";

    private static final String GREATER_THAN = ">";

    private static final String NEGATE = "!";

    private static final String NOT = "NOT";

    private static final String BETWEEN = "BETWEEN";

    private static final String AND = "AND";

    private static final String OR = "OR";

    private static final String LIKE = "LIKE";

    private static final String OPEN_PARAN = "(";

    private static final String CLOSE_PARAN = ")";
    
    private static final String COMMA = ",";
    
    private static final String WITHIN = "WITHIN(";
    
    private static final String DWITHIN = "DWITHIN(";

    private static final String INTERSECTS = "INTERSECTS(";
    
    private static final String CROSSES = "CROSSES(";

    private static final String BEYOND = "BEYOND(";

    private static final String DISJOINT = "DISJOINT(";

    private static final String CONTAINS = "CONTAINS(";
    
    private static final String OVERLAPS = "OVERLAP(";

    private static final String TOUCHES = "TOUCH(";
    
    private static final String IS_NULL = "IS NULL";

    private static final String ANY_TEXT = "AnyText";

    private static final String ONE = "'1'";

    private static final String ONE_DOT_ZERO = "'1.0'";

    private static final String TRUE = "'true'";

    private static final String BAR = "'*bar*'";

    private static final String SINGLE_QUOTE = "'";

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

    private final String likeLiteral = "*bar*";

    private final String polygonWkt = "POLYGON ((30 -10, 30 30, 10 30, 10 -10, 30 -10))";

    private final String pointWkt = "POINT (30 30)";

    private final String lineStringWkt = "LINESTRING (30 -10, 30 30, 10 30, 10 -10)";

    private final String multiPolygonWkt = "MULTIPOLYGON (((40 40, 20 45, 45 30, 40 40)), ((20 35, 45 20, 30 5, 10 10, 10 30, 20 35), (30 20, 20 25, 20 15, 30 20)))";

    private final String multiPointWkt = "MULTIPOINT ((10 40), (40 30), (20 20), (30 10))";

    private final String multiLineStringWkt = "MULTILINESTRING ((10 10, 20 20, 10 40), (40 40, 30 30, 40 20, 30 10))";

    private final double distance = 123.456;

    private static final String REPLACE_START_DATE = "REPLACE_START_DATE";

    private static final String REPLACE_END_DATE = "REPLACE_END_DATE";

    private static final String REPLACE_TEMPORAL_PROPERTY = "REPLACE_TEMPORAL_PROPERTY";

    private final String during = REPLACE_TEMPORAL_PROPERTY + SPACE + BETWEEN + SPACE
            + SINGLE_QUOTE + REPLACE_START_DATE + SINGLE_QUOTE + SPACE + AND + SPACE + SINGLE_QUOTE
            + REPLACE_END_DATE + SINGLE_QUOTE;

    private final String propertyIsEqualTo = DEFAULT_PROPERTY_NAME + SPACE + EQUALS + SPACE + ONE;

    private final String propertyIsEqualToWithDecimal = DEFAULT_PROPERTY_NAME + SPACE + EQUALS
            + SPACE + ONE_DOT_ZERO;

    private final String propertyIsEqualToAnyText = ANY_TEXT + SPACE + EQUALS + SPACE + ONE;

    private final String propertyIsEqualToContentType = "type" + SPACE + EQUALS + SPACE
            + "'myType'";

    private final String propertyIsEqualToWithDate = getPropertyIsEqualToWithDate(getDate());

    private final String propertyIsEqualToWithBoolean = DEFAULT_PROPERTY_NAME + SPACE + EQUALS
            + SPACE + TRUE;

    private final String propertyIsNotEqualTo = DEFAULT_PROPERTY_NAME + SPACE + NEGATE + EQUALS
            + SPACE + ONE;

    private final String propertyIsNotEqualToWithDecimal = DEFAULT_PROPERTY_NAME + SPACE + NEGATE
            + EQUALS + SPACE + ONE_DOT_ZERO;

    private final String propertyIsNotEqualToAnyText = CswConstants.ANY_TEXT + SPACE + NEGATE + EQUALS
            + SPACE + ONE;

    private final String propertyIsNotEqualToWithBoolean = DEFAULT_PROPERTY_NAME + SPACE + NEGATE
            + EQUALS + SPACE + TRUE;

    private final String propertyIsGreaterThan = DEFAULT_PROPERTY_NAME + SPACE + GREATER_THAN
            + SPACE + ONE;

    private final String propertyIsGreaterThanWithDecimal = DEFAULT_PROPERTY_NAME + SPACE
            + GREATER_THAN + SPACE + ONE_DOT_ZERO;

    private final String propertyIsGreaterThanAnyText = CswConstants.ANY_TEXT + SPACE
            + GREATER_THAN + SPACE + ONE;

    private final String propertyIsGreaterThanOrEqualTo = DEFAULT_PROPERTY_NAME + SPACE
            + GREATER_THAN + EQUALS + SPACE + ONE;

    private final String propertyIsGreaterThanOrEqualToWithDecimal = DEFAULT_PROPERTY_NAME + SPACE
            + GREATER_THAN + EQUALS + SPACE + ONE_DOT_ZERO;

    private final String propertyIsGreaterThanOrEqualToAnyText = CswConstants.ANY_TEXT + SPACE
            + GREATER_THAN + EQUALS + SPACE + ONE;

    private final String propertyIsLessThan = DEFAULT_PROPERTY_NAME + SPACE + LESS_THAN + SPACE
            + ONE;

    private final String propertyIsLessThanWithDecimal = DEFAULT_PROPERTY_NAME + SPACE + LESS_THAN
            + SPACE + ONE_DOT_ZERO;

    private final String propertyIsLessThanAnyText = CswConstants.ANY_TEXT + SPACE + LESS_THAN
            + SPACE + ONE;

    private final String propertyIsLessThanOrEqualTo = DEFAULT_PROPERTY_NAME + SPACE + LESS_THAN
            + EQUALS + SPACE + ONE;

    private final String propertyIsLessThanOrEqualToWithDecimal = DEFAULT_PROPERTY_NAME + SPACE
            + LESS_THAN + EQUALS + SPACE + ONE_DOT_ZERO;

    private final String propertyIsLessThanOrEqualToAnyText = CswConstants.ANY_TEXT + SPACE
            + LESS_THAN + EQUALS + SPACE + ONE;

    private final String propertyIsBetween = DEFAULT_PROPERTY_NAME + SPACE + BETWEEN + SPACE
            + "'5'" + SPACE + AND + SPACE + "'15'";

    private final String propertyIsBetweenWithDecimal = DEFAULT_PROPERTY_NAME + SPACE + BETWEEN
            + SPACE + "'5.0'" + SPACE + AND + SPACE + "'15.0'";

    private final String propertyIsNull = DEFAULT_PROPERTY_NAME + SPACE + IS_NULL; 

    private final String propertyIsLike = DEFAULT_PROPERTY_NAME + SPACE + LIKE + SPACE + BAR;

    private final String propertyIsLikeAnyText = CswConstants.ANY_TEXT + SPACE + LIKE + SPACE + BAR;

    private final String orComparisonOps = OPEN_PARAN + propertyIsEqualToWithBoolean + SPACE + OR
            + SPACE + propertyIsLike + CLOSE_PARAN;

    private final String intersectsPolygonPropertyOwsBoundingBox = INTERSECTS
            + CswConstants.BBOX_PROP + COMMA + SPACE + polygonWkt + CLOSE_PARAN;

    private final String intersectsPolygonPropertyDctSpatial = INTERSECTS
            + CswConstants.SPATIAL_PROP + COMMA + SPACE + polygonWkt + CLOSE_PARAN;

    private final String intersectsPointPropertyOwsBoundingBox = INTERSECTS
            + CswConstants.BBOX_PROP + COMMA + SPACE + pointWkt + CLOSE_PARAN;

    private final String intersectsLineStringPropertyOwsBoundingBox = INTERSECTS
            + CswConstants.BBOX_PROP + COMMA + SPACE + lineStringWkt + CLOSE_PARAN;

    private final String intersectsMultiPolygonPropertyOwsBoundingBox = INTERSECTS
            + CswConstants.BBOX_PROP + COMMA + SPACE + multiPolygonWkt + CLOSE_PARAN;

    private final String intersectsMultiPointPropertyOwsBoundingBox = INTERSECTS
            + CswConstants.BBOX_PROP + COMMA + SPACE + multiPointWkt + CLOSE_PARAN;

    private final String intersectsMultiLineStringPropertyOwsBoundingBox = INTERSECTS
            + CswConstants.BBOX_PROP + COMMA + SPACE + multiLineStringWkt + CLOSE_PARAN;

    private final String crossesPolygonPropertyOwsBoundingBox = CROSSES + CswConstants.BBOX_PROP
            + COMMA + SPACE + polygonWkt + CLOSE_PARAN;

    private final String withinPolygonPropertyOwsBoundingBox = WITHIN + CswConstants.BBOX_PROP
            + COMMA + SPACE + polygonWkt + CLOSE_PARAN;

    private final String containsPolygonXmlPropertyOwsBoundingBox = CONTAINS
            + CswConstants.BBOX_PROP + COMMA + SPACE + polygonWkt + CLOSE_PARAN;

    private final String disjointPolygonXmlPropertyOwsBoundingBox = DISJOINT
            + CswConstants.BBOX_PROP + COMMA + SPACE + polygonWkt + CLOSE_PARAN;

    private final String overlapsPolygonPropertyOwsBoundingBox = OVERLAPS + CswConstants.BBOX_PROP
            + COMMA + SPACE + polygonWkt + CLOSE_PARAN;

    private final String touchesPolygonPropertyOwsBoundingBox = TOUCHES + CswConstants.BBOX_PROP
            + COMMA + SPACE + polygonWkt + CLOSE_PARAN;

    private final String beyondPointPropertyOwsBoundingBox = BEYOND + CswConstants.BBOX_PROP
            + COMMA + SPACE + pointWkt + CLOSE_PARAN;

    private final String dwithinPolygonPropertyOwsBoundingBox = DWITHIN + CswConstants.BBOX_PROP
            + COMMA + SPACE + polygonWkt + COMMA + SPACE + distance + COMMA + SPACE
            + CswConstants.METERS + CLOSE_PARAN;

    private final String orLogicOps = OPEN_PARAN + NOT + SPACE + OPEN_PARAN + propertyIsLike
            + CLOSE_PARAN + SPACE + OR + SPACE + propertyIsEqualToWithBoolean + CLOSE_PARAN;

    private final String orSpatialOps = OPEN_PARAN + DWITHIN + CswConstants.BBOX_PROP + COMMA
            + SPACE + pointWkt + COMMA + SPACE + distance + COMMA + SPACE + CswConstants.METERS
            + CLOSE_PARAN + SPACE + OR + SPACE + propertyIsLike + CLOSE_PARAN;

    private final String andComparisonOps = OPEN_PARAN + propertyIsEqualToWithBoolean + SPACE + AND
            + SPACE + propertyIsLike + CLOSE_PARAN;

    private final String andLogicOps = OPEN_PARAN + NOT + SPACE + OPEN_PARAN + propertyIsLike
            + CLOSE_PARAN + SPACE + AND + SPACE + propertyIsEqualToWithBoolean + CLOSE_PARAN;

    private final String andSpatialOps = OPEN_PARAN + DWITHIN + CswConstants.BBOX_PROP + COMMA
            + SPACE + pointWkt + COMMA + SPACE + distance + COMMA + SPACE + CswConstants.METERS
            + CLOSE_PARAN + SPACE + OR + SPACE + propertyIsLike + CLOSE_PARAN;

    private static final String cswRecordId = "cswRecord.1234";

    private final String featureIdCql = CswRecordMetacardType.CSW_IDENTIFIER + SPACE + EQUALS
            + SPACE + SINGLE_QUOTE + cswRecordId + SINGLE_QUOTE;

    /**
     * Property is equal to tests
     * 
     * @throws UnsupportedQueryException
     */
    @Test
    public void testPropertyIsEqualToStringLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, stringLiteral,
                isCaseSensitive);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsEqualTo, cqlText);
    }

    /**
     * Verify that when given a non ISO 8601 formatted date, the CswFilterDelegate converts the date
     * to ISO 8601 format (ie. the xml generated off of the filterType should have an ISO 8601
     * formatted date in it).
     */
    @Test
    public void testPropertyIsEqualToDateLiteral() throws UnsupportedQueryException {
        LOGGER.debug("Input date: {}", date);
        LOGGER.debug("ISO 8601 formatted date: {}", convertDateToIso8601Format(getDate()));
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, date);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsEqualToWithDate, cqlText);
    }

    @Test
    public void testPropertyIsEqualToStringLiteralAnyText() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyNameAnyText,
                stringLiteral, isCaseSensitive);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsEqualToAnyText, cqlText);
    }

    @Test
    public void testDuring() throws UnsupportedQueryException {

        DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
        DateTime endDate =  new DateTime(2013, 12, 31, 0, 0, 0, 0);
        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE, effectiveDateMapping, createdDateMapping, modifiedDateMapping);

        CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
        FilterType filterType = cswFilterDelegate.during(propertyNameModified,
                startDate.toCalendar(null).getTime(), endDate.toCalendar(null).getTime());
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);

        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String startDateStr = fmt.print(startDate);
        String endDateStr = fmt.print(endDate);
        String testResponse = during.replace(REPLACE_START_DATE, startDateStr).replace(REPLACE_END_DATE, endDateStr).replace(REPLACE_TEMPORAL_PROPERTY, modifiedDateMapping);

        assertEquals(testResponse, cqlText);

    }

    @Test
    public void testDuringAlteredEffectiveDateMapping() throws UnsupportedQueryException {

        DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
        DateTime endDate =  new DateTime(2013, 12, 31, 0, 0, 0, 0);

        String replacedTemporalProperty = "myEffectiveDate";
        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE, replacedTemporalProperty, createdDateMapping, modifiedDateMapping);

        CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
        FilterType filterType = cswFilterDelegate.during(propertyNameEffective,
                startDate.toCalendar(null).getTime(), endDate.toCalendar(null).getTime());
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String startDateStr = fmt.print(startDate);
        String endDateStr = fmt.print(endDate);
        String testResponse = during.replace(REPLACE_START_DATE, startDateStr).replace(REPLACE_END_DATE, endDateStr).replace(REPLACE_TEMPORAL_PROPERTY, replacedTemporalProperty);
        assertEquals(testResponse, cqlText);
    }

    @Test
    public void testDuringAlteredCreatedDateMapping() throws UnsupportedQueryException {

        DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
        DateTime endDate =  new DateTime(2013, 12, 31, 0, 0, 0, 0);

        String replacedTemporalProperty = "myCreatedDate";
        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE, effectiveDateMapping, replacedTemporalProperty, modifiedDateMapping);

        CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
        FilterType filterType = cswFilterDelegate.during(propertyNameCreated,
                startDate.toCalendar(null).getTime(), endDate.toCalendar(null).getTime());
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);

        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String startDateStr = fmt.print(startDate);
        String endDateStr = fmt.print(endDate);
        String testResponse = during.replace(REPLACE_START_DATE, startDateStr).replace(REPLACE_END_DATE, endDateStr).replace(REPLACE_TEMPORAL_PROPERTY, replacedTemporalProperty);
        assertEquals(testResponse, cqlText);
    }

    @Test
    public void testDuringAlteredModifiedDateMapping() throws UnsupportedQueryException {

        DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
        DateTime endDate =  new DateTime(2013, 12, 31, 0, 0, 0, 0);

        String replacedTemporalProperty = "myModifiedDate";
        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE, effectiveDateMapping, createdDateMapping, replacedTemporalProperty);

        CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
        FilterType filterType = cswFilterDelegate.during(propertyNameModified,
                startDate.toCalendar(null).getTime(), endDate.toCalendar(null).getTime());
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);

        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String startDateStr = fmt.print(startDate);
        String endDateStr = fmt.print(endDate);
        String testResponse = during.replace(REPLACE_START_DATE, startDateStr).replace(REPLACE_END_DATE, endDateStr).replace(REPLACE_TEMPORAL_PROPERTY, replacedTemporalProperty);
        assertEquals(testResponse, cqlText);

    }


    @Test
    public void testRelative() throws UnsupportedQueryException {
        long duration = 92000000000L;
        CswSourceConfiguration cswSourceConfiguration = initCswSourceConfiguration(false,
                CswRecordMetacardType.CSW_TYPE, effectiveDateMapping, createdDateMapping, modifiedDateMapping);
        CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
        FilterType filterType = cswFilterDelegate.relative(propertyNameModified,
                duration);

        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);

        String durationCompare = during.replace(REPLACE_START_DATE, "")
                .replace(REPLACE_END_DATE, "")
                .replace(REPLACE_TEMPORAL_PROPERTY, modifiedDateMapping);
        String pattern = "(?i)(')(.+?)(')";
        String compareXml = cqlText.replaceAll(pattern, "''");
        assertEquals(durationCompare, compareXml);
    }

    @Test(expected = UnsupportedQueryException.class)
    public void testPropertyIsEqualToStringLiteralNonQueryableProperty()
        throws UnsupportedQueryException {
        /**
         * See CswRecordMetacardType.java for queryable and non-queryable properties.
         */
        String nonQueryableProperty = Metacard.METADATA;
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(nonQueryableProperty,
                stringLiteral, isCaseSensitive);
        CswCqlTextFilter.getInstance().getCqlText(filterType);
    }

    @Test
    public void testPropertyIsEqualToStringLiteralType() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(Metacard.CONTENT_TYPE,
                contentTypeLiteral, isCaseSensitive);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsEqualToContentType, cqlText);
    }

    @Test
    public void testPropertyIsEqualToIntLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, intLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsEqualToShortLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, shortLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsEqualToLongLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, longLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsEqualToFloatLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, floatLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsEqualToWithDecimal, cqlText);
    }

    @Test
    public void testPropertyIsEqualToDoubleLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, doubleLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsEqualToWithDecimal, cqlText);
    }

    @Test
    public void testPropertyIsEqualToBooleanLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, booleanLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsEqualToWithBoolean, cqlText);
    }

    /**
     * Property is not equal to tests
     */
    @Test
    public void testPropertyIsNotEqualToStringLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, stringLiteral,
                isCaseSensitive);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsNotEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsNotEqualToStringLiteralAnyText() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyNameAnyText,
                stringLiteral, isCaseSensitive);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsNotEqualToAnyText, cqlText);
    }

    @Test
    public void testPropertyIsNotEqualToIntLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, intLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsNotEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsNotEqualToShortLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, shortLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsNotEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsNotEqualToLongLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, longLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsNotEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsNotEqualToFloatLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, floatLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsNotEqualToWithDecimal, cqlText);
    }

    @Test
    public void testPropertyIsNotEqualToDoubleLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, doubleLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsNotEqualToWithDecimal, cqlText);
    }

    @Test
    public void testPropertyIsNotEqualToBooleanLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate
                .propertyIsNotEqualTo(propertyName, booleanLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsNotEqualToWithBoolean, cqlText);
    }

    /**
     * Property is greater than tests
     */
    @Test
    public void testPropertyIsGreaterThanStringLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate
                .propertyIsGreaterThan(propertyName, stringLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsGreaterThan, cqlText);
    }

    @Test
    public void testPropertyIsGreaterThanStringLiteralAnyText() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyNameAnyText,
                stringLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsGreaterThanAnyText, cqlText);
    }

    @Test
    public void testPropertyIsGreaterThanIntLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName, intLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsGreaterThan, cqlText);
    }

    @Test
    public void testPropertyIsGreaterThanShortLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName, shortLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsGreaterThan, cqlText);
    }

    @Test
    public void testPropertyIsGreaterThanLongLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName, longLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsGreaterThan, cqlText);
    }

    @Test
    public void testPropertyIsGreaterThanFloatLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName, floatLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsGreaterThanWithDecimal, cqlText);
    }

    @Test
    public void testPropertyIsGreaterThanDoubleLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate
                .propertyIsGreaterThan(propertyName, doubleLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsGreaterThanWithDecimal, cqlText);
    }

    /**
     * Property is greater than or equal to tests
     */
    @Test
    public void testPropertyIsGreaterThanOrEqualToStringLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                stringLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsGreaterThanOrEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToStringLiteralAnyText()
        throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(
                propertyNameAnyText, stringLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsGreaterThanOrEqualToAnyText, cqlText);
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToIntLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                intLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsGreaterThanOrEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToShortLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                shortLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsGreaterThanOrEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToLongLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                longLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsGreaterThanOrEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToFloatLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                floatLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsGreaterThanOrEqualToWithDecimal, cqlText);
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToDoubleLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName,
                doubleLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsGreaterThanOrEqualToWithDecimal, cqlText);
    }

    /**
     * Property is less than tests
     */
    @Test
    public void testPropertyIsLessThanStringLiteral() throws UnsupportedQueryException {

        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, stringLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLessThan, cqlText);
    }

    @Test
    public void testPropertyIsLessThanStringLiteralAnyText() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyNameAnyText,
                stringLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLessThanAnyText, cqlText);
    }

    @Test
    public void testPropertyIsLessThanIntLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, intLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLessThan, cqlText);
    }

    @Test
    public void testPropertyIsLessThanShortLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, shortLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLessThan, cqlText);
    }

    @Test
    public void testPropertyIsLessThanLongLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, longLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLessThan, cqlText);
    }

    @Test
    public void testPropertyIsLessThanFloatLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, floatLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLessThanWithDecimal, cqlText);
    }

    @Test
    public void testPropertyIsLessThanDoubleLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, doubleLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLessThanWithDecimal, cqlText);
    }

    /**
     * Property is less than or equal to tests
     */
    @Test
    public void testPropertyIsLessThanOrEqualToStringLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                stringLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLessThanOrEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsLessThanOrEqualToStringLiteralAnyText()
        throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyNameAnyText,
                stringLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLessThanOrEqualToAnyText, cqlText);
    }

    @Test
    public void testPropertyIsLessThanOrEqualToIntLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                intLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLessThanOrEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsLessThanOrEqualToShortLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                intLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLessThanOrEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsLessThanOrEqualToLongLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                shortLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLessThanOrEqualTo, cqlText);
    }

    @Test
    public void testPropertyIsLessThanOrEqualToFloatLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                floatLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLessThanOrEqualToWithDecimal, cqlText);
    }

    @Test
    public void testPropertyIsLessThanOrEqualToDoubleLiteral() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName,
                doubleLiteral);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLessThanOrEqualToWithDecimal, cqlText);
    }

    /**
     * Property is between tests
     */
    @Test
    public void testPropertyBetweenStringLiterals() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsBetween(propertyName,
                stringLowerBoundary, stringUpperBoundary);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsBetween, cqlText);
    }

    @Test
    public void testPropertyBetweenIntLiterals() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsBetween(propertyName, intLowerBoundary,
                intUpperBoundary);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsBetween, cqlText);
    }

    @Test
    public void testPropertyBetweenShortLiterals() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsBetween(propertyName,
                shortLowerBoundary, shortUpperBoundary);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsBetween, cqlText);
    }

    @Test
    public void testPropertyBetweenLongLiterals() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsBetween(propertyName,
                longLowerBoundary, longUpperBoundary);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsBetween, cqlText);
    }

    @Test
    public void testPropertyBetweenFloatLiterals() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsBetween(propertyName,
                floatLowerBoundary, floatUpperBoundary);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsBetweenWithDecimal, cqlText);
    }

    @Test
    public void testPropertyBetweenDoubleLiterals() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsBetween(propertyName,
                doubleLowerBoundary, doubleUpperBoundary);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsBetweenWithDecimal, cqlText);
    }

    @Test
    public void testPropertyNull() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsNull(propertyName);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsNull, cqlText);
    }

    @Test
    public void testPropertyLike() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLike(propertyName, likeLiteral,
                isCaseSensitive);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLike, cqlText);
    }

    @Test
    public void testPropertyLikeAnyText() throws UnsupportedQueryException {
        FilterType filterType = cswFilterDelegate.propertyIsLike(propertyNameAnyText, likeLiteral,
                isCaseSensitive);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(propertyIsLikeAnyText, cqlText);
    }

    @Test
    public void testComparisonOpsOr() throws UnsupportedQueryException {
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);
        FilterType propertyIsEqualFilter = cswFilterDelegate.propertyIsEqualTo(propertyName,
                booleanLiteral);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(propertyIsEqualFilter);
        filters.add(propertyIsLikeFilter);
        FilterType filter = cswFilterDelegate.or(filters);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
        assertEquals(orComparisonOps, cqlText);
    }

    @Test
    public void testLogicOpsFiltersOr() throws UnsupportedQueryException {
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);
        FilterType notFilter = cswFilterDelegate.not(propertyIsLikeFilter);
        FilterType propertyIsEqualFilter = cswFilterDelegate.propertyIsEqualTo(propertyName,
                booleanLiteral);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(notFilter);
        filters.add(propertyIsEqualFilter);
        FilterType filter = cswFilterDelegate.or(filters);

        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
        assertEquals(orLogicOps, cqlText);
    }

    @Test
    public void testSpatialOpsOr() throws UnsupportedQueryException {
        FilterType spatialFilter = cswFilterDelegate.dwithin(CswConstants.BBOX_PROP, pointWkt,
                distance);
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(spatialFilter);
        filters.add(propertyIsLikeFilter);

        FilterType filter = cswFilterDelegate.or(filters);

        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
        assertEquals(orSpatialOps, cqlText);
    }

    @Test
    public void testFeatureId() throws UnsupportedQueryException {
        FilterType filter = cswFilterDelegate.propertyIsEqualTo(Metacard.ID,
                String.valueOf(cswRecordId), false);

        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
        assertEquals(featureIdCql, cqlText);
    }

    @Test
    public void testComparisonOpsAnd() throws UnsupportedQueryException {
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);
        FilterType propertyIsEqualFilter = cswFilterDelegate.propertyIsEqualTo(propertyName,
                booleanLiteral);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(propertyIsEqualFilter);
        filters.add(propertyIsLikeFilter);
        FilterType filter = cswFilterDelegate.and(filters);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
        assertEquals(andComparisonOps, cqlText);
    }

    @Test
    public void testLogicOpsFiltersAnd() throws UnsupportedQueryException {
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(propertyName,
                likeLiteral, isCaseSensitive);
        FilterType notFilter = cswFilterDelegate.not(propertyIsLikeFilter);
        FilterType propertyIsEqualFilter = cswFilterDelegate.propertyIsEqualTo(propertyName,
                booleanLiteral);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(notFilter);
        filters.add(propertyIsEqualFilter);
        FilterType filter = cswFilterDelegate.and(filters);

        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
        assertEquals(andLogicOps, cqlText);
    }

    @Test
    public void testSpatialOpsAnd() throws UnsupportedQueryException {
        FilterType spatialFilter = cswFilterDelegate.dwithin(CswConstants.BBOX_PROP, pointWkt,
                distance);
        FilterType propertyIsLikeFilter = cswFilterDelegate.propertyIsLike(DEFAULT_PROPERTY_NAME,
                likeLiteral, isCaseSensitive);

        List<FilterType> filters = new ArrayList<FilterType>();
        filters.add(spatialFilter);
        filters.add(propertyIsLikeFilter);

        FilterType filter = cswFilterDelegate.or(filters);

        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
        assertEquals(andSpatialOps, cqlText);
    }

    @Test
    public void testIntersectsPropertyAnyGeo() throws UnsupportedQueryException {
        String propName = Metacard.ANY_GEO;
        FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(intersectsPolygonPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testIntersectsPropertyDctSpatial() throws UnsupportedQueryException {
        String propName = CswConstants.SPATIAL_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(intersectsPolygonPropertyDctSpatial, cqlText);
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(intersectsPolygonPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxPoint() throws UnsupportedQueryException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, pointWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(intersectsPointPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testIntersectsPolygonLonLatIsConvertedToLatLon() throws UnsupportedQueryException,
        com.vividsolutions.jts.io.ParseException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(intersectsPolygonPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxLineString() throws UnsupportedQueryException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, lineStringWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(intersectsLineStringPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxMultiPolygon() throws UnsupportedQueryException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, multiPolygonWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(intersectsMultiPolygonPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxMultiPoint() throws UnsupportedQueryException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, multiPointWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(intersectsMultiPointPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testIntersectsPropertyOwsBoundingBoxMultiLineString()
        throws UnsupportedQueryException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.intersects(propName, multiLineStringWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(intersectsMultiLineStringPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testCrossesPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.crosses(propName, polygonWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(crossesPolygonPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testWithinPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException,
        com.vividsolutions.jts.io.ParseException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.within(propName, polygonWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(withinPolygonPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testContainsPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.contains(propName, polygonWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(containsPolygonXmlPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testBeyondPropertyOwsBoundingBoxPoint() throws UnsupportedQueryException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.beyond(propName, pointWkt, distance);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(beyondPointPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testDWithinPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.dwithin(propName, polygonWkt, distance);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(dwithinPolygonPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testTouchesPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.touches(propName, polygonWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(touchesPolygonPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testOverlapsPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.overlaps(propName, polygonWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(overlapsPolygonPropertyOwsBoundingBox, cqlText);
    }

    @Test
    public void testDisjointPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException {
        String propName = CswConstants.BBOX_PROP;
        FilterType filterType = cswFilterDelegate.disjoint(propName, polygonWkt);
        String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
        assertEquals(disjointPolygonXmlPropertyOwsBoundingBox, cqlText);
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

    private Date getDate() {
        String dateString = "Jun 11 2002";
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d yyyy");
        Date date = null;
        try {
            date = formatter.parse(dateString);
        } catch (ParseException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return date;
    }

    private String getPropertyIsEqualToWithDate(Date date) {
        String propertyIsEqualToXmlWithDate = DEFAULT_PROPERTY_NAME + SPACE + EQUALS + SPACE + "'" +convertDateToIso8601Format(date)+"'";
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
