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
package ddf.geo.formatter;

import java.io.StringReader;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;

public class LibraryTest extends AbstractTestCompositeGeometry {

  @Test(expected = IllegalArgumentException.class)
  public void testNullPointArgument() {
    new Point(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullMultiPointArgument() {
    new MultiPoint(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullLineStringArgument() {
    new LineString(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNotAGeometryCollection() throws ParseException {
    new GeometryCollection(getSamplePolygon());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNotALineString() throws ParseException {
    new LineString(getSamplePolygon());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNotAMultiLineString() throws ParseException {
    new MultiLineString(getSamplePolygon());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNotAMultiPoint() throws ParseException {
    new MultiPoint(getSamplePolygon());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNotAMultiPolygon() throws ParseException {
    new MultiPolygon(getSamplePolygon());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNotAPoint() throws ParseException {

    new Point(getSamplePolygon());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNotAPolygon() throws ParseException {

    new Polygon(reader.read(new StringReader("POINT (1 0)")));
  }

  protected Geometry getSamplePolygon() throws ParseException {
    return reader.read(
        new StringReader(
            "POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10),(20 30, 35 35, 30 20, 20 30))"));
  }
}
