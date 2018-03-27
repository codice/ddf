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
package ddf.catalog.source.opensearch.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.vividsolutions.jts.geom.Polygon;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.impl.filter.TemporalFilter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.codice.ddf.opensearch.OpenSearchConstants;
import org.exparity.hamcrest.date.DateMatchers;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.temporal.TOverlapsImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.TOverlaps;

public class OpenSearchFilterVisitorTest {

  private static final double WKT_LON = 1;

  private static final double WKT_LAT = 2;

  private static final String WKT_POINT = "POINT(" + WKT_LON + " " + WKT_LAT + ")";

  private static final String WKT_POLYGON =
      "POLYGON ((1.1 1.1, 1.1 2.1, 2.1 2.1, 2.1 1.1, 1.1 1.1))";

  private static final String TEST_STRING = "test";

  private static final String UI_WILDCARD = "%";

  private static final String WILDCARD = "*";

  private static final Date START_DATE = new Date(10000);

  private static final Date END_DATE = new Date(10005);

  private static final Date MINIMUM_DATE = new Date(0L);

  private static final Date MAXIMUM_DATE = new Date(Long.MAX_VALUE);

  private OpenSearchFilterVisitor openSearchFilterVisitor;

  private final GeotoolsFilterBuilder geotoolsFilterBuilder = new GeotoolsFilterBuilder();

  private static final String SOME_ATTRIBUTE_NAME = "this attribute name is ignored";

  private static final String ID_ATTRIBUTE_NAME = Core.ID;

  /**
   * The OpenSearch temporal parameters are based on {@value Core#MODIFIED} timestamps of the
   * records.
   */
  private static final String TEMPORAL_ATTRIBUTE_NAME = Core.MODIFIED;

  /**
   * The OpenSearch temporal parameters are based on {@value Metacard#ANY_GEO} values of the
   * records.
   */
  private static final String SPATIAL_ATTRIBUTE_NAME = Metacard.ANY_GEO;

  @Before
  public void setUp() {
    openSearchFilterVisitor = new OpenSearchFilterVisitor();
  }

  @Test
  public void testNotFilter() {
    Filter textLikeFilter =
        geotoolsFilterBuilder.attribute(SOME_ATTRIBUTE_NAME).is().like().text(TEST_STRING);
    Not notFilter = geotoolsFilterBuilder.not(textLikeFilter);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(notFilter, openSearchFilterVisitorObject);
    assertThat(openSearchFilterVisitorObject, is(result));
  }

  @Test
  public void testOrFilter() {
    Filter textLikeFilter =
        geotoolsFilterBuilder.attribute(SOME_ATTRIBUTE_NAME).is().like().text(TEST_STRING);
    Or orFilter = geotoolsFilterBuilder.anyOf(textLikeFilter, textLikeFilter);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(orFilter, openSearchFilterVisitorObject);
    assertThat(openSearchFilterVisitorObject, is(result));
  }

  @Test
  public void testAndFilter() {
    Filter textLikeFilter =
        geotoolsFilterBuilder.attribute(SOME_ATTRIBUTE_NAME).is().like().text(TEST_STRING);
    And andFilter = geotoolsFilterBuilder.allOf(textLikeFilter, textLikeFilter);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(andFilter, openSearchFilterVisitorObject);
    assertThat(openSearchFilterVisitorObject, is(result));
  }

  @Test
  public void testDWithin() {
    final double radius = 5;
    DWithin dWithinFilter =
        (DWithin)
            geotoolsFilterBuilder
                .attribute(SPATIAL_ATTRIBUTE_NAME)
                .is()
                .withinBuffer()
                .wkt(WKT_POINT, radius);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(dWithinFilter, openSearchFilterVisitorObject);
    assertThat(
        result.getPointRadiusSearch(),
        allOf(
            is(notNullValue()),
            hasProperty("lon", is(WKT_LON)),
            hasProperty("lat", is(WKT_LAT)),
            hasProperty("radius", is(radius))));
  }

  @Test
  public void testDWithinNullNest() {
    final double radius = 5;
    DWithin dWithinFilter =
        (DWithin)
            geotoolsFilterBuilder
                .attribute(SPATIAL_ATTRIBUTE_NAME)
                .is()
                .withinBuffer()
                .wkt(WKT_POINT, radius);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(dWithinFilter, openSearchFilterVisitorObject);
    assertThat(
        result.getPointRadiusSearch(),
        allOf(
            is(notNullValue()),
            hasProperty("lon", is(WKT_LON)),
            hasProperty("lat", is(WKT_LAT)),
            hasProperty("radius", is(radius))));
  }

  @Test
  public void testDWithinOrNest() {
    DWithin dWithinFilter =
        (DWithin)
            geotoolsFilterBuilder
                .attribute(SPATIAL_ATTRIBUTE_NAME)
                .is()
                .withinBuffer()
                .wkt(WKT_POINT, 5);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.OR);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(dWithinFilter, openSearchFilterVisitorObject);
    assertThat(result.getPointRadiusSearch(), is(nullValue()));
  }

  @Test
  public void testDWithinInvalidRadius() {
    DWithin dWithinFilter =
        (DWithin)
            geotoolsFilterBuilder
                .attribute(SPATIAL_ATTRIBUTE_NAME)
                .is()
                .withinBuffer()
                .wkt(WKT_POINT, 0);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(dWithinFilter, openSearchFilterVisitorObject);
    assertThat(result.getPointRadiusSearch(), is(nullValue()));
  }

  @Test
  public void testDWithinCqlFilter() throws CQLException {
    final double lon = 1.0;
    final double lat = 2.0;
    final double radius = 1;
    DWithin dWithinFilter =
        (DWithin)
            ECQL.toFilter(
                "(DWITHIN("
                    + SPATIAL_ATTRIBUTE_NAME
                    + ", "
                    + WKT_POINT
                    + ", "
                    + radius
                    + ", meters))");
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(dWithinFilter, openSearchFilterVisitorObject);
    assertThat(
        result.getPointRadiusSearch(),
        allOf(
            is(notNullValue()),
            hasProperty("lon", is(lon)),
            hasProperty("lat", is(lat)),
            hasProperty("radius", is(radius))));
  }

  @Test
  public void testContains() {
    Contains containsFilter =
        (Contains)
            geotoolsFilterBuilder.attribute(SPATIAL_ATTRIBUTE_NAME).containing().wkt(WKT_POLYGON);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(containsFilter, openSearchFilterVisitorObject);
    final Polygon polygon = result.getPolygonSearch();
    assertThat(polygon, is(notNullValue()));
    assertThat(polygon.toString(), is(WKT_POLYGON));
  }

  @Test
  public void testContainsNullNest() {
    Contains containsFilter =
        (Contains)
            geotoolsFilterBuilder.attribute(SPATIAL_ATTRIBUTE_NAME).containing().wkt(WKT_POLYGON);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(containsFilter, openSearchFilterVisitorObject);
    final Polygon polygon = result.getPolygonSearch();
    assertThat(polygon, is(notNullValue()));
    assertThat(polygon.toString(), is(WKT_POLYGON));
  }

  @Test
  public void testContainsOrNest() {
    Contains containsFilter =
        (Contains)
            geotoolsFilterBuilder.attribute(SPATIAL_ATTRIBUTE_NAME).containing().wkt(WKT_POLYGON);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.OR);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(containsFilter, openSearchFilterVisitorObject);
    assertThat(result.getPointRadiusSearch(), is(nullValue()));
  }

  @Test
  public void testContainsCqlFilter() throws CQLException {
    Contains containsFilter =
        (Contains) ECQL.toFilter("(CONTAINS(" + SPATIAL_ATTRIBUTE_NAME + ", " + WKT_POLYGON + "))");
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(containsFilter, openSearchFilterVisitorObject);
    final Polygon polygon = result.getPolygonSearch();
    assertThat(polygon, is(notNullValue()));
    assertThat(polygon.toString(), is(WKT_POLYGON));
  }

  @Test
  public void testContainsWithPoint() {
    Contains containsFilter =
        (Contains)
            geotoolsFilterBuilder.attribute(SPATIAL_ATTRIBUTE_NAME).containing().wkt(WKT_POINT);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(containsFilter, openSearchFilterVisitorObject);
    assertThat(result.getPointRadiusSearch(), is(nullValue()));
  }

  @Test
  public void testIntersects() {
    Intersects intersectsFilter =
        (Intersects)
            geotoolsFilterBuilder.attribute(SPATIAL_ATTRIBUTE_NAME).intersecting().wkt(WKT_POLYGON);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(intersectsFilter, openSearchFilterVisitorObject);
    final Polygon polygon = result.getPolygonSearch();
    assertThat(polygon, is(notNullValue()));
    assertThat(polygon.toString(), is(WKT_POLYGON));
  }

  @Test
  public void testIntersectsWithPoint() {
    Intersects intersectsFilter =
        (Intersects)
            geotoolsFilterBuilder.attribute(SPATIAL_ATTRIBUTE_NAME).intersecting().wkt(WKT_POINT);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(intersectsFilter, openSearchFilterVisitorObject);
    assertThat(result.getPointRadiusSearch(), is(nullValue()));
  }

  @Test
  public void testIntersectsNullNest() {
    Intersects intersectsFilter =
        (Intersects)
            geotoolsFilterBuilder.attribute(SPATIAL_ATTRIBUTE_NAME).intersecting().wkt(WKT_POLYGON);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(intersectsFilter, openSearchFilterVisitorObject);
    final Polygon polygon = result.getPolygonSearch();
    assertThat(polygon, is(notNullValue()));
    assertThat(polygon.toString(), is(WKT_POLYGON));
  }

  @Test
  public void testIntersectsOrNest() {
    Intersects intersectsFilter =
        (Intersects)
            geotoolsFilterBuilder.attribute(SPATIAL_ATTRIBUTE_NAME).intersecting().wkt(WKT_POINT);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.OR);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(intersectsFilter, openSearchFilterVisitorObject);
    assertThat(result.getPointRadiusSearch(), is(nullValue()));
  }

  @Test
  public void testIntersectsCqlFilter() throws CQLException {
    Intersects intersectsFilter =
        (Intersects)
            ECQL.toFilter("(INTERSECTS(" + SPATIAL_ATTRIBUTE_NAME + ", " + WKT_POLYGON + "))");
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(intersectsFilter, openSearchFilterVisitorObject);
    final Polygon polygon = result.getPolygonSearch();
    assertThat(polygon, is(notNullValue()));
    assertThat(polygon.toString(), is(WKT_POLYGON));
  }

  @Test
  public void testOverlaps() {
    During duringFilter =
        (During)
            geotoolsFilterBuilder
                .attribute(TEMPORAL_ATTRIBUTE_NAME)
                .during()
                .dates(START_DATE, END_DATE);
    TOverlaps overlapsFilter =
        new TOverlapsImpl(duringFilter.getExpression1(), duringFilter.getExpression2());
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(overlapsFilter, openSearchFilterVisitorObject);
    assertThat(
        result.getTemporalSearch(),
        allOf(
            is(notNullValue()),
            hasProperty("startDate", is(START_DATE)),
            hasProperty("endDate", is(END_DATE))));
  }

  @Test
  public void testOverlapsInstant() {
    After afterFilter =
        (After) geotoolsFilterBuilder.attribute(TEMPORAL_ATTRIBUTE_NAME).after().date(START_DATE);
    TOverlaps overlapsFilter =
        new TOverlapsImpl(afterFilter.getExpression1(), afterFilter.getExpression2());
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(overlapsFilter, openSearchFilterVisitorObject);
    assertThat(
        result.getTemporalSearch(),
        allOf(
            is(notNullValue()),
            hasProperty("startDate", is(START_DATE)),
            hasProperty("endDate", is(START_DATE))));
  }

  @Test
  public void testOverlapsNullNest() {
    During duringFilter =
        (During)
            geotoolsFilterBuilder
                .attribute(TEMPORAL_ATTRIBUTE_NAME)
                .during()
                .dates(START_DATE, END_DATE);
    TOverlaps overlapsFilter =
        new TOverlapsImpl(duringFilter.getExpression1(), duringFilter.getExpression2());
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(overlapsFilter, openSearchFilterVisitorObject);
    assertThat(
        result.getTemporalSearch(),
        allOf(
            is(notNullValue()),
            hasProperty("startDate", is(START_DATE)),
            hasProperty("endDate", is(END_DATE))));
  }

  @Test
  public void testOverlapsOrNest() {
    During duringFilter =
        (During)
            geotoolsFilterBuilder
                .attribute(TEMPORAL_ATTRIBUTE_NAME)
                .during()
                .dates(START_DATE, END_DATE);
    TOverlaps overlapsFilter =
        new TOverlapsImpl(duringFilter.getExpression1(), duringFilter.getExpression2());
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.OR);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(overlapsFilter, openSearchFilterVisitorObject);
    assertThat(result.getTemporalSearch(), is(nullValue()));
  }

  @Test
  public void testDuringDates() {
    During duringFilter =
        (During)
            geotoolsFilterBuilder
                .attribute(TEMPORAL_ATTRIBUTE_NAME)
                .during()
                .dates(START_DATE, END_DATE);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(duringFilter, openSearchFilterVisitorObject);
    assertThat(
        result.getTemporalSearch(),
        allOf(
            is(notNullValue()),
            hasProperty("startDate", is(START_DATE)),
            hasProperty("endDate", is(END_DATE))));
  }

  @Test
  public void testDuringNullNest() {
    During duringFilter =
        (During)
            geotoolsFilterBuilder
                .attribute(TEMPORAL_ATTRIBUTE_NAME)
                .during()
                .dates(START_DATE, END_DATE);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(duringFilter, openSearchFilterVisitorObject);
    assertThat(
        result.getTemporalSearch(),
        allOf(
            is(notNullValue()),
            hasProperty("startDate", is(START_DATE)),
            hasProperty("endDate", is(END_DATE))));
  }

  @Test
  public void testDuringOrNest() {
    During duringFilter =
        (During)
            geotoolsFilterBuilder
                .attribute(TEMPORAL_ATTRIBUTE_NAME)
                .during()
                .dates(START_DATE, END_DATE);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.OR);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(duringFilter, openSearchFilterVisitorObject);
    assertThat(result.getTemporalSearch(), is(nullValue()));
  }

  @Test
  public void testDuringLast() {
    long durationInMilliSeconds = TimeUnit.DAYS.toMillis(10);
    During duringFilter =
        (During)
            geotoolsFilterBuilder
                .attribute(TEMPORAL_ATTRIBUTE_NAME)
                .during()
                .last(durationInMilliSeconds);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    Date currentDate = Calendar.getInstance().getTime();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(duringFilter, openSearchFilterVisitorObject);
    TemporalFilter temporalSearch = result.getTemporalSearch();
    assertThat(temporalSearch, is(notNullValue()));
    Date endDate = temporalSearch.getEndDate();
    assertThat(
        "end date should be the current date",
        endDate,
        DateMatchers.within(10, ChronoUnit.SECONDS, currentDate));
    assertThat(
        "start date should be before the end date",
        temporalSearch.getStartDate(),
        DateMatchers.sameInstant(endDate.getTime() - durationInMilliSeconds));
  }

  @Test
  public void testAfter() {
    After afterFilter =
        (After) geotoolsFilterBuilder.attribute(TEMPORAL_ATTRIBUTE_NAME).after().date(START_DATE);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(afterFilter, openSearchFilterVisitorObject);
    assertThat(
        result.getTemporalSearch(),
        allOf(
            is(notNullValue()),
            hasProperty("startDate", is(START_DATE)),
            hasProperty("endDate", is(MAXIMUM_DATE))));
  }

  @Test
  public void testBefore() {
    org.opengis.filter.temporal.Before beforeFilter =
        (org.opengis.filter.temporal.Before)
            geotoolsFilterBuilder.attribute(TEMPORAL_ATTRIBUTE_NAME).before().date(END_DATE);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(beforeFilter, openSearchFilterVisitorObject);
    assertThat(
        result.getTemporalSearch(),
        allOf(
            is(notNullValue()),
            hasProperty("startDate", is(MINIMUM_DATE)),
            hasProperty("endDate", is(END_DATE))));
  }

  @Test
  public void testPropertyIsLike() {
    PropertyIsLike textLikeFilter =
        (PropertyIsLike)
            geotoolsFilterBuilder.attribute(SOME_ATTRIBUTE_NAME).is().like().text(TEST_STRING);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(textLikeFilter, openSearchFilterVisitorObject);
    assertThat(
        result.getContextualSearch(),
        allOf(
            is(notNullValue()),
            hasProperty(
                "searchPhraseMap", hasEntry(OpenSearchConstants.SEARCH_TERMS, TEST_STRING))));
  }

  @Test
  public void testPropertyLikeWildcard() {
    FilterFactory2 factory = new FilterFactoryImpl();
    PropertyIsLike textLikeFilter =
        factory.like(factory.property(Core.TITLE), UI_WILDCARD, UI_WILDCARD, "'", "_", false);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(textLikeFilter, openSearchFilterVisitorObject);
    assertThat(
        result.getContextualSearch(),
        allOf(
            is(notNullValue()),
            hasProperty("searchPhraseMap", hasEntry(OpenSearchConstants.SEARCH_TERMS, WILDCARD))));
  }

  @Test
  public void testPropertyIsLikeAnd() {
    PropertyIsLike textLikeFilter =
        (PropertyIsLike)
            geotoolsFilterBuilder.attribute(SOME_ATTRIBUTE_NAME).is().like().text(TEST_STRING);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitor.visit(textLikeFilter, openSearchFilterVisitorObject);
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(textLikeFilter, openSearchFilterVisitorObject);
    assertThat(
        result.getContextualSearch(),
        allOf(
            is(notNullValue()),
            hasProperty(
                "searchPhraseMap", hasEntry(OpenSearchConstants.SEARCH_TERMS, "test AND test"))));
  }

  @Test
  public void testPropertyIsLikeNonSearchTerm() {
    PropertyIsLike textLikeFilter =
        (PropertyIsLike)
            geotoolsFilterBuilder.attribute(SOME_ATTRIBUTE_NAME).is().like().text(TEST_STRING);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    Map<String, String> searchPhraseMap = new HashMap<>();
    searchPhraseMap.put("anotherTerm", TEST_STRING);
    openSearchFilterVisitorObject.setContextualSearch(
        new ContextualSearch(SOME_ATTRIBUTE_NAME, searchPhraseMap, true));
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(textLikeFilter, openSearchFilterVisitorObject);
    assertThat(
        result.getContextualSearch(),
        allOf(
            is(notNullValue()),
            hasProperty(
                "searchPhraseMap", hasEntry(OpenSearchConstants.SEARCH_TERMS, TEST_STRING))));
  }

  @Test
  public void testPropertyEqualTo() {
    PropertyIsEqualTo propertyIsEqualToFilter =
        (PropertyIsEqualTo)
            geotoolsFilterBuilder.attribute(ID_ATTRIBUTE_NAME).is().equalTo().text(TEST_STRING);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(propertyIsEqualToFilter, openSearchFilterVisitorObject);
    assertThat(result.getId(), is(TEST_STRING));
  }

  @Test
  public void testPropertyEqualToNotNest() {
    PropertyIsEqualTo propertyIsEqualToFilter =
        (PropertyIsEqualTo)
            geotoolsFilterBuilder.attribute(ID_ATTRIBUTE_NAME).is().equalTo().text(TEST_STRING);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.NOT);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(propertyIsEqualToFilter, openSearchFilterVisitorObject);
    assertThat(result.getId(), is(nullValue()));
  }

  @Test
  public void testPropertyEqualToNotIdAttribute() {
    PropertyIsEqualTo propertyIsEqualToFilter =
        (PropertyIsEqualTo)
            geotoolsFilterBuilder.attribute("not id attribute").is().equalTo().text(TEST_STRING);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(propertyIsEqualToFilter, openSearchFilterVisitorObject);
    assertThat(result.getId(), is(nullValue()));
  }

  /**
   * Temporal {@link Filter}s for attributes other than {@value TEMPORAL_ATTRIBUTE_NAME} should be
   * ignored.
   */
  @Test
  public void testNotModifiedTemporalFilter() {
    During duringFilter =
        (During)
            geotoolsFilterBuilder
                .attribute(SOME_ATTRIBUTE_NAME)
                .during()
                .dates(START_DATE, END_DATE);

    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(duringFilter, openSearchFilterVisitorObject);

    assertThat(result.getTemporalSearch(), is(nullValue()));
  }

  /**
   * Spatial {@link Filter}s for attributes other than {@value SPATIAL_ATTRIBUTE_NAME} should be
   * ignored.
   */
  @Test
  public void testNotLocationSpatialFilter() {
    Contains containsFilter =
        (Contains)
            geotoolsFilterBuilder.attribute(SOME_ATTRIBUTE_NAME).containing().wkt(WKT_POLYGON);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(containsFilter, openSearchFilterVisitorObject);

    assertThat(result.getPolygonSearch(), is(nullValue()));
  }

  /**
   * Test that the {@link OpenSearchFilterVisitorObject} is populated with multiple filters.
   * Combines the {@link Filter}s from {@link #testDuringDates} and {@link #testContains()}.
   */
  @Test
  public void testMultipleFilters() {
    During duringFilter =
        (During)
            geotoolsFilterBuilder
                .attribute(TEMPORAL_ATTRIBUTE_NAME)
                .during()
                .dates(START_DATE, END_DATE);

    Contains containsFilter =
        (Contains)
            geotoolsFilterBuilder.attribute(SPATIAL_ATTRIBUTE_NAME).containing().wkt(WKT_POLYGON);

    And andFilter = geotoolsFilterBuilder.allOf(duringFilter, containsFilter);

    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(andFilter, openSearchFilterVisitorObject);

    assertThat(
        result.getTemporalSearch(),
        allOf(
            is(notNullValue()),
            hasProperty("startDate", is(START_DATE)),
            hasProperty("endDate", is(END_DATE))));

    final Polygon polygon = result.getPolygonSearch();
    assertThat(polygon, is(notNullValue()));
    assertThat(polygon.toString(), is(WKT_POLYGON));
  }

  /**
   * Test that other {@link Filter}s are still visited when a {@link Filter} is ignored. The {@link
   * Filter}s in this test are the same as {@link #testMultipleFilters} except that the temporal
   * criteria is from {@link #testNotModifiedTemporalFilter()}.
   */
  @Test
  public void testMultipleFiltersWhereOneFilterIsIgnored() {
    During duringFilter =
        (During)
            geotoolsFilterBuilder
                .attribute(SOME_ATTRIBUTE_NAME)
                .during()
                .dates(START_DATE, END_DATE);

    Contains containsFilter =
        (Contains)
            geotoolsFilterBuilder.attribute(SPATIAL_ATTRIBUTE_NAME).containing().wkt(WKT_POLYGON);

    And andFilter = geotoolsFilterBuilder.allOf(duringFilter, containsFilter);

    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(andFilter, openSearchFilterVisitorObject);

    assertThat(result.getTemporalSearch(), is(nullValue()));

    final Polygon polygon = result.getPolygonSearch();
    assertThat(polygon, is(notNullValue()));
    assertThat(polygon.toString(), is(WKT_POLYGON));
  }
}
