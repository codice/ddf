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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.pattern.PatternMatcher.matchesPattern;
import static org.hamcrest.text.pattern.Patterns.anyCharacterIn;
import static org.hamcrest.text.pattern.Patterns.oneOrMore;
import static org.hamcrest.text.pattern.Patterns.sequence;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import net.opengis.filter.v_2_0_0.BinaryTemporalOpType;
import net.opengis.filter.v_2_0_0.FilterCapabilities;
import net.opengis.filter.v_2_0_0.FilterType;
import net.opengis.gml.v_3_2_0.TimeInstantType;
import net.opengis.gml.v_3_2_0.TimePeriodType;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureAttributeDescriptor;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.COMPARISON_OPERATORS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.SPATIAL_OPERATORS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.TEMPORAL_OPERATORS;
import org.hamcrest.text.pattern.PatternMatcher;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestWfsFilterDelegate {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TestWfsFilterDelegate.class);
    
    private static final String FILTER_QNAME_LOCAL_PART = "Filter";

    private FeatureMetacardType mockFeatureMetacardType = mock(FeatureMetacardType.class);

    @Test
    public void testFullFilterCapabilities() {
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN);
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
                Wfs20Constants.EPSG_4326_URN);
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
                Wfs20Constants.EPSG_4326_URN);
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
                Wfs20Constants.EPSG_4326_URN);
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
                Wfs20Constants.EPSG_4326_URN);
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
    /**
     * Doing a Absolute query from the search UI creates a During filter with the selected Begin and End date/times.
     * 
     * Example During filter:
     * 
     *   <Filter>
     *     <During>
     *         <ValueReference>myPropertyName</ValueReference>
     *         <ns4:TimePeriod ns4:id="myType.1406219647420">
     *             <ns4:beginPosition>1974-08-01T16:29:45.430-07:00</ns4:beginPosition>
     *             <ns4:endPosition>2014-07-22T16:29:45.430-07:00</ns4:endPosition>
     *         </ns4:TimePeriod>
     *     </During>
     * </Filter>
     * 
     **/
    public void testDuring_PropertyIsOfTemporalType() throws Exception {
        // Setup
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        List<String> mockProperties = new ArrayList<String>(1);
        mockProperties.add(mockProperty);
        when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getName()).thenReturn(mockType);
        when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockProperties);
        FeatureAttributeDescriptor mockFeatureAttributeDescriptor = mock(FeatureAttributeDescriptor.class);
        when(mockFeatureAttributeDescriptor.isIndexed()).thenReturn(true);
        when(mockFeatureAttributeDescriptor.getPropertyName()).thenReturn(mockProperty);
        when(mockFeatureMetacardType.getAttributeDescriptor(mockProperty)).thenReturn(mockFeatureAttributeDescriptor);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN);
        DateTime startDate = new DateTime().minusDays(365);
        DateTime endDate = new DateTime().minusDays(10);
        
        // Perform Test
        FilterType filter = delegate.during(mockProperty, startDate.toDate(), endDate.toDate());

        //Verify
        assertThat(filter.getTemporalOps().getName().toString(), is("{http://www.opengis.net/fes/2.0}During"));
        BinaryTemporalOpType binaryTemporalOpType = (BinaryTemporalOpType) filter.getTemporalOps().getValue();
        assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
        assertThat(binaryTemporalOpType.getValueReference(), is(mockProperty));
        assertThat(binaryTemporalOpType.isSetExpression(), is(true));
        TimePeriodType timePeriod = (TimePeriodType) binaryTemporalOpType.getExpression().getValue();
        assertThat(timePeriod.getBeginPosition().getValue().get(0), is(startDate.toString()));
        assertThat(timePeriod.getEndPosition().getValue().get(0), is(endDate.toString()));
        assertThat(timePeriod.getId(), is(matchesPattern(new PatternMatcher(sequence(mockType, ".", oneOrMore(anyCharacterIn("0-9")))))));
    }
    
    @Test(expected=IllegalArgumentException.class)
    /**
     * Verify that when Feature property "myPropertyName" is not defined in the Feature schema as a {http://www.opengis.net/gml/3.2}TimePeriodType
     * an IllegalArgumentException is thrown.
     */
    public void testDuring_PropertyIsNotOfTemporalType() {
        // Setup
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        List<String> mockProperties = new ArrayList<String>(1);
        mockProperties.add(mockProperty);
        when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getName()).thenReturn(mockType);
        List<String> mockTemporalProperties = Collections.emptyList();
        when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockTemporalProperties);
        FeatureAttributeDescriptor mockFeatureAttributeDescriptor = mock(FeatureAttributeDescriptor.class);
        when(mockFeatureAttributeDescriptor.isIndexed()).thenReturn(true);
        when(mockFeatureAttributeDescriptor.getPropertyName()).thenReturn(mockProperty);
        when(mockFeatureMetacardType.getAttributeDescriptor(mockProperty)).thenReturn(mockFeatureAttributeDescriptor);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN);
        DateTime startDate = new DateTime().minusDays(365);
        DateTime endDate = new DateTime().minusDays(10);
        
        // Perform Test
        FilterType filter = delegate.during(mockProperty, startDate.toDate(), endDate.toDate());
    }
    
    @Test
    /**
     * Doing a Relative query from the search UI creates a During filter with the selected End date/time and the
     * Begin date/time calculated based on the duration.
     * 
     *   <Filter>
     *     <During>
     *         <ValueReference>myPropertyName</ValueReference>
     *         <ns4:TimePeriod ns4:id="myType.1406219647420">
     *             <ns4:beginPosition>1974-08-01T16:29:45.430-07:00</ns4:beginPosition>
     *             <ns4:endPosition>2014-07-22T16:29:45.430-07:00</ns4:endPosition>
     *         </ns4:TimePeriod>
     *     </During>
     * </Filter>
     * 
     **/
    public void testRelative_PropertyIsOfTemporalType() throws Exception {
        // Setup
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        List<String> mockProperties = new ArrayList<String>(1);
        mockProperties.add(mockProperty);
        when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getName()).thenReturn(mockType);
        when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockProperties);
        FeatureAttributeDescriptor mockFeatureAttributeDescriptor = mock(FeatureAttributeDescriptor.class);
        when(mockFeatureAttributeDescriptor.isIndexed()).thenReturn(true);
        when(mockFeatureAttributeDescriptor.getPropertyName()).thenReturn(mockProperty);
        when(mockFeatureMetacardType.getAttributeDescriptor(mockProperty)).thenReturn(mockFeatureAttributeDescriptor);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN);
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
        
        DateTime startDate = now.minus(duration);
   
        
        // Perform Test
        FilterType filter = delegate.relative(mockProperty, duration);

        //Verify
        assertThat(filter.getTemporalOps().getName().toString(), is("{http://www.opengis.net/fes/2.0}During"));
        BinaryTemporalOpType binaryTemporalOpType = (BinaryTemporalOpType) filter.getTemporalOps().getValue();
        assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
        assertThat(binaryTemporalOpType.getValueReference(), is(mockProperty));
        assertThat(binaryTemporalOpType.isSetExpression(), is(true));
        TimePeriodType timePeriod = (TimePeriodType) binaryTemporalOpType.getExpression().getValue();
        assertThat(timePeriod.getBeginPosition().getValue().get(0), is(startDate.toString()));
        assertThat(timePeriod.getEndPosition().getValue().get(0), is(now.toString()));
        assertThat(timePeriod.getId(), is(matchesPattern(new PatternMatcher(sequence(mockType, ".", oneOrMore(anyCharacterIn("0-9")))))));
        
        // Reset the System time
        DateTimeUtils.setCurrentMillisSystem();
    }
    
    @Test(expected=IllegalArgumentException.class)
    /**
     * Verify that when Feature property "myPropertyName" is not defined in the Feature schema as a {http://www.opengis.net/gml/3.2}TimePeriodType
     * an IllegalArgumentException is thrown.
     */
    public void testRelative_PropertyIsNotOfTemporalType() {
        // Setup
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        List<String> mockProperties = new ArrayList<String>(1);
        mockProperties.add(mockProperty);
        when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getName()).thenReturn(mockType);
        List<String> mockTemporalProperties = Collections.emptyList();
        when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockTemporalProperties);
        FeatureAttributeDescriptor mockFeatureAttributeDescriptor = mock(FeatureAttributeDescriptor.class);
        when(mockFeatureAttributeDescriptor.isIndexed()).thenReturn(true);
        when(mockFeatureAttributeDescriptor.getPropertyName()).thenReturn(mockProperty);
        when(mockFeatureMetacardType.getAttributeDescriptor(mockProperty)).thenReturn(mockFeatureAttributeDescriptor);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN);
        long duration = 604800000;

        // Perform Test
        FilterType filter = delegate.relative(mockProperty, duration);
    }
    
    @Test
    /**
     * Example After filter:
     * 
     *    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
     *    <ns5:Filter xmlns:ns2="http://www.opengis.net/ows/1.1" xmlns="http://www.opengis.net/fes/2.0" xmlns:ns4="http://www.opengis.net/gml" xmlns:ns3="http://www.w3.org/1999/xlink" xmlns:ns5="http://www.opengis.net/ogc">
     *        <After>
     *            <ValueReference>myPropertyName</ValueReference>
     *            <ns4:TimeInstant ns4:id="myType.1406219647420">
     *                <ns4:timePosition>2013-07-23T14:02:09.239-07:00</ns4:timePosition>
     *            </ns4:TimeInstant>
     *         </After>
     *     </ns5:Filter>
     */
    public void testAfter_PropertyIsOfTemporalType() throws Exception {
        // Setup
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        List<String> mockProperties = new ArrayList<String>(1);
        mockProperties.add(mockProperty);
        when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getName()).thenReturn(mockType);
        when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockProperties);
        FeatureAttributeDescriptor mockFeatureAttributeDescriptor = mock(FeatureAttributeDescriptor.class);
        when(mockFeatureAttributeDescriptor.isIndexed()).thenReturn(true);
        when(mockFeatureAttributeDescriptor.getPropertyName()).thenReturn(mockProperty);
        when(mockFeatureMetacardType.getAttributeDescriptor(mockProperty)).thenReturn(mockFeatureAttributeDescriptor);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN);
        DateTime date = new DateTime().minusDays(365);
        
        // Perform Test
        FilterType filter = delegate.after(mockProperty, date.toDate());

        //Verify
        assertThat(filter.getTemporalOps().getName().toString(), is("{http://www.opengis.net/fes/2.0}After"));
        BinaryTemporalOpType binaryTemporalOpType = (BinaryTemporalOpType) filter.getTemporalOps().getValue();
        assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
        assertThat(binaryTemporalOpType.getValueReference(), is(mockProperty));
        assertThat(binaryTemporalOpType.isSetExpression(), is(true));
        TimeInstantType timeInstant = (TimeInstantType) binaryTemporalOpType.getExpression().getValue();
        assertThat(timeInstant.getTimePosition().getValue().get(0), is(date.toString()));
        assertThat(timeInstant.getId(), is(matchesPattern(new PatternMatcher(sequence(mockType, ".", oneOrMore(anyCharacterIn("0-9")))))));
    }
    
    @Test(expected=IllegalArgumentException.class)
    /**
     * Verify that when Feature property "myPropertyName" is not defined in the Feature schema as a {http://www.opengis.net/gml/3.2}TimeInstantType
     * an IllegalArgumentException is thrown.
     */
    public void testAfter_PropertyIsNotOfTemporalType() {
        // Setup
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        List<String> mockProperties = new ArrayList<String>(1);
        mockProperties.add(mockProperty);
        when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getName()).thenReturn(mockType);
        List<String> mockTemporalProperties = Collections.emptyList();
        when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockTemporalProperties);
        FeatureAttributeDescriptor mockFeatureAttributeDescriptor = mock(FeatureAttributeDescriptor.class);
        when(mockFeatureAttributeDescriptor.isIndexed()).thenReturn(true);
        when(mockFeatureAttributeDescriptor.getPropertyName()).thenReturn(mockProperty);
        when(mockFeatureMetacardType.getAttributeDescriptor(mockProperty)).thenReturn(mockFeatureAttributeDescriptor);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN);
        DateTime date = new DateTime().minusDays(365);
        
        // Perform Test
        FilterType filter = delegate.after(mockProperty, date.toDate());
    }
    
    @Test
    /**
     * Example Before filter:
     * 
     * <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
     * <ns5:Filter xmlns:ns2="http://www.opengis.net/ows/1.1" xmlns="http://www.opengis.net/fes/2.0" xmlns:ns4="http://www.opengis.net/gml" xmlns:ns3="http://www.w3.org/1999/xlink" xmlns:ns5="http://www.opengis.net/ogc">
     *     <Before>
     *         <ValueReference>myPropertyName</ValueReference>
     *         <ns4:TimeInstant ns4:id="myType.1406219647420">
     *             <ns4:timePosition>2013-07-23T14:04:50.853-07:00</ns4:timePosition>
     *         </ns4:TimeInstant>
     *     </Before>
     * </ns5:Filter>
     * 
     */
    public void testBefore_PropertyIsOfTemporalType() throws Exception {
        // Setup
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        List<String> mockProperties = new ArrayList<String>(1);
        mockProperties.add(mockProperty);
        when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getName()).thenReturn(mockType);
        when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockProperties);
        FeatureAttributeDescriptor mockFeatureAttributeDescriptor = mock(FeatureAttributeDescriptor.class);
        when(mockFeatureAttributeDescriptor.isIndexed()).thenReturn(true);
        when(mockFeatureAttributeDescriptor.getPropertyName()).thenReturn(mockProperty);
        when(mockFeatureMetacardType.getAttributeDescriptor(mockProperty)).thenReturn(mockFeatureAttributeDescriptor);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN);
        DateTime date = new DateTime().minusDays(365);
        
        // Perform Test
        FilterType filter = delegate.before(mockProperty, date.toDate());

        //Verify
        assertThat(filter.getTemporalOps().getName().toString(), is("{http://www.opengis.net/fes/2.0}Before"));
        BinaryTemporalOpType binaryTemporalOpType = (BinaryTemporalOpType) filter.getTemporalOps().getValue();
        assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
        assertThat(binaryTemporalOpType.getValueReference(), is(mockProperty));
        assertThat(binaryTemporalOpType.isSetExpression(), is(true));
        TimeInstantType timeInstant = (TimeInstantType) binaryTemporalOpType.getExpression().getValue();
        assertThat(timeInstant.getTimePosition().getValue().get(0), is(date.toString()));
        assertThat(timeInstant.getId(), is(matchesPattern(new PatternMatcher(sequence(mockType, ".", oneOrMore(anyCharacterIn("0-9")))))));
    }
    
    @Test(expected=IllegalArgumentException.class)
    /**
     * Verify that when Feature property "myPropertyName" is not defined in the Feature schema as a {http://www.opengis.net/gml/3.2}TimeInstantType
     * an IllegalArgumentException is thrown.
     */
    public void testBefore_PropertyIsNotOfTemporalType() {
        // Setup
        String mockProperty = "myPropertyName";
        String mockType = "myType";
        List<String> mockProperties = new ArrayList<String>(1);
        mockProperties.add(mockProperty);
        when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
        when(mockFeatureMetacardType.getName()).thenReturn(mockType);
        List<String> mockTemporalProperties = Collections.emptyList();
        when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockTemporalProperties);
        FeatureAttributeDescriptor mockFeatureAttributeDescriptor = mock(FeatureAttributeDescriptor.class);
        when(mockFeatureAttributeDescriptor.isIndexed()).thenReturn(true);
        when(mockFeatureAttributeDescriptor.getPropertyName()).thenReturn(mockProperty);
        when(mockFeatureMetacardType.getAttributeDescriptor(mockProperty)).thenReturn(mockFeatureAttributeDescriptor);
        WfsFilterDelegate delegate = new WfsFilterDelegate(mockFeatureMetacardType,
                MockWfsServer.getFilterCapabilities(), Wfs20Constants.EPSG_4326_URN);
        DateTime date = new DateTime().minusDays(365);
        
        // Perform Test
        FilterType filter = delegate.before(mockProperty, date.toDate());
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

        String contextPath = StringUtils.join(new String[] {Wfs20Constants.OGC_FILTER_PACKAGE,
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
    
}
