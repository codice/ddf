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

import de.micromata.opengis.kml.v_2_2_0.PhotoOverlay;
import org.locationtech.jts.geom.Point;

public class KmlPhotoOverlayToJtsPointConverter {
  private KmlPhotoOverlayToJtsPointConverter() {}

  public static Point from(PhotoOverlay kmlPhotoOverlay) {
    if (!isValidKmlPhotoOverlay(kmlPhotoOverlay)) {
      return null;
    }

    return KmlToJtsPointConverter.from(kmlPhotoOverlay.getPoint());
  }

  public static boolean isValidKmlPhotoOverlay(PhotoOverlay kmlPhotoOverlay) {
    if (kmlPhotoOverlay == null) {
      return false;
    }

    return KmlToJtsPointConverter.isValidKmlPoint(kmlPhotoOverlay.getPoint());
  }
}
