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

import static java.lang.Math.abs;

import de.micromata.opengis.kml.v_2_2_0.LatLonBox;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;

public class KmlLatLonBoxToJtsGeometryConverter {
  private static final double ERROR_THRESHOLD = .000001;

  private KmlLatLonBoxToJtsGeometryConverter() {}

  public static Geometry from(LatLonBox kmlLatLonBox) {
    if (!isValidKmlLatLonBox(kmlLatLonBox)) {
      return null;
    }

    GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

    return geometryFactory.createPolygon(createLinearRing(geometryFactory, kmlLatLonBox), null);
  }

  private static LinearRing createLinearRing(GeometryFactory geometryFactory, LatLonBox latLonBox) {
    double minX = latLonBox.getWest();
    double maxX = latLonBox.getEast();
    if (minX > maxX) {
      minX = maxX;
      maxX = latLonBox.getWest();
    }

    double minY = latLonBox.getSouth();
    double maxY = latLonBox.getNorth();
    if (minY > maxY) {
      minY = maxY;
      maxY = latLonBox.getSouth();
    }

    // WKT wants the bounding box to start upper right and go clockwise
    return geometryFactory.createLinearRing(
        new Coordinate[] {
          new Coordinate(maxX, maxY),
          new Coordinate(maxX, minY),
          new Coordinate(minX, minY),
          new Coordinate(minX, maxY),
          new Coordinate(maxX, maxY)
        });
  }

  public static boolean isValidKmlLatLonBox(LatLonBox latLonBox) {
    if (latLonBox == null) {
      return false;
    }

    // check for empty lat lon box using .000001 as an error threshold
    return ((abs(latLonBox.getNorth()) > ERROR_THRESHOLD)
        || (abs(latLonBox.getSouth()) > ERROR_THRESHOLD)
        || (abs(latLonBox.getEast()) > ERROR_THRESHOLD)
        || (abs(latLonBox.getWest()) > ERROR_THRESHOLD));
  }
}
