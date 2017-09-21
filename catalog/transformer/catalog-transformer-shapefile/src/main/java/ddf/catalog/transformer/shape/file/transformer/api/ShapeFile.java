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
package ddf.catalog.transformer.shape.file.transformer.api;

/** This interface defines the extra metacard taxonomy types a shape file has. */
public interface ShapeFile {
  /** @see {@link ddf.catalog.transformer.shape.file.reader.impl.ShapeFileHeaderImpl#minLat} */
  String SHAPE_MIN_LAT = "ext.shapefile.minLat";

  /** @see {@link ddf.catalog.transformer.shape.file.reader.impl.ShapeFileHeaderImpl#maxLat} */
  String SHAPE_MAX_LAT = "ext.shapefile.maxLat";

  /** @see {@link ddf.catalog.transformer.shape.file.reader.impl.ShapeFileHeaderImpl#minLon} */
  String SHAPE_MIN_LON = "ext.shapefile.minLon";

  /** @see {@link ddf.catalog.transformer.shape.file.reader.impl.ShapeFileHeaderImpl#minLat} */
  String SHAPE_MAX_LON = "ext.shapefile.maxLon";

  /** @see {@link ddf.catalog.transformer.shape.file.reader.impl.ShapeFileHeaderImpl#shapeType} */
  String SHAPE_TYPE = "ext.shapefile.shapeType";
}
