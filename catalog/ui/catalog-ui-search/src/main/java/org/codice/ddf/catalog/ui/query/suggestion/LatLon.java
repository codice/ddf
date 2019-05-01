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
package org.codice.ddf.catalog.ui.query.suggestion;

import static org.apache.commons.lang3.Validate.notNull;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.math3.util.Precision;
import org.codice.usng4j.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to serialize lat/lon data for the frontend. While there exists several third
 * party classes that could have been used, taking this approach ensures that:
 *
 * <ol>
 *   <li>The serialization format lives within the application, nearby its usage.
 *   <li>The serialization format cannot break when third party dependencies are upgraded.
 * </ol>
 */
class LatLon {
  private static final Logger LOGGER = LoggerFactory.getLogger(LatLon.class);

  private final Double lat;
  private final Double lon;

  LatLon(Double lat, Double lon) {
    LOGGER.trace("Creating LatLon ({}, {})", lat, lon);
    this.lat = lat;
    this.lon = lon;
  }

  @Nullable
  public static LatLon createIfValid(Double lat, Double lon) {
    return (LatLon.isValidLatitude(lat) && LatLon.isValidLongitude(lon))
        ? new LatLon(lat, lon)
        : null;
  }

  Double getLat() {
    return lat;
  }

  Double getLon() {
    return lon;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LatLon latLon = (LatLon) o;
    return Precision.equals(lat, latLon.lat, .0000000001)
        && Precision.equals(lon, latLon.lon, .0000000001);
  }

  @Override
  public int hashCode() {
    int result = lat.hashCode();
    result = 31 * result + lon.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "(" + getLat() + ", " + getLon() + ")";
  }

  static boolean isValidLatitude(double latitude) {
    return latitude >= -90 && latitude <= 90;
  }

  static boolean isValidLongitude(double longitude) {
    return longitude >= -180 && longitude <= 180;
  }

  static List<LatLon> fromBoundingBox(BoundingBox bb) {
    LOGGER.trace("Creating LatLon from BoundingBox [{}]", bb);
    notNull(bb, "The provided bounding box cannot be null");
    return ImmutableList.of(
        new LatLon(bb.getSouth(), bb.getWest()),
        new LatLon(bb.getNorth(), bb.getWest()),
        new LatLon(bb.getNorth(), bb.getEast()),
        new LatLon(bb.getSouth(), bb.getEast()));
  }
}
