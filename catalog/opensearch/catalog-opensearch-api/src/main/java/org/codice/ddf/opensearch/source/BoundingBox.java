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

/**
 * Using a custom class here instead of {@link org.locationtech.jts.geom.Envelope} because {@link
 * #west} may be greater than {@link #east} in a bounding box but not in an {@link
 * org.locationtech.jts.geom.Envelope}.
 */
public class BoundingBox {

  private final double west;

  private final double south;

  private final double east;

  private final double north;

  public BoundingBox(double west, double south, double east, double north) {
    this.west = west;
    this.south = south;
    this.east = east;
    this.north = north;
  }

  public double getWest() {
    return west;
  }

  public double getSouth() {
    return south;
  }

  public double getEast() {
    return east;
  }

  public double getNorth() {
    return north;
  }
}
