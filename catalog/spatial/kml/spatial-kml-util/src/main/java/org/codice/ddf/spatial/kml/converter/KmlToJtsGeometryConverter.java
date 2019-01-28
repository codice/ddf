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

import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Model;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Polygon;

public class KmlToJtsGeometryConverter {

  private KmlToJtsGeometryConverter() {}

  public static org.locationtech.jts.geom.Geometry from(Geometry kmlGeometry) {
    if (kmlGeometry == null) {
      return null;
    }

    if (kmlGeometry instanceof Point) {
      return KmlToJtsPointConverter.from((Point) kmlGeometry);
    }

    if (kmlGeometry instanceof LineString) {
      return KmlToJtsLineStringConverter.from((LineString) kmlGeometry);
    }

    if (kmlGeometry instanceof LinearRing) {
      return KmlToJtsLinearRingConverter.from((LinearRing) kmlGeometry);
    }

    if (kmlGeometry instanceof Polygon) {
      return KmlToJtsPolygonConverter.from((Polygon) kmlGeometry);
    }

    if (kmlGeometry instanceof MultiGeometry) {
      return KmlToJtsMultiGeometryConverter.from((MultiGeometry) kmlGeometry);
    }

    if (kmlGeometry instanceof Model) {
      return KmlModelToJtsPointConverter.from((Model) kmlGeometry);
    }

    // Shouldn't get here
    return null;
  }
}
