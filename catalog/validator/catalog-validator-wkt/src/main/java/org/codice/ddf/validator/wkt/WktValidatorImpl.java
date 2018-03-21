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
package org.codice.ddf.validator.wkt;

import java.text.ParseException;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.exception.InvalidShapeException;
import org.locationtech.spatial4j.io.WKTReader;

public class WktValidatorImpl implements WktValidator {
  private static final JtsSpatialContextFactory JTS_SPATIAL_CONTEXT_FACTORY =
      new JtsSpatialContextFactory();

  static {
    JTS_SPATIAL_CONTEXT_FACTORY.allowMultiOverlap = true;
  }

  private static final SpatialContext SPATIAL_CONTEXT =
      JTS_SPATIAL_CONTEXT_FACTORY.newSpatialContext();

  private WKTReader wktReader;

  public WktValidatorImpl() {
    this.wktReader = new WKTReader(SPATIAL_CONTEXT, JTS_SPATIAL_CONTEXT_FACTORY);
  }

  @Override
  public boolean isValid(String wkt) {
    try {
      wktReader.parse(wkt);
      return true;
    } catch (ParseException | InvalidShapeException e) {
      return false;
    }
  }
}
