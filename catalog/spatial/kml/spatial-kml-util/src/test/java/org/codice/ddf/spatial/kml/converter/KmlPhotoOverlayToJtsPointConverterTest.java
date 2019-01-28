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
import de.micromata.opengis.kml.v_2_2_0.PhotoOverlay;
import java.io.InputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Point;

public class KmlPhotoOverlayToJtsPointConverterTest {
  private static PhotoOverlay testKmlPhotoOverlay;

  @BeforeClass
  public static void setupClass() {
    InputStream stream =
        KmlPhotoOverlayToJtsPointConverterTest.class.getResourceAsStream("/kmlPhotoOverlay.kml");

    Kml kml = Kml.unmarshal(stream);

    testKmlPhotoOverlay = ((PhotoOverlay) kml.getFeature());
  }

  @Test
  public void testConvertKmlPhotoOverlayToJtsPoint() {
    Point jtsPoint = KmlPhotoOverlayToJtsPointConverter.from(testKmlPhotoOverlay);

    assertKmlPhotoOverlayToJtsPoint(testKmlPhotoOverlay, jtsPoint);
  }

  @Test
  public void testConvertNullKmlPhotoOverlayReturnsNullPoint() {
    Point jtsPoint = KmlPhotoOverlayToJtsPointConverter.from(null);
    assertThat(jtsPoint, nullValue());
  }

  @Test
  public void testConvertEmptyKmlPhotoOverlayReturnsNullPoint() {
    Point jtsPoint = KmlPhotoOverlayToJtsPointConverter.from(new PhotoOverlay());
    assertThat(jtsPoint, nullValue());
  }

  static void assertKmlPhotoOverlayToJtsPoint(PhotoOverlay kmlPhotoOverlay, Point jtsPoint) {
    assertThat(jtsPoint, notNullValue());

    KmlToJtsPointConverterTest.assertJtsPoint(kmlPhotoOverlay.getPoint(), jtsPoint);
  }
}
