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
public class DatelineAdjustmentTest {
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          // A rectangle whose edges are parallel to the latitude and longitude lines
          {
            "POLYGON ((175 5, -175 5, -175 -5, 175 -5, 175 5))",
            "POLYGON ((175 5, 185 5, 185 -5, 175 -5, 175 5))"
          },
          // A rectangle whose edges are not parallel to the latitude and longitude lines
          {
            "POLYGON ((176 0, 179 5, -178 0, 179 -5, 176 0))",
            "POLYGON ((176 0, 179 5, 182 0, 179 -5, 176 0))"
          }
        });
  }

  private final String originalWkt;

  private final String expectedWkt;

  public DatelineAdjustmentTest(final String originalWkt, final String expectedWkt) {
    this.originalWkt = originalWkt;
    this.expectedWkt = expectedWkt;
  }

  @Test
  public void normalizePolygon() throws ParseException {
    final Polygon polygon = (Polygon) new WKTReader().read(originalWkt);
    GeometryUtils.adjustForDateline(polygon);
    assertThat(polygon.toText(), is(expectedWkt));
  }
}
