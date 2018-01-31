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
package ddf.catalog.transformer.shape.file.reader.api;

import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.shape.file.reader.api.shapes.Shape;
import java.nio.ByteBuffer;

/**
 * This class is used to read the contents of a shape file's header to determine the type of shape
 * contained in a file.
 */
public interface ShapeFileHeader {
  /**
   * Ensures the file is valid by checking it's magic and version number. Also determines the type
   * of shapes contained in the file.
   *
   * @param buffer @see {@link
   *     ddf.catalog.transformer.shape.file.reader.impl.ShapeFileReaderImpl#buffer}
   * @throws CatalogTransformerException Thrown if the magic number or version number check fails
   */
  public void read(ByteBuffer buffer) throws CatalogTransformerException;

  /**
   * @return @see {@link
   *     ddf.catalog.transformer.shape.file.reader.impl.ShapeFileHeaderImpl#shapeType}
   */
  public Shape.ShapeType getShapeType();

  /**
   * @return @see {@link ddf.catalog.transformer.shape.file.reader.impl.ShapeFileHeaderImpl#minLon}
   */
  public double getMinLon();

  /**
   * @return @see {@link ddf.catalog.transformer.shape.file.reader.impl.ShapeFileHeaderImpl#minLat}
   */
  public double getMinLat();

  /**
   * @return @see {@link ddf.catalog.transformer.shape.file.reader.impl.ShapeFileHeaderImpl#maxLon}
   */
  public double getMaxLon();

  /**
   * @return @see {@link ddf.catalog.transformer.shape.file.reader.impl.ShapeFileHeaderImpl#maxLat}
   */
  public double getMaxLat();

  /** @return A Bbox around all shapes in the file */
  public String getBbox();
}
