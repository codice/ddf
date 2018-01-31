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
import java.text.DecimalFormat;
import java.text.NumberFormat;

/** This class is used to create a structure to store a Point shape. */
public class Point extends Shape {
  /** POINT_FORMAT - WKT format for a point shape. */
  private static final String POINT_FORMAT = "POINT (%s %s)";

  /** x - x coordinate for the point shape. */
  private double x;

  /** y - y coordinate for the point shape. */
  private double y;

  /**
   * Constructor for Point class.
   *
   * @param shapeType @see {@link
   *     ddf.catalog.transformer.shape.file.reader.api.shapes.Shape#shapeType}
   */
  public Point(Shape.ShapeType shapeType) {
    super(shapeType);
  }

  @Override
  protected void readRecordContent(ByteBuffer buffer) {
    x = buffer.getDouble();
    y = buffer.getDouble();
  }

  @Override
  public String getWKT() {
    NumberFormat nf = new DecimalFormat("##.###");
    return String.format(POINT_FORMAT, nf.format(x), nf.format(y));
  }
}
