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
package ddf.catalog.impl.filter;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpatialFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(SpatialFilter.class);

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  protected String geometryWkt;

  private WKTReader reader;

  public SpatialFilter() {
    this(null);
  }

  public SpatialFilter(String geometryWkt) {
    this.geometryWkt = geometryWkt;

    this.reader = new WKTReader(GEOMETRY_FACTORY);
  }

  public String getGeometryWkt() {
    return geometryWkt;
  }

  public void setGeometryWkt(String geometryWkt) {
    this.geometryWkt = geometryWkt;
  }

  public Geometry getGeometry() {
    Geometry geometry = null;

    try {
      geometry = reader.read(geometryWkt);
    } catch (ParseException e) {
      LOGGER.debug("Unable to read geometry for WKT = {}", this.geometryWkt, e);
    }

    return geometry;
  }
}
