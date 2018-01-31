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

import com.vividsolutions.jts.geom.GeometryCollection;
import ddf.catalog.transform.CatalogTransformerException;
import java.util.ArrayList;

/**
 * This interface is used to read the contents of a shape file and return a Geometry Collection WKT.
 */
public interface ShapeFileReader {
  /**
   * Reads the buffer until either the end is reached or the maximum number of shapes that can be
   * returned from a shape file has been reached.
   *
   * @throws CatalogTransformerException Thrown if the magic number or version number check fails
   */
  public void read() throws CatalogTransformerException;

  /**
   * Reads only the header of the shape file.
   *
   * @throws CatalogTransformerException Thrown if the magic number or version number check fails
   */
  public void readHeader() throws CatalogTransformerException;

  /**
   * Iterates through the list of shapes to create the Geometry Collection WKT.
   *
   * @return A Geometry Collection WKT string of all shapes within the shape file
   */
  public String createGeometryCollectionWKT();

  /**
   * Creates a set of Bbox strings around each shape in the file. Null and point shapes don't
   * support this feature.
   *
   * @return Set of WKT Polygon strings around individual shapes in the shape file
   */
  public ArrayList<String> createShapesBbox();

  /**
   * Creates a Geometry Collection object which can determine the Bbox of the set of shapes in the
   * shape file.
   *
   * @return A GeometryCollection object based on the WKT
   */
  public GeometryCollection createGeometryCollection();

  /** @return The number of shapes in the file */
  public int getShapesCount();

  /**
   * @param maxFeatureCount @see {@link
   *     ddf.catalog.transformer.shape.file.reader.impl.ShapeFileReaderImpl#maxFeatureCount}
   */
  public void setMaxFeatureCount(int maxFeatureCount);

  /** @return A Bbox around all shapes in the file */
  public ShapeFileHeader getShapeFileHeader();
}
