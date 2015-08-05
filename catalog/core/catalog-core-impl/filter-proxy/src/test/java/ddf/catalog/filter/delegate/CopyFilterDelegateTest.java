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
package ddf.catalog.filter.delegate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.text.WKTParser;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.UomOgcMapping;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriod;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.geotools.temporal.object.DefaultPosition;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.Geometry;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.PeriodDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.impl.filter.FuzzyFunction;
import ddf.catalog.source.UnsupportedQueryException;

public class CopyFilterDelegateTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopyFilterDelegateTest.class);

    private static final FilterFactory FF = new FilterFactoryImpl();

    private static final String TEST_PROPERTY_VALUE = "Test";

    private static final PropertyName TEST_PROPERTY = FF.property(TEST_PROPERTY_VALUE);

    private static final String FOO_LITERAL_VALUE = "foo";

    private static final Literal FOO_LITERAL = FF.literal(FOO_LITERAL_VALUE);

    private static final int DAY_IN_MILLISECONDS = 1000 * 60 * 60 * 24;

    private static final Date EPOCH = new Date(0);

    private static final Date EPOCH_PLUS_DAY = new Date(DAY_IN_MILLISECONDS);

    private static final Instant EPOCH_INSTANT = new DefaultInstant(new DefaultPosition(EPOCH));

    private static final Instant EPOCH_PLUS_DAY_INSTANT = new DefaultInstant(
            new DefaultPosition(EPOCH_PLUS_DAY));

    private static final Period EPOCH_DAY_PERIOD = new DefaultPeriod(EPOCH_INSTANT,
            EPOCH_PLUS_DAY_INSTANT);

    private static final PeriodDuration DAY_DURATION = new DefaultPeriodDuration(
            DAY_IN_MILLISECONDS);

    private static final double DISTANCE_10 = 10.0;

    private static final String POLYGON_WKT = "POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))";

    private static final GeometryBuilder GEO_BUILDER = new GeometryBuilder(
            DefaultGeographicCRS.WGS84);

    private static final WKTParser WKT_PARSER = new WKTParser(GEO_BUILDER);

    @Test
    public void testIncludeFilter() {
        Filter filterIn = Filter.INCLUDE;
        FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
        FilterDelegate<Filter> delegate = new CopyFilterDelegate(filterBuilder);
        FilterAdapter fa = new GeotoolsFilterAdapterImpl();

        Filter filterCopy = null;
        try {
            filterCopy = fa.adapt(filterIn, delegate);
        } catch (UnsupportedQueryException e) {
            fail(e.getMessage());
        }

        assertNotNull(filterCopy);
        assertSame(filterIn, filterCopy);
    }

    @Test
    public void testExcludeFilter() {
        Filter filterIn = Filter.EXCLUDE;
        FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
        FilterDelegate<Filter> delegate = new CopyFilterDelegate(filterBuilder);
        FilterAdapter fa = new GeotoolsFilterAdapterImpl();

        Filter filterCopy = null;
        try {
            filterCopy = fa.adapt(filterIn, delegate);
        } catch (UnsupportedQueryException e) {
            fail(e.getMessage());
        }

        assertNotNull(filterCopy);
        assertSame(filterIn, filterCopy);
    }

    @Test
    public void notFilter() {
        Filter filter = FF.equals(TEST_PROPERTY, FOO_LITERAL);

        assertFilterEquals(FF.not(filter));
        assertFilterEquals(FF.not(FF.not(filter)));
    }

    @Test
    public void testAndFilter() {
        Filter filter1 = FF.equals(FF.property("Test1"), FOO_LITERAL);
        Filter filter2 = FF.equals(FF.property("Test2"), FF.literal("bar"));

        assertFilterEquals(FF.and(filter1, filter2));
    }

    @Test
    public void testOrFilter() {
        Filter filter1 = FF.equals(FF.property("Test1"), FOO_LITERAL);
        Filter filter2 = FF.equals(FF.property("Test2"), FF.literal("bar"));

        assertFilterEquals(FF.or(filter1, filter2));
    }

    @Test
    public void testPropertyIsNull() {
        assertFilterEquals(FF.isNull(TEST_PROPERTY));
    }

    @Test
    public void testPropertyIsEqualToStringCaseInsensitive() {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(FOO_LITERAL_VALUE.toUpperCase())));
    }

    @Test
    public void testPropertyIsEqualToStringCaseSensitive() {
        assertFilterEquals(FF.equal(TEST_PROPERTY, FOO_LITERAL, true));
    }

    @Test
    public void testPropertyIsEqualToDate() {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(EPOCH)));
    }

    @Test
    public void testPropertyIsEqualToDateRange() throws Exception {
        assertFilterException(FF.equals(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));
    }

    @Test
    public void testPropertyIsEqualToInt() {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(new Integer(5))));
    }

    @Test
    public void testPropertyIsEqualToLong() {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(new Long(5))));
    }

    @Test
    public void testPropertyIsEqualToShort() {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(new Short((short) 5))));
    }

    @Test
    public void testPropertyIsEqualToFloat() {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(new Float(5.0))));
    }

    @Test
    public void testPropertyIsEqualToDouble() {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(new Double(5.0))));
    }

    @Test
    public void testPropertyIsEqualToBoolean() {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(true)));
    }

    @Test
    public void testPropertyIsEqualToByteArray() {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(new byte[] {5, 6})));
    }

    @Test
    public void testPropertyIsEqualToObject() throws Exception {
        assertFilterException(FF.equals(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }

    @Test
    public void testPropertyIsNotEqualToStringCaseInsensitive() {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(FOO_LITERAL_VALUE.toUpperCase())));
    }

    @Test
    public void testPropertyIsNotEqualToStringCaseSensitive() {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FOO_LITERAL, true));
    }

    @Test
    public void testPropertyIsNotEqualToDate() {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(EPOCH)));
    }

    @Test
    public void testPropertyIsNotEqualToDateRange() throws Exception {
        assertFilterException(FF.notEqual(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));
    }

    @Test
    public void testPropertyIsNotEqualToInt() {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(new Integer(5))));
    }

    @Test
    public void testPropertyIsNotEqualToLong() {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(new Long(5))));
    }

    @Test
    public void testPropertyIsNotEqualToShort() {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(new Short((short) 5))));
    }

    @Test
    public void testPropertyIsNotEqualToFloat() {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(new Float(5.0))));
    }

    @Test
    public void testPropertyIsNotEqualToDouble() {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(new Double(5.0))));
    }

    @Test
    public void testPropertyIsNotEqualToBoolean() {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(true)));
    }

    @Test
    public void testPropertyIsNotEqualToByteArray() {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(new byte[] {5, 6})));
    }

    @Test
    public void testPropertyIsNotEqualToObject() throws Exception {
        assertFilterException(FF.notEqual(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }

    @Test
    public void testPropertyIsGreaterThanString() throws Exception {
        assertFilterException(
                FF.greater(TEST_PROPERTY, FF.literal(FOO_LITERAL_VALUE.toUpperCase())));
    }

    @Test
    public void testPropertyIsGreaterThanDate() throws Exception {
        assertFilterException(FF.greater(TEST_PROPERTY, FF.literal(EPOCH)));
    }

    @Test
    public void testPropertyIsGreaterThanInt() {
        assertFilterEquals(FF.greater(TEST_PROPERTY, FF.literal(new Integer(5))));
    }

    @Test
    public void testPropertyIsGreaterThanLong() {
        assertFilterEquals(FF.greater(TEST_PROPERTY, FF.literal(new Long(5))));
    }

    @Test
    public void testPropertyIsGreaterThanShort() {
        assertFilterEquals(FF.greater(TEST_PROPERTY, FF.literal(new Short((short) 5))));
    }

    @Test
    public void testPropertyIsGreaterThanFloat() {
        assertFilterEquals(FF.greater(TEST_PROPERTY, FF.literal(new Float(5.0))));
    }

    @Test
    public void testPropertyIsGreaterThanDouble() {
        assertFilterEquals(FF.greater(TEST_PROPERTY, FF.literal(new Double(5.0))));
    }

    @Test
    public void testPropertyIsGreaterThanObject() throws Exception {
        assertFilterException(FF.greater(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToString() throws Exception {
        assertFilterException(
                FF.greaterOrEqual(TEST_PROPERTY, FF.literal(FOO_LITERAL_VALUE.toUpperCase())));
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToDate() throws Exception {
        assertFilterException(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(EPOCH)));
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToInt() {
        assertFilterEquals(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Integer(5))));
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToLong() {
        assertFilterEquals(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Long(5))));
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToShort() {
        assertFilterEquals(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Short((short) 5))));
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToFloat() {
        assertFilterEquals(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Float(5.0))));
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToDouble() {
        assertFilterEquals(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Double(5.0))));
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualToObject() throws Exception {
        assertFilterException(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }

    @Test
    public void testPropertyIsLesserThanString() throws Exception {
        assertFilterException(FF.less(TEST_PROPERTY, FF.literal(FOO_LITERAL_VALUE.toUpperCase())));
    }

    @Test
    public void testPropertyIsLesserThanDate() throws Exception {
        assertFilterException(FF.less(TEST_PROPERTY, FF.literal(EPOCH)));
    }

    @Test
    public void testPropertyIsLesserThanInt() {
        assertFilterEquals(FF.less(TEST_PROPERTY, FF.literal(new Integer(5))));
    }

    @Test
    public void testPropertyIsLesserThanLong() {
        assertFilterEquals(FF.less(TEST_PROPERTY, FF.literal(new Long(5))));
    }

    @Test
    public void testPropertyIsLesserThanShort() {
        assertFilterEquals(FF.less(TEST_PROPERTY, FF.literal(new Short((short) 5))));
    }

    @Test
    public void testPropertyIsLesserThanFloat() {
        assertFilterEquals(FF.less(TEST_PROPERTY, FF.literal(new Float(5.0))));
    }

    @Test
    public void testPropertyIsLesserThanDouble() {
        assertFilterEquals(FF.less(TEST_PROPERTY, FF.literal(new Double(5.0))));
    }

    @Test
    public void testPropertyIsLesserThanObject() throws Exception {
        assertFilterException(FF.less(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }

    @Test
    public void testPropertyIsLesserThanOrEqualToString() throws Exception {
        assertFilterException(
                FF.lessOrEqual(TEST_PROPERTY, FF.literal(FOO_LITERAL_VALUE.toUpperCase())));
    }

    @Test
    public void testPropertyIsLesserThanOrEqualToDate() throws Exception {
        assertFilterException(FF.lessOrEqual(TEST_PROPERTY, FF.literal(EPOCH)));
    }

    @Test
    public void testPropertyIsLesserThanOrEqualToInt() {
        assertFilterEquals(FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Integer(5))));
    }

    @Test
    public void testPropertyIsLesserThanOrEqualToLong() {
        assertFilterEquals(FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Long(5))));
    }

    @Test
    public void testPropertyIsLesserThanOrEqualToShort() {
        assertFilterEquals(FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Short((short) 5))));
    }

    @Test
    public void testPropertyIsLesserThanOrEqualToFloat() {
        assertFilterEquals(FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Float(5.0))));
    }

    @Test
    public void testPropertyIsLesserThanOrEqualToDouble() {
        assertFilterEquals(FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Double(5.0))));
    }

    @Test
    public void testPropertyIsLesserThanOrEqualToObject() throws Exception {
        assertFilterException(FF.lessOrEqual(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }

    @Test
    public void testPropertyIsBetweenStrings() throws Exception {
        assertFilterException(FF.between(TEST_PROPERTY, FOO_LITERAL, FF.literal("bar")));
    }

    @Test
    public void testPropertyIsBetweenDates() throws Exception {
        assertFilterException(
                FF.between(TEST_PROPERTY, FF.literal(EPOCH), FF.literal(EPOCH_PLUS_DAY)));
    }

    @Test
    public void testPropertyIsBetweenInts() throws Exception {
        assertFilterEquals(
                FF.between(TEST_PROPERTY, FF.literal(new Integer(1)), FF.literal(new Integer(5))));
    }

    @Test
    public void testPropertyIsBetweenShorts() throws Exception {
        assertFilterEquals(FF.between(TEST_PROPERTY, FF.literal(new Short((short) 1)),
                FF.literal(new Short((short) 5))));
    }

    @Test
    public void testPropertyIsBetweenLongs() throws Exception {
        assertFilterEquals(
                FF.between(TEST_PROPERTY, FF.literal(new Long(1)), FF.literal(new Long(5))));
    }

    @Test
    public void testPropertyIsBetweenFloats() throws Exception {
        assertFilterEquals(
                FF.between(TEST_PROPERTY, FF.literal(new Float(1.0)), FF.literal(new Float(5.0))));
    }

    @Test
    public void testPropertyIsBetweenDoubles() throws Exception {
        assertFilterEquals(FF.between(TEST_PROPERTY, FF.literal(new Double(1.0)),
                FF.literal(new Double(5.0))));
    }

    @Test
    public void testPropertyIsBetweenObjects() throws Exception {
        assertFilterException(FF.between(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3)),
                FF.literal(Arrays.asList(4, 5, 6))));
    }

    @Test
    public void testPropertyIsLikeCaseInsensitive() {
        assertFilterEquals(FF.like(TEST_PROPERTY, FOO_LITERAL_VALUE.toUpperCase()));
    }

    @Test
    public void testPropertyIsLikeCaseSensitive() {
        assertFilterEquals(FF.like(TEST_PROPERTY, FOO_LITERAL_VALUE.toUpperCase(),
                FilterDelegate.WILDCARD_CHAR, FilterDelegate.SINGLE_CHAR,
                FilterDelegate.ESCAPE_CHAR, true));
    }

    @Test
    public void testPropertyIsLikeFuzzy() {
        assertFilterEquals(FF.like(new FuzzyFunction(Arrays.asList((Expression) (TEST_PROPERTY)),
                FF.literal("")), FOO_LITERAL_VALUE));
    }

    @Test
    public void testXpathExists() {
        assertFilterEquals(FF.like(FF.property("//ns:title"), ""));
        // assertFilterEquals(FF.like(FF.property("//ns:title"), "*"));
    }

    @Test
    public void testXpathIsLike() {
        assertFilterEquals(FF.like(FF.property("//ns:title"), "foo*"));
    }

    @Test
    public void testXpathIsLikeCaseSensitive() {
        assertFilterEquals(FF.like(FF.property("//ns:title"), "foo*", FilterDelegate.WILDCARD_CHAR,
                FilterDelegate.SINGLE_CHAR, FilterDelegate.ESCAPE_CHAR, true));
    }

    @Test
    public void testXpathIsFuzzy() {
        assertFilterEquals(
                FF.like(new FuzzyFunction(Arrays.asList((Expression) (FF.property("//ns:title"))),
                                FF.literal("")), "foo*?"));
    }

    @Test
    public void testSpatialBeyond() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals(FF.beyond(TEST_PROPERTY_VALUE, polygonGeometry, DISTANCE_10,
                UomOgcMapping.METRE.name()));
    }

    @Test
    public void testSpatialContains() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals(FF.contains(TEST_PROPERTY_VALUE, polygonGeometry));
    }

    @Test
    public void testSpatialDWithin() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals(FF.dwithin(TEST_PROPERTY_VALUE, polygonGeometry, DISTANCE_10,
                UomOgcMapping.METRE.name()));
    }

    @Test
    public void testSpatialIntersects() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals(FF.intersects(TEST_PROPERTY_VALUE, polygonGeometry));
    }

    @Test
    public void testSpatialWithin() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals(FF.within(TEST_PROPERTY_VALUE, polygonGeometry));
    }

    @Test
    public void testSpatialCrosses() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterException(FF.crosses(TEST_PROPERTY_VALUE, polygonGeometry));
    }

    @Test
    public void testSpatialDisjoint() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterException(FF.disjoint(TEST_PROPERTY_VALUE, polygonGeometry));
    }

    @Test
    public void testSpatialTouches() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterException(FF.touches(TEST_PROPERTY_VALUE, polygonGeometry));
    }

    @Test
    public void testSpatialOverlaps() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterException(FF.overlaps(TEST_PROPERTY_VALUE, polygonGeometry));
    }

    @Test
    public void testTemporalAfterDate() {
        assertFilterEquals(FF.after(TEST_PROPERTY, FF.literal(EPOCH)));
    }

    @Test
    public void testTemporalAfterDateInstant() {
        assertFilterEquals(FF.after(TEST_PROPERTY, FF.literal(EPOCH_INSTANT)));
    }

    @Test
    public void testTemporalAfterDatePeriod() {
        assertFilterEquals(FF.after(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));
    }

    @Test
    public void testTemporalBeforeDate() {
        assertFilterEquals(FF.before(TEST_PROPERTY, FF.literal(EPOCH)));
    }

    @Test
    public void testTemporalBeforeDateInstant() {
        assertFilterEquals(FF.before(TEST_PROPERTY, FF.literal(EPOCH_INSTANT)));
    }

    @Test
    public void testTemporalBeforeDatePeriod() {
        assertFilterEquals(FF.before(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));
    }

    @Test
    public void testTemporalDuringAbsolute() {
        assertFilterEquals(FF.during(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));
    }

    @Test
    public void testTemporalDuringRelative() {
        assertFilterEquals(FF.during(TEST_PROPERTY, FF.literal(DAY_DURATION)));
    }

    @Test
    public void testFilterModification() {
        Filter filterIn = FF.equals(TEST_PROPERTY, FOO_LITERAL);
        FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
        FilterDelegate<Filter> delegate = new FilterModifierDelegate(filterBuilder);
        FilterAdapter fa = new GeotoolsFilterAdapterImpl();

        Filter modifiedFilter = null;
        try {
            modifiedFilter = fa.adapt(filterIn, delegate);
        } catch (UnsupportedQueryException e) {
            fail(e.getMessage());
        }

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("testFeatureType");
        b.add(TEST_PROPERTY_VALUE, String.class);
        b.add("classification", String.class);
        SimpleFeatureType featureType = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        builder.add(FOO_LITERAL_VALUE);
        builder.add("UNCLASS");
        SimpleFeature feature = builder.buildFeature("test");

        assertTrue(modifiedFilter.evaluate(feature));
    }

    private void assertFilterException(Filter filterIn) {
        FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
        FilterDelegate<Filter> delegate = new CopyFilterDelegate(filterBuilder);
        FilterAdapter fa = new GeotoolsFilterAdapterImpl();

        // Explicitly test that an UnsupportedOperationException, thrown by the
        // CopyFilterDelegate, was the root cause of the failure, which is wrapped by
        // the UnsupportedQueryException thrown by the FilterAdapter.
        try {
            fa.adapt(filterIn, delegate);
            fail("Should have gotten an UnsupportedQueryException");
        } catch (UnsupportedQueryException e) {
            assertTrue(e.getCause() instanceof UnsupportedOperationException);
        }
    }

    private void assertFilterEquals(Filter filterIn) {
        FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
        FilterDelegate<Filter> delegate = new CopyFilterDelegate(filterBuilder);
        FilterAdapter fa = new GeotoolsFilterAdapterImpl();

        Filter filterCopy = null;
        try {
            filterCopy = fa.adapt(filterIn, delegate);
        } catch (UnsupportedQueryException e) {
            fail(e.getMessage());
        }

        assertNotNull(filterCopy);

        // Verify object references are different, indicating a copy was made of the filter
        assertNotSame(filterIn, filterCopy);

        assertFilterContentsEqual(filterIn, filterCopy);

        // Verify filter contents (attributes, operands, etc) are identical
        assertThat(filterCopy, is(filterIn));
    }

    private void assertFilterContentsEqual(Filter filterIn, Filter filterCopy) {
        FilterDelegate<String> delegate = new FilterToTextDelegate();
        FilterAdapter fa = new GeotoolsFilterAdapterImpl();

        String filterInString = null;
        String filterCopyString = null;
        try {
            filterInString = fa.adapt(filterIn, delegate);
            filterCopyString = fa.adapt(filterCopy, delegate);
        } catch (UnsupportedQueryException e) {
            fail(e.getMessage());
        }

        LOGGER.debug("filterInString: {}", filterInString);
        LOGGER.debug("filterCopyString: {}", filterCopyString);

        assertNotNull(filterInString);
        assertNotNull(filterCopyString);
        assertEquals(filterInString, filterCopyString);
    }

    private Geometry wktToGeometry(String wkt) {
        Geometry geometry = null;
        try {
            geometry = WKT_PARSER.parse(wkt);
        } catch (ParseException e) {
            fail();
        }
        return geometry;
    }
}
