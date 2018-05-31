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

import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import ddf.catalog.transform.CatalogTransformerException;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

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

    com.vividsolutions.jts.geom.Geometry geo = readGeoFromWkt(wkt);
    Geometry kmlGeo = createKmlGeometry(geo);
    if (!POINT_TYPE.equals(geo.getGeometryType())) {
      kmlGeo = addPointToKmlGeo(kmlGeo, geo.getCoordinate());
    }
    return kmlGeo;
  }

  private static Geometry createKmlGeometry(com.vividsolutions.jts.geom.Geometry jtsGeometry)
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

  private static Geometry createGeometryCollectionGeo(com.vividsolutions.jts.geom.Geometry jtsGeo)
      throws CatalogTransformerException {
    List<Geometry> kmlGeos = new ArrayList<>();
    for (int i = 0; i < jtsGeo.getNumGeometries(); i++) {
      kmlGeos.add(createKmlGeometry(jtsGeo.getGeometryN(i)));
    }
    return KmlFactory.createMultiGeometry().withGeometry(kmlGeos);
  }

  private static Geometry createPolygonGeo(Polygon jtsPoly) {
    de.micromata.opengis.kml.v_2_2_0.Polygon kmlPoly = KmlFactory.createPolygon();
    List<Coordinate> kmlCoords =
        kmlPoly.createAndSetOuterBoundaryIs().createAndSetLinearRing().createAndSetCoordinates();
    for (com.vividsolutions.jts.geom.Coordinate coord : jtsPoly.getCoordinates()) {
      kmlCoords.add(new Coordinate(coord.x, coord.y));
    }
    return kmlPoly;
  }

  private static Geometry createLineStringGeo(LineString jtsLS) {
    de.micromata.opengis.kml.v_2_2_0.LineString kmlLS = KmlFactory.createLineString();
    List<Coordinate> kmlCoords = kmlLS.createAndSetCoordinates();
    for (com.vividsolutions.jts.geom.Coordinate coord : jtsLS.getCoordinates()) {
      kmlCoords.add(new Coordinate(coord.x, coord.y));
    }
    return kmlLS;
  }

  private static Geometry createPointGeo(Point jtsPoint) {
    return KmlFactory.createPoint().addToCoordinates(jtsPoint.getX(), jtsPoint.getY());
  }

  private static com.vividsolutions.jts.geom.Geometry readGeoFromWkt(final String wkt)
      throws CatalogTransformerException {
    try {
      return WKT_READER_THREAD_LOCAL.get().read(wkt);
    } catch (ParseException e) {
      throw new CatalogTransformerException("Unable to parse WKT to Geometry.", e);
    }
  }

  private static Geometry addPointToKmlGeo(
      Geometry kmlGeo, com.vividsolutions.jts.geom.Coordinate vertex) {
    if (null != vertex) {
      de.micromata.opengis.kml.v_2_2_0.Point kmlPoint =
          KmlFactory.createPoint().addToCoordinates(vertex.x, vertex.y);
      return KmlFactory.createMultiGeometry().addToGeometry(kmlPoint).addToGeometry(kmlGeo);
    } else {
      return null;
    }
  }
}
