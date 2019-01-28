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
package org.codice.ddf.spatial.kml.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.LineString;

public class KmlToJtsPolygonConverterTest {

  private static Polygon testKmlPolygon;

  @BeforeClass
  public static void setupClass() {
    InputStream stream = KmlToJtsPolygonConverterTest.class.getResourceAsStream("/kmlPolygon.kml");

    Kml kml = Kml.unmarshal(stream);

    testKmlPolygon = ((Polygon) ((Placemark) kml.getFeature()).getGeometry());
  }

  @Test
  public void testConvertPolygon() {
    org.locationtech.jts.geom.Polygon jtsPolygon = KmlToJtsPolygonConverter.from(testKmlPolygon);

    assertJtsPolygon(testKmlPolygon, jtsPolygon);
  }

  @Test
  public void testConvertPolygonWithHoles() {
    Polygon kmlPolygonWithHoles = getTestKmlPolygonWithHoles();
    org.locationtech.jts.geom.Polygon jtsPolygon =
        KmlToJtsPolygonConverter.from(kmlPolygonWithHoles);

    assertJtsPolygon(kmlPolygonWithHoles, jtsPolygon);
  }

  @Test
  public void testNullKmlPolygonReturnsNullJtsPolygon() {
    org.locationtech.jts.geom.Polygon jtsPolygon = KmlToJtsPolygonConverter.from(null);

    assertThat(jtsPolygon, nullValue());
  }

  @Test
  public void testEmptyKmlPolygonReturnsNull() {
    org.locationtech.jts.geom.Polygon jtsPolygon = KmlToJtsPolygonConverter.from(new Polygon());

    assertThat(jtsPolygon, nullValue());
  }

  static void assertJtsPolygon(Polygon kmlPolygon, org.locationtech.jts.geom.Polygon jtsPolygon) {
    assertThat(jtsPolygon, notNullValue());

    assertThat(
        jtsPolygon.getNumInteriorRing(), is(equalTo(kmlPolygon.getInnerBoundaryIs().size())));
    assertKmlLinearRingMatchesJtsLineString(
        kmlPolygon.getOuterBoundaryIs().getLinearRing(), jtsPolygon.getExteriorRing());

    for (int i = 0; i < kmlPolygon.getInnerBoundaryIs().size(); i++) {
      assertKmlLinearRingMatchesJtsLineString(
          kmlPolygon.getInnerBoundaryIs().get(i).getLinearRing(), jtsPolygon.getInteriorRingN(i));
    }
  }

  private static void assertKmlLinearRingMatchesJtsLineString(
      LinearRing kmlLinearRing, LineString jtsLineString) {
    KmlToJtsCoordinateConverterTest.assertJtsCoordinatesFromKmlCoordinates(
        kmlLinearRing.getCoordinates(), jtsLineString.getCoordinates());
  }

  private Polygon getTestKmlPolygonWithHoles() {
    InputStream stream =
        KmlToJtsPolygonConverterTest.class.getResourceAsStream("/kmlPolygonWithHoles.kml");

    Kml kml = Kml.unmarshal(stream);
    assertThat(kml, notNullValue());

    Polygon polygon = ((Polygon) ((Placemark) kml.getFeature()).getGeometry());
    assertThat(polygon, notNullValue());

    LinearRing linearRing = polygon.getOuterBoundaryIs().getLinearRing();
    assertThat(linearRing, notNullValue());

    List<LinearRing> holes =
        polygon
            .getInnerBoundaryIs()
            .stream()
            .map(Boundary::getLinearRing)
            .collect(Collectors.toList());
    assertThat(holes, not(empty()));

    return polygon;
  }
}
