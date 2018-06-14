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

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import ddf.catalog.transform.CatalogTransformerException;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Point;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MetacardToKmlTest {

  @Mock private com.vividsolutions.jts.geom.LineString jtsLineString;

  @Test
  public void wktPointToKmlPoint() throws CatalogTransformerException {
    final Point kmlGeoFromWkt = (Point) MetacardToKml.getKmlGeoFromWkt("POINT (2 3)");
    final List<Coordinate> coordinates = kmlGeoFromWkt.getCoordinates();
    assertThat(coordinates, hasSize(1));
    assertThat(coordinates.get(0).getLatitude(), is(3.0));
    assertThat(coordinates.get(0).getLongitude(), is(2.0));
  }

  @Test
  public void addJtsGeoPointsToKmlGeo() {
    final LineString kmlLineString = new LineString();
    kmlLineString.setCoordinates(singletonList(new Coordinate(80.0, 170.0)));

    doReturn(new com.vividsolutions.jts.geom.Coordinate()).when(jtsLineString).getCoordinate();

    final Geometry newKmlGeometry =
        MetacardToKml.addJtsGeoPointsToKmlGeo(jtsLineString, kmlLineString);

    assertTrue(newKmlGeometry instanceof MultiGeometry);

    final MultiGeometry multiGeometry = (MultiGeometry) newKmlGeometry;
    assertThat(
        "New KML Geometry should be MultiGeometry containing 2 Geometries",
        multiGeometry.getGeometry(),
        hasSize(2));
  }
}
