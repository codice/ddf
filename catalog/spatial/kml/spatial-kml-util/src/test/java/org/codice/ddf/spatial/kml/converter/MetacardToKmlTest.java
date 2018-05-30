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
package org.codice.ddf.spatial.kml.converter;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import ddf.catalog.transform.CatalogTransformerException;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Point;
import java.util.List;
import org.junit.Test;

public class MetacardToKmlTest {

  @Test
  public void wktPointToKmlPoint() throws CatalogTransformerException {
    final Point kmlGeoFromWkt = (Point) MetacardToKml.getKmlGeoFromWkt("POINT (2 3)");
    final List<Coordinate> coordinates = kmlGeoFromWkt.getCoordinates();
    assertThat(coordinates, hasSize(1));
    assertThat(coordinates.get(0).getLatitude(), is(3.0));
    assertThat(coordinates.get(0).getLongitude(), is(2.0));
  }
}
