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

public class GeometryCollection extends MultiPolygon {

  public static final String TYPE = "GeometryCollection";

  public GeometryCollection(Geometry geometry) {
    super(geometry);
  }

  public static CompositeGeometry toCompositeGeometry(List geometries) {
    Geometry[] allGeometries = new Geometry[geometries.size()];

    for (int i = 0; i < allGeometries.length; i++) {
      Map jsonGeometry = (Map) geometries.get(i);
      allGeometries[i] =
          getCompositeGeometry(jsonGeometry.get(TYPE_KEY).toString(), jsonGeometry).getGeometry();
    }
    return new GeometryCollection(GEOMETRY_FACTORY.createGeometryCollection(allGeometries));
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

      List<Map> listOfGeometries = new ArrayList<Map>();

      for (int i = 0; i < geometry.getNumGeometries(); i++) {
        CompositeGeometry compositeGeo = getCompositeGeometry(geometry.getGeometryN(i));
        if (null != compositeGeo) {
          listOfGeometries.add(compositeGeo.toJsonMap());
        }
      }

      map.put(GEOMETRIES_KEY, listOfGeometries);

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
