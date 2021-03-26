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

import net.opengis.kml.v_2_2_0.AbstractFeatureType;
import net.opengis.kml.v_2_2_0.DocumentType;
import net.opengis.kml.v_2_2_0.FolderType;
import net.opengis.kml.v_2_2_0.GroundOverlayType;
import net.opengis.kml.v_2_2_0.PhotoOverlayType;
import net.opengis.kml.v_2_2_0.PlacemarkType;
import org.locationtech.jts.geom.Geometry;

public class KmlFeatureToJtsGeometryConverter {
  private KmlFeatureToJtsGeometryConverter() {}

  public static Geometry from(AbstractFeatureType kmlFeature) {
    if (kmlFeature == null) {
      return null;
    }

    if (kmlFeature instanceof DocumentType) {
      return KmlDocumentToJtsGeometryConverter.from((DocumentType) kmlFeature);
    }

    if (kmlFeature instanceof FolderType) {
      return KmlFolderToJtsGeometryConverter.from((FolderType) kmlFeature);
    }

    if (kmlFeature instanceof PlacemarkType) {
      return KmlPlacemarkToJtsGeometryConverter.from((PlacemarkType) kmlFeature);
    }

    if (kmlFeature instanceof PhotoOverlayType) {
      return KmlPhotoOverlayToJtsPointConverter.from((PhotoOverlayType) kmlFeature);
    }

    if (kmlFeature instanceof GroundOverlayType) {
      return KmlGroundOverlayToJtsGeometryConverter.from((GroundOverlayType) kmlFeature);
    }

    return null;
  }
}
