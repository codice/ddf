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
package org.codice.ddf.opensearch.source;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BoundingBoxUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(BoundingBoxUtils.class);

  private BoundingBoxUtils() {}

  // constants for Vincenty's formula
  // length of semi-major axis of the Earth (radius at equator) = 6378137.0 metres in WGS-84
  private static final double LENGTH_OF_SEMI_MAJOR_AXIS_IN_METERS = 6378137.0;

  // flattening of the Earth = 1/298.257223563 in WGS-84
  private static final double FLATTENING = 1 / 298.257223563;

  // length of semi-minor axis of the Earth (radius at the poles) = 6356752.314245 meters in WGS-84
  private static final double LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS =
      (1 - FLATTENING) * LENGTH_OF_SEMI_MAJOR_AXIS_IN_METERS;

  private static final int MAXIMUM_VINCENTYS_FORMULA_ITERATIONS = 100;
  // end constants for Vincenty's formula

  private static final double MAX_LAT = 90;

  private static final double MIN_LAT = -90;

  private static final double MAX_LON = 180;

  private static final double MIN_LON = -180;

  private static final double FULL_LON_ROTATION = MAX_LON - MIN_LON;

  /**
   * Takes in a {@link PointRadius} search and converts it to a (rough approximation) bounding box
   * using Vincenty's formula (direct) and the WGS-84 approximation of the Earth.
   */
  public static BoundingBox createBoundingBox(PointRadius pointRadius) {
    final double lonInDegrees = pointRadius.getLon();
    final double latInDegrees = pointRadius.getLat();
    final double searchRadiusInMeters = pointRadius.getRadius();

    final double latDifferenceInDegrees =
        Math.toDegrees(searchRadiusInMeters / LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS);

    double west;
    double south = latInDegrees - latDifferenceInDegrees;
    double east;
    double north = latInDegrees + latDifferenceInDegrees;

    if (south > MIN_LAT && north < MAX_LAT) {
      final double latInRadians = Math.toRadians(latInDegrees);

      final double tanU1 = (1 - FLATTENING) * Math.tan(latInRadians);
      final double cosU1 = 1 / Math.sqrt((1 + tanU1 * tanU1));
      final double sigma1 = Math.atan2(tanU1, 0);
      final double cosSquaredAlpha = 1 - cosU1 * cosU1;
      final double uSq =
          cosSquaredAlpha
              * (LENGTH_OF_SEMI_MAJOR_AXIS_IN_METERS * LENGTH_OF_SEMI_MAJOR_AXIS_IN_METERS
                  - LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS * LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS)
              / (LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS * LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS);
      final double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
      final double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));

      double cos2sigmaM;
      double sinSigma;
      double cosSigma;
      double deltaSigma;

      double sigma = searchRadiusInMeters / (LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS * A);
      double oldSigma;

      int iterationCount = 0;
      do {
        if (iterationCount > MAXIMUM_VINCENTYS_FORMULA_ITERATIONS) {
          LOGGER.debug(
              "Vincenty's formula failed to converge after {} iterations. Unable to calculate a bounding box for lon={} degrees, lat={} degrees, search radius={} meters.",
              MAXIMUM_VINCENTYS_FORMULA_ITERATIONS,
              lonInDegrees,
              latInDegrees,
              searchRadiusInMeters);
          throw new IllegalArgumentException(
              "Unable to create bounding box using Vincenty's formula");
        }

        cos2sigmaM = Math.cos(2 * sigma1 + sigma);
        sinSigma = Math.sin(sigma);
        cosSigma = Math.cos(sigma);
        deltaSigma =
            B
                * sinSigma
                * (cos2sigmaM
                    + B
                        / 4
                        * (cosSigma * (-1 + 2 * cos2sigmaM * cos2sigmaM)
                            - B
                                / 6
                                * cos2sigmaM
                                * (-3 + 4 * sinSigma * sinSigma)
                                * (-3 + 4 * cos2sigmaM * cos2sigmaM)));
        oldSigma = sigma;
        sigma = searchRadiusInMeters / (LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS * A) + deltaSigma;

        iterationCount++;
      } while (Math.abs(sigma - oldSigma) > 1e-12);

      final double lambda = Math.atan2(sinSigma, cosU1 * cosSigma);
      final double C =
          FLATTENING / 16 * cosSquaredAlpha * (4 + FLATTENING * (4 - 3 * cosSquaredAlpha));
      final double L =
          lambda
              - (1 - C)
                  * FLATTENING
                  * cosU1
                  * (sigma
                      + C
                          * sinSigma
                          * (cos2sigmaM + C * cosSigma * (-1 + 2 * cos2sigmaM * cos2sigmaM)));

      final double xDifferenceInDegrees = Math.toDegrees(L);

      west = lonInDegrees - xDifferenceInDegrees;
      if (west < MIN_LON) {
        west += FULL_LON_ROTATION;
      }

      east = lonInDegrees + xDifferenceInDegrees;
      if (east > MAX_LON) {
        east -= FULL_LON_ROTATION;
      }
    } else {
      // The search area overlaps one of the poles.
      west = MIN_LON;
      south = Math.max(south, MIN_LAT);
      east = MAX_LON;
      north = Math.min(north, MAX_LAT);
    }

    return new BoundingBox(west, south, east, north);
  }

  /**
   * Takes in a {@link Polygon} and converts it to a (rough approximation) {@link BoundingBox}.
   *
   * <p>Note: Searches being performed where the polygon goes through the antimeridian will return
   * an incorrect bounding box. TODO DDF-3742
   */
  public static BoundingBox createBoundingBox(Polygon polygon) {
    double west = Double.POSITIVE_INFINITY;
    double south = Double.POSITIVE_INFINITY;
    double east = Double.NEGATIVE_INFINITY;
    double north = Double.NEGATIVE_INFINITY;

    for (Coordinate coordinate : polygon.getCoordinates()) {
      final double lon = coordinate.x;
      final double lat = coordinate.y;

      if (lon < west) {
        west = lon;
      }
      if (lon > east) {
        east = lon;
      }
      if (lat < south) {
        south = lat;
      }
      if (lat > north) {
        north = lat;
      }
    }

    return new BoundingBox(west, south, east, north);
  }

  /**
   * Takes in a {@link BoundingBox} and extracts the coordinates in a counter clockwise matter with
   * first and last coordinate being the same point
   *
   * @param boundingBox
   * @return
   */
  public static List<List> getBoundingBoxCoordinatesList(BoundingBox boundingBox) {
    List<List> coordinates = new ArrayList<>();
    List coordinate = new ArrayList<>();
    coordinate.add(boundingBox.getWest());
    coordinate.add(boundingBox.getSouth());
    coordinates.add(coordinate);

    coordinate = new ArrayList<>();
    coordinate.add(boundingBox.getEast());
    coordinate.add(boundingBox.getSouth());
    coordinates.add(coordinate);

    coordinate = new ArrayList<>();
    coordinate.add(boundingBox.getEast());
    coordinate.add(boundingBox.getNorth());
    coordinates.add(coordinate);

    coordinate = new ArrayList<>();
    coordinate.add(boundingBox.getWest());
    coordinate.add(boundingBox.getNorth());
    coordinates.add(coordinate);

    coordinate = new ArrayList<>();
    coordinate.add(boundingBox.getWest());
    coordinate.add(boundingBox.getSouth());
    coordinates.add(coordinate);

    return coordinates;
  }
}
