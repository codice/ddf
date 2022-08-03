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
package ddf.catalog.transformer;

import ddf.catalog.transform.CatalogTransformerException;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.apache.commons.lang.StringUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

class GeometryUtils {
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  private GeometryUtils() {}

  static Geometry parseGeometry(String wkt) throws CatalogTransformerException {
    if (StringUtils.isBlank(wkt)) {
      throw new CatalogTransformerException(
          "The metacard has no location: the overlay image cannot be rotated.");
    }

    try {
      return new WKTReader().read(wkt);
    } catch (ParseException e) {
      throw new CatalogTransformerException(e);
    }
  }

  /**
   * Takes a four-sided polygon and returns a new polygon with the same vertices as the original,
   * but with the vertex assumed to be the upper-left of the polygon as the first vertex.
   *
   * <p>To determine which vertex is supposed to be the upper-left, this method first locates a
   * vertex of {@code polygon} on the northern edge of its bounding box. Then it looks at the two
   * adjacent vertices, and assumes the vertex with the greater latitude is the other vertex of the
   * polygon's northern edge. The upper-left is the first of the two vertices on this northern edge
   * when considering them in a clockwise order.
   *
   * @param polygon a polygon with 4 sides
   * @return a new, normalized {@link Polygon}, or the original {@code polygon} reference with no
   *     changes to the object if {@code polygon} does not have exactly 4 sides
   */
  static Polygon normalizePolygon(final Polygon polygon) {
    if (polygon.getNumPoints() != 5) {
      return polygon;
    }

    final Coordinate[] uniqueCoordinates = Arrays.copyOfRange(polygon.getCoordinates(), 0, 4);
    final Envelope bbox = polygon.getEnvelopeInternal();
    final int northEdgeIndex =
        IntStream.range(0, uniqueCoordinates.length)
            .filter(index -> uniqueCoordinates[index].y == bbox.getMaxY())
            .findFirst()
            .orElse(0);

    final int leftVertexIndex = northEdgeIndex == 0 ? 3 : northEdgeIndex - 1;
    final int rightVertexIndex = northEdgeIndex == 3 ? 0 : northEdgeIndex + 1;
    final int ulIndex =
        uniqueCoordinates[leftVertexIndex].y > uniqueCoordinates[rightVertexIndex].y
            ? leftVertexIndex
            : northEdgeIndex;

    final Coordinate[] normalizedCoordinates = new Coordinate[5];
    final int firstCopyLength = uniqueCoordinates.length - ulIndex;
    System.arraycopy(uniqueCoordinates, ulIndex, normalizedCoordinates, 0, firstCopyLength);
    System.arraycopy(
        uniqueCoordinates, 0, normalizedCoordinates, firstCopyLength, 5 - firstCopyLength);
    return GEOMETRY_FACTORY.createPolygon(normalizedCoordinates);
  }

  static boolean canHandleGeometry(Geometry geometry) {
    return geometry instanceof Polygon && geometry.getCoordinates().length == 5;
  }

  static boolean crossesDateline(final Geometry geometry) {
    return geometry.getEnvelopeInternal().getWidth() > 180;
  }

  static void adjustForDateline(final Geometry geometry) {
    geometry.apply(
        new CoordinateSequenceFilter() {
          private boolean done = false;

          @Override
          public void filter(final CoordinateSequence seq, final int i) {
            final Coordinate coordinate = seq.getCoordinate(i);
            if (coordinate.getX() < 0) {
              coordinate.setX(coordinate.getX() + 360);
            }
            done = (i == 4);
          }

          @Override
          public boolean isDone() {
            return done;
          }

          @Override
          public boolean isGeometryChanged() {
            return true;
          }
        });
  }
}
