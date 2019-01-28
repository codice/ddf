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
package org.codice.ddf.spatial.kml.converter;

import ddf.catalog.transform.CatalogTransformerException;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class MetacardToKml {

  private static final String POINT_TYPE = "Point";

  private static final String LINES_STRING_TYPE = "LineString";

  private static final String POLYGON_TYPE = "Polygon";

  private static final ThreadLocal<WKTReader> WKT_READER_THREAD_LOCAL =
      ThreadLocal.withInitial(WKTReader::new);

  /**
   * Convert wkt string into JTS Geometry and then from JTS to KML Geo.
   *
   * @param wkt
   * @return
   * @throws CatalogTransformerException
   */
  public static Geometry getKmlGeoFromWkt(final String wkt) throws CatalogTransformerException {
    if (StringUtils.isBlank(wkt)) {
      throw new CatalogTransformerException(
          "WKT was null or empty. Unable to preform KML Transform on Metacard.");
    }

    org.locationtech.jts.geom.Geometry geo = getJtsGeoFromWkt(wkt);
    return getKmlGeoFromJtsGeo(geo);
  }

  public static Geometry addJtsGeoPointsToKmlGeo(
      org.locationtech.jts.geom.Geometry jtsGeo, Geometry kmlGeo) {
    if (!POINT_TYPE.equals(jtsGeo.getGeometryType())) {
      kmlGeo = addJtsCoordinateToKmlGeo(kmlGeo, jtsGeo.getCoordinate());
    }
    return kmlGeo;
  }

  public static Geometry getKmlGeoFromJtsGeo(org.locationtech.jts.geom.Geometry jtsGeometry)
      throws CatalogTransformerException {
    Geometry kmlGeometry;
    if (POINT_TYPE.equals(jtsGeometry.getGeometryType())) {
      kmlGeometry = createPointGeo((Point) jtsGeometry);
    } else if (LINES_STRING_TYPE.equals(jtsGeometry.getGeometryType())) {
      kmlGeometry = createLineStringGeo((LineString) jtsGeometry);
    } else if (POLYGON_TYPE.equals(jtsGeometry.getGeometryType())) {
      kmlGeometry = createPolygonGeo((Polygon) jtsGeometry);
    } else if (jtsGeometry instanceof GeometryCollection) {
      kmlGeometry = createGeometryCollectionGeo(jtsGeometry);
    } else {
      throw new CatalogTransformerException(
          "Unknown / Unsupported Geometry Type '"
              + jtsGeometry.getGeometryType()
              + "'. Unale to preform KML Transform.");
    }
    return kmlGeometry;
  }

  private static Geometry createGeometryCollectionGeo(org.locationtech.jts.geom.Geometry jtsGeo)
      throws CatalogTransformerException {
    List<Geometry> kmlGeos = new ArrayList<>();
    for (int i = 0; i < jtsGeo.getNumGeometries(); i++) {
      kmlGeos.add(getKmlGeoFromJtsGeo(jtsGeo.getGeometryN(i)));
    }
    return KmlFactory.createMultiGeometry().withGeometry(kmlGeos);
  }

  private static Geometry createPolygonGeo(Polygon jtsPoly) {
    de.micromata.opengis.kml.v_2_2_0.Polygon kmlPoly = KmlFactory.createPolygon();
    List<Coordinate> kmlCoords =
        kmlPoly.createAndSetOuterBoundaryIs().createAndSetLinearRing().createAndSetCoordinates();
    for (org.locationtech.jts.geom.Coordinate coord : jtsPoly.getCoordinates()) {
      kmlCoords.add(new Coordinate(coord.x, coord.y));
    }
    return kmlPoly;
  }

  private static Geometry createLineStringGeo(LineString jtsLS) {
    de.micromata.opengis.kml.v_2_2_0.LineString kmlLS = KmlFactory.createLineString();
    List<Coordinate> kmlCoords = kmlLS.createAndSetCoordinates();
    for (org.locationtech.jts.geom.Coordinate coord : jtsLS.getCoordinates()) {
      kmlCoords.add(new Coordinate(coord.x, coord.y));
    }
    return kmlLS;
  }

  private static Geometry createPointGeo(Point jtsPoint) {
    return KmlFactory.createPoint().addToCoordinates(jtsPoint.getX(), jtsPoint.getY());
  }

  public static org.locationtech.jts.geom.Geometry getJtsGeoFromWkt(@Nullable final String wkt)
      throws CatalogTransformerException {

    if (StringUtils.isBlank(wkt)) {
      throw new CatalogTransformerException(
          "WKT was null or empty. Unable to convert WKT to JTS Geometry.");
    }

    try {
      return WKT_READER_THREAD_LOCAL.get().read(wkt);
    } catch (ParseException e) {
      throw new CatalogTransformerException("Unable to parse WKT to Geometry.", e);
    }
  }

  private static Geometry addJtsCoordinateToKmlGeo(
      Geometry kmlGeo, org.locationtech.jts.geom.Coordinate vertex) {
    if (null != vertex) {
      de.micromata.opengis.kml.v_2_2_0.Point kmlPoint =
          KmlFactory.createPoint().addToCoordinates(vertex.x, vertex.y);
      return KmlFactory.createMultiGeometry().addToGeometry(kmlPoint).addToGeometry(kmlGeo);
    } else {
      return null;
    }
  }
}
