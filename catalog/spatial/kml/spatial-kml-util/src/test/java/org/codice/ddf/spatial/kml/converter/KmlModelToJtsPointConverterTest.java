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

import java.util.Collections;
import net.opengis.kml.v_2_2_0.LocationType;
import net.opengis.kml.v_2_2_0.ModelType;
import org.locationtech.jts.geom.Point;

public class KmlModelToJtsPointConverterTest {
  private static ModelType testKmlModel;

  //  @BeforeClass
  //  public static void setupClass() {
  //    InputStream stream =
  // KmlModelToJtsPointConverterTest.class.getResourceAsStream("/kmlModel.kml");
  //
  //    Kml kml = Kml.unmarshal(stream);
  //
  //    testKmlModel = ((Model) ((Placemark) kml.getFeature()).getGeometry());
  //  }
  //
  //  @Test
  //  public void testConvertKmlModelToJtsPoint() {
  //    Point jtsPoint = KmlModelToJtsPointConverter.from(testKmlModel);
  //
  //    assertKmlModelToJtsPoint(testKmlModel, jtsPoint);
  //  }
  //
  //  @Test
  //  public void testConvertNullKmlModelReturnsNullPoint() {
  //    Point jtsPoint = KmlModelToJtsPointConverter.from(null);
  //    assertThat(jtsPoint, nullValue());
  //  }
  //
  //  @Test
  //  public void testConvertEmptyKmlModelReturnsNullPoint() {
  //    Point jtsPoint = KmlModelToJtsPointConverter.from(new Model());
  //    assertThat(jtsPoint, nullValue());
  //  }

  static void assertKmlModelToJtsPoint(ModelType kmlModel, Point jtsPoint) {
    assertThat(jtsPoint, notNullValue());

    LocationType location = kmlModel.getLocation();
    KmlToJtsCoordinateConverterTest.assertJtsCoordinatesFromKmlCoordinates(
        Collections.singletonList("Gordo fix the coord stuff"),
        //            new Coordinate(
        //                location.getLongitude(), location.getLatitude(), location.getAltitude())),
        jtsPoint.getCoordinates());
  }
}
