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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import ddf.catalog.transformer.shape.file.reader.impl.ShapeFileReaderImpl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ShapeFileReaderTest {

  private static final String EMPTY_GEOMETRY_COLLECTION = "";

  private static final String POINT_GEOMETRY_COLLECTION =
      "GEOMETRYCOLLECTION (POINT (-1 0), POINT (-3 -2), POINT (1 2), POINT (3 4), POINT (5 6), POINT (7 8), POINT (9 10), POINT (11 12), POINT (13 14), POINT (15 16))";

  private static final String MULTIPOINT_GEOMETRY_COLLECTION =
      "GEOMETRYCOLLECTION (MULTIPOINT ((1 2), (3 4), (5 6), (7 8), (9 10)), MULTIPOINT ((11 12), (13 14), (15 16), (17 18), (19 20)))";

  private static final String POLYGON_GEOMETRY_COLLECTION =
      "GEOMETRYCOLLECTION (POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0)), POLYGON ((0 0, 0 4, 4 4, 4 0, 0 0), (1 1, 3 1, 3 3, 1 3, 1 1)))";

  private static final String POLYLINE_GEOMETRY_COLLECTION =
      "GEOMETRYCOLLECTION (LINESTRING (1 2, 9 10), MULTILINESTRING ((11 12, 13 14), (15 16, 19 20)))";

  private static final String POLYGON_BBOX = "POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))";

  private static final String SHAPE_FILE_BBOX = "POLYGON ((0 0, 0 5, 5 5, 5 0, 0 0))";

  @Test
  public void testEmptyShapeFile() throws Exception {
    String fileContent = getFileContent("empty.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    ShapeFileReader reader = new ShapeFileReaderImpl(stream, 10);
    reader.read();

    assertThat(reader.getShapesCount(), equalTo(0));
    assertThat(reader.createGeometryCollectionWKT(), equalTo(EMPTY_GEOMETRY_COLLECTION));
  }

  @Test
  public void testPointShapeFile() throws Exception {
    String fileContent = getFileContent("points.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    ShapeFileReader reader = new ShapeFileReaderImpl(stream, 10);
    reader.read();

    assertThat(reader.getShapesCount(), equalTo(10));
    assertThat(reader.createGeometryCollectionWKT(), equalTo(POINT_GEOMETRY_COLLECTION));
  }

  @Test
  public void testMultipointShapeFile() throws Exception {
    String fileContent = getFileContent("multipoints.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    ShapeFileReader reader = new ShapeFileReaderImpl(stream, 10);
    reader.read();

    assertThat(reader.getShapesCount(), equalTo(2));
    assertThat(reader.createGeometryCollectionWKT(), equalTo(MULTIPOINT_GEOMETRY_COLLECTION));
  }

  @Test
  public void testPolygonShapeFile() throws Exception {
    String fileContent = getFileContent("polygons.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    ShapeFileReader reader = new ShapeFileReaderImpl(stream, 10);
    reader.read();

    assertThat(reader.getShapesCount(), equalTo(2));
    assertThat(reader.createGeometryCollectionWKT(), equalTo(POLYGON_GEOMETRY_COLLECTION));
  }

  @Test
  public void testPolylinesFile() throws Exception {
    String fileContent = getFileContent("polylines.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    ShapeFileReader reader = new ShapeFileReaderImpl(stream, 10);
    reader.read();

    assertThat(reader.getShapesCount(), equalTo(2));
    assertThat(reader.createGeometryCollectionWKT(), equalTo(POLYLINE_GEOMETRY_COLLECTION));
  }

  @Test(expected = Exception.class)
  public void testNullShapeFile() throws Exception {
    String fileContent = getFileContent("null.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    ShapeFileReader reader = new ShapeFileReaderImpl(stream, 10);
    reader.read();
  }

  @Test
  public void testMaxFeatureCount() throws Exception {
    String fileContent = getFileContent("points.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    ShapeFileReader reader = new ShapeFileReaderImpl(stream, 5);
    reader.read();

    assertThat(reader.getShapesCount(), equalTo(5));
  }

  @Test
  public void testShapesBbox() throws Exception {
    String fileContent = getFileContent("polygons.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    ShapeFileReader reader = new ShapeFileReaderImpl(stream, 10);
    reader.read();
    List<String> shapesBbox = reader.createShapesBbox();

    assertThat(shapesBbox.size(), equalTo(2));
    assertThat(shapesBbox.contains(POLYGON_BBOX), is(true));
  }

  @Test
  public void testShapeFileBbox() throws Exception {
    String fileContent = getFileContent("polygons.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    ShapeFileReader reader = new ShapeFileReaderImpl(stream, 10);
    reader.readHeader();

    assertThat(reader.getShapeFileHeader().getBbox(), equalTo(SHAPE_FILE_BBOX));
  }

  private String getFileContent(String filePath) throws IOException {
    return IOUtils.toString(
        this.getClass().getClassLoader().getResourceAsStream(filePath),
        StandardCharsets.ISO_8859_1);
  }
}
