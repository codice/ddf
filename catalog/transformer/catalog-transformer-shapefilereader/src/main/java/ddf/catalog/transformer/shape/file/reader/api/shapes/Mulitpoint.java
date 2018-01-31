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
package ddf.catalog.transformer.shape.file.reader.api.shapes;

import java.nio.ByteBuffer;

/** This class is used to create a structure to store a Multipoint shape. */
public class Mulitpoint extends MultiCoordinateShape {
  /** MULTIPOINT_FORMAT - WKT format for a multipoint shape. */
  private static final String MULTIPOINT_FORMAT = "MULTIPOINT %s";

  /** points - Array containing the x/y coordinates for each point in the shape. */
  protected double[][] points;

  /**
   * Constructor for Mulitpoint class.
   *
   * @param shapeType @see {@link
   *     ddf.catalog.transformer.shape.file.reader.api.shapes.Shape#shapeType}
   */
  public Mulitpoint(ShapeType shapeType) {
    super(shapeType);
  }

  @Override
  protected void readRecordContent(ByteBuffer buffer) {
    minX = buffer.getDouble();
    minY = buffer.getDouble();
    maxX = buffer.getDouble();
    maxY = buffer.getDouble();

    noOfPoints = buffer.getInt();

    points = new double[noOfPoints][2];
    for (int i = 0; i < noOfPoints; i++) {
      points[i][0] = buffer.getDouble();
      points[i][1] = buffer.getDouble();
    }
  }

  @Override
  public String getWKT() {
    String multipointWKT = createPointDataString(points);
    return String.format(MULTIPOINT_FORMAT, multipointWKT);
  }
}
