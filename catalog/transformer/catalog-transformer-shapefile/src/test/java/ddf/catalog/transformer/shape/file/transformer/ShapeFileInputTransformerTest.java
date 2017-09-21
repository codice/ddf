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
package ddf.catalog.transformer.shape.file.transformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transformer.shape.file.reader.api.ShapeFileReaderFactory;
import ddf.catalog.transformer.shape.file.reader.impl.ShapeFileReaderFactoryImpl;
import ddf.catalog.transformer.shape.file.transformer.api.ShapeFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

public class ShapeFileInputTransformerTest {

  private static final String EMPTY_GEOMETRY_COLLECTION = "POLYGON ((0 0, 0 0, 0 0, 0 0, 0 0))";

  private static final String POINT_BBOX = "POLYGON ((-3 -2, -3 18, 17 18, 17 -2, -3 -2))";

  private static final String MULTIPOINT_BBOX = "POLYGON ((1 2, 1 20, 19 20, 19 2, 1 2))";

  private static final String POLYGON_BBOX = "POLYGON ((0 0, 0 5, 5 5, 5 0, 0 0))";

  private static final String POLYLINE_BBOX = "POLYGON ((1 2, 1 20, 19 20, 19 2, 1 2))";

  private ShapeFileInputTransformer transformer;

  private final ShapeFileReaderFactory factory = new ShapeFileReaderFactoryImpl(10);

  @Before
  public void setup() {
    transformer =
        new ShapeFileInputTransformer(factory, new MetacardTypeImpl("shapefile", (List) null));
  }

  @Test
  public void testEmptyShapeFile() throws Exception {
    String fileContent = getFileContent("empty.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    validateShapeFileMetacard(transformer.transform(stream, null), null, EMPTY_GEOMETRY_COLLECTION);
  }

  @Test
  public void testPointShapeFile() throws Exception {
    String fileContent = getFileContent("points.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    validateShapeFileMetacard(transformer.transform(stream, null), null, POINT_BBOX);
  }

  @Test
  public void testMultipointShapeFile() throws Exception {
    String fileContent = getFileContent("multipoints.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    validateShapeFileMetacard(transformer.transform(stream, null), null, MULTIPOINT_BBOX);
  }

  @Test
  public void testPolygonShapeFile() throws Exception {
    String fileContent = getFileContent("polygons.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    validateShapeFileMetacard(transformer.transform(stream, null), null, POLYGON_BBOX);
  }

  @Test
  public void testPolylinesFile() throws Exception {
    String fileContent = getFileContent("polylines.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    validateShapeFileMetacard(transformer.transform(stream, null), null, POLYLINE_BBOX);
  }

  @Test
  public void testPolygonShapeFileWithId() throws Exception {
    String fileContent = getFileContent("polygons.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    validateShapeFileMetacard(transformer.transform(stream, "testId"), "testId", POLYGON_BBOX);
  }

  @Test
  public void testNullShapeFile() throws Exception {
    String fileContent = getFileContent("null.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    transformer.transform(stream);
  }

  @Test
  public void testMinMaxLatLon() throws Exception {
    String fileContent = getFileContent("polygons.shp");
    InputStream stream =
        new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.ISO_8859_1));

    Metacard metacard = transformer.transform(stream);

    assertThat(getDouble(metacard, ShapeFile.SHAPE_MIN_LAT), closeTo(0, 0.00001));
    assertThat(getDouble(metacard, ShapeFile.SHAPE_MAX_LAT), closeTo(5, 0.00001));
    assertThat(getDouble(metacard, ShapeFile.SHAPE_MIN_LON), closeTo(0, 0.00001));
    assertThat(getDouble(metacard, ShapeFile.SHAPE_MAX_LON), closeTo(5, 0.00001));
  }

  private String getFileContent(String filePath) throws IOException {
    return IOUtils.toString(
        this.getClass().getClassLoader().getResourceAsStream(filePath),
        StandardCharsets.ISO_8859_1);
  }

  private void validateShapeFileMetacard(
      Metacard mcard, String expectedId, String expectedGeometryCollection) {
    if (expectedId != null) {
      assertThat(mcard.getId(), equalTo(expectedId));
    }

    if (mcard.getLocation() != null) {
      assertThat(mcard.getLocation(), equalTo(expectedGeometryCollection));
    }
  }

  /**
   * Converts attribute to double. If a String tries to parse it, if a double, returns it. If
   * neither, throws an exception which is fine for testing purposes.
   *
   * @param metacard
   * @param attribute
   * @return double value of the attribute
   */
  private double getDouble(Metacard metacard, String attribute) {
    Object obj = metacard.getAttribute(attribute).getValue();
    if (obj instanceof String) {
      return Double.valueOf((String) obj);
    }
    return ((Double) obj).doubleValue();
  }
}
