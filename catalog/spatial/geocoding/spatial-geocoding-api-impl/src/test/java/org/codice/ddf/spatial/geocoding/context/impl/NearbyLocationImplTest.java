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
package org.codice.ddf.spatial.geocoding.context.impl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.text.NumberFormat;
import java.util.Locale;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.junit.Test;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.impl.PointImpl;

public class NearbyLocationImplTest {
  @Test
  public void testNearbyLocationImpl() {
    final Point source = new PointImpl(50, 50, SpatialContext.GEO);
    final Point nearby = new PointImpl(50.5, 50.5, SpatialContext.GEO);

    final NearbyLocation nearbyLocation = new NearbyLocationImpl(source, nearby, "Nearby");

    assertThat(nearbyLocation.getCardinalDirection(), is("SW"));

    // This distance value was obtained from http://www.movable-type.co.uk/scripts/latlong.html
    String expected = NumberFormat.getNumberInstance(Locale.getDefault()).format(65.99);
    assertThat(String.format("%.2f", nearbyLocation.getDistance()), is(expected));

    assertThat(nearbyLocation.getName(), is("Nearby"));
  }
}
