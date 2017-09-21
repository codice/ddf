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

/** This class is used to create a structure to store a Polyline shape. */
public class Polyline extends PolyShape {
  /** LINESTRING_FORMAT - WKT format for a linestring shape. */
  private static final String LINESTRING_FORMAT = "LINESTRING %s";

  /** MULTILINESTRING_FORMAT - WKT format for a multilinestring shape. */
  private static final String MULTILINESTRING_FORMAT = "MULTILINESTRING (%s)";

  /**
   * Constructor for Polyline class.
   *
   * @param shapeType @see {@link
   *     ddf.catalog.transformer.shape.file.reader.api.shapes.Shape#shapeType}
   */
  public Polyline(ShapeType shapeType) {
    super(shapeType);
  }

  @Override
  public String getWKT() {
    return createPolyWKT(LINESTRING_FORMAT, MULTILINESTRING_FORMAT);
  }
}
