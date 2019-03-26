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
package org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.source;

import static javolution.testing.TestContext.assertTrue;
import static junit.framework.TestCase.assertNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.types.Core;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import ogc.schema.opengis.filter.v_1_0_0.BBOXType;
import ogc.schema.opengis.filter.v_1_0_0.BinarySpatialOpType;
import ogc.schema.opengis.filter.v_1_0_0.DistanceBufferType;
import ogc.schema.opengis.filter.v_1_0_0.FilterType;
import ogc.schema.opengis.filter.v_1_0_0.UnaryLogicOpType;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureAttributeDescriptor;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.Wfs10Constants.SPATIAL_OPERATORS;
import org.custommonkey.xmlunit.XMLUnit;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class WfsFilterDelegateTest {

  private static final JAXBContext JAXB_CONTEXT = initJaxbContext();

  private static final String PROPERTY_NAME = "PropertyName";

  private static final String MOCK_PROPERTY = "mockProperty";

  private static final String MOCK_PROPERTY_2 = "mockProperty2";

  private static final String MOCK_GEOM = "ground_geom";

  private static final String MOCK_GEOM2 = "ground_geom2";

  private static final String LITERAL = "Literal";

  private static final String UNLITERAL = "Unliteral";

  private static final List<String> SUPPORTED_GEO = Arrays.asList("Intersects", "BBox", "Within");

  private static final String SRS_NAME = "EPSG:4326";

  private static final String POLYGON = "POLYGON ((30 -10, 30 30, 10 30, 10 -10, 30 -10))";

  private static final String POINT = "POINT (30 -10)";

  private static final double DISTANCE = 1000.0;

  private static final String NO_OP = "NoOp";

  private final Date date = getDate();

  private final Date endDate = getEndDate();

  private static final Logger LOGGER = LoggerFactory.getLogger(WfsFilterDelegateTest.class);

  private static final String FILTER_QNAME_LOCAL_PART = "Filter";

  private final String propertyIsEqualToXmlBoolean =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsEqualTo>"
          + "<Literal>false</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsEqualTo>"
          + "</Filter>";

  private final String propertyIsEqualToXml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsEqualTo>"
          + "<Literal>1</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsEqualTo>"
          + "</Filter>";

  private final String propertyIsEqualToXmlDecimal =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsEqualTo>"
          + "<Literal>1.0</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsEqualTo>"
          + "</Filter>";

  private final String propertyIsEqualToXmlLiteral =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsEqualTo>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsEqualTo>"
          + "</Filter>";

  private final String propertyNotEqualToXmlLiteral =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsNotEqualTo>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsNotEqualTo>"
          + "</Filter>";

  private String propertyIsEqualToXmlDate = getPropertyEqualToXmlDate();

  private final String propertyNotEqualToXml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsNotEqualTo>"
          + "<Literal>1</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsNotEqualTo>"
          + "</Filter>";

  private final String propertyNotEqualToXmlDecimal =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsNotEqualTo>"
          + "<Literal>1.0</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsNotEqualTo>"
          + "</Filter>";

  private final String propertyNotEqualToXmlDate = getPropertyNotEqualToXmlDate();

  private final String propertyNotEqualToXmlBoolean =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsNotEqualTo>"
          + "<Literal>false</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsNotEqualTo>"
          + "</Filter>";

  private final String propertyGreaterThanXmlLiteral =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsGreaterThan>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsGreaterThan>"
          + "</Filter>";

  private final String propertyGreaterThanXml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsGreaterThan>"
          + "<Literal>1</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsGreaterThan>"
          + "</Filter>";

  private final String propertyGreaterThanXmlDecimal =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsGreaterThan>"
          + "<Literal>1.0</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsGreaterThan>"
          + "</Filter>";

  private final String propertyGreaterThanXmlDate = getPropertyGreaterThanXmlDate();

  private final String propertyGreaterThanOrEqualToXmlLiteral =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsGreaterThanOrEqualTo>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsGreaterThanOrEqualTo>"
          + "</Filter>";

  private final String propertyGreaterThanOrEqualToXml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsGreaterThanOrEqualTo>"
          + "<Literal>1</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsGreaterThanOrEqualTo>"
          + "</Filter>";

  private final String propertyGreaterThanOrEqualToXmlDecimal =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsGreaterThanOrEqualTo>"
          + "<Literal>1.0</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsGreaterThanOrEqualTo>"
          + "</Filter>";

  private final String propertyGreaterThanOrEqualToXmlDate =
      getPropertyGreaterThanOrEqualToXmlDate();

  private final String propertyLessThanXmlLiteral =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsLessThan>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsLessThan>"
          + "</Filter>";

  private final String propertyLessThanXml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsLessThan>"
          + "<Literal>1</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsLessThan>"
          + "</Filter>";

  private final String propertyLessThanXmlDecimal =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsLessThan>"
          + "<Literal>1.0</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsLessThan>"
          + "</Filter>";

  private final String propertyLessThanXmlDate = getPropertyLessThanXmlDate();

  private final String propertyLessThanOrEqualToXmlLiteral =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsLessThanOrEqualTo>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsLessThanOrEqualTo>"
          + "</Filter>";

  private final String propertyLessThanOrEqualToXml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsLessThanOrEqualTo>"
          + "<Literal>1</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsLessThanOrEqualTo>"
          + "</Filter>";

  private final String propertyLessThanOrEqualToXmlDecimal =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsLessThanOrEqualTo>"
          + "<Literal>1.0</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsLessThanOrEqualTo>"
          + "</Filter>";

  private final String propertyLessThanOrEqualToXmlDate = getPropertyLessThanOrEqualToXmlDate();

  private final String propertyBetweenXmlLiteral =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsBetween>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<LowerBoundary><Literal>Literal</Literal></LowerBoundary>"
          + "<UpperBoundary><Literal>Unliteral</Literal></UpperBoundary>"
          + "</PropertyIsBetween>"
          + "</Filter>";

  private final String propertyBetweenXml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsBetween>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<LowerBoundary><Literal>1</Literal></LowerBoundary>"
          + "<UpperBoundary><Literal>10</Literal></UpperBoundary>"
          + "</PropertyIsBetween>"
          + "</Filter>";

  private final String propertyBetweenXmlDecimal =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsBetween>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<LowerBoundary><Literal>1.0</Literal></LowerBoundary>"
          + "<UpperBoundary><Literal>10.0</Literal></UpperBoundary>"
          + "</PropertyIsBetween>"
          + "</Filter>";

  private final String propertyBetweenXmlDate = getPropertyBetweenXmlDate();

  private FeatureMetacardType featureMetacardType = mock(FeatureMetacardType.class);

  private List<String> mockGmlProps = new ArrayList<String>();

  @Before
  public void setUp() {
    when(featureMetacardType.getGmlProperties()).thenReturn(mockGmlProps);
  }

  @Test
  public void testWFSFilterDelegate() {
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    assertNotNull(delegate);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWFSFilterDelegateNullSchema() {
    new WfsFilterDelegate(null, null, null);
  }

  @Test
  public void testAnd() {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.STRING_TYPE));

    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    FilterType filterToCheck = delegate.and(Arrays.asList(filter, filter));
    assertNotNull(filterToCheck);
    assertTrue(filterToCheck.isSetLogicOps());
  }

  @Test
  public void testAndSingleFilter() {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.STRING_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    ArrayList<FilterType> filters = new ArrayList<FilterType>();
    filters.add(filter);
    filters.add(new FilterType());
    FilterType filterToCheck = delegate.and(filters);
    assertNotNull(filterToCheck);
    // Should not have an AND filter
    assertFalse(filterToCheck.isSetLogicOps());
  }

  @Test
  public void testOr() {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.STRING_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    FilterType filterToCheck = delegate.or(Arrays.asList(filter, filter));
    assertNotNull(filterToCheck);
    assertTrue(filterToCheck.isSetLogicOps());
  }

  @Test
  public void testOrSingleFilter() {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.STRING_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    ArrayList<FilterType> filters = new ArrayList<FilterType>();
    filters.add(filter);
    filters.add(new FilterType());
    FilterType filterToCheck = delegate.or(filters);
    assertNotNull(filterToCheck);
    // Should not have an AND filter
    assertFalse(filterToCheck.isSetLogicOps());
  }

  @Test
  public void testNot() {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.STRING_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    FilterType filterToCheck = delegate.not(filter);
    assertNotNull(filterToCheck);
    assertTrue(filterToCheck.isSetLogicOps());
  }

  @Test
  public void testPropertyIsEqualToStringStringBoolean()
      throws JAXBException, SAXException, IOException {
    List<String> mockProps = new ArrayList<String>();
    mockProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.STRING_TYPE));

    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, LITERAL, true);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    LOGGER.debug(writer.toString());
    assertXMLEqual(propertyIsEqualToXmlLiteral, writer.toString());
  }

  @Test
  public void testPropertyIsEqualToStringStringBooleanAnyText() {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.STRING_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    // 1 property will produce a ComparisonOp
    assertTrue(filter.isSetComparisonOps());
    assertNotNull(filter.getComparisonOps());
    assertTrue(filter.getComparisonOps() instanceof JAXBElement<?>);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyIsEqualToStringStringBooleanAnyTextNullMetacardType() {

    WfsFilterDelegate delegate = new WfsFilterDelegate(null, SUPPORTED_GEO, SRS_NAME);
    delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
  }

  @Test
  public void testPropertyIsEqualToStringStringBooleanAnyTextMultipleProperties() {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    mockTextProps.add(MOCK_PROPERTY_2);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.STRING_TYPE));
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY_2))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY_2, MOCK_PROPERTY_2, true, true, true, true, BasicTypes.STRING_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    // Mulitple properties will produce a LogicOp (OR)
    assertFalse(filter.isSetComparisonOps());
    assertTrue(filter.isSetLogicOps());
    assertNotNull(filter.getLogicOps());
    assertTrue(filter.getLogicOps() instanceof JAXBElement<?>);
  }

  @Test
  public void testPropertyIsEqualToDate() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.DATE_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, date);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    LOGGER.debug(writer.toString());
    assertXMLEqual(propertyIsEqualToXmlDate, writer.toString());
  }

  @Test
  public void testPropertyIsEqualToInt() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.INTEGER_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, 1);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();

    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    String xml = writer.toString();

    LOGGER.debug(xml);
    assertXMLEqual(propertyIsEqualToXml, xml);
  }

  @Test
  public void testPropertyIsEqualToShort() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.SHORT_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    short literal = 1;
    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyIsEqualToXml, writer.toString());
  }

  @Test
  public void testPropertyIsEqualToLong() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    long literal = 1L;
    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyIsEqualToXml, writer.toString());
  }

  @Test
  public void testPropertyIsEqualToFloat() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.FLOAT_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, 1.0F);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyIsEqualToXmlDecimal, writer.toString());
  }

  @Test
  public void testPropertyIsEqualToDouble() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.DOUBLE_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, 1.0);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyIsEqualToXmlDecimal, writer.toString());
  }

  @Test
  public void testPropertyIsEqualToBoolean() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.BOOLEAN_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, false);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyIsEqualToXmlBoolean, writer.toString());
  }

  @Test
  public void testPropertyIsNotEqualToString() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.BOOLEAN_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, LITERAL, true);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyNotEqualToXmlLiteral, writer.toString());
  }

  @Test
  public void testPropertyIsNotEqualToDate() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.BOOLEAN_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, date);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyNotEqualToXmlDate, writer.toString());
  }

  @Test
  public void testPropertyIsNotEqualToInt() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.INTEGER_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, 1);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    String xml = writer.toString();

    assertXMLEqual(propertyNotEqualToXml, xml);
  }

  @Test
  public void testPropertyIsNotEqualToShort() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.INTEGER_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    short literal = 1;

    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, literal);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    String xml = writer.toString();

    assertXMLEqual(propertyNotEqualToXml, xml);
  }

  @Test
  public void testPropertyIsNotEqualToLong() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    long literal = 1L;
    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyNotEqualToXml, writer.toString());
  }

  @Test
  public void testPropertyIsNotEqualToFloat() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    float literal = 1.0F;
    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyNotEqualToXmlDecimal, writer.toString());
  }

  @Test
  public void testPropertyIsNotEqualToDouble() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    double literal = 1.0;
    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyNotEqualToXmlDecimal, writer.toString());
  }

  @Test
  public void testPropertyIsNotEqualToBoolean() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, false);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyNotEqualToXmlBoolean, writer.toString());
  }

  @Test
  public void testPropertyIsGreaterThanString() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.BOOLEAN_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsGreaterThan(MOCK_PROPERTY, LITERAL);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyGreaterThanXmlLiteral, writer.toString());
  }

  @Test
  public void testPropertyIsGreaterThanDate() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.BOOLEAN_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsGreaterThan(MOCK_PROPERTY, date);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyGreaterThanXmlDate, writer.toString());
  }

  @Test
  public void testPropertyIsGreaterThanInt() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.INTEGER_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsGreaterThan(MOCK_PROPERTY, 1);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    String xml = writer.toString();

    assertXMLEqual(propertyGreaterThanXml, xml);
  }

  @Test
  public void testPropertyIsGreaterThanShort() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.INTEGER_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    short literal = 1;

    FilterType filter = delegate.propertyIsGreaterThan(MOCK_PROPERTY, literal);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    String xml = writer.toString();

    assertXMLEqual(propertyGreaterThanXml, xml);
  }

  @Test
  public void testPropertyIsGreaterThanLong() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    long literal = 1L;
    FilterType filter = delegate.propertyIsGreaterThan(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyGreaterThanXml, writer.toString());
  }

  @Test
  public void testPropertyIsGreaterThanFloat() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    float literal = 1.0F;
    FilterType filter = delegate.propertyIsGreaterThan(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyGreaterThanXmlDecimal, writer.toString());
  }

  @Test
  public void testPropertyIsGreaterThanDouble() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    double literal = 1.0;
    FilterType filter = delegate.propertyIsGreaterThan(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyGreaterThanXmlDecimal, writer.toString());
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToString()
      throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.BOOLEAN_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsGreaterThanOrEqualTo(MOCK_PROPERTY, LITERAL);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyGreaterThanOrEqualToXmlLiteral, writer.toString());
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToDate()
      throws JAXBException, SAXException, IOException {

    LOGGER.debug("Input date: {}", date);
    LOGGER.debug("ISO 8601 formatted date: {}", convertDateToIso8601Format(getDate()));

    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.BOOLEAN_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsGreaterThanOrEqualTo(MOCK_PROPERTY, date);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    LOGGER.debug(writer.toString());
    assertXMLEqual(propertyGreaterThanOrEqualToXmlDate, writer.toString());
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToInt()
      throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.INTEGER_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsGreaterThanOrEqualTo(MOCK_PROPERTY, 1);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    String xml = writer.toString();

    assertXMLEqual(propertyGreaterThanOrEqualToXml, xml);
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToShort()
      throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.INTEGER_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    short literal = 1;

    FilterType filter = delegate.propertyIsGreaterThanOrEqualTo(MOCK_PROPERTY, literal);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    String xml = writer.toString();

    assertXMLEqual(propertyGreaterThanOrEqualToXml, xml);
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToLong()
      throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    long literal = 1L;
    FilterType filter = delegate.propertyIsGreaterThanOrEqualTo(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyGreaterThanOrEqualToXml, writer.toString());
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToFloat()
      throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    float literal = 1.0F;
    FilterType filter = delegate.propertyIsGreaterThanOrEqualTo(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyGreaterThanOrEqualToXmlDecimal, writer.toString());
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToDouble()
      throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    double literal = 1.0;
    FilterType filter = delegate.propertyIsGreaterThanOrEqualTo(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyGreaterThanOrEqualToXmlDecimal, writer.toString());
  }

  @Test
  public void testPropertyIsLessThanString() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.BOOLEAN_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsLessThan(MOCK_PROPERTY, LITERAL);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyLessThanXmlLiteral, writer.toString());
  }

  @Test
  public void testPropertyIsLessThanDate() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.BOOLEAN_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsLessThan(MOCK_PROPERTY, date);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyLessThanXmlDate, writer.toString());
  }

  @Test
  public void testPropertyIsLessThanInt() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.INTEGER_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsLessThan(MOCK_PROPERTY, 1);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    String xml = writer.toString();

    assertXMLEqual(propertyLessThanXml, xml);
  }

  @Test
  public void testPropertyIsLessThanShort() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.INTEGER_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    short literal = 1;

    FilterType filter = delegate.propertyIsLessThan(MOCK_PROPERTY, literal);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    String xml = writer.toString();

    assertXMLEqual(propertyLessThanXml, xml);
  }

  @Test
  public void testPropertyIsLessThanLong() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    long literal = 1L;
    FilterType filter = delegate.propertyIsLessThan(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyLessThanXml, writer.toString());
  }

  @Test
  public void testPropertyIsLessThanFloat() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    float literal = 1.0F;
    FilterType filter = delegate.propertyIsLessThan(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyLessThanXmlDecimal, writer.toString());
  }

  @Test
  public void testPropertyIsLessThanDouble() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    double literal = 1.0;
    FilterType filter = delegate.propertyIsLessThan(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyLessThanXmlDecimal, writer.toString());
  }

  @Test
  public void testPropertyIsLessThanOrEqualToString()
      throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.BOOLEAN_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsLessThanOrEqualTo(MOCK_PROPERTY, LITERAL);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyLessThanOrEqualToXmlLiteral, writer.toString());
  }

  @Test
  public void testPropertyIsLessThanOrEqualToDate()
      throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.BOOLEAN_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsLessThanOrEqualTo(MOCK_PROPERTY, date);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyLessThanOrEqualToXmlDate, writer.toString());
  }

  @Test
  public void testPropertyIsLessThanOrEqualToInt() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.INTEGER_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsLessThanOrEqualTo(MOCK_PROPERTY, 1);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    String xml = writer.toString();

    assertXMLEqual(propertyLessThanOrEqualToXml, xml);
  }

  @Test
  public void testPropertyIsLessThanOrEqualToShort()
      throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.INTEGER_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    short literal = 1;

    FilterType filter = delegate.propertyIsLessThanOrEqualTo(MOCK_PROPERTY, literal);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    String xml = writer.toString();

    assertXMLEqual(propertyLessThanOrEqualToXml, xml);
  }

  @Test
  public void testPropertyIsLessThanOrEqualToLong()
      throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    long literal = 1L;
    FilterType filter = delegate.propertyIsLessThanOrEqualTo(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyLessThanOrEqualToXml, writer.toString());
  }

  @Test
  public void testPropertyIsLessThanOrEqualToFloat()
      throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    float literal = 1.0F;
    FilterType filter = delegate.propertyIsLessThanOrEqualTo(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyLessThanOrEqualToXmlDecimal, writer.toString());
  }

  @Test
  public void testPropertyIsLessThanOrEqualToDouble()
      throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    double literal = 1.0;
    FilterType filter = delegate.propertyIsLessThanOrEqualTo(MOCK_PROPERTY, literal);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyLessThanOrEqualToXmlDecimal, writer.toString());
  }

  @Test
  public void testPropertyIsBetweenString() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.BOOLEAN_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsBetween(MOCK_PROPERTY, LITERAL, UNLITERAL);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    LOGGER.debug(writer.toString());
    assertXMLEqual(propertyBetweenXmlLiteral, writer.toString());
  }

  @Test
  public void testPropertyIsBetweenDate() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.BOOLEAN_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsBetween(MOCK_PROPERTY, date, getEndDate());
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyBetweenXmlDate, writer.toString());
  }

  @Test
  public void testPropertyIsBetweenInt() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.INTEGER_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsBetween(MOCK_PROPERTY, 1, 10);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    String xml = writer.toString();

    assertXMLEqual(propertyBetweenXml, xml);
  }

  @Test
  public void testPropertyIsBetweenShort() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.INTEGER_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    short lower = 1;
    short upper = 10;

    FilterType filter = delegate.propertyIsBetween(MOCK_PROPERTY, lower, upper);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    String xml = writer.toString();

    assertXMLEqual(propertyBetweenXml, xml);
  }

  @Test
  public void testPropertyIsBetweenLong() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    long lower = 1L;
    long upper = 10L;

    FilterType filter = delegate.propertyIsBetween(MOCK_PROPERTY, lower, upper);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyBetweenXml, writer.toString());
  }

  @Test
  public void testPropertyIsBetweenFloat() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    float lower = 1.0F;
    float upper = 10.0F;
    FilterType filter = delegate.propertyIsBetween(MOCK_PROPERTY, lower, upper);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyBetweenXmlDecimal, writer.toString());
  }

  @Test
  public void testPropertyIsBetweenDouble() throws JAXBException, SAXException, IOException {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.LONG_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    double lower = 1.0;
    double upper = 10.0;
    FilterType filter = delegate.propertyIsBetween(MOCK_PROPERTY, lower, upper);
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    assertXMLEqual(propertyBetweenXmlDecimal, writer.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyIsBetweenNullLowerBoundary() {
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    String lower = null;
    delegate.propertyIsBetween(MOCK_PROPERTY, lower, LITERAL);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyIsBetweenNullUpperBoundary() {
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    String upper = null;
    delegate.propertyIsBetween(MOCK_PROPERTY, LITERAL, upper);
  }

  @Test
  public void testPropertyIsLikeStringStringBoolean() {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockTextProps);
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsLike(PROPERTY_NAME, LITERAL, true);
    // Ensure this is an invalid FilterType
    assertTrue(filter == null);
  }

  @Test
  public void testPropertyIsLikeStringStringBooleanAnyText() {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.STRING_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    // 1 property will produce a ComparisonOp
    assertTrue(filter.isSetComparisonOps());
    assertNotNull(filter.getComparisonOps());
    assertTrue(filter.getComparisonOps() instanceof JAXBElement<?>);
  }

  @Test
  public void testPropertyIsLikeStringStringBooleanAnyTextMultipleProperties() {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    mockTextProps.add(MOCK_PROPERTY_2);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.STRING_TYPE));
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY_2))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY_2, MOCK_PROPERTY_2, true, true, true, true, BasicTypes.STRING_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    // Mulitple properties will produce a LogicOp (OR)
    assertFalse(filter.isSetComparisonOps());
    assertTrue(filter.isSetLogicOps());
    assertNotNull(filter.getLogicOps());
    assertTrue(filter.getLogicOps() instanceof JAXBElement<?>);
  }

  @Test
  public void testPropertyIsLikeAnyTextNoAttributes() {
    List<String> mockTextProps = new ArrayList<String>();
    when(featureMetacardType.getTextualProperties()).thenReturn(mockTextProps);
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    assertTrue(filter == null);
  }

  @Test
  public void testPropertyIsEqualToAnyTextNoAttributes() {
    List<String> mockTextProps = new ArrayList<String>();
    when(featureMetacardType.getTextualProperties()).thenReturn(mockTextProps);
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    assertTrue(filter == null);
  }

  @Test
  public void testPropertyIsEqualToMetacardId() {
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    when(featureMetacardType.getName()).thenReturn("test");
    FilterType filter = delegate.propertyIsEqualTo(Core.ID, "test.123", true);
    assertNotNull(filter);
  }

  @Test
  public void testPropertyIsEqualToMetacardIdSimpleId() {
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    when(featureMetacardType.getName()).thenReturn("test");
    FilterType filter = delegate.propertyIsEqualTo(Core.ID, "123", true);
    assertNotNull(filter);
  }

  @Test
  public void testPropertyIsEqualToMetacardIdMismatchFeature() {
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    when(featureMetacardType.getName()).thenReturn("badType");
    FilterType filter = delegate.propertyIsEqualTo(Core.ID, "test.123", true);
    assertNull(filter);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyIsLikePropertyBlacklisted() {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);

    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, false, false, false, false, BasicTypes.STRING_TYPE));

    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    delegate.propertyIsLike(MOCK_PROPERTY, LITERAL, false);
  }

  @Test
  public void testAllTextualPropertiesBlacklisted() {
    List<String> mockTextProps = new ArrayList<String>();
    mockTextProps.add(MOCK_PROPERTY);
    mockTextProps.add(MOCK_PROPERTY_2);

    when(featureMetacardType.getProperties()).thenReturn(mockTextProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, false, false, false, false, BasicTypes.STRING_TYPE));
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY_2))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY_2,
                MOCK_PROPERTY_2,
                false,
                false,
                false,
                false,
                BasicTypes.STRING_TYPE));
    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);

    FilterType filter = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, false);
    assertNull(filter);
  }

  @Test
  public void testNonTextualPropertyIsLike() {
    List<String> mockProps = new ArrayList<String>();
    mockProps.add(MOCK_PROPERTY);

    when(featureMetacardType.getProperties()).thenReturn(mockProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, false, false, false, BasicTypes.LONG_TYPE));

    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsLike(MOCK_PROPERTY, LITERAL, false);
    assertNotNull(filter);
  }

  @Test
  public void testNonTextualPropertyIsEqual() throws JAXBException, SAXException, IOException {
    List<String> mockProps = new ArrayList<String>();
    mockProps.add(MOCK_PROPERTY);

    when(featureMetacardType.getProperties()).thenReturn(mockProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, false, false, false, BasicTypes.LONG_TYPE));

    WfsFilterDelegate delegate =
        new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO, SRS_NAME);
    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, false);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();

    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    String xml = writer.toString();

    assertXMLEqual(propertyIsEqualToXmlBoolean, xml);
  }

  private WfsFilterDelegate setupFilterDelegate(String spatialOpType) {
    List<String> gmlProps = new ArrayList<String>();
    gmlProps.add(MOCK_GEOM);

    when(featureMetacardType.getGmlProperties()).thenReturn(gmlProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_GEOM))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_GEOM, MOCK_GEOM, true, false, false, false, BasicTypes.STRING_TYPE));

    List<String> supportedGeo = Arrays.asList(spatialOpType);
    return new WfsFilterDelegate(featureMetacardType, supportedGeo, SRS_NAME);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBlacklistedGeoProperty() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.BBOX.toString());

    when(featureMetacardType.getAttributeDescriptor(MOCK_GEOM))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_GEOM, MOCK_GEOM, false, false, false, false, BasicTypes.STRING_TYPE));
    delegate.intersects(MOCK_GEOM, POLYGON);
  }

  @Test
  public void testBeyondFilter() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Beyond.toString());

    FilterType filter = delegate.beyond(Metacard.ANY_GEO, POLYGON, DISTANCE);
    assertTrue(filter.isSetSpatialOps());
    assertTrue(filter.getSpatialOps().getValue() instanceof DistanceBufferType);
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
  public void testBeyondFilterUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);

    FilterType filter = delegate.beyond(Metacard.ANY_GEO, POLYGON, DISTANCE);
    assertTrue(filter == null);
  }

  @Test
  public void testContainsFilter() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Contains.toString());

    FilterType filter = delegate.contains(Metacard.ANY_GEO, POLYGON);
    assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
    assertFalse(filter.isSetLogicOps());
  }

  @Test
  public void testContainsUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);

    FilterType filter = delegate.contains(Metacard.ANY_GEO, POLYGON);
    assertTrue(filter == null);
  }

  @Test
  public void testCrossesFilter() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Crosses.toString());

    FilterType filter = delegate.crosses(Metacard.ANY_GEO, POLYGON);

    assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
    assertFalse(filter.isSetLogicOps());
  }

  @Test
  public void testCrossesUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);

    FilterType filter = delegate.crosses(Metacard.ANY_GEO, POLYGON);
    assertTrue(filter == null);
  }

  @Test
  public void testDisjointFilter() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Disjoint.toString());

    FilterType filter = delegate.disjoint(Metacard.ANY_GEO, POLYGON);
    assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
  }

  @Test
  public void testDisjointAsNotBBox() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.BBOX.toString());

    FilterType filter = delegate.disjoint(Metacard.ANY_GEO, POLYGON);
    assertTrue(filter.getLogicOps().getValue() instanceof UnaryLogicOpType);
    UnaryLogicOpType type = (UnaryLogicOpType) filter.getLogicOps().getValue();
    assertTrue(type.getSpatialOps().getValue() instanceof BBOXType);
  }

  @Test
  public void testDWithinFilterPolygon() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.DWithin.toString());

    FilterType filter = delegate.dwithin(Metacard.ANY_GEO, POLYGON, DISTANCE);
    assertFalse(filter.isSetLogicOps());
    assertTrue(filter.isSetSpatialOps());
    assertTrue(filter.getSpatialOps().getValue() instanceof DistanceBufferType);
  }

  @Test
  public void testDWithinFilterPoint() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.DWithin.toString());

    FilterType filter = delegate.dwithin(Metacard.ANY_GEO, POINT, DISTANCE);
    assertFalse(filter.isSetLogicOps());
    assertTrue(filter.isSetSpatialOps());
    assertTrue(filter.getSpatialOps().getValue() instanceof DistanceBufferType);
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
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Intersect.toString());
    /**
     * Made distance a large value so if the original WKT and the buffered WKT are plotted at:
     * http://openlayers.org/dev/examples/vector-formats.html one can easily see the buffer.
     */
    double distance = 200000.0;
    FilterType filter = delegate.dwithin(Metacard.ANY_GEO, POINT, distance);

    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);

    LOGGER.debug(writer.toString());
    XMLUnit.setNormalizeWhitespace(true);
    XMLUnit.setIgnoreWhitespace(true);
    assertXMLEqual(getDWithinAsIntersectsXml(), writer.toString());
  }

  @Test
  public void testDwithinUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);

    FilterType filter = delegate.dwithin(Metacard.ANY_GEO, POLYGON, DISTANCE);
    assertTrue(filter == null);
  }

  @Test
  public void testIntersects() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Intersect.toString());

    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

    assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
    assertFalse(filter.isSetLogicOps());
  }

  @Test
  public void testIntersectsAsBoundingBox() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.BBOX.toString());

    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);
    assertTrue(filter instanceof FilterType);
    assertTrue(filter.getSpatialOps().getValue() instanceof BBOXType);
    assertFalse(filter.isSetLogicOps());
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
  public void testIntersectsUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);

    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);
    assertTrue(filter == null);
  }

  @Test
  public void testOverlapsFilter() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Overlaps.toString());
    FilterType filter = delegate.overlaps(Metacard.ANY_GEO, POLYGON);

    assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
    assertFalse(filter.isSetLogicOps());
  }

  @Test
  public void testOverlapsUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);
    FilterType filter = delegate.overlaps(Metacard.ANY_GEO, POLYGON);

    assertTrue(filter == null);
  }

  @Test
  public void testTouchesFilter() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Touches.toString());

    FilterType filter = delegate.touches(Metacard.ANY_GEO, POLYGON);

    assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
    assertFalse(filter.isSetLogicOps());
  }

  @Test
  public void testTouchesUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);
    FilterType filter = delegate.touches(Metacard.ANY_GEO, POLYGON);

    assertTrue(filter == null);
  }

  @Test
  public void testWithinFilter() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Within.toString());

    FilterType filter = delegate.within(Metacard.ANY_GEO, POLYGON);

    assertTrue(filter.getSpatialOps().getValue() instanceof BinarySpatialOpType);
    assertFalse(filter.isSetLogicOps());
  }

  @Test
  public void testWithinUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);
    FilterType filter = delegate.within(Metacard.ANY_GEO, POLYGON);

    assertTrue(filter == null);
  }

  @Test
  public void testIntersectsMultipleProperties() {

    List<String> gmlProps = new ArrayList<String>();
    gmlProps.add(MOCK_GEOM);
    gmlProps.add(MOCK_GEOM2);
    when(featureMetacardType.getGmlProperties()).thenReturn(gmlProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_GEOM))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_GEOM, MOCK_GEOM, true, false, false, false, BasicTypes.STRING_TYPE));

    when(featureMetacardType.getAttributeDescriptor(MOCK_GEOM2))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_GEOM2, MOCK_GEOM2, true, false, false, false, BasicTypes.STRING_TYPE));

    List<String> supportedGeo = Arrays.asList(SPATIAL_OPERATORS.Intersect.toString());
    WfsFilterDelegate delegate = new WfsFilterDelegate(featureMetacardType, supportedGeo, SRS_NAME);

    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);
    assertNotNull(filter);
    assertTrue(filter.isSetLogicOps());
    assertNotNull(filter.getLogicOps());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSingleGmlPropertyBlacklisted() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Contains.toString());
    when(featureMetacardType.getAttributeDescriptor(MOCK_GEOM))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_GEOM, MOCK_GEOM, false, false, false, false, BasicTypes.STRING_TYPE));

    delegate.contains(MOCK_GEOM, POLYGON);
  }

  @Test
  public void testAllGmlPropertiesBlacklisted() {
    List<String> gmlProps = new ArrayList<String>();
    gmlProps.add(MOCK_GEOM);
    gmlProps.add(MOCK_GEOM2);
    when(featureMetacardType.getGmlProperties()).thenReturn(gmlProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_GEOM))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_GEOM, MOCK_GEOM, false, false, false, false, BasicTypes.STRING_TYPE));

    when(featureMetacardType.getAttributeDescriptor(MOCK_GEOM))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_GEOM, MOCK_GEOM, false, false, false, false, BasicTypes.STRING_TYPE));
    List<String> supportedGeo = Arrays.asList(SPATIAL_OPERATORS.Intersect.toString());
    WfsFilterDelegate delegate = new WfsFilterDelegate(featureMetacardType, supportedGeo, SRS_NAME);

    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);
    assertNull(filter);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadPolygonWkt() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.Intersect.toString());
    delegate.intersects(Metacard.ANY_GEO, "junk");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadPointWkt() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.DWithin.toString());
    delegate.dwithin(Metacard.ANY_GEO, "junk", DISTANCE);
  }

  @Test
  public void testNonEpsg4326Srs() {
    List<String> gmlProps = new ArrayList<String>();
    gmlProps.add(MOCK_GEOM);
    when(featureMetacardType.getGmlProperties()).thenReturn(gmlProps);

    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            featureMetacardType,
            Arrays.asList(SPATIAL_OPERATORS.Intersect.toString()),
            "EPSG:42304");
    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

    assertTrue(filter == null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGeoFilterNullMetacardType() {
    List<String> supportedGeo = Arrays.asList(SPATIAL_OPERATORS.Beyond.toString());

    WfsFilterDelegate delegate = new WfsFilterDelegate(null, supportedGeo, SRS_NAME);

    delegate.beyond(Metacard.ANY_GEO, POLYGON, DISTANCE);
  }

  private JAXBElement<FilterType> getFilterTypeJaxbElement(FilterType filterType) {
    JAXBElement<FilterType> filterTypeJaxbElement =
        new JAXBElement<FilterType>(
            new QName("http://www.opengis.net/ogc", FILTER_QNAME_LOCAL_PART),
            FilterType.class,
            filterType);
    return filterTypeJaxbElement;
  }

  private static JAXBContext initJaxbContext() {

    JAXBContext jaxbContext = null;

    try {
      jaxbContext =
          JAXBContext.newInstance(
              "ogc.schema.opengis.filter.v_1_0_0:ogc.schema.opengis.gml.v_2_1_2");
    } catch (JAXBException e) {
      LOGGER.error(e.getMessage(), e);
    }

    return jaxbContext;
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

  private Date getEndDate() {
    String dateString = "Jul 11 2002";
    SimpleDateFormat formatter = new SimpleDateFormat("MMM d yyyy");
    Date date = null;
    try {
      date = formatter.parse(dateString);
    } catch (ParseException e) {
      LOGGER.error(e.getMessage(), e);
    }
    return date;
  }

  private DateTime convertDateToIso8601Format(Date inputDate) {
    DateTime outputDate = new DateTime(inputDate);
    return outputDate;
  }

  private String getPropertyEqualToXmlDate() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
        + "<PropertyIsEqualTo>"
        + "<Literal>"
        + convertDateToIso8601Format(date)
        + "</Literal>"
        + "<PropertyName>mockProperty</PropertyName>"
        + "</PropertyIsEqualTo>"
        + "</Filter>";
  }

  private String getPropertyNotEqualToXmlDate() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
        + "<PropertyIsNotEqualTo>"
        + "<PropertyName>mockProperty</PropertyName>"
        + "<Literal>"
        + convertDateToIso8601Format(date)
        + "</Literal>"
        + "</PropertyIsNotEqualTo>"
        + "</Filter>";
  }

  private String getPropertyGreaterThanXmlDate() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
        + "<PropertyIsGreaterThan>"
        + "<PropertyName>mockProperty</PropertyName>"
        + "<Literal>"
        + convertDateToIso8601Format(date)
        + "</Literal>"
        + "</PropertyIsGreaterThan>"
        + "</Filter>";
  }

  private String getPropertyGreaterThanOrEqualToXmlDate() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
        + "<PropertyIsGreaterThanOrEqualTo>"
        + "<PropertyName>mockProperty</PropertyName>"
        + "<Literal>"
        + convertDateToIso8601Format(date)
        + "</Literal>"
        + "</PropertyIsGreaterThanOrEqualTo>"
        + "</Filter>";
  }

  private String getPropertyLessThanXmlDate() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
        + "<PropertyIsLessThan>"
        + "<PropertyName>mockProperty</PropertyName>"
        + "<Literal>"
        + convertDateToIso8601Format(date)
        + "</Literal>"
        + "</PropertyIsLessThan>"
        + "</Filter>";
  }

  private String getPropertyLessThanOrEqualToXmlDate() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
        + "<PropertyIsLessThanOrEqualTo>"
        + "<PropertyName>mockProperty</PropertyName>"
        + "<Literal>"
        + convertDateToIso8601Format(date)
        + "</Literal>"
        + "</PropertyIsLessThanOrEqualTo>"
        + "</Filter>";
  }

  private String getPropertyBetweenXmlDate() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
        + "<PropertyIsBetween>"
        + "<PropertyName>mockProperty</PropertyName>"
        + "<LowerBoundary><Literal>"
        + convertDateToIso8601Format(date)
        + "</Literal></LowerBoundary>"
        + "<UpperBoundary><Literal>"
        + convertDateToIso8601Format(endDate)
        + "</Literal></UpperBoundary>"
        + "</PropertyIsBetween>"
        + "</Filter>";
  }

  private String getDWithinAsIntersectsXml() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
        + "<Intersects>"
        + "<PropertyName>ground_geom</PropertyName>"
        + "<ns2:Polygon srsName=\"EPSG:4326\">"
        + "<ns2:outerBoundaryIs>"
        + "<ns2:LinearRing>"
        + "<ns2:coordinates decimal=\".\" cs=\",\" ts=\" \">"
        + "31.79864073552333,-10.0 "
        + "31.76408035813492,-10.350897400284572 "
        + "31.66172736189105,-10.688310010261736 "
        + "31.495515115037147,-10.999271252553244 "
        + "31.271831061006903,-11.271831061006903 "
        + "30.999271252553243,-11.495515115037145 "
        + "30.688310010261738,-11.66172736189105 "
        + "30.350897400284573,-11.764080358134919 "
        + "30.0,-11.798640735523328 "
        + "29.649102599715427,-11.764080358134919 "
        + "29.311689989738262,-11.66172736189105 "
        + "29.000728747446757,-11.495515115037145 "
        + "28.728168938993097,-11.271831061006905 "
        + "28.504484884962853,-10.999271252553244 "
        + "28.33827263810895,-10.688310010261736 "
        + "28.23591964186508,-10.350897400284572 "
        + "28.20135926447667,-9.999999999999998 "
        + "28.23591964186508,-9.649102599715427 "
        + "28.338272638108954,-9.311689989738262 "
        + "28.504484884962856,-9.000728747446754 "
        + "28.728168938993097,-8.728168938993093 "
        + "29.000728747446757,-8.504484884962853 "
        + "29.311689989738266,-8.33827263810895 "
        + "29.649102599715434,-8.23591964186508 "
        + "30.000000000000004,-8.201359264476672 "
        + "30.350897400284577,-8.235919641865081 "
        + "30.68831001026174,-8.338272638108954 "
        + "30.99927125255325,-8.504484884962856 "
        + "31.271831061006907,-8.7281689389931 "
        + "31.49551511503715,-9.000728747446761 "
        + "31.661727361891053,-9.31168998973827 "
        + "31.76408035813492,-9.649102599715436 "
        + "31.79864073552333,-10.0 "
        + "</ns2:coordinates>"
        + "</ns2:LinearRing>"
        + "</ns2:outerBoundaryIs>"
        + "</ns2:Polygon>"
        + "</Intersects>"
        + "</Filter>";
  }
}
