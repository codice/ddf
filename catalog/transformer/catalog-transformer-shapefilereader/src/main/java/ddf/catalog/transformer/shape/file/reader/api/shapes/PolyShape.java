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

/**
 * This class is used to create a generic class which contains shared variables and methods for all
 * Poly shapes.
 */
public abstract class PolyShape extends MultiCoordinateShape {
  /** noOfParts - Number of parts the shape has. */
  protected int noOfParts;

  /** startOfParts - Indicates the start point of a part. */
  protected int[] startOfParts;

  /** parts - Stores the x/y coordinates for every point in every part. */
  protected double[][][] parts;

  /**
   * Constructor for PolyShape class.
   *
   * @param shapeType @see {@link
   *     ddf.catalog.transformer.shape.file.reader.api.shapes.Shape#shapeType}
   */
  protected PolyShape(ShapeType shapeType) {
    super(shapeType);
  }

  @Override
  protected void readRecordContent(ByteBuffer buffer) {
    minX = buffer.getDouble();
    minY = buffer.getDouble();
    maxX = buffer.getDouble();
    maxY = buffer.getDouble();

    noOfParts = buffer.getInt();
    noOfPoints = buffer.getInt();

    startOfParts = new int[noOfParts];
    for (int i = 0; i < noOfParts; i++) {
      startOfParts[i] = buffer.getInt();
    }

    double[][] points = new double[noOfPoints][2];
    for (int i = 0; i < noOfPoints; i++) {
      points[i][0] = buffer.getDouble();
      points[i][1] = buffer.getDouble();
    }

    createPartsArray(points);
  }

  /**
   * Creates the parts array which stores the points for each part within an array.
   *
   * @param points x/y coordinates for every point in the shape
   */
  private void createPartsArray(double[][] points) {
    int[] indices = new int[noOfParts + 1];
    System.arraycopy(startOfParts, 0, indices, 0, noOfParts);

    indices[indices.length - 1] = noOfPoints;

    parts = new double[noOfParts][][];
    for (int i = 0; i < indices.length - 1; i++) {
      int from = indices[i];
      int to = indices[i + 1];
      int size = to - from;

      parts[i] = new double[size][2];

      for (int idx = 0; (from + idx) < to; idx++) {
        parts[i][idx][0] = points[from + idx][0];
        parts[i][idx][1] = points[from + idx][1];
      }
    }
  }

  /**
   * Creates a poly shape WKT depending on the number of part it has.
   *
   * @param singlePartFormat format used if the shape has only one part
   * @param multiPartFormat format used if the shape has multiple part
   * @return WKT format string of the shape depending on the number of parts
   */
  protected String createPolyWKT(String singlePartFormat, String multiPartFormat) {
    StringBuilder polygonWKT = new StringBuilder();

    for (int i = 0; i < noOfParts; i++) {
      double[][] pointData = getPointsForPart(i);
      polygonWKT.append(createPointDataString(pointData));

      if (noOfParts != 1 && i != noOfParts - 1) {
        polygonWKT.append(", ");
      }
    }

    if (noOfParts != 1) {
      return String.format(multiPartFormat, polygonWKT);
    }

    return String.format(singlePartFormat, polygonWKT);
  }

  /**
   * @param index index of the part
   * @return points contained in that part
   */
  protected double[][] getPointsForPart(int index) {
    return parts[index];
  }
}
