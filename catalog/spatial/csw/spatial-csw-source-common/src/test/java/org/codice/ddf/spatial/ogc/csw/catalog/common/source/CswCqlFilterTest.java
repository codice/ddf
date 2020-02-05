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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.source.UnsupportedQueryException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CswCqlFilterTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(CswCqlFilterTest.class);

  private static final String DEFAULT_PROPERTY_NAME = "title";

  private static final String SRID = "SRID=4326;";

  private static final String QUOTE = "\"";

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

  private static final String OVERLAPS = "OVERLAPS(";

  private static final String TOUCHES = "TOUCHES(";

  private static final String IS_NULL = "IS NULL";

  private static final String ANY_TEXT = "AnyText";

  private static final String ONE = "'1'";

  private static final String ONE_DOT_ZERO = "'1.0'";

  private static final String TRUE = "'true'";

  private static final String BAR = "'*bar*'";

  private static final String SINGLE_QUOTE = "'";

  private static final String REPLACE_START_DATE = "REPLACE_START_DATE";

  private static final String REPLACE_END_DATE = "REPLACE_END_DATE";

  private static final String REPLACE_TEMPORAL_PROPERTY = "REPLACE_TEMPORAL_PROPERTY";

  private static final String CSW_RECORD_ID = "cswRecord.1234";

  private final CswFilterDelegate cswFilterDelegate =
      initDefaultCswFilterDelegate(
          initCswSourceConfiguration(CswAxisOrder.LON_LAT, CswConstants.CSW_TYPE));

  private final Date date = getDate();

  private final String propertyName = DEFAULT_PROPERTY_NAME;

  private final String propertyNameAnyText = Metacard.ANY_TEXT;

  private final String propertyNameModified = Core.MODIFIED;

  private final String propertyNameEffective = Metacard.EFFECTIVE;

  private final String propertyNameCreated = Core.CREATED;

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

  private final boolean isCaseSensitive = true;

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

  private final String multiPolygonWkt =
      "MULTIPOLYGON (((40 40, 20 45, 45 30, 40 40)), ((20 35, 45 20, 30 5, 10 10, 10 30, 20 35), (30 20, 20 25, 20 15, 30 20)))";

  private final String multiPointWkt = "MULTIPOINT ((10 40), (40 30), (20 20), (30 10))";

  private final String multiLineStringWkt =
      "MULTILINESTRING ((10 10, 20 20, 10 40), (40 40, 30 30, 40 20, 30 10))";

  private final double distance = 123.456;

  private final String during =
      REPLACE_TEMPORAL_PROPERTY
          + SPACE
          + BETWEEN
          + SPACE
          + SINGLE_QUOTE
          + REPLACE_START_DATE
          + SINGLE_QUOTE
          + SPACE
          + AND
          + SPACE
          + SINGLE_QUOTE
          + REPLACE_END_DATE
          + SINGLE_QUOTE;

  private final String propertyIsEqualTo = DEFAULT_PROPERTY_NAME + SPACE + EQUALS + SPACE + ONE;

  private final String propertyIsEqualToWithDecimal =
      DEFAULT_PROPERTY_NAME + SPACE + EQUALS + SPACE + ONE_DOT_ZERO;

  private final String propertyIsEqualToAnyText = ANY_TEXT + SPACE + EQUALS + SPACE + ONE;

  private final String propertyIsEqualToContentType = "type" + SPACE + EQUALS + SPACE + "'myType'";

  private final String propertyIsEqualToWithDate = getPropertyIsEqualToWithDate(getDate());

  private final String propertyIsEqualToWithBoolean =
      DEFAULT_PROPERTY_NAME + SPACE + EQUALS + SPACE + TRUE;

  private final String propertyIsNotEqualTo =
      DEFAULT_PROPERTY_NAME + SPACE + LESS_THAN + GREATER_THAN + SPACE + ONE;

  private final String propertyIsNotEqualToWithDecimal =
      DEFAULT_PROPERTY_NAME + SPACE + LESS_THAN + GREATER_THAN + SPACE + ONE_DOT_ZERO;

  private final String propertyIsNotEqualToAnyText =
      CswConstants.ANY_TEXT + SPACE + LESS_THAN + GREATER_THAN + SPACE + ONE;

  private final String propertyIsNotEqualToWithBoolean =
      DEFAULT_PROPERTY_NAME + SPACE + LESS_THAN + GREATER_THAN + SPACE + TRUE;

  private final String propertyIsGreaterThan =
      DEFAULT_PROPERTY_NAME + SPACE + GREATER_THAN + SPACE + ONE;

  private final String propertyIsGreaterThanWithDecimal =
      DEFAULT_PROPERTY_NAME + SPACE + GREATER_THAN + SPACE + ONE_DOT_ZERO;

  private final String propertyIsGreaterThanAnyText =
      CswConstants.ANY_TEXT + SPACE + GREATER_THAN + SPACE + ONE;

  private final String propertyIsGreaterThanOrEqualTo =
      DEFAULT_PROPERTY_NAME + SPACE + GREATER_THAN + EQUALS + SPACE + ONE;

  private final String propertyIsGreaterThanOrEqualToWithDecimal =
      DEFAULT_PROPERTY_NAME + SPACE + GREATER_THAN + EQUALS + SPACE + ONE_DOT_ZERO;

  private final String propertyIsGreaterThanOrEqualToAnyText =
      CswConstants.ANY_TEXT + SPACE + GREATER_THAN + EQUALS + SPACE + ONE;

  private final String propertyIsLessThan = DEFAULT_PROPERTY_NAME + SPACE + LESS_THAN + SPACE + ONE;

  private final String propertyIsLessThanWithDecimal =
      DEFAULT_PROPERTY_NAME + SPACE + LESS_THAN + SPACE + ONE_DOT_ZERO;

  private final String propertyIsLessThanAnyText =
      CswConstants.ANY_TEXT + SPACE + LESS_THAN + SPACE + ONE;

  private final String propertyIsLessThanOrEqualTo =
      DEFAULT_PROPERTY_NAME + SPACE + LESS_THAN + EQUALS + SPACE + ONE;

  private final String propertyIsLessThanOrEqualToWithDecimal =
      DEFAULT_PROPERTY_NAME + SPACE + LESS_THAN + EQUALS + SPACE + ONE_DOT_ZERO;

  private final String propertyIsLessThanOrEqualToAnyText =
      CswConstants.ANY_TEXT + SPACE + LESS_THAN + EQUALS + SPACE + ONE;

  private final String propertyIsBetween =
      DEFAULT_PROPERTY_NAME + SPACE + BETWEEN + SPACE + "'5'" + SPACE + AND + SPACE + "'15'";

  private final String propertyIsBetweenWithDecimal =
      DEFAULT_PROPERTY_NAME + SPACE + BETWEEN + SPACE + "'5.0'" + SPACE + AND + SPACE + "'15.0'";

  private final String propertyIsNull = DEFAULT_PROPERTY_NAME + SPACE + IS_NULL;

  private final String propertyIsLike = DEFAULT_PROPERTY_NAME + SPACE + LIKE + SPACE + BAR;

  private final String propertyIsLikeAnyText = CswConstants.ANY_TEXT + SPACE + LIKE + SPACE + BAR;

  private final String orComparisonOps =
      propertyIsEqualToWithBoolean + SPACE + OR + SPACE + propertyIsLike;

  private final String intersectsPolygonPropertyOwsBoundingBox =
      INTERSECTS
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + polygonWkt
          + CLOSE_PARAN;

  private final String intersectsPolygonPropertyDctSpatial =
      INTERSECTS
          + QUOTE
          + CswConstants.SPATIAL_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + polygonWkt
          + CLOSE_PARAN;

  private final String intersectsPointPropertyOwsBoundingBox =
      INTERSECTS
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + pointWkt
          + CLOSE_PARAN;

  private final String intersectsLineStringPropertyOwsBoundingBox =
      INTERSECTS
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + lineStringWkt
          + CLOSE_PARAN;

  private final String intersectsMultiPolygonPropertyOwsBoundingBox =
      INTERSECTS
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + multiPolygonWkt
          + CLOSE_PARAN;

  private final String intersectsMultiPointPropertyOwsBoundingBox =
      INTERSECTS
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + multiPointWkt
          + CLOSE_PARAN;

  private final String intersectsMultiLineStringPropertyOwsBoundingBox =
      INTERSECTS
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + multiLineStringWkt
          + CLOSE_PARAN;

  private final String crossesPolygonPropertyOwsBoundingBox =
      CROSSES
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + polygonWkt
          + CLOSE_PARAN;

  private final String withinPolygonPropertyOwsBoundingBox =
      WITHIN
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + polygonWkt
          + CLOSE_PARAN;

  private final String containsPolygonXmlPropertyOwsBoundingBox =
      CONTAINS
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + polygonWkt
          + CLOSE_PARAN;

  private final String disjointPolygonXmlPropertyOwsBoundingBox =
      DISJOINT
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + polygonWkt
          + CLOSE_PARAN;

  private final String overlapsPolygonPropertyOwsBoundingBox =
      OVERLAPS
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + polygonWkt
          + CLOSE_PARAN;

  private final String touchesPolygonPropertyOwsBoundingBox =
      TOUCHES
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + polygonWkt
          + CLOSE_PARAN;

  private final String beyondPointPropertyOwsBoundingBox =
      BEYOND
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + pointWkt
          + COMMA
          + SPACE
          + distance
          + COMMA
          + SPACE
          + CswConstants.METERS
          + CLOSE_PARAN;

  private final String dwithinPolygonPropertyOwsBoundingBox =
      DWITHIN
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + polygonWkt
          + COMMA
          + SPACE
          + distance
          + COMMA
          + SPACE
          + CswConstants.METERS
          + CLOSE_PARAN;

  private final String orLogicOps =
      NOT
          + SPACE
          + OPEN_PARAN
          + propertyIsLike
          + CLOSE_PARAN
          + SPACE
          + OR
          + SPACE
          + propertyIsEqualToWithBoolean;

  private final String orSpatialOps =
      DWITHIN
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + pointWkt
          + COMMA
          + SPACE
          + distance
          + COMMA
          + SPACE
          + CswConstants.METERS
          + CLOSE_PARAN
          + SPACE
          + OR
          + SPACE
          + propertyIsLike;

  private final String andComparisonOps =
      propertyIsEqualToWithBoolean + SPACE + AND + SPACE + propertyIsLike;

  private final String andLogicOps =
      NOT
          + SPACE
          + OPEN_PARAN
          + propertyIsLike
          + CLOSE_PARAN
          + SPACE
          + AND
          + SPACE
          + propertyIsEqualToWithBoolean;

  private final String andSpatialOps =
      DWITHIN
          + QUOTE
          + CswConstants.BBOX_PROP
          + QUOTE
          + COMMA
          + SPACE
          + SRID
          + pointWkt
          + COMMA
          + SPACE
          + distance
          + COMMA
          + SPACE
          + CswConstants.METERS
          + CLOSE_PARAN
          + SPACE
          + OR
          + SPACE
          + propertyIsLike;

  private final String featureIdCql =
      CswConstants.CSW_IDENTIFIER
          + SPACE
          + EQUALS
          + SPACE
          + SINGLE_QUOTE
          + CSW_RECORD_ID
          + SINGLE_QUOTE;

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
    List<SpatialOperatorType> spatialOperatorList = new ArrayList<>();
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
    List<ComparisonOperatorType> comparisonOpsList = new ArrayList<>();
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
    List<QName> mockGeometryOperands = new ArrayList<>();
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
    when(mockSpatialCapabilitiesType.getGeometryOperands()).thenReturn(mockGeometryOperandsType);

    return mockSpatialCapabilitiesType;
  }

  private static Operation getOperation() {
    List<DomainType> getRecordsParameters = new ArrayList<>(6);
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
    List<Operation> operations = new ArrayList<>(1);
    operations.add(getRecords);

    return getRecords;
  }

  /**
   * Property is equal to tests
   *
   * @throws UnsupportedQueryException
   */
  @Test
  public void testPropertyIsEqualToStringLiteral() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsEqualTo(propertyName, stringLiteral, isCaseSensitive);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsEqualTo));
  }

  /**
   * Property is equal to with alternate id property
   *
   * @throws UnsupportedQueryException
   */
  @Test
  public void testPropertyIsEqualToStringLiteralAlternateIdMapping()
      throws UnsupportedQueryException {
    String replacedIdentifierProperty = propertyName;
    CswSourceConfiguration cswSourceConfiguration =
        initCswSourceConfiguration(
            CswAxisOrder.LAT_LON,
            CswConstants.CSW_TYPE,
            effectiveDateMapping,
            createdDateMapping,
            modifiedDateMapping,
            replacedIdentifierProperty);

    CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
    FilterType filterType =
        cswFilterDelegate.propertyIsEqualTo(propertyName, stringLiteral, isCaseSensitive);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsEqualTo));
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
    assertThat(cqlText, is(propertyIsEqualToWithDate));
  }

  @Test
  public void testPropertyIsEqualToStringLiteralAnyText() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsEqualTo(propertyNameAnyText, stringLiteral, isCaseSensitive);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsEqualToAnyText));
  }

  @Test
  public void testDuring() throws UnsupportedQueryException {

    DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
    DateTime endDate = new DateTime(2013, 12, 31, 0, 0, 0, 0);
    CswSourceConfiguration cswSourceConfiguration =
        initCswSourceConfiguration(
            CswAxisOrder.LAT_LON,
            CswConstants.CSW_TYPE,
            effectiveDateMapping,
            createdDateMapping,
            modifiedDateMapping,
            CswConstants.CSW_IDENTIFIER);

    CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
    FilterType filterType =
        cswFilterDelegate.during(
            propertyNameModified,
            startDate.toCalendar(null).getTime(),
            endDate.toCalendar(null).getTime());
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);

    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    String startDateStr = fmt.print(startDate);
    String endDateStr = fmt.print(endDate);
    String testResponse =
        during
            .replace(REPLACE_START_DATE, startDateStr)
            .replace(REPLACE_END_DATE, endDateStr)
            .replace(REPLACE_TEMPORAL_PROPERTY, modifiedDateMapping);

    assertThat(cqlText, is(testResponse));
  }

  @Test
  public void testDuringAlteredEffectiveDateMapping() throws UnsupportedQueryException {

    DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
    DateTime endDate = new DateTime(2013, 12, 31, 0, 0, 0, 0);

    String replacedTemporalProperty = "myEffectiveDate";
    CswSourceConfiguration cswSourceConfiguration =
        initCswSourceConfiguration(
            CswAxisOrder.LAT_LON,
            CswConstants.CSW_TYPE,
            replacedTemporalProperty,
            createdDateMapping,
            modifiedDateMapping,
            CswConstants.CSW_IDENTIFIER);

    CswFilterDelegate cswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
    FilterType filterType =
        cswFilterDelegate.during(
            propertyNameEffective,
            startDate.toCalendar(null).getTime(),
            endDate.toCalendar(null).getTime());
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    String startDateStr = fmt.print(startDate);
    String endDateStr = fmt.print(endDate);
    String testResponse =
        during
            .replace(REPLACE_START_DATE, startDateStr)
            .replace(REPLACE_END_DATE, endDateStr)
            .replace(REPLACE_TEMPORAL_PROPERTY, replacedTemporalProperty);
    assertThat(cqlText, is(testResponse));
  }

  @Test
  public void testDuringAlteredCreatedDateMapping() throws UnsupportedQueryException {

    DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
    DateTime endDate = new DateTime(2013, 12, 31, 0, 0, 0, 0);

    String replacedTemporalProperty = "myCreatedDate";
    CswSourceConfiguration cswSourceConfiguration =
        initCswSourceConfiguration(
            CswAxisOrder.LAT_LON,
            CswConstants.CSW_TYPE,
            effectiveDateMapping,
            replacedTemporalProperty,
            modifiedDateMapping,
            CswConstants.CSW_IDENTIFIER);

    CswFilterDelegate localCswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
    FilterType filterType =
        localCswFilterDelegate.during(
            propertyNameCreated,
            startDate.toCalendar(null).getTime(),
            endDate.toCalendar(null).getTime());
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);

    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    String startDateStr = fmt.print(startDate);
    String endDateStr = fmt.print(endDate);
    String testResponse =
        during
            .replace(REPLACE_START_DATE, startDateStr)
            .replace(REPLACE_END_DATE, endDateStr)
            .replace(REPLACE_TEMPORAL_PROPERTY, replacedTemporalProperty);
    assertThat(cqlText, is(testResponse));
  }

  @Test
  public void testDuringAlteredModifiedDateMapping() throws UnsupportedQueryException {

    DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
    DateTime endDate = new DateTime(2013, 12, 31, 0, 0, 0, 0);

    String replacedTemporalProperty = "myModifiedDate";
    CswSourceConfiguration cswSourceConfiguration =
        initCswSourceConfiguration(
            CswAxisOrder.LAT_LON,
            CswConstants.CSW_TYPE,
            effectiveDateMapping,
            createdDateMapping,
            replacedTemporalProperty,
            CswConstants.CSW_IDENTIFIER);

    CswFilterDelegate localCswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
    FilterType filterType =
        localCswFilterDelegate.during(
            propertyNameModified,
            startDate.toCalendar(null).getTime(),
            endDate.toCalendar(null).getTime());
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);

    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    String startDateStr = fmt.print(startDate);
    String endDateStr = fmt.print(endDate);
    String testResponse =
        during
            .replace(REPLACE_START_DATE, startDateStr)
            .replace(REPLACE_END_DATE, endDateStr)
            .replace(REPLACE_TEMPORAL_PROPERTY, replacedTemporalProperty);
    assertThat(cqlText, is(testResponse));
  }

  @Test
  public void testRelative() throws UnsupportedQueryException {
    long duration = 92000000000L;
    CswSourceConfiguration cswSourceConfiguration =
        initCswSourceConfiguration(
            CswAxisOrder.LAT_LON,
            CswConstants.CSW_TYPE,
            effectiveDateMapping,
            createdDateMapping,
            modifiedDateMapping,
            CswConstants.CSW_IDENTIFIER);
    CswFilterDelegate localCswFilterDelegate = initDefaultCswFilterDelegate(cswSourceConfiguration);
    FilterType filterType = localCswFilterDelegate.relative(propertyNameModified, duration);

    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);

    String durationCompare =
        during
            .replace(REPLACE_START_DATE, "")
            .replace(REPLACE_END_DATE, "")
            .replace(REPLACE_TEMPORAL_PROPERTY, modifiedDateMapping);
    String pattern = "(?i)(')(.+?)(')";
    String compareXml = cqlText.replaceAll(pattern, "''");
    assertThat(durationCompare, is(compareXml));
  }

  @Test
  public void testPropertyIsEqualToStringLiteralType() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsEqualTo(
            Metacard.CONTENT_TYPE, contentTypeLiteral, isCaseSensitive);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsEqualToContentType));
  }

  @Test
  public void testPropertyIsEqualToIntLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, intLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsEqualTo));
  }

  @Test
  public void testPropertyIsEqualToShortLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, shortLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsEqualTo));
  }

  @Test
  public void testPropertyIsEqualToLongLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, longLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsEqualTo));
  }

  @Test
  public void testPropertyIsEqualToFloatLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, floatLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsEqualToWithDecimal));
  }

  @Test
  public void testPropertyIsEqualToDoubleLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, doubleLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsEqualToWithDecimal));
  }

  @Test
  public void testPropertyIsEqualToBooleanLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsEqualTo(propertyName, booleanLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsEqualToWithBoolean));
  }

  /** Property is not equal to tests */
  @Test
  public void testPropertyIsNotEqualToStringLiteral() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsNotEqualTo(propertyName, stringLiteral, isCaseSensitive);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsNotEqualTo));
  }

  @Test
  public void testPropertyIsNotEqualToStringLiteralAnyText() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsNotEqualTo(propertyNameAnyText, stringLiteral, isCaseSensitive);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsNotEqualToAnyText));
  }

  @Test
  public void testPropertyIsNotEqualToIntLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, intLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsNotEqualTo));
  }

  @Test
  public void testPropertyIsNotEqualToShortLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, shortLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsNotEqualTo));
  }

  @Test
  public void testPropertyIsNotEqualToLongLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, longLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsNotEqualTo));
  }

  @Test
  public void testPropertyIsNotEqualToFloatLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, floatLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsNotEqualToWithDecimal));
  }

  @Test
  public void testPropertyIsNotEqualToDoubleLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, doubleLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsNotEqualToWithDecimal));
  }

  @Test
  public void testPropertyIsNotEqualToBooleanLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsNotEqualTo(propertyName, booleanLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsNotEqualToWithBoolean));
  }

  /** Property is greater than tests */
  @Test
  public void testPropertyIsGreaterThanStringLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName, stringLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsGreaterThan));
  }

  @Test
  public void testPropertyIsGreaterThanStringLiteralAnyText() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsGreaterThan(propertyNameAnyText, stringLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsGreaterThanAnyText));
  }

  @Test
  public void testPropertyIsGreaterThanIntLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName, intLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsGreaterThan));
  }

  @Test
  public void testPropertyIsGreaterThanShortLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName, shortLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsGreaterThan));
  }

  @Test
  public void testPropertyIsGreaterThanLongLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName, longLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsGreaterThan));
  }

  @Test
  public void testPropertyIsGreaterThanFloatLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName, floatLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsGreaterThanWithDecimal));
  }

  @Test
  public void testPropertyIsGreaterThanDoubleLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsGreaterThan(propertyName, doubleLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsGreaterThanWithDecimal));
  }

  /** Property is greater than or equal to tests */
  @Test
  public void testPropertyIsGreaterThanOrEqualToStringLiteral() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName, stringLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsGreaterThanOrEqualTo));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToStringLiteralAnyText()
      throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyNameAnyText, stringLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsGreaterThanOrEqualToAnyText));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToIntLiteral() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName, intLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsGreaterThanOrEqualTo));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToShortLiteral() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName, shortLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsGreaterThanOrEqualTo));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToLongLiteral() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName, longLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsGreaterThanOrEqualTo));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToFloatLiteral() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName, floatLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsGreaterThanOrEqualToWithDecimal));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToDoubleLiteral() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsGreaterThanOrEqualTo(propertyName, doubleLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsGreaterThanOrEqualToWithDecimal));
  }

  /** Property is less than tests */
  @Test
  public void testPropertyIsLessThanStringLiteral() throws UnsupportedQueryException {

    FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, stringLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLessThan));
  }

  @Test
  public void testPropertyIsLessThanStringLiteralAnyText() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsLessThan(propertyNameAnyText, stringLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLessThanAnyText));
  }

  @Test
  public void testPropertyIsLessThanIntLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, intLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLessThan));
  }

  @Test
  public void testPropertyIsLessThanShortLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, shortLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLessThan));
  }

  @Test
  public void testPropertyIsLessThanLongLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, longLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLessThan));
  }

  @Test
  public void testPropertyIsLessThanFloatLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, floatLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLessThanWithDecimal));
  }

  @Test
  public void testPropertyIsLessThanDoubleLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsLessThan(propertyName, doubleLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLessThanWithDecimal));
  }

  /** Property is less than or equal to tests */
  @Test
  public void testPropertyIsLessThanOrEqualToStringLiteral() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName, stringLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLessThanOrEqualTo));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToStringLiteralAnyText()
      throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyNameAnyText, stringLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLessThanOrEqualToAnyText));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToIntLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName, intLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLessThanOrEqualTo));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToShortLiteral() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName, intLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLessThanOrEqualTo));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToLongLiteral() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName, shortLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLessThanOrEqualTo));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToFloatLiteral() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName, floatLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLessThanOrEqualToWithDecimal));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToDoubleLiteral() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsLessThanOrEqualTo(propertyName, doubleLiteral);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLessThanOrEqualToWithDecimal));
  }

  /** Property is between tests */
  @Test
  public void testPropertyBetweenStringLiterals() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsBetween(propertyName, stringLowerBoundary, stringUpperBoundary);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsBetween));
  }

  @Test
  public void testPropertyBetweenIntLiterals() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsBetween(propertyName, intLowerBoundary, intUpperBoundary);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsBetween));
  }

  @Test
  public void testPropertyBetweenShortLiterals() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsBetween(propertyName, shortLowerBoundary, shortUpperBoundary);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsBetween));
  }

  @Test
  public void testPropertyBetweenLongLiterals() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsBetween(propertyName, longLowerBoundary, longUpperBoundary);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsBetween));
  }

  @Test
  public void testPropertyBetweenFloatLiterals() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsBetween(propertyName, floatLowerBoundary, floatUpperBoundary);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsBetweenWithDecimal));
  }

  @Test
  public void testPropertyBetweenDoubleLiterals() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsBetween(propertyName, doubleLowerBoundary, doubleUpperBoundary);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsBetweenWithDecimal));
  }

  @Test
  public void testPropertyNull() throws UnsupportedQueryException {
    FilterType filterType = cswFilterDelegate.propertyIsNull(propertyName);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsNull));
  }

  @Test
  public void testPropertyLike() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsLike(propertyName, likeLiteral, isCaseSensitive);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLike));
  }

  @Test
  public void testPropertyLikeAnyText() throws UnsupportedQueryException {
    FilterType filterType =
        cswFilterDelegate.propertyIsLike(propertyNameAnyText, likeLiteral, isCaseSensitive);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(propertyIsLikeAnyText));
  }

  @Test
  public void testComparisonOpsOr() throws UnsupportedQueryException {
    FilterType propertyIsLikeFilter =
        cswFilterDelegate.propertyIsLike(propertyName, likeLiteral, isCaseSensitive);
    FilterType propertyIsEqualFilter =
        cswFilterDelegate.propertyIsEqualTo(propertyName, booleanLiteral);

    List<FilterType> filters = new ArrayList<>();
    filters.add(propertyIsEqualFilter);
    filters.add(propertyIsLikeFilter);
    FilterType filter = cswFilterDelegate.or(filters);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
    assertThat(cqlText, is(orComparisonOps));
  }

  @Test
  public void testLogicOpsFiltersOr() throws UnsupportedQueryException {
    FilterType propertyIsLikeFilter =
        cswFilterDelegate.propertyIsLike(propertyName, likeLiteral, isCaseSensitive);
    FilterType notFilter = cswFilterDelegate.not(propertyIsLikeFilter);
    FilterType propertyIsEqualFilter =
        cswFilterDelegate.propertyIsEqualTo(propertyName, booleanLiteral);

    List<FilterType> filters = new ArrayList<>();
    filters.add(notFilter);
    filters.add(propertyIsEqualFilter);
    FilterType filter = cswFilterDelegate.or(filters);

    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
    assertThat(cqlText, is(orLogicOps));
  }

  @Test
  public void testSpatialOpsOr() throws UnsupportedQueryException {
    FilterType spatialFilter =
        cswFilterDelegate.dwithin(CswConstants.BBOX_PROP, pointWkt, distance);
    FilterType propertyIsLikeFilter =
        cswFilterDelegate.propertyIsLike(propertyName, likeLiteral, isCaseSensitive);

    List<FilterType> filters = new ArrayList<>();
    filters.add(spatialFilter);
    filters.add(propertyIsLikeFilter);

    FilterType filter = cswFilterDelegate.or(filters);

    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
    assertThat(cqlText, is(orSpatialOps));
  }

  @Test
  public void testFeatureId() throws UnsupportedQueryException {
    FilterType filter =
        cswFilterDelegate.propertyIsEqualTo(Core.ID, String.valueOf(CSW_RECORD_ID), false);

    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
    assertThat(cqlText, is(featureIdCql));
  }

  @Test
  public void testIdQuoting() throws UnsupportedQueryException {
    CswFilterFactory factory = new CswFilterFactory(CswAxisOrder.LON_LAT, true);
    FilterType filter = factory.buildPropertyIsEqualToFilter(Core.ID, "12345", false);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
    assertThat(cqlText, is("\"id\" = '12345'"));
  }

  @Test
  public void testComparisonOpsAnd() throws UnsupportedQueryException {
    FilterType propertyIsLikeFilter =
        cswFilterDelegate.propertyIsLike(propertyName, likeLiteral, isCaseSensitive);
    FilterType propertyIsEqualFilter =
        cswFilterDelegate.propertyIsEqualTo(propertyName, booleanLiteral);

    List<FilterType> filters = new ArrayList<>();
    filters.add(propertyIsEqualFilter);
    filters.add(propertyIsLikeFilter);
    FilterType filter = cswFilterDelegate.and(filters);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
    assertThat(cqlText, is(andComparisonOps));
  }

  @Test
  public void testLogicOpsFiltersAnd() throws UnsupportedQueryException {
    FilterType propertyIsLikeFilter =
        cswFilterDelegate.propertyIsLike(propertyName, likeLiteral, isCaseSensitive);
    FilterType notFilter = cswFilterDelegate.not(propertyIsLikeFilter);
    FilterType propertyIsEqualFilter =
        cswFilterDelegate.propertyIsEqualTo(propertyName, booleanLiteral);

    List<FilterType> filters = new ArrayList<>();
    filters.add(notFilter);
    filters.add(propertyIsEqualFilter);
    FilterType filter = cswFilterDelegate.and(filters);

    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
    assertThat(cqlText, is(andLogicOps));
  }

  @Test
  public void testSpatialOpsAnd() throws UnsupportedQueryException {
    FilterType spatialFilter =
        cswFilterDelegate.dwithin(CswConstants.BBOX_PROP, pointWkt, distance);
    FilterType propertyIsLikeFilter =
        cswFilterDelegate.propertyIsLike(DEFAULT_PROPERTY_NAME, likeLiteral, isCaseSensitive);

    List<FilterType> filters = new ArrayList<>();
    filters.add(spatialFilter);
    filters.add(propertyIsLikeFilter);

    FilterType filter = cswFilterDelegate.or(filters);

    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filter);
    assertThat(cqlText, is(andSpatialOps));
  }

  @Test
  public void testIntersectsPropertyAnyGeo() throws UnsupportedQueryException {
    String propName = Metacard.ANY_GEO;
    FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(intersectsPolygonPropertyOwsBoundingBox));
  }

  @Test
  public void testIntersectsPropertyDctSpatial() throws UnsupportedQueryException {
    String propName = CswConstants.SPATIAL_PROP;
    FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(intersectsPolygonPropertyDctSpatial));
  }

  @Test
  public void testIntersectsPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(intersectsPolygonPropertyOwsBoundingBox));
  }

  @Test
  public void testIntersectsPropertyOwsBoundingBoxPoint() throws UnsupportedQueryException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.intersects(propName, pointWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(intersectsPointPropertyOwsBoundingBox));
  }

  @Test
  public void testIntersectsPolygonLonLatIsConvertedToLatLon()
      throws UnsupportedQueryException, org.locationtech.jts.io.ParseException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.intersects(propName, polygonWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(intersectsPolygonPropertyOwsBoundingBox));
  }

  @Test
  public void testIntersectsPropertyOwsBoundingBoxLineString() throws UnsupportedQueryException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.intersects(propName, lineStringWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(intersectsLineStringPropertyOwsBoundingBox));
  }

  @Test
  public void testIntersectsPropertyOwsBoundingBoxMultiPolygon() throws UnsupportedQueryException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.intersects(propName, multiPolygonWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(intersectsMultiPolygonPropertyOwsBoundingBox));
  }

  @Test
  public void testIntersectsPropertyOwsBoundingBoxMultiPoint() throws UnsupportedQueryException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.intersects(propName, multiPointWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(intersectsMultiPointPropertyOwsBoundingBox));
  }

  @Test
  public void testIntersectsPropertyOwsBoundingBoxMultiLineString()
      throws UnsupportedQueryException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.intersects(propName, multiLineStringWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(intersectsMultiLineStringPropertyOwsBoundingBox));
  }

  @Test
  public void testCrossesPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.crosses(propName, polygonWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(crossesPolygonPropertyOwsBoundingBox));
  }

  @Test
  public void testWithinPropertyOwsBoundingBoxPolygon()
      throws UnsupportedQueryException, org.locationtech.jts.io.ParseException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.within(propName, polygonWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(withinPolygonPropertyOwsBoundingBox));
  }

  @Test
  public void testContainsPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.contains(propName, polygonWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(containsPolygonXmlPropertyOwsBoundingBox));
  }

  @Test
  public void testBeyondPropertyOwsBoundingBoxPoint() throws UnsupportedQueryException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.beyond(propName, pointWkt, distance);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(beyondPointPropertyOwsBoundingBox));
  }

  @Test
  public void testDWithinPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.dwithin(propName, polygonWkt, distance);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(dwithinPolygonPropertyOwsBoundingBox));
  }

  @Test
  public void testTouchesPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.touches(propName, polygonWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(touchesPolygonPropertyOwsBoundingBox));
  }

  @Test
  public void testOverlapsPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.overlaps(propName, polygonWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(overlapsPolygonPropertyOwsBoundingBox));
  }

  @Test
  public void testDisjointPropertyOwsBoundingBoxPolygon() throws UnsupportedQueryException {
    String propName = CswConstants.BBOX_PROP;
    FilterType filterType = cswFilterDelegate.disjoint(propName, polygonWkt);
    String cqlText = CswCqlTextFilter.getInstance().getCqlText(filterType);
    assertThat(cqlText, is(disjointPolygonXmlPropertyOwsBoundingBox));
  }

  private Date getDate() {
    String dateString = "Jun 11 2002";
    SimpleDateFormat formatter = new SimpleDateFormat("MMM d yyyy");
    Date aDate = null;
    try {
      aDate = formatter.parse(dateString);
    } catch (ParseException e) {
      LOGGER.error(e.getMessage(), e);
    }
    return aDate;
  }

  private String getPropertyIsEqualToWithDate(Date aDate) {
    String propertyIsEqualToXmlWithDate =
        DEFAULT_PROPERTY_NAME
            + SPACE
            + EQUALS
            + SPACE
            + "'"
            + convertDateToIso8601Format(aDate)
            + "'";
    return propertyIsEqualToXmlWithDate;
  }

  private DateTime convertDateToIso8601Format(Date inputDate) {
    DateTime outputDate = new DateTime(inputDate);
    return outputDate;
  }

  private CswFilterDelegate initDefaultCswFilterDelegate(
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

    CswFilterDelegate localCswFilterDelegate =
        new CswFilterDelegate(
            getOperation(),
            getMockFilterCapabilities(),
            outputFormatValues,
            resultTypesValues,
            cswSourceConfiguration);
    return localCswFilterDelegate;
  }

  private CswSourceConfiguration initCswSourceConfiguration(
      CswAxisOrder cswAxisOrder, String contentType) {
    CswSourceConfiguration cswSourceConfiguration = new CswSourceConfiguration();
    cswSourceConfiguration.putMetacardCswMapping(Core.ID, CswConstants.CSW_IDENTIFIER);
    cswSourceConfiguration.setCswAxisOrder(cswAxisOrder);
    cswSourceConfiguration.putMetacardCswMapping(Metacard.CONTENT_TYPE, contentType);
    return cswSourceConfiguration;
  }

  private CswSourceConfiguration initCswSourceConfiguration(
      CswAxisOrder cswAxisOrder,
      String contentType,
      String effectiveDateMapping,
      String createdDateMapping,
      String modifiedDateMapping,
      String identifierMapping) {
    CswSourceConfiguration cswSourceConfiguration = new CswSourceConfiguration();
    cswSourceConfiguration.putMetacardCswMapping(Core.ID, identifierMapping);
    cswSourceConfiguration.setCswAxisOrder(cswAxisOrder);
    cswSourceConfiguration.putMetacardCswMapping(Metacard.CONTENT_TYPE, contentType);
    cswSourceConfiguration.putMetacardCswMapping(Metacard.EFFECTIVE, effectiveDateMapping);
    cswSourceConfiguration.putMetacardCswMapping(Core.CREATED, createdDateMapping);
    cswSourceConfiguration.putMetacardCswMapping(Core.MODIFIED, modifiedDateMapping);
    return cswSourceConfiguration;
  }
}
