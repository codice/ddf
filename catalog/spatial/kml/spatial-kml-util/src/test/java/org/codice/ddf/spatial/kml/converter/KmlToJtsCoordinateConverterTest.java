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
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class KmlToJtsCoordinateConverterTest {

  private static List<Coordinate> testKmlCoordinates;

  @BeforeClass
  public static void setupClass() {
    InputStream stream =
        KmlToJtsCoordinateConverterTest.class.getResourceAsStream("/kmlLinearRing.kml");

    Kml kml = Kml.unmarshal(stream);

    LinearRing kmlLinearRing = ((LinearRing) ((Placemark) kml.getFeature()).getGeometry());

    testKmlCoordinates = kmlLinearRing.getCoordinates();
  }

  @Test
  public void testCoordinateConversion() {
    org.locationtech.jts.geom.Coordinate jtsCoordinate;

    for (Coordinate kmlCoordinate : testKmlCoordinates) {
      jtsCoordinate = KmlToJtsCoordinateConverter.from(kmlCoordinate);
      assertJtsCoordinateFromKmlCoordinate(kmlCoordinate, jtsCoordinate);
    }
  }

  @Test
  public void testCoordinatesConversion() {
    org.locationtech.jts.geom.Coordinate[] jtsCoordinates =
        KmlToJtsCoordinateConverter.from(testKmlCoordinates);

    assertJtsCoordinatesFromKmlCoordinates(testKmlCoordinates, jtsCoordinates);
  }

  @Test
  public void testNullKmlCoordinateReturnsNullJtsCoordinate() {
    org.locationtech.jts.geom.Coordinate jtsCoordinate =
        KmlToJtsCoordinateConverter.from((Coordinate) null);
    assertThat(jtsCoordinate, nullValue());
  }

  @Test
  public void testNullKmlCoordinateListReturnsNullJtsCoordinateArray() {
    org.locationtech.jts.geom.Coordinate[] jtsCoordinates =
        KmlToJtsCoordinateConverter.from((List<Coordinate>) null);
    assertThat(jtsCoordinates, arrayWithSize(0));
  }

  @Test
  public void testKmlCoordinateListWithNullObjectReturnsNullJtsCoordinateArrays() {
    org.locationtech.jts.geom.Coordinate[] jtsCoordinates =
        KmlToJtsCoordinateConverter.from(Collections.singletonList(null));
    assertThat(jtsCoordinates, arrayWithSize(0));
  }

  @Test
  public void testKmlCoordinateListWithGoodCoordinatesAndANullObjectIgnoresTheNullObject() {
    List<Coordinate> kmlCoordinatesWithNull = getTestKmlCoordinatesWithNull();

    org.locationtech.jts.geom.Coordinate[] jtsCoordinates =
        KmlToJtsCoordinateConverter.from(kmlCoordinatesWithNull);

    // testing against the global testKmlCoordinates because that set doesn't include the null
    assertJtsCoordinatesFromKmlCoordinates(testKmlCoordinates, jtsCoordinates);
  }

  private List<Coordinate> getTestKmlCoordinatesWithNull() {
    List<Coordinate> copy = new ArrayList<>(testKmlCoordinates);
    copy.add(null);
    return copy;
  }

  private void assertJtsCoordinateFromKmlCoordinate(
      Coordinate kmlCoordinate, org.locationtech.jts.geom.Coordinate jtsCoordinate) {
    assertThat(jtsCoordinate.x, is(equalTo(kmlCoordinate.getLongitude())));
    assertThat(jtsCoordinate.y, is(equalTo(kmlCoordinate.getLatitude())));
    assertThat(jtsCoordinate.z, is(equalTo(kmlCoordinate.getAltitude())));
  }

  static void assertJtsCoordinatesFromKmlCoordinates(
      List<Coordinate> kmlCoordinates, org.locationtech.jts.geom.Coordinate[] jtsCoordinates) {
    assertThat(jtsCoordinates.length, is(equalTo(kmlCoordinates.size())));

    for (Coordinate kmlCoordinate : kmlCoordinates) {
      org.locationtech.jts.geom.Coordinate jtsCoordinate =
          new org.locationtech.jts.geom.Coordinate(
              kmlCoordinate.getLongitude(),
              kmlCoordinate.getLatitude(),
              kmlCoordinate.getAltitude());
      assertThat(jtsCoordinates, hasItemInArray((jtsCoordinate)));
    }
  }
}
