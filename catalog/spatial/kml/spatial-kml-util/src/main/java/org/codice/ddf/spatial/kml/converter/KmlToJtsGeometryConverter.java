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

import net.opengis.kml.v_2_2_0.AbstractGeometryType;
import net.opengis.kml.v_2_2_0.LineStringType;
import net.opengis.kml.v_2_2_0.LinearRingType;
import net.opengis.kml.v_2_2_0.ModelType;
import net.opengis.kml.v_2_2_0.MultiGeometryType;
import net.opengis.kml.v_2_2_0.PointType;
import net.opengis.kml.v_2_2_0.PolygonType;

public class KmlToJtsGeometryConverter {

  private KmlToJtsGeometryConverter() {}

  public static org.locationtech.jts.geom.Geometry from(AbstractGeometryType kmlGeometry) {
    if (kmlGeometry == null) {
      return null;
    }

    if (kmlGeometry instanceof PointType) {
      return KmlToJtsPointConverter.from((PointType) kmlGeometry);
    }

    if (kmlGeometry instanceof LineStringType) {
      return KmlToJtsLineStringConverter.from((LineStringType) kmlGeometry);
    }

    if (kmlGeometry instanceof LinearRingType) {
      return KmlToJtsLinearRingConverter.from((LinearRingType) kmlGeometry);
    }

    if (kmlGeometry instanceof PolygonType) {
      return KmlToJtsPolygonConverter.from((PolygonType) kmlGeometry);
    }

    if (kmlGeometry instanceof MultiGeometryType) {
      return KmlToJtsMultiGeometryConverter.from((MultiGeometryType) kmlGeometry);
    }

    if (kmlGeometry instanceof ModelType) {
      return KmlModelToJtsPointConverter.from((ModelType) kmlGeometry);
    }

    // Shouldn't get here
    return null;
  }
}
