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

/** This class is used to create a structure to store a Polygon shape. */
public class Polygon extends PolyShape {
  /** POLYGON_FORMAT - WKT format for a polygon shape. */
  private static final String POLYGON_FORMAT = "POLYGON (%s)";

  /** MULTIPOLYGON_FORMAT - WKT format for a multipolygon shape. */
  private static final String MULTIPOLYGON_FORMAT = "MULTIPOLYGON ((%s))";

  /**
   * Constructor for Polygon class.
   *
   * @param shapeType @see {@link
   *     ddf.catalog.transformer.shape.file.reader.api.shapes.Shape#shapeType}
   */
  public Polygon(ShapeType shapeType) {
    super(shapeType);
  }

  @Override
  public String getWKT() {
    return createPolyWKT(POLYGON_FORMAT, MULTIPOLYGON_FORMAT);
  }
}
