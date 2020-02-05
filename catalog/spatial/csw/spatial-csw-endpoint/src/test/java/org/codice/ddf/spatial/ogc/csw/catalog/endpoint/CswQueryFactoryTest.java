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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeRegistryImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.AssociationsAttributes;
import ddf.catalog.data.impl.types.ContactAttributes;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.impl.types.MediaAttributes;
import ddf.catalog.data.impl.types.TopicAttributes;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.delegate.TagsFilterDelegate;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.QueryFilterTransformer;
import ddf.catalog.transform.QueryFilterTransformerProvider;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.DistributedSearchType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import net.opengis.filter.v_1_1_0.BinaryComparisonOpType;
import net.opengis.filter.v_1_1_0.BinarySpatialOpType;
import net.opengis.filter.v_1_1_0.DistanceBufferType;
import net.opengis.filter.v_1_1_0.DistanceType;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.LiteralType;
import net.opengis.filter.v_1_1_0.ObjectFactory;
import net.opengis.filter.v_1_1_0.PropertyNameType;
import net.opengis.filter.v_1_1_0.SortByType;
import net.opengis.filter.v_1_1_0.SortPropertyType;
import net.opengis.gml.v_3_1_1.AbstractGeometryType;
import net.opengis.gml.v_3_1_1.AbstractRingPropertyType;
import net.opengis.gml.v_3_1_1.CoordType;
import net.opengis.gml.v_3_1_1.LinearRingType;
import net.opengis.gml.v_3_1_1.PolygonType;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordsRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.mappings.MetacardCswRecordMap;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer.CswQueryFilterTransformer;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer.CswRecordMap;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.styling.UomOgcMapping;
import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.Filter;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.DistanceBufferOperator;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;

public class CswQueryFactoryTest {

  private static final String VALID_TYPES = "csw:Record,csw:Record";

  private static final String VALID_TYPE = "Record";

  private static final String VALID_PREFIX = "csw";

  private static final String CONTEXTUAL_TEST_ATTRIBUTE = "csw:title";

  private static final String SPATIAL_TEST_ATTRIBUTE = "location";

  private static final String CQL_FRAMEWORK_TEST_ATTRIBUTE = "title";

  private static final String TITLE_TEST_ATTRIBUTE = "dc:title";

  private static final String CQL_CONTEXTUAL_PATTERN = "some title";

  private static final String POLYGON_STR = "POLYGON((10 10, 10 25, 40 25, 40 10, 10 10))";

  private static final double REL_GEO_DISTANCE = 100;

  private static final String REL_GEO_UNITS = "kilometers";

  private static final double EXPECTED_GEO_DISTANCE = REL_GEO_DISTANCE * 1000;

  private static final String CQL_CONTEXTUAL_LIKE_QUERY =
      CONTEXTUAL_TEST_ATTRIBUTE + " Like '" + CQL_CONTEXTUAL_PATTERN + "'";

  private static final String CQL_CONTEXTUAL_FUNCTION_QUERY =
      "proximity('" + CONTEXTUAL_TEST_ATTRIBUTE + "', 1,'" + CQL_CONTEXTUAL_PATTERN + "')=true";

  private static final String CQL_FEDERATED_QUERY =
      "\"source-id\" = 'source1' AND " + CQL_CONTEXTUAL_LIKE_QUERY;

  private static final String CQL_SPATIAL_EQUALS_QUERY =
      "equals(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

  private static final String CQL_SPATIAL_DISJOINT_QUERY =
      "disjoint(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

  private static final String CQL_SPATIAL_INTERSECTS_QUERY =
      "intersects(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

  private static final String CQL_SPATIAL_TOUCHES_QUERY =
      "touches(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

  private static final String CQL_SPATIAL_CROSSES_QUERY =
      "crosses(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

  private static final String CQL_SPATIAL_WITHIN_QUERY =
      "within(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

  private static final String CQL_SPATIAL_CONTAINS_QUERY =
      "contains(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

  private static final String CQL_SPATIAL_OVERLAPS_QUERY =
      "overlaps(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

  private static final String CQL_SPATIAL_DWITHIN_QUERY =
      "dwithin("
          + SPATIAL_TEST_ATTRIBUTE
          + ", "
          + POLYGON_STR
          + ", "
          + REL_GEO_DISTANCE
          + ", "
          + REL_GEO_UNITS
          + ")";

  private static final String CQL_SPATIAL_BEYOND_QUERY =
      "beyond("
          + SPATIAL_TEST_ATTRIBUTE
          + ", "
          + POLYGON_STR
          + ", "
          + REL_GEO_DISTANCE
          + ", "
          + REL_GEO_UNITS
          + ")";

  private static final String TIMESTAMP = "2009-12-04T12:00:00Z";

  private static final String DURATION = "P40D";

  private static final String CQL_BEFORE = "before";

  private static final String CQL_AFTER = "after";

  private static final String CQL_DURING = "during";

  private static final String CQL_BEFORE_OR_DURING = "before or during";

  private static final String CQL_DURING_OR_AFTER = "during OR after";

  private static CswQueryFactory queryFactory;

  private static Geometry polygon;

  private static net.opengis.gml.v_3_1_1.ObjectFactory gmlObjectFactory;

  private static ObjectFactory filterObjectFactory;

  private static QName cswQnameOutPutSchema = new QName(CswConstants.CSW_OUTPUT_SCHEMA);

  private QueryFilterTransformerProvider queryFilterTransformerProvider;

  public static MetacardType getCswMetacardType() {
    return new MetacardTypeImpl(
        CswConstants.CSW_METACARD_TYPE_NAME,
        Arrays.asList(
            new ContactAttributes(),
            new LocationAttributes(),
            new MediaAttributes(),
            new TopicAttributes(),
            new AssociationsAttributes(),
            new MetacardTypeImpl(
                "TestDate",
                Collections.singleton(
                    new AttributeDescriptorImpl(
                        "TestDate", true, true, true, false, BasicTypes.DATE_TYPE)))));
  }

  @org.junit.Before
  public void setUp()
      throws URISyntaxException, SourceUnavailableException, UnsupportedQueryException,
          FederationException, ParseException, IngestException {
    FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
    FilterAdapter filterAdapter = new GeotoolsFilterAdapterImpl();
    CswRecordMap cswRecordMap = new MetacardCswRecordMap();

    queryFactory = new CswQueryFactory(cswRecordMap, filterBuilder, filterAdapter);

    AttributeRegistryImpl attributeRegistry = new AttributeRegistryImpl();
    attributeRegistry.registerMetacardType(getCswMetacardType());
    queryFactory.setAttributeRegistry(attributeRegistry);

    polygon = new WKTReader().read(POLYGON_STR);
    gmlObjectFactory = new net.opengis.gml.v_3_1_1.ObjectFactory();
    filterObjectFactory = new ObjectFactory();

    queryFilterTransformerProvider = mock(QueryFilterTransformerProvider.class);
    QueryFilterTransformer cswQueryFilter =
        new CswQueryFilterTransformer(new MetacardCswRecordMap(), attributeRegistry);
    when(queryFilterTransformerProvider.getTransformer(
            new QName(CswConstants.CSW_OUTPUT_SCHEMA, "Record")))
        .thenReturn(Optional.of(cswQueryFilter));
    when(queryFilterTransformerProvider.getTransformer(anyString()))
        .thenReturn(Optional.of(cswQueryFilter));

    queryFactory.setQueryFilterTransformerProvider(queryFilterTransformerProvider);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPostGetRecordsDistributedSearchNotSet()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    QueryRequest queryRequest = queryFactory.getQuery(grr);
    assertThat(queryRequest.isEnterprise(), is(false));
    assertThat(queryRequest.getSourceIds(), anyOf(nullValue(), empty()));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPostGetRecordsDistributedSearchSetToOne()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    DistributedSearchType distributedSearch = new DistributedSearchType();
    distributedSearch.setHopCount(BigInteger.ONE);

    grr.setDistributedSearch(distributedSearch);

    QueryRequest queryRequest = queryFactory.getQuery(grr);
    assertThat(queryRequest.isEnterprise(), is(false));
    assertThat(queryRequest.getSourceIds(), anyOf(nullValue(), empty()));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPostGetRecordsDistributedSearchSetToTen()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    DistributedSearchType distributedSearch = new DistributedSearchType();
    distributedSearch.setHopCount(BigInteger.TEN);

    grr.setDistributedSearch(distributedSearch);

    QueryRequest queryRequest = queryFactory.getQuery(grr);
    assertThat(queryRequest.isEnterprise(), is(true));
    assertThat(queryRequest.getSourceIds(), anyOf(nullValue(), empty()));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPostGetRecordsDistributedSearchSpecificSources()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    DistributedSearchType distributedSearch = new DistributedSearchType();
    distributedSearch.setHopCount(BigInteger.TEN);

    grr.setDistributedSearch(distributedSearch);

    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));

    query.setTypeNames(typeNames);
    QueryConstraintType constraint = new QueryConstraintType();

    constraint.setCqlText(CQL_FEDERATED_QUERY);

    query.setConstraint(constraint);

    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement(cswQnameOutPutSchema, QueryType.class, query);
    grr.setAbstractQuery(jaxbQuery);

    QueryRequest queryRequest = queryFactory.getQuery(grr);
    assertThat(queryRequest.isEnterprise(), is(false));
    assertThat(queryRequest.getSourceIds(), contains("source1"));
  }

  @Test
  public void testPostGetRecordsContextualCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    query.setTypeNames(typeNames);
    QueryConstraintType constraint = new QueryConstraintType();
    constraint.setCqlText(CQL_CONTEXTUAL_LIKE_QUERY);

    query.setConstraint(constraint);

    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);

    grr.setAbstractQuery(jaxbQuery);

    QueryRequest queryRequest = queryFactory.getQuery(grr);
    QueryImpl frameworkQuery = (QueryImpl) queryRequest.getQuery();
    Filter queryFilter = ((QueryImpl) frameworkQuery.getFilter()).getFilter();
    assertThat(queryFilter, instanceOf(PropertyIsLike.class));
    PropertyIsLike like = (PropertyIsLike) queryFilter;
    assertThat(like.getLiteral(), is(CQL_CONTEXTUAL_PATTERN));
    assertThat(
        ((AttributeExpressionImpl) like.getExpression()).getPropertyName(),
        is(CQL_FRAMEWORK_TEST_ATTRIBUTE));
  }

  @Test
  public void testPostGetRecordsValidSort()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    grr.setResultType(ResultType.RESULTS);
    QueryType query = new QueryType();

    SortByType incomingSort = new SortByType();
    SortPropertyType propType = new SortPropertyType();
    PropertyNameType propName = new PropertyNameType();
    propName.setContent(Collections.singletonList(TITLE_TEST_ATTRIBUTE));
    propType.setPropertyName(propName);
    incomingSort.getSortProperty().add(propType);
    query.setSortBy(incomingSort);

    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);

    grr.setAbstractQuery(jaxbQuery);

    QueryRequest queryRequest = queryFactory.getQuery(grr);

    SortBy resultSort = queryRequest.getQuery().getSortBy();

    assertThat(resultSort.getPropertyName().getPropertyName(), is(CQL_FRAMEWORK_TEST_ATTRIBUTE));
    assertThat(resultSort.getSortOrder(), is(SortOrder.ASCENDING));
  }

  @Test
  public void testPostGetRecordsFunctionCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    query.setTypeNames(typeNames);
    QueryConstraintType constraint = new QueryConstraintType();
    constraint.setCqlText(CQL_CONTEXTUAL_FUNCTION_QUERY);

    query.setConstraint(constraint);

    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);

    grr.setAbstractQuery(jaxbQuery);

    QueryRequest queryRequest = queryFactory.getQuery(grr);
    QueryImpl frameworkQuery = (QueryImpl) queryRequest.getQuery();
    Filter queryFilter = ((QueryImpl) frameworkQuery.getFilter()).getFilter();
    assertThat(queryFilter, instanceOf(PropertyIsEqualTo.class));
    PropertyIsEqualTo equalTo = (PropertyIsEqualTo) queryFilter;
    assertThat(equalTo.getExpression1(), instanceOf(Function.class));
    Function function = (Function) equalTo.getExpression1();
    assertThat(equalTo.getExpression2(), instanceOf(Literal.class));
    Literal literal = (Literal) equalTo.getExpression2();
    assertThat(function.getName(), is("proximity"));
    assertThat(function.getParameters().get(0).evaluate(null), is(CONTEXTUAL_TEST_ATTRIBUTE));
    assertThat(function.getParameters().get(1).evaluate(null), is(1L));
    assertThat(function.getParameters().get(2).evaluate(null), is(CQL_CONTEXTUAL_PATTERN));

    assertThat(literal.getValue(), is(true));
  }

  @Test
  public void testPostGetRecordsSpatialEqualsCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    cqlSpatialQuery(Equals.class, CQL_SPATIAL_EQUALS_QUERY);
  }

  @Test
  public void testPostGetRecordsSpatialDisjointCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    cqlSpatialQuery(Disjoint.class, CQL_SPATIAL_DISJOINT_QUERY);
  }

  @Test
  public void testPostGetRecordsSpatialIntersectsCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    cqlSpatialQuery(Intersects.class, CQL_SPATIAL_INTERSECTS_QUERY);
  }

  @Test
  public void testPostGetRecordsSpatialTouchesCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    cqlSpatialQuery(Touches.class, CQL_SPATIAL_TOUCHES_QUERY);
  }

  @Test
  public void testPostGetRecordsSpatialCrossesCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    cqlSpatialQuery(Crosses.class, CQL_SPATIAL_CROSSES_QUERY);
  }

  @Test
  public void testPostGetRecordsSpatialWithinCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    cqlSpatialQuery(Within.class, CQL_SPATIAL_WITHIN_QUERY);
  }

  @Test
  public void testPostGetRecordsSpatialContainsCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    cqlSpatialQuery(Contains.class, CQL_SPATIAL_CONTAINS_QUERY);
  }

  @Test
  public void testPostGetRecordsSpatialOverlapsCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    cqlSpatialQuery(Overlaps.class, CQL_SPATIAL_OVERLAPS_QUERY);
  }

  @Test
  public void testPostGetRecordsSpatialDWithinCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    cqlSpatialRelativeQuery(DWithin.class, CQL_SPATIAL_DWITHIN_QUERY);
  }

  @Test
  public void testPostGetRecordsSpatialBeyondCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    cqlSpatialRelativeQuery(Beyond.class, CQL_SPATIAL_BEYOND_QUERY);
  }

  @Test
  public void testPostGetRecordsSpatialEqualsOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    BinarySpatialOpType op = createBinarySpatialOpType();

    ogcSpatialQuery(Equals.class, filterObjectFactory.createEquals(op));
  }

  @Test
  public void testPostGetRecordsSpatialDisjointOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    BinarySpatialOpType op = createBinarySpatialOpType();
    ogcSpatialQuery(Disjoint.class, filterObjectFactory.createDisjoint(op));
  }

  @Test
  public void testPostGetRecordsSpatialIntersectsOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    BinarySpatialOpType op = createBinarySpatialOpType();
    ogcSpatialQuery(Intersects.class, filterObjectFactory.createIntersects(op));
  }

  @Test
  public void testPostGetRecordsSpatialTouchesOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    BinarySpatialOpType op = createBinarySpatialOpType();
    ogcSpatialQuery(Touches.class, filterObjectFactory.createTouches(op));
  }

  @Test
  public void testPostGetRecordsSpatialCrossesOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    BinarySpatialOpType op = createBinarySpatialOpType();
    ogcSpatialQuery(Crosses.class, filterObjectFactory.createCrosses(op));
  }

  @Test
  public void testPostGetRecordsSpatialWithinOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    BinarySpatialOpType op = createBinarySpatialOpType();
    ogcSpatialQuery(Within.class, filterObjectFactory.createWithin(op));
  }

  @Test
  public void testPostGetRecordsSpatialContainsOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    BinarySpatialOpType op = createBinarySpatialOpType();
    ogcSpatialQuery(Contains.class, filterObjectFactory.createContains(op));
  }

  @Test
  public void testPostGetRecordsSpatialOverlapsOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    BinarySpatialOpType op = createBinarySpatialOpType();
    ogcSpatialQuery(Overlaps.class, filterObjectFactory.createOverlaps(op));
  }

  @Test
  public void testPostGetRecordsSpatialDWithinOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    DistanceBufferType op = createDistanceBufferType();
    ogcSpatialRelativeQuery(DWithin.class, filterObjectFactory.createDWithin(op));
  }

  @Test
  public void testPostGetRecordsSpatialBeyondOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    DistanceBufferType op = createDistanceBufferType();
    ogcSpatialRelativeQuery(Beyond.class, filterObjectFactory.createBeyond(op));
  }

  @Test
  public void testGetGetRecordsSpatialDWithinOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    String constraint = createDistanceBufferQuery("DWithin");
    ogcSpatialRelativeQuery(DWithin.class, constraint);
  }

  @Test
  public void testGetGetRecordsSpatialBeyondOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    String constraint = createDistanceBufferQuery("Beyond");
    ogcSpatialRelativeQuery(Beyond.class, constraint);
  }

  @Test
  public void testPostGetRecordsTemporalPropertyIsLessOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    BinaryComparisonOpType op =
        createTemporalBinaryComparisonOpType(CswConstants.CSW_CREATED, TIMESTAMP);
    ogcTemporalQuery(Core.CREATED, filterObjectFactory.createPropertyIsLessThan(op), Before.class);
  }

  @Ignore(
      "TODO: the functions this test tests has been augmented to play well with the limited capabilities of the Solr provider.  "
          + "These tests and the functions they test should be reenabled and refactored after DDF-311 is addressed")
  @Test
  public void testPostGetRecordsTemporalPropertyIsLessOrEqualOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    BinaryComparisonOpType op =
        createTemporalBinaryComparisonOpType(CswConstants.CSW_CREATED, TIMESTAMP);
    ogcOrdTemporalQuery(filterObjectFactory.createPropertyIsLessThanOrEqualTo(op));
  }

  @Ignore(
      "TODO: the functions this test tests has been augmented to play well with the limited capabilities of the Solr provider.  "
          + "These tests and the functions they test should be reenabled and refactored after DDF-311 is addressed")
  @Test
  public void testPostGetRecordsTemporalPropertyIsGreaterOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    BinaryComparisonOpType op =
        createTemporalBinaryComparisonOpType(CswConstants.CSW_CREATED, TIMESTAMP);
    ogcTemporalQuery(
        Core.CREATED, filterObjectFactory.createPropertyIsGreaterThan(op), After.class);
  }

  @Ignore(
      "TODO: the functions this test tests has been augmented to play well with the limited capabilities of the Solr provider.  "
          + "These tests and the functions they test should be reenabled and refactored after DDF-311 is addressed")
  @Test
  public void testPostGetRecordsTemporalPropertyIsGreaterOrEqualOgcFilter()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    BinaryComparisonOpType op =
        createTemporalBinaryComparisonOpType(CswConstants.CSW_CREATED, TIMESTAMP);
    ogcOrdTemporalQuery(filterObjectFactory.createPropertyIsGreaterThanOrEqualTo(op));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPostGetRecordsTemporalBeforeCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {

    String[] cqlTextValues =
        new String[] {CswConstants.CSW_NO_PREFIX_CREATED, CQL_BEFORE, TIMESTAMP};
    String cqlText = StringUtils.join(cqlTextValues, " ");
    cqlTemporalQuery(Core.CREATED, cqlText, new Class[] {Before.class});
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPostGetRecordsTemporalAfterCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {

    String[] cqlTextValues = new String[] {CswConstants.CSW_ISSUED, CQL_AFTER, TIMESTAMP};
    String cqlText = StringUtils.join(cqlTextValues, " ");
    cqlTemporalQuery(Core.MODIFIED, cqlText, new Class[] {After.class});
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPostGetRecordsTemporalDuringCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {

    String[] cqlTextValues =
        new String[] {CswConstants.CSW_DATE_ACCEPTED, CQL_DURING, TIMESTAMP, "/", DURATION};
    String cqlText = StringUtils.join(cqlTextValues, " ");
    cqlTemporalQuery(Metacard.EFFECTIVE, cqlText, new Class[] {During.class});
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPostGetRecordsTemporalBeforeOrDuringCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {

    String[] cqlTextValues =
        new String[] {CswConstants.CSW_DATE, CQL_BEFORE_OR_DURING, TIMESTAMP, "/", DURATION};
    String cqlText = StringUtils.join(cqlTextValues, " ");
    cqlTemporalQuery(Core.MODIFIED, cqlText, new Class[] {Before.class, During.class});
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPostGetRecordsTemporalAfterOrDuringCQLQuery()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {

    String[] cqlTextValues =
        new String[] {CswConstants.CSW_VALID, CQL_DURING_OR_AFTER, TIMESTAMP, "/", DURATION};
    String cqlText = StringUtils.join(cqlTextValues, " ");
    cqlTemporalQuery(Core.EXPIRATION, cqlText, new Class[] {During.class, After.class});
  }

  @Test
  public void testQueryTags() throws Exception {
    queryFactory.setSchemaToTagsMapping(new String[] {CswConstants.CSW_OUTPUT_SCHEMA + "=myTag"});

    List<String> ids = new ArrayList<>();
    ids.add("someId");

    FilterAdapter adapter = new GeotoolsFilterAdapterImpl();

    assertThat(
        adapter.adapt(
            queryFactory
                .updateQueryRequestTags(
                    queryFactory.getQueryById(ids), CswConstants.CSW_OUTPUT_SCHEMA)
                .getQuery(),
            new TagsFilterDelegate("myTag")),
        is(true));
  }

  @Test
  public void testMultipleQueryFilterTransformers() throws CswException {
    List<String> namespaces = Arrays.asList("{namespace}one", "{namespace}two");

    QueryConstraintType constraint = mock(QueryConstraintType.class);
    when(constraint.isSetCqlText()).thenReturn(true);
    when(constraint.getCqlText()).thenReturn(CQL_CONTEXTUAL_LIKE_QUERY);

    for (String namespace : namespaces) {
      QueryRequest request = mock(QueryRequest.class);
      addQueryFilterTransformer(namespace, request);
      QueryRequest result = queryFactory.getQuery(constraint, namespace);
      assertThat(result, equalTo(request));
    }
  }

  /**
   * Runs a binary Spatial CQL Query, verifying that the right filter class is generated based on
   * CQL
   *
   * @param clz Class of filter to generate
   * @param cql CQL Query String
   * @throws UnsupportedQueryException
   * @throws SourceUnavailableException
   * @throws FederationException
   * @throws CswException
   */
  private <N extends BinarySpatialOperator> void cqlSpatialQuery(Class<N> clz, String cql)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          CswException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    query.setTypeNames(typeNames);
    QueryConstraintType constraint = new QueryConstraintType();
    constraint.setCqlText(cql);

    query.setConstraint(constraint);
    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);

    grr.setAbstractQuery(jaxbQuery);

    QueryImpl frameworkQuery = (QueryImpl) queryFactory.getQuery(grr).getQuery();
    Filter queryFilter = ((QueryImpl) frameworkQuery.getFilter()).getFilter();
    assertThat(queryFilter, instanceOf(clz));
    @SuppressWarnings("unchecked")
    N spatial = (N) queryFilter;
    assertThat(((LiteralExpressionImpl) spatial.getExpression2()).getValue(), is(polygon));

    assertThat(
        ((AttributeExpressionImpl) spatial.getExpression1()).getPropertyName(),
        is(SPATIAL_TEST_ATTRIBUTE));
  }

  /**
   * Runs a relative spatial CQL Query, verifying that the right filter class is generated based on
   * CQL
   *
   * @param clz Class of filter to generate
   * @param cql CQL Query String
   * @throws UnsupportedQueryException
   * @throws SourceUnavailableException
   * @throws FederationException
   * @throws CswException
   */
  private <N extends DistanceBufferOperator> void cqlSpatialRelativeQuery(Class<N> clz, String cql)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          CswException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    query.setTypeNames(typeNames);
    QueryConstraintType constraint = new QueryConstraintType();
    constraint.setCqlText(cql);

    query.setConstraint(constraint);
    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);

    grr.setAbstractQuery(jaxbQuery);

    QueryImpl frameworkQuery = (QueryImpl) queryFactory.getQuery(grr).getQuery();
    Filter queryFilter = ((QueryImpl) frameworkQuery.getFilter()).getFilter();
    assertThat(queryFilter, instanceOf(clz));
    @SuppressWarnings("unchecked")
    N spatial = (N) queryFilter;
    assertThat(((LiteralExpressionImpl) spatial.getExpression2()).getValue(), is(polygon));

    assertThat(
        ((AttributeExpressionImpl) spatial.getExpression1()).getPropertyName(),
        is(SPATIAL_TEST_ATTRIBUTE));

    assertThat(spatial.getDistanceUnits(), is(UomOgcMapping.METRE.name()));
    assertThat(spatial.getDistance(), is(EXPECTED_GEO_DISTANCE));
  }

  private BinaryComparisonOpType createTemporalBinaryComparisonOpType(
      String attr, String comparison) {
    BinaryComparisonOpType comparisonOp = new BinaryComparisonOpType();

    PropertyNameType propName = new PropertyNameType();
    propName.getContent().add(attr);

    comparisonOp.getExpression().add(filterObjectFactory.createPropertyName(propName));

    LiteralType literal = new LiteralType();
    literal.getContent().add(comparison);

    comparisonOp.getExpression().add(filterObjectFactory.createLiteral(literal));
    return comparisonOp;
  }

  private BinarySpatialOpType createBinarySpatialOpType() {
    BinarySpatialOpType binarySpatialOps = new BinarySpatialOpType();

    PropertyNameType propName = new PropertyNameType();
    propName.getContent().add(SPATIAL_TEST_ATTRIBUTE);
    binarySpatialOps.getPropertyName().add(propName);

    binarySpatialOps.setGeometry(createPolygon());
    return binarySpatialOps;
  }

  private String createDistanceBufferQuery(String comparison) {
    String query =
        "      <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\">"
            + "        <ogc:"
            + comparison
            + ">"
            + "          <ogc:PropertyName>"
            + SPATIAL_TEST_ATTRIBUTE
            + "</ogc:PropertyName>"
            + "          <gml:Polygon gml:id=\"Pl001\">"
            + "            <gml:exterior>"
            + "              <gml:LinearRing>"
            + "                <gml:pos>10 10</gml:pos>"
            + "                <gml:pos>10 25</gml:pos>"
            + "                <gml:pos>40 25</gml:pos>"
            + "                <gml:pos>40 10</gml:pos>"
            + "                <gml:pos>10 10</gml:pos>"
            + "              </gml:LinearRing>"
            + "            </gml:exterior>"
            + "          </gml:Polygon>"
            + "          <ogc:Distance units=\""
            + REL_GEO_UNITS
            + "\">"
            + REL_GEO_DISTANCE
            + "</ogc:Distance>"
            + "        </ogc:"
            + comparison
            + ">"
            + "      </ogc:Filter>";

    return query;
  }

  private DistanceBufferType createDistanceBufferType() {
    DistanceBufferType distanceBuffer = new DistanceBufferType();

    PropertyNameType propName = new PropertyNameType();
    propName.getContent().add(SPATIAL_TEST_ATTRIBUTE);
    distanceBuffer.setPropertyName(propName);

    DistanceType distance = filterObjectFactory.createDistanceType();
    distance.setUnits(REL_GEO_UNITS);
    distance.setValue(REL_GEO_DISTANCE);

    distanceBuffer.setDistance(distance);
    distanceBuffer.setGeometry(createPolygon());
    return distanceBuffer;
  }

  private JAXBElement<AbstractGeometryType> createPolygon() {
    PolygonType localPolygon = new PolygonType();

    LinearRingType ring = new LinearRingType();
    for (Coordinate coordinate : polygon.getCoordinates()) {
      CoordType coord = new CoordType();
      coord.setX(BigDecimal.valueOf(coordinate.x));
      coord.setY(BigDecimal.valueOf(coordinate.y));
      if (!Double.isNaN(coordinate.z)) {
        coord.setZ(BigDecimal.valueOf(coordinate.z));
      }
      ring.getCoord().add(coord);
    }
    AbstractRingPropertyType abstractRing = new AbstractRingPropertyType();
    abstractRing.setRing(gmlObjectFactory.createLinearRing(ring));
    localPolygon.setExterior(gmlObjectFactory.createExterior(abstractRing));

    JAXBElement<AbstractGeometryType> agt =
        new JAXBElement<>(
            new QName("http://www.opengis.net/gml", "Polygon"),
            AbstractGeometryType.class,
            null,
            localPolygon);
    return agt;
  }

  /**
   * Runs a binary Spatial OGC Query, verifying that the right filter class is generated based on
   * OGC Filter
   *
   * @param constraint The OGC Filter Constraint as an XML string
   * @throws UnsupportedQueryException
   * @throws SourceUnavailableException
   * @throws FederationException
   * @throws CswException
   */
  private <N extends DistanceBufferOperator> void ogcSpatialRelativeQuery(
      Class<N> clz, String constraint)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          CswException {
    GetRecordsRequest grr = createDefaultGetRecordsRequest();

    grr.setConstraintLanguage("FILTER");
    grr.setConstraint(constraint);

    QueryImpl frameworkQuery =
        (QueryImpl) queryFactory.getQuery(grr.get202RecordsType()).getQuery();
    Filter queryFilter = ((QueryImpl) frameworkQuery.getFilter()).getFilter();
    assertThat(queryFilter, instanceOf(clz));
    @SuppressWarnings("unchecked")
    N spatial = (N) queryFilter;
    assertThat(((LiteralExpressionImpl) spatial.getExpression2()).getValue(), is(polygon));

    assertThat(
        ((AttributeExpressionImpl) spatial.getExpression1()).getPropertyName(),
        is(SPATIAL_TEST_ATTRIBUTE));
  }

  /**
   * Runs a binary Spatial OGC Query, verifying that the right filter class is generated based on
   * OGC Filter
   *
   * @param spatialOps BinarySpatialOps query
   * @throws UnsupportedQueryException
   * @throws SourceUnavailableException
   * @throws FederationException
   * @throws CswException
   */
  private <N extends DistanceBufferOperator> void ogcSpatialRelativeQuery(
      Class<N> clz, JAXBElement<DistanceBufferType> spatialOps)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          CswException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    query.setTypeNames(typeNames);
    QueryConstraintType constraint = new QueryConstraintType();
    FilterType filter = new FilterType();
    filter.setSpatialOps(spatialOps);

    constraint.setFilter(filter);

    query.setConstraint(constraint);
    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);

    grr.setAbstractQuery(jaxbQuery);

    QueryImpl frameworkQuery = (QueryImpl) queryFactory.getQuery(grr).getQuery();
    Filter queryFilter = ((QueryImpl) frameworkQuery.getFilter()).getFilter();
    assertThat(queryFilter, instanceOf(clz));
    @SuppressWarnings("unchecked")
    N spatial = (N) queryFilter;
    assertThat(((LiteralExpressionImpl) spatial.getExpression2()).getValue(), is(polygon));

    assertThat(
        ((AttributeExpressionImpl) spatial.getExpression1()).getPropertyName(),
        is(SPATIAL_TEST_ATTRIBUTE));
  }

  /**
   * Runs a binary Spatial OGC Query, verifying that the right filter class is generated based on
   * OGC Filter
   *
   * @param spatialOps BinarySpatialOps query
   * @throws UnsupportedQueryException
   * @throws SourceUnavailableException
   * @throws FederationException
   * @throws CswException
   */
  private <N extends BinarySpatialOperator> void ogcSpatialQuery(
      Class<N> clz, JAXBElement<BinarySpatialOpType> spatialOps)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          CswException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    query.setTypeNames(typeNames);
    QueryConstraintType constraint = new QueryConstraintType();
    FilterType filter = new FilterType();
    filter.setSpatialOps(spatialOps);

    constraint.setFilter(filter);

    query.setConstraint(constraint);
    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);

    grr.setAbstractQuery(jaxbQuery);

    QueryImpl frameworkQuery = (QueryImpl) queryFactory.getQuery(grr).getQuery();
    Filter queryFilter = ((QueryImpl) frameworkQuery.getFilter()).getFilter();
    assertThat(queryFilter, instanceOf(clz));
    @SuppressWarnings("unchecked")
    N spatial = (N) queryFilter;
    assertThat(((LiteralExpressionImpl) spatial.getExpression2()).getValue(), is(polygon));

    assertThat(
        ((AttributeExpressionImpl) spatial.getExpression1()).getPropertyName(),
        is(SPATIAL_TEST_ATTRIBUTE));
  }

  /**
   * Runs a binary Temporal OGC Query, verifying that the right filter class is generated based on
   * OGC Filter
   *
   * @param expectedAttr Exprected Mapped Attribute
   * @param temporalOps The Temporal query, in terms of a binary comparison
   * @param clz the Expected Class result
   * @throws UnsupportedQueryException
   * @throws SourceUnavailableException
   * @throws FederationException
   * @throws CswException
   */
  @SuppressWarnings("unchecked")
  private <N extends BinaryTemporalOperator> void ogcTemporalQuery(
      String expectedAttr, JAXBElement<BinaryComparisonOpType> temporalOps, Class<N> clz)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          CswException {
    Filter filter = generateTemporalFilter(temporalOps);

    assertThat(filter, instanceOf(clz));

    N temporal = (N) filter;
    assertThat(
        ((AttributeExpressionImpl) temporal.getExpression1()).getPropertyName(), is(expectedAttr));
  }

  /**
   * Runs an Or'd query of multiple binary Temporal OGC Query, verifying that the right filter class
   * is generated based on OGC Filter
   *
   * @param temporalOps The Temporal query, in terms of a binary comparison
   * @throws UnsupportedQueryException
   * @throws SourceUnavailableException
   * @throws FederationException
   * @throws CswException
   */
  @SuppressWarnings("unchecked")
  private void ogcOrdTemporalQuery(JAXBElement<BinaryComparisonOpType> temporalOps)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          CswException {
    Filter filter = generateTemporalFilter(temporalOps);

    assertThat(filter, instanceOf(Or.class));

    Or ordTemporal = (Or) filter;

    List<Filter> temporalFilters = ordTemporal.getChildren();

    List<Class<? extends BinaryTemporalOperator>> classes = new ArrayList<>();

    for (Filter temporal : temporalFilters) {
      assertThat(temporal, instanceOf(BinaryTemporalOperator.class));
      classes.add((Class<? extends BinaryTemporalOperator>) temporal.getClass());
    }
  }

  private Filter generateTemporalFilter(JAXBElement<BinaryComparisonOpType> temporalOps)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          CswException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    query.setTypeNames(typeNames);
    QueryConstraintType constraint = new QueryConstraintType();
    FilterType filter = new FilterType();
    filter.setComparisonOps(temporalOps);

    constraint.setFilter(filter);

    query.setConstraint(constraint);
    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);

    grr.setAbstractQuery(jaxbQuery);

    QueryImpl frameworkQuery = (QueryImpl) queryFactory.getQuery(grr).getQuery();
    return ((QueryImpl) frameworkQuery.getFilter()).getFilter();
  }

  @SuppressWarnings("unchecked")
  private <N extends BinaryTemporalOperator> void cqlTemporalQuery(
      String expectedAttr, String cqlSpatialDwithinQuery, Class<N>[] classes)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          CswException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    query.setTypeNames(typeNames);
    QueryConstraintType constraint = new QueryConstraintType();
    constraint.setCqlText(cqlSpatialDwithinQuery);

    query.setConstraint(constraint);
    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement(cswQnameOutPutSchema, QueryType.class, query);

    grr.setAbstractQuery(jaxbQuery);

    QueryImpl frameworkQuery = (QueryImpl) queryFactory.getQuery(grr).getQuery();
    N temporal = null;
    Filter queryFilter = ((QueryImpl) frameworkQuery.getFilter()).getFilter();
    if (classes.length > 1) {
      assertThat(queryFilter, instanceOf(Or.class));
      int i = 0;
      for (Filter filter : ((Or) queryFilter).getChildren()) {
        assertThat(filter, instanceOf(classes[i++]));
        temporal = (N) filter;
      }
    } else {
      assertThat(queryFilter, instanceOf(classes[0]));
      temporal = (N) queryFilter;
    }
    assertThat(
        ((AttributeExpressionImpl) temporal.getExpression1()).getPropertyName(), is(expectedAttr));
  }

  /**
   * Creates default GetRecordsRequest GET request, with no sections specified
   *
   * @return Vanilla valid GetRecordsRequest object
   */
  private GetRecordsRequest createDefaultGetRecordsRequest() {
    GetRecordsRequest grr = new GetRecordsRequest();
    grr.setService(CswConstants.CSW);
    grr.setVersion(CswConstants.VERSION_2_0_2);
    grr.setRequest(CswConstants.GET_RECORDS);
    grr.setNamespace(
        CswConstants.XMLNS_DEFINITION_PREFIX
            + CswConstants.CSW_NAMESPACE_PREFIX
            + CswConstants.EQUALS_CHAR
            + CswConstants.CSW_OUTPUT_SCHEMA
            + CswConstants.XMLNS_DEFINITION_POSTFIX
            + CswConstants.COMMA
            + CswConstants.XMLNS_DEFINITION_PREFIX
            + CswConstants.OGC_NAMESPACE_PREFIX
            + CswConstants.EQUALS_CHAR
            + CswConstants.OGC_SCHEMA
            + CswConstants.XMLNS_DEFINITION_POSTFIX
            + CswConstants.COMMA
            + CswConstants.XMLNS_DEFINITION_PREFIX
            + CswConstants.GML_NAMESPACE_PREFIX
            + CswConstants.EQUALS_CHAR
            + CswConstants.GML_SCHEMA
            + CswConstants.XMLNS_DEFINITION_POSTFIX
            + CswConstants.COMMA);

    grr.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
    grr.setOutputFormat(CswConstants.OUTPUT_FORMAT_XML);
    grr.setTypeNames(VALID_TYPES);
    return grr;
  }

  /**
   * Creates default GetRecordsType POST request, with no sections specified
   *
   * @return Vanilla valid GetRecordsType object
   */
  private GetRecordsType createDefaultPostRecordsRequest() {
    GetRecordsType grr = new GetRecordsType();

    grr.setOutputFormat(CswConstants.OUTPUT_FORMAT_XML);
    grr.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);

    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));

    query.setTypeNames(typeNames);

    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);
    grr.setAbstractQuery(jaxbQuery);
    return grr;
  }

  private void addQueryFilterTransformer(String namespace, QueryRequest request) {
    QueryFilterTransformer transformer = mock(QueryFilterTransformer.class);
    when(transformer.transform(any(), any())).thenReturn(request);
    when(queryFilterTransformerProvider.getTransformer(namespace))
        .thenReturn(Optional.of(transformer));
  }
}
