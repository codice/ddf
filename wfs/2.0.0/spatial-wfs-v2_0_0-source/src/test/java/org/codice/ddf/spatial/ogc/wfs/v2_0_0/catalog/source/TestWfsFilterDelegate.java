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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.source;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.pattern.PatternMatcher.matchesPattern;
import static org.hamcrest.text.pattern.Patterns.anyCharacterIn;
import static org.hamcrest.text.pattern.Patterns.oneOrMore;
import static org.hamcrest.text.pattern.Patterns.sequence;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ObjectArrays;
import net.opengis.filter.v_2_0_0.BBOXType;
import net.opengis.filter.v_2_0_0.BinaryLogicOpType;
import net.opengis.filter.v_2_0_0.BinarySpatialOpType;
import net.opengis.filter.v_2_0_0.BinaryTemporalOpType;
import net.opengis.filter.v_2_0_0.ComparisonOperatorType;
import net.opengis.filter.v_2_0_0.ComparisonOperatorsType;
import net.opengis.filter.v_2_0_0.ConformanceType;
import net.opengis.filter.v_2_0_0.DistanceBufferType;
import net.opengis.filter.v_2_0_0.FilterCapabilities;
import net.opengis.filter.v_2_0_0.FilterType;
import net.opengis.filter.v_2_0_0.GeometryOperandsType.GeometryOperand;
import net.opengis.filter.v_2_0_0.LiteralType;
import net.opengis.filter.v_2_0_0.PropertyIsLikeType;
import net.opengis.filter.v_2_0_0.ResourceIdType;
import net.opengis.filter.v_2_0_0.ScalarCapabilitiesType;
import net.opengis.filter.v_2_0_0.SpatialOperatorType;
import net.opengis.filter.v_2_0_0.TemporalCapabilitiesType;
import net.opengis.filter.v_2_0_0.TemporalOperandsType;
import net.opengis.filter.v_2_0_0.TemporalOperatorType;
import net.opengis.filter.v_2_0_0.TemporalOperatorsType;
import net.opengis.filter.v_2_0_0.UnaryLogicOpType;
import net.opengis.gml.v_3_2_1.TimeInstantType;
import net.opengis.gml.v_3_2_1.TimePeriodType;
import net.opengis.gml.v_3_2_1.TimePositionType;
import net.opengis.ows.v_1_1_0.AllowedValues;
import net.opengis.ows.v_1_1_0.DomainType;
import net.opengis.ows.v_1_1_0.ValueType;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureAttributeDescriptor;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.COMPARISON_OPERATORS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.SPATIAL_OPERATORS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.TEMPORAL_OPERATORS;
import org.custommonkey.xmlunit.XMLUnit;
import org.hamcrest.text.pattern.PatternMatcher;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BasicTypes;

public class TestWfsFilterDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestWfsFilterDelegate.class);

    private static final String FILTER_QNAME_LOCAL_PART = "Filter";

    private static final String LITERAL = "Literal";

    private static final String VALUE_REFERENCE = "ValueReference";

    private static final String LOGICAL_OR_NAME = "{http://www.opengis.net/fes/2.0}Or";

    private static final String LOGICAL_AND_NAME = "{http://www.opengis.net/fes/2.0}And";

    private static final String LOGICAL_NOT_NAME = "{http://www.opengis.net/fes/2.0}Not";

    private static final String MOCK_GEOM = "geom";

    private static final String MOCK_GEOM2 = "geom2";

    private static final String POLYGON = "POLYGON ((30 -10, 30 30, 10 30, 10 -10, 30 -10))";

    private static final String LINESTRING = "LINESTRING (30 -10, 30 30, 10 30, 10 -10)";

    private static final String POINT = "POINT (30 -10)";

    private static final double DISTANCE = 1000.0;

    private String MOCK_METACARD_ATTRIBUTE = "modified";

    private String MOCK_FEATURE_TYPE = "mockFeatureType";

    private String MOCK_FEATURE_PROPERTY = "mockFeatureProperty";

    private MetacardMapper MOCK_MAPPER;

    private FeatureMetacardType mockFeatureMetacardType = mock(FeatureMetacardType.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUp() {
        XMLUnit.setNormalizeWhitespace(true);
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Test
    public void testFullFilterCapabilities() {
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN, null, WfsConstants.LAT_LON_ORDER);
        assertThat(delegate.isLogicalOps(), is(true));
        assertThat(delegate.isEpsg4326(), is(true));
        assertThat(delegate.isSortingSupported(), is(true));
        assertThat(delegate.getSrsName(), is(Wfs20Constants.EPSG_4326_URN));
        assertThat(delegate.getComparisonOps().size(), is(COMPARISON_OPERATORS.values().length));
        assertThat(delegate.getGeometryOperands().size(), greaterThan(0));
        assertThat(delegate.getSpatialOps().size(), is(SPATIAL_OPERATORS.values().length));
        assertThat(delegate.getTemporalOps().size(), is(TEMPORAL_OPERATORS.values().length));
        assertThat(delegate.getTemporalOperands().size(), greaterThan(0));
    }

    @Test
    public void testNoConformance() {
        FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
        capabilities.setConformance(null);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType, capabilities,
                Wfs20Constants.EPSG_4326_URN, null, WfsConstants.LAT_LON_ORDER);
        assertThat(delegate.isLogicalOps(), is(true));
        assertThat(delegate.isEpsg4326(), is(true));
        assertThat(delegate.isSortingSupported(), is(false));
        assertThat(delegate.getSrsName(), is(Wfs20Constants.EPSG_4326_URN));
        assertThat(delegate.getComparisonOps().size(), is(COMPARISON_OPERATORS.values().length));
        assertThat(delegate.getGeometryOperands().size(), greaterThan(0));
        assertThat(delegate.getSpatialOps().size(), is(SPATIAL_OPERATORS.values().length));
        assertThat(delegate.getTemporalOps().size(), is(TEMPORAL_OPERATORS.values().length));
        assertThat(delegate.getTemporalOperands().size(), greaterThan(0));
    }

    @Test
    public void testNoComparisonOps() {
        FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
        capabilities.setScalarCapabilities(null);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType, capabilities,
                Wfs20Constants.EPSG_4326_URN, null, WfsConstants.LAT_LON_ORDER);
        assertThat(delegate.isLogicalOps(), is(false));
        assertThat(delegate.isEpsg4326(), is(true));
        assertThat(delegate.isSortingSupported(), is(true));
        assertThat(delegate.getSrsName(), is(Wfs20Constants.EPSG_4326_URN));
        assertThat(delegate.getComparisonOps().size(), is(0));
        assertThat(delegate.getGeometryOperands().size(), greaterThan(0));
        assertThat(delegate.getSpatialOps().size(), is(SPATIAL_OPERATORS.values().length));
        assertThat(delegate.getTemporalOps().size(), is(TEMPORAL_OPERATORS.values().length));
        assertThat(delegate.getTemporalOperands().size(), greaterThan(0));
    }

    @Test
    public void testNoSpatialOps() {
        FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
        capabilities.setSpatialCapabilities(null);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType, capabilities,
                Wfs20Constants.EPSG_4326_URN, null, WfsConstants.LAT_LON_ORDER);
        assertThat(delegate.isLogicalOps(), is(true));
        assertThat(delegate.isEpsg4326(), is(true));
        assertThat(delegate.isSortingSupported(), is(true));
        assertThat(delegate.getSrsName(), is(Wfs20Constants.EPSG_4326_URN));
        assertThat(delegate.getComparisonOps().size(), is(COMPARISON_OPERATORS.values().length));
        assertThat(delegate.getGeometryOperands().size(), is(0));
        assertThat(delegate.getSpatialOps().size(), is(0));
        assertThat(delegate.getTemporalOps().size(), is(TEMPORAL_OPERATORS.values().length));
        assertThat(delegate.getTemporalOperands().size(), greaterThan(0));
    }

    @Test
    public void testNoTemporalOps() {
        FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
        capabilities.setTemporalCapabilities(null);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType, capabilities,
                Wfs20Constants.EPSG_4326_URN, null, WfsConstants.LAT_LON_ORDER);
        assertThat(delegate.isLogicalOps(), is(true));
        assertThat(delegate.isEpsg4326(), is(true));
        assertThat(delegate.isSortingSupported(), is(true));
        assertThat(delegate.getSrsName(), is(Wfs20Constants.EPSG_4326_URN));
        assertThat(delegate.getComparisonOps().size(), is(COMPARISON_OPERATORS.values().length));
        assertThat(delegate.getGeometryOperands().size(), greaterThan(0));
        assertThat(delegate.getSpatialOps().size(), is(SPATIAL_OPERATORS.values().length));
        assertThat(delegate.getTemporalOps().size(), is(0));
        assertThat(delegate.getTemporalOperands().size(), is(0));
    }

    @Test
    public void testConformanceAllowedValues() {
        // Setup
        FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
        ConformanceType conformance = capabilities.getConformance();
        List<DomainType> domainTypes = conformance.getConstraint();
        for (DomainType domainType : domainTypes) {
            if (StringUtils.equals(domainType.getName(), "ImplementsSorting")) {
                domainType.setNoValues(null);
                ValueType asc = new ValueType();
                asc.setValue("ASC");
                ValueType desc = new ValueType();
                desc.setValue("DESC");
                AllowedValues allowedValues = new AllowedValues();
                List<Object> values = new ArrayList<Object>();
                values.add(asc);
                values.add(desc);
                allowedValues.setValueOrRange(values);
                domainType.setAllowedValues(allowedValues);
                ValueType defaultValue = new ValueType();
                defaultValue.setValue("ASC");
                domainType.setDefaultValue(defaultValue);
                break;
            }
        }

        // Perform Test
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType, capabilities,
                Wfs20Constants.EPSG_4326_URN, null, WfsConstants.LAT_LON_ORDER);

        // Verify
        assertThat(delegate.isSortingSupported(), is(true));
        assertThat(delegate.getAllowedSortOrders().size(), is(2));
        assertThat(delegate.getAllowedSortOrders().contains(SortOrder.ASCENDING), is(true));
        assertThat(delegate.getAllowedSortOrders().contains(SortOrder.DESCENDING), is(true));
    }

    @Test
    /**
     * Doing a Absolute query from the search UI creates a During filter with the selected Begin and End date/times.
     *
     * Example During filter:
     *
     *   <Filter>
     *     <During>
     *         <ValueReference>myFeatureProperty</ValueReference>
     *         <ns4:TimePeriod ns4:id="myFeatureType.1406219647420">
     *             <ns4:beginPosition>1974-08-01T16:29:45.430-07:00</ns4:beginPosition>
     *             <ns4:endPosition>2014-07-22T16:29:45.430-07:00</ns4:endPosition>
     *         </ns4:TimePeriod>
     *     </During>
     * </Filter>
     *
     **/
    public void testDuring_PropertyIsOfTemporalType() throws Exception {
        SequentialTestMockHolder sequentialTestMockHolder = new SequentialTestMockHolder().invoke();
        WfsFilterDelegate delegate = sequentialTestMockHolder.getDelegate();
        String mockMetacardAttribute = sequentialTestMockHolder.getMockMetacardAttribute();
        String mockFeatureProperty = sequentialTestMockHolder.getMockFeatureProperty();
        String mockFeatureType = sequentialTestMockHolder.getMockFeatureType();

        DateTime startDate = new DateTime(2014, 01, 01, 01, 01, 01, 123, DateTimeZone.forID("-07:00"));
        DateTime endDate = new DateTime(2014, 01, 02, 01, 01, 01, 123, DateTimeZone.forID("-07:00"));

        // Perform Test
        FilterType filter = delegate.during(mockMetacardAttribute, startDate.toDate(),
                endDate.toDate());

        //Verify
        assertThat(filter.getTemporalOps().getName().toString(), is("{http://www.opengis.net/fes/2.0}During"));
        BinaryTemporalOpType binaryTemporalOpType = (BinaryTemporalOpType) filter.getTemporalOps().getValue();
        assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
        assertThat(binaryTemporalOpType.getValueReference(), is(mockFeatureProperty));
        assertThat(binaryTemporalOpType.isSetExpression(), is(true));
        TimePeriodType timePeriod = (TimePeriodType) binaryTemporalOpType.getExpression().getValue();

        assertThat(timePeriod.getBeginPosition().getValue().get(0), is("2014-01-01T08:01:01Z"));
        assertThat(timePeriod.getEndPosition().getValue().get(0), is("2014-01-02T08:01:01Z"));
        assertThat(timePeriod.getId(), is(matchesPattern(new PatternMatcher(sequence(mockFeatureType, ".", oneOrMore(anyCharacterIn("0-9")))))));

    }

    @Test(expected = IllegalArgumentException.class)
    /**
     * Verify that when Feature property "myFeatureProperty" is not defined in the Feature schema as a {http://www.opengis.net/gml/3.2}TimePeriodType
     * an IllegalArgumentException is thrown.
     */
    public void testDuring_PropertyIsNotOfTemporalType() throws Throwable {
        testSequential_PropertyIsNotOfTemporalType("during", new DateTime().minusDays(365).toDate(),
                new DateTime().minusDays(10).toDate());
    }

    @Test
    /**
     * Doing a Relative query from the search UI creates a During filter with the selected End date/time and the
     * Begin date/time calculated based on the duration.
     *
     *   <Filter>
     *     <During>
     *         <ValueReference>myFeatureProperty</ValueReference>
     *         <ns4:TimePeriod ns4:id="myFeatureType.1406219647420">
     *             <ns4:beginPosition>1974-08-01T16:29:45.430-07:00</ns4:beginPosition>
     *             <ns4:endPosition>2014-07-22T16:29:45.430-07:00</ns4:endPosition>
     *         </ns4:TimePeriod>
     *     </During>
     * </Filter>
     *
     **/
    public void testRelative_PropertyIsOfTemporalType() throws Exception {
        // Setup
        SequentialTestMockHolder sequentialTestMockHolder = new SequentialTestMockHolder().invoke();
        WfsFilterDelegate delegate = sequentialTestMockHolder.getDelegate();
        String mockMetacardAttribute = sequentialTestMockHolder.getMockMetacardAttribute();
        String mockFeatureProperty = sequentialTestMockHolder.getMockFeatureProperty();
        String mockFeatureType = sequentialTestMockHolder.getMockFeatureType();

        long duration = 604800000;
        DateTime now = new DateTime();
        /**
         * When delegate.relative(mockProperty, duration) is called, the current time (now) is calculated and used for the
         * end date/time and the start date/time is calculated based on the end date/time and duration (now - duration).  Once we
         * get the current time (now) in the test, we want to hold the System time fixed so when the current time (now) is retrieved
         * in delegate.relative(mockProperty, duration) there is no discrepancy.  This allows us to easily assert the begin position and 
         * end position in the Verify step of this test.
         */
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        String startDate = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC).print(
                now.minus(duration));

        // Perform Test
        FilterType filter = delegate.relative(mockMetacardAttribute, duration);

        //Verify
        assertThat(filter.getTemporalOps().getName().toString(),
                is("{http://www.opengis.net/fes/2.0}During"));
        BinaryTemporalOpType binaryTemporalOpType = (BinaryTemporalOpType) filter.getTemporalOps().getValue();
        assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
        assertThat(binaryTemporalOpType.getValueReference(), is(mockFeatureProperty));
        assertThat(binaryTemporalOpType.isSetExpression(), is(true));
        TimePeriodType timePeriod = (TimePeriodType) binaryTemporalOpType.getExpression().getValue();
        assertThat(timePeriod.getBeginPosition().getValue().get(0), is(startDate.toString()));
        assertThat(timePeriod.getEndPosition().getValue().get(0), is(ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC).print(now)));
        assertThat(timePeriod.getId(), is(matchesPattern(new PatternMatcher(sequence(mockFeatureType, ".", oneOrMore(anyCharacterIn("0-9")))))));

        // Reset the System time
        DateTimeUtils.setCurrentMillisSystem();
    }

    /**
     * Verify that when Feature property "myFeatureProperty" is not defined in the Feature schema as a {http://www.opengis.net/gml/3.2}TimePeriodType
     * an IllegalArgumentException is thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRelative_PropertyIsNotOfTemporalType() throws Throwable {
        testSequential_PropertyIsNotOfTemporalType("relative", 604800000L);
    }

    /**
     * Example After filter:
     *
     *    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
     *    <ns5:Filter xmlns:ns2="http://www.opengis.net/ows/1.1" xmlns="http://www.opengis.net/fes/2.0" xmlns:ns4="http://www.opengis.net/gml" xmlns:ns3="http://www.w3.org/1999/xlink" xmlns:ns5="http://www.opengis.net/ogc">
     *        <After>
     *            <ValueReference>myFeatureProperty</ValueReference>
     *            <ns4:TimeInstant ns4:id="myFeatureType.1406219647420">
     *                <ns4:timePosition>2013-07-23T14:02:09.239-07:00</ns4:timePosition>
     *            </ns4:TimeInstant>
     *         </After>
     *     </ns5:Filter>
     */
    @Test
    public void testAfter_PropertyIsOfTemporalType() throws Exception {
        testSequential_PropertyIsOfTemporalType("after", "{http://www.opengis.net/fes/2.0}After");
    }


    /**
     * Verify that when Feature property "myFeatureProperty" is not defined in the Feature schema as a {http://www.opengis.net/gml/3.2}TimeInstantType
     * an IllegalArgumentException is thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAfter_PropertyIsNotOfTemporalType() throws Throwable {
        testSequential_PropertyIsNotOfTemporalType("after", new DateTime().minusDays(365).toDate());
    }

    /**
     * Example Before filter:
     *
     * <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
     * <ns5:Filter xmlns:ns2="http://www.opengis.net/ows/1.1" xmlns="http://www.opengis.net/fes/2.0" xmlns:ns4="http://www.opengis.net/gml" xmlns:ns3="http://www.w3.org/1999/xlink" xmlns:ns5="http://www.opengis.net/ogc">
     *     <Before>
     *         <ValueReference>myFeatureProperty</ValueReference>
     *         <ns4:TimeInstant ns4:id="myFeatureType.1406219647420">
     *             <ns4:timePosition>2013-07-23T14:04:50.853-07:00</ns4:timePosition>
     *         </ns4:TimeInstant>
     *     </Before>
     * </ns5:Filter>
     *
     */
    @Test
    public void testBefore_PropertyIsOfTemporalType() throws Exception {
        testSequential_PropertyIsOfTemporalType("before", "{http://www.opengis.net/fes/2.0}Before");
    }

    private void testSequential_PropertyIsOfTemporalType(String methName, String temporalOpName) throws Exception {
        SequentialTestMockHolder sequentialTestMockHolder = new SequentialTestMockHolder().invoke();
        WfsFilterDelegate delegate = sequentialTestMockHolder.getDelegate();
        String mockMetacardAttribute = sequentialTestMockHolder.getMockMetacardAttribute();
        String mockFeatureProperty = sequentialTestMockHolder.getMockFeatureProperty();
        String mockFeatureType = sequentialTestMockHolder.getMockFeatureType();

        DateTime date = new DateTime().minusDays(365);

        // Perform Test
        Method method = WfsFilterDelegate.class.getMethod(methName, String.class, Date.class);
        FilterType filter = (FilterType) method.invoke(delegate, mockMetacardAttribute, date.toDate());

        //Verify
        assertThat(filter.getTemporalOps().getName().toString(), is(temporalOpName));
        BinaryTemporalOpType binaryTemporalOpType = (BinaryTemporalOpType) filter.getTemporalOps().getValue();
        assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
        assertThat(binaryTemporalOpType.getValueReference(), is(mockFeatureProperty));
        assertThat(binaryTemporalOpType.isSetExpression(), is(true));
        TimeInstantType timeInstant = (TimeInstantType) binaryTemporalOpType.getExpression().getValue();
        assertThat(timeInstant.getTimePosition().getValue().get(0), is(ISODateTimeFormat.dateTimeNoMillis().withZone( DateTimeZone.UTC).print(date)));
        assertThat(timeInstant.getId(), is(matchesPattern(new PatternMatcher(sequence(mockFeatureType, ".", oneOrMore(anyCharacterIn("0-9")))))));
    }

    /**
     * Verify that when Feature property "myFeatureProperty" is not defined in the Feature schema as a {http://www.opengis.net/gml/3.2}TimeInstantType
     * an IllegalArgumentException is thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBefore_PropertyIsNotOfTemporalType() throws Throwable {
        testSequential_PropertyIsNotOfTemporalType("before", new DateTime().minusDays(365).toDate());
    }

    private void testSequential_PropertyIsNotOfTemporalType(String methName, Object... inputParams)
            throws Throwable {
        String mockMetacardAttribute = "myMetacardAttribute";
        String mockFeatureType = "myFeatureType";
        String mockFeatureProperty = "myFeatureProperty";
        List<String> mockProperties = new ArrayList<String>(1);
        mockProperties.add(mockFeatureProperty);
        when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getName()).thenReturn(mockFeatureType);
        List<String> mockTemporalProperties = Collections.emptyList();
        when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockTemporalProperties);
        FeatureAttributeDescriptor mockFeatureAttributeDescriptor = mock(
                FeatureAttributeDescriptor.class);
        when(mockFeatureAttributeDescriptor.isIndexed()).thenReturn(true);
        when(mockFeatureAttributeDescriptor.getPropertyName()).thenReturn(mockFeatureProperty);
        when(mockFeatureMetacardType.getAttributeDescriptor(mockFeatureProperty))
                .thenReturn(mockFeatureAttributeDescriptor);
        MetacardMapper mockMapper = mock(MetacardMapper.class);
        when(mockMapper.getFeatureProperty(mockMetacardAttribute)).thenReturn(mockFeatureProperty);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN, mockMapper,
                WfsConstants.LAT_LON_ORDER);

        try {
            // Inject the mockMetacardAttribute at the head of the array
            Object[] methParams = ObjectArrays.concat(mockMetacardAttribute, inputParams);
            // Generate the array of class types for the reflection call
            Class<?>[] classTypes = FluentIterable.from(Arrays.asList(methParams))
                    .transform(new Function<Object, Class>() {
                        @Override public Class<?> apply(Object o) {
                            // Autoboxing is a small problem with reflection when trying to be too clever
                            return (o instanceof Long) ? long.class : o.getClass();
                        }
                    }).toArray(Class.class);

            Method method = WfsFilterDelegate.class.getMethod(methName, classTypes);
            method.invoke(delegate, methParams);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }


    @Test
    public void testLogicalNotOfComparison() throws Exception {

        String mockProperty = "myPropertyName";
        String mockType = "myType";
        WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

        FilterType filterToBeNoted = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);

        //Perform Test
        FilterType filter = delegate.not(filterToBeNoted);

        //Verify
        assertThat(filter.getLogicOps().getName().toString(), is(LOGICAL_NOT_NAME));
        UnaryLogicOpType logicOpType = (UnaryLogicOpType) filter.getLogicOps().getValue();

        PropertyIsLikeType compOpsType1 = (PropertyIsLikeType) logicOpType.getComparisonOps().getValue();
        String valRef1 = fetchPropertyIsLikeExpression(compOpsType1, VALUE_REFERENCE);
        assertThat(valRef1, is(mockProperty));
        String literal1 = fetchPropertyIsLikeExpression(compOpsType1, LITERAL);
        assertThat(literal1, is(LITERAL));
    }

    @Test
    public void testLogicalOrOfComparisons() throws Exception {
        testLogicalAndOrComparison("or", LOGICAL_OR_NAME);
    }

    @Test
    public void testLogicalAndOfComparison() throws Exception {
        testLogicalAndOrComparison("and", LOGICAL_AND_NAME);
    }

    private void testLogicalAndOrComparison(String methName, String compOpName) throws Exception {
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

        FilterType compFilter1 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
        FilterType compFilter2 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
        List<FilterType> filtersToCombine = new ArrayList<FilterType>();
        filtersToCombine.add(compFilter1);
        filtersToCombine.add(compFilter2);

        //Perform Test
        Method method = WfsFilterDelegate.class.getMethod(methName, List.class);
        FilterType filter = (FilterType) method.invoke(delegate, filtersToCombine);

        //Verify
        assertThat(filter.getLogicOps().getName().toString(), is(compOpName));
        BinaryLogicOpType logicOpType = (BinaryLogicOpType) filter.getLogicOps().getValue();

        Assert.assertThat(logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().size(), is(2));

        for (JAXBElement<?> jaxbElement : logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps()) {
            PropertyIsLikeType compOpsType = (PropertyIsLikeType) jaxbElement.getValue();
            String valRef = fetchPropertyIsLikeExpression(compOpsType, VALUE_REFERENCE);
            assertThat(valRef, is(mockProperty));
            String literal = fetchPropertyIsLikeExpression(compOpsType, LITERAL);
            assertThat(literal, is(LITERAL));
        }
    }

    @Test
    public void testLogicalNotOfSpatial() throws Exception {

        String mockProperty = "myPropertyName";
        String mockType = "myType";
        WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

        FilterType spatialFilter1 = delegate.dwithin(Metacard.ANY_GEO, "POINT (30 10)", Double.valueOf(1000));

        //Perform Test
        FilterType filter = delegate.not(spatialFilter1);

        //Verify
        assertThat(filter.getLogicOps().getName().toString(), is(LOGICAL_NOT_NAME));
        UnaryLogicOpType logicOpType = (UnaryLogicOpType) filter.getLogicOps().getValue();
        DistanceBufferType spatialOpsType1 = (DistanceBufferType) logicOpType.getSpatialOps().getValue();
        assertThat(Double.toString(spatialOpsType1.getDistance().getValue()), is(Double.valueOf(1000).toString()));

    }

    @Test
    public void testLogicalOrOfSpatial() throws Exception {
        testLogicalAndOrSpatial("or", LOGICAL_OR_NAME);
    }

    @Test
    public void testLogicalAndOfSpatial() throws Exception {
        testLogicalAndOrSpatial("and", LOGICAL_AND_NAME);
    }

    private void testLogicalAndOrSpatial(String methName, String compOpName) throws Exception {
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);


        FilterType spatialFilter1 = delegate.dwithin(Metacard.ANY_GEO, "POINT (30 10)",
                Double.valueOf(1000));
        FilterType spatialFilter2 = delegate.dwithin(Metacard.ANY_GEO, "POINT (50 10)",
                Double.valueOf(1500));

        List<FilterType> filtersToCombine = new ArrayList<FilterType>();
        filtersToCombine.add(spatialFilter1);
        filtersToCombine.add(spatialFilter2);

        //Perform Test
        Method method = WfsFilterDelegate.class.getMethod(methName, List.class);
        FilterType filter = (FilterType) method.invoke(delegate, filtersToCombine);

        //Verify
        assertThat(filter.getLogicOps().getName().toString(), is(compOpName));
        BinaryLogicOpType logicOpType = (BinaryLogicOpType) filter.getLogicOps().getValue();
        DistanceBufferType spatialOpsType1 = (DistanceBufferType) logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(0).getValue();
        assertThat(Double.toString(spatialOpsType1.getDistance().getValue()), is(Double.valueOf(1000).toString()));

        DistanceBufferType spatialOpsType2 = (DistanceBufferType) logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(1).getValue();
        assertThat(Double.toString(spatialOpsType2.getDistance().getValue()), is(Double.valueOf(1500).toString()));
    }

    /**
     * Verifies that a temporal criteria can be AND'ed to other criteria.
     *
     * @throws Exception
     */
    @Test
    public void testLogicalAndOfSpatialTemporal() throws Exception {

        String mockProperty = "myPropertyName";
        String mockType = "myType";
        WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);


        FilterType spatialFilter = delegate.dwithin(Metacard.ANY_GEO, "POINT (30 10)",
                Double.valueOf(1000));
        FilterType temporalFilter = delegate.during(mockProperty,
                new DateTime().minusDays(365).toDate(), new DateTime().minusDays(10).toDate());

        List<FilterType> filtersToBeAnded = new ArrayList<FilterType>(Arrays.asList(spatialFilter, temporalFilter));

        //Perform Test
        FilterType filter = delegate.and(filtersToBeAnded);

        //Verify AND op used
        if (filter.getLogicOps() == null) {
            fail("No AND/OR element found in the generated FilterType.");
        }
        assertEquals(LOGICAL_AND_NAME, filter.getLogicOps().getName().toString());
        BinaryLogicOpType logicOpType = (BinaryLogicOpType) filter.getLogicOps().getValue();

        //Verify two items were AND'ed
        assertEquals(2, logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().size());

        //Verify first is spatial, second is temporal
        assertTrue(logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(0)
                .getValue() instanceof DistanceBufferType);
        assertTrue(logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(1)
                .getValue() instanceof BinaryTemporalOpType);
    }

    @Test
    public void testLogicalOrOfLogicals() throws Exception {
        testLogicalCombinationOfLogicals("or", LOGICAL_OR_NAME);
    }

    @Test
    public void testLogicalAndOfLogicals() throws Exception {
        testLogicalCombinationOfLogicals("and", LOGICAL_AND_NAME);
    }

    private void testLogicalCombinationOfLogicals(String methName, String compOpName) throws Exception {
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

        FilterType compFilter1 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
        FilterType compFilter2 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
        List<FilterType> subFiltersToBeOred = new ArrayList<>();
        subFiltersToBeOred.add(compFilter1);
        subFiltersToBeOred.add(compFilter2);

        FilterType spatialFilter1 = delegate.dwithin(Metacard.ANY_GEO, "POINT (30 10)", Double.valueOf(1000));
        FilterType spatialFilter2 = delegate.dwithin(Metacard.ANY_GEO, "POINT (50 10)", Double.valueOf(1500));
        List<FilterType> subFiltersToBeAnded = new ArrayList<>();
        subFiltersToBeAnded.add(spatialFilter1);
        subFiltersToBeAnded.add(spatialFilter2);

        List<FilterType> filtersToCombine = new ArrayList<>();
        filtersToCombine.add(delegate.or(subFiltersToBeOred));
        filtersToCombine.add(delegate.and(subFiltersToBeAnded));

        Method method = WfsFilterDelegate.class.getMethod(methName, List.class);
        FilterType filter = (FilterType) method.invoke(delegate, filtersToCombine);

        //Verify
        assertThat(filter.getLogicOps().getName().toString(), is(compOpName));
        BinaryLogicOpType logicOpType = (BinaryLogicOpType) filter.getLogicOps().getValue();

        BinaryLogicOpType logicOrType = (BinaryLogicOpType) logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(0).getValue();
        assertThat(logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(0).getName().toString(), is(LOGICAL_OR_NAME));
        assertThat(logicOrType.getComparisonOpsOrSpatialOpsOrTemporalOps().size(), is(2));

        for (JAXBElement<?> jaxbElement : logicOrType.getComparisonOpsOrSpatialOpsOrTemporalOps()) {
            PropertyIsLikeType compOpsType = (PropertyIsLikeType) jaxbElement.getValue();
            String valRef = fetchPropertyIsLikeExpression(compOpsType, VALUE_REFERENCE);
            assertThat(valRef, is(mockProperty));
            String literal = fetchPropertyIsLikeExpression(compOpsType, LITERAL);
            assertThat(literal, is(LITERAL));
        }

        BinaryLogicOpType logicAndType = (BinaryLogicOpType) logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(1).getValue();
        assertThat(logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(1).getName().toString(), is(LOGICAL_AND_NAME));

        DistanceBufferType spatialOpsType1 = (DistanceBufferType) logicAndType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(0).getValue();
        assertThat(Double.toString(spatialOpsType1.getDistance().getValue()), is(Double.valueOf(1000).toString()));

        DistanceBufferType spatialOpsType2 = (DistanceBufferType) logicAndType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(1).getValue();
        assertThat(Double.toString(spatialOpsType2.getDistance().getValue()), is(Double.valueOf(1500).toString()));
    }

    @Test
    public void testLogicalNotOfLogicals() throws Exception {
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

        FilterType compFilter1 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
        FilterType compFilter2 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
        List<FilterType> subFiltersToBeOred = new ArrayList<FilterType>();
        subFiltersToBeOred.add(compFilter1);
        subFiltersToBeOred.add(compFilter2);

        //Perform Test
        FilterType filter = delegate.not(delegate.or(subFiltersToBeOred));

        //Verify
        assertThat(filter.getLogicOps().getName().toString(), is(LOGICAL_NOT_NAME));
        UnaryLogicOpType logicOpType = (UnaryLogicOpType) filter.getLogicOps().getValue();

        BinaryLogicOpType logicOrType = (BinaryLogicOpType) logicOpType.getLogicOps().getValue();
        assertThat(logicOpType.getLogicOps().getName().toString(), is(LOGICAL_OR_NAME));

        PropertyIsLikeType compOpsType1 = (PropertyIsLikeType) logicOrType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(0).getValue();
        String valRef1 = fetchPropertyIsLikeExpression(compOpsType1, VALUE_REFERENCE);
        assertThat(valRef1, is(mockProperty));
        String literal1 = fetchPropertyIsLikeExpression(compOpsType1, LITERAL);
        assertThat(literal1, is(LITERAL));

        PropertyIsLikeType compOpsType2 = (PropertyIsLikeType) logicOrType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(1).getValue();
        String valRef2 = fetchPropertyIsLikeExpression(compOpsType2, VALUE_REFERENCE);
        assertThat(valRef2, is(mockProperty));
        String literal2 = fetchPropertyIsLikeExpression(compOpsType2, LITERAL);
        assertThat(literal2, is(LITERAL));
    }

    @Test
    public void testLogicalOrOneItem() throws Exception {
        testLogicalCombinatorsOneItem("or");
    }

    @Test
    public void testLogicalAndOneItem() throws Exception {
        testLogicalCombinatorsOneItem("and");
    }

    private void testLogicalCombinatorsOneItem(String methName) throws Exception {
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

        FilterType compFilter1 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
        List<FilterType> filtersToCombine = new ArrayList<FilterType>();
        filtersToCombine.add(compFilter1);

        Method method = WfsFilterDelegate.class.getMethod(methName, List.class);
        FilterType filter = (FilterType) method.invoke(delegate, filtersToCombine);

        // Only one filter was provided to combinator so only that filter is returned as not
        // enough filters to combine together
        assertNull(filter.getLogicOps());
        PropertyIsLikeType compOpsType1 = (PropertyIsLikeType) filter.getComparisonOps().getValue();
        String valRef1 = fetchPropertyIsLikeExpression(compOpsType1, VALUE_REFERENCE);
        assertThat(valRef1, is(mockProperty));
        String literal1 = fetchPropertyIsLikeExpression(compOpsType1, LITERAL);
        assertThat(literal1, is(LITERAL));
    }

    @Test
    public void testLogicalWithNullOrEmpty() throws Exception {
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

        // Test with null list/entry
        FilterType filter = delegate.and(null);
        assertNull(filter);
        filter = delegate.not(null);
        assertNull(filter);

        // Test with empty list
        List<FilterType> filtersToBeCombined = new ArrayList<>();
        filter = delegate.and(filtersToBeCombined);
        assertNull(filter);
        filter = delegate.or(filtersToBeCombined);
        assertNull(filter);

        // Test with list with null entries
        filtersToBeCombined.add(null);
        filtersToBeCombined.add(null);
        filter = delegate.and(filtersToBeCombined);
        assertNull(filter);
        filter = delegate.or(filtersToBeCombined);
        assertNull(filter);

        // Finally, test a null list with an or
        thrown.expect(NullPointerException.class);
        filter = delegate.or(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testLogicalAndNoLogicalSupport() throws Exception {
        WfsFilterDelegate delegate = makeDelegateForLogicalSupportTests();

        FilterType compFilter1 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
        FilterType compFilter2 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
        List<FilterType> filtersToBeAnded = new ArrayList<FilterType>();
        filtersToBeAnded.add(compFilter1);
        filtersToBeAnded.add(compFilter2);

        //Perform Test
        FilterType filter = delegate.and(filtersToBeAnded);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testLogicalOrNoLogicalSupport() throws Exception {
        WfsFilterDelegate delegate = makeDelegateForLogicalSupportTests();

        FilterType compFilter1 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
        FilterType compFilter2 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
        List<FilterType> filtersToBeOred = new ArrayList<FilterType>();
        filtersToBeOred.add(compFilter1);
        filtersToBeOred.add(compFilter2);

        //Perform Test
        FilterType filter = delegate.or(filtersToBeOred);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testLogicalNotNoLogicalSupport() throws Exception {
        WfsFilterDelegate delegate = makeDelegateForLogicalSupportTests();
        FilterType filterToBeNoted = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);

        //Perform Test
        FilterType filter = delegate.not(filterToBeNoted);
    }

    private WfsFilterDelegate makeDelegateForLogicalSupportTests() throws Exception {
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        List<String> mockProperties = new ArrayList<String>(1);
        mockProperties.add(mockProperty);
        when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getGmlProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getTextualProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getName()).thenReturn(mockType);
        FeatureAttributeDescriptor mockFeatureAttributeDescriptor = mock(FeatureAttributeDescriptor.class);
        when(mockFeatureAttributeDescriptor.isIndexed()).thenReturn(true);
        when(mockFeatureAttributeDescriptor.getPropertyName()).thenReturn(mockProperty);
        when(mockFeatureMetacardType.getAttributeDescriptor(mockProperty)).thenReturn(mockFeatureAttributeDescriptor);

        FilterCapabilities filterCap = MockWfsServer.getFilterCapabilities();

        //Create new ScalarCapabiltiesType without Logical Operator support
        ScalarCapabilitiesType scalar = new ScalarCapabilitiesType();
        scalar.setComparisonOperators(new ComparisonOperatorsType());
        for (COMPARISON_OPERATORS compOp : COMPARISON_OPERATORS.values()) {
            ComparisonOperatorType operator = new ComparisonOperatorType();
            operator.setName(compOp.toString());
            scalar.getComparisonOperators().getComparisonOperator().add(operator);
        }
        filterCap.setScalarCapabilities(scalar);

        return new WfsFilterDelegate(mockFeatureMetacardType, filterCap,
                Wfs20Constants.EPSG_4326_URN, null, WfsConstants.LAT_LON_ORDER);
    }

    @Test
    public void testFeatureID() throws Exception {
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);
        String featureId = "1234567";

        //Perform Test
        FilterType matchIdFilter = delegate.propertyIsLike(Metacard.ID, featureId, true);

        //Verify
        assertThat(((ResourceIdType) matchIdFilter.getId().get(0).getValue()).getRid(),
                is(featureId));
    }

    @Test
    public void testFeatureTypeFeatureID() throws Exception {
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

        String featureId = "1234567";
        String mockTypeFeatureId = mockType + "." + featureId;

        //Perform Test
        FilterType matchIdFilter = delegate.propertyIsEqualTo(Metacard.ID, mockTypeFeatureId, true);

        //Verify
        assertThat(((ResourceIdType) matchIdFilter.getId().get(0).getValue()).getRid(),
                is(mockTypeFeatureId));
    }

    @Test
    public void testInvalidFeatureTypeFeatureID() throws Exception {
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);
        String nonExistentType = "myBadType";
        String featureId = "1234567";
        String mockTypeFeatureId = nonExistentType + "." + featureId;

        //Perform Test
        FilterType matchIdFilter = delegate.propertyIsEqualTo(Metacard.ID, mockTypeFeatureId, true);
        assertNull(matchIdFilter);
    }

    private WfsFilterDelegate setupFilterDelegate(String spatialOpType) {
        List<String> gmlProps = new ArrayList<String>();
        gmlProps.add(MOCK_GEOM);

        when(mockFeatureMetacardType.getGmlProperties()).thenReturn(gmlProps);
        when(mockFeatureMetacardType.getAttributeDescriptor(MOCK_GEOM)).thenReturn(
                new FeatureAttributeDescriptor(MOCK_GEOM, MOCK_GEOM, true, false, false, false,
                        BasicTypes.STRING_TYPE));

        SpatialOperatorType operator = new SpatialOperatorType();
        operator.setName(spatialOpType.toString());
        FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
        capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().clear();
        capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator()
                .add(operator);
        return new WfsFilterDelegate(mockFeatureMetacardType, capabilities,
                Wfs20Constants.EPSG_4326_URN, null, WfsConstants.LAT_LON_ORDER);
    }

    @Test
    public void testBeyondFilter() throws JAXBException, SAXException, IOException {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Beyond.toString());

        FilterType filter = delegate.beyond(Metacard.ANY_GEO, POLYGON, DISTANCE);

        assertTrue(filter.isSetSpatialOps());
        assertTrue(filter.getSpatialOps().getValue() instanceof DistanceBufferType);

        assertXMLEqual(MockWfsServer.getBeyondXmlFilter(), getXmlFromMarshaller(filter));
    }

    @Test
    public void testBeyondAsNotDwithin() {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.DWithin.toString());

        FilterType filter = delegate.beyond(Metacard.ANY_GEO, POLYGON, DISTANCE);
        assertTrue(filter.getLogicOps().getValue() instanceof UnaryLogicOpType);
        UnaryLogicOpType type = (UnaryLogicOpType) filter.getLogicOps().getValue();
        assertTrue(type.getSpatialOps().getValue() instanceof DistanceBufferType);
    }

    @Test
    public void testBeyondFilterUnsupported() throws Exception {
        testUnsupportedFilterTypeWithDistance("beyond", SPATIAL_OPERATORS.Intersects, DISTANCE);
    }

    @Test
    public void testContainsFilter() throws JAXBException, SAXException, IOException {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Contains.toString());

        FilterType filter = delegate.contains(Metacard.ANY_GEO, POLYGON);
        assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
        assertFalse(filter.isSetLogicOps());
        assertXMLEqual(MockWfsServer.getContainsXmlFilter(), getXmlFromMarshaller(filter));
    }

    @Test
    public void testContainsUnsupported() throws Exception {
        testUnsupportedFilterType("contains", SPATIAL_OPERATORS.Intersects);
    }

    @Test
    public void testCrossesFilter() throws SAXException, IOException, JAXBException {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Crosses.toString());
        FilterType filter = delegate.crosses(Metacard.ANY_GEO, LINESTRING);
        assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
        assertFalse(filter.isSetLogicOps());
        assertXMLEqual(MockWfsServer.getCrossesXmlFilter(), getXmlFromMarshaller(filter));
    }

    @Test
    public void testCrossesUnsupported() throws Exception {
        testUnsupportedFilterType("crosses", SPATIAL_OPERATORS.Intersects);
    }

    @Test
    public void testDisjointFilter() throws SAXException, IOException, JAXBException {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Disjoint.toString());

        FilterType filter = delegate.disjoint(Metacard.ANY_GEO, POLYGON);
        assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
        assertXMLEqual(MockWfsServer.getDisjointXmlFilter(), getXmlFromMarshaller(filter));
    }

    @Test
    public void testDisjointAsNotBBox() throws SAXException, IOException, JAXBException {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.BBOX.toString());

        FilterType filter = delegate.disjoint(Metacard.ANY_GEO, POLYGON);
        assertTrue(filter.getLogicOps().getValue() instanceof UnaryLogicOpType);
        UnaryLogicOpType type = (UnaryLogicOpType) filter.getLogicOps().getValue();
        assertTrue(type.getSpatialOps().getValue() instanceof BBOXType);
        assertXMLEqual(MockWfsServer.getNotBboxXmlFilter(), getXmlFromMarshaller(filter));
    }

    @Test
    public void testDisjointAsNotIntersects() throws SAXException, IOException, JAXBException {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Intersects.toString());

        FilterType filter = delegate.disjoint(Metacard.ANY_GEO, POLYGON);
        assertTrue(filter.getLogicOps().getValue() instanceof UnaryLogicOpType);
        UnaryLogicOpType type = (UnaryLogicOpType) filter.getLogicOps().getValue();
        assertTrue(type.getSpatialOps().getValue() instanceof BinarySpatialOpType);
        assertXMLEqual(MockWfsServer.getNotIntersectsXmlFilter(), getXmlFromMarshaller(filter));
    }

    @Test
    public void testDWithinFilterPolygon() throws SAXException, IOException, JAXBException {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.DWithin.toString());

        FilterType filter = delegate.dwithin(Metacard.ANY_GEO, POLYGON, DISTANCE);
        assertFalse(filter.isSetLogicOps());
        assertTrue(filter.isSetSpatialOps());
        assertTrue(filter.getSpatialOps().getValue() instanceof DistanceBufferType);

        assertXMLEqual(MockWfsServer.getDWithinXmlFilter(), getXmlFromMarshaller(filter));
    }

    @Test
    public void testDWithinFilterPoint() throws SAXException, IOException, JAXBException {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.DWithin.toString());

        FilterType filter = delegate.dwithin(Metacard.ANY_GEO, POINT, DISTANCE);
        assertFalse(filter.isSetLogicOps());
        assertTrue(filter.isSetSpatialOps());
        assertTrue(filter.getSpatialOps().getValue() instanceof DistanceBufferType);
        assertXMLEqual(MockWfsServer.getDWithinPointXmlFilter(), getXmlFromMarshaller(filter));
    }

    @Test
    public void testDwithinAsNotBeyond() {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Beyond.toString());

        FilterType filter = delegate.dwithin(Metacard.ANY_GEO, POLYGON, DISTANCE);
        assertTrue(filter.getLogicOps().getValue() instanceof UnaryLogicOpType);
        UnaryLogicOpType type = (UnaryLogicOpType) filter.getLogicOps().getValue();
        assertTrue(type.getSpatialOps().getValue() instanceof DistanceBufferType);

    }

    /**
     * From the Search UI, point-radius uses dwithin. We want dwithin to fallback to intersects as a
     * last resort. We buffer the geometry (the point) by the radius and do an intersects.
     */
    @Test
    public void testDwithinAsIntersects() throws JAXBException, SAXException, IOException {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Intersects.toString());
        /**
         * Made distance a large value so if the original WKT and the buffered WKT are plotted at:
         * http://openlayers.org/dev/examples/vector-formats.html one can easily see the buffer.
         */
        double distance = 200000.0;
        FilterType filter = delegate.dwithin(Metacard.ANY_GEO, POINT, distance);

        String xml = getXmlFromMarshaller(filter);

        assertXMLEqual(MockWfsServer.getDWithinAsIntersectsXml(), xml);
    }

    @Test
    public void testDwithinUnsupported() throws Exception {
        testUnsupportedFilterTypeWithDistance("dwithin", SPATIAL_OPERATORS.Contains, DISTANCE);
    }

    @Test
    public void testIntersects() throws SAXException, IOException, JAXBException {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Intersects.toString());

        FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

        assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
        assertFalse(filter.isSetLogicOps());
        assertXMLEqual(MockWfsServer.getIntersectsXmlFilter(), getXmlFromMarshaller(filter));
    }

    @Test
    public void testIntersectsWithEnvelope() throws SAXException, IOException, JAXBException {
        List<String> gmlProps = new ArrayList<String>();
        gmlProps.add(MOCK_GEOM);

        when(mockFeatureMetacardType.getGmlProperties()).thenReturn(gmlProps);
        when(mockFeatureMetacardType.getAttributeDescriptor(MOCK_GEOM)).thenReturn(
                new FeatureAttributeDescriptor(MOCK_GEOM, MOCK_GEOM, true, false, false, false,
                        BasicTypes.STRING_TYPE));

        SpatialOperatorType operator = new SpatialOperatorType();
        operator.setName(SPATIAL_OPERATORS.Intersects.toString());
        FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
        capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().clear();
        capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator()
                .add(operator);
        capabilities.getSpatialCapabilities().getGeometryOperands().getGeometryOperand().clear();
        GeometryOperand geoOperand = new GeometryOperand();
        geoOperand.setName(Wfs20Constants.ENVELOPE);
        capabilities.getSpatialCapabilities().getGeometryOperands().getGeometryOperand()
                .add(geoOperand);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType, capabilities,
                Wfs20Constants.EPSG_4326_URN, null, WfsConstants.LAT_LON_ORDER);

        FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

        assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
        assertFalse(filter.isSetLogicOps());
        assertXMLEqual(MockWfsServer.getIntersectsWithEnvelopeXmlFilter(),
                getXmlFromMarshaller(filter));
    }

    @Test
    public void testIntersectsLonLat() throws SAXException, IOException, JAXBException {
        List<String> gmlProps = new ArrayList<String>();
        gmlProps.add(MOCK_GEOM);

        when(mockFeatureMetacardType.getGmlProperties()).thenReturn(gmlProps);
        when(mockFeatureMetacardType.getAttributeDescriptor(MOCK_GEOM)).thenReturn(
                new FeatureAttributeDescriptor(MOCK_GEOM, MOCK_GEOM, true, false, false, false,
                        BasicTypes.STRING_TYPE));

        SpatialOperatorType operator = new SpatialOperatorType();
        operator.setName(SPATIAL_OPERATORS.Intersects.toString());
        FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
        capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().clear();
        capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator()
                .add(operator);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType, capabilities,
                Wfs20Constants.EPSG_4326_URN, null, WfsConstants.LON_LAT_ORDER);

        FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

        assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
        assertFalse(filter.isSetLogicOps());
        assertXMLEqual(MockWfsServer.getIntersectsLonLatXmlFilter(), getXmlFromMarshaller(filter));
    }

    @Test
    public void testIntersectsWithEnvelopeLonLat() throws SAXException, IOException, JAXBException {
        List<String> gmlProps = new ArrayList<String>();
        gmlProps.add(MOCK_GEOM);

        when(mockFeatureMetacardType.getGmlProperties()).thenReturn(gmlProps);
        when(mockFeatureMetacardType.getAttributeDescriptor(MOCK_GEOM)).thenReturn(
                new FeatureAttributeDescriptor(MOCK_GEOM, MOCK_GEOM, true, false, false, false,
                        BasicTypes.STRING_TYPE));

        SpatialOperatorType operator = new SpatialOperatorType();
        operator.setName(SPATIAL_OPERATORS.Intersects.toString());
        FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
        capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().clear();
        capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator()
                .add(operator);
        capabilities.getSpatialCapabilities().getGeometryOperands().getGeometryOperand().clear();
        GeometryOperand geoOperand = new GeometryOperand();
        geoOperand.setName(Wfs20Constants.ENVELOPE);
        capabilities.getSpatialCapabilities().getGeometryOperands().getGeometryOperand()
                .add(geoOperand);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType, capabilities,
                Wfs20Constants.EPSG_4326_URN, null, WfsConstants.LON_LAT_ORDER);

        FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

        assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
        assertFalse(filter.isSetLogicOps());
        assertXMLEqual(MockWfsServer.getIntersectsWithEnvelopeLonLatXmlFilter(),
                getXmlFromMarshaller(filter));
    }

    @Test
    public void testIntersectsAsBoundingBox() throws SAXException, IOException, JAXBException {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.BBOX.toString());

        FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);
        assertNotNull(filter);
        assertTrue(filter.getSpatialOps().getValue() instanceof BBOXType);
        assertFalse(filter.isSetLogicOps());
        assertXMLEqual(MockWfsServer.getBboxXmlFilter(), getXmlFromMarshaller(filter));
    }

    @Test
    public void testIntersectsAsNotDisjoint() {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Disjoint.toString());

        FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

        assertTrue(filter.isSetLogicOps());
        assertTrue(filter.getLogicOps().getValue() instanceof UnaryLogicOpType);
        UnaryLogicOpType type = (UnaryLogicOpType) filter.getLogicOps().getValue();
        assertTrue(type.isSetSpatialOps());
        assertTrue(type.getSpatialOps().getValue() instanceof BinarySpatialOpType);
    }

    @Test
    public void testIntersectsUnsupported() throws Exception {
        testUnsupportedFilterType("intersects", SPATIAL_OPERATORS.Contains);
    }

    @Test
    public void testOverlapsFilter() throws Exception {
        ImmutableMap<SPATIAL_OPERATORS, String> acceptedSpatialTypes = ImmutableMap
                .of(SPATIAL_OPERATORS.Overlaps, MockWfsServer.getOverlapsXmlFilter());
        testBinarySpatialFilter(acceptedSpatialTypes, "overlaps");
    }

    @Test
    public void testTouchesFilter() throws Exception {
        ImmutableMap<SPATIAL_OPERATORS, String> acceptedSpatialTypes = ImmutableMap
                .of(SPATIAL_OPERATORS.Touches, MockWfsServer.getTouchesXmlFilter());
        testBinarySpatialFilter(acceptedSpatialTypes, "touches");
    }

    @Test
    public void testWithinFilter() throws Exception {
        ImmutableMap<SPATIAL_OPERATORS, String> acceptedSpatialTypes = ImmutableMap
                .of(SPATIAL_OPERATORS.Within, MockWfsServer.getWithinXmlFilter(),
                        SPATIAL_OPERATORS.Contains, MockWfsServer.getContainsXmlFilter());
        testBinarySpatialFilter(acceptedSpatialTypes, "within");
    }

    private void testBinarySpatialFilter(Map<SPATIAL_OPERATORS, String> acceptedSpatialTypes,
            String delegateMethodName) throws Exception {
        List<SPATIAL_OPERATORS> failTypes = new ArrayList<>(
                Arrays.asList(SPATIAL_OPERATORS.values()));
        failTypes.removeAll(acceptedSpatialTypes.keySet());

        Method spatialOpMeth = WfsFilterDelegate.class
                .getMethod(delegateMethodName, String.class, String.class);
        for (Map.Entry<SPATIAL_OPERATORS, String> row : acceptedSpatialTypes.entrySet()) {
            WfsFilterDelegate delegate = setupFilterDelegate(row.getKey().toString());

            FilterType filter = (FilterType) spatialOpMeth
                    .invoke(delegate, Metacard.ANY_GEO, POLYGON);

            assertThat(filter.getSpatialOps().getValue(), instanceOf(BinarySpatialOpType.class));
            assertFalse(filter.isSetLogicOps());
            assertXMLEqual(row.getValue(), getXmlFromMarshaller(filter));
        }

        for (SPATIAL_OPERATORS failType : failTypes) {
            testUnsupportedFilterType(spatialOpMeth, failType);
        }
    }

    private void testUnsupportedFilterType(String spatialOpMethName, SPATIAL_OPERATORS failType)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method spatialOpMeth = WfsFilterDelegate.class
                .getMethod(spatialOpMethName, String.class, String.class);

        testUnsupportedFilterType(spatialOpMeth, failType);
    }

    private void testUnsupportedFilterType(Method spatialOpMeth, SPATIAL_OPERATORS failType)
            throws InvocationTargetException, IllegalAccessException {
        WfsFilterDelegate delegate = setupFilterDelegate(failType.toString());
        FilterType filter = (FilterType) spatialOpMeth
                .invoke(delegate, Metacard.ANY_GEO, POLYGON);

        assertNull(filter);
    }

    private void testUnsupportedFilterTypeWithDistance(String spatialOpMethName,
            SPATIAL_OPERATORS failType, double distance)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method spatialOpMeth = WfsFilterDelegate.class
                .getMethod(spatialOpMethName, String.class, String.class, double.class);

        WfsFilterDelegate delegate = setupFilterDelegate(failType.toString());
        FilterType filter = (FilterType) spatialOpMeth
                .invoke(delegate, Metacard.ANY_GEO, POLYGON, distance);

        assertNull(filter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSingleGmlPropertyBlacklisted() {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Contains.toString());
        when(mockFeatureMetacardType.getAttributeDescriptor(MOCK_GEOM)).thenReturn(
                new FeatureAttributeDescriptor(MOCK_GEOM, MOCK_GEOM, false, false, false, false,
                        BasicTypes.STRING_TYPE));

        delegate.contains(MOCK_GEOM, POLYGON);
    }

    @Test
    public void testIntersectsMultipleProperties() {
        intersectsMultiple(true);
    }

    @Test
    public void testAllGmlPropertiesBlacklisted() {
        intersectsMultiple(false);
    }

    private void intersectsMultiple(boolean indexed) {
        List<String> gmlProps = new ArrayList<String>();
        gmlProps.add(MOCK_GEOM);
        gmlProps.add(MOCK_GEOM2);
        when(mockFeatureMetacardType.getGmlProperties()).thenReturn(gmlProps);

        for (String gmlProp : gmlProps) {
            when(mockFeatureMetacardType.
                    getAttributeDescriptor(gmlProp)).
                    thenReturn(new FeatureAttributeDescriptor(gmlProp, gmlProp, indexed,
                            false, false, false, BasicTypes.STRING_TYPE));
        }

        SpatialOperatorType operator = new SpatialOperatorType();
        operator.setName(SPATIAL_OPERATORS.Intersects.toString());
        FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
        capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().clear();
        capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator()
                .add(operator);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType, capabilities,
                Wfs20Constants.EPSG_4326_URN, null, WfsConstants.LAT_LON_ORDER);

        FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);
        if (indexed) {
            assertNotNull(filter);
            assertTrue(filter.isSetLogicOps());
            assertNotNull(filter.getLogicOps());
        } else {
            assertNull(filter);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBadPolygonWkt() {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Intersects.toString());
        delegate.intersects(Metacard.ANY_GEO, "junk");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBadPointWkt() {
        WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.DWithin.toString());
        delegate.dwithin(Metacard.ANY_GEO, "junk", DISTANCE);
    }

    @Test
    public void testNonEpsg4326Srs() {
        List<String> gmlProps = new ArrayList<String>();
        gmlProps.add(MOCK_GEOM);
        when(mockFeatureMetacardType.getGmlProperties()).thenReturn(gmlProps);

        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), "EPSG:42304", null, WfsConstants.LAT_LON_ORDER);
        FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

        assertTrue(filter == null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGeoFilterNullMetacardType() {
        WfsFilterDelegate delegate = new WfsFilterDelegate(null,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN, null, WfsConstants.LAT_LON_ORDER);

        delegate.beyond(Metacard.ANY_GEO, POLYGON, DISTANCE);
    }

    private String fetchPropertyIsLikeExpression(PropertyIsLikeType compOpsType, String expressionType) {
        String result = null;
        List<JAXBElement<?>> expressions = compOpsType.getExpression();
        for (int i = 0; i < expressions.size(); ++i) {
            String item = expressions.get(i).getName().getLocalPart();
            if (item.equals(VALUE_REFERENCE) && item.equals(expressionType)) {
                result = expressions.get(i).getValue().toString();

            } else if (item.equals(LITERAL) && item.equals(expressionType)) {
                LiteralType literal = (LiteralType) expressions.get(i).getValue();
                result = literal.getContent().get(0).toString();
            }
        }

        return result;
    }

    private WfsFilterDelegate mockFeatureMetacardCreateDelegate(String mockProperty, String mockType) {
        List<String> mockProperties = new ArrayList<String>(1);
        mockProperties.add(mockProperty);
        when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getGmlProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getTextualProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getName()).thenReturn(mockType);
        FeatureAttributeDescriptor mockFeatureAttributeDescriptor = mock(FeatureAttributeDescriptor.class);
        when(mockFeatureAttributeDescriptor.isIndexed()).thenReturn(true);
        when(mockFeatureAttributeDescriptor.getPropertyName()).thenReturn(mockProperty);
        when(mockFeatureMetacardType.getAttributeDescriptor(mockProperty)).thenReturn(mockFeatureAttributeDescriptor);

        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN, null, WfsConstants.LAT_LON_ORDER);

        return delegate;
    }


    private String getXmlFromMarshaller(FilterType filterType) throws JAXBException {
        JAXBContext jaxbContext = initJaxbContext();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        Writer writer = new StringWriter();
        marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
        String xml = writer.toString();
        LOGGER.debug("XML returned by Marshaller:\n{}", xml);
        LOGGER.trace(Thread.currentThread().getStackTrace().toString());
        return xml;
    }

    private static JAXBContext initJaxbContext() {
        JAXBContext localJaxbContext = null;

        String contextPath = StringUtils.join(new String[]{Wfs20Constants.OGC_FILTER_PACKAGE,
                Wfs20Constants.OGC_GML_PACKAGE, Wfs20Constants.OGC_OWS_PACKAGE}, ":");

        try {
            LOGGER.debug("Creating JAXB context with context path: {}", contextPath);
            localJaxbContext = JAXBContext.newInstance(contextPath);
        } catch (JAXBException e) {
            LOGGER.error("Unable to create JAXB context using contextPath: {}", contextPath, e);
        }

        return localJaxbContext;
    }

    private JAXBElement<FilterType> getFilterTypeJaxbElement(FilterType filterType) {
        JAXBElement<FilterType> filterTypeJaxbElement = new JAXBElement<FilterType>(new QName(
                "http://www.opengis.net/ogc", FILTER_QNAME_LOCAL_PART), FilterType.class,
                filterType);
        return filterTypeJaxbElement;
    }

    @Test
    public void testDuringTemporalFallback() throws Exception {
        setupMockMetacardType();
        FilterType afterFilter = setupAfterFilterType();
        FilterType beforeFilter = setupBeforeFilterType();
        FilterCapabilities duringFilterCapabilities = setupFilterCapabilities();
        WfsFilterDelegate duringDelegate = new WfsFilterDelegate(mockFeatureMetacardType,
                duringFilterCapabilities, Wfs20Constants.EPSG_4326_URN, MOCK_MAPPER, WfsConstants.LAT_LON_ORDER);

        WfsFilterDelegate spatialDelegate = mockFeatureMetacardCreateDelegate(MOCK_FEATURE_PROPERTY,
                MOCK_FEATURE_TYPE);
        FilterType spatialFilter = spatialDelegate.dwithin(Metacard.ANY_GEO, "POINT (30 10)",
                Double.valueOf(1000));

        List<List<FilterType>> testFilters = new ArrayList<>();
        testFilters.add(Arrays.asList(afterFilter, beforeFilter));
        testFilters.add(Arrays.asList(afterFilter, beforeFilter, spatialFilter));

        for (List<FilterType> filtersToBeConverted : testFilters) {
            List<FilterType> convertedFilters = duringDelegate.applyTemporalFallbacks(filtersToBeConverted);
            FilterType duringFilter = convertedFilters.get(0);

            if (filtersToBeConverted.contains(spatialFilter)) {
                // verify that results contains the spatial filter type
                assertThat(convertedFilters.contains(spatialFilter), is(true));

                assertThat(convertedFilters.size(), is(2));

                if (duringFilter.isSetSpatialOps()) {
                    duringFilter = convertedFilters.get(1);
                }

                //Verify during Filter is correct
                assertThat(duringFilter.isSetTemporalOps(), is(true));
            }

            assertThat(duringFilter.getTemporalOps().getName().toString(), is("{http://www.opengis.net/fes/2.0}During"));

            BinaryTemporalOpType binaryTemporalOpType = (BinaryTemporalOpType) duringFilter.getTemporalOps().getValue();
            assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
            assertThat(binaryTemporalOpType.isSetExpression(), is(true));
            TimePeriodType timePeriod = (TimePeriodType) binaryTemporalOpType.getExpression().getValue();

            TimePositionType beginPositionType = timePeriod.getBeginPosition();
            Date beginDate = timePositionTypeToDate(beginPositionType);
            TimePositionType endPositionType = timePeriod.getEndPosition();
            Date endDate = timePositionTypeToDate(endPositionType);

            // Verify Date range is created correctly
            assertThat(endDate.after(beginDate), is(true));
        }
    }

    @Test
    public void testDuringFilterTypeDates() throws Exception {
        setupMockMetacardType();
        FilterType duringFilter = setupDuringFilterType();
        BinaryTemporalOpType binaryTemporalOpType = (BinaryTemporalOpType) duringFilter.getTemporalOps().getValue();
        assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
        assertThat(binaryTemporalOpType.isSetExpression(), is(true));
        TimePeriodType timePeriod = (TimePeriodType) binaryTemporalOpType.getExpression().getValue();

        TimePositionType beginPositionType = timePeriod.getBeginPosition();
        Date beginDate = timePositionTypeToDate(beginPositionType);

        TimePositionType endPositionType = timePeriod.getEndPosition();
        Date endDate = timePositionTypeToDate(endPositionType);

        assertThat(endDate.after(beginDate), is(true));
    }

    @Test
    public void testIsInFilterSequenceType() throws Exception {
        setupMockMetacardType();
        FilterType beforeFilter = setupBeforeFilterType();
        FilterType afterFilter = setupAfterFilterType();
        FilterType duringFilter = setupDuringFilterType();

        WfsFilterDelegate afterDelegate = setupTemporalFilterDelegate();
        WfsFilterDelegate beforeDelegate = setupTemporalFilterDelegate();

        // Test is before filter type
        assertThat(beforeDelegate.isBeforeFilter(beforeFilter), is(true));
        assertThat(beforeDelegate.isBeforeFilter(afterFilter), is(false));
        assertThat(beforeDelegate.isBeforeFilter(duringFilter), is(false));

        // Test is during filter type
        assertThat(afterDelegate.isDuringFilter(duringFilter), is(true));
        assertThat(afterDelegate.isDuringFilter(beforeFilter), is(false));
        assertThat(afterDelegate.isDuringFilter(afterFilter), is(false));

        // Test is after filter type
        assertThat(afterDelegate.isAfterFilter(afterFilter), is(true));
        assertThat(afterDelegate.isAfterFilter(beforeFilter), is(false));
        assertThat(afterDelegate.isBeforeFilter(duringFilter), is(false));
    }


    private WfsFilterDelegate setupTemporalFilterDelegate() {
        MetacardMapper mockMapper = mock(MetacardMapper.class);
        return new WfsFilterDelegate(mockFeatureMetacardType, MockWfsServer.getFilterCapabilities(),
                Wfs20Constants.EPSG_4326_URN, mockMapper, WfsConstants.LAT_LON_ORDER);
    }

    private void setupMockMetacardType() {
        MOCK_METACARD_ATTRIBUTE = "modified";
        MOCK_FEATURE_TYPE = "myFeatureType";
        MOCK_FEATURE_PROPERTY = "localpart.EXACT_COLLECT_DATE";
        List<String> mockProperties = new ArrayList<>(1);
        mockProperties.add(MOCK_FEATURE_PROPERTY);

        QName localName = new QName("EXACT_COLLECT_DATE", "localpart");
        when(mockFeatureMetacardType.getFeatureType()).thenReturn(localName);
        when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getName()).thenReturn(MOCK_FEATURE_TYPE);
        when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockProperties);

        FeatureAttributeDescriptor mockFeatureAttributeDescriptor = mock(FeatureAttributeDescriptor.class);
        when(mockFeatureAttributeDescriptor.isIndexed()).thenReturn(true);
        when(mockFeatureAttributeDescriptor.getPropertyName()).thenReturn("EXACT_COLLECT_DATE");
        when(mockFeatureMetacardType.getAttributeDescriptor(MOCK_FEATURE_PROPERTY)).thenReturn(mockFeatureAttributeDescriptor);

        MOCK_MAPPER = mock(MetacardMapper.class);
        when(MOCK_MAPPER.getFeatureProperty(MOCK_METACARD_ATTRIBUTE)).thenReturn(MOCK_FEATURE_PROPERTY);
    }

    private FilterType setupBeforeFilterType() {
        WfsFilterDelegate beforeDelegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN, MOCK_MAPPER, WfsConstants.LAT_LON_ORDER);
        DateTime beforeDate = new DateTime().minusDays(1);
        return beforeDelegate.before(MOCK_METACARD_ATTRIBUTE, beforeDate.toDate());
    }

    private FilterType setupAfterFilterType() {
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN, MOCK_MAPPER, WfsConstants.LAT_LON_ORDER);
        DateTime afterDate = new DateTime().minusDays(30);
        return delegate.after(MOCK_METACARD_ATTRIBUTE, afterDate.toDate());
    }

    private FilterType setupDuringFilterType() {
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN, MOCK_MAPPER, WfsConstants.LAT_LON_ORDER);
        DateTime startDate = new DateTime(2014, 01, 01, 01, 01, 01, 123, DateTimeZone.forID("-07:00"));
        DateTime endDate = new DateTime(2014, 01, 02, 01, 01, 01, 123, DateTimeZone.forID("-07:00"));
        return delegate.during(MOCK_METACARD_ATTRIBUTE, startDate.toDate(), endDate.toDate());
    }

    private Date timePositionTypeToDate(TimePositionType timePositionType) {
        Date date = new Date();
        try {
            List<String> timeList = timePositionType.getValue();
            String dateTimeString = timeList.get(0);
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");  // 2014-12-14T17:14:43Z
            date = format.parse(dateTimeString);
        } catch (ParseException pe) {
            LOGGER.debug("Parse Exception {}", pe);
        }
        return date;
    }

    private FilterCapabilities setupFilterCapabilities() {
        FilterCapabilities filterCapabilities = new FilterCapabilities();
        TemporalCapabilitiesType temporal = new TemporalCapabilitiesType();
        temporal.setTemporalOperators(new TemporalOperatorsType());
        TemporalOperatorType duringOperator = new TemporalOperatorType();
        duringOperator.setName(TEMPORAL_OPERATORS.During.name());
        temporal.getTemporalOperators().getTemporalOperator().add(duringOperator);

        TemporalOperandsType temporalOperands = new TemporalOperandsType();
        List<QName> timeQNames = Arrays.asList(new QName(Wfs20Constants.GML_3_2_NAMESPACE,
                "TimePeriod"), new QName(Wfs20Constants.GML_3_2_NAMESPACE, "TimeInstant"));
        for (QName qName : timeQNames) {
            TemporalOperandsType.TemporalOperand operand = new TemporalOperandsType.TemporalOperand();
            operand.setName(qName);
            temporalOperands.getTemporalOperand().add(operand);
        }
        temporal.setTemporalOperands(temporalOperands);
        filterCapabilities.setTemporalCapabilities(temporal);
        return filterCapabilities;
    }

    private class SequentialTestMockHolder {
        private String mockMetacardAttribute;

        private String mockFeatureType;

        private String mockFeatureProperty;

        private WfsFilterDelegate delegate;

        public String getMockMetacardAttribute() {
            return mockMetacardAttribute;
        }

        public String getMockFeatureType() {
            return mockFeatureType;
        }

        public String getMockFeatureProperty() {
            return mockFeatureProperty;
        }

        public WfsFilterDelegate getDelegate() {
            return delegate;
        }

        public SequentialTestMockHolder invoke() {
            mockMetacardAttribute = "myMetacardAttribute";
            mockFeatureType = "myFeatureType";
            mockFeatureProperty = "myFeatureProperty";
            List<String> mockProperties = new ArrayList<String>(1);
            mockProperties.add(mockFeatureProperty);
            when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
            when(mockFeatureMetacardType.getName()).thenReturn(mockFeatureType);
            when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockProperties);
            FeatureAttributeDescriptor mockFeatureAttributeDescriptor = mock(FeatureAttributeDescriptor.class);
            when(mockFeatureAttributeDescriptor.isIndexed()).thenReturn(true);
            when(mockFeatureAttributeDescriptor.getPropertyName()).thenReturn(mockFeatureProperty);
            when(mockFeatureMetacardType.getAttributeDescriptor(mockFeatureProperty)).thenReturn(mockFeatureAttributeDescriptor);
            MetacardMapper mockMapper = mock(MetacardMapper.class);
            when(mockMapper.getFeatureProperty(mockMetacardAttribute)).thenReturn(mockFeatureProperty);
            delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                    MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN, mockMapper,
                    WfsConstants.LAT_LON_ORDER);
            return this;
        }
    }
}
