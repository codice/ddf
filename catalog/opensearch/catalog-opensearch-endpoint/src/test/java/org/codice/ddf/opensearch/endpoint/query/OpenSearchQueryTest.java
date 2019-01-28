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
package org.codice.ddf.opensearch.endpoint.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.impl.filter.TemporalFilter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import org.codice.ddf.opensearch.OpenSearchConstants;
import org.codice.ddf.opensearch.endpoint.query.filter.BBoxSpatialFilter;
import org.codice.ddf.opensearch.endpoint.query.filter.PolygonSpatialFilter;
import org.geotools.filter.AndImpl;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.FilterTransformer;
import org.geotools.filter.IsEqualsToImpl;
import org.geotools.filter.LikeFilterImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.filter.OrImpl;
import org.geotools.filter.spatial.DWithinImpl;
import org.geotools.filter.spatial.IntersectsImpl;
import org.geotools.filter.temporal.DuringImpl;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.jts.spatialschema.geometry.GeometryImpl;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.PrimitiveFactoryImpl;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl;
import org.geotools.geometry.text.WKTParser;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Intersects;
import org.opengis.geometry.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.temporal.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSearchQueryTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchQueryTest.class);

  private static final FilterBuilder FILTER_BUILDER = new GeotoolsFilterBuilder();

  private static final double DOUBLE_DELTA = 0.00001;

  private static final String GEOMETRY_WKT =
      "GEOMETRYCOLLECTION (POINT (-105.2071712 40.0160994), LINESTRING (4 6, 7 10))";

  private static final String POLYGON_WKT =
      "POLYGON ((-120.032 30.943, -120.032 35.039, -110.856 35.039, -110.856 30.943, -120.032 30.943))";

  private static final String POLYGON_WKT_2 =
      "POLYGON ((100 -30, 100 -35, 110 -35, 110 -30, 100 -30))";

  private static final String POINT_WKT = "POINT (117.3425 33.9283)";

  private static final DWithin D_WITHIN_FILTER =
      (DWithin)
          FILTER_BUILDER
              .attribute(OpenSearchConstants.SUPPORTED_SPATIAL_SEARCH_TERM)
              .is()
              .withinBuffer()
              .wkt(POINT_WKT, 5);
  private static final Intersects INTERSECTS_FILTER =
      (Intersects)
          FILTER_BUILDER
              .attribute(OpenSearchConstants.SUPPORTED_SPATIAL_SEARCH_TERM)
              .intersecting()
              .wkt(POLYGON_WKT);
  private static final Intersects INTERSECTS_FILTER_2 =
      (Intersects)
          FILTER_BUILDER
              .attribute(OpenSearchConstants.SUPPORTED_SPATIAL_SEARCH_TERM)
              .intersecting()
              .wkt(POLYGON_WKT_2);
  private static final Intersects GEOMETRY_COLLECTION =
      (Intersects)
          FILTER_BUILDER
              .attribute(OpenSearchConstants.SUPPORTED_SPATIAL_SEARCH_TERM)
              .intersecting()
              .wkt(GEOMETRY_WKT);

  private static final WKTWriter WKT_WRITER = new WKTWriter();

  private Filter getKeywordAttributeFilter(String keyword) {
    return FILTER_BUILDER.attribute(Metacard.ANY_TEXT).is().like().text(keyword);
  }

  @Test
  public void testComplexContextualFilterSpecialCharacters() {
    testKeywordFiler("נּ▓ﻨﻨﻨ￼◄€", getKeywordAttributeFilter("נּ▓ﻨﻨﻨ￼◄€"));
  }

  @Test
  public void testComplexContextualFilterOperatorsInKeywords() {
    // Test Keyword
    testKeywordFiler("Keyword  ", getKeywordAttributeFilter("Keyword"));
    testKeywordFiler("OR", getKeywordAttributeFilter("OR"));
    testKeywordFiler("AND", getKeywordAttributeFilter("AND"));
    testKeywordFiler(" NOT ", getKeywordAttributeFilter("NOT"));
    testKeywordFiler("\" quotes\" ", getKeywordAttributeFilter("quotes"));
    testKeywordFiler("wildcard*", getKeywordAttributeFilter("wildcard*"));
  }

  @Test
  public void testComplexContextualFilterPhrases() {
    // Test Phrase
    testKeywordFiler("\"OR AND NOT\"", getKeywordAttributeFilter("OR AND NOT"));
    testKeywordFiler("\"This is a sentence.\"", getKeywordAttributeFilter("This is a sentence."));
    testKeywordFiler(
        "A \"test keyword2 keyword3\" OR test2",
        FILTER_BUILDER.anyOf( // OR
            FILTER_BUILDER.allOf(
                getKeywordAttributeFilter("A"),
                getKeywordAttributeFilter("test keyword2 keyword3")), // A AND test keyword2
            // keyword3
            getKeywordAttributeFilter("test2"))); // test2

    testKeywordFiler(
        "A ((\"test\" OR test2) NOT test3)",
        FILTER_BUILDER.allOf( // AND
            getKeywordAttributeFilter("A"), // A
            FILTER_BUILDER.allOf( // AND
                FILTER_BUILDER.anyOf(
                    getKeywordAttributeFilter("test"),
                    getKeywordAttributeFilter("test2")), // test OR test2
                FILTER_BUILDER.not(getKeywordAttributeFilter("test3"))))); // NOT test3

    testKeywordFiler(
        "some!keyword*1 ((\"test keyword1 keyword!1#A\" OR test2) NOT test3)",
        FILTER_BUILDER.allOf( // AND
            getKeywordAttributeFilter("some!keyword*1"), // some!keyword*1
            FILTER_BUILDER.allOf( // AND
                FILTER_BUILDER.anyOf(
                    getKeywordAttributeFilter("test keyword1 keyword!1#A"),
                    getKeywordAttributeFilter("test2")), // test keyword1
                // keyword!1#A OR test2
                FILTER_BUILDER.not(getKeywordAttributeFilter("test3"))))); // NOT
    // test3
  }

  @Test
  public void testComplexContextualFilterGroups() {
    // Test Group
    testKeywordFiler(
        "( (Apple) AND ((Orange OR Banana)   ) NOT (Strawberry )  )",
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.allOf(
                getKeywordAttributeFilter("Apple"),
                FILTER_BUILDER.anyOf(
                    getKeywordAttributeFilter("Orange"), getKeywordAttributeFilter("Banana"))),
            FILTER_BUILDER.not(getKeywordAttributeFilter("Strawberry"))));
  }

  @Test
  public void testComplexContextualFilterMixedBooleanOperators() {
    // Test Boolean Operators
    testKeywordFiler(
        "A AND B OR C",
        FILTER_BUILDER.anyOf( // OR
            FILTER_BUILDER.allOf(
                getKeywordAttributeFilter("A"), getKeywordAttributeFilter("B")), // A AND B
            getKeywordAttributeFilter("C"))); // C

    testKeywordFiler(
        "  A AND B AND C",
        FILTER_BUILDER.allOf( // AND
            FILTER_BUILDER.allOf(
                getKeywordAttributeFilter("A"), getKeywordAttributeFilter("B")), // A AND B
            getKeywordAttributeFilter("C"))); // C

    testKeywordFiler(
        "A AND B OR C NOT D  ",
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.anyOf(
                FILTER_BUILDER.allOf(
                    getKeywordAttributeFilter("A"), getKeywordAttributeFilter("B")),
                getKeywordAttributeFilter("C")),
            FILTER_BUILDER.not(getKeywordAttributeFilter("D"))));

    testKeywordFiler(
        "A B OR (C NOT D)",
        FILTER_BUILDER.anyOf( // OR
            FILTER_BUILDER.allOf(
                getKeywordAttributeFilter("A"), getKeywordAttributeFilter("B")), // A AND B
            FILTER_BUILDER.allOf(
                getKeywordAttributeFilter("C"),
                FILTER_BUILDER.not(getKeywordAttributeFilter("D"))))); // C NOT D

    testKeywordFiler(
        "A (\"test\") OR test2",
        FILTER_BUILDER.anyOf( // OR
            FILTER_BUILDER.allOf(getKeywordAttributeFilter("A"), getKeywordAttributeFilter("test")),
            // A AND (test test2)
            getKeywordAttributeFilter("test2"))); // test2
  }

  @Test
  public void testComplexContextualFilterNOTOperator() {
    // Test NOT Operator
    testKeywordFiler(
        "A NOT B",
        FILTER_BUILDER.allOf(
            getKeywordAttributeFilter("A"), FILTER_BUILDER.not(getKeywordAttributeFilter("B"))));
    testKeywordFiler(
        "A NOT \"B\"",
        FILTER_BUILDER.allOf(
            getKeywordAttributeFilter("A"), FILTER_BUILDER.not(getKeywordAttributeFilter("B"))));
    testKeywordFiler(
        "A NOT   A",
        FILTER_BUILDER.allOf(
            getKeywordAttributeFilter("A"), FILTER_BUILDER.not(getKeywordAttributeFilter("A"))));
    testKeywordFiler(
        "NOT    NOT NOT",
        FILTER_BUILDER.allOf(
            getKeywordAttributeFilter("NOT"),
            FILTER_BUILDER.not(getKeywordAttributeFilter("NOT"))));
  }

  @Test
  public void testComplexContextualFilterOROperator() {
    // Test OR Operator
    testKeywordFiler(
        "A OR B",
        FILTER_BUILDER.anyOf(getKeywordAttributeFilter("A"), getKeywordAttributeFilter("B")));
    testKeywordFiler(
        "(A OR A)",
        FILTER_BUILDER.anyOf(getKeywordAttributeFilter("A"), getKeywordAttributeFilter("A")));
    testKeywordFiler(
        "OR OR OR OR OR OR OR",
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.anyOf(
                FILTER_BUILDER.anyOf(
                    getKeywordAttributeFilter("OR"), getKeywordAttributeFilter("OR")),
                getKeywordAttributeFilter("OR")),
            getKeywordAttributeFilter("OR")));
  }

  @Test
  public void testComplexContextualFilterANDOperator() {
    // Test AND Operator
    testKeywordFiler(
        "A AND 999",
        FILTER_BUILDER.allOf(getKeywordAttributeFilter("A"), getKeywordAttributeFilter("999")));
    // TODO with the new keyword query grammar, this... probably shouldn't be allowed?
    // testKeywordFiler("AND AND", FILTER_BUILDER.allOf(getKeywordAttributeFilter("AND"),
    // getKeywordAttributeFilter("AND")));
    testKeywordFiler(
        "A B C D",
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.allOf(
                    getKeywordAttributeFilter("A"), getKeywordAttributeFilter("B")),
                getKeywordAttributeFilter("C")),
            getKeywordAttributeFilter("D")));
    testKeywordFiler(
        "A B C AND D",
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.allOf(
                    getKeywordAttributeFilter("A"), getKeywordAttributeFilter("B")),
                getKeywordAttributeFilter("C")),
            getKeywordAttributeFilter("D")));
    testKeywordFiler(
        "A AND AND AND C",
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.allOf(getKeywordAttributeFilter("A"), getKeywordAttributeFilter("AND")),
            getKeywordAttributeFilter("C")));
  }

  @Test
  public void testComplexContextualFilterCommonWords() {
    // Test Common Words
    // Note: Spec said implementation SHOULD ignores these words but did not say we must;
    // therefore, DDF do not ignore them
    testKeywordFiler("a", getKeywordAttributeFilter("a"));
    testKeywordFiler("the", getKeywordAttributeFilter("the"));
  }

  @Test
  public void testComplexContextualFilterOrderOfPrecedence() {
    // Test Order of Precedence
    testKeywordFiler(
        "A OR B   OR C OR D",
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.anyOf(
                FILTER_BUILDER.anyOf(
                    getKeywordAttributeFilter("A"), getKeywordAttributeFilter("B")),
                getKeywordAttributeFilter("C")),
            getKeywordAttributeFilter("D")));
    testKeywordFiler(
        "A AND B AND   C AND D  ",
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.allOf(
                    getKeywordAttributeFilter("A"), getKeywordAttributeFilter("B")),
                getKeywordAttributeFilter("C")),
            getKeywordAttributeFilter("D")));
    testKeywordFiler(
        "A AND (B AND C) AND D",
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.allOf(
                getKeywordAttributeFilter("A"),
                FILTER_BUILDER.allOf(
                    getKeywordAttributeFilter("B"), getKeywordAttributeFilter("C"))),
            getKeywordAttributeFilter("D")));
    testKeywordFiler(
        "A AND (\"B\" AND \"C\")",
        FILTER_BUILDER.allOf(
            getKeywordAttributeFilter("A"),
            FILTER_BUILDER.allOf(getKeywordAttributeFilter("B"), getKeywordAttributeFilter("C"))));
  }

  private void testKeywordFiler(String inputKeywordPhrase, Filter expectedFilter) {
    OpenSearchQuery osq = new OpenSearchQuery(0, 0, "relevance", "asc", 0, FILTER_BUILDER);

    LOGGER.info("Testing filter: {}", inputKeywordPhrase);
    osq.addContextualFilter(inputKeywordPhrase, null);

    assertEquals(
        "Incorrect Filter was produced for input filter string: " + inputKeywordPhrase + "\n\t",
        expectedFilter,
        osq.getFilter());
  }

  @Test
  public void testContextualFilterPhraseOnly() {
    String searchTerm = "cat";
    String selector = null;

    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addContextualFilter(searchTerm, selector);
    Filter filter = query.getFilter();

    // String filterXml = getFilterAsXml( filter );

    VerificationVisitor verificationVisitor = new VerificationVisitor();
    filter.accept(verificationVisitor, null);
    HashMap<String, FilterStatus> map =
        (HashMap<String, FilterStatus>) verificationVisitor.getMap();

    printFilterStatusMap(map);

    List<Filter> filters = getFilters(map, LikeFilterImpl.class.getName());
    assertEquals(1, filters.size());

    LikeFilterImpl likeFilter = (LikeFilterImpl) filters.get(0);
    String extractedSearchTerm = likeFilter.getLiteral();
    LOGGER.debug("extractedSearchTerm = [{}]", extractedSearchTerm);
    assertEquals(searchTerm, extractedSearchTerm);
  }

  @Test
  public void testContextualFilterPhraseAndSelector() {
    String searchTerm = "cat";
    String selector = "//fileTitle";

    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addContextualFilter(searchTerm, selector);
    Filter filter = query.getFilter();

    // String filterXml = getFilterAsXml( filter );

    VerificationVisitor verificationVisitor = new VerificationVisitor();
    filter.accept(verificationVisitor, null);
    HashMap<String, FilterStatus> map =
        (HashMap<String, FilterStatus>) verificationVisitor.getMap();

    printFilterStatusMap(map);

    List<Filter> filters = getFilters(map, LikeFilterImpl.class.getName());
    assertEquals(1, filters.size());

    String[] expectedXpathExpressions = selector.split(",");
    for (int i = 0; i < filters.size(); i++) {
      verifyContextualFilter(filters.get(i), expectedXpathExpressions[i], searchTerm);
    }
  }

  @Test
  public void testContextualFilterMultipleSelectors() {
    String searchTerm = "cat";
    String selectors = "//fileTitle,//nitf";

    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addContextualFilter(searchTerm, selectors);
    Filter filter = query.getFilter();

    // String filterXml = getFilterAsXml( filter );

    VerificationVisitor verificationVisitor = new VerificationVisitor();
    filter.accept(verificationVisitor, null);
    HashMap<String, FilterStatus> map =
        (HashMap<String, FilterStatus>) verificationVisitor.getMap();

    printFilterStatusMap(map);

    List<Filter> filters = getFilters(map, LikeFilterImpl.class.getName());
    assertEquals(2, filters.size());

    String[] expectedXpathExpressions = selectors.split(",");
    for (int i = 0; i < filters.size(); i++) {
      verifyContextualFilter(filters.get(i), expectedXpathExpressions[i], searchTerm);
    }
  }

  @Test
  public void testTemporalFilterModifiedSearch() {
    String dateOffset = "1800000"; // 30 minutes

    // create a filter here so we can grab the start/end dates to validate the filter added to the
    // query
    TemporalFilter temporalFilter = new TemporalFilter(Long.parseLong(dateOffset));

    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addOffsetTemporalFilter(dateOffset);
    Filter filter = query.getFilter();

    // String filterXml = getFilterAsXml( filter );

    VerificationVisitor verificationVisitor = new VerificationVisitor();
    filter.accept(verificationVisitor, null);
    HashMap<String, FilterStatus> map =
        (HashMap<String, FilterStatus>) verificationVisitor.getMap();

    printFilterStatusMap(map);

    List<Filter> filters = getFilters(map, DuringImpl.class.getName());
    assertEquals(1, filters.size());

    verifyTemporalFilter(
        filters.get(0),
        temporalFilter.getStartDate().toString(),
        temporalFilter.getEndDate().toString(),
        false);
  }

  @Test
  public void testTemporalFilterAbsoluteSearch() {
    String startDate = "2011-10-4T05:48:27.891-07:00";
    String endDate = "2011-10-4T06:18:27.581-07:00";

    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addStartEndTemporalFilter(startDate, endDate);
    Filter filter = query.getFilter();

    // String filterXml = getFilterAsXml( filter );

    VerificationVisitor verificationVisitor = new VerificationVisitor();
    filter.accept(verificationVisitor, null);
    HashMap<String, FilterStatus> map =
        (HashMap<String, FilterStatus>) verificationVisitor.getMap();

    printFilterStatusMap(map);

    List<Filter> filters = getFilters(map, DuringImpl.class.getName());
    assertEquals(1, filters.size());

    verifyTemporalFilter(filters.get(0), startDate, endDate);
  }

  @Test
  public void testTemporalFilterAbsoluteSearchStringDates() {
    String startDate = "2011-10-4T05:48:27.891-07:00";
    String endDate = "2011-10-4T06:18:27.581-07:00";

    TemporalFilter temporalFilter = new TemporalFilter(startDate, endDate);
    LOGGER.debug(temporalFilter.toString());

    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addStartEndTemporalFilter(startDate, endDate);
    Filter filter = query.getFilter();

    // String filterXml = getFilterAsXml( filter );

    VerificationVisitor verificationVisitor = new VerificationVisitor();
    filter.accept(verificationVisitor, null);
    HashMap<String, FilterStatus> map =
        (HashMap<String, FilterStatus>) verificationVisitor.getMap();

    printFilterStatusMap(map);

    List<Filter> filters = getFilters(map, DuringImpl.class.getName());
    assertEquals(1, filters.size());

    verifyTemporalFilter(filters.get(0), startDate, endDate);
  }

  @Test
  public void testContextualTemporalFilter() {
    String searchTerm = "cat";
    String selector = null;

    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addContextualFilter(searchTerm, selector);

    String startDate = "2011-10-4T05:48:27.891-07:00";
    String endDate = "2011-10-4T06:18:27.581-07:00";

    query.addStartEndTemporalFilter(startDate, endDate);
    Filter filter = query.getFilter();

    // String filterXml = getFilterAsXml( filter );

    VerificationVisitor verificationVisitor = new VerificationVisitor();
    filter.accept(verificationVisitor, null);
    HashMap<String, FilterStatus> map =
        (HashMap<String, FilterStatus>) verificationVisitor.getMap();

    List<Filter> andFilters = getFilters(map, AndImpl.class.getName());
    assertEquals(1, andFilters.size());

    AndImpl andFilter = (AndImpl) andFilters.get(0);
    List<Filter> childFilters = andFilter.getChildren();
    assertTrue(childFilters.size() == 2);

    verifyContextualFilter(childFilters.get(0), "anyText", searchTerm);
    verifyTemporalFilter(childFilters.get(1), startDate, endDate);
  }

  @Test
  public void testPolygonSpatialFilterWktConversion() {
    // WKT is lon/lat, polygon is lat/lon
    String expectedGeometryWkt = "POLYGON((10 0,30 0,30 20,10 20,10 0))";

    String latLon = "0,10,0,30,20,30,20,10,0,10";
    PolygonSpatialFilter term = new PolygonSpatialFilter(latLon);
    String geometryWkt = term.getGeometryWkt();
    LOGGER.debug("geometryWkt = {}", geometryWkt);

    assertEquals(expectedGeometryWkt, geometryWkt);
  }

  @Test
  public void testBboxSpatialFilterWktConversion() {
    // NOTE: BBoxSpatialFilter converts bbox corners to doubles, hence the double values
    // in this expected WKT string
    String expectedGeometryWkt = "POLYGON((0.0 10.0,0.0 30.0,20.0 30.0,20.0 10.0,0.0 10.0))";

    String bboxCorners = "0,10,20,30";
    BBoxSpatialFilter term = new BBoxSpatialFilter(bboxCorners);
    String geometryWkt = term.getGeometryWkt();
    LOGGER.debug("geometryWkt = {}", geometryWkt);

    assertEquals(expectedGeometryWkt, geometryWkt);
  }

  @Test
  public void testBboxSpatialFilter() {
    String bboxCorners = "0,10,20,30";

    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addBBoxSpatialFilter(bboxCorners);

    Filter filter = query.getFilter();

    // String filterXml = getFilterAsXml( filter );

    VerificationVisitor verificationVisitor = new VerificationVisitor();
    filter.accept(verificationVisitor, null);
    HashMap<String, FilterStatus> map =
        (HashMap<String, FilterStatus>) verificationVisitor.getMap();

    printFilterStatusMap(map);

    // List<Filter> filters = getFilters( map, ContainsImpl.class.getName() );
    List<Filter> filters = getFilters(map, IntersectsImpl.class.getName());
    assertEquals(1, filters.size());

    // ContainsImpl containsFilter = (ContainsImpl) filters.get( 0 );
    IntersectsImpl containsFilter = (IntersectsImpl) filters.get(0);

    // The geometric point is wrapped in a <Literal> element, so have to
    // get geometry expression as literal and then evaluate it to get the
    // geometry.
    // Example:
    // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl@64a7c45e</ogc:Literal>
    Literal literalWrapper = (Literal) containsFilter.getExpression2();

    // Luckily we know what type the geometry expression should be, so we can cast it
    SurfaceImpl bbox = (SurfaceImpl) literalWrapper.evaluate(null);

    String[] expectedCoords = bboxCorners.split(",");

    double[] lowerCornerCoords = bbox.getEnvelope().getLowerCorner().getCoordinate();
    LOGGER.debug(
        "lowerCornerCoords:  [0] = {},   [1] = {}", lowerCornerCoords[0], lowerCornerCoords[1]);
    assertEquals(Double.parseDouble(expectedCoords[0]), lowerCornerCoords[0], DOUBLE_DELTA);
    assertEquals(Double.parseDouble(expectedCoords[1]), lowerCornerCoords[1], DOUBLE_DELTA);

    double[] upperCornerCoords = bbox.getEnvelope().getUpperCorner().getCoordinate();
    LOGGER.debug(
        "upperCornerCoords:  [0] = {},   [1] = {}", upperCornerCoords[0], upperCornerCoords[1]);
    assertEquals(Double.parseDouble(expectedCoords[2]), upperCornerCoords[0], DOUBLE_DELTA);
    assertEquals(Double.parseDouble(expectedCoords[3]), upperCornerCoords[1], DOUBLE_DELTA);
  }

  @Test
  public void testSpatialDistanceFilter() {
    String lon = "10";
    String lat = "20";
    String radius = "5000";

    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addPointRadiusSpatialFilter(lon, lat, radius);
    Filter filter = query.getFilter();

    // String filterXml = getFilterAsXml( filter );

    VerificationVisitor verificationVisitor = new VerificationVisitor();
    filter.accept(verificationVisitor, null);
    HashMap<String, FilterStatus> map =
        (HashMap<String, FilterStatus>) verificationVisitor.getMap();

    List<Filter> filters = getFilters(map, DWithinImpl.class.getName());
    assertEquals(1, filters.size());

    DWithinImpl dwithinFilter = (DWithinImpl) filters.get(0);

    // The geometric point is wrapped in a <Literal> element, so have to
    // get geometry expression as literal and then evaluate it to get the
    // geometry.
    // Example:
    // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl@dc33f184</ogc:Literal>
    Literal literalWrapper = (Literal) dwithinFilter.getExpression2();

    // Luckily we know what type the geometry expression should be, so we can cast it
    PointImpl point = (PointImpl) literalWrapper.evaluate(null);
    double[] coords = point.getCentroid().getCoordinate();

    LOGGER.debug("coords[0] = {},   coords[1] = {}", coords[0], coords[1]);
    assertEquals(Double.parseDouble(lon), coords[0], DOUBLE_DELTA);
    assertEquals(Double.parseDouble(lat), coords[1], DOUBLE_DELTA);
    LOGGER.debug("dwithinFilter.getDistance() = {}", dwithinFilter.getDistance());
    assertEquals(Double.parseDouble(radius), dwithinFilter.getDistance(), DOUBLE_DELTA);
  }

  @Test
  public void testPolygonSpatialFilter() {
    String latLon = "0,10,0,30,20,30,20,10,0,10";
    String lonLat = "10,0,30,0,30,20,10,20,10,0";

    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addPolygonSpatialFilter(latLon);

    Filter filter = query.getFilter();

    // String filterXml = getFilterAsXml( filter );

    VerificationVisitor verificationVisitor = new VerificationVisitor();
    filter.accept(verificationVisitor, null);
    HashMap<String, FilterStatus> map =
        (HashMap<String, FilterStatus>) verificationVisitor.getMap();

    printFilterStatusMap(map);

    // List<Filter> filters = getFilters( map, ContainsImpl.class.getName() );
    List<Filter> filters = getFilters(map, IntersectsImpl.class.getName());
    assertEquals(1, filters.size());

    // ContainsImpl containsFilter = (ContainsImpl) filters.get( 0 );
    IntersectsImpl containsFilter = (IntersectsImpl) filters.get(0);

    // The geometric point is wrapped in a <Literal> element, so have to
    // get geometry expression as literal and then evaluate it to get the
    // geometry.
    // Example:
    // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl@64a7c45e</ogc:Literal>
    Literal literalWrapper = (Literal) containsFilter.getExpression2();

    // Luckily we know what type the geometry expression should be, so we can cast it
    SurfaceImpl polygon = (SurfaceImpl) literalWrapper.evaluate(null);

    // WKT is lon/lat, polygon is lat/lon
    String[] expectedCoords = lonLat.split(",");
    Coordinate[] coords = polygon.getJTSGeometry().getCoordinates();
    int i = 0;
    for (Coordinate coord : coords) {
      LOGGER.debug("coord {}: x = {},   y = {}", (i + 1), coord.x, coord.y);
      int index = i * 2 + 1;
      assertEquals(Double.parseDouble(expectedCoords[index - 1]), coord.x, DOUBLE_DELTA);
      assertEquals(Double.parseDouble(expectedCoords[index]), coord.y, DOUBLE_DELTA);
      i++;
    }
  }

  @Test
  public void testGeometrySpatialFilter() {
    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addGeometrySpatialFilter(GEOMETRY_WKT);
    Filter filter = query.getFilter();
    assertThat(filter, notNullValue());
    Intersects intersects = (Intersects) filter;
    Literal literalWrapper = (Literal) intersects.getExpression2();
    Object geometryExpression = literalWrapper.getValue();
    assertThat(geometryExpression, instanceOf(GeometryImpl.class));
    org.locationtech.jts.geom.Geometry polygon =
        ((GeometryImpl) geometryExpression).getJTSGeometry();
    assertThat(WKT_WRITER.write(polygon), is(GEOMETRY_WKT));
  }

  @Test
  public void testMultipleSpatialFilter() {
    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addGeometrySpatialFilter(GEOMETRY_WKT);
    query.addPolygonSpatialFilter(
        "30.943,-120.032,35.039,-120.032,35.039,-110.856,30.943,-110.856,30.943,-120.032");
    query.addBBoxSpatialFilter("-120.032,30.943,-110.856,35.039");
    query.addPointRadiusSpatialFilter("117.3425", "33.9283", "5000");
    query.addPolygonSpatialFilter("-30,100,-35,100,-35,110,-30,110,-30,100");

    Filter filter = query.getFilter();
    assertThat(filter, instanceOf(OrImpl.class));

    OrImpl topFilter = (OrImpl) filter;
    List<Filter> spatialFilters = topFilter.getChildren();
    assertThat(spatialFilters.size(), is(5));

    for (Filter spatialFilter : spatialFilters) {
      if (spatialFilter instanceof DWithinImpl) {
        assertThat(spatialFilter, notNullValue());
        DWithinImpl dWithin = (DWithinImpl) spatialFilter;
        assertThat(dWithin.getDistance(), is(5000.0));
        Literal literal = (Literal) dWithin.getExpression2();
        PointImpl point = (PointImpl) literal.getValue();
        String wkt = WKT_WRITER.write(point.getJTSGeometry());
        assertThat(wkt, is(POINT_WKT));
      } else if (spatialFilter instanceof IntersectsImpl) {
        assertThat(spatialFilter, notNullValue());
        IntersectsImpl intersects = (IntersectsImpl) spatialFilter;
        Literal literal = (Literal) intersects.getExpression2();
        Object geometryExpression = literal.getValue();
        if (geometryExpression instanceof SurfaceImpl) {
          SurfaceImpl surface = (SurfaceImpl) literal.getValue();
          String wkt = WKT_WRITER.write(surface.getJTSGeometry());
          assertThat(wkt, anyOf(is(POLYGON_WKT), is(POLYGON_WKT_2)));
        } else if (geometryExpression instanceof GeometryImpl) {
          org.locationtech.jts.geom.Geometry polygon =
              ((GeometryImpl) geometryExpression).getJTSGeometry();
          assertThat(WKT_WRITER.write(polygon), is(GEOMETRY_WKT));
        }
      }
    }
  }

  @Test
  public void testTypeFilterTypeOnly() {
    String type = "nitf";
    String versions = "";

    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addTypeFilter(type, versions);
    Filter filter = query.getFilter();

    // String filterXml = getFilterAsXml( filter );

    VerificationVisitor verificationVisitor = new VerificationVisitor();
    filter.accept(verificationVisitor, null);
    HashMap<String, FilterStatus> map =
        (HashMap<String, FilterStatus>) verificationVisitor.getMap();

    printFilterStatusMap(map);

    List<Filter> filters = getFilters(map, IsEqualsToImpl.class.getName());
    assertEquals(1, filters.size());

    verifyEqualsFilter(filters.get(0), "metadata-content-type", type);
  }

  @Test
  public void testTypeFilterWildcardTypeAndVersion() {
    String type = "*";
    String versions = "collectorPosition";

    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addTypeFilter(type, versions);
    Filter filter = query.getFilter();

    // String filterXml = getFilterAsXml( filter );

    VerificationVisitor verificationVisitor = new VerificationVisitor();
    filter.accept(verificationVisitor, null);
    HashMap<String, FilterStatus> map =
        (HashMap<String, FilterStatus>) verificationVisitor.getMap();

    printFilterStatusMap(map);

    String[] expectedVersions = versions.split(",");

    List<Filter> andFilters = getFilters(map, AndImpl.class.getName());
    assertEquals(expectedVersions.length, andFilters.size());

    List<Filter> equalsFilters = getFilters(map, IsEqualsToImpl.class.getName());
    assertEquals((expectedVersions.length), equalsFilters.size());

    List<Filter> likeFilters = getFilters(map, LikeFilterImpl.class.getName());
    assertEquals((expectedVersions.length), likeFilters.size());

    int i = 0;

    for (Filter f : andFilters) {
      List<Filter> childFilters = ((AndImpl) f).getChildren();
      verifyTypeVersionFilter(childFilters, type, expectedVersions[i]);
      i++;
    }
  }

  @Test
  public void testTypeFilterWildcardTypeAndMultipleVersions() {
    String type = "*";
    String versions = "v20,invalid_version,*";

    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addTypeFilter(type, versions);
    Filter filter = query.getFilter();

    // String filterXml = getFilterAsXml( filter );

    VerificationVisitor verificationVisitor = new VerificationVisitor();
    filter.accept(verificationVisitor, null);
    HashMap<String, FilterStatus> map =
        (HashMap<String, FilterStatus>) verificationVisitor.getMap();

    printFilterStatusMap(map);

    String[] expectedVersions = versions.split(",");

    List<Filter> orFilters = getFilters(map, OrImpl.class.getName());
    assertEquals(1, orFilters.size());

    List<Filter> andFilters = getFilters(map, AndImpl.class.getName());
    assertEquals(expectedVersions.length, andFilters.size());

    List<Filter> equalsFilters = getFilters(map, IsEqualsToImpl.class.getName());
    assertEquals(2, equalsFilters.size());

    List<Filter> likeFilters = getFilters(map, LikeFilterImpl.class.getName());
    assertEquals(4, likeFilters.size());

    int i = 0;

    for (Filter f : andFilters) {
      List<Filter> childFilters = ((AndImpl) f).getChildren();
      verifyTypeVersionFilter(childFilters, type, expectedVersions[i]);
      i++;
    }

    // Requires gt-jdbc.jar dependency in POM, but whn that JAR is loaded it causes
    // IncompatibleClassChangeError class loader issues in OpenSearchQuery.addTemporalFilter()
    // HUGH filter.accept( new FilterToSQL( new PrintWriter( System.out, true ) ), null );
  }

  @Test
  public void testWktParser() throws Exception {
    String geometryWkt = "POINT( 48.44 -123.37)";
    GeometryBuilder builder = new GeometryBuilder(DefaultGeographicCRS.WGS84);
    WKTParser parser = new WKTParser(builder);

    // This fixed the NPE in parser.parse() - seems GeoTools has bug with
    // keeping the CRS hint set ...
    parser.setFactory(new PrimitiveFactoryImpl(DefaultGeographicCRS.WGS84));

    Geometry geometry = parser.parse(geometryWkt);
    CoordinateReferenceSystem crs = geometry.getCoordinateReferenceSystem();
    assertNotNull(crs);

    String geometryWkt2 = "POINT( 48.44 -123.37)";
    builder = new GeometryBuilder(DefaultGeographicCRS.WGS84);
    WKTParser parser2 = new WKTParser(builder);
    Geometry geometry2 = parser2.parse(geometryWkt2);

    assertTrue(geometry2.intersects(geometry));
    double[] coords = geometry.getCentroid().getCoordinate();
    LOGGER.debug("coords[0] = {},   coords[1] = {}", coords[0], coords[1]);
  }

  @Test
  @Ignore
  public void testWktParserPolygon() throws Exception {
    String geometryWkt = "POLYGON(( 0 10, 0 30, 20 30, 20 10, 0 10 ))";
    GeometryBuilder builder = new GeometryBuilder(DefaultGeographicCRS.WGS84);
    WKTParser parser = new WKTParser(builder);

    // This fixed the NPE in parser.parse() - seems GeoTools has bug with
    // keeping the CRS hint set ...
    parser.setFactory(new PrimitiveFactoryImpl(DefaultGeographicCRS.WGS84));

    Geometry geometry = parser.parse(geometryWkt);
    CoordinateReferenceSystem crs = geometry.getCoordinateReferenceSystem();
    assertNotNull(crs);
    double[] coords = geometry.getCentroid().getCoordinate();
    LOGGER.debug("coords[0] = {},   coords[1] = {}", coords[0], coords[1]);

    // String geometryWkt2 = "POINT( 10 20 )";
    String geometryWkt2 = "POLYGON(( 10 15, 10 25, 15 25, 15 15, 10 15 ))";
    builder = new GeometryBuilder(DefaultGeographicCRS.WGS84);
    WKTParser parser2 = new WKTParser(builder);
    // This fixed the NPE in parser.parse() - seems GeoTools has bug with
    // keeping the CRS hint set ...
    parser2.setFactory(new PrimitiveFactoryImpl(DefaultGeographicCRS.WGS84));
    Geometry geometry2 = parser2.parse(geometryWkt2);
    double[] coords2 = geometry2.getCentroid().getCoordinate();
    LOGGER.debug("coords[0] = {},   coords[1] = {}", coords2[0], coords2[1]);

    // This fails - why?
    assertTrue(geometry.contains(geometry2));
  }

  @Test
  // @Ignore
  public void testOgcFilterEvaluateTemporalBetween() throws Exception {
    FilterFactory filterFactory = new FilterFactoryImpl();

    // get a calendar instance, which defaults to "now"
    Calendar calendar = Calendar.getInstance();

    // get a date to represent "today"
    Date now = calendar.getTime();

    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

    String dateInRange = dateFormatter.format(now);

    // set calendar time in past to create start date for temporal filter's criteria
    calendar.add(Calendar.DAY_OF_YEAR, -1);
    Date start = calendar.getTime();
    String startDate = dateFormatter.format(start);
    LOGGER.debug("startDate = {}", startDate);

    // set calendar time in future to create end date for temporal filter's criteria
    calendar.add(Calendar.DAY_OF_YEAR, +3);
    Date end = calendar.getTime();
    String endDate = dateFormatter.format(end);
    LOGGER.debug("endDate = {}", endDate);

    // Test date between start and end dates
    Filter filter =
        filterFactory.between(
            filterFactory.literal(dateInRange),
            filterFactory.literal(startDate),
            filterFactory.literal(endDate));

    FilterTransformer transform = new FilterTransformer();
    transform.setIndentation(2);
    LOGGER.debug(transform.transform(filter));

    boolean result = filter.evaluate(null);
    LOGGER.debug("result = {}", result);
    assertTrue(result);

    // Test date that is after end date
    calendar.add(Calendar.DAY_OF_YEAR, +3);
    Date outOfRange = calendar.getTime();
    String outOfRangeDate = dateFormatter.format(outOfRange);

    filter =
        filterFactory.between(
            filterFactory.literal(outOfRangeDate),
            filterFactory.literal(startDate),
            filterFactory.literal(endDate));
    LOGGER.debug(transform.transform(filter));

    result = filter.evaluate(null);
    LOGGER.debug("result = {}", result);
    assertFalse(result);

    // Test date that is before start date
    calendar.add(Calendar.DAY_OF_YEAR, -20);
    Date outOfRange2 = calendar.getTime();
    String outOfRangeDate2 = dateFormatter.format(outOfRange2);

    filter =
        filterFactory.between(
            filterFactory.literal(outOfRangeDate2),
            filterFactory.literal(startDate),
            filterFactory.literal(endDate));
    LOGGER.debug(transform.transform(filter));

    result = filter.evaluate(null);
    LOGGER.debug("result = {}", result);
    assertFalse(result);

    // Test date that is equal to start date
    filter =
        filterFactory.between(
            filterFactory.literal(startDate),
            filterFactory.literal(startDate),
            filterFactory.literal(endDate));
    LOGGER.debug(transform.transform(filter));

    result = filter.evaluate(null);
    LOGGER.debug("result = {}", result);
    assertTrue(result);

    // Test date that is equal to end date
    filter =
        filterFactory.between(
            filterFactory.literal(endDate),
            filterFactory.literal(startDate),
            filterFactory.literal(endDate));
    LOGGER.debug(transform.transform(filter));

    result = filter.evaluate(null);
    LOGGER.debug("result = {}", result);
    assertTrue(result);
  }

  @Test
  public void testCompoundFilter() {
    String searchTerm = "cat";
    String selectors = "//fileTitle,//nitf";
    OpenSearchQuery query = new OpenSearchQuery(0, 10, "relevance", "desc", 30000, FILTER_BUILDER);
    query.addContextualFilter(searchTerm, selectors);

    String startDate = "2011-10-4T05:48:27.891-07:00";
    String endDate = "2011-10-4T06:18:27.581-07:00";

    query.addStartEndTemporalFilter(startDate, endDate);

    String type = "nitf";
    String versions = "v20,invalid_version,*";
    query.addTypeFilter(type, versions);

    String lon = "10";
    String lat = "20";
    String radius = "5000";
    query.addPointRadiusSpatialFilter(lon, lat, radius);

    // Filter filter = query.getFilter();

    // String filterXml = getFilterAsXml( filter );
  }

  @Test
  @Ignore
  public void testOgcFilterEvaluateContextualLike() throws Exception {
    // String input = "abc_cat_dog_xyz";
    String input = "<ns1:thing xmlns:ns1=\"http://ddf.codice.org/mynamespace\">cat</ns1:thing>";
    // String searchTerm = "cat";
    // List<Filter> filters = new ArrayList<Filter>();
    FilterFactory filterFactory = new FilterFactoryImpl();
    // Filter filter = filterFactory.like( filterFactory.property( "AnyText" ), searchTerm );
    // Filter filter = filterFactory.equal( filterFactory.property( "thing" ),
    // filterFactory.literal( searchTerm ), false );
    // Filter filter = filterFactory.like( filterFactory.property( Query.ANY_TEXT ), searchTerm,
    // "*", "?", "\\" );
    // Filter filter = filterFactory.equal( filterFactory.property( Query.ANY_TEXT ),
    // searchTerm, false );
    // Filter filter = filterFactory.like( filterFactory.property( Query.ANY_TEXT ), searchTerm,
    // "*", "?", "\\" );
    Calendar.getInstance().getTime();
    String startDate = "2011-10-8T05:48:27.891-07:00";
    String endDate = "2011-10-10T06:18:27.581-07:00";

    TemporalFilter temporalFilter = new TemporalFilter(startDate, endDate);
    // WORKS Filter filter = filterFactory.between( filterFactory.literal( new Date() ),
    // filterFactory.literal( temporalFilter.getStartDate() ), filterFactory.literal(
    // temporalFilter.getEndDate() ) );
    Filter filter =
        filterFactory.between(
            filterFactory.literal(new Date()),
            filterFactory.literal(temporalFilter.getStartDate()),
            filterFactory.literal(temporalFilter.getEndDate()));

    FilterTransformer transform = new FilterTransformer();
    transform.setIndentation(2);
    LOGGER.debug(transform.transform(filter));
    boolean result = filter.evaluate(input);
    LOGGER.debug("result = {}", result);
    // filters.add( filter );
  }

  // private Document getDocument( String xml ) throws Exception
  // {
  // DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
  // factory.setNamespaceAware( true );
  // DocumentBuilder builder = factory.newDocumentBuilder();
  // StringReader reader = new StringReader( xml );
  // InputSource inputSource = new InputSource( reader );
  // Document doc = builder.parse( inputSource );
  // reader.close();
  //
  // return doc;
  // }

  // private String getFilterAsXml( Filter filter ) throws Exception
  // {
  // FilterTransformer transform = new FilterTransformer();
  // transform.setIndentation( 2 );
  // String filterXml = transform.transform( filter );
  // LOGGER.debug( filterXml );
  //
  // return filterXml;
  // }

  private void verifyContextualFilter(
      Filter filter, String expectedPropertyName, String expectedSearchTerm) {
    LikeFilterImpl likeFilter = (LikeFilterImpl) filter;

    AttributeExpressionImpl expression = (AttributeExpressionImpl) likeFilter.getExpression();
    LOGGER.debug("propertyName = {}", expression.getPropertyName());
    assertEquals(expectedPropertyName, expression.getPropertyName());

    String extractedSearchTerm = likeFilter.getLiteral();
    LOGGER.debug("extractedSearchTerm = [{}]", extractedSearchTerm);
    assertEquals(expectedSearchTerm, extractedSearchTerm);
  }

  private void verifyTemporalFilter(
      Filter filter, String expectedStartDate, String expectedEndDate) {
    verifyTemporalFilter(filter, expectedStartDate, expectedEndDate, true);
  }

  private void verifyTemporalFilter(
      Filter filter, String expectedStartDate, String expectedEndDate, boolean reformatDates) {
    DuringImpl duringFilter = (DuringImpl) filter;

    // The TOverlaps temporal range is wrapped in a <Literal> element, so have to
    // get expression as literal and then evaluate it to get the
    // temporal data.
    // Example:
    // <ogc:TOverlaps>
    // <ogc:PropertyName>modifiedDate</ogc:PropertyName>
    // <ogc:Literal>Period:
    // begin:Instant:
    // position:Position:
    // position:Tue Oct 04 05:48:27 MST 2011
    //
    //
    // end:Instant:
    // position:Position:
    // position:Tue Oct 04 06:18:27 MST 2011
    //
    //
    // </ogc:Literal>
    // </ogc:TOverlaps>

    Literal literalWrapper = (Literal) duringFilter.getExpression2();

    // Luckily we know what type the temporal expression should be, so we can cast it
    Period period = (Period) literalWrapper.evaluate(null);

    // Extract the start and end dates from the filter
    Date start = period.getBeginning().getPosition().getDate();
    Date end = period.getEnding().getPosition().getDate();

    if (reformatDates) {
      String formattedStartDate = reformatDate(start);
      String formattedEndDate = reformatDate(end);
      LOGGER.debug("startDate = {}", formattedStartDate);
      LOGGER.debug("endDate = {}", formattedEndDate);

      assertEquals(expectedStartDate, formattedStartDate);
      assertEquals(expectedEndDate, formattedEndDate);
    } else {
      assertEquals(expectedStartDate, start.toString());
      assertEquals(expectedEndDate, end.toString());
    }
  }

  private void verifyTypeVersionFilter(
      List<Filter> childFilters, String expectedType, String expectedVersion) {
    assertEquals(2, childFilters.size());

    if (expectedType.contains("*")) {
      verifyLikeFilter(childFilters.get(0), "metadata-content-type", expectedType);
    } else {
      verifyEqualsFilter(childFilters.get(0), "metadata-content-type", expectedType);
    }

    if (expectedVersion.contains("*")) {
      verifyLikeFilter(childFilters.get(1), "metadata-content-type-version", expectedVersion);
    } else {
      verifyEqualsFilter(childFilters.get(1), "metadata-content-type-version", expectedVersion);
    }
  }

  private void verifyLikeFilter(Filter filter, String expectedPropertyName, String expectedValue) {
    assertTrue(filter instanceof LikeFilterImpl);

    LikeFilterImpl likeFilter = (LikeFilterImpl) filter;
    AttributeExpressionImpl expression = (AttributeExpressionImpl) likeFilter.getExpression();
    LOGGER.debug("propertyName = {}", expression.getPropertyName());
    assertEquals(expectedPropertyName, expression.getPropertyName());

    String pattern = likeFilter.getLiteral();
    LOGGER.debug("value to search for = {}", pattern);
    assertEquals(expectedValue, pattern);
  }

  private void verifyEqualsFilter(
      Filter filter, String expectedPropertyName, String expectedValue) {
    assertTrue(filter instanceof IsEqualsToImpl);

    IsEqualsToImpl equalsFilter = (IsEqualsToImpl) filter;
    AttributeExpressionImpl expression1 = (AttributeExpressionImpl) equalsFilter.getExpression1();
    LOGGER.debug("propertyName = {}", expression1.getPropertyName());
    assertEquals(expectedPropertyName, expression1.getPropertyName());

    LiteralExpressionImpl expression2 = (LiteralExpressionImpl) equalsFilter.getExpression2();
    LOGGER.debug("version to search for = {}", expression2.getValue());
    assertEquals(expectedValue, expression2.getValue());
  }

  private List<Filter> getFilters(HashMap<String, FilterStatus> map, String filterClassName) {
    FilterStatus filterStatus = map.get(filterClassName);

    return filterStatus.getFilters();
  }

  private String reformatDate(Date date) {
    // Reformat date into original format that was used to create filter
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-d'T'HH:mm:ss.SSSZZ");
    dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT-07"));
    String formattedDate = dateFormatter.format(date);

    // Add colon in GMT offset, e.g., -07:00 vs. -0700
    StringBuilder sb = new StringBuilder(formattedDate);
    sb.insert(formattedDate.length() - 2, ":");
    formattedDate = sb.toString();

    LOGGER.debug("formattedDate = {}", formattedDate);

    return formattedDate;
  }

  private void printFilterStatusMap(HashMap<String, FilterStatus> map) {
    for (String key : map.keySet()) {
      LOGGER.debug("key = {}", key);
      FilterStatus fs = map.get(key);
      LOGGER.debug(fs.toString());
    }
  }
}
