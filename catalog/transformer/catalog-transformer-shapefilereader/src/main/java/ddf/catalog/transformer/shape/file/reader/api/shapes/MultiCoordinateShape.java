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

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * This class is used to create a generic class which contains shared variables and methods for all
 * Poly and Multipoint shapes.
 */
public abstract class MultiCoordinateShape extends Shape {
  /** COORDINATE_FORMAT - Coordinate format for a shape. */
  protected static final String COORDINATE_FORMAT = "%s %s";

  /** noOfPoints - Number of points the shape has. */
  protected int noOfPoints;

  /** minX, minY, maxX, maxY - min/max x/y coordinates. */
  protected double minX, minY, maxX, maxY;

  /**
   * Constructor for MultiCoordinateShape class.
   *
   * @param shapeType @see {@link
   *     ddf.catalog.transformer.shape.file.reader.api.shapes.Shape#shapeType}
   */
  public MultiCoordinateShape(ShapeType shapeType) {
    super(shapeType);
  }

  /**
   * @param pointData Array containing point data for that shape
   * @return Point data in WKT format
   */
  protected String createPointDataString(double[][] pointData) {
    StringBuilder pointDataWKT = new StringBuilder("(");

    for (int i = 0; i < pointData.length; i++) {
      double x = pointData[i][0];
      double y = pointData[i][1];

      pointDataWKT.append(createPointString(x, y));

      if (i != pointData.length - 1) {
        pointDataWKT.append(", ");
      }
    }

    pointDataWKT.append(")");

    return pointDataWKT.toString();
  }

  /**
   * @param x x coordinate of the shape
   * @param y y coordinate of the shape
   * @return coordinates in WKT format
   */
  private String createPointString(double x, double y) {
    NumberFormat nf = new DecimalFormat("##.###");
    return String.format(COORDINATE_FORMAT, nf.format(x), nf.format(y));
  }
}
