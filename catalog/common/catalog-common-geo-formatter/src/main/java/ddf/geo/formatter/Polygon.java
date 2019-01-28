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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.abdera.ext.geo.Position;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;

public class Polygon extends MultiPoint {

  public static final String TYPE = "Polygon";

  public Polygon(Geometry geometry) {
    super(geometry);
  }

  /**
   * @param coordinates a List of coordinates formatted in the GeoJSON Array equivalent
   * @return
   */
  public static CompositeGeometry toCompositeGeometry(List coordinates) {
    return new Polygon(buildPolygon(coordinates));
  }

  public static org.locationtech.jts.geom.Polygon buildPolygon(List coordinates) {

    // according to the GeoJson specification, first ring is the exterior
    LinearRing exterior =
        GEOMETRY_FACTORY.createLinearRing(getCoordinates((List) coordinates.get(0)));

    LinearRing[] interiorHoles = new LinearRing[coordinates.size() - 1];

    for (int i = 1; i < coordinates.size(); i++) {
      interiorHoles[i - 1] =
          GEOMETRY_FACTORY.createLinearRing(getCoordinates((List) coordinates.get(i)));
    }

    return GEOMETRY_FACTORY.createPolygon(exterior, interiorHoles);
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

      List linearRingsList = buildJsonPolygon((org.locationtech.jts.geom.Polygon) geometry);

      map.put(COORDINATES_KEY, linearRingsList);

    } else {
      throw new UnsupportedOperationException("Geometry is not a " + TYPE);
    }

    return map;
  }

  protected List buildJsonPolygon(org.locationtech.jts.geom.Polygon polygon) {
    List linearRingsList = new ArrayList();

    // According GeoJSON spec, first LinearRing is the exterior ring
    linearRingsList.add(buildCoordinatesList(polygon.getExteriorRing().getCoordinates()));

    for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
      linearRingsList.add(buildCoordinatesList(polygon.getInteriorRingN(i).getCoordinates()));
    }
    return linearRingsList;
  }

  @Override
  public List<Position> toGeoRssPositions() {

    org.apache.abdera.ext.geo.Coordinates coords =
        getPolygonCoordinates((org.locationtech.jts.geom.Polygon) geometry);

    return Arrays.asList((Position) (new org.apache.abdera.ext.geo.Polygon(coords)));
  }

  protected org.apache.abdera.ext.geo.Coordinates getPolygonCoordinates(
      org.locationtech.jts.geom.Polygon polygon) {

    org.apache.abdera.ext.geo.Coordinates coords = new org.apache.abdera.ext.geo.Coordinates();

    // it does not look like http://georss.org/simple or
    // http://georss.org/gml can handle
    // interior rings
    for (org.locationtech.jts.geom.Coordinate jtsCoordinate :
        polygon.getExteriorRing().getCoordinates()) {

      coords.add(convert(jtsCoordinate));
    }
    return coords;
  }
}
