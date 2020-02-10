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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.source;

import static java.util.Arrays.asList;

import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

class LatLonCoordinateStrategy implements CoordinateStrategy {
  @Override
  public String toString(final Coordinate coordinate) {
    return coordinate.y + "," + coordinate.x;
  }

  @Override
  public List<Double> lowerCorner(final Envelope envelope) {
    return asList(envelope.getMinY(), envelope.getMinX());
  }

  @Override
  public List<Double> upperCorner(final Envelope envelope) {
    return asList(envelope.getMaxY(), envelope.getMaxX());
  }
}
