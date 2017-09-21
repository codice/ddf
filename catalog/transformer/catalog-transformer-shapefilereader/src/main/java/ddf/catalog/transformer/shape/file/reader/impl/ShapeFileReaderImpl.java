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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.shape.file.reader.api.ShapeFileHeader;
import ddf.catalog.transformer.shape.file.reader.api.ShapeFileReader;
import ddf.catalog.transformer.shape.file.reader.api.shapes.Mulitpoint;
import ddf.catalog.transformer.shape.file.reader.api.shapes.Point;
import ddf.catalog.transformer.shape.file.reader.api.shapes.Polygon;
import ddf.catalog.transformer.shape.file.reader.api.shapes.Polyline;
import ddf.catalog.transformer.shape.file.reader.api.shapes.Shape;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class is used to read the contents of a shape file and return a Geometry Collection WKT. */
public class ShapeFileReaderImpl implements ShapeFileReader {
  /** LOGGER - used to log to the ddf.log file. */
  private static final Logger LOGGER = LoggerFactory.getLogger(ShapeFileReaderImpl.class);

  /** GEOMETRY_COLLECTION - Starting string of a geometry collection. */
  private static final String GEOMETRY_COLLECTION = "GEOMETRYCOLLECTION (%s)";

  /** DISTANCE_TOLERANCE - Maximum distance of simplified vertices from the original vertices. */
  private static final double DISTANCE_TOLERANCE = 0.0001;

  /** stream - contains the shape file's content. */
  private InputStream stream;

  /** buffer - contains the shape file's content. */
  private ByteBuffer buffer;

  /** shapes - list of shapes contained in the shape file. */
  private List<Geometry> shapes = new ArrayList<>();

  /** maxFeatureCount - Maximum number of features returned from a shape file. */
  private int maxFeatureCount;

  /**
   * geometryCollectionWKT - A Geometry Collection WKT string of all shapes within the shape file.
   */
  private String geometryCollectionWKT;

  /** header - Reads the contents of the shape file's header. */
  private ShapeFileHeader header;

  /**
   * Constructor for ShapeFileReader class.
   *
   * @param maxFeatureCount @see {@link
   *     ddf.catalog.transformer.shape.file.reader.impl.ShapeFileReaderImpl#maxFeatureCount}
   */
  public ShapeFileReaderImpl(InputStream stream, int maxFeatureCount) {
    this.handleMaxFeatureCount(maxFeatureCount);
    this.stream = stream;
    this.buffer = createByteBuffer();
  }

  /**
   * Determines if a max feature count should exist
   *
   * @param maxFeatureCount - the maximum features to display
   */
  private void handleMaxFeatureCount(int maxFeatureCount) {
    this.maxFeatureCount = (maxFeatureCount > 0) ? maxFeatureCount : 0;
  }

  public void read() throws CatalogTransformerException {
    readHeader();
    Shape.ShapeType shapeType = header.getShapeType();

    switch (shapeType) {
      case Null_Shape:
        break;
      case Point:
        while (hasShapes()) {
          Shape point = new Point(shapeType).read(buffer);
          addShape(point);
        }
        break;
      case Polygon:
        while (hasShapes()) {
          Shape polygon = new Polygon(shapeType).read(buffer);
          addShape(polygon);
        }
        break;
      case PolyLine:
        while (hasShapes()) {
          Shape polyline = new Polyline(shapeType).read(buffer);
          addShape(polyline);
        }
        break;
      case MultiPoint:
        while (hasShapes()) {
          Shape multipoint = new Mulitpoint(shapeType).read(buffer);
          addShape(multipoint);
        }
        break;
      default:
        break;
    }
  }

  public void readHeader() throws CatalogTransformerException {
    header = new ShapeFileHeaderImpl();
    header.read(buffer);
  }

  /**
   * Checks if the shape is valid then adds it to the shapes list.
   *
   * @param shape shape to be added to the shapes list
   */
  private void addShape(Shape shape) {
    Geometry geometry = createGeometry(shape.getWKT());

    if (geometry != null) {
      geometry = TopologyPreservingSimplifier.simplify(geometry, DISTANCE_TOLERANCE);
      shapes.add(geometry);
    }
  }

  /**
   * Check if there are still shapes to read in the file or the max number of shapes has been read.
   *
   * @return True or False depending on if the condition above are met
   */
  private Boolean hasShapes() {
    Boolean hasShapes = buffer.position() != buffer.capacity();
    if (maxFeatureCount > 0 && hasShapes) {
      hasShapes = shapes.size() != maxFeatureCount;
    }
    return hasShapes;
  }

  public String createGeometryCollectionWKT() {
    StringBuilder geometryCollectionSb = new StringBuilder();

    for (int i = 0; i < shapes.size(); i++) {
      Geometry shape = shapes.get(i);
      geometryCollectionSb.append(shape.toText());

      if (i != shapes.size() - 1) {
        geometryCollectionSb.append(", ");
      }
    }

    if (geometryCollectionSb.length() != 0) {
      geometryCollectionWKT = String.format(GEOMETRY_COLLECTION, geometryCollectionSb);
      return geometryCollectionWKT;
    }

    return "";
  }

  public ArrayList<String> createShapesBbox() {
    ArrayList<String> shapeBboxs = new ArrayList<>();
    Shape.ShapeType shapeType = header.getShapeType();

    switch (shapeType) {
      case Polygon:
      case PolyLine:
      case MultiPoint:
        for (int i = 0; i < shapes.size(); i++) {
          Geometry shape = shapes.get(i);
          shapeBboxs.add(shape.getEnvelope().toText());
        }
        break;
      default:
        break;
    }

    return shapeBboxs;
  }

  /**
   * Uses IOUtils to convert the stream to a byte array and then wraps the data within a ByteBuffer.
   *
   * @return A ByteBuffer object with the content of the shape file inside
   */
  private ByteBuffer createByteBuffer() {
    try {
      byte[] data = IOUtils.toByteArray(stream);
      return ByteBuffer.wrap(data);
    } catch (IOException e) {
      LOGGER.info("Cannot create ByteBuffer");
    }

    return null;
  }

  /**
   * Check if the geometry is valid. E.g. no holes or loops.
   *
   * @param shapeWKT WKT string for the shape
   * @return Geometry object based on the WKT
   */
  private Geometry createGeometry(String shapeWKT) {
    WKTReader reader = new WKTReader();
    try {
      Geometry geometry = reader.read(shapeWKT);

      // Check if the geometry is valid. E.g. no holes or loops
      if (geometry.isValid()) {
        return geometry;
      }
    } catch (ParseException e) {
      LOGGER.info("Failed to create geometry from WKT");
    }

    return null;
  }

  public GeometryCollection createGeometryCollection() {
    WKTReader reader = new WKTReader();
    try {
      return (GeometryCollection) reader.read(geometryCollectionWKT);
    } catch (ParseException e) {
      LOGGER.info("Failed to create geometry from WKT");
    }

    return null;
  }

  public int getShapesCount() {
    return shapes.size();
  }

  public void setMaxFeatureCount(int maxFeatureCount) {
    this.maxFeatureCount = maxFeatureCount;
  }

  public ShapeFileHeader getShapeFileHeader() {
    return header;
  }
}
