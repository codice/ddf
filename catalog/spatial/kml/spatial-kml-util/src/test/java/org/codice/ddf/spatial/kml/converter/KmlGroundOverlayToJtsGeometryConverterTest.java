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

import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import java.io.InputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;

public class KmlGroundOverlayToJtsGeometryConverterTest {
  private static GroundOverlay testKmlGroundOverlay;

  @BeforeClass
  public static void setupClass() {
    InputStream stream =
        KmlGroundOverlayToJtsGeometryConverterTest.class.getResourceAsStream(
            "/kmlGroundOverlay.kml");

    Kml kml = Kml.unmarshal(stream);

    testKmlGroundOverlay = ((GroundOverlay) kml.getFeature());
  }

  @Test
  public void testConvertKmlGroundOverlayToJtsPoint() {
    Geometry jtsGeometry = KmlGroundOverlayToJtsGeometryConverter.from(testKmlGroundOverlay);

    assertKmlGroundOverlayToJtsGeometry(testKmlGroundOverlay, jtsGeometry);
  }

  @Test
  public void testConvertNullKmlGroundOverlayReturnsNullPoint() {
    Geometry jtsGeometry = KmlGroundOverlayToJtsGeometryConverter.from(null);
    assertThat(jtsGeometry, nullValue());
  }

  @Test
  public void testConvertEmptyKmlGroundOverlayReturnsNullPoint() {
    Geometry jtsGeometry = KmlGroundOverlayToJtsGeometryConverter.from(new GroundOverlay());
    assertThat(jtsGeometry, nullValue());
  }

  static void assertKmlGroundOverlayToJtsGeometry(
      GroundOverlay groundOverlay, Geometry jtsGeometry) {
    assertThat(jtsGeometry, notNullValue());

    KmlLatLonBoxToJtsGeometryConverterTest.assertKmlLatLonBoxToJtsGeometry(
        groundOverlay.getLatLonBox(), jtsGeometry);
  }
}
