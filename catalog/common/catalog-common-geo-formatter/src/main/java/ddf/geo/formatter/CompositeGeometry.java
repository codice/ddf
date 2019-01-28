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

import java.util.List;
import java.util.Map;
import org.apache.abdera.ext.geo.Position;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Abstract class that represents the geometric portion of GeoJSON.
 *
 * @author Ashraf Barakat
 */
public abstract class CompositeGeometry {

  public static final String TYPE_KEY = "type";

  public static final String COORDINATES_KEY = "coordinates";

  public static final String PROPERTIES_KEY = "properties";

  public static final String GEOMETRY_KEY = "geometry";

  public static final String GEOMETRIES_KEY = "geometries";

  protected static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  /**
   * Deciphers the {@link CompositeGeometry} object to return based on {@link Geometry}
   *
   * @param geometry
   * @return {@link CompositeGeometry}
   */
  public static CompositeGeometry getCompositeGeometry(Geometry geometry) {

    if (geometry != null) {
      if (Point.TYPE.equals(geometry.getGeometryType())) {
        return new Point(geometry);
      }
      if (LineString.TYPE.equals(geometry.getGeometryType())) {
        return new LineString(geometry);
      }
      if (MultiPoint.TYPE.equals(geometry.getGeometryType())) {
        return new MultiPoint(geometry);
      }
      if (MultiLineString.TYPE.equals(geometry.getGeometryType())) {
        return new MultiLineString(geometry);
      }
      if (Polygon.TYPE.equals(geometry.getGeometryType())) {
        return new Polygon(geometry);
      }
      if (MultiPolygon.TYPE.equals(geometry.getGeometryType())) {
        return new MultiPolygon(geometry);
      }
      if (GeometryCollection.TYPE.equals(geometry.getGeometryType())) {
        return new GeometryCollection(geometry);
      }
    }
    return null;
  }

  /**
   * @param type case-sensitive String of the geometric type, must match the exact spelling in the
   *     GeoJSON standard (geojson.org)
   * @param jsonObject Map of coordinates or geometries
   * @return {@link CompositeGeometry} based on the <code>type</code> passed in, returns <code>null
   *     </code> if the type is not one of the standard GeoJSON geometric strings.
   */
  public static CompositeGeometry getCompositeGeometry(String type, Map jsonObject) {
    if (type != null) {
      if (GeometryCollection.TYPE.equals(type)) {
        List geometries = (List) jsonObject.get(CompositeGeometry.GEOMETRIES_KEY);
        return GeometryCollection.toCompositeGeometry(geometries);
      } else {

        List coordinates = (List) jsonObject.get(CompositeGeometry.COORDINATES_KEY);

        if (Point.TYPE.equals(type)) {

          return Point.toCompositeGeometry(coordinates);
        }
        if (LineString.TYPE.equals(type)) {

          return LineString.toCompositeGeometry(coordinates);
        }
        if (MultiPoint.TYPE.equals(type)) {

          return MultiPoint.toCompositeGeometry(coordinates);
        }
        if (MultiLineString.TYPE.equals(type)) {

          return MultiLineString.toCompositeGeometry(coordinates);
        }
        if (Polygon.TYPE.equals(type)) {

          return Polygon.toCompositeGeometry(coordinates);
        }
        if (MultiPolygon.TYPE.equals(type)) {

          return MultiPolygon.toCompositeGeometry(coordinates);
        }
      }
    }
    return null;
  }

  /**
   * @param primitiveCoordinates requires x and y coordinate information as a {@link List}, x
   *     coordinate is the first item in the list
   * @return {@link Coordinate} object
   */
  protected static Coordinate getCoordinate(List primitiveCoordinates) {
    if (primitiveCoordinates != null && !primitiveCoordinates.isEmpty()) {
      double x = getDouble(primitiveCoordinates.get(0));
      double y = getDouble(primitiveCoordinates.get(1));
      return new Coordinate(x, y);
    } else {
      return new Coordinate();
    }
  }

  /**
   * This method is to retrieve numerical information when it is not known how that number will be
   * provided beforehand.
   *
   * @param object
   * @return double
   */
  private static double getDouble(Object object) {
    if (object instanceof Double) {
      return (Double) object;
    } else {
      if (object != null) {
        return Double.valueOf(object.toString());
      } else {
        return 0.0;
      }
    }
  }

  /**
   * @param primitiveCoordinatesList a List of [x,y] coordinates. The [x,y] coordinates is a list of
   *     two objects where x coordinate is first and the y coordinate is second in the list
   * @return an array of {@link Coordinate} objects, if input is <code>null</code> or empty, it will
   *     return an empty array
   */
  protected static Coordinate[] getCoordinates(List<List> primitiveCoordinatesList) {
    if (primitiveCoordinatesList != null && !primitiveCoordinatesList.isEmpty()) {
      Coordinate[] coordinatesArray = new Coordinate[primitiveCoordinatesList.size()];

      for (int i = 0; i < coordinatesArray.length; i++) {
        coordinatesArray[i] = getCoordinate(primitiveCoordinatesList.get(i));
      }
      return coordinatesArray;
    } else {
      return new Coordinate[0];
    }
  }

  /**
   * Creates a Map that mimics the GeoJSON structure.
   *
   * @throws UnsupportedOperationException when the object cannot write into the map
   */
  public abstract Map toJsonMap() throws UnsupportedOperationException;

  /** @return well-known text of underlying equivalent Geometric object */
  public abstract String toWkt();

  public abstract List<Position> toGeoRssPositions();

  /** @return equivalent geometric object */
  public abstract Geometry getGeometry();
}
