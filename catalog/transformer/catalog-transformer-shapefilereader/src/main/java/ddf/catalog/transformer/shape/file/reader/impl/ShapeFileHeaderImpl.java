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
package ddf.catalog.transformer.shape.file.reader.impl;

import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.shape.file.reader.api.ShapeFileHeader;
import ddf.catalog.transformer.shape.file.reader.api.shapes.Shape;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;

/**
 * This class is used to read the contents of a shape file's header to determine the type of shape
 * contained in a file.
 */
public class ShapeFileHeaderImpl implements ShapeFileHeader {
  /** MAGIC - used to ensure the file isn't corrupt. */
  private static final int MAGIC = 9994;

  /** VERSION - used to ensure the file is the right version. */
  private static final int VERSION = 1000;

  /** BUFFER_VERSION - location of the version within the ByteBuffer. */
  private static final int BUFFER_VERSION_OFFSET = 28;

  /** BUFFER_SHAPE_TYPE - location of the shape type within the ByteBuffer. */
  private static final int BUFFER_SHAPE_TYPE_OFFSET = 32;

  /** HEADER_LENGTH - length of the shape file header. */
  private static final int HEADER_LENGTH = 100;

  /** BBOX - Format of the bbox string. */
  private static final String BBOX = "POLYGON ((%s %s, %s %s, %s %s, %s %s, %s %s))";

  /** format the lat/lon values */
  private static final DecimalFormat DF = new DecimalFormat("#.####");

  /** shapeType - Type of shape that the file contains. */
  private Shape.ShapeType shapeType;

  /** minLon, minLat, maxLon, maxLat - Min/Max x/y values in the shape file. */
  private double minLon, minLat, maxLon, maxLat;

  /**
   * Ensures the file is valid by checking it's magic and version number. Also determines the type
   * of shapes contained in the file.
   *
   * @param buffer @see {@link
   *     ddf.catalog.transformer.shape.file.reader.impl.ShapeFileReaderImpl#buffer}
   * @throws CatalogTransformerException Thrown if the magic number or version number check fails
   */
  public void read(ByteBuffer buffer) throws CatalogTransformerException {
    buffer.order(ByteOrder.BIG_ENDIAN);

    int magic = buffer.getInt(0);
    if (magic != MAGIC) {
      throw new CatalogTransformerException("Magic number check failed");
    }

    buffer.order(ByteOrder.LITTLE_ENDIAN);

    int version = buffer.getInt(BUFFER_VERSION_OFFSET);
    if (version != VERSION) {
      throw new CatalogTransformerException("Version number check failed");
    }

    int shapeTypeInt = buffer.getInt(BUFFER_SHAPE_TYPE_OFFSET);
    shapeType = Shape.ShapeType.getShapeTypeByID(shapeTypeInt);

    minLon = buffer.getDouble(36);
    minLat = buffer.getDouble(44);
    maxLon = buffer.getDouble(52);
    maxLat = buffer.getDouble(60);

    buffer.position(HEADER_LENGTH);
  }

  public Shape.ShapeType getShapeType() {
    return shapeType;
  }

  public double getMinLon() {
    return minLon;
  }

  public double getMinLat() {
    return minLat;
  }

  public double getMaxLon() {
    return maxLon;
  }

  public double getMaxLat() {
    return maxLat;
  }

  public String getBbox() {
    return String.format(
        BBOX,
        DF.format(minLon),
        DF.format(minLat),
        DF.format(minLon),
        DF.format(maxLat),
        DF.format(maxLon),
        DF.format(maxLat),
        DF.format(maxLon),
        DF.format(minLat),
        DF.format(minLon),
        DF.format(minLat));
  }
}
