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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import ddf.catalog.data.types.Core;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.geotools.filter.temporal.TOverlapsImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
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

  private static final String WKT_POINT = "POINT(1.0 2.0)";

  private static final String WKT_POLYGON =
      "POLYGON ((1.1 1.1, 1.1 2.1, 2.1 2.1, 2.1 1.1, 1.1 1.1))";

  private static final String CQL_DWITHIN = "(DWITHIN(anyGeo, " + WKT_POINT + ", 1, meters))";

  private static final String CQL_CONTAINS = "(CONTAINS(anyGeo, " + WKT_POLYGON + "))";

  private static final String CQL_INTERSECTS = "(INTERSECTS(anyGeo, " + WKT_POLYGON + "))";

  private static final String TEST_STRING = "test";

  private static final Date START_DATE = new Date(10000);

  private static final Date END_DATE = new Date(10005);

  private OpenSearchFilterVisitor openSearchFilterVisitor;

  private GeotoolsFilterBuilder geotoolsFilterBuilder = new GeotoolsFilterBuilder();

  @Before
  public void setUp() {
    openSearchFilterVisitor = new OpenSearchFilterVisitor();
  }

  @Test
  public void testNotFilter() {
    Filter textLikeFilter =
        geotoolsFilterBuilder.attribute(Core.TITLE).is().like().text(TEST_STRING);
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
        geotoolsFilterBuilder.attribute(Core.TITLE).is().like().text(TEST_STRING);
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
        geotoolsFilterBuilder.attribute(Core.TITLE).is().like().text(TEST_STRING);
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
    DWithin dWithinFilter =
        (DWithin) geotoolsFilterBuilder.attribute(Core.LOCATION).is().withinBuffer().wkt(WKT_POINT);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(dWithinFilter, openSearchFilterVisitorObject);
    SpatialFilter spatialFilter = result.getSpatialSearch();
    assertThat(spatialFilter, notNullValue());
    assertThat(spatialFilter.getGeometryWkt(), is(WKT_POINT));
  }

  @Test
  public void testDWithinNullNest() {
    DWithin dWithinFilter =
        (DWithin) geotoolsFilterBuilder.attribute(Core.LOCATION).is().withinBuffer().wkt(WKT_POINT);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(dWithinFilter, openSearchFilterVisitorObject);
    SpatialFilter spatialFilter = result.getSpatialSearch();
    assertThat(spatialFilter, notNullValue());
    assertThat(spatialFilter.getGeometryWkt(), is(WKT_POINT));
  }

  @Test
  public void testDWithinOrNest() {
    DWithin dWithinFilter =
        (DWithin) geotoolsFilterBuilder.attribute(Core.LOCATION).is().withinBuffer().wkt(WKT_POINT);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.OR);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(dWithinFilter, openSearchFilterVisitorObject);
    SpatialFilter spatialFilter = result.getSpatialSearch();
    assertThat(spatialFilter, nullValue());
  }

  @Test
  public void testDWithinCqlFilter() throws CQLException {
    DWithin dWithinFilter = (DWithin) ECQL.toFilter(CQL_DWITHIN);

    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(dWithinFilter, openSearchFilterVisitorObject);
    SpatialFilter spatialFilter = result.getSpatialSearch();
    assertThat(spatialFilter, notNullValue());
    assertThat(spatialFilter.getGeometryWkt(), is(WKT_POINT));
  }

  @Test
  public void testContains() {
    Contains containsFilter =
        (Contains) geotoolsFilterBuilder.attribute(Core.TITLE).containing().wkt(WKT_POLYGON);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(containsFilter, openSearchFilterVisitorObject);
    SpatialFilter spatialFilter = result.getSpatialSearch();
    assertThat(spatialFilter, notNullValue());
    assertThat(spatialFilter.getGeometryWkt(), is(WKT_POLYGON));
  }

  @Test
  public void testContainsNullNest() {
    Contains containsFilter =
        (Contains) geotoolsFilterBuilder.attribute(Core.TITLE).containing().wkt(WKT_POLYGON);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(containsFilter, openSearchFilterVisitorObject);
    SpatialFilter spatialFilter = result.getSpatialSearch();
    assertThat(spatialFilter, notNullValue());
    assertThat(spatialFilter.getGeometryWkt(), is(WKT_POLYGON));
  }

  @Test
  public void testContainsOrNest() {
    Contains containsFilter =
        (Contains) geotoolsFilterBuilder.attribute(Core.TITLE).containing().wkt(WKT_POLYGON);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.OR);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(containsFilter, openSearchFilterVisitorObject);
    SpatialFilter spatialFilter = result.getSpatialSearch();
    assertThat(spatialFilter, nullValue());
  }

  @Test
  public void testContainsCqlFilter() throws CQLException {
    Contains containsFilter = (Contains) ECQL.toFilter(CQL_CONTAINS);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(containsFilter, openSearchFilterVisitorObject);
    SpatialFilter spatialFilter = result.getSpatialSearch();
    assertThat(spatialFilter, notNullValue());
    assertThat(spatialFilter.getGeometryWkt(), is(WKT_POLYGON));
  }

  @Test
  public void testContainsWithPoint() {
    Contains containsFilter =
        (Contains) geotoolsFilterBuilder.attribute(Core.TITLE).containing().wkt(WKT_POINT);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(containsFilter, openSearchFilterVisitorObject);
    SpatialFilter spatialFilter = result.getSpatialSearch();
    assertThat(spatialFilter, nullValue());
  }

  @Test
  public void testIntersects() {
    Intersects intersectsFilter =
        (Intersects) geotoolsFilterBuilder.attribute(Core.TITLE).intersecting().wkt(WKT_POLYGON);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(intersectsFilter, openSearchFilterVisitorObject);
    SpatialFilter spatialFilter = result.getSpatialSearch();
    assertThat(spatialFilter, notNullValue());
    assertThat(spatialFilter.getGeometryWkt(), is(WKT_POLYGON));
  }

  @Test
  public void testIntersectsWithPoint() {
    Intersects intersectsFilter =
        (Intersects) geotoolsFilterBuilder.attribute(Core.TITLE).intersecting().wkt(WKT_POINT);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(intersectsFilter, openSearchFilterVisitorObject);
    SpatialFilter spatialFilter = result.getSpatialSearch();
    assertThat(spatialFilter, nullValue());
  }

  @Test
  public void testIntersectsNullNest() {
    Intersects intersectsFilter =
        (Intersects) geotoolsFilterBuilder.attribute(Core.TITLE).intersecting().wkt(WKT_POLYGON);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(intersectsFilter, openSearchFilterVisitorObject);
    SpatialFilter spatialFilter = result.getSpatialSearch();
    assertThat(spatialFilter, notNullValue());
    assertThat(spatialFilter.getGeometryWkt(), is(WKT_POLYGON));
  }

  @Test
  public void testIntersectsOrNest() {
    Intersects intersectsFilter =
        (Intersects) geotoolsFilterBuilder.attribute(Core.TITLE).intersecting().wkt(WKT_POINT);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.OR);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(intersectsFilter, openSearchFilterVisitorObject);
    SpatialFilter spatialFilter = result.getSpatialSearch();
    assertThat(spatialFilter, nullValue());
  }

  @Test
  public void testIntersectsCqlFilter() throws CQLException {
    Intersects intersectsFilter = (Intersects) ECQL.toFilter(CQL_INTERSECTS);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(intersectsFilter, openSearchFilterVisitorObject);
    SpatialFilter spatialFilter = result.getSpatialSearch();
    assertThat(spatialFilter, notNullValue());
    assertThat(spatialFilter.getGeometryWkt(), is(WKT_POLYGON));
  }

  @Test
  public void testOverlaps() {
    During during =
        (During) geotoolsFilterBuilder.attribute(Core.TITLE).during().dates(START_DATE, END_DATE);
    TOverlaps overlapsFilter = new TOverlapsImpl(during.getExpression1(), during.getExpression2());
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(overlapsFilter, openSearchFilterVisitorObject);
    TemporalFilter temporalFilter = result.getTemporalSearch();
    assertThat(temporalFilter, notNullValue());
    assertThat(temporalFilter.getStartDate(), is(START_DATE));
    assertThat(temporalFilter.getEndDate(), is(END_DATE));
  }

  @Test
  public void testOverlapsInstant() {
    After after = (After) geotoolsFilterBuilder.attribute(Core.TITLE).after().date(new Date());
    TOverlaps overlapsFilter = new TOverlapsImpl(after.getExpression1(), after.getExpression2());
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(overlapsFilter, openSearchFilterVisitorObject);
    TemporalFilter temporalFilter = result.getTemporalSearch();
    assertThat(temporalFilter, nullValue());
  }

  @Test
  public void testOverlapsNullNest() {
    During during =
        (During) geotoolsFilterBuilder.attribute(Core.TITLE).during().dates(START_DATE, END_DATE);
    TOverlaps overlapsFilter = new TOverlapsImpl(during.getExpression1(), during.getExpression2());
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(overlapsFilter, openSearchFilterVisitorObject);
    TemporalFilter temporalFilter = result.getTemporalSearch();
    assertThat(temporalFilter, notNullValue());
    assertThat(temporalFilter.getStartDate(), is(START_DATE));
    assertThat(temporalFilter.getEndDate(), is(END_DATE));
  }

  @Test
  public void testOverlapsOrNest() {
    During during =
        (During) geotoolsFilterBuilder.attribute(Core.TITLE).during().dates(START_DATE, END_DATE);
    TOverlaps overlapsFilter = new TOverlapsImpl(during.getExpression1(), during.getExpression2());
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.OR);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(overlapsFilter, openSearchFilterVisitorObject);
    TemporalFilter temporalFilter = result.getTemporalSearch();
    assertThat(temporalFilter, nullValue());
  }

  @Test
  public void testDuring() {
    During during =
        (During) geotoolsFilterBuilder.attribute(Core.TITLE).during().dates(START_DATE, END_DATE);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(during, openSearchFilterVisitorObject);
    TemporalFilter temporalSearch = result.getTemporalSearch();
    assertThat(temporalSearch, notNullValue());
    assertThat(temporalSearch.getStartDate(), is(START_DATE));
    assertThat(temporalSearch.getEndDate(), is(END_DATE));
  }

  @Test
  public void testDuringNullNest() {
    During during =
        (During) geotoolsFilterBuilder.attribute(Core.TITLE).during().dates(START_DATE, END_DATE);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(during, openSearchFilterVisitorObject);
    TemporalFilter temporalSearch = result.getTemporalSearch();
    assertThat(temporalSearch, notNullValue());
    assertThat(temporalSearch.getStartDate(), is(START_DATE));
    assertThat(temporalSearch.getEndDate(), is(END_DATE));
  }

  @Test
  public void testDuringOrNest() {
    During overlaps =
        (During) geotoolsFilterBuilder.attribute(Core.TITLE).during().dates(START_DATE, END_DATE);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.OR);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(overlaps, openSearchFilterVisitorObject);
    TemporalFilter temporalSearch = result.getTemporalSearch();
    assertThat(temporalSearch, nullValue());
  }

  @Test
  public void testPropertyIsLike() {
    PropertyIsLike textLikeFilter =
        (PropertyIsLike) geotoolsFilterBuilder.attribute(Core.TITLE).is().like().text(TEST_STRING);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(textLikeFilter, openSearchFilterVisitorObject);

    ContextualSearch contextualSearch = result.getContextualSearch();
    Map<String, String> searchPhraseMap = contextualSearch.getSearchPhraseMap();
    assertThat(searchPhraseMap.containsKey(OpenSearchParserImpl.SEARCH_TERMS), is(true));
    String contextualSearchTerm = searchPhraseMap.get(OpenSearchParserImpl.SEARCH_TERMS);
    assertThat(contextualSearchTerm, is(TEST_STRING));
  }

  @Test
  public void testPropertyIsLikeAnd() {
    PropertyIsLike textLikeFilter =
        (PropertyIsLike) geotoolsFilterBuilder.attribute(Core.TITLE).is().like().text(TEST_STRING);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitor.visit(textLikeFilter, openSearchFilterVisitorObject);
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(textLikeFilter, openSearchFilterVisitorObject);
    ContextualSearch contextualSearch = result.getContextualSearch();
    Map<String, String> searchPhraseMap = contextualSearch.getSearchPhraseMap();
    assertThat(searchPhraseMap.containsKey(OpenSearchParserImpl.SEARCH_TERMS), is(true));
    String contextualSearchTerm = searchPhraseMap.get(OpenSearchParserImpl.SEARCH_TERMS);
    assertThat(contextualSearchTerm, is("test AND test"));
  }

  @Test
  public void testPropertyIsLikeNonSearchTerm() {
    PropertyIsLike textLikeFilter =
        (PropertyIsLike) geotoolsFilterBuilder.attribute(Core.TITLE).is().like().text(TEST_STRING);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    Map<String, String> searchPhraseMap = new HashMap<>();
    searchPhraseMap.put("anotherTerm", TEST_STRING);
    openSearchFilterVisitorObject.setContextualSearch(
        new ContextualSearch(Core.TITLE, searchPhraseMap, true));

    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(textLikeFilter, openSearchFilterVisitorObject);
    ContextualSearch contextualSearch = result.getContextualSearch();
    searchPhraseMap = contextualSearch.getSearchPhraseMap();
    assertThat(searchPhraseMap.containsKey(OpenSearchParserImpl.SEARCH_TERMS), is(true));
    String contextualSearchTerm = searchPhraseMap.get(OpenSearchParserImpl.SEARCH_TERMS);
    assertThat(contextualSearchTerm, is(TEST_STRING));
  }

  @Test
  public void testPropertyEqualTo() {
    PropertyIsEqualTo propertyIsEqualToFilter =
        (PropertyIsEqualTo)
            geotoolsFilterBuilder.attribute(Core.ID).is().equalTo().text(TEST_STRING);
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
            geotoolsFilterBuilder.attribute(Core.ID).is().equalTo().text(TEST_STRING);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.NOT);
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(propertyIsEqualToFilter, openSearchFilterVisitorObject);
    assertThat(result.getId(), nullValue());
  }

  @Test
  public void testPropertyEqualToNotIdAttribute() {
    PropertyIsEqualTo propertyIsEqualToFilter =
        (PropertyIsEqualTo)
            geotoolsFilterBuilder.attribute(Core.TITLE).is().equalTo().text(TEST_STRING);
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        new OpenSearchFilterVisitorObject();
    OpenSearchFilterVisitorObject result =
        (OpenSearchFilterVisitorObject)
            openSearchFilterVisitor.visit(propertyIsEqualToFilter, openSearchFilterVisitorObject);
    assertThat(result.getId(), nullValue());
  }
}
