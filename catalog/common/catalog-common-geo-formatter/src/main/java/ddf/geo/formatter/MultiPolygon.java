/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.geo.formatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.abdera.ext.geo.Position;
import org.locationtech.jts.geom.Geometry;

public class MultiPolygon extends Polygon {

  public static final String TYPE = "MultiPolygon";

  public MultiPolygon(Geometry geometry) {
    super(geometry);
  }

  public static CompositeGeometry toCompositeGeometry(List coordinates) {
    org.locationtech.jts.geom.Polygon[] allPolygons =
        new org.locationtech.jts.geom.Polygon[coordinates.size()];

    for (int i = 0; i < allPolygons.length; i++) {
      allPolygons[i] = buildPolygon((List) coordinates.get(i));
    }
    return new MultiPolygon(GEOMETRY_FACTORY.createMultiPolygon(allPolygons));
  }

  @Override
  protected boolean isNotType(Geometry geo) {
    return !TYPE.equals(geo.getGeometryType());
  }

  @Override
  public Map toJsonMap() {
    Map map = new HashMap();

    if (TYPE.equals(geometry.getGeometryType())) {

      map.put(TYPE_KEY, TYPE);

      List<List> listOfPolygons = new ArrayList<List>();

      for (int i = 0; i < geometry.getNumGeometries(); i++) {
        List polygon =
            buildJsonPolygon((org.locationtech.jts.geom.Polygon) geometry.getGeometryN(i));
        listOfPolygons.add(polygon);
      }

      map.put(COORDINATES_KEY, listOfPolygons);

    } else {
      throw new UnsupportedOperationException("Geometry is not a " + TYPE);
    }

    return map;
  }

  @Override
  public List<Position> toGeoRssPositions() {

    List<Position> positions = new ArrayList<Position>();

    if (null != geometry) {
      for (int i = 0; i < geometry.getNumGeometries(); i++) {
        CompositeGeometry compositeGeo =
            CompositeGeometry.getCompositeGeometry(geometry.getGeometryN(i));
        if (null != compositeGeo) {
          positions.addAll(compositeGeo.toGeoRssPositions());
        }
      }
    }

    return positions;
  }
}
