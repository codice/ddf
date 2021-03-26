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

import net.opengis.kml.v_2_2_0.LocationType;
import net.opengis.kml.v_2_2_0.ModelType;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class KmlModelToJtsPointConverter {
  private KmlModelToJtsPointConverter() {}

  public static Point from(ModelType kmlModel) {
    if (kmlModel == null || kmlModel.getLocation() == null) {
      return null;
    }

    LocationType kmlLocation = kmlModel.getLocation();

    Coordinate jtsCoordinate =
        new Coordinate(
            kmlLocation.getLongitude(), kmlLocation.getLatitude(), kmlLocation.getAltitude());

    GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

    return geometryFactory.createPoint(jtsCoordinate);
  }
}
