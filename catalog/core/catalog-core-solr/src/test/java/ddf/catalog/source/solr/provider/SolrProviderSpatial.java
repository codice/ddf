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
package ddf.catalog.source.solr.provider;

import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.create;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.deleteAll;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.getFilterBuilder;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.update;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.solr.ConfigurationStore;
import ddf.catalog.source.solr.SolrCatalogProviderImpl;
import ddf.catalog.source.solr.SolrProviderTest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.SortByImpl;
import org.geotools.geometry.jts.spatialschema.geometry.DirectPositionImpl;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.UomOgcMapping;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrProviderSpatial {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrProviderSpatial.class);

  private static final double METERS_PER_KM = 1000.0;

  private final FilterFactory filterFactory = new FilterFactoryImpl();

  private static SolrCatalogProviderImpl provider;

  @BeforeClass
  public static void setUp() {
    provider = SolrProviderTest.getProvider();
  }

  @Test
  public void testSpatialOutOfBoundsSortingGeoCenterPoint() throws Exception {
    deleteAll(provider);

    MetacardImpl metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    metacard1.setLocation("POINT (-180.00000000000003 76.50325000000001)");

    MetacardImpl metacard2 = new MockMetacard(Library.getTampaRecord());
    metacard2.setTitle("Tampa");
    metacard2.setLocation("POINT (-180 76.50321)");

    List<Metacard> list = Arrays.asList(metacard1, metacard2);

    create(list, provider);
    QueryImpl query = pointRadius(-180.0, 76.50329, 20.0);
    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

    assertThat("Failed to find the two records.", sourceResponse.getResults().size(), is(2));

    assertThat(
        "Failed to properly sort the two records.",
        sourceResponse.getResults().get(0).getMetacard().getTitle(),
        is(metacard1.getTitle()));
  }

  @Test
  public void testSpatialPointRadius() throws Exception {

    deleteAll(provider);
    MetacardImpl metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    MetacardImpl metacard2 = new MockMetacard(Library.getTampaRecord());
    MetacardImpl metacard3 = new MockMetacard(Library.getShowLowRecord());

    // Add in the geometry
    metacard1.setLocation(Library.FLAGSTAFF_AIRPORT_POINT_WKT);
    metacard2.setLocation(Library.TAMPA_AIRPORT_POINT_WKT);
    metacard3.setLocation(Library.SHOW_LOW_AIRPORT_POINT_WKT);

    List<Metacard> list = Arrays.asList(metacard1, metacard2, metacard3);

    // CREATE
    create(list, provider);

    Filter filter = getFilterBuilder().attribute(Metacard.ID).is().like().text("*");
    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
    assertEquals("Failed to find all records.", 3, sourceResponse.getResults().size());

    // Right on Flagstaff
    QueryImpl query = pointRadius(-111.67121887207031, 35.138454437255859, 10.0);
    query.setPageSize(1);
    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals("Failed to find Flagstaff record only.", 1, sourceResponse.getResults().size());

    for (Result r : sourceResponse.getResults()) {
      assertTrue(
          "Wrong record, Flagstaff keyword was not found.",
          r.getMetacard().getMetadata().contains(Library.FLAGSTAFF_QUERY_PHRASE));
      LOGGER.info("Distance to Flagstaff: {}", r.getDistanceInMeters());
      // assertTrue(r.getDistanceInMeters() != null);
    }

    // Right on Flagstaff, finding 2 records with 195 km radius
    query = pointRadius(-111.67121887207031, 35.138454437255859, 195000);
    query.setSortBy(new ddf.catalog.filter.impl.SortByImpl("foo", SortOrder.ASCENDING));
    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals("Failed to find the two records.", 2, sourceResponse.getResults().size());

    ArrayList<Result> results = new ArrayList<>(sourceResponse.getResults());

    // must be in order because that was specified by the Sortby in the
    // querybuilder
    for (int i = 0; i < 2; i++) {
      Result result = results.get(i);

      LOGGER.info("Distance of [{}]]: {}", i, result.getDistanceInMeters());

      if (i == 0) {
        assertTrue(
            "Grabbed the wrong record.",
            result.getMetacard().getMetadata().contains(Library.FLAGSTAFF_QUERY_PHRASE));
      }
      if (i == 1) {
        assertTrue(
            "Grabbed the wrong record - should be Show Low.",
            result.getMetacard().getMetadata().contains("Show Low"));
      }
    }

    // NEGATIVE CASE
    query = pointRadius(80.1, 25, 195000);
    query.setPageSize(3);
    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals("Should have not found any records.", 0, sourceResponse.getResults().size());

    // FEET
    double[] coords = {-111.67121887207031, 35.138454437255859};
    query =
        new QueryImpl(
            filterFactory.dwithin(
                Metacard.ANY_GEO,
                new PointImpl(new DirectPositionImpl(coords), DefaultGeographicCRS.WGS84),
                195000,
                UomOgcMapping.FOOT.name()));

    query.setStartIndex(1);

    SortByImpl sortby =
        new SortByImpl(
            filterFactory.property(Result.DISTANCE), org.opengis.filter.sort.SortOrder.ASCENDING);
    query.setSortBy(sortby);
    query.setPageSize(3);
    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(1, sourceResponse.getResults().size());
  }

  @Test
  public void testSortedPointRadiusWithComplexQuery() throws Exception {
    deleteAll(provider);
    MetacardImpl metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    MetacardImpl metacard2 = new MockMetacard(Library.getTampaRecord());
    MetacardImpl metacard3 = new MockMetacard(Library.getShowLowRecord());

    // Add in the geometry
    metacard1.setLocation(Library.FLAGSTAFF_AIRPORT_POINT_WKT);
    metacard2.setLocation(Library.TAMPA_AIRPORT_POINT_WKT);
    metacard3.setLocation(Library.SHOW_LOW_AIRPORT_POINT_WKT);

    // Add in a content type
    metacard1.setAttribute(Metacard.CONTENT_TYPE, "product");

    List<Metacard> list = Arrays.asList(metacard1, metacard2, metacard3);

    // CREATE
    create(list, provider);

    // create a filter that has spatial and content type criteria
    Filter contentFilter = getFilterBuilder().attribute(Metacard.CONTENT_TYPE).is().text("product");
    Filter spatialFilter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .intersecting()
            .wkt(Library.FLAGSTAFF_AIRPORT_POINT_WKT);

    Filter finalFilter =
        getFilterBuilder()
            .allOf(
                getFilterBuilder().attribute(Metacard.ANY_TEXT).like().text("flagstaff"),
                getFilterBuilder().allOf(contentFilter, spatialFilter));

    // sort by distance
    QueryImpl query = new QueryImpl(finalFilter);
    SortBy sortby =
        new ddf.catalog.filter.impl.SortByImpl(
            Result.DISTANCE, org.opengis.filter.sort.SortOrder.DESCENDING);
    query.setSortBy(sortby);

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(1, sourceResponse.getResults().size());
  }

  @Test
  public void testSpatialNearestNeighbor() throws Exception {
    deleteAll(provider);

    MetacardImpl metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    MetacardImpl metacard2 = new MockMetacard(Library.getTampaRecord());
    MetacardImpl metacard3 = new MockMetacard(Library.getShowLowRecord());

    // Add in the geometry
    metacard1.setLocation(Library.FLAGSTAFF_AIRPORT_POINT_WKT);
    metacard2.setLocation(Library.TAMPA_AIRPORT_POINT_WKT);
    metacard3.setLocation(Library.SHOW_LOW_AIRPORT_POINT_WKT);

    List<Metacard> list = Arrays.asList(metacard1, metacard2, metacard3);
    create(list, provider);

    // Ascending
    Filter positiveFilter =
        getFilterBuilder().attribute(Metacard.GEOGRAPHY).beyond().wkt(Library.PHOENIX_POINT_WKT, 0);
    QueryImpl query = new QueryImpl(positiveFilter);
    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(
        "Failed to find two records within 1000 nautical miles.",
        2,
        sourceResponse.getResults().size());
    assertTrue(
        "Flagstaff record was not first in ascending order.",
        sourceResponse
                .getResults()
                .get(0)
                .getMetacard()
                .getMetadata()
                .indexOf(Library.FLAGSTAFF_QUERY_PHRASE)
            > 0);

    // Descending
    SortBy sortby =
        new ddf.catalog.filter.impl.SortByImpl(
            Result.DISTANCE, org.opengis.filter.sort.SortOrder.DESCENDING);
    query.setSortBy(sortby);
    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(
        "Failed to find two records within 1000 nautical miles.",
        2,
        sourceResponse.getResults().size());
    assertTrue(
        "Flagstaff record was not last in descending order.",
        sourceResponse
                .getResults()
                .get(1)
                .getMetacard()
                .getMetadata()
                .indexOf(Library.FLAGSTAFF_QUERY_PHRASE)
            > 0);

    // Using WKT polygon
    positiveFilter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .beyond()
            .wkt(Library.ARIZONA_POLYGON_WKT, 0);
    query = new QueryImpl(positiveFilter);
    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(
        "Failed to find two records based on polygon centroid.",
        2,
        sourceResponse.getResults().size());
  }

  @Test
  public void testNearestNeighborBadConfig() {
    ConfigurationStore.getInstance().setNearestNeighborDistanceLimit(1000d);
    ConfigurationStore.getInstance().setNearestNeighborDistanceLimit((double) -3);
    assertTrue(ConfigurationStore.getInstance().getNearestNeighborDistanceLimit() != -3);
    ConfigurationStore.getInstance().setNearestNeighborDistanceLimit(12d);
    assertTrue(ConfigurationStore.getInstance().getNearestNeighborDistanceLimit() == 12);
    ConfigurationStore.getInstance().setNearestNeighborDistanceLimit(1000d);
  }

  @Test
  public void testSpatialDistanceWithinPolygon() throws Exception {
    Filter positiveFilter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .withinBuffer()
            .wkt(Library.ARIZONA_POLYGON_WKT, 50 * METERS_PER_KM);
    Filter negativeFilter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .withinBuffer()
            .wkt(Library.ARIZONA_POLYGON_WKT, 10 * METERS_PER_KM);
    testSpatialWithWkt(Library.LAS_VEGAS_POINT_WKT, positiveFilter, negativeFilter);
  }

  @Test
  public void testSpatialDistanceCalculationExactPoint() throws Exception {
    deleteAll(provider);

    // given
    double radiusInKilometers = 50;
    double radiusInMeters = radiusInKilometers * METERS_PER_KM;
    Filter positiveFilter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .withinBuffer()
            .wkt(Library.LAS_VEGAS_POINT_WKT, radiusInMeters);

    MetacardImpl metacard = new MockMetacard(Library.getFlagstaffRecord());
    metacard.setLocation(Library.LAS_VEGAS_POINT_WKT);
    List<Metacard> list = Collections.singletonList(metacard);

    create(list, provider);

    QueryImpl query = new QueryImpl(positiveFilter);
    query.setSortBy(new ddf.catalog.filter.impl.SortByImpl(Result.DISTANCE, SortOrder.ASCENDING));

    // when
    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

    // then
    assertEquals("Failed to find metacard WKT with filter", 1, sourceResponse.getResults().size());
    Result result = sourceResponse.getResults().get(0);

    assertThat(result.getDistanceInMeters(), is(notNullValue()));
    assertThat(
        "Point radius search should be less than the radius given.",
        result.getDistanceInMeters(),
        is(lessThanOrEqualTo(radiusInMeters)));

    double oneMeter = 1.0;
    assertThat(
        "The distance should be close to zero since we are right upon the point.",
        result.getDistanceInMeters(),
        is(lessThanOrEqualTo(oneMeter)));
  }

  @Test
  public void testSpatialDistanceCalculationBetweenTwoPointsUsingDistanceSortBy() throws Exception {
    spatialDistanceCalculationBetweenTwoPoints(Metacard.GEOGRAPHY, Result.DISTANCE);
  }

  @Test
  public void testSpatialDistanceCalculationBetweenTwoPointsUsingAttributeSortBy()
      throws Exception {
    spatialDistanceCalculationBetweenTwoPoints("geoattribute", "geoattribute");
  }

  private void spatialDistanceCalculationBetweenTwoPoints(String attribute, String sortBy)
      throws Exception {
    deleteAll(provider);

    // given
    double radiusInKilometers = 500;
    double radiusInMeters = radiusInKilometers * METERS_PER_KM;
    Filter positiveFilter =
        getFilterBuilder()
            .attribute(attribute)
            .withinBuffer()
            .wkt(Library.PHOENIX_POINT_WKT, radiusInMeters);

    MetacardImpl metacard;
    if (attribute.equals(Metacard.GEOGRAPHY)) {
      metacard = new MockMetacard(Library.getFlagstaffRecord());
    } else {
      metacard =
          new MockMetacard(
              Library.getFlagstaffRecord(),
              new MetacardTypeImpl(
                  "distanceTest",
                  MetacardImpl.BASIC_METACARD,
                  Sets.newSet(
                      new AttributeDescriptorImpl(
                          attribute, true, true, true, true, BasicTypes.GEO_TYPE))));
    }

    metacard.setAttribute(attribute, Library.LAS_VEGAS_POINT_WKT);
    List<Metacard> list = Collections.singletonList(metacard);

    create(list, provider);

    QueryImpl query = new QueryImpl(positiveFilter);
    query.setSortBy(new ddf.catalog.filter.impl.SortByImpl(sortBy, SortOrder.ASCENDING));

    // when
    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

    // then
    assertEquals("Failed to find metacard WKT with filter", 1, sourceResponse.getResults().size());
    Result result = sourceResponse.getResults().get(0);

    assertThat(result.getDistanceInMeters(), is(notNullValue()));
    assertThat(
        "Point radius search should be less than the radius given.",
        result.getDistanceInMeters(),
        is(lessThanOrEqualTo(radiusInMeters)));

    // expected distance calculated from
    // http://www.movable-type.co.uk/scripts/latlong.html
    double expectedDistanceBetweenCitiesInMeters = 412700;
    double precisionPercentage = .001; // +/-0.1%
    double lowerBound = expectedDistanceBetweenCitiesInMeters * (1 - precisionPercentage);
    double upperBound = expectedDistanceBetweenCitiesInMeters * (1 + precisionPercentage);

    assertThat(
        "The distance returned should at least be above the lower bound of error.",
        result.getDistanceInMeters(),
        is(greaterThanOrEqualTo(lowerBound)));
    assertThat(
        "The distance returned should at least be below the upper bound of error.",
        result.getDistanceInMeters(),
        is(lessThanOrEqualTo(upperBound)));
  }

  @Test
  public void testSpatialWithin() throws Exception {
    Filter positiveFilter =
        getFilterBuilder().attribute(Metacard.GEOGRAPHY).within().wkt(Library.ARIZONA_POLYGON_WKT);
    Filter negativeFilter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .within()
            .wkt(Library.GULF_OF_GUINEA_POLYGON_WKT);
    testSpatialWithWkt(Library.FLAGSTAFF_AIRPORT_POINT_WKT, positiveFilter, negativeFilter);
  }

  @Test
  public void testSpatialQueryWithClockwiseRectangle() throws Exception {
    deleteAll(provider);

    MetacardImpl metacard = new MockMetacard(Library.getFlagstaffRecord());
    metacard.setLocation(Library.FLAGSTAFF_AIRPORT_POINT_WKT);
    List<Metacard> list = Collections.singletonList(metacard);

    // CREATE
    create(list, provider);

    // POSITIVE
    Filter filter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .intersecting()
            .wkt(Library.CLOCKWISE_ARIZONA_RECTANGLE_WKT);
    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Failed to find Flagstaff record.", 1, sourceResponse.getResults().size());

    for (Result r : sourceResponse.getResults()) {
      assertTrue(
          "Wrong record, Flagstaff keyword was not found.",
          r.getMetacard().getMetadata().contains(Library.FLAGSTAFF_QUERY_PHRASE));
    }
  }

  @Test
  public void testSpatialQueryAcrossInternationalDateLine() throws Exception {
    deleteAll(provider);

    MetacardImpl metacard = new MockMetacard(Library.getFlagstaffRecord());
    metacard.setLocation(Library.MIDWAY_ISLANDS_POINT_WKT);
    List<Metacard> list = Collections.singletonList(metacard);

    create(list, provider);

    // POSITIVE - Counter Clockwise Orientation
    Filter filter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .intersecting()
            .wkt(Library.ACROSS_INTERNATIONAL_DATELINE_LARGE_CCW_WKT);
    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Failed to find the correct record. ", 1, sourceResponse.getResults().size());

    for (Result r : sourceResponse.getResults()) {
      assertTrue(
          "Wrong record, Flagstaff keyword was not found.",
          r.getMetacard().getMetadata().contains(Library.FLAGSTAFF_QUERY_PHRASE));
    }

    // POSITIVE - Clockwise Orientation
    filter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .intersecting()
            .wkt(Library.ACROSS_INTERNATIONAL_DATELINE_LARGE_CW_WKT);
    sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Failed to find the correct record. ", 1, sourceResponse.getResults().size());

    for (Result r : sourceResponse.getResults()) {
      assertTrue(
          "Wrong record, Flagstaff keyword was not found.",
          r.getMetacard().getMetadata().contains(Library.FLAGSTAFF_QUERY_PHRASE));
    }

    // NEGATIVE
    filter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .intersecting()
            .wkt(Library.ACROSS_INTERNATIONAL_DATELINE_SMALL_WKT);
    sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Should not find a record. ", 0, sourceResponse.getResults().size());
  }

  @Test
  public void testSpatialCreateAndUpdateWithClockwiseRectangle() throws Exception {
    deleteAll(provider);

    // CREATE
    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());
    metacard.setLocation(Library.CLOCKWISE_ARIZONA_RECTANGLE_WKT);

    CreateResponse createResponse = create(Collections.singletonList(metacard), provider);
    assertEquals(1, createResponse.getCreatedMetacards().size());

    Filter filter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .intersecting()
            .wkt(Library.FLAGSTAFF_AIRPORT_POINT_WKT);
    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Failed to find correct record.", 1, sourceResponse.getResults().size());

    // UPDATE
    MockMetacard updatedMetacard = new MockMetacard(Library.getTampaRecord());
    updatedMetacard.setLocation(Library.CLOCKWISE_ARIZONA_RECTANGLE_WKT);

    String[] ids = {metacard.getId()};
    UpdateResponse updateResponse =
        update(ids, Collections.singletonList(updatedMetacard), provider);
    assertEquals(1, updateResponse.getUpdatedMetacards().size());

    filter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .intersecting()
            .wkt(Library.FLAGSTAFF_AIRPORT_POINT_WKT);
    sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Failed to find correct record.", 1, sourceResponse.getResults().size());
  }

  @Test
  public void testSpatialQueryWithCounterClockwiseRectangle() throws Exception {
    deleteAll(provider);

    MetacardImpl metacard = new MockMetacard(Library.getFlagstaffRecord());
    metacard.setLocation(Library.FLAGSTAFF_AIRPORT_POINT_WKT);
    List<Metacard> list = Collections.singletonList(metacard);

    // CREATE
    create(list, provider);

    // POSITIVE
    Filter filter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .intersecting()
            .wkt(Library.COUNTERCLOCKWISE_ARIZONA_RECTANGLE_WKT);
    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Failed to find Flagstaff record.", 1, sourceResponse.getResults().size());

    for (Result r : sourceResponse.getResults()) {
      assertTrue(
          "Wrong record, Flagstaff keyword was not found.",
          r.getMetacard().getMetadata().contains(Library.FLAGSTAFF_QUERY_PHRASE));
    }
  }

  @Test
  public void testSpatialPointIntersectsPoint() throws Exception {
    testSpatialIntersectsWithWkt(
        Library.FLAGSTAFF_AIRPORT_POINT_WKT,
        Library.FLAGSTAFF_AIRPORT_POINT_WKT,
        Library.GULF_OF_GUINEA_POINT_WKT);
  }

  @Test
  public void testSpatialMultiPointIntersectsPoint() throws Exception {
    testSpatialIntersectsWithWkt(
        Library.GULF_OF_GUINEA_POINT_WKT,
        Library.GULF_OF_GUINEA_MULTIPOINT_WKT,
        Library.FLAGSTAFF_AIRPORT_POINT_WKT);
  }

  @Test
  public void testSpatialMultiPointSingleIntersectsPoint() throws Exception {
    testSpatialIntersectsWithWkt(
        Library.GULF_OF_GUINEA_POINT_WKT,
        Library.GULF_OF_GUINEA_MULTIPOINT_SINGLE_WKT,
        Library.FLAGSTAFF_AIRPORT_POINT_WKT);
  }

  @Test
  public void testSpatialPolygonIntersectsPoint() throws Exception {
    testSpatialIntersectsWithWkt(
        Library.ARIZONA_POLYGON_WKT,
        Library.FLAGSTAFF_AIRPORT_POINT_WKT,
        Library.GULF_OF_GUINEA_POINT_WKT);
  }

  @Test
  public void testSpatialPolygonIntersectsLineString() throws Exception {
    testSpatialIntersectsWithWkt(
        Library.ARIZONA_POLYGON_WKT,
        Library.ARIZONA_INTERSECTING_LINESTING_WKT,
        Library.GULF_OF_GUINEA_LINESTRING_WKT);
  }

  @Test
  public void testSpatialPolygonIntersectsPolygon() throws Exception {
    testSpatialIntersectsWithWkt(
        Library.ARIZONA_POLYGON_WKT,
        Library.ARIZONA_INTERSECTING_POLYGON_WKT,
        Library.GULF_OF_GUINEA_POLYGON_WKT);
  }

  @Test
  public void testSpatialPolygonIntersectsMultiPoint() throws Exception {
    testSpatialIntersectsWithWkt(
        Library.ARIZONA_POLYGON_WKT,
        Library.PHOENIX_AND_LAS_VEGAS_MULTIPOINT_WKT,
        Library.GULF_OF_GUINEA_MULTIPOINT_WKT);
  }

  @Test
  public void testSpatialPolygonIntersectsMultiLineString() throws Exception {
    testSpatialIntersectsWithWkt(
        Library.ARIZONA_POLYGON_WKT,
        Library.ARIZONA_INTERSECTING_MULTILINESTING_WKT,
        Library.GULF_OF_GUINEA_MULTILINESTRING_WKT);
  }

  @Test
  public void testSpatialPolygonIntersectsMultiPolygon() throws Exception {
    testSpatialIntersectsWithWkt(
        Library.ARIZONA_POLYGON_WKT,
        Library.ARIZONA_INTERSECTING_MULTIPOLYGON_WKT,
        Library.GULF_OF_GUINEA_MULTIPOLYGON_WKT);
  }

  @Test
  public void testSpatialPolygonIntersectsGeometryCollection() throws Exception {
    testSpatialIntersectsWithWkt(
        Library.ARIZONA_POLYGON_WKT,
        Library.ARIZONA_INTERSECTING_GEOMETRYCOLLECTION_WKT,
        Library.GULF_OF_GUINEA_GEOMETRYCOLLECTION_WKT);
  }

  @Test
  public void testSpatialGeometryCollectionIntersectsPoint() throws Exception {
    testSpatialIntersectsWithWkt(
        Library.ARABIAN_SEA_OVERLAPPING_GEOMETRYCOLLECTION_WKT,
        Library.ARABIAN_SEA_POINT_WKT,
        Library.GULF_OF_GUINEA_GEOMETRYCOLLECTION_WKT);
  }

  @Test
  public void testSpatialPointWithinPolygon() throws Exception {
    testSpatialWithinWithWkt(
        Library.FLAGSTAFF_AIRPORT_POINT_WKT,
        Library.WEST_USA_CONTAINING_POLYGON_WKT,
        Library.GULF_OF_GUINEA_POLYGON_WKT);
  }

  @Test
  public void testSpatialLineStringWithinPolygon() throws Exception {
    testSpatialWithinWithWkt(
        Library.ARIZONA_INTERSECTING_LINESTING_WKT,
        Library.WEST_USA_CONTAINING_POLYGON_WKT,
        Library.GULF_OF_GUINEA_POLYGON_WKT);
  }

  @Test
  public void testSpatialPolygonWithinPolygon() throws Exception {
    testSpatialWithinWithWkt(
        Library.ARIZONA_INTERSECTING_POLYGON_WKT,
        Library.WEST_USA_CONTAINING_POLYGON_WKT,
        Library.GULF_OF_GUINEA_POLYGON_WKT);
  }

  @Test
  public void testSpatialMultiPointWithinPolygon() throws Exception {
    testSpatialWithinWithWkt(
        Library.PHOENIX_AND_LAS_VEGAS_MULTIPOINT_WKT,
        Library.WEST_USA_CONTAINING_POLYGON_WKT,
        Library.GULF_OF_GUINEA_POLYGON_WKT);
  }

  @Test
  public void testSpatialMultiLineStringWithinPolygon() throws Exception {
    testSpatialWithinWithWkt(
        Library.ARIZONA_INTERSECTING_MULTILINESTING_WKT,
        Library.WEST_USA_CONTAINING_POLYGON_WKT,
        Library.GULF_OF_GUINEA_POLYGON_WKT);
  }

  @Test
  public void testSpatialMultiPolygonWithinPolygon() throws Exception {
    testSpatialWithinWithWkt(
        Library.ARIZONA_INTERSECTING_MULTIPOLYGON_WKT,
        Library.WEST_USA_CONTAINING_POLYGON_WKT,
        Library.GULF_OF_GUINEA_POLYGON_WKT);
  }

  @Test
  public void testSpatialGeometryCollectionWithinPolygon() throws Exception {
    testSpatialWithinWithWkt(
        Library.ARIZONA_INTERSECTING_GEOMETRYCOLLECTION_WKT,
        Library.WEST_USA_CONTAINING_POLYGON_WKT,
        Library.GULF_OF_GUINEA_POLYGON_WKT);
  }

  @Test
  public void testSpatialPolygonContainsPoint() throws Exception {
    Filter positiveFilter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .containing()
            .wkt(Library.FLAGSTAFF_AIRPORT_POINT_WKT);
    Filter negativeFilter =
        getFilterBuilder()
            .attribute(Metacard.GEOGRAPHY)
            .containing()
            .wkt(Library.GULF_OF_GUINEA_POINT_WKT);
    testSpatialWithWkt(Library.ARIZONA_POLYGON_WKT, positiveFilter, negativeFilter);
  }

  @Test
  public void testSpatialAnyGeo() throws Exception {
    Filter positiveFilter =
        getFilterBuilder().attribute(Metacard.ANY_GEO).within().wkt(Library.ARIZONA_POLYGON_WKT);
    Filter negativeFilter =
        getFilterBuilder()
            .attribute(Metacard.ANY_GEO)
            .within()
            .wkt(Library.GULF_OF_GUINEA_POLYGON_WKT);
    testSpatialWithWkt(Library.FLAGSTAFF_AIRPORT_POINT_WKT, positiveFilter, negativeFilter);
  }

  private void testSpatialWithinWithWkt(String metacardWkt, String positiveWkt, String negativeWkt)
      throws Exception {
    Filter positiveFilter =
        getFilterBuilder().attribute(Metacard.ANY_GEO).within().wkt(positiveWkt);
    Filter negativeFilter =
        getFilterBuilder().attribute(Metacard.ANY_GEO).within().wkt(negativeWkt);
    testSpatialWithWkt(metacardWkt, positiveFilter, negativeFilter);
  }

  private void testSpatialIntersectsWithWkt(
      String metacardWkt, String positiveWkt, String negativeWkt) throws Exception {
    Filter positiveFilter =
        getFilterBuilder().attribute(Metacard.ANY_GEO).intersecting().wkt(positiveWkt);
    Filter negativeFilter =
        getFilterBuilder().attribute(Metacard.ANY_GEO).intersecting().wkt(negativeWkt);
    testSpatialWithWkt(metacardWkt, positiveFilter, negativeFilter);

    positiveFilter = getFilterBuilder().attribute(Metacard.ANY_GEO).intersecting().wkt(metacardWkt);
    testSpatialWithWkt(positiveWkt, positiveFilter, negativeFilter);
  }

  private void testSpatialWithWkt(String metacardWkt, Filter positiveFilter, Filter negativeFilter)
      throws Exception {
    deleteAll(4, provider);

    MetacardImpl metacard = new MockMetacard(Library.getFlagstaffRecord());
    metacard.setLocation(metacardWkt);
    List<Metacard> list = Collections.singletonList(metacard);

    create(list, provider);

    SourceResponse sourceResponse =
        provider.query(new QueryRequestImpl(new QueryImpl(positiveFilter)));
    assertEquals("Failed to find metacard WKT with filter", 1, sourceResponse.getResults().size());

    sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(negativeFilter)));
    assertEquals("Should not have found metacard record.", 0, sourceResponse.getResults().size());
  }

  /** Creates a point radius {@link QueryImpl} with units of measurement of meters. */
  private QueryImpl pointRadius(double x, double y, double distance) {

    double[] coords = {x, y};

    QueryImpl query =
        new QueryImpl(
            filterFactory.dwithin(
                Metacard.ANY_GEO,
                new PointImpl(new DirectPositionImpl(coords), DefaultGeographicCRS.WGS84),
                distance,
                UomOgcMapping.METRE.name()));

    query.setStartIndex(1);

    SortByImpl sortby =
        new SortByImpl(
            filterFactory.property(Result.DISTANCE), org.opengis.filter.sort.SortOrder.ASCENDING);
    query.setSortBy(sortby);

    return query;
  }
}
