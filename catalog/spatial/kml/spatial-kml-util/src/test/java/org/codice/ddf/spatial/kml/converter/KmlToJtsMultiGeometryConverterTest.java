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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import java.io.InputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.GeometryCollection;

public class KmlToJtsMultiGeometryConverterTest {
  private static MultiGeometry testKmlMultiGeometry;

  @BeforeClass
  public static void setupClass() {
    InputStream stream =
        KmlToJtsMultiGeometryConverterTest.class.getResourceAsStream("/kmlMultiGeometry.kml");

    Kml kml = Kml.unmarshal(stream);

    testKmlMultiGeometry = ((MultiGeometry) ((Placemark) kml.getFeature()).getGeometry());
  }

  @Test
  public void testNullKmlMultiGeometryReturnsNullJtsGeometryCollection() {
    GeometryCollection geometryCollection = KmlToJtsMultiGeometryConverter.from(null);

    assertThat(geometryCollection, nullValue());
  }

  @Test
  public void testConvertMultiGeometry() {
    GeometryCollection geometryCollection =
        KmlToJtsMultiGeometryConverter.from(testKmlMultiGeometry);

    assertJtsGeometryCollection(geometryCollection);
  }

  private void assertJtsGeometryCollection(GeometryCollection jtsGeometryCollection) {
    assertJtsGeometryCollection(testKmlMultiGeometry, jtsGeometryCollection);
  }

  static void assertJtsGeometryCollection(
      MultiGeometry kmlMultiGeometry, GeometryCollection jtsGeometryCollection) {
    assertThat(jtsGeometryCollection, notNullValue());

    assertThat(
        jtsGeometryCollection.getNumGeometries(),
        is(equalTo(kmlMultiGeometry.getGeometry().size())));

    for (int i = 0; i < jtsGeometryCollection.getNumGeometries(); i++) {
      KmlToJtsGeometryConverterTest.assertSpecificGeometry(
          kmlMultiGeometry.getGeometry().get(i), jtsGeometryCollection.getGeometryN(i));
    }
  }
}
