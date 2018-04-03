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
package org.codice.ddf.opensearch.query.filter;

import ddf.catalog.impl.filter.SpatialFilter;

public class BBoxSpatialFilter extends SpatialFilter {
  private static final int MAX_Y_COORDINATE_INDEX = 3;

  private final double minX;

  private final double minY;

  private final double maxX;

  private final double maxY;

  /**
   * @param bbox comma-delimited list of lat/lon (deg) bounding box coordinates
   *     (West,South,East,North)
   */
  public BBoxSpatialFilter(String bbox) {
    super();

    String[] bboxArY = bbox.split(" |,\\p{Space}?");

    this.minX = Double.parseDouble(bboxArY[0]);
    this.minY = Double.parseDouble(bboxArY[1]);
    this.maxX = Double.parseDouble(bboxArY[2]);
    this.maxY = Double.parseDouble(bboxArY[MAX_Y_COORDINATE_INDEX]);

    this.geometryWkt = createWKT();
  }

  /**
   * Creates a WKT string from the bbox coordinates
   *
   * @return the wkt String
   */
  private String createWKT() {
    return String.format(
        "POLYGON((%1$s %2$s,%1$s %4$s,%3$s %4$s,%3$s %2$s,%1$s %2$s))", minX, minY, maxX, maxY);
  }
}
