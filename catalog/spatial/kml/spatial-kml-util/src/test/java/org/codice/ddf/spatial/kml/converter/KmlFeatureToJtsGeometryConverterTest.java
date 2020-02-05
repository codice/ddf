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

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.PhotoOverlay;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import java.io.InputStream;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

public class KmlFeatureToJtsGeometryConverterTest {

  @Test
  public void testConverNullKmlFeatureReturnsNullJtsGeometry() {
    Geometry jtsGeometry = KmlFeatureToJtsGeometryConverter.from(null);

    assertThat(jtsGeometry, nullValue());
  }

  @Test
  public void testConvertKmlDocumentFeature() {
    InputStream stream =
        KmlFeatureToJtsGeometryConverterTest.class.getResourceAsStream("/kmlDocument.kml");

    Kml kml = Kml.unmarshal(stream);
    assertThat(kml, notNullValue());

    Document kmlDocument = ((Document) kml.getFeature());
    assertThat(kmlDocument, notNullValue());

    Geometry jtsGeometry = KmlFeatureToJtsGeometryConverter.from(kmlDocument);

    assertThat(jtsGeometry, notNullValue());
    assertFeature(kmlDocument, jtsGeometry);
  }

  @Test
  public void testConvertKmlFolderFeature() {
    InputStream stream =
        KmlFeatureToJtsGeometryConverterTest.class.getResourceAsStream("/kmlFolder.kml");

    Kml kml = Kml.unmarshal(stream);
    assertThat(kml, notNullValue());

    Folder kmlFolder = ((Folder) kml.getFeature());
    assertThat(kmlFolder, notNullValue());

    Geometry jtsGeometry = KmlFeatureToJtsGeometryConverter.from(kmlFolder);

    assertThat(jtsGeometry, notNullValue());
    assertFeature(kmlFolder, jtsGeometry);
  }

  @Test
  public void testConvertKmlPlaceMarkFeature() {
    InputStream stream =
        KmlFeatureToJtsGeometryConverterTest.class.getResourceAsStream("/kmlPoint.kml");

    Kml kml = Kml.unmarshal(stream);
    assertThat(kml, notNullValue());

    Placemark kmlPlacemark = (Placemark) kml.getFeature();
    assertThat(kmlPlacemark, notNullValue());

    Geometry geometry = KmlFeatureToJtsGeometryConverter.from(kmlPlacemark);

    assertThat(geometry, notNullValue());

    assertFeature(kmlPlacemark, geometry);
  }

  @Test
  public void testConvertKmlPhotoOverlayFeature() {
    InputStream stream =
        KmlFeatureToJtsGeometryConverterTest.class.getResourceAsStream("/kmlPhotoOverlay.kml");

    Kml kml = Kml.unmarshal(stream);
    assertThat(kml, notNullValue());

    PhotoOverlay kmlPhotoOverlay = (PhotoOverlay) kml.getFeature();
    assertThat(kmlPhotoOverlay, notNullValue());

    Geometry geometry = KmlFeatureToJtsGeometryConverter.from(kmlPhotoOverlay);

    assertThat(geometry, notNullValue());

    assertFeature(kmlPhotoOverlay, geometry);
  }

  @Test
  public void testConvertKmlGroundOverlayFeature() {
    InputStream stream =
        KmlFeatureToJtsGeometryConverterTest.class.getResourceAsStream("/kmlGroundOverlay.kml");

    Kml kml = Kml.unmarshal(stream);
    assertThat(kml, notNullValue());

    GroundOverlay kmlGroundOverlay = (GroundOverlay) kml.getFeature();
    assertThat(kmlGroundOverlay, notNullValue());

    Geometry geometry = KmlFeatureToJtsGeometryConverter.from(kmlGroundOverlay);

    assertThat(geometry, notNullValue());

    assertFeature(kmlGroundOverlay, geometry);
  }

  static void assertFeature(Feature kmlFeature, Geometry jtsGeometry) {
    if (kmlFeature instanceof Document) {
      KmlDocumentToJtsGeometryConverterTest.assertKmlDocument((Document) kmlFeature, jtsGeometry);
    }
    if (kmlFeature instanceof Folder) {
      KmlFolderToJtsGeometryConverterTest.assertKmlFolder((Folder) kmlFeature, jtsGeometry);
    }
    if (kmlFeature instanceof Placemark) {
      KmlPlacemarkToJtsGeometryConverterTest.assertPlacemark((Placemark) kmlFeature, jtsGeometry);
    }
    if (kmlFeature instanceof PhotoOverlay) {
      KmlPhotoOverlayToJtsPointConverterTest.assertKmlPhotoOverlayToJtsPoint(
          (PhotoOverlay) kmlFeature, (Point) jtsGeometry);
    }
    if (kmlFeature instanceof GroundOverlay) {
      KmlGroundOverlayToJtsGeometryConverterTest.assertKmlGroundOverlayToJtsGeometry(
          (GroundOverlay) kmlFeature, jtsGeometry);
    }
  }
}
