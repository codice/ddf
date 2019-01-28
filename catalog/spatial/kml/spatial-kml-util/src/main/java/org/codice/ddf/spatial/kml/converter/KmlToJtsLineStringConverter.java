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

import de.micromata.opengis.kml.v_2_2_0.LineString;
import org.apache.commons.collections.CollectionUtils;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

public class KmlToJtsLineStringConverter {
  private static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();

  private KmlToJtsLineStringConverter() {}

  public static org.locationtech.jts.geom.LineString from(LineString kmlLineString) {
    if (!isValidKmlLineString(kmlLineString)) {
      return null;
    }
    Coordinate[] jtsCoordinates = KmlToJtsCoordinateConverter.from(kmlLineString.getCoordinates());

    return GEOMETRY_FACTORY.createLineString(jtsCoordinates);
  }

  public static boolean isValidKmlLineString(LineString kmlLineString) {
    return kmlLineString != null && !CollectionUtils.isEmpty(kmlLineString.getCoordinates());
  }
}
