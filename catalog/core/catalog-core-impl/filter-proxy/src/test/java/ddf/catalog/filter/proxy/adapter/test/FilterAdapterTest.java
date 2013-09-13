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
package ddf.catalog.filter.proxy.adapter.test;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

import org.geotools.filter.FilterFactoryImpl;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.text.WKTParser;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.UomOgcMapping;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriod;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.geotools.temporal.object.DefaultPosition;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.geometry.Geometry;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.PeriodDuration;

import com.vividsolutions.jts.io.WKTReader;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.delegate.FilterToTextDelegate;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.impl.filter.FuzzyFunction;
import ddf.catalog.impl.filter.JTSGeometryWrapper;
import ddf.catalog.source.UnsupportedQueryException;

public class FilterAdapterTest {

    private static final double DISTANCE_10 = 10.0;

    private static final String TEST_PROPERTY_VALUE = "Test";

    private static final String FOO_LITERAL_VALUE = "foo";

    private static final String MULTIPOLYGON_WKT = "MULTIPOLYGON (((40 40, 20 45, 45 30, 40 40)), ((20 35, 45 20, 30 5, 10 10, 10 30, 20 35), (30 20, 20 25, 20 15, 30 20)))";

    private static final String POLYGON_WKT = "POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))";

    private static final int DAY_IN_MILLISECONDS = 1000 * 60 * 60 * 24;

    private static final FilterFactory FF = new FilterFactoryImpl();

    private static final PropertyName TEST_PROPERTY = FF.property(TEST_PROPERTY_VALUE);

    private static final Literal FOO_LITERAL = FF.literal(FOO_LITERAL_VALUE);

    private static final GeometryBuilder GEO_BUILDER = new GeometryBuilder(
            DefaultGeographicCRS.WGS84);

    private static final WKTParser WKT_PARSER = new WKTParser(GEO_BUILDER);

    private static final Date EPOCH = new Date(0);

    private static final Date EPOCH_PLUS_DAY = new Date(DAY_IN_MILLISECONDS);

    private static final Instant EPOCH_INSTANT = new DefaultInstant(new DefaultPosition(EPOCH));

    private static final Instant EPOCH_PLUS_DAY_INSTANT = new DefaultInstant(new DefaultPosition(
            EPOCH_PLUS_DAY));

    private static final Period EPOCH_DAY_PERIOD = new DefaultPeriod(EPOCH_INSTANT,
            EPOCH_PLUS_DAY_INSTANT);

    private static final PeriodDuration DAY_DURATION = new DefaultPeriodDuration(
            DAY_IN_MILLISECONDS);

    @BeforeClass
    public static void setup() {
        org.apache.log4j.BasicConfigurator.configure();
    }

    @Test
    public void includeFilter() {
        assertFilterEquals("true", Filter.INCLUDE);
    }

    @Test
    public void excludeFilter() {
        assertFilterEquals("false", Filter.EXCLUDE);
    }

    @Test
    public void notFilter() {
        Filter filter = FF.equals(TEST_PROPERTY, FOO_LITERAL);

        assertFilterEquals("not(Test=foo)", FF.not(filter));
        assertFilterEquals("not(not(Test=foo))", FF.not(FF.not(filter)));
    }

    @Test
    public void andFilter() {
        Filter filter1 = FF.equals(FF.property("Test1"), FOO_LITERAL);
        Filter filter2 = FF.equals(FF.property("Test2"), FF.literal("bar"));
        Filter filter3 = FF.equals(FF.property("Test3"), FF.literal("baz"));

        assertFilterEquals("Test1=foo", FF.and(Arrays.asList(filter1)));
        assertFilterEquals("Test1=foo",
                FF.and(Arrays.asList((Filter) FF.and(Arrays.asList(filter1)))));
        assertFilterEquals("and(Test1=foo,Test2=bar)", FF.and(filter1, filter2));
        assertFilterEquals("and(Test1=foo,and(Test2=bar,Test3=baz))",
                FF.and(filter1, FF.and(filter2, filter3)));
        assertFilterEquals("and(Test1=foo,Test2=bar,Test3=baz)",
                FF.and(Arrays.asList(filter1, filter2, filter3)));

        // remove useless ands
        // and(filter1)
        assertFilterEquals("Test1=foo", FF.and(Arrays.asList(filter1)));
        // and(and(filter1))
        assertFilterEquals("Test1=foo",
                FF.and(Arrays.asList((Filter) FF.and(Arrays.asList(filter1)))));
        // and(and(filter1,filter2))
        assertFilterEquals("and(Test1=foo,Test2=bar)",
                FF.and(Arrays.asList((Filter) FF.and(filter1, filter2))));
    }

    @Test
    public void orFilter() {
        Filter filter1 = FF.equals(FF.property("Test1"), FOO_LITERAL);
        Filter filter2 = FF.equals(FF.property("Test2"), FF.literal("bar"));
        Filter filter3 = FF.equals(FF.property("Test3"), FF.literal("baz"));

        assertFilterEquals("or(Test1=foo)", FF.or(Arrays.asList(filter1)));
        assertFilterEquals("or(Test1=foo,Test2=bar)", FF.or(filter1, filter2));
        assertFilterEquals("or(Test1=foo,or(Test2=bar,Test3=baz))",
                FF.or(filter1, FF.or(filter2, filter3)));
        assertFilterEquals("or(Test1=foo,Test2=bar,Test3=baz)",
                FF.or(Arrays.asList(filter1, filter2, filter3)));
    }

    @Test
    public void propertyIsNull() {
        assertFilterEquals("null(Test)", FF.isNull(TEST_PROPERTY));
    }

    @Test
    public void propertyIsEqualTo() {
        assertFilterEquals("Test=foo", FF.equals(TEST_PROPERTY, FOO_LITERAL));
        assertFilterEquals("Test=" + EPOCH.toString(), FF.equals(TEST_PROPERTY, FF.literal(EPOCH)));
        assertFilterEquals("Test=" + EPOCH.toString(),
                FF.equals(TEST_PROPERTY, FF.literal(EPOCH_INSTANT)));
        assertFilterEquals("Test=" + EPOCH.toString() + " to " + EPOCH_PLUS_DAY.toString(),
                FF.equals(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));
        assertFilterEquals("Test=5i", FF.equals(TEST_PROPERTY, FF.literal(new Integer(5))));
        assertFilterEquals("Test=5s", FF.equals(TEST_PROPERTY, FF.literal(new Short((short) 5))));
        assertFilterEquals("Test=5l", FF.equals(TEST_PROPERTY, FF.literal(new Long(5))));
        assertFilterEquals("Test=5.0d", FF.equals(TEST_PROPERTY, FF.literal(new Double(5))));
        assertFilterEquals("Test=5.0f", FF.equals(TEST_PROPERTY, FF.literal(new Float(5))));
        assertFilterEquals("Test={5,6}b", FF.equals(TEST_PROPERTY, FF.literal(new byte[] {5, 6})));
        assertFilterEquals("Test=true", FF.equals(TEST_PROPERTY, FF.literal(true)));
        // test Object case
        assertFilterEquals("Test=[1, 2, 3]o",
                FF.equals(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }

    @Test
    public void propertyIsEqualToUri() throws URISyntaxException {
        String url = "http://www.test.com";
        URI uri = null;
        uri = new URI(url);
        assertFilterEquals("Test=" + url + "o", FF.equals(TEST_PROPERTY, FF.literal(uri)));
    }

    @Test
    public void propteryIsWithNulls() {
        assertFilterFails(FF.equals(TEST_PROPERTY, FF.literal(null)));
        assertFilterFails(FF.equals(FF.property(null), FOO_LITERAL));
        assertFilterFails(FF.equals(FF.property(null), FF.property(null)));

        assertFilterFails(FF.notEqual(FF.property(null), FF.literal(null)));
        assertFilterFails(FF.greater(FF.property(null), FF.literal(null)));
        assertFilterFails(FF.greaterOrEqual(FF.property(null), FF.literal(null)));
        assertFilterFails(FF.less(FF.property(null), FF.literal(null)));
        assertFilterFails(FF.lessOrEqual(FF.property(null), FF.literal(null)));
        assertFilterFails(FF.between(FF.property(null), FF.literal(null), FF.literal(null)));

        assertFilterFails(FF.isNull(FF.property(null)));
        assertFilterFails(FF.like(FF.property(null), null));
    }

    @Test
    public void propertyIsNotEqualTo() {
        assertFilterEquals("Test!=foo", FF.notEqual(TEST_PROPERTY, FOO_LITERAL));
        assertFilterEquals("Test!=" + EPOCH.toString(),
                FF.notEqual(TEST_PROPERTY, FF.literal(EPOCH)));
        assertFilterEquals("Test!=" + EPOCH.toString(),
                FF.notEqual(TEST_PROPERTY, FF.literal(EPOCH_INSTANT)));
        assertFilterEquals("Test!=" + EPOCH.toString() + " to " + EPOCH_PLUS_DAY.toString(),
                FF.notEqual(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));
        assertFilterEquals("Test!=5i", FF.notEqual(TEST_PROPERTY, FF.literal(new Integer(5))));
        assertFilterEquals("Test!=5s", FF.notEqual(TEST_PROPERTY, FF.literal(new Short((short) 5))));
        assertFilterEquals("Test!=5l", FF.notEqual(TEST_PROPERTY, FF.literal(new Long(5))));
        assertFilterEquals("Test!=5.0d", FF.notEqual(TEST_PROPERTY, FF.literal(new Double(5))));
        assertFilterEquals("Test!=5.0f", FF.notEqual(TEST_PROPERTY, FF.literal(new Float(5))));
        assertFilterEquals("Test!={5,6}b",
                FF.notEqual(TEST_PROPERTY, FF.literal(new byte[] {5, 6})));
        assertFilterEquals("Test!=true", FF.notEqual(TEST_PROPERTY, FF.literal(true)));
        // test Object case
        assertFilterEquals("Test!=[1, 2, 3]o",
                FF.notEqual(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }

    @Test
    public void propertyIsGreaterThan() {
        assertFilterEquals("Test>foo", FF.greater(TEST_PROPERTY, FOO_LITERAL));
        assertFilterEquals("Test>" + EPOCH.toString(), FF.greater(TEST_PROPERTY, FF.literal(EPOCH)));
        assertFilterEquals("Test>5i", FF.greater(TEST_PROPERTY, FF.literal(new Integer(5))));
        assertFilterEquals("Test>5s", FF.greater(TEST_PROPERTY, FF.literal(new Short((short) 5))));
        assertFilterEquals("Test>5l", FF.greater(TEST_PROPERTY, FF.literal(new Long(5))));
        assertFilterEquals("Test>5.0d", FF.greater(TEST_PROPERTY, FF.literal(new Double(5))));
        assertFilterEquals("Test>5.0f", FF.greater(TEST_PROPERTY, FF.literal(new Float(5))));
        // test Object case
        assertFilterEquals("Test>[1, 2, 3]o",
                FF.greater(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }

    @Test
    public void propertyIsGreaterThanOrEqualTo() {
        assertFilterEquals("Test>=foo", FF.greaterOrEqual(TEST_PROPERTY, FOO_LITERAL));
        assertFilterEquals("Test>=" + EPOCH.toString(),
                FF.greaterOrEqual(TEST_PROPERTY, FF.literal(EPOCH)));
        assertFilterEquals("Test>=5i", FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Integer(5))));
        assertFilterEquals("Test>=5s",
                FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Short((short) 5))));
        assertFilterEquals("Test>=5l", FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Long(5))));
        assertFilterEquals("Test>=5.0d",
                FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Double(5))));
        assertFilterEquals("Test>=5.0f", FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Float(5))));
        // test Object case
        assertFilterEquals("Test>=[1, 2, 3]o",
                FF.greaterOrEqual(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }

    @Test
    public void propertyIsLessThan() {
        assertFilterEquals("Test<foo", FF.less(TEST_PROPERTY, FOO_LITERAL));
        assertFilterEquals("Test<" + EPOCH.toString(), FF.less(TEST_PROPERTY, FF.literal(EPOCH)));
        assertFilterEquals("Test<5i", FF.less(TEST_PROPERTY, FF.literal(new Integer(5))));
        assertFilterEquals("Test<5s", FF.less(TEST_PROPERTY, FF.literal(new Short((short) 5))));
        assertFilterEquals("Test<5l", FF.less(TEST_PROPERTY, FF.literal(new Long(5))));
        assertFilterEquals("Test<5.0d", FF.less(TEST_PROPERTY, FF.literal(new Double(5))));
        assertFilterEquals("Test<5.0f", FF.less(TEST_PROPERTY, FF.literal(new Float(5))));
        // test Object case
        assertFilterEquals("Test<[1, 2, 3]o",
                FF.less(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }

    @Test
    public void propertyIsLessThanOrEqualTo() {
        assertFilterEquals("Test<=foo", FF.lessOrEqual(TEST_PROPERTY, FOO_LITERAL));
        assertFilterEquals("Test<=" + EPOCH.toString(),
                FF.lessOrEqual(TEST_PROPERTY, FF.literal(EPOCH)));
        assertFilterEquals("Test<=5i", FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Integer(5))));
        assertFilterEquals("Test<=5s",
                FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Short((short) 5))));
        assertFilterEquals("Test<=5l", FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Long(5))));
        assertFilterEquals("Test<=5.0d", FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Double(5))));
        assertFilterEquals("Test<=5.0f", FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Float(5))));
        // test Object case
        assertFilterEquals("Test<=[1, 2, 3]o",
                FF.lessOrEqual(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }

    @Test
    public void propertyIsBetween() {
        assertFilterEquals("foo<=Test<=bar",
                FF.between(TEST_PROPERTY, FOO_LITERAL, FF.literal("bar")));
        assertFilterEquals(EPOCH.toString() + "<=Test<=" + EPOCH_PLUS_DAY.toString(),
                FF.between(TEST_PROPERTY, FF.literal(EPOCH), FF.literal(EPOCH_PLUS_DAY)));
        assertFilterEquals(
                EPOCH.toString() + "<=Test<=" + EPOCH_PLUS_DAY.toString(),
                FF.between(TEST_PROPERTY, FF.literal(EPOCH_INSTANT),
                        FF.literal(EPOCH_PLUS_DAY_INSTANT)));
        assertFilterEquals("1i<=Test<=5i",
                FF.between(TEST_PROPERTY, FF.literal(new Integer(1)), FF.literal(new Integer(5))));
        assertFilterEquals(
                "1s<=Test<=5s",
                FF.between(TEST_PROPERTY, FF.literal(new Short((short) 1)),
                        FF.literal(new Short((short) 5))));
        assertFilterEquals("1l<=Test<=5l",
                FF.between(TEST_PROPERTY, FF.literal(new Long(1)), FF.literal(new Long(5))));
        assertFilterEquals("1.0d<=Test<=5.0d",
                FF.between(TEST_PROPERTY, FF.literal(new Double(1)), FF.literal(new Double(5))));
        assertFilterEquals("1.0f<=Test<=5.0f",
                FF.between(TEST_PROPERTY, FF.literal(new Float(1)), FF.literal(new Float(5))));
        // test Object case
        assertFilterEquals(
                "[1, 2, 3]o<=Test<=[2, 3, 4]o",
                FF.between(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3)),
                        FF.literal(Arrays.asList(2, 3, 4))));
    }

    @Test(expected = UnsupportedQueryException.class)
    public void bbox() throws UnsupportedQueryException {
        new GeotoolsFilterAdapterImpl().adapt(
                FF.bbox(TEST_PROPERTY.toString(), 1.0, 1.0, 2.0, 2.0, null), null);
    }

    @Test(expected = UnsupportedQueryException.class)
    public void propertyIsNil() throws UnsupportedQueryException {
        new GeotoolsFilterAdapterImpl().adapt(FF.isNil(null, null), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void bboxNull() throws UnsupportedQueryException {
        new GeotoolsFilterAdapterImpl().adapt((BBOX) null, null);
    }

    @Test
    public void propertyIsLike() {
        // propertyIsLike

        // remove dangling escape character
        assertFilterEquals("like(Test,foo*?)", FF.like(TEST_PROPERTY, "foo*?\\"));
        // replace special characters with normalized characters
        assertFilterEquals("like(Test,foo*?)", FF.like(TEST_PROPERTY, "foo!@#", "!", "@", "#"));
        assertFilterEquals("like(Test,! @ # \\* )",
                FF.like(TEST_PROPERTY, "#! #@ ## #* #", "!", "@", "#"));

        // fuzzy search
        assertFilterEquals("fuzzy(Test,foo)", FF.like(
                new FuzzyFunction(Arrays.asList((Expression) (TEST_PROPERTY)), FF.literal("")),
                FOO_LITERAL_VALUE));
        // TODO test failure if wildcard characters are sent

        String xpath = "//ns:name";

        // xpath exists
        assertFilterEquals("xpath(" + xpath + ")", FF.like(FF.property(xpath), ""));
        assertFilterEquals("xpath(" + xpath + ")", FF.like(FF.property(xpath), "*"));

        // xpath is like
        // Verify if search phrase ends with an escape character that the
        // escape character is stripped from the phrase
        assertFilterEquals("xpath(" + xpath + ",foo*?)", FF.like(FF.property(xpath), "foo*?\\"));
        // Verify can override widlcard, escape character, and single character defaults
        assertFilterEquals("xpath(" + xpath + ",foo*?)",
                FF.like(FF.property(xpath), "foo!@#", "!", "@", "#"));

        // xpath is fuzzy
        assertFilterEquals("xpath(" + xpath + ",fuzzy(foo*?))",
                FF.like(new FuzzyFunction(Arrays.asList((Expression) (FF.property(xpath))), FF
                        .literal("")), "foo*?\\"));
        assertFilterEquals("xpath(" + xpath + ",fuzzy(foo*?))",
                FF.like(new FuzzyFunction(Arrays.asList((Expression) (FF.property(xpath))), FF
                        .literal("")), "foo!@#", "!", "@", "#"));
    }

    @Test
    public void unsupportedWildcards() {
        // more than one character
        assertFilterFails(FF.like(TEST_PROPERTY, FOO_LITERAL_VALUE, "!!", "@", "#"));
        assertFilterFails(FF.like(TEST_PROPERTY, FOO_LITERAL_VALUE, "!", "@@", "#"));
        assertFilterFails(FF.like(TEST_PROPERTY, FOO_LITERAL_VALUE, "!", "@", "##"));

        // duplicate characters
        assertFilterFails(FF.like(TEST_PROPERTY, FOO_LITERAL_VALUE, "!", "!", "#"));
        assertFilterFails(FF.like(TEST_PROPERTY, FOO_LITERAL_VALUE, "!", "@", "@"));
        assertFilterFails(FF.like(TEST_PROPERTY, FOO_LITERAL_VALUE, "!", "@", "!"));
    }

    @Test
    public void reversedPropertyNameAndLiteral() {
        assertFilterEquals("Test=foo", FF.equals(FOO_LITERAL, TEST_PROPERTY));
        assertFilterEquals("Test!=foo", FF.notEqual(FOO_LITERAL, TEST_PROPERTY));
        assertFilterEquals("Test<foo", FF.greater(FOO_LITERAL, TEST_PROPERTY));
        assertFilterEquals("Test<=foo", FF.greaterOrEqual(FOO_LITERAL, TEST_PROPERTY));
        assertFilterEquals("Test>foo", FF.less(FOO_LITERAL, TEST_PROPERTY));
        assertFilterEquals("Test>=foo", FF.lessOrEqual(FOO_LITERAL, TEST_PROPERTY));
    }

    @Test
    public void unsupportedExpressions() {
        // divide
        assertFilterFails(FF.equals(TEST_PROPERTY, FF.divide(FF.literal(6), FF.literal(3))));
        // two expressions
        assertFilterFails(FF.equals(TEST_PROPERTY, FF.property("Test2")));
    }

    @Test
    public void spatialBeyondFilter() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        // meters
        assertFilterEquals(
                "beyond(Test,wkt(" + POLYGON_WKT + "),10.0)",
                FF.beyond(TEST_PROPERTY_VALUE, polygonGeometry, DISTANCE_10,
                        UomOgcMapping.METRE.name()));
        // feet
        assertFilterEquals(
                "beyond(Test,wkt(" + POLYGON_WKT + "),3.048006096012192)",
                FF.beyond(TEST_PROPERTY_VALUE, polygonGeometry, DISTANCE_10,
                        UomOgcMapping.FOOT.name()));

        // nearest neighbor
        assertFilterEquals("nn(Test,wkt(" + POLYGON_WKT + "))",
                FF.beyond(TEST_PROPERTY_VALUE, polygonGeometry, 0, UomOgcMapping.METRE.name()));
    }

    @Test
    public void spatialContainsFilter() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals("contains(Test,wkt(" + POLYGON_WKT + "))",
                FF.contains(TEST_PROPERTY_VALUE, polygonGeometry));
    }

    @Test
    public void spatialDWithinFilter() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        // as meters
        assertFilterEquals(
                "dwithin(Test,wkt(" + POLYGON_WKT + "),10.0)",
                FF.dwithin(TEST_PROPERTY_VALUE, polygonGeometry, DISTANCE_10,
                        UomOgcMapping.METRE.name()));
        // as feet
        assertFilterEquals(
                "dwithin(Test,wkt(" + POLYGON_WKT + "),3.048006096012192)",
                FF.dwithin(TEST_PROPERTY_VALUE, polygonGeometry, DISTANCE_10,
                        UomOgcMapping.FOOT.name()));
    }

    @Test
    public void spatialIntersectsFilter() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals("intersects(Test,wkt(" + POLYGON_WKT + "))",
                FF.intersects(TEST_PROPERTY_VALUE, polygonGeometry));
    }

    @Test
    public void spatialWithinFilter() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals("within(Test,wkt(" + POLYGON_WKT + "))",
                FF.within(TEST_PROPERTY_VALUE, polygonGeometry));
    }

    @Test
    public void spatialCrossesFilter() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals("crosses(Test,wkt(" + POLYGON_WKT + "))",
                FF.crosses(TEST_PROPERTY_VALUE, polygonGeometry));
    }

    @Test
    public void spatialDisjointFilter() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals("disjoint(Test,wkt(" + POLYGON_WKT + "))",
                FF.disjoint(TEST_PROPERTY_VALUE, polygonGeometry));
    }

    @Test
    public void spatialOverlapsFilter() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals("overlaps(Test,wkt(" + POLYGON_WKT + "))",
                FF.overlaps(TEST_PROPERTY_VALUE, polygonGeometry));
    }

    @Test
    public void spatialTouchesFilter() {
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals("touches(Test,wkt(" + POLYGON_WKT + "))",
                FF.touches(TEST_PROPERTY_VALUE, polygonGeometry));
    }

    @Test
    public void spatialWithNulls() {
        assertFilterFails(FF.beyond(null, null, 0, null));
        assertFilterFails(FF.contains(null, null));
        assertFilterFails(FF.dwithin(null, null, 0, null));
        assertFilterFails(FF.intersects(null, null));
        assertFilterFails(FF.within(null, null));
        assertFilterFails(FF.crosses(null, null));
        assertFilterFails(FF.disjoint(null, null));
        assertFilterFails(FF.overlaps(null, null));
        assertFilterFails(FF.touches(null, null));
    }

    @Test
    public void spatialWrappedGeometry() throws com.vividsolutions.jts.io.ParseException {
        WKTReader reader = new WKTReader();
        com.vividsolutions.jts.geom.Geometry geo = reader.read(MULTIPOLYGON_WKT);
        Geometry geometry = new JTSGeometryWrapper(geo);

        assertFilterEquals("beyond(Test,wkt(" + MULTIPOLYGON_WKT + "),10.0)",
                FF.beyond(TEST_PROPERTY_VALUE, geometry, DISTANCE_10, UomOgcMapping.METRE.name()));
        assertFilterEquals("contains(Test,wkt(" + MULTIPOLYGON_WKT + "))",
                FF.contains(TEST_PROPERTY_VALUE, geometry));
        assertFilterEquals("dwithin(Test,wkt(" + MULTIPOLYGON_WKT + "),10.0)",
                FF.dwithin(TEST_PROPERTY_VALUE, geometry, DISTANCE_10, UomOgcMapping.METRE.name()));
        assertFilterEquals("intersects(Test,wkt(" + MULTIPOLYGON_WKT + "))",
                FF.intersects(TEST_PROPERTY_VALUE, geometry));
        assertFilterEquals("within(Test,wkt(" + MULTIPOLYGON_WKT + "))",
                FF.within(TEST_PROPERTY_VALUE, geometry));
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

    @Test
    public void temporialAfterFilter() {
        // date
        assertFilterEquals("after(Test," + EPOCH.toString() + ")",
                FF.after(TEST_PROPERTY, FF.literal(EPOCH)));
        // instant
        assertFilterEquals("after(Test," + EPOCH.toString() + ")",
                FF.after(TEST_PROPERTY, FF.literal(EPOCH_INSTANT)));
        // period
        assertFilterEquals("after(Test," + EPOCH_PLUS_DAY.toString() + ")",
                FF.after(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));
    }

    @Test
    public void temporialBeforeFilter() {
        // date
        assertFilterEquals("before(Test," + EPOCH.toString() + ")",
                FF.before(TEST_PROPERTY, FF.literal(EPOCH)));
        // instant
        assertFilterEquals("before(Test," + EPOCH.toString() + ")",
                FF.before(TEST_PROPERTY, FF.literal(EPOCH_INSTANT)));
        // period
        assertFilterEquals("before(Test," + EPOCH.toString() + ")",
                FF.before(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));
    }

    @Test
    public void temporialDuringFilter() {
        // Absolute
        assertFilterEquals("during(Test," + EPOCH.toString() + "," + EPOCH_PLUS_DAY.toString()
                + ")", FF.during(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));

        // Relative
        assertFilterEquals("relative(Test," + DAY_IN_MILLISECONDS + ")",
                FF.during(TEST_PROPERTY, FF.literal(DAY_DURATION)));
    }

    private void assertFilterFails(Filter filter) {
        FilterDelegate<String> delegate = new FilterToTextDelegate();
        FilterAdapter fa = new GeotoolsFilterAdapterImpl();

        try {
            fa.adapt(filter, delegate);
            fail("Expected UnsupportedQueryException");
        } catch (UnsupportedQueryException e) {
            // pass
        }
    }

    private void assertFilterEquals(String expected, Filter filter) {
        FilterDelegate<String> delegate = new FilterToTextDelegate();
        FilterAdapter fa = new GeotoolsFilterAdapterImpl();

        String result = null;
        try {
            result = fa.adapt(filter, delegate);
        } catch (UnsupportedQueryException e) {
            fail(e.getMessage());
        }

        assertEquals(expected, result);
    }
}
