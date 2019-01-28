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
package ddf.catalog.pubsub.criteria.geospatial;

import static java.lang.Math.abs;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class GeospatialEvaluator {
  public static final String METADATA_DOD_MIL_CRS_WGS84E_2D =
      "http://metadata.dod.mil/mdr/ns/GSIP/crs/WGS84E_2D";

  public static final String EPSG_4326 = "EPSG:4326";

  private static final double ERROR_THRESHOLD = .000001;

  private static final Logger LOGGER = LoggerFactory.getLogger(GeospatialEvaluator.class);

  // If both criteria and input are GeometryCollections, each element of input must lie entirely
  // within one component
  // of criteria.
  private static boolean containsWithGeometryCollection(Geometry criteria, Geometry input) {
    for (int whichInput = 0; whichInput < input.getNumGeometries(); ++whichInput) {
      boolean thisInputOk = false;
      for (int whichCriteria = 0; whichCriteria < criteria.getNumGeometries(); ++whichCriteria) {
        if (criteria.getGeometryN(whichCriteria).contains(input.getGeometryN(whichInput))) {
          thisInputOk = true;
          break;
        }
      }
      if (!thisInputOk) {
        // We found an input component which is not in a criteria component
        return false;
      }
    }
    return true;
  }

  private static boolean overlapsWithGeometryCollection(Geometry criteria, Geometry input) {
    for (int i = 0; i < criteria.getNumGeometries(); ++i) {
      for (int j = 0; j < input.getNumGeometries(); ++j) {
        // The legacy interpretation of OVERLAPS corresponds better to a JTS INTERSECTS
        // Intersects means NOT DISJOINT. In other words the two geometries have at least
        // one point in common
        // JTS's interpretation of OVERLAPS is the geometries have some but NOT all points
        // in common. This
        // means that if geometry A is a large, and geometry B is smaller than A and is
        // completely inside A,
        // A and B DO NOT overlap.
        if (criteria.getGeometryN(i).intersects(input.getGeometryN(j))) {
          // Criteria overlaps input if any component of either overlaps a component of
          // the other.
          return true;
        }
      }
    }
    // Nothing overlapped anything else
    return false;
  }

  public static boolean evaluate(GeospatialEvaluationCriteria gec) {
    String methodName = "evaluate";
    LOGGER.trace("ENTERING: {}", methodName);

    String operation = gec.getOperation();
    Geometry input = gec.getInput();
    Geometry criteria = gec.getCriteria();
    double distance = gec.getDistance();

    LOGGER.debug("operation = {}", operation);

    boolean evaluation = false;

    if (abs(distance) < ERROR_THRESHOLD) {
      switch (SpatialOperator.valueOf(operation.toUpperCase())) {
        case CONTAINS:
          LOGGER.debug("Doing CONTAINS evaluation");
          evaluation = containsWithGeometryCollection(criteria, input);
          break;

        case OVERLAPS:
          LOGGER.debug("Doing OVERLAPS evaluation");
          evaluation = overlapsWithGeometryCollection(criteria, input);
          break;

          // Unsupported as of release DDF 2.0.0 10/24/11
          // case EQUALS:
          // evaluation = criteria.equals(input);
          // break;
          //
          // case DISJOINT:
          // evaluation = criteria.disjoint(input);
          // break;
          //
          // case INTERSECTS:
          // evaluation = criteria.intersects(input);
          // break;
          //
          // case TOUCHES:
          // evaluation = criteria.touches(input);
          // break;
          //
          // case CROSSES:
          // evaluation = criteria.crosses(input);
          // break;
          //
          // case WITHIN:
          // evaluation = criteria.within(input);
          // break;

        default:
          LOGGER.debug("Doing default evaluation - always false");
          evaluation = false;
          break;
      }
    } else {
      LOGGER.debug("Doing DISTANCE evaluation");

      // compare each geometry's closest distance to each other
      double distanceBetweenNearestPtsOnGeometries = DistanceOp.distance(input, criteria);
      LOGGER.debug(
          "distanceBetweenNearestPtsOnGeometries = {},    distance = {}",
          distanceBetweenNearestPtsOnGeometries,
          distance);
      evaluation = distanceBetweenNearestPtsOnGeometries <= distance;
    }

    LOGGER.debug("evaluation = {}", evaluation);

    LOGGER.trace("EXITING: {}", methodName);

    return evaluation;
  }

  public static Geometry buildGeometry(String gmlText)
      throws IOException, SAXException, ParserConfigurationException {
    String methodName = "buildGeometry";
    LOGGER.trace("ENTERING: {}", methodName);

    Geometry geometry = null;

    gmlText = supportSRSName(gmlText);

    try {
      LOGGER.debug("Creating geoTools Configuration ...");
      Configuration config = new org.geotools.gml3.GMLConfiguration();

      LOGGER.debug("Parsing geoTools configuration");
      Parser parser = new Parser(config);

      LOGGER.debug("Parsing gmlText");
      geometry = (Geometry) (parser.parse(new StringReader(gmlText)));
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("geometry (before conversion): {}", geometry.toText());
      }

      // The metadata schema states that <gml:pos> elements specify points in
      // LAT,LON order. But WKT specifies points in LON,LAT order. When the geoTools
      // libraries return the geometry data, it's WKT is in LAT,LON order (which is
      // incorrect).

      // As a workaround here, for Polygons and Points (which are currently the only spatial
      // criteria supported) we must swap the x,y of each coordinate so that they are
      // specified in LON,LAT order and then use the swapped coordinates to create a new
      // Polygon or Point to be returned to the caller.
      GeometryFactory geometryFactory = new GeometryFactory();

      if (geometry instanceof Polygon) {
        // Build new array of coordinates using the swapped coordinates
        ArrayList<Coordinate> newCoords = new ArrayList<Coordinate>();

        // Swap each coordinate's x,y so that they specify LON,LAT order
        for (Coordinate coord : geometry.getCoordinates()) {
          newCoords.add(new Coordinate(coord.y, coord.x));
        }

        // Create a new polygon using the swapped coordinates
        Polygon polygon =
            new Polygon(
                geometryFactory.createLinearRing(
                    newCoords.toArray(new Coordinate[newCoords.size()])),
                null,
                geometryFactory);

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Translates to {}", polygon.toText()); // this logs the transformed WKT
          // with LON,LAT ordered points
          LOGGER.trace("EXITING: {}", methodName);
        }

        return polygon;
      }

      if (geometry instanceof Point) {
        // Create a new point using the swapped coordinates that specify LON,LAT order
        Point point =
            geometryFactory.createPoint(
                new Coordinate(geometry.getCoordinate().y, geometry.getCoordinate().x));

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Translates to {}", point.toText()); // this logs the transformed WKT
          // with a LON,LAT ordered point
          LOGGER.trace("EXITING: {}", methodName);
        }

        return point;
      }
    } catch (Exception e) {
      LOGGER.debug("Exception using geotools", e);
    }

    LOGGER.debug("No translation done for geometry - probably not good ...");

    LOGGER.trace("EXITING: {}", methodName);

    return geometry;
  }

  public static String supportSRSName(String gml) {
    String methodName = "supportSRSName";
    LOGGER.trace("ENTERING: {}", methodName);

    if (gml.contains(METADATA_DOD_MIL_CRS_WGS84E_2D)) {
      gml = gml.replaceAll(Pattern.quote(METADATA_DOD_MIL_CRS_WGS84E_2D), EPSG_4326);
    }

    LOGGER.trace("EXITING: {}  --  gml = {}", methodName, gml);

    return gml;
  }
}
