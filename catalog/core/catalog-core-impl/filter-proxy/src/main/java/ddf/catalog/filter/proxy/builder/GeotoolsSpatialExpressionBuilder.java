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
package ddf.catalog.filter.proxy.builder;

import ddf.catalog.filter.SpatialExpressionBuilder;
import org.geotools.api.filter.Filter;

public class GeotoolsSpatialExpressionBuilder extends GeotoolsBuilder
    implements SpatialExpressionBuilder {

  GeotoolsSpatialExpressionBuilder(GeotoolsBuilder builder) {
    super(builder);
    setSecondaryValue(0.0d);
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.filter.SpatialExpressionBuilder#wkt(java.lang.String)
   */
  @Override
  public Filter wkt(String string) {
    return build(string);
  }
}
