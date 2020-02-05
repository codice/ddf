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
package ddf.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.io.ParseException;

/**
 * Tests the {@link WktStandard} class
 *
 * @author Ashraf Barakat
 * @author Phillip Klinefelter
 */
public class WktStandardTest {

  private static final GeometryFactory GEO_FACTORY = new GeometryFactory();

  private static final Coordinate[] WHOLE_NUMBER_COORDINATES =
      new Coordinate[] {new Coordinate(10, 10), new Coordinate(20, 20), new Coordinate(30, 30)};

  private static final MultiPoint WHOLE_NUMBER_MULTIPOINT =
      GEO_FACTORY.createMultiPoint(WHOLE_NUMBER_COORDINATES);

  private static final Coordinate[] DECIMAL_NUMBER_COORDINATES =
      new Coordinate[] {new Coordinate(10.1, 10.2), new Coordinate(20.22, 20.33)};

  private static final MultiPoint DECIMAL_NUMBER_MULTIPOINT =
      GEO_FACTORY.createMultiPoint(DECIMAL_NUMBER_COORDINATES);

  @Test
  public void denormalizeMultipointWithWholeNumbers() {
    assertThat(
        WktStandard.denormalize(WHOLE_NUMBER_MULTIPOINT.toText()),
        is("MULTIPOINT (10 10, 20 20, 30 30)"));
  }

  @Test
  public void denormalizeMultipointWithDecimalNumbers() {
    assertThat(
        WktStandard.denormalize(DECIMAL_NUMBER_MULTIPOINT.toText()),
        is("MULTIPOINT (10.1 10.2, 20.22 20.33)"));
  }

  @Test
  public void denormalizeMultipointWithNegatives() {
    assertThat(
        WktStandard.denormalize("MULTIPOINT ((-1 -1), (29.5 -15.5), (-30.5 14.5))"),
        is("MULTIPOINT (-1 -1, 29.5 -15.5, -30.5 14.5)"));
  }

  @Test
  public void denormalizeMultipointWithSingleDecimal() throws ParseException {

    assertThat(
        WktStandard.denormalize("MULTIPOINT ((-1. -1.), (29.5 -15.5), (-30.5 14.5))"),
        is("MULTIPOINT (-1. -1., 29.5 -15.5, -30.5 14.5)"));
  }

  @Test
  public void denormalizeMultipointWithSinglePoint() throws ParseException {

    assertThat(WktStandard.denormalize("MULTIPOINT ((-1 -1))"), is("MULTIPOINT (-1 -1)"));
  }

  @Test
  public void denormalizeMultipointWithExtraEndingSpace() throws ParseException {

    assertThat(
        WktStandard.denormalize("MULTIPOINT ((-1. -1.), (29.5 -15.5), (-30.5 14.5) )"),
        is("MULTIPOINT (-1. -1., 29.5 -15.5, -30.5 14.5 )"));
  }

  @Test
  public void denormalizeGeometryCollection() {
    Geometry[] geometries =
        new Geometry[] {
          WHOLE_NUMBER_MULTIPOINT,
          GEO_FACTORY.createPoint(new Coordinate(5, 20)),
          DECIMAL_NUMBER_MULTIPOINT
        };

    assertThat(
        WktStandard.denormalize(GEO_FACTORY.createGeometryCollection(geometries).toText()),
        is(
            "GEOMETRYCOLLECTION (MULTIPOINT (10 10, 20 20, 30 30), POINT (5 20), MULTIPOINT (10.1 10.2, 20.22 20.33))"));
  }

  @Test
  public void denormalizeNotNeeded() {
    String denormalizedWkt = "MULTIPOINT (10 10, 20 20)";

    assertThat(WktStandard.denormalize(denormalizedWkt), is(denormalizedWkt));
  }

  @Test
  public void denormalizeEmptyMultiPoint() {
    String emptyMultipoint = "MULTIPOINT (EMPTY)";

    assertThat(WktStandard.denormalize(emptyMultipoint), is(emptyMultipoint));
  }

  @Test
  public void denormalizeNull() {
    assertThat(WktStandard.denormalize(null), is(nullValue()));
  }

  @Test
  public void denormalizeEmptyString() {
    assertThat(WktStandard.denormalize(""), is(""));
  }

  @Test
  public void normalizeGeometryCollection() {
    assertThat(
        WktStandard.normalize(
            "GEOMETRYCOLLECTION ( MULTIPOINT(10 10,20 20) , POINT (5 20) ,  MULTIPOINT(  30 30  ,  40 40  ) )"),
        is(
            "GEOMETRYCOLLECTION (MULTIPOINT ((10 10), (20 20)), POINT (5 20), MULTIPOINT ((30 30), (40 40)))"));
  }

  @Test
  public void normalizeNull() {
    assertThat(WktStandard.normalize(null), is(nullValue()));
  }

  @Test
  public void normalizeEmptyString() {
    assertThat(WktStandard.normalize(StringUtils.EMPTY), is(StringUtils.EMPTY));
  }
}
