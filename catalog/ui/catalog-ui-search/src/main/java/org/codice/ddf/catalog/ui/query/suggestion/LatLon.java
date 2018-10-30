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

class LatLon {
  private final Double lat;
  private final Double lon;

  LatLon(Double lat, Double lon) {
    this.lat = lat;
    this.lon = lon;
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
    return lat.equals(latLon.lat) && lon.equals(latLon.lon);
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
}
