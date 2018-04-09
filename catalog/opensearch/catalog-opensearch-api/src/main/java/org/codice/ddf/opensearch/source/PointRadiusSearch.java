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

import java.util.Objects;

public class PointRadiusSearch {

  private double lon; // EPSG:4326 decimal degrees

  private double lat; // EPSG:4326 decimal degrees

  private double radius; // meters

  public PointRadiusSearch(double lon, double lat, double radius) {
    this.lon = lon;
    this.lat = lat;
    this.radius = radius;
  }

  public double getLon() {
    return lon;
  }

  public double getLat() {
    return lat;
  }

  public double getRadius() {
    return radius;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (!(object instanceof PointRadiusSearch)) {
      return false;
    }

    final PointRadiusSearch pointRadiusSearch = (PointRadiusSearch) object;
    return Double.compare(pointRadiusSearch.getLon(), getLon()) == 0
        && Double.compare(pointRadiusSearch.getLat(), getLat()) == 0
        && Double.compare(pointRadiusSearch.getRadius(), getRadius()) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLon(), getLat(), getRadius());
  }
}
