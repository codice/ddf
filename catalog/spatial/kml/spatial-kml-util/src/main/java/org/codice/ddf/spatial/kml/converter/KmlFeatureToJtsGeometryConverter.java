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

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;
import de.micromata.opengis.kml.v_2_2_0.PhotoOverlay;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import org.locationtech.jts.geom.Geometry;

public class KmlFeatureToJtsGeometryConverter {
  private KmlFeatureToJtsGeometryConverter() {}

  public static Geometry from(Feature kmlFeature) {
    if (kmlFeature == null) {
      return null;
    }

    if (kmlFeature instanceof Document) {
      return KmlDocumentToJtsGeometryConverter.from((Document) kmlFeature);
    }

    if (kmlFeature instanceof Folder) {
      return KmlFolderToJtsGeometryConverter.from((Folder) kmlFeature);
    }

    if (kmlFeature instanceof Placemark) {
      return KmlPlacemarkToJtsGeometryConverter.from((Placemark) kmlFeature);
    }

    if (kmlFeature instanceof PhotoOverlay) {
      return KmlPhotoOverlayToJtsPointConverter.from((PhotoOverlay) kmlFeature);
    }

    if (kmlFeature instanceof GroundOverlay) {
      return KmlGroundOverlayToJtsGeometryConverter.from((GroundOverlay) kmlFeature);
    }

    return null;
  }
}
