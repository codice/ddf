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
import java.io.InputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;

public class KmlPlacemarkToJtsGeometryConverterTest {
  private static Placemark testKmlPlacemark;

  @BeforeClass
  public static void setupClass() {
    InputStream stream =
        KmlPlacemarkToJtsGeometryConverterTest.class.getResourceAsStream("/kmlPoint.kml");

    Kml kml = Kml.unmarshal(stream);

    testKmlPlacemark = (Placemark) kml.getFeature();
  }

  @Test
  public void testConvertPlacemark() {
    Geometry geometry = KmlPlacemarkToJtsGeometryConverter.from(testKmlPlacemark);

    assertThat(geometry, notNullValue());

    assertPlacemark(testKmlPlacemark, geometry);
  }

  @Test
  public void testConvertNullPlacemarkReturnsNull() {
    Geometry geometry = KmlPlacemarkToJtsGeometryConverter.from(null);

    assertThat(geometry, nullValue());
  }

  static void assertPlacemark(Placemark kmlPlacemark, Geometry jtsGeometry) {
    KmlToJtsGeometryConverterTest.assertSpecificGeometry(kmlPlacemark.getGeometry(), jtsGeometry);
  }
}
