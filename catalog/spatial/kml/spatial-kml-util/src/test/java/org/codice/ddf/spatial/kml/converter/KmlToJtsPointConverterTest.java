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

import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import java.io.InputStream;
import org.junit.BeforeClass;
import org.junit.Test;

public class KmlToJtsPointConverterTest {
  private static Point testKmlPoint;

  @BeforeClass
  public static void setupClass() {
    InputStream stream = KmlToJtsPointConverterTest.class.getResourceAsStream("/kmlPoint.kml");

    Kml kml = Kml.unmarshal(stream);

    testKmlPoint = ((Point) ((Placemark) kml.getFeature()).getGeometry());
  }

  @Test
  public void testPointConversion() {
    org.locationtech.jts.geom.Point jtsPoint = KmlToJtsPointConverter.from(testKmlPoint);

    assertJtsPoint(testKmlPoint, jtsPoint);
  }

  @Test
  public void testNullKmlPointReturnsNullJtsPoint() {
    org.locationtech.jts.geom.Point jtsPoint = KmlToJtsPointConverter.from(null);

    assertThat(jtsPoint, nullValue());
  }

  @Test
  public void testKmlPointWithNoCoordinatesReturnsNullJtsPoint() {
    org.locationtech.jts.geom.Point jtsPoint = KmlToJtsPointConverter.from(new Point());

    assertThat(jtsPoint, nullValue());
  }

  static void assertJtsPoint(Point kmlPoint, org.locationtech.jts.geom.Point jtsPoint) {
    assertThat(jtsPoint, notNullValue());

    KmlToJtsCoordinateConverterTest.assertJtsCoordinatesFromKmlCoordinates(
        kmlPoint.getCoordinates(), jtsPoint.getCoordinates());
  }
}
