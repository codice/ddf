/*
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.kml.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LatLonBox;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;

public class KmlLatLonBoxToJtsGeometryConverterTest {
  private static LatLonBox testKmlLatLonBox;

  @BeforeClass
  public static void setupClass() {
    InputStream stream =
        KmlLatLonBoxToJtsGeometryConverterTest.class.getResourceAsStream("/kmlGroundOverlay.kml");

    Kml kml = Kml.unmarshal(stream);

    GroundOverlay groundOverlay = ((GroundOverlay) kml.getFeature());

    testKmlLatLonBox = groundOverlay.getLatLonBox();
  }

  @Test
  public void testConvertKmlLatLonBoxToJtsPoint() {
    Geometry jtsGeometry = KmlLatLonBoxToJtsGeometryConverter.from(testKmlLatLonBox);

    assertKmlLatLonBoxToJtsGeometry(testKmlLatLonBox, jtsGeometry);
  }

  @Test
  public void testConvertNullKmlLatLonBoxReturnsNullPoint() {
    Geometry jtsGeometry = KmlLatLonBoxToJtsGeometryConverter.from(null);
    assertThat(jtsGeometry, nullValue());
  }

  @Test
  public void testConvertEmptyKmlLatLonBoxReturnsNullPoint() {
    Geometry jtsGeometry = KmlLatLonBoxToJtsGeometryConverter.from(new LatLonBox());
    assertThat(jtsGeometry, nullValue());
  }

  static void assertKmlLatLonBoxToJtsGeometry(LatLonBox latLonBox, Geometry jtsGeometry) {
    assertThat(jtsGeometry, notNullValue());

    assertCoordinates(latLonBox, jtsGeometry);
  }

  private static void assertCoordinates(LatLonBox latLonBox, Geometry jtsGeometry) {
    double minX = latLonBox.getWest();
    double maxX = latLonBox.getEast();
    if (minX > maxX) {
      minX = maxX;
      maxX = latLonBox.getWest();
    }

    double minY = latLonBox.getSouth();
    double maxY = latLonBox.getNorth();
    if (minY > maxY) {
      minY = maxY;
      maxY = latLonBox.getSouth();
    }

    List<Coordinate> boundingBoxCoordinates =
        createKmlBoundingBoxCoordinates(minX, maxX, minY, maxY);
    KmlToJtsCoordinateConverterTest.assertJtsCoordinatesFromKmlCoordinates(
        boundingBoxCoordinates, jtsGeometry.getCoordinates());
  }

  private static List<Coordinate> createKmlBoundingBoxCoordinates(
      double minX, double maxX, double minY, double maxY) {
    List<Coordinate> coordinates = new ArrayList<>();

    // This is the order that WKT wants the polygon
    // Starting Upper Right, moving clockwise
    coordinates.add(new Coordinate(maxX, maxY));
    coordinates.add(new Coordinate(maxX, minY));
    coordinates.add(new Coordinate(minX, minY));
    coordinates.add(new Coordinate(minX, maxY));
    coordinates.add(new Coordinate(maxX, maxY));

    return coordinates;
  }
}
