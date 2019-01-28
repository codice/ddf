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
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MetacardToKmlTest {

  @Mock private org.locationtech.jts.geom.LineString jtsLineString;

  @Test
  public void getKmlPointGeoFromWkt() throws CatalogTransformerException {
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

    doReturn(new org.locationtech.jts.geom.Coordinate()).when(jtsLineString).getCoordinate();

    final Geometry newKmlGeometry =
        MetacardToKml.addJtsGeoPointsToKmlGeo(jtsLineString, kmlLineString);

    assertTrue(newKmlGeometry instanceof MultiGeometry);

    final MultiGeometry multiGeometry = (MultiGeometry) newKmlGeometry;
    assertThat(
        "New KML Geometry should be MultiGeometry containing 2 Geometries",
        multiGeometry.getGeometry(),
        hasSize(2));
  }

  @Test
  public void getKmlGeoFromJtsGeo() throws CatalogTransformerException {
    final org.locationtech.jts.geom.Geometry jtsGeo =
        new GeometryFactory().createPoint(new org.locationtech.jts.geom.Coordinate(1.0, 2.0));

    final Geometry kmlGeo = MetacardToKml.getKmlGeoFromJtsGeo(jtsGeo);

    assertTrue(kmlGeo instanceof Point);

    final Point kmlPoint = (Point) kmlGeo;

    assertThat(kmlPoint.getCoordinates(), hasSize(1));
    assertThat(kmlPoint.getCoordinates().get(0).getLongitude(), is(1.0));
    assertThat(kmlPoint.getCoordinates().get(0).getLatitude(), is(2.0));
  }

  @Test(expected = CatalogTransformerException.class)
  public void getKmlGeoFromJtsGeoError() throws CatalogTransformerException {
    final org.locationtech.jts.geom.Geometry jtsGeo =
        Mockito.mock(org.locationtech.jts.geom.Geometry.class);

    doReturn("UNKNOWN").when(jtsGeo).getGeometryType();

    MetacardToKml.getKmlGeoFromJtsGeo(jtsGeo);
  }

  @Test
  public void getJtsGeoFromWkt() throws CatalogTransformerException {
    final org.locationtech.jts.geom.Geometry jtsGeoFromWkt =
        MetacardToKml.getJtsGeoFromWkt("LINESTRING (1 10, 2 20)");

    assertThat(jtsGeoFromWkt.getGeometryType(), is("LineString"));

    final org.locationtech.jts.geom.Coordinate coordinate1 = jtsGeoFromWkt.getCoordinates()[0];
    assertThat(coordinate1.x, is(1.0));
    assertThat(coordinate1.y, is(10.0));

    final org.locationtech.jts.geom.Coordinate coordinate2 = jtsGeoFromWkt.getCoordinates()[1];
    assertThat(coordinate2.x, is(2.0));
    assertThat(coordinate2.y, is(20.0));
  }

  @Test(expected = CatalogTransformerException.class)
  public void getJtsGeoFromWktInvalidWkt() throws CatalogTransformerException {
    MetacardToKml.getJtsGeoFromWkt("x");
  }
}
