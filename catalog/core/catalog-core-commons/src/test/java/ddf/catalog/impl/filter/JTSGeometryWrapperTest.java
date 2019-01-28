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
package ddf.catalog.impl.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

public class JTSGeometryWrapperTest {
  JTSGeometryWrapper toTest;

  @Test
  public void testComputeJTSPeerWithNullGeometry() {
    toTest = new JTSGeometryWrapper(null);
    assertNull(toTest.computeJTSPeer());
  }

  @Test
  public void testComputeJTSPeerWithGeometry() {
    GeometryFactory fac = new GeometryFactory();
    Geometry toWrap = fac.createPoint(new Coordinate(0, 0));
    toTest = new JTSGeometryWrapper(toWrap);
    assertEquals(toWrap, toTest.computeJTSPeer());
  }
}
