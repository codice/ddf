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

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;

public class KmlToJtsCoordinateConverter {

  private KmlToJtsCoordinateConverter() {}

  public static org.locationtech.jts.geom.Coordinate from(Coordinate kmlCoordinate) {
    if (kmlCoordinate == null) {
      return null;
    }

    return new org.locationtech.jts.geom.Coordinate(
        kmlCoordinate.getLongitude(), kmlCoordinate.getLatitude(), kmlCoordinate.getAltitude());
  }

  public static org.locationtech.jts.geom.Coordinate[] from(List<Coordinate> kmlCoordinates) {
    if (CollectionUtils.isEmpty(kmlCoordinates)) {
      return new org.locationtech.jts.geom.Coordinate[0];
    }

    List<org.locationtech.jts.geom.Coordinate> jtsCoordinates =
        kmlCoordinates
            .stream()
            .filter(Objects::nonNull)
            .map(KmlToJtsCoordinateConverter::from)
            .collect(Collectors.toList());
    if (CollectionUtils.isEmpty(jtsCoordinates)) {
      return new org.locationtech.jts.geom.Coordinate[0];
    }

    return jtsCoordinates.toArray(new org.locationtech.jts.geom.Coordinate[0]);
  }
}
