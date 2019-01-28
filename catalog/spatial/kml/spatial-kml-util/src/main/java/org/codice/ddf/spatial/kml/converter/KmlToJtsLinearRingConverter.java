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

import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.util.CollectionUtils;

public class KmlToJtsLinearRingConverter {
  private KmlToJtsLinearRingConverter() {}

  public static org.locationtech.jts.geom.LinearRing from(LinearRing kmlLinearRing) {
    if (!isValidKmlLinearRing(kmlLinearRing)) {
      return null;
    }

    GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

    Coordinate[] jtsCoordinates = KmlToJtsCoordinateConverter.from(kmlLinearRing.getCoordinates());

    return geometryFactory.createLinearRing(jtsCoordinates);
  }

  public static boolean isValidKmlLinearRing(LinearRing kmlLinearRing) {
    return kmlLinearRing != null && !CollectionUtils.isEmpty(kmlLinearRing.getCoordinates());
  }
}
