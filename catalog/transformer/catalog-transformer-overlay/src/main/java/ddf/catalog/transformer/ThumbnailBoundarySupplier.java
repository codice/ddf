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
package ddf.catalog.transformer;

import com.vividsolutions.jts.geom.Geometry;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThumbnailBoundarySupplier
    implements BiFunction<Metacard, Map<String, Serializable>, Optional<Geometry>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailBoundarySupplier.class);

  @Override
  public Optional<Geometry> apply(
      Metacard metacard, Map<String, Serializable> stringSerializableMap) {
    try {
      Geometry geometry = GeometryUtils.parseGeometry(metacard.getLocation());
      return Optional.of(geometry);
    } catch (CatalogTransformerException e) {
      LOGGER.debug("Unable to parse location", e);
      return Optional.empty();
    }
  }
}
