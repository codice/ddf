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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.source;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ObjectArrays;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
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
import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.COMPARISON_OPERATORS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.SPATIAL_OPERATORS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.TEMPORAL_OPERATORS;
import org.custommonkey.xmlunit.XMLUnit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class WfsFilterDelegateTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(WfsFilterDelegateTest.class);

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

  private String mockMetacardAttribute = "modified";

  private String mockFeatureType = "mockFeatureType";

  private String mockFeatureProperty = "mockFeatureProperty";

  private MetacardMapper mockMapper = mock(MetacardMapper.class);

  private FeatureMetacardType mockFeatureMetacardType = mock(FeatureMetacardType.class);

  @BeforeClass
  public static void setUp() {
    XMLUnit.setNormalizeWhitespace(true);
    XMLUnit.setIgnoreWhitespace(true);
  }

  private static JAXBContext initJaxbContext() {
    JAXBContext localJaxbContext = null;

    String contextPath =
        StringUtils.join(
            new String[] {
              Wfs20Constants.OGC_FILTER_PACKAGE,
              Wfs20Constants.OGC_GML_PACKAGE,
              Wfs20Constants.OGC_OWS_PACKAGE
            },
            ":");

    try {
      LOGGER.debug("Creating JAXB context with context path: {}", contextPath);
      localJaxbContext = JAXBContext.newInstance(contextPath);
    } catch (JAXBException e) {
      LOGGER.error("Unable to create JAXB context using contextPath: {}", contextPath, e);
    }

    return localJaxbContext;
  }

  @Test
  public void testFullFilterCapabilities() {
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);
    assertThat(delegate.isLogicalOps(), is(true));
    assertThat(delegate.isEpsg4326(), is(true));
    assertThat(delegate.isSortingSupported(), is(true));
    assertThat(delegate.getSrsName(), is(GeospatialUtil.EPSG_4326_URN));
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
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            capabilities,
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);
    assertThat(delegate.isLogicalOps(), is(true));
    assertThat(delegate.isEpsg4326(), is(true));
    assertThat(delegate.isSortingSupported(), is(false));
    assertThat(delegate.getSrsName(), is(GeospatialUtil.EPSG_4326_URN));
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
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            capabilities,
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);
    assertThat(delegate.isLogicalOps(), is(false));
    assertThat(delegate.isEpsg4326(), is(true));
    assertThat(delegate.isSortingSupported(), is(true));
    assertThat(delegate.getSrsName(), is(GeospatialUtil.EPSG_4326_URN));
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
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            capabilities,
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);
    assertThat(delegate.isLogicalOps(), is(true));
    assertThat(delegate.isEpsg4326(), is(true));
    assertThat(delegate.isSortingSupported(), is(true));
    assertThat(delegate.getSrsName(), is(GeospatialUtil.EPSG_4326_URN));
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
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            capabilities,
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);
    assertThat(delegate.isLogicalOps(), is(true));
    assertThat(delegate.isEpsg4326(), is(true));
    assertThat(delegate.isSortingSupported(), is(true));
    assertThat(delegate.getSrsName(), is(GeospatialUtil.EPSG_4326_URN));
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
        List<Object> values = new ArrayList<>();
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
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            capabilities,
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    // Verify
    assertThat(delegate.isSortingSupported(), is(true));
    assertThat(delegate.getAllowedSortOrders().size(), is(2));
    assertThat(delegate.getAllowedSortOrders().contains(SortOrder.ASCENDING), is(true));
    assertThat(delegate.getAllowedSortOrders().contains(SortOrder.DESCENDING), is(true));
  }

  @Test
  /**
   * Doing a Absolute query from the search UI creates a During filter with the selected Begin and
   * End date/times.
   *
   * <p>Example During filter:
   *
   * <p><Filter> <During> <ValueReference>myFeatureProperty</ValueReference> <ns4:TimePeriod
   * ns4:id="myFeatureType.1406219647420">
   * <ns4:beginPosition>1974-08-01T16:29:45.430-07:00</ns4:beginPosition>
   * <ns4:endPosition>2014-07-22T16:29:45.430-07:00</ns4:endPosition> </ns4:TimePeriod> </During>
   * </Filter>
   */
  public void testDuringPropertyIsOfTemporalType() {
    SequentialTestMockHolder sequentialTestMockHolder = new SequentialTestMockHolder().invoke();
    WfsFilterDelegate delegate = sequentialTestMockHolder.getDelegate();
    String mockMetacardAttribute = sequentialTestMockHolder.getMockMetacardAttribute();
    String mockFeatureProperty = sequentialTestMockHolder.getMockFeatureProperty();
    String mockFeatureType = sequentialTestMockHolder.getMockFeatureType();

    DateTime startDate = new DateTime(2014, 01, 01, 01, 01, 01, 123, DateTimeZone.forID("-07:00"));
    DateTime endDate = new DateTime(2014, 01, 02, 01, 01, 01, 123, DateTimeZone.forID("-07:00"));

    // Perform Test
    FilterType filter =
        delegate.during(mockMetacardAttribute, startDate.toDate(), endDate.toDate());

    // Verify
    assertThat(
        filter.getTemporalOps().getName().toString(), is("{http://www.opengis.net/fes/2.0}During"));
    BinaryTemporalOpType binaryTemporalOpType =
        (BinaryTemporalOpType) filter.getTemporalOps().getValue();
    assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
    assertThat(binaryTemporalOpType.getValueReference(), is(mockFeatureProperty));
    assertThat(binaryTemporalOpType.isSetExpression(), is(true));
    TimePeriodType timePeriod = (TimePeriodType) binaryTemporalOpType.getExpression().getValue();

    assertThat(timePeriod.getBeginPosition().getValue().get(0), is("2014-01-01T08:01:01Z"));
    assertThat(timePeriod.getEndPosition().getValue().get(0), is("2014-01-02T08:01:01Z"));
    assertThat(
        "Strings matches expected pattern",
        timePeriod.getId().matches(getRegEx(mockFeatureType)),
        equalTo(true));
  }

  private String getRegEx(String mockFeatureType) {
    return mockFeatureType + "\\.\\d+";
  }

  @Test(expected = IllegalArgumentException.class)
  /**
   * Verify that when Feature property "myFeatureProperty" is not defined in the Feature schema as a
   * {http://www.opengis.net/gml/3.2}TimePeriodType an IllegalArgumentException is thrown.
   */
  public void testDuringPropertyIsNotOfTemporalType() throws Throwable {
    testSequentialPropertyIsNotOfTemporalType(
        "during", new DateTime().minusDays(365).toDate(), new DateTime().minusDays(10).toDate());
  }

  @Test
  public void testDuringFilterWithMetacardAttributeMappedToFeatureProperty() throws Exception {
    doReturn(mockFeatureProperty).when(mockMapper).getFeatureProperty(Core.CREATED);
    doReturn(singletonList(mockFeatureProperty))
        .when(mockFeatureMetacardType)
        .getTemporalProperties();
    doReturn(true).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    final FilterType filter = delegate.during(Core.CREATED, new Date(1), new Date(2));
    final String filterXml = getXmlFromMarshaller(filter);

    final Map<String, String> namespaceContext =
        new ImmutableMap.Builder<String, String>()
            .put("fes", "http://www.opengis.net/fes/2.0")
            .put("ogc", "http://www.opengis.net/ogc")
            .build();

    assertThat(
        filterXml,
        hasXPath("/ogc:Filter/fes:During/fes:ValueReference/text()", is(mockFeatureProperty))
            .withNamespaceContext(namespaceContext));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDuringFilterFeaturePropertyIsNotQueryable() {
    doReturn(mockFeatureProperty).when(mockMapper).getFeatureProperty(Core.CREATED);
    doReturn(singletonList(mockFeatureProperty))
        .when(mockFeatureMetacardType)
        .getTemporalProperties();
    doReturn(false).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    delegate.during(Core.CREATED, new Date(1), new Date(2));
  }

  @Test
  /**
   * Doing a Relative query from the search UI creates a During filter with the selected End
   * date/time and the Begin date/time calculated based on the duration.
   *
   * <p><Filter> <During> <ValueReference>myFeatureProperty</ValueReference> <ns4:TimePeriod
   * ns4:id="myFeatureType.1406219647420">
   * <ns4:beginPosition>1974-08-01T16:29:45.430-07:00</ns4:beginPosition>
   * <ns4:endPosition>2014-07-22T16:29:45.430-07:00</ns4:endPosition> </ns4:TimePeriod> </During>
   * </Filter>
   */
  public void testRelativePropertyIsOfTemporalType() {
    // Setup
    SequentialTestMockHolder sequentialTestMockHolder = new SequentialTestMockHolder().invoke();
    WfsFilterDelegate delegate = sequentialTestMockHolder.getDelegate();
    String mockMetacardAttribute = sequentialTestMockHolder.getMockMetacardAttribute();
    String mockFeatureProperty = sequentialTestMockHolder.getMockFeatureProperty();
    String mockFeatureType = sequentialTestMockHolder.getMockFeatureType();

    long duration = 604800000;
    DateTime now = new DateTime();
    /**
     * When delegate.relative(mockProperty, duration) is called, the current time (now) is
     * calculated and used for the end date/time and the start date/time is calculated based on the
     * end date/time and duration (now - duration). Once we get the current time (now) in the test,
     * we want to hold the System time fixed so when the current time (now) is retrieved in
     * delegate.relative(mockProperty, duration) there is no discrepancy. This allows us to easily
     * assert the begin position and end position in the Verify step of this test.
     */
    DateTimeUtils.setCurrentMillisFixed(now.getMillis());

    String startDate =
        ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC).print(now.minus(duration));

    // Perform Test
    FilterType filter = delegate.relative(mockMetacardAttribute, duration);

    // Verify
    assertThat(
        filter.getTemporalOps().getName().toString(), is("{http://www.opengis.net/fes/2.0}During"));
    BinaryTemporalOpType binaryTemporalOpType =
        (BinaryTemporalOpType) filter.getTemporalOps().getValue();
    assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
    assertThat(binaryTemporalOpType.getValueReference(), is(mockFeatureProperty));
    assertThat(binaryTemporalOpType.isSetExpression(), is(true));
    TimePeriodType timePeriod = (TimePeriodType) binaryTemporalOpType.getExpression().getValue();
    assertThat(timePeriod.getBeginPosition().getValue().get(0), is(startDate));
    assertThat(
        timePeriod.getEndPosition().getValue().get(0),
        is(ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC).print(now)));
    assertThat(
        "Strings matches expected pattern",
        timePeriod.getId().matches(getRegEx(mockFeatureType)),
        equalTo(true));

    // Reset the System time
    DateTimeUtils.setCurrentMillisSystem();
  }

  /**
   * Verify that when Feature property "myFeatureProperty" is not defined in the Feature schema as a
   * {http://www.opengis.net/gml/3.2}TimePeriodType an IllegalArgumentException is thrown.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testRelativePropertyIsNotOfTemporalType() throws Throwable {
    testSequentialPropertyIsNotOfTemporalType("relative", 604800000L);
  }

  @Test
  public void testRelativeFilterWithMetacardAttributeMappedToFeatureProperty() throws Exception {
    doReturn(mockFeatureProperty).when(mockMapper).getFeatureProperty(Core.CREATED);
    doReturn(singletonList(mockFeatureProperty))
        .when(mockFeatureMetacardType)
        .getTemporalProperties();
    doReturn(true).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    final FilterType filter = delegate.relative(Core.CREATED, TimeUnit.DAYS.toMillis(1));
    final String filterXml = getXmlFromMarshaller(filter);

    final Map<String, String> namespaceContext =
        new ImmutableMap.Builder<String, String>()
            .put("fes", "http://www.opengis.net/fes/2.0")
            .put("ogc", "http://www.opengis.net/ogc")
            .build();

    assertThat(
        filterXml,
        hasXPath("/ogc:Filter/fes:During/fes:ValueReference/text()", is(mockFeatureProperty))
            .withNamespaceContext(namespaceContext));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRelativeFilterFeaturePropertyIsNotQueryable() {
    doReturn(mockFeatureProperty).when(mockMapper).getFeatureProperty(Core.CREATED);
    doReturn(singletonList(mockFeatureProperty))
        .when(mockFeatureMetacardType)
        .getTemporalProperties();
    doReturn(false).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    delegate.relative(Core.CREATED, TimeUnit.DAYS.toMillis(1));
  }

  /**
   * Example After filter:
   *
   * <p><?xml version="1.0" encoding="UTF-8" standalone="yes"?> <ns5:Filter
   * xmlns:ns2="http://www.opengis.net/ows/1.1" xmlns="http://www.opengis.net/fes/2.0"
   * xmlns:ns4="http://www.opengis.net/gml" xmlns:ns3="http://www.w3.org/1999/xlink"
   * xmlns:ns5="http://www.opengis.net/ogc"> <After>
   * <ValueReference>myFeatureProperty</ValueReference> <ns4:TimeInstant
   * ns4:id="myFeatureType.1406219647420">
   * <ns4:timePosition>2013-07-23T14:02:09.239-07:00</ns4:timePosition> </ns4:TimeInstant> </After>
   * </ns5:Filter>
   */
  @Test
  public void testAfterPropertyIsOfTemporalType() throws Exception {
    testSequentialPropertyIsOfTemporalType("after", "{http://www.opengis.net/fes/2.0}After");
  }

  /**
   * Verify that when Feature property "myFeatureProperty" is not defined in the Feature schema as a
   * {http://www.opengis.net/gml/3.2}TimeInstantType an IllegalArgumentException is thrown.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testAfterPropertyIsNotOfTemporalType() throws Throwable {
    testSequentialPropertyIsNotOfTemporalType("after", new DateTime().minusDays(365).toDate());
  }

  @Test
  public void testAfterFilterWithMetacardAttributeMappedToFeatureProperty() throws Exception {
    doReturn(mockFeatureProperty).when(mockMapper).getFeatureProperty(Core.CREATED);
    doReturn(singletonList(mockFeatureProperty))
        .when(mockFeatureMetacardType)
        .getTemporalProperties();
    doReturn(true).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    final FilterType filter = delegate.after(Core.CREATED, new Date());
    final String filterXml = getXmlFromMarshaller(filter);

    final Map<String, String> namespaceContext =
        new ImmutableMap.Builder<String, String>()
            .put("fes", "http://www.opengis.net/fes/2.0")
            .put("ogc", "http://www.opengis.net/ogc")
            .build();

    assertThat(
        filterXml,
        hasXPath("/ogc:Filter/fes:After/fes:ValueReference/text()", is(mockFeatureProperty))
            .withNamespaceContext(namespaceContext));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAfterFilterFeaturePropertyIsNotQueryable() {
    doReturn(mockFeatureProperty).when(mockMapper).getFeatureProperty(Core.CREATED);
    doReturn(singletonList(mockFeatureProperty))
        .when(mockFeatureMetacardType)
        .getTemporalProperties();
    doReturn(false).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    delegate.after(Core.CREATED, new Date());
  }

  /**
   * Example Before filter:
   *
   * <p><?xml version="1.0" encoding="UTF-8" standalone="yes"?> <ns5:Filter
   * xmlns:ns2="http://www.opengis.net/ows/1.1" xmlns="http://www.opengis.net/fes/2.0"
   * xmlns:ns4="http://www.opengis.net/gml" xmlns:ns3="http://www.w3.org/1999/xlink"
   * xmlns:ns5="http://www.opengis.net/ogc"> <Before>
   * <ValueReference>myFeatureProperty</ValueReference> <ns4:TimeInstant
   * ns4:id="myFeatureType.1406219647420">
   * <ns4:timePosition>2013-07-23T14:04:50.853-07:00</ns4:timePosition> </ns4:TimeInstant> </Before>
   * </ns5:Filter>
   */
  @Test
  public void testBeforePropertyIsOfTemporalType() throws Exception {
    testSequentialPropertyIsOfTemporalType("before", "{http://www.opengis.net/fes/2.0}Before");
  }

  private void testSequentialPropertyIsOfTemporalType(String methName, String temporalOpName)
      throws Exception {
    SequentialTestMockHolder sequentialTestMockHolder = new SequentialTestMockHolder().invoke();
    WfsFilterDelegate delegate = sequentialTestMockHolder.getDelegate();
    String mockMetacardAttribute = sequentialTestMockHolder.getMockMetacardAttribute();
    String mockFeatureProperty = sequentialTestMockHolder.getMockFeatureProperty();
    String mockFeatureType = sequentialTestMockHolder.getMockFeatureType();

    DateTime date = new DateTime().minusDays(365);

    // Perform Test
    Method method = WfsFilterDelegate.class.getMethod(methName, String.class, Date.class);
    FilterType filter = (FilterType) method.invoke(delegate, mockMetacardAttribute, date.toDate());

    // Verify
    assertThat(filter.getTemporalOps().getName().toString(), is(temporalOpName));
    BinaryTemporalOpType binaryTemporalOpType =
        (BinaryTemporalOpType) filter.getTemporalOps().getValue();
    assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
    assertThat(binaryTemporalOpType.getValueReference(), is(mockFeatureProperty));
    assertThat(binaryTemporalOpType.isSetExpression(), is(true));
    TimeInstantType timeInstant = (TimeInstantType) binaryTemporalOpType.getExpression().getValue();
    assertThat(
        timeInstant.getTimePosition().getValue().get(0),
        is(ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC).print(date)));

    assertThat(
        "Strings matches expected pattern",
        timeInstant.getId().matches(getRegEx(mockFeatureType)),
        equalTo(true));
  }

  /**
   * Verify that when Feature property "myFeatureProperty" is not defined in the Feature schema as a
   * {http://www.opengis.net/gml/3.2}TimeInstantType an IllegalArgumentException is thrown.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testBeforePropertyIsNotOfTemporalType() throws Throwable {
    testSequentialPropertyIsNotOfTemporalType("before", new DateTime().minusDays(365).toDate());
  }

  @Test
  public void testBeforeFilterWithMetacardAttributeMappedToFeatureProperty() throws Exception {
    doReturn(mockFeatureProperty).when(mockMapper).getFeatureProperty(Core.CREATED);
    doReturn(singletonList(mockFeatureProperty))
        .when(mockFeatureMetacardType)
        .getTemporalProperties();
    doReturn(true).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    final FilterType filter = delegate.before(Core.CREATED, new Date());
    final String filterXml = getXmlFromMarshaller(filter);

    final Map<String, String> namespaceContext =
        new ImmutableMap.Builder<String, String>()
            .put("fes", "http://www.opengis.net/fes/2.0")
            .put("ogc", "http://www.opengis.net/ogc")
            .build();

    assertThat(
        filterXml,
        hasXPath("/ogc:Filter/fes:Before/fes:ValueReference/text()", is(mockFeatureProperty))
            .withNamespaceContext(namespaceContext));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBeforeFilterFeaturePropertyIsNotQueryable() {
    doReturn(mockFeatureProperty).when(mockMapper).getFeatureProperty(Core.CREATED);
    doReturn(singletonList(mockFeatureProperty))
        .when(mockFeatureMetacardType)
        .getTemporalProperties();
    doReturn(false).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    delegate.before(Core.CREATED, new Date());
  }

  private void testSequentialPropertyIsNotOfTemporalType(String methName, Object... inputParams)
      throws Throwable {
    String mockMetacardAttribute = "myMetacardAttribute";
    String mockFeatureType = "myFeatureType";
    String mockFeatureProperty = "myFeatureProperty";
    List<String> mockProperties = new ArrayList<>(1);
    mockProperties.add(mockFeatureProperty);
    when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
    when(mockFeatureMetacardType.getName()).thenReturn(mockFeatureType);
    List<String> mockTemporalProperties = emptyList();
    when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockTemporalProperties);
    when(mockFeatureMetacardType.isQueryable(mockFeatureProperty)).thenReturn(true);
    when(mockMapper.getFeatureProperty(mockMetacardAttribute)).thenReturn(mockFeatureProperty);
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    try {
      // Inject the mockMetacardAttribute at the head of the array
      Object[] methParams = ObjectArrays.concat(mockMetacardAttribute, inputParams);
      // Generate the array of class types for the reflection call
      Class<?>[] classTypes =
          FluentIterable.from(Arrays.asList(methParams))
              .transform(
                  new Function<Object, Class>() {
                    @Override
                    public Class<?> apply(Object o) {
                      // Autoboxing is a small problem with reflection when trying to be too clever
                      return (o instanceof Long) ? long.class : o.getClass();
                    }
                  })
              .toArray(Class.class);

      Method method = WfsFilterDelegate.class.getMethod(methName, classTypes);
      method.invoke(delegate, methParams);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  @Test
  public void testLogicalNotOfComparison() {

    String mockProperty = "myPropertyName";
    String mockType = "myType";
    WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

    FilterType filterToBeNoted = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);

    // Perform Test
    FilterType filter = delegate.not(filterToBeNoted);

    // Verify
    assertThat(filter.getLogicOps().getName().toString(), is(LOGICAL_NOT_NAME));
    UnaryLogicOpType logicOpType = (UnaryLogicOpType) filter.getLogicOps().getValue();

    PropertyIsLikeType compOpsType1 =
        (PropertyIsLikeType) logicOpType.getComparisonOps().getValue();
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
    List<FilterType> filtersToCombine = new ArrayList<>();
    filtersToCombine.add(compFilter1);
    filtersToCombine.add(compFilter2);

    // Perform Test
    Method method = WfsFilterDelegate.class.getMethod(methName, List.class);
    FilterType filter = (FilterType) method.invoke(delegate, filtersToCombine);

    // Verify
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
  public void testLogicalNotOfSpatial() {

    String mockProperty = "myPropertyName";
    String mockType = "myType";
    WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

    FilterType spatialFilter1 = delegate.dwithin(Metacard.ANY_GEO, "POINT (30 10)", 1000);

    // Perform Test
    FilterType filter = delegate.not(spatialFilter1);

    // Verify
    assertThat(filter.getLogicOps().getName().toString(), is(LOGICAL_NOT_NAME));
    UnaryLogicOpType logicOpType = (UnaryLogicOpType) filter.getLogicOps().getValue();
    DistanceBufferType spatialOpsType1 =
        (DistanceBufferType) logicOpType.getSpatialOps().getValue();
    assertThat(spatialOpsType1.getDistance().getValue(), is(1000d));
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

    FilterType spatialFilter1 = delegate.dwithin(Metacard.ANY_GEO, "POINT (30 10)", 1000);
    FilterType spatialFilter2 = delegate.dwithin(Metacard.ANY_GEO, "POINT (50 10)", 1500);

    List<FilterType> filtersToCombine = new ArrayList<>();
    filtersToCombine.add(spatialFilter1);
    filtersToCombine.add(spatialFilter2);

    // Perform Test
    Method method = WfsFilterDelegate.class.getMethod(methName, List.class);
    FilterType filter = (FilterType) method.invoke(delegate, filtersToCombine);

    // Verify
    assertThat(filter.getLogicOps().getName().toString(), is(compOpName));
    BinaryLogicOpType logicOpType = (BinaryLogicOpType) filter.getLogicOps().getValue();
    DistanceBufferType spatialOpsType1 =
        (DistanceBufferType)
            logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(0).getValue();
    assertThat(spatialOpsType1.getDistance().getValue(), is(1000d));

    DistanceBufferType spatialOpsType2 =
        (DistanceBufferType)
            logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(1).getValue();
    assertThat(spatialOpsType2.getDistance().getValue(), is(1500d));
  }

  /** Verifies that a temporal criteria can be AND'ed to other criteria. */
  @Test
  public void testLogicalAndOfSpatialTemporal() {

    String mockProperty = "myPropertyName";
    String mockType = "myType";
    WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

    FilterType spatialFilter = delegate.dwithin(Metacard.ANY_GEO, "POINT (30 10)", 1000);
    FilterType temporalFilter =
        delegate.during(
            mockProperty,
            new DateTime().minusDays(365).toDate(),
            new DateTime().minusDays(10).toDate());

    List<FilterType> filtersToBeAnded =
        new ArrayList<>(Arrays.asList(spatialFilter, temporalFilter));

    // Perform Test
    FilterType filter = delegate.and(filtersToBeAnded);

    // Verify AND op used
    if (filter.getLogicOps() == null) {
      fail("No AND/OR element found in the generated FilterType.");
    }
    assertEquals(LOGICAL_AND_NAME, filter.getLogicOps().getName().toString());
    BinaryLogicOpType logicOpType = (BinaryLogicOpType) filter.getLogicOps().getValue();

    // Verify two items were AND'ed
    assertEquals(2, logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().size());

    // Verify first is spatial, second is temporal
    assertTrue(
        logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(0).getValue()
            instanceof DistanceBufferType);
    assertTrue(
        logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(1).getValue()
            instanceof BinaryTemporalOpType);
  }

  @Test
  public void testLogicalOrOfLogicals() throws Exception {
    testLogicalCombinationOfLogicals("or", LOGICAL_OR_NAME);
  }

  @Test
  public void testLogicalAndOfLogicals() throws Exception {
    testLogicalCombinationOfLogicals("and", LOGICAL_AND_NAME);
  }

  private void testLogicalCombinationOfLogicals(String methName, String compOpName)
      throws Exception {
    String mockProperty = "myPropertyName";
    String mockType = "myType";
    WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

    FilterType compFilter1 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    FilterType compFilter2 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    List<FilterType> subFiltersToBeOred = new ArrayList<>();
    subFiltersToBeOred.add(compFilter1);
    subFiltersToBeOred.add(compFilter2);

    FilterType spatialFilter1 = delegate.dwithin(Metacard.ANY_GEO, "POINT (30 10)", 1000);
    FilterType spatialFilter2 = delegate.dwithin(Metacard.ANY_GEO, "POINT (50 10)", 1500);
    List<FilterType> subFiltersToBeAnded = new ArrayList<>();
    subFiltersToBeAnded.add(spatialFilter1);
    subFiltersToBeAnded.add(spatialFilter2);

    List<FilterType> filtersToCombine = new ArrayList<>();
    filtersToCombine.add(delegate.or(subFiltersToBeOred));
    filtersToCombine.add(delegate.and(subFiltersToBeAnded));

    Method method = WfsFilterDelegate.class.getMethod(methName, List.class);
    FilterType filter = (FilterType) method.invoke(delegate, filtersToCombine);

    // Verify
    assertThat(filter.getLogicOps().getName().toString(), is(compOpName));
    BinaryLogicOpType logicOpType = (BinaryLogicOpType) filter.getLogicOps().getValue();

    BinaryLogicOpType logicOrType =
        (BinaryLogicOpType)
            logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(0).getValue();
    assertThat(
        logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(0).getName().toString(),
        is(LOGICAL_OR_NAME));
    assertThat(logicOrType.getComparisonOpsOrSpatialOpsOrTemporalOps().size(), is(2));

    for (JAXBElement<?> jaxbElement : logicOrType.getComparisonOpsOrSpatialOpsOrTemporalOps()) {
      PropertyIsLikeType compOpsType = (PropertyIsLikeType) jaxbElement.getValue();
      String valRef = fetchPropertyIsLikeExpression(compOpsType, VALUE_REFERENCE);
      assertThat(valRef, is(mockProperty));
      String literal = fetchPropertyIsLikeExpression(compOpsType, LITERAL);
      assertThat(literal, is(LITERAL));
    }

    BinaryLogicOpType logicAndType =
        (BinaryLogicOpType)
            logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(1).getValue();
    assertThat(
        logicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(1).getName().toString(),
        is(LOGICAL_AND_NAME));

    DistanceBufferType spatialOpsType1 =
        (DistanceBufferType)
            logicAndType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(0).getValue();
    assertThat(spatialOpsType1.getDistance().getValue(), is(1000d));

    DistanceBufferType spatialOpsType2 =
        (DistanceBufferType)
            logicAndType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(1).getValue();
    assertThat(spatialOpsType2.getDistance().getValue(), is(1500d));
  }

  @Test
  public void testLogicalNotOfLogicals() {
    String mockProperty = "myPropertyName";
    String mockType = "myType";
    WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

    FilterType compFilter1 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    FilterType compFilter2 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    List<FilterType> subFiltersToBeOred = new ArrayList<>();
    subFiltersToBeOred.add(compFilter1);
    subFiltersToBeOred.add(compFilter2);

    // Perform Test
    FilterType filter = delegate.not(delegate.or(subFiltersToBeOred));

    // Verify
    assertThat(filter.getLogicOps().getName().toString(), is(LOGICAL_NOT_NAME));
    UnaryLogicOpType logicOpType = (UnaryLogicOpType) filter.getLogicOps().getValue();

    BinaryLogicOpType logicOrType = (BinaryLogicOpType) logicOpType.getLogicOps().getValue();
    assertThat(logicOpType.getLogicOps().getName().toString(), is(LOGICAL_OR_NAME));

    PropertyIsLikeType compOpsType1 =
        (PropertyIsLikeType)
            logicOrType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(0).getValue();
    String valRef1 = fetchPropertyIsLikeExpression(compOpsType1, VALUE_REFERENCE);
    assertThat(valRef1, is(mockProperty));
    String literal1 = fetchPropertyIsLikeExpression(compOpsType1, LITERAL);
    assertThat(literal1, is(LITERAL));

    PropertyIsLikeType compOpsType2 =
        (PropertyIsLikeType)
            logicOrType.getComparisonOpsOrSpatialOpsOrTemporalOps().get(1).getValue();
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
    List<FilterType> filtersToCombine = new ArrayList<>();
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

  @Test(expected = UnsupportedOperationException.class)
  public void testLogicalWithNullOrEmpty() {
    String mockProperty = "myPropertyName";
    String mockType = "myType";
    WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

    // Test with null list/entry
    assertNull(delegate.and(null));
    assertNull(delegate.not(null));

    // Test with empty list
    List<FilterType> filtersToBeCombined = new ArrayList<>();
    assertNull(delegate.and(filtersToBeCombined));
    assertNull(delegate.or(filtersToBeCombined));

    // Test with list with null entries
    filtersToBeCombined.add(null);
    filtersToBeCombined.add(null);
    assertNull(delegate.and(filtersToBeCombined));
    assertNull(delegate.or(filtersToBeCombined));

    // Finally, test a null list with an or
    delegate.or(null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testLogicalAndNoLogicalSupport() {
    WfsFilterDelegate delegate = makeDelegateForLogicalSupportTests();

    FilterType compFilter1 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    FilterType compFilter2 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    List<FilterType> filtersToBeAnded = new ArrayList<>();
    filtersToBeAnded.add(compFilter1);
    filtersToBeAnded.add(compFilter2);

    // Perform Test
    delegate.and(filtersToBeAnded);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testLogicalOrNoLogicalSupport() {
    WfsFilterDelegate delegate = makeDelegateForLogicalSupportTests();

    FilterType compFilter1 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    FilterType compFilter2 = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    List<FilterType> filtersToBeOred = new ArrayList<>();
    filtersToBeOred.add(compFilter1);
    filtersToBeOred.add(compFilter2);

    // Perform Test
    delegate.or(filtersToBeOred);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testLogicalNotNoLogicalSupport() {
    WfsFilterDelegate delegate = makeDelegateForLogicalSupportTests();
    FilterType filterToBeNoted = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);

    // Perform Test
    delegate.not(filterToBeNoted);
  }

  private WfsFilterDelegate makeDelegateForLogicalSupportTests() {
    String mockProperty = "myPropertyName";
    String mockType = "myType";
    List<String> mockProperties = new ArrayList<>(1);
    mockProperties.add(mockProperty);
    when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
    when(mockFeatureMetacardType.getGmlProperties()).thenReturn(mockProperties);
    when(mockFeatureMetacardType.getTextualProperties()).thenReturn(mockProperties);
    when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockProperties);
    when(mockFeatureMetacardType.getName()).thenReturn(mockType);
    when(mockFeatureMetacardType.isQueryable(mockProperty)).thenReturn(true);

    FilterCapabilities filterCap = MockWfsServer.getFilterCapabilities();

    // Create new ScalarCapabiltiesType without Logical Operator support
    ScalarCapabilitiesType scalar = new ScalarCapabilitiesType();
    scalar.setComparisonOperators(new ComparisonOperatorsType());
    for (COMPARISON_OPERATORS compOp : COMPARISON_OPERATORS.values()) {
      ComparisonOperatorType operator = new ComparisonOperatorType();
      operator.setName(compOp.toString());
      scalar.getComparisonOperators().getComparisonOperator().add(operator);
    }
    filterCap.setScalarCapabilities(scalar);

    return new WfsFilterDelegate(
        mockFeatureMetacardType,
        filterCap,
        GeospatialUtil.EPSG_4326_URN,
        mockMapper,
        GeospatialUtil.LAT_LON_ORDER);
  }

  @Test
  public void testFeatureID() {
    String mockProperty = "myPropertyName";
    String mockType = "myType";
    WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);
    String featureId = "1234567";

    // Perform Test
    FilterType matchIdFilter = delegate.propertyIsLike(Metacard.ID, featureId, true);

    // Verify
    assertThat(((ResourceIdType) matchIdFilter.getId().get(0).getValue()).getRid(), is(featureId));
  }

  @Test
  public void testFeatureTypeFeatureID() {
    String mockProperty = "myPropertyName";
    String mockType = "myType";
    WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);

    String featureId = "1234567";
    String mockTypeFeatureId = mockType + "." + featureId;

    // Perform Test
    FilterType matchIdFilter = delegate.propertyIsEqualTo(Metacard.ID, mockTypeFeatureId, true);

    // Verify
    assertThat(
        ((ResourceIdType) matchIdFilter.getId().get(0).getValue()).getRid(), is(mockTypeFeatureId));
  }

  @Test
  public void testInvalidFeatureTypeFeatureID() {
    String mockProperty = "myPropertyName";
    String mockType = "myType";
    WfsFilterDelegate delegate = mockFeatureMetacardCreateDelegate(mockProperty, mockType);
    String nonExistentType = "myBadType";
    String featureId = "1234567";
    String mockTypeFeatureId = nonExistentType + "." + featureId;

    // Perform Test
    FilterType matchIdFilter = delegate.propertyIsEqualTo(Metacard.ID, mockTypeFeatureId, true);
    assertNull(matchIdFilter);
  }

  private WfsFilterDelegate setupFilterDelegate(String spatialOpType) {
    List<String> gmlProps = new ArrayList<>();
    gmlProps.add(MOCK_GEOM);

    when(mockFeatureMetacardType.getGmlProperties()).thenReturn(gmlProps);
    when(mockFeatureMetacardType.isQueryable(MOCK_GEOM)).thenReturn(true);

    SpatialOperatorType operator = new SpatialOperatorType();
    operator.setName(spatialOpType);
    FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
    capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().clear();
    capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().add(operator);
    return new WfsFilterDelegate(
        mockFeatureMetacardType,
        capabilities,
        GeospatialUtil.EPSG_4326_URN,
        mockMapper,
        GeospatialUtil.LAT_LON_ORDER);
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
    List<String> gmlProps = new ArrayList<>();
    gmlProps.add(MOCK_GEOM);

    when(mockFeatureMetacardType.getGmlProperties()).thenReturn(gmlProps);
    when(mockFeatureMetacardType.isQueryable(MOCK_GEOM)).thenReturn(true);

    SpatialOperatorType operator = new SpatialOperatorType();
    operator.setName(SPATIAL_OPERATORS.Intersects.toString());
    FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
    capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().clear();
    capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().add(operator);
    capabilities.getSpatialCapabilities().getGeometryOperands().getGeometryOperand().clear();
    GeometryOperand geoOperand = new GeometryOperand();
    geoOperand.setName(Wfs20Constants.ENVELOPE);
    capabilities
        .getSpatialCapabilities()
        .getGeometryOperands()
        .getGeometryOperand()
        .add(geoOperand);
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            capabilities,
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

    assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
    assertFalse(filter.isSetLogicOps());
    assertXMLEqual(
        MockWfsServer.getIntersectsWithEnvelopeXmlFilter(), getXmlFromMarshaller(filter));
  }

  @Test
  public void testIntersectsLonLat() throws SAXException, IOException, JAXBException {
    List<String> gmlProps = new ArrayList<>();
    gmlProps.add(MOCK_GEOM);

    when(mockFeatureMetacardType.getGmlProperties()).thenReturn(gmlProps);
    when(mockFeatureMetacardType.isQueryable(MOCK_GEOM)).thenReturn(true);

    SpatialOperatorType operator = new SpatialOperatorType();
    operator.setName(SPATIAL_OPERATORS.Intersects.toString());
    FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
    capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().clear();
    capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().add(operator);
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            capabilities,
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LON_LAT_ORDER);

    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

    assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
    assertFalse(filter.isSetLogicOps());
    assertXMLEqual(MockWfsServer.getIntersectsLonLatXmlFilter(), getXmlFromMarshaller(filter));
  }

  @Test
  public void testIntersectsWithEnvelopeLonLat() throws SAXException, IOException, JAXBException {
    List<String> gmlProps = new ArrayList<>();
    gmlProps.add(MOCK_GEOM);

    when(mockFeatureMetacardType.getGmlProperties()).thenReturn(gmlProps);
    when(mockFeatureMetacardType.isQueryable(MOCK_GEOM)).thenReturn(true);

    SpatialOperatorType operator = new SpatialOperatorType();
    operator.setName(SPATIAL_OPERATORS.Intersects.toString());
    FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
    capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().clear();
    capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().add(operator);
    capabilities.getSpatialCapabilities().getGeometryOperands().getGeometryOperand().clear();
    GeometryOperand geoOperand = new GeometryOperand();
    geoOperand.setName(Wfs20Constants.ENVELOPE);
    capabilities
        .getSpatialCapabilities()
        .getGeometryOperands()
        .getGeometryOperand()
        .add(geoOperand);
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            capabilities,
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LON_LAT_ORDER);

    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

    assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
    assertFalse(filter.isSetLogicOps());
    assertXMLEqual(
        MockWfsServer.getIntersectsWithEnvelopeLonLatXmlFilter(), getXmlFromMarshaller(filter));
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
    ImmutableMap<SPATIAL_OPERATORS, String> acceptedSpatialTypes =
        ImmutableMap.of(SPATIAL_OPERATORS.Overlaps, MockWfsServer.getOverlapsXmlFilter());
    testBinarySpatialFilter(acceptedSpatialTypes, "overlaps");
  }

  @Test
  public void testTouchesFilter() throws Exception {
    ImmutableMap<SPATIAL_OPERATORS, String> acceptedSpatialTypes =
        ImmutableMap.of(SPATIAL_OPERATORS.Touches, MockWfsServer.getTouchesXmlFilter());
    testBinarySpatialFilter(acceptedSpatialTypes, "touches");
  }

  @Test
  public void testWithinFilter() throws Exception {
    ImmutableMap<SPATIAL_OPERATORS, String> acceptedSpatialTypes =
        ImmutableMap.of(
            SPATIAL_OPERATORS.Within,
            MockWfsServer.getWithinXmlFilter(),
            SPATIAL_OPERATORS.Contains,
            MockWfsServer.getContainsXmlFilter());
    testBinarySpatialFilter(acceptedSpatialTypes, "within");
  }

  private void testBinarySpatialFilter(
      Map<SPATIAL_OPERATORS, String> acceptedSpatialTypes, String delegateMethodName)
      throws Exception {
    List<SPATIAL_OPERATORS> failTypes = new ArrayList<>(Arrays.asList(SPATIAL_OPERATORS.values()));
    failTypes.removeAll(acceptedSpatialTypes.keySet());

    Method spatialOpMeth =
        WfsFilterDelegate.class.getMethod(delegateMethodName, String.class, String.class);
    for (Map.Entry<SPATIAL_OPERATORS, String> row : acceptedSpatialTypes.entrySet()) {
      WfsFilterDelegate delegate = setupFilterDelegate(row.getKey().toString());

      FilterType filter = (FilterType) spatialOpMeth.invoke(delegate, Metacard.ANY_GEO, POLYGON);

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
    Method spatialOpMeth =
        WfsFilterDelegate.class.getMethod(spatialOpMethName, String.class, String.class);

    testUnsupportedFilterType(spatialOpMeth, failType);
  }

  private void testUnsupportedFilterType(Method spatialOpMeth, SPATIAL_OPERATORS failType)
      throws InvocationTargetException, IllegalAccessException {
    WfsFilterDelegate delegate = setupFilterDelegate(failType.toString());
    FilterType filter = (FilterType) spatialOpMeth.invoke(delegate, Metacard.ANY_GEO, POLYGON);

    assertNull(filter);
  }

  private void testUnsupportedFilterTypeWithDistance(
      String spatialOpMethName, SPATIAL_OPERATORS failType, double distance)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method spatialOpMeth =
        WfsFilterDelegate.class.getMethod(
            spatialOpMethName, String.class, String.class, double.class);

    WfsFilterDelegate delegate = setupFilterDelegate(failType.toString());
    FilterType filter =
        (FilterType) spatialOpMeth.invoke(delegate, Metacard.ANY_GEO, POLYGON, distance);

    assertNull(filter);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSingleGmlPropertyBlacklisted() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Contains.toString());
    when(mockFeatureMetacardType.isQueryable(MOCK_GEOM)).thenReturn(false);

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
    List<String> gmlProps = new ArrayList<>();
    gmlProps.add(MOCK_GEOM);
    gmlProps.add(MOCK_GEOM2);
    when(mockFeatureMetacardType.getGmlProperties()).thenReturn(gmlProps);

    for (String gmlProp : gmlProps) {
      when(mockFeatureMetacardType.isQueryable(gmlProp)).thenReturn(indexed);
    }

    SpatialOperatorType operator = new SpatialOperatorType();
    operator.setName(SPATIAL_OPERATORS.Intersects.toString());
    FilterCapabilities capabilities = MockWfsServer.getFilterCapabilities();
    capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().clear();
    capabilities.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().add(operator);
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            capabilities,
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

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
    List<String> gmlProps = new ArrayList<>();
    gmlProps.add(MOCK_GEOM);
    when(mockFeatureMetacardType.getGmlProperties()).thenReturn(gmlProps);

    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            "EPSG:42304",
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);
    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

    assertTrue(filter == null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGeoFilterNullMetacardType() {
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            null,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    delegate.beyond(Metacard.ANY_GEO, POLYGON, DISTANCE);
  }

  private String fetchPropertyIsLikeExpression(
      PropertyIsLikeType compOpsType, String expressionType) {
    String result = null;
    List<JAXBElement<?>> expressions = compOpsType.getExpression();
    for (JAXBElement<?> expression : expressions) {
      String item = expression.getName().getLocalPart();
      if (item.equals(VALUE_REFERENCE) && item.equals(expressionType)) {
        result = expression.getValue().toString();

      } else if (item.equals(LITERAL) && item.equals(expressionType)) {
        LiteralType literal = (LiteralType) expression.getValue();
        result = literal.getContent().get(0).toString();
      }
    }

    return result;
  }

  private WfsFilterDelegate mockFeatureMetacardCreateDelegate(
      String mockProperty, String mockType) {
    List<String> mockProperties = new ArrayList<>(1);
    mockProperties.add(mockProperty);
    when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
    when(mockFeatureMetacardType.getGmlProperties()).thenReturn(mockProperties);
    when(mockFeatureMetacardType.getTextualProperties()).thenReturn(mockProperties);
    when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockProperties);
    when(mockFeatureMetacardType.getName()).thenReturn(mockType);
    when(mockFeatureMetacardType.isQueryable(mockProperty)).thenReturn(true);

    return new WfsFilterDelegate(
        mockFeatureMetacardType,
        MockWfsServer.getFilterCapabilities(),
        GeospatialUtil.EPSG_4326_URN,
        mockMapper,
        GeospatialUtil.LAT_LON_ORDER);
  }

  private String getXmlFromMarshaller(FilterType filterType) throws JAXBException {
    JAXBContext jaxbContext = initJaxbContext();
    Marshaller marshaller = jaxbContext.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    Writer writer = new StringWriter();
    marshaller.marshal(getFilterTypeJaxbElement(filterType), writer);
    String xml = writer.toString();
    LOGGER.debug("XML returned by Marshaller:\n{}", xml);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(Arrays.toString(Thread.currentThread().getStackTrace()));
    }
    return xml;
  }

  private JAXBElement<FilterType> getFilterTypeJaxbElement(FilterType filterType) {
    return new JAXBElement<>(
        new QName("http://www.opengis.net/ogc", FILTER_QNAME_LOCAL_PART),
        FilterType.class,
        filterType);
  }

  @Test
  public void testDuringTemporalFallback() {
    setupMockMetacardType();
    FilterType afterFilter = setupAfterFilterType();
    FilterType beforeFilter = setupBeforeFilterType();
    FilterCapabilities duringFilterCapabilities = setupFilterCapabilities();
    WfsFilterDelegate duringDelegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            duringFilterCapabilities,
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    WfsFilterDelegate spatialDelegate =
        mockFeatureMetacardCreateDelegate(mockFeatureProperty, mockFeatureType);
    FilterType spatialFilter = spatialDelegate.dwithin(Metacard.ANY_GEO, "POINT (30 10)", 1000);

    List<List<FilterType>> testFilters = new ArrayList<>();
    testFilters.add(Arrays.asList(afterFilter, beforeFilter));
    testFilters.add(Arrays.asList(afterFilter, beforeFilter, spatialFilter));

    for (List<FilterType> filtersToBeConverted : testFilters) {
      List<FilterType> convertedFilters =
          duringDelegate.applyTemporalFallbacks(filtersToBeConverted);
      FilterType duringFilter = convertedFilters.get(0);

      if (filtersToBeConverted.contains(spatialFilter)) {
        // verify that results contains the spatial filter type
        assertThat(convertedFilters.contains(spatialFilter), is(true));

        assertThat(convertedFilters.size(), is(2));

        if (duringFilter.isSetSpatialOps()) {
          duringFilter = convertedFilters.get(1);
        }

        // Verify during Filter is correct
        assertThat(duringFilter.isSetTemporalOps(), is(true));
      }

      assertThat(
          duringFilter.getTemporalOps().getName().toString(),
          is("{http://www.opengis.net/fes/2.0}During"));

      BinaryTemporalOpType binaryTemporalOpType =
          (BinaryTemporalOpType) duringFilter.getTemporalOps().getValue();
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
  public void testDuringFilterTypeDates() {
    setupMockMetacardType();
    FilterType duringFilter = setupDuringFilterType();
    BinaryTemporalOpType binaryTemporalOpType =
        (BinaryTemporalOpType) duringFilter.getTemporalOps().getValue();
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
  public void testIsInFilterSequenceType() {
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
    assertThat(afterDelegate.isAfterFilter(duringFilter), is(false));
  }

  /**
   * If the WFS server does not support an 'After' temporal query and supports a 'During' temporal
   * query, the query should be translated into a 'During <date> to <now>'
   */
  @Test
  public void testRelativeTemporalOnlyQueryAfterUnsupported() {

    setupMockMetacardType();
    FilterType afterFilter = setupAfterFilterType();

    FilterCapabilities duringFilterCapabilities = setupFilterCapabilities();
    WfsFilterDelegate duringDelegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            duringFilterCapabilities,
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    List<FilterType> testFilters = new ArrayList<>();
    testFilters.add(afterFilter);

    List<FilterType> convertedFilters = duringDelegate.applyTemporalFallbacks(testFilters);
    FilterType duringFilter = convertedFilters.get(0);

    assertThat(
        duringFilter.getTemporalOps().getName().toString(),
        is("{http://www.opengis.net/fes/2.0}During"));

    BinaryTemporalOpType binaryTemporalOpType =
        (BinaryTemporalOpType) duringFilter.getTemporalOps().getValue();
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

  /**
   * If the WFS server does support an 'After' temporal query and supports a 'During' temporal
   * query, the query should remain an 'After' query
   */
  @Test
  public void testRelativeTemporalOnlyQueryAfterSupported() {

    setupMockMetacardType();
    FilterType afterFilter = setupAfterFilterType();

    assertThat(
        afterFilter.getTemporalOps().getName().toString(),
        is("{http://www.opengis.net/fes/2.0}After"));

    BinaryTemporalOpType binaryTemporalOpType =
        (BinaryTemporalOpType) afterFilter.getTemporalOps().getValue();
    assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
    assertThat(binaryTemporalOpType.isSetExpression(), is(true));
    TimeInstantType timePeriod = (TimeInstantType) binaryTemporalOpType.getExpression().getValue();

    TimePositionType beginPositionType = timePeriod.getTimePosition();
    Date beginDate = timePositionTypeToDate(beginPositionType);
    Date endDate = new Date();
    // Verify Date range is created correctly
    assertThat(endDate.after(beginDate), is(true));
  }

  /**
   * If the WFS server does not support an 'After' and 'Before' temporal query, and supports a
   * 'During' temporal query, the query should be translated into 'During <after> to <before>'
   */
  @Test
  public void testAbsoluteTemporalOnlyQueryDuringSupported() {

    setupMockMetacardType();
    FilterType afterFilter = setupAfterFilterType();
    FilterType beforeFilter = setupBeforeFilterType();

    FilterCapabilities duringFilterCapabilities = setupFilterCapabilities();
    WfsFilterDelegate duringDelegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            duringFilterCapabilities,
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    // Get After Filter Date
    BinaryTemporalOpType binaryTemporalOpType =
        (BinaryTemporalOpType) afterFilter.getTemporalOps().getValue();
    assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
    assertThat(binaryTemporalOpType.isSetExpression(), is(true));
    TimeInstantType timePeriod = (TimeInstantType) binaryTemporalOpType.getExpression().getValue();
    TimePositionType beginPositionType = timePeriod.getTimePosition();
    Date afterDate = timePositionTypeToDate(beginPositionType);

    // Get Before Filter Date
    binaryTemporalOpType = (BinaryTemporalOpType) beforeFilter.getTemporalOps().getValue();
    assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
    assertThat(binaryTemporalOpType.isSetExpression(), is(true));
    timePeriod = (TimeInstantType) binaryTemporalOpType.getExpression().getValue();
    TimePositionType endPositionType = timePeriod.getTimePosition();
    Date beforeDate = timePositionTypeToDate(endPositionType);

    List<FilterType> testFilters = new ArrayList<>();
    testFilters.add(afterFilter);
    testFilters.add(beforeFilter);

    List<FilterType> convertedFilters = duringDelegate.applyTemporalFallbacks(testFilters);
    FilterType duringFilter = convertedFilters.get(0);

    assertThat(
        duringFilter.getTemporalOps().getName().toString(),
        is("{http://www.opengis.net/fes/2.0}During"));

    binaryTemporalOpType = (BinaryTemporalOpType) duringFilter.getTemporalOps().getValue();
    assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
    assertThat(binaryTemporalOpType.isSetExpression(), is(true));
    TimePeriodType timePeriodType =
        (TimePeriodType) binaryTemporalOpType.getExpression().getValue();

    beginPositionType = timePeriodType.getBeginPosition();
    Date beginDate = timePositionTypeToDate(beginPositionType);
    endPositionType = timePeriodType.getEndPosition();
    Date endDate = timePositionTypeToDate(endPositionType);
    // Verify Date range is created correctly
    assertThat(endDate.after(beginDate), is(true));
    assertThat(endDate.equals(beforeDate), is(true));
    assertThat(beginDate.equals(afterDate), is(true));
  }

  /**
   * If the WFS server does not support an 'After' and 'Before' temporal query, and supports a
   * 'During' temporal query, the query should be translated into 'During <after> to <before>'
   */
  @Test
  public void testAbsoluteTemporalOnlyQueryDuringUnSupported() {

    setupMockMetacardType();
    FilterType afterFilter = setupAfterFilterType();
    FilterType beforeFilter = setupBeforeFilterType();
    WfsFilterDelegate delegate = setupTemporalFilterDelegate();

    // Get After Filter Date
    BinaryTemporalOpType binaryTemporalOpType =
        (BinaryTemporalOpType) afterFilter.getTemporalOps().getValue();
    assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
    assertThat(binaryTemporalOpType.isSetExpression(), is(true));
    TimeInstantType timePeriod = (TimeInstantType) binaryTemporalOpType.getExpression().getValue();
    TimePositionType beginPositionType = timePeriod.getTimePosition();
    Date afterDate = timePositionTypeToDate(beginPositionType);

    // Get Before Filter Date
    binaryTemporalOpType = (BinaryTemporalOpType) beforeFilter.getTemporalOps().getValue();
    assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
    assertThat(binaryTemporalOpType.isSetExpression(), is(true));
    timePeriod = (TimeInstantType) binaryTemporalOpType.getExpression().getValue();
    TimePositionType endPositionType = timePeriod.getTimePosition();
    Date beforeDate = timePositionTypeToDate(endPositionType);

    List<FilterType> testFilters = new ArrayList<>();
    testFilters.add(afterFilter);
    testFilters.add(beforeFilter);

    List<FilterType> convertedFilters = delegate.applyTemporalFallbacks(testFilters);
    FilterType resultAfterFilter = convertedFilters.get(0);
    FilterType resultBeforeFilter = convertedFilters.get(1);

    assertThat(
        resultAfterFilter.getTemporalOps().getName().toString(),
        is("{http://www.opengis.net/fes/2.0}After"));
    assertThat(
        resultBeforeFilter.getTemporalOps().getName().toString(),
        is("{http://www.opengis.net/fes/2.0}Before"));

    // Get Resulting After Filter Date
    binaryTemporalOpType = (BinaryTemporalOpType) resultAfterFilter.getTemporalOps().getValue();
    assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
    assertThat(binaryTemporalOpType.isSetExpression(), is(true));
    TimeInstantType timePeriodType =
        (TimeInstantType) binaryTemporalOpType.getExpression().getValue();
    beginPositionType = timePeriodType.getTimePosition();
    Date beginDate = timePositionTypeToDate(beginPositionType);

    // Get Resulting Before Filter Date
    binaryTemporalOpType = (BinaryTemporalOpType) resultBeforeFilter.getTemporalOps().getValue();
    assertThat(binaryTemporalOpType.isSetValueReference(), is(true));
    assertThat(binaryTemporalOpType.isSetExpression(), is(true));
    timePeriodType = (TimeInstantType) binaryTemporalOpType.getExpression().getValue();
    endPositionType = timePeriodType.getTimePosition();
    Date endDate = timePositionTypeToDate(endPositionType);

    // Verify Date range is created correctly
    assertThat(endDate.after(beginDate), is(true));
    assertThat(endDate.equals(beforeDate), is(true));
    assertThat(beginDate.equals(afterDate), is(true));
  }

  @Test
  public void testPropertyIsFilterWithMetacardAttributeMappedToFeatureProperty() throws Exception {
    doReturn(mockFeatureProperty).when(mockMapper).getFeatureProperty(Core.TITLE);
    doReturn(singletonList(mockFeatureProperty)).when(mockFeatureMetacardType).getProperties();
    doReturn(true).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    final FilterType filter = delegate.propertyIsEqualTo(Core.TITLE, LITERAL, true);
    assertXMLEqual(MockWfsServer.getPropertyIsEqualToFilter(), getXmlFromMarshaller(filter));
  }

  @Test
  public void testPropertyIsFilterCannotMapToFeatureProperty() {
    doReturn(singletonList(mockFeatureProperty)).when(mockFeatureMetacardType).getProperties();
    doReturn(true).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    final FilterType filter = delegate.propertyIsEqualTo(Core.TITLE, LITERAL, true);
    assertThat(
        "The filter should have been null because 'title' is not mapped to a WFS feature property.",
        filter,
        is(nullValue()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyIsFilterFeaturePropertyIsNotQueryable() {
    doReturn(mockFeatureProperty).when(mockMapper).getFeatureProperty(Core.TITLE);
    doReturn(singletonList(mockFeatureProperty)).when(mockFeatureMetacardType).getProperties();
    doReturn(false).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    delegate.propertyIsEqualTo(Core.TITLE, LITERAL, true);
  }

  @Test
  public void testPropertyIsBetweenFilterWithMetacardAttributeMappedToFeatureProperty()
      throws Exception {
    doReturn(mockFeatureProperty).when(mockMapper).getFeatureProperty(Core.RESOURCE_SIZE);
    doReturn(singletonList(mockFeatureProperty)).when(mockFeatureMetacardType).getProperties();
    doReturn(true).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    final FilterType filter = delegate.propertyIsBetween(Core.RESOURCE_SIZE, 100, 200);
    assertXMLEqual(MockWfsServer.getPropertyIsBetweenFilter(), getXmlFromMarshaller(filter));
  }

  @Test
  public void testPropertyIsBetweenFilterCannotMapToFeatureProperty() {
    doReturn(singletonList(mockFeatureProperty)).when(mockFeatureMetacardType).getProperties();
    doReturn(true).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    final FilterType filter = delegate.propertyIsBetween(Core.RESOURCE_SIZE, 100, 200);
    assertThat(
        "The filter should have been null because 'resource-size' is not mapped to a WFS feature property.",
        filter,
        is(nullValue()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyIsBetweenFilterFeaturePropertyIsNotQueryable() {
    doReturn(mockFeatureProperty).when(mockMapper).getFeatureProperty(Core.RESOURCE_SIZE);
    doReturn(singletonList(mockFeatureProperty)).when(mockFeatureMetacardType).getProperties();
    doReturn(false).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    delegate.propertyIsBetween(Core.RESOURCE_SIZE, 100, 200);
  }

  @Test
  public void testGeospatialFilterWithMetacardAttributeMappedToFeatureProperty() throws Exception {
    doReturn(MOCK_GEOM).when(mockMapper).getFeatureProperty(Core.LOCATION);
    doReturn(singletonList(MOCK_GEOM)).when(mockFeatureMetacardType).getGmlProperties();
    doReturn(true).when(mockFeatureMetacardType).isQueryable(MOCK_GEOM);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    final FilterType filter = delegate.dwithin(Core.LOCATION, POINT, DISTANCE);
    assertXMLEqual(MockWfsServer.getDWithinPointXmlFilter(), getXmlFromMarshaller(filter));
  }

  @Test
  public void testGeospatialFilterCannotMapToFeatureProperty() {
    doReturn(singletonList(MOCK_GEOM)).when(mockFeatureMetacardType).getGmlProperties();
    doReturn(true).when(mockFeatureMetacardType).isQueryable(MOCK_GEOM);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    final FilterType filter = delegate.dwithin(Core.LOCATION, POINT, DISTANCE);
    assertThat(
        "The filter should have been null because 'location' is not mapped to a WFS feature property.",
        filter,
        is(nullValue()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGeospatialFilterFeaturePropertyIsNotQueryable() {
    doReturn(MOCK_GEOM).when(mockMapper).getFeatureProperty(Core.LOCATION);
    doReturn(singletonList(MOCK_GEOM)).when(mockFeatureMetacardType).getGmlProperties();
    doReturn(false).when(mockFeatureMetacardType).isQueryable(mockFeatureProperty);

    final WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);

    delegate.dwithin(Core.LOCATION, POINT, DISTANCE);
  }

  private WfsFilterDelegate setupTemporalFilterDelegate() {
    return new WfsFilterDelegate(
        mockFeatureMetacardType,
        MockWfsServer.getFilterCapabilities(),
        GeospatialUtil.EPSG_4326_URN,
        mockMapper,
        GeospatialUtil.LAT_LON_ORDER);
  }

  private void setupMockMetacardType() {
    mockMetacardAttribute = "modified";
    mockFeatureType = "myFeatureType";
    mockFeatureProperty = "EXACT_COLLECT_DATE";
    List<String> mockProperties = new ArrayList<>(1);
    mockProperties.add(mockFeatureProperty);

    when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
    when(mockFeatureMetacardType.getName()).thenReturn(mockFeatureType);
    when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockProperties);
    when(mockFeatureMetacardType.isQueryable(mockFeatureProperty)).thenReturn(true);

    when(mockMapper.getFeatureProperty(mockMetacardAttribute)).thenReturn(mockFeatureProperty);
  }

  private FilterType setupBeforeFilterType() {
    WfsFilterDelegate beforeDelegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);
    DateTime beforeDate = new DateTime().minusDays(1);
    return beforeDelegate.before(mockMetacardAttribute, beforeDate.toDate());
  }

  private FilterType setupAfterFilterType() {
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);
    DateTime afterDate = new DateTime().minusDays(30);
    return delegate.after(mockMetacardAttribute, afterDate.toDate());
  }

  private FilterType setupDuringFilterType() {
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            mockFeatureMetacardType,
            MockWfsServer.getFilterCapabilities(),
            GeospatialUtil.EPSG_4326_URN,
            mockMapper,
            GeospatialUtil.LAT_LON_ORDER);
    DateTime startDate = new DateTime(2014, 01, 01, 01, 01, 01, 123, DateTimeZone.forID("-07:00"));
    DateTime endDate = new DateTime(2014, 01, 02, 01, 01, 01, 123, DateTimeZone.forID("-07:00"));
    return delegate.during(mockMetacardAttribute, startDate.toDate(), endDate.toDate());
  }

  private Date timePositionTypeToDate(TimePositionType timePositionType) {
    Date date = new Date();
    try {
      List<String> timeList = timePositionType.getValue();
      String dateTimeString = timeList.get(0);
      DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // 2014-12-14T17:14:43Z
      date = format.parse(dateTimeString);
    } catch (ParseException pe) {
      LOGGER.debug("Parse Exception", pe);
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
    List<QName> timeQNames =
        Arrays.asList(
            new QName(Wfs20Constants.GML_3_2_NAMESPACE, "TimePeriod"),
            new QName(Wfs20Constants.GML_3_2_NAMESPACE, "TimeInstant"));
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
      List<String> mockProperties = new ArrayList<>(1);
      mockProperties.add(mockFeatureProperty);
      when(mockFeatureMetacardType.getProperties()).thenReturn(mockProperties);
      when(mockFeatureMetacardType.getName()).thenReturn(mockFeatureType);
      when(mockFeatureMetacardType.getTemporalProperties()).thenReturn(mockProperties);
      when(mockFeatureMetacardType.isQueryable(mockFeatureProperty)).thenReturn(true);
      when(mockMapper.getFeatureProperty(mockMetacardAttribute)).thenReturn(mockFeatureProperty);
      delegate =
          new WfsFilterDelegate(
              mockFeatureMetacardType,
              MockWfsServer.getFilterCapabilities(),
              GeospatialUtil.EPSG_4326_URN,
              mockMapper,
              GeospatialUtil.LAT_LON_ORDER);
      return this;
    }
  }
}
