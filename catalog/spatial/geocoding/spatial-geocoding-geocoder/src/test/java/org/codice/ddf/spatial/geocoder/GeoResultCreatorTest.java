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
package org.codice.ddf.spatial.geocoder;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.geotools.geometry.jts.spatialschema.geometry.DirectPositionImpl;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl;
import org.junit.Test;
import org.opengis.geometry.primitive.Point;

public class GeoResultCreatorTest {
  private void verifyGeoResult(
      final String name,
      final double latitude,
      final double longitude,
      final String featureCode,
      final long population,
      final double expectedLatitudeOffset,
      final double expectedLongitudeOffset) {
    final GeoResult geoResult =
        GeoResultCreator.createGeoResult(name, latitude, longitude, featureCode, population);
    verifyGeoResult(
        name, latitude, longitude, expectedLatitudeOffset, expectedLongitudeOffset, geoResult);
  }

  private void verifyGeoResult(
      final String name,
      final double latitude,
      final double longitude,
      final double expectedLatitudeOffset,
      final double expectedLongitudeOffset,
      final GeoResult geoResult) {

    assertThat(geoResult.fullName, is(equalTo(name)));

    final Point point = new PointImpl(new DirectPositionImpl(longitude, latitude));
    assertThat(geoResult.point, is(equalTo(point)));

    assertThat(geoResult.bbox.size(), is(2));

    assertEquals(
        geoResult.bbox.get(0).getDirectPosition().getCoordinate()[0],
        longitude - expectedLongitudeOffset,
        0.001);
    assertEquals(
        geoResult.bbox.get(0).getDirectPosition().getCoordinate()[1],
        latitude + expectedLatitudeOffset,
        0.001);

    assertEquals(
        geoResult.bbox.get(1).getDirectPosition().getCoordinate()[0],
        longitude + expectedLongitudeOffset,
        0.001);
    assertEquals(
        geoResult.bbox.get(1).getDirectPosition().getCoordinate()[1],
        latitude - expectedLatitudeOffset,
        0.001);
  }

  @Test
  public void testCreateGeoResultAdministrativeDivision() {
    verifyGeoResult("Phoenix", 5, 10, "ADM1", 0, 5, 5);
    verifyGeoResult("Phoenix", 10.7, 5, "ADM2", 10000, 4, 4);
    verifyGeoResult("Phoenix", 0, 0.389, "ADM3", 100000, 3, 3);
    verifyGeoResult("Phoenix", -5.4, -10, "ADM4", 1000000, 2, 2);
    verifyGeoResult("Phoenix", 0, -5, "ADM5", 10000000, 1, 1);
    verifyGeoResult("Phoenix", 0, -5, "ADMD", 10000000, 0, 0);
  }

  @Test
  public void testCreateGeoResultPoliticalEntity() {
    verifyGeoResult("Tempe", -10, 5, "PCL", 0, 6, 6);
    verifyGeoResult("Tempe", -10, -10, "PCL", 100, 3, 3);
    verifyGeoResult("Tempe", -10, 10, "PCL", 5000000, 4.8, 4.8);
    verifyGeoResult("Tempe", 20, -20, "PCL", 20000000, 6, 6);
    verifyGeoResult("Tempe", -50, -50, "PCL", 200000000, 12, 12);
  }

  @Test
  public void testCreateGeoResultPopulatedPlace() {
    verifyGeoResult("Glendale", -10.5, 5.2, "PPL", 0, 0.5, 0.5);
    verifyGeoResult("Glendale", -10.893, -10.901, "PPLA", 100, 0.1, 0.1);
    verifyGeoResult("Glendale", -10.7, 10.789, "PPLA2", 50000, 0.15, 0.15);
    verifyGeoResult("Glendale", 20.999, -20.5, "PPLA3", 999999, 0.25, 0.25);
    verifyGeoResult("Glendale", -50.983, -50.784, "PPLA4", 1000001, 0.4, 0.4);
    verifyGeoResult("Glendale", 0.001, 1.234, "PPLC", 12345678, 0.75, 0.75);
  }

  @Test
  public void testCreateGeoResultOtherFeatureCode() {
    verifyGeoResult("Scottsdale", 0.123, 89.013, "ANCH", 100000, 0.1, 0.1);
  }

  @Test
  public void testGeoEntryConstructor() {
    final GeoResult geoResult =
        GeoResultCreator.createGeoResult(
            new GeoEntry.Builder()
                .name("Phoenix")
                .latitude(5.0)
                .longitude(10.0)
                .featureCode("ADM1")
                .population(1000)
                .build());
    verifyGeoResult("Phoenix", 5, 10, 5, 5, geoResult);
  }
}
