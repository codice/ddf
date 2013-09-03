/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.filter.delegate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

import org.apache.log4j.Logger;
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
import org.junit.BeforeClass;
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

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.impl.filter.FuzzyFunction;
import ddf.catalog.source.UnsupportedQueryException;


public class CopyFilterDelegateTest
{
    private static final transient Logger LOGGER = Logger.getLogger(CopyFilterDelegateTest.class);
    
    private static final FilterFactory FF = new FilterFactoryImpl();
    private static final String TEST_PROPERTY_VALUE = "Test";
    private static final PropertyName TEST_PROPERTY = FF.property(TEST_PROPERTY_VALUE);
    private static final String FOO_LITERAL_VALUE = "foo";
    private static final Literal FOO_LITERAL = FF.literal(FOO_LITERAL_VALUE);
    
    private static final int DAY_IN_MILLISECONDS = 1000 * 60 * 60 * 24;
    private static final Date EPOCH = new Date(0);
    private static final Date EPOCH_PLUS_DAY = new Date(DAY_IN_MILLISECONDS);
    private static final Instant EPOCH_INSTANT = new DefaultInstant(
            new DefaultPosition(EPOCH));
    private static final Instant EPOCH_PLUS_DAY_INSTANT = new DefaultInstant(
            new DefaultPosition(EPOCH_PLUS_DAY));
    private static final Period EPOCH_DAY_PERIOD = new DefaultPeriod(
            EPOCH_INSTANT, EPOCH_PLUS_DAY_INSTANT);
    private static final PeriodDuration DAY_DURATION = new DefaultPeriodDuration(
            DAY_IN_MILLISECONDS);
    
    private static final double DISTANCE_10 = 10.0;
    private static final String POLYGON_WKT = "POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))";
    
    private static final GeometryBuilder GEO_BUILDER = new GeometryBuilder(
        DefaultGeographicCRS.WGS84);
    private static final WKTParser WKT_PARSER = new WKTParser(GEO_BUILDER);
    
    
    @BeforeClass
    public static void setup() {
        org.apache.log4j.BasicConfigurator.configure();
    }
    
    
    @Test
    public void testIncludeFilter()
    {
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
    public void testExcludeFilter()
    {
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
    public void notFilter() 
    {
        Filter filter = FF.equals(TEST_PROPERTY, FOO_LITERAL);

        assertFilterEquals(FF.not(filter));
        assertFilterEquals(FF.not(FF.not(filter)));
    }
    
    
    @Test
    public void testAndFilter() 
    {
        Filter filter1 = FF.equals(FF.property("Test1"), FOO_LITERAL);
        Filter filter2 = FF.equals(FF.property("Test2"), FF.literal("bar"));

        assertFilterEquals(FF.and(filter1, filter2));    
    }
    
    
    @Test
    public void testOrFilter() 
    {
        Filter filter1 = FF.equals(FF.property("Test1"), FOO_LITERAL);
        Filter filter2 = FF.equals(FF.property("Test2"), FF.literal("bar"));

        assertFilterEquals(FF.or(filter1, filter2));    
    }
    
    
    @Test
    public void testPropertyIsNull()
    {
        assertFilterEquals(FF.isNull(TEST_PROPERTY));
    }  
    
    
    @Test
    public void testPropertyIsEqualTo_StringCaseInsensitive()
    {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(FOO_LITERAL_VALUE.toUpperCase())));
    }
    
    
    @Test
    public void testPropertyIsEqualTo_StringCaseSensitive()
    {
        assertFilterEquals(FF.equal(TEST_PROPERTY, FOO_LITERAL, true));
    }
    
    
    @Test
    public void testPropertyIsEqualTo_Date()
    {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(EPOCH)));
    }
    
    
    @Test
    public void testPropertyIsEqualTo_DateRange() throws Exception
    {
        assertFilterException(FF.equals(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));
    }
    
    
    @Test
    public void testPropertyIsEqualTo_Int()
    {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(new Integer(5))));
    }
    
    
    @Test
    public void testPropertyIsEqualTo_Long()
    {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(new Long(5))));
    }
    
    
    @Test
    public void testPropertyIsEqualTo_Short()
    {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(new Short((short) 5))));
    }
    
    
    @Test
    public void testPropertyIsEqualTo_Float()
    {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(new Float(5.0))));
    }
    
    
    @Test
    public void testPropertyIsEqualTo_Double()
    {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(new Double(5.0))));
    }
    
    
    @Test
    public void testPropertyIsEqualTo_Boolean()
    {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(true)));
    }
    
    
    @Test
    public void testPropertyIsEqualTo_ByteArray()
    {
        assertFilterEquals(FF.equals(TEST_PROPERTY, FF.literal(new byte[] { 5, 6 })));
    }
    
    
    @Test
    public void testPropertyIsEqualTo_Object() throws Exception
    {
        assertFilterException(FF.equals(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }
    
    
    @Test
    public void testPropertyIsNotEqualTo_StringCaseInsensitive()
    {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(FOO_LITERAL_VALUE.toUpperCase())));
    }
    
    
    @Test
    public void testPropertyIsNotEqualTo_StringCaseSensitive()
    {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FOO_LITERAL, true));
    }
    
    
    @Test
    public void testPropertyIsNotEqualTo_Date()
    {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(EPOCH)));
    }
    
    
    @Test
    public void testPropertyIsNotEqualTo_DateRange() throws Exception
    {
        assertFilterException(FF.notEqual(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));
    }
    
    
    @Test
    public void testPropertyIsNotEqualTo_Int()
    {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(new Integer(5))));
    }
    
    
    @Test
    public void testPropertyIsNotEqualTo_Long()
    {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(new Long(5))));
    }
    
    
    @Test
    public void testPropertyIsNotEqualTo_Short()
    {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(new Short((short) 5))));
    }
    
    
    @Test
    public void testPropertyIsNotEqualTo_Float()
    {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(new Float(5.0))));
    }
    
    
    @Test
    public void testPropertyIsNotEqualTo_Double()
    {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(new Double(5.0))));
    }
    
    
    @Test
    public void testPropertyIsNotEqualTo_Boolean()
    {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(true)));
    }
    
    
    @Test
    public void testPropertyIsNotEqualTo_ByteArray()
    {
        assertFilterEquals(FF.notEqual(TEST_PROPERTY, FF.literal(new byte[] { 5, 6 })));
    }
    
    
    @Test
    public void testPropertyIsNotEqualTo_Object() throws Exception
    {
        assertFilterException(FF.notEqual(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }
    
    
    @Test
    public void testPropertyIsGreaterThan_String() throws Exception
    {
        assertFilterException(FF.greater(TEST_PROPERTY, FF.literal(FOO_LITERAL_VALUE.toUpperCase())));
    }
    
    
    @Test
    public void testPropertyIsGreaterThan_Date() throws Exception
    {
        assertFilterException(FF.greater(TEST_PROPERTY, FF.literal(EPOCH)));
    }
    
    
    @Test
    public void testPropertyIsGreaterThan_Int()
    {
        assertFilterEquals(FF.greater(TEST_PROPERTY, FF.literal(new Integer(5))));
    }
    
    
    @Test
    public void testPropertyIsGreaterThan_Long()
    {
        assertFilterEquals(FF.greater(TEST_PROPERTY, FF.literal(new Long(5))));
    }
    
    
    @Test
    public void testPropertyIsGreaterThan_Short()
    {
        assertFilterEquals(FF.greater(TEST_PROPERTY, FF.literal(new Short((short) 5))));
    }
    
    
    @Test
    public void testPropertyIsGreaterThan_Float()
    {
        assertFilterEquals(FF.greater(TEST_PROPERTY, FF.literal(new Float(5.0))));
    }
    
    
    @Test
    public void testPropertyIsGreaterThan_Double()
    {
        assertFilterEquals(FF.greater(TEST_PROPERTY, FF.literal(new Double(5.0))));
    }
    
    
    @Test
    public void testPropertyIsGreaterThan_Object() throws Exception
    {
        assertFilterException(FF.greater(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }
    
    
    @Test
    public void testPropertyIsGreaterThanOrEqualTo_String() throws Exception
    {
        assertFilterException(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(FOO_LITERAL_VALUE.toUpperCase())));
    }
    
    
    @Test
    public void testPropertyIsGreaterThanOrEqualTo_Date() throws Exception
    {
        assertFilterException(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(EPOCH)));
    }
    
    
    @Test
    public void testPropertyIsGreaterThanOrEqualTo_Int()
    {
        assertFilterEquals(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Integer(5))));
    }
    
    
    @Test
    public void testPropertyIsGreaterThanOrEqualTo_Long()
    {
        assertFilterEquals(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Long(5))));
    }
    
    
    @Test
    public void testPropertyIsGreaterThanOrEqualTo_Short()
    {
        assertFilterEquals(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Short((short) 5))));
    }
    
    
    @Test
    public void testPropertyIsGreaterThanOrEqualTo_Float()
    {
        assertFilterEquals(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Float(5.0))));
    }
    
    
    @Test
    public void testPropertyIsGreaterThanOrEqualTo_Double()
    {
        assertFilterEquals(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(new Double(5.0))));
    }
    
    
    @Test
    public void testPropertyIsGreaterThanOrEqualTo_Object() throws Exception
    {
        assertFilterException(FF.greaterOrEqual(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }
    
    
    @Test
    public void testPropertyIsLesserThan_String() throws Exception
    {
        assertFilterException(FF.less(TEST_PROPERTY, FF.literal(FOO_LITERAL_VALUE.toUpperCase())));
    }
    
    
    @Test
    public void testPropertyIsLesserThan_Date() throws Exception
    {
        assertFilterException(FF.less(TEST_PROPERTY, FF.literal(EPOCH)));
    }
    
    
    @Test
    public void testPropertyIsLesserThan_Int()
    {
        assertFilterEquals(FF.less(TEST_PROPERTY, FF.literal(new Integer(5))));
    }
    
    
    @Test
    public void testPropertyIsLesserThan_Long()
    {
        assertFilterEquals(FF.less(TEST_PROPERTY, FF.literal(new Long(5))));
    }
    
    
    @Test
    public void testPropertyIsLesserThan_Short()
    {
        assertFilterEquals(FF.less(TEST_PROPERTY, FF.literal(new Short((short) 5))));
    }
    
    
    @Test
    public void testPropertyIsLesserThan_Float()
    {
        assertFilterEquals(FF.less(TEST_PROPERTY, FF.literal(new Float(5.0))));
    }
    
    
    @Test
    public void testPropertyIsLesserThan_Double()
    {
        assertFilterEquals(FF.less(TEST_PROPERTY, FF.literal(new Double(5.0))));
    }
    
    
    @Test
    public void testPropertyIsLesserThan_Object() throws Exception
    {
        assertFilterException(FF.less(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }
    
    
    @Test
    public void testPropertyIsLesserThanOrEqualTo_String() throws Exception
    {
        assertFilterException(FF.lessOrEqual(TEST_PROPERTY, FF.literal(FOO_LITERAL_VALUE.toUpperCase())));
    }
    
    
    @Test
    public void testPropertyIsLesserThanOrEqualTo_Date() throws Exception
    {
        assertFilterException(FF.lessOrEqual(TEST_PROPERTY, FF.literal(EPOCH)));
    }
    
    
    @Test
    public void testPropertyIsLesserThanOrEqualTo_Int()
    {
        assertFilterEquals(FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Integer(5))));
    }
    
    
    @Test
    public void testPropertyIsLesserThanOrEqualTo_Long()
    {
        assertFilterEquals(FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Long(5))));
    }
    
    
    @Test
    public void testPropertyIsLesserThanOrEqualTo_Short()
    {
        assertFilterEquals(FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Short((short) 5))));
    }
    
    
    @Test
    public void testPropertyIsLesserThanOrEqualTo_Float()
    {
        assertFilterEquals(FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Float(5.0))));
    }
    
    
    @Test
    public void testPropertyIsLesserThanOrEqualTo_Double()
    {
        assertFilterEquals(FF.lessOrEqual(TEST_PROPERTY, FF.literal(new Double(5.0))));
    }
    
    
    @Test
    public void testPropertyIsLesserThanOrEqualTo_Object() throws Exception
    {
        assertFilterException(FF.lessOrEqual(TEST_PROPERTY, FF.literal(Arrays.asList(1, 2, 3))));
    }
    
    
    @Test
    public void testPropertyIsBetween_Strings() throws Exception
    {
        assertFilterException(FF.between(TEST_PROPERTY, FOO_LITERAL, FF.literal("bar")));
    }
 
    
    @Test
    public void testPropertyIsBetween_Dates() throws Exception
    {
        assertFilterException(FF.between(TEST_PROPERTY, 
            FF.literal(EPOCH), FF.literal(EPOCH_PLUS_DAY)));
    }
 
    
    @Test
    public void testPropertyIsBetween_Ints() throws Exception
    {
        assertFilterEquals(FF.between(TEST_PROPERTY, 
            FF.literal(new Integer(1)), FF.literal(new Integer(5))));
    }
 
    
    @Test
    public void testPropertyIsBetween_Shorts() throws Exception
    {
        assertFilterEquals(FF.between(TEST_PROPERTY, 
            FF.literal(new Short((short) 1)), FF.literal(new Short((short) 5))));
    }
 
    
    @Test
    public void testPropertyIsBetween_Longs() throws Exception
    {
        assertFilterEquals(FF.between(TEST_PROPERTY, 
            FF.literal(new Long(1)), FF.literal(new Long(5))));
    }
 
    
    @Test
    public void testPropertyIsBetween_Floats() throws Exception
    {
        assertFilterEquals(FF.between(TEST_PROPERTY, 
            FF.literal(new Float(1.0)), FF.literal(new Float(5.0))));
    }

    
    @Test
    public void testPropertyIsBetween_Doubles() throws Exception
    {
        assertFilterEquals(FF.between(TEST_PROPERTY, 
            FF.literal(new Double(1.0)), FF.literal(new Double(5.0))));
    }

    
    @Test
    public void testPropertyIsBetween_Objects() throws Exception
    {
        assertFilterException(FF.between(TEST_PROPERTY, 
            FF.literal(Arrays.asList(1, 2, 3)), FF.literal(Arrays.asList(4, 5, 6))));
    }
    
    
    @Test
    public void testPropertyIsLike_CaseInsensitive()
    {
        assertFilterEquals(FF.like(TEST_PROPERTY, FOO_LITERAL_VALUE.toUpperCase()));
    }
    
    
    @Test
    public void testPropertyIsLike_CaseSensitive()
    {
        assertFilterEquals(FF.like(TEST_PROPERTY, FOO_LITERAL_VALUE.toUpperCase(), 
            FilterDelegate.WILDCARD_CHAR, FilterDelegate.SINGLE_CHAR, FilterDelegate.ESCAPE_CHAR, true));
    } 
       
    
    @Test
    public void testPropertyIsLike_Fuzzy()
    {
        assertFilterEquals(FF.like( new FuzzyFunction(Arrays.asList((Expression) (TEST_PROPERTY)),
                    FF.literal("")), FOO_LITERAL_VALUE));
    }
    
    
    @Test
    public void testXpathExists()
    {
        assertFilterEquals(FF.like(FF.property("//ns:title"), ""));
        //assertFilterEquals(FF.like(FF.property("//ns:title"), "*"));
    } 
    
    
    @Test
    public void testXpathIsLike()
    {
        assertFilterEquals(FF.like(FF.property("//ns:title"), "foo*"));
    } 
    
    
    @Test
    public void testXpathIsLike_CaseSensitive()
    {
        assertFilterEquals(FF.like(FF.property("//ns:title"), "foo*",
            FilterDelegate.WILDCARD_CHAR, FilterDelegate.SINGLE_CHAR, FilterDelegate.ESCAPE_CHAR, true));
    } 
    
    
    @Test
    public void testXpathIsFuzzy()
    {       
        assertFilterEquals(FF.like(
            new FuzzyFunction(Arrays.asList((Expression) (FF.property("//ns:title"))), 
                FF.literal("")), "foo*?"));
    } 
    
    
    @Test
    public void testSpatialBeyond()
    {       
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals(FF.beyond(TEST_PROPERTY_VALUE, polygonGeometry, DISTANCE_10,
            UomOgcMapping.METRE.name()));
    }   
    
    
    @Test
    public void testSpatialContains()
    {       
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals(FF.contains(TEST_PROPERTY_VALUE, polygonGeometry));
    }   
    
    
    @Test
    public void testSpatialDWithin()
    {       
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals(FF.dwithin(TEST_PROPERTY_VALUE, polygonGeometry, DISTANCE_10,
            UomOgcMapping.METRE.name()));
    }   
    
    
    @Test
    public void testSpatialIntersects()
    {       
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals(FF.intersects(TEST_PROPERTY_VALUE, polygonGeometry));
    }   
    
    
    @Test
    public void testSpatialWithin()
    {       
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterEquals(FF.within(TEST_PROPERTY_VALUE, polygonGeometry));
    }   
    
    
    @Test
    public void testSpatialCrosses()
    {       
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterException(FF.crosses(TEST_PROPERTY_VALUE, polygonGeometry));
    }   
    
    
    @Test
    public void testSpatialDisjoint()
    {       
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterException(FF.disjoint(TEST_PROPERTY_VALUE, polygonGeometry));
    }   
    
    
    @Test
    public void testSpatialTouches()
    {       
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterException(FF.touches(TEST_PROPERTY_VALUE, polygonGeometry));
    }   
    
    
    @Test
    public void testSpatialOverlaps()
    {       
        Geometry polygonGeometry = wktToGeometry(POLYGON_WKT);
        assertFilterException(FF.overlaps(TEST_PROPERTY_VALUE, polygonGeometry));
    }   
    
    
    @Test
    public void testTemporalAfter_Date()
    {
        assertFilterEquals(FF.after(TEST_PROPERTY, FF.literal(EPOCH)));
    }
    
    
    @Test
    public void testTemporalAfter_DateInstant()
    {
        assertFilterEquals(FF.after(TEST_PROPERTY, FF.literal(EPOCH_INSTANT)));
    }
    
    
    @Test
    public void testTemporalAfter_DatePeriod()
    {
        assertFilterEquals(FF.after(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));
    }
    
    
    @Test
    public void testTemporalBefore_Date()
    {
        assertFilterEquals(FF.before(TEST_PROPERTY, FF.literal(EPOCH)));
    }
    
    
    @Test
    public void testTemporalBefore_DateInstant()
    {
        assertFilterEquals(FF.before(TEST_PROPERTY, FF.literal(EPOCH_INSTANT)));
    }
    
    
    @Test
    public void testTemporalBefore_DatePeriod()
    {
        assertFilterEquals(FF.before(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));
    }
    
    
    @Test
    public void testTemporalDuring_Absolute()
    {
        assertFilterEquals(FF.during(TEST_PROPERTY, FF.literal(EPOCH_DAY_PERIOD)));
    }
    
    
    @Test
    public void testTemporalDuring_Relative()
    {
        assertFilterEquals(FF.during(TEST_PROPERTY, FF.literal(DAY_DURATION)));
    }
    
    
    @Test
    public void testFilterModification()
    {
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
    

    private void assertFilterException(Filter filterIn)
    {
        FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
        FilterDelegate<Filter> delegate = new CopyFilterDelegate(filterBuilder);
        FilterAdapter fa = new GeotoolsFilterAdapterImpl();

        // Explicitly test that an UnsupportedOperationException, thrown by the
        // CopyFilterDelegate, was the root cause of the failure, which is wrapped by 
        // the UnsupportedQueryException thrown by the FilterAdapter.
        try
        {
            fa.adapt(filterIn, delegate);
            fail("Should have gotten an UnsupportedQueryException");
        }
        catch (UnsupportedQueryException e)
        {
            assertTrue(e.getCause() instanceof UnsupportedOperationException);
        }
    }
    

    private void assertFilterEquals(Filter filterIn) 
    {
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
    
    
    private void assertFilterContentsEqual(Filter filterIn, Filter filterCopy)
    {
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
        
        LOGGER.debug("filterInString: " + filterInString);
        LOGGER.debug("filterCopyString: " + filterCopyString);

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
