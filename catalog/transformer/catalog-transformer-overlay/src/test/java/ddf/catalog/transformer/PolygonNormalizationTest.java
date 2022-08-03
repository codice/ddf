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
package ddf.catalog.transformer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

@RunWith(Parameterized.class)
public class PolygonNormalizationTest {
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          // Polygon with size != 5 is unchanged
          {"POLYGON ((0 0, 5 5, 10 0, 0 0))", "POLYGON ((0 0, 5 5, 10 0, 0 0))"},
          {
            // Upper-left adjustment, first coord is upper-right
            "POLYGON ((10 10, 10 0, 0 0, 0 10, 10 10))", "POLYGON ((0 10, 10 10, 10 0, 0 0, 0 10))",
          },
          {
            // Upper-left adjustment, first coord is lower-right
            "POLYGON ((10 0, 0 0, 0 10, 10 10, 10 0))", "POLYGON ((0 10, 10 10, 10 0, 0 0, 0 10))"
          },
          {
            // Upper-left adjustment, first coord is lower-left
            "POLYGON ((0 0, 0 10, 10 10, 10 0, 0 0))", "POLYGON ((0 10, 10 10, 10 0, 0 0, 0 10))"
          },
          {
            // Upper-left adjustment, north edge is left-top
            "POLYGON ((5 10, 10 4, 5 0, 0 6, 5 10))", "POLYGON ((0 6, 5 10, 10 4, 5 0, 0 6))"
          },
          {
            // Upper-left adjustment, north edge is top-right
            "POLYGON ((10 6, 5 0, 0 4, 5 10, 10 6))", "POLYGON ((5 10, 10 6, 5 0, 0 4, 5 10))"
          },
          {
            // Clockwise and first coord is upper-left; no adjustments
            "POLYGON ((0 10, 10 10, 10 0, 0 0, 0 10))", "POLYGON ((0 10, 10 10, 10 0, 0 0, 0 10))"
          }
        });
  }

  private final String originalWkt;

  private final String expectedWkt;

  public PolygonNormalizationTest(final String originalWkt, final String expectedWkt) {
    this.originalWkt = originalWkt;
    this.expectedWkt = expectedWkt;
  }

  @Test
  public void normalizePolygon() throws ParseException {
    final Polygon original = (Polygon) new WKTReader().read(originalWkt);
    final Polygon normalized = GeometryUtils.normalizePolygon(original);
    assertThat(normalized.toText(), is(expectedWkt));
  }
}
