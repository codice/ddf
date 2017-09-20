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
package org.codice.ddf.spatial.geocoding.context.impl;

import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.shape.Point;

public class NearbyLocationImpl implements NearbyLocation {
  private static final SpatialContext SPATIAL_CONTEXT = SpatialContext.GEO;

  private final String name;

  private final String cardinalDirection;

  private final double distanceInKm;

  /**
   * Constructs a {@code NearbyLocationImpl} that describes the position of {@code sourceLocation}
   * relative to {@code nearbyLocation}.
   *
   * @param sourceLocation the primary location
   * @param nearbyLocation the location close to {@code sourceLocation}
   * @param nearbyLocationName the name of the nearby location
   */
  public NearbyLocationImpl(
      final Point sourceLocation, final Point nearbyLocation, final String nearbyLocationName) {
    final double distanceInKm =
        SPATIAL_CONTEXT.calcDistance(sourceLocation, nearbyLocation) * DistanceUtils.DEG_TO_KM;

    final double bearingToSource = getBearing(nearbyLocation, sourceLocation);

    final String cardinalDirectionToSource = bearingToCardinalDirection(bearingToSource);

    this.distanceInKm = distanceInKm;
    this.cardinalDirection = cardinalDirectionToSource;
    this.name = nearbyLocationName;
  }

  /**
   * Calculates the bearing from the start point to the end point (i.e., the <em>initial bearing
   * </em>) in degrees.
   *
   * @param startPoint the point from which to start
   * @param endPoint the point at which to end
   * @return the bearing from {@code startPoint} to {@code endPoint}, in degrees
   */
  private static double getBearing(final Point startPoint, final Point endPoint) {
    final double lat1 = startPoint.getY();
    final double lon1 = startPoint.getX();

    final double lat2 = endPoint.getY();
    final double lon2 = endPoint.getX();

    final double lonDiffRads = Math.toRadians(lon2 - lon1);
    final double lat1Rads = Math.toRadians(lat1);
    final double lat2Rads = Math.toRadians(lat2);
    final double y = Math.sin(lonDiffRads) * Math.cos(lat2Rads);
    final double x =
        Math.cos(lat1Rads) * Math.sin(lat2Rads)
            - Math.sin(lat1Rads) * Math.cos(lat2Rads) * Math.cos(lonDiffRads);

    return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
  }

  /**
   * Takes a bearing in degrees and returns the corresponding cardinal direction as a string.
   *
   * @param bearing the bearing, in degrees
   * @return the cardinal direction corresponding to {@code bearing} (N, NE, E, SE, S, SW, W, NW)
   */
  private static String bearingToCardinalDirection(final double bearing) {
    final String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
    return directions[(int) Math.round(bearing / 45)];
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getCardinalDirection() {
    return cardinalDirection;
  }

  @Override
  public double getDistance() {
    return distanceInKm;
  }
}
