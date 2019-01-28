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

import de.micromata.opengis.kml.v_2_2_0.Point;
import org.apache.commons.collections.CollectionUtils;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

public class KmlToJtsPointConverter {
  private static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();

  private KmlToJtsPointConverter() {}

  public static org.locationtech.jts.geom.Point from(Point kmlPoint) {
    if (!isValidKmlPoint(kmlPoint)) {
      return null;
    }

    // get(0) is valid because the KML documentation states that a point contains a single tuple
    // even though the Point object contains a list of coordinates.
    Coordinate jtsCoordinate = KmlToJtsCoordinateConverter.from(kmlPoint.getCoordinates().get(0));

    return GEOMETRY_FACTORY.createPoint(jtsCoordinate);
  }

  public static boolean isValidKmlPoint(Point kmlPoint) {
    return kmlPoint != null && !CollectionUtils.isEmpty(kmlPoint.getCoordinates());
  }
}
