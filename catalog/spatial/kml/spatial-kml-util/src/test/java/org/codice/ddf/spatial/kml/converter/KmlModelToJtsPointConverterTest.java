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
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Location;
import de.micromata.opengis.kml.v_2_2_0.Model;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import java.io.InputStream;
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Point;

public class KmlModelToJtsPointConverterTest {
  private static Model testKmlModel;

  @BeforeClass
  public static void setupClass() {
    InputStream stream = KmlModelToJtsPointConverterTest.class.getResourceAsStream("/kmlModel.kml");

    Kml kml = Kml.unmarshal(stream);

    testKmlModel = ((Model) ((Placemark) kml.getFeature()).getGeometry());
  }

  @Test
  public void testConvertKmlModelToJtsPoint() {
    Point jtsPoint = KmlModelToJtsPointConverter.from(testKmlModel);

    assertKmlModelToJtsPoint(testKmlModel, jtsPoint);
  }

  @Test
  public void testConvertNullKmlModelReturnsNullPoint() {
    Point jtsPoint = KmlModelToJtsPointConverter.from(null);
    assertThat(jtsPoint, nullValue());
  }

  @Test
  public void testConvertEmptyKmlModelReturnsNullPoint() {
    Point jtsPoint = KmlModelToJtsPointConverter.from(new Model());
    assertThat(jtsPoint, nullValue());
  }

  static void assertKmlModelToJtsPoint(Model kmlModel, Point jtsPoint) {
    assertThat(jtsPoint, notNullValue());

    Location location = kmlModel.getLocation();
    KmlToJtsCoordinateConverterTest.assertJtsCoordinatesFromKmlCoordinates(
        Collections.singletonList(
            new Coordinate(
                location.getLongitude(), location.getLatitude(), location.getAltitude())),
        jtsPoint.getCoordinates());
  }
}
