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
package org.codice.ddf.opensearch.endpoint.query.filter;

import ddf.catalog.impl.filter.SpatialFilter;
import org.codice.ddf.opensearch.OpenSearchConstants;

public class BBoxSpatialFilter extends SpatialFilter {
  private final double minX;

  private final double minY;

  private final double maxX;

  private final double maxY;

  /**
   * @param bbox comma-delimited list of lat/lon (deg) bounding box coordinates
   *     (West,South,East,North)
   */
  public BBoxSpatialFilter(final String bbox) {
    super();

    String[] bboxArY = bbox.split(OpenSearchConstants.BBOX_DELIMITER);

    if (bboxArY.length != OpenSearchConstants.BBOX_LIST_SIZE) {
      throw new IllegalArgumentException(
          OpenSearchConstants.BBOX
              + " value must have exactly "
              + OpenSearchConstants.BBOX_LIST_SIZE
              + " values in the format West,South,East,North (or minX,minY,maxX,maxY)");
    }

    try {
      this.minX = Double.parseDouble(bboxArY[0]);
      this.minY = Double.parseDouble(bboxArY[1]);
      this.maxX = Double.parseDouble(bboxArY[2]);
      this.maxY = Double.parseDouble(bboxArY[OpenSearchConstants.BBOX_LIST_SIZE - 1]);
    } catch (NumberFormatException e) {
      final String message =
          String.format(
              "The %s OpenSearch Endpoint parameter must have four %s-delimited double values but is \"%s\".",
              OpenSearchConstants.BBOX, OpenSearchConstants.BBOX_DELIMITER, bbox);
      throw new IllegalArgumentException(message, e);
    }

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
