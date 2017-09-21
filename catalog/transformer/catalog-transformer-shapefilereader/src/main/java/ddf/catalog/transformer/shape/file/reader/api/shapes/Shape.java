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

import ddf.catalog.transform.CatalogTransformerException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class is used to create a generic class which contains shared variables and methods for all
 * shapes.
 */
public abstract class Shape {
  /** shapeType - Type of shape. */
  private Shape.ShapeType shapeType;

  /**
   * Constructor for Shape class.
   *
   * @param shapeType @see {@link
   *     ddf.catalog.transformer.shape.file.reader.api.shapes.Shape#shapeType}
   */
  protected Shape(Shape.ShapeType shapeType) {
    this.shapeType = shapeType;
  }

  /**
   * Reads the ByteBuffer using the overridden implementation of the readRecordContent method
   * depending on the type of shape.
   *
   * @param buffer @see {@link ddf.catalog.transformer.shape.file.reader.api.ShapeFileReader#buffer}
   * @return Shape object depending on the shape type
   * @throws CatalogTransformerException Thrown if the shape read isn't of the type expected
   */
  public Shape read(ByteBuffer buffer) throws CatalogTransformerException {
    buffer.order(ByteOrder.BIG_ENDIAN);

    // Skip the recordNumber
    buffer.getInt();
    int shapeContentLength = buffer.getInt();

    int startPosition = buffer.position();

    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int shapeTypeInt = buffer.getInt();

    Shape.ShapeType tempShapeType = Shape.ShapeType.getShapeTypeByID(shapeTypeInt);

    if (tempShapeType.equals(shapeType)) {
      readRecordContent(buffer);
    } else {
      throw new CatalogTransformerException("Shape type read doesn't match expected shape type");
    }

    int endPosition = buffer.position();
    int contentLength = endPosition - startPosition;

    if (contentLength != shapeContentLength * 2) {
      throw new CatalogTransformerException("Shape type read doesn't match expected shape type");
    }

    return this;
  }

  /**
   * This method has been overridden in the base classes with the specific implementation of that
   * shape type.
   *
   * @param buffer @see {@link ddf.catalog.transformer.shape.file.reader.api.ShapeFileReader#buffer}
   */
  protected abstract void readRecordContent(ByteBuffer buffer);

  /** @return A WKT formatted string of the shape */
  public abstract String getWKT();

  /** Enum used to determine the shape type the shape file contains. */
  public enum ShapeType {
    /** Null_Shape enum. */
    Null_Shape(0),
    /** Point enum. */
    Point(1),
    /** PolyLine enum. */
    PolyLine(3),
    /** Polygon enum. */
    Polygon(5),
    /** MultiPoint enum. */
    MultiPoint(8);

    /** id - id corresponding to a enum shape type. */
    private final int id;

    /**
     * Constructor for ShapeType enum.
     *
     * @param id @see {@link
     *     ddf.catalog.transformer.shape.file.reader.api.shapes.Shape.ShapeType#id}
     */
    ShapeType(int id) {
      this.id = id;
    }

    /**
     * Used to determine the shape type for a given id.
     *
     * @param id id of the shape type
     * @return A ShapeType object which corresponds to the id provided
     */
    public static Shape.ShapeType getShapeTypeByID(int id) {
      for (Shape.ShapeType st : Shape.ShapeType.values()) {
        if (st.id == id) {
          return st;
        }
      }

      return null;
    }
  }
}
