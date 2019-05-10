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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.source;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import org.junit.Test;

public class LonLatCoordinateStrategyTest {
  private final LonLatCoordinateStrategy lonLatCoordinateStrategy = new LonLatCoordinateStrategy();

  @Test
  public void toStringSingleCoordinateReturnsCommaSeparatedCoordinateValuesInLatLonOrder() {
    assertThat(lonLatCoordinateStrategy.toString(new Coordinate(10, 20)), is("10.0,20.0"));
  }

  @Test
  public void toStringMultipleCoordinatesReturnsSpaceSeparatedCoordinatePairsInLatLonOrder() {
    assertThat(
        lonLatCoordinateStrategy.toString(
            new Coordinate[] {new Coordinate(10, 20), new Coordinate(20, 30)}),
        is("10.0,20.0 20.0,30.0"));
  }

  @Test
  public void lowerCornerReturnsEnvelopeMinimumValuesInLatLonOrder() {
    final Envelope envelope = new Envelope(10, 20, 30, 40);
    assertThat(lonLatCoordinateStrategy.lowerCorner(envelope), contains(10.0, 30.0));
  }

  @Test
  public void upperCornerReturnsEnvelopeMaximumValuesInLatLonOrder() {
    final Envelope envelope = new Envelope(10, 20, 30, 40);
    assertThat(lonLatCoordinateStrategy.upperCorner(envelope), contains(20.0, 40.0));
  }
}
