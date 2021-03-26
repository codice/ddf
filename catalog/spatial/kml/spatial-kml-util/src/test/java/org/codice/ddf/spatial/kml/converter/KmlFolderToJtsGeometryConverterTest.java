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

import net.opengis.kml.v_2_2_0.FolderType;

public class KmlFolderToJtsGeometryConverterTest {

  private static FolderType testKmlFolder;

  //  @BeforeClass
  //  public static void setupClass() {
  //    InputStream stream =
  //        KmlFolderToJtsGeometryConverterTest.class.getResourceAsStream("/kmlFolder.kml");
  //
  //    Kml kml = Kml.unmarshal(stream);
  //
  //    testKmlFolder = ((Folder) kml.getFeature());
  //  }
  //
  //  @Test
  //  public void testConvertKmlFolder() {
  //    Geometry jtsGeometry = KmlFolderToJtsGeometryConverter.from(testKmlFolder);
  //
  //    assertThat(jtsGeometry, notNullValue());
  //    assertKmlFolder(testKmlFolder, jtsGeometry);
  //  }
  //
  //  @Test
  //  public void testConvertNullKmlFolderReturnsNullJtsGeometry() {
  //    Geometry jtsGeometry = KmlFolderToJtsGeometryConverter.from(null);
  //
  //    assertThat(jtsGeometry, nullValue());
  //  }

  //  static void assertKmlFolder(Folder kmlFolder, Geometry jtsGeometry) {
  //    assertThat(kmlFolder.getFeature().size(), is(equalTo(jtsGeometry.getNumGeometries())));
  //
  //    for (int i = 0; i < kmlFolder.getFeature().size(); i++) {
  //      KmlPlacemarkToJtsGeometryConverterTest.assertPlacemark(
  //          (Placemark) kmlFolder.getFeature().get(i), jtsGeometry.getGeometryN(i));
  //    }
  //  }
}
