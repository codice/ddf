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
package org.codice.ddf.catalog.ui.query.geofeature;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import java.util.Arrays;
import java.util.List;
import org.codice.ddf.spatial.geocoder.GeoResult;
import org.codice.ddf.spatial.geocoder.GeoResultCreator;
import org.codice.ddf.spatial.geocoding.FeatureQueryException;
import org.codice.ddf.spatial.geocoding.FeatureQueryable;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryable;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

public class GazetteerFeatureServiceTest {
  private static final GeoEntry GEO_ENTRY_1 =
      new GeoEntry.Builder()
          .name("Philadelphia")
          .latitude(40.0)
          .longitude(-71.0)
          .featureCode("PPL")
          .population(1000000)
          .alternateNames("")
          .build();

  private static final GeoEntry GEO_ENTRY_2 =
      new GeoEntry.Builder()
          .name("Canada")
          .latitude(55.0)
          .longitude(-100.0)
          .featureCode("PCL1")
          .countryCode("CA")
          .population(10000000)
          .alternateNames("")
          .build();

  private static final List<GeoEntry> QUERYABLE_RESULTS = Arrays.asList(GEO_ENTRY_1, GEO_ENTRY_2);

  private static final String TEST_QUERY = "example";

  private GazetteerFeatureService gazetteerFeatureService;

  private GeoEntryQueryable geoEntryQueryable;

  private FeatureQueryable featureQueryable;

  @Before
  public void setUp() {
    geoEntryQueryable = mock(GeoEntryQueryable.class);
    featureQueryable = mock(FeatureQueryable.class);
    gazetteerFeatureService = new GazetteerFeatureService();
    gazetteerFeatureService.setGeoEntryQueryable(geoEntryQueryable);
    gazetteerFeatureService.setFeatureQueryable(featureQueryable);
  }

  @Test
  public void testGetSuggestedFeatureNames() throws GeoEntryQueryException {
    final int maxResults = 2;
    doReturn(QUERYABLE_RESULTS).when(geoEntryQueryable).query(TEST_QUERY, maxResults);

    List<String> results = gazetteerFeatureService.getSuggestedFeatureNames(TEST_QUERY, maxResults);
    assertThat(results, contains(GEO_ENTRY_1.getName(), GEO_ENTRY_2.getName()));
  }

  @Test
  public void testGetCityFeatureByName() throws GeoEntryQueryException {
    doReturn(QUERYABLE_RESULTS).when(geoEntryQueryable).query(TEST_QUERY, 1);

    SimpleFeature feature = gazetteerFeatureService.getFeatureByName(TEST_QUERY);
    Geometry geometry = (Geometry) feature.getDefaultGeometry();

    assertThat(feature.getID(), is(GEO_ENTRY_1.getName()));
    assertThat(geometry.getGeometryType(), is("Polygon"));

    GeoResult geoResult = GeoResultCreator.createGeoResult(GEO_ENTRY_1);
    double[] p0 = geoResult.getBbox().get(0).getDirectPosition().getCoordinate();
    double[] p1 = geoResult.getBbox().get(1).getDirectPosition().getCoordinate();

    Coordinate[] expectedCoordinates =
        new Coordinate[] {
          new Coordinate(p0[0], p1[1]),
          new Coordinate(p1[0], p1[1]),
          new Coordinate(p1[0], p0[1]),
          new Coordinate(p0[0], p0[1]),
          new Coordinate(p0[0], p1[1]),
        };
    assertThat(geometry.getCoordinates(), is(expectedCoordinates));
  }

  @Test
  public void testGetCountryFeatureByName() throws GeoEntryQueryException, FeatureQueryException {
    doReturn(Arrays.asList(GEO_ENTRY_2)).when(geoEntryQueryable).query(TEST_QUERY, 1);

    Coordinate[] countryCoordinates =
        new Coordinate[] {
          new Coordinate(-104.8, 50.2),
          new Coordinate(-95.2, 50.2),
          new Coordinate(-95.2, 59.8),
          new Coordinate(-104.8, 59.8),
          new Coordinate(-104.8, 50.2)
        };
    GeometryFactory geometryFactory = new GeometryFactory();
    Polygon countryPolygon =
        geometryFactory.createPolygon(geometryFactory.createLinearRing(countryCoordinates), null);

    SimpleFeature expectedFeature =
        GazetteerFeatureService.getSimpleFeatureBuilder(countryPolygon)
            .buildFeature(GEO_ENTRY_2.getName());
    doReturn(Arrays.asList(expectedFeature))
        .when(featureQueryable)
        .query("CAN", GEO_ENTRY_2.getFeatureCode(), 1);

    SimpleFeature feature = gazetteerFeatureService.getFeatureByName(TEST_QUERY);
    Geometry geometry = (Geometry) feature.getDefaultGeometry();

    assertThat(feature.getID(), is(GEO_ENTRY_2.getName()));
    assertThat(geometry.getGeometryType(), is("Polygon"));
    assertThat(geometry.equalsExact((Geometry) expectedFeature.getDefaultGeometry()), is(true));
  }
}
