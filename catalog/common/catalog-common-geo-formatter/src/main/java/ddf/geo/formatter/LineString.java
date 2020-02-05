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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.abdera.ext.geo.Coordinates;
import org.apache.abdera.ext.geo.Line;
import org.apache.abdera.ext.geo.Position;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class LineString extends MultiPoint {

  public static final String TYPE = "LineString";

  public LineString(Geometry geometry) {
    super(geometry);
  }

  public static CompositeGeometry toCompositeGeometry(List coordinates) {
    return new LineString(GEOMETRY_FACTORY.createLineString(getCoordinates(coordinates)));
  }

  @Override
  public Map toJsonMap() {

    return createMap(COORDINATES_KEY, buildCoordinatesList(geometry.getCoordinates()));
  }

  @Override
  protected boolean isNotType(Geometry geo) {
    return !TYPE.equals(geo.getGeometryType());
  }

  @Override
  public List<Position> toGeoRssPositions() {

    Coordinates coordinates = getLineStringCoordinates(geometry);

    return Arrays.asList((Position) new Line(coordinates));
  }

  protected Coordinates getLineStringCoordinates(Geometry geometry) {
    Coordinates coordinates = new Coordinates();

    for (int i = 0; i < geometry.getCoordinates().length; i++) {

      Coordinate coordinate = geometry.getCoordinates()[i];

      coordinates.add(convert(coordinate));
    }
    return coordinates;
  }
}
