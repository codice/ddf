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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import java.io.InputStream;
import org.junit.BeforeClass;
import org.junit.Test;

public class KmlToJtsLineStringConverterTest {
  private static LineString testKmlLineString;

  @BeforeClass
  public static void setupClass() {
    InputStream stream =
        KmlToJtsLineStringConverterTest.class.getResourceAsStream("/kmlLineString.kml");

    Kml kml = Kml.unmarshal(stream);

    testKmlLineString = ((LineString) ((Placemark) kml.getFeature()).getGeometry());
  }

  @Test
  public void testConversion() {
    org.locationtech.jts.geom.LineString jtsLineString =
        KmlToJtsLineStringConverter.from(testKmlLineString);

    assertTestKmlLineString(jtsLineString);
  }

  @Test
  public void testNullKmlLineStringReturnsNullJtsLineString() {
    org.locationtech.jts.geom.LineString jtsLineString = KmlToJtsLineStringConverter.from(null);

    assertThat(jtsLineString, nullValue());
  }

  @Test
  public void testKmlLineStringWithNoCoordinatesReturnsNull() {
    org.locationtech.jts.geom.LineString jtsLineString =
        KmlToJtsLineStringConverter.from(new LineString());

    assertThat(jtsLineString, nullValue());
  }

  private void assertTestKmlLineString(org.locationtech.jts.geom.LineString lineString) {
    assertTestKmlLineString(testKmlLineString, lineString);
  }

  static void assertTestKmlLineString(
      LineString kmlLineString, org.locationtech.jts.geom.LineString jtsLineString) {
    assertThat(jtsLineString, notNullValue());

    KmlToJtsCoordinateConverterTest.assertJtsCoordinatesFromKmlCoordinates(
        kmlLineString.getCoordinates(), jtsLineString.getCoordinates());
  }
}
