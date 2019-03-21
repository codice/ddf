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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.source;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BasicTypes;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import net.opengis.filter.v_1_1_0.BBOXType;
import net.opengis.filter.v_1_1_0.BinarySpatialOpType;
import net.opengis.filter.v_1_1_0.DistanceBufferType;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.UnaryLogicOpType;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureAttributeDescriptor;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.Wfs11Constants.SPATIAL_OPERATORS;
import org.custommonkey.xmlunit.XMLUnit;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@SuppressWarnings("FieldCanBeLocal")
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
          + "<PropertyIsEqualTo matchCase=\"true\">"
          + "<Literal>false</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsEqualTo>"
          + "</Filter>";

  private final String propertyIsEqualToXml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsEqualTo matchCase=\"true\">"
          + "<Literal>1</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsEqualTo>"
          + "</Filter>";

  private final String propertyIsEqualToXmlDecimal =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsEqualTo matchCase=\"true\">"
          + "<Literal>1.0</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsEqualTo>"
          + "</Filter>";

  private final String propertyIsEqualToXmlLiteral =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsEqualTo matchCase=\"true\">"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsEqualTo>"
          + "</Filter>";

  private final String propertyIsEqualToXmlLiteralMatchCaseFalse =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsEqualTo matchCase=\"false\">"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsEqualTo>"
          + "</Filter>";

  private final String propertyNotEqualToXmlLiteral =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsNotEqualTo matchCase=\"true\">"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsNotEqualTo>"
          + "</Filter>";

  private final String propertyNotEqualToXmlLiteralMatchCaseFalse =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsNotEqualTo matchCase=\"false\">"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsNotEqualTo>"
          + "</Filter>";

  private String propertyIsEqualToXmlDate = getPropertyEqualToXmlDate();

  private final String propertyNotEqualToXml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsNotEqualTo matchCase=\"true\">"
          + "<Literal>1</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsNotEqualTo>"
          + "</Filter>";

  private final String propertyNotEqualToXmlDecimal =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsNotEqualTo matchCase=\"true\">"
          + "<Literal>1.0</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsNotEqualTo>"
          + "</Filter>";

  private final String propertyNotEqualToXmlDate = getPropertyNotEqualToXmlDate();

  private final String propertyNotEqualToXmlBoolean =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsNotEqualTo matchCase=\"true\">"
          + "<Literal>false</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsNotEqualTo>"
          + "</Filter>";

  private final String propertyGreaterThanXmlLiteral =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsGreaterThan matchCase=\"true\">"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsGreaterThan>"
          + "</Filter>";

  private final String propertyGreaterThanXml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsGreaterThan matchCase=\"true\">"
          + "<Literal>1</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsGreaterThan>"
          + "</Filter>";

  private final String propertyGreaterThanXmlDecimal =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsGreaterThan matchCase=\"true\">"
          + "<Literal>1.0</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsGreaterThan>"
          + "</Filter>";

  private final String propertyGreaterThanXmlDate = getPropertyGreaterThanXmlDate();

  private final String propertyGreaterThanOrEqualToXmlLiteral =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsGreaterThanOrEqualTo matchCase=\"true\">"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsGreaterThanOrEqualTo>"
          + "</Filter>";

  private final String propertyGreaterThanOrEqualToXml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsGreaterThanOrEqualTo matchCase=\"true\">"
          + "<Literal>1</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsGreaterThanOrEqualTo>"
          + "</Filter>";

  private final String propertyGreaterThanOrEqualToXmlDecimal =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsGreaterThanOrEqualTo matchCase=\"true\">"
          + "<Literal>1.0</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsGreaterThanOrEqualTo>"
          + "</Filter>";

  private final String propertyGreaterThanOrEqualToXmlDate =
      getPropertyGreaterThanOrEqualToXmlDate();

  private final String propertyLessThanXmlLiteral =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsLessThan matchCase=\"true\">"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsLessThan>"
          + "</Filter>";

  private final String propertyLessThanXml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsLessThan matchCase=\"true\">"
          + "<Literal>1</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsLessThan>"
          + "</Filter>";

  private final String propertyLessThanXmlDecimal =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsLessThan matchCase=\"true\">"
          + "<Literal>1.0</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsLessThan>"
          + "</Filter>";

  private final String propertyLessThanXmlDate = getPropertyLessThanXmlDate();

  private final String propertyLessThanOrEqualToXmlLiteral =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsLessThanOrEqualTo matchCase=\"true\">"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsLessThanOrEqualTo>"
          + "</Filter>";

  private final String propertyLessThanOrEqualToXml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsLessThanOrEqualTo matchCase=\"true\">"
          + "<Literal>1</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsLessThanOrEqualTo>"
          + "</Filter>";

  private final String propertyLessThanOrEqualToXmlDecimal =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsLessThanOrEqualTo matchCase=\"true\">"
          + "<Literal>1.0</Literal>"
          + "<PropertyName>mockProperty</PropertyName>"
          + "</PropertyIsLessThanOrEqualTo>"
          + "</Filter>";

  private final String propertyLessThanOrEqualToXmlDate = getPropertyLessThanOrEqualToXmlDate();

  private final String propertyIsLikeXmlLiteral =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsLike matchCase=\"true\" escapeChar=\"!\" singleChar=\"?\" wildCard=\"*\">"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsLike>"
          + "</Filter>";

  private final String propertyIsLikeXmlLiteralMatchCaseFalse =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<PropertyIsLike matchCase=\"false\" escapeChar=\"!\" singleChar=\"?\" wildCard=\"*\">"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsLike>"
          + "</Filter>";

  private final String propertyIsLikeXmlLiteralAnyText =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<Or>"
          + "<PropertyIsLike matchCase=\"true\" escapeChar=\"!\" singleChar=\"?\" wildCard=\"*\">"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsLike>"
          + "<PropertyIsLike matchCase=\"true\" escapeChar=\"!\" singleChar=\"?\" wildCard=\"*\">"
          + "<PropertyName>mockProperty2</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsLike>"
          + "</Or>"
          + "</Filter>";

  private final String propertyIsLikeXmlLiteralAnyTextMatchCaseFalse =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
          + "<Or>"
          + "<PropertyIsLike matchCase=\"false\" escapeChar=\"!\" singleChar=\"?\" wildCard=\"*\">"
          + "<PropertyName>mockProperty</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsLike>"
          + "<PropertyIsLike matchCase=\"false\" escapeChar=\"!\" singleChar=\"?\" wildCard=\"*\">"
          + "<PropertyName>mockProperty2</PropertyName>"
          + "<Literal>Literal</Literal>"
          + "</PropertyIsLike>"
          + "</Or>"
          + "</Filter>";

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

  private List<String> mockGmlProps = new ArrayList<>();

  private List<String> mockProps;

  @Before
  public void setUp() {
    mockProps = new ArrayList<>();
    when(featureMetacardType.getGmlProperties()).thenReturn(mockGmlProps);
  }

  @Test
  public void testWFSFilterDelegate() {
    WfsFilterDelegate delegate = createDelegate();

    assertThat(delegate, notNullValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWFSFilterDelegateNullSchema() {
    new WfsFilterDelegate(null, null);
  }

  @Test
  public void testAnd() {
    WfsFilterDelegate delegate = createTextualDelegate();
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    FilterType filterToCheck = delegate.and(Arrays.asList(filter, filter));
    assertThat(filterToCheck, notNullValue());
    assertThat(filterToCheck.isSetLogicOps(), is(true));
  }

  @Test
  public void testAndSingleFilter() {
    WfsFilterDelegate delegate = createTextualDelegate();
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    ArrayList<FilterType> filters = new ArrayList<>();
    filters.add(filter);
    filters.add(new FilterType());
    FilterType filterToCheck = delegate.and(filters);
    assertThat(filterToCheck, notNullValue());
    // Should not have an AND filter
    assertThat(filterToCheck.isSetLogicOps(), is(false));
  }

  @Test
  public void testOr() {
    WfsFilterDelegate delegate = createTextualDelegate();
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    FilterType filterToCheck = delegate.or(Arrays.asList(filter, filter));
    assertThat(filterToCheck, notNullValue());
    assertThat(filterToCheck.isSetLogicOps(), is(true));
  }

  @Test
  public void testOrSingleFilter() {
    WfsFilterDelegate delegate = createTextualDelegate();
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    ArrayList<FilterType> filters = new ArrayList<>();
    filters.add(filter);
    filters.add(new FilterType());
    FilterType filterToCheck = delegate.or(filters);
    assertThat(filterToCheck, notNullValue());
    // Should not have an AND filter
    assertThat(filterToCheck.isSetLogicOps(), is(false));
  }

  @Test
  public void testNot() {
    WfsFilterDelegate delegate = createTextualDelegate();
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    FilterType filterToCheck = delegate.not(filter);
    assertThat(filterToCheck, notNullValue());
    assertThat(filterToCheck.isSetLogicOps(), is(true));
  }

  @Test
  public void testPropertyIsEqualToStringStringBoolean()
      throws JAXBException, SAXException, IOException {

    whenPropertiesStringType();

    WfsFilterDelegate delegate = createDelegate();

    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, LITERAL, true);

    assertXMLEqual(propertyIsEqualToXmlLiteral, marshal(filter));
  }

  @Test
  public void testPropertyIsEqualToStringMatchCase()
      throws JAXBException, SAXException, IOException {

    whenPropertiesStringType();

    WfsFilterDelegate delegate = createDelegate();

    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, LITERAL, false);

    assertXMLEqual(propertyIsEqualToXmlLiteralMatchCaseFalse, marshal(filter));

    filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, LITERAL, true);

    assertXMLEqual(propertyIsEqualToXmlLiteral, marshal(filter));
  }

  @Test
  public void testPropertyIsEqualToStringStringBooleanAnyText() {
    WfsFilterDelegate delegate = createTextualDelegate();
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    // 1 property will produce a ComparisonOp
    assertThat(filter.isSetComparisonOps(), is(true));
    assertThat(filter.getComparisonOps(), notNullValue());
    assertThat(filter.getComparisonOps(), is(instanceOf(JAXBElement.class)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyIsEqualToStringStringBooleanAnyTextNullMetacardType() {

    WfsFilterDelegate delegate = new WfsFilterDelegate(null, SUPPORTED_GEO);
    delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
  }

  @Test
  public void testPropertyIsEqualToStringStringBooleanAnyTextMultipleProperties() {
    mockProps.add(MOCK_PROPERTY);
    mockProps.add(MOCK_PROPERTY_2);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.STRING_TYPE));

    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY_2))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY_2, MOCK_PROPERTY_2, true, true, true, true, BasicTypes.STRING_TYPE));

    WfsFilterDelegate delegate = createDelegate();
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    // Mulitple properties will produce a LogicOp (OR)
    assertThat(filter.isSetComparisonOps(), is(false));
    assertThat(filter.isSetLogicOps(), is(true));
    assertThat(filter.getLogicOps(), notNullValue());
    assertThat(filter.getLogicOps(), is(instanceOf(JAXBElement.class)));
  }

  @Test
  public void testPropertyIsEqualToDate() throws JAXBException, SAXException, IOException {
    whenPropertiesDateType();
    WfsFilterDelegate delegate = createDelegate();

    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, date);

    assertXMLEqual(propertyIsEqualToXmlDate, marshal(filter));
  }

  @Test
  public void testPropertyIsEqualToInt() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createIntegerDelegate();
    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, 1);

    assertXMLEqual(propertyIsEqualToXml, marshal(filter));
  }

  @Test
  public void testPropertyIsEqualToShort() throws JAXBException, SAXException, IOException {
    whenPropertiesShortType();
    WfsFilterDelegate delegate = createDelegate();
    short literal = 1;
    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyIsEqualToXml, marshal(filter));
  }

  @Test
  public void testPropertyIsEqualToLong() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    long literal = 1L;
    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyIsEqualToXml, marshal(filter));
  }

  @Test
  public void testPropertyIsEqualToFloat() throws JAXBException, SAXException, IOException {
    mockProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.FLOAT_TYPE));
    WfsFilterDelegate delegate = createDelegate();

    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, 1.0F);

    assertXMLEqual(propertyIsEqualToXmlDecimal, marshal(filter));
  }

  @Test
  public void testPropertyIsEqualToDouble() throws JAXBException, SAXException, IOException {
    mockProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.DOUBLE_TYPE));
    WfsFilterDelegate delegate = createDelegate();

    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, 1.0);

    assertXMLEqual(propertyIsEqualToXmlDecimal, marshal(filter));
  }

  @Test
  public void testPropertyIsEqualToBoolean() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createBooleanDelegate();

    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, false);

    assertXMLEqual(propertyIsEqualToXmlBoolean, marshal(filter));
  }

  @Test
  public void testPropertyIsNotEqualToString() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createBooleanDelegate();

    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, LITERAL, true);

    assertXMLEqual(propertyNotEqualToXmlLiteral, marshal(filter));
  }

  @Test
  public void testPropertyIsNotEqualToStringMatchCase()
      throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createBooleanDelegate();

    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, LITERAL, true);

    assertXMLEqual(propertyNotEqualToXmlLiteral, marshal(filter));

    filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, LITERAL, false);

    assertXMLEqual(propertyNotEqualToXmlLiteralMatchCaseFalse, marshal(filter));
  }

  @Test
  public void testPropertyIsNotEqualToDate() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createBooleanDelegate();

    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, date);

    assertXMLEqual(propertyNotEqualToXmlDate, marshal(filter));
  }

  @Test
  public void testPropertyIsNotEqualToInt() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createIntegerDelegate();
    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, 1);

    assertXMLEqual(propertyNotEqualToXml, marshal(filter));
  }

  @Test
  public void testPropertyIsNotEqualToShort() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createIntegerDelegate();
    short literal = 1;

    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyNotEqualToXml, marshal(filter));
  }

  @Test
  public void testPropertyIsNotEqualToLong() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    long literal = 1L;
    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyNotEqualToXml, marshal(filter));
  }

  @Test
  public void testPropertyIsNotEqualToFloat() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    float literal = 1.0F;
    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyNotEqualToXmlDecimal, marshal(filter));
  }

  @Test
  public void testPropertyIsNotEqualToDouble() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    double literal = 1.0;
    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyNotEqualToXmlDecimal, marshal(filter));
  }

  @Test
  public void testPropertyIsNotEqualToBoolean() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();

    FilterType filter = delegate.propertyIsNotEqualTo(MOCK_PROPERTY, false);

    assertXMLEqual(propertyNotEqualToXmlBoolean, marshal(filter));
  }

  @Test
  public void testPropertyIsGreaterThanString() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createBooleanDelegate();

    FilterType filter = delegate.propertyIsGreaterThan(MOCK_PROPERTY, LITERAL);

    assertXMLEqual(propertyGreaterThanXmlLiteral, marshal(filter));
  }

  @Test
  public void testPropertyIsGreaterThanDate() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createBooleanDelegate();

    FilterType filter = delegate.propertyIsGreaterThan(MOCK_PROPERTY, date);

    assertXMLEqual(propertyGreaterThanXmlDate, marshal(filter));
  }

  @Test
  public void testPropertyIsGreaterThanInt() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createIntegerDelegate();
    FilterType filter = delegate.propertyIsGreaterThan(MOCK_PROPERTY, 1);

    assertXMLEqual(propertyGreaterThanXml, marshal(filter));
  }

  @Test
  public void testPropertyIsGreaterThanShort() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createIntegerDelegate();
    short literal = 1;

    FilterType filter = delegate.propertyIsGreaterThan(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyGreaterThanXml, marshal(filter));
  }

  @Test
  public void testPropertyIsGreaterThanLong() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    long literal = 1L;
    FilterType filter = delegate.propertyIsGreaterThan(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyGreaterThanXml, marshal(filter));
  }

  @Test
  public void testPropertyIsGreaterThanFloat() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    float literal = 1.0F;
    FilterType filter = delegate.propertyIsGreaterThan(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyGreaterThanXmlDecimal, marshal(filter));
  }

  @Test
  public void testPropertyIsGreaterThanDouble() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    double literal = 1.0;
    FilterType filter = delegate.propertyIsGreaterThan(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyGreaterThanXmlDecimal, marshal(filter));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToString()
      throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createBooleanDelegate();

    FilterType filter = delegate.propertyIsGreaterThanOrEqualTo(MOCK_PROPERTY, LITERAL);

    assertXMLEqual(propertyGreaterThanOrEqualToXmlLiteral, marshal(filter));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToDate()
      throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createBooleanDelegate();

    FilterType filter = delegate.propertyIsGreaterThanOrEqualTo(MOCK_PROPERTY, date);
    assertXMLEqual(propertyGreaterThanOrEqualToXmlDate, marshal(filter));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToInt()
      throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createIntegerDelegate();
    FilterType filter = delegate.propertyIsGreaterThanOrEqualTo(MOCK_PROPERTY, 1);

    assertXMLEqual(propertyGreaterThanOrEqualToXml, marshal(filter));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToShort()
      throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createIntegerDelegate();
    short literal = 1;

    FilterType filter = delegate.propertyIsGreaterThanOrEqualTo(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyGreaterThanOrEqualToXml, marshal(filter));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToLong()
      throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    long literal = 1L;
    FilterType filter = delegate.propertyIsGreaterThanOrEqualTo(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyGreaterThanOrEqualToXml, marshal(filter));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToFloat()
      throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    float literal = 1.0F;
    FilterType filter = delegate.propertyIsGreaterThanOrEqualTo(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyGreaterThanOrEqualToXmlDecimal, marshal(filter));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToDouble()
      throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    double literal = 1.0;
    FilterType filter = delegate.propertyIsGreaterThanOrEqualTo(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyGreaterThanOrEqualToXmlDecimal, marshal(filter));
  }

  @Test
  public void testPropertyIsLessThanString() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createBooleanDelegate();

    FilterType filter = delegate.propertyIsLessThan(MOCK_PROPERTY, LITERAL);

    assertXMLEqual(propertyLessThanXmlLiteral, marshal(filter));
  }

  @Test
  public void testPropertyIsLessThanDate() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createBooleanDelegate();

    FilterType filter = delegate.propertyIsLessThan(MOCK_PROPERTY, date);

    assertXMLEqual(propertyLessThanXmlDate, marshal(filter));
  }

  @Test
  public void testPropertyIsLessThanInt() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createIntegerDelegate();
    FilterType filter = delegate.propertyIsLessThan(MOCK_PROPERTY, 1);

    assertXMLEqual(propertyLessThanXml, marshal(filter));
  }

  @Test
  public void testPropertyIsLessThanShort() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createIntegerDelegate();
    short literal = 1;

    FilterType filter = delegate.propertyIsLessThan(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyLessThanXml, marshal(filter));
  }

  @Test
  public void testPropertyIsLessThanLong() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    long literal = 1L;
    FilterType filter = delegate.propertyIsLessThan(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyLessThanXml, marshal(filter));
  }

  @Test
  public void testPropertyIsLessThanFloat() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    float literal = 1.0F;
    FilterType filter = delegate.propertyIsLessThan(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyLessThanXmlDecimal, marshal(filter));
  }

  @Test
  public void testPropertyIsLessThanDouble() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    double literal = 1.0;
    FilterType filter = delegate.propertyIsLessThan(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyLessThanXmlDecimal, marshal(filter));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToString()
      throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createBooleanDelegate();

    FilterType filter = delegate.propertyIsLessThanOrEqualTo(MOCK_PROPERTY, LITERAL);

    assertXMLEqual(propertyLessThanOrEqualToXmlLiteral, marshal(filter));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToDate()
      throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createBooleanDelegate();

    FilterType filter = delegate.propertyIsLessThanOrEqualTo(MOCK_PROPERTY, date);

    assertXMLEqual(propertyLessThanOrEqualToXmlDate, marshal(filter));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToInt() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createIntegerDelegate();
    FilterType filter = delegate.propertyIsLessThanOrEqualTo(MOCK_PROPERTY, 1);

    assertXMLEqual(propertyLessThanOrEqualToXml, marshal(filter));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToShort()
      throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createIntegerDelegate();
    short literal = 1;

    FilterType filter = delegate.propertyIsLessThanOrEqualTo(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyLessThanOrEqualToXml, marshal(filter));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToLong()
      throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    long literal = 1L;
    FilterType filter = delegate.propertyIsLessThanOrEqualTo(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyLessThanOrEqualToXml, marshal(filter));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToFloat()
      throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    float literal = 1.0F;
    FilterType filter = delegate.propertyIsLessThanOrEqualTo(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyLessThanOrEqualToXmlDecimal, marshal(filter));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToDouble()
      throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    double literal = 1.0;
    FilterType filter = delegate.propertyIsLessThanOrEqualTo(MOCK_PROPERTY, literal);

    assertXMLEqual(propertyLessThanOrEqualToXmlDecimal, marshal(filter));
  }

  @Test
  public void testPropertyIsBetweenString() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createBooleanDelegate();

    FilterType filter = delegate.propertyIsBetween(MOCK_PROPERTY, LITERAL, UNLITERAL);
    assertXMLEqual(propertyBetweenXmlLiteral, marshal(filter));
  }

  @Test
  public void testPropertyIsBetweenDate() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createBooleanDelegate();

    FilterType filter = delegate.propertyIsBetween(MOCK_PROPERTY, date, getEndDate());

    assertXMLEqual(propertyBetweenXmlDate, marshal(filter));
  }

  @Test
  public void testPropertyIsBetweenInt() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createIntegerDelegate();
    FilterType filter = delegate.propertyIsBetween(MOCK_PROPERTY, 1, 10);

    assertXMLEqual(propertyBetweenXml, marshal(filter));
  }

  @Test
  public void testPropertyIsBetweenShort() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createIntegerDelegate();
    short lower = 1;
    short upper = 10;

    FilterType filter = delegate.propertyIsBetween(MOCK_PROPERTY, lower, upper);

    assertXMLEqual(propertyBetweenXml, marshal(filter));
  }

  @Test
  public void testPropertyIsBetweenLong() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    long lower = 1L;
    long upper = 10L;

    FilterType filter = delegate.propertyIsBetween(MOCK_PROPERTY, lower, upper);

    assertXMLEqual(propertyBetweenXml, marshal(filter));
  }

  @Test
  public void testPropertyIsBetweenFloat() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    float lower = 1.0F;
    float upper = 10.0F;
    FilterType filter = delegate.propertyIsBetween(MOCK_PROPERTY, lower, upper);

    assertXMLEqual(propertyBetweenXmlDecimal, marshal(filter));
  }

  @Test
  public void testPropertyIsBetweenDouble() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    double lower = 1.0;
    double upper = 10.0;
    FilterType filter = delegate.propertyIsBetween(MOCK_PROPERTY, lower, upper);

    assertXMLEqual(propertyBetweenXmlDecimal, marshal(filter));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyIsBetweenNullLowerBoundary() {
    WfsFilterDelegate delegate = createDelegate();
    delegate.propertyIsBetween(MOCK_PROPERTY, null, LITERAL);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyIsBetweenNullUpperBoundary() {
    WfsFilterDelegate delegate = createDelegate();
    delegate.propertyIsBetween(MOCK_PROPERTY, LITERAL, null);
  }

  @Test
  public void testPropertyIsLikeStringStringBoolean() {
    mockProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockProps);
    WfsFilterDelegate delegate = createDelegate();
    FilterType filter = delegate.propertyIsLike(PROPERTY_NAME, LITERAL, true);
    // Ensure this is an invalid FilterType
    assertThat(filter, nullValue());
  }

  @Test
  public void testPropertyIsLikeStringStringBooleanAnyText() throws Exception {
    WfsFilterDelegate delegate = createTextualDelegate();
    FilterType filter = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    assertXMLEqual(propertyIsLikeXmlLiteral, marshal(filter));
  }

  @Test
  public void testPropertyIsLikeMatchCase() throws Exception {
    whenPropertiesStringType();
    WfsFilterDelegate delegate = createDelegate();

    FilterType filter = delegate.propertyIsLike(MOCK_PROPERTY, LITERAL, true);
    assertXMLEqual(propertyIsLikeXmlLiteral, marshal(filter));

    filter = delegate.propertyIsLike(MOCK_PROPERTY, LITERAL, false);
    assertXMLEqual(propertyIsLikeXmlLiteralMatchCaseFalse, marshal(filter));
  }

  @Test
  public void testPropertyIsLikeStringStringBooleanAnyTextMultipleProperties() throws Exception {
    mockProps.add(MOCK_PROPERTY);
    mockProps.add(MOCK_PROPERTY_2);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.STRING_TYPE));
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY_2))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY_2, MOCK_PROPERTY_2, true, true, true, true, BasicTypes.STRING_TYPE));
    WfsFilterDelegate delegate = createDelegate();
    FilterType filter = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    assertXMLEqual(propertyIsLikeXmlLiteralAnyText, marshal(filter));
  }

  @Test
  public void testPropertyIsLikeAnyTextMultiplePropertiesMatchCase() throws Exception {
    mockProps.add(MOCK_PROPERTY);
    mockProps.add(MOCK_PROPERTY_2);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.STRING_TYPE));
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY_2))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY_2, MOCK_PROPERTY_2, true, true, true, true, BasicTypes.STRING_TYPE));
    WfsFilterDelegate delegate = createDelegate();
    FilterType filter = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    assertXMLEqual(propertyIsLikeXmlLiteralAnyText, marshal(filter));

    filter = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, false);
    assertXMLEqual(propertyIsLikeXmlLiteralAnyTextMatchCaseFalse, marshal(filter));
  }

  @Test
  public void testPropertyIsLikeAnyTextNoAttributes() {
    when(featureMetacardType.getTextualProperties()).thenReturn(mockProps);
    WfsFilterDelegate delegate = createDelegate();
    FilterType filter = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, true);
    assertThat(filter, nullValue());
  }

  @Test
  public void testPropertyIsEqualToAnyTextNoAttributes() {
    when(featureMetacardType.getTextualProperties()).thenReturn(mockProps);
    WfsFilterDelegate delegate = createDelegate();
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ANY_TEXT, LITERAL, true);
    assertThat(filter, nullValue());
  }

  @Test
  public void testPropertyIsEqualToMetacardId() {
    WfsFilterDelegate delegate = createDelegate();
    when(featureMetacardType.getName()).thenReturn("test");
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ID, "test.123", true);
    assertThat(filter, notNullValue());
  }

  @Test
  public void testPropertyIsEqualToMetacardIdSimpleId() {
    WfsFilterDelegate delegate = createDelegate();
    when(featureMetacardType.getName()).thenReturn("test");
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ID, "123", true);
    assertThat(filter, notNullValue());
  }

  @Test
  public void testPropertyIsEqualToMetacardIdMismatchFeature() {
    WfsFilterDelegate delegate = createDelegate();
    when(featureMetacardType.getName()).thenReturn("badType");
    FilterType filter = delegate.propertyIsEqualTo(Metacard.ID, "test.123", true);
    assertThat(filter, nullValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyIsLikePropertyBlacklisted() {
    mockProps.add(MOCK_PROPERTY);

    when(featureMetacardType.getProperties()).thenReturn(mockProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, false, false, false, false, BasicTypes.STRING_TYPE));

    WfsFilterDelegate delegate = createDelegate();

    delegate.propertyIsLike(MOCK_PROPERTY, LITERAL, false);
  }

  @Test
  public void testAllTextualPropertiesBlacklisted() {
    mockProps.add(MOCK_PROPERTY);
    mockProps.add(MOCK_PROPERTY_2);

    when(featureMetacardType.getProperties()).thenReturn(mockProps);
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
    WfsFilterDelegate delegate = createDelegate();

    FilterType filter = delegate.propertyIsLike(Metacard.ANY_TEXT, LITERAL, false);
    assertThat(filter, nullValue());
  }

  @Test
  public void testNonTextualPropertyIsLike() {
    WfsFilterDelegate delegate = createLongDelegate();
    FilterType filter = delegate.propertyIsLike(MOCK_PROPERTY, LITERAL, false);
    assertThat(filter, notNullValue());
  }

  @Test
  public void testNonTextualPropertyIsEqual() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = createLongDelegate();
    FilterType filter = delegate.propertyIsEqualTo(MOCK_PROPERTY, false);

    assertXMLEqual(propertyIsEqualToXmlBoolean, marshal(filter));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBlacklistedGeoProperty() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.BBOX.getValue());

    when(featureMetacardType.getAttributeDescriptor(MOCK_GEOM))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_GEOM, MOCK_GEOM, false, false, false, false, BasicTypes.STRING_TYPE));
    delegate.intersects(MOCK_GEOM, POLYGON);
  }

  @Test
  public void testBeyondFilter() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.BEYOND.getValue());

    FilterType filter = delegate.beyond(Metacard.ANY_GEO, POLYGON, DISTANCE);
    assertThat(filter.isSetSpatialOps(), is(true));
    assertThat(filter.getSpatialOps().getValue(), is(instanceOf(DistanceBufferType.class)));
  }

  @Test
  public void testBeyondAsNotDwithin() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.DWITHIN.getValue());

    FilterType filter = delegate.beyond(Metacard.ANY_GEO, POLYGON, DISTANCE);
    assertThat(filter.getLogicOps().getValue(), is(instanceOf(UnaryLogicOpType.class)));
    UnaryLogicOpType type = (UnaryLogicOpType) filter.getLogicOps().getValue();
    assertThat(type.getSpatialOps().getValue(), is(instanceOf(DistanceBufferType.class)));
  }

  @Test
  public void testBeyondFilterUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);

    FilterType filter = delegate.beyond(Metacard.ANY_GEO, POLYGON, DISTANCE);
    assertThat(filter, nullValue());
  }

  @Test
  public void testContainsFilter() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.CONTAINS.getValue());

    FilterType filter = delegate.contains(Metacard.ANY_GEO, POLYGON);
    assertBinarySpatialOpFilter(filter);
  }

  @Test
  public void testContainsUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);

    FilterType filter = delegate.contains(Metacard.ANY_GEO, POLYGON);
    assertThat(filter, nullValue());
  }

  @Test
  public void testCrossesFilter() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.CROSSES.getValue());

    FilterType filter = delegate.crosses(Metacard.ANY_GEO, POLYGON);

    assertBinarySpatialOpFilter(filter);
  }

  @Test
  public void testCrossesUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);

    FilterType filter = delegate.crosses(Metacard.ANY_GEO, POLYGON);
    assertThat(filter, nullValue());
  }

  @Test
  public void testDisjointFilter() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.DISJOINT.getValue());

    FilterType filter = delegate.disjoint(Metacard.ANY_GEO, POLYGON);
    assertThat(filter.getSpatialOps().getValue(), is(instanceOf(BinarySpatialOpType.class)));
  }

  @Test
  public void testDisjointAsNotBBox() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.BBOX.getValue());

    FilterType filter = delegate.disjoint(Metacard.ANY_GEO, POLYGON);
    assertThat(filter.getLogicOps().getValue(), is(instanceOf(UnaryLogicOpType.class)));
    UnaryLogicOpType type = (UnaryLogicOpType) filter.getLogicOps().getValue();
    assertThat(type.getSpatialOps().getValue(), is(instanceOf(BBOXType.class)));
  }

  @Test
  public void testDWithinFilterPolygon() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.DWITHIN.getValue());

    FilterType filter = delegate.dwithin(Metacard.ANY_GEO, POLYGON, DISTANCE);
    assertDistanceBufferFilter(filter);
  }

  @Test
  public void testDWithinFilterPoint() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.DWITHIN.getValue());

    FilterType filter = delegate.dwithin(Metacard.ANY_GEO, POINT, DISTANCE);
    assertDistanceBufferFilter(filter);
  }

  @Test
  public void testDwithinAsNotBeyond() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.BEYOND.getValue());

    FilterType filter = delegate.dwithin(Metacard.ANY_GEO, POLYGON, DISTANCE);
    assertThat(filter.getLogicOps().getValue(), is(instanceOf(UnaryLogicOpType.class)));
    UnaryLogicOpType type = (UnaryLogicOpType) filter.getLogicOps().getValue();
    assertThat(type.getSpatialOps().getValue(), is(instanceOf(DistanceBufferType.class)));
  }

  /**
   * From the Search UI, point-radius uses dwithin. We want dwithin to fallback to intersects as a
   * last resort. We buffer the geometry (the point) by the radius and do an intersects.
   */
  @Test
  public void testDwithinAsIntersects() throws JAXBException, SAXException, IOException {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.INTERSECTS.getValue());
    /**
     * Made distance a large value so if the original WKT and the buffered WKT are plotted at:
     * http://openlayers.org/dev/examples/vector-formats.html one can easily see the buffer.
     */
    double distance = 200000.0;
    FilterType filter = delegate.dwithin(Metacard.ANY_GEO, POINT, distance);

    XMLUnit.setNormalizeWhitespace(true);
    XMLUnit.setIgnoreWhitespace(true);
    assertXMLEqual(getDWithinAsIntersectsXml(), marshal(filter));
  }

  @Test
  public void testDwithinUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);

    FilterType filter = delegate.dwithin(Metacard.ANY_GEO, POLYGON, DISTANCE);
    assertThat(filter, nullValue());
  }

  @Test
  public void testIntersects() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.INTERSECTS.getValue());

    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

    assertBinarySpatialOpFilter(filter);
  }

  @Test
  public void testIntersectsAsBoundingBox() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.BBOX.getValue());

    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);
    assertThat(filter.getSpatialOps().getValue(), is(instanceOf(BBOXType.class)));
    assertThat(filter.isSetLogicOps(), is(false));
  }

  @Test
  public void testIntersectsAsNotDisjoint() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.DISJOINT.getValue());

    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

    assertThat(filter.isSetLogicOps(), is(true));
    assertThat(filter.getLogicOps().getValue(), is(instanceOf(UnaryLogicOpType.class)));
    UnaryLogicOpType type = (UnaryLogicOpType) filter.getLogicOps().getValue();
    assertThat(type.isSetSpatialOps(), is(true));
    assertThat(type.getSpatialOps().getValue(), is(instanceOf(BinarySpatialOpType.class)));
  }

  @Test
  public void testIntersectsUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);

    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);
    assertThat(filter, nullValue());
  }

  @Test
  public void testOverlapsFilter() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.OVERLAPS.getValue());
    FilterType filter = delegate.overlaps(Metacard.ANY_GEO, POLYGON);

    assertBinarySpatialOpFilter(filter);
  }

  @Test
  public void testOverlapsUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);
    FilterType filter = delegate.overlaps(Metacard.ANY_GEO, POLYGON);

    assertThat(filter, nullValue());
  }

  @Test
  public void testTouchesFilter() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.TOUCHES.getValue());

    FilterType filter = delegate.touches(Metacard.ANY_GEO, POLYGON);

    assertBinarySpatialOpFilter(filter);
  }

  @Test
  public void testTouchesUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);
    FilterType filter = delegate.touches(Metacard.ANY_GEO, POLYGON);

    assertThat(filter, nullValue());
  }

  @Test
  public void testWithinFilter() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.WITHIN.getValue());

    FilterType filter = delegate.within(Metacard.ANY_GEO, POLYGON);

    assertBinarySpatialOpFilter(filter);
  }

  @Test
  public void testWithinUnsupported() {
    WfsFilterDelegate delegate = setupFilterDelegate(NO_OP);
    FilterType filter = delegate.within(Metacard.ANY_GEO, POLYGON);

    assertThat(filter, nullValue());
  }

  @Test
  public void testIntersectsMultipleProperties() {

    whenGeom(MOCK_GEOM, MOCK_GEOM2, true, true);

    List<String> supportedGeo = Collections.singletonList(SPATIAL_OPERATORS.INTERSECTS.getValue());
    WfsFilterDelegate delegate = new WfsFilterDelegate(featureMetacardType, supportedGeo);

    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);
    assertThat(filter, notNullValue());
    assertThat(filter.isSetLogicOps(), is(true));
    assertThat(filter.getLogicOps(), notNullValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSingleGmlPropertyBlacklisted() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.CONTAINS.getValue());
    when(featureMetacardType.getAttributeDescriptor(MOCK_GEOM))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_GEOM, MOCK_GEOM, false, false, false, false, BasicTypes.STRING_TYPE));

    delegate.contains(MOCK_GEOM, POLYGON);
  }

  @Test
  public void testAllGmlPropertiesBlacklisted() {
    whenGeom(MOCK_GEOM, MOCK_GEOM2, false, false);

    List<String> supportedGeo = Collections.singletonList(SPATIAL_OPERATORS.INTERSECTS.getValue());
    WfsFilterDelegate delegate = new WfsFilterDelegate(featureMetacardType, supportedGeo);

    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);
    assertThat(filter, nullValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadPolygonWkt() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.INTERSECTS.getValue());
    delegate.intersects(Metacard.ANY_GEO, "junk");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadPointWkt() {
    WfsFilterDelegate delegate = setupFilterDelegate(SPATIAL_OPERATORS.DWITHIN.getValue());
    delegate.dwithin(Metacard.ANY_GEO, "junk", DISTANCE);
  }

  @Test
  public void testNonEpsg4326Srs() {
    List<String> gmlProps = new ArrayList<>();
    gmlProps.add(MOCK_GEOM);
    when(featureMetacardType.getGmlProperties()).thenReturn(gmlProps);

    WfsFilterDelegate delegate =
        new WfsFilterDelegate(
            featureMetacardType,
            Collections.singletonList(SPATIAL_OPERATORS.INTERSECTS.getValue()));
    FilterType filter = delegate.intersects(Metacard.ANY_GEO, POLYGON);

    assertThat(filter, nullValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGeoFilterNullMetacardType() {
    List<String> supportedGeo = Collections.singletonList(SPATIAL_OPERATORS.BEYOND.getValue());

    WfsFilterDelegate delegate = new WfsFilterDelegate(null, supportedGeo);

    delegate.beyond(Metacard.ANY_GEO, POLYGON, DISTANCE);
  }

  private JAXBElement<FilterType> getFilterTypeJaxbElement(FilterType filterType) {
    return new JAXBElement<>(
        new QName("http://www.opengis.net/ogc", FILTER_QNAME_LOCAL_PART),
        FilterType.class,
        filterType);
  }

  private static JAXBContext initJaxbContext() {

    JAXBContext jaxbContext = null;

    try {
      jaxbContext = JAXBContext.newInstance("net.opengis.filter.v_1_1_0");
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
    return new DateTime(inputDate);
  }

  private String getPropertyEqualToXmlDate() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<Filter xmlns:ns2=\"http://www.opengis.net/gml\" xmlns=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
        + "<PropertyIsEqualTo matchCase=\"true\">"
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
        + "<PropertyIsNotEqualTo matchCase=\"true\">"
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
        + "<PropertyIsGreaterThan matchCase=\"true\">"
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
        + "<PropertyIsGreaterThanOrEqualTo matchCase=\"true\">"
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
        + "<PropertyIsLessThan matchCase=\"true\">"
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
        + "<PropertyIsLessThanOrEqualTo matchCase=\"true\">"
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
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Filter xmlns=\"http://www.opengis.net/ogc\" xmlns:ns5=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns2=\"http://www.opengis.net/gml\" xmlns:ns4=\"http://www.w3.org/2001/SMIL20/\" xmlns:ns3=\"http://www.w3.org/1999/xlink\"><Intersects><PropertyName>ground_geom</PropertyName><ns2:Polygon><ns2:exterior><ns2:LinearRing><ns2:coordinates decimal=\".\" cs=\",\" ts=\" \">-10.0,31.79864073552333 -10.350897400284572,31.76408035813492 -10.688310010261736,31.66172736189105 -10.999271252553244,31.495515115037147 -11.271831061006903,31.271831061006903 -11.495515115037145,30.999271252553243 -11.66172736189105,30.688310010261738 -11.764080358134919,30.350897400284573 -11.798640735523328,30.0 -11.764080358134919,29.649102599715427 -11.66172736189105,29.311689989738262 -11.495515115037145,29.000728747446757 -11.271831061006905,28.728168938993097 -10.999271252553244,28.504484884962853 -10.688310010261736,28.33827263810895 -10.350897400284572,28.23591964186508 -9.999999999999998,28.20135926447667 -9.649102599715427,28.23591964186508 -9.311689989738262,28.338272638108954 -9.000728747446754,28.504484884962856 -8.728168938993093,28.728168938993097 -8.504484884962853,29.000728747446757 -8.33827263810895,29.311689989738266 -8.23591964186508,29.649102599715434 -8.201359264476672,30.000000000000004 -8.235919641865081,30.350897400284577 -8.338272638108954,30.68831001026174 -8.504484884962856,30.99927125255325 -8.7281689389931,31.271831061006907 -9.000728747446761,31.49551511503715 -9.31168998973827,31.661727361891053 -9.649102599715436,31.76408035813492 -10.0,31.79864073552333 </ns2:coordinates></ns2:LinearRing></ns2:exterior></ns2:Polygon></Intersects></Filter>";
  }

  private String marshal(FilterType filter) throws JAXBException {
    Writer writer = new StringWriter();
    Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
    marshaller.marshal(getFilterTypeJaxbElement(filter), writer);
    return writer.toString();
  }

  private void whenGeom(String geom1, String geom2, boolean geom1Indexed, boolean geom2Indexed) {
    List<String> gmlProps = new ArrayList<>();
    gmlProps.add(geom1);
    gmlProps.add(geom2);
    when(featureMetacardType.getGmlProperties()).thenReturn(gmlProps);
    when(featureMetacardType.getAttributeDescriptor(geom1))
        .thenReturn(
            new FeatureAttributeDescriptor(
                geom1, geom1, geom1Indexed, false, false, false, BasicTypes.STRING_TYPE));

    when(featureMetacardType.getAttributeDescriptor(geom2))
        .thenReturn(
            new FeatureAttributeDescriptor(
                geom2, geom2, geom2Indexed, false, false, false, BasicTypes.STRING_TYPE));
  }

  private WfsFilterDelegate createDelegate() {
    return new WfsFilterDelegate(featureMetacardType, SUPPORTED_GEO);
  }

  private WfsFilterDelegate createIntegerDelegate() {
    whenPropertiesIntegerType();
    return createDelegate();
  }

  private WfsFilterDelegate createLongDelegate() {
    whenPropertiesLongType();
    return createDelegate();
  }

  private WfsFilterDelegate createBooleanDelegate() {
    whenPropertiesBooleanType();
    return createDelegate();
  }

  private WfsFilterDelegate createTextualDelegate() {
    whenTextualStringType();
    return createDelegate();
  }

  private void assertBinarySpatialOpFilter(FilterType filter) {
    assertThat(filter.getSpatialOps().getValue(), is(instanceOf(BinarySpatialOpType.class)));
    assertThat(filter.isSetLogicOps(), is(false));
  }

  private void assertDistanceBufferFilter(FilterType filter) {
    assertThat(filter.isSetLogicOps(), is(false));
    assertThat(filter.isSetSpatialOps(), is(true));
    assertThat(filter.getSpatialOps().getValue(), is(instanceOf(DistanceBufferType.class)));
  }

  private WfsFilterDelegate setupFilterDelegate(String spatialOpType) {
    List<String> gmlProps = new ArrayList<>();
    gmlProps.add(MOCK_GEOM);

    when(featureMetacardType.getGmlProperties()).thenReturn(gmlProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_GEOM))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_GEOM, MOCK_GEOM, true, false, false, false, BasicTypes.STRING_TYPE));

    List<String> supportedGeo = Collections.singletonList(spatialOpType);
    return new WfsFilterDelegate(featureMetacardType, supportedGeo);
  }

  private void whenTextualStringType() {
    mockProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getTextualProperties()).thenReturn(mockProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, BasicTypes.STRING_TYPE));
  }

  private void whenProperties(AttributeType<?> type) {
    mockProps.add(MOCK_PROPERTY);
    when(featureMetacardType.getProperties()).thenReturn(mockProps);
    when(featureMetacardType.getAttributeDescriptor(MOCK_PROPERTY))
        .thenReturn(
            new FeatureAttributeDescriptor(
                MOCK_PROPERTY, MOCK_PROPERTY, true, true, true, true, type));
  }

  private void whenPropertiesStringType() {
    whenProperties(BasicTypes.STRING_TYPE);
  }

  private void whenPropertiesDateType() {
    whenProperties(BasicTypes.DATE_TYPE);
  }

  private void whenPropertiesIntegerType() {
    whenProperties(BasicTypes.INTEGER_TYPE);
  }

  private void whenPropertiesShortType() {
    whenProperties(BasicTypes.SHORT_TYPE);
  }

  private void whenPropertiesLongType() {
    whenProperties(BasicTypes.LONG_TYPE);
  }

  private void whenPropertiesBooleanType() {
    whenProperties(BasicTypes.BOOLEAN_TYPE);
  }
}
